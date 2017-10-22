#!/bin/bash

VOLUME_DIR_NAME=".mongodb-files"
CURR_DIR=$(pwd)
DB_VOLUME_PATH="$CURR_DIR/$VOLUME_DIR_NAME"

echo "Checking for $VOLUME_DIR_NAME..."

if [ -d $DB_VOLUME_PATH ]
then
  echo "Found $VOLUME_DIR_NAME."
else
  echo "Could not find $VOLUME_DIR_NAME. Creating..."
  mkdir $DB_VOLUME_PATH
  echo "$VOLUME_DIR_NAME created."

  echo "IMPORTANT: This will be the first time MongoDB is started. Remember to"
  echo "initialise the replica set! This script will attempt to initialize it"
  echo "for you. If it should fail, you try doing it manually with these steps:"
  echo "  1. docker exec -it symbiotic-mongo mongo"
  echo "  2. rs.initiate() - will init the replica set"
  echo "  3. rs.status() - to verify the replica set is created"
  echo ""
fi

echo "Will use $DB_VOLUME_PATH as a mounted volume when starting Docker."
echo "Exposing MongoDB default port on 27017 on host machine."

echo "Starting MongoDB docker container..."
docker pull mongo:latest
docker run --name symbiotic-mongo -p 27017:27017 -v $DB_VOLUME_PATH:/data/db -d mongo --replSet symbiotic-repl --storageEngine wiredTiger --quiet

echo "Initializing..."
sleep 10s;

echo "Verifying replica set..."
docker exec symbiotic-mongo mongo --eval "printjson(rs.initiate())"
docker exec symbiotic-mongo mongo --eval "printjson(rs.status())"

echo "MongoDB docker init completed..."
