#!/bin/bash

ARG=$1

function clean {
  echo "Removing containers and data folders..."
  docker rm symbiotic-mongo symbiotic-postgres
  rm -rf ./.mongodb-files
}
function stop {
  echo "Stopping backend containers..."
  docker stop symbiotic-mongo
  docker stop symbiotic-postgres
  echo "Backend containers stopped."
}

function start {
  MONGODB_EXISTS=$( docker ps --quiet --filter name=symbiotic-mongo )
  PGSQL_EXISTS=$( docker ps --quiet --filter name=symbiotic-postgres )

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

  echo "Backend containers started."
}

if [ "$ARG" == "start" ]; then
  start

elif [ "$ARG" == "stop" ]; then
  stop

elif [ "$ARG" == "clean" ]; then
    stop
    clean
fi