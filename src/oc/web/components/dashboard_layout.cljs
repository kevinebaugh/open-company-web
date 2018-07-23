(ns oc.web.components.dashboard-layout
  (:require [rum.core :as rum]
            [cuerdas.core :as s]
            [org.martinklepsch.derivatives :as drv]
            [taoensso.timbre :as timbre]
            [oc.web.lib.jwt :as jwt]
            [oc.web.urls :as oc-urls]
            [oc.web.router :as router]
            [oc.web.dispatcher :as dis]
            [oc.web.lib.utils :as utils]
            [oc.web.lib.cookies :as cook]
            [oc.web.mixins.ui :as ui-mixins]
            [oc.web.lib.responsive :as responsive]
            [oc.web.actions.nux :as nux-actions]
            [oc.web.actions.section :as section-actions]
            [oc.web.actions.activity :as activity-actions]
            [oc.web.components.all-posts :refer (all-posts)]
            [oc.web.components.ui.empty-org :refer (empty-org)]
            [oc.web.components.ui.empty-board :refer (empty-board)]
            [oc.web.components.section-stream :refer (section-stream)]
            [oc.web.components.entries-layout :refer (entries-layout)]
            [oc.web.components.ui.dropdown-list :refer (dropdown-list)]
            [oc.web.components.ui.section-editor :refer (section-editor)]
            [oc.web.components.navigation-sidebar :refer (navigation-sidebar)]
            [goog.events :as events]
            [goog.events.EventType :as EventType]))

(def min-scroll 50)
(def max-scroll 92)

(defn document-scroll-top []
  (if (.-body js/document)
    (max (.-pageYOffset js/window)
         (.-scrollTop (.-documentElement js/document))
         (.-scrollTop (.-body js/document)))
    0))

(defn calc-opacity [scroll-top]
  (let [fixed-scroll-top (/
                          (*
                           (- (min scroll-top max-scroll) 50)
                           100)
                          (- max-scroll min-scroll))]
    (max 0 (min (/ fixed-scroll-top 100) 1))))

(defn did-scroll [e s]
  (let [entry-floating (js/$ "#new-entry-floating-btn-container")]
    (when (pos? (.-length entry-floating))
      (let [scroll-top (document-scroll-top)
            opacity (if (responsive/is-tablet-or-mobile?)
                      1
                      (calc-opacity scroll-top))]
        (.css entry-floating #js {:opacity opacity
                                 :display (if (pos? opacity) "block" "none")}))))
  (let [dashboard-layout (rum/dom-node s)]
    (if (>= (.-scrollY js/window) 64)
      (.add (.-classList dashboard-layout) "sticky-board-name")
      (.remove (.-classList dashboard-layout) "sticky-board-name"))))

