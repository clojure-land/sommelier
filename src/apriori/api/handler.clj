(ns apriori.api.handler
  (:require [compojure.core :refer :all]
            [cheshire.core :refer :all]
            [apriori.api.health :refer :all]
            [apriori.api.project :refer :all]
            [apriori.api.data_set :refer :all]
            [apriori.utlis.freq_item_set :refer :all]
            [ring.util.response :refer :all]
            [ring.middleware.json :as middleware]
            [schema.core :as schema]
            [compojure.handler :as handler]
            [compojure.route :as route]))

; api routes
(defroutes api-routes
           (GET "/health" [] (get-health))
           ;(GET "/rules/project/:id" [id] (get-association-rules))

           (context "/project" [] (PUT "/" {body :body} (add-edit-project nil body))
                                  (context "/:id" [id] (GET "/" [id] (get-project id))
                                                       (POST "/" {body :body} (add-edit-project id body))
                                                       (DELETE "/" [id] (delete-project id))))

           (context "/data/project/:id" [id] (POST "/" {body :body} (add-data body)))

           (GET "/item-set" [] (generate-freq-item-set))

           (route/not-found (status (response {:meta {:status "not found"} :data {:errors ["The requested endpoint does not exist."]}}) 404)))

; run our api :)
(def api
  (-> (handler/api api-routes)
      (middleware/wrap-json-body)
      (middleware/wrap-json-response)))