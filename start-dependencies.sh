#!/usr/bin/env bash

set -e

CURRENT_DIR=$(pwd)
echo "CURRENT_DIR=$CURRENT_DIR"

function create_topic {
    TOPIC_NAME=$1
    PARTITIONS=$2
    REPLICATION_FACTOR=$3
    echo "Creating topic ${TOPIC_NAME} with ${PARTITIONS} partitions and replication factor of ${REPLICATION_FACTOR}."
    docker-compose -f ./docker-compose.yml exec -T broker kafka-topics --create --topic ${TOPIC_NAME} --partitions ${PARTITIONS} --replication-factor ${REPLICATION_FACTOR} --if-not-exists --zookeeper zookeeper:2181
}

# docker-compose -f ./docker-compose.yml stop minio
# docker-compose -f ./docker-compose.yml rm -f minio

echo "Starting MongoDb..."
docker-compose -f docker-compose.yml up -d mongo

echo "Starting Kafka cluster..."
docker-compose -f ./docker-compose.yml up -d zookeeper broker

sleep 30

create_topic items 1 1
create_topic buy-events 1 1
create_topic sell-events 1 1
create_topic pawn-events 1 1

echo "Building docker images."

echo "Building and starting Dispatcher container...."
sbt 'dispatcher/docker:publishLocal'
docker-compose -f ./docker-compose.yml up -d dispatcher

echo "Building and starting Dispatcher container...."
sbt 'worker/docker:publishLocal'
docker-compose -f ./docker-compose.yml up -d worker

# mkdir ./minio/data/mini-monix-platform/feeder-data/
# cp ./feeder/src/test/resources/fraudsters.txt ./minio/data/mini-monix-platform/feeder-data/fraudsters.txt



echo -e "Docker ps."
docker ps