(defn get-default-section [s]
  (let [editable-boards @(drv/get-ref s :editable-boards)
        org-slug (router/current-org-slug)
        cookie-name (router/last-used-board-slug-cookie org-slug)
        cookie-value (cook/get-cookie cookie-name)
        board-from-cookie (some #(when (= (:slug %) cookie-value) %) (vals editable-boards))
        board-data (or board-from-cookie (first (sort-by :name (vals editable-boards))))]
    {:board-name (:name board-data)
     :board-slug (:slug board-data)}))

(defn get-board-for-edit [s]
  (let [board-data @(drv/get-ref s :board-data)
        route @(drv/get-ref s :route)
        is-all-posts (or (utils/in? (:route route) "all-posts")
                         (:from-all-posts route))
        is-must-see (utils/in? (:route route) "must-see")
        is-drafts-board (= (:slug board-data) utils/default-drafts-board-slug)]
    (if (or is-drafts-board is-all-posts is-must-see)
      (get-default-section s)
      {:board-slug (:slug board-data)
       :board-name (:name board-data)})))

(defn win-width
  "Save the window width in the state."
  [s]
  (reset! (::ww s) (responsive/ww)))

(defn- update-tooltips [s]
  (when-let [$compose-button (js/$ (rum/ref-node s :top-compose-button))]
    (.tooltip $compose-button (.attr $compose-button "data-viewer")))
  (when-let [$board-switcher (js/$ (rum/ref-node s "board-switcher"))]
    (.tooltip $board-switcher)
    (doto $board-switcher
     (.tooltip "hide")
     (.tooltip "fixTitle"))))

(defn compose-fn [s type]
  (utils/remove-tooltips)
  (reset! (::showing-compose-picker s) false)
  (case type
    :video (activity-actions/capture-video (get-board-for-edit s))
    (activity-actions/entry-edit (get-board-for-edit s)))
  ;; If the add post tooltip is visible
  (when @(drv/get-ref s :show-add-post-tooltip)
    ;; Dismiss it and bring up the invite people tooltip
    (utils/after 1000 nux-actions/dismiss-add-post-tooltip)))

(rum/defcs dashboard-layout < rum/reactive
                              ;; Derivative
                              (drv/drv :route)
                              (drv/drv :org-data)
                              (drv/drv :board-data)
                              (drv/drv :all-posts)
                              (drv/drv :must-see)
                              (drv/drv :editable-boards)
                              (drv/drv :show-section-editor)
                              (drv/drv :show-section-add)
                              (drv/drv :show-add-post-tooltip)
                              (drv/drv :mobile-navigation-sidebar)
                              ;; Locals
                              (rum/local nil ::force-update)
                              (rum/local nil ::ww)
                              (rum/local nil ::scroll-listener)
                              (rum/local nil ::show-top-boards-dropdown)
                              (rum/local nil ::show-floating-boards-dropdown)
                              (rum/local nil ::board-switch)
                              (rum/local false ::showing-compose-picker)
                              ;; Mixins
                              (ui-mixins/render-on-resize win-width)
                              {:before-render (fn [s]
                                ;; Check if it needs any NUX stuff
                                (nux-actions/check-nux)
                                s)
                               :will-mount (fn [s]
                                (win-width s)
                                (let [board-view-cookie (router/last-board-view-cookie (router/current-org-slug))
                                      cookie-value (cook/get-cookie board-view-cookie)
                                      board-view (or (keyword cookie-value) :stream)
                                      fixed-board-view (if (responsive/is-tablet-or-mobile?)
                                                        :stream
                                                        board-view)]
                                  (reset! (::board-switch s) fixed-board-view))
                                s)
                               :did-mount (fn [s]
                                (when-not (utils/is-test-env?)
                                  (.tooltip (js/$ "[data-toggle=\"tooltip\"]"))
                                  (reset! (::scroll-listener s)
                                   (events/listen js/window EventType/SCROLL #(did-scroll % s))))
                                (update-tooltips s)
                                s)
                               :will-unmount (fn [s]
                                (when-not (utils/is-test-env?)
                                  (when @(::scroll-listener s)
                                    (events/unlistenByKey @(::scroll-listener s))
                                    (reset! (::scroll-listener s) nil)))
                                s)
                               :did-update (fn [s]
                                (update-tooltips s)
                                s)}
  [s]
  (let [org-data (drv/react s :org-data)
        board-data-react (drv/react s :board-data)
        all-posts-data (drv/react s :all-posts)
        must-see-data (drv/react s :must-see)
        route (drv/react s :route)
        is-all-posts (or (utils/in? (:route route) "all-posts")
                         (:from-all-posts route))
        is-must-see (utils/in? (:route route) "must-see")
        board-data (cond
                     is-must-see
                     must-see-data

                     :default
                     board-data-react)
        current-activity-id (router/current-activity-id)
        is-mobile? (responsive/is-tablet-or-mobile?)
        empty-board? (zero? (count (:fixed-items board-data)))
        is-drafts-board (= (:slug board-data) utils/default-drafts-board-slug)
        all-boards (drv/react s :editable-boards)
        board-view-cookie (router/last-board-view-cookie (router/current-org-slug))
        show-section-editor (drv/react s :show-section-editor)
        show-section-add (drv/react s :show-section-add)
        drafts-board (first (filter #(= (:slug %) utils/default-drafts-board-slug) (:boards org-data)))
        drafts-link (utils/link-for (:links drafts-board) "self")
        board-switch (::board-switch s)
        show-drafts (pos? (:count drafts-link))
        mobile-navigation-sidebar (drv/react s :mobile-navigation-sidebar)
        can-compose (or (and (or is-all-posts
                                 is-drafts-board)
                             (pos? (count all-boards)))
                        (and (not is-all-posts)
                             (not is-drafts-board)
                             board-data
                             (not (:read-only board-data))))
        should-show-top-compose (jwt/user-is-part-of-the-team (:team-id org-data))]
      ;; Entries list
      [:div.dashboard-layout.group
        ;; Show create new section for desktop
        (when (and show-section-add
                   (not (responsive/is-tablet-or-mobile?)))
          [:div.section-add
            {:class (when show-drafts "has-drafts")}
           (section-editor nil (fn [board-data note]
                                  (when board-data
                                    (section-actions/section-save board-data note
                                     #(dis/dispatch! [:input [:show-section-add] false])))))])
        [:div.dashboard-layout-container.group
          (when-not is-mobile?
            (navigation-sidebar))
          ;; Show the board always on desktop and
          ;; on mobile only when the navigation menu is not visible
          (when (or (not is-mobile?)
                    (not mobile-navigation-sidebar))
            [:div.board-container.group
              {:class (when is-all-posts "all-posts-container")}
              ;; Board name row: board name, settings button and say something button
              [:div.board-name-container.group
                {:on-click #(dis/dispatch! [:input [:mobile-navigation-sidebar] (not mobile-navigation-sidebar)])}
                ;; Board name and settings button
                [:div.board-name
                  (when (router/current-board-slug)
                    [:div.board-name-with-icon
                      {:dangerouslySetInnerHTML (cond
                                                 is-all-posts
                                                 #js {"__html" "All posts"}

                                                 is-must-see
                                                 #js {"__html" "Must see"}

                                                 :default
                                                 (utils/emojify (:name board-data)))}])
                  ;; Settings button
                  (when (and (router/current-board-slug)
                             (not is-all-posts)
                             (not is-must-see)
                             (not (:read-only board-data)))
                    [:div.board-settings-container.group
                      [:button.mlb-reset.board-settings-bt
                        {:data-toggle (when-not is-mobile? "tooltip")
                         :data-placement "top"
                         :data-container "body"
                         :data-delay "{\"show\":\"500\", \"hide\":\"0\"}"
                         :title (str (:name board-data) " settings")
                         :on-click #(dis/dispatch! [:input [:show-section-editor] true])}]
                      ;; Show section settings for desktop
                      (when (and show-section-editor
                                 (not (responsive/is-tablet-or-mobile?)))
                        (section-editor board-data
                         (fn [section-data note]
                           (when section-data
                             (section-actions/section-save section-data note
                               #(dis/dispatch! [:input [:show-section-editor] false]))))))])
                  (when (= (:access board-data) "private")
                    [:div.private-board
                      {:data-toggle "tooltip"
                       :data-placement "top"
                       :data-container "body"
                       :data-delay "{\"show\":\"500\", \"hide\":\"0\"}"
                       :title (if (= (router/current-board-slug) utils/default-drafts-board-slug)
                               "Only visible to you"
                               "Only visible to invited team members")}
                      "Private"])
                  (when (= (:access board-data) "public")
                    [:div.public-board
                      {:data-toggle "tooltip"
                       :data-placement "top"
                       :data-container "body"
                       :data-delay "{\"show\":\"500\", \"hide\":\"0\"}"
                       :title "Visible to the world, including search engines"}
                      "Public"])]
                ;; Add entry button
                (when should-show-top-compose
                  [:div.new-post-top-dropdown-container.group
                    {:on-mouse-leave #(reset! (::showing-compose-picker s) false)}
                    (let [show-tooltip? (boolean (and should-show-top-compose (not can-compose)))]
                      [:button.mlb-reset.mlb-default.add-to-board-top-button.group
                        {:ref :top-compose-button
                         :on-click #(when can-compose (swap! (::showing-compose-picker s) not))
                         :class (when-not can-compose "disabled")
                         :title (when show-tooltip? "You are a view-only user.")
                         :data-viewer (if show-tooltip? "enable" "disable")
                         :data-toggle (when show-tooltip? "tooltip")
                         :data-placement (when show-tooltip? "top")
                         :data-container (when show-tooltip? "body")
                         :data-delay "{\"show\":\"500\", \"hide\":\"0\"}"}
                        [:div.add-to-board-plus]
                        [:label.add-to-board-label
                          "New"]])
                    (when @(::showing-compose-picker s)
                      [:div.compose-type-dropdown-wrapper
                        [:div.compose-type-dropdown
                          [:button.mlb-reset.compose-type.compose-type-post
                            {:on-click #(compose-fn s :post)}
                            "Text"]
                          [:div.compose-type-separator]
                          [:button.mlb-reset.compose-type.compose-type-video
                            {:on-click #(compose-fn s :video)}
                            "Video"]]])
                    (when @(::show-top-boards-dropdown s)
                      (dropdown-list
                       {:items (map
                                #(-> %
                                  (select-keys [:name :slug])
                                  (clojure.set/rename-keys {:name :label :slug :value}))
                                (vals all-boards))
                        :value ""
                        :on-blur #(reset! (::show-top-boards-dropdown s) false)
                        :on-change (fn [item]
                                     (reset! (::show-top-boards-dropdown s) false)
                                     (activity-actions/entry-edit {:board-slug (:value item)
                                                                   :board-name (:label item)}))}))])
                (when-not is-mobile?
                  [:div.board-switcher.group
                    (let [grid-view? (= @board-switch :grid)]
                      [:button.mlb-reset.board-switcher-bt
                        {:class (if grid-view? "stream-view" "grid-view")
                         :ref "board-switcher"
                         :on-click #(do
                                      (reset! board-switch (if grid-view? :stream :grid))
                                      (cook/set-cookie! board-view-cookie (if grid-view? "stream" "grid")
                                       (* 60 60 24 365)))
                         :data-toggle "tooltip"
                         :data-placement "top"
                         :data-container "body"
                         :data-delay "{\"show\":\"500\", \"hide\":\"0\"}"
                         :title (if grid-view? "Stream view" "Grid view")}])])]
              (when (drv/react s :show-add-post-tooltip)
                [:div.add-post-tooltip-container.group
                  [:button.mlb-reset.add-post-tooltip-dismiss
                    {:on-click #(nux-actions/dismiss-add-post-tooltip)}]
                  [:div.add-post-tooltip-icon]
                  [:div.add-post-tooltips
                    [:div.add-post-tooltip
                      "Welcome! Now you’re ready to post new updates and information for your team."]
                    [:div.add-post-tooltip.second-line
                      "You can delete the sample post at anytime."]]
                  [:div.add-post-tooltip-arrow]
                  [:button.mlb-reset.add-post-tooltip-compose-bt
                    {:on-click #(compose-fn s :post)}
                    "Create new post"]])
              ;; Board content: empty org, all posts, empty board, drafts view, entries view
              (cond
                ;; No boards
                (zero? (count (:boards org-data)))
                (empty-org)
                ;; All Posts
                (and is-all-posts
                     (= @board-switch :stream))
                (all-posts)
                ;; Empty board
                empty-board?
                (empty-board)
                ;; Layout boards activities
                :else
                (cond
                  ;; Entries grid view
                  (= @board-switch :grid)
                  (entries-layout)
                  ;; Entries stream view
                  :else
                  (section-stream)))
              ;; Add entry floating button
              (when can-compose
                (let [opacity (if (responsive/is-tablet-or-mobile?)
                                1
                                (calc-opacity (document-scroll-top)))]
                  [:div.new-post-floating-dropdown-container.group
                    {:id "new-entry-floating-btn-container"
                     :style {:opacity opacity
                             :display (if (pos? opacity) "block" "none")}}
                    [:button.mlb-reset.mlb-default.add-to-board-floating-button
                      {:data-placement "left"
                       :data-container "body"
                       :data-toggle (when-not is-mobile? "tooltip")
                       :title "Start a new post"
                       :data-delay "{\"show\":\"500\", \"hide\":\"0\"}"
                       :on-click #(compose-fn s :post)}
                      [:div.add-to-board-plus]]]))])]]))