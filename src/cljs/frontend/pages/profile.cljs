(ns frontend.pages.profile
  (:require ["lucide-react" :refer [Camera User Lock Save Info]]
            [frontend.state.auth :as auth]
            [frontend.state.user :as user]
            [frontend.state.ui-fx :as ui-fx]
            [frontend.ui.avatar :refer [Avatar]]
            [frontend.ui.button :refer [Button]]
            [frontend.ui.card :refer [Card CardHeader CardFooter]]
            [frontend.ui.field :refer [FieldWrapper TextInput PasswordInput]]
            [frontend.uix.hooks :refer [use-subscribe]]
            [re-frame.core :as rf]
            [uix.core :as uix :refer [$ defui]]))

(defui profile-page [_]
  (let [auth (use-subscribe [::auth/auth])
        user (:user auth)
        user (if (and (map? user) (:body user)) (:body user) user)
        name0 (:name user)
        email (:email user)
        avatar (:avatar-url user)
        provider (:provider user)
        has-password? (boolean (:has-password? user))

        [name set-name!] (uix/use-state (or name0 ""))
        [cur set-cur!] (uix/use-state "")
        [nw set-nw!] (uix/use-state "")
        [nw2 set-nw2!] (uix/use-state "")
        [show-cur? set-show-cur!] (uix/use-state false)
        [show-nw? set-show-nw!] (uix/use-state false)
        [show-nw2? set-show-nw2!] (uix/use-state false)]

    ($ :div {:class "space-section"}
       ($ :div {:class "text-center space-y-2"}
          ($ :h1 {:class "text-heading-2"} "Profile Settings")
          ($ :p {:class "text-body text-base-content/70"}
             "Manage your account information and security."))

       ($ :div {:class "grid gap-6 md:grid-cols-2"}
          ;; Avatar card
          ($ Card {:glass? true}
             ($ CardHeader nil "Profile picture")
             ($ :div {:class "flex items-center gap-6"}
                ($ :div {:class "relative"}
                   ($ Avatar {:src avatar :name name0 :email email :size :xl})
                   ($ :label {:class "absolute bottom-0 right-0 btn btn-circle btn-primary shadow-md cursor-pointer"}
                      ($ Camera {:size 16})
                      ($ :input {:type "file" :accept "image/*" :class "hidden"
                                 :on-change (fn [e]
                                              (when-let [file (aget (.. e -target -files) 0)]
                                                (rf/dispatch [::auth/upload-avatar file [::user/avatar-uploaded]])))})))
                ($ :div {:class "text-sm text-base-content/70"}
                   ($ :p nil "Use a clear, centered image for best results.")
                   ($ :p nil "We optimize and crop images automatically.")))
             ($ CardFooter nil
                 ($ :div {:class "flex items-center gap-3"}
                    ($ :div {:class "text-xs text-base-content/60"}
                       "PNG is used internally for consistency.")
                    (when (seq avatar)
                      ($ :button {:class "btn btn-xs btn-outline"
                                  :on-click #(rf/dispatch [::user/remove-avatar])}
                         "Remove photo")))))

          ;; Profile info
          ($ Card {:glass? true}
             ($ CardHeader nil "Profile information")
             ($ FieldWrapper {:id "name" :label "Display name"}
                ($ TextInput {:id "name" :full? true :start-icon User
                              :placeholder "Your name" :value name
                              :on-change #(set-name! (.. % -target -value))}))
             ($ CardFooter nil
                ($ Button {:variant :primary :icon Save
                           :on-click #(rf/dispatch [::user/update-profile {:name name}])}
                   "Save changes")))

          ;; Password change
          ($ Card {:glass? true}
             ($ CardHeader nil "Change password")
             (when (and (= provider "google") (not has-password?))
               ($ :div {:class "alert alert-info"}
                  ($ Info {:size 16})
                  ($ :span nil "This account was created with Google. You can set a password to enable password login.")))
             ($ FieldWrapper {:id "current" :label "Current password"}
                ($ PasswordInput {:id "current" :full? true :start-icon Lock
                                  :show? show-cur? :toggle! set-show-cur!
                                  :placeholder "••••••••" :value cur
                                  :on-change #(set-cur! (.. % -target -value))}))
             ($ FieldWrapper {:id "new" :label "New password"}
                ($ PasswordInput {:id "new" :full? true :start-icon Lock
                                  :show? show-nw? :toggle! set-show-nw!
                                  :placeholder "At least 8 characters" :value nw
                                  :on-change #(set-nw! (.. % -target -value))}))
             ($ FieldWrapper {:id "confirm" :label "Confirm new password"}
                ($ PasswordInput {:id "confirm" :full? true :start-icon Lock
                                  :show? show-nw2? :toggle! set-show-nw2!
                                  :placeholder "Repeat new password" :value nw2
                                  :on-change #(set-nw2! (.. % -target -value))}))
             ($ CardFooter nil
                ($ Button {:variant :primary :icon Save
                           :on-click (fn []
                                       (if (not= nw nw2)
                                         (rf/dispatch [:profile/passwords-mismatch])
                                         (rf/dispatch [::user/change-password {:current cur :new nw :confirm nw2}
                                                      [:profile/clear-passwords]])))}
                   "Update password"))))

       ;; Clear password fields after success
       ($ :div {:class "hidden"}
          (do (rf/reg-event-fx
               :profile/clear-passwords
               (fn [_ _]
                 (set-cur! "")
                 (set-nw! "")
                 (set-nw2! "")
                 {}))
              (rf/reg-event-fx
               :profile/passwords-mismatch
               (fn [_ _]
                 {::ui-fx/toast {:text "Passwords do not match" :variant :error}}))
              nil)))))
