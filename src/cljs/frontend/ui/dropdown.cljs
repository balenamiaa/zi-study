(ns frontend.ui.dropdown
  (:require
   [frontend.ui.core :as ui]
   [uix.core :as uix :refer [$ defui]]))

(defui Dropdown
  "DaisyUI dropdown wrapper.
  Props:
  - :trigger   — React element to act as button/label
  - :align     — :start | :end (default :start)
  - :items     — optional vector of {:label :icon :href :on-click :key} (renders <ul>)
  - :menu-class — extra classes for menu
  - :class     — extra classes for container
  - :children  — alternatively pass custom menu node in props"
  [{:keys [trigger align items class menu-class children]}]
  (let [align-cls (case align :end "dropdown-end" "")
        container-cls (ui/cx "dropdown" align-cls class)
        menu-cls (ui/cx "menu menu-sm dropdown-content mt-3 z-[1] p-2 shadow-lg bg-base-100 rounded-box w-56 border border-base-200"
                        menu-class)]
    ($ :div {:class container-cls}
       ($ :label {:tabIndex 0} trigger)
       (if items
         ($ :ul {:tabIndex 0 :class menu-cls}
            (for [{:keys [label icon href on-click key]} items]
              ($ :li {:key (or key (hash label))}
                 (if href
                   ($ :a {:href href} (when icon ($ icon {:size 16 :className "mr-2"})) label)
                   ($ :a {:href "#" :on-click (fn [e] (.preventDefault e) (when on-click (on-click)))}
                      (when icon ($ icon {:size 16 :className "mr-2"})) label)))))
         ;; custom menu content
         ($ :div {:tabIndex 0 :class menu-cls} children)))))

