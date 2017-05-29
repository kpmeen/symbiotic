#!/bin/bash

docker run --name symbiotic-postgres -p 5432:5432 -e POSTGRES_PASSWORD=postgres -d postgres