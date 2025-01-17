(ns oc.web.components.user-profile-modal
  (:require [rum.core :as rum]
            [cuerdas.core :as s]
            [goog.object :as googobj]
            [cljsjs.moment-timezone]
            [org.martinklepsch.derivatives :as drv]
            [oc.web.dispatcher :as dis]
            [oc.web.lib.utils :as utils]
            [oc.web.mixins.ui :as ui-mixins]
            [oc.web.lib.image-upload :as iu]
            [oc.web.utils.user :as user-utils]
            [oc.web.stores.user :as user-stores]
            [oc.web.actions.user :as user-actions]
            [oc.web.actions.nav-sidebar :as nav-actions]
            [oc.web.components.ui.alert-modal :as alert-modal]
            [oc.web.actions.notifications :as notification-actions]
            [oc.web.components.ui.small-loading :refer (small-loading)]
            [oc.web.components.ui.user-avatar :refer (user-avatar-image)]))

(defn real-close-cb [editing-user-data dismiss-action mobile-back-bt]
  (when (:has-changes editing-user-data)
    (user-actions/user-profile-reset))
  (dismiss-action)
  (when mobile-back-bt
    (nav-actions/menu-toggle)))

(def default-user-profile (oc.web.stores.user/random-user-image))

(defn- update-tooltip [s]
  (utils/after 100
   #(let [header-avatar (rum/ref-node s "user-profile-avatar")
          $header-avatar (js/$ header-avatar)
          edit-user-profile-avatar @(drv/get-ref s :edit-user-profile-avatar)
          title (if (empty? edit-user-profile-avatar)
                  "Add a photo"
                  "Change photo")
          profile-tab? (.hasClass $header-avatar "profile-tab")]
      (if profile-tab?
        (.tooltip $header-avatar #js {:title title
                                      :trigger "hover focus"
                                      :position "top"
                                      :container "body"})
        (.tooltip $header-avatar "destroy")))))

(defn close-cb [current-user-data dismiss-action & [mobile-back-bt]]
  (dis/dispatch! [:input [:latest-entry-point] 0])
  (if (:has-changes current-user-data)
    (let [alert-data {:icon "/img/ML/trash.svg"
                      :action "user-profile-unsaved-edits"
                      :message "Leave without saving your changes?"
                      :link-button-title "Stay"
                      :link-button-cb #(alert-modal/hide-alert)
                      :solid-button-style :red
                      :solid-button-title "Lose changes"
                      :solid-button-cb #(do
                                          (alert-modal/hide-alert)
                                          (real-close-cb current-user-data dismiss-action mobile-back-bt))}]
      (alert-modal/show-alert alert-data))
    (real-close-cb current-user-data dismiss-action mobile-back-bt)))

(defn error-cb [res error]
  (notification-actions/show-notification
    {:title "Image upload error"
     :description "An error occurred while processing your image. Please retry."
     :expire 3
     :dismiss true}))

(defn success-cb
  [res]
  (let [url (googobj/get res "url")]
    (if-not url
      (error-cb nil nil)
      (do
        (dis/dispatch! [:input [:edit-user-profile-avatar] url])
        (user-actions/user-avatar-save url)))))

(defn progress-cb [res progress])

(defn upload-user-profile-pictuer-clicked []
  (iu/upload! user-utils/user-avatar-filestack-config success-cb progress-cb error-cb))

(defn change! [s k v]
  (reset! (::name-error s) false)
  (reset! (::email-error s) false)
  (reset! (::password-error s) false)
  (reset! (::current-password-error s) false)
  (dis/dispatch! [:input [:edit-user-profile k] v])
  (dis/dispatch! [:input [:edit-user-profile :has-changes] true]))

(defn save-clicked [s]
  (when (compare-and-set! (::loading s) false true)
    (reset! (::name-error s) false)
    (reset! (::email-error s) false)
    (reset! (::password-error s) false)
    (reset! (::current-password-error s) false)
    (let [edit-user-profile @(drv/get-ref s :edit-user-profile)
          current-user-data @(drv/get-ref s :current-user-data)
          user-data (:user-data edit-user-profile)]
      (cond
        (and (empty? (:first-name user-data))
             (empty? (:last-name user-data)))
        (reset! (::name-error s) true)

        (not (utils/valid-email? (:email user-data)))
        (reset! (::email-error s) true)

        (and (seq (:password user-data))
             (empty? (:current-password user-data)))
        (reset! (::current-password-error s) true)

        (and (seq (:password user-data))
             (< (count (:password user-data)) 8))
        (reset! (::password-error s) true)

        :else
        (user-actions/user-profile-save current-user-data edit-user-profile)))))

