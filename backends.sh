#!/bin/bash

ARG=$1

# Clean docker repo and folders
function clean {
  echo "Removing containers and data folders..."
  docker rm symbiotic-mongo symbiotic-postgres symbiotic-elasticsearch
  rm -rf ./.mongodb-files
  rm -rf ./dman
  rm -rf ./examples/symbiotic-server/dman
}
# Stop docker
function stop {
  echo "Stopping backend containers..."
  docker stop symbiotic-mongo
  docker stop symbiotic-postgres
  docker stop symbiotic-elasticsearch
  echo "Backend containers stopped."
}

function status {
  MONGODB_EXISTS=$( docker ps --quiet --filter name=symbiotic-mongo )
  PGSQL_EXISTS=$( docker ps --quiet --filter name=symbiotic-postgres )
  ELASTIC_EXISTS=$( docker ps --quiet --filter name=symbiotic-elasticsearch )

  if [[ -n "$MONGODB_EXISTS" ]]; then
    echo -e "MongoDB       \033[1;32m up\033[0;0m"
  else
    echo -e "MongoDB       \033[1;31m down\033[0;0m"
  fi
  if [[ -n "$PGSQL_EXISTS" ]]; then
    echo -e "PostgreSQL    \033[1;32m up\033[0;0m"
  else
    echo -e "PostgreSQL    \033[1;31m down\033[0;0m"
  fi
  if [[ -n "$ELASTIC_EXISTS" ]]; then
    echo -e "ElasticSearch \033[1;32m up\033[0;0m"
  else
    echo -e "ElasticSearch \033[1;31m down\033[0;0m"
  fi
}

# Init and/or start docker containers
function start {
  MONGODB_EXISTS=$( docker ps --quiet --filter name=symbiotic-mongo )
  PGSQL_EXISTS=$( docker ps --quiet --filter name=symbiotic-postgres )
  ELASTIC_EXISTS=$( docker ps --quiet --filter name=symbiotic-elasticsearch )

  # Try to start a MongoDB container
  if [[ -n "$MONGODB_EXISTS" ]]; then
    echo "Starting MongoDB..."
    docker start symbiotic-mongo
  else
    source ./.docker-mongo.sh
  fi

  # Try to start a Postgres container
  if [[ -n "$PGSQL_EXISTS" ]]; then
    echo "Starting Postgres..."
    docker start symbiotic-postgres
  else
    source ./.docker-postgres.sh
  fi

  if [[ -n "$ELASTIC_EXISTS" ]]; then
    echo "Starting ElasticSearch..."
    docker start symbiotic-elasticsearch
  else
    source ./.docker-elasticsearch.sh
  fi
}

function reset {
  stop
  clean
  start
}

if [ "$ARG" == "start" ]; then
  start
  status

elif [ "$ARG" == "stop" ]; then
  stop
  status

elif [ "$ARG" == "clean" ]; then
  stop
  clean

elif [ "$ARG" == "reset" ]; then
  reset
  status

elif [ "$ARG" == "status" ]; then
  echo "Backend container status:"
  status

fi
