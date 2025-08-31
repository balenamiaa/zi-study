(ns frontend.utilities.theme)

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

(defn persist-theme! [theme]
  (.setItem js/localStorage "theme" (name theme))
  (set! (.-cookie js/document)
        (str "theme=" (name theme)
             "; path=/; max-age=" (* 60 60 24 365)
             "; SameSite=Lax")))

(defn get-saved-theme []
  (when-let [saved (.getItem js/localStorage "theme")]
    (keyword saved)))

(defn listen-system-change!
  "Attach a listener for system theme changes. Calls `f` on change."
  [f]
  (when js/window.matchMedia
    (.addEventListener
     (.matchMedia js/window "(prefers-color-scheme: dark)")
     "change"
     (fn [_e] (f)))))
