(ns metadata.routes.comments
  (:use [common-swagger-api.schema]
        [common-swagger-api.schema.metadata.comments]
        [metadata.routes.schemas.common]
        [ring.util.http-response :only [ok]])
  (:require [compojure.api.middleware :as middleware]
            [common-swagger-api.schema.metadata :as schema]
            [metadata.services.comments :as comments]))

(defroutes admin-comment-routes
  (context "/admin/comments" []
    :tags ["admin-comments"]

    (GET "/:commenter-id" []
      :path-params [commenter-id :- CommenterId]
      :query [{:keys [user]} StandardUserQueryParams]
      :responses {200      {:schema      CommentDetailsList
                            :description "Comment details are listed in the response"}
                  500      {:schema      ErrorResponseUnchecked
                            :description "Unchecked errors"}
                  :default {:schema      ErrorResponse
                            :description "All other errors"}}
      :summary "List All Comments Added by a User"
      :description "This endpoint allows an administrator to list all comments that were entered by a single user."
      (ok (comments/list-user-comments commenter-id)))

    (DELETE "/:commenter-id" []
      :path-params [commenter-id :- CommenterId]
      :query [{:keys [user]} StandardUserQueryParams]
      :coercion middleware/no-response-coercion
      :responses {200      {:schema      nil
                            :description "The comments were deleted successfully"}
                  500      {:schema      ErrorResponseUnchecked
                            :description "Unchecked errors"}
                  :default {:schema      ErrorResponse
                            :description "All other errors"}}
      :summary "Delete All Comments Added by a User"
      :description "This endpoint allows an administrator to delete all comments that were entered by a single user."
      (comments/delete-user-comments commenter-id)
      (ok))))

(defroutes data-comment-routes
  (context "/filesystem/data" []
    :tags ["data-comments"]

    (GET "/:data-id/comments" []
      :path-params [data-id :- schema/TargetIdParam]
      :return CommentList
      :summary "Listing Data Comments"
      :description "This endpoint retrieves all of the comments made on a file or folder."
      (ok (comments/list-data-comments data-id)))

    (POST "/:data-id/comments" []
      :path-params [data-id :- schema/TargetIdParam]
      :query [{:keys [user data-type]} StandardDataItemQueryParams]
      :body [body (describe CommentRequest "The comment to add.")]
      :return CommentResponse
      :summary "Create a Comment"
      :description "This endpoint allows a user to post a comment on a file or folder."
      (ok (comments/add-data-comment user data-id data-type body)))

    (PATCH "/:data-id/comments/:comment-id" []
      :path-params [data-id :- schema/TargetIdParam
                    comment-id :- CommentIdPathParam]
      :query [{:keys [user retracted]} RetractCommentQueryParams]
      :summary "Retract/Readmit a Comment"
      :description "
This endpoint allows a user to retract a comment on a file or folder, if the user is the commenter.
This endpoint also allows a user to readmit a comment the user previously retracted by setting the
`retracted` parameter to `false`."
      (ok (comments/update-data-retract-status user data-id comment-id retracted)))))

(defroutes admin-data-comment-routes
  (context "/admin/filesystem/data" []
    :tags ["admin-data-comments"]

    (DELETE "/:data-id/comments/:comment-id" []
      :path-params [data-id :- schema/TargetIdParam
                    comment-id :- CommentIdPathParam]
      :query [{:keys [user]} StandardUserQueryParams]
      :summary "Delete a Comment"
      :description
      "This endpoint allows an administrative user to delete a comment on a file or folder."
      (ok (comments/delete-data-comment data-id comment-id)))

    (PATCH "/:data-id/comments/:comment-id" []
      :path-params [data-id :- schema/TargetIdParam
                    comment-id :- CommentIdPathParam]
      :query [{:keys [user retracted]} RetractCommentQueryParams]
      :summary "Retract/Readmit a Comment"
      :description "
This endpoint allows an administrative user to retract any comment on a file or folder.
This endpoint also allows a user to readmit a comment the user previously retracted by setting the
`retracted` parameter to `false`."
      (ok (comments/admin-update-retract-status user data-id comment-id retracted)))))

(defroutes app-comment-routes
  (context "/apps" []
    :tags ["app-comments"]

    (GET "/:app-id/comments" []
      :path-params [app-id :- schema/TargetIdParam]
      :return CommentList
      :summary "Listing App Comments"
      :description "This endpoint retrieves all of the comments made on an app."
      (ok (comments/list-app-comments app-id)))

    (POST "/:app-id/comments" []
      :path-params [app-id :- schema/TargetIdParam]
      :query [{:keys [user]} StandardUserQueryParams]
      :body [body (describe CommentRequest "The comment to add.")]
      :return CommentResponse
      :summary "Create a Comment"
      :description "This endpoint allows a user to post a comment on an app."
      (ok (comments/add-app-comment user app-id body)))

    (PATCH "/:app-id/comments/:comment-id" []
      :path-params [app-id :- schema/TargetIdParam
                    comment-id :- CommentIdPathParam]
      :query [{:keys [user retracted]} RetractCommentQueryParams]
      :summary "Retract/Readmit a Comment"
      :description "
This endpoint allows a user to retract a comment on an app, if the user is the commenter.
This endpoint also allows a user to readmit a comment the user previously retracted by setting the
`retracted` parameter to `false`."
      (ok (comments/update-app-retract-status user app-id comment-id retracted)))))

(defroutes admin-app-comment-routes
  (context "/admin/apps" []
    :tags ["admin-app-comments"]

    (DELETE "/:app-id/comments/:comment-id" []
      :path-params [app-id :- schema/TargetIdParam
                    comment-id :- CommentIdPathParam]
      :query [{:keys [user]} StandardUserQueryParams]
      :summary "Delete an App Comment"
      :description "This endpoint allows an administrative user to delete a comment on an app."
      (ok (comments/delete-app-comment app-id comment-id)))

    (PATCH "/:app-id/comments/:comment-id" []
      :path-params [app-id :- schema/TargetIdParam
                    comment-id :- CommentIdPathParam]
      :query [{:keys [user retracted]} RetractCommentQueryParams]
      :summary "Retract/Readmit a Comment"
      :description "
This endpoint allows an administrative user to retract any comment on an app.
This endpoint also allows a user to readmit a comment the user previously retracted by setting the
`retracted` parameter to `false`."
      (ok (comments/admin-update-retract-status user app-id comment-id retracted)))))
