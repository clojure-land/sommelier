(ns apriori.utlis.freq_item_set
  (:require [ring.util.response :refer :all]
            [clojure.java.jdbc :refer :all]
            [apriori.utlis.storage :refer :all]
            [apriori.utlis.data-set :refer :all]
            [cheshire.core :refer :all]
            [clojure.math.combinatorics :as combo]
            [clj-time.core :as time]
            [clj-time.coerce :as coerce]
            [clj-time.format :as format]))

; item set element
(defrecord ItemSetElement [key])

; make item set element
(defn make-item-set-element [key]
  (->ItemSetElement key))

; unwrap item set element
(defn unwrap-item-set-element [element]
  (:key element))

; wrap item set element
(defn wrap-item-set-element [key element]
  (with-meta (make-item-set-element element) {::key key}))

; prepare the data set
(defn prepare-data-set [data-set]
  (mapcat
    (fn [[key ss]]
      (map (partial wrap-item-set-element key) ss))
    data-set))

; reconstruct item sets
(defn reconstruct-item-sets [subset]
  (->> subset
       (group-by #(::key (meta %)))
       (map (fn [[key elements]]
              [key (vec (map unwrap-item-set-element elements))] ))
       (into {})))

; generate frequency item set
(defn generate-freq-item-set []
  (let [result (get-data-set "data_set")

        item-set (as-> () items
                       (for [row result]
                         (conj items (->> (parse-string (:data row))
                                          prepare-data-set
                                          combo/subsets
                                          (map reconstruct-item-sets)))))

        frequencies (->> (apply concat (apply concat item-set))
                         (frequencies))]

    (for [row frequencies]
      (insert! db :frequency_item_set {:item (first row) :frequency (second row) :created (quot (System/currentTimeMillis) 1000)}))))