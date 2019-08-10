(defproject job "0.1.0"
    :dependencies [
                   [org.clojure/clojure "1.9.0"]
                   [org.clojure/tools.cli "0.3.5"]
                   [org.clojure/tools.logging "0.4.0"]
                   [log4j/log4j "1.2.17" :exclusions [javax.mail/mail
                                                      javax.jms/jms
                                                      com.sun.jmdk/jmxtools
                                                      com.sun.jmx/jmxri]]
                   [com.novemberain/monger "3.5.0"]
                   [org.slf4j/slf4j-log4j12 "1.7.25"]
                   [amazonica "0.3.145"]
                   [cheshire "5.8.0"]
                   ]

    :main core

    :source-paths ["src"]

    :resource-paths ["resources"]

    :profiles {:default [:local]
               :uberjar {:aot :all}
               :local   {:plugins [[lein-kibit "0.1.6"]]}
               }
)