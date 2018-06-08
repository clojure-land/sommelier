(ns apriori.api.routes.meta
  (:require [compojure.api.sweet :refer :all]
            [compojure.api.exception :as ex]
            [compojure.route :as route]
            [clojure.walk :as walk]
            [clj-time.local :as local]
            [clj-time.format :as format]
            [cheshire.core :as json]
            [schema.core :as schema]
            [environ.core :refer [env]]
            [ring.util.response :as ring]
            [apriori.util.logger :as logger]
            [apriori.util.response :as response]
            [apriori.util.health :as health]
            [apriori.domain.api :as api]))

;; ***** Meta implementation ********************************************************

(defn get-health-check []
  (let [version (health/get-version)
        build-time (as-> "" b
                         (if (not= (env :build-time) nil)
                           (format/unparse (format/formatter "yyyy-MM-dd HH:mm:ssZ") (health/get-build-time)) nil))
        uptime (health/get-up-time)
        ghash (health/get-ghash)
        database-latency (health/get-db-latency)
        status (as-> "" s
                     (if (> database-latency 0.1) "unhealthy" "ok"))]
    (response/ok {:status status :version version :built build-time :uptime uptime :ghash ghash}
                 {:database {:latency database-latency}})))

;; ***** API definition *******************************************************

(def meta-routes
  (api
    (GET "/healthcheck" [] :tags ["meta"]
                         :responses {200 {:schema      {:meta api/Meta :data {:database {:latency schema/Num}}}
                                          :description "ok"}} (get-health-check))))