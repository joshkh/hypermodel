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
