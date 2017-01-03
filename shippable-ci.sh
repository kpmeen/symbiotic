#!/bin/sh

cd server;

if [[ -n "$CODACY_PROJECT_TOKEN" ]]; then
  sbt clean coverage test coverageReport codacyCoverage;
else
  echo "Coverage reporting disabled for PR from forks";
  sbt clean coverage test coverageReport;
fi

