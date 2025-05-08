(ns zi-study.frontend.components.flashgroup
  (:require
   [reagent.core :as r]
   [zi-study.frontend.components.alert :refer [toast]]
   [zi-study.frontend.state :refer [get-flash-messages remove-flash-message]]
   [zi-study.frontend.utilities :refer [cx]]))

(defn get-position-classes [position]
  (case position
    :top-left "top-4 left-4"
    :top-center "top-4 left-1/2 -translate-x-1/2"
    :top-right "top-4 right-4"
    :bottom-left "bottom-4 left-4"
    :bottom-center "bottom-4 left-1/2 -translate-x-1/2"
    :bottom-right "bottom-4 right-4"
    "top-4 right-4")) ; Default to top-right

(defn get-animation-classes [position exiting?]
  (cond
    ;; Entry animations
    (not exiting?)
    (cond
      (#{:top-right :bottom-right} position) "slide-in-right"
      (#{:top-left :bottom-left} position) "slide-in-left"
      (#{:top-center} position) "slide-in-top"
      (#{:bottom-center} position) "slide-in-bottom"
      :else "slide-in-right")
    
    ;; Exit animations
    :else
    (cond
      (#{:top-right :bottom-right} position) "slide-out-right"
      (#{:top-left :bottom-left} position) "slide-out-left"
      (#{:top-center :bottom-center} position) "slide-out-top"
      :else "slide-out-right")))

(defn flash-message
  "Renders a single flash message"
  [{:keys [id message color variant auto-hide on-hide position]}]
  (let [exiting (r/atom false)]
    (fn []
      [:div.transition-all.duration-300.ease-in-out.transform.flash-message.animate-enter
       {:class (cx "my-2 shadow-3"
                   (get-animation-classes position @exiting))
        :style {:opacity (if @exiting 0 1)}}
       [toast {:variant (or variant :soft)
               :color (or color :info)
               :auto-hide auto-hide
               :on-hide (fn []
                          (reset! exiting true)
                          ;; Allow time for exit animation
                          (js/setTimeout #(on-hide id) 300))}
        message]])))

(defn flashgroup
  "A component that displays flash messages at different positions on the screen.
   
   Flash messages are fetched from app state and automatically removed when dismissed
   or after their auto-hide timer expires."
  [{:keys [default-position]
    :or {default-position :top-right}}]
  (let [messages (get-flash-messages)]
    (r/create-class
     {:display-name "FlashGroup"
      
      :reagent-render
      (fn []
        (when (seq @messages)
          [:div
           ;; Group messages by position
           (for [[position pos-messages] (->> @messages
                                             (group-by #(or (:position %) default-position)))]
             ^{:key (name position)}
             [:div.fixed.z-50.pointer-events-none.flash-container
              {:class (cx (get-position-classes position)
                          {"bottom-position" (#{:bottom-left :bottom-center :bottom-right} position)})}
              
              ;; Render messages for this position
              (for [msg pos-messages]
                ^{:key (:id msg)}
                [:div.pointer-events-auto
                 [flash-message (assoc msg 
                                       :position position
                                       :on-hide remove-flash-message)]])])]))})))