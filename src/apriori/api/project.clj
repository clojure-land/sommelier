(ns apriori.api.project
  (:require [ring.util.response :refer :all]
            [clojure.java.jdbc :refer :all]
            [apriori.utlis.storage :refer :all]
            [clj-time.core :as time]
            [clj-time.coerce :as coerce]
            [clj-time.format :as format]
            [environ.core :refer [env]]))

(defn get-project [id]
  (let [result (query db ["select * from project where id =?", id])]
    (if (empty? result)
      (status (response {:meta {:status "not found"}}) 404)
      (status (response {:meta {:status "ok"} :data (first result)}) 200))))

(defn add-edit-project [edit-id body]
  (let [project (as-> {} p
                      (assoc p :id (if (not-empty edit-id) edit-id (str (java.util.UUID/randomUUID))))
                      (assoc p :name (if (empty? (get body "name")) "" (get body "name")))
                      (assoc p :support (if (integer? (get body "support")) (get body "support") (try (Integer/parseInt (get body "support")) (catch Exception e 0))))
                      (assoc p :confidence (if (integer? (get body "confidence")) (get body "confidence") (try (Integer/parseInt (get body "confidence")) (catch Exception e 0))))
                      (assoc p :modified (quot (System/currentTimeMillis) 1000)))

        err (as-> () e
                  (if (not= edit-id nil)
                    (let [result (query db ["select COUNT(*) AS count from project where id =?", edit-id])]
                      (if (= (get (first result) :count) 0)
                        (conj e "Project id does not exist.") e)) e)
                  (if (= (get project :name) nil)
                    (conj e "A project name is required.") e))]

    (if (= err ())
      (if (not= edit-id nil)
        (let [update (update! db :project project ["id =?" edit-id])]
          (status (response {:meta {:status "updated"} :data project}) 200))
        (let [insert (insert! db :project project)]
          (status (response {:meta {:status "created"} :data project}) 201)))
      (status (response {:meta {:status "bad request"} :data {:errors err}}) 400))))

(defn delete-project [id]
  (let [result (delete! db :project ["id =?" id])]
    (status (response {:meta {:status "deleted"}}) 200)))