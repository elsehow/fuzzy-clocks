(ns ^:figwheel-always fuzzy-clock-2.core
    (:require-macros [cljs.core.async.macros :refer [go]])
    (:require [om.core :as om :include-macros true]
              [om.dom :as dom :include-macros true]
              [cljs.core.async :refer [put! chan <!]]))

(enable-console-print!)
(def period-in-seconds 1000) ; 28800 == 8 hours
(def update-time-interval 100) ; ms

(defn now [] (.now js/Date))
(defn square [x] (* x x))
(defn round [x] (.round js/Math x))
(defn map-range [[a1 a2] [b1 b2] s]
  (+ b1 (/ (* (- s a1) (- b2 b1)) (- a2 a1))))

(def period (* 1000 period-in-seconds))

(defn time-in-sin-period [t0 t1]
  "Maps t1's modular progress through period onto the period of a sin wave (0-2π)."
  (map-range [0 period] 
            [0 (* 2 (.-PI js/Math))]
            (mod (- t1 t0) period)))

(def color-fn
  "Maps a value 0-1 to a value 0-255"
  (partial map-range [0 1] [0 255]))

(defn sin-color-fn [offset frequency]
  "Returns a (fn [x]) where x is 0-2π and returns number 0-255."
  (let [sin-fn #(.sin js/Math (+ (* % frequency) offset))]
    (fn [x]
      (-> (sin-fn x)
          (square)
          (color-fn)
          (round)))))
(def red 
  (sin-color-fn 0 0))

(def green
  (sin-color-fn 2 2))

(def blue 
  (sin-color-fn 1.3 5))

(defn rgb-str [r g b]
  (str "rgb(" r "," g "," b ")"))

(defn background [x]
  "Given an x 0-2π, returns #js {:backgroundColor: 'rgb:(...)'"
  #js {:backgroundColor (rgb-str
                          (red x)
                          (green x)
                          (blue x))})


(defonce app-state 
  (atom {:start-time (now)}))

(defn viz [data owner]
  (let [sin-time-fn (partial time-in-sin-period (:start-time data))]
    (reify
      om/IInitState
      (init-state [_]
        {:now (chan)})
      ;; set clock polling interval
      om/IWillMount
      (will-mount [_]
        (js/setInterval 
          #(om/update! data [:now] (now))
          update-time-interval))
      ;; render
      om/IRender
      (render [_]
        (dom/h1
          #js {
            :id "clock"
            :style (background 
                    (sin-time-fn 
                      (:now data)))}
          (sin-time-fn (:now data)))))))

(om/root
  viz
  app-state
  {:target (. js/document (getElementById "app"))})

(println "app done+launched!")

