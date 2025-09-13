(ns frontend.ui.link
  (:require
   [reitit.frontend.easy :as rfe]
   [uix.core :as uix :refer [$ defui]]))

(defui Link
  "Semantic anchor with route awareness.
  Props:
  - :route   — Reitit route name; when provided, href is computed via rfe/href
  - :params  — route path-params
  - :query   — route query params
  - :href    — used when :route is not provided
  - :class   — extra classes
  Children: link content."
  [{:keys [class route params query children] :as props}]
  (let [href (if route (rfe/href route params query) (:href props))
        props (-> props
                  (dissoc :route :params :query :class)
                  (assoc :href href :class (str "link link-hover " class)))]
    ($ :a props children)))
