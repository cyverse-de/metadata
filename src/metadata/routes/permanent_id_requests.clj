(ns metadata.routes.permanent-id-requests
  (:use [common-swagger-api.schema]
        [metadata.routes.schemas.common]
        [metadata.routes.schemas.permanent-id-requests]
        [metadata.services.permanent-id-requests]
        [otel.middleware :only [otel-middleware]]
        [ring.util.http-response :only [ok]])
  (:require [common-swagger-api.schema.permanent-id-requests :as schema]))

(defroutes permanent-id-request-routes
  (context "/permanent-id-requests" []
    :tags ["permanent-id-requests"]

    (GET "/" []
      :middleware [otel-middleware]
      :query [params PermanentIDRequestListPagingParams]
      :return PermanentIDRequestList
      :summary schema/PermanentIDRequestListSummary
      :description schema/PermanentIDRequestListDescription
      (ok (list-permanent-id-requests params)))

    (POST "/" []
      :middleware [otel-middleware]
      :query [{:keys [user]} StandardUserQueryParams]
      :body [body PermanentIDRequest]
      :return PermanentIDRequestDetails
      :summary schema/PermanentIDRequestSummary
      :description schema/PermanentIDRequestDescription
      (ok (create-permanent-id-request user body)))

    (GET "/status-codes" []
      :middleware [otel-middleware]
      :query [params StandardUserQueryParams]
      :return schema/PermanentIDRequestStatusCodeList
      :summary schema/PermanentIDRequestStatusCodeListSummary
      :description schema/PermanentIDRequestStatusCodeListDescription
      (ok (list-permanent-id-request-status-codes params)))

    (GET "/types" []
      :middleware [otel-middleware]
      :query [params StandardUserQueryParams]
      :return schema/PermanentIDRequestTypeList
      :summary schema/PermanentIDRequestTypesSummary
      :description schema/PermanentIDRequestTypesDescription
      (ok (list-permanent-id-request-types params)))

    (GET "/:request-id" []
      :middleware [otel-middleware]
      :path-params [request-id :- schema/PermanentIDRequestIdParam]
      :query [{:keys [user]} StandardUserQueryParams]
      :return PermanentIDRequestDetails
      :summary schema/PermanentIDRequestDetailsSummary
      :description schema/PermanentIDRequestDetailsDescription
      (ok (get-permanent-id-request user request-id)))))

(defroutes admin-permanent-id-request-routes
  (context "/admin/permanent-id-requests" []
    :tags ["admin-permanent-id-requests"]

    (GET "/" []
      :middleware [otel-middleware]
      :query [params PermanentIDRequestListPagingParams]
      :return PermanentIDRequestList
      :summary schema/PermanentIDRequestAdminListSummary
      :description schema/PermanentIDRequestAdminListDescription
      (ok (admin-list-permanent-id-requests params)))

    (GET "/:request-id" []
      :middleware [otel-middleware]
      :path-params [request-id :- schema/PermanentIDRequestIdParam]
      :query [{:keys [user]} StandardUserQueryParams]
      :return PermanentIDRequestDetails
      :summary schema/PermanentIDRequestAdminDetailsSummary
      :description schema/PermanentIDRequestAdminDetailsDescription
      (ok (admin-get-permanent-id-request user request-id)))

    (POST "/:request-id/status" []
      :middleware [otel-middleware]
      :path-params [request-id :- schema/PermanentIDRequestIdParam]
      :query [{:keys [user]} StandardUserQueryParams]
      :body [body schema/PermanentIDRequestStatusUpdate]
      :return PermanentIDRequestDetails
      :summary schema/PermanentIDRequestAdminStatusUpdateSummary
      :description schema/PermanentIDRequestAdminStatusUpdateDescription
      (ok (update-permanent-id-request request-id user body)))))
