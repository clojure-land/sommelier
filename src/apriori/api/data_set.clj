(ns apriori.api.data_set
  (:require [ring.util.response :refer :all]
            [clojure.java.jdbc :refer :all]
            [clojure.math.combinatorics :as combo]
            [apriori.utlis.storage :refer :all]
            [cheshire.core :refer :all]
            [clj-time.core :as time]
            [clj-time.coerce :as coerce]
            [clj-time.format :as format]
            [environ.core :refer [env]]))

(defn add-data [body]

  (for [row
            ;[["beer"]
            ; ["beer" "cheese"]
            ; ["banana" "beer" "cheese" "nuts"]
            ; ["beer" "nuts"]
            ; ["beer" "cheese" "nuts"]
            ; ["banana" "cheese" "nuts"]
            ; ["beer" "cheese"]
            ; ["banana" "beer" "cheese" "nuts"]
            ; ["beer" "cheese" "nuts"]
            ; ["banana" "beer" "cheese" "nuts"]]]
            [["bread" "milk"]
             ["bread" "diaper" "beer" "eggs"]
             ["milk" "diaper" "beer" "coke"]
             ["bread" "milk" "diaper" "beer"]
             ["bread" "milk" "diaper" "coke"]]]

    ;["bread" "diaper" "beer" -> "eggs"]
    ;["eggs" -> "bread" "diaper" "beer"]



    ;; Note that partitions intelligently handles duplicate items
    ;=> (combo/partitions [1 1 2])
    ;(([1 1 2])
    ;  ([1 1] [2])
    ;  ([1 2] [1])
    ;  ([1] [1] [2]))
    ;
    ;; You can also specify a min and max number of partitions
    ;(combo/partitions [1 1 2 2] :min 2 :max 3)
    ;(([1 1 2] [2])
    ;  ([1 1] [2 2])
    ;  ([1 1] [2] [2])
    ;  ([1 2 2] [1])
    ;  ([1 2] [1 2])
    ;  ([1 2] [1] [2])
    ;  ([1] [1] [2 2]))

    (insert! db :data_set {:data (generate-string row) :created (quot (System/currentTimeMillis) 1000)})))


  ;(let [data {}
  ;      insert (insert! db :data_set {:data (generate-string body) :created (quot (System/currentTimeMillis) 1000)})]


    ;(status (response {:meta {:status "ok"}}) 200)))



  ;(reduce + [1 2 3 4 5])
  ;(reduce - [1 2 3 4 5])
  ;(reduce (fn [a b] (+ a b)) [1 2 3 4 5])
  ;(reduce (fn [a b] ({a: (+ a) b: (+ b) c: (+ c) }) [{a:1 b:2 c:3},{a:4 b:5 c:6},{a:7 b:8 c:9}])
  ;(reduce (fn [a b] {:a (+ (:a a) (:a b)) :b (+ (:b a) (:b b)) :c (+ (:c a) (:c b))}) [{:a 1 :b 2 :c 3},{:a 4 :b 5 :c 6},{:a 7 :b 8 :c 9}])
  ;(->> [{:a 1 :b 2 :c 3}, {:a 4 :b 5 :c 6}, {:a 7 :b 8 :c 9}]
  ;     (reduce (fn [a b] {:a (+ (:a a) (:a b)) :b (+ (:b a) (:b b)) :c (+ (:c a) (:c b))})))
  ;(->> [{:a 1 :b 2 :c 3}, {:a 4 :b 5 :c 6}, {:a 7 :b 8 :c 9}]
  ;     #(filter even? (:a %))
  ;     (reduce (fn [a b] {:a (+ (:a a))})))
  ;(reduce (fn [a b] (conj (frequencies a) (frequencies b))n))
  ;(prn (combo/subsets [{:product_id "a" "b" "c"}]))
  ;frequencies (sort-by last >
  ;                     (->> (for [row result] (
  ;                                              (prn (parse-string (:data row)))
  ;                                              ;(parse-string (:data row))
  ;                                              ))
  ;                          ;(frequencies)
  ;
  ;                          ))
  ;]
  ;(query db ["select data from data_set"])

  ;{
  ; :item-set-1 ["a", "b", "c"]
  ; :item-set-2 ["d", "e", "f"]
  ; }
  ;
  ;{
  ; [:item-set-1 ["a"]]
  ; [:item-set-1 ["a" "b"]]
  ; [:item-set-1 ["a" "b" "c"]]
  ; [:item-set-2 ["d"]]
  ; [:item-set-2 ["d" "e"]]
  ; [:item-set-2 ["d" "e" "f"]]
  ;
  ; [:item-set-1 ["a"] [:item-set-2 ["d"]]]
  ; [:item-set-1 ["b"] [:item-set-2 ["e"]]]
  ; [:item-set-1 ["c"] [:item-set-2 ["f"]]]
  ;
  ; [:item-set-1 ["a" "b"] [:item-set-2 ["d" "e"]]]
  ; [:item-set-1 ["a" "b"] [:item-set-2 ["e" "f"]]]
  ; [:item-set-1 ["b" "c"] [:item-set-2 ["d" "e"]]]
  ; [:item-set-1 ["b" "c"] [:item-set-2 ["e" "f"]]]
  ;
  ; [:item-set-1 ["a" "b" "c"] [:item-set-2 ["d" "e" "f"]]]
  ; }


  ;(let [result (query db ["select data from data_set"])
  ;      items (as-> () i
  ;                  (for [row result]
  ;                    ;(for [item (parse-string (:data row))]
  ;                    ;  (prn item)
  ;
  ;                      )))]))


  ;(for [row result]
  ;  (for [item (parse-string (:data row))]
  ;
  ;    (prn (->> #{1 2 3}
  ;         (combinatorics/subsets)
  ;         (remove empty?)
  ;         (map set)
  ;         (set)))
  ;
  ;
  ;    ))