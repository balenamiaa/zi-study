(ns frontend.state.toasts
  (:require [re-frame.core :as rf]))

(def default-timeout-ms 2000)

(rf/reg-event-fx
 ::show
 (fn [{:keys [db]} [_ {:keys [id timeout-ms] :as t}]]
   (let [id (or id (random-uuid))
         ms (cond
              (nil? timeout-ms) default-timeout-ms
              (pos? timeout-ms) timeout-ms
              :else 0)
         toast (assoc t :id id)]
     (merge {:db (update-in db [:ui :toasts] (fnil conj []) toast)}
            (when (pos? ms)
              {::dispatch-after {:ms ms :event [::remove id]}}))))
)

(rf/reg-event-db
 ::remove
 (fn [db [_ id]]
   (update-in db [:ui :toasts] (fn [xs] (vec (remove (fn [x] (= (:id x) id)) xs))))))

(rf/reg-event-db
 ::clear
 (fn [db _]
   (assoc-in db [:ui :toasts] [])))

(rf/reg-sub
 ::items
 (fn [db _]
   (or (get-in db [:ui :toasts]) [])))

(rf/reg-fx
 ::dispatch-after
 (fn [{:keys [ms event]}]
   (js/setTimeout #(rf/dispatch event) ms)))
