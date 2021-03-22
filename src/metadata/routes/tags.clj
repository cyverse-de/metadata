(ns metadata.routes.tags
  (:use [common-swagger-api.schema]
        [common-swagger-api.schema.metadata.tags]
        [metadata.routes.schemas.common]
        [metadata.routes.schemas.tags]
        [otel.middleware :only [otel-middleware]]
        [ring.util.http-response :only [ok]])
  (:require [compojure.api.middleware :as middleware]
            [common-swagger-api.schema.metadata :as schema]
            [metadata.services.tags :as tags]))

(defroutes filesystem-tags
  (context "/filesystem/data" []
    :tags ["tags"]

    (GET "/tags" []
      :middleware [otel-middleware]
      :query [{:keys [user]} StandardUserQueryParams]
      :responses GetTagsResponses
      :summary GetTagsSummary
      :description GetTagsDescription
      (ok (tags/list-all-attached-tags user)))

    (DELETE "/tags" []
      :middleware [otel-middleware]
      :query [{:keys [user]} StandardUserQueryParams]
      :coercion middleware/no-response-coercion
      :responses DeleteTagsResponses
      :summary DeleteTagsSummary
      :description DeleteTagsDescription
      (tags/delete-all-attached-tags user)
      (ok))

    (GET "/:data-id/tags" []
      :middleware [otel-middleware]
      :path-params [data-id :- schema/TargetIdParam]
      :query [{:keys [user]} StandardUserQueryParams]
      :responses GetAttachedTagResponses
      :summary GetAttachedTagSummary
      :description GetAttachedTagDescription
      (ok (tags/list-attached-tags user data-id)))

    (PATCH "/:data-id/tags" []
      :middleware [otel-middleware]
      :path-params [data-id :- schema/TargetIdParam]
      :query [{:keys [user data-type type]} UpdateAttachedTagsQueryParams]
      :body [body TagIdList]
      :responses PatchTagsResponses
      :summary PatchTagsSummary
      :description PatchTagsDescription
      (ok (tags/handle-patch-file-tags user data-id data-type type body)))))

(defroutes tags
  (context "/tags" []
    :tags ["tags"]

    (GET "/suggestions" []
      :middleware [otel-middleware]
      :query [{:keys [user contains limit]} TagSuggestUserQueryParams]
      :responses GetTagSuggestionsResponses
      :summary GetTagSuggestionsSummary
      :description GetTagSuggestionsDescription
      (ok (tags/suggest-tags user contains limit)))

    (GET "/user" []
      :middleware [otel-middleware]
      :query [{:keys [user]} StandardUserQueryParams]
      :responses GetUserTagsResponses
      :summary GetUserTagsSummary
      :description GetUserTagsDescription
      (ok (tags/list-tags-defined-by user)))

    (DELETE "/user" []
      :middleware [otel-middleware]
      :query [{:keys [user]} StandardUserQueryParams]
      :coercion middleware/no-response-coercion
      :responses DeleteUserTagsResponses
      :summary DeleteUserTagsSummary
      :description DeleteUserTagsDescription
      (tags/delete-tags-defined-by user)
      (ok))

    (POST "/user" []
      :middleware [otel-middleware]
      :query [{:keys [user]} StandardUserQueryParams]
      :body [body TagRequest]
      :responses PostTagResponses
      :summary PostTagSummary
      :description PostTagDescription
      (ok (tags/create-user-tag user body)))

    (DELETE "/user/:tag-id" []
      :middleware [otel-middleware]
      :path-params [tag-id :- TagIdPathParam]
      :query [{:keys [user]} StandardUserQueryParams]
      :coercion middleware/no-response-coercion
      :responses DeleteTagResponses
      :summary DeleteTagSummary
      :description DeleteTagDescription
      (ok (tags/delete-user-tag user tag-id)))

    (PATCH "/user/:tag-id" []
      :middleware [otel-middleware]
      :path-params [tag-id :- TagIdPathParam]
      :query [{:keys [user]} StandardUserQueryParams]
      :body [body TagUpdateRequest]
      :responses PatchTagResponses
      :summary PatchTagSummary
      :description PatchTagDescription
      (ok (tags/update-user-tag user tag-id body)))))
