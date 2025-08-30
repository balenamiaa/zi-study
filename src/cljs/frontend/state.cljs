(ns frontend.state
  (:require [re-frame.core :as rf]))

(rf/reg-sub
 ::current-route
 (fn [db _]
   (:current-route db)))

(rf/reg-sub
 ::ui-state
 (fn [db _]
   (:ui db)))

(rf/reg-event-db
 ::set-current-route
 (fn [db [_ route]]
   (assoc db :current-route route)))

(rf/reg-event-db
 ::set-theme
 (fn [db [_ theme]]
   (assoc-in db [:ui :theme] theme)))

(defn get-current-route []
  (rf/subscribe [::current-route]))

(defn get-ui-state []
  (rf/subscribe [::ui-state]))

(defn set-current-route [route]
  (rf/dispatch [::set-current-route route]))

(defn set-theme [theme]
  (rf/dispatch [::set-theme theme]))