(ns open-company-web.components.topic-list
  (:require [om.core :as om :include-macros true]
            [om-tools.core :as om-core :refer-macros [defcomponent]]
            [om-tools.dom :as dom :include-macros true]
            [open-company-web.components.topic :refer (topic)]))

(defcomponent topic-list [data owner]

  (render [_]
    (dom/div {:class "topic-list"}
      (let [company-data (:company-data data)
            active-category (keyword (:active-category data))
            active-sections (get-in company-data [:sections active-category])]
        (for [section-name active-sections
              :let [sd (->> section-name keyword (get company-data))]]
          (dom/div {:class "topic-row"
                    :key (str "topic-row-" (name section-name))}
            (when-not (and (:read-only company-data) (:placeholder sd))
              (om/build topic {:section-data sd
                               :section-name section-name
                               :currency (:currency company-data)}))))))))
