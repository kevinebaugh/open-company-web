(ns open-company-web.components.finances-pieces.revenue
  (:require [om.core :as om :include-macros true]
            [om-tools.core :as om-core :refer-macros [defcomponent]]
            [om-tools.dom :as dom :include-macros true]
            [open-company-web.router :as router]
            [open-company-web.lib.utils :as utils]
            [open-company-web.lib.iso4217 :refer [iso4217]]
            [open-company-web.components.charts :refer [column-chart]]
            [open-company-web.components.finances-pieces.utils :refer [get-chart-data]]
            [open-company-web.components.utility-components :refer [editable-pen]]))

(defcomponent revenue [data owner]
  (render [_]
    (let [finances-data (:data (:finances data))
          value-set (first finances-data)
          revenue-val (utils/format-value (:revenue value-set))
          period (utils/period-string (:period value-set))
          cur-symbol (utils/get-symbol-for-currency-code (:currency data))]
      (dom/div {:class "section revenue"}
        (dom/h3 {}
                (str cur-symbol revenue-val)
                (om/build editable-pen {}))
        (dom/p {} period)
        (om/build column-chart (get-chart-data finances-data cur-symbol :revenue "Revenue"))))))