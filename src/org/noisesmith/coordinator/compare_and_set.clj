(ns org.noisesmith.coordinator.compare-and-set
  (:require [org.noisesmith.coordinator :as coord]))

(defn operate
  [state new-state callback]
  (let [apply? (matches? state new-state)
        next-state (if apply? new-state state)]
    (callback apply? new-state state next-state)
    next-state))
