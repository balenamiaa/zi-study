(ns zi-study.backend.core
  (:require [shadow.cljs.devtools.api :as shadow]
            [shadow.cljs.devtools.server :as shadow-server]
            [org.httpkit.server :as server]
            [reitit.ring :as ring]
            [reitit.ring.middleware.parameters :refer [parameters-middleware]]
            [reitit.ring.middleware.muuntaja :as muuntaja]
            [reitit.ring.coercion :as coercion]
            [reitit.coercion.spec]
            [muuntaja.core :as m]
            [ring.middleware.file :refer [wrap-file]]
            [ring.middleware.content-type :refer [wrap-content-type]]
            [ring.middleware.not-modified :refer [wrap-not-modified]]
            [ring.util.response :as resp]
            [ring.middleware.reload :refer [wrap-reload]]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [ring.middleware.nested-params :refer [wrap-nested-params]]))

(defn hello-handler [req]
  {:status 200
   :body {:message "Hello from the other side!"}})

(defn echo-handler [{{:keys [message]} :path-params}]
  {:status 200
   :body {:echo message}})

(defn index-handler [_]
  (let [response (resp/file-response "index.html" {:root "public"})]
    (-> response
        (resp/content-type "text/html")
        (assoc-in [:headers "Content-Disposition"] "inline"))))

(def routes
  [["/" {:get index-handler}]
   ["/api/hello" {:get hello-handler}]
   ["/api/echo/:message" {:get echo-handler}]])


(def app
  (-> (ring/ring-handler
       (ring/router
        routes
        {:data {:muuntaja m/instance
                :middleware [parameters-middleware
                             muuntaja/format-middleware
                             coercion/coerce-exceptions-middleware
                             coercion/coerce-request-middleware
                             coercion/coerce-response-middleware
                             wrap-keyword-params
                             wrap-nested-params]}})
       ; default handler
       index-handler)
      (wrap-file "public" {:index-files? false})
      (wrap-content-type)
      (wrap-not-modified)))

(defonce server-state (atom nil))
(defonce shadow-state (atom nil))

(defn start-server [port]
  (println "Starting server on port" port)
  (println "Serving static files from public directory")
  (println "Visit http://localhost:" port " to access the application")

  (println "Starting shadow-cljs server and watcher...")
  (shadow-server/start!)
  (shadow/watch :frontend)
  (reset! shadow-state true)

  (println "Starting http-kit server...")
  (reset! server-state (server/run-server (wrap-reload #'app) {:port port}))

  @server-state)


(defn stop-server []
  (when @server-state
    (println "Stopping http-kit server...")
    (@server-state :timeout 100)
    (reset! server-state nil))

  (when @shadow-state
    (println "Stopping shadow-cljs server and watcher...")
    (shadow-server/stop!)
    (reset! shadow-state nil)))


(defn -main [& args] ; Use -main for gen-class entry point
  (let [port (or (some-> (first args) (Integer/parseInt)) 3000)]
    (start-server port)))

;; Manual reload
(defn reload []
  (println "\n--- Reloading backend code ---")
  ;; Stop existing servers
  (stop-server)
  ;; Re-require the namespace to pick up code changes
  (require 'zi-study.backend.core :reload)
  ;; Restart servers using the reloaded code
  (-main)) 