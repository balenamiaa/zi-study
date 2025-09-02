(ns backend.routes
  (:require [backend.api.todo :as todo]
            [backend.api.user]
            [backend.api.auth :as auth]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.tools.logging :as log]
            [cognitect.transit :as transit]
            [common.schema :as schema]
            [hiccup2.core :as hiccup]
            [integrant.core :as ig]
            [muuntaja.core :as m]
            [reitit.coercion.malli :as malli.coercion]
            [reitit.ring :as ring]
            [reitit.ring.coercion :as ring.coercion]
            [reitit.ring.middleware.exception]
            [reitit.ring.middleware.muuntaja :as muuntaja]
            [reitit.swagger :as swagger]
            [reitit.swagger-ui :as swagger-ui]
            [ring.middleware.multipart-params :refer [wrap-multipart-params]]
            [ring.middleware.session :refer [wrap-session]]
            [ring.middleware.session.cookie :refer [cookie-store]]
            [ring.util.http-response :as resp]))

(def muuntaja-instance
  (m/create (-> m/default-options
                (assoc-in [:formats "application/transit+json" :encoder-opts :handlers java.time.Instant]
                          (transit/write-handler (constantly "Instant") #(.toString %)))
                (assoc-in [:formats "application/transit+json" :decoder-opts :handlers "Instant"]
                          (transit/read-handler #(java.time.Instant/parse %))))))

(defn wrap-database-middleware
  "Return a middleware that associates our database instance to the request map."
  [handler database]
  (fn
    ([request]
     (handler (assoc request :db database)))
    ([request respond raise]
     (handler (assoc request :db database)
              respond
              raise))))

(defn wrap-env-middleware
  "Attach selected env config to request map."
  [handler env]
  (fn
    ([request]
     (handler (assoc request :env env)))
    ([request respond raise]
     (handler (assoc request :env env) respond raise))))

(defn default-error-handler
  "Default safe handler for any exception."
  [^Exception e _]
  (prn e)
  {:status 500
   :body {:type "exception"
          :class (.getName (.getClass e))}})

(defn main-js-file []
  (-> (or (io/resource "public/js/manifest.edn")
          (io/file "target/dev/public/js/manifest.edn"))
      slurp
      edn/read-string
      first
      :output-name))

(defn ^:private cookie-theme [req]
  (some-> (get-in req [:cookies "theme" :value]) str/lower-case))

(defn ^:private cookie->data-theme [theme]
  (case theme
    "light" "gold_light"
    "dark"  "gold_dark"
    nil))

(defn index [req]
  (let [initial-data-theme (cookie->data-theme (cookie-theme req))]
    (hiccup/html {:mode :html}
                 (hiccup/raw "<!DOCTYPE html>\n")
                 [:html
                  (cond-> {:lang "en"}
                    initial-data-theme (assoc :data-theme initial-data-theme))
                  [:head
                   [:title "Template Title"]
                   [:meta {:charset "utf-8"}]
                   [:meta {:name "viewport" :content "width=device-width, initial-scale=1, shrink-to-fit=no"}]
                   [:link {:rel "icon" :type "image/svg+xml" :href "/css/logo.svg"}]
                   ;; Set theme ASAP before CSS loads to avoid FOUC
                  [:script
                   (hiccup/raw
                    (str
                     "(function(){\n"
                     "  try {\n"
                     "    var existing=document.documentElement.getAttribute('data-theme');\n"
                     "    if(!existing){\n"
                     "      var saved=localStorage.getItem('theme')||'system';\n"
                     "      var prefersDark=window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches;\n"
                     "      var effective=(saved==='system')?(prefersDark?'dark':'light'):saved;\n"
                     "      var name=(effective==='dark')?'gold_dark':'gold_light';\n"
                     "      document.documentElement.setAttribute('data-theme', name);\n"
                     "    }\n"
                     "  } catch(e){}\n"
                     "})();"))]
                   [:link {:rel "stylesheet" :href "/css/main.css"}]
                   [:link {:rel "stylesheet" :href "/css/autofill-fix.css"}]]
                  [:body {:class "bg-base-100 antialiased"}
                   [:div#app
                    ;; Beautiful, theme-aware preloader
                    [:div {:class "min-h-screen flex items-center justify-center relative overflow-hidden"}
                     ;; soft glow background
                     [:div {:class "absolute inset-0 -z-10 pointer-events-none"}
                      [:div {:class "absolute -top-24 -left-24 w-72 h-72 bg-primary/20 rounded-full blur-3xl"}]
                      [:div {:class "absolute -bottom-24 -right-24 w-72 h-72 bg-secondary/20 rounded-full blur-3xl"}]]
                     [:div {:class "text-center space-y-5"}
                      ;; ring loader
                      [:div {:class "relative mx-auto w-24 h-24"}
                       [:div {:class "absolute inset-0 rounded-full bg-gradient-to-tr from-primary to-secondary opacity-80 animate-spin p-[2px]"}
                        [:div {:class "w-full h-full rounded-full bg-base-100"}]]
                       [:div {:class "absolute inset-0 flex items-center justify-center"}
                        [:span {:class "loading loading-spinner loading-lg text-primary"}]]]
                      [:h1 {:class "text-2xl font-bold text-base-content"} "Loading"]
                      [:p {:class "text-base-content/60"} "Preparing your application…"]]]]
                   [:noscript
                    [:div {:class "p-4 text-center text-warning"}
                     "JavaScript is required to run this app."]]
                   [:script {:defer true :src (str "/js/" (main-js-file))}]]])))

(defn app [env]
  (ring/ring-handler
   (ring/router
    ["/api"
     ["/user"
      ["/profile" {:post {:handler #'backend.api.user/update-profile}}]
      ["/avatar" {:post {:handler #'backend.api.user/upload-avatar}}]]
     ["/auth"
      ["/me" {:get {:handler #'auth/me}}]
      ["/register" {:post {:handler #'auth/register}}]
      ["/login" {:post {:handler #'auth/login}}]
      ["/logout" {:post {:handler #'auth/logout}}]
      ["/google/start" {:get {:handler #'auth/google-start}}]
      ["/google/callback" {:get {:handler #'auth/google-callback}}]]
     ["/todo"
      [""
       {:summary "Return a list of todo items"
        :get {:handler #'todo/get-todos
              :responses {200 {:body [:sequential schema/todo]}}}
        :post {:handler #'todo/create-todo
               :responses {200 {:body schema/todo}}
               :parameters {:body schema/new-todo}}}]
      ["/:id"
       {:parameters {:path [:map [:id :int]]}
        :put {:parameters {:body schema/update-todo}
              :responses {200 {:body schema/todo}}
              :handler #'todo/update-todo}
        :delete {:handler #'todo/delete-todo}}]]
     ["/swagger.json" {:no-doc true
                       :get (swagger/create-swagger-handler)}]
     ["/docs/*" {:no-doc true
                 :get (swagger-ui/create-swagger-ui-handler {:url "/api/swagger.json"})}]]
    {:data {:muuntaja muuntaja-instance
            :coercion malli.coercion/coercion
            :middleware [muuntaja/format-middleware
                         (reitit.ring.middleware.exception/create-exception-middleware {:reitit.ring.middleware.exception/default default-error-handler})
                         ring.coercion/coerce-exceptions-middleware
                         ring.coercion/coerce-request-middleware
                         ring.coercion/coerce-response-middleware
                         (fn [handler] (wrap-multipart-params handler))
                         [wrap-database-middleware (:db env)]
                         [wrap-env-middleware (:env env)]
                         ;; session cookie store (signed). Provide a real secret in env.
                         (fn [handler]
                           (let [cfg (:env env)]
                             (wrap-session handler {:store (cookie-store {:key (.getBytes (get-in cfg [:session :secret]) "UTF-8")
                                                                           :readers {} :writers {}})
                                                   :cookie-name (or (get-in cfg [:session :cookie-name]) "sid")
                                                   :cookie-attrs {:http-only true :same-site :lax}})))]}})

  ;; Default handler - handle resources (js files), index.html and 404 for API endpoints
  (ring/routes
    (ring/create-resource-handler {:path ""
                                   :root "public"})
    ;; Serve uploaded files from configurable uploads root at /uploads
    (let [uploads-root (or (get-in (:env env) [:uploads :root]) "uploads")]
      (ring/create-file-handler {:path "/uploads"
                                 :root uploads-root}))
    (ring/ring-handler
     (ring/router
      [""
       ["/api/*" {:handler (fn [_req]
                             (resp/not-found))}]
       ;; Return index.html for any non-API routes for History API routing
       ["/*" {:get {:handler (fn [req] (resp/ok (str (index req))))}}]]
      {:conflicts nil})))))

(defmethod ig/init-key :web/routes [_ env]
  (app env))

(defmethod ig/halt-key! :web/routes [_ _routes]
  (log/info "Shutting down web routes"))
