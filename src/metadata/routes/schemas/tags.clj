(ns metadata.routes.schemas.tags
  (:use [clojure-commons.error-codes]
        [common-swagger-api.schema :only [->optional-param
                                          describe
                                          ErrorResponse
                                          NonBlankString
                                          StandardUserQueryParams]]
        [metadata.routes.schemas.common])
  (:require [schema.core :as s]))

(s/defschema UpdateAttachedTagsQueryParams
  (merge StandardDataItemQueryParams
    {:type
     (describe (s/enum "attach" "detach")
       "Whether to attach or detach the provided set of tags to the file/folder")}))

(s/defschema ErrorResponseBadTagRequest
  (assoc ErrorResponse
    :error_code (describe (s/enum ERR_ILLEGAL_ARGUMENT ERR_NOT_UNIQUE) "Bad Tag Request error codes")))
