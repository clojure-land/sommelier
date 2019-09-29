(ns routes.transactions
  (:require [compojure.api.sweet :refer :all]
            [util.response :refer :all]
            [util.auth :refer :all]
            [util.mdb :as mdb]
            [domain.response :refer :all]
            [domain.project :refer :all]
            [domain.transactions :refer :all]
            [domain.job :refer :all]
            [monger.json]
            [clojure.core.matrix :refer :all]
            [clojure.math.combinatorics :as combo])
  (:import [org.bson.types ObjectId]))

;; ***** Transactions implementation ********************************************************

;(defn- random-transaction [x]
;  (let [transaction (take
;                      (rand-int x)
;                      ((partial shuffle ["a" "b" "p" "m" "r" "k" "o" "t" "pp" "m" "wm"])))]
;
;    (if (empty? transaction)
;      (random-transaction x)
;      (clojure.string/join "," transaction))))

(defn- matrix-concat
  "Concatenates each coll in matrix.

  e.g. (matrix-concat [[1 2 3] [4 5 6]])"
  [matrix]

  (if (matrix? matrix)
    (map (fn [coll] (clojure.string/join "," coll)) matrix) []))

(defn- matrix-subsets
  "Returns subsets of each coll in matrix.

   e.g. (matrix-subsets [[1 2 3] [4 5 6]])"
  [matrix]

  (if (matrix? matrix)
    (apply concat (map (fn [coll] (combo/subsets coll)) matrix)) []))

(defn- group-by-operation
  "Groups transaction-freq by insert or update operation."
  [job-id transaction-freq]

  (let [transactions (mdb/get-transactions job-id {:transaction {"$in" (keys transaction-freq)}})]
    (reduce (fn [group freq]
              (let [transaction (first (filter #(= (get % :transaction) (key freq)) transactions))]
                (if (empty? transaction)
                  (assoc-in group [:insert (key freq)] (val freq))
                  (assoc-in group [:update (get transaction :_id)] (val freq))))) {:insert {} :update {}} transaction-freq)))

(defn- get-scheduled-job
  "Retrieves the scheduled job, if none exists then creates a new one."
  [project-id]

  (let [job (mdb/get-job {:project-id (ObjectId. (str project-id)) :state "scheduled"})]
    (if (= job [])
      (mdb/save-job nil {:project-id (ObjectId. (str project-id)) :state "scheduled" :transactions 0}) job)))

(defn- save-transactions-count
  [job-id transactions-count]

  (mdb/save-job job-id {"$inc" {:transactions transactions-count}}))

(defn- ids
  [transactions]
  (reduce (fn [ids update]
            (assoc ids (val update) (conj (get ids (val update)) (key update)) )) {} transactions))

(defn- save-transactions-freq
  "Inserts or updates frequency of transactions."
  [project-id author body]

  (if (and (mdb/objectId? project-id)
           (not= (mdb/get-project {:_id (ObjectId. (str project-id)) :author author}) []))

    (let [job (first (get-scheduled-job project-id))
          transactions (->>
                         (get body :transactions)
                         (matrix-subsets)
                         (matrix-concat)
                         (frequencies)
                         (group-by-operation (get job :_id)))]

      (do
        (if (not (empty? (get transactions :insert)))
          (mdb/save-transactions (get job :_id) nil (map (fn [row] {:transaction (key row) :count (val row)}) (get transactions :insert))))

        (if (not (empty? (get transactions :update)))
          (doseq [row (ids (get transactions :update))]
            (mdb/save-transactions (get job :_id) {:_id {"$in" (val row)}} {"$inc" {:count (key row)}})))

        (api-response (->ApiData {:status "accepted"}
                                 ;(->
                                 ;  (update job :transactions + (count (get body :transactions)))
                                 ;  (vector)
                                 ;  (job->resource-object))
                                 (job->resource-object (save-transactions-count (get job :_id) (count (get body :transactions))))
                                 )
                      202 [{:content-location (str "/v1/job/" (get job :_id))}])))
  (project-not-found project-id)))

;; ***** Transactions definition *******************************************************

(def transactions-routes
  (POST "/:id/transactions" []
    :path-params [id :- ProjectId]
    :tags ["project"]
    :operationId "appendTransactions"
    :summary "Appends transactions for processing."
    :body [transactions TransactionsSchema]
    ;:middleware [#(util.auth/auth! %)]
    ;:current-user profile
    :responses {202 {:schema {:meta Meta :data (vector (assoc DataObject :attributes JobSchema))}
                     :description "accepted"}
                400 {:schema {:meta Meta :errors [ErrorObject]}
                     :description "bad request"}
                403 {:schema {:meta Meta :errors [ErrorObject]}
                     :description "unauthorized"}
                404 {:schema {:meta Meta :errors [ErrorObject]}
                     :description "not found"}} (save-transactions-freq id "" transactions)))