(ns zi-study.frontend.utilities.theme
  (:require [zi-study.frontend.utilities.storage :as storage]
            [zi-study.frontend.state :as state]))

(def THEME_KEY "app-theme")
(def THEMES {:light "light" :dark "dark" :system "system"})

(defn get-system-theme
  "Get the system theme based on prefers-color-scheme media query. Returns string 'light' or 'dark'."
  []
  (if (and (exists? js/window)
           (.-matchMedia js/window)
           (.matchMedia js/window "(prefers-color-scheme: dark)")
           (.-matches (.matchMedia js/window "(prefers-color-scheme: dark)")))
    (:dark THEMES)
    (:light THEMES)))

(defn get-effective-theme
  "Get the effective theme keyword (:light or :dark) from a chosen theme keyword (which can be :system)."
  [chosen-theme-kw]
  (if (= chosen-theme-kw :system)
    (if (= (get-system-theme) (:dark THEMES)) ; get-system-theme returns string "dark" or "light"
      :dark
      :light)
    chosen-theme-kw))

(defn apply-theme
  "Apply the effective theme keyword (:light or :dark) to the document for Tailwind CSS dark mode."
  [effective-theme-kw]
  (when (exists? js/document)
    (let [doc-el (.-documentElement js/document)
          is-dark (= effective-theme-kw :dark)]
      (if is-dark
        (.add (.-classList doc-el) "dark")
        (.remove (.-classList doc-el) "dark")))))

;; --- System Theme Change Listener ---
(defonce system-theme-media-query
  (when (and (exists? js/window) (.-matchMedia js/window))
    (.matchMedia js/window "(prefers-color-scheme: dark)")))

(defn- handle-system-theme-change [_event]
  ;; Only re-apply if the current app setting is :system
  (when (= (:theme @(state/get-ui-state)) :system)
    (js/console.log "System theme changed, re-applying effective theme for :system choice.")
    (apply-theme (get-effective-theme :system))))

;; Store the cleanup function for the listener
(defonce cleanup-system-theme-listener-fn (atom nil))

(defn- setup-system-theme-listener []
  (when system-theme-media-query
    ;; If a listener already exists, remove it first.
    (when-let [cleanup-fn @cleanup-system-theme-listener-fn]
      (cleanup-fn)
      (reset! cleanup-system-theme-listener-fn nil))

    (.addEventListener system-theme-media-query "change" handle-system-theme-change)
    (reset! cleanup-system-theme-listener-fn
            #(.removeEventListener system-theme-media-query "change" handle-system-theme-change))
    (js/console.log "System theme change listener initialized.")))
;; --- End System Theme Change Listener ---

(defn initialize-theme
  "Initialize theme from localStorage or default to :system.
  Sets up application state and applies the effective theme.
  Also sets up a listener for system theme changes."
  []
  (let [saved-theme-str (storage/get-item THEME_KEY) ; "light", "dark", "system", or nil
        ;; Determine the initial theme for app state. Default to :system.
        initial-app-theme-kw (condp = saved-theme-str
                               (:light THEMES) :light
                               (:dark THEMES) :dark
                               (:system THEMES) :system
                               ;; Default case: if null or unrecognized, use :system
                               :system)
        effective-initial-theme-kw (get-effective-theme initial-app-theme-kw)]

    (state/set-theme initial-app-theme-kw)
    ;; index.html has already applied a theme. This call ensures consistency
    ;; and applies the theme if CLJS loads much later or conditions changed.
    (apply-theme effective-initial-theme-kw)
    (setup-system-theme-listener)))

(defn set-theme
  "Set and persist the chosen theme (:light, :dark, or :system).
  Updates app state and applies the effective theme to the document."
  [chosen-theme-kw]
  (storage/set-item THEME_KEY (name chosen-theme-kw)) ; Store "light", "dark", or "system"
  (state/set-theme chosen-theme-kw) ; Update app state to :light, :dark, or :system
  (apply-theme (get-effective-theme chosen-theme-kw))

  ;; If the theme is set to :system, ensure our listener is active and correct theme is applied.
  ;; If set to :light or :dark, the listener for system changes isn't strictly needed to drive updates,
  ;; but it doesn't harm to keep it active. The handle-system-theme-change checks current app state.
  )

