(ns routes.task
  (:require [compojure.api.sweet :refer :all]
            [util.response :refer :all]
            [util.auth :refer :all]
            [util.mdb :refer :all]
            [domain.response :refer :all]
            [domain.project :refer :all]
            [domain.task :refer :all]
            [domain.association :refer :all]
            [schema.core :as schema]
            [clojure.string :as str]
            [clojure.walk :as walk]
            [monger.json]
            [clojure.tools.logging :as log])
  (:import [org.bson.types ObjectId]))

;; ***** Task implementation ********************************************************

(defn- parse-params
  "Returns params parsed by field type.

  e.g (parse-params {:support_gte 'double'} {:support_gte '0.7'}}"
  [fields params]

  (reduce (fn [parsed [key type]]
            (try
              (if (contains? params key)
                (case type
                  "string" (assoc parsed key (str (get params key)))
                  "double" (assoc parsed key (Double/parseDouble (get params key)))) parsed)
              (catch Exception e (log/error e)))) {} fields))

(defn- filter-by-params
  "Returns ref for mdb.

  e.g (filter-by-params {:support_gte 0.25})"
  [params]

  (reduce (fn [filter [key val]]
           (let [k (str/split (name key) #"_")]
             (case (last k)
               "gte" (assoc filter (keyword (first k)) {"$gte" val})
               "lte" (assoc filter (keyword (first k)) {"$lte" val})
               (assoc filter (keyword (first k)) val)))) {} params))

(defn- read-associations
  "Retrieves a project.

  e.g. (read-associations {:sub ''} '5da04bbd9194be00066ac0b8' {})"
  [profile id query-params]

  (if-let [task (first (get-tasks {:_id (ObjectId. (str id))}))]
    (if-let [project (get-project-if-exists (get task :project-id))]
      (if (has-permission? (get profile :sub) id "project")
          (api-response (->>
                          (walk/keywordize-keys query-params)
                          (parse-params {:antecedents     "string"
                                         :consequents     "string"
                                         :support_gte     "double"
                                         :support_gle     "double"
                                         :confidence_gte  "double"
                                         :confidence_lte  "double"
                                         :lift_gte        "double"
                                         :lift_lte        "double"
                                         :leverage_gte    "double"
                                         :leverage_lte    "double"
                                         :conviction_gte  "double"
                                         :conviction_lte  "double"})
                          (filter-by-params)
                          (get-association-rules id
                                                 (try
                                                   (Integer/parseInt (get query-params "page"))
                                                   (catch Exception e (log/error e) 1))

                                                 (if (not (empty? (get query-params "sort_by")))
                                                   (keyword (get query-params "sort_by")) :lift)

                                                 (if (= (get query-params "order_by") "asc") 1 -1))
                          (association->resource-object)
                          (->ApiData {:status "ok"})) 200 [])
          (forbidden))
      (project-not-found (get task :project-id)))
    (task-not-found id)))

;; ***** Task definition *******************************************************

(def task-routes
  (context "/task" []

    (GET "/:id" []
      :path-params [id :- TaskId]
      :tags ["task"]
      :operationId "getTasks"
      :summary "Retrieves tasks."
      ;:middleware [#(auth! %)]
      ;:current-user profile
      :responses {200 {;:schema {:meta Meta ::data [{:type "task" :attributes {:status "scheduled,running,done,failed"}}]
                       ;content-location = /project/1/task
                       :description "ok"}
                  400 {:schema {:meta Meta :errors [ErrorObject]}
                       :description "bad request"}
                  401 {:schema {:meta Meta :errors [ErrorObject]}
                       :description "unauthorized"}
                  403 {:schema {:meta Meta :errors [ErrorObject]}
                       :description "forbidden"}
                  404 {:schema {:meta Meta :errors [ErrorObject]}
                       :description "not found"}} "")

    ; run, delete
    ;(POST "/:id/action" []
    ;  :path-params [id :- TaskId]
    ;  :tags ["task"]
    ;  :operationId "runTask"
    ;  :summary "Retrieves tasks."
    ;  ;:middleware [#(util.auth/auth! %)]
    ;  ;:current-user profile
    ;  :responses {200 {;:schema {:meta Meta ::data [{:type "task" :attributes {:status "scheduled,running,done,failed"}}]
    ;                   ;content-location = /project/1/task
    ;                   :description "ok"}
    ;              400 {:schema {:meta Meta :errors [ErrorObject]}
    ;                   :description "bad request"}
    ;              403 {:schema {:meta Meta :errors [ErrorObject]}
    ;                   :description "unauthorized"}
    ;              404 {:schema {:meta Meta :errors [ErrorObject]}
    ;                   :description "not found"}} "")

    ;(GET "/:id/frequencies" []
    ;  :path-params [id :- TaskId]
    ;  :tags ["task"]
    ;  :operationId "getFrequencies"
    ;  :summary "Retrieves tasks."
    ;  ;:middleware [#(util.auth/auth! %)]
    ;  ;:current-user profile
    ;  :responses {200 {:description "ok"}
    ;              400 {:schema {:meta Meta :errors [ErrorObject]}
    ;                   :description "bad request"}
    ;              403 {:schema {:meta Meta :errors [ErrorObject]}
    ;                   :description "unauthorized"}
    ;              404 {:schema {:meta Meta :errors [ErrorObject]}
    ;                   :description "not found"}} "")

    (GET "/:id/associations" {q :query-params}
      :path-params [id :- TaskId]
      :query-params [{page :- (describe schema/Num "page number.") 0}
                     {order_by :- (describe (schema/enum "desc", "asc") "order results.") "desc"}
                     {sort_by :- (describe (schema/enum "support", "confidence", "lift", "leverage", "conviction") "sort results.") "lift"}
                     {antecedents :- (describe schema/Str "antecedents delimited by comma.") nil}
                     {consequents :- (describe schema/Str "consequents delimited by comma.") nil}
                     {support_gte :- (describe schema/Num "support greater than or equal to.") nil}
                     {support_lte :- (describe schema/Num "support less than or equal to.") nil}
                     {confidence_gte :- (describe schema/Num "confidence greater than or equal to.") nil}
                     {confidence_lte :- (describe schema/Num "confidence less than or equal to.") nil}
                     {lift_gte :- (describe schema/Num "lift greater than or equal to.") nil}
                     {lift_lte :- (describe schema/Num "lift less than or equal to.") nil}
                     {leverage_gte :- (describe schema/Num "leverage greater than or equal to.") nil}
                     {leverage_lte :- (describe schema/Num "leverage less than or equal to.") nil}
                     {conviction_gte :- (describe schema/Num "conviction greater than or equal to.") nil}
                     {conviction_lte :- (describe schema/Num "conviction less than or equal to.") nil}]
      :tags ["task"]
      :operationId "getAssociations"
      :summary "Retrieves associations."
      :middleware [#(auth! %)]
      :current-user profile
      :responses {200 {:schema {:meta Meta :data (vector (assoc DataObject :attributes AssociationSchema))}
                       :description "ok"}
                  400 {:schema {:meta Meta :errors [ErrorObject]}
                       :description "bad request"}
                  401 {:schema {:meta Meta :errors [ErrorObject]}
                       :description "unauthorized"}
                  403 {:schema {:meta Meta :errors [ErrorObject]}
                       :description "forbidden"}
                  404 {:schema {:meta Meta :errors [ErrorObject]}
                       :description "not found"}} (read-associations profile id q))))
