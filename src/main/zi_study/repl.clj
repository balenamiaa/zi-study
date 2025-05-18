(ns zi-study.repl
  (:require [nrepl.server :as nrepl]
            [cider.nrepl :as cider-nrepl-mw]
            [clojure.tools.logging :as log]
            [zi-study.backend.core :as backend-core]))

(defonce nrepl-server (atom nil))
(defonce passed-args (atom nil))

(defn start-servers
  "Starts an nREPL server and the main web server."
  [web-server-port]
  (if @nrepl-server
    (log/info "nREPL server already running.")
    (try
      (let [nrepl-port (Integer/parseInt (or (System/getenv "NREPL_PORT") "7888"))
            server (nrepl/start-server :port nrepl-port :handler cider-nrepl-mw/cider-nrepl-handler)]
        (reset! nrepl-server server)
        (log/info (str "nREPL server with CIDER middleware started on port " nrepl-port))
        (println (str "nREPL server (CIDER friendly) started on port " nrepl-port ". Connect via your editor.")))
      (catch Exception e
        (log/error e "Failed to start nREPL server")
        (println "Failed to start nREPL server:" (ex-message e)))))

  ;; Start the web server
  (try
    (log/info (str "Attempting to start web server on port " web-server-port))
    (backend-core/start-server web-server-port) ;; Call backend-core to start web server
    (log/info (str "Web server started or start attempt initiated on port " web-server-port))
    (catch Exception e
      (log/error e (str "Failed to start web server on port " web-server-port))
      (println (str "Failed to start web server on port " web-server-port ":" (ex-message e))))))

(defn stop-servers
  "Stops the running nREPL server and the web server."
  []
  (when-let [server @nrepl-server]
    (nrepl/stop-server server)
    (reset! nrepl-server nil)
    (log/info "nREPL server stopped.")
    (println "nREPL server stopped."))
  (when-not @nrepl-server ; Ensure log message if already stopped or never started
    (log/info "nREPL server was not running or already stopped."))

  ;; Stop the web server
  (try
    (log/info "Attempting to stop web server.")
    (backend-core/stop-server) ;; Call backend-core to stop web server
    (log/info "Web server stopped or stop attempt initiated.")
    (catch Exception e
      (log/error e "Failed to stop web server")
      (println "Failed to stop web server:" (ex-message e)))))

(defn -main
  [& args]
  (reset! passed-args args)
  (let [web-port (if (first args)
                   (try
                     (Integer/parseInt (first args))
                     (catch NumberFormatException _
                       (log/warn (str "Invalid port specified: " (first args) ". Defaulting to 3000."))
                       3000))
                   3000)]
    (log/info (str "Resolved web server port to: " web-port))
    (start-servers web-port))
  ;; Add a shutdown hook to stop the servers gracefully
  (.addShutdownHook (Runtime/getRuntime) (Thread. #'stop-servers))
  ;; Keep the main thread alive
  @(promise))

