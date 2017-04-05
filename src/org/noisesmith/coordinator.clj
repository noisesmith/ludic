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
