(ns oc.web.components.home-page
  (:require-macros [dommy.core :refer (sel1)])
  (:require [rum.core :as rum]
            [oc.web.lib.jwt :as jwt]
            [oc.web.urls :as oc-urls]
            [oc.web.router :as router]
            [oc.web.dispatcher :as dis]
            [oc.web.actions.user :as user]
            [oc.web.lib.utils :as utils]
            [oc.web.components.ui.shared-misc :as shared-misc]
            [oc.web.components.ui.site-header :refer (site-header)]
            [oc.web.components.ui.site-footer :refer (site-footer)]
            [oc.web.components.ui.try-it-form :refer (try-it-form)]
            [oc.web.components.ui.site-mobile-menu :refer (site-mobile-menu)]
            [oc.web.components.ui.login-overlay :refer (login-overlays-handler)]))

(rum/defcs home-page < (rum/local false ::thanks-box-top)
                       (rum/local false ::thanks-box-bottom)
                       (rum/local false ::confirm)
                       {:did-mount (fn [s]
                                    (when (:tif (:query-params @router/path))
                                      (utils/after 1500 #(.focus (sel1 [:input.try-it-form-central-input]))))
                                    (when (exists? (.-OCWebSetupMarketingSiteJS js/window))
                                      (js/OCWebSetupMarketingSiteJS))
                                    s)
                       :will-mount (fn [s]
                                     (when (:confirm (:query-params @router/path))
                                       (reset! (::confirm s) true))
                                     s)}
  [s]
  [:div
    (site-header)
    (site-mobile-menu)
    [:div.home-wrap
      {:id "wrap"}
      (login-overlays-handler)
      [:div.main.home-page
        (shared-misc/video-lightbox)
        ; Hope page header
        [:section.cta.group

          [:h1.headline
            "Lead with clarity"]
          [:div.subheadline
            (str
             "Carrot is a platform for must-see communication. Share the "
             "important team updates and news that keeps your team focused "
             "on what matters. With Carrot, nothing gets lost in the noise.")]

          [:div.get-started-button-container.group
            [:button.mlb-reset.get-started-button
              {:id "get-started-centred-bt"}
              "Create your team - It's free"]]

          [:div.main-animation-container
            [:img.main-animation
              {:src (utils/cdn "/img/ML/homepage_screenshot.png")
               :srcSet (str (utils/cdn "/img/ML/homepage_screenshot@2x.png") " 2x")}]]

          shared-misc/testimonials-logos-line]

        (shared-misc/keep-aligned-section false)

        shared-misc/carrot-in-actions

        shared-misc/testimonials-section

        shared-misc/keep-aligned-bottom]]

    (site-footer)])
