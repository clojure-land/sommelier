(ns routes.projects
  (:require [compojure.api.sweet :refer :all]
            [util.response :refer :all]
            [util.auth :refer :all]
            [util.mdb :refer :all]
            [domain.response :refer :all]
            [domain.project :refer :all]
            [monger.json]))

;; ***** Project implementation ********************************************************

(defn- read-projects
  "Retrieves projects.

  e.g. (read-projects 'user')"
  [author]

  (api-response (->ApiData {:status "ok"} (project->resource-object (get-project {:author author}))) 200 []))

;; ***** Projects definition *******************************************************

(def projects-routes
  (context "/projects" []
    (GET "/" []
      :tags ["projects"]
      :operationId "getProjects"
      :summary "Retrieves projects."
      ;:middleware [#(util.auth/auth! %)]
      ;:current-user profile
      :responses {200 {:schema {:meta Meta :data (vector (assoc DataObject :attributes ProjectSchema))}
                       :description "ok"}
                  403 {:schema {:meta Meta :errors [ErrorObject]}
                       :description "unauthorized"}} (read-projects ""))))

