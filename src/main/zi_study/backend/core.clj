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
            [ring.middleware.multipart-params :as multipart-params]
            [ring.util.response :as resp]
            [ring.middleware.reload :refer [wrap-reload]]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [ring.middleware.nested-params :refer [wrap-nested-params]]
            [ring.middleware.gzip :as gzip]
            [zi-study.backend.db :as db]
            [zi-study.backend.auth :as auth]
            [zi-study.backend.uploads :as uploads]
            [zi-study.backend.handlers.question-bank-handlers :as qb]
            [clojure.string :as str]))



(defn index-handler [_]
  (let [response (resp/file-response "index.html" {:root "public"})]
    (-> response
        (resp/content-type "text/html")
        (assoc-in [:headers "Content-Disposition"] "inline"))))


(defn spa-handler [request]
  (if (str/starts-with? (:uri request) "/api")
    (resp/not-found "Not Found")
    (let [response (resp/file-response "index.html" {:root "public"})]
      (-> response
          (resp/content-type "text/html")
          (assoc-in [:headers "Content-Disposition"] "inline")))))

(def routes
  [["/" {:get index-handler}]
   ["/api/auth" {}
    ["/register" {:post {:handler auth/register-handler
                         :middleware [parameters-middleware
                                      muuntaja/format-request-middleware
                                      wrap-keyword-params]}}]
    ["/login"    {:post {:handler auth/login-handler}}]
    ["/logout"   {:post {:handler auth/logout-handler}}]
    ["/me"       {:get {:handler auth/current-user-handler
                        :middleware [auth/wrap-authentication]}}]]
   ["/api/uploads" {}
    ["/profile-picture" {:post {:handler uploads/profile-picture-upload-handler
                                :middleware [multipart-params/wrap-multipart-params]}}]]
   ["/api" {:middleware [auth/wrap-authentication]}
    ["/tags" {:get {:handler qb/list-tags-handler}}]
    ["/question-sets" {:get {:handler qb/list-sets-handler}}]
    ["/question-sets/:set-id"
     ["" {:get {:handler qb/get-set-details-handler}}]
     ["/questions" {:get {:handler qb/get-set-questions-handler}}]
     ["/answers" {:delete {:handler qb/delete-set-answers-handler}}]]
    ["/questions" {}
     ["/search" {:get {:handler qb/search-questions-handler}}]]
    ["/questions/:question-id" {}
     ["/answer" {:post {:handler qb/submit-answer-handler}
                 :delete {:handler qb/delete-answer-handler}}]
     ["/self-evaluate" {:post {:handler qb/self-evaluate-handler}}]
     ["/bookmark" {:post {:handler qb/toggle-bookmark-handler}}]]
    ["/bookmarks" {:get {:handler qb/list-bookmarks-handler}}]]])


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
       (ring/routes
        (ring/create-file-handler {:path "/" :root "public"})
        ; use spa-handler for all unmatched routes
        spa-handler))
      (wrap-file "public" {:index-files? false})
      (wrap-content-type)
      (wrap-not-modified)
      gzip/wrap-gzip))

(defonce server-state (atom nil))
(defonce shadow-state (atom nil))

; Forward declare stop-server
(declare stop-server)


(comment
  (System/setProperty "JVM_ENV" "production")
  (dev-mode?))

(defn dev-mode? []
  (= "development" (or (System/getProperty "JVM_ENV") "development")))

(defn start-server [port]
  (println "Initializing backend...")

  ; Initialize DB first
  (db/init-db!)

  (println "Starting shadow-cljs server and watcher...")
  (try
    (when (dev-mode?)
      (shadow-server/start!)
      (shadow/watch :frontend)
      (reset! shadow-state true)
      (println "Development mode: Shadow-CLJS watcher started"))
    (catch Exception e
      (println "Error starting shadow-cljs:" (ex-message e))))

  (println "Starting server on port" port)
  (println "Serving static files from public directory")
  (println "Visit http://localhost:" port " to access the application")

  (println "Starting http-kit server...")
  (try
    (let [server-instance (if (dev-mode?)
                            (do
                              (println "Development mode: Using wrap-reload middleware")
                              (server/run-server (wrap-reload #'app) {:port port}))
                            (server/run-server app {:port port}))]
      (reset! server-state server-instance)
      (println "HTTP Server started."))
    (catch Exception e
      (println "Error starting http-kit server:" (ex-message e))
      ; Attempt to stop already started components if server fails
      (stop-server) ; Use the declared stop-server
      (throw e)))

  @server-state)

; Define stop-server implementation
(defn stop-server []
  (println "Shutting down backend...")
  ; Stop server first to prevent new requests
  (when-let [server-instance @server-state]
    (println "Stopping http-kit server...")
    (try
      (server-instance :timeout 100) ; Call the stop function returned by run-server
      (catch Exception e
        (println "Error stopping http-kit server:" (ex-message e))))
    (reset! server-state nil))

  (when @shadow-state
    (println "Stopping shadow-cljs server and watcher...")
    (try
      (shadow-server/stop!)
      (catch Exception e
        (println "Error stopping shadow-cljs:" (ex-message e))))
    (reset! shadow-state nil))

  ; Close DB pool last
  (db/close-db!)

  (println "Backend shutdown complete."))


(defn -main [& args]
  (let [port (or (some-> (first args) (Integer/parseInt)) 3000)]
    (try
      (start-server port)
      ; Now stop-server is declared before its use here
      (.addShutdownHook (Runtime/getRuntime) (Thread. #'stop-server))
      (println "Backend started successfully.")
      (catch Exception e
        (println "Failed to start backend:" (ex-message e))
        (System/exit 1)))))


(comment
  (-main))