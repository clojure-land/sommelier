(ns apriori.utlis.apriori
  (:require [ring.util.response :refer :all]
            [clojure.java.jdbc :refer :all]
            [apriori.utlis.storage :refer :all]
            [apriori.utlis.data-set :refer :all]
            [cheshire.core :refer :all]
            [clojure.math.combinatorics :as combo]
            [clj-time.core :as time]
            [clj-time.coerce :as coerce]
            [clj-time.format :as format]))

; generate association rules
(defn generate []
  (let [time (clj-time.coerce/to-long (time/now))

        project ()

        ; a collection of related items.
        data-set (get-data-set "data_set")

        ; total number of items in the data set.
        total-items (count data-set)

        ; collection of one or more items.
        item-set (->>
                   (as-> () sets
                         (for [row data-set]
                           (->>
                             (parse-string (:data row))
                             (combo/subsets)
                             (conj sets))))
                   (apply concat)
                   (apply concat))

        ;todo binary partitions?

        ; occurrences of item set.
        frequencies (frequencies item-set)
                      ;(filter (fn [n] (not-empty (first n))))
                      ;(map (fn [s] [(first s) (last s)]))
                      ;(into {}))

        ; percentage of transaction containing item -> frequencies(a,b) / total transaction.
        support (->>
                  (as-> () s
                        (for [key (keys frequencies)]
                          (->>
                            (/ (get frequencies key) total-items)
                            (float)
                            (list)
                            (map (fn [n] [key n]))
                            (conj s))))
                  (apply concat)
                  (apply concat)
                  (map (fn [s] [(first s) (last s)]))
                  (into {}))

        ; percentage of b appears in transactions containing a -> frequencies(a,b) / frequencies(a) ).
        confidence (->>
                     (as-> () c
                           (for [key (keys frequencies)]
                             (->>
                               (/ (get frequencies key) (get frequencies (drop-last key)))
                               (float)
                               (list)
                               (map (fn [n] [key n]))
                               (conj c))))
                     (apply concat)
                     (apply concat)
                     (map (fn [s] [(first s) (last s)]))
                     (into {}))

        ; indicate how infesting the association is -> confidence(a,b) / support(b).
        ;lift (->>
        ;       (as-> () l
        ;             (for [key (keys frequencies)]
        ;               (->>
        ;                 (/ (get confidence key) (get support [(last key)]))
        ;                 (float)
        ;                 (list)
        ;                 (map (fn [n] [key n]))
        ;                 (conj l))))
        ;       (apply concat)
        ;       (apply concat)
        ;       (map (fn [s] [(first s) (last s)]))
        ;       (into {}))
        ]

    ;todo pruning
    ;todo discard all single item sets from support, confidence & lift as it does not apply to them

    (for [key (keys frequencies)]
      (prn (insert! db :frequency_item_set {:item       (generate-string key)
                                            :frequency  (get frequencies key)
                                            :support    (format "%.2f" (get support key))
                                            :confidence (format "%.2f" (get confidence key))
                                            :lift       "" ;(format "%.2f" (get lift key))
                                            :created    (quot (System/currentTimeMillis) 1000)})))

    ;(prn (str "execution time: " (float (/ (- (clj-time.coerce/to-long (time/now)) time) 1000)) "s"))

    ))
