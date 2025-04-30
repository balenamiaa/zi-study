(ns zi-study.frontend.utilities.storage)

(defn get-item
  "Retrieve an item from localStorage."
  [key]
  (when (exists? js/localStorage)
    (.getItem js/localStorage key)))

(defn set-item
  "Store an item in localStorage."
  [key value]
  (when (exists? js/localStorage)
    (.setItem js/localStorage key value)))

(defn remove-item
  "Remove an item from localStorage."
  [key]
  (when (exists? js/localStorage)
    (.removeItem js/localStorage key)))