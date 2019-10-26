(ns routes.jobs
  (:require [compojure.api.sweet :refer :all]
            [util.response :refer :all]
            [util.auth :refer :all]
            [util.mdb :refer :all]
            [domain.response :refer :all]
            [domain.project :refer :all]
            [domain.job :refer :all]
            [monger.json])
  (:import [org.bson.types ObjectId]))

;; ***** Jobs implementation ********************************************************

(defn- read-jobs
  "Retrieves jobs.

  e.g. (read-jobs '5da04bbd9194be00066ac0b8' 'user')"
  [profile id]

  (if-let [project (get-project-if-exists id)]
    (if (has-permission? (get profile :sub) id "project")
      (api-response (->ApiData {:status "ok"} (job->resource-object (get-jobs {:project-id (ObjectId. (str id))}))) 200 [])
      (forbidden))
    (project-not-found id)))

;; ***** Jobs definition *******************************************************

(def jobs-routes
  (GET "/:id/jobs" []
    :path-params [id :- ProjectId]
    :tags ["project"]
    :operationId "getJobs"
    :summary "Retrieves jobs."
    :middleware [#(auth! %)]
    :current-user profile
    :responses {200 {:schema {:meta Meta :data (vector (assoc DataObject :attributes JobSchema))}
                     :description "ok"}
                400 {:schema {:meta Meta :errors [ErrorObject]}
                     :description "bad request"}
                403 {:schema {:meta Meta :errors [ErrorObject]}
                     :description "unauthorized"}
                404 {:schema {:meta Meta :errors [ErrorObject]}
                     :description "not found"}} (read-jobs profile id)))