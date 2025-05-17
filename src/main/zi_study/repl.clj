(ns zi-study.repl
  (:require [nrepl.server :as nrepl]
            [cider.nrepl :as cider-nrepl-mw]
            [clojure.tools.logging :as log]))

(defonce nrepl-server (atom nil))
(defonce passed-args (atom nil))

(defn start-nrepl
  "Starts an nREPL server on a configured port with CIDER middleware."
  []
  (if @nrepl-server
    (log/info "nREPL server already running.")
    (try
      (let [port (Integer/parseInt (or (System/getenv "NREPL_PORT") "7888"))
            server (nrepl/start-server :port port :handler cider-nrepl-mw/cider-nrepl-handler)]
        (reset! nrepl-server server)
        (log/info (str "nREPL server with CIDER middleware started on port " port))
        (println (str "nREPL server (CIDER friendly) started on port " port ". Connect via your editor.")))
      (catch Exception e
        (log/error e "Failed to start nREPL server")
        (println "Failed to start nREPL server:" (ex-message e))))))

(defn stop-nrepl
  "Stops the running nREPL server."
  []
  (if-let [server @nrepl-server]
    (do
      (nrepl/stop-server server)
      (reset! nrepl-server nil)
      (log/info "nREPL server stopped.")
      (println "nREPL server stopped."))
    (log/info "nREPL server not running.")))

(defn -main
  [& args]
  (reset! passed-args args)
  (start-nrepl)
  ;; Add a shutdown hook to stop the nREPL server gracefully
  (.addShutdownHook (Runtime/getRuntime) (Thread. #'stop-nrepl))
  ;; Keep the main thread alive
  @(promise))

(comment
  ;; To start the server manually from a REPL
  (start-nrepl)

  ;; To stop it
  (stop-nrepl))