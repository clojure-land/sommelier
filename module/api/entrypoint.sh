#!/usr/bin/env bash

start(){
    echo "--> INFO: starting api..."
    java -jar api-*-standalone.jar
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