(ns oc.web.actions.nav-sidebar
  (:require [oc.web.urls :as oc-urls]
            [oc.web.router :as router]
            [oc.web.dispatcher :as dis]
            [oc.web.lib.utils :as utils]
            [oc.shared.useragent :as ua]
            [oc.web.utils.dom :as dom-utils]
            [oc.web.actions.nux :as nux-actions]
            [oc.web.lib.responsive :as responsive]
            [oc.web.actions.user :as user-actions]
            [oc.web.actions.cmail :as cmail-actions]
            [oc.web.actions.routing :as routing-actions]
            [oc.web.actions.section :as section-actions]
            [oc.web.components.ui.alert-modal :as alert-modal]))

;; Panels
;; :menu
;; :org
;; :integrations
;; :team
;; :invite
;; :billing
;; :profile
;; :notifications
;; :reminders
;; :reminder-{uuid}/:reminder-new
;; :section-add
;; :section-edit
;; :wrt-{uuid}

(defn nav-to-url! [e url]
  (when (and e
             (.-preventDefault e))
    (.preventDefault e))
  (when ua/mobile?
    (dis/dispatch! [:input [:mobile-navigation-sidebar] false]))
  (let [current-path (str (.. js/window -location -pathname) (.. js/window -location -search))]
     (if (= current-path url)
       (do
         (js/console.log "DBG refresh current")
         (routing-actions/routing @router/path)
         (user-actions/initial-loading true))
       (router/nav! url)))
  (utils/after 0 (fn []
   (cmail-actions/cmail-hide)
   (user-actions/hide-mobile-user-notifications))))

;; Push panel

(defn- push-panel
  "Push a panel at the top of the stack."
  [panel]
  (when (zero? (count (get @dis/app-state :panel-stack [])))
    (dom-utils/lock-page-scroll))
  (dis/dispatch! [:update [:panel-stack] #(vec (conj (or % []) panel))]))

(defn- pop-panel
  "Pop the panel at the top of stack from it and return it."
  []
  (let [panel-stack (:panel-stack @dis/app-state)]
    (when (pos? (count panel-stack))
      (dis/dispatch! [:update [:panel-stack] pop]))
    (when (= (count panel-stack) 1)
      (dom-utils/unlock-page-scroll))
    (peek panel-stack)))

(defn close-all-panels
  "Remove all the panels from the stack and return the stack itself.
   The return value is never used, but it's being returned to be consistent with
   the pop-panel function."
  []
  (let [panel-stack (:panel-stack @dis/app-state)]
    (dis/dispatch! [:input [:panel-stack] []])
    (dom-utils/unlock-page-scroll)
    panel-stack))

;; Section settings

(defn show-section-editor [section-slug]
  (section-actions/setup-section-editing section-slug)
  (push-panel :section-edit))

(defn hide-section-editor []
  (pop-panel))

(defn show-section-add []
  (dis/dispatch! [:input [:show-section-add-cb]
   (fn [sec-data note dismiss-action]
     (if sec-data
       (section-actions/section-save sec-data note dismiss-action)
       (when (fn? dismiss-action)
        (dismiss-action))))])
  (push-panel :section-add))

(defn show-section-add-with-callback [callback]
  (dis/dispatch! [:input [:show-section-add-cb]
   (fn [sec-data note dismiss-action]
     (callback sec-data note dismiss-action)
     (dis/dispatch! [:input [:show-section-add-cb] nil])
     (pop-panel))])
  (push-panel :section-add))

(defn hide-section-add []
  (pop-panel))

;; Reminders

(defn show-reminders []
  (push-panel :reminders))

(defn show-new-reminder []
  (push-panel :reminder-new))

(defn edit-reminder [reminder-uuid]
  (push-panel (keyword (str "reminder-" reminder-uuid))))

(defn close-reminders []
  (pop-panel))

;; Menu

(defn menu-toggle []
  (let [panel-stack (:panel-stack @dis/app-state)]
    (if (= (peek panel-stack) :menu)
      (pop-panel)
      (push-panel :menu))))

(defn menu-close []
  (pop-panel))

;; Show panels

(defn show-org-settings [panel]
  (if panel
    (push-panel panel)
    (pop-panel)))

(defn show-user-settings [panel]
  (if panel
    (push-panel panel)
    (pop-panel)))

;; WRT

(defn show-wrt [activity-uuid]
  (push-panel (keyword (str "wrt-" activity-uuid))))

(defn hide-wrt []
  (pop-panel))

;; Follow-ups users picker

(defn show-follow-ups-picker [activity-uuid callback]
  (dis/dispatch! [:input [:follow-ups-picker-callback] (fn [users-list]
   (when (fn? callback)
     (callback users-list))
   (dis/dispatch! [:input [:follow-ups-picker-callback] nil]))])
  (push-panel (keyword (str "follow-ups-picker-" (or activity-uuid "")))))

;; Integrations

(defn open-integrations-panel [e]
  (when e
    (.preventDefault e)
    (.stopPropagation e))
  (if (responsive/is-mobile-size?)
    (let [alert-data {:action "mobile-integrations-link"
                      :message "Carrot integrations need to be configured in a desktop browser."
                      :solid-button-style :green
                      :solid-button-title "OK, got it"
                      :solid-button-cb #(alert-modal/hide-alert)}]
      (alert-modal/show-alert alert-data))
    (show-org-settings :integrations)))

(set! (.-OCWebStaticOpenIntegrationsPanel js/window) open-integrations-panel)