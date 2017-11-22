(ns apriori.utlis.data-set
  (:require [clojure.java.jdbc :refer :all]
            [apriori.utlis.storage :refer :all]))

; get data set
(defn get-data-set [data-set-table-name]
  (query db [(str "select * from " data-set-table-name)]))