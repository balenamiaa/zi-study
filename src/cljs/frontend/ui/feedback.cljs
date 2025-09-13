(ns frontend.ui.feedback
  (:require
   ["lucide-react" :refer [X]]
   [frontend.state.toasts :as toasts]
   [frontend.ui.core :as ui]
   [frontend.ui.portal :as portal]
   [frontend.uix.hooks :as hooks]
   [re-frame.core :as rf]
   [uix.core :as uix :refer [$ defui]]))

;; Toasts only (alerts/flash consolidated into toasts)

(defn- position-classes [pos]
  (case pos
    :top-left "fixed z-50 left-4 top-4 space-y-2 w-[min(92vw,360px)]"
    :top-center "fixed z-50 top-4 left-1/2 -translate-x-1/2 space-y-2 w-[min(92vw,360px)]"
    :top-right "fixed z-50 right-4 top-4 space-y-2 w-[min(92vw,360px)]"
    :bottom-left "fixed z-50 left-4 bottom-4 space-y-2 w-[min(92vw,360px)]"
    :bottom-center "fixed z-50 bottom-4 left-1/2 -translate-x-1/2 space-y-2 w-[min(92vw,360px)]"
    :bottom-right "fixed z-50 right-4 bottom-4 space-y-2 w-[min(92vw,360px)]"
    ;; default
    "fixed z-50 right-4 top-4 space-y-2 w-[min(92vw,360px)]"))

;; Public API for toasts via re-frame app-db
(defn toast!
  "Show a toast via re-frame.
  Payload keys: :text (required), :variant (:info|:success|:warning|:error), :timeout-ms, :id, :position."
  [payload]
  (rf/dispatch [::toasts/show payload]))

(defn dismiss!
  "Dismiss a toast by id."
  [id]
  (rf/dispatch [::toasts/remove id]))

(defui Toasts
  "Viewport toast container. Mount once (layout). Uses a body portal by default.
  Props:
  - :portal?   — true (default) to render into document.body
  - :position  — keyword or set of positions: :top-right (default) | :top-left | :top-center | :bottom-*
  Data source: subscribes to [:ui :toasts] via frontend.state.toasts."
  [{:keys [portal? position]}]
  (let [items (hooks/use-subscribe [::toasts/items])
        pos (or position :top-right)
        pos-set (if (coll? pos) (set pos) #{pos})
        render-pos (fn [p]
                     ($ :div {:key (str "toasts-" (name (keyword p))) :class (position-classes p)}
                        (for [{:keys [id text variant position]} items
                              :when (contains? pos-set (or position p))]
                          ($ :div {:key id :class (ui/cx "toast-item glass-panel p-3 rounded-box shadow-md"
                                                         (case variant
                                                           :success "bg-success/10 text-success"
                                                           :error "bg-error/10 text-error"
                                                           :warning "bg-warning/10 text-warning"
                                                           :info "bg-info/10 text-info"))}
                             ($ :div {:class "flex items-center justify-between gap-3"}
                                ($ :div {:class "text-sm"} text)
                                ($ :button {:class "btn btn-ghost btn-xs" :on-click #(dismiss! id)
                                            :aria-label "Close"}
                                   ($ X {:size 14})))))))
        content (if (coll? pos-set)
                  ($ :div nil (map render-pos pos-set))
                  (render-pos (first pos-set)))]
    (if (false? portal?)
      content
      ($ portal/Portal nil content))))
