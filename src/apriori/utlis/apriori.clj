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
(defn run [project-id]
  (let [time (clj-time.coerce/to-long (time/now))

        project project-id

        ; a collection of related items.
        data-set (get-data-set "data_set")

        ; collection of one or more items.
        item-set (->>
                   (for [row data-set]
                     (->>
                       (parse-string (:data row))
                       (combo/subsets)
                       (map (fn [n] (combo/partitions n :max 2)))))
                   (apply concat)
                   (apply concat))

        ; total number of items in the data set.
        total-items (count data-set)

        ; occurrences of item set.
        frequencies (->>
                      (frequencies item-set)
                      (filter (fn [n] (not-empty (first n))))
                      (into {}))

        ; percentage of transaction containing item -> frequencies(a,b) / total transaction.
        support (->>
                  (for [key (keys frequencies)]
                    {key (float (/ (get frequencies key) total-items))})
                  (into {}))

        ;todo pruning with reduce to remove all infrequent data sets

        ; percentage of b appearing in transactions containing a -> frequencies(a,b) / frequencies(a) ).
        confidence (->>
                     (for [key (keys frequencies)]
                       (if (> (count key) 1)
                         {key (float (/ (get frequencies key) (get frequencies (drop-last key))))}
                         {key nil}))
                     (into {}))

        ; indicate how interesting the association is -> confidence(a,b) / support(b).
        lift (->>
               (for [key (keys frequencies)]
                 (if (> (count key) 1)
                   {key (float (/ (get confidence key) (get support [(last key)])))}
                   {key nil}))
               (into {}))]

    ;todo discard all single item sets from support, confidence & lift as it does not apply to them

    (for [key (keys frequencies)]
      (prn {:item       key
            :frequency  (get frequencies key)
            :support    (format "%.2f" (get support key))
            :confidence (format "%.2f" (get confidence key))
            :lift       (format "%.2f" (get lift key))
            :created    (quot (System/currentTimeMillis) 1000)}))

    ;  (prn (insert! db :frequency_item_set {:item       (generate-string key)
    ;                                        :frequency  (get frequencies key)
    ;                                        :support    (format "%.2f" (get support key))
    ;                                        :confidence (format "%.2f" (get confidence key))
    ;                                        :lift       "" ;(format "%.2f" (get lift key))
    ;                                        :created    (quot (System/currentTimeMillis) 1000)})))

    ;(prn (str "execution time: " (float (/ (- (clj-time.coerce/to-long (time/now)) time) 1000)) "s"))

    ))
