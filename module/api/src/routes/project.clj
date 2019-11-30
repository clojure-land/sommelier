(ns routes.project
  (:require [compojure.api.sweet :refer :all]
            [util.response :refer :all]
            [util.auth :refer :all]
            [util.mdb :refer :all]
            [domain.response :refer :all]
            [domain.project :refer :all]
            [monger.json]
            [ring.util.response :as ring]
            [routes.transactions :refer [transactions-routes]]
            [routes.tasks :refer [tasks-routes]])
  (:import [org.bson.types ObjectId]))

;; ***** Project implementation ********************************************************

(defn- create-project
  "Creates a new project, and grants the active user corresponding permission to it.

  e.g. (create-project {:sub ''} {:name 'some name', :description '', :window 0, :min-support 0, :min-confidence 0})"
  [profile body]

  (let [data (save-project nil body)]
    (if (true? (insert-permission (get profile :sub) "project" (get (first data) :_id)))
      (api-response (->ApiData {:status "created"} (project->resource-object data)) 201 [{:location (str "/project/" (get (first data) :_id))}])
      (forbidden))))

(defn- read-project
  "Retrieves a project, only if the active user has permission to the project.

  e.g. (read-project {:sub ''} '5da04bbd9194be00066ac0b8')"
  [profile id]

  (if-let [project (get-project-if-exists id)]
    (if (has-permission? (get profile :sub) id "project")
        (api-response (->ApiData {:status "ok"} (project->resource-object project)) 200 [])
        (forbidden))
    (project-not-found id)))

(defn- update-project
  "Updates an existing project, only if the active user has permission to the project.

  e.g (update-project {:sub ''} '5da04b299194be00066ac0b6' {:name 'some name', :description '', :window 0, :min-support 0, :min-confidence 0})"
  [profile id body]

  (if-let [project (get-project-if-exists id)]
    (if (has-permission? (get profile :sub) id "project")
      (api-response (->>
                      (save-project id body)
                      (project->resource-object)
                      (->ApiData {:status "ok"})) 200 [])
      (forbidden))
    (project-not-found id)))

;todo - Deletes an existing project, removes all associated tasks and revokes all corresponding permissions.
(defn- delete-project!
  "Deletes an existing project.

  e.g (delete-project {:sub ''} '5da04b299194be00066ac0b6')"
  [profile id]

  (if-let [project (get-project-if-exists id)]
    (if (has-permission? (get profile :sub) id "project")
      (do
        (remove-project (ObjectId. (str id)))
        (ring/status nil 204))
      (forbidden))
    (project-not-found id)))

;; ***** Project definition *******************************************************

(def project-routes
  (context "/project" []
    (POST "/" []
      :tags ["project"]
      :operationId "createProject"
      :summary "Creates a new project."
      :body [project ProjectSchema]
      :middleware [#(auth! %)]
      :current-user profile
      :responses {201 {:schema      {:meta Meta :data (vector (assoc DataObject :attributes ProjectSchema))}
                       :description "created"}
                  400 {:schema      {:meta Meta :errors [ErrorObject]}
                       :description "bad request"}
                  401 {:schema      {:meta Meta :errors [ErrorObject]}
                       :description "unauthorized"}
                  403 {:schema      {:meta Meta :errors [ErrorObject]}
                       :description "forbidden"}} (create-project profile project))

    (context "/:id" []
      (GET "/" []
        :tags ["project"]
        :operationId "getProject"
        :summary "Retrieves a project."
        :path-params [id :- ProjectId]
        :middleware [#(auth! %)]
        :current-user profile
        :responses {200 {:schema      {:meta Meta :data (vector (assoc DataObject :attributes ProjectSchema))}
                         :description "ok"}
                    401 {:schema      {:meta Meta :errors [ErrorObject]}
                         :description "unauthorized"}
                    403 {:schema      {:meta Meta :errors [ErrorObject]}
                         :description "forbidden"}
                    404 {:schema      {:meta Meta :errors [ErrorObject]}
                         :description "not found"}} (read-project profile id))

      (POST "/" []
        :tags ["project"]
        :operationId "updateProject"
        :summary "Updates an existing project."
        :body [project ProjectSchema]
        :path-params [id :- ProjectId]
        :middleware [#(auth! %)]
        :current-user profile
        :responses {200 {:schema      {:meta Meta :data (vector (assoc DataObject :attributes ProjectSchema))}
                         :description "ok"}
                    400 {:schema      {:meta Meta :errors [ErrorObject]}
                         :description "bad request"}
                    401 {:schema      {:meta Meta :errors [ErrorObject]}
                         :description "unauthorized"}
                    403 {:schema      {:meta Meta :errors [ErrorObject]}
                         :description "forbidden"}
                    404 {:schema      {:meta Meta :errors [ErrorObject]}
                         :description "not found"}} (update-project profile id project))

      (DELETE "/" []
        :tags ["project"]
        :operationId "deleteProject"
        :summary "Deletes an existing project."
        :path-params [id :- ProjectId]
        :middleware [#(auth! %)]
        :current-user profile
        :responses {204 {:description "no content"}
                    401 {:schema      {:meta Meta :errors [ErrorObject]}
                         :description "unauthorized"}
                    403 {:schema      {:meta Meta :errors [ErrorObject]}
                         :description "forbidden"}
                    404 {:schema      {:meta Meta :errors [ErrorObject]}
                         :description "not found"}} (delete-project! profile id)))
    transactions-routes
    tasks-routes))