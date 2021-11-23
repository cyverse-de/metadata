(ns metadata.persistence.ontologies
  (:use [kameleon.util.search])
  (:require [metadata.util.db :refer [ds t]]
            [next.jdbc :as jdbc]
            [next.jdbc.plan :as plan]
            [next.jdbc.sql :as jsql]
            [next.jdbc.types :as jtypes]
            [honey.sql :as sql]
            [honey.sql.helpers :as h]
            [korma.core :as ksql]))

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

(defn add-classes
  [ontology-version classes]
  (let [class-values (map #(assoc % :ontology_version ontology-version) classes)]
    (when-not (empty? class-values)
      (ksql/insert :ontology_classes (ksql/values class-values)))))

(defn get-classes
  [ontology-version]
  (ksql/select :ontology_classes
          (ksql/fields :iri
                  :label
                  :description)
          (ksql/where {:ontology_version ontology-version})))

(defn- search-classes-base
  [ontology-version search-term]
  (let [search-term (str "%" (format-query-wildcards search-term) "%")]
    (-> (ksql/select* :ontology_classes)
        (ksql/where {:ontology_version    ontology-version
                (ksql/sqlfn lower :label) [like (ksql/sqlfn lower search-term)]}))))

(defn search-classes-subselect
  [ontology-version search-term]
  (-> (search-classes-base ontology-version search-term)
      (ksql/subselect (ksql/fields :iri))))

(defn delete-classes
  [ontology-version class-iris]
  (ksql/delete :ontology_classes
          (ksql/where {:ontology_version ontology-version
                  :iri              [in class-iris]})))

(defn add-hierarchies
  [ontology-version class-subclass-pairs]
  (when-not (empty? class-subclass-pairs)
    (let [hierarchy-values (map #(assoc % :ontology_version ontology-version) class-subclass-pairs)]
      (ksql/insert :ontology_hierarchies (ksql/values hierarchy-values)))))

(defn get-ontology-hierarchy-pairs
  [ontology-version]
  (ksql/select :ontology_hierarchies
          (ksql/fields :class_iri :subclass_iri)
          (ksql/where {:ontology_version ontology-version})))

(defn get-ontology-hierarchy-roots
  [ontology-version]
  (ksql/select :ontology_hierarchies
          (ksql/modifier "DISTINCT")
          (ksql/fields :class_iri)
          (ksql/where {:ontology_version ontology-version
                  :class_iri [not-in (ksql/subselect :ontology_hierarchies
                                                (ksql/fields :subclass_iri)
                                                (ksql/where {:ontology_version ontology-version}))]})))

(defn get-ontology-class-hierarchy
  "Gets the class hierarchy rooted at the class with the given IRI."
  [ontology-version root-iri]
  (ksql/select (ksql/sqlfn :ontology_class_hierarchy ontology-version root-iri)))
