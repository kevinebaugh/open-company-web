(ns oc.web.stores.activity
  (:require [taoensso.timbre :as timbre]
            [oc.web.dispatcher :as dispatcher]
            [oc.web.lib.jwt :as j]
            [oc.web.lib.utils :as utils]))

(defmethod dispatcher/action :activity-modal-fade-in
  [db [_ activity-data editing]]
  (if (get-in db [:search-active])
    db
    (-> db
      (assoc :activity-modal-fade-in (:uuid activity-data))
      (assoc :dismiss-modal-on-editing-stop editing)
      ;; Make sure the seen-at is not reset when navigating to modal view
      (assoc :no-reset-seen-at true))))

(defmethod dispatcher/action :activity-modal-fade-out
  [db [_ board-slug]]
  (if (get-in db [:search-active])
    db
    (-> db
      (dissoc :activity-modal-fade-in)
      (dissoc :modal-editing)
      (dissoc :dismiss-modal-on-editing-stop))))

(defmethod dispatcher/action :entry-edit/dismiss
  [db [_]]
  (-> db
    (dissoc :entry-editing)
    (assoc :entry-edit-dissmissing true)))

(defmethod dispatcher/action :modal-editing-deactivate
  [db [_]]
  (dissoc db :modal-editing))

(defmethod dispatcher/action :modal-editing-activate
  [db [_]]
  (-> db
    (assoc :modal-editing true)
    (assoc :entry-save-on-exit true)))

(defmethod dispatcher/action :entry-toggle-save-on-exit
  [db [_ enabled?]]
  (assoc db :entry-save-on-exit enabled?))