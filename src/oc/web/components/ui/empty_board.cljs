(ns oc.web.components.ui.empty-board
  (:require [rum.core :as rum]
            [org.martinklepsch.derivatives :as drv]
            [oc.web.dispatcher :as dis]
            [oc.web.lib.utils :as utils]
            [oc.web.lib.responsive :as responsive]))

(def mobile-image-size
 {:width 360
  :height 322
  :ratio (/ 360 322)})

(rum/defcs empty-board < rum/reactive
                         (drv/drv :board-data)
  [s]
  (let [board-data (drv/react s :board-data)
        mobile? (responsive/is-mobile-size?)
        ww (when mobile? (responsive/ww))]
    [:div.empty-board.group
      (when-not mobile?
        [:div.empty-board-headline
          (str "There aren’t any posts in " (:name board-data) " yet. ")
          (when-not (:read-only board-data)
            [:button.mlb-reset
              {:on-click #(dis/dispatch! [:entry-edit {:board-slug (:slug board-data) :board-name (:name board-data)}])}
              "Add one?"])])
      [:img.empty-board-image
        {:src (utils/cdn (str "/img/ML/" (when mobile? "mobile_") "empty_board.svg"))
         :style {:width (str (if mobile? (- ww 24 24) 416) "px")
                 :height (str (if mobile? (* (- ww 24 24) (:ratio mobile-image-size)) 424) "px")
                 :max-width (str (if mobile? (:width mobile-image-size) 416) "px")
                 :max-height (str (if mobile? (:height mobile-image-size) 424) "px")}}]
      (when mobile?
        [:div.empty-board-footer
          "Shoot, looks like there aren’t any posts in Design yet...."])
      (when mobile?
        [:button.mlb-reset.empty-board-create-first-post
          "Create the first post"])]))