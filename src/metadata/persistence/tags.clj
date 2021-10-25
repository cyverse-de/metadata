(ns metadata.persistence.tags
  (:use [korma.core :exclude [update]])
  (:require [kameleon.db :as db]
            [korma.core :as ksql]
            [metadata.util.db :refer [ds t]]
            [next.jdbc :as jdbc]
            [next.jdbc.plan :as plan]
            [next.jdbc.sql :as jsql]
            [next.jdbc.types :as jtypes]
            [honey.sql :as sql]
            [honey.sql.helpers :as h])
  (:import [java.util UUID]
           [clojure.lang IPersistentMap ISeq]))

(defn get-tag
  "Retrieves the tag with the given ID.

   Parameters:
     tag-id - The UUID of tag.

   Returns:
     The tag with the given ID, or nil if the tag doesn't exist."
  [tag-id]
  (let [cols [:id :value :description :public :owner_id :created_on :modified_on]
        q (-> (apply h/select cols)
              (h/from (t "tags"))
              (h/where [:= :id tag-id]))]
    (plan/select-one! ds cols (sql/format q))))

(defn filter-tags-owned-by-user
  "Filters a set of tags for those owned by the given user.

   Parameters:
     owner   - the owner of the tags to keep
     tag-ids - the UUIDs of the tags to filter

   Returns:
     It returns a lazy sequence of tag UUIDs owned by the given user."
  [owner tag-ids]
  (let [q (-> (h/select :id)
              (h/from (t "tags"))
              (h/where [:= :owner_id owner]
                       [:in :id tag-ids]))]
    (plan/select! ds :id (sql/format q))))

(defn- tags-base-query
  "Creates the base query for attached tags.

   Returns:
     The base query."
  []
  (-> (select* :tags)
      (fields :id :value :description)))


