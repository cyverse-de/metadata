(ns metadata.persistence.comments
  (:require [metadata.util.db :refer [ds t]]
            [next.jdbc :as jdbc]
            [next.jdbc.plan :as plan]
            [next.jdbc.sql :as jsql]
            [next.jdbc.types :as jtypes]
            [honey.sql :as sql]
            [honey.sql.helpers :as h]))

(def comment-columns [:id :owner_id :post_time :retracted :retracted_by :value])

(defn- comments-base-query
  [cols]
  (-> (apply h/select cols)
      (h/from (t "comments"))))

(defn- fmt-comment
  [comment]
  (when comment
    {:id           (:id comment)
     :commenter    (:owner_id comment)
     :post_time    (:post_time comment)
     :retracted    (:retracted comment)
     :retracted_by (:retracted_by comment)
     :comment      (:value comment)}))

(defn comment-on?
  "Indicates whether or not a given comment was attached to a given target.

   Parameters:
     comment-id - The UUID of the comment
     target-id  - The UUID of the target

   Returns:
     It returns true if the comment is attached to the target, otherwise it returns false."
  [comment-id target-id]
  (let [q (-> (h/select [[:> :%count.* 0] :comment_exists])
              (h/from (t "comments"))
              (h/where [:= :id comment-id]
                       [:= :target_id target-id]
                       [:= :deleted false]))]
    (plan/select-one! ds :comment_exists (sql/format q))))

(defn select-comment
  "Retrieves a comment resource

    Parameters:
      comment-id - The UUID of the comment

    Returns:
      The comment resource or nil if comment-id isn't a comment that hasn't been deleted."
  [comment-id]
  (let [q (-> (comments-base-query comment-columns)
              (h/where [:= :id comment-id]
                       [:= :deleted false]))]
    (plan/select-one! ds fmt-comment (sql/format q))))

(defn select-all-comments
  "Retrieves all undeleted comments attached to a given target.

   Parameters:
     target-id - The UUID of the target of interest

   Returns:
     It returns a collection of comment resources attached to the target. If the target doesn't
     exist, an empty collection will be returned."
  [target-id]
  (let [q (-> (comments-base-query comment-columns)
              (h/where [:= :target_id target-id]
                       [:= :deleted false]))]
    (plan/select! ds fmt-comment (sql/format q))))

(defn select-user-comments
  "Retrieves a listing of all comments that were added by a single user.

   Parameters:
     commenter-id - The username of the person who added the comments

   Returns:
     A lazy sequence of detailed comment information."
  [commenter-id]
  (let [select-cols [:id [:owner_id :commenter] :post_time :retracted :retracted_by [:value :comment] :deleted :target_id :target_type]
        col-keys (mapv #(if (vector? %) (nth % 1) %) select-cols)
        q (-> (comments-base-query select-cols)
              (h/where [:= :owner_id commenter-id]))]
    (plan/select! ds col-keys (sql/format q))))

(defn insert-comment
  "Inserts a comment into the comments table.

   Parameters:
     owner       - The authenticated user making the comment
     target-id   - The UUID of the thing being commented on
     target-type - The type of target (`analysis`|`app`|`data`|`user`)
     comment     - The comment

   Returns:
     It returns a comment resource"
  [owner target-id target-type comment]
  (let [insert-vals (jsql/insert! ds
                                  (t "comments")
                                  {:owner_id    owner
                                   :target_id   target-id
                                   :target_type (jtypes/as-other target-type)
                                   :value       comment})]
    (select-comment (:comments/id insert-vals))))

(defn delete-user-comments
  "Deletes all comments that were added by a single user.

   Parameters:
     commenter-id - The username of the person who added the comments"
  [commenter-id]
  (jsql/delete! ds (t "comments") {:owner_id commenter-id}))

(defn retract-comment
  "Marks a comment as retracted. It assumes the retracting user is an authenticated user. If the
   comment doesn't exist, it silently fails.

   Parameters:
     comment-id      - The UUID of the comment being retracted
     retracting-user - The authenticated user retracting the comment."
  [comment-id retracting-user]
  (jsql/update! ds
                (t "comments")
                {:retracted true
                 :retracted_by retracting-user}
                {:id comment-id})
  nil)

(defn readmit-comment
  "Unmarks a comment as retracted. If the comment doesn't exist, it silently fails.

   Parameters:
     comment-id - The UUID of the comment being readmitted."
  [comment-id]
  (jsql/update! ds
                (t "comments")
                {:retracted false
                 :retracted_by nil}
                {:id comment-id})
  nil)

(defn mark-comment-deleted
  "Deletes a comment from the metadata database. If the comment doesn't exist, it silently fails.

   Parameters:
     comment-id - The UUID of the comment being deleted."
  [comment-id deleted?]
  (jsql/update! ds
                (t "comments")
                {:deleted deleted?}
                {:id comment-id}))
