(ns net.nightweb.main
  (:use [neko.application :only [defapplication]]
        [neko.notify :only [notification]]
        [neko.resource :only [get-resource]]
        [net.nightweb.clandroid.service :only [defservice start-foreground]]
        [nightweb.router :only [start-router
                                stop-router
                                start-download-manager]]))

(defapplication net.nightweb.Application)

(defservice
  net.nightweb.MainService
  :def service
  :on-create
  (fn [this]
    ; create notification
    (start-foreground
      this 1 (notification
               :icon (get-resource :drawable :ic_launcher)
               :content-title "Nightweb is running"
               :content-text "Touch to shut down"
               :action [:broadcast "ACTION_CLOSE_APP"]))
    (def service-receiver (proxy [android.content.BroadcastReceiver] []
                            (onReceive [context intent]
                              (.stopSelf service))))
    (.registerReceiver this
                       service-receiver
                       (android.content.IntentFilter. "ACTION_CLOSE_APP"))
    ; start router
    (start-router this)
    (def download-manager (start-download-manager)))
  :on-destroy
  (fn [this]
    (.unregisterReceiver this service-receiver)
    (stop-router))
  :on-action
  (fn [this action]
    (if (= action :test)
      (println action))))