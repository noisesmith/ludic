(ns org.noisesmith.coordinator-test
  (:require [clojure.test :as test :refer [deftest testing is]]
            [org.noisesmith.coordinator :as coord]))

(deftest simple-consume-test
  (is (= {:a 0}
         (coord/consume {} [:set :a 0])))
  (is (= {}
         (coord/consume {:a 0} [:unset :a])))
  (is (= {:a [0]}
         (coord/consume {} [:store :a 0])))
  (is (= {:a []}
         (coord/consume {:a [0]} [:drop :a]))))

(deftest consume-drop-test
  (is (= {:a ()}
         (coord/consume {} [:drop :a])))
  (is (= {:a ()}
         (coord/consume {:a ()} [:drop :a]))))
