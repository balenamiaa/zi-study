(ns dev
  "Development preload. Keep this lightweight and React-19 safe."
  (:require [re-frame.db]))

;; Note:
;; - Removed reagent-dev-tools which calls ReactDOM.render (removed in React 19),
;;   causing `render is not a function` at startup. If we want DB inspection,
;;   we can add a custom view or use re-frame-10x when it supports React 19.
;; - Keeping this ns as a no-op preload so shadow-cljs config stays intact.

(js/console.info "dev preload active – reagent-dev-tools disabled for React 19")
