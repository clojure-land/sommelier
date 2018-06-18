(ns apriori.util.repository.authorization-code
  (:require [apriori.util.storage :as storage]
            [apriori.util.logger :as logger]
            [schema.core :as schema]
            [crypto.random :as crypto]
            [clj-time.core :as time]
            [clj-time.local :as local]
            [clj-time.coerce :as coerce]
            [pandect.algo.sha256 :refer :all]))

(def UserId
  schema/Uuid)

(def ClientId
  (schema/maybe schema/Uuid))

(def AuthorizationCode
  schema/Str)

(def RedirectUri
  (schema/maybe schema/Str))

(def Expires
  schema/Inst)

(schema/defrecord AuthorizationCodeRecord
  [authorization_code :- AuthorizationCode
   user_id :- UserId
   expires :- Expires]
  {(schema/optional-key :client_id)    ClientId
   (schema/optional-key :redirect_uri) RedirectUri})

(schema/defn get-authorization-code :- AuthorizationCode
  [user-record :- AuthorizationCodeRecord]

  (:authorization_code user-record))

(schema/defn get-client-id :- ClientId
  [user-record :- AuthorizationCodeRecord]

  (:client_id user-record))

(schema/defn get-user-id :- UserId
  [user-record :- AuthorizationCodeRecord]

  (:user_id user-record))

(schema/defn get-redirect-uri :- (schema/maybe RedirectUri)
  [user-record :- AuthorizationCodeRecord]

  (:redirect_uri user-record))

(schema/defn get-expires :- Expires
  [user-record :- AuthorizationCodeRecord]

  (:expires user-record))

(schema/defn filter-by-user-id :- (schema/maybe {schema/Keyword schema/Any})
  "Filter by User Id."
  [userId :- UserId]

  (logger/log :debug {:meta       #'filter-by-user-id
                      :arg-values {:id (str userId)}})

  (if (not-empty (str userId))
    {:authorization_code.user_id userId}))

(schema/defn hydrate :- (schema/maybe AuthorizationCodeRecord)
  "Hydrate a database row.

  e.g. (hydrate {...})"
  [row]

  (logger/log :debug {:meta       #'hydrate
                      :arg-values {:row (str row)}})
  (prn row)
  (map->AuthorizationCodeRecord (storage/transform row [[:id :read-uuid]
                                                        [:expires :read-timestamp]] [:*] [])))

(schema/defn fetch :- [AuthorizationCodeRecord]
  "Fetch database records.

  e.g. (fetch [(filter-by-id f7d9ffed-e535-4cd7-8f01-78948af8f528)]"
  [conditions :- [(schema/maybe {schema/Keyword schema/Any})]]

  (logger/log :debug {:meta       #'fetch
                      :arg-values {:conditions (str conditions)}})

  (map hydrate (storage/fetch-entity storage/authorization_code [:*] conditions nil [:expires])))

(schema/defn save :- AuthorizationCodeRecord
  "Save database record.

  e.g. (save (->UserRecord(...))"
  [auth-code :- AuthorizationCodeRecord]

  (logger/log :debug {:meta       #'save
                      :arg-values {:auth-code (str auth-code)}})

  (storage/save-entity storage/authorization_code
                                                     (storage/transform auth-code [] [:*] [:user_id])
                                                     (storage/transform auth-code [[:user_id :write-uuid]
                                                                                   [:expires :write-timestamp]] [:user_id] [])
                                                     [(filter-by-user-id (get-user-id auth-code))])

  (first (fetch [(filter-by-user-id (get-user-id auth-code))])))

(schema/defn spawn [client-id :- ClientId user-id :- UserId] :- AuthorizationCodeRecord
  (->
    (->AuthorizationCodeRecord (sha256-hmac (str client-id "+" user-id) (sha256 (crypto/base32 32)))
                               user-id
                               (coerce/to-timestamp
                                 (local/format-local-time (time/plus (local/local-now)
                                                                     (time/seconds 30)) :date-time-no-ms)))
    (assoc :client_id client-id)
    (save)))

(schema/defn delete [conditions :- [(schema/maybe {schema/Keyword schema/Any})]] nil)
