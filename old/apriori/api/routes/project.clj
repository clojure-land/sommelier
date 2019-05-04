(ns apriori.api.routes.project
  (:require [compojure.api.sweet :refer :all]
            [compojure.api.exception :as ex]
            [compojure.route :as route]
            [clojure.walk :as walk]
            [clj-time.local :as local]
            [clj-time.format :as format]
            [ring.util.http-response :as http-response]
            [cheshire.core :as json]
            [schema.core :as schema]
            [environ.core :refer [env]]
            [ring.util.response :as ring]
            [apriori.util.logger :as logger]
            [apriori.util.domain.response :as response]
            ))

;; ***** Project implementation ********************************************************

(defn create-project [session-id name]
  ;(project/save (project/->ProjectRecord (java.util.UUID/randomUUID) "" name 0 nil nil))
  )

(defn modify-project [id name]
  ;(project/save (project/->ProjectRecord (java.util.UUID/randomUUID) "" name 0 nil nil))
  )

(defn get-project [id]
  ;(response/ok nil (project/fetch [(project/filter-by-id id)]))
  )

(defn refresh-project-api-key [token refresh-key]
  nil)

(defn record-transactions
  "Record transactions in the database."
  [token transactions]

  ;(if (not-empty (project/fetch [(project/filter-by-id token)]))
  ;  (prn transactions)
  ;  (response/not-found nil "Can not record transactions, no project was found!"))
  )

;; ***** API definition *******************************************************

(schema/def ProjectName
  (->
    schema/Str
    (schema/constrained #(re-matches #"^[a-zA-Z0-9\s\d]*" %) 'alphanumeric?)
    (schema/constrained #(<= 0 (count %) 225) (list 'length? 0 225))))

(schema/defschema RequestProject
  {(schema/required-key :name) ProjectName})

(schema/defschema ResponseProject
  {:uuid         schema/Uuid
   :name         schema/Str
   :modified     schema/Inst
   :created      schema/Inst})

(def project-routes
  (context "/project" [] :tags ["project"]

                         (POST "/" []
                           :summary "Create a project."
                           :body [project RequestProject]
                           :auth-roles #{:user}
                           :current-session session-id
                           :responses {201 {:schema      {:meta response/Meta :data ResponseProject}
                                            :description "created"}} (create-project session-id project))

                         ;(context "/:id" []
                         ;  (GET "/" []
                         ;    :summary "Get a project."
                         ;    :header-params [authorization :- schema/Str]
                         ;    :path-params [id :- schema/Uuid]
                         ;    :auth-roles #{"read"} (get-project id))
                         ;
                         ;  (POST "/" []
                         ;    :summary "Modify a project."
                         ;    :path-params [id :- schema/Uuid]
                         ;    :body-params [name :- schema/Str]
                         ;    :auth-roles #{"write"} (modify-project id name))
                         ;
                         ;  (POST "/transactions" []
                         ;    :summary "Register transactions."
                         ;    :body-params [transactions schema/Any]
                         ;    :auth-roles #{"write"})        ;(record-transactions token transactions)
                         ;
                         ;  (GET "/associations" []
                         ;    :summary "Fetch associations."
                         ;    :query-params [{item :- schema/Str nil}
                         ;                   {timestamp_from :- schema/Str nil}
                         ;                   {timestamp_to :- schema/Str nil}
                         ;                   {support_min :- schema/Int nil}
                         ;                   {support_max :- schema/Int nil}
                         ;                   {confidence_min :- schema/Int nil}
                         ;                   {confidence_max :- schema/Int nil}
                         ;                   {lift_min :- schema/Int nil}
                         ;                   {lift_max :- schema/Int nil}]
                         ;    :auth-roles #{"read"}))

                         ))