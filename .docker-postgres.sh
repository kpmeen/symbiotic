#!/bin/bash

echo "Starting Postgres docker container..."

docker run --name symbiotic-postgres -p 5432:5432 -e POSTGRES_PASSWORD=postgres -d postgres

echo "Postgres docker init completed..."