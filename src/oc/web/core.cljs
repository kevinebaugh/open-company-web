(ns oc.web.core
  (:require [secretary.core :as secretary :refer-macros (defroute)]
            [dommy.core :as dommy :refer-macros (sel1)]
            [taoensso.timbre :as timbre]
            [rum.core :as rum]
            [org.martinklepsch.derivatives :as drv]
            [cuerdas.core :as s]
            [oc.web.rum-utils :as ru]
            ;; Pull in functions for interfacing with Expo mobile app
            [oc.web.expo :as expo]
            [oc.shared.useragent :as ua]
            ;; Pull in all the stores to register the events
            [oc.web.actions]
            [oc.web.stores.routing]
            [oc.web.stores.jwt]
            [oc.web.stores.org]
            [oc.web.stores.team]
            [oc.web.stores.user]
            [oc.web.stores.search]
            [oc.web.stores.activity]
            [oc.web.stores.comment]
            [oc.web.stores.reaction]
            [oc.web.stores.subscription]
            [oc.web.stores.section]
            [oc.web.stores.notifications]
            [oc.web.stores.reminder]
            ;; Pull in the needed file for the ws interaction events
            [oc.web.ws.interaction-client]
            [oc.web.actions.team]
            [oc.web.actions.activity :as aa]
            [oc.web.actions.org :as oa]
            [oc.web.actions.comment :as ca]
            [oc.web.actions.reaction :as ra]
            [oc.web.actions.section :as sa]
            [oc.web.actions.nux :as na]
            [oc.web.actions.jwt :as ja]
            [oc.web.actions.user :as user-actions]
            [oc.web.actions.web-app-update :as web-app-update-actions]
            [oc.web.actions.notifications :as notification-actions]
            [oc.web.actions.routing :as routing-actions]
            [oc.web.api :as api]
            [oc.web.urls :as urls]
            [oc.web.router :as router]
            [oc.web.dispatcher :as dis]
            [oc.web.local-settings :as ls]
            [oc.web.lib.jwt :as jwt]
            [oc.web.lib.utils :as utils]
            [oc.web.lib.cookies :as cook]
            [oc.web.lib.sentry :as sentry]
            [oc.web.lib.logging :as logging]
            [oc.web.lib.responsive :as responsive]
            [oc.web.components.ui.loading :refer (loading)]
            [oc.web.components.org-dashboard :refer (org-dashboard)]
            [oc.web.components.secure-activity :refer (secure-activity)]
            [oc.web.components.ui.onboard-wrapper :refer (onboard-wrapper)]
            [oc.web.components.ui.notifications :refer (notifications)]
            [oc.web.components.ui.login-wall :refer (login-wall)]))

(enable-console-print!)

