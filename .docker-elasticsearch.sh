#!/bin/bash

echo "Starting ElasticSearch docker container..."

docker run --name symbiotic-elasticsearch -p 9200:9200 -d kpmeen/docker-elasticsearch:latest

echo "ElasticSearch docker init completed..."
