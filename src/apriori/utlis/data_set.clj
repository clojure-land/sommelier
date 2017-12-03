(ns apriori.utlis.data-set
  (:require [clojure.java.jdbc :as jdbc]
            [cheshire.core :as cheshire]
            [apriori.utlis.storage :as storage]))

; get data set
(defn get-data-set [data-set-table-name]
  (jdbc/query storage/db [(str "select * from " data-set-table-name)]))

; upload
(defn upload [path]
  (for [row [["b" "m"]
            ["b" "d" "bb" "e"]
            ["m" "d" "bb" "c"]
            ["b" "m" "d" "bb"]
            ["b" "m" "d" "c"]]]
    (jdbc/insert! storage/db :data_set {:data (cheshire/generate-string row) :created (quot (System/currentTimeMillis) 1000)})))