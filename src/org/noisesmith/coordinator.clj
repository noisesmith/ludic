(ns org.noisesmith.coordinator
  "defines a simple state accumulator")

(defn- matches?
  [data to-match]
  (every? #(= (first (get data %))
              (get to-match %))
          (keys to-match)))

(defn- apply-action
  [state action place value]
  (case action
    :set (assoc state place value)
    :store (update state place conj value)
    :unset (dissoc state place)
    :drop (update state place rest)))

(defn- call-back
  "calls any callbacks on the state, and removes any that return a false value"
  [applies? state next-state place message]
  (update-in next-state
             [::local place]
             (fn [callbacks]
               (filter (fn [f] (f applies? message next-state))
                       callbacks))))

(defn consume
  [state message]
  (let [[action place value mandatory] message
        applies? (matches? message mandatory)
        next-state (if applies?
                     (apply-action state action place value)
                     state)
        updated (if (contains? (::local next-state) place)
                  (call-back applies? state next-state place message)
                  next-state)]
    updated))

(defn do-coordinated
  "nominates us to perform f, if no-one else nominates first,
   if someone else wins f will never be called"
  [state place f]
  (let [claim (gensym (str place))
        nomination-place [::nominated place]]
    (-> state
        (assoc-in [::local place]
                  (fn [applied? state next-state _ _]
                    (when applied?
                      (f))))
        (update ::pending (fnil conj [])
                [:store nomination-place claim]
                [:set place claim {nomination-place claim}]))))

(defn propagate!
  [state transmission-fn]
  (let [events (get state ::pending)]
    (doseq [event events]
      (transmission-fn event))
    (dissoc state ::pending)))
