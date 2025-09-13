 (ns frontend.state.ui
   (:require [frontend.utilities.theme :as theme]
             [re-frame.core :as rf]
             [re-frame.db :as rfdb]))

 ;; Effects
 (rf/reg-fx ::apply-theme
            (fn [theme]
              (theme/apply-theme theme)
              (theme/persist-theme! theme)))

 ;; Events
 (rf/reg-event-fx
  ::set-theme
  (fn [{:keys [db]} [_ theme-val]]
    {:db (assoc-in db [:ui :theme] theme-val)
     ::apply-theme theme-val}))

 ;; Subs
 (rf/reg-sub
  ::ui-state
  (fn [db _]
    (:ui db)))

 ;; Runtime initializer to set theme and wire system listener.
 (defn init-theme! []
   (let [saved-theme (or (theme/get-saved-theme) :system)]
     (rf/dispatch [::set-theme saved-theme])
     (when (= saved-theme :system)
       (theme/listen-system-change!
        (fn []
          (when (= (get-in @rfdb/app-db [:ui :theme]) :system)
            (rf/dispatch [::set-theme :system])))))))

