(ns routes.project_test
  (:require [clojure.test :refer :all]
            [util.mdb :refer :all]
            [environ.core :refer [env]]
            [domain.response :as response]
            [schema.core :as schema]
            [handler :as handler]))

(deftest project_test

  (testing "can create project."
    (with-redefs [save-project (fn [_ _] [{:_id "5da5d963fb1e0ac91601424d" :name "some project" :description "" :window 0 :min-support 0 :min-confidence 0}])]
      (let [response (handler/app {:request-method :post
                                   :uri            "/v1/project"
                                   :body-params    {:name "some project"
                                                    :description ""
                                                    :window 0
                                                    :min-support 0
                                                    :min-confidence 0}})
            body (helper/parse-body (:body response))]

        (is (schema/validate {:meta response/Meta :data [response/DataObject]} body))
        (is (= (:status response) 201))
        (is (= (get-in body [:data 0 :id]) "5da5d963fb1e0ac91601424d")))))

  (testing "returns bad request when creating project with missing body params."
    (let [response (handler/app {:request-method :post
                                 :uri            "/v1/project"
                                 :body-params    {}})
          body (helper/parse-body (:body response))]

      (is (schema/validate {:meta response/Meta :errors [response/ErrorObject]} body))
      (is (= (:status response) 400))))

  (testing "returns bad request when creating project with invalid body params."
    (let [response (handler/app {:request-method :post
                                 :uri            "/v1/project"
                                 :body-params    {:name "!@£$%^&*()"
                                                  :description "!@£$%^&*()"
                                                  :window "0"
                                                  :min-support "0"
                                                  :min-confidence "0"}})
          body (helper/parse-body (:body response))]

      (is (schema/validate {:meta response/Meta :errors [response/ErrorObject]} body))
      (is (= (:status response) 400))))

  (testing "can update project."
    (with-redefs [get-project (fn [_] [{:_id "5da5d963fb1e0ac91601424d" :name "some project" :description "" :window 0 :min-support 0 :min-confidence 0}])
                  save-project (fn [_ _] [{:_id "5da5d963fb1e0ac91601424d" :name "some project" :description "" :window 0 :min-support 0 :min-confidence 0}])]
      (let [response (handler/app {:request-method :post
                                   :uri            "/v1/project/5da5d963fb1e0ac91601424d"
                                   :body-params    {:name "some project"
                                                    :description ""
                                                    :window 0
                                                    :min-support 0
                                                    :min-confidence 0}})
            body (helper/parse-body (:body response))]

        (is (schema/validate {:meta response/Meta :data [response/DataObject]} body))
        (is (= (:status response) 200))
        (is (= (get-in body [:data 0 :id]) "5da5d963fb1e0ac91601424d")))))

  (testing "returns not found when updating project with invalid object id."
    (let [response (handler/app {:request-method :post
                                 :uri            "/v1/project/1"
                                 :body-params    {:name "some project"
                                                  :description ""
                                                  :window 0
                                                  :min-support 0
                                                  :min-confidence 0}})
          body (helper/parse-body (:body response))]

      (is (schema/validate {:meta response/Meta :errors [response/ErrorObject]} body))
      (is (= (:status response) 404))))

  (testing "returns bad request when updating project with missing body params."
    (let [response (handler/app {:request-method :post
                                 :uri            "/v1/project/5da5d963fb1e0ac91601424d"
                                 :body-params    {}})
          body (helper/parse-body (:body response))]

      (is (schema/validate {:meta response/Meta :errors [response/ErrorObject]} body))
      (is (= (:status response) 400))))

  (testing "returns bad request when updating project with invalid body params."
    (let [response (handler/app {:request-method :post
                                 :uri            "/v1/project/5da5d963fb1e0ac91601424d"
                                 :body-params    {:name "!@£$%^&*()"
                                                  :description "!@£$%^&*()"
                                                  :window "0"
                                                  :min-support "0"
                                                  :min-confidence "0"}})
          body (helper/parse-body (:body response))]

      (is (schema/validate {:meta response/Meta :errors [response/ErrorObject]} body))
      (is (= (:status response) 400))))

  (testing "returns not found when updating project which does not exist."
    (with-redefs [get-project (fn [_] [])]
      (let [response (handler/app {:request-method :post
                                   :uri            "/v1/project/1"
                                   :body-params    {:name "some project"
                                                    :description ""
                                                    :window 0
                                                    :min-support 0
                                                    :min-confidence 0}})
            body (helper/parse-body (:body response))]

        (is (schema/validate {:meta response/Meta :errors [response/ErrorObject]} body))
        (is (= (:status response) 404)))))

  (testing "can retrieve project."
    (with-redefs [get-project (fn [_] [{:_id "5da5d963fb1e0ac91601424d" :name "some project" :description "" :window 0 :min-support 0 :min-confidence 0}])]
      (let [response (handler/app {:request-method :get
                                   :uri            "/v1/project/5da5d963fb1e0ac91601424d"})
            body (helper/parse-body (:body response))]

        (is (schema/validate {:meta response/Meta :data [response/DataObject]} body))
        (is (= (:status response) 200))
        (is (= (get-in body [:data 0 :id]) "5da5d963fb1e0ac91601424d")))))

  (testing "returns not found when retrieving project with invalid object id."
    (let [response (handler/app {:request-method :get
                                 :uri            "/v1/project/1"})
          body (helper/parse-body (:body response))]

      (is (schema/validate {:meta response/Meta :errors [response/ErrorObject]} body))
      (is (= (:status response) 404))))

  (testing "returns not found when retrieving project which does not exist."
    (with-redefs [get-project (fn [_] [])]
      (let [response (handler/app {:request-method :get
                                   :uri            "/v1/project/5da5d963fb1e0ac91601424d"})
            body (helper/parse-body (:body response))]

        (is (schema/validate {:meta response/Meta :errors [response/ErrorObject]} body))
        (is (= (:status response) 404)))))

  (testing "can delete project."
    (with-redefs [get-project (fn [_] [{:_id "5da5d963fb1e0ac91601424d" :name "some project" :description "" :window 0 :min-support 0 :min-confidence 0}])
                  remove-project (fn [_] true) ]
      (let [response (handler/app {:request-method :delete
                                   :uri            "/v1/project/5da5d963fb1e0ac91601424d"})
            body (helper/parse-body (:body response))]

        (is (= (:status response) 204))
        (is (= body nil)))))

  (testing "returns not found when deleting project with invalid object id."
    (let [response (handler/app {:request-method :delete
                                 :uri            "/v1/project/1"})
          body (helper/parse-body (:body response))]

      (is (schema/validate {:meta response/Meta :errors [response/ErrorObject]} body))
      (is (= (:status response) 404))))

  (testing "returns not found when deleting project which does not exist."
    (with-redefs [get-project (fn [_] [])]
      (let [response (handler/app {:request-method :delete
                                   :uri            "/v1/project/5da5d963fb1e0ac91601424d"})
            body (helper/parse-body (:body response))]

        (is (schema/validate {:meta response/Meta :errors [response/ErrorObject]} body))
        (is (= (:status response) 404))))))