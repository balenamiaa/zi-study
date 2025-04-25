(ns zi-study.frontend.state
  (:require [reagent.core :as r]))


(defonce app-state (r/atom {:text "Hello, Reagent!"}))
(defonce router-match (r/atom nil))