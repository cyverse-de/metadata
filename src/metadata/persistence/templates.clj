(ns metadata.persistence.templates
  (:use [clojure-commons.core :only [remove-nil-values]]
        [clojure-commons.assertions :only [assert-found]]
        [korma.core :exclude [update]])
  (:require [cheshire.core :as json]
            [korma.core :as sql]))

(declare format-attribute)

(defn- add-deleted-where-clause
  [query hide-deleted?]
  (if hide-deleted?
    (where query {:deleted false})
    query))

(defn list-templates
  ([]
     (list-templates true))
  ([hide-deleted?]
     (select :templates
             (fields :id :name :description :deleted :created_by :created_on :modified_by :modified_on)
             (add-deleted-where-clause hide-deleted?)
             (order :name))))

(defn- add-attr-synonyms
  [{:keys [id] :as attr}]
  (->> (select (sqlfn :attribute_synonyms id))
       (map :id)
       (doall)
       (assoc attr :synonyms)))

(defn- list-attr-enum-values
  [attr-id]
  (select :attr_enum_values
          (fields :id
                  :value
                  :is_default)
          (where {:attribute_id attr-id})
          (order :display_order)))

(defn- add-attr-enum-values
  [{:keys [id type] :as attr}]
  (if (= "Enum" type)
    (assoc attr :values (list-attr-enum-values id))
    attr))

(defn- format-attr-settings
  [{:keys [settings] :as attr}]
  (if settings
    (assoc attr :settings (json/decode settings))
    attr))

(defn- attr-fields
  [query]
  (fields query
          [:attr.id          :id]
          [:attr.name        :name]
          [:attr.description :description]
          [:attr.required    :required]
          [:attr.settings    :settings]
          [:value_type.name  :type]
          [:attr.created_by  :created_by]
          [:attr.created_on  :created_on]
          [:attr.modified_by :modified_by]
          [:attr.modified_on :modified_on]))

(defn- metadata-attribute-base-query
  []
  (-> (select* [:attributes :attr])
      (join [:value_types :value_type] {:attr.value_type_id :value_type.id})
      attr-fields))

(defn- get-nested-attributes [{parent-id :id}]
  (-> (metadata-attribute-base-query)
      (join [:attr_attrs :aa] {:attr.id :aa.child_id})
      (where {:aa.parent_id parent-id})
      (order :aa.display_order)
      select))

(defn- add-nested-attributes [attr]
  (if-let [nested-attributes (seq (get-nested-attributes attr))]
    (assoc attr :attributes (mapv format-attribute nested-attributes))
    attr))

(defn- format-attribute
  [attr]
  (->> attr
       format-attr-settings
       add-attr-synonyms
       add-attr-enum-values
       add-nested-attributes
       remove-nil-values))

(defn- list-metadata-template-attributes
  [template-id]
  (select [:template_attrs :mta]
          (join [:attributes :attr] {:mta.attribute_id :attr.id})
          (join [:value_types :value_type] {:attr.value_type_id :value_type.id})
          (attr-fields)
          (where {:mta.template_id template-id})
          (order [:mta.display_order])))

(defn- get-metadata-template
  [id]
  (first (select [:templates :template]
                 (fields :id
                         :name
                         :description
                         :deleted
                         :created_by
                         :created_on
                         :modified_by
                         :modified_on)
                 (where {:id id}))))

(defn view-template
  [id]
  (when-let [template (get-metadata-template id)]
    (->> (list-metadata-template-attributes id)
         (map format-attribute)
         (assoc template :attributes))))

(defn- get-metadata-attribute
  [id]
  (-> (metadata-attribute-base-query)
      (where {:attr.id id})
      select
      first))

(defn view-attribute
  [id]
  (when-let [attr (get-metadata-attribute id)]
    (format-attribute attr)))

(defn- prepare-attr-enum-value-insertion
  [attr-id order enum-value]
  (->> (assoc (select-keys enum-value [:id :value :is_default])
         :attribute_id  attr-id
         :display_order order)
       (remove-nil-values)))

(defn- add-attr-enum-value
  [attr-id order enum-value]
  (insert :attr_enum_values
          (values (prepare-attr-enum-value-insertion attr-id order enum-value))))

(defn- insert-template-attr
  [template-id order attr-id]
  (insert :template_attrs (values {:template_id   template-id
                                   :attribute_id  attr-id
                                   :display_order order})))

