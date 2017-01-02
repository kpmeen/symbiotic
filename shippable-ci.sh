#!/bin/sh

cd server;

sbt clean coverage test coverageReport

