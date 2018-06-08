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
            [apriori.util.response :as response]
            [apriori.util.health :as health]
            [apriori.domain.api :as api]
            [apriori.util.repository.project :as project]))

;; ***** Project implementation ********************************************************

(defn create-project [name]
  (project/save (project/->ProjectRecord (java.util.UUID/randomUUID) "" name 0 nil nil)))

(defn modify-project [id name]
  (project/save (project/->ProjectRecord (java.util.UUID/randomUUID) "" name 0 nil nil)))

(defn get-project [id]
  (response/ok nil (project/fetch [(project/filter-by-id id)])))

(defn refresh-project-api-key [token refresh-key]
  nil)

(defn record-transactions
  "Record transactions in the database."
  [token transactions]

  (if (not-empty (project/fetch [(project/filter-by-id token)]))
    (prn transactions)
    (response/not-found nil "Can not record transactions, no project was found!")))

;; ***** API definition *******************************************************

(schema/defschema RequestProject
  {(schema/required-key :name)     schema/Str
   (schema/required-key :password) schema/Str})

(schema/defschema ResponseProject
  {:id           schema/Uuid
   :name         schema/Str
   :api_key      schema/Str
   :transactions schema/Int
   :modified     schema/Inst
   :created      schema/Inst})

(def project-routes
  (context "/project" [] :tags ["project"]

                         (PUT "/" []
                           :summary "Create a project."
                           :body [project RequestProject]
                           :responses {201 {:schema      {:meta api/Meta :data ResponseProject}
                                            :description "created"}} (create-project name))

                         (context "/:token" []
                           (GET "/" []
                             :summary "Get a project."
                             :header-params [authorization :- schema/Str]
                             :path-params [token :- schema/Uuid]
                             :auth-roles #{"read"} (get-project token))

                           (POST "/" []
                             :summary "Modify a project."
                             :path-params [token :- schema/Uuid]
                             :body-params [name :- schema/Str]
                             :auth-roles #{"write"} (modify-project token name))

                           (POST "/refresh" []
                             :summary "Refresh the api key."
                             :path-params [token :- schema/Uuid
                                           refresh-key :- schema/Uuid]
                             :body-params [api_key :- schema/Uuid]
                             :auth-roles #{"admin"} (refresh-project-api-key token refresh-key))

                           (POST "/transactions" []
                             :summary "Register transactions."
                             :body-params [transactions schema/Any]
                             :auth-roles #{"write"}) ;(record-transactions token transactions)

                           (GET "/associations" []
                             :summary "Fetch associations."
                             :query-params [{item :- schema/Str nil}
                                            {timestamp_from :- schema/Str nil}
                                            {timestamp_to :- schema/Str nil}
                                            {support_min :- schema/Int nil}
                                            {support_max :- schema/Int nil}
                                            {confidence_min :- schema/Int nil}
                                            {confidence_max :- schema/Int nil}
                                            {lift_min :- schema/Int nil}
                                            {lift_max :- schema/Int nil}]
                             :auth-roles #{"read"}))))