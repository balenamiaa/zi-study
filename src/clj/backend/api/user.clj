(ns backend.api.user
  (:require [backend.db :as db]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [integrant.core :as ig]
            [next.jdbc.sql :as sql]
            [ring.util.http-response :as resp])
  (:import (java.util UUID)))

(defn- ensure-dir! [^String path]
  (let [f (io/file path)]
    (.mkdirs f)
    f))

(def allowed-exts #{"jpg" "jpeg" "png" "webp"})

(defn- sanitize-ext [filename]
  (some-> filename (str/split #"\\.") last str/lower-case))

(defn require-auth [req]
  (if-let [user (get-in req [:session :user])]
    user
    (throw (ex-info "Unauthorized" {:type :unauthorized}))))

(defn update-profile [req]
  (let [{:keys [name]} (:body (:parameters req))
        user (require-auth req)
        id (:id user)]
    (sql/update! (:db req) :app_user {:name name} ["id = ?" id] db/opts)
    (resp/ok (assoc user :name name))))

(defn upload-avatar [req]
  (let [user (require-auth req)
        {:strs [file]} (:multipart-params req)
        tempfile (:tempfile file)
        filename (:filename file)
        ext (sanitize-ext filename)
        uploads-root (or (get-in req [:env :uploads :root]) "uploads")]
    (if (and tempfile (allowed-exts ext))
      (let [dir (str (io/file uploads-root "avatars"))
            _ (ensure-dir! dir)
            new-name (str (:id user) "_" (UUID/randomUUID) "." ext)
            dest (io/file dir new-name)
            _ (io/copy tempfile dest)
            url (str "/uploads/avatars/" new-name)]
        (sql/update! (:db req) :app_user {:avatar_url url} ["id = ?" (:id user)] db/opts)
        (resp/ok {:url url}))
      (resp/bad-request {:error :invalid-file}))))
