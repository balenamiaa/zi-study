(ns backend.api.auth
  (:require [backend.db :as db]
            [buddy.hashers :as hashers]
            [buddy.sign.jws :as jws]
            [cheshire.core :as json]
            [clojure.string :as str]
            [next.jdbc :as jdbc]
            [next.jdbc.sql :as sql]
            [ring.util.http-response :as resp])
  (:import (java.net.http HttpClient HttpRequest HttpRequest$BodyPublishers HttpResponse$BodyHandlers)
           (java.net URI)
           (java.nio.charset StandardCharsets)
           (java.security KeyFactory)
           (java.security.spec RSAPublicKeySpec)
           (java.math BigInteger)))

(defn- normalize-email [e]
  (some-> e str/trim str/lower-case))

(defn- user-public [user]
  (let [has-pwd? (boolean (or (:password-hash user)
                              (:password_hash user)))]
    (-> user
        (select-keys [:id :email :name :provider :provider-id :created-at :avatar-url])
        (assoc :has-password? has-pwd?)
        (update :created-at (fn [ts] (when ts (str ts)))))))

(defn- remember-ttl [req]
  (if (get-in req [:session :remember?]) (* 60 60 24 30) (* 60 60 24 1)))

(defn me [req]
  (if-let [sess-user (:user (:session req))]
    (let [id (:id sess-user)
          row (when id (sql/get-by-id (:db req) :app_user id :id db/opts))
          pub (user-public (or row sess-user))]
      (-> (resp/ok pub)
          ;; refresh session user with latest DB view
          (assoc :session (assoc (:session req) :user pub))
          (assoc :session-cookie-attrs {:max-age (remember-ttl req)})))
    (resp/unauthorized {:error :not-authenticated})))

(defn register [req]
  (let [params (or (get-in req [:parameters :body]) (:body req) (:body-params req))
        {:keys [email password name remember?]} params
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
                ;; On registration, default to non-remembered short session
                (assoc :session (assoc (:session req) :user pub :remember? false))
                (assoc :session-cookie-attrs {:max-age (* 60 60 24 1)}))))))))

