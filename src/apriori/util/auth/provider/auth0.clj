(ns apriori.util.auth.provider.auth0
  (:require [clj-http.client :as http]
            [cheshire.core :as json]
            [clojure.walk :as walk]
            [apriori.util.response :as response]
            [apriori.util.auth.provider :refer :all])
  (:import (apriori.util.auth.provider AuthProviderInterface)
           (apriori.util.auth.provider AuthManagementInterface)))

(defrecord Auth []
  AuthProviderInterface
  (ExchangeAuthorizationCode [this code redirect-uri]
    (let [response (->
                     (str authentication-server "/oauth/token")
                     (http/post {:debug            false
                                 :throw-exceptions false
                                 :content-type     :json
                                 :body             (json/encode {:grant_type    "authorization_code"
                                                                 :client_id     client-id
                                                                 :client_secret client-secret
                                                                 :code          code
                                                                 :redirect_uri  redirect-uri})})
                     (:body)
                     (json/decode)
                     (walk/keywordize-keys))]

      (if (contains? response :access_token)
        (->AccessToken (get response :access_token) (get response :expires_in) (get response :refresh_token))
        (->AuthError (get response :error_description)))))

  (RequestManagementToken [this] "")
  (RefreshToken [this token] "")
  (UserInfo [this token]
    (prn token)
    (->
      (str authentication-server "/userinfo")
      (http/get {:debug            false
                 :throw-exceptions false
                 :headers          {"Authorization" (str "Bearer " token)}})
      (get :body)
      (json/decode))))

(defrecord Management []
  AuthManagementInterface
  (FetchUsers [this filter page] [(->User 1 "test")]))

;https://freid001.auth0.com/authorize?client_id=5fdBTK08X8MDxQPSUTD6TXMBmH08Aa9c&redirect_uri=http://localhost:3000&response_type=code