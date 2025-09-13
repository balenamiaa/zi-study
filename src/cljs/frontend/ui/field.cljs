(ns frontend.ui.field
  (:require
   ["lucide-react" :refer [Eye EyeOff]]
   [frontend.ui.core :as ui]
   [uix.core :as uix :refer [$ defui]]))

(defn- with-status [base {:keys [invalid? valid?]}]
  (ui/cx base
         (when invalid? "input-error")
         (when valid? "input-success")))

(def input-colors
  {:neutral "input-neutral"
   :primary "input-primary"
   :secondary "input-secondary"
   :accent "input-accent"
   :info "input-info"
   :success "input-success"
   :warning "input-warning"
   :error "input-error"})

(defui FieldWrapper
  "Form field scaffold: renders label (if provided), content, and hint/error line.
  Props:
  - :id     — associates label with inner input
  - :label  — text label above field
  - :hint   — helper text under field
  - :error  — error text under field (styled)
  - :class  — wrapper classes
  - :children — field contents"
  [{:keys [id label hint error class children]}]
  ($ :div {:class (ui/cx "form-control" class)}
     (when label ($ :label {:for id :class "label pb-1"}
                    ($ :span {:class "label-text"} label)))
     children
     (when (or hint error)
       ($ :label {:class "label pt-1"}
          ($ :span {:class (ui/cx "label-text-alt"
                                  (when error "text-error"))}
             (or error hint))))))

(defui TextInput
  "DaisyUI-compliant text input wrapper.
  Renders label.input wrapper with optional icons/elements.
  Props:
  - :id           — input id
  - :start-icon   — lucide component shown on the left
  - :end-icon     — lucide component shown on the right (ignored if :end-node present)
  - :end-node     — custom React node placed on the right (e.g., a button)
  - :size         — :sm | :md (default) | :lg
  - :ghost?       — true for input-ghost
  - :color        — :neutral|:primary|:secondary|:accent|:info|:success|:warning|:error
  - :invalid?     — applies input-error
  - :valid?       — applies input-success
  - :full?        — true makes wrapper w-full
  - :input-class  — extra classes for inner <input>
  Other props are forwarded to the inner <input>."
  [{:keys [id class start-icon end-icon end-node size invalid? valid? full? ghost? color input-class] :as props}]
  (let [size (or size :md)
        wrapper-cls (ui/cx (with-status "input" {:invalid? invalid? :valid? valid?})
                           (if ghost? "input-ghost" "input-bordered")
                           (ui/input-sizes size)
                           (when color (get input-colors color))
                           (ui/full-class full?)
                           class)
        icls (ui/cx "grow min-w-0" input-class)
        input-props (-> props
                        (dissoc :class :start-icon :end-icon :end-node :size :invalid? :valid? :full? :ghost? :color :input-class)
                        (assoc :id id :class icls :type (or (:type props) "text")))]
    ($ :label {:class wrapper-cls}
       (when start-icon ($ start-icon {:class "h-[1em] w-[1em] opacity-50"}))
       ($ :input input-props)
       (cond
         end-node end-node
         end-icon ($ end-icon {:class "h-[1em] w-[1em] opacity-50"})))))

(defui PasswordInput
  "Password input built on TextInput with optional show/hide toggle.
  Props:
  - :show?    — boolean, when true shows text instead of password
  - :toggle!  — function (required for toggle button); when absent, no toggle rendered
  Other props are passed to TextInput."
  [{:keys [show? toggle!] :as props}]
  (let [type (if show? "text" "password")
        end (when toggle!
              ($ :button {:type "button" :class "btn btn-ghost btn-sm"
                          :on-click #(toggle! (not show?))}
                 (if show? ($ Eye {:size 16}) ($ EyeOff {:size 16}))))]
    ($ TextInput (-> props
                     (dissoc :show? :toggle!)
                     (assoc :type type)
                     (cond-> end (assoc :end-node end))))))

(defui TextArea
  "Multi-line input based on DaisyUI textarea.
  Props: :size (:sm|:md|:lg), :invalid?, :valid?, :full?"
  [{:keys [class size invalid? valid? full?] :as props}]
  (let [size (or size :md)
        cls (ui/cx (with-status ui/textarea-base {:invalid? invalid? :valid? valid?})
                   (case size
                     :sm "textarea-sm"
                     :lg "textarea-lg"
                     "")
                   (ui/full-class full?)
                   class)
        props (-> props (dissoc :class :size :invalid? :valid? :full?) (assoc :class cls))]
    ($ :textarea props)))

(defui Select
  "Styled select based on DaisyUI select.
  Props: :size (:sm|:md|:lg), :invalid?, :valid?, :full?
  Children: <option> nodes."
  [{:keys [class size invalid? valid? full? children] :as props}]
  (let [size (or size :md)
        cls (ui/cx (with-status ui/select-base {:invalid? invalid? :valid? valid?})
                   (case size
                     :sm "select-sm"
                     :lg "select-lg"
                     "")
                   (ui/full-class full?)
                   class)]
    ($ :select (-> props (dissoc :class :size :invalid? :valid? :full?) (assoc :class cls))
       children)))

(defui Checkbox
  "Primary checkbox with label.
  Props: :label, :checked, :on-change, :class"
  [{:keys [class label checked on-change]}]
  ($ :label {:class (ui/cx "label cursor-pointer justify-start gap-2" class)}
     ($ :input {:type "checkbox" :class "checkbox checkbox-primary"
                :checked checked :on-change on-change})
     (when label ($ :span {:class "label-text"} label))))

(defui RadioGroup
  "Vertical radio group.
  Props:
  - :name     — radio name attribute
  - :value    — selected value
  - :on-change — (fn [val]) when selection changes
  - :options  — vector of {:label :val}
  - :class    — extra classes"
  [{:keys [name value on-change options class]}]
  ($ :div {:class (ui/cx "flex flex-col gap-2" class)}
     (for [{:keys [label val]} options]
       ($ :label {:key (str name "-" (pr-str val)) :class "label cursor-pointer justify-start gap-2"}
          ($ :input {:type "radio" :name name :class "radio radio-primary"
                     :checked (= value val)
                     :on-change #(on-change val)})
          ($ :span {:class "label-text"} label)))))