(rum/defcs user-profile-modal <
  rum/reactive
  (drv/drv :edit-user-profile)
  (drv/drv :current-user-data)
  (drv/drv :edit-user-profile-avatar)
  ;; Locals
  (rum/local false ::loading)
  (rum/local false ::show-success)
  (rum/local false ::name-error)
  (rum/local false ::email-error)
  (rum/local false ::password-error)
  (rum/local false ::current-password-error)
  ;; Mixins
  ui-mixins/refresh-tooltips-mixin
  {:will-mount (fn [s]
    (user-actions/get-user nil)
    s)
    :did-remount (fn [old-state new-state]
    (let [user-data (:user-data @(drv/get-ref new-state :edit-user-profile))]
      (when (and @(::loading new-state)
                 (not (:has-changes user-data)))
        (reset! (::show-success new-state) true)
        (reset! (::loading new-state) false)
        (utils/after 2500 (fn [] (reset! (::show-success new-state) false)))))
    new-state)}
  [s]
  (let [edit-user-profile-avatar (drv/react s :edit-user-profile-avatar)
        is-jelly-head-avatar (s/includes? edit-user-profile-avatar "/img/ML/happy_face_")
        user-profile-data (drv/react s :edit-user-profile)
        current-user-data (:user-data user-profile-data)
        user-for-avatar (merge current-user-data {:avatar-url edit-user-profile-avatar})
        timezones (.names (.-tz js/moment))]
    [:div.user-profile-modal-container
      [:button.mlb-reset.modal-close-bt
        {:on-click #(close-cb current-user-data nav-actions/close-all-panels)}]
      [:div.user-profile-modal
        [:div.user-profile-header
          [:div.user-profile-header-title
            "My profile"]
          [:button.mlb-reset.save-bt
            {:on-click #(when-not @(::loading s)
                          (if (:has-changes current-user-data)
                            (save-clicked s)
                            (nav-actions/show-user-settings nil)))
             :class (when @(::show-success s) "no-disable")
             :disabled @(::loading s)}
            (if @(::show-success s)
              "Saved!"
              "Save")]
          [:button.mlb-reset.cancel-bt
            {:on-click (fn [_] (close-cb current-user-data #(nav-actions/show-user-settings nil)))}
            "Back"]]
        [:div.user-profile-body
          [:div.user-profile-avatar
            {:ref "user-profile-avatar"
             :on-click #(upload-user-profile-pictuer-clicked)}
            (if is-jelly-head-avatar
              [:div.empty-user-avatar-placeholder]
              (user-avatar-image user-for-avatar))
            [:div.user-profile-avatar-label "Edit profile photo"]]
          [:div.user-profile-modal-fields
            [:div.field-label "First name"
              (when @(::name-error s)
                [:span.error "Please provide your name."])]
            [:input.field-value.oc-input
              {:value (:first-name current-user-data)
               :type "text"
               :tab-index 1
               :max-length user-utils/user-name-max-lenth
               :on-change #(change! s :first-name (.. % -target -value))}]
            [:div.field-label "Last name"]
            [:input.field-value.oc-input
              {:value (:last-name current-user-data)
               :type "text"
               :tab-index 2
               :max-length user-utils/user-name-max-lenth
               :on-change #(change! s :last-name (.. % -target -value))}]
            [:div.field-label "Email"
              (when @(::email-error s)
                [:span.error "This email isn't valid."])]
            [:input.field-value.not-allowed.oc-input
              {:value (:email current-user-data)
               :placeholder "Your email address"
               :read-only true
               :type "text"}]
            ;; Current password
            (when (= (:auth-source current-user-data) "email")
              [:div.field-label "Currrent password"
                (when @(::current-password-error s)
                    [:span.error "Current password required"])])
            (when (= (:auth-source current-user-data) "email")
              [:input.field-value.oc-input
                {:type "password"
                 :tab-index 3
                 :on-change #(change! s :current-password (.. % -target -value))
                 :value (:current-password current-user-data)}])
            (when (= (:auth-source current-user-data) "email")
              [:div.field-label "New password"
                (when @(::password-error s)
                  [:span.error "Minimum 8 characters"])])
            (when (= (:auth-source current-user-data) "email")
              [:input.field-value.oc-input
                {:type "password"
                   :tab-index 4
                   :on-change #(change! s :password (.. % -target -value))
                   :value (:password current-user-data)}])
            [:div.field-label "Timezone"]
            [:select.field-value.oc-input
              {:value (:timezone current-user-data)
               :on-change #(change! s :timezone (.. % -target -value))}
              ;; Promoted timezones
              (for [t ["US/Eastern" "US/Central" "US/Mountain" "US/Pacific"]]
                [:option
                  {:key (str "timezone-" t "-promoted")
                   :value t} t])
              ;; Divider line option
              [:option
                {:disabled true
                 :value ""}
                "------------"]
              ;; All the timezones, repeating the promoted
              (for [t timezones]
                [:option
                  {:key (str "timezone-" t)
                   :value t}
                  t])]]
          ;; Delete account - hide for the moment since we don't have an API for it
          ; [:div.delete-account-container.hidden
          ;   [:button.mlb-reset.delete-account-bt
          ;     {:on-click #()}
          ;     "Delete my account"]]
          ]]]))