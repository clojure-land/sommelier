package com.sommelier;

import java.util.HashMap;
import java.util.Map;

import static io.restassured.RestAssured.baseURI;

public class Helper {
    private static final String HOST = System.getenv("cucumber.server");

    /**
     * Configures the REST assured static properties for a test run.
     * Bases the values on the tag attribute.
     */
    public static void configureRestAssured() {
        baseURI = HOST;
    }

    public static String replacePlaceholders(HashMap<String, Object> attributes, String string)
    {
        for (Map.Entry attr : attributes.entrySet()) {
            string = string.replaceAll(attr.getKey().toString(), attr.getValue().toString());
        }

        return string;
    }
}



//;[clj-http.client :as client]
//
//        ;(defn- fetch-transactions
//        ;       "Retrieves transactions paginated."
//        ;       [db-name page]
//        ;
//        ;       (let [conn (mg/connect {:host host :port 27017})
//        ;             db   (mg/get-db conn db-name)]
//        ;
//        ;            (mq/with-collection db "transactions"
//        ;                                (mq/paginate :page page :per-page 1000))))
//
//        ;(defn random-transaction [x]
//        ;      (let [transaction (take
//        ;                          (rand-int x)
//        ;                          ((partial shuffle ["a" "b" "c" "d" "e", "f", "g", "h", "i", "j", "k"])))]
//        ;
//        ;           (if (empty? transaction)
//        ;             (random-transaction x) transaction)))
//
//        ;(defn test-data []
//        ;      (reduce (fn [x number]
//        ;                  (conj x (vec
//        ;                            (if (= (rand-int 4) 1)
//        ;                              (random-transaction 10)
//        ;                              (try
//        ;                                (rand-nth x)
//        ;                                (catch Exception e (random-transaction 10)))))))
//        ;              []
//        ;              (take 1000 (iterate inc 1))))
//
//        ;(defn post-data []
//        ;      (dotimes [i 10]
//        ;               (->
//        ;                 (client/post "http://localhost:3000/v1/project/5d481cc224aa9a00077152b5/transactions"
//        ;                              {:body (json/generate-string {:transactions (test-data)})
//        ;                               :content-type :json
//        ;                               :accept :json})
//        ;                 (get :body)
//        ;                 (json/decode)
//        ;                 (get "data")
//        ;                 (first)
//        ;                 (get-in ["attributes" "transactions"])
//        ;                 (println))))