(ns ^:figwheel-always fuzzy-clock-3.core
    (:require-macros [cljs.core.async.macros :refer [go]])
    (:require [om.core :as om :include-macros true]
              [om.dom :as dom :include-macros true]
              [cljs.core.async :refer [put! chan <!]]))

(enable-console-print!)

(def period-in-seconds 30) ; 28800 == 8 hours

(def update-time-interval 100) ; ms

(defn now [] (.now js/Date))

(defn square [x] (* x x))

(defn round [x] (.round js/Math x))

(defn map-range [[a1 a2] [b1 b2] s] (+ b1 (/ (* (- s a1) (- b2 b1)) (- a2 a1))))

(def period (* 1000 period-in-seconds))

(defn rgb-str [r g b]
  (str "rgb(" r "," g "," b ")"))

(defn linear-gradient-str [bottom-left top-right]
  (str "linear-gradient(to top right," bottom-left "," top-right ")"))


(defn cyclic-time [t0 t1]
  "Maps t1's modular progress through period onto the period of a sin wave (0-2π)."
  (map-range [0 period] 
            [0 (* 2 (.-PI js/Math))]
            (mod (- t1 t0) period)))

(def map-to-color-range
  "Maps a value 0-1 to a value 0-255"
  (partial map-range [0 1] [0 255]))

(defn sin-color-fn [offset frequency]
  "Returns a (fn [x]) where x is 0-2π and returns number 0-255."
  (let [sin-fn #(.sin js/Math (+ (* % frequency) offset))]
    (fn [x]
      (-> (sin-fn x)
          (square)
          (map-to-color-range)
          (round)))))

(defn sin-rgb [[r-off r-freq] [g-off g-freq] [b-off b-freq]]
  "Retruns (fn [x]) where x is 0-2π, (fn [x]) returns a string 'rgb(a,b,c)', where a, b, c are produced by sin functions with given offsets and frequencies."
    (fn [x]
      (let [c #((sin-color-fn %1 %2) x)]
        (rgb-str
          (c r-off r-freq)
          (c g-off g-freq)
          (c b-off b-freq)))))

(def slow-color
  (sin-rgb [0 1] [2 2] [1.3 1.3]))

(def fast-color 
  (sin-rgb [2 5] [1.5 6] [5 8]))

(defn background [x]
  "Given an x 0-2π, returns #js {:background: 'rgb:(...)'"
    #js {:background (linear-gradient-str
                      (slow-color x)
                      (fast-color x))})


(defn gradient [x]
  "Given an x 0-2π, returns a div with a background defined by x."
  (dom/h1
    #js {
      :id "clock"
      :style (background x)}))

(defn viz [data owner]
  (reify
    om/IInitState
    (init-state [_]
      {:now (chan)})
    ;; mount
    om/IWillMount
    (will-mount [_]
      (js/setInterval 
        #(om/update! data [:now] (now))
        update-time-interval))
    ;; render 
    om/IRender
    (render [_]
      (gradient (cyclic-time (:now data)
                             (:start-time data))))))
(defonce app-state 
  (atom {:start-time (now)}))

(om/root
  viz
  app-state
  {:target (. js/document (getElementById "app"))})

(println "app done+launched!")

