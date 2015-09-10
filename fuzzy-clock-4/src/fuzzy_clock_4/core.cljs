(ns ^:figwheel-always fuzzy-clock-4.core
    (:require [om.core :as om :include-macros true]
              [om.dom :as dom :include-macros true]))

(enable-console-print!)

(def slow-color-period 28800) ; seconds   28800 == 8 hours

(def fast-color-period (/ slow-color-period 100)) ; seconds   

(def squares-period (/ fast-color-period 10)) ; seconds

(def update-time-interval 25) ; ms

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
  "Returns (fn [x]) where x is 0-2π, (fn [x]) returns a string 'rgb(r,g,b)', where r, g, b are produced by sin functions with given offsets and frequencies."
  (fn [x]
    (rgb-str
      ((sin-color-fn r-off r-freq) x)
      ((sin-color-fn g-off g-freq) x)
      ((sin-color-fn b-off b-freq) x))))

(def my-sin-color
  "Returns a function that takes an x and returns a string rgb(r,g,b). r,g,b are sin waves with offsets 0, 2, or 1.3 and frequencies 1, 2 or 1.5."
  (sin-rgb [0 1] [2 2] [1.3 1.5]))

(defn background [fast-x slow-x]
  "Given an x 0-2π, returns #js {:background .... }'"
    #js {:background (linear-gradient-str
                      (my-sin-color slow-x)
                      (my-sin-color fast-x))})

(defn samples-from-period [n]
  "Takes n samples from the period of a sin wave (0-2π)"
  (let [scale 100
        sample-range (* scale two-pi)
        samples (range sample-range)]
    (map 
     #(/ % scale)
     (take-nth (/ sample-range n) samples))))
      
(defn opacities [num-squares offset]
  (->> (samples-from-period num-squares)
       (map (sin-fn (mod offset two-pi) 1))
       (reverse)))

(defn square-div [opacity]
  (dom/span
    #js {:style #js {:opacity opacity}
                     ;:WebkitFilter (str "blur(" (- 1 (* 4 opacity)) "pc)")}
         :id "square"}))

(defn squares [x] 
  (let [num-squares 9
        square-opacities (opacities num-squares x)]
    (dom/div
      #js {:id "squaresContainer"}
      (map square-div square-opacities))))

(defn clock [fast-x slow-x squares-x]
  "Given some x values 0-2π, returns a div of the clock's state."
  (dom/div
    #js {
      :id "clock"
      :style (background fast-x slow-x)}
    (squares squares-x)))

(defn my-cool-viz [start now]
  (let [point-in-cycle (fn [period] (cyclic-time start now period))]
      (clock
        (point-in-cycle fast-color-period)
        (point-in-cycle slow-color-period)
        (point-in-cycle squares-period))))

(defn viz [data owner]
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
        (my-cool-viz (:start-time data) (:now data)))))

(defonce app-state 
  (atom {:start-time (now)}))

(om/root
  viz
  app-state
  {:target (. js/document (getElementById "app"))})

(println "app done+launched!")

