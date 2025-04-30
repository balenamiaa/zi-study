(ns zi-study.frontend.utilities.auth-core
  (:require [zi-study.frontend.utilities.storage :as storage]))

(def TOKEN_KEY "auth-token")

(defn get-token []
  (storage/get-item TOKEN_KEY))

(defn set-token [token]
  (storage/set-item TOKEN_KEY token))

(defn remove-token []
  (storage/remove-item TOKEN_KEY))