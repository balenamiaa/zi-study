(ns frontend.pages.todos
  (:require ["lucide-react" :as lucide]
            [frontend.handlers :as h]
            [frontend.subs :as s]
            [frontend.uix.hooks :refer [use-subscribe]]
            [re-frame.core :as rf]
            [uix.core :as uix :refer [$ defui]]))

(defui todo-input [{:keys [on-add-todo]}]
  (let [[value set-value!] (uix/use-state "")]
    ($ :div {:class "join w-full"}
       ($ :input
          {:type "text"
           :placeholder "What needs to be done?"
           :class "input input-bordered join-item flex-1"
           :value value
           :on-change #(set-value! (.. % -target -value))
           :on-key-down (fn [e]
                          (when (= "Enter" (.-key e))
                            (when (not= "" value)
                              (on-add-todo {:text value :status :unresolved})
                              (set-value! ""))))})
       ($ :button
          {:class "btn btn-primary join-item"
           :on-click (fn []
                       (when (not= "" value)
                         (on-add-todo {:text value :status :unresolved})
                         (set-value! "")))}
          ($ :> lucide/Plus {:size 20})))))

(defui todo-item [{:keys [todo]}]
  (let [{:keys [id text status]} todo
        [editing? set-editing!] (uix/use-state false)
        [edit-value set-edit-value!] (uix/use-state text)]

    ($ :div {:class "card bg-base-200 shadow-sm hover:shadow-md transition-shadow"}
       ($ :div {:class "card-body p-4"}
          ($ :div {:class "flex items-center gap-3"}
             ($ :input
                {:type "checkbox"
                 :checked (= :resolved status)
                 :class "checkbox checkbox-primary"
                 :on-change #(rf/dispatch [::h/save-changes id
                                           {:status (if (= :resolved status)
                                                      :unresolved
                                                      :resolved)}])})

             (if editing?
               ($ :input
                  {:type "text"
                   :class "input input-sm input-bordered flex-1"
                   :value edit-value
                   :auto-focus true
                   :on-change #(set-edit-value! (.. % -target -value))
                   :on-blur #(do
                               (rf/dispatch [::h/save-changes id {:text edit-value}])
                               (set-editing! false))
                   :on-key-down (fn [e]
                                  (when (= "Enter" (.-key e))
                                    (rf/dispatch [::h/save-changes id {:text edit-value}])
                                    (set-editing! false)))})

               ($ :div {:class (str "flex-1 cursor-pointer "
                                    (when (= :resolved status) "line-through opacity-60"))
                        :on-click #(set-editing! true)}
                  text))

             ($ :button
                {:class "btn btn-ghost btn-circle btn-sm text-error"
                 :on-click #(rf/dispatch [::h/remove id])}
                ($ :> lucide/Trash2 {:size 16})))))))

(defui todos-page [_match]
  (let [todos (use-subscribe [::s/todos])]
    (uix/use-effect
     #(rf/dispatch [::h/get-todos])
     [])

    ($ :div {:class "max-w-4xl mx-auto space-y-6"}
       ($ :div {:class "text-center space-y-2"}
          ($ :h1 {:class "text-4xl font-bold"} "Todo List")
          ($ :p {:class "text-base-content/70"}
             "Keep track of your tasks with this beautiful todo app"))

       ($ todo-input {:on-add-todo #(rf/dispatch [::h/add %])})

       (if (empty? todos)
         ($ :div {:class "card bg-base-200"}
            ($ :div {:class "card-body text-center py-12"}
               ($ :> lucide/ListTodo {:size 48 :class "mx-auto mb-4 text-base-content/30"})
               ($ :p {:class "text-lg text-base-content/70"} "No todos yet!")
               ($ :p {:class "text-base-content/50"} "Add one above to get started")))

         ($ :div {:class "space-y-2"}
            (for [todo todos]
              ($ todo-item {:key (:id todo) :todo todo})))))))