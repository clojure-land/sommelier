(ns apriori.util.storage
  (:import [org.postgresql.util PGobject])
  (:require [apriori.util.logger :as logger]
            [korma.db :refer :all]
            [korma.core :as korma]
            [black.water.korma :refer [decorate-korma!]]
            [black.water.log :refer [log-sql set-logger!]]
            [environ.core :refer [env]]
            [clojure.string :as str]
            [clj-time.coerce :as coerce]
            [cheshire.core :as json])
  (:import (org.postgresql.util PGobject)))

; database config
(def db {:dbtype   "postgresql"
         :dbname   (env :db-name)
         :port     (env :db-port)
         :host     (env :db-host)
         :user     (env :db-user)
         :password (env :db-password)})

(set-logger! (fn [sql millis] (logger/log :info {:sql sql :query-time millis})))

(declare user client authorization_code project frequencies associations)

(korma/defentity user
                 (korma/pk :id)
                 (korma/database db)
                 (korma/table :apriori.user)
                 (korma/has-one authorization_code {:fk :user_id})
                 (korma/has-many client {:fk :user_id}))

(korma/defentity client
                 (korma/pk :id)
                 (korma/database db)
                 (korma/table :apriori.client)
                 (korma/belongs-to user {:fk :user_id}))

(korma/defentity authorization_code
                 (korma/pk :user_id)
                 (korma/database db)
                 (korma/table :apriori.authorization_code)
                 (korma/belongs-to user {:fk :user_id}))

(korma/defentity project
                 (korma/pk :id)
                 (korma/database db)
                 (korma/table :apriori.project)
                 (korma/belongs-to user {:fk :author}))

(korma/defentity frequencies
                 (korma/pk :id)
                 (korma/pk :transaction)
                 (korma/database db)
                 (korma/table :apriori.project)
                 (korma/belongs-to project {:fk :project_id}))

(korma/defentity associations
                 (korma/pk :id)
                 (korma/pk :association)
                 (korma/pk :created)
                 (korma/database db)
                 (korma/table :apriori.project)
                 (korma/belongs-to project {:fk :project_id}))

(defn- read-pgobject
  "Read pgobject."
  [^org.postgresql.util.PGobject x]

  (logger/log :debug {:meta       #'read-pgobject
                      :arg-values {:x (str x)}})

  (try
    (when-let [val (.getValue x)]
      (json/parse-string val true))
    (catch Exception e nil (logger/log :warn e))))

(defn- write-pgobject
  "Write pgobject."
  [json-type data]

  (logger/log :debug {:meta       #'write-pgobject
                      :arg-values {:json-type (str json-type)
                                   :data      (str data)}})

  (doto (PGobject.)
    (.setType json-type)
    (.setValue (json/generate-string data))))

(defn- write-uuid
  "Write pgobject uuid"
  [uuid-string]

  (logger/log :debug {:meta       #'write-uuid
                      :arg-values {:uuid-string (str uuid-string)}})

  (doto (PGobject.)
    (.setType "uuid")
    (.setValue uuid-string)))

(defn- with-fields
  "Fetch fields for thi
  s entity."
  [query entity fields]

  (logger/log :debug {:meta       #'with-fields
                      :arg-values {:query  (korma/as-sql query)
                                   :entity (str entity)
                                   :fields (str fields)}})

  (if (contains? fields (keyword entity))
    (assoc query :fields (fields (keyword entity))) query))

(defn- filter-by
  "Conditions to append to the query."
  [query conditions]

  (logger/log :debug {:meta       #'filter-by
                      :arg-values {:query      (korma/as-sql query)
                                   :conditions (str conditions)}})

  (reduce (fn [query condition]
            (korma/where query condition)) query (filter map? conditions)))

(defn- fetch-with
  "Join additional entities to the query."
  [query entities fields]

  (logger/log :debug {:meta       #'fetch-with
                      :arg-values {:query    (korma/as-sql query)
                                   :entities (str entities)
                                   :fields   (str fields)}})

  (reduce (fn [query entity]
            (case entity
              :user (korma/with query user (with-fields entity fields))
              :project (korma/with query project (with-fields entity fields))
              :frequencies (korma/with query frequencies (with-fields entity fields))
              :associations (korma/with query associations (with-fields entity fields))))
          query (filter keyword? entities)))

(defn fetch-entity
  "Run select query against an entity."
  [entity fields conditions entities [& order]]

  (logger/log :debug {:meta       #'fetch-entity
                      :arg-values {:entity     (entity :name)
                                   :fields     (str fields)
                                   :conditions (str conditions)
                                   :entities   (str entities)
                                   :order      (str order)}})

  (decorate-korma!)
  (->
    (korma/select* entity)

    (with-fields (entity :name) fields)
    (fetch-with entities fields)
    (filter-by conditions)

    (as-> query
          (if (seq order)
            (korma/order query order :desc) query))

    (korma/exec)))

(defn save-entity
  "Save a entity to the database."
  [entity values append-values-on-insert conditions]

  (logger/log :debug {:meta       #'save-entity
                      :arg-values {:entity                  (entity :name)
                                   :values                  (str values)
                                   :append-values-on-insert (str append-values-on-insert)
                                   :conditions              (str conditions)}})

  (decorate-korma!)
  (if (empty? (fetch-entity entity [:*] conditions nil nil))
    (->
      (korma/insert* entity)
      (korma/values (merge values append-values-on-insert))
      (korma/exec))
    (->
      (korma/update* entity)
      (korma/set-fields values)
      (filter-by conditions)
      (korma/exec))))

(defn delete-entity
  "Delete a entity from the database."
  [entity conditions] nil)

(defn hydrate
  "Hydrate row with a schema."
  [row schema] nil)

(defn transform
  "Transform a row for database read or write operations."
  [row transform select remove]

  (logger/log :debug {:meta       #'transform
                      :arg-values {:row       (str row)
                                   :transform (str transform)
                                   :select    (str select)
                                   :remove    (str remove)}})

  (if (map? row)
    (as-> (into {} row) map
          (if (not-empty transform)
            (reduce (fn [map transform]
                      (case (second transform)
                        :write-uuid (assoc map
                                      (first transform)
                                      (write-uuid (str ((first transform) map))))

                        :write-timestamp (update-in map [(first transform)] coerce/to-timestamp)

                        :write-jsonb (assoc map
                                       (first transform)
                                       (write-pgobject "jsonb" ((first transform) map)))

                        :read-timestamp (update-in map [(first transform)] coerce/to-date)

                        :read-jsonb (update-in map [(first transform)] read-pgobject)

                        map)) map transform) map)

          (if (not= select [:*])
            (select-keys map select) map)

          (if (not-empty remove)
            (reduce dissoc map remove) map)) {}))
