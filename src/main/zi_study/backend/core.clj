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
            [ring.middleware.content-type :refer [wrap-content-type]]
            [ring.middleware.not-modified :refer [wrap-not-modified]]
            [ring.middleware.multipart-params :as multipart-params]
            [ring.util.response :as resp]
            [ring.middleware.reload :refer [wrap-reload]]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [ring.middleware.nested-params :refer [wrap-nested-params]]
            [zi-study.backend.db :as db]
            [zi-study.backend.auth :as auth]
            [zi-study.backend.uploads :as uploads]
            [zi-study.backend.handlers.active-learning-handlers :as alh]
            [clojure.string :as str]
            [clojure.java.shell :as shell]))

(defn spa-handler [request]
  (if (str/starts-with? (:uri request) "/api")
    (resp/not-found "Not Found")
    (let [response (resp/file-response "index.html" {:root "public"})]
      (-> response
          (resp/content-type "text/html")
          (assoc-in [:headers "Content-Disposition"] "inline")))))

(defn not-found-handler [_request]
  (-> (resp/not-found "Not Found by Ring backend.")
      (resp/content-type "text/plain")))

(defn dev-mode? []
  (= "development" (or (System/getProperty "JVM_ENV") "development")))

(def routes
  [["/api/auth" {}
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
    ["/folders" {}
     ["" {:post {:handler alh/create-folder-handler}
          :get {:handler alh/list-user-folders-handler}}]
     ["/public" {:get {:handler alh/list-public-folders-handler}}]
     ["/folder/:folder-id" {}
      ["" {:get {:handler alh/get-folder-details-handler}
           :put {:handler alh/update-folder-handler}
           :delete {:handler alh/delete-folder-handler}}]
      ["/sets" {:post {:handler alh/add-set-to-folder-handler}
                :put {:handler alh/reorder-sets-in-folder-handler}}]
      ["/sets/:set-id" {:delete {:handler alh/remove-set-from-folder-handler}}]]]
    ["/tags" {:get {:handler alh/list-tags-handler}}]
    ["/question-sets" {:get {:handler alh/list-sets-handler}}]
    ["/question-sets/:set-id"
     ["" {:get {:handler alh/get-set-details-handler}}]
     ["/questions" {:get {:handler alh/get-set-questions-handler}}]
     ["/answers" {:delete {:handler alh/delete-set-answers-handler}}]]
    ["/questions" {}
     ["/search" {:get {:handler alh/search-questions-handler}}]]
    ["/questions/:question-id" {}
     ["/answer" {:post {:handler alh/submit-answer-handler}
                 :delete {:handler alh/delete-answer-handler}}]
     ["/self-evaluate" {:post {:handler alh/self-evaluate-handler}}]
     ["/bookmark" {:post {:handler alh/toggle-bookmark-handler}}]]
    ["/bookmarks" {:get {:handler alh/list-bookmarks-handler}}]]])

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
       (if (dev-mode?)
         (ring/routes
          (ring/create-file-handler {:path "/public" :root "public"})
          spa-handler)
         not-found-handler))
      (wrap-content-type)
      (wrap-not-modified)))

(defonce server-state (atom nil))
(defonce shadow-state (atom nil))

(declare stop-server)

(defn run-postcss [& args]
  (prn)
  (let [result (apply shell/sh "cmd" "/c" "npx" "postcss" "public/app.input.css" "-o" "public/app.output.css" args)]
    (println "PostCSS result:" result)))

(defn run-postcss-prod [& args]
  (apply run-postcss :env (merge (into {} (System/getenv)) {"NODE_ENV" "production"}) args))


(defn start-server [port]
  (println "Initializing backend...")

  (db/init-db!)

  (if (dev-mode?)
    (try
      (shadow-server/start!)
      (shadow/watch :frontend)
      (reset! shadow-state true)
      (println "Development mode: Shadow-CLJS watcher started")
      (catch Exception e
        (println "Error starting shadow-cljs:" (ex-message e))))

    (try
      (run-postcss-prod)
      (shadow/release :frontend)
      (println "Shadow-CLJS release-mode build complete")
      (catch Exception e
        (println "Error starting shadow-cljs:" (ex-message e)))))

  (println (str "Hosted on http://localhost:" port))

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
      (stop-server)
      (throw e)))

  @server-state)

(defn stop-server []
  (println "Shutting down backend...")

  (when-let [server-instance @server-state]
    (println "Stopping http-kit server...")
    (try
      (server-instance :timeout 100)
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

  (db/close-db!)

  (println "Backend shutdown complete."))


(comment
  (start-server 3000)

  (stop-server)

  (System/setProperty "JVM_ENV" "production")

  (System/setProperty "JVM_ENV" "development")
  (System/setProperty "NODE_ENV" "development"))