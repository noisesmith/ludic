(ns org.noisesmith.coordinator.compare-and-set
  (:require [org.noisesmith.coordinator :as coord]))

(defn merge-over
  ;; TODO - optional max length of value list under each key
  [older newer]
  (merge-with conj older newer))

(defn operate
  [state new-state callback]
  (let [apply? (matches? state new-state)
        next-state (if apply?
                     (merge-over state new-state)
                     state)]
    (callback apply? new-state state next-state)
    next-state))
