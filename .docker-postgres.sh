#!/bin/bash

echo "Starting Postgres docker container..."
docker pull registry.gitlab.com/kpmeen/docker-postgres-test
docker run --name symbiotic-postgres -p 5432:5432 -e POSTGRES_PASSWORD=postgres -d registry.gitlab.com/kpmeen/docker-postgres-test

echo "Postgres docker init completed..."