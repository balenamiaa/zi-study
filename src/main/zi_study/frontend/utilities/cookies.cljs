(ns zi-study.frontend.utilities.cookies)

(defn get-cookie
  "Get a cookie value by name"
  [name]
  (let [cookies (.-cookie js/document)
        cookie-parts (-> cookies (.split ";") (.map #(.trim %)))
        cookie-map (reduce (fn [acc cookie]
                             (let [[k v] (.split cookie "=")]
                               (assoc acc k v)))
                           {}
                           cookie-parts)]
    (get cookie-map name)))

(defn set-cookie
  "Set a cookie with name, value, and optional days to expire"
  [name value & [days]]
  (let [expires (when days
                  (let [date (js/Date.)]
                    (.setTime date (+ (.getTime date) (* days 24 60 60 1000)))
                    (str "; expires=" (.toUTCString date))))
        cookie (str name "=" value expires "; path=/")]
    (set! (.-cookie js/document) cookie)))

(defn delete-cookie
  "Delete a cookie by name"
  [name]
  (set-cookie name "" -1))
