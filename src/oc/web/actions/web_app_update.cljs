(ns oc.web.actions.web-app-update
  (:require [taoensso.timbre :as timbre]
            [oc.web.api :as api]
            [oc.web.actions.notifications :as notification-actions]
            [oc.shared.interval :as interval]
            [oc.shared.useragent :as ua]))

(declare web-app-update-interval)

(def default-update-interval-ms (* 1000 60 5)) ;; 5 minutes
(def extended-update-interval-ms (* 1000 60 60 24)) ;; 24 hours

(def update-verbage
  (cond
   (or ua/mobile?
       ua/mobile-app?)
   "Get the new release of Carrot"
   ua/pseudo-native?
   "Update"
   :else
   "Refresh page"))

(defn- on-notification-dismissed
  []
  ;; Extend the interval as to not annoy the user
  (interval/restart-interval! web-app-update-interval extended-update-interval-ms))

(defn- real-web-app-update-check
  "Check for app updates, show the notification if necessary, set a new timeout else."
  []
  (timbre/info "Checking for Carrot web app updates")
  (interval/stop-interval! web-app-update-interval)
  (api/web-app-version-check
   (fn [{:keys [success body status]}]
     (if (= status 404)
       (do
         (timbre/info "New app update avalable! Showing notification to the user")
         (notification-actions/show-notification {:title "New version of Carrot available!"
                                                  :web-app-update true
                                                  :id :web-app-update-error
                                                  :dismiss on-notification-dismissed
                                                  :dismiss-x true
                                                  :secondary-bt-title update-verbage
                                                  :secondary-bt-style :green
                                                  :secondary-bt-class :update-app-bt
                                                  :secondary-bt-cb #(js/window.location.reload)
                                                  :click #(js/window.location.reload)
                                                  :expire 0}))
       (interval/restart-interval! web-app-update-interval default-update-interval-ms)))))

(defonce web-app-update-interval (interval/make-interval {:fn real-web-app-update-check
                                                          :ms default-update-interval-ms}))

(defn start-web-app-update-check!
  "Start the app update cycle, make sure it's started only once."
  []
  (timbre/info "Starting web app update checking cycle")
  (interval/start-interval! web-app-update-interval))
