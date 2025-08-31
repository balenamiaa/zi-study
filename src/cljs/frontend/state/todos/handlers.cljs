(ns frontend.state.todos.handlers
  (:require [frontend.state.http-fx :as http]
            [re-frame.core :as rf]))

(rf/reg-event-fx ::get-todos
                 (fn [_ _]
                   {:fx [[:dispatch [:http/init [:todos]]]
                         [::http/fetch {:method :get
                                        :url "/api/todo"
                                        :on-success [:http/success [:todos]]
                                        :on-failure [:http/failure [:todos]]}]]}))

(rf/reg-event-fx ::add
                 (fn [_ [_ todo]]
                   {::http/fetch {:method :post
                                  :url "/api/todo"
                                  :request-content-type :json
                                  :body todo
                                  :on-success [::get-todos]}}))

(rf/reg-event-fx ::remove
                 (fn [_ [_ id]]
                   {::http/fetch {:method :delete
                                  :url (str "/api/todo/" id)
                                  :on-success [::get-todos]}}))

(rf/reg-event-fx ::save-changes
                 (fn [_ [_ id changes]]
                   {::http/fetch {:method :put
                                  :url (str "/api/todo/" id)
                                  :request-content-type :json
                                  :body changes
                                  :on-success [::get-todos]}}))
