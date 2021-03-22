(ns metadata.routes.avus
  (:use [common-swagger-api.schema]
        [metadata.routes.schemas.common
         :only [AvuSearchQueryParams
                TargetIDList
                TargetTypeEnum
                TargetItemList]]
        [metadata.routes.schemas.avus]
        [otel.middleware :only [otel-middleware]]
        [ring.util.http-response :only [ok]])
  (:require [common-swagger-api.schema.metadata :as schema]
            [metadata.services.avus :as avus]))

(defroutes avus
  (context "/avus" []
    :tags ["avus"]

    (GET "/" []
           :middleware [otel-middleware]
           :query [params AvuSearchQueryParams]
           :return schema/AvuList
           :summary "List AVUs."
           :description "Lists AVUs matching parameters in the query string."
           (ok (avus/list-avus params)))

    (POST "/deleter" []
          :middleware [otel-middleware]
          :query [{:keys [user]} StandardUserQueryParams]
          :body [{:keys [target-types target-ids avus]} DeleteTargetAvusRequest]
          :summary "Delete Target AVUs"
          :description "Deletes AVUs matching those in the given `avus` list from the given `target-ids`."
          (ok (avus/delete-target-avus target-types target-ids avus)))

    (POST "/filter-targets" []
           :middleware [otel-middleware]
           :query [{:keys [user]} StandardUserQueryParams]
           :body [{:keys [target-types target-ids avus]} FilterByAvusRequest]
           :return TargetIDList
           :summary "Filter Targets by AVU"
           :description
           "Filters the given target IDs by returning a list of any that have metadata with the given
            `attrs` and `values`."
           (ok (avus/filter-targets-by-avus target-types target-ids avus)))

    (GET "/:target-type/:target-id" []
          :middleware [otel-middleware]
          :path-params [target-id :- schema/TargetIdParam
                        target-type :- TargetTypeEnum]
          :query [{:keys [user]} StandardUserQueryParams]
          :return schema/AvuList
          :summary "View all Metadata AVUs on a Target"
          :description "Lists all AVUs associated with the target item."
          (ok (avus/list-avus target-type target-id)))

    (POST "/:target-type/:target-id" []
           :middleware [otel-middleware]
           :path-params [target-id :- schema/TargetIdParam
                         target-type :- TargetTypeEnum]
           :query [{:keys [user]} StandardUserQueryParams]
           :body [body schema/AvuListRequest]
           :return schema/AvuList
           :summary "Add/Update Metadata AVUs"
           :description "
Adds or updates Metadata AVUs on the given target item.

Including an existing AVU’s ID in its JSON in the request body will update its values and `modified` fields,
but only if the existing AVU does not already match the given `attr`, `value`, and `unit` values.
AVUs included without an ID will be added to the target item, only if an AVU does not already exist with
matching `attr`, `value`, `unit`, `target`, and `type`."
           (ok (avus/update-avus user target-type target-id body)))

    (PUT "/:target-type/:target-id" []
          :middleware [otel-middleware]
          :path-params [target-id :- schema/TargetIdParam
                        target-type :- TargetTypeEnum]
          :query [{:keys [user]} StandardUserQueryParams]
          :body [body schema/SetAvuRequest]
          :return schema/AvuList
          :summary "Set Metadata AVUs on a Target"
          :description "
Sets Metadata AVUs on the given target item.

Any AVUs not included in the request will be deleted. If the AVUs are omitted, then all AVUs for the
given target ID will be deleted.

Including an existing AVU’s ID in its JSON in the request body will update its values and `modified` fields,
but only if the existing AVU does not already match the given `attr`, `value`, and `unit` values.
All other existing AVUs (not included in the request with an ID) will be deleted from the target item,
then all remaining AVUs in the request will be added to the target item."
          (ok (avus/set-avus user target-type target-id body)))

    (POST "/:target-type/:target-id/copy" []
           :middleware [otel-middleware]
           :path-params [target-id :- schema/TargetIdParam
                    target-type :- TargetTypeEnum]
           :query [{:keys [user]} StandardUserQueryParams]
           :body [body (describe TargetItemList "The destination targets.")]
           :summary "Copy all Metadata AVUs from a Target"
           :description "
Copies all Metadata Template AVUs from the data item with the ID given in the URL to other data
items sent in the request body."
           (ok (avus/copy-avus user target-type target-id body)))))
