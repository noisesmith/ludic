(ns org.noisesmith.coordinator.state-graph)

(def task-states
  {:new [['f :rejected]
         :available]
   :rejected nil
   :available [['f :claimed]]
   :claimed [:time-out
             :failed
             :done]
   :time-out [:available]
   :failed nil
   :done nil})

(def worker-states
  {:available [['f :working]]
   :working [:available :time-out]
   :time-out [:available]})
