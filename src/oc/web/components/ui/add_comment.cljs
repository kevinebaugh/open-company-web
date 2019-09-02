(ns oc.web.components.ui.add-comment
  (:require [rum.core :as rum]
            [goog.events :as events]
            [dommy.core :refer-macros (sel1)]
            [goog.events.EventType :as EventType]
            [org.martinklepsch.derivatives :as drv]
            [oc.web.lib.jwt :as jwt]
            [oc.web.dispatcher :as dis]
            [oc.web.lib.utils :as utils]
            [oc.web.utils.comment :as cu]
            [oc.web.lib.responsive :as responsive]
            [oc.web.utils.mention :as mention-utils]
            [oc.web.mixins.mention :as mention-mixins]
            [oc.web.actions.comment :as comment-actions]
            [oc.web.mixins.ui :as ui-mixins]
            [oc.web.components.ui.alert-modal :as alert-modal]
            [oc.web.utils.medium-editor-media :as me-media-utils]
            [oc.web.actions.notifications :as notification-actions]
            [oc.web.components.ui.emoji-picker :refer (emoji-picker)]
            [oc.web.components.ui.giphy-picker :refer (giphy-picker)]
            [oc.web.components.ui.user-avatar :refer (user-avatar-image)]
            [oc.web.components.ui.media-video-modal :refer (media-video-modal)]
            [oc.web.actions.activity :as activity-actions]
            [oc.web.components.ui.user-avatar :refer (user-avatar-image)]
            [oc.web.components.ui.carrot-checkbox :refer (carrot-checkbox)]))

;; Add commnet handling
(defn enable-add-comment? [s]
  (when-let [add-comment-div (rum/ref-node s "editor-node")]
    (let [activity-data (first (:rum/args s))
          parent-comment-uuid (second (:rum/args s))
          comment-text (.-innerHTML add-comment-div)
          next-add-bt-disabled (or (nil? comment-text) (not (seq comment-text)))
          edit-comment-data (get (vec (:rum/args s)) 3 nil)]
      (comment-actions/add-comment-change activity-data parent-comment-uuid (:uuid edit-comment-data) comment-text)
      (when (not= next-add-bt-disabled @(::add-button-disabled s))
        (reset! (::add-button-disabled s) next-add-bt-disabled)))))

(defn focus-add-comment [s]
  (enable-add-comment? s)
  (let [activity-data (first (:rum/args s))
        parent-comment-uuid (second (:rum/args s))]
    (if parent-comment-uuid
      (comment-actions/add-comment-focus parent-comment-uuid)
      (comment-actions/add-comment-focus (:uuid activity-data)))))

(defn disable-add-comment-if-needed [s]
  (when-let [add-comment-node (rum/ref-node s "editor-node")]
    (enable-add-comment? s)
    (when-not (seq (.-innerHTML add-comment-node))
      (comment-actions/add-comment-blur))))

