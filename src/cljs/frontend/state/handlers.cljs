(ns frontend.state.handlers
  (:require [frontend.utilities.theme :as theme]
            [re-frame.core :as rf]
            [re-frame.db :as rfdb]))

;; Effects
(rf/reg-fx ::apply-theme
           (fn [theme]
             ;; Apply data-theme and persist user choice (cookie + localStorage)
             (theme/apply-theme theme)
             (theme/persist-theme! theme)))

;; Events
(rf/reg-event-db
 ::set-current-route
 (fn [db [_ route]]
   (assoc db :current-route route)))

(rf/reg-event-fx
 ::set-theme
 (fn [{:keys [db]} [_ theme]]
   {:db (assoc-in db [:ui :theme] theme)
    ::apply-theme theme}))

;; Runtime initializer to set theme and wire system listener.
(defn init-theme! []
  (let [saved-theme (or (theme/get-saved-theme) :system)]
    (rf/dispatch [::set-theme saved-theme])
    (when (= saved-theme :system)
      (theme/listen-system-change!
       (fn []
         (when (= (get-in @rfdb/app-db [:ui :theme]) :system)
           (rf/dispatch [::set-theme :system])))))))
