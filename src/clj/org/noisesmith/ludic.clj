(ns org.noisesmith.ludic
  "\"showing spontaneous and undirected playfulness\"
   let the games begin!
   Ludic is a library describing the passage of logical time as a deterministic
   fold over a series of inputs.")

(defprotocol GameBoard
  (enabled [this]
    "the rules that are currently accessible")
  (state [this]
    "the data collections implementing the full state of the current game")
  (transition [this message]
    "an arbitrary transform of the GameBoard, must return a GameBoard")
  (tick [this]
    "runs a currently enabled rule and updates the clock")
  (fire [this]
    "executes all side-effecting consequences of the next tick")
  (clock [this]
    "the comparable data representing \"now\""))

(defprotocol Rule
  (ready [this state]
    "pseudo-boolean, is this rule implied by the current state?")
  (run [this accepted state]
    "applies this rule to the state, returning a new state")
  (execute [this accepted old-state new-state]
    "runs all side effects implied by this rule, given the state"))

(defn spy
  [handler v & args]
  (when handler
    (apply handler (concat args [(pr-str v)])))
  v)

(defrecord Game
           [game-state rules t debug]
  GameBoard
  (enabled [this]
    (spy debug
         (filter #(ready % game-state) rules)
         ::enabled "returning list of enabled rules..."))
  (state [this]
    game-state)
  (transition [this message]
    (message this))
  (tick [this]
    (if-let [rule (spy debug
                       (first (enabled this))
                       ::tick "ticking rule is")]
      (-> this
          (update :game-state
                  (fn [s]
                    (run rule (ready rule s) s)))
          (update :t inc))
      this))
  (fire [this]
    (let [upcoming (-> this
                       (tick)
                       (:game-state))]
      (when-let [rule (spy debug
                           (first (enabled this))
                           ::fire "firing rule is")]
        (-> this
            (enabled)
            (first)
            (as-> r
                  (execute r (ready r game-state) this upcoming))))))
  (clock [this] t))
