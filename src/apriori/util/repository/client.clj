(ns apriori.util.repository.client
  (:require [apriori.util.storage :as storage]
            [apriori.util.logger :as logger]
            [schema.core :as schema]
            [crypto.random :as crypto]
            [pandect.algo.sha256 :refer :all]))

(def Id
  schema/Uuid)

(def Name
  (schema/constrained
    schema/Str #(<= 7 (count %) 225) (list 'not-length-in-range 7 225)))

(def ClientId
  schema/Str)

(def ClientSecret
  schema/Str)

(def RedirectUri
  (schema/maybe
    (schema/constrained
      schema/Str
      #(re-matches #"(https?:\/\/(?:www\.|(?!www))[a-zA-Z0-9][a-zA-Z0-9-]+[a-zA-Z0-9]\.[^\s]{2,}|www\.[a-zA-Z0-9][a-zA-Z0-9-]+[a-zA-Z0-9]\.[^\s]{2,}|https?:\/\/(?:www\.|(?!www))[a-zA-Z0-9]\.[^\s]{2,}|www\.[a-zA-Z0-9]\.[^\s]{2,})" (name %)) 'not-url)))

(def GrantType
  (schema/enum "code" "password" "client_credentials"))

(def Scope
  schema/Str)

(def UserId
  (schema/maybe schema/Uuid))

(schema/defrecord ClientRecord
  [id :- Id
   name :- Name
   client_id :- ClientId
   client_secret :- ClientSecret
   grant_type :- GrantType
   scope :- Scope]

  {(schema/optional-key :user_id)      UserId
   (schema/optional-key :redirect_uri) RedirectUri}

  (fn [{:as this :keys [grant_type redirect_uri]}]
    ;(if (or (and (not= grant_type "password") (not= grant_type "code"))
    ;        (and (= grant_type "password") (not-empty redirect_uri))
    ;        (and (= grant_type "code") (not-empty redirect_uri)))
    ;  ;"(not)"
    ;  ;(throw (ex-info "(not (instance? java.lang.Number \"4\"))" {}))
    ;  true
    ;  false)
    ;(prn "extra")
    (or (and (not= grant_type "password") (not= grant_type "code"))
        (and (= grant_type "password") (not-empty redirect_uri))
        (and (= grant_type "code") (not-empty redirect_uri)))
    ))

;(->ClientRecord (java.util.UUID/randomUUID)  "test" "111" "222" "password" "profile")
;(humanize-schema-message (schema/check ClientRecord (->ClientRecord (java.util.UUID/randomUUID)  "test" "111" "222" "password" "profile")))
;(schema/validate ClientRecord (->ClientRecord (java.util.UUID/randomUUID)  "test" "111" "222" "client_credentials" "profile"))
;(schema/validate ClientRecord (map->ClientRecord {:id (java.util.UUID/randomUUID)
;                                                  :name "test"
;                                                  :client_id "111"
;                                                  :client_secret "222"
;                                                  :grant_type "password"
;                                                  :scope "profile"
;                                                  :redirect_uri ""}))

;(schema/validate ClientRecord (map->ClientRecord {:id (java.util.UUID/randomUUID)
;                                                  :name "test"
;                                                  :client_id "111"
;                                                  :client_secret "222"
;                                                  :grant_type "password"
;                                                  :scope "profile"
;                                                  :redirect_uri "test"}))

;https://blog.ayalog.com/2015/07/14/extra_validator_function.html

(schema/defn get-id :- Id
  [client-record :- ClientRecord]

  (:id client-record))

(schema/defn get-name :- Name
  [client-record :- ClientRecord]

  (:name client-record))

(schema/defn get-client-id :- ClientId
  [client-record :- ClientRecord]

  (:client_id client-record))

(schema/defn get-client-secret :- ClientSecret
  [client-record :- ClientRecord]

  (:client_secret client-record))

(schema/defn get-redirect-uri :- RedirectUri
  [client-record :- ClientRecord]

  (:redirect_uri client-record))

(schema/defn get-grant-type :- GrantType
  [client-record :- ClientRecord]

  (:grant_type client-record))

(schema/defn get-scope :- Scope
  [client-record :- ClientRecord]

  (:scope client-record))

(schema/defn get-user-id :- UserId
  [client-record :- ClientRecord]

  (:user_id client-record))

(schema/defn filter-by-id :- (schema/maybe {schema/Keyword schema/Any})
  "Filter by Id."
  [id :- Id]

  (logger/log :debug {:meta       #'filter-by-id
                      :arg-values {:id (str id)}})

  (if (not-empty (str id))
    {:client.id id}))

(schema/defn hydrate :- (schema/maybe ClientRecord)
  "Hydrate a database row.

  e.g. (hydrate {...})"
  [row]

  (logger/log :debug {:meta       #'hydrate
                      :arg-values {:row (str row)}})

  (map->ClientRecord (storage/transform row [[:id :read-uuid]] [:*] [])))

(schema/defn fetch :- [ClientRecord]
  "Fetch database records.

  e.g. (fetch [(filter-by-id f7d9ffed-e535-4cd7-8f01-78948af8f528)]"
  [conditions :- [(schema/maybe {schema/Keyword schema/Any})]]

  (logger/log :debug {:meta       #'fetch
                      :arg-values {:conditions (str conditions)}})

  (map hydrate (storage/fetch-entity storage/client [:*] conditions nil [:id])))

(schema/defn save :- ClientRecord
  "Save database record.

  e.g. (save (->ClientRecord(...))"
  [client :- ClientRecord]

  (logger/log :debug {:meta       #'save
                      :arg-values {:client (str client)}})

  (map->ClientRecord (storage/save-entity storage/client
                                          (storage/transform client [] [:*] [:id])
                                          (storage/transform client [[:id :write-uuid]] [:id] [])
                                          [(filter-by-id (get-id client))])))
