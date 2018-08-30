(ns apriori.util.auth.provider
  (:require [environ.core :refer [env]]
            [apriori.domain.api :as api]
            [apriori.util.response :as response]
            [cheshire.core :as json]))

(def client-id (env :client-id))
(def client-secret (env :client-secret))
(def authentication-server (env :authentication-server))

(defrecord User [id user])
(defrecord AccessToken [token expires refresh_token])
(defrecord AuthError [error])

(definterface AuthProviderInterface
  (ExchangeAuthorizationCode [code redirect-uri] "Exchange a users authorization code for an access token.")
  (RequestManagementToken [] "Request an access token which is issued to the application itself, instead of an end user.")
  (RefreshToken [token] "Use the refresh token to obtain a new access token.")
  (UserInfo [token] "Fetch user info."))

(definterface AuthManagementInterface
  (FetchUsers [filter page] "Retrieve a list of users."))

(defmulti auth-handler class)

(defmethod auth-handler AccessToken [access-token]
  (response/ok nil (json/encode access-token)))

(defmethod auth-handler AuthError [auth-error]
  (response/unauthorized (api/->Meta "unauthorized" nil) {:token (:error auth-error)}))