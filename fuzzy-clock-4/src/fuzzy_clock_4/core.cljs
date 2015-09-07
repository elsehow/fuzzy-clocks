(ns ^:figwheel-always fuzzy-clock-4.core
    (:require [om.core :as om :include-macros true]
              [om.dom :as dom :include-macros true]))

(enable-console-print!)

(def color-period 28800) ; seconds   28800 == 8 hours

(def squares-period 10) ; seconds  

(def update-time-interval 100) ; ms

(defn now [] (.now js/Date))

(defn square [x] (* x x))

(defn round [x] (.round js/Math x))

(defn map-range [[a1 a2] [b1 b2] s] (+ b1 (/ (* (- s a1) (- b2 b1)) (- a2 a1))))

(def pi (.-PI js/Math))

(def two-pi (* 2 pi))

(defn rgb-str [r g b]
  (str "rgb(" r "," g "," b ")"))

(defn linear-gradient-str [bottom-left top-right]
  (str "linear-gradient(to top right," bottom-left "," top-right ")"))

(defn cyclic-time [t0 t1 period]
  "Maps t1's modular progress through period (in seconds) onto the period of a sin wave (0-2π)."
  (let [ms-period (* 1000 period)]
    (map-range [0 ms-period] 
               [0 two-pi] 
               (mod (- t1 t0) ms-period))))

(def map-to-color-range
  "Maps a value 0-1 to a value 0-255"
  (partial map-range [0 1] [0 255]))

(defn sin-fn [offset frequency]
  (fn [x] 
    (.sin js/Math (+ (* x frequency) offset))))

(defn sin-color-fn [offset frequency]
  "Returns a (fn [x]) where x is 0-2π and returns number 0-255."
  (fn [x]
    (-> ((sin-fn offset frequency) x)
        (square)
        (map-to-color-range)
        (round))))

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

(defn samples-from-period [n]
  "Takes n samples from the period of a sin wave (0-2π)"
  (let [factor 100
        sample-range (* factor two-pi)]
    (map 
     #(/ % factor)
     (take-nth (/ sample-range n) (range sample-range)))))
      
(defn opacities [num-squares offset]
  (map
   #(Math/sin (+ % (mod offset two-pi)))
   (samples-from-period num-squares)))


(defn clock [x]
  "Given an x 0-2π, returns a div with a background defined by x."
  (dom/div
    #js {
      :id "clock"
      :style (background x)}))


;; TODO
;; where to draw squares?
;; what to name those point-in-color-cycle things
;; and how to make them more DRY
(println (opacities 10 6.29))

(defn viz [data owner]
  (let [point-in-color-cycle #(cyclic-time % 
                                           (:start-time data)
                                           color-period)
        point-in-squares-cycle #(cyclic-time % 
                                           (:start-time data)
                                           squares-period)]
    (reify
      ;; mount
      om/IWillMount
      (will-mount [_]
        (js/setInterval 
          #(om/update! data [:now] (now))
          update-time-interval))
      ;; render 
      om/IRender
      (render [_]
        (clock (point-in-color-cycle (:now data)))))))

(defonce app-state 
  (atom {:start-time (now)}))

(om/root
  viz
  app-state
  {:target (. js/document (getElementById "app"))})

(println "app done+launched!")

