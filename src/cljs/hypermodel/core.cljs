(ns hypermodel.core
    (:require [reagent.core :as reagent]
              [re-frame.core :as re-frame]
              [hypermodel.handlers]
              [hypermodel.subs]
              [hypermodel.views :as views]
              [hypermodel.config :as config]))

(when config/debug?
  (println "dev mode"))

(defn mount-root []
  (reagent/render [views/main-panel]
                  (.getElementById js/document "app")))

(defn ^:export init [] 
  (re-frame/dispatch-sync [:initialize-db])
  (re-frame/dispatch [:bootstrap-app])
  (mount-root))
