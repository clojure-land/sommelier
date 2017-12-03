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
  ;(prn (for [row body]
  ;       ;(prn (type row)))) ;clojure.lang.PersistentVector
  ;       (->>
  ;         (filter vector row)
  ;         (map (fn [d] {:data (generate-string d) :created (quot (System/currentTimeMillis) 1000)}))))))

  ;{:data (generate-string row) :created (quot (System/currentTimeMillis) 1000)} )))

  ;(insert-multi! db :data_set
  ;               (for [row body] {:data (generate-string row) :created (quot (System/currentTimeMillis) 1000)})))



  ;["a" "b"]["c" "d"]["d" "e" "f"]["a" "f"]

  (for [row [;["b" "m"]
             ["b" "d" "bb" "e"]]]
             ;["m" "d" "bb" "c"]
             ;["b" "m" "d" "bb"]
             ;["b" "m" "d" "c"]]]
    (insert! db :data_set {:data (generate-string row) :created (quot (System/currentTimeMillis) 1000)})))



;  (for [row
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
;[["bread" "milk"]
; ["bread" "diaper" "beer" "eggs"]
; ["milk" "diaper" "beer" "coke"]
; ["bread" "milk" "diaper" "beer"]
; ["bread" "milk" "diaper" "coke"]]]




;(let [insert (insert! db :data_set {:data (generate-string row) :created (quot (System/currentTimeMillis) 1000)})
;
;      insert-multi! db :data_set
;      [:name :cost]
;      [["Mango" 722]
;       ["Feijoa" 441]]
;
;      ]
;  (status (response {:meta {:status "ok"}}) 200))))

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