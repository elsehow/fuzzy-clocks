(ns ^:figwheel-always fuzzy-clock-1.core
    (:require-macros [cljs.core.async.macros :refer [go]])
    (:require [om.core :as om :include-macros true]
              [om.dom :as dom :include-macros true]
              [cljs.core.async :refer [put! chan <!]]))

(enable-console-print!)
(def period 1000)
(def update-time-interval 100)
(defn now [] (.now js/Date))
(defn square [x] (* x x))
(defn map-range [[a1 a2] [b1 b2] s]
  (+ b1 (/ (* (- s a1) (- b2 b1)) (- a2 a1))))

(defn round [x]
  (.round js/Math x))

(def color-fn
  "Maps a value 0-1 to a value 0-255"
  (partial map-range [0 1] [0 255]))

(defn sin-color-fn [offset frequency]
  "Returns a number 0-255 produced on a sin wave with offset"
  (fn [x]
    (round
      (color-fn
        (square (.sin js/Math (+ (* x frequency) offset)))))))

(def red 
  (sin-color-fn 0 0))

(def green
  (sin-color-fn 2 2))

(def blue 
  (sin-color-fn 1.3 5))

(defn rgb-str [r g b]
  (str "rgb(" r "," g "," b ")"))

(defn background [t]
  "Given a time t from 0-1, returns #js {:backgroundColor: 'rgb:(...)'"
  #js {:backgroundColor (rgb-str
                          (red t)
                          (green t)
                          (blue t))})

(def time-in-sin-period
  "Function that maps value time-period to a value  0 -2π"
  (partial map-range [0 period] [0 (* 2 (.-PI js/Math))]))

(defn cyclic-time [t0 t1]
  "Maps clock's progress through period onto the period of a sin wave (0-2π)."
  (time-in-sin-period
    (mod (- t1 t0) period)))

(defonce app-state 
  (atom {:start-time (now)}))

(defn viz [data owner]
  (let [sin-time-fn (partial cyclic-time (:start-time data))]
    (reify
      om/IInitState
      (init-state [_]
        {:now (chan)})
      om/IWillMount
      (will-mount [_]
        (js/setInterval 
          #(om/update! data [:now] (now))
          update-time-interval))
      om/IRender
      (render [_]
        (dom/h1
          ; #js {
          ;   :id "clock"
          ;   :style (background (sin-time-fn (:now data)))}
          ; (dom/h1 
            (sin-time-fn (:now data)))))))

(om/root
  viz
  app-state
  {:target (. js/document (getElementById "app"))})

(println "App launched!")

(defn on-js-reload []
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  ;; (swap! app-state update-in [:__figwheel_counter] inc)
)