(defn- send-clicked [event s]
  (reset! (::add-button-disabled s) true)
  (let [add-comment-div (rum/ref-node s "editor-node")
        comment-body (cu/add-comment-content add-comment-div true)
        args (vec (:rum/args s))
        activity-data (first args)
        parent-comment-uuid (second args)
        dismiss-reply-cb (get args 2)
        edit-comment-data (get args 3 nil)
        save-done-cb (fn [success]
                      (if success
                        (do
                          (when add-comment-div
                            (set! (.-innerHTML add-comment-div) ""))
                          (when (fn? dismiss-reply-cb)
                            (dismiss-reply-cb false)))
                        (notification-actions/show-notification
                         {:title "An error occurred while saving your comment."
                          :description "Please try again"
                          :dismiss true
                          :expire 3
                          :id (if edit-comment-data :update-comment-error :add-comment-error)})))
        complete? @(::complete-follow-up s)]
    (if edit-comment-data
      (comment-actions/save-comment activity-data edit-comment-data comment-body save-done-cb)
      (comment-actions/add-comment activity-data comment-body parent-comment-uuid save-done-cb))
    (when complete?
      (let [follow-up (first (filterv #(= (-> % :assignee :user-id) (jwt/user-id)) (:follow-ups activity-data)))
            show-follow-up-button? (and follow-up
                                        (not (:completed? follow-up)))
            complete-follow-up-link (when show-follow-up-button?
                                      (utils/link-for (:links follow-up) "mark-complete" "POST"))]
        (activity-actions/complete-follow-up activity-data follow-up)))))

(defn me-options [reply-comment?]
  {:media-config ["gif" "photo" "video"]
   :placeholder (if reply-comment? "Reply…" "Add a comment…")
   :use-inline-media-picker true
   :media-picker-initially-visible false})

(defn add-comment-did-change [s]
  (reset! (::did-change s) true)
  (reset! (::show-post-button s) true)
  (enable-add-comment? s))

(defn- should-focus-field? [s]
  (let [activity-data (first (:rum/args s))
        parent-comment-uuid (second (:rum/args s))
        add-comment-focus @(drv/get-ref s :add-comment-focus)
        edit-comment-data (get (vec (:rum/args s)) 3 nil)]
    (or edit-comment-data
        (and (= (:uuid activity-data) add-comment-focus)
             (not parent-comment-uuid))
        (and (seq parent-comment-uuid)
             (= parent-comment-uuid add-comment-focus)))))

(rum/defcs add-comment < rum/reactive
                         ;; Locals
                         (rum/local nil :me/editor)
                         (rum/local nil :me/media-picker-ext)
                         (rum/local false :me/media-photo)
                         (rum/local false :me/media-video)
                         (rum/local false :me/media-attachment)
                         (rum/local false :me/media-photo-did-success)
                         (rum/local false :me/media-attachment-did-success)
                         (rum/local false :me/showing-media-video-modal)
                         (rum/local false :me/showing-gif-selector)
                         ;; Image upload lock
                         (rum/local false :me/upload-lock)
                         (rum/local "" ::add-comment-id)

                         ;; Derivatives
                         (drv/drv :media-input)
                         (drv/drv :add-comment-focus)
                         (drv/drv :add-comment-data)
                         (drv/drv :team-roster)
                         (drv/drv :current-user-data)
                         ;; Locals
                         (rum/local true ::add-button-disabled)
                         (rum/local "" ::initial-add-comment)
                         (rum/local false ::did-change)
                         (rum/local false ::show-post-button)
                         (rum/local false ::complete-follow-up)
                         ;; Force update key, we have 2 versions: one in the app-state
                         ;; one local. They are set up equally when the component is mounted
                         ;; When it changes in the app-state we can compare
                         ;; and force the add comment field to change it's body
                         ;; with the new one
                         ;; Used to force clean the field or to re-add the body
                         ;; when a comment POST fails
                         (drv/drv :add-comment-force-update)
                         (rum/local nil ::add-comment-force-update)
                         ;; Mixins
                         ui-mixins/first-render-mixin
                         (mention-mixins/oc-mentions-hover)

                         (ui-mixins/on-window-click-mixin (fn [s e]
                          (when (and @(:me/showing-media-video-modal s)
                                     (not (.contains (.-classList (.-target e)) "media-video"))
                                     (not (utils/event-inside? e (rum/ref-node s :video-container))))
                            (me-media-utils/media-video-add s @(:me/media-picker-ext s) nil)
                            (reset! (:me/showing-media-video-modal s) false))
                          (when (and @(:me/showing-gif-selector s)
                                     (not (.contains (.-classList (.-target e)) "media-gif"))
                                     (not (utils/event-inside? e (sel1 [:div.giphy-picker]))))
                            (me-media-utils/media-gif-add s @(:me/media-picker-ext s) nil)
                            (reset! (:me/showing-gif-selector s) false))))
                         {:will-mount (fn [s]
                          (reset! (::add-comment-id s) (utils/activity-uuid))
                          (let [activity-data (first (:rum/args s))
                                add-comment-data @(drv/get-ref s :add-comment-data)
                                parent-comment-uuid (second (:rum/args s))
                                edit-comment-data (get (vec (:rum/args s)) 3 nil)
                                add-comment-key (str (:uuid activity-data) "-" parent-comment-uuid "-" (:uuid edit-comment-data))
                                activity-add-comment-data (get add-comment-data add-comment-key)
                                add-comment-activity-data (get add-comment-data (:uuid activity-data))
                                force-update-uuid (utils/activity-uuid)]
                            (reset! (::initial-add-comment s) (or activity-add-comment-data ""))
                            (reset! (::show-post-button s) (should-focus-field? s))
                            (reset! (::add-comment-force-update s) force-update-uuid)
                            (dis/dispatch! [:input [:add-comment-force-update add-comment-key] force-update-uuid]))
                          s)
                          :did-mount (fn [s]
                           (let [activity-data (first (:rum/args s))
                                 follow-up (first (filterv #(= (-> % :assignee :user-id) (jwt/user-id)) (:follow-ups activity-data)))]
                             ;; Default to complete follow-up on add comment if user has one
                             (when (and follow-up
                                        (not (:completed? follow-up)))
                               (reset! (::complete-follow-up s) true)))
                           (me-media-utils/setup-editor s add-comment-did-change (me-options (second (:rum/args s))))
                           (let [add-comment-node (rum/ref-node s "editor-node")]
                             (when (should-focus-field? s)
                               (.focus add-comment-node)
                               (utils/after 0
                                #(utils/to-end-of-content-editable add-comment-node))))
                           (utils/after 2500 #(js/emojiAutocomplete))
                           s)
                          :will-update (fn [s]
                           (me-media-utils/setup-editor s add-comment-did-change (me-options (second (:rum/args s))))
                           (let [data @(drv/get-ref s :media-input)
                                 video-data (:media-video data)]
                              (when (and @(:me/media-video s)
                                         (or (= video-data :dismiss)
                                             (map? video-data)))
                                (when (or (= video-data :dismiss)
                                          (map? video-data))
                                  (reset! (:me/media-video s) false)
                                  (dis/dispatch! [:update [:media-input] #(dissoc % :media-video)]))
                                (if (map? video-data)
                                  (me-media-utils/media-video-add s @(:me/media-picker-ext s) video-data)
                                  (me-media-utils/media-video-add s @(:me/media-picker-ext s) nil))))
                           ;; Handle force updates of the add comment body
                           (let [args (vec (:rum/args s))
                                 activity-data (first args)
                                 parent-comment-uuid (second args)
                                 edit-comment-data (get (vec (:rum/args s)) 3 nil)
                                 add-comment-key (dis/add-comment-string-key (:uuid activity-data) parent-comment-uuid (:uuid edit-comment-data))
                                 add-comment-force-update @(drv/get-ref s :add-comment-force-update)
                                 force-update (get add-comment-force-update add-comment-key)]
                             (when-not (= @(::add-comment-force-update s) force-update)
                               (reset! (::add-comment-force-update s) force-update)
                               (let [add-comment-data @(drv/get-ref s :add-comment-data)
                                     activity-add-comment-data (get add-comment-data add-comment-key)
                                     add-comment-text (or activity-add-comment-data "")]
                                 (reset! (::add-button-disabled s) (clojure.string/blank? add-comment-text))
                                 (reset! (::initial-add-comment s) add-comment-text))))
                           s)
                          :will-unmount (fn [s]
                           (when @(:me/editor s)
                             (.destroy @(:me/editor s))
                             (reset! (:me/editor s) nil))
                           s)}
  [s activity-data parent-comment-uuid dismiss-reply-cb edit-comment-data]
  (let [_add-comment-data (drv/react s :add-comment-data)
        _media-input (drv/react s :media-input)
        _team-roster (drv/react s :team-roster)
        _add-comment-force-update (drv/react s :add-comment-force-update)
        add-comment-focus (drv/react s :add-comment-focus)
        current-user-data (drv/react s :current-user-data)
        add-comment-class (str "add-comment-" @(::add-comment-id s))
        container-class (str "add-comment-box-container-" @(::add-comment-id s))
        is-focused? (should-focus-field? s)
        should-hide-post-button (and ;; Hide post button only for the last add comment field, not
                                     ;; for the reply to comments
                                     (not parent-comment-uuid)
                                     (not @(::show-post-button s))
                                     (not is-focused?))
        follow-up (first (filterv #(= (-> % :assignee :user-id) (jwt/user-id)) (:follow-ups activity-data)))
        complete-follow-up-link (when follow-up
                                  (utils/link-for (:links follow-up) "mark-complete" "POST"))
        show-follow-up-button? (and follow-up
                                    (not (:completed? follow-up))
                                    complete-follow-up-link
                                    (not parent-comment-uuid))]
    [:div.add-comment-box-container
      {:class container-class}
      [:div.add-comment-box.group
        (user-avatar-image current-user-data)
        [:div.add-comment-internal
          {:class (when-not should-hide-post-button "active")}
          [:div.add-comment.emoji-autocomplete.emojiable.oc-mentions.oc-mentions-hover.editing
           {:ref "editor-node"
            :class (utils/class-set {add-comment-class true
                                     :medium-editor-placeholder-hidden @(::did-change s)
                                     utils/hide-class true})
            :on-focus #(focus-add-comment s)
            :on-blur #(disable-add-comment-if-needed s)
            :on-key-down (fn [e]
                          (let [add-comment-node (rum/ref-node s "editor-node")]
                            (when (and (= (.-key e) "Escape")
                                       (= (.-activeElement js/document) add-comment-node))
                              (if edit-comment-data
                                (when (fn? dismiss-reply-cb)
                                  (dismiss-reply-cb true))
                                (.blur add-comment-node)))
                            (when (and (= (.-activeElement js/document) add-comment-node)
                                       (.-metaKey e)
                                       (= (.-key e) "Enter"))
                              (send-clicked e s))))
            :content-editable true
            :dangerouslySetInnerHTML #js {"__html" @(::initial-add-comment s)}}]]
        (when @(:me/showing-media-video-modal s)
          [:div.video-container
            {:ref :video-container}
            (media-video-modal {:fullscreen false
                                :dismiss-cb #(do
                                              (me-media-utils/media-video-add s @(:me/media-picker-ext s) nil)
                                              (reset! (:me/showing-media-video-modal s) false))
                                :offset-element-selector [(keyword (str "div." container-class))]
                                :outer-container-selector [(keyword (str "div." container-class))]})])
        (when @(:me/showing-gif-selector s)
          (giphy-picker {:fullscreen false
                         :pick-emoji-cb (fn [gif-obj]
                                         (reset! (:me/showing-gif-selector s) false)
                                         (me-media-utils/media-gif-add s @(:me/media-picker-ext s) gif-obj))
                         :offset-element-selector [(keyword (str "div." container-class))]
                         :outer-container-selector [(keyword (str "div." container-class))]}))
        [:div.add-comment-footer
          {:class (when should-hide-post-button "hide-footer")}
          [:button.mlb-reset.send-btn
            {:on-click #(when-not @(::add-button-disabled s)
                          (send-clicked % s))
             :disabled @(::add-button-disabled s)}
            (if edit-comment-data
              "Save"
              "Comment")]
          (when (and parent-comment-uuid
                     (fn? dismiss-reply-cb))
            [:button.mlb-reset.close-reply-bt
              {:on-click (fn [_]
                          (if @(::did-change s)
                            (let [alert-data {:icon "/img/ML/trash.svg"
                                              :action "cancel-comment-edit"
                                              :message "Are you sure you want to cancel? All your changes to this comment will be lost."
                                              :link-button-title "Keep"
                                              :link-button-cb #(alert-modal/hide-alert)
                                              :solid-button-style :red
                                              :solid-button-title "Yes"
                                              :solid-button-cb (fn []
                                                                (dismiss-reply-cb true)
                                                                (alert-modal/hide-alert))}]
                              (alert-modal/show-alert alert-data))
                            (dismiss-reply-cb true)))
               :data-toggle (if (responsive/is-tablet-or-mobile?) "" "tooltip")
               :data-placement "top"
               :title (if edit-comment-data "Cancel edit" "Close")}])
          (emoji-picker {:add-emoji-cb #(add-comment-did-change s)
                         :width 24
                         :height 24
                         :position "top"
                         :default-field-selector (str "div." add-comment-class)
                         :container-selector (str "div." add-comment-class)})
          (when show-follow-up-button?
            [:div.buttons-separator])
          (when show-follow-up-button?
            [:button.mlb-reset.complete-follow-up
              {:class (when-not @(::complete-follow-up s) "unselected")
               :data-toggle "tooltip"
               :data-placement "top"
               :data-container "body"
               :title "Complete follow-up when the comment is posted"
               :on-click #(do
                           (utils/event-stop %)
                           (swap! (::complete-follow-up s) not))}
              (carrot-checkbox {:selected @(::complete-follow-up s)})
              "Complete follow-up"])]]]))
