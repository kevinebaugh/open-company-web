(ns oc.web.stores.section
  (:require [taoensso.timbre :as timbre]
            [cljs-flux.dispatcher :as flux]
            [oc.web.lib.jwt :as jwt]
            [oc.web.dispatcher :as dispatcher]
            [oc.lib.time :as oc-time]
            [oc.web.lib.utils :as utils]
            [oc.web.utils.activity :as au]))

;; Reducers used to watch for org/section dispatch data
(defmulti reducer (fn [db [action-type & _]]
                    (when-not (some #{action-type} [:update :input])
                      (timbre/debug "Dispatching section reducer:" action-type))
                    action-type))

(def sections-dispatch
  (flux/register
   dispatcher/actions
   (fn [payload]
     (swap! dispatcher/app-state reducer payload))))

(defmethod dispatcher/action :section
  [db [_ sort-type section-data]]
  (let [db-loading (if (:is-loaded section-data)
                     (dissoc db :loading)
                     db)
        with-entries (:entries section-data)
        org-slug (utils/section-org-slug section-data)
        fixed-section-data (au/fix-board section-data (dispatcher/change-data db))
        old-section-data (get-in db (dispatcher/board-data-key org-slug (:slug section-data) sort-type))
        with-current-edit (if (and (:is-loaded section-data)
                                   (:entry-editing db))
                            old-section-data
                            fixed-section-data)
        posts-key (dispatcher/posts-data-key org-slug)
        merged-items (merge (get-in db posts-key)
                            (:fixed-items fixed-section-data))
        with-merged-items (if with-entries
                            (assoc-in db-loading posts-key merged-items)
                            db-loading)]
    (assoc-in with-merged-items
              (dispatcher/board-data-key org-slug (:slug section-data) sort-type)
              (dissoc with-current-edit :fixed-items))))

(defn fix-org-section-data
  [db org-data changes]
  (assoc-in db
            (dispatcher/org-data-key (:slug org-data))
            org-data))

(defn fix-posts-new-label
  [db changes]
  (let [posts-data (dispatcher/posts-data db)
        org-slug (:org (:router-path db))]
    (reduce
      #(let [posts-key (dispatcher/activity-key org-slug (:uuid %2))]
          (update-in %1 posts-key merge {:unseen (au/post-unseen? %2 changes)
                                         :unread (au/post-unread? %2 changes)}))
      db
      (vals posts-data))))

(defmethod dispatcher/action :section-change
  [db [_ section-uuid]]
  db)

(defmethod dispatcher/action :section-edit-save/finish
  [db [_ sort-type section-data]]
  (let [org-slug (utils/section-org-slug section-data)
        section-slug (:slug section-data)
        board-key (dispatcher/board-data-key org-slug section-slug :recently-posted)
        recent-board-key (dispatcher/board-data-key org-slug section-slug :recent-activity)
        ;; Parse the new section data
        fixed-section-data (au/fix-board section-data (dispatcher/change-data db))
        old-board-data (get-in db board-key)
        ;; Replace the old section data for :recently-posted sort
        ;; w/o overriding the posts and links to avoid breaking pagination
        next-board-data (merge fixed-section-data
                         (select-keys old-board-data [:posts-list :fixed-items :links]))
        old-recent-board-data (get-in db recent-board-key)
        ;; Same for the :recent-activity sort
        next-recent-board-data (merge fixed-section-data
                                (select-keys old-recent-board-data [:posts-list :fixed-items :links]))]
    (-> db
        (assoc-in board-key next-board-data)
        (assoc-in recent-board-key next-recent-board-data)
        (dissoc :section-editing))))

(defmethod dispatcher/action :section-edit/dismiss
  [db [_]]
  (dissoc db :section-editing))

