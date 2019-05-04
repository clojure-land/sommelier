(ns apriori.api.routes.auth
  (:require [compojure.api.sweet :refer :all]
            [schema.core :as schema]
            [ring.util.http-response :as http-response]
            [environ.core :refer [env]]
            [pandect.algo.sha256 :refer :all]
            [apriori.util.domain.response :refer :all]
            [apriori.util.storage.redis :as redis]
            [apriori.util.auth.provider :refer :all]
            [apriori.util.auth.provider.auth0 :refer :all]))

;; ***** Auth implementation ****************************************************
(defn- parse-header [request token-name]
  (some->> (some-> (http-response/find-header request "authorization")
                   (second))
           (re-find (re-pattern (str "^" token-name " (.+)$")))
           (second)))

(defn- admin? [session]
  (if (contains? #{12083} (:id session))
    (assoc session :role "admin")
    (assoc session :role "user")))

(defn has-role? [role required-roles]
  (let [has-roles (case role
                    "admin" #{:user :admin}
                    "user" #{:user}
                    #{})

        matched-roles (clojure.set/intersection has-roles required-roles)]
    (not (empty? matched-roles))))

(defn- token-session [^apriori.util.auth.provider.AccessToken token]
  (if (empty? (redis/wcar* (redis/get (get token :token))))
    (do
      (->>
        (SessionInfo (->Auth (env :client-id)
                             (env :client-secret)
                             (env :authentication-server)) (get token :token))
        (redis/set (get token :token))
        (redis/wcar*))
      (redis/wcar* (redis/expire (get token :token) (get token :expires)))))

  (redis/wcar* (redis/get (get token :token))))

;; ***** Handle auth provider *************************************************
(defmulti auth-provider class)

(defmethod auth-provider apriori.util.auth.provider.AccessToken
  [token]

  (let [session (SessionInfo (->Auth (env :client-id)
                                     (env :client-secret)
                                     (env :authentication-server))
                             (get token :token))]

    (if (instance? apriori.util.auth.provider.Session session)
      (do
        (token-session token)

        (Status
          (->ApiResponse nil token) 200))
      (auth-provider session))))

(defmethod auth-provider apriori.util.auth.provider.AuthError
  [error]

  (Status
    (->ApiResponse nil {:authorization (:error error)}) 401))

(defmethod auth-provider apriori.util.auth.provider.PermissionError
  [error]

  (Status
    (->ApiResponse nil {:authorization (:error error)}) 403))

;; ***** Request an access token ***********************************************
(defmulti request-token (fn [grant] (get grant :grant)))

(defmethod request-token "code"
  [grant]

  (auth-provider (ExchangeAuthorizationCode (->Auth (env :client-id)
                                                    (env :client-secret)
                                                    (env :authentication-server))
                                            (get grant :code)
                                            (get grant :redirect_uri))))

(defmethod request-token "password"
  [grant]

  (auth-provider (ExchangePassword (->Auth (env :client-id)
                                           (env :client-secret)
                                           (env :authentication-server))
                                   (get grant :username)
                                   (get grant :password))))

(defmethod request-token "client_credentials"
  [grant]

  (auth-provider (ExchangeClient (->Auth (get grant :client_id)
                                         (get grant :client_secret)
                                         (env :authentication-server)))))

;; ***** Auth middleware **************************************************
(defn- require-roles [handler roles]
  (fn [request]
    (let [token (parse-header request "Bearer")
          session (token-session (->AccessToken token nil nil "Bearer" 900))]

      (if (instance? apriori.util.auth.provider.Session session)
        (if (has-role? (:role (admin? session)) roles)
          (handler (assoc request :identity (:id session)))
          (auth-provider (->PermissionError "Permission denied.")))
        (auth-provider (->AuthError "Unauthorized request."))))))

(defmethod compojure.api.meta/restructure-param :auth-roles
  [_ required-roles acc]

  (update-in acc [:middleware] conj [require-roles required-roles]))

(defmethod compojure.api.meta/restructure-param :current-session
  [_ binding acc]

  (update-in acc [:letks] into [binding `(:identity ~'+compojure-api-request+)]))

;; ***** API definition *******************************************************
(schema/defschema Grant
  {(schema/required-key :grant)         (schema/enum "code" "client_credentials" "password")
   (schema/optional-key :username)      schema/Str
   (schema/optional-key :password)      schema/Str
   (schema/optional-key :client_id)     schema/Str
   (schema/optional-key :client_secret) schema/Str
   (schema/optional-key :code)          schema/Str
   (schema/optional-key :redirect_uri)  schema/Str})

(def auth-routes
  (routes
    (POST "/oauth/token" request
      :tags ["auth"]
      :summary "Request an access token."
      :body [grant Grant]
      :responses {200 {:description "authorization"}
                  401 {:description "unauthorized"}} (request-token grant))))