(defn get-tags-by-value
  "Retrieves up to a certain number of the tags owned by a given user that have a value matching a
   given pattern. If no max number is provided, all tags will be returned.

   Parameters:
     owner       - the owner of the tags to return
     value-glob  - the pattern of the value to match. A `%` means zero or more of any character. A
                   `_` means one of any character.
     max-results - (OPTIONAL) If this is provided, no more than this number of results will be
                   returned.

   Returns:
     A lazy sequence of tags."
  [owner value-glob & [max-results]]
  (let [query  (-> (tags-base-query)
                   (where (and {:owner_id owner}
                               (raw (str "lower(value) like lower('" value-glob "')")))))
        query' (if max-results
                 (-> query (limit max-results))
                 query)]
    (select query')))

(defn get-tag-owner
  "Retrieves the user name of the owner of the given tag.

   Parameters:
     tag-id - The UUID of tag.

   Returns:
     The user name or nil if the tag doesn't exist."
  [tag-id]
  (let [q (-> (h/select :owner_id)
              (h/from (t "tags"))
              (h/where [:= :id tag-id]))]
    (plan/select-one! ds :owner_id (sql/format q))))

(defn ^IPersistentMap insert-user-tag
  "Inserts a user tag.

   Parameters:
     owner       - The user name of the owner of the new tag.
     value       - The value of the tag. It must be no more than 255 characters.
     description - The description of the tag. If nil, an empty string will be inserted.

   Returns:
     It returns the new database tag entry."
  [^String owner ^String value ^String description]
  (let [description (if description description "")]
    (insert :tags
      (values {:value       value
               :description description
               :owner_id    owner}))))


(defn ^IPersistentMap update-user-tag
  "Updates a user tag's description and/or value.

   Parameters:
     tag-id  - The UUID of the tag to update
     updates - A map containing the updates. The map may contain only the keys :value and
               :description. It doesn't need to contain both.  The :value key will map to a new
               value for the tag, and the :description key will map to a new description. A nil
               description will be converted to an empty string.

   Returns:
     It returns the updated tag record."
  [^UUID tag-id ^IPersistentMap updates]
  (let [updates (if (get updates :description :not-found)
                  updates
                  (assoc updates :description ""))]
    (ksql/update :tags
      (set-fields updates)
      (where {:id tag-id}))
    (first (select :tags (where {:id tag-id})))))


(defn delete-user-tag
  "This detaches a user tag from all metadata and deletes it.

   Parameters:
     tag-id - The UUID of the tag to delete."
  [tag-id]
  (delete :attached_tags (where {:tag_id tag-id}))
  (delete :tags (where {:id tag-id}))
  nil)


(defn select-tags-defined-by
  "Lists all tags that were defined by a user.

   Parameters:
     user - The username.

   Returns:
     A lazy sequence of tag information."
  [user]
  (select (tags-base-query)
    (where {:owner_id user})))


(defn delete-tags-defined-by
  "Deletes all tags that were defined by a user.

   Parameters:
     user - The username."
  [user]
  (delete :tags (where {:owner_id user})))


(defn select-all-attached-tags
  "Retrieves the set of tags that a user has attached to anything.

   Parameters:
     user - the user name

   Returns:
     A lazy sequence of tag resources"
  [user]
  (select (tags-base-query)
    (where {:owner_id user
            :id       [in (subselect :attached_tags
                            (fields :tag_id)
                            (where {:attacher_id user}))]})))


(defn delete-all-attached-tags
  "Deletes all tag attachments that were added by a user.

   Parameters:
     user - the user name"
  [user]
  (delete :attached_tags
          (where {:attacher_id user})))


(defn select-attached-tags
  "Retrieves the set of tags a user has attached to something.

   Parameters:
     user      - the user name
     target-id - The UUID of the thing of interest

   Returns:
     It returns a lazy sequence of tag resources."
  [user target-id]
  (select (tags-base-query)
    (where {:owner_id user
            :id       [in (subselect :attached_tags
                            (fields :tag_id)
                            (where {:target_id target-id :detached_on nil}))]})))

(defn filter-attached-tags
  "Filter a set of tags for those attached to a given target.

   Parameters:
     target-id - the UUID of the target used to filter the tags
     tag-ids   - the set of tags to filter

   Returns:
     It returns a lazy sequence of tag UUIDs that have been filtered."
  [target-id tag-ids]
  (map :tag_id
    (select :attached_tags
      (fields :tag_id)
      (where {:target_id   target-id
              :detached_on nil
              :tag_id      [in tag-ids]}))))

(defn insert-attached-tags
  "Attach a set of user tags to a target.

   Parameters:
     attacher    - The user name of the user attaching the tags.
     target-id   - The UUID of target receiving tags
     target-type - the type of target (`analysis`|`app`|`data`|`user`)
     tag-ids     - the collection of tags to attach"
  [attacher target-id target-type tag-ids]
  (when-not (empty? tag-ids)
    (let [target-type (db/->enum-val target-type)
          new-values  (map #(hash-map :tag_id      %
                                      :target_id   target-id
                                      :target_type target-type
                                      :attacher_id attacher)
                           tag-ids)]
      (insert :attached_tags (values new-values))
      nil)))

(defn mark-tags-detached
  "Detach a set of user tags from a target.

   Parameters:
     detacher  - The user name of the user detaching the tags.
     target-id - The UUID of the target having some of its tags removed.
     tag-ids   - the collection tags to detach"
  [detacher target-id tag-ids]
  (ksql/update :attached_tags
    (set-fields {:detacher_id detacher
                 :detached_on (sqlfn now)})
    (where {:target_id   target-id
            :detached_on nil
            :tag_id      [in tag-ids]}))
  nil)


(defn ^ISeq select-tag-targets
  "Retrieve all of the objects that have the given tags attached.

   Parameters:
     tag-ids - The ids of the tags

   Returns:
     It returns the attachment records for the tags."
  [^ISeq tag-id & [include-detached?]]
  (letfn [(add-detached-filter [query] (if include-detached? query (where query {:detached_on nil})))]
    (-> (select* :attached_tags)
        (where {:tag_id tag-id})
        (add-detached-filter)
        (select))))
