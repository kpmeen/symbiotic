#!/bin/sh

cd server;

sbt clean coverage test coverageReport

cd $SHIPPABLE_BUILD_DIR;

