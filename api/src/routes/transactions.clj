(ns routes.transactions
  (:require [compojure.api.sweet :refer :all]
            [util.response :refer :all]
            [util.auth :refer :all]
            [util.mdb :as mdb]
            [domain.response :refer :all]
            [domain.project :refer :all]
            [domain.transactions :refer :all]
            [monger.json]
            [clojure.math.combinatorics :as combo])
  (:import [org.bson.types ObjectId]))

;; ***** Transactions implementation ********************************************************

(defn- random-transaction [x]
  (let [transaction (take
                      (rand-int x)
                      ((partial shuffle ["a" "b" "p" "m" "r", "k", "o", "t", "pp", "m", "wm"])))]

    (if (empty? transaction)
      (random-transaction x)
      (clojure.string/join "," transaction))))

(defn- coll-concat
  "Returns a string foreach item in the collection."
  [coll]

  (map #(clojure.string/join "," %) coll))

(defn- coll-with-subsets
  "Returns subsets foreach item in collection."
  [coll]

  (apply concat (map (fn [items]

                       (filter #(> (count %) 1) (combo/subsets items))

                          ;check if already exists
                          ;{(coll-concat items)}
                          ;(prn subsets)
                          ;(clojure.string/join "," items)
                          ;(assoc subsets (coll-concat items) (filter #(> (count %) 1) (combo/subsets items)))

                       ) {} coll)))

(defn- group-transactions
  "Groups transactions into insert or update."
  [id new-transactions]

  (let [old-transactions (mdb/get-transactions id {:transaction {"$in" new-transactions}})]
    (reduce (fn [return new-transaction]
              (let [old-transaction (first (filter #(= (get % :transaction) new-transaction) old-transactions))]
                (if (empty? old-transaction)
                  (if (get-in return [:insert new-transaction])
                    (update-in return [:insert new-transaction] inc)
                    (assoc-in return [:insert new-transaction] 1))
                  (if (get-in return [:update (get old-transaction :_id)])
                    (update-in return [:update (get old-transaction :_id)] inc)
                    (assoc-in return [:update (get old-transaction :_id)] 1)))))
            {:insert {} :update {}} new-transactions)))

(defn- save-transactions-frequency
  "Inserts or updates transactions frequency."
  [id author body]

  (if (and (mdb/objectId? id)
           (not= (mdb/get-project {:_id (ObjectId. (str id)) :author author}) []))

    (let [stmt (->>
                 (get body :transactions)
                 (coll-with-subsets)
                 ;(coll-concat)
                 ;(group-transactions id)
                 (prn)
                 )]

        (if (not (empty? (get stmt :insert)))
          (mdb/insert-transactions id (map (fn [row] {:transaction (key row) :count (val row)}) (get stmt :insert))))

        (if (not (empty? (get stmt :update)))
          (doseq [row (get stmt :update)]
            (mdb/update-transactions id {:_id (key row)} {"$inc" {:count (val row)}})))

    (api-response (->ApiData {:status "accepted"} [{:type "job" :id "" :attributes {:status "scheduled"}}]) 202 [{:content-location (str "/project/" id "/job")}]))
  (project-not-found id)))

;"a","b","c","d","e","f","g","h","i","j","k","l","m","n","o","p","q","r","s","t","u","v","w","x","y","z"
;"a","b","c","d","e","f","g","h","i","j","k","l"

;; ***** Transactions definition *******************************************************

(def transactions-routes
  (context "/transactions" []
    ;(GET "/job/:id" []
    ;  :path-params [id :- ProjectId]
    ;
    ;  (api-response (->ApiData {:status "ok"} (mdb/count-transactions id)) 200 []))
    ;
    ;(POST "/job/:id")

    (POST "/:id" []
      :path-params [id :- ProjectId]
      :tags ["transactions"]
      :operationId "appendTransactions"
      :summary "Appends transactions to scheduled job."
      :body [transactions TransactionsSchema]
      ;:middleware [#(util.auth/auth! %)]
      ;:current-user profile
      :responses {202 {;:schema {:meta Meta ::data [{:type "job" :attributes {:status "scheduled,running,done,failed"}}]
                       ;content-location = /project/1/job
                       :description "accepted"}
                  400 {:schema {:meta Meta :errors [ErrorObject]}
                       :description "bad request"}
                  403 {:schema {:meta Meta :errors [ErrorObject]}
                       :description "unauthorized"}
                  404 {:schema {:meta Meta :errors [ErrorObject]}
                       :description "not found"}} (save-transactions-frequency id "" transactions))))

; POST - /transactions/project/:id

; GET - /project/:id/jobs
; GET - /jobs/project/:id
; GET - /job/:id

; POST - /transactions/job/:id -> 200,404 ir 410
; GET  - /transactions/job/:id -> 200

; GET - /associations/project/:id?q=...
; GET - /associations/job/:id?q=...