(defmethod dispatcher/action :private-section-user-add
  [db [_ user user-type]]
  (let [section-data (:section-editing db)
        current-notifications (filterv #(not= (:user-id %) (:user-id user))
                                       (:private-notifications section-data))
        current-authors (filterv #(not= % (:user-id user)) (:authors section-data))
        current-viewers (filterv #(not= % (:user-id user)) (:viewers section-data))
        next-authors (if (#{:admin :author} user-type)
                       (vec (conj current-authors (:user-id user)))
                       current-authors)
        next-viewers (if (= user-type :viewer)
                       (vec (conj current-viewers (:user-id user)))
                       current-viewers)
        next-notifications (vec (conj current-notifications user))]
    (assoc db :section-editing
           (merge section-data {:has-changes true
                                :authors next-authors
                                :viewers next-viewers
                                :private-notifications next-notifications}))))

(defmethod dispatcher/action :private-section-user-remove
  [db [_ user]]
  (let [section-data (:section-editing db)
        private-notifications (filterv #(not= (:user-id %) (:user-id user))
                                       (:private-notifications section-data))
        next-authors (filterv #(not= % (:user-id user)) (:authors section-data))
        next-viewers (filterv #(not= % (:user-id user)) (:viewers section-data))]
    (assoc db :section-editing
           (merge section-data {:has-changes true
                                :authors next-authors
                                :viewers next-viewers
                                :private-notifications private-notifications}))))

(defmethod dispatcher/action :private-section-kick-out-self/finish
  [db [_ success]]
  (if success
    ;; Force board editing dismiss
    (dissoc db :section-editing)
    ;; An error occurred while kicking the user out, no-op to let the user retry
    db))

(defmethod dispatcher/action :section-delete
  [db [_ org-slug section-slug]]
  (let [section-key (dispatcher/board-key org-slug section-slug)
        org-sections-key (vec (conj (dispatcher/org-data-key org-slug) :boards))
        remaining-sections (remove #(= (:slug %) section-slug) (get-in db org-sections-key))
        posts-key (dispatcher/posts-data-key org-slug)
        old-posts (get-in db posts-key)
        removed-posts (filterv (fn [p] (not= (:board-slug p) section-slug))
                               (vals old-posts))]
    (-> db
      (update-in (butlast section-key) dissoc (last section-key))
      (assoc posts-key (zipmap (map :uuid removed-posts) removed-posts))
      (assoc org-sections-key remaining-sections)
      (dissoc :section-editing))))

(defmethod dispatcher/action :container/status
  [db [_ change-data replace-change-data?]]
  (timbre/debug "Change status received:" change-data)
  (if change-data
    (let [org-data (dispatcher/org-data db)
          current-board-slug (:board (:router-path db))
          current-board-uuid (:uuid (dispatcher/board-data db))
          filtered-change-data (if (= current-board-slug "all-posts")
                                 {} ;; ignore all changes if we are on AP
                                 (into {} (filter (fn [[buid _]](not= buid current-board-uuid)) change-data)))
          old-change-data (dispatcher/change-data db)
          new-change-data (merge old-change-data change-data)
          next-db (fix-org-section-data db org-data new-change-data)]
      (timbre/debug "Change status data:" new-change-data)
      (-> next-db
        (fix-posts-new-label new-change-data)
        (assoc-in (dispatcher/change-data-key (:slug org-data)) new-change-data)))
    db))

(defn update-unseen-unread-remove [old-change-data item-id container-id new-changes]
  (let [old-container-change-data (get old-change-data container-id)
        old-unseen (or (:unseen old-container-change-data) [])
        next-unseen (filter #(not= % item-id) old-unseen)
        old-unread (or (:unread old-container-change-data) [])
        next-unread (filter #(not= % item-id) old-unread)
        next-container-change-data (if old-container-change-data
                                     (assoc old-container-change-data
                                      :unseen next-unseen
                                      :unread next-unread)
                                     {:container-id container-id
                                      :unseen next-unseen
                                      :unread next-unread})]
    (assoc old-change-data container-id next-container-change-data)))

(defmethod dispatcher/action :item-delete/unseen
  [db [_ org-slug change-data]]
  (let [item-id (:item-id change-data)
        container-id (:container-id change-data)
        change-key (dispatcher/change-data-key org-slug)
        old-change-data (get-in db change-key)]
    (assoc-in db change-key (update-unseen-unread-remove old-change-data item-id container-id change-data))))

(defn update-unseen-unread-add [old-change-data item-id container-id new-changes]
  (let [old-container-change-data (get old-change-data container-id)
        old-unseen (or (:unseen old-container-change-data) [])
        next-unseen (vec (seq (conj old-unseen item-id)))
        old-unread (or (:unread old-container-change-data) [])
        next-unread (vec (seq (conj old-unread item-id)))
        next-container-change-data (if old-container-change-data
                                     (assoc old-container-change-data
                                      :unseen next-unseen
                                      :unread next-unread)
                                     {:container-id container-id
                                      :unseen next-unseen
                                      :unread next-unread})]
    (assoc old-change-data container-id next-container-change-data)))

(defmethod dispatcher/action :item-add/unseen
  [db [_ org-slug change-data]]
  (let [item-id (:item-id change-data)
        container-id (:container-id change-data)
        change-key (dispatcher/change-data-key org-slug)
        old-change-data (get-in db change-key)]
    (assoc-in db change-key (update-unseen-unread-add old-change-data item-id container-id change-data))))

;; Section store specific reducers
(defmethod reducer :default [db payload]
  ;; ignore state changes not specific to reactions
  db)

(defmethod reducer :org-loaded
  [db [_ org-data saved?]]
  (fix-org-section-data db org-data (dispatcher/change-data db)))

(defmethod dispatcher/action :section-more
  [db [_ org-slug board-slug sort-type]]
  (let [container-key (dispatcher/board-data-key org-slug board-slug sort-type)
        container-data (get-in db container-key)
        next-container-data (assoc container-data :loading-more true)]
    (assoc-in db container-key next-container-data)))

(defmethod dispatcher/action :section-more/finish
  [db [_ org board direction sort-type next-board-data]]
  (if next-board-data
    (let [container-key (dispatcher/board-data-key org board sort-type)
          container-data (get-in db container-key)
          posts-data-key (dispatcher/posts-data-key org)
          old-posts (get-in db posts-data-key)
          prepare-board-data (merge next-board-data {:posts-list (:posts-list container-data)
                                                     :old-links (:links container-data)})
          fixed-posts-data (au/fix-board prepare-board-data (dispatcher/change-data db) direction)
          new-items-map (merge old-posts (:fixed-items fixed-posts-data))
          new-container-data (-> fixed-posts-data
                              (assoc :direction direction)
                              (dissoc :loading-more))]
      (-> db
        (assoc-in container-key new-container-data)
        (assoc-in posts-data-key new-items-map)))
    db))

(defmethod dispatcher/action :setup-section-editing
  [db [_ board-data]]
  (assoc db :initial-section-editing board-data))