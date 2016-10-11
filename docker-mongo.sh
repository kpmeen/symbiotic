#!/bin/bash

CURR_DIR=$(pwd)

docker run --name symbiotic-mongo -p 27017:27017 -v $CURR_DIR/.mongodb-files:/data/db -d mongo --replSet symbiotic-repl --storageEngine wiredTiger
