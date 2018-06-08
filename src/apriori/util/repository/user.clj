(ns apriori.util.repository.user
  (:require [apriori.util.storage :as storage]
            [apriori.util.logger :as logger]
            [schema.core :as schema]
            [crypto.random :as crypto]
            [pandect.algo.sha256 :refer :all]))

(def Id
  schema/Uuid)

(def Email
  (schema/constrained
    schema/Str
    #(re-matches #"^[a-zA-Z0-9.!#$%&'*+\/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$" (name %)) 'not-email))

(def PlainTextPassword
  (schema/constrained
    schema/Str #(<= 7 (count %) 128) (list 'not-length-in-range 7 128)))

(def Password
  schema/Str)

(def Salt
  schema/Str)

(schema/defrecord UserRecord
  [id :- Id
   email :- Email
   password :- Password
   salt :- Salt]
  {(schema/optional-key :plain-text-password) PlainTextPassword})

(schema/defn get-id :- Id [user-record :- UserRecord]
  (:id user-record))

(schema/defn get-email :- Email [user-record :- UserRecord]
  (:email user-record))

(schema/defn get-plain-test-password :- (schema/maybe PlainTextPassword)
  [user-record :- UserRecord]
  (:plain-text-password user-record))

(schema/defn get-password :- Password [user-record :- UserRecord]
  (:password user-record))

(schema/defn get-salt :- Salt [user-record :- UserRecord]
  (:salt user-record))

(schema/defn hashPlainTextPassword :- UserRecord
  "Hash password with salt."
  [user :- UserRecord]

  (let [salt (if (empty? (get-salt user))
                         (sha256 (crypto/base32 32))
                         (get-salt user))]
    (->
      (assoc user :password (sha256-hmac (get-plain-test-password user) salt))
      (assoc :salt salt))))

(schema/defn filter-by-id :- (schema/maybe {schema/Keyword schema/Any})
  "Filter by Id."
  [id :- Id]

  (logger/log :debug {:meta       #'filter-by-id
                      :arg-values {:id (str id)}})

  (if (not-empty (str id))
    {:user.id id}))

(schema/defn filter-by-email :- (schema/maybe {schema/Keyword schema/Any})
  "Filter by Email."
  [email :- Email]

  (logger/log :debug {:meta       #'filter-by-email
                      :arg-values {:email (str email)}})

  (if (not-empty (str email))
    {:user.email email}))

(schema/defn hydrate :- (schema/maybe UserRecord)
  "Hydrate a database row.

  e.g. (hydrate {...})"
  [row]

  (logger/log :debug {:meta       #'hydrate
                      :arg-values {:row (str row)}})

  (map->UserRecord (storage/transform row [[:id :read-uuid]] [:*] [])))

(schema/defn fetch :- [UserRecord]
  "Fetch database user records.

  e.g. (fetch [(filter-by-id f7d9ffed-e535-4cd7-8f01-78948af8f528)]"
  [conditions :- [(schema/maybe {schema/Keyword schema/Any})]]

  (logger/log :debug {:meta       #'fetch
                      :arg-values {:conditions (str conditions)}})

  (map hydrate (storage/fetch-entity storage/user [:*] conditions nil [:id])))

(schema/defn save :- UserRecord
  "Save database user record.

  e.g. (save (->UserRecord(...))"
  [user :- UserRecord]

  (logger/log :debug {:meta       #'save
                      :arg-values {:user (str user)}})

  (let [db-user (if (not-empty (get-plain-test-password user))
                  (hashPlainTextPassword user) user)]

    (map->UserRecord (storage/save-entity storage/user
                                          (storage/transform db-user [] [:*] [:id :plain-text-password])
                                          (storage/transform db-user [[:id :write-uuid]] [:id] [])
                                          [(filter-by-id (get-id db-user))]))))