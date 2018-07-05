(ns oc.web.components.ui.shared-misc
  (:require [oc.web.urls :as oc-urls]
            [oc.web.router :as router]
            [oc.web.lib.utils :as utils]))

(def carrot-testimonials
  ; [:section.testimonials-section
  ;   [:div.testimonials-section-title
  ;     "Don’t take our word for it"]
  ;   [:div.testimonials-section-subtitle
  ;     "Here’s how we’re helping teams like yours."]
  ;   [:div.testimonials-cards-container.group
  ;     [:img.card
  ;       {:src (utils/cdn "/img/ML/testimonial_katie.png")
  ;        :src-set (str (utils/cdn "/img/ML/testimonial_katie@2x.png") " 2x")}]
  ;     [:img.card
  ;       {:src (utils/cdn "/img/ML/testimonial_riley.png")
  ;        :src-set (str (utils/cdn "/img/ML/testimonial_riley@2x.png") " 2x")}]
  ;     [:img.card
  ;       {:src (utils/cdn "/img/ML/testimonial_matt.png")
  ;        :src-set (str (utils/cdn "/img/ML/testimonial_matt@2x.png") " 2x")}]]]
  )

(def no-credit-card
  [:div.no-credit-card
    "No credit card required&nbsp;•&nbsp;Works with Slack"])

(def keep-aligned
  [:section.keep-aligned
    [:div.keep-aligned-title
      "It’s never been easier to keep everyone on the same page"]
    [:button.mlb-reset.get-started-button
      "Get started for free"]
    no-credit-card])

(def core-values-list
  [:div.core-values-list.group
    [:div.core-value.key-announcement
      "Key announcements"]
    [:div.core-value.company-updates
      "Company & team updates"]
    [:div.core-value.strategic-plans
      "Strategic plans"]
    [:div.core-value.ideas-discussions
      "Decisions"]])

(def testimonials-section
  [:section.testimonials-section
    [:div.testimonials-section-title
      "Don’t take our word for it"]
    [:div.testimonials-section-subtitle
      "Here’s how we’re helping teams like yours."]
    [:div.testimonials-cards-container.group
      [:img.card
        {:src (utils/cdn "/img/ML/testimonial_katie.png")
         :src-set (str (utils/cdn "/img/ML/testimonial_katie@2x.png") " 2x")}]
      [:img.card
        {:src (utils/cdn "/img/ML/testimonial_riley.png")
         :src-set (str (utils/cdn "/img/ML/testimonial_riley@2x.png") " 2x")}]
      [:img.card
        {:src (utils/cdn "/img/ML/testimonial_matt.png")
         :src-set (str (utils/cdn "/img/ML/testimonial_matt@2x.png") " 2x")}]]])

(def keep-aligned-bottom
  [:section.keep-aligned
    [:div.keep-aligned-title
      "It’s never been easier to keep everyone on the same page"]
    [:button.mlb-reset.get-started-button
      "Get started for free"]
    no-credit-card])

(def keep-aligned-section
  [:section.home-keep-aligned
    [:h2.keep-aligned-title
      "Carrot keeps leaders and teams aligned"]

    [:div.keep-aligned-section
      [:div.keep-aligned-section-row
        [:div.keep-aligned-section-screenshot.screenshot-2]
        [:div.keep-aligned-section-copy
          [:div.keep-aligned-section-copy-title
            "Create engaging updates"]
          [:div.keep-aligned-section-list-item
            "Room for more than a quick chat"]
          [:div.keep-aligned-section-list-item
            "Capture video to add a human touch"]
          [:div.keep-aligned-section-list-item
            "Attachments from Google, Dropbox, & others"]]]

      [:div.keep-aligned-section-row
        [:div.keep-aligned-section-screenshot.screenshot-1]
        [:div.keep-aligned-section-copy
          [:div.keep-aligned-section-copy-title
            "Get up to speed quickly"]
          [:div.keep-aligned-section-list-item
            "“Must see” updates rise to the top"]
          [:div.keep-aligned-section-list-item
            "Follow people and topics you can’t miss"]
          [:div.keep-aligned-section-list-item
            "Organized and easy to browse"]]]

      [:div.keep-aligned-section-row
        [:div.keep-aligned-section-screenshot.screenshot-3]
        [:div.keep-aligned-section-copy
          [:div.keep-aligned-section-copy-title
            "Spark better discussions"]
          [:div.keep-aligned-section-list-item
            "Encourage more comments and questions"]
          [:div.keep-aligned-section-list-item
            "Keep interactions together for greater context"]
          [:div.keep-aligned-section-list-item
            "Sync to Slack to discuss anywhere"]]]

      [:div.keep-aligned-section-row
        [:div.keep-aligned-section-screenshot.screenshot-4]
        [:div.keep-aligned-section-copy
          [:div.keep-aligned-section-copy-title
            "Know who’s up to date"]
          [:div.keep-aligned-section-list-item
            "Eliminate communication gaps"]
          [:div.keep-aligned-section-list-item
            "Send reminders with a single click"]
          [:div.keep-aligned-section-list-item
            "Ensures alignment on important items"]]]]])

(def access-anywhere-section
  [:section.access-anywhere-section
    [:div.access-anywhere-section-container
      [:div.access-anywhere-copy
        [:div.access-anywhere-copy-title
          "Stay informed on the go."]
        [:div.access-anywhere-copy-subtitle
          "Fully responsive mobile web app."]
        [:div.access-anywhere-copy-subtitle.second-line
          "No app install required."]]
      [:div.access-anywhere-screenshot]]])

(defn slack-comparison-section [& [slack-version?]]
  [:section.slack-comparison
    [:div.slack-comparison-headline
      "PERFECT FOR SLACK TEAMS"]
    (when-not slack-version?
      [:div.slack-comparison-headline-1
        "Slack keeps your team connected in the moment."])
    (if slack-version? 
      [:div.slack-comparison-headline-2
        "Keep your team informed without distractions."]
      [:div.slack-comparison-headline-2
        "Carrot keeps it aligned over time."])
    [:img.slack-comparison-screenshot.big-web-only
      {:src (utils/cdn "/img/ML/slack_comparison_screenshot.png")
       :src-set (str (utils/cdn "/img/ML/slack_comparison_screenshot@2x.png") " 2x")}]
    [:img.slack-comparison-screenshot.mobile-only
      {:src (utils/cdn "/img/ML/slack_comparison_screenshot_mobile.png")
       :src-set (str (utils/cdn "/img/ML/slack_comparison_screenshot_mobile@2x.png") " 2x")}]])