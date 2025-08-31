(ns frontend.utilities.theme
  (:require [frontend.state :as state]
            [re-frame.db :as rfdb]))

(defn get-system-preference []
  (if (and js/window.matchMedia
           (.-matches (.matchMedia js/window "(prefers-color-scheme: dark)")))
    :dark
    :light))

(defn apply-theme [theme]
  (let [effective-theme (if (= theme :system)
                          (get-system-preference)
                          theme)
        html-element (.-documentElement js/document)]
    (.setAttribute html-element "data-theme"
                   (if (= effective-theme :dark) "gold_dark" "gold_light"))))

(defn set-theme [theme]
  (state/set-theme theme)
  (apply-theme theme)
  (.setItem js/localStorage "theme" (name theme))
  ;; Also persist in a cookie so the server can render index.html with correct theme immediately
  (set! (.-cookie js/document)
        (str "theme=" (name theme)
             "; path=/; max-age=" (* 60 60 24 365)
             "; SameSite=Lax")))

(defn get-saved-theme []
  (when-let [saved (.getItem js/localStorage "theme")]
    (keyword saved)))

(defn initialize-theme []
  (let [saved-theme (or (get-saved-theme) :system)]
    (set-theme saved-theme)

    (when (and js/window.matchMedia (= saved-theme :system))
      (.addEventListener
       (.matchMedia js/window "(prefers-color-scheme: dark)")
       "change"
       (fn [_e]
         ;; Avoid creating a subscription outside reactive context
         (when (= (get-in @rfdb/app-db [:ui :theme]) :system)
           (apply-theme :system)))))))
