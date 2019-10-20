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
            [routes.jobs :refer [jobs-routes]])
  (:import [org.bson.types ObjectId]))

;; ***** Project implementation ********************************************************

(defn- create-project-if-not-exists
  "Creates a new project.

  e.g. (create-project 'user' {:name 'some name', :description '', :window 0, :min-support 0, :min-confidence 0})"
  [author body]

  (let [data (project->resource-object (save-project nil (merge body {:author author})))]
    (api-response (->ApiData {:status "created"} data) 201 [{:location (str "/project/" (get (first data) :id))}])))

(defn- read-project
  "Retrieves a project.

  e.g. (read-project '5da04bbd9194be00066ac0b8')"
  [id author]

  (if (objectId? id)
    (let [project (get-project {:_id (ObjectId. (str id)) :author author})]
      (if (not= project [])
        (api-response (->ApiData {:status "ok"} (project->resource-object project)) 200 [])
        (project-not-found id)))
    (project-not-found id)))

(defn- update-project
  "Updates an existing project.

  e.g (update-project '5da04b299194be00066ac0b6' 'user' {:name 'some name', :description '', :window 0, :min-support 0, :min-confidence 0})"
  [id author body]

  (if (and (objectId? id)
           (not= (get-project {:_id (ObjectId. (str id)) :author author}) []))

    (api-response
      (->>
        (save-project id (merge body {:author author}))
        (project->resource-object)
        (->ApiData {:status "ok"})) 200 [])
    (project-not-found id)))

(defn- delete-project!
  "Deletes an existing project.

  e.g (delete-project '5da04b299194be00066ac0b6' 'user')"
  [id author]

  (if (and (objectId? id)
           (not= (get-project {:_id (ObjectId. (str id)) :author author}) []))
    (do
      (remove-project (ObjectId. (str id)))
      (ring/status nil 204))
    (project-not-found id)))

;; ***** Project definition *******************************************************

(def project-routes
  (context "/project" []
    (POST "/" []
      :tags ["project"]
      :operationId "createProject"
      :summary "Creates a new project."
      :body [project ProjectSchema]
      ;:middleware [#(util.auth/auth! %)]
      ;:current-user profile
      :responses {201 {:schema {:meta Meta :data (vector (assoc DataObject :attributes ProjectSchema))}
                       :description "created"}
                  400 {:schema {:meta Meta :errors [ErrorObject]}
                       :description "bad request"}
                  403 {:schema {:meta Meta :errors [ErrorObject]}
                       :description "unauthorized"}} (create-project-if-not-exists "" project))

    (context "/:id" []
      (GET "/" []
        :tags ["project"]
        :operationId "getProject"
        :summary "Retrieves a project."
        :path-params [id :- ProjectId]
        ;:middleware [#(util.auth/auth! %)]
        ;:current-user profile
        :responses {200 {:schema {:meta Meta :data (vector (assoc DataObject :attributes ProjectSchema))}
                         :description "ok"}
                    403 {:schema {:meta Meta :errors [ErrorObject]}
                         :description "unauthorized"}
                    404 {:schema {:meta Meta :errors [ErrorObject]}
                         :description "not found"}} (read-project id ""))

      (POST "/" []
        :tags ["project"]
        :operationId "updateProject"
        :summary "Updates an existing project."
        :body [project ProjectSchema]
        :path-params [id :- ProjectId]
        ;:middleware [#(util.auth/auth! %)]
        ;:current-user profile
        :responses {200 {:schema {:meta Meta :data (vector (assoc DataObject :attributes ProjectSchema))}
                         :description "ok"}
                    400 {:schema {:meta Meta :errors [ErrorObject]}
                         :description "bad request"}
                    403 {:schema {:meta Meta :errors [ErrorObject]}
                         :description "unauthorized"}
                    404 {:schema {:meta Meta :errors [ErrorObject]}
                         :description "not found"}} (update-project id "" project))

      (DELETE "/" []
        :tags ["project"]
        :operationId "deleteProject"
        :summary "Deletes an existing project."
        :path-params [id :- ProjectId]
        ;:middleware [#(util.auth/auth! %)]
        ;:current-user profile
        :responses {204 {:description "no content"}
                    403 {:schema {:meta Meta :errors [ErrorObject]}
                         :description "unauthorized"}
                    404 {:schema {:meta Meta :errors [ErrorObject]}
                         :description "not found"}} (delete-project! id "")))
    transactions-routes
    jobs-routes))