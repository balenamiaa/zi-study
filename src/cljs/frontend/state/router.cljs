 (ns frontend.state.router
   (:require [re-frame.core :as rf]))

 ;; Event to update current route match
 (rf/reg-event-db
  ::set-current-route
  (fn [db [_ route]]
    (assoc db :current-route route)))

 ;; Sub to read current route match
 (rf/reg-sub
  ::current-route
  (fn [db _]
    (:current-route db)))

