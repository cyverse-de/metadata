(ns metadata.persistence.ontologies
  (:use [kameleon.util.search])
  (:require [metadata.util.db :refer [ds t]]
            [next.jdbc :as jdbc]
            [next.jdbc.plan :as plan]
            [next.jdbc.sql :as jsql]
            [next.jdbc.types :as jtypes]
            [honey.sql :as sql]
            [honey.sql.helpers :as h]))

(def ontology-columns [:version :iri :created_by :created_on])
(defn- ontologies-base-query
  [cols]
  (-> (apply h/select cols)
      (h/from (t "ontologies"))))

(defn add-ontology-xml
  [user version iri ontology-xml]
  (jsql/insert! ds
                (t "ontologies")
                {:version    version
                 :iri        iri
                 :xml        ontology-xml
                 :created_by user}))

(defn get-ontology-xml
  [ontology-version]
  (let [q (-> (ontologies-base-query [:xml])
              (h/where [:= :version ontology-version]))]
    (plan/select-one! ds :xml (sql/format q))))

(defn get-ontology-details
  [ontology-version]
  (let [q (-> (ontologies-base-query ontology-columns)
              (h/where [:= :version ontology-version]))]
    (plan/select-one! ds ontology-columns (sql/format q))))

(defn set-ontology-deleted
  [ontology-version deleted]
  (jsql/update! ds
                (t "ontologies")
                {:deleted deleted}
                {:version ontology-version}))

(defn list-ontologies
  []
  (let [q (-> (ontologies-base-query ontology-columns)
              (h/where [:= :deleted false]))]
    (plan/select! ds ontology-columns (sql/format q))))

(def ontology-class-columns [:iri :label :description])
(defn- ontology-classes-base-query
  [cols]
  (-> (apply h/select cols)
      (h/from (t "ontology_classes"))))

(defn add-classes
  [ontology-version classes]
  (let [class-values (map #(assoc % :ontology_version ontology-version) classes)]
    (when-not (empty? class-values)
      (let [new-values (mapv #(vector (:ontology_version %) (:iri %) (:label %) (:description %)) class-values)]
        (jsql/insert-multi! ds
                            (t "ontology_classes")
                            [:ontology_version :iri :label :description]
                            new-values)))))

(defn get-classes
  [ontology-version]
  (let [q (-> (ontology-classes-base-query ontology-class-columns)
              (h/where [:= :ontology_version ontology-version]))]
    (plan/select! ds ontology-class-columns (sql/format q))))

(defn search-classes-subselect
  [ontology-version search-term]
  (let [search-term (str "%" (format-query-wildcards search-term) "%")]
    (-> (h/select :iri)
        (h/from (t "ontology_classes"))
        (h/where [:= :ontology_version ontology-version]
                 [:ilike :label search-term]))))

(defn delete-classes
  [ontology-version class-iris]
  (let [q (-> (h/delete-from (t "ontology_classes"))
              (h/where [:= :ontology_version ontology-version]
                       [:in :iri class-iris]))]
    (jdbc/execute-one! ds (sql/format q))))

(defn add-hierarchies
  [ontology-version class-subclass-pairs]
  (when-not (empty? class-subclass-pairs)
    (let [hierarchy-values (map #(assoc % :ontology_version ontology-version) class-subclass-pairs)
          insert-values (mapv #(vector (:ontology_version %) (:class_iri %) (:subclass_iri %)) hierarchy-values)]
      (jsql/insert-multi! ds
                          (t "ontology_hierarchies")
                          [:ontology_version :class_iri :subclass_iri]
                          insert-values))))

(defn get-ontology-hierarchy-pairs
  [ontology-version]
  (let [q (-> (h/select :class_iri :subclass_iri)
              (h/from (t "ontology_hierarchies"))
              (h/where [:= :ontology_version ontology-version]))]
    (plan/select! ds [:class_iri :subclass_iri] (sql/format q))))

(defn get-ontology-hierarchy-roots
  [ontology-version]
  (let [subq (-> (h/select :subclass_iri)
                 (h/from (t "ontology_hierarchies"))
                 (h/where [:= :ontology_version ontology-version]))
        q (-> (h/select-distinct :class_iri)
              (h/from (t "ontology_hierarchies"))
              (h/where [:= :ontology_version ontology-version]
                       [:not-in :class_iri subq]))]
    (plan/select! ds [:class_iri] (sql/format q))))

(defn get-ontology-class-hierarchy
  "Gets the class hierarchy rooted at the class with the given IRI."
  [ontology-version root-iri]
  (plan/select! ds
                [:parent_iri :iri :label]
                (sql/format
                  (h/select [[:ontology_class_hierarchy ontology-version root-iri]]))))
