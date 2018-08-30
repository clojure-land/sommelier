(ns apriori.api.handler
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
            [buddy.auth.accessrules :refer [restrict]]
            [buddy.auth :refer [authenticated?]]
            [buddy.auth.middleware :refer [wrap-authentication wrap-authorization]]
            [buddy.auth.backends.session :refer [session-backend]]
            [ring.util.response :as ring]
            [apriori.util.logger :as logger]
            [apriori.util.response :as response]
            [apriori.util.health :as health]
            [apriori.domain.api :as api]
            [crypto.random :as crypto]
            [apriori.util.repository.user :as user]
            [apriori.util.repository.project :as project]
            [clj-http.client :as http]
            [apriori.domain.base :as base]
            [apriori.api.routes.meta :refer [meta-routes]]
            [apriori.api.routes.auth :refer [auth-routes]]
            [apriori.api.routes.project :refer [project-routes]]))

(schema/set-fn-validation! (not-empty (env :schema-validation)))

;; ***** Exception handlers *******************************************************

;(schema/set-fn-validation! (not-empty (env :schema-validation)))

(defn ^:private generate-request-id
  "Generate a request identifier."
  []

  (java.util.UUID/randomUUID))

(defn with-request-id
  "Create new request-id binding."
  [f]

  (fn [request]
    (binding [logger/*request-id* (generate-request-id)]
      (f request))))

(defn exception-handler
  "Handles exceptions."
  [f]

  (fn [^Exception e data request]
    (logger/log :warn e)

    (f (api/->ApiResponse (api/->Meta "failed" nil) nil))))

(defn bad-request-handler
  "Handles bad requests."
  [f]

  (fn [^Exception e data request]
    (let [message (logger/humanize-schema-exception e)]
      (logger/log :warn e)

      (f (api/->ApiResponse (api/->Meta "bad request" nil) message)))))

(defn parse-exception-handler
  "Handles parse exceptions."
  [f]

  (fn [^Exception e data request]
    (logger/log :warn e)

    (f (api/->ApiResponse (api/->Meta "json parse error" nil) "Error parsing request."))))

;; ***** API definition *******************************************************

(def app
  (with-request-id
    (logger/wrapper-with-logger
      (api
        {:exceptions {:handlers
                      {::ex/request-parsing    (parse-exception-handler http-response/bad-request)
                       ::ex/request-validation (bad-request-handler http-response/bad-request)
                       ::ex/default            (exception-handler http-response/internal-server-error)}}
         :middleware [apriori.api.routes.auth/authorized-for-docs?]
         :swagger    {:ui   "/api/docs"
                      :spec "/api/swagger.json"
                      :data {:info                {:title       "API"
                                                   :version     (health/get-version)
                                                   :description ""}
                             :securityDefinitions {:authorization {:type "apiKey"
                                                                  :name "Authorization"
                                                                  :in   "header"}}}}}

         (context "/api" []
           meta-routes
           auth-routes
           project-routes

           (GET "/user" []
             ;:query-params [token]
             :auth-roles #{"read"}
             :current-user user

             (response/ok nil user))

           ;(GET "/users" request
           ;  (->
           ;    (str (env :oauth-domain) "/oauth/token")
           ;    (http/post {:debug        false
           ;                :content-type :json
           ;                :body         (json/encode {:grant_type    "client_credentials"
           ;                                            :client_id     (env :oauth-client-id)
           ;                                            :client_secret (env :oauth-client-secret)
           ;                                            :redirect_uri  (base/get-base-uri request)})})
           ;    (get :body)
           ;    (json/parse-string)
           ;    (response/ok nil)))
           )

                     (undocumented
                       (route/not-found (response/not-found nil nil)))))))