(ns metadata.persistence.favorites
  (:use [korma.core :exclude [update]])
  (:require [kameleon.db :as db]
            [metadata.util.db :refer [ds t]]
            [next.jdbc :as jdbc]
            [next.jdbc.plan :as plan]
            [next.jdbc.sql :as jsql]
            [next.jdbc.types :as jtypes]
            [honey.sql :as sql]
            [honey.sql.helpers :as h])
  (:import [java.util UUID]))

(defn is-favorite?
  "Indicates whether or not given target is a favorite of the given authenticated user.

   Parameters:
     user      - the authenticated user name
     target-id - the UUID of the thing being marked as a user favorite

   Returns:
     It returns true if the give target has been marked as a favorite, otherwise it returns false.
     It also returns false if the user or target doesn't exist."
  [user ^UUID target-id]
  (let [q (-> (h/select [[:> :%count.* 0] :is_favorite])
              (h/from (t "favorites"))
              (h/where [:= :target_id target-id]
                       [:= :owner_id user]))]
    (plan/select-one! ds :is_favorite (sql/format q))))

(defn- base-select-favorites
  "A base selection query for all targets of a given type that have are favorites of a given
   authenticated user.

   Parameters:
     user         - the authenticated user name
     target-types - the set of types of target may belong to
                    (`analysis`|`app`|`file`|`folder`|`user`)

   Returns:
     A base selection query."
  [user target-types]
  (-> (h/select :target_id)
      (h/from (t "favorites"))
      (h/where [:in :target_type [:inline target-types]]
               [:= :owner_id user])))

(defn select-favorites-of-type
  "Selects all targets of a given type that have are favorites of a given authenticated user.

   Parameters:
     user         - the authenticated user name
     target-types - the set of types of target may belong to
                    (`analysis`|`app`|`file`|`folder`|`user`)
     target-ids   - (optional) filter the result set to IDs that match those in this set

   Returns:
     It returns a sequence of favorite target UUIDs. If the user doesn't exist, the sequence
     will be empty."
  ([user target-types]
    (plan/select! ds :target_id (sql/format (base-select-favorites user target-types))))
  ([user target-types target-ids]
    (plan/select! ds :target_id
                  (sql/format
                    (-> (base-select-favorites user target-types)
                        (h/where [:in :target_id target-ids]))))))

(defn insert-favorite
  "Marks a given target as a favorite of the given authenticated user. It assumes the authenticated
   user exists and the target is of the indicated type.

   Parameters:
     user        - the authenticated user name
     target-id   - the UUID of the target
     target-type - the type of target (`analysis`|`app`|`file`|`folder`|`user`)"
  [user target-id target-type]
  (jsql/insert! ds
                (t "favorites")
                {:target_id target-id
                 :target_type (jtypes/as-other target-type)
                 :owner_id user})
  nil)

(defn delete-favorite
  "Unmarks a given target as a favorite of the given authenticated user.

   Parameters:
     user      - the authenticated user name
     target-id - the UUID of the target"
  [user target-id]
  (jsql/delete! ds
                (t "favorites")
                {:target_id target-id
                 :owner_id user})
  nil)

(defn delete-favorites-of-type
  "Completely clears a user's list of favorites for the given target types.

   Parameters:
     user         - the authenticated user name
     target-types - the types of targets to remove from the list."
  [user target-types]
  (let [q (-> (h/delete-from (t "favorites"))
              (h/where [:in :target_type [:inline target-types]]
                       [:= :owner_id user]))]
    (jdbc/execute! ds (sql/format q))))
