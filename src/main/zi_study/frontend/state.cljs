(ns zi-study.frontend.state
  (:require [reagent.core :as r]))


(defonce app-state
  (r/atom {:text "Hello, Reagent!"
           :auth/loading? true
           :auth/token nil
           :auth/authenticated? false
           :auth/current-user nil}))

(defonce router-match (r/atom nil))