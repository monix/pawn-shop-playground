#!/usr/bin/env bash

set -e

echo "Stopping all docker containers..."
docker rm $(docker stop $(docker ps -aq))

echo -e "Docker ps."
docker ps

