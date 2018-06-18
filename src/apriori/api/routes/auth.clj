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
            [apriori.domain.api :as api]
            [clj-time.local :as local]
            [pandect.algo.sha256 :refer :all]))

;; ***** Auth implementation ****************************************************

(defn- parse-header [request token-name]
  (some->> (some-> (http-response/find-header request "authorization")
                   (second))
           (re-find (re-pattern (str "^" token-name " (.+)$")))
           (second)))

(defn- is-valid-token? [token]
  (try
    (->
      (str (env :oauth-domain) "/userinfo")
      (http/get {:headers {"Authorization" (str "Bearer " token)}})
      (get :body)
      (json/parse-string)
      (merge {:role "read"}))
    (catch Exception e (logger/log :warn e) (prn e) nil)))

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
                   (is-valid-token?))]

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

; scope: email, profile, projects.

(schema/defschema Credentials
  {(schema/required-key :email)         user/Email
   (schema/required-key :password)      user/Password})

(schema/defschema Grant
  {(schema/required-key :grant)         (schema/enum "code" "password" "client_credentials")
   (schema/required-key :client_id)     schema/Str
   (schema/required-key :client_secret) schema/Str
   (schema/optional-key :code)          schema/Str
   (schema/optional-key :audience)      schema/Str})

(schema/defschema Authorization
  {:authorization_code schema/Str
   :expires            (schema/maybe
                         (schema/constrained
                           schema/Str
                           #(re-matches #"^\d+-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}Z$" (name %)) 'not-timestamp))
   })

(schema/defn authorize
  [client-id :- (schema/maybe auth-code/ClientId) credentials :- Credentials]

  (let [user (first (user/fetch [(user/filter-by-email (:email credentials))]))]
    (if (not-empty user)
      (let [hashed-password (->
                              (assoc user :plain_text_password (:password credentials))
                              (user/hashPlainTextPassword)
                              (user/get-password))]

        (if (= hashed-password (user/get-password user))
          (let [authorization-code (auth-code/spawn client-id (user/get-id user))]
            (response/ok nil {:code    (auth-code/get-authorization-code authorization-code)
                              :expires (local/format-local-time (auth-code/get-expires authorization-code) :date-time-no-ms)}))
          (response/unauthorized nil "Invalid login credentials!")))
      (response/unauthorized nil "Invalid login credentials!"))))

(defn access-token [grant]
  ;(:type grant)
  ;(:client_id grant)
  ;(:client_secret grant)
  ;(:code code)

  ;1) IF grant type = authorization_code client_credentials
  ;2) query auth_code table where client_id => client and client_secret => client and code => code
  ;3) IF result true then make & return token (step 8)
  ;4) DELETE auth code from table
  ;===
  ;5) IF grant type = client_credentials
  ;6) query client table where client_id => client and client_secret => client
  ;7) IF result true then make & return token (step 8)

  ;8) create token hash
  ;9)

  ;{
  ; "access_token": "eyJz93a...k4laUWw",
  ; "refresh_token": "GEbRxBN...edjnXbL",
  ; "id_token": "eyJ0XAi...4faeEoQ",
  ; "token_type": "Bearer"
  ; }

  (response/ok nil nil))

(def auth-routes
  (api
    (POST "/authorize" [] :tags ["auth"]
                          :summary "Request an authorize code."
                          :query-params [client_id :- auth-code/ClientId
                                         response_type :- (schema/enum "code" "token")
                                         redirect_uri :- schema/Str
                                         {client_secret :- schema/Str nil}
                                         {scope :- schema/Str nil}]
                          :body [credentials Credentials]
                          :responses {200 {:schema      {:meta api/Meta :data Authorization}
                                           :description "authorization"}
                                      401 {:description "unauthorized"}} (authorize client_id credentials))

    (context "/oauth" [] :tags ["auth"]
                         (POST "/token" []
                           :summary "Request an access_token."
                           :body [grant Grant]
                           :responses {200 {:description "authorization"}
                                       401 {:description "unauthorized"}} (access-token nil)))))

;(try
;  (->
;    (str (env :oauth-domain) "/oauth/token")
;    (http/post {:debug        false
;                :content-type :json
;                :body         (json/encode {:grant_type    "authorization_code"
;                                            :client_id     (env :oauth-client-id)
;                                            :client_secret (env :oauth-client-secret)
;                                            :code          code
;                                            :redirect_uri  (get-base-uri request)})})
;    (get :body)
;    (json/parse-string)
;    (response/ok nil))
;  (catch Exception e (logger/log :warn e)
;                     (response/unauthorized nil "Unauthorized request."))))