(defn get-value-type-names
  []
  (map #(:name %) (select :value_types (fields :name))))

(defn- get-value-type-id
  [type-name]
  (:id (first (select :value_types (where {:name type-name})))))

(defn- save-attr-settings
  "Saves arbitrary JSON settings for an attribute."
  [attr-id settings]
  (exec-raw ["UPDATE attributes SET settings = CAST ( ? AS json ) WHERE id = ?"
             [(cast Object (json/encode settings)) attr-id]]))

(defn- prepare-attr-insertion
  [user {:keys [type] :as attribute}]
  (->> (assoc (select-keys attribute [:id :name :description :required])
         :created_by    user
         :modified_by   user
         :value_type_id (assert-found (get-value-type-id type) "value type" type))
       (remove-nil-values)))

(defn- insert-attribute
  [user {:keys [settings] :as attribute}]
  (let [attr-id (:id (insert :attributes (values (prepare-attr-insertion user attribute))))]
    (when settings
      (save-attr-settings attr-id settings))
    attr-id))

(defn- add-template-attribute
  [user template-id order {enum-values :values :as attribute}]
  (let [attr-id (insert-attribute user attribute)]
    (insert-template-attr template-id order attr-id)
    (dorun (map-indexed (partial add-attr-enum-value attr-id) enum-values))))

(defn- prepare-template-insertion
  [user template]
  (->> (assoc (select-keys template [:id :name :description])
         :created_by  user
         :modified_by user)
       (remove-nil-values)))

(defn- insert-template
  [user template]
  (:id (insert :templates (values (prepare-template-insertion user template)))))

(defn add-template
  [user {:keys [attributes] :as template}]
  (let [template-id (insert-template user template)]
    (dorun (map-indexed (partial add-template-attribute user template-id) attributes))
    template-id))

(defn- prepare-template-update
  [user template]
  (->> (assoc (select-keys template [:name :deleted :description])
         :modified_by user
         :modified_on (sqlfn now))
       (remove-nil-values)))

(defn- prepare-attr-update
  [user {:keys [type] :as attr}]
  (->> (assoc (select-keys attr [:id :name :description :required])
         :modified_by   user
         :modified_on   (sqlfn now)
         :value_type_id (assert-found (get-value-type-id type) "value type" type))
       (remove-nil-values)))

(defn- update-attribute
  [user attr-id {:keys [settings] :as attr}]
  (sql/update :attributes
          (set-fields (prepare-attr-update user attr))
          (where {:id attr-id}))
  (when settings
    (save-attr-settings attr-id settings))
  (:id (first (select :attributes (where {:id attr-id})))))

(defn- attr-exists?
  [attr-id]
  (and (not (nil? attr-id))
       (not (nil? (get-metadata-attribute attr-id)))))

(defn- insert-or-update-attribute
  [user {:keys [id] :as attr}]
  (if (attr-exists? id)
    (update-attribute user id attr)
    (insert-attribute user attr)))

(defn- update-template-attribute
  [user template-id order {enum-values :values id :id :as attr}]
  (let [attr-id (insert-or-update-attribute user attr)]
    (insert-template-attr template-id order attr-id)
    (delete :attr_enum_values (where {:attribute_id attr-id}))
    (dorun (map-indexed (partial add-attr-enum-value attr-id) enum-values))))

(defn- template-attr-subselect
  []
  (subselect [:template_attrs :ta]
             (where {:attributes.id :ta.attribute_id})))

(defn- attr-synonym-subselect
  []
  (subselect [:attr_synonyms :s]
             (where {:attributes.id :s.synonym_id})))

(defn- delete-orphan-attributes
  []
  (delete :attributes
          (where (and (not (exists (template-attr-subselect)))
                      (not (exists (attr-synonym-subselect)))))))

(defn update-template
  [user template-id {:keys [attributes] :as template}]
  (assert-found (get-metadata-template template-id) "metadata template" template-id)
  (sql/update :templates
          (set-fields (prepare-template-update user template))
          (where {:id template-id}))
  (delete :template_attrs (where {:template_id template-id}))
  (dorun (map-indexed (partial update-template-attribute user template-id) attributes))
  (delete-orphan-attributes)
  template-id)

(defn- prepare-template-deletion
  [user]
  {:deleted     true
   :modified_by user
   :modified_on (sqlfn now)})

(defn delete-template
  [user template-id]
  (assert-found (get-metadata-template template-id) "metadata template" template-id)
  (sql/update :templates
          (set-fields (prepare-template-deletion user))
          (where {:id template-id}))
  nil)

;; TODO: depending on how synonyms are going to be implemented, it may be necessary to check for existing synonyms
;; before deleting the attributes as well.
(defn- used-attribute-subselect
  []
  (subselect [:template_attrs :ta] (where {:ta.attribute_id :attributes.id})))

(defn permanently-delete-template
  [template-id]
  (assert-found (get-metadata-template template-id) "metadata template" template-id)
  (delete :templates (where {:id template-id}))
  (delete :attributes (where (not (exists (used-attribute-subselect)))))
  nil)
