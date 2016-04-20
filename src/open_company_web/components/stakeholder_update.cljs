(ns open-company-web.components.stakeholder-update
  (:require [om.core :as om :include-macros true]
            [om-tools.core :as om-core :refer-macros (defcomponent)]
            [om-tools.dom :as dom :include-macros true]
            [open-company-web.router :as router]
            [open-company-web.api :as api]
            [open-company-web.lib.utils :as utils]
            [open-company-web.components.navbar :refer (navbar)]
            [open-company-web.components.topic-body :refer (topic-body)]
            [open-company-web.components.company-header :refer [company-header]]
            [open-company-web.components.stakeholder-update-header :refer (stakeholder-update-header)]
            [open-company-web.components.stakeholder-update-footer :refer (stakeholder-update-footer)]
            [open-company-web.components.ui.link :refer (link)]
            [open-company-web.components.ui.side-drawer :refer (side-drawer)]
            [open-company-web.components.ui.drawer-toggler :refer (drawer-toggler)]
            [cljs-dynamic-resources.core :as cdr]
            [goog.style :refer (setStyle)]
            [goog.events :as events]
            [goog.history.EventType :as EventType]
            [clojure.string :as clj-string]))

(defn get-key-from-sections [sections]
  (clojure.string/join
    (map #(str (name (get % 0)) (clojure.string/join (get % 1))) sections)))

(defcomponent prior-updates [data owner]
  (render [_]
    (dom/div "")))

(defcomponent stakeholder-update-topic [data owner]
  (render [_]
    (let [section (:section data)
          section-data (:section-data data)
          headline (:headline section-data)]
      (dom/div {:class "update-topic"}

        ;; topic title
        (dom/div {:class "topic-title"} (:title section-data))

        ;; topic headline
        (when headline
          (dom/div {:class "topic-headline"} headline))
        
        ;; topic body
        (om/build topic-body {:section section
                              :section-data section-data
                              :currency (:currency data)
                              :expanded true})))))

(defn save-stakeholder-update [old-stakeholder-update active-topics]
  (let [new-stakeholder-update (assoc old-stakeholder-update :sections (vec active-topics))]
    (api/patch-stakeholder-update new-stakeholder-update)))

(defcomponent selected-topics [data owner]

  (init-state [_] {
    :drawer-open false})

  (render-state [_ {:keys [drawer-open]}]
    (let [slug (keyword (:slug @router/path))
          company-data (get data slug)
          stakeholder-update (:stakeholder-update company-data)
          section-keys (map keyword (:sections stakeholder-update))
          all-sections-key (get-key-from-sections (:sections company-data))]
      (dom/div {:class "update-sections"
                :key all-sections-key}

        (dom/div {:class "stakeholder-update-drawer"}
          (when (and (not (:read-only company-data))
                     (not (utils/is-mobile))
                     (not (:loading data)))
            ;; drawer toggler
            (om/build drawer-toggler {:close (not drawer-open)
                                      :click-cb #(om/update-state! owner :drawer-open not)}))
          (when-not (or (:read-only company-data)
                        (utils/is-mobile)
                        (:loading data)))
          ;; side drawer
          (let [all-sections (vec (flatten (vals (:sections company-data))))
                all-section-keys (map keyword all-sections)
                list-data (merge data {:active true
                                       :all-topics (select-keys company-data all-section-keys)
                                       :active-topics-list (:sections stakeholder-update)})
                list-opts {:did-change-active-topics (partial save-stakeholder-update stakeholder-update)}]
            (om/build side-drawer {:open drawer-open
                                   :list-key "su-update"
                                   :list-data list-data}
                                  {:opts {:list-opts list-opts
                                          :bg-click-cb #(om/set-state! owner :drawer-open false)}})))

        (dom/div {:class "update-sections-internal"}
          (dom/div {:class "update-sections-internal-width"}
            (dom/div {:class "overlay"})
            (for [section section-keys]
              (let [section-data (section company-data)]
                (when-not (and (:read-only section-data) (:placeholder section-data))
                  (om/build stakeholder-update-topic {
                                          :section-data section-data
                                          :section section
                                          :currency (:currency company-data)
                                          :loading (:loading data)}))))))))))

(defn fix-buttons-position [owner]
  (when-not (om/get-state owner :fixed-buttons-position)
    (when-let [su-header (om/get-ref owner "stakeholder-update-header")]
      (when-let [buttons (om/get-ref owner "floating-buttons")]
        (let [offset-top (utils/offset-top su-header)]
          (om/set-state! owner :fixed-buttons-position offset-top)
          (setStyle buttons #js {:top (str offset-top "px")}))))))

(defn- get-title [data]
  (let [title (:title data)]
    (if (clj-string/blank? title)
      (let [js-date (utils/js-date)
            month (utils/month-string (utils/add-zero (.getMonth js-date)))
            year (.getFullYear js-date)]
        (str month " " year " Update"))
      title)))

(def before-unload-message "You have unsaved changes.")

(defn save-click [owner]
  (let [title (om/get-state owner :title)
        intro-body (or (om/get-state owner :out-intro) (om/get-state owner :intro))
        outro-body (or (om/get-state owner :out-outro) (om/get-state owner :outro))]
    (api/patch-stakeholder-update {:title title
                                   :intro {:body intro-body}
                                   :sections (:sections (om/get-props owner))
                                   :outro {:body outro-body}})))

(defn cancel-click [owner]
  (om/set-state! owner :has-changes false)
  (let [current-state (om/get-state owner)]
    (om/set-state! owner :title (:initial-title current-state))
    (om/set-state! owner :intro (:initial-intro current-state))
    (om/set-state! owner :outro (:initial-outro current-state))))

(defn get-state [data current-state]
  (let [slug (keyword (:slug @router/path))
        company-data (get data slug)
        su-data (:stakeholder-update company-data)
        title (get-title su-data)
        intro (:body (:intro su-data))
        outro (:body (:outro su-data))]
    {:title title
     :initial-title title
     :history-listener-id (or (:history-listener-id current-state) nil)
     :has-changes false
     :initial-intro intro
     :intro intro
     :initial-outro outro
     :outro outro
     :fixed-buttons-position (or (:fixed-buttons-position current-state) false)
     :drawer-open (or (:drawer-open current-state) false)}))

(defn change-cb [owner k v]
  (om/set-state! owner :has-changes true)
  (cond
    (= k :outro)
    (om/set-state! owner :out-outro v)

    (= k :intro)
    (om/set-state! owner :out-intro v)

    :else
    (om/set-state! owner k v)))

(defcomponent stakeholder-update [data owner]

  (init-state [_]
    (cdr/add-style! "/css/medium-editor/medium-editor.css")
    (cdr/add-style! "/css/medium-editor/default.css")
    (get-state data nil))

  (will-unmount [_]
    (when-not (utils/is-test-env?)
      ; re enable the route dispatcher
      (reset! open-company-web.core/prevent-route-dispatch false)
      ; remove the onbeforeunload handler
      (set! (.-onbeforeunload js/window) nil)
      ; remove history change listener
      (events/unlistenByKey (om/get-state owner :history-listener-id))))

  (did-mount [_]
    (fix-buttons-position owner)
    (when-not (utils/is-test-env?)
      (utils/after 100 (fn []
        (reset! open-company-web.core/prevent-route-dispatch true)
        (let [win-location (.-location js/window)
              current-token (str (.-pathname win-location) (.-search win-location) (.-hash win-location))
              listener (events/listen open-company-web.core/history EventType/NAVIGATE
                         #(when-not (= (.-token %) current-token)
                            (if (om/get-state owner :has-changes)
                              (if (js/confirm (str before-unload-message " Are you sure you want to leave this page?"))
                                ; dispatch the current url
                                (open-company-web.core/route-dispatch! (router/get-token))
                                ; go back to the previous token
                                (.setToken open-company-web.core/history current-token))
                              ; dispatch the current url
                              (open-company-web.core/route-dispatch! (router/get-token)))))]
          (om/set-state! owner :history-listener-id listener))))))

  (did-update [_ _ _]
    (fix-buttons-position owner))

  (will-receive-props [_ next-props]
    (get-state next-props (om/get-state owner)))

  (render-state [_ {:keys [drawer-open has-changes title intro outro]}]
    (let [slug (keyword (:slug @router/path))
          company-data (get data slug)
          stakeholder-update-data (:stakeholder-update company-data)]

      (utils/update-page-title (str "OpenCompany - Stakeholder Update Edit - " (:name company-data)))

      (cond

        ;; The data is still loading
        (:loading data)
        (dom/div
          (dom/h4 "Loading data..."))

        ;; Stakeholder update
        (and (not (contains? data :loading)) (contains? data slug))
        (dom/div {:class (utils/class-set {:stakeholder-update true
                                           :navbar-offset (not (utils/is-mobile))})}
          ;; Company / user header
          (when-not (utils/is-mobile)
            (om/build navbar data))
            
          ;; Company header
          (om/build company-header {
              :editing-topic true ; no category nav
              :company-data company-data
              :stakeholder-update true})
          
          (dom/div #js {:className "update-internal"
                        :ref "update-internal"}
          
            (dom/div {:class "sections group"}; col-md-9 col-sm-12"}
              (dom/div #js {:className "floating-buttons group"
                            :ref "floating-buttons"}
                (when has-changes
                  (dom/button {:class "cancel"
                               :on-click #(cancel-click owner)} "cancel"))
                (when has-changes
                  (dom/button {:class "save"
                               :on-click #(save-click owner)} "Save")))
              ;; Stakeholder update header
              (dom/div #js {:className "stakeholder-update-header"
                            :ref "stakeholder-update-header"}
                (om/build stakeholder-update-header {:title title
                                                     :intro intro}
                                                    {:opts {:change-cb (partial change-cb owner)}}))
              ;; Stakeholder update topics
              (om/build selected-topics data)
              ;; Stakeholder update footer
              (om/build stakeholder-update-footer {:outro outro}
                                                  {:opts {:change-cb (partial change-cb owner)}})
              ;; Dashboard link
              (when (utils/is-mobile)
                (dom/div {:class "dashboard-link"}
                  (om/build link {:href (str "/" (:slug company-data)) :name "View Dashboard"}))))
            
            (dom/div {:class "col-md-3 col-sm-0"} 
              (om/build prior-updates company-data))))


        ;; Error fallback
        :else
        (dom/div
          (dom/h2 (str (name slug) " not found"))
          (om/build link {:href "/" :name "Back home"}))))))