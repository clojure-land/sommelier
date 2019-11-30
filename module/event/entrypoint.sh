#!/usr/bin/env bash

start(){
    echo "--> INFO: starting event consumer..."
    java -jar event-*-standalone.jar
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