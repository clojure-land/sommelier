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
            [apriori.util.repository.user_project :as user_project]

            [clj-http.client :as http]

            [apriori.api.routes.meta :refer [meta-routes]]
            [apriori.api.routes.auth :refer [auth-routes]]
            [apriori.api.routes.project :refer [project-routes]]))

;; ***** Exception handlers *******************************************************

(schema/set-fn-validation! (not-empty (env :schema-validation)))

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

(defn get-base-uri [request]
  "Generate a base uri from a ring request."
  (let [scheme (name (:scheme request))
        context (:context request)
        hostname (get (:headers request) "host")]
    (str scheme "://" hostname)))

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
                             :securityDefinitions {:api_key {:type "Bearer"
                                                             :name "Authorization"
                                                             :in   "header"}}}}}

        ;https://freid001.auth0.com/login?client=Ms08md6V1yC2CGCcnsFL4kZvZFlzlWA3&redirect_uri=http://localhost:3000/api/authenticate/user&response_type=code&scope=openid profile

        (context "/api" []
          meta-routes
          auth-routes
          project-routes

          (GET "/user" []
            :query-params [token]
            :auth-roles #{"read"}
            :current-user user

            (response/ok nil user))

          (GET "/users" request
            (->
              (str (env :oauth-domain) "/oauth/token")
              (http/post {:debug        false
                          :content-type :json
                          :body         (json/encode {:grant_type    "client_credentials"
                                                      :client_id     (env :oauth-client-id)
                                                      :client_secret (env :oauth-client-secret)
                                                      :redirect_uri  (get-base-uri request)})})
              (get :body)
              (json/parse-string)
              (response/ok nil))))

        (undocumented
          (route/not-found (response/not-found nil nil)))))))