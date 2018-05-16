#!/bin/bash

CURR_DIR=$(pwd)

cd $CURR_DIR/docker/infra

bash ./backends $@

cd $CURR_DIR