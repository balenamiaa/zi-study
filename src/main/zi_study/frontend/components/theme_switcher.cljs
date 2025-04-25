(ns zi-study.frontend.components.theme-switcher
  (:require
   [reagent.core :as r]
   ["lucide-react" :as lucide-icons]))

(defonce theme (r/atom (if (.-matches (js/window.matchMedia "(prefers-color-scheme: dark)"))
                         "dark"
                         "light")))

(defn toggle-theme []
  (swap! theme #(if (= % "light") "dark" "light"))
  (if (= @theme "dark")
    (-> js/document .-documentElement .-classList (.add "dark"))
    (-> js/document .-documentElement .-classList (.remove "dark")))

  (-> js/window .-localStorage (.setItem "theme" @theme)))

;; Initialize theme from localStorage or system preference on page load
(defn init-theme! []
  (let [stored-theme (-> js/window .-localStorage (.getItem "theme"))
        initial-theme (or stored-theme @theme)]
    (reset! theme initial-theme)
    (when (= initial-theme "dark")
      (-> js/document .-documentElement .-classList (.add "dark")))))

;; Ripple effect handler for buttons
(defn add-ripple-effect [e]
  (let [button (.-currentTarget e)
        rect (.getBoundingClientRect button)
        x (- (.-clientX e) (.-left rect))
        y (- (.-clientY e) (.-top rect))
        ripple (.createElement js/document "span")]

    ;; Set ripple style
    (set! (.. ripple -style -width) "300px")
    (set! (.. ripple -style -height) "300px")
    (set! (.. ripple -style -left) (str (- x 150) "px"))
    (set! (.. ripple -style -top) (str (- y 150) "px"))
    (set! (.. ripple -className) "ripple")

    ;; Add ripple to button
    (.appendChild button ripple)

    ;; Remove ripple after animation
    (js/setTimeout #(.remove ripple) 600)))

(defn theme-switcher []
  (r/create-class
   {:component-did-mount init-theme!
    :reagent-render
    (fn []
      [:button
       {:class (str "relative overflow-hidden rounded-full w-10 h-10 flex items-center justify-center "
                    "transition-all duration-300 ease-in-out hover:bg-[var(--color-primary-50)] "
                    "dark:hover:bg-[rgba(233,30,99,0.15)] focus:outline-none focus:ring-2 "
                    "focus:ring-[var(--color-primary-500)] dark:focus:ring-[var(--color-primary-300)]")
        :aria-label (str "Switch to " (if (= @theme "light") "dark" "light") " theme")
        :on-click (fn [e]
                    (toggle-theme)
                    (add-ripple-effect e))}

       ;; Moon icon (shown in dark mode)
       [:div
        {:class (str "absolute transition-all duration-500 ease-in-out "
                     (if (= @theme "light")
                       "opacity-0 scale-50 rotate-180"
                       "opacity-100 scale-100 rotate-0"))}
        [:> lucide-icons/Moon {:size 20
                               :className "text-[var(--color-primary-700)] dark:text-[var(--color-primary-300)]"}]]

       ;; Sun icon (shown in light mode)
       [:div
        {:class (str "absolute transition-all duration-500 ease-in-out "
                     (if (= @theme "light")
                       "opacity-100 scale-100 rotate-0"
                       "opacity-0 scale-50 -rotate-180"))}
        [:> lucide-icons/Sun {:size 20
                              :className "text-[var(--color-primary-700)] dark:text-[var(--color-primary-300)]"}]]])}))