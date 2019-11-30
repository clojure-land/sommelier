(ns handler
  (:require [domain.response :refer :all]
            [util.request :refer :all]
            [util.response :refer :all]
            [util.auth :refer :all]
            [util.exception-handlers :refer :all]
            [compojure.api.sweet :refer :all]
            [compojure.api.exception :as ex]
            [compojure.route :as route]
            [ring.logger.messages :as ring-messages]
            [ring.logger :as ring-log]
            [schema.core :as schema]
            [environ.core :refer [env]]
            [routes.projects :refer [projects-routes]]
            [routes.project :refer [project-routes]]
            [routes.task :refer [task-routes]]
            [clojure.tools.trace :as trace-tool]
            [clojure.tools.logging :as log]
            [clj-time.core :as time]
            [monger.core :as mg]
            [monger.command :as cmd]))

;; ***** Api implementation ********************************************************

(def ^:private server-start-time (clj-time.coerce/to-long (time/now)))

(schema/set-fn-validation! (log/enabled? :debug))

(if (log/enabled? :trace)
  (do
    (trace-tool/trace-ns routes.tasks)
    (trace-tool/trace-ns routes.task)
    (trace-tool/trace-ns routes.projects)
    (trace-tool/trace-ns routes.project)
    (trace-tool/trace-ns routes.transactions)
    (trace-tool/trace-ns util.auth)
    (trace-tool/trace-ns util.mdb)
    (trace-tool/trace-ns util.request)
    (trace-tool/trace-ns util.response)))

(defn- generate-request-id
  "Generate a request identifier."
  []

  (java.util.UUID/randomUUID))

(defn- with-request-id
  "Create new request-id binding."
  [f]

  (fn [request]
    (binding [*id* (generate-request-id)]
      (f request))))

(defn- get-total-time
  "Get the total time a request makes."
  [{:keys [logger-start-time logger-end-time] :as req}]

  (- logger-end-time logger-start-time))

(defmethod ring-messages/starting :messages
  [{:keys [logger] :as options} req]

  (log/info *id* {:uri (get req :uri)
                  :request-method (get req :request-method)
                  :remote-addr    (get req :remote-addr)
                  :query-string     (get req :query-string)}))

(defmethod ring-messages/finished :messages
  [{:keys [timing] :as options} req {:keys [status] :as resp}]

  (log/info *id* {:uri (get req :uri)
                  :status    status
                  :load-time (when timing (get-total-time req))}))

(defn- current-session
  "Retrieves the active session."
  [session]

  (api-response (->ApiData {:status "ok"} [{:type "user" :attributes session}]) 200 []))

(defn- health
  "Retrieves the health check."
  []

  (let [conn (mg/connect {:host "mongo" :port 27017})
        db   (mg/get-db conn "sommelier")
        current-time (clj-time.coerce/to-long (time/now))
        mongo (try
                {:status "healthy" :uptime (get (cmd/server-status db) "uptime")}
                (catch Exception ex {:status "unhealthy" :uptime 0}))

        api (if (= (get mongo :status) "healthy")
              {:status "healthy" :uptime (int (/ (- current-time server-start-time) 1000))}
              {:status "unhealthy" :uptime 0})]

  (api-response (->ApiData {:status "ok"} [{:type "api" :attributes api}
                                           {:type "mongo" :attributes mongo}]) 200 [])))

;; ***** Api definition *******************************************************

(def app
  (with-request-id
    (ring-log/wrap-with-logger
      (api
        {:exceptions {:handlers
                      {::ex/request-parsing    (parsing-exception-handler ring.util.http-response/bad-request)
                       ::ex/request-validation (bad-request-handler ring.util.http-response/bad-request)
                       ::ex/default            (exception-handler ring.util.http-response/internal-server-error)}}

         :swagger    {:ui      "/docs"
                      :spec    "/swagger.json"
                      :options {:ui {:validatorUrl nil}}
                      :data    {:produces            ["application/json"],
                                :consumes            ["application/json"],
                                :info                {:title   ""
                                                      :version (if (not-empty (env :version)) (env :version) "")}
                                :securityDefinitions {:api_key {:type "apiKey" :name "Authorization" :in "header"}}}}}

        (context "/v1" []
          (GET "/health" []
            :operationId "getHealth"
            :summary "Retrieves the health check."
            :responses {200 {:description "ok"}} (health))

          (GET "/session" []
            :operationId "getSession"
            :summary "Retrieves the active session."
            :middleware [#(util.auth/auth! %)]
            :current-user session
            :responses {200 {:description "ok"}
                        401 {:description "unauthorized"}
                        403 {:description "forbidden"}} (current-session session))

          projects-routes
          project-routes
          task-routes)

        (undocumented
          (route/not-found
            (api-response (->ApiError {} [{:message "Route not found."}]) 404 []))))

      {:exceptions false
       :printer    :messages})))

; GET    /projects

; POST   /project
; POST   /project/:id
; GET    /project/:id
; DELETE /project/:id
; POST   /project/:id/transactions
; GET    /project/:id/tasks?latest=true|false&id={objectId}

; GET    /task/:id
; DELETE /task/:id
; GET    /task/:id/associations
; GET    /task/:id/frequencies
; POST   /task/:id/action -> run

; subset(2 2)
; [a b c] -> [a b][b c]
;