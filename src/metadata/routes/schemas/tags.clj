(ns metadata.routes.schemas.tags
  (:use [clojure-commons.error-codes]
        [common-swagger-api.schema :only [->optional-param
                                          describe
                                          ErrorResponse
                                          ErrorResponseNotFound
                                          NonBlankString
                                          StandardUserQueryParams]]
        [common-swagger-api.schema.metadata.tags]
        [metadata.routes.schemas.common])
  (:require [schema.core :as s]))

(s/defschema TagSuggestUserQueryParams
  (merge StandardUserQueryParams
         TagSuggestQueryParams))

(s/defschema UpdateAttachedTagsQueryParams
  (merge StandardDataItemQueryParams
    TagTypeEnum))

(def PatchTagsResponses
  (merge {200 {:schema      AttachedTagsListing
               :description "The tags were attached or detached from the file or folder"}
          400 PatchTags400Response
          404 PatchTags404Response}
         TagDefaultErrorResponses))

(def PostTagResponses
  (merge {200 {:schema      TagDetails
               :description "The tag was successfully created"}
          400 PostTag400Response}
         TagDefaultErrorResponses))

(def PatchTagResponses
  (merge {200 {:schema      TagDetails
               :description "The tag was successfully updated"}
          400 PostTag400Response}
         TagDefaultErrorResponses))