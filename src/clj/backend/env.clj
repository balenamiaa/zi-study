(ns backend.env
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.tools.logging :as log]
            [integrant.core :as ig]))

(defn- deep-merge
  [& xs]
  (letfn [(dm [a b]
            (if (map? b)
              (merge-with (fn [x y]
                            (if (and (map? x) (map? y))
                              (dm x y)
                              y))
                          a b)
              a))]
    (reduce dm {} xs)))

(defn- read-secret-file []
  (let [from-env (System/getenv "SECRET_EDN_PATH")
        root-file (io/file "secret.edn")
        res (io/resource "secret.edn")]
    (cond
      (some? from-env)
      (let [f (io/file from-env)]
        (if (.exists f)
          (try (edn/read-string (slurp f))
               (catch Throwable t
                 (log/warn t (str "Failed to read SECRET_EDN_PATH=" from-env "; ignoring"))
                 nil))
          (do (log/warn "SECRET_EDN_PATH set but file not found:" from-env) nil)))

      (.exists root-file)
      (try (edn/read-string (slurp root-file))
           (catch Throwable t
             (log/warn t "Failed to read secret.edn in project root; ignoring")
             nil))

      res
      (do (log/warn "Loading secret.edn from classpath (not recommended for production)")
          (try (edn/read-string (slurp res))
               (catch Throwable t
                 (log/warn t "Failed to read classpath secret.edn; ignoring")
                 nil)))

      :else nil)))

(defn- env-vars []
  (let [get (fn [k] (System/getenv k))]
    {:session {:secret (get "SESSION_SECRET")
               :cookie-name (get "SESSION_COOKIE_NAME")}
     :google-oauth {:client-id (get "GOOGLE_CLIENT_ID")
                    :client-secret (get "GOOGLE_CLIENT_SECRET")
                    :redirect-uri (get "GOOGLE_REDIRECT_URI")
                    :scope (get "GOOGLE_OAUTH_SCOPE")}
     :uploads {:root (get "UPLOADS_ROOT")}}))

(def defaults
  {:session {:secret "dev-secret-16b!!"
             :cookie-name "sid"}
   :google-oauth {:client-id nil
                  :client-secret nil
                  :redirect-uri nil
                  :scope "openid email profile"}
   ;; Default uploads root relative to CWD; override with UPLOADS_ROOT
   :uploads {:root "uploads"}})

(defn load-env []
  (let [file (read-secret-file)
        envv (env-vars)
        merged (deep-merge defaults envv file)]
    (when (= (get-in merged [:session :secret]) (get-in defaults [:session :secret]))
      (log/warn "Using DEFAULT session secret; set SESSION_SECRET or provide secret.edn"))
    (when (or (nil? (get-in merged [:google-oauth :client-id]))
              (nil? (get-in merged [:google-oauth :client-secret]))
              (nil? (get-in merged [:google-oauth :redirect-uri])))
      (log/warn "Google OAuth configuration incomplete; set GOOGLE_CLIENT_ID/SECRET/REDIRECT_URI or provide secret.edn"))
    merged))

(defmethod ig/init-key :config/env [_ _]
  (load-env))
