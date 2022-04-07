(ns metadata.persistence.avu
  (:use [slingshot.slingshot :only [throw+]])
  (:require [metadata.util.db :refer [ds t]]
            [next.jdbc :as jdbc]
            [next.jdbc.plan :as plan]
            [next.jdbc.sql :as jsql]
            [next.jdbc.types :as jtypes]
            [honey.sql :as sql]
            [honey.sql.helpers :as h]
            [kameleon.db :as db]))

(def avu-columns [:id :attribute :value :unit :target_id :target_type :created_by :modified_by :created_on :modified_on])

(defn- avus-base-query
  [cols]
  (-> (apply h/select cols)
      (h/from (t "avus"))))

(defn filter-targets-by-attrs-values
  "Finds the given targets that have any of the given attributes and values."
  [target-types target-ids attributes values]
  (let [cols [:target_id :target_type]
        q (-> (avus-base-query cols)
              (dissoc :select) ;; change to select distinct
              (h/select-distinct cols)
              (h/where [:in :target_id target-ids]
                       [:in :target_type (map jtypes/as-other target-types)]
                       [:in :attribute attributes]
                       [:in :value values]))]
    (plan/select! ds cols (sql/format q))))

(defn get-avu-by-id
  "Fetches an AVU by its ID."
  [id]
  (let [q (-> (avus-base-query avu-columns)
              (h/where [:= :id id]))]
    (plan/select-one! ds avu-columns (sql/format q))))

(defn get-avus-by-ids
  "Finds existing AVUs by a set of IDs."
  [ids]
  (let [q (-> (avus-base-query avu-columns)
              (h/where [:in :id ids]))]
    (plan/select! ds avu-columns (sql/format q))))


(defn get-avus-for-target
  "Gets AVUs for the given target."
  [target-type target-id]
  (let [q (-> (avus-base-query avu-columns)
              (h/where [:= :target_id target-id]
                       [:= :target_type (jtypes/as-other target-type)]))]
    (plan/select! ds avu-columns (sql/format q))))

(defn get-avus-by-attrs
  "Finds all existing AVUs by the given targets and the given set of attributes."
  [target-types target-ids attributes]
  (let [q (-> (avus-base-query avu-columns)
              (h/where [:in :attribute attributes]
                       [:in :target_id target-ids]
                       [:in :target_type (map jtypes/as-other target-types)]))]
    (plan/select! ds avu-columns (sql/format q))))

(defn add-avus
  "Adds the given AVUs to the Metadata database."
  [user-id avus]
  (let [cols [:id :attribute :value :unit :target_type :target_id :created_by :modified_by]
        fmt-avu (fn [avu]
                  (let [avu (-> avu
                                (update :target_type jtypes/as-other)
                                (assoc :created_by user-id
                                       :modified_by user-id))]
                    (mapv #(get avu %) cols)))]
    (jsql/insert-multi! ds
                        (t "avus")
                        cols
                        (map fmt-avu avus))))

(defn update-avu
  "Updates the attribute, value, unit, modified_by, and modified_on fields of the given AVU."
  [user-id avu]
  (let [q (-> (h/update (t "avus"))
              (h/set (-> (select-keys avu [:attribute :value :unit])
                         (assoc :modified_by user-id
                                :modified_on :%now)))
              (h/where [:= :id (:id avu)]))]
    (jdbc/execute-one! ds (sql/format q))))

(defn- get-avu-for-target
  "Finds an AVU by ID, validating its target_type and target_id match what's fetched from the database.
   Returns nil if an AVU with the given ID does not already exist in the database."
  [{:keys [id] :as avu}]
  (when-let [existing-avu (when id (get-avu-by-id id))]
    (when (not= (select-keys existing-avu [:target_type :target_id])
                (select-keys avu          [:target_type :target_id]))
      (throw+ {:type  :clojure-commons.exception/exists
               :error "AVU already attached to another target item."
               :avu   existing-avu}))
    existing-avu))

(defn- update-valid-avu
  "Updates an AVU, validating its given target_type and target_id match what's already in the database.
   Returns nil if an AVU with the given ID does not already exist in the database."
  [user-id avu]
  (when (get-avu-for-target avu)
    (update-avu user-id avu)
    avu))

(defn- find-matching-avu
  "Finds an existing AVU by attribute, value, unit, target_type, target_id, and (if included) its ID."
  [avu]
  (let [required-keys [:attribute :value :unit :target_type :target_id]]
    (when (every? (partial contains? avu) required-keys)
      (let [avu (update avu :target_type jtypes/as-other)
            keys-used (if (contains? avu :id) (conj required-keys :id) required-keys)
            q (-> (avus-base-query avu-columns)
                  (apply h/where (map #(vector := % (get avu %)) keys-used)))]
        (plan/select-one! ds avu-columns (sql/format q))))))

(defn add-or-update-avu
  [user-id avu]
  (if-let [matching-avu (find-matching-avu avu)]
    matching-avu
    (if-let [existing-avu (update-valid-avu user-id avu)]
      existing-avu
      (add-avus user-id [avu]))))

(defn remove-orphaned-avus
  "Removes AVUs for the given target-id that are not in the given set of avu-ids-to-keep."
  [target-type target-id avu-ids-to-keep]
  (let [add-keep-clause (fn [query avu-ids-to-keep]
                          (if (empty? avu-ids-to-keep)
                            query
                            (h/where query [:not-in :id avu-ids-to-keep])))
        q (-> (avus-base-query [:id])
              (h/where [:= :target_id target-id]
                       [:= :target_type (jtypes/as-other target-type)])
              (add-keep-clause avu-ids-to-keep))
        avus-to-remove (plan/select! ds [:id] (sql/format q))]
    ;; Remove orphaned sub-AVUs of any AVUs to be removed by this request.
    (doseq [{:keys [id]} avus-to-remove]
      (remove-orphaned-avus "avu" id nil))
    (jdbc/execute-one! ds
                       (-> q
                           (dissoc :select)
                           (h/delete-from (t "avus"))
                           sql/format))))

(defn delete-target-avu
  "Deletes the AVU with the given attribute, value, and unit from the given target."
  [target-types target-id attribute value unit]
  (let [q (-> (h/delete-from (t "avus"))
              (h/where [:= :target_id target-id]
                       [:in :target_type (map jtypes/as-other target-types)]
                       [:= :attribute attribute]
                       [:= :value value]
                       [:= :unit unit]))]
    (jdbc/execute-one! ds (sql/format q))))

(defn format-avu
  "Formats a Metadata AVU for JSON responses."
  [{:keys [id attribute] :as avu}]
  (let [convert-timestamp #(update %1 %2 db/millis-from-timestamp)
        attached-avus (map format-avu (get-avus-for-target "avu" id))]
    (-> avu
        (convert-timestamp :created_on)
        (convert-timestamp :modified_on)
        (assoc :attr attribute
               :avus attached-avus)
        (dissoc :attribute :target_type))))

(defn- find-avus
  "Searches for AVUs matching the given criteria."
  [attributes target-types values units]
  (let [add-criterion (fn [query f vs] (if (seq vs) (h/where query [:in f vs]) query))]
    (plan/select! ds avu-columns
      (-> (avus-base-query avu-columns)
          (add-criterion :attribute attributes)
          (add-criterion :target_type (map jtypes/as-other target-types))
          (add-criterion :value values)
          (add-criterion :unit units)
          (sql/format)))))

(defn avu-list
  "Lists AVUs for the given target."
  ([target-type target-id]
   (map format-avu (get-avus-for-target target-type target-id)))
  ([attributes target-types values units]
   (map format-avu (find-avus attributes target-types values units))))
