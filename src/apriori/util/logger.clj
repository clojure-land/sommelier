(ns apriori.util.logger
  (:require [ring.logger :as ring-log]
            [ring.logger.protocols :as ring-logger.protocols]
            [ring.logger.messages :as ring-messages]
            [ring.logger.tools-logging :refer [make-tools-logging-logger]]
            [clojure.tools.logging :as clojure-log]
            [humanize.schema :as humanize]
            [cheshire.core :as json]
            [environ.core :refer [env]]
            [schema.core :as schema]
            [clojure.walk :as walk]))

(def ^:dynamic *request-id*
  "A unique identifier representing a request."
  nil)

(defn ^:private append-request-id
  [item]

  (if (not= *request-id* nil)
    (conj {:request-id *request-id*} item) item))

(defn ^:private append-meta-data
  [item]

  (if (and
        (not-empty (env :log-verbose))
        (contains? item :meta))
    (assoc item :meta (select-keys (update-in (meta (item :meta)) [:ns] str) [:file :ns :line :name :arglists]))
    (dissoc item :meta)))

(defn log
  "Evaluates and logs a message with the request-id.

  e.g. (log :error (throw (Exception. \"my exception message\")))"
  [level body]

  (as->
    (cond
      (instance? java.lang.Throwable body) {:exception (str body)
                                            :trace     (map str (.getStackTrace body))}

      (instance? clojure.lang.PersistentArrayMap body) body

      :else {:message body}) item

    (try
      (clojure-log/log level (json/generate-string (append-meta-data (append-request-id item))))
      (catch Exception e
        (log :error e)))))

(defn ^:private get-total-time
  "Get the total time a request makes."
  [{:keys [logger-start-time logger-end-time] :as req}]

  (- logger-end-time logger-start-time))

(defmethod ring-messages/starting :messages
  [{:keys [logger] :as options} req]

  (log :info {:uri            (get req :uri)
              :request-method (get req :request-method)
              :remote-addr    (get req :remote-addr)
              :query-string   (get req :query-string)}))

(defmethod ring-messages/finished :messages
  [{:keys [timing] :as options} req {:keys [status] :as resp}]

  (log :info {:uri       (get req :uri)
              :status    status
              :load-time (when timing (get-total-time req))}))

(defn wrapper-with-logger
  "Wraps a request within a logger."
  [handler]

  (ring-log/wrap-with-logger handler {:exceptions false
                                      :printer    :messages}))

(defn humanize-schema-exception
  "Humanize schema exceptions."
  [^Exception e]

  (if (schema.utils/error? (ex-data e))
    (humanize/explain (:error (ex-data e))
                      (fn [x]
                        (let [UUID java.util.UUID]
                          (clojure.core.match/match
                            x
                            ['not ['instance? UUID uuid]]
                            (str "The value is not a uuid but it should be.")

                            ['not ['not-timestamp timestamp]]
                            (str "The value is not yyyy-mm-ddThh:mm:ss.mmZ but it should be.")

                            ['not ['not-file-size file-size]]
                            (str "'" file-size "' is not a valid file size but it should be.")

                            ['not ['not-advertiser-id advertiser-id]]
                            (str "'" advertiser-id "' is not a valid advertiser id but it should be.")

                            ['not ['not-email email]]
                            (str "'" email "' is not an email address but it should be.")

                            ;['not [['not-length-in-range min max] item]]
                            ;(str "'" item "' length must be between " min " and " max ".")

                            :else x))))))

  ;(humanize/explain (:error (ex-data e))
  ;                  (fn [x]
  ;                      (let [UUID java.util.UUID]
  ;                        (clojure.core.match/match
  ;                          x
  ;                          ['not ['instance? UUID uuid]]
  ;                          (str "The value is not a uuid but it should be.")
  ;
  ;                          ['not ['not-timestamp timestamp]]
  ;                          (str "The value is not yyyy-mm-ddThh:mm:ss.mmZ but it should be.")
  ;
  ;                          ['not ['not-file-size file-size]]
  ;                          (str "'" file-size "' is not a valid file size but it should be.")
  ;
  ;                          ['not ['not-advertiser-id advertiser-id]]
  ;                          (str "'" advertiser-id "' is not a valid advertiser id but it should be.")
  ;
  ;                          ['not ['not-email email]]
  ;                          (str "'" email "' is not an email address but it should be.")
  ;
  ;                          ['not [['not-length-in-range min max] item]]
  ;                          (str "'" item "' length must be between " min " and " max ".")
  ;
  ;                          :else x)))))