(ns zi-study.frontend.components.skeleton
  (:require [zi-study.frontend.utilities :refer [cx]]
            [reagent.core :as r]))


(defonce ^private default-debounce-time 500)

(defn skeleton [_initial-props]
  (let [timer-ref (r/atom nil)]
    (r/create-class
     {:display-name "skeleton"
      :get-initial-state
      (fn [this]
        {:visible? (zero? (:debounce (r/props this) default-debounce-time))})
      :component-did-mount
      (fn [this]
        (let [props (r/props this)
              debounce (:debounce props default-debounce-time)]
          (when (pos? debounce)
            (reset! timer-ref
                    (js/setTimeout
                     #(r/set-state this {:visible? true})
                     debounce)))))
      :component-will-unmount
      (fn [_this]
        (when @timer-ref
          (js/clearTimeout @timer-ref)
          (reset! timer-ref nil)))
      :reagent-render
      (fn [current-props]
        (let [this (r/current-component)
              state (r/state this)]
          (when (:visible? state)
            (let [{:keys [variant width height animation class]
                   :or {variant :rectangular
                        animation :pulse}} current-props
                  base-classes "bg-gradient-to-r from-[var(--color-light-divider)] to-[var(--color-light-bg-paper)] dark:from-[var(--color-dark-divider)] dark:to-[var(--color-dark-bg-paper)]"
                  animation-class (case animation
                                    :pulse "animate-pulse"
                                    :wave "animate-skeleton-wave"
                                    nil)
                  variant-classes (case variant
                                    :text "h-4 rounded-md overflow-hidden"
                                    :circular "rounded-full overflow-hidden"
                                    :avatar "rounded-full overflow-hidden border border-[var(--color-light-divider)] dark:border-[var(--color-dark-divider)]"
                                    :card "rounded-xl overflow-hidden border border-[var(--color-light-divider)] dark:border-[var(--color-dark-divider)] shadow-sm"
                                    :rectangular "rounded-md overflow-hidden")
                  default-height (case variant
                                   :text "1rem"
                                   :circular "3rem"
                                   :avatar "3rem"
                                   :card "12rem"
                                   :rectangular "1.5rem")
                  default-width (case variant
                                  :text "100%"
                                  :circular "3rem"
                                  :avatar "3rem"
                                  :card "100%"
                                  :rectangular "100%")]
              [:div {:class (cx base-classes variant-classes animation-class class)
                     :style {:width (or width default-width)
                             :height (or height default-height)}}]))))})))

(defn skeleton-text [_initial-props]
  (let [timer-ref (r/atom nil)]
    (r/create-class
     {:display-name "skeleton-text"
      :get-initial-state
      (fn [this]
        {:visible? (zero? (:debounce (r/props this) default-debounce-time))})
      :component-did-mount
      (fn [this]
        (let [props (r/props this)
              debounce (:debounce props default-debounce-time)]
          (when (pos? debounce)
            (reset! timer-ref
                    (js/setTimeout
                     #(r/set-state this {:visible? true})
                     debounce)))))
      :component-will-unmount
      (fn [_this]
        (when @timer-ref
          (js/clearTimeout @timer-ref)
          (reset! timer-ref nil)))
      :reagent-render
      (fn [current-props]
        (let [this (r/current-component)
              state (r/state this)]
          (when (:visible? state)
            (let [{:keys [rows variant-width animation class]
                   :or {rows 3
                        variant-width true
                        animation :pulse}} current-props]
              [:div {:class class}
               (for [i (range rows)]
                 ^{:key i}
                 [skeleton (assoc current-props ; Pass current_props down, override debounce
                                  :variant :text
                                  :animation animation
                                  :width (when variant-width
                                           (str (- 100 (* i (if (even? i) 15 10))) "%"))
                                  :class "mb-2"
                                  :debounce 0)])]))))})))

