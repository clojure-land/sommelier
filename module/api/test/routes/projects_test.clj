(ns routes.projects_test
  (:require [clojure.test :refer :all]
            [util.mdb :refer :all]
            [environ.core :refer [env]]
            [domain.response :as response]
            [schema.core :as schema]
            [handler :as handler]))

(deftest projects_test

  (testing "can retrieve projects."
    (with-redefs [get-project (fn [_] [{:_id "5da5d963fb1e0ac91601424d" :name "some project" :description "" :window 0 :min-support 0 :min-confidence 0}])]
      (let [response (handler/app {:request-method :get
                                   :uri            "/v1/projects"
                                   :query-params   {}})
            body (helper/parse-body (:body response))]

        (is (schema/validate {:meta response/Meta :data [response/DataObject]} body))
        (is (= (:status response) 200))
        (is (= (get-in body [:data 0 :id]) "5da5d963fb1e0ac91601424d"))))))