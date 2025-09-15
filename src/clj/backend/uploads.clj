(ns backend.uploads
  (:require [clojure.java.io :as io]
            [clojure.string :as str])
  (:import (java.util UUID)))

(defn uploads-root
  "Return uploads root from request env or default to `uploads`."
  [req]
  (or (get-in req [:env :uploads :root]) "uploads"))

(defn sanitize-ext [filename]
  (when-let [m (re-find #"\.([^.]+)$" (str filename))]
    (-> (nth m 1) str/lower-case)))

(defn ensure-dir! [^java.io.File dir]
  (.mkdirs dir)
  dir)

(defn save-upload!
  "Save a multipart `file` under `category` (subfolder) with optional `basename`.
  - `allowed-exts`: set of allowed file extensions (lowercase), or nil to allow any.
  Returns {:url \"/uploads/<category>/<name>\", :path full-path, :filename name}.
  Throws on invalid file or extension."
  [req category {:keys [^java.io.File tempfile filename] :as file} allowed-exts basename]
  (when (or (nil? file) (nil? tempfile) (nil? filename))
    (throw (ex-info "Invalid file payload" {:error :invalid-file})))
  (let [ext (sanitize-ext filename)]
    (when (and allowed-exts (not (contains? allowed-exts ext)))
      (throw (ex-info "Invalid file extension" {:error :invalid-extension :ext ext})))
    (let [root (uploads-root req)
          dir (ensure-dir! (io/file root (str category)))
          new-name (str (or basename (str (UUID/randomUUID))) "." ext)
          dest (io/file dir new-name)]
      (io/copy tempfile dest)
      {:url (str "/uploads/" category "/" new-name)
       :path (.getPath dest)
       :filename new-name})))
