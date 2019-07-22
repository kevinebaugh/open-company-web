(ns oc.electron.main)

(def auto-updater (.-autoUpdater (js/require "electron-updater")))
(.checkForUpdatesAndNotify auto-updater)

(def rate-in-minutes-to-check-for-updates 5)
(js/setInterval
  (fn []
    (.checkForUpdatesAndNotify auto-updater))
  (* rate-in-minutes-to-check-for-updates 60 1000))

(def main-window (atom nil))
(def quitting? (atom false))

(def path (js/require "path"))
(def URL (.-URL (js/require "url")))
(def electron (js/require "electron"))

(def app (.-app electron))
(def ipc-main (.-ipcMain electron))
(def session (.-session electron))
(def shell (.-shell electron))
(def BrowserWindow (.-BrowserWindow electron))

(goog-define dev? true)
(goog-define web-origin "http://localhost:3559")
(goog-define auth-origin "http://localhost:3003")
(goog-define init-path "/login/desktop")
(def init-url (str web-origin init-path))

(defn mac?
  []
  (= (.-platform js/process) "darwin"))

(defn win32?
  []
  (= (.-platform js/process) "win32"))

(defn- load-page
  [window]
  (println "Loading " init-url)
  (.loadURL window init-url))

(defn- mk-window
  [w h show?]
  (let [mac-frame-settings (when (mac?) {:titleBarStyle "hiddenInset"})
        win-frame-settings (when (win32?) {:frame true})]
    (BrowserWindow. (clj->js
                     (merge
                      mac-frame-settings
                      win-frame-settings
                      {:width w
                       :height h
                       :show show?
                       ;; Icon of Ubuntu/Linux. Other platforms are configured in package.json
                       :icon (.join path (.getAppPath app) "carrot.iconset/icon_512x512.png")
                       :webPreferences #js {:enableRemoteModule false
                                            :preload (.join path (.getAppPath app) "electron" "renderer.js")}
                       })))))

(defn- set-csp
  []
  ;; Define Content Security Policy
  ;; https://electronjs.org/docs/tutorial/security#6-define-a-content-security-policy
  (.. session -defaultSession -webRequest
      (onHeadersReceived
       (fn [details callback]
         (let [details-cljs (js->clj details)
               merged-details (merge details-cljs
                                     {"Content-Security-Policy" ["default-src \"none\""]})]
           (callback (clj->js merged-details)))))))

(def slack-origin "https://slack.com")
(def slack-origin-re #"^https://.*\.slack\.com$")
(def google-accounts-origin "https://accounts.google.com")

(defn- allowed-origin?
  [o]
  (or
    (= o web-origin)
    (= o auth-origin)
    (= o slack-origin)
    (= o google-accounts-origin)
    (re-matches slack-origin-re o)))

(defn- prevent-navigation-external-to-carrot
  []
  (.on app "web-contents-created"
    (fn [event contents]
      (.on contents "will-navigate"
        (fn [event navigation-url]
          (let [parsed-url    (URL. navigation-url)
                target-origin (.-origin parsed-url)]
            (println "Attempting to navigate to origin: " target-origin)
            (when (not (allowed-origin? target-origin))
              (println "Navigation prevented")
              (.preventDefault event)))))
      (.on contents "new-window"
        (fn [event navigation-url]
          (.preventDefault event)
          (.openExternal shell navigation-url))))))

(defn- init-browser
  []
  (if (some? @main-window)
    (.show @main-window)
    (do (set-csp)
        (prevent-navigation-external-to-carrot)
        (reset! main-window (mk-window 1280 720 true))
        (when (win32?)
          (.setMenuBarVisibility @main-window false))
        (load-page @main-window)
        (when dev? (.openDevTools @main-window))
        ;; -- Main window event handlers --
        (.on @main-window "close" #(if (or @quitting? (win32?))
                                     (reset! main-window nil)
                                     (do (.preventDefault %)
                                         (.hide @main-window)))))))

(defn init
  []
  (set! *main-cli-fn* (fn [] nil))

  ;; Required for desktop notifications to work on Windows
  ;; https://electronjs.org/docs/tutorial/notifications#windows
  ;; When testing, add `node_modules\electron\dist\electron.exe` to your Start Menu
  (when (win32?)
    (.setAppUserModelId app "io.carrot.desktop"))

  ;; -- App event handlers --
  (.on app "window-all-closed" #(when (not (mac?))
                                  (.quit app)))
  (.on app "activate" #(when-let [w @main-window]
                         (.show w)))
  (.on app "before-quit" #(reset! quitting? true))
  (.on app "ready" init-browser)

  ;; -- Inter-process Communication event handlers --
  ;; see resources/electron/renderer.js
  (.on ipc-main "set-badge-count" (fn [event arg] (.setBadgeCount app arg)))
  (.on ipc-main "show-desktop-window" (fn [event arg] (when @main-window
                                                        (.show @main-window))))
  )