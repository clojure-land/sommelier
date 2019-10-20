(ns util.auth
  (:require
    [domain.response :refer :all]
    [util.request :refer :all]
    [util.response :refer :all]
    [compojure.api.sweet :refer :all]
    [ring.util.http-response :as http-response]
    [buddy.core.keys :as keys]
    [buddy.core.codecs.base64 :as b64]
    [buddy.sign.jwt :as jwt]
    [buddy.sign.jws :as jws]
    [clj-time.local :as local]
    [clj-time.coerce :as coerce]
    [cheshire.core :as json]
    [environ.core :refer [env]]
    [clojure.tools.logging :as log]))

(def jwk-endpoint
  (env :jwk-endpoint))

(def client-secret
  (env :client-secret))

(defn- parse-header [request token-name]
  (some->> (some-> (http-response/find-header request "authorization")
                   (second))
           (re-find (re-pattern (str "^" token-name " (.+)$")))
           (second)))

(defn- fetch-key []
  (if (some? jwk-endpoint)
    (->
      (slurp jwk-endpoint)
      (json/parse-string keyword)
      (get :keys)
      (first)
      (keys/jwk->public-key))
    (b64/decode client-secret)))

(defn- fetch-alg []
  (if (some? jwk-endpoint)
    {:alg :rs256}
    {:alg :hs256}))

(defn- unsign [token]
  (try
    (->
      (jws/unsign token (fetch-key) (fetch-alg))
      (String.)
      (json/parse-string true))
    (catch Exception ex (log/warn *id* (ex-data ex)))))

(defn- unauthorized? [profile]
  (cond
    (empty? profile) true
    (and (some? (get profile :exp))
         (< (get profile :exp) (coerce/to-epoch (local/local-now)))) true
    :else false))

(defn auth! [handler]
  (fn [request]
    (let [profile (unsign (parse-header request "Bearer"))]
      (if (unauthorized? profile)
        (api-response (->ApiError {:status "unauthorized"} [{:message "Unauthorized request."}]) 401 [])
        (handler (assoc request :identity profile))))))

(defmethod compojure.api.meta/restructure-param :current-user
  [_ binding acc]
  (update-in acc [:letks] into [binding `(:identity ~'+compojure-api-request+)]))
