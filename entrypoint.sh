#!/usr/bin/env bash

start(){
    echo "--> INFO: starting api..."
    java -jar apriori-0.1.0-standalone.jar api --start --port=3000
}

case $1 in

run)
    shift 1
    start $@
;;

*)
   >&2 echo "---> INFO: running: '$1'."
;;
esac