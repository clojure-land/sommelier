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
  "Retrieves all projects the active user has permission to access.

  e.g. (read-projects {:sub ''})"
  [profile]

  (api-response (->>
                  (get-projects {:_id {"$in" (map #(get % :resource) (get-permissions (get profile :sub) "project"))}})
                  (project->resource-object)
                  (->ApiData {:status "ok"})) 200 []))

;; ***** Projects definition *******************************************************

(def projects-routes
  (context "/projects" []
    (GET "/" []
      :tags ["projects"]
      :operationId "getProjects"
      :summary "Retrieves projects."
      :middleware [#(auth! %)]
      :current-user profile
      :responses {200 {:schema {:meta Meta :data (vector (assoc DataObject :attributes ProjectSchema))}
                       :description "ok"}
                  403 {:schema {:meta Meta :errors [ErrorObject]}
                       :description "unauthorized"}} (read-projects profile))))
