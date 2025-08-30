(ns frontend.utilities.theme
  (:require [frontend.state :as state]))

(defn get-system-preference []
  (if (and js/window.matchMedia
           (.matches (.matchMedia js/window "(prefers-color-scheme: dark)")))
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
  (.setItem js/localStorage "theme" (name theme)))

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
         (when (= (:theme @(state/get-ui-state)) :system)
           (apply-theme :system)))))))