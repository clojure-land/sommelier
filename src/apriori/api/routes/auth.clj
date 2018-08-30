(ns apriori.api.routes.auth
  (:require [compojure.api.sweet :refer :all]
            [schema.core :as schema]
            [buddy.core.mac :as mac]
            [buddy.core.codecs :as codecs]
            [clojure.string :as cstr]
            [apriori.util.response :as response]
            [ring.util.http-response :as http-response]
            [cheshire.core :as json]
            [clj-http.client :as http]
            [clj-time.core :as t]
            [clj-time.format :as format]
            [clj-time.coerce :as coerce]
            [crypto.random :as crypto]
            [ring.util.response :as ring]
            [environ.core :refer [env]]
            [apriori.util.logger :as logger]
            [apriori.util.repository.user :as user]
            [apriori.util.repository.authorization-code :as auth-code]
            [crypto.random :as crypto]
            [apriori.domain.base :as base]
            [apriori.domain.api :as api]
            [clj-time.local :as local]
            [pandect.algo.sha256 :refer :all]
            [apriori.util.auth.provider.auth0 :refer :all])
  (:import (apriori.util.auth.provider AuthProviderInterface)
           (apriori.util.auth.provider AuthManagementInterface)))

;; ***** Auth implementation ****************************************************

(defn- parse-header [request token-name]

  (some->> (some-> (http-response/find-header request "Authorization")
                   (second))
           (re-find (re-pattern (str "^" token-name " (.+)$")))
           (second)))

(defn- has-role? [role required-roles]
  (let [has-roles (case role
                    "admin" #{"read" "write" "author" "admin"}
                    "author" #{"read" "write" "author"}
                    "write" #{"read" "write"}
                    "read" #{"read"}
                    #{})
        matched-roles (clojure.set/intersection has-roles required-roles)]
    (not (empty? matched-roles))))

(defn authorized-for-docs? [handler]
  (fn [request]
    (let [auth-header (get (:headers request) "authorization")]
      (cond
        (nil? auth-header)
        (response/unauthorized nil {:user "Unauthorized request."})

        (= auth-header "Your Basic Auth Secret")
        (handler request)

        :else
        (response/unauthorized nil {:user "Unauthorized request."})))))

(defn- require-roles [handler roles]
  (fn [request]

    (let [user (-> (parse-header request "Bearer")
                   (.UserInfo (->Auth)))]
      (prn user)

      (if-not user
        (response/unauthorized nil {:user "Unauthorized request."})
        (if-not (has-role? (:role user) roles)
          (response/forbidden nil {:user "Permission denied."})
          (let [request (assoc request :identity user)]
            (handler request)))))))

;; ***** Route helpers ********************************************************

(defmethod compojure.api.meta/restructure-param :auth-roles
  [_ required-roles acc]
  (update-in acc [:middleware] conj [require-roles required-roles]))

(defmethod compojure.api.meta/restructure-param :current-user
  [_ binding acc]
  (update-in acc [:letks] into [binding `(:identity ~'+compojure-api-request+)]))

;; ***** API definition *******************************************************

(schema/defschema Grant
  {(schema/required-key :grant)         (schema/enum "code" "refresh_token" "client_credentials")
   (schema/optional-key :client_id)     schema/Str
   (schema/optional-key :client_secret) schema/Str
   (schema/optional-key :code)          schema/Str
   (schema/optional-key :token)         schema/Str
   (schema/optional-key :audience)      schema/Str})

(schema/defn request-token [base-uri grant :- Grant]
  (let [response (case
                   (get grant :grant)
                   "code" (.ExchangeAuthorizationCode (->Auth) (get grant :code) base-uri)
                   "refresh_token" (.RefreshToken (->Auth) (get grant :token))
                   "client_credentials" nil)]

    (apriori.util.auth.provider/auth-handler response)))

(def auth-routes
  (api
    (context "/oauth" [] :tags ["auth"]
                         (POST "/token" request
                           :summary "Request an access token."
                           :body [grant Grant]
                           :responses {200 {:description "authorization"}
                                       401 {:description "unauthorized"}} (request-token (base/get-base-uri request) grant)))))