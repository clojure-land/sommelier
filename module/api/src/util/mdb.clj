(ns util.mdb
  (:refer-clojure :exclude [sort find])
  (:require [domain.association :as association]
            [monger.core :as mg]
            [monger.collection :as mc]
            [monger.result :refer [acknowledged?]]
            [monger.query :refer :all])
  (:import [org.bson.types ObjectId]))

(def conn
  (mg/connect {:host "mongo" :port 27017}))

(defn objectId? [id]
  "Verify that id is an objectId."

  (try
    (do
      (ObjectId. (str id)) true)
    (catch Exception ex false)))

;; ***** Permissions collection ********************************************************
;todo: change value to resource.
(defn get-permissions
  "Retrieves permissions from mdb."
  [sub type]

  (let [db (mg/get-db conn "sommelier")]
    (mc/find-maps db "permissions" {:sub sub :type type})))

(defn insert-permission
  "Inserts permission into mdb."
  [sub type resource]

  (let [db (mg/get-db conn "sommelier")]
    (acknowledged? (mc/insert db "permissions" {:sub sub :type type :resource resource}))))

(defn delete-permission [id])

;; ***** Project collection ********************************************************

(defn get-projects
  "Retrieves projects from mdb."
  [ref]

  (let [db (mg/get-db conn "sommelier")]
    (mc/find-maps db "project" ref)))

(defn get-project-if-exists
  "Retrieves project, but if it does not exist then returns false."
  [id]

  (if (objectId? id)
    (let [project (get-projects {:_id (ObjectId. (str id))})]
      (if (not= project []) project false)) false))

(defn save-project
  "Inserts or updates a project in mdb."
  [^org.bson.types.ObjectId id document]

  (let [db (mg/get-db conn "sommelier")]
    (if (some? id)
      (do
        (mc/update-by-id db "project" (ObjectId. (str id)) document)
        (get-projects {:_id (ObjectId. (str id))}))
      (vector (mc/insert-and-return db "project" document)))))

(defn remove-project
  "Permanently removes a project from mdb."
  [^org.bson.types.ObjectId id]

  (let [conn (mg/connect {:host "mongo" :port 27017})
        db (mg/get-db conn "sommelier")]
    (mc/remove-by-id db "project" id)))

;; ***** Job collection ********************************************************

(defn get-jobs
  "Retrieves jobs from mdb."
  [ref]

  (let [db (mg/get-db conn "sommelier")]
    (mc/find-maps db "job" ref)))

(defn save-job
  "Inserts or updates a job."
  [^org.bson.types.ObjectId id document]

  (let [db   (mg/get-db conn "sommelier")]
    (if (some? id)
      (do
        (mc/update-by-id db "job" (ObjectId. (str id)) document)
        (get-jobs {:_id (ObjectId. (str id))}))
      (mc/insert-and-return db "job" document))))

;; ***** Transactions collection ********************************************************

(defn get-transactions
  "Retrieves transactions from mdb."
  [job-id ref]

  (let [db   (mg/get-db conn (str "job_" job-id))]
    (mc/find-maps db "transactions" ref)))

(defn save-transactions
  "Inserts or updates transactions in mdb."
  [job-id conditions document]

  (let [db (mg/get-db conn (str "job_" job-id))]
    (if (some? conditions)
      (mc/update db "transactions" conditions document {:multi true})
      (mc/insert-batch db "transactions" document))))

;; ***** Associations collection ********************************************************

(defn get-association-rules
  "Retrieves associations from mdb."
  [job-id page sort-by order ref]

  (let [db (mg/get-db conn (str "job_" job-id))]

    (with-collection db "rules"
       (find ref)
       (fields (conj (keys association/AssociationSchema) :_id))
       (paginate :page page :per-page 10)
       (sort (array-map sort-by order)))))
