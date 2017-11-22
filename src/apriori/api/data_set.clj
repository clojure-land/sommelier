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
  (let [data (as-> {} d
                   (assoc d :a (if (empty? (get body "a")) "" (get body "a")))
                   (assoc d :b (if (empty? (get body "b")) "" (get body "b")))
                   (assoc d :c (if (empty? (get body "c")) "" (get body "c")))
                   (assoc d :d (if (empty? (get body "d")) "" (get body "d"))))
        insert (insert! db :data_set {:data (generate-string body) :created (quot (System/currentTimeMillis) 1000)})]
    (status (response {:meta {:status "ok"}}) 200)))



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



  ; "product_name" {"a"}                                  1
  ; "product_name" {"a", "b"}                             1
  ; "product_name" {"a", "b", "c"}                        2
  ; "product_name" {"a"} "product_color" {"b"}            1
  ; "product_name" {"a", "b"} "product_color" {"b", "b"}  4
  ; "product_name" {"a"} , "product_color" {"b"} -> "product_name" {"c"} (60%)

  ;([
  ;  ({
  ;    "product_name" ["a" "b" "c" "d"],
  ;    "product_color" ["b" "b" "b" "r"],
  ;    "product_size" ["s" "s" "s" "s"]}) 2]
  ;  [
  ;   ({"product_name" ["a" "b" "c" "d"],
  ;     "product_color" ["b" "g" "g" "r"],
  ;     "product_size" ["s" "xs" "l" "s"]}) 1]
  ;  [
  ;   ({"product_name" ["a" "b" "c" "d"],
  ;     "product_color" ["b" "g" "r" "r"],
  ;     "product_size" ["s" "xs" "l" "s"]}) 1]
  ;  [
  ;   ({"product_name" ["a" "b" "c" "d"],
  ;     "product_color" ["b" "b" "b" "r"],
  ;     "product_size" ["s" "xs" "l" "s"]}) 1])



  ;(let [result (query db ["select * from data_set"])
  ;      sets (as-> {} i
  ;                 (for [row result]
  ;                   (for [item (parse-string (:data row) true)]
  ;                     (prn item)
  ;                     )))]))

  ; BUILD
  ;{
  ;  "id": "40dd3e7f-e187-4117-a3dd-54d8e868aec7",
  ;  "name": "test111",
  ;  "support": 0,
  ;  "confidence": 0,
  ;  "schema" : {"product_price", "product_color", "product_size"}
  ;  "modified": 1511193918
  ;}

  ; GET
  ; product_price | product_color | product_size
  ; 2, 3, 2       | b, g, r       | xs, s, xs

  ; POST
  ; {
  ;   "product_price" => [2,3,2],
  ;   "product_color" => ["b","g","r"]
  ;   "product_size"  => ["xs","s","xs"]
  ; }

  ; {"product_price":3} => {"product_color":"g"}, {"product_size":"s"} (100%)
  ; [
  ;   {"product_price":3}
  ;   {"product_price":2}
  ;   {"product_price":2}
  ; ]