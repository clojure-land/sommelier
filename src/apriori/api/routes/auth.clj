(ns apriori.api.routes.auth
  (:require [compojure.api.sweet :refer :all]
            [schema.core :as schema]
            [buddy.core.mac :as mac]
            [buddy.core.codecs :as codecs]
            [clojure.string :as cstr]
            [apriori.util.response :as response]
            [ring.util.http-response :as http-response]
            [cheshire.core :as json]
            [clj-time.local :as local]
            [clj-http.client :as http]
            [clj-time.core :as t]
            [ring.util.response :as ring]
            [environ.core :refer [env]]
            [apriori.util.logger :as logger]
            [apriori.util.repository.user :as user]))

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

(schema/defschema Credentials
  {(schema/required-key :email)    user/Email
   (schema/required-key :password) user/Password})

(schema/defschema Grant
  {(schema/required-key :grant)         (schema/enum "authorization_code" "client_credentials")
   (schema/required-key :client_id)     schema/Str
   (schema/required-key :client_secret) schema/Str
   (schema/optional-key :code)          schema/Str})

; sign in -> code
; code -> token

; token -> project


(schema/defn auth [credentials :- Credentials]
  (let [user (first (user/fetch [(user/filter-by-email (:email credentials))]))]

    (if (not-empty user)
      (let [hashed-password (->
                              (assoc user :plain-text-password (:password credentials))
                              (user/hashPlainTextPassword)
                              (user/get-password))]

        (if (= hashed-password (user/get-password user))
          (response/ok nil {:auth 123})
          (response/unauthorized nil "Invalid login credentials!")))
      (response/unauthorized nil "Invalid login credentials!"))))

(defn access_token [grant]
  (response/ok nil nil))

(def auth-routes
  (api
    (POST "/authorize" [] :tags ["auth"]
                          :summary "Request an authorize code."
                          :query-params [{client_id :- schema/Str nil}
                                         {redirect_uri :- schema/Str nil}]
                          :body [credentials Credentials]
                          :responses {200 {:description "authorization"}
                                      401 {:description "unauthorized"}} (auth credentials))

    (context "/oauth" [] :tags ["auth"]
                         (POST "/token" []
                           :summary "Request an access_token."
                           :body [grant Grant]
                           :responses {200 {:description "authorization"}
                                       401 {:description "unauthorized"}} (access_token nil))

                         )))


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