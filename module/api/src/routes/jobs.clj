(ns routes.jobs
  (:require [compojure.api.sweet :refer :all]
            [util.response :refer :all]
            [util.auth :refer :all]
            [util.mdb :as mdb]
            [domain.response :refer :all]
            [domain.project :refer :all]
            [domain.job :refer :all]
            [monger.json])
  (:import [org.bson.types ObjectId]))

;; ***** Jobs implementation ********************************************************

(defn- read-jobs
  "Retrieves jobs."
  [id author]

  (if (and (mdb/objectId? id)
           (not= (mdb/get-project {:_id (ObjectId. (str id)) :author author}) []))
    (api-response (->ApiData {:status "ok"} (job->resource-object (mdb/get-job {:project-id (ObjectId. (str id))}))) 200 [])
    (project-not-found id)))

;; ***** Jobs definition *******************************************************

(def jobs-routes
  (GET "/:id/jobs" []
    :path-params [id :- ProjectId]
    :tags ["project"]
    :operationId "getJobs"
    :summary "Retrieves jobs."
    ;:middleware [#(util.auth/auth! %)]
    ;:current-user profile
    :responses {200 {:schema {:meta Meta :data (vector (assoc DataObject :attributes JobSchema))}
                     :description "ok"}
                400 {:schema {:meta Meta :errors [ErrorObject]}
                     :description "bad request"}
                403 {:schema {:meta Meta :errors [ErrorObject]}
                     :description "unauthorized"}
                404 {:schema {:meta Meta :errors [ErrorObject]}
                     :description "not found"}} (read-jobs id "")))