(ns oc.web.components.ui.activity-attachments
  (:require [rum.core :as rum]
            [oc.web.lib.utils :as utils]
            [clojure.contrib.humanize :refer (filesize)]
            [goog.events :as events]
            [goog.events.EventType :as EventType]))

(rum/defcs activity-attachments < (rum/local false ::attachments-dropdown)
  [s activity-data]
  (let [attachments (utils/get-attachments-from-body (:body activity-data))]
    (when (pos? (count attachments))
      [:div.activity-attachments
        {:on-mouse-enter #(reset! (::attachments-dropdown s) true)
         :on-mouse-leave #(reset! (::attachments-dropdown s) false)}
        [:button.mlb-reset.attachments-button
          {:ref "attachments-button"
           :class (when @(::attachments-dropdown s) "expanded")}
          (count attachments)]
        (when @(::attachments-dropdown s)
          [:div.attachments-dropdown
            [:div.triangle]
            [:div.attachments-dropdown-list
              [:div.attachments-dropdown-header.group
                [:div.title "Attachments"]
                [:div.subtitle (str (count attachments) " files")]]
              (for [atch attachments
                    :let [author (:author atch)
                          createdat (:createdat atch)
                          size (:size atch)
                          subtitle (str "Uploaded "
                                      (when author
                                        (str "by " author " "))
                                      (when createdat
                                        (str (utils/time-since createdat) " "))
                                      (when size
                                        (str "- " (filesize size :binary false :format "%.2f"))))]]
                [:a.attachments-dropdown-item.group
                  {:key (str "attachment-" size "-" (:url atch))
                   :href (:url atch)
                   :target "_blank"}
                  [:div.file-icon
                    [:i.fa
                      {:class (utils/icon-for-mimetype (:mimetype atch))}]]
                  [:div.file-title
                    (:name atch)]
                  [:div.file-subtitle
                    subtitle]
                  [:div.file-download]])]])])))