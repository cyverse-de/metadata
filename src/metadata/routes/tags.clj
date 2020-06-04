(ns metadata.routes.tags
  (:use [common-swagger-api.schema]
        [metadata.routes.schemas.common]
        [metadata.routes.schemas.tags]
        [ring.util.http-response :only [ok]])
  (:require [compojure.api.middleware :as middleware]
            [common-swagger-api.schema.metadata :as schema]
            [common-swagger-api.schema.metadata.tags :as ts]
            [metadata.services.tags :as tags]))

(defroutes filesystem-tags
  (context "/filesystem/data" []
    :tags ["tags"]

    (GET "/tags" []
      :query [{:keys [user]} StandardUserQueryParams]
      :responses {200      {:schema      ts/AttachedTagsListing
                            :description "Attached tags are listed in the response"}
                  500      {:schema      ErrorResponseUnchecked
                            :description "Unchecked errors"}
                  :default {:schema      ErrorResponse
                            :description "All other errors"}}
      :summary ts/GetTagsSummary
      :description ts/GetTagsDescription
      (ok (tags/list-all-attached-tags user)))

    (DELETE "/tags" []
      :query [{:keys [user]} StandardUserQueryParams]
      :coercion middleware/no-response-coercion
      :responses {200      {:schema      nil
                            :description "The attached tags were successfully deleted"}
                  500      {:schema      ErrorResponseUnchecked
                            :description "Unchecked errors"}
                  :default {:schema      ErrorResponse
                            :description "All other errors"}}
      :summary ts/DeleteTagsSummary
      :description ts/DeleteTagsDescription
      (tags/delete-all-attached-tags user)
      (ok))

    (GET "/:data-id/tags" []
      :path-params [data-id :- schema/TargetIdParam]
      :query [{:keys [user]} StandardUserQueryParams]
      :responses {200      {:schema      ts/TagList
                            :description "The tags are listed in the response"}
                  500      {:schema      ErrorResponseUnchecked
                            :description "Unchecked errors"}
                  :default {:schema      ErrorResponse
                            :description "All other errors"}}
      :summary ts/GetAttachedTagSummary
      :description ts/GetAttachedTagDescription
      (ok (tags/list-attached-tags user data-id)))

    (PATCH "/:data-id/tags" []
      :path-params [data-id :- schema/TargetIdParam]
      :query [{:keys [user data-type type]} UpdateAttachedTagsQueryParams]
      :body [body ts/TagIdList]
      :responses {200      {:schema     ts/AttachedTagsListing
                            :description "The tags were attached or detached from the file or folder"}
                  400      {:schema      ErrorResponseBadTagRequest
                            :description "The `type` wasn't provided or had a value other than `attach` or `detach`;
                             or the request body wasn't syntactically correct"}
                  404      {:schema      ErrorResponseNotFound
                            :description "One of the provided tag Ids doesn't map to a tag for the authenticated user"}
                  500      {:schema      ErrorResponseUnchecked
                            :description "Unchecked errors"}
                  :default {:schema      ErrorResponse
                            :description "All other errors"}}
      :summary ts/PatchTagsSummary
      :description ts/PatchTagsDescription
      (ok (tags/handle-patch-file-tags user data-id data-type type body)))))

(defroutes tags
  (context "/tags" []
    :tags ["tags"]

    (GET "/suggestions" []
      :query [{:keys [user contains limit]} ts/TagSuggestQueryParams]
      :responses {200      {:schema      ts/TagList
                            :description "zero or more suggestions were returned"}
                  400      {:schema      ErrorResponseIllegalArgument
                            :description "the `contains` parameter was missing or
                                the `limit` parameter was set to a something other than a non-negative number."}
                  500      {:schema      ErrorResponseUnchecked
                            :description "Unchecked errors"}
                  :default {:schema      ErrorResponse
                            :description "All other errors"}}
      :summary ts/GetTagSuggestionsSummary
      :description ts/GetTagSuggestionsDescription
      (ok (tags/suggest-tags user contains limit)))

    (GET "/user" []
      :query [{:keys [user]} StandardUserQueryParams]
      :responses {200      {:schema      ts/TagList
                            :description "The tags are listed in the response"}
                  500      {:schema      ErrorResponseUnchecked
                            :description "Unchecked errors"}
                  :default {:schema      ErrorResponse
                            :description "All other errors"}}
      :summary ts/GetUserTagsSummary
      :description ts/GetUserTagsDescription
      (ok (tags/list-tags-defined-by user)))

    (DELETE "/user" []
      :query [{:keys [user]} StandardUserQueryParams]
      :coercion middleware/no-response-coercion
      :responses {200      {:schema      nil
                            :description "The tags were successfully deleted"}
                  500      {:schema      ErrorResponseUnchecked
                            :description "Unchecked errors"}
                  :default {:schema      ErrorResponse
                            :description "All other errors"}}
      :summary ts/DeleteUserTagsSummary
      :description ts/DeleteUserTagsDescription
      (tags/delete-tags-defined-by user)
      (ok))

    (POST "/user" []
      :query [{:keys [user]} StandardUserQueryParams]
      :body [body ts/TagRequest]
      :responses {200      {:schema      ts/TagDetails
                            :description "The tag was successfully created"}
                  400      {:schema      ErrorResponseBadTagRequest
                            :description "The `value` was not unique, too long,
                             or the request body wasn't syntactically correct"}
                  500      {:schema      ErrorResponseUnchecked
                            :description "Unchecked errors"}
                  :default {:schema      ErrorResponse
                            :description "All other errors"}}
      :summary ts/PostTagSummary
      :description ts/PostTagDescription
      (ok (tags/create-user-tag user body)))

    (DELETE "/user/:tag-id" []
      :path-params [tag-id :- ts/TagIdPathParam]
      :query [{:keys [user]} StandardUserQueryParams]
      :coercion middleware/no-response-coercion
      :responses {200      {:schema      nil
                            :description "The tag was successfully deleted"}
                  404      {:schema      ErrorResponseNotFound
                            :description "`tag-id` wasn't a UUID of a tag owned by the authenticated user"}
                  500      {:schema      ErrorResponseUnchecked
                            :description "Unchecked errors"}
                  :default {:schema      ErrorResponse
                            :description "All other errors"}}
      :summary ts/DeleteTagSummary
      :description ts/DeleteTagDescription
      (ok (tags/delete-user-tag user tag-id)))

    (PATCH "/user/:tag-id" []
      :path-params [tag-id :- ts/TagIdPathParam]
      :query [{:keys [user]} StandardUserQueryParams]
      :body [body ts/TagUpdateRequest]
      :responses {200      {:schema      ts/TagDetails
                            :description "The tag was successfully created"}
                  400      {:schema      ErrorResponseBadTagRequest
                            :description "The `value` was not unique, too long,
                             or the request body wasn't syntactically correct"}
                  500      {:schema      ErrorResponseUnchecked
                            :description "Unchecked errors"}
                  :default {:schema      ErrorResponse
                            :description "All other errors"}}
      :summary ts/PatchTagSummary
      :description ts/PatchTagDescription
      (ok (tags/update-user-tag user tag-id body)))))
