(ns routes.job
  (:require [compojure.api.sweet :refer :all]
            [util.response :refer :all]
            [util.auth :refer :all]
            [util.mdb :as mdb]
            [domain.response :refer :all]
            [domain.project :refer :all]
            [domain.job :refer :all]
            [monger.json]
            [clojure.math.combinatorics :as combo])
  (:import [org.bson.types ObjectId]))

;; ***** Job implementation ********************************************************



;; ***** Job definition *******************************************************

(def job-routes
  (context "/job" []

    (GET "/:id" []
      :path-params [id :- JobId]
      :tags ["job"]
      :operationId "getJobs"
      :summary "Retrieves jobs."
      ;:middleware [#(util.auth/auth! %)]
      ;:current-user profile
      :responses {200 {;:schema {:meta Meta ::data [{:type "job" :attributes {:status "scheduled,running,done,failed"}}]
                       ;content-location = /project/1/job
                       :description "accepted"}
                  400 {:schema {:meta Meta :errors [ErrorObject]}
                       :description "bad request"}
                  403 {:schema {:meta Meta :errors [ErrorObject]}
                       :description "unauthorized"}
                  404 {:schema {:meta Meta :errors [ErrorObject]}
                       :description "not found"}} "")

    (GET "/:id/frequencies" []
      :path-params [id :- JobId]
      :tags ["job"]
      :operationId "getJobs"
      :summary "Retrieves jobs."
      ;:middleware [#(util.auth/auth! %)]
      ;:current-user profile
      :responses {200 {;:schema {:meta Meta ::data [{:type "job" :attributes {:status "scheduled,running,done,failed"}}]
                       ;content-location = /project/1/job
                       :description "accepted"}
                  400 {:schema {:meta Meta :errors [ErrorObject]}
                       :description "bad request"}
                  403 {:schema {:meta Meta :errors [ErrorObject]}
                       :description "unauthorized"}
                  404 {:schema {:meta Meta :errors [ErrorObject]}
                       :description "not found"}} "")

    (GET "/:id/associations" []
      :path-params [id :- JobId]
      :tags ["job"]
      :operationId "getJobs"
      :summary "Retrieves jobs."
      ;:middleware [#(util.auth/auth! %)]
      ;:current-user profile
      :responses {200 {;:schema {:meta Meta ::data [{:type "job" :attributes {:status "scheduled,running,done,failed"}}]
                       ;content-location = /project/1/job
                       :description "accepted"}
                  400 {:schema {:meta Meta :errors [ErrorObject]}
                       :description "bad request"}
                  403 {:schema {:meta Meta :errors [ErrorObject]}
                       :description "unauthorized"}
                  404 {:schema {:meta Meta :errors [ErrorObject]}
                       :description "not found"}} "")))
