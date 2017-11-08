(ns metadata.routes.schemas.comments
  (:use [common-swagger-api.schema :only [describe NonBlankString StandardUserQueryParams]]
        [metadata.routes.schemas.common])
  (:require [schema.core :as s])
  (:import [java.util UUID]))

(def CommenterId (describe String "The username of the person who added the comment."))

(def CommentIdPathParam (describe UUID "The comment's UUID"))
(def CommentTextParam (describe NonBlankString "The text of the comment"))

(s/defschema RetractCommentQueryParams
  (assoc StandardUserQueryParams
    :retracted (describe Boolean
                 "Whether to retract the comment (`true`) or readmit a retraction (`false`)")))

(s/defschema Comment
  {:id        (describe UUID "The service-provided UUID associated with the comment")
   :commenter (describe NonBlankString "The authenticated username of the person who made the comment")
   :post_time (describe Long "The time when the comment was posted in ms since the POSIX epoch")
   :retracted (describe Boolean "A flag indicating whether or not the comment is currently retracted")
   :comment   CommentTextParam})

(s/defschema CommentList
  {:comments (describe [Comment] "A list of comments")})

(s/defschema CommentRequest
  {:comment CommentTextParam})

(s/defschema CommentResponse
  {:comment Comment})

(s/defschema CommentDetails
  (assoc Comment
    (s/optional-key :retracted_by)
    (describe String "The username of the person who retracted the comment")

    :deleted
    (describe Boolean "True if the comment has been deleted")

    :target_id
    (describe UUID "The identifier of the target the comment is attached to")

    :target_type
    (describe String "The type of target the comment is attached to")))

(s/defschema CommentDetailsList
  {:comments (describe [CommentDetails] "A list of comment details")})
