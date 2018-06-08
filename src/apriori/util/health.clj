(ns apriori.util.health
  (:require [ring.util.response :refer :all]
            [clojure.java.jdbc :refer :all]
            [apriori.util.storage :refer :all]
            [clj-time.core :as time]
            [clj-time.coerce :as coerce]
            [clj-time.format :as format]
            [environ.core :refer [env]]))

(def server-start-time (clj-time.coerce/to-long (time/now)))

(defn get-version
  "Get version"
  []
  (env :version))

(defn get-build-time
  "Get build time"
  []
  (if (not= (env :build-time) nil)
    (format/parse (format/formatter "yyyyMMdd HHmmssZ") (env :build-time)) nil))

(defn get-up-time
  "Get up time"
  []
  (let [current-time (clj-time.coerce/to-long (time/now))]
    (float (/ (- current-time server-start-time) 1000))))

(defn get-ghash
  "Get ghash"
  []
  (env :ghash))

(defn get-db-latency
  "Get db latency"
  []
  (let [now (clj-time.coerce/to-long (time/now))]
    (let [result (query db ["select COUNT(*) AS count from apriori.project LIMIT 1"])]
      (float (/ (- (clj-time.coerce/to-long (time/now)) now) 1000)))))