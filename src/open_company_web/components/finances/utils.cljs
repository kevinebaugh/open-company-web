(ns open-company-web.components.finances.utils
  (:require [open-company-web.lib.utils :as utils]))

(def columns 7)

(defn chart-data-at-index [data keyw column-name prefix idx]
  (let [data (to-array data)
        rev-idx (- (- (min (count data) columns) 1) idx)
        obj (get data rev-idx)]
    [(utils/period-string (:period obj))
     (keyw obj)
     (str (utils/period-string (:period obj)) " " column-name ": " prefix (.toLocaleString (keyw obj)))]))

(defn- get-chart-data [data prefix keyw column-name & [style fill-color pattern]]
  "Vector of max *columns elements of [:Label value]"
  (let [chart-data (partial chart-data-at-index data keyw column-name prefix)
        placeholder-vect (into [] (range (min (count data) columns)))
        columns [["string" column-name]
                 ["number" (utils/camel-case-str (name keyw))]
                 #js {"type" "string" "role" "tooltip"}]
        columns (if style (conj columns style) columns)
        values (into [] (map chart-data placeholder-vect))
        values (if fill-color (map #(assoc % 3 fill-color) values) values)]
    { :prefix prefix
      :columns columns
      :values values
      :pattern (if pattern pattern "###,###.##")}))