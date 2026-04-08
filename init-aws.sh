#!/bin/bash
echo "Initializing LocalStack AWS infrastructure..."

awslocal sqs create-queue --queue-name submission-dlq
awslocal sqs create-queue --queue-name submission-queue

awslocal sqs create-queue --queue-name match-watch-queue
awslocal sqs create-queue --queue-name match-result-queue

awslocal s3 mb s3://testcase-bucket

echo "LocalStack initialization complete!"