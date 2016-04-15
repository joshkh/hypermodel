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



;(defn get-random-from [model class quantity]
;  (let [candidates (-> model class :collections seq)]
;    ;(println "candidates" candidates)
;    (map (fn [x]
;           (println "RAND GOT" x)
;           (-> (nth candidates x) second :referencedType)) (unique-random-numbers quantity))))

;(assoc total
;  (keyword (str next)) (reduce conj [] (map #(assoc {} :node % :parent nil) (get-random-from (-> db :model) :Gene 1)) ))

(defn get-random-from [model class quantity]
  (let [pool (merge (:collections (class model)) (:references (class model)))
        candidates (seq pool)]
    ;(println "POOL" pool)
    (map (fn [x]
           (-> (nth candidates x) second :referencedType keyword))
         [1 2])))


;(map (fn [i]
;       (println "sees i" i)
;       (println "returning" (get-random-from (-> db :model) (keyword i) 1))
;       (get-random-from (-> db :model) (keyword i) 1))
;     (map :node ((keyword (str (dec next))) total)))


(re-frame/register-handler
  :fill-tiers
  (fn [db _]
    (let [cols (-> db :model :Gene :collections seq)]

      (println "running" (reduce
                           (fn [total next]
                             (let [parents (map :node ((keyword (str (dec next))) total))]
                               (assoc total (keyword (str next))
                                            (mapcat (fn [parent]
                                                      (map (fn [rando] (assoc {} :node rando :parent parent))
                                                           (get-random-from (-> db :model) parent 3)))

                                                    parents))
                               ))
                           {:1 [{:node :Gene :parent nil}]}
                           [2 3]))

      (assoc db :tiers (reduce
                         (fn [total next]
                           (let [parents (map :node ((keyword (str (dec next))) total))]
                             (assoc total (keyword (str next))
                                          (vec (mapcat (fn [parent]
                                                     (map (fn [rando] (assoc {} :node rando :parent parent))
                                                          (get-random-from (-> db :model) parent 3)))

                                                   parents)))
                             ))
                         {:1 [{:node :Gene :parent nil}]}
                         [2 3])))))

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

(re-frame/register-handler
  :click-node
  (fn [db [_ label tier]]
    (assoc-in db [:data-view] {:selected (keyword label)
                               :tier tier})))