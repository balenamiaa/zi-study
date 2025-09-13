
(ns backend.api.user
  (:require [backend.db :as db]
            [backend.image :as image]
            [backend.uploads :as uploads]
            [buddy.hashers :as hashers]
            [clojure.string :as str]
            [next.jdbc.sql :as sql]
            [ring.util.http-response :as resp])
  (:import (java.util UUID)
           (java.io File)))

(def allowed-exts #{"jpg" "jpeg" "png" "webp"})
(def processed-avatar-ext "png")

(defn require-auth [req]
  (if-let [user (get-in req [:session :user])]
    user
    (throw (ex-info "Unauthorized" {:type :unauthorized}))))

(defn update-profile [req]
  (let [{:keys [name]} (:body (:parameters req))
        user (require-auth req)
        id (:id user)]
    (sql/update! (:db req) :app_user {:name name} ["id = ?" id] db/opts)
    (-> (resp/ok (assoc user :name name))
        (assoc :session (assoc (:session req) :user (assoc user :name name))))))

(defn upload-avatar [req]
  (let [user (require-auth req)
        {:strs [file]} (:multipart-params req)
        basename (str (:id user) "_" (UUID/randomUUID))]
    (try
      (when (or (nil? file) (nil? (:tempfile file)))
        (throw (ex-info "Missing file" {:error :invalid-file})))
      ;; Process image to a safe PNG (square, max 512px)
      (let [^File processed (image/process-avatar! (:tempfile file) 512)
            ring-file {:tempfile processed :filename (str (or basename "avatar") "." processed-avatar-ext)}
            {:keys [url]} (uploads/save-upload! req "avatars" ring-file nil basename)
            id (:id user)]
        (sql/update! (:db req) :app_user {:avatar_url url} ["id = ?" id] db/opts)
        (-> (resp/ok {:url url})
            ;; also refresh session user so /me reflects latest avatar
            (assoc :session (assoc (:session req) :user (assoc user :avatar-url url)))))
      (catch Exception e
        (resp/bad-request {:error :invalid-file
                           :message (.getMessage e)})))))

(defn delete-avatar [req]
  (let [user (require-auth req)
        id (:id user)]
    (sql/update! (:db req) :app_user {:avatar_url nil} ["id = ?" id] db/opts)
    (-> (resp/ok {:success true})
        (assoc :session (assoc (:session req) :user (dissoc user :avatar-url))))))

(defn change-password [req]
  (let [{:keys [current new confirm]} (:body (:parameters req))
        user (require-auth req)
        id (:id user)
        row (sql/get-by-id (:db req) :app_user id :id db/opts)
        stored-hash (or (:password-hash row) (:password_hash row))]
    (cond
      (str/blank? new)
      (resp/bad-request {:error :invalid-params})

      (and (some? stored-hash) (str/blank? (str current)))
      (resp/bad-request {:error :current-required})

      (and (some? confirm) (not= (str new) (str confirm)))
      (resp/bad-request {:error :password-mismatch})

      (and (some? stored-hash) (not (hashers/check (or current "") stored-hash)))
      (resp/unauthorized {:error :invalid-current-password})

      :else
      (do (sql/update! (:db req) :app_user {:password_hash (hashers/derive new)} ["id = ?" id] db/opts)
          (let [row2 (sql/get-by-id (:db req) :app_user id :id db/opts)
                pub ((resolve 'backend.api.auth/user-public) row2)]
            (-> (resp/ok {:success true})
                (assoc :session (assoc (:session req) :user pub))))))))
