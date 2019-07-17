(ns oc.web.components.ui.giphy-picker
  (:require [rum.core :as rum]
            [dommy.core :refer-macros (sel1)]
            [oc.web.local-settings :as ls]
            [oc.web.utils.dom :as dom-utils]
            [oc.web.lib.react-utils :as react-utils]))

(def giphy-picker-max-height 408)

(rum/defcs giphy-picker < (rum/local 0 ::offset-top)
                          {:will-mount (fn [s]
                            (when-let [picker-el (sel1 [:div.medium-editor-media-picker])]
                              (js/console.log "DBG media-picker offsetTop" (.-offsetTop picker-el))
                              (reset! (::offset-top s) (.-offsetTop picker-el)))
                           s)}
  [s {:keys [fullscreen pick-emoji-cb]}]
  (let [scrolling-element (if fullscreen (sel1 [:div.cmail-content-outer]) (.-scrollingElement js/document))
        win-height (or (.-clientHeight (.-documentElement js/document))
                       (.-innerHeight js/window))
        top-offset-limit (.-offsetTop (sel1 [:div.rich-body-editor-outer-container]))
        ; offset-height (.-scrollHeight scrolling-element)
        scroll-top (.-scrollTop scrolling-element)
        top-position (max 0 @(::offset-top s))
        relative-position (+ top-position
                             top-offset-limit
                             (* scroll-top -1)
                             giphy-picker-max-height)
        adjusted-position (if (> relative-position win-height) ;; 286 is the top offset of the body
                            (max 0 (- top-position (- relative-position win-height) 16))
                            top-position)]
    (js/console.log "DBG giphy-picker/render win-height" win-height)
    (js/console.log "DBG    top-offset-limit" top-offset-limit)
    (js/console.log "DBG    scroll-top" scroll-top)
    (js/console.log "DBG    top-position" top-position)
    (js/console.log "DBG    relative" relative-position)
    (js/console.log "DBG    adjusted-position" adjusted-position)

    [:div.giphy-picker
      {:style {:top (str adjusted-position "px")}}
      (react-utils/build (.-Selector js/ReactGiphySelector)
       {:apiKey ls/giphy-api-key
        :queryInputPlaceholder "Search for GIF"
        :resultColumns 1
        :preloadTrending true
        :containerClassName "giphy-picker-container"
        :queryFormAutoFocus true
        :queryFormClassName "giphy-picker-form"
        :queryFormInputClassName "giphy-picker-form-input"
        :queryFormSubmitClassName "mlb-reset giphy-picker-form-submit"
        :queryFormSubmitContent "Seach"
        :searchResultsClassName (str "giphy-picker-results-container" (when fullscreen " fullscreen"))
        :searchResultClassName "giphy-picker-results-item"
        :suggestionsClassName "giphy-picker-suggestions"
        :suggestionClassName "giphy-picker-suggestions-suggestion"
        :loaderClassName "giphy-picker-loader"
        :onGifSelected pick-emoji-cb})]))