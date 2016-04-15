(ns hypermodel.handlers
  (:require-macros [cljs.core.async.macros :refer [go]])
    (:require [re-frame.core :as re-frame]
              [hypermodel.db :as db]
              [cljs-http.client :as http]
              [clojure.set :as set]
              [cljs.core.async :refer [chan <!]]))

(defn unique-random-numbers [n]
  (let [a-set (set (take n (repeatedly #(rand-int n))))]
    (concat a-set (set/difference (set (take n (range)))
                                  a-set))))

(defn model [service]
  "Given a service URL, a type to search for, and an attribute field, return the display name."
  (go (let [response (<! (http/get (str "http://" service "/service/model/json") {:with-credentials? false :keywordize-keys true}))]
        (-> response :body :model :classes))))

(re-frame/register-handler
 :initialize-db
 (fn  [_ _]
   db/default-db))

(defn get-random-from [col n]
  (map (fn [x] (-> (nth col x) second :referencedType)) (unique-random-numbers 3)))

(re-frame/register-handler
  :fill-tiers
  (fn [db _]
    (let [cols (-> db :model :Gene :collections seq)]
      (fn [re])
      (println "random" (get-random-from cols 4)))
    db))

(re-frame/register-handler
  :handle-bootstrap-model
  (fn [db [_ response]]
    (re-frame/dispatch [:fill-tiers])
    (assoc-in db [:model] response)))

(re-frame/register-handler
  :bootstrap-app
  (fn [db _]
    (println "running")
    (go (re-frame/dispatch [:handle-bootstrap-model (<! (model "www.flymine.org/query")) ]))
    db))