(ns metadata.services.avus
  (:require [korma.db :refer [transaction]]
            [metadata.amqp :as amqp]
            [metadata.persistence.avu :as persistence]))

(defn- filter-targets-by-attr-values
  [target-types target-ids [attr avus]]
  (persistence/filter-targets-by-attrs-values target-types target-ids [attr] (map :value avus)))

(defn filter-targets-by-avus
  "Filters the given target IDs by returning a list of any that have the given attrs and values applied."
  [target-types target-ids avus]
  (let [found-targets (mapcat (partial filter-targets-by-attr-values target-types target-ids)
                              (group-by :attr avus))]
    {:target-ids (seq (set (map :target_id found-targets)))}))

(defn- format-avu
  "Formats the given AVU for adding or updating."
  [target-type target-id {:keys [attr] :as avu}]
  (-> (select-keys avu [:id :value :unit :avus])
      (assoc
        :attribute   attr
        :target_type target-type
        :target_id   target-id)))

(defn list-avus
  ([target-type target-id]
   {:avus (persistence/avu-list target-type target-id)})
  ([{attributes :attribute target-types :target-type target-ids :target-id values :value units :unit}]
   {:avus (persistence/avu-list attributes target-types target-ids values units)}))

(defn- add-or-update-avu
  "Adds or Updates an AVU and its attached AVUs for the given user ID."
  [user-id {:keys [avus] :as avu}]
  (let [{:keys [id]} (persistence/add-or-update-avu user-id avu)]
  (doseq [child-avu avus]
    (add-or-update-avu user-id (format-avu "avu" id child-avu)))))

(defn update-avus
  [user-id target-type target-id {avus :avus}]
  (transaction
   (doseq [avu avus]
     (add-or-update-avu user-id (format-avu target-type target-id avu))))
  (amqp/publish-metadata-update user-id target-id)
  (list-avus target-type target-id))

(defn- remove-orphaned-avus
  "Removes any AVU for the given target-type and target-id that does not have a matching ID in the given
   set of avus."
  [target-type target-id avus-to-keep]
  ;; Cleanup orphaned sub-AVUs of any AVUs to be kept by this request
  (doseq [{:keys [id avus]} avus-to-keep]
    (remove-orphaned-avus "avu" id avus))
  (->> avus-to-keep
       (map :id)
       (remove nil?)
       (persistence/get-avus-by-ids)
       (map :id)
       (persistence/remove-orphaned-avus target-type target-id)))

(defn set-avus
  "Sets AVUs for the given user's data item."
  [user-id target-type target-id {avus :avus}]
  (transaction
   (remove-orphaned-avus target-type target-id avus)
   (doseq [avu avus]
     (add-or-update-avu user-id (format-avu target-type target-id avu))))
  (amqp/publish-metadata-update user-id target-id)
  (list-avus target-type target-id))

(defn- copy-avus-to-dest-targets
  "Copies Metadata AVUs to the given dest-targets."
  [user avus dest-targets]
  (transaction
    (doseq [{:keys [id type]} dest-targets]
      (update-avus user type id {:avus avus}))))

(defn- get-avu-copy
  "Returns only the attr, value, unit, and avus of the given avu.
   The nested avus are also copied recursively with this function."
  [{:keys [avus] :as avu}]
  (when avu
    (-> avu
        (select-keys [:attr :value :unit])
        (assoc :avus (map get-avu-copy avus)))))

(defn- get-avu-copies
  "Fetches the list of Metadata AVUs for the given target,
   returning only the attr, value, and unit of each avu."
  [target-type target-id]
  (let [avus (persistence/avu-list target-type target-id)]
    (map get-avu-copy avus)))

(defn copy-avus
  "Copies Metadata AVUs from the target item to dest-items."
  [user target-type target-id {dest-items :targets}]
  (let [avus (get-avu-copies target-type target-id)]
    (copy-avus-to-dest-targets user avus dest-items)
    nil))

(defn delete-target-avus
  "Deletes the given AVUs from the given targets in a transaction."
  [target-types target-ids avus]
  (transaction
    (doseq [target-id target-ids]
      (doseq [{:keys [attr value unit]} avus]
        (persistence/delete-target-avu target-types
                                       target-id
                                       attr
                                       value
                                       unit))))
  nil)
