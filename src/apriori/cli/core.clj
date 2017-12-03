(ns apriori.cli.core
  (:require [clojure.string :as string]
            [clj-sub-command.core :refer [sub-command candidate-message]]
            [clojure.tools.cli :as cli]
            [apriori.api.handler :as app]
            [apriori.utlis.apriori :as apriori]
            [apriori.utlis.data-set :as data-set]
            [ring.adapter.jetty :as jetty])
  (:gen-class))

; exit
(defn exit [status msg]
  (println msg)
  (System/exit status))

; run
(defn run [args]
  (let [{:keys [options arguments errors summary]}
        (cli/parse-opts args [["-p" "--project ID" "A project identifier." :parse-fn #(Integer/parseInt %)]
                              ["-h" "--help" "Show help."]])]
    (case options
      :project (->>
                 (get options :project)
                 (apriori/run))
      :help (println summary))))

; data-set
(defn data-set [args]
  (let [{:keys [options arguments errors summary]}
        (cli/parse-opts args [["-u" "--upload FILE" "Upload a json file."]
                              ["-h" "--help" "Show help."]])]

    (cond
      (:upload options) (->>
                          ""
                          (data-set/upload))
      (:help options) (println summary))))

; api
(defn api [args]
  (let [{:keys [options arguments errors summary]}
        (cli/parse-opts args [["-h" "--help" "Show help."]
                              ["-p" "--port PORT" "A port number." :parse-fn #(Integer/parseInt %)]
                              [nil "--start" "Launch an instance of the api."]
                              [nil "--stop" "Terminate an api instance."]])
        port (if (contains? options :port) (get options :port) 3000)]

    (cond
      (:start options) (jetty/run-jetty app/api {:port port})
      (:help options) (println summary)
      :else
      (exit 1 (str "Options not found, try running: api --help\n")))))

; main
(defn -main [& args]
  (let [[opts cmd args help cands]
        (sub-command args
                     :options [["-h" "--help" "Show help." :default false :flag true]]
                     :commands [["api" "Start or stop an instance of the api."]
                                ["data-set" "Manage data-sets."]
                                ["project" "Manage projects."]
                                ["run" "Run the apriori.sh algorithm."]])]

    (when (:help opts)
      (exit 0 help))

    (case cmd
      :api (api args)
      :run (run args)
      :data-set (data-set args)
      (exit 1 (str "Command not found, try running: --help\n" (candidate-message cands))))))

; apriori.sh run --project-id={id}
; apriori.sh data-set --upload={path} --remove-all --remove-before={date-time} -remove-after={date-time}
; apriori.sh api --start --stop
; apriori.sh schedule --schedule={cron-date-time}
; apriori.sh project --create --update==1 --delete==1 --name=test --min-support=0.5 --min-confidence=0.4
; apriori.sh --help
; run-jetty (run-jetty #'engine {:port 8080})