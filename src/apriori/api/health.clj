(ns apriori.api.health
  (:require [ring.util.response :refer :all]
            [clojure.java.jdbc :refer :all]
            [apriori.utlis.storage :refer :all]
            [clj-time.core :as time]
            [clj-time.coerce :as coerce]
            [clj-time.format :as format]
            [environ.core :refer [env]]))

; server start time
(def server-start-time (clj-time.coerce/to-long (time/now)))

; get version
(defn get-version []
  (env :version))

; get build time
(defn get-build-time []
  (if (not= (env :build-time) nil)
    (format/parse (format/formatter "yyyyMMdd HHmmssZ") (env :build-time)) nil))

; get up time
(defn get-up-time []
  (let [current-time (clj-time.coerce/to-long (time/now))]
    (float (/ (- current-time server-start-time) 1000))))

; get ghash
(defn get-ghash []
  (env :ghash))

; get db latency
(defn get-db-latency []
  (let [now (clj-time.coerce/to-long (time/now))]
    (let [result (query db ["select COUNT(*) AS count from project LIMIT 1"])]
      (float (/ (- (clj-time.coerce/to-long (time/now)) now) 1000)))))

; get health
(defn get-health []
  (let [version (get-version)
        build-time (as-> "" b
                       (if (not= (env :build-time) nil)
                         (format/unparse (format/formatter "yyyy-MM-dd HH:mm:ssZ") (get-build-time)) nil))
        uptime (get-up-time)
        ghash (get-ghash)
        database-latency (get-db-latency)
        status (as-> "" s
                     (if (> database-latency 0.1) "unhealthy" "ok"))]
    (response {:meta {:status status :version version :built build-time :uptime uptime :ghash ghash}
               :data {:database {:latency database-latency}}})))