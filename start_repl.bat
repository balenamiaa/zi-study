@echo off
REM Starts the Clojure nREPL server

REM Set the nREPL port if you want to use a different one
SET NREPL_PORT=7888
SET JVM_ENV=production
set NODE_ENV=production
SET WEBSERVER_PORT=5000

clojure -M -m zi-study.repl %WEBSERVER_PORT%