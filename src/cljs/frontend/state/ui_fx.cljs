(ns frontend.state.ui-fx
  (:require [frontend.ui.feedback :as feedback]
            [re-frame.core :as rf]))

;; Side-effect to show a toast using UI feedback component
(rf/reg-fx
 ::toast
 (fn [payload]
   (when (map? payload)
     (feedback/toast! payload))))

