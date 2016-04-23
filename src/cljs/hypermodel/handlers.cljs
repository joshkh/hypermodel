(ns hypermodel.handlers
  (:require-macros [cljs.core.async.macros :refer [go]])
    (:require [re-frame.core :as re-frame]
              [hypermodel.db :as db]
              [cljs-http.client :as http]
              [clojure.set :as set]
              [hypermodel.transforms :as t]
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
      ;(println "random" (get-random-from cols 4))
      )
    db))

(re-frame/register-handler
  :handle-bootstrap-model
  (fn [db [_ response]]
    (re-frame/dispatch [:fill-tiers])
    (assoc-in db [:model] response)))



(def modelv ['({:self :A, :parent nil})
             '({:self :B, :parent :A} {:self :X, :parent :A})
             '({:self :Y, :parent :B} {:self :C, :parent :B} {:self :E, :parent :B})
             '({:self :D, :parent :C} {:self :A, :parent :E})
             '({:self :E, :parent :D} {:self :B, :parent :A} {:self :X, :parent :A})])

(defn center-tier [tier]
  (map (fn [node]
         (assoc node :a 0
                     :r 20)) tier))

(defn evenly-space-tier [tier]
  (let [nodes-in-tier (count tier)
        chunks (/ 360 nodes-in-tier)]
    (map-indexed (fn [index node]
                   (assoc node :r 20
                               :a (* chunks (inc index)))) tier)))

(defn inherit-angle-tier [parent-tier child-tier]
  (let [grouped-parents (group-by :self parent-tier)]
    (map (fn [node]
           (assoc node :r 20
                       :a (-> node :parent grouped-parents first :a))) child-tier)))

(defn re-angle [tiers]
  (reduce (fn [total next-tier]
            (cond
              (= next-tier 0) (conj total (center-tier (nth modelv next-tier)))
              (= next-tier 1) (conj total (evenly-space-tier (nth modelv next-tier)))
              :else (conj total (inherit-angle-tier (nth total (dec next-tier))
                                                    (nth modelv next-tier)))))
          [] (range 0 (count modelv))))


(defn radial [origin-x origin-y radius angle]
  (println "testing")
  {:x (+ origin-x (* 2 radius (Math/cos (* angle (/ (.-PI js/Math) 180)))))
   :y (+ origin-y (* radius (Math/sin (* angle (/ (.-PI js/Math) 180)))))})


(defn xy-center-tier [tiers]
  (map (fn [node]
         (println "xy-centering node" node)
         (let [{x :x y :y} (radial 0 0 50 (:a node))]
           (assoc node :cx x :cy y))) tiers))

(defn place [tiers]
  (reduce (fn [total next-tier]
            (cond
              ;(= next-tier 0) (conj total (center-tier (nth tiers next-tier)))
              (> next-tier 0) (conj total (xy-center-tier (nth tiers next-tier)))
              :else (conj total (nth tiers next-tier) )))
          [] (range 0 (count tiers))))



(re-frame/register-handler
  :bootstrap-app
  (fn [db _]
    (println "running")
    ;(go (re-frame/dispatch [:handle-bootstrap-model (<! (model "www.flymine.org/query")) ]))
    (update db :new (comp place re-angle))))