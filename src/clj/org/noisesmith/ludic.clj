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
    "runs all currently enabled rules and updates the clock")
  (fire [this]
    "executes all side-effecting consequences of the current state")
  (clock [this]
    "the comparable data representing \"now\""))

(defprotocol Rule
  (ready [this state]
    "boolean, is this rule implied by the current state?")
  (run [this state]
    "applies this rule to the state, returning a new state")
  (execute [this state]
    "runs all side effects implied by this rule, given the state"))

(defrecord Game
           [game-state rules t]
  (enabled [this]
    (filter #(ready % game-state) rules))
  (state [this]
    game-state)
  (transition [this message]
    (message this))
  (tick [this]
    (-> this
        (update :game-state
                #(reduce (fn [st rule]
                           (run rule st))
                         rules))
        (update :t inc)))
  (fire [this]
    (doseq [rule rules]
      (execute rule game-state)))
  (clock [this] t))
