(ns metadata.services.tags
  (:use [kameleon.db :only [millis-from-timestamp]]
        [slingshot.slingshot :only [throw+]])
  (:require [clojure.set :as set]
            [clojure.string :as string]
            [metadata.persistence.tags :as db])
  (:import [java.util UUID]
           [clojure.lang IPersistentMap]))

(defn- format-tag
  [tag]
  (-> tag
      (update :created_on millis-from-timestamp)
      (update :modified_on millis-from-timestamp)))

(defn- get-tag-details
  [id]
  (format-tag (db/get-tag id)))

(defn- get-tag-target-details
  [tag-ids & [{:keys [include-detached?]}]]
  (letfn [(fmt-tgt ([{id :target_id type :target_type}]
                     {:id id :type (str type)}))
          (get-tag-detail ([id]
                            (assoc (get-tag-details id)
                              :targets (map fmt-tgt (db/select-tag-targets id include-detached?)))))]
    {:tags (map get-tag-detail tag-ids)}))


(defn- attach-tags
  [user data-id data-type new-tags]
  (let [tag-set         (set new-tags)
        known-tags      (set (db/filter-tags-owned-by-user user tag-set))
        unknown-tags    (set/difference tag-set known-tags)
        unattached-tags (set/difference known-tags
                                        (set (db/filter-attached-tags data-id known-tags)))]
    (when-not (empty? unknown-tags)
      (throw+ {:type    :clojure-commons.exception/not-found
               :tag-ids unknown-tags}))
    (db/insert-attached-tags user data-id data-type unattached-tags)
    (get-tag-target-details known-tags)))


(defn- detach-tags
  [user data-id tag-ids]
  (let [known-tags (db/filter-tags-owned-by-user user (set tag-ids))]
    (db/mark-tags-detached user data-id known-tags)
    (get-tag-target-details known-tags)))


(defn ^IPersistentMap create-user-tag
  "Creates a new user tag

   Parameters:
     owner - The user name of the owner of the new tag.
     body - This is the request body. It should be a map containing a `value` text field
            and optionally a `description` text field.

   Returns:
     The new tag."
  [^String owner ^IPersistentMap {:keys [value description]}]
  (let [value (string/trim value)
        description (if-not (nil? description) (string/trim description) description)]
    (if (empty? (db/get-tags-by-value owner value))
      (format-tag (db/insert-user-tag owner value description))
      (throw+ {:type  :clojure-commons.exception/not-unique
               :user  owner
               :value value}))))


(defn ^IPersistentMap delete-user-tag
  "Deletes a user tag. This will detach it from all metadata.

   Parameters:
     user - The user name of the requestor.
     tag-id - The tag's UUID from the URL

   Returns:
     A success response with no body.

   Throws:
     ERR_NOT_FOUND - if the text isn't a UUID owned by the authenticated user."
  [^String user ^UUID tag-id]
  (let [tag-owner (db/get-tag-owner tag-id)]
    (when (not= tag-owner user)
      (throw+ {:type   :clojure-commons.exception/not-found
               :tag-id tag-id}))
    (db/delete-user-tag tag-id)
    nil))


(defn handle-patch-file-tags
  "Adds or removes tags to a data item.

   Parameters:
     user - The user name of the requestor.
     data-id - The data-id from the request. It should be a filesystem UUID.
     type - The `type` query parameter. It should be either `attach` or `detach`.
     body - This is the request body. It should be a JSON document containing a `tags` field. This
            field should hold an array of tag UUIDs."
  [user data-id data-type type {mods :tags}]
  (condp = type
    "attach" (attach-tags user data-id data-type mods)
    "detach" (detach-tags user data-id mods)
    (throw+ {:type      :clojure-commons.exception/bad-query-params
             :parameter type})))


(defn list-all-attached-tags
  "Lists all tags attached to any data item by the current user.

   Parameters:
     user - The user name of the requestor."
  [user]
  (let [tag-ids (map :id (db/select-all-attached-tags user))]
    (get-tag-target-details tag-ids {:include-detached? true})))


(defn delete-all-attached-tags
  "Permanently deletes all tag attachments that have been added to a file or folder by the current user.

   Parameters:
     user - The user name of the requestor."
  [user]
  (db/delete-all-attached-tags user))


(defn list-attached-tags
  "Lists the tags attached to a data item.

   Parameters:
     user - The user name of the requestor.
     data-id - The data-id from the request.  It should be a filesystem UUID."
  [user data-id]
  (let [tags (db/select-attached-tags user data-id)]
    {:tags tags}))


(defn list-tags-defined-by
  "Lists all of the tags defined by a user.

   Parameters:
     user - The user name of the requestor."
  [user]
  {:tags (db/select-tags-defined-by user)})


(defn delete-tags-defined-by
  "Deletes all of the tags defined by a user. Any tag that is attached to a target will automatically be detached.

   Parameters:
     user - the user name of the requestor."
  [user]
  (db/delete-tags-defined-by user))


(defn suggest-tags
  "Given a tag value fragment, this function will return a list tags whose values contain that
   fragment.

   Parameters:
     user - The user name of the requestor.
     contains - The `contains` query parameter.
     limit - The `limit` query parameter. It should be a positive integer."
  [user contains limit]
  (let [matches (db/get-tags-by-value user (str "%" contains "%") limit)]
    {:tags matches}))


(defn- prepare-tag-update
  [update-req]
  (let [new-value       (:value update-req)
        new-description (:description update-req)]
    (cond
      (and new-value new-description) {:value new-value :description new-description}
      new-value                       {:value new-value}
      new-description                 {:description new-description})))


(defn ^IPersistentMap update-user-tag
  "updates the value and/or description of a tag.

   Parameters:
     owner   - The user name of the requestor.
     tag-str - The tag-id from request URL. It should be a tag UUID.
     body    - The request body. It should be a JSON document containing at most one `value` text
               field and one `description` text field.

    Returns:
      It returns the response."
  [^String owner ^UUID tag-id ^IPersistentMap tag]
  (let [tag-owner (db/get-tag-owner tag-id)
        do-update (fn []
                    (let [update (prepare-tag-update tag)]
                      (when-not (empty? update)
                        (db/update-user-tag tag-id update)))
                    (get-tag-details tag-id))]
    (cond
      (nil? tag-owner)       (throw+ {:type   :clojure-commons.exception/not-found
                                      :tag-id tag-id})
      (not= owner tag-owner) (throw+ {:type :clojure-commons.exception/not-owner
                                      :user owner})
      :else                  (do-update))))
