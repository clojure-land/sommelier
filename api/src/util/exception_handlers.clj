(ns util.exception-handlers
  (:require
    [domain.response :refer :all]
    [util.request :refer :all]
    [util.response :refer :all]
    [clojure.tools.logging :as log]
    [humanize.schema :as humanize]))

(defn exception-handler
  "Handles exceptions."
  [handler]

  (fn [ex data request]
    (log/error *id* ex)
    (prn ex)
    (api-response (->ApiError {:status "failed"} [{:message "Server error."}]) 500 [])))

(defn bad-request-translations
  "Translates bad requests into human readable format."
  [error]

  (clojure.core.match/match
    error
    ['not ['datetime? value]]
    (str "The value is expected to be rfc3339 format, yyyy-mm-ddThh:mm:ssZ.")

    ['not ['alphanumeric? value]]
    (str "The value is expected to be alphanumeric.")

    ['not [['length? min max] value]]
    (str "The length is expected to be between " min " and " max " characters long.")

    ['not [['size? min max] value]]
    (str "The size is expected to be between " min " and " max ".")

    ['not [['between? min max] value]]
    (str "The value is expected to be between " min " and " max ".")

    :else error))

(defn bad-request-handler
  "Handles bad requests."
  [handler]

  (fn [ex data request]
      (if (instance? schema.utils.ErrorContainer (ex-data ex))
        (api-response (->ApiError {:status "bad request"}
                    (map (fn [error]
                           {:message (val error) :pointer (str "/" (name (key error)))})
                         (humanize/explain (:error (ex-data ex)) bad-request-translations))) 400 [])

        (api-response (->ApiError {:status "bad request"} [{:message "Bad request error."}]) 400 []))))

(defn parsing-exception-handler
  "Handles parse exceptions."
  [handler]

  (fn [ex data request]
      (api-response
        (->ApiError {:status "bad request"} [{:message "Parsing error."}]) 400 [])))