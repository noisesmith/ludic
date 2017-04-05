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

(defn consume
  ([state message] (consume state message (constantly nil)))
  ([state message call-back]
   (let [[action place value mandatory] message
         applies? (matches? message mandatory)
         next-state (if applies?
                      (apply-action state action place value)
                      state)]
     (call-back applies? message state next-state)
     next-state)))

#_(defn do-coordinated
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

#_(defn propagate!
    [state transmission-fn]
    (let [events (get state ::pending)]
      (doseq [event events]
        (transmission-fn event))
      (dissoc state ::pending)))
