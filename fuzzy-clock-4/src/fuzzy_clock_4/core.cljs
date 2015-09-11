(ns ^:figwheel-always fuzzy-clock-4.core
    (:require [om.core :as om :include-macros true]
              [om.dom :as dom :include-macros true]
              [thi.ng.color.gradients :as gradients]))

(enable-console-print!)


(def slow-period 28800) ; seconds   28800 == 8 hours

(def medium-period (/ slow-period 100)) ; seconds   

(def fast-period (/ slow-period 1000)) ; seconds

(def update-time-interval 25) ; ms

(defn now [] (.now js/Date))

(defn square [x] (* x x))

(defn round [x] (.round js/Math x))

(defn map-range [[a1 a2] [b1 b2] s] (+ b1 (/ (* (- s a1) (- b2 b1)) (- a2 a1))))

(def pi (.-PI js/Math))

(def two-pi (* 2 pi))

(defn rgb-str [[r g b]]
  (str "rgb(" r "," g "," b ")"))

(defn linear-gradient-str [bottom-left top-right]
  (str "linear-gradient(to top right," bottom-left "," top-right ")"))

(def map-to-color-range
  "Maps a value 0-1 to a value 0-255"
  (partial map-range [0 1] [0 255]))

(defn my-sin-color [x]
  ;; see https://github.com/thi-ng/color/blob/master/src/gradients.org
  (let [color-fn #(gradients/cosine-gradient-color 
    [0.5 0.5 0.5] [1 1 1] [1.0 1.0 1.0] [0 0.3333 0.6666]
                    %)]
  (->> (color-fn x) 
       (map map-to-color-range)
       (map round)
       (rgb-str))))

(defn sin-fn [offset frequency]
  (fn [x] 
    (.sin js/Math (+ (* x frequency) offset))))

(defn background [fast-x slow-x]
  "Given an x 0-1, returns #js {:background .... }'"
  #js {:background (linear-gradient-str
                      (my-sin-color slow-x)
                      (my-sin-color fast-x))})

(defn samples-from-period [n]
  "Takes n samples from the period of a sin wave (0-1)"
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
                     ;:WebkitFilter (str "blur(" (map-range [-1 1] [0 7] opacity) "px)")}
         :id "square"}))

(defn squares [x] 
  (let [num-squares 9
        square-opacities (opacities num-squares x)]
    (dom/div
      #js {:id "squaresContainer"}
      (map square-div square-opacities))))

(defn clock [slow-x medium-x fast-x]
  "Given some x values 0-1, returns a div of the clock's state."
  (dom/div
    #js {
      :id "clock"
      :style (background medium-x slow-x) }
    (squares fast-x)))

(defn x-in-cycle [t period target-range]
  "Maps t (a unix time)'s progress through period (in seconds) onto target-range, a vector (e.g. [0 1] or [0 two-pi])."
  (let [ms-per (* 1000 period)]
    (map-range [0 ms-per] 
               target-range
               (mod t ms-per))))

(defn my-cool-viz [t]
  (clock
    (x-in-cycle t slow-period [0 1])
    (x-in-cycle t medium-period [0 1])
    (x-in-cycle t fast-period [0 two-pi])))

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
        (my-cool-viz (:now data)))))

(defonce app-state 
  (atom {:now (now)}))

(om/root
  viz
  app-state
  {:target (. js/document (getElementById "app"))})

(println "app done+launched!")