(defn login [req]
  (let [{:keys [email password remember?]} (or (get-in req [:parameters :body]) (:body req) (:body-params req))
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

(defn- rand-token []
  (str (java.util.UUID/randomUUID)))

(defn google-start [req]
  (let [{:keys [client-id redirect-uri scope]} (get-in req [:env :google-oauth])
        scope (or scope "openid email profile")
        state (rand-token)
        nonce (rand-token)
        params {:client_id client-id
                :redirect_uri redirect-uri
                :response_type "code"
                :scope scope
                :include_granted_scopes "true"
                :access_type "online"
                :state state
                :nonce nonce}
        qs (str/join "&" (map (fn [[k v]] (str (name k) "=" (java.net.URLEncoder/encode (str v) "UTF-8"))) params))]
    (-> {:status 302
         :headers {"Location" (str "https://accounts.google.com/o/oauth2/v2/auth?" qs)}
         :body "Redirecting to Google"}
        (assoc :session (assoc-in (:session req) [:oauth :google] {:state state :nonce nonce})))))

(def ^:private token-endpoint "https://oauth2.googleapis.com/token")
(def ^:private userinfo-endpoint "https://openidconnect.googleapis.com/v1/userinfo")
(def ^:private jwks-uri "https://www.googleapis.com/oauth2/v3/certs")

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

(defonce ^:private jwks-cache (atom {:keys nil :fetched-at 0}))

(defn- fetch-jwks []
  (let [client (http-client)
        req (-> (HttpRequest/newBuilder (URI. jwks-uri))
                (.GET)
                .build)
        res (.send client req (HttpResponse$BodyHandlers/ofString))]
    (json/parse-string (.body res) true)))

(defn- get-jwk-key [kid]
  (let [{:keys [keys fetched-at]} @jwks-cache
        now (System/currentTimeMillis)]
    (if (and keys (< (- now fetched-at) (* 1000 60 10)))
      (some #(when (= (:kid %) kid) %) keys)
      (let [jwks (fetch-jwks)
            ks (:keys jwks)]
        (reset! jwks-cache {:keys ks :fetched-at now})
        (some #(when (= (:kid %) kid) %) ks)))))

(defn- jwk->rsa-public-key [{:keys [n e]}]
  (let [kf (KeyFactory/getInstance "RSA")
        n (BigInteger. 1 (.decode (java.util.Base64/getUrlDecoder) ^String n))
        e (BigInteger. 1 (.decode (java.util.Base64/getUrlDecoder) ^String e))
        spec (RSAPublicKeySpec. n e)]
    (.generatePublic kf spec)))

(defn- verify-google-id-token [id-token client-id expected-nonce]
  ;; Decode header to find kid
  (let [[header _payload _sig] (str/split id-token #"\.")
        header-json (String. (.decode (java.util.Base64/getUrlDecoder) header) StandardCharsets/UTF_8)
        {:keys [kid alg]} (json/parse-string header-json true)
        jwk (get-jwk-key kid)
        pub (jwk->rsa-public-key jwk)
        payload (jws/unsign id-token pub {:alg :rs256})
        claims (json/parse-string (if (string? payload)
                                    payload
                                    (String. ^bytes payload StandardCharsets/UTF_8))
                                  true)
        {:keys [iss aud exp nonce email email_verified sub name]} claims
        now (quot (System/currentTimeMillis) 1000)]
    (when-not (some #{iss} ["https://accounts.google.com" "accounts.google.com"]) (throw (ex-info "Invalid iss" {})))
    (when-not (= aud client-id) (throw (ex-info "Invalid aud" {})))
    (when (<= exp (- now 60)) (throw (ex-info "Expired token" {})))
    (when (and expected-nonce (not= nonce expected-nonce)) (throw (ex-info "Invalid nonce" {})))
    {:sub sub :email email :email-verified (boolean email_verified) :name name}))

(defn- upsert-google-user! [db {:keys [sub email name email-verified]}]
  (jdbc/with-transaction [tx db]
    (let [by-prov (first (sql/find-by-keys tx :app_user {:provider "google" :provider_id sub} db/opts))
          by-email (when email (sql/get-by-id tx :app_user email :email db/opts))]
      (cond
        by-prov (user-public by-prov)
        by-email (do
                   (when-not email-verified
                     (throw (ex-info "Email not verified by provider" {:error :email-not-verified})))
                   (sql/update! tx :app_user {:provider "google" :provider_id sub :name (or name (:name by-email))}
                                ["id = ?" (:id by-email)] db/opts)
                   (user-public (sql/get-by-id tx :app_user (:id by-email) :id db/opts)))
        :else (user-public (sql/insert! tx :app_user {:email email :name name :provider "google" :provider_id sub} db/opts))))))

(defn google-callback [req]
  (let [code (get-in req [:query-params "code"])
        state-param (get-in req [:query-params "state"])
        {:keys [client-id client-secret redirect-uri]} (get-in req [:env :google-oauth])
        {:keys [oauth-state expected-nonce]} (let [{:keys [state nonce]} (get-in req [:session :oauth :google])] {:oauth-state state :expected-nonce nonce})]
    (if (str/blank? code)
      (resp/bad-request {:error :missing-code})
      (try
        (when-not (= state-param oauth-state) (throw (ex-info "Invalid state" {:error :invalid-state})))
        (let [token-res (http-post-json token-endpoint {:code code
                                                        :client_id client-id
                                                        :client_secret client-secret
                                                        :redirect_uri redirect-uri
                                                        :grant_type "authorization_code"})
              id-token (:id_token token-res)
              access-token (:access_token token-res)
              {:keys [sub email name email-verified]} (verify-google-id-token id-token client-id expected-nonce)
              _profile (http-get-json userinfo-endpoint access-token) ; optional
              user (upsert-google-user! (:db req) {:sub sub :email email :name name :email-verified email-verified})]
          (-> (resp/see-other "/")
              (assoc :session (assoc (:session req) :user user))))
        (catch Exception e
          (let [data (ex-data e)]
            (if (= (:error data) :email-not-verified)
              (resp/unauthorized {:error :email-not-verified})
              (resp/internal-server-error {:error :google-oauth-failed
                                           :message (.getMessage e)}))))))))
