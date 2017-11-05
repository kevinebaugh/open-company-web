(ns oc.web.components.ui.filters-dropdown
  (:require [rum.core :as rum]
            [cuerdas.core :as s]
            [medley.core :as med]
            [org.martinklepsch.derivatives :as drv]
            [oc.web.urls :as oc-urls]
            [oc.web.router :as router]
            [oc.web.dispatcher :as dis]
            [oc.web.lib.utils :as utils]
            [oc.web.lib.cookies :as cook]
            [oc.web.local-settings :as ls]
            [oc.web.components.ui.dropdown-list :refer (dropdown-list)]
            [oc.web.components.ui.carrot-close-bt :refer (carrot-close-bt)]))

(defn compare-topic-names [topics topic-slug-1 topic-slug-2]
  (let [topic-name-1 (some #(when (= (:slug %) topic-slug-1) (:name %)) topics)
        topic-name-2 (some #(when (= (:slug %) topic-slug-2) (:name %)) topics)]
    (compare topic-name-1 topic-name-2)))

(rum/defcs filters-dropdown < rum/reactive
                              (drv/drv :board-filters)
                              (drv/drv :board-data)
                              (rum/local false ::show-filters-dropdown)
  [s]
  (let [board-data (drv/react s :board-data)
        board-filters (drv/react s :board-filters)
        topic-groups (group-by :topic-slug (vals (:fixed-items board-data)))
        org-slug (router/current-org-slug)
        board-slug (router/current-board-slug)]
    [:div.filters-dropdown-name.group
      (when (string? board-filters)
        (carrot-close-bt {:width 24
                          :height 24
                          :on-click #(if (= (cook/get-cookie (router/last-board-filter-cookie org-slug board-slug)) "by-topic")
                                       (router/nav! (oc-urls/board-sort-by-topic))
                                       (router/nav! (oc-urls/board)))}))
      [:button.mlb-reset.filters-dropdown-button.choice
        {:type "button"
         :on-click #(reset! (::show-filters-dropdown s) (not @(::show-filters-dropdown s)))}
        (cond
          (= board-filters :by-topic)
          [:span "View by " [:span.filter-highlight "topic"] " "]
          (string? board-filters)
          [:span.filter-highlight
            (if (= board-filters "uncategorized")
              "No topic "
              (str (:name (utils/get-topic (:topics board-data) board-filters)) " "))]
          :else
          [:span "View by " [:span.filter-highlight "most recent"] " "])
        [:i.fa.fa-caret-down]]
      (let [sorted-topics (sort #(compare-topic-names (:topics board-data) %1 %2) (remove empty? (keys topic-groups)))
            selected-topics (filter #(utils/in? sorted-topics (:slug %)) (med/distinct-by :slug (:topics board-data)))
            topics (vec (map #(clojure.set/rename-keys % {:name :label :slug :value}) selected-topics))
            default-options [{:label "Most recent" :value :latest} {:label "By topic" :value :by-topic}]
            divider-line-option {:label :divider-line :value nil}
            start-options (if (pos? (count topics)) (vec (conj default-options divider-line-option)) default-options)
            final-topics (vec (concat start-options topics))]
        (when @(::show-filters-dropdown s)
          (dropdown-list
            {:items final-topics
             :value board-filters
             :on-change (fn [t]
                          (reset! (::show-filters-dropdown s) false)
                          (cond
                            (= (:value t) :latest)
                            (do
                              (cook/set-cookie! (router/last-board-filter-cookie org-slug board-slug) (name :latest) (* 60 60 24 30) "/" ls/jwt-cookie-domain ls/jwt-cookie-secure)
                              (router/nav! (oc-urls/board)))
                            (= (:value t) :by-topic)
                            (do
                              (cook/set-cookie! (router/last-board-filter-cookie org-slug board-slug) (name :by-topic) (* 60 60 24 30) "/" ls/jwt-cookie-domain ls/jwt-cookie-secure)
                              (router/nav! (oc-urls/board-sort-by-topic)))
                            :else
                            (router/nav! (oc-urls/board-filter-by-topic (or (:value t) "uncategorized")))))
             :on-blur (fn []
                        (reset! (::show-filters-dropdown s) false))})))]))