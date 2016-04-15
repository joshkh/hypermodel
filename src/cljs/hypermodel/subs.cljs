(ns hypermodel.subs
    (:require-macros [reagent.ratom :refer [reaction]])
    (:require [re-frame.core :as re-frame]))

(re-frame/register-sub
 :name
 (fn [db]
   (reaction (:name @db))))

(re-frame/register-sub
  :model
  (fn [db]
    (reaction (:model @db))))

(re-frame/register-sub
  :tiers
  (fn [db [_ tier]]
    (reaction (tier (:tiers @db)))))

(re-frame/register-sub
  :data-view
  (fn [db [_ tier]]
    (reaction (:data-view @db))))

(re-frame/register-sub
  :selectable
  (fn [db _]
    (let [dv (re-frame/subscribe [:data-view])]
      (reaction (-> @db :model (-> @dv :selected))))))
