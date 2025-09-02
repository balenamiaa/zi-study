(ns backend.api.auth
  (:require [backend.db :as db]
            [buddy.hashers :as hashers]
            [cheshire.core :as json]
            [clojure.string :as str]
            [next.jdbc :as jdbc]
            [next.jdbc.sql :as sql]
            [ring.util.http-response :as resp])
  (:import (java.net.http HttpClient HttpRequest HttpRequest$BodyPublishers HttpResponse$BodyHandlers)
           (java.net URI)
           (java.nio.charset StandardCharsets)))

(defn- normalize-email [e]
  (some-> e str/trim str/lower-case))

(defn- user-public [user]
  (select-keys user [:id :email :name :provider :provider-id :created-at]))

(defn- remember-ttl [req]
  (if (get-in req [:session :remember?]) (* 60 60 24 30) (* 60 60 24 1)))

(defn me [req]
  (if-let [user (:user (:session req))]
    (-> (resp/ok (user-public user))
        (assoc :session (:session req))
        (assoc :session-cookie-attrs {:max-age (remember-ttl req)}))
    (resp/unauthorized {:error :not-authenticated})))

(defn register [req]
  (let [{:keys [email password name remember?]} (:body (:parameters req))
        email (normalize-email email)]
    (if (or (str/blank? email) (str/blank? password))
      (resp/bad-request {:error :invalid-params})
      (jdbc/with-transaction [tx (:db req)]
        (if (sql/get-by-id tx :app_user email :email db/opts)
          (resp/conflict {:error :email-taken})
          (let [pwd (hashers/derive password)
                user (sql/insert! tx :app_user {:email email :password_hash pwd :name name} db/opts)
                pub (user-public user)]
            (-> (resp/ok pub)
                (assoc :session (assoc (:session req) :user pub :remember? remember?))
                (assoc :session-cookie-attrs {:max-age (if remember? (* 60 60 24 30) (* 60 60 24 1))}))))))))

(defn login [req]
  (let [{:keys [email password remember?]} (:body (:parameters req))
        email (normalize-email email)
        user (some-> (sql/get-by-id (:db req) :app_user email :email db/opts)
                     (update-keys #(keyword (name %))))]
    (if (and user (hashers/check password (:password-hash user)))
      (let [pub (user-public user)]
        (-> (resp/ok pub)
            (assoc :session (assoc (:session req) :user pub :remember? remember?))
            (assoc :session-cookie-attrs {:max-age (if remember? (* 60 60 24 30) (* 60 60 24 1))})))
      (resp/unauthorized {:error :invalid-credentials}))))

(defn logout [req]
  (-> (resp/ok {:success true})
      (assoc :session (dissoc (:session req) :user))))

;; Google OAuth 2.0 (server-side) — endpoints skeleton
;; Expect env config for client-id/secret and redirect-uri in system conf.
;; Implement token exchange + profile fetch in your environment.

(defn google-start [req]
  (let [{:keys [client-id redirect-uri scope]} (get-in req [:env :google-oauth])
        scope (or scope "openid email profile")
        state "stub-state" ; TODO: CSRF state handling
        params {:client_id client-id
                :redirect_uri redirect-uri
                :response_type "code"
                :scope scope
                :include_granted_scopes "true"
                :access_type "online"
                :state state}
        qs (str/join "&" (map (fn [[k v]] (str (name k) "=" (java.net.URLEncoder/encode (str v) "UTF-8"))) params))]
    {:status 302
     :headers {"Location" (str "https://accounts.google.com/o/oauth2/v2/auth?" qs)}
     :body "Redirecting to Google"}))

(def ^:private token-endpoint "https://oauth2.googleapis.com/token")
(def ^:private userinfo-endpoint "https://openidconnect.googleapis.com/v1/userinfo")

(defn- http-client [] (HttpClient/newHttpClient))

(defn- form-encode [m]
  (->> m
       (map (fn [[k v]] (str (name k) "=" (java.net.URLEncoder/encode (str v) "UTF-8"))))
       (str/join "&")))

(defn- http-post-json [url form-map]
  (let [client (http-client)
        body (form-encode form-map)
        req (-> (HttpRequest/newBuilder (URI. url))
                (.header "Content-Type" "application/x-www-form-urlencoded")
                (.POST (HttpRequest$BodyPublishers/ofString body StandardCharsets/UTF_8))
                .build)
        res (.send client req (HttpResponse$BodyHandlers/ofString))]
    (json/parse-string (.body res) true)))

(defn- http-get-json [url access-token]
  (let [client (http-client)
        req (-> (HttpRequest/newBuilder (URI. url))
                (.header "Authorization" (str "Bearer " access-token))
                (.GET)
                .build)
        res (.send client req (HttpResponse$BodyHandlers/ofString))]
    (json/parse-string (.body res) true)))

(defn- upsert-google-user! [db {:keys [sub email name]}]
  (jdbc/with-transaction [tx db]
    (let [by-prov (first (sql/find-by-keys tx :app_user {:provider "google" :provider_id sub} db/opts))
          by-email (when email (sql/get-by-id tx :app_user email :email db/opts))]
      (cond
        by-prov (user-public by-prov)
        by-email (do (sql/update! tx :app_user {:provider "google" :provider_id sub :name (or name (:name by-email))}
                                  ["id = ?" (:id by-email)] db/opts)
                     (user-public (sql/get-by-id tx :app_user (:id by-email) :id db/opts)))
        :else (user-public (sql/insert! tx :app_user {:email email :name name :provider "google" :provider_id sub} db/opts))))))

(defn google-callback [req]
  (let [code (get-in req [:query-params "code"])
        {:keys [client-id client-secret redirect-uri]} (get-in req [:env :google-oauth])]
    (if (str/blank? code)
      (resp/bad-request {:error :missing-code})
      (try
        (let [token-res (http-post-json token-endpoint {:code code
                                                        :client_id client-id
                                                        :client_secret client-secret
                                                        :redirect_uri redirect-uri
                                                        :grant_type "authorization_code"})
              access-token (:access_token token-res)
              profile (http-get-json userinfo-endpoint access-token)
              {:keys [sub email name]} profile
              user (upsert-google-user! (:db req) {:sub sub :email email :name name})]
          (-> (resp/see-other "/")
              (assoc :session (assoc (:session req) :user user))))
        (catch Exception e
          (resp/internal-server-error {:error :google-oauth-failed
                                       :message (.getMessage e)}))))))
