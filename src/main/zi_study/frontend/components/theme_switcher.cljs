(ns zi-study.frontend.components.theme-switcher
  (:require
   [reagent.core :as r]
   ["lucide-react" :as lucide]))

(defn update-theme [^string theme]
  (let [html-el (js/document.querySelector "html")]
    (if (= theme "dark")
      (.add (.-classList html-el) "dark")
      (.remove (.-classList html-el) "dark"))
    ;; Update localStorage
    (.setItem js/localStorage "theme" theme)
    ;; Update theme color meta tag for mobile browsers
    (let [theme-color (.querySelector js/document "meta[name='theme-color']")
          color (if (= theme "dark") "#171717" "#ffffff")]
      (when theme-color
        (.setAttribute theme-color "content" color)))))

(defn get-system-theme []
  (if (and (exists? js/window) 
           (exists? js/window.matchMedia) 
           (.-matches (.matchMedia js/window "(prefers-color-scheme: dark)")))
    "dark"
    "light"))

(defn get-initial-theme []
  (if (exists? js/localStorage)
    (let [stored-theme (.getItem js/localStorage "theme")]
      (if stored-theme
        stored-theme
        (get-system-theme)))
    "light"))

(defn theme-switcher
  "A component for switching between light and dark themes.
  
  Options:
  - size: The size of the switcher (:sm, :md, :lg)
  - class: Additional CSS classes
  - on-change: Function to call when theme changes (receives 'light' or 'dark' as argument)"
  [{:keys [size class on-change]
    :or {size :md}
    :as props}]
  
  (let [current-theme (r/atom (get-initial-theme))
        
        toggle-theme (fn []
                      (let [new-theme (if (= @current-theme "dark") "light" "dark")]
                        (reset! current-theme new-theme)
                        (update-theme new-theme)
                        (when on-change
                          (on-change new-theme))))
        
        size-map {:sm {:button "h-8 w-8" :icon 14}
                  :md {:button "h-10 w-10" :icon 16}
                  :lg {:button "h-12 w-12" :icon 18}}
        
        icon-size (get-in size-map [size :icon])
        button-size (get-in size-map [size :button])]
    
    ;; Apply theme on component mount
    (r/create-class
     {:component-did-mount
      (fn [_] (update-theme @current-theme))
      
      :reagent-render
      (fn [{:keys [size class]}]
        [:button
         {:class (str "flex items-center justify-center rounded-full transition-colors "
                      button-size " "
                      "text-[var(--color-light-text-primary)] dark:text-[var(--color-dark-text-primary)] "
                      "bg-[var(--color-light-bg-paper)] dark:bg-[var(--color-dark-bg-paper)] "
                      "hover:bg-[var(--color-light-bg-hover)] dark:hover:bg-[var(--color-dark-bg-hover)] "
                      class)
          :aria-label (if (= @current-theme "dark") "Switch to light mode" "Switch to dark mode")
          :title (if (= @current-theme "dark") "Switch to light mode" "Switch to dark mode")
          :on-click toggle-theme}
         
         (if (= @current-theme "dark")
           [:> lucide/Sun {:size icon-size}]
           [:> lucide/Moon {:size icon-size}])])})))