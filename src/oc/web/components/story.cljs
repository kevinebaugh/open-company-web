(ns oc.web.components.story
  (:require [rum.core :as rum]
            [org.martinklepsch.derivatives :as drv]
            [oc.web.urls :as oc-urls]
            [oc.web.router :as router]
            [oc.web.dispatcher :as dis]
            [oc.web.lib.utils :as utils]
            [oc.web.components.comments :refer (comments)]
            [oc.web.components.reactions :refer (reactions)]
            [oc.web.components.ui.user-avatar :refer (user-avatar-image)]
            [oc.web.components.ui.interactions-summary :refer (comments-summary)]))

(rum/defcs story < rum/reactive
                   (drv/drv :story-data)
                   (rum/local false ::comments-expanded)
                   (rum/local false ::close-hovering)
                   {:will-mount (fn [s]
                                  (dis/dispatch! [:story-get (first (:rum/args s))])
                                  s)
                    :after-render (fn [s]
                                    (.tooltip (js/$ "[data-toggle=\"tooltip\"]"))
                                    s)}
  [s]
  (let [story-data (drv/react s :story-data)
        story-author (if (map? (:author story-data)) (:author story-data) (first story-data))]
    [:div.story-container
      [:div.story-header.group
        [:div.story-header-left
          [:span.board-name (:storyboard-name story-data)]
          [:span.arrow ">"]
          [:span.story-title (:title story-data)]]
        [:div.story-header-right
          (reactions story-data)
          [:div.comments-summary-container.group
            {:on-click #(reset! (::comments-expanded s) (not @(::comments-expanded s)))}
            (comments-summary story-data true)]
          [:button.mlb-reset.mlb-link.share-button
            {:on-click #()}
            "Share"]]]
      [:div.story-content
        {:class (when @(::comments-expanded s) "comments-expanded")
         :style #js {:marginLeft (str (int (/ (- (.-innerWidth js/window) 840 (when @(::comments-expanded s) 360)) 2)) "px")}}
        (when (:banner-url story-data)
          [:div.story-banner
            {:style #js {:backgroundImage (str "url(" (:banner-url story-data) ")")
                         :height (str (min 200 (* (/ (:banner-height story-data) (:banner-width story-data)) 840)) "px")}}])
        [:div.story-author.group
          (user-avatar-image story-author)
          [:div.name (:name story-author)]
          [:div.time-since
            [:time
              {:date-time (:created-at story-data)
               :data-toggle "tooltip"
               :data-placement "top"
               :title (utils/activity-date-tooltip story-data)}
              (utils/time-since (:created-at story-data))]]]
        (when (:storyboard-name story-data)
          [:div.story-tags
            [:div.activity-tag
              {:on-click #(router/nav! (oc-urls/board (router/current-org-slug) (:board-slug story-data)))}
              (:storyboard-name story-data)]])
        (when (:title story-data)
          [:div.story-title
            {:dangerouslySetInnerHTML (utils/emojify (:title story-data))}])
        (when (:body story-data)
          [:div.story-body
            {:dangerouslySetInnerHTML (utils/emojify (:body story-data))}])
        [:div.story-content-footer.group
          [:div.you-did-it "The End. You did it!"]
          [:div.caught-up]]]
      [:div.story-comments-container
        {:class (when @(::comments-expanded s) "comments-expanded")}
        [:button.close-comments.mlb-reset
          {:on-click #(reset! (::comments-expanded s) false)
           :on-mouse-enter #(reset! (::close-hovering s) true)
           :on-mouse-leave #(reset! (::close-hovering s) false)}
          [:img {:src (utils/cdn (str "/img/ML/board_remove_filter" (when @(::close-hovering s) "_white") ".png")) :width 12 :height 12}]]
        (comments story-data)]]))