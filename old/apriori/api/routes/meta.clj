(ns apriori.api.routes.meta
  (:require [compojure.api.sweet :refer :all]
            [ring.middleware.cors :refer [wrap-cors]]
            [clj-time.core :as time]
            [environ.core :refer [env]]
            [clj-time.format :as format]
            [schema.core :as schema]
            [apriori.util.logger :as logger]
            [apriori.util.domain.response :refer :all]
            [apriori.util.domain.repository :refer :all]))

;; ***** Meta implementation ********************************************************

(def ^:private server-start-time (clj-time.coerce/to-long (time/now)))

(defn- build-time []
  (try
    (format/parse (format/formatters :date-time-no-ms) (env :build-time))
    (catch Exception e nil (logger/log :warn e))))

(defn- up-time []
  (let [current-time (clj-time.coerce/to-long (time/now))]
    (float (/ (- current-time server-start-time) 1000))))

(defn- db-latency []
  (let [now (clj-time.coerce/to-long (time/now))
        db-status (try
                    (do
                      ;
                      :ok)
                    (catch Exception e
                      (logger/log :warn e) :error))]

    [db-status (float (/ (- (clj-time.coerce/to-long (time/now)) now) 1000))]))

(defn- state [db-status db-latency]
  (if (or (= db-status :error) (> db-latency 0.1)) "unhealthy" "ok"))

(defn- status-code [state]
  (if (= state "unhealthy") 503 200))

(defn- get-health-check []
  "Retrieve app health

  e.g. (get-health-check)"

  (logger/log :debug {:meta #'get-health-check :arg-values {}})

  (let [[db-status db-latency] (db-latency)]
    (Status
      (->ApiResponse {:status  (state db-status db-latency)
                      :version (env :version)
                      :built   (when-not (nil? (build-time)) (format/unparse (format/formatters :date-time-no-ms) (build-time)))
                      :uptime  (up-time)
                      :ghash   (env :ghash)}
                     {:database {:latency db-latency}})
      (status-code (state db-status db-latency)))))

;; ***** Meta definition *******************************************************

(def meta-routes
  (GET "/healthcheck" []
    :tags ["meta"]
    :middleware [#(wrap-cors % :access-control-allow-origin [#".*"]
                             :access-control-allow-methods [:get])]
    :summary "Reports app health."
    :responses {200 {:schema      {:meta Meta :data {:database {:latency schema/Num}}}
                     :description "ok"}
                503 {:description "service unavailable"}} (get-health-check)))