(defn skeleton-card [_initial-props]
  (let [timer-ref (r/atom nil)]
    (r/create-class
     {:display-name "skeleton-card"
      :get-initial-state
      (fn [this]
        {:visible? (zero? (:debounce (r/props this) default-debounce-time))})
      :component-did-mount
      (fn [this]
        (let [props (r/props this)
              debounce (:debounce props default-debounce-time)]
          (when (pos? debounce)
            (reset! timer-ref
                    (js/setTimeout
                     #(r/set-state this {:visible? true})
                     debounce)))))
      :component-will-unmount
      (fn [_this]
        (when @timer-ref
          (js/clearTimeout @timer-ref)
          (reset! timer-ref nil)))
      :reagent-render
      (fn [current-props & children]
        (let [this (r/current-component)
              state (r/state this)]
          (when (:visible? state)
            (let [{:keys [header media content-rows footer animation class]
                   :or {header true
                        media true
                        content-rows 3
                        footer true
                        animation :pulse}} current-props]
              [:div {:class (cx "bg-[var(--color-light-card)] dark:bg-[var(--color-dark-card)] rounded-xl overflow-hidden shadow-sm"
                                "border border-[var(--color-light-divider)] dark:border-[var(--color-dark-divider)]"
                                class)}
               ;; If children are provided, render them instead of default content
               (if (seq children)
                 children
                 ;; Default skeleton card content
                 [:<>
                  (when media
                    [skeleton {:variant :rectangular :height "12rem" :animation animation :debounce 0}])
                  (when header
                    [:div {:class "px-6 py-4 border-b border-[var(--color-light-divider)] dark:border-[var(--color-dark-divider)]"}
                     [skeleton {:variant :text :width "60%" :height "1.5rem" :animation animation :class "mb-2" :debounce 0}]
                     [skeleton {:variant :text :width "40%" :height "1rem" :animation animation :debounce 0}]])
                  [:div {:class "px-6 py-4"}
                   [skeleton-text {:rows content-rows :animation animation :debounce 0}]]
                  (when footer
                    [:div {:class "px-6 py-4 border-t border-[var(--color-light-divider)] dark:border-[var(--color-dark-divider)] flex gap-3 justify-end"}
                     [skeleton {:variant :rectangular :width "5rem" :height "2rem" :animation animation :class "rounded-md" :debounce 0}]
                     [skeleton {:variant :rectangular :width "5rem" :height "2rem" :animation animation :class "rounded-md" :debounce 0}]])])]))))})))

(defn skeleton-avatar-with-text [_initial-props]
  (let [timer-ref (r/atom nil)]
    (r/create-class
     {:display-name "skeleton-avatar-with-text"
      :get-initial-state
      (fn [this]
        {:visible? (zero? (:debounce (r/props this) default-debounce-time))})
      :component-did-mount
      (fn [this]
        (let [props (r/props this)
              debounce (:debounce props default-debounce-time)]
          (when (pos? debounce)
            (reset! timer-ref
                    (js/setTimeout
                     #(r/set-state this {:visible? true})
                     debounce)))))
      :component-will-unmount
      (fn [_this]
        (when @timer-ref
          (js/clearTimeout @timer-ref)
          (reset! timer-ref nil)))
      :reagent-render
      (fn [current-props]
        (let [this (r/current-component)
              state (r/state this)]
          (when (:visible? state)
            (let [{:keys [rows avatar-size animation class]
                   :or {rows 2
                        avatar-size "3rem"
                        animation :pulse}} current-props]
              [:div {:class (cx "flex gap-3 p-3 rounded-lg hover:bg-[var(--color-light-bg-paper)] dark:hover:bg-[var(--color-dark-bg-paper)] transition-colors" class)}
               [skeleton (assoc current-props
                                :variant :avatar
                                :width avatar-size
                                :height avatar-size
                                :animation animation
                                :debounce 0)]
               [:div {:class "flex-1"}
                (for [i (range rows)]
                  ^{:key i}
                  [skeleton (assoc current-props
                                   :variant :text
                                   :width (if (zero? i) "40%" "70%")
                                   :animation animation
                                   :class "mb-2"
                                   :debounce 0)])]]))))})))

(defn skeleton-table [_initial-props]
  (let [timer-ref (r/atom nil)]
    (r/create-class
     {:display-name "skeleton-table"
      :get-initial-state
      (fn [this]
        {:visible? (zero? (:debounce (r/props this) default-debounce-time))})
      :component-did-mount
      (fn [this]
        (let [props (r/props this)
              debounce (:debounce props default-debounce-time)]
          (when (pos? debounce)
            (reset! timer-ref
                    (js/setTimeout
                     #(r/set-state this {:visible? true})
                     debounce)))))
      :component-will-unmount
      (fn [_this]
        (when @timer-ref
          (js/clearTimeout @timer-ref)
          (reset! timer-ref nil)))
      :reagent-render
      (fn [current-props]
        (let [this (r/current-component)
              state (r/state this)]
          (when (:visible? state)
            (let [{:keys [rows cols header animation class]
                   :or {rows 4
                        cols 3
                        header true
                        animation :pulse}} current-props]
              [:div {:class (cx "w-full rounded-lg overflow-hidden border border-[var(--color-light-divider)] dark:border-[var(--color-dark-divider)]" class)}
               (when header
                 [:div {:class (cx "flex gap-4 p-4 border-b border-[var(--color-light-divider)] dark:border-[var(--color-dark-divider)]"
                                   "bg-[var(--color-light-bg-paper)] dark:bg-[var(--color-dark-bg-paper)]")}
                  (for [i (range cols)]
                    ^{:key i}
                    [skeleton {:variant :text :width (str (+ 20 (* i 5)) "%") :height "1.5rem" :animation animation :debounce 0}])])
               [:div {:class "p-2"}
                (for [r (range rows)]
                  ^{:key r}
                  [:div {:class (cx "flex gap-4 p-2 rounded-md"
                                    (when (even? r) "bg-[var(--color-light-bg-paper)] dark:bg-[var(--color-dark-bg-paper)]"))}
                   (for [c (range cols)]
                     ^{:key c}
                     [skeleton {:variant :text :width (str (+ 20 (* c 5)) "%") :animation animation :debounce 0}])])]]))))})))

