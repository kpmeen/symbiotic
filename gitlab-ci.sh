#!/bin/bash

cd $CI_PROJECT_DIR/server;

if [ -n "$CODACY_PROJECT_TOKEN" ]; then
  echo "Building from upstream. Codacy reporting enabled.";
  sbt clean coverage test coverageReport codacyCoverage;
else
  echo "Building PR from fork. Codacy reporting disabled.";
  sbt clean coverage test coverageReport;
fi

