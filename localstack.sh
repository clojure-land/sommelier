#!/usr/bin/env bash

create_local_stack_resources(){
    sleep 10 # sleep for 10s because local stack is sometimes slow to start.

    echo "--> INFO: creating local stack resources..."

    aws --endpoint-url=http://localstack:4576 --region=us-east-1 sqs create-queue --queue-name "sommelier-apriori" --attributes '{"VisibilityTimeout":"300"}'

    aws --endpoint-url=http://localstack:4576 --region=us-east-1 sqs create-queue --queue-name "sommelier-event" --attributes '{"VisibilityTimeout":"300"}'

    echo "--> INFO: sqs:"
    aws --endpoint-url=http://localstack:4576 sqs list-queues --region=us-east-1
}

case $1 in

run) # invoked by docker-compose awscli image.
    shift 1

    create_local_stack_resources

    tail -f /dev/null
;;

*)
   >&2 echo "---> INFO: running: '$1'."
;;

esac