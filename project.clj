(defproject apriori "0.1.0"
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/tools.logging "0.4.0"]
                 [org.clojure/tools.cli "0.3.5"]
                 [org.clojure/data.json "0.2.6"]
                 [org.clojure/math.combinatorics "0.1.4"]
                 [org.postgresql/postgresql "9.4-1201-jdbc41"]
                 [org.clojure/tools.logging "0.4.0"]
                 [org.slf4j/slf4j-log4j12 "1.7.25"]
                 [log4j/log4j "1.2.17" :exclusions [javax.mail/mail
                                                    javax.jms/jms
                                                    com.sun.jmdk/jmxtools
                                                    com.sun.jmx/jmxri]]
                 [metosin/compojure-api "1.1.11"]
                 [metosin/ring-swagger "0.24.3"]
                 [compojure "1.6.0"]
                 [cheshire "5.8.0"]
                 [clj-time "0.14.2"]
                 [pandect "0.6.1"]
                 [crypto-random "1.2.0"]
                 [ring "1.6.3"]
                 [ring/ring-json "0.4.0"]
                 [ring-logger "0.7.7"]
                 [environ "1.1.0"]
                 [korma "0.4.3"]
                 [blackwater "0.0.9"]
                 [buddy "2.0.0"]
                 [prismatic/schema "1.1.7"]
                 [siili/humanize "0.1.1"]
                 [clj-http "3.7.0"]]

  :main apriori.cli.core

  :test-paths ["test"]

  :profiles {:uberjar {:aot :all}

             :user    {
                       :plugins
                       [
                              [lein-kibit "0.1.6"]
                              [nightlight/lein-nightlight "1.0.0"]
                              [jonase/eastwood "0.2.3"]
                              [lein-cloverage "1.0.9"]]
                       }

             :dev     {
                       :dependencies [
                                      [ring-mock "0.1.3"]
                                      [clj-http "3.7.0"]
                                      [org.clojure/data.json "0.2.6"]
                                      [clj-time "0.14.2"]
                                      [listora/again "0.1.0"]
                                      [environ "1.1.0"]
                                      ]
                       }
             })