(defn drv-root [component target]
  (ru/drv-root {:state dis/app-state
                :drv-spec (dis/drv-spec dis/app-state router/path)
                :component component
                :target target})
  (when-let [notifications-mount-point (sel1 [:div#oc-notifications-container])]
    (ru/drv-root {:state dis/app-state
                  :drv-spec (dis/drv-spec dis/app-state router/path)
                  :component notifications
                  :target notifications-mount-point})))

;; setup Sentry error reporting
(defonce sentry (sentry/sentry-setup))

;; Avoid warnings

(defn check-get-params [query-params]
  (when (contains? query-params :browser-type)
    ; if :browser-type is "mobile" the mobile site is forced
    ; any other value will be set as big web
    ; remove the cookie to let it calculate the type of site
    ; Rules set via css won't be affected by this
    (cook/set-cookie! :force-browser-type (:browser-type query-params) (* 60 60 24 6))))

(defn inject-loading []
  (let [target (sel1 [:div#oc-loading])]
    (drv-root loading target)))

(defn rewrite-url [& [{:keys [query-params keep-params]}]]
  (let [l (.-location js/window)
        rewrite-to (str (.-pathname l) (.-hash l))
        search-values (when (seq keep-params)
                        (remove nil?
                         (map #(when (get query-params %)
                                 (str (name %) "=" (get query-params %))) keep-params)))
        with-search (if (pos? (count search-values))
                      (str rewrite-to "?"
                        (clojure.string/join "&" search-values))
                      rewrite-to)]
    ;; Push state only if the query string has parameters or the history will have duplicates.
    (when (seq (.-search l))
      (.pushState (.-history js/window) #js {} (.-title js/document) with-search))))

(defn pre-routing [query-params & [should-rewrite-url rewrite-params]]
  ;; Add Electron classes if needed
  (let [body (sel1 [:body])]
    (when ua/desktop-app?
      (dommy/add-class! body :electron)
      (when ua/mac?
        (dommy/add-class! body :mac-electron))
      (when ua/windows?
        (dommy/add-class! body :win-electron))))
  ;; Setup timbre log level
  (when (:log-level query-params)
    (logging/config-log-level! (:log-level query-params)))
  ; make sure the menu is closed
  (let [pathname (.. js/window -location -pathname)]
    (when (not= pathname (s/lower pathname))
      (let [lower-location (str (s/lower pathname) (.. js/window -location -search) (.. js/window -location -hash))]
        (set! (.-location js/window) lower-location))))
  (swap! router/path {})
  (when (and (contains? query-params :jwt)
             (map? (js->clj (jwt/decode (:jwt query-params)))))
    ; contains :jwt, so saving it
    (ja/update-jwt (:jwt query-params)))
  (when (and (not (jwt/jwt))
             (contains? query-params :id)
             (map? (js->clj (jwt/decode (:id query-params)))))
    ; contains :id, so saving it
    (ja/update-id-token (:id query-params)))
  (check-get-params query-params)
  (when should-rewrite-url
    (rewrite-url rewrite-params))
  (when (= (:new query-params) "true")
    (swap! dis/app-state assoc :new-slack-user true))
  (inject-loading))

(defn post-routing []
  (routing-actions/routing @router/path)
  (user-actions/initial-loading))

(defn check-nux [query-params]
  (let [has-at-param (contains? query-params :at)
        loading (or (and ;; if is board page
                         (not (contains? query-params :ap))
                         ;; if the board data are not present
                         (not (dis/posts-data)))
                         ;; if the all-posts data are not preset
                    (and (contains? query-params :ap)
                         ;; this latter is used when displaying modal over AP
                         (not (dis/posts-data))))
        user-settings (when (and (contains? query-params :user-settings)
                                 (#{:profile :notifications} (keyword (:user-settings query-params))))
                        (keyword (:user-settings query-params)))
        org-settings (when (and (not user-settings)
                              (contains? query-params :org-settings)
                              (#{:org :team :invite :integrations} (keyword (:org-settings query-params))))
                       (keyword (:org-settings query-params)))
        reminders (when (and (not org-settings)
                             (contains? query-params :reminders))
                    :reminders)
        panel-stack (cond
                      org-settings [org-settings]
                      user-settings [user-settings]
                      reminders [reminders]
                      :else [])
        bot-access (when (contains? query-params :access)
                      (:access query-params))
        next-app-state {:loading loading
                        :panel-stack panel-stack
                        :bot-access bot-access}]
    (swap! dis/app-state merge next-app-state)))

(defn- read-sort-type-from-cookie
  "Read the sort order from the cookie, fallback to the default,
   if it's on drafts board force the recently posted sort since that has only that"
  [params]
  (let [last-sort-type (aa/saved-sort-type (:org params))]
    (if (or (= last-sort-type dis/other-sort-type)
            (= (:board params) utils/default-drafts-board-slug))
      :recently-posted
      dis/default-sort-type)))

;; Company list
(defn org-handler [route target component params]
  (let [org (:org params)
        board (:board params)
        sort-type (read-sort-type-from-cookie params)
        query-params (:query-params params)
        ;; First ever landing cookie name
        first-ever-cookie-name (when (= route "all-posts")
                                 (router/first-ever-ap-land-cookie (jwt/user-id)))
        ;; First ever landing cookie value
        first-ever-cookie (when (= route "all-posts")
                            (cook/get-cookie first-ever-cookie-name))]
    (if first-ever-cookie
      ;; If first ever land cookie is set redirect user to the hello url
      (do
        ;; Remove the cookie
        (cook/remove-cookie! first-ever-cookie-name)
        ;; Redirect to the first ever landing page
        (router/redirect! (urls/first-ever-all-posts org)))
      (do
        (pre-routing query-params true {:query-params query-params :keep-params [:at]})
        ;; save route
        (router/set-route! [org route] {:org org :board board :sort-type sort-type :query-params (:query-params params)})
        ;; load data from api
        (when-not (dis/org-data)
          (swap! dis/app-state merge {:loading true}))
        (check-nux query-params)
        (post-routing)
        ;; render component
        (drv-root component target)))))

(defn simple-handler [component route-name target params & [rewrite-url]]
  (pre-routing (:query-params params) rewrite-url)
  ;; save route
  (let [org (:org params)]
    (router/set-route! (vec (remove nil? [route-name org])) {:org org :query-params (:query-params params)}))
  (post-routing)
  (when-not (contains? (:query-params params) :jwt)
    ; remove rum component if mounted to the same node
    (rum/unmount target)
    ;; render component
    (drv-root component target)))

;; Component specific to a board
(defn board-handler [route target component params]
  (let [org (:org params)
        board (:board params)
        entry (:entry params)
        comment (:comment params)
        sort-type (read-sort-type-from-cookie params)
        query-params (:query-params params)
        has-at-param (contains? query-params :at)]
    (pre-routing query-params true {:query-params query-params :keep-params [:at]})
    ;; save the route
    (router/set-route!
     (vec
      (remove
       nil?
       [org board (when entry entry) (when comment comment) route]))
     {:org org
      :board board
      :activity entry
      :comment comment
      :sort-type sort-type
      :query-params query-params})
    (check-nux query-params)
    (post-routing)
    ;; render component
    (drv-root component target)))

;; Component specific to a secure activity
(defn secure-activity-handler [component route target params pre-routing?]
  (let [org (:org params)
        secure-id (:secure-id params)
        query-params (:query-params params)]
    (when pre-routing?
      (pre-routing query-params true))
    ;; save the route
    (router/set-route!
     (vec
      (remove
       nil?
       [org route secure-id]))
     {:org org
      :activity (:entry params)
      :secure-id (or secure-id (:secure-uuid (jwt/get-id-token-contents)))
      :comment (:comment params)
      :query-params query-params})
     ;; do we have the company data already?
    (when (or ;; if the company data are not present
              (not (dis/board-data))
              ;; or the entries key is missing that means we have only
              (not (:posts-list (dis/board-data)))
              ;; a subset of the company data loaded with a SU
              (not (dis/secure-activity-data)))
      (swap! dis/app-state merge {:loading true}))
    (post-routing)
    ;; render component
    (drv-root component target)))

(defn entry-handler [target params]
  (pre-routing (:query-params params) true)
  (if (and (not (jwt/jwt))
           (:secure-uuid (jwt/get-id-token-contents)))
    (secure-activity-handler secure-activity "secure-activity" target params false)
    (board-handler "activity" target org-dashboard params)))

(defn slack-lander-check [params]
  (pre-routing (:query-params params) true)
  (let [new-user (= (:new (:query-params params)) "true")]
    (when new-user
      (na/new-user-registered "slack"))
    (user-actions/lander-check-team-redirect)))

(defn google-lander-check [params]
  (pre-routing (:query-params params) true)
  (let [new-user (= (:new (:query-params params)) "true")]
    (when new-user
      (na/new-user-registered "google"))
    (user-actions/lander-check-team-redirect)))

;; Routes - Do not define routes when js/document#app
;; is undefined because it breaks tests
(if-let [target (sel1 :div#app)]
  (do
    (defroute _loading_route "/__loading" {:as params}
      (timbre/info "Routing _loading_route __loading")
      (pre-routing (:query-params params)))

    (defroute login-route urls/login {:as params}
      (timbre/info "Routing login-route" urls/login)
      ;; In case user is logged in and has a last org cookie
      
      (let [last-org-cookie (cook/get-cookie (router/last-org-cookie))]
        (if (and (jwt/jwt)
                   (seq last-org-cookie))
          (do
            ;; remove the last used org cookie
            ;; to avoid infinite loop redirects.
            (cook/remove-cookie! (router/last-org-cookie))
            ;; and redirect him there,
            (router/redirect! (urls/all-posts last-org-cookie)))
          ;; if no cookie or logged out do the login dance
          (simple-handler #(login-wall {:title "Welcome to Carrot" :desc ""}) "login" target params true))))

    (defroute signup-route urls/sign-up {:as params}
      (timbre/info "Routing signup-route" urls/sign-up)
      (when (jwt/jwt)
        (if (seq (cook/get-cookie (router/last-org-cookie)))
          (router/redirect! (urls/all-posts (cook/get-cookie (router/last-org-cookie))))
          (router/redirect! urls/sign-up-profile)))
      (simple-handler #(onboard-wrapper :lander) "sign-up" target params))

    (defroute signup-slash-route (str urls/sign-up "/") {:as params}
      (timbre/info "Routing signup-slash-route" (str urls/sign-up "/"))
      (when (and (jwt/jwt)
                 (seq (cook/get-cookie (router/last-org-cookie))))
        (router/redirect! (urls/all-posts (cook/get-cookie (router/last-org-cookie)))))
      (simple-handler #(onboard-wrapper :lander) "sign-up" target params))

    (defroute signup-profile-route urls/sign-up-profile {:as params}
      (timbre/info "Routing signup-profile-route" urls/sign-up-profile)
      (when-not (jwt/jwt)
        (router/redirect! urls/sign-up))
      (simple-handler #(onboard-wrapper :lander-profile) "sign-up" target params))

    (defroute signup-profile-slash-route (str urls/sign-up-profile "/") {:as params}
      (timbre/info "Routing signup-profile-slash-route" (str urls/sign-up-profile "/"))
      (when-not (jwt/jwt)
        (router/redirect! urls/sign-up))
      (simple-handler #(onboard-wrapper :lander-profile) "sign-up" target params))

    (defroute signup-team-route urls/sign-up-team {:as params}
      (timbre/info "Routing signup-team-route" urls/sign-up-team)
      (if (jwt/jwt)
        (when (seq (cook/get-cookie (router/last-org-cookie)))
          (router/redirect! (urls/all-posts (cook/get-cookie (router/last-org-cookie)))))
        (router/redirect! urls/sign-up))
      (simple-handler #(onboard-wrapper :lander-team) "sign-up" target params))

    (defroute signup-team-slash-route (str urls/sign-up-team "/") {:as params}
      (timbre/info "Routing signup-team-slash-route" (str urls/sign-up-team "/"))
      (if (jwt/jwt)
        (when (seq (cook/get-cookie (router/last-org-cookie)))
          (router/redirect! (urls/all-posts (cook/get-cookie (router/last-org-cookie)))))
        (router/redirect! urls/sign-up))
      (simple-handler #(onboard-wrapper :lander-team) "sign-up" target params))

    (defroute signup-update-team-route (urls/sign-up-update-team ":org") {:as params}
      (timbre/info "Routing signup-update-team-route" (urls/sign-up-update-team ":org"))
      (when-not (jwt/jwt)
        (router/redirect! urls/sign-up))
      (simple-handler #(onboard-wrapper :lander-profile) "sign-up" target params))

    (defroute signup-update-team-slash-route (str (urls/sign-up-update-team ":org") "/") {:as params}
      (timbre/info "Routing signup-update-team-slash-route" (str (urls/sign-up-update-team ":org") "/"))
      (when-not (jwt/jwt)
        (router/redirect! urls/sign-up))
      (simple-handler #(onboard-wrapper :lander-profile) "sign-up" target params))

    (defroute signup-invite-route (urls/sign-up-invite ":org") {:as params}
      (timbre/info "Routing signup-invite-route" (urls/sign-up-invite ":org"))
      (when-not (jwt/jwt)
        (router/redirect! urls/sign-up))
      (simple-handler #(onboard-wrapper :lander-invite) "sign-up" target params))

    (defroute signup-invite-slash-route (str (urls/sign-up-invite ":org") "/") {:as params}
      (timbre/info "Routing signup-invite-slash-route" (str (urls/sign-up-invite ":org") "/"))
      (when-not (jwt/jwt)
        (router/redirect! urls/sign-up))
      (simple-handler #(onboard-wrapper :lander-invite) "sign-up" target params))

    (defroute slack-lander-check-route urls/slack-lander-check {:as params}
      (timbre/info "Routing slack-lander-check-route" urls/slack-lander-check)
      ;; Check if the user already have filled the needed data or if it needs to
      (slack-lander-check params))

    (defroute slack-lander-check-slash-route (str urls/slack-lander-check "/") {:as params}
      (timbre/info "Routing slack-lander-check-slash-route" (str urls/slack-lander-check "/"))
      ;; Check if the user already have filled the needed data or if it needs to
      (slack-lander-check params))

    (defroute google-lander-check-route urls/google-lander-check {:as params}
      (timbre/info "Routing google-lander-check-route" urls/google-lander-check)
      ;; Check if the user already have filled the needed data or if it needs to
      (google-lander-check params))

    (defroute google-lander-check-slash-route (str urls/google-lander-check "/") {:as params}
      (timbre/info "Routing google-lander-check-slash-route" (str urls/google-lander-check "/"))
      ;; Check if the user already have filled the needed data or if it needs to
      (google-lander-check params))

    (defroute email-confirmation-route urls/email-confirmation {:as params}
      (timbre/info "Routing email-confirmation-route" urls/email-confirmation)
      (when-not (seq (:token (:query-params params)))
        (router/redirect! (if (jwt/jwt) (utils/your-digest-url) urls/home)))
      (cook/remove-cookie! :jwt)
      (cook/remove-cookie! :show-login-overlay)
      (simple-handler #(onboard-wrapper :email-verified) "email-verification" target params))

    (defroute password-reset-route urls/password-reset {:as params}
      (timbre/info "Routing password-reset-route" urls/password-reset)
      (when (jwt/jwt)
        (router/redirect! urls/home))
      (simple-handler #(onboard-wrapper :password-reset-lander) "password-reset" target params))

    (defroute confirm-invitation-route urls/confirm-invitation {:keys [query-params] :as params}
      (timbre/info "Routing confirm-invitation-route" urls/confirm-invitation)
      (when (empty? (:token query-params))
        (router/redirect! urls/home))
      (when (jwt/jwt)
        (cook/remove-cookie! :jwt)
        (cook/remove-cookie! :show-login-overlay))
      (simple-handler #(onboard-wrapper :invitee-lander) "confirm-invitation" target params))

    (defroute confirm-invitation-password-route urls/confirm-invitation-password {:as params}
      (timbre/info "Routing confirm-invitation-password-route" urls/confirm-invitation-password)
      (when-not (jwt/jwt)
        (router/redirect! urls/home))
      (simple-handler #(onboard-wrapper :invitee-lander-password) "confirm-invitation" target params))

    (defroute confirm-invitation-profile-route urls/confirm-invitation-profile {:as params}
      (timbre/info "Routing confirm-invitation-profile-route" urls/confirm-invitation-profile)
      (when-not (jwt/jwt)
        (router/redirect! urls/home))
      (simple-handler #(onboard-wrapper :invitee-lander-profile) "confirm-invitation" target params))

    (defroute email-wall-route urls/email-wall {:keys [query-params] :as params}
      (timbre/info "Routing email-wall-route" urls/email-wall)
      ; Email wall is shown only to not logged in users
      (when (jwt/jwt)
        (router/redirect! urls/home))
      (simple-handler #(onboard-wrapper :email-wall) "email-wall" target params true))

    (defroute email-wall-slash-route (str urls/email-wall "/") {:keys [query-params] :as params}
      (timbre/info "Routing email-wall-slash-route" (str urls/email-wall "/"))
      (when (jwt/jwt)
        (router/redirect! (urls/all-posts (cook/get-cookie (router/last-org-cookie)))))
      (simple-handler #(onboard-wrapper :email-wall) "email-wall" target params true))

    (defroute login-wall-route urls/login-wall {:keys [query-params] :as params}
      (timbre/info "Routing login-wall-route" urls/login-wall)
      ; Email wall is shown only to not logged in users
      (when (jwt/jwt)
        (router/redirect-404!))
      (simple-handler login-wall "login-wall" target params true))

    (defroute login-wall-slash-route (str urls/login-wall "/") {:keys [query-params] :as params}
      (timbre/info "Routing login-wall-slash-route" (str urls/login-wall "/"))
      (when (jwt/jwt)
        (router/redirect-404!))
      (simple-handler login-wall "login-wall" target params true))

    (defroute native-login-route urls/native-login {:keys [query-params] :as params}
      (timbre/info "Routing native-login-route" urls/native-login)
      (if (jwt/jwt)
        (router/redirect!
         (if (seq (cook/get-cookie (router/last-org-cookie)))
           (urls/all-posts (cook/get-cookie (router/last-org-cookie)))
           urls/login))
        (simple-handler #(login-wall {:title "Welcome to Carrot" :desc ""}) "login-wall" target params true)))

    (defroute native-login-slash-route (str urls/native-login "/") {:keys [query-params] :as params}
      (timbre/info "Routing native-login-slash-route" (str urls/native-login "/"))
      (if (jwt/jwt)
        (router/redirect!
         (if (seq (cook/get-cookie (router/last-org-cookie)))
           (urls/all-posts (cook/get-cookie (router/last-org-cookie)))
           urls/login))
        (simple-handler #(login-wall {:title "Welcome to Carrot" :desc ""}) "login-wall" target params true)))

    (defroute logout-route urls/logout {:as params}
      (timbre/info "Routing logout-route" urls/logout)
      (cook/remove-cookie! :jwt)
      (cook/remove-cookie! :show-login-overlay)
      (router/redirect! (if ua/pseudo-native?
                          urls/native-login
                          urls/home)))

    (defroute apps-detect-route urls/apps-detect {:as params}
      (timbre/info "Routing apps-detect-route" urls/apps-detect)
      (router/redirect!
       (cond
        ua/mac? ls/mac-app-url
        ua/windows? ls/win-app-url
        ua/ios? ls/ios-app-url
        ua/android? ls/android-app-url
        :else urls/home)))

    (defroute org-route (urls/org ":org") {:as params}
      (timbre/info "Routing org-route" (urls/org ":org"))
      (org-handler "org" target org-dashboard params))

    (defroute org-slash-route (str (urls/org ":org") "/") {:as params}
      (timbre/info "Routing org-slash-route" (str (urls/org ":org") "/"))
      (org-handler "org" target org-dashboard params))

    (defroute all-posts-route (urls/all-posts ":org") {:as params}
      (timbre/info "Routing all-posts-route" (urls/all-posts ":org"))
      (org-handler "all-posts" target org-dashboard (assoc params :board "all-posts")))

    (defroute all-posts-slash-route (str (urls/all-posts ":org") "/") {:as params}
      (timbre/info "Routing all-posts-slash-route" (str (urls/all-posts ":org") "/"))
      (org-handler "all-posts" target org-dashboard (assoc params :board "all-posts")))

    (defroute first-ever-all-posts-route (urls/first-ever-all-posts ":org") {:as params}
      (timbre/info "Routing first-ever-all-posts-route" (urls/first-ever-all-posts ":org"))
      (org-handler "all-posts" target org-dashboard (assoc params :board "all-posts")))

    (defroute first-ever-all-posts-slash-route (str (urls/first-ever-all-posts ":org") "/") {:as params}
      (timbre/info "Routing first-ever-all-posts-slash-route" (str (urls/first-ever-all-posts ":org") "/"))
      (org-handler "all-posts" target org-dashboard (assoc params :board "all-posts")))

    (defroute follow-ups-route (urls/follow-ups ":org") {:as params}
      (timbre/info "Routing follow-ups-route" (urls/follow-ups ":org"))
      (org-handler "follow-ups" target org-dashboard (assoc params :board "follow-ups")))

    (defroute follow-ups-slash-route (str (urls/follow-ups ":org") "/") {:as params}
      (timbre/info "Routing follow-ups-slash-route" (str (urls/follow-ups ":org") "/"))
      (org-handler "follow-ups" target org-dashboard (assoc params :board "follow-ups")))

    (defroute drafts-route (urls/drafts ":org") {:as params}
      (timbre/info "Routing board-route" (urls/drafts ":org"))
      (board-handler "dashboard" target org-dashboard (assoc params :board "drafts")))

    (defroute drafts-slash-route (str (urls/drafts ":org") "/") {:as params}
      (timbre/info "Routing board-slash-route" (str (urls/drafts ":org") "/"))
      (board-handler "dashboard" target org-dashboard (assoc params :board "drafts")))

    (defroute secure-activity-route (urls/secure-activity ":org" ":secure-id") {:as params}
      (timbre/info "Routing secure-activity-route" (urls/secure-activity ":org" ":secure-id"))
      (secure-activity-handler secure-activity "secure-activity" target params true))

    (defroute secure-activity-slash-route (str (urls/secure-activity ":org" ":secure-id") "/") {:as params}
      (timbre/info "Routing secure-activity-slash-route" (str (urls/secure-activity ":org" ":secure-id") "/"))
      (secure-activity-handler secure-activity "secure-activity" target params true))

    (defroute board-route (urls/board ":org" ":board") {:as params}
      (timbre/info "Routing board-route" (urls/board ":org" ":board"))
      (board-handler "dashboard" target org-dashboard params))

    (defroute board-slash-route (str (urls/board ":org" ":board") "/") {:as params}
      (timbre/info "Routing board-route-slash" (str (urls/board ":org" ":board") "/"))
      (board-handler "dashboard" target org-dashboard params))

    (defroute entry-route (urls/entry ":org" ":board" ":entry") {:as params}
      (timbre/info "Routing entry-route" (urls/entry ":org" ":board" ":entry"))
      (entry-handler target params))

    (defroute entry-slash-route (str (urls/entry ":org" ":board" ":entry") "/") {:as params}
      (timbre/info "Routing entry-route" (str (urls/entry ":org" ":board" ":entry") "/"))
      (entry-handler target params))

    (defroute comment-route (urls/comment-url ":org" ":board" ":entry" ":comment") {:as params}
      (timbre/info "Routing comment-route" (urls/comment-url ":org" ":board" ":entry" ":comment"))
      (entry-handler target params))

    (defroute comment-slash-route (str (urls/comment-url ":org" ":board" ":entry" ":comment") "/") {:as params}
      (timbre/info "Routing comment-slash-route" (str (urls/comment-url ":org" ":board" ":entry" ":comment") "/"))
      (entry-handler target params))

    (defroute not-found-route "*" []
      (timbre/info "Routing not-found-route" "*")
      ;; render component
      (if (jwt/jwt)
        (router/redirect-404!)
        (router/redirect! (str urls/login-wall "?login-redirect=" (js/encodeURIComponent (router/get-token))))))

    (defn handle-url-change [e]
      ;; we are checking if this event is due to user action,
      ;; such as initial page load, click a link, a back button, etc.
      ;; as opposed to programmatically setting the URL with the API
      (when-not (.-isNavigation e)
        ;; in this case, we're setting it so
        ;; let's scroll to the top to simulate a navigation
        (if ua/edge?
          (set! (.. js/document -scrollingElement -scrollTop) (utils/page-scroll-top))
          (js/window.scrollTo (utils/page-scroll-top) 0)))
      ;; dispatch on the token
      (secretary/dispatch! (router/get-token))
      ; remove all the tooltips
      (utils/after 100 #(utils/remove-tooltips))))
  (do
    (timbre/error "Error: div#app is not defined!")
    (sentry/capture-message! "Error: div#app is not defined!")))

(defn init []
  ;; Setup timbre log level
  (logging/config-log-level! (or (:log-level (:query-params @router/path)) ls/log-level))
  ;; Setup API requests
  (api/config-request
   #(ja/update-jwt %) ;; success jwt refresh after expire
   #(ja/logout) ;; failed to refresh jwt
   ;; network error
   #(notification-actions/show-notification (assoc utils/network-error :expire 5)))

  ;; Persist JWT in App State
  (ja/dispatch-jwt)
  (ja/dispatch-id-token)

  ;; Recall Expo push token into app state (push notification permission)
  (user-actions/recall-expo-push-token)
  ;; Get the mobile app deep link origin if we're on mobile
  (when ua/mobile-app?
    (expo/bridge-get-deep-link-origin)
    (expo/bridge-get-app-version))

  ;; Subscribe to websocket client events
  (aa/ws-change-subscribe)
  (sa/ws-change-subscribe)
  (sa/ws-interaction-subscribe)
  (oa/subscribe)
  (ra/subscribe)
  (ca/subscribe)
  (user-actions/subscribe)

  ;; Start the app update check cicle
  (web-app-update-actions/start-web-app-update-check!)

  ;; on any click remove all the shown tooltips to make sure they don't get stuck
  (.click (js/$ js/window) #(utils/remove-tooltips))
  ;; setup the router navigation only when handle-url-change and route-disaptch!
  ;; are defined, this is used to avoid crash on tests
  (when handle-url-change
    (router/setup-navigation! handle-url-change)))

(defn on-js-reload []
  (.clear js/console)
  (secretary/dispatch! (router/get-token)))
