@echo off
REM Starts the Clojure nREPL server

REM Set the nREPL port if you want to use a different one
SET NREPL_PORT=7888
SET NODE_ENV=production
SET WEBSERVER_PORT=5000

clojure -J-DJVM_ENV=%JVM_ENV% -J-DNODE_ENV=%NODE_ENV% -M -m zi-study.repl %WEBSERVER_PORT%