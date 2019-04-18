(ns metadata.routes.schemas.avus
  (:use [common-swagger-api.schema :only [describe]]
        [metadata.routes.schemas.common
         :only [TargetFilterRequest
                TargetIDList
                TargetTypesList]])
  (:require [common-swagger-api.schema.metadata :as schema]
            [schema.core :as s]))

(s/defschema AvuFilterRequest
  (select-keys schema/Avu [:attr :value]))

(s/defschema FilterByAvusRequest
  (merge TargetFilterRequest
         {:avus (describe [AvuFilterRequest] "The AVUs to use to filter the given targets")}))

(s/defschema DeleteTargetAvusRequest
  (merge TargetIDList
         TargetTypesList
         {:avus (describe [(select-keys schema/Avu [:attr :value :unit])] "The AVUs to match for removal")}))
