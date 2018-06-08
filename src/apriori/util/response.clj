(ns apriori.util.response
  (:require [apriori.domain.api :as api]
            [ring.util.response :as ring]
            [schema.core :as schema]))

(schema/defn response
  [code :- schema/Int body :- api/ApiResponseSchema]

  (ring/status (ring/response body) code))

(schema/defn ok
  [meta-data :- (schema/maybe {schema/Keyword schema/Any})
   body :- api/Data]

  (response 200 (api/->ApiResponse (api/->Meta "ok" meta-data) body)))

(schema/defn created
  [meta-data :- (schema/maybe {schema/Keyword schema/Any})
   body :- api/Data]

  (response 201 (api/->ApiResponse (api/->Meta "created" meta-data) body)))

(schema/defn unauthorized
  [meta-data :- (schema/maybe {schema/Keyword schema/Any})
   body :- api/Data]

  (response 401 (api/->ApiResponse (api/->Meta "unauthorized" meta-data) body)))

(schema/defn forbidden
  [meta-data :- (schema/maybe {schema/Keyword schema/Any})
   body :- api/Data]

  (response 403 (api/->ApiResponse (api/->Meta "forbidden" meta-data) body)))

(schema/defn not-found
  [meta-data :- (schema/maybe {schema/Keyword schema/Any})
   body :- api/Data]

  (response 404 (api/->ApiResponse (api/->Meta "not found" meta-data) body)))

(schema/defn conflict
  [meta-data :- (schema/maybe {schema/Keyword schema/Any})
   body :- api/Data]

  (response 409 (api/->ApiResponse (api/->Meta "conflict" meta-data) body)))
