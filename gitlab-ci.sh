#!/bin/bash

cd $CI_PROJECT_DIR/server;

sbt coverage test coverageReport

if [ -n "$CODACY_PROJECT_TOKEN" ]; then
  echo "Building from upstream. Codacy reporting enabled.";
   sbt coverageAggregate codacyCoverage;
else
  echo "Building PR from fork. Codacy reporting disabled.";
  sbt coverageAggregate;
fi

