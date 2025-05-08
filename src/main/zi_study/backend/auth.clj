(ns zi-study.backend.auth
  (:require [buddy.hashers :as hashers]
            [clojure.string :as str]
            [next.jdbc.sql :as sql]
            [next.jdbc.result-set :as rs]
            [zi-study.backend.db :as db]
            [ring.util.response :as resp])
  (:import [com.auth0.jwt JWT]
           [com.auth0.jwt.algorithms Algorithm]
           [com.auth0.jwt.exceptions JWTVerificationException]
           [java.util Date]
           [java.sql SQLException]))

;; --- Configuration --- ;; TODO: Move to config/env vars
(def ^:private jwt-secret "replace-this-with-a-very-strong-secret-key-preferably-from-env")
(def ^:private jwt-algorithm (Algorithm/HMAC256 jwt-secret))
(def ^:private jwt-issuer "zi-study-app")
(def ^:private token-expiry-millis (* 1000 60 60 24)) ; 24 hours

;; --- Password Hashing --- 
(defn hash-password [raw-password]
  (hashers/derive raw-password {:alg :pbkdf2+sha256}))

(defn verify-password [raw-password hashed-password]
  (try
    (= (:valid (hashers/verify raw-password hashed-password)) true)
    (catch Exception _ false))) ; Handle potential errors during verification

;; --- JWT Functions --- 
(defn create-token [user-payload]
  (let [now (Date.)
        expires-at (Date. (+ (.getTime now) token-expiry-millis))
        builder (-> (JWT/create)
                    (.withIssuer jwt-issuer)
                    (.withIssuedAt now)
                    (.withExpiresAt expires-at)
                    (.withClaim "user-id" (:id user-payload))
                    (.withClaim "email" (:email user-payload))
                    (.withClaim "role" (:role user-payload)))]
    (-> (if-let [pfp-url (:profile_picture_url user-payload)]
          (.withClaim builder "profile_picture_url" pfp-url)
          builder) ; Pass builder through if no pfp
        (.sign jwt-algorithm))))

(defn verify-token [token]
  (try
    (let [verifier (-> (JWT/require jwt-algorithm)
                       (.withIssuer jwt-issuer)
                       (.build))]
      (.verify verifier token))
    (catch JWTVerificationException _ nil))) ; Return nil if verification fails

(defn decode-token [token]
  (when-let [decoded-jwt (verify-token token)]
    (let [pfp-claim (.getClaim decoded-jwt "profile_picture_url")]
      {:id    (. (.getClaim decoded-jwt "user-id") asLong)
       :email (. (.getClaim decoded-jwt "email") asString)
       :role  (. (.getClaim decoded-jwt "role") asString)
       :profile_picture_url (when-not (.isNull pfp-claim) (.asString pfp-claim))
       ; Extract other claims
       })))

;; --- Database Functions --- 
(defn create-user! [email password first-name last-name profile-pic-url]
  (let [hashed-password (hash-password password)
        user-data {:email email
                   :password_hash hashed-password
                   :first_name first-name
                   :last_name last-name
                   ; role defaults in DB
                   }]
    (try
      (sql/insert! @db/db-pool :users
                   (if profile-pic-url
                     (assoc user-data :profile_picture_url profile-pic-url)
                     user-data))
      (catch SQLException e
        (if (or (.contains (.getMessage e) "UNIQUE constraint failed: users.email") ; SQLite
                (.contains (.getMessage e) "unique constraint violation")) ; PostgreSQL
          {:error :email-exists}
          (do
            (println "SQL Error creating user:" (ex-message e))
            (.printStackTrace e)
            {:error :db-error :message (ex-message e)})))
      (catch Exception e
        (println "Error creating user:" (ex-message e))
        (.printStackTrace e)
        {:error :unknown :message (ex-message e)}))))

(defn get-user-by-email [email]
  (when-not (str/blank? email)
    (first (sql/query @db/db-pool
                      ["SELECT * FROM users WHERE email = ?" email]
                      {:builder-fn rs/as-unqualified-lower-maps}))))


;; --- Ring Handlers --- 
(defn register-handler [request]
  (let [;; Expect JSON body params now
        {:keys [email password first_name last_name profile_picture_url]} (get-in request [:body-params])
        ; Keep original variable names for clarity below
        first-name first_name
        last-name last_name
        profile-pic-url profile_picture_url]
    (if (or (str/blank? email) (str/blank? password))
      (-> (resp/response {:error "Email and password are required."})
          (resp/status 400))
      (let [;; Removed profile picture saving logic from here
            result (create-user! email password first-name last-name profile-pic-url)] ; Pass URL string
        (if-let [error-type (:error result)]
          (condp = error-type
            :email-exists (-> (resp/response {:error "Email already exists."})
                              (resp/status 409)) ; Conflict
            ; Default case for other errors (DB or unknown)
            (-> (resp/response {:error "Failed to create user."})
                (resp/status 500))) ; Internal Server Error status
          ; Success case - result is the inserted map from sql/insert!
          (-> (resp/response {:message "User created successfully."
                             ; Return the created user without sensitive info
                              :user (dissoc result :password_hash)})
              (resp/status 201))))))) ; Set Created status

(defn login-handler [request]
  (let [{:keys [email password]} (get-in request [:body-params])]
    (if (or (str/blank? email) (str/blank? password))
      (-> (resp/response {:error "Email and password are required."})
          (resp/status 400)) ; Set Bad Request status
      (if-let [user (get-user-by-email email)]
        (if (verify-password password (:password_hash user))
          (let [token (create-token user)
                response-body {:token token
                               :user (dissoc user :password_hash)}
                cookie-attrs {:http-only true
                              :secure false ; Set to true in production if using HTTPS
                              :same-site :lax
                              :path "/"
                              :max-age (/ token-expiry-millis 1000)}] ; Max-Age is in seconds
            (-> (resp/response response-body)
                (resp/set-cookie "auth-session-active" "true" cookie-attrs)))
          (-> (resp/response {:error "Invalid credentials."})
              (resp/status 401))) ; Unauthorized
        (-> (resp/response {:error "Invalid credentials."})
            (resp/status 401)))))) ; Unauthorized

(defn logout-handler [_request]
  (-> (resp/response {:message "Logged out successfully"})
      (resp/set-cookie "auth-session-active" "" {:max-age 0 :path "/" :http-only true})))

;; --- Authentication Middleware & Handler --- 

(defn- get-bearer-token [request]
  (when-let [header (get-in request [:headers "authorization"])]
    (second (re-matches #"(?i)Bearer\s+(.*)" header))))

(defn- unauthorized-response [message]
  (-> (resp/response {:error message})
      (resp/status 401)))

(defn wrap-authentication [handler]
  (fn [request]
    (if-let [token (get-bearer-token request)]
      (if-let [identity (decode-token token)]
        (handler (assoc request :identity identity))
        (unauthorized-response "Invalid or expired token."))
      (unauthorized-response "Authentication required."))))

(defn current-user-handler [request]
  (if-let [identity (:identity request)]
    (resp/response {:user identity})
    ;; This case should theoretically not be reached if middleware is applied correctly
    ;; But keep it as a safeguard or for direct testing.
    (-> (resp/response {:error "Authentication identity missing after middleware."}) ; More specific error
        (resp/status 500)))) ; Internal server error might be more appropriate here



