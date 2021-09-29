(ns metadata.routes.schemas.permanent-id-requests
  (:use [common-swagger-api.schema :only [describe
                                          PagingParams
                                          SortFieldDocs
                                          SortFieldOptionalKey
                                          StandardUserQueryParams]]
        [clojure-commons.error-codes]
        [metadata.routes.schemas.common])
  (:require [common-swagger-api.schema.permanent-id-requests :as schema]
            [metadata.persistence.permanent-id-requests :as db]
            [schema-tools.core :as st]
            [schema.core :as s])
  (:import [java.util UUID]))

(def PermanentIDRequestTypeEnum (apply s/enum (map :type (db/list-permanent-id-request-types))))

(s/defschema PermanentIDRequest
  (st/merge schema/PermanentID
            schema/PermanentIDRequestOrigPath
            {:type        (describe PermanentIDRequestTypeEnum "The type of persistent ID requested")
             :target_id   (describe UUID "The UUID of the data item for which the persistent ID is being requested")
             :target_type DataTypeParam}))

(s/defschema PermanentIDRequestBase
  (st/merge schema/PermanentIDRequestBase PermanentIDRequest))

(s/defschema PermanentIDRequestDetails
  (st/merge schema/PermanentIDRequestDetails PermanentIDRequestBase))

(s/defschema PermanentIDRequestListing
  (st/merge schema/PermanentIDRequestListing PermanentIDRequestBase))

(s/defschema PermanentIDRequestList
  (st/merge schema/PermanentIDRequestList
            {:requests (describe [PermanentIDRequestListing] "A list of Permanent ID Requests")}))

(s/defschema PermanentIDRequestListPagingParams
  (st/merge schema/PermanentIDRequestListPagingParams StandardUserQueryParams))
