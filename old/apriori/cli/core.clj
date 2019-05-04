(ns apriori.cli.core
  (:require
    [pandect.algo.sha256 :refer :all]
    [apriori.api.handler :as api]
    [environ.core :refer [env]]
    [clojure.tools.cli :refer [parse-opts]]
    [clojure.string :as string]
    [cheshire.core :as json]
    [apriori.util.apriori :as apriori]
    [ring.adapter.jetty :as jetty]
    [schema.core :as schema]
    [apriori.util.logger :as logger])
  (:gen-class))

(defn usage [options-summary]
  (string/join \newline ["Usage: apriori action [options]" "" "Options:" options-summary "" "Actions:" "  launch-api         Launch an instance of the api." ""]))

(defn error-msg
  "Output error message."
  [errors]

  (str (string/join \newline errors) \newline))

(defn exit
  "Output message with a system status."
  [status msg]

  (println msg)
  (System/exit status))

(def cli-options
  "Command line interface options."

  [["-p" "--port PORT" "Port number"
    :default 3000
    :parse-fn #(Integer/parseInt %)
    :validate [#(< 0 % 0x10000) "Must be a number between 0 and 65536"]]

   [nil "--email EMAIL" "Email address"
    :default ""]

   [nil "--password PASSWORD" "Password"
    :default ""]

   [nil "--name NAME" "Email address"]
   [nil "--redirect-uri URI" "Redirect URI delimited by spaces."]
   [nil "--scope SCOPE" "Scope delimited by spaces."]
   ["-h" "--help"]])

(defn validate-args
  "Validate arguments."
  [args]

  (let [{:keys [options arguments errors summary]} (parse-opts args cli-options)]
    (cond
      (:help options)
      {:exit-message (usage summary) :ok? true}

      errors {:exit-message (error-msg errors)}

      (and (= 1 (count arguments))
           (#{"launch-api"
              "apriori"} (first arguments))) {:action (first arguments) :options options}

      :else
      {:exit-message (error-msg ["Command not found, try running: --help"])})))

(defn random-transaction [x]
  (let [transaction (take
                      (rand-int x)
                      ((partial shuffle ["apple" "banana" "pear" "mango" "raspberries", "kiwi", "orange", "tomato", "pineapple", "melon", "water melon"])))]

    (if (empty? transaction)
      (random-transaction x)
      transaction)))

(defn test-data []
  (reduce (fn [x number]
            (conj x (vec
                      (if (= (rand-int 4) 1)
                        (random-transaction 10)
                        (try
                          (rand-nth x)
                          (catch Exception e (random-transaction 10)))))))
          []
          (take 1000 (iterate inc 1))))

(defn -main
  "Run the command line interface."
  [& args]

  (let [{:keys [action options exit-message ok?]} (validate-args args)]
    (if exit-message
      (exit (if ok? 0 1) exit-message)

      (case action
        "launch-api" (jetty/run-jetty api/app {:port (get options :port)})

        "apriori" (apriori/run
                    (test-data)
                    ;[["beer"]
                    ; ["beer" "cheese"]
                    ; ["banana" "beer" "cheese" "nuts"]
                    ; ["beer" "nuts"]
                    ; ["beer" "cheese" "nuts"]
                    ; ["banana" "cheese" "nuts"]
                    ; ["beer" "cheese"]
                    ; ["banana" "beer" "cheese" "nuts"]
                    ; ["beer" "cheese" "nuts"]
                    ; ["banana" "beer" "cheese" "nuts"]]

                    ;[["bread" "milk"]
                    ; ["bread" "diaper" "beer" "eggs"]
                    ; ["milk" "diaper" "beer" "coke"]
                    ; ["bread" "milk" "diaper" "beer"]
                    ; ["bread" "milk" "diaper" "coke"]]]
                    )

        "default" nil))))
