(ns apriori.util.apriori
  (:require [ring.util.response :refer :all]
            ;[clojure.java.jdbc :refer :all]
            [cheshire.core :refer :all]
            [clojure.math.combinatorics :as combo]
            [clj-time.core :as time]
            [clj-time.coerce :as coerce]
            [clj-time.format :as format]))

; generate association rules
(defn run [data-set]
  (let [time (clj-time.coerce/to-long (time/now))

        ; collection of one or more items.
        item-set (->>
                   (for [row data-set]
                     (->>
                       (combo/subsets row)
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

        ; percentage of transaction containing item -> frequencies.clj(a,b) / total transaction.
        support (->>
                  (for [key (keys frequencies)]
                    {key (float (/ (get frequencies key) total-items))})
                  (into {}))

        ;todo pruning with reduce to remove all infrequent data sets

        ; percentage of b appearing in transactions containing a -> frequencies.clj(a,b) / frequencies.clj(a) ).
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

    ;(newline)
    ;(prn "item set:")
    ;(doseq [row item-set]
    ;  (prn row))

    (newline)
    (prn "assoc rules:")
    (doseq [assoc (sort-by :lift
                           (for [key (keys frequencies)]
                             (if (get confidence key)
                               (if (> (get support key) 0.1)
                                 (if (> (get confidence key) 0.2)
                                   {:transaction key
                                    :frequency   (get frequencies key)
                                    :support     (get support key)
                                    :confidence  (get confidence key)
                                    :lift        (get lift key)
                                    :association (if (< (get lift key) 1) "negative" "positive")
                                    })))))]

      (if (not (nil? assoc))
        ; Support - is an indication of how frequently the item set appears in the data set.
        ; Confidence - is an indication of how often the rule has been found to be true.
        ; Lift - is this association rule interesting.

        (->
          (update-in assoc [:support] (fn [x] (format "%.2f" x)))
          (update-in [:confidence] (fn [x] (format "%.2f" x)))
          (update-in [:lift] (fn [x] (format "%.2f" x)))
          (prn))))))
