#!/bin/sh
# Starts the Clojure nREPL server

# Set the nREPL port if you want to use a different one
export NREPL_PORT=7888
export JVM_ENV=production
export NODE_ENV=production
export WEBSERVER_PORT=5000

clojure -J-DJVM_ENV=%JVM_ENV% -J-DNODE_ENV=%NODE_ENV% -M -m zi-study.repl $WEBSERVER_PORT