(ns metadata.routes.schemas.common
  (:use [common-swagger-api.schema :only [describe StandardUserQueryParams]]
        [common-swagger-api.schema.metadata])
  (:require [common-swagger-api.schema.metadata :as schema]
            [schema.core :as s])
  (:import [java.util UUID]))

(def TargetTypes (concat DataTypes ["analysis" "app" "user"]))

(def TargetTypeEnum (apply s/enum TargetTypes))

(def DataTypeParam (describe DataTypeEnum "The type of the requested data item."))

(s/defschema StandardDataItemQueryParams
  (assoc StandardUserQueryParams
    :data-type DataTypeParam))

(s/defschema AvuSearchQueryParams
  (assoc StandardUserQueryParams
    (s/optional-key :attribute)   (describe [String] "Attribute names to search for.")
    (s/optional-key :target-type) (describe [TargetTypeEnum] "Target types to search for.")
    (s/optional-key :value)       (describe [String] "Values to search for.")
    (s/optional-key :unit)        (describe [String] "Units to search for.")))

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
