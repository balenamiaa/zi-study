(ns frontend.state.todos.subs
  (:require [re-frame.core :as rf]))

(rf/reg-sub ::todos
            :<- [:http/body [:todos]]
            (fn [todos _]
              (sort-by :id todos)))
