(ns util.response
  (:require [domain.response :refer :all]
            [ring.util.response :as ring]))

;; ***** Api implementation ********************************************************

(defn api-response
  "Returns a Ring response which satisfies the ResponseProtocol."
  [body code headers]

  (if (satisfies? ResponseProtocol body)
    (if (and (vector? headers) (pos? (count headers)))
      (reduce (fn [resp header]
                (ring/header resp (name (first (keys header))) (first (vals header))))
              (api-response body code []) headers)
      (ring/status (ring/response (Get body)) code))))

;; ***** Project implementation ********************************************************

(defn project->resource-object
  "Transforms a project into a resource object."
  [projects]

  (map (fn [row]
         {:type       "project"
          :id         (str (get row :_id))
          :attributes (dissoc row :_id :author)}) projects))

(defn project-not-found
  "Returns a 404 Ring response."
  [id]

  (api-response (->ApiError {:status "not found"} [{:message (str "No project found for id: " id)}]) 404 []))

;; ***** Job implementation ********************************************************

(defn job->resource-object
  "Transforms a job into a resource object."
  [jobs]

  (map (fn [row]
         (prn row)

         {:type       "job"
          :id         (str (get row :_id))
          :attributes (update  (dissoc row :_id) :project-id str)}) jobs))
