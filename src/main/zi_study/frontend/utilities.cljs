(ns zi-study.frontend.utilities)

(defn add-ripple-effect [e]
  (let [button (.-currentTarget e)
        rect (.getBoundingClientRect button)
        x (- (.-clientX e) (.-left rect))
        y (- (.-clientY e) (.-top rect))
        ripple (.createElement js/document "span")]

    ;; Set ripple style
    (set! (.. ripple -style -width) "300px")
    (set! (.. ripple -style -height) "300px")
    (set! (.. ripple -style -left) (str (- x 150) "px"))
    (set! (.. ripple -style -top) (str (- y 150) "px"))
    (set! (.. ripple -className) "ripple")

    ;; Add ripple to button
    (.appendChild button ripple)

    ;; Remove ripple after animation
    (js/setTimeout #(.remove ripple) 600)))