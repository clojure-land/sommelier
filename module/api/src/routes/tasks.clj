(ns routes.tasks
  (:require [compojure.api.sweet :refer :all]
            [util.response :refer :all]
            [util.auth :refer :all]
            [util.mdb :refer :all]
            [domain.response :refer :all]
            [domain.project :refer :all]
            [domain.task :refer :all]
            [monger.json])
  (:import [org.bson.types ObjectId]))

;; ***** Tasks implementation ********************************************************

(defn- read-tasks
  "Retrieves tasks.

  e.g. (read-tasks {:sub ''} '5da04bbd9194be00066ac0b8' 'user')"
  [profile id]

  (if-let [project (get-project-if-exists id)]
    (if (has-permission? (get profile :sub) id "project")
      (api-response (->ApiData {:status "ok"} (task->resource-object (get-tasks {:project-id (ObjectId. (str id))}))) 200 [])
      (forbidden))
    (project-not-found id)))

;; ***** Tasks definition *******************************************************

(def tasks-routes
  (GET "/:id/tasks" []
    :path-params [id :- ProjectId]
    :tags ["project"]
    :operationId "getTasks"
    :summary "Retrieves tasks."
    :middleware [#(auth! %)]
    :current-user profile
    :responses {200 {:schema {:meta Meta :data (vector (assoc DataObject :attributes TaskSchema))}
                     :description "ok"}
                400 {:schema {:meta Meta :errors [ErrorObject]}
                     :description "bad request"}
                401 {:schema {:meta Meta :errors [ErrorObject]}
                     :description "unauthorized"}
                403 {:schema {:meta Meta :errors [ErrorObject]}
                     :description "forbidden"}
                404 {:schema {:meta Meta :errors [ErrorObject]}
                     :description "not found"}} (read-tasks profile id)))