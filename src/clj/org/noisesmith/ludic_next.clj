(ns org.noisesmith.ludic-next
  (:require [org.noisesmith.ludic :as ludic]))

(defprotocol GameBoard
  (activate [this]
    "marks rules as active in this context, and tracks their context data"))

(defprotocol Rule
  (consumes [this]
    "the parts of a game record it removes data from")
  (produces [this]
    "the parts of a game record it puts data into"))

(defrecord Game
           [game-state rules t]
  GameBoard
  (activate [this]
    (assoc this
           :enabled (first
                     (sequence (comp
                                (map (juxt identity
                                           #(ludic/ready % game-state)))
                                (filter second))
                               rules))))
  ludic/GameBoard
  (enabled [this]
    (:enabled this))
  (state [this]
    game-state)
  (transition [this message x1]
    (message this x1))
  (tick [this]
    (if-let [[rule data] (:enabled this)]
      (-> this
          (update :game-state
                  (fn [s]
                    (ludic/run rule data s)))
          (update :t inc))
      this))
  (fire [this]
    (let [upcoming (-> this (ludic/tick) (:game-state))]
      (when-let [[rule data] (:enabled this)]
        (ludic/execute rule data this upcoming))
      (assoc this :enabled nil)))
  (clock [this]
    t))

(defn step-until-idle
  "iterates fire / tick until no progress is possible"
  [game spy callback]
  (if-not (callback game ::cycle)
    game
    (let [next-game (-> (doto game ludic/fire)
                        (activate)
                        (spy ::cycle)
                        (ludic/tick))]
      (if (= (ludic/clock game)
             (ludic/clock next-game))
        game
        (recur next-game spy callback)))))

(defn main-loop
  ([game get-command dispatch]
   (main-loop game get-command dispatch (constantly nil)))
  ([game get-command dispatch callback]
   (let [spy (fn spy [& context]
               (apply ludic/spy (:debug game) context))
         command (get-command game)]
     (if-not (callback game command)
       game
       (recur (-> game
                  (spy ::pre-dispatch command)
                  (ludic/transition dispatch command)
                  (spy ::post-dispatch command)
                  (step-until-idle spy callback))
              get-command
              dispatch
              callback)))))
