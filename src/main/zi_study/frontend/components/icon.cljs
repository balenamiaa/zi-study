;; (ns zi-study.frontend.components.icon
;;   (:require
;;    [reagent.core :as r]
;;    ["lucide" :refer [createIcons icons]]))

;; ;; Your icon component
;; (defn icon
;;   ([icon-type class]
;;    (let [ensure-icons (fn [] (createIcons (clj->js {:icons icons})))]
;;      (r/create-class
;;       {:component-did-mount ensure-icons
;;        :component-did-update ensure-icons
;;        :reagent-render (fn []
;;                          [:i {:class class :data-lucide icon-type}])})))
;;   ([icon-type] (icon icon-type "")))