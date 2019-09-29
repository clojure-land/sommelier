(ns util.mdb
  (:require [monger.core :as mg]
            [monger.collection :as mc])
  (:import [org.bson.types ObjectId]))

(def conn
  (mg/connect {:host "mongo" :port 27017}))

(defn objectId? [id]
  "Verify that id is an objectId."

  (try
    (do
      (ObjectId. (str id)) true)
    (catch Exception ex false)))

;; ***** Project collection ********************************************************

(defn get-project
  "Retrieves projects from mdb."
  [ref]

  (let [db (mg/get-db conn "sommelier")]
    (mc/find-maps db "project" ref)))

(defn save-project
  "Inserts or updates a project in mdb."
  [project-id document]

  (let [db (mg/get-db conn "sommelier")]
    (if (some? project-id)
      (do
        (mc/update-by-id db "project" (ObjectId. (str project-id)) document)
        (get-project {:_id (ObjectId. (str project-id))}))
      (vector (mc/insert-and-return db "project" document)))))

(defn remove-project!
  "Permanently removes a project from mdb."
  [project-id]

  (let [conn (mg/connect {:host "mongo" :port 27017})
        db (mg/get-db conn "sommelier")]
    (mc/remove-by-id db "project" project-id)))

;; ***** Job collection ********************************************************

(defn get-job
  "Retrieves jobs from mdb."
  [ref]

  (let [db   (mg/get-db conn "sommelier")]
    (mc/find-maps db "job" ref)))

(defn save-job
  "Inserts or updates a job."
  [job-id document]

  (let [db   (mg/get-db conn "sommelier")]
    (if (some? job-id)
      (do
        (mc/update-by-id db "job" (ObjectId. (str job-id)) document)
        (get-job {:_id (ObjectId. (str job-id))}))
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

;(defn count-transactions [projectId]
;  (let [conn (mg/connect {:host "mongo" :port 27017})
;        db (mg/get-db conn (str "job_" projectId))]
;
;    (mc/aggregate db "transactions" [{"$group" {:_id "$transaction" :count {"$sum" 1}}}])))

;; ***** Associations collection ********************************************************
