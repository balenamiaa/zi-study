(ns zi-study.backend.uploads
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.data.json :as json])
  (:import [java.util UUID]))

(defonce ^:private uploads-dir "public/uploads/profiles/")

(defn- generate-unique-filename [original-filename]
  (let [extension (second (re-find #"\.(\w+)$" original-filename))
        uuid (UUID/randomUUID)]
    (str uuid (when extension (str "." extension)))))

(defn save-profile-picture!
  "Saves an uploaded profile picture file and returns its public URL path.
   Expects a file map like {:filename \"...\" :tempfile ... :content-type \"...\"}"
  [file-map]
  (when file-map
    (try
      (let [original-filename (:filename file-map)
            tempfile (:tempfile file-map)
            unique-filename (generate-unique-filename original-filename)
            target-dir (io/file uploads-dir)
            target-path (io/file target-dir unique-filename)]

        ; Ensure target directory exists
        (.mkdirs target-dir)

        ; Copy the uploaded file
        (io/copy tempfile target-path)

        ; Return the public URL path (relative to web root)
        (str "/uploads/profiles/" unique-filename))
      (catch Exception e
        (println "Error saving profile picture:" (ex-message e))
        (.printStackTrace e)
        nil)))) ; Return nil on error

(defn profile-picture-upload-handler [request]
  (if-let [file-map (get-in request [:multipart-params "profile_picture"])]
    (if-let [file-url (save-profile-picture! file-map)]
      {:status 200
       :headers {"Content-Type" "application/json"}
       :body (json/write-str {:url file-url})}
      {:status 500
       :headers {"Content-Type" "application/json"}
       :body (json/write-str {:error "Failed to save profile picture."})})
    {:status 400
     :headers {"Content-Type" "application/json"}
     :body (json/write-str {:error "Missing 'profile_picture' in request."})}))
