#!/bin/bash
echo "Initializing LocalStack AWS infrastructure..."

# Main Backend Queues
awslocal sqs create-queue --queue-name submission-dlq
awslocal sqs create-queue --queue-name submission-queue

# Sentinel Service Queues
awslocal sqs create-queue --queue-name match-watch-queue
awslocal sqs create-queue --queue-name match-result-queue

# S3 Bucket
awslocal s3 mb s3://testcase-bucket

echo "LocalStack initialization complete!"