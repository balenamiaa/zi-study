(ns zi-study.frontend.utilities.theme
  (:require [zi-study.frontend.utilities.storage :as storage]
            [zi-study.frontend.state :as state]))

(def THEME_KEY "app-theme")
(def THEMES {:light "light" :dark "dark"})

(defn get-system-theme
  "Get the system theme based on prefers-color-scheme media query"
  []
  (if (and (exists? js/window)
           (.-matchMedia js/window)
           (.matchMedia js/window "(prefers-color-scheme: dark)")
           (.-matches (.matchMedia js/window "(prefers-color-scheme: dark)")))
    (:dark THEMES)
    (:light THEMES)))

(defn get-effective-theme
  "Get the effective theme (resolves :system to actual theme)"
  [theme]
  (if (= theme (:system THEMES))
    (get-system-theme)
    theme))

(defn apply-theme
  "Apply the theme to the document for Tailwind CSS dark mode"
  [theme]
  (when (exists? js/document)
    (let [doc-el (.-documentElement js/document)
          is-dark (= (keyword theme) :dark)]
      (if is-dark
        (.add (.-classList doc-el) "dark")
        (.remove (.-classList doc-el) "dark")))))

(defn initialize-theme
  "Initialize theme from localStorage or default to light theme"
  []
  (let [saved-theme (storage/get-item THEME_KEY)
        theme (if (= saved-theme (:dark THEMES))
                :dark
                :light)]
    (state/set-theme theme)
    (apply-theme theme)))

(defn set-theme
  "Set and persist the theme"
  [theme]
  (let [theme-kw (keyword theme)]
    (storage/set-item THEME_KEY (name theme-kw))
    (state/set-theme theme-kw)
    (apply-theme theme-kw)))

(defn toggle-theme
  "Toggle between light and dark themes"
  []
  (let [current-theme (:theme (state/get-ui-state))
        new-theme (if (= current-theme :light) :dark :light)]
    (set-theme new-theme))) 