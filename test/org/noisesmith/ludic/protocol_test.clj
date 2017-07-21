(ns org.noisesmith.ludic.protocol-test
  (:require [clojure.test :as test :refer [is testing deftest]]
            [org.noisesmith.ludic.protocol :as l]))

(defn null-rule
  []
  (reify l/Rule
    (ready [this state] false)
    (run [this picked state] state)
    (execute [this picked state-a state-b])))

(deftest simple-rule-test
  (is (null-rule)))

(defn null-game
  []
  (reify l/GameBoard
    (enabled [this] ())
    (state [this] {})
    (transition [this message] this)
    (tick [this] this)
    (fire [this])
    (clock [this] 0)))

(deftest simple-game-test
  (is (null-game)))
