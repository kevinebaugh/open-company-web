(ns oc.web.components.ui.activity-removed
  (:require [rum.core :as rum]
            [org.martinklepsch.derivatives :as drv]
            [oc.web.urls :as oc-urls]
            [oc.web.router :as router]
            [oc.web.dispatcher :as dis]
            [oc.web.lib.utils :as utils]))

(rum/defcs activity-removed < rum/static
  [s]
  [:div.activity-removed-container
    [:div.activity-removed-wrapper
      [:div.activity-removed-left
        [:div.activity-removed-logo]
        [:div.activity-removed-box]]
      [:div.activity-removed-right
        [:div.activity-removed-right-content
          [:div.info-icon]
          [:div.content-title
            "Post removed"]
          [:div.content-description
            "Looks like the post you’re trying to access was removed."]
          [:button.mlb-reset.go-to-homepage-btn
            {:aria-label "Homepage"
             :on-click #(do
                          (dis/dispatch! [:input [:show-activity-removed] false])
                          (router/nav! (utils/your-digest-url)))}
            "Go to homepage"]]]]])