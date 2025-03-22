(ns metadata.routes.schemas.common
  (:require [common-swagger-api.schema :refer [describe StandardUserQueryParams]]
            [common-swagger-api.schema.metadata :as schema]
            [schema.core :as s])
  (:import [java.util UUID]))

(def TargetTypes (concat schema/DataTypes ["analysis" "app" "user" "quick_launch" "instant_launch"]))

(def TargetTypeEnum (apply s/enum TargetTypes))

(def DataTypeParam (describe schema/DataTypeEnum "The type of the requested data item."))

(s/defschema StandardDataItemQueryParams
  (assoc StandardUserQueryParams
         :data-type DataTypeParam))

(s/defschema AvuSearchParams
  (merge schema/AvuSearchParams
         {(s/optional-key :target-type) (describe [TargetTypeEnum] "Target types to search for.")}))

(s/defschema AvuSearchQueryParams
  (merge StandardUserQueryParams
         AvuSearchParams))

(s/defschema TargetIDList
  {:target-ids (describe [UUID] "A list of target IDs")})

(s/defschema TargetTypesList
  {:target-types (describe [TargetTypeEnum]
                           (str "The types of the given IDs."
                                " This list will usually only contain 1 type,"
                                " but multiple are supported for cases such as `file` and `folder` types"
                                " where it is known that the `target-ids` will be unique within those types"))})

(s/defschema DataIdList
  {:filesystem (describe [UUID] "A list of UUIDs, each for a file or folder")})

(s/defschema TargetFilterRequest
  (merge {:target-ids (describe [UUID] "List of IDs to filter")}
         TargetTypesList))

(s/defschema TargetItem
  {:id   schema/TargetIdParam
   :type (describe TargetTypeEnum "The type of this data item")})

(s/defschema TargetItemList
  {:targets (describe [TargetItem] "A list of target items")})
