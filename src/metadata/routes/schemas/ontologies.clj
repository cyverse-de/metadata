(ns metadata.routes.schemas.ontologies
  (:use [common-swagger-api.schema]
        [metadata.routes.schemas.common])
  (:require [common-swagger-api.schema.ontologies :as schema]
            [schema.core :as s]))

(s/defschema OntologyHierarchyFilterParams
  (merge StandardUserQueryParams
         schema/OntologyHierarchyFilterParams))

(s/defschema OntologySearchParams
  (merge StandardUserQueryParams
         {:label (describe String "The ontology class label search term")}))

(s/defschema OntologyDetailsList
  {:ontologies (describe [schema/OntologyDetails] "List of saved Ontologies")})

(s/defschema TargetHierarchyFilterRequest
  (merge TargetItem
         {:attrs
          (describe [String] "The metadata attributes that store class IRIs for the given ontology")}))

(s/defschema OntologySearchFilterRequest
  (merge TargetFilterRequest
         {:attrs
          (describe [String] "The metadata attributes that store class IRIs for the given ontology")}))
