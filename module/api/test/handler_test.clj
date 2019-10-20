(ns handler_test
  (:require [clojure.test :refer :all]
            [environ.core :refer [env]]
            [handler :as handler]))

(deftest handler

  (testing "undocumented roots return bad request"
    (let [response (handler/app {:request-method :get
                                 :uri            "/some/undocumented/root"
                                 :query-params   {}})]

      (is (= (:status response) 404)))))