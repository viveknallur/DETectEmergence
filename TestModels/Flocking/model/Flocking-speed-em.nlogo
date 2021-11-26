extensions[detect]
 
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;                                                      Define global and agent/turtle based variables
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; Declare the global variables forming the model
globals [
  objM              ; Records the value of the objective measure (objM) of emergence in the system. This is a centralised count. In this model it is the 
                    ; number of patches with no turtles within 2 patches from its center.
  changPer          ; The percentage of agents who have detected a change using CUSUM recently.
  emPercent         ; The percentage of agents who believe there is an emergent event.
  
  run-name          ; The name of the simulation run. This is passed into Netlogo from the Java library that initiates the simulation.
  
  lasso-win         ; The size of the Sliding Observation window before lasso. When full it triggers the model selection.
  regWinL           ; The size of the Regression Sliding Observation Window. When full it triggers a regression analysis.
  smoothL           ; The size of the smoothing average window. When full the window is averaged and the average is added to the regression Sliding Observation window. Typically 5.
  
  minNeighSize      ; The minimum number of flockmates and agent must have before it can gossip.
  maxNeighSize      ; The maximum number of flockmates an agent will consider as potential gossiping partner
  changeMemLen      ; How long an agent will remember a change detected by a CUSUM. This is measured in subsequent Regression/CUSUM analysis runs.
  aggThreshold      ; What the gossiping average must be over before an agent will conclude an emergent event.
  threshold-low     ; What value the CUSUM must exceed before a change is detected. Typically set to 4. (h parameter in CUSUM (see thesis)
  scaler            ; What percent an agent will scale the gossiping average to their own local belief. Typically 5% of the difference.
  
  car-number         ; The number of agents in the model. TODO, change this name so it is generic
  
  churn              ; Records the percentage of agents with varying neighbourhood sizes at each timestep.
  
  global-temperature ; A randomly variable simulation of temperature in the system.
]

turtles-own [
  flockmates         ;; agentset of nearby turtles
  nearest-neighbor   ;; closest one of our flockmates
  speed              ; Number of patches forward the agent moves at each timestep
  
  ; DETECT variables ;
  
  ;;;;;;;;;;;;;;;;;;Internal Variables
  myHeading          ; the Direction the agent is travelling
  mySpeed            ; The speed of the agent
  age                ; The age of the agent
  myVision           ; How far the agent can see
  ; The smoothing averaging windows for each variable
  speed-mem          
  heading-mem
  age-mem
  vision-mem
  ; The sliding observation windows for each variable
  mem-int-speed
  mem-int-heading
  mem-int-age
  mem-int-vision
  
  
  ;;;;;;;;;;;;;;;;;;External Variables
  flockHead          ; The average heading of flockmates
  flockSpeed         ; The average speed of flockmates
  distNear           ; The distance to the nearest flockmate
  flockCount         ; The number of agents in the agents flock
  temperature        ; The current temperature
  distanceMap        ; The distance the agent is from the center of the map
  ; The smoothing averaging windows for each variable
  birds-near-mem     ; flockCount
  dist-near-bird-mem
  flock-head-mem
  flock-speed-mem
  temperature-mem
  distMap-mem
  ; The sliding observation windows for each variable
  mem-ext-bn
  mem-ext-dn
  mem-ext-fh
  mem-ext-fs
  mem-ext-temperature
  mem-ext-distMap
  
  ; These store the CUSUM values for each internal-external variable pair.
  ; First my speed against all external variables
  coeff-bn-myS_cusum   ; birds near 
  coeff-dn-myS_cusum   ; distance to nearest
  coeff-fh-myS_cusum   ; flock heading
  coeff-fs-myS_cusum   ; flock speed
  coeff-tm-myS_cusum   ; temperature
  coeff-dm-myS_cusum   ; distance map
  ; My heading
  coeff-bn-myH_cusum
  coeff-dn-myH_cusum
  coeff-fh-myH_cusum
  coeff-fs-myH_cusum
  coeff-tm-myH_cusum
  coeff-dm-myH_cusum
  ;My Age
  coeff-bn-myAge_cusum
  coeff-dn-myAge_cusum
  coeff-fh-myAge_cusum
  coeff-fs-myAge_cusum
  coeff-tm-myAge_cusum
  coeff-dm-myAge_cusum
  ;My Vision
  coeff-bn-myVision_cusum
  coeff-dn-myVision_cusum
  coeff-fh-myVision_cusum
  coeff-fs-myVision_cusum
  coeff-tm-myVision_cusum
  coeff-dm-myVision_cusum
  
  
  ;Emergence Stuff
  localV            ; My local emergence belief, whether I have recently detected a change through CUSUM.
  changeDecay       ; Used to indicate how many more cycles I will remember a change detected by CUSUM for
  signChange        ; Used to store whether a change has been detected by CUSUM at this time step
  Vq                ; My local gossiping average score
  Vp                ; My gossiping partners average score
       
  emergeBelief?     ; Boolean to indicate if agent believes there is a current emergent event
  emergeRelent?     ; Boolean to indicate if agent believe a detected emergent event has now finished.
  
  varsAdded?        ; Boolean to indicate if the agent has told DETect about its internal and external variables
  relsChosen?       ; Boolean to indicate if the agent has selected a model using DETect
  id                ; The name of the agent
  
  lastFlockSize     ; What was the size of the agents flock at the last timestep
]

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; This go function is what is called every timestep. It is used to prompt the agents to flock and then to call
; relevant DETect associated functionality like choosing variables, running regression and gossiping.
to go
  ask turtles [ flock ]
  set scaler 0.05
  ; This is simply to output the progress of simulation running in headless mode.
  if ticks mod 100 = 0 [
   print (word run-name " is at " ticks) 
  ]
  ; If 20000 timesteps have elapsed print the variable relationships selected by DETect for each agent
  if ticks = 20000 [
   detect:printVariables
  ]
  ; This switches flocking "On" at these time steps
  if ticks = 10000 or ticks = 30000 [
   set max-align-turn 5.0   
  ]
  ; This switches flocking "Off" at these time steps.
  if ticks = 20000 or ticks = 40000 [
   set max-align-turn 0
    
  ]
  ; DETect associated. Ask the turtles to either choose their model, or if the model is already selected (relsChosen == true) to run regressions
  if ticks > 1 [
     ask turtles [
       ifelse relsChosen? [
         run-Regressions
       ][
       choose-variables
       ]
     ]
  ]
  ; Ask turtles to record how many flockmates they currently have so that it can be compared with the next timestep
  ask turtles [
   set lastFlockSize count flockmates 
  ]
  ; DETect: Ask turtles to gossip by swapping information with neighbours.
  swap-neigh
  
  ;; the following line is used to make the turtles
  ;; animate more smoothly.
  repeat 5 [ ask turtles [ fd speed / 5 ] display ]
  ;; for greater efficiency, at the expense of smooth
  ;; animation, substitute the following line instead:
  ;;   ask turtles [ fd speed ]
  
  ; Update global parameters that may be tracked and create a log file every 50 timesteps.
  if ticks > 0 and ticks mod 50 = 0 [
   cal-globals
   write-to-file-comprehensive 
  ]
  ; Update the temperature
  if ticks > 0 and ticks mod 20 = 0 [
    update-temp 
  ]
  ; Advance the clock
  tick
end

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;                                         Turtle/Agent based procedures
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; This function is called to get the agents to flock
to flock  
  find-flockmates
  if any? flockmates
    [ find-nearest-neighbor
      ifelse distance nearest-neighbor < minimum-separation
        [ separate ]
        [ align
          cohere ] 
      set speed (speed + mean [speed] of flockmates) / 2 
      get-Int-Variables ; DETect
      get-Ext-Variables ; DETect
      fill-memory-em    ; DETect
    ]
  set speed speed + random-normal 0 random-acceleration
  if (not allow-move-backward? and speed < 0)
    [ set speed 0 ]
end

; Find the other turtles that within the agents field of vision
to find-flockmates  ;; turtle procedure
  set flockmates other turtles in-radius vision
end

; Find the nearest flockmate
to find-nearest-neighbor ;; turtle procedure
  set nearest-neighbor min-one-of flockmates [distance myself]
end

;;; SEPARATE
to separate  ;; turtle procedure
  turn-away ([heading] of nearest-neighbor) max-separate-turn
end

;;; ALIGN
to align  ;; turtle procedure
  turn-towards average-flockmate-heading max-align-turn
end

to-report average-flockmate-heading  ;; turtle procedure
                                     ;; We can't just average the heading variables here.
                                     ;; For example, the average of 1 and 359 should be 0,
                                     ;; not 180.  So we have to use trigonometry.
  let x-component sum [sin heading] of flockmates
  let y-component sum [cos heading] of flockmates
  ifelse x-component = 0 and y-component = 0
    [ report heading ]
    [ report atan x-component y-component ]
end

;;; COHERE
to cohere  ;; turtle procedure
  turn-towards average-heading-towards-flockmates max-cohere-turn
end

to-report average-heading-towards-flockmates  ;; turtle procedure
                                              ;; "towards myself" gives us the heading from the other turtle
                                              ;; to me, but we want the heading from me to the other turtle,
                                              ;; so we add 180
  let x-component mean [sin (towards myself + 180)] of flockmates
  let y-component mean [cos (towards myself + 180)] of flockmates
  ifelse x-component = 0 and y-component = 0
    [ report heading ]
    [ report atan x-component y-component ]
end

to turn-towards [new-heading max-turn]  ;; turtle procedure
  turn-at-most (subtract-headings new-heading heading) max-turn
end

to turn-away [new-heading max-turn]  ;; turtle procedure
  turn-at-most (subtract-headings heading new-heading) max-turn
end

;; turn right by "turn" degrees (or left if "turn" is negative),
;; but never turn more than "max-turn" degrees
to turn-at-most [turn max-turn]  ;; turtle procedure
  ifelse abs turn > max-turn
    [ ifelse turn > 0
      [ rt max-turn ]
      [ lt max-turn ] ]
    [ rt turn ]
end



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;                                         DETect procedures
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

; This updates the internal variables of the agent
to get-Int-Variables
  set myHeading heading
  set mySpeed speed
end

; This calculates and updates the external variables of the agent
to get-Ext-Variables
  set flockHead mean [myHeading] of other turtles in-radius vision
  set flockSpeed mean [mySpeed] of other turtles in-radius vision
  set distNear distance min-one-of other turtles in-radius vision [distance myself]
  set flockCount count other turtles in-radius vision
end

; The observations for all internal and external variables are stored in list/arrays. This function
; adds the observations calculated in the above 2 procedures the relevant short term list "varName-mem". If that is of length 5
; it is averaged and the result added to the relevant long term sliding observation window "mem-int-varName" for internal
; or "mem-ext-varName" for external.
to fill-memory-em
  ;; Internal Variables  
  set heading-mem lput (myHeading) heading-mem
  set speed-mem lput (mySpeed) speed-mem
  set age-mem lput (age) age-mem
  set vision-mem lput (myVision) vision-mem
  
  ;; External Variables
  set birds-near-mem lput (flockCount) birds-near-mem
  set dist-near-bird-mem lput (distNear) dist-near-bird-mem
  set flock-head-mem lput (flockHead) flock-head-mem
  set flock-speed-mem lput (flockSpeed) flock-speed-mem
  set temperature-mem lput (global-temperature) temperature-mem
  set distMap-mem lput (distancexy 0 0) distMap-mem
  
  ;; Every 5 we aggregate by calculating the mean
  if length heading-mem = smoothL [
    set mem-int-speed lput mean speed-mem mem-int-speed
    set mem-int-heading lput mean heading-mem mem-int-heading
    set mem-int-age lput mean age-mem mem-int-age
    set mem-int-vision lput mean vision-mem mem-int-vision
    
    set mem-ext-bn lput mean birds-near-mem mem-ext-bn
    set mem-ext-dn lput mean dist-near-bird-mem mem-ext-dn
    set mem-ext-fh lput mean flock-head-mem mem-ext-fh
    set mem-ext-fs lput mean flock-speed-mem mem-ext-fs
    set mem-ext-temperature lput mean temperature-mem mem-ext-temperature
    set mem-ext-distMap lput mean distMap-mem mem-ext-distMap
    
    ;; Now clear memory
    ;; Internal
    set heading-mem[]
    set speed-mem[]
    set age-mem[]
    set vision-mem[]
    
    ;; External
    set birds-near-mem[]
    set dist-near-bird-mem[]
    set flock-head-mem[]
    set flock-speed-mem[]
    set temperature-mem[]
    set distMap-mem[]
  ]
end 

; This function is the gossiping function for DETect. 
to swap-neigh
  ; First ask each turtle to pick a random flockmate. If the flock is too small (belwo minNeighSize) the turtle does not gossip;
  ; If the flock is too big (above maxNeighSize) then only the closest maxNeighSize neighbours are potential gossip partners.
  ask turtles [
    let flockmatesGoss other turtles in-radius (vision)
    ifelse count flockmatesGoss >= minNeighSize [
      let flockmates-all sort-by [[distance myself] of ?1 < [distance myself] of ?2] flockmatesGoss
      while [length flockmates-all > maxNeighSize][
        let fmallSize length flockmates-all
        set flockmates-all remove-item (fmallSize - 1) flockmates-all
      ]
      
      let maxNum length flockmates-all
      let rand random maxNum
      let partner item rand flockmates-all ; Pick a random partner
      
      ; Get partners Vp
      set Vq [Vp] of partner
      set Vp (Vp + Vq) / 2 ; average
      let newVp Vp
        ask partner [
                set Vp newVp ; ask partner to update
        ]
    ][
        set Vp 0 ; If no partner, set Vp to 0, as agent is alone and thus not gossiping and no emergence present.

    ]
  ]
  ; Next ask turtles to scale towards their own feedback state (localV). 
  ; Thus requires constant feedback from neighbours to change mind
  ask turtles [
    let diff Vp - localV
    set Vp Vp - (diff * (scaler))
  ]
end

; This function is used to initiate the model selection process using Lasso.
to choose-variables
  if length mem-int-speed = lasso-win [ ; Are the observation windows full? All windows are updated concurrently so they are all the same size.
    detect:startR id
    if varsAdded? = false [ ; If the agent hasn't already told DETect what the variables being monitored are, do it now.
      ;Add internal variables
      detect:add-var id "ext" "birdsNear"
      detect:add-var id "ext" "distNear"
      detect:add-var id "ext" "flockHead"
      detect:add-var id "ext" "flockSpeed"
      detect:add-var id "ext" "temperature"
      detect:add-var id "ext" "distMap" 
      
      ;Add internal variables
      detect:add-var id "int" "myHead"
      detect:add-var id "int" "speed"
      detect:add-var id "int" "age"
      detect:add-var id "int" "vision"
      
      set varsAdded? true
    ]
    ; Pass the internal variable data to DETect
    detect:update-var id "int" "myHead" mem-int-heading
    detect:update-var id "int" "speed" mem-int-speed
    detect:update-var id "int" "age" mem-int-age
    detect:update-var id "int" "vision" mem-int-vision
    
    ; Pass the external variable data to DETect
    detect:update-var id "ext" "birdsNear" mem-ext-bn
    detect:update-var id "ext" "distNear" mem-ext-dn
    detect:update-var id "ext" "flockHead" mem-ext-fh
    detect:update-var id "ext" "flockSpeed" mem-ext-fs
    detect:update-var id "ext" "temperature" mem-ext-temperature
    detect:update-var id "ext" "distMap" mem-ext-distMap
    
    ;Run the lasso. The return value is the number of internal variables in the model. If this is above 0 model selection was successful.
    let intChose detect:runLasso id
    
    detect:stopR id
    
    if intChose > 0 [ ; Model select was successul, so delete all but the latest 20 observations.
      set relsChosen? true
      set mem-int-heading sublist mem-int-heading 480 500
      set mem-int-speed sublist mem-int-speed 480 500
      set mem-int-age sublist mem-int-age 480 500
      set mem-int-vision sublist mem-int-vision 480 500
    
      set mem-ext-bn sublist mem-ext-bn 480 500
      set mem-ext-dn sublist mem-ext-dn 480 500
      set mem-ext-fh sublist mem-ext-fh 480 500
      set mem-ext-fs sublist mem-ext-fs 480 500
      set mem-ext-temperature sublist mem-ext-temperature 480 500
      set mem-ext-distMap sublist mem-ext-distMap 480 500
    ]
    slide-Window
  ]
end

; This function is used to slide each of the observation windows. It does this by simply deleting the oldest 10.
to slide-Window
  set mem-int-heading remove-item 0 mem-int-heading
  set mem-int-heading remove-item 0 mem-int-heading
  set mem-int-heading remove-item 0 mem-int-heading
  set mem-int-heading remove-item 0 mem-int-heading
  set mem-int-heading remove-item 0 mem-int-heading
  set mem-int-heading remove-item 0 mem-int-heading
  set mem-int-heading remove-item 0 mem-int-heading
  set mem-int-heading remove-item 0 mem-int-heading
  set mem-int-heading remove-item 0 mem-int-heading
  set mem-int-heading remove-item 0 mem-int-heading  
  
  set mem-int-speed remove-item 0 mem-int-speed
  set mem-int-speed remove-item 0 mem-int-speed
  set mem-int-speed remove-item 0 mem-int-speed
  set mem-int-speed remove-item 0 mem-int-speed
  set mem-int-speed remove-item 0 mem-int-speed
  set mem-int-speed remove-item 0 mem-int-speed
  set mem-int-speed remove-item 0 mem-int-speed
  set mem-int-speed remove-item 0 mem-int-speed
  set mem-int-speed remove-item 0 mem-int-speed
  set mem-int-speed remove-item 0 mem-int-speed
  
  set mem-int-age remove-item 0 mem-int-age
  set mem-int-age remove-item 0 mem-int-age
  set mem-int-age remove-item 0 mem-int-age
  set mem-int-age remove-item 0 mem-int-age
  set mem-int-age remove-item 0 mem-int-age
  set mem-int-age remove-item 0 mem-int-age
  set mem-int-age remove-item 0 mem-int-age
  set mem-int-age remove-item 0 mem-int-age
  set mem-int-age remove-item 0 mem-int-age
  set mem-int-age remove-item 0 mem-int-age
  
  set mem-int-vision remove-item 0 mem-int-vision
  set mem-int-vision remove-item 0 mem-int-vision
  set mem-int-vision remove-item 0 mem-int-vision
  set mem-int-vision remove-item 0 mem-int-vision
  set mem-int-vision remove-item 0 mem-int-vision
  set mem-int-vision remove-item 0 mem-int-vision
  set mem-int-vision remove-item 0 mem-int-vision
  set mem-int-vision remove-item 0 mem-int-vision
  set mem-int-vision remove-item 0 mem-int-vision
  set mem-int-vision remove-item 0 mem-int-vision
  
  ; External
  set mem-ext-bn remove-item 0 mem-ext-bn
  set mem-ext-bn remove-item 0 mem-ext-bn
  set mem-ext-bn remove-item 0 mem-ext-bn
  set mem-ext-bn remove-item 0 mem-ext-bn
  set mem-ext-bn remove-item 0 mem-ext-bn
  set mem-ext-bn remove-item 0 mem-ext-bn
  set mem-ext-bn remove-item 0 mem-ext-bn
  set mem-ext-bn remove-item 0 mem-ext-bn
  set mem-ext-bn remove-item 0 mem-ext-bn
  set mem-ext-bn remove-item 0 mem-ext-bn
  
  set mem-ext-dn remove-item 0 mem-ext-dn
  set mem-ext-dn remove-item 0 mem-ext-dn
  set mem-ext-dn remove-item 0 mem-ext-dn
  set mem-ext-dn remove-item 0 mem-ext-dn
  set mem-ext-dn remove-item 0 mem-ext-dn
  set mem-ext-dn remove-item 0 mem-ext-dn
  set mem-ext-dn remove-item 0 mem-ext-dn 
  set mem-ext-dn remove-item 0 mem-ext-dn
  set mem-ext-dn remove-item 0 mem-ext-dn
  set mem-ext-dn remove-item 0 mem-ext-dn
  
  set mem-ext-fh remove-item 0 mem-ext-fh
  set mem-ext-fh remove-item 0 mem-ext-fh
  set mem-ext-fh remove-item 0 mem-ext-fh
  set mem-ext-fh remove-item 0 mem-ext-fh
  set mem-ext-fh remove-item 0 mem-ext-fh
  set mem-ext-fh remove-item 0 mem-ext-fh
  set mem-ext-fh remove-item 0 mem-ext-fh
  set mem-ext-fh remove-item 0 mem-ext-fh
  set mem-ext-fh remove-item 0 mem-ext-fh
  set mem-ext-fh remove-item 0 mem-ext-fh
  
  set mem-ext-fs remove-item 0 mem-ext-fs
  set mem-ext-fs remove-item 0 mem-ext-fs
  set mem-ext-fs remove-item 0 mem-ext-fs
  set mem-ext-fs remove-item 0 mem-ext-fs
  set mem-ext-fs remove-item 0 mem-ext-fs
  set mem-ext-fs remove-item 0 mem-ext-fs
  set mem-ext-fs remove-item 0 mem-ext-fs
  set mem-ext-fs remove-item 0 mem-ext-fs
  set mem-ext-fs remove-item 0 mem-ext-fs
  set mem-ext-fs remove-item 0 mem-ext-fs
  
  set mem-ext-temperature remove-item 0 mem-ext-temperature
  set mem-ext-temperature remove-item 0 mem-ext-temperature
  set mem-ext-temperature remove-item 0 mem-ext-temperature
  set mem-ext-temperature remove-item 0 mem-ext-temperature
  set mem-ext-temperature remove-item 0 mem-ext-temperature
  set mem-ext-temperature remove-item 0 mem-ext-temperature
  set mem-ext-temperature remove-item 0 mem-ext-temperature
  set mem-ext-temperature remove-item 0 mem-ext-temperature
  set mem-ext-temperature remove-item 0 mem-ext-temperature
  set mem-ext-temperature remove-item 0 mem-ext-temperature
  
  set mem-ext-distMap remove-item 0 mem-ext-distMap
  set mem-ext-distMap remove-item 0 mem-ext-distMap
  set mem-ext-distMap remove-item 0 mem-ext-distMap
  set mem-ext-distMap remove-item 0 mem-ext-distMap
  set mem-ext-distMap remove-item 0 mem-ext-distMap
  set mem-ext-distMap remove-item 0 mem-ext-distMap
  set mem-ext-distMap remove-item 0 mem-ext-distMap
  set mem-ext-distMap remove-item 0 mem-ext-distMap
  set mem-ext-distMap remove-item 0 mem-ext-distMap
  set mem-ext-distMap remove-item 0 mem-ext-distMap
end

; This function is used to initiate the regression analysis for DETect
to run-Regressions
  if length mem-int-speed = regWinL[  ; Is the observation window full? All will be of equal length as they are update concurrently.
    ; Start R and send the latest variable values.
    detect:startR id
    detect:update-var id "int" "myHead" mem-int-heading
    detect:update-var id "int" "speed" mem-int-speed
    detect:update-var id "int" "age" mem-int-age
    detect:update-var id "int" "vision" mem-int-vision
    
    ;External
    detect:update-var id "ext" "birdsNear" mem-ext-bn
    detect:update-var id "ext" "distNear" mem-ext-dn
    detect:update-var id "ext" "flockHead" mem-ext-fh
    detect:update-var id "ext" "flockSpeed" mem-ext-fs
    detect:update-var id "ext" "temperature" mem-ext-temperature
    detect:update-var id "ext" "distMap" mem-ext-distMap
    
    ; Run the regression analysis and then stop R.
    detect:run-regress id
    detect:stopR id
    
    ; Retrieve the CUSUM scores for each internal variable. Each value in the CUSUM list contains the maximum CUSUM value for each external variable
    ; being monitored.
    let sCoeff_Cusum detect:report-Cusum id "1"
    let hCoeff_Cusum detect:report-Cusum id "0"
    let agCoeff_Cusum detect:report-Cusum id "2"
    let vsCoeff_Cusum detect:report-Cusum id "3"
    
    ; Next check to see if any of the CUSUMS are above the CUSUM threshold value? If so, set the local belief on feedback (localV) to 1.
    ; A change is remembered for a length of changeMemLen, and this "decays" with each subsequent Regression/CUSUM analysis.
    let trigger threshold-low
    set signChange 0
    ifelse (item 0 sCoeff_Cusum >= trigger or item 1 sCoeff_Cusum >= trigger or item 2 sCoeff_Cusum >= trigger or item 3 sCoeff_Cusum >= trigger or 
             item 4 sCoeff_Cusum >= trigger or item 5 sCoeff_Cusum >= trigger) 
    [
      set signChange 1
      set localV 1
      set changeDecay changeMemLen
    ]
    [
      ifelse (item 0 hCoeff_Cusum >= trigger or item 1 hCoeff_Cusum >= trigger or item 2 hCoeff_Cusum >= trigger or item 3 hCoeff_Cusum >= trigger or 
        item 4 hCoeff_Cusum >= trigger or item 5 hCoeff_Cusum >= trigger) 
      [
        set signChange 1
        set localV 1  ;;Set own Emergence belief to be 1 ie. yes
        set changeDecay changeMemLen
      ]
      [
        ifelse (item 0 agCoeff_Cusum >= trigger or item 1 agCoeff_Cusum >= trigger or item 2 agCoeff_Cusum >= trigger or item 3 agCoeff_Cusum >= trigger or 
          item 4 agCoeff_Cusum >= trigger or item 5 agCoeff_Cusum >= trigger) 
        [
          set signChange 1
          set localV 1  ;;Set own Emergence belief to be 1 ie. yes
          set changeDecay changeMemLen
        ]
        [
          ifelse (item 0 vsCoeff_Cusum >= trigger or item 1 vsCoeff_Cusum >= trigger or item 2 vsCoeff_Cusum >= trigger or item 3 vsCoeff_Cusum >= trigger or 
             item 4 vsCoeff_Cusum >= trigger or item 5 vsCoeff_Cusum >= trigger) 
          [
            set signChange 1
            set localV 1  ;;Set own Emergence belief to be 1 ie. yes
            set changeDecay changeMemLen
          ]
          
          [
            ; If no change detected this time and there was in the recent past, decay the memory.
            set signChange 0
            if changeDecay > 0 [
              set changeDecay changeDecay - 1
            ]
          ]
          
        ]
      ]
    ]
    ; If no memory of change, set local belief (local V) to 0.
    if changeDecay = 0 [
      set localV 0
    ]

    ; Here the maximum CUSUM for each relationship for the agent is split into its own variable. This allows the number of agents who detect feedback
    ; for each relationship to be counted and recorded in the logs.
    set coeff-bn-myS_Cusum item 0 sCoeff_Cusum
    set coeff-dn-myS_Cusum item 1 sCoeff_Cusum
    set coeff-fh-myS_Cusum item 2 sCoeff_Cusum
    set coeff-fs-myS_Cusum item 3 sCoeff_Cusum
    set coeff-tm-myS_Cusum item 4 sCoeff_Cusum
    set coeff-dm-myS_Cusum item 5 sCoeff_Cusum
    
    set coeff-bn-myH_Cusum item 0 hCoeff_Cusum
    set coeff-dn-myH_Cusum item 1 hCoeff_Cusum
    set coeff-fh-myH_Cusum item 2 hCoeff_Cusum
    set coeff-fs-myH_Cusum item 3 hCoeff_Cusum
    set coeff-tm-myH_Cusum item 4 hCoeff_Cusum
    set coeff-dm-myH_Cusum item 5 hCoeff_Cusum
    
    set coeff-bn-myAge_Cusum item 0 agCoeff_Cusum
    set coeff-dn-myAge_Cusum item 1 agCoeff_Cusum
    set coeff-fh-myAge_Cusum item 2 agCoeff_Cusum
    set coeff-fs-myAge_Cusum item 3 agCoeff_Cusum
    set coeff-tm-myAge_Cusum item 4 agCoeff_Cusum
    set coeff-dm-myAge_Cusum item 5 agCoeff_Cusum
    
    set coeff-bn-myVision_Cusum item 0 vsCoeff_Cusum
    set coeff-dn-myVision_Cusum item 1 vsCoeff_Cusum
    set coeff-fh-myVision_Cusum item 2 vsCoeff_Cusum
    set coeff-fs-myVision_Cusum item 3 vsCoeff_Cusum
    set coeff-tm-myVision_Cusum item 4 vsCoeff_Cusum
    set coeff-dm-myVision_Cusum item 5 vsCoeff_Cusum
    
    slide-Window
    updateEmergenceBelief
  ]
end

; This function updates the agents emergence belief, i.e. whether it thinks there is currently an emergent event.
; This is controlled using two boolean variables. If Vp is above the aggThreshold and the agent thought there was no emergence (emergeRelent?)
; emergence belief is set to true. This belief remains until the Vp fades away, dropping below 0.1.
to updateEmergenceBelief
  ifelse Vp >= aggThreshold [
    if emergeRelent? = true [ ;; have we come from a case where VP dropped below 0.3 from the last time it was above 0.5
                              ;; We switch our belief in emergence
      set emergeBelief? true
      set emergeRelent? false
    ]
  ][
  if Vp <= 0.1 [
    set emergeBelief? false
    set emergeRelent?  true ;; Next time VP goes over 0.5, change emergeBelief?
  ]
  ]
end

; This function updates the temperature randomly making a small change
to update-temp
  ask one-of turtles [
    detect:startR id
    let changeTemp detect:getRandom id
    detect:stopR id
    set global-temperature global-temperature + changeTemp
  ]
end

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;                                                                       Setup Procedures
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

; This function setups the inital value for the global parameters.
to setup-globals  
  print "Setting globals"
  ;These are passed in by the simulation starter
  set run-name ""
  set minNeighSize 0
  set maxNeighSize 0
  set changeMemLen 0
  set aggThreshold 0
  set lasso-Win 500
  
  ; Set scaler to 5%
  set scaler 0.05
  
  set objM 0
  set changPer 0
  set emPercent 0
  
  set churn 0
  set global-temperature 72
end

to setup
  ;; (for this model to work with NetLogo's new plotting features,
  ;; __clear-all-and-reset-ticks should be replaced with clear-all at
  ;; the beginning of your setup procedure and reset-ticks at the end
  ;; of the procedure.)
  __clear-all-and-reset-ticks
  setup-globals 
end

; This is the second part of the setup process managed by the Java run library. 
; It creates the agents and initialses DETect.
to setup-part2
    crt car-number
    [ set color yellow - 2 + random 7  ;; random shades look nice
      set size 1.5  ;; easier to see
      setxy random-xcor random-ycor
      set speed 1.0
    ]
  print count turtles
  set threshold-low 4.0
  set random-acceleration 0.1
  set allow-move-backward? false
  setup-birds
  detect:initialise
  ask turtles  [
    setup-stats
  ]
  detect:itest
  
end

; This function is used to set up the DETect stats parameters owned by each agent. This basically initialses all the empty arrays that will be used, outlines the number of internal and 
; external variables and adds them to the DETect object.
to setup-stats
  ; Internal variables
  set myHeading 0
  
  set speed-mem[]
  set heading-mem[]
  set age-mem[]
  set vision-mem[]
  
  set mem-int-speed[]
  set mem-int-heading[]
  set mem-int-age[]
  set mem-int-vision[]
  
  ;;External Variables
  set flockCount 0       
  set distNear  0  
  set flockHead 0
  set flockSpeed 0
  
  set birds-near-mem[]
  set dist-near-bird-mem[]
  set flock-head-mem[]
  set flock-speed-mem[]
  set temperature-mem[]
  set distMap-mem[]
  
  set mem-ext-bn[]
  set mem-ext-dn[]
  set mem-ext-fh[]
  set mem-ext-fs[]
  set mem-ext-temperature[]
  set mem-ext-distMap[]
  
  
  detect:new-data "4" "6" id    ; 4 internal variables, 6 external variables
  
  detect:add-var id "ext" "birdsNear"
  detect:add-var id "ext" "distNear"
  detect:add-var id "ext" "flockHead"
  detect:add-var id "ext" "flockSpeed"
  detect:add-var id "ext" "temperature"
  detect:add-var id "ext" "distMap"

  ;Add internal variables
  detect:add-var id "int" "myHead"
  detect:add-var id "int" "speed"
  detect:add-var id "int" "age"
  detect:add-var id "int" "vision"
  
  set relsChosen? false
end

; This function asks the agents/birds to initialse personal parameters.
to setup-birds
  ask turtles[
    set id (word "turtle-" who)
    set varsAdded? false
    
    set localV 0
    set Vp 0
    set changeDecay 0
    
    set emergeBelief? false
    set emergeRelent? true
    
    set lastFlockSize 1
    set myVision 5
    set age 5
    set age age + (random-float 5 - 2.5)
  ]
  set vision 5
end

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;                                                                 Log writing functions
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
to write-to-file
  file-print (word ticks "," objM "," changPer "," emPercent "," churn)
end 

to cal-globals
  set objM ((count patches with [count turtles in-radius 2 = 0]) / count patches) * 100
  set changPer ((count turtles with [localV = 1]) / count turtles) * 100
  set emPercent ((count turtles with [emergeBelief?]) / count turtles) * 100 
  set churn ((count turtles with [count flockmates = lastFlockSize]) / count turtles) * 100 
end

; This function is used to print a comprehensive logfile every 50 timesteps during the simulation run.
to write-to-file-comprehensive
  let file-name (word run-name "-" ticks ".csv")
  file-open file-name
  file-print "** Global Statistics **"
  file-print (word "Time,ChangePercent,EmergePercent,Objective,Align")
  file-print (word ticks ","  changPer  ","  emPercent  ","  objM ","  max-align-turn)
    
  
  file-print "**Turles-With-Change**"
  let turtChange [who] of turtles with [localV = 1]
  file-print length turtChange
  foreach turtChange [
    let line (word ? "," [localV] of turtle ? "," [emergeBelief?] of turtle ? )
    file-print line 
  ]
  
  file-print "**4. Agents breaking threshold**"
  file-print "**coeff-bn-myS_cusum*"
  let turtListHere [who] of turtles with [coeff-bn-myS_cusum >=  threshold-low ];and coeff-bn-myS_cusum < threshold-mid-low]
  file-print length turtListHere
  foreach turtListHere [
    let line (word ? ","  [coeff-bn-myS_cusum] of turtle ?)
    file-print line
  ]
  file-print "**coeff-dn-myS_cusum*"
  set turtListHere [who] of turtles with [coeff-dn-myS_cusum >=  threshold-low ];and coeff-dn-myS_cusum < threshold-mid-low]
  file-print length turtListHere
  foreach turtListHere [
    let line (word ? ","  [coeff-dn-myS_cusum] of turtle ?)
    file-print line
  ]
  file-print "**coeff-fh-myS_cusum*"
  set turtListHere [who] of turtles with [coeff-fh-myS_cusum >=  threshold-low ];and coeff-fh-myS_cusum < threshold-mid-low]
  file-print length turtListHere
  foreach turtListHere [
    let line (word ? ","  [coeff-fh-myS_cusum] of turtle ?)
    file-print line
  ]
  
  file-print "**coeff-fs-myS_cusum*"
  set turtListHere [who] of turtles with [coeff-fs-myS_cusum >=  threshold-low ];and coeff-fs-myS_cusum < threshold-mid-low]
  file-print length turtListHere
  foreach turtListHere [
    let line (word ? ","  [coeff-fs-myS_cusum] of turtle ?)
    file-print line
  ]
  
  file-print "**coeff-tm-myS_cusum*"
  set turtListHere [who] of turtles with [coeff-tm-myS_cusum >=  threshold-low ];and coeff-fs-myS_cusum < threshold-mid-low]
  file-print length turtListHere
  foreach turtListHere [
    let line (word ? ","  [coeff-tm-myS_cusum] of turtle ?)
    file-print line
  ]
  
  file-print "**coeff-dm-myS_cusum*"
  set turtListHere [who] of turtles with [coeff-dm-myS_cusum >=  threshold-low ];and coeff-fs-myS_cusum < threshold-mid-low]
  file-print length turtListHere
  foreach turtListHere [
    let line (word ? ","  [coeff-dm-myS_cusum] of turtle ?)
    file-print line
  ]
  
  file-print "**coeff-bn-myH_cusum*"
  set turtListHere [who] of turtles with [coeff-bn-myH_cusum >=  threshold-low ];and coeff-bn-myH_cusum < threshold-mid-low]
  file-print length turtListHere
  foreach turtListHere [
    let line (word ? ","  [coeff-bn-myH_cusum] of turtle ?)
    file-print line
  ]
  
  file-print "**coeff-dn-myH_cusum*"
  set turtListHere [who] of turtles with [coeff-dn-myH_cusum >=  threshold-low ];and coeff-dn-myH_cusum < threshold-mid-low]
  file-print length turtListHere
  foreach turtListHere [
    let line (word ? ","  [coeff-dn-myH_cusum] of turtle ?)
    file-print line
  ]
  
  file-print "**coeff-fh-myH_cusum*"
  set turtListHere [who] of turtles with [coeff-fh-myH_cusum >=  threshold-low ];and coeff-fh-myH_cusum < threshold-mid-low]
  file-print length turtListHere
  foreach turtListHere [
    let line (word ? ","  [coeff-fh-myH_cusum] of turtle ?)
    file-print line
  ]
  
  file-print "**coeff-fs-myH_cusum*"
  set turtListHere [who] of turtles with [coeff-fs-myH_cusum >=  threshold-low ];and coeff-fs-myH_cusum < threshold-mid-low]
  file-print length turtListHere
  foreach turtListHere [
    let line (word ? ","  [coeff-fs-myH_cusum] of turtle ?)
    file-print line
  ]
  
  file-print "**coeff-tm-myH_cusum*"
  set turtListHere [who] of turtles with [coeff-tm-myH_cusum >=  threshold-low ];and coeff-fs-myS_cusum < threshold-mid-low]
  file-print length turtListHere
  foreach turtListHere [
    let line (word ? ","  [coeff-tm-myH_cusum] of turtle ?)
    file-print line
  ]
  
  file-print "**coeff-dm-myH_cusum*"
  set turtListHere [who] of turtles with [coeff-dm-myH_cusum >=  threshold-low ];and coeff-fs-myS_cusum < threshold-mid-low]
  file-print length turtListHere
  foreach turtListHere [
    let line (word ? ","  [coeff-dm-myH_cusum] of turtle ?)
    file-print line
  ]
  
  file-print "**coeff-bn-myAge_cusum*"
  set turtListHere [who] of turtles with [coeff-bn-myAge_cusum >=  threshold-low ];and coeff-bn-myH_cusum < threshold-mid-low]
  file-print length turtListHere
  foreach turtListHere [
    let line (word ? ","  [coeff-bn-myAge_cusum] of turtle ?)
    file-print line
  ]
  
  file-print "**coeff-dn-myH_cusum*"
  set turtListHere [who] of turtles with [coeff-dn-myH_cusum >=  threshold-low ];and coeff-dn-myH_cusum < threshold-mid-low]
  file-print length turtListHere
  foreach turtListHere [
    let line (word ? ","  [coeff-dn-myH_cusum] of turtle ?)
    file-print line
  ]
  
  file-print "**coeff-fh-myH_cusum*"
  set turtListHere [who] of turtles with [coeff-fh-myH_cusum >=  threshold-low ];and coeff-fh-myH_cusum < threshold-mid-low]
  file-print length turtListHere
  foreach turtListHere [
    let line (word ? ","  [coeff-fh-myH_cusum] of turtle ?)
    file-print line
  ]
  
  file-print "**coeff-fs-myH_cusum*"
  set turtListHere [who] of turtles with [coeff-fs-myH_cusum >=  threshold-low ];and coeff-fs-myH_cusum < threshold-mid-low]
  file-print length turtListHere
  foreach turtListHere [
    let line (word ? ","  [coeff-fs-myH_cusum] of turtle ?)
    file-print line
  ]
  
  file-print "**coeff-tm-myH_cusum*"
  set turtListHere [who] of turtles with [coeff-tm-myH_cusum >=  threshold-low ];and coeff-fs-myS_cusum < threshold-mid-low]
  file-print length turtListHere
  foreach turtListHere [
    let line (word ? ","  [coeff-tm-myH_cusum] of turtle ?)
    file-print line
  ]
  
  file-print "**coeff-dm-myH_cusum*"
  set turtListHere [who] of turtles with [coeff-dm-myH_cusum >=  threshold-low ];and coeff-fs-myS_cusum < threshold-mid-low]
  file-print length turtListHere
  foreach turtListHere [
    let line (word ? ","  [coeff-dm-myH_cusum] of turtle ?)
    file-print line
  ]
  
  file-print "**coeff-bn-myAge_cusum*"
  set turtListHere [who] of turtles with [coeff-bn-myAge_cusum >=  threshold-low ];and coeff-bn-myAge_cusum < threshold-mid-low]
  file-print length turtListHere
  foreach turtListHere [
    let line (word ? ","  [coeff-bn-myAge_cusum] of turtle ?)
    file-print line
  ]
  
  file-print "**coeff-dn-myAge_cusum*"
  set turtListHere [who] of turtles with [coeff-dn-myAge_cusum >=  threshold-low ];and coeff-dn-myAge_cusum < threshold-mid-low]
  file-print length turtListHere
  foreach turtListHere [
    let line (word ? ","  [coeff-dn-myAge_cusum] of turtle ?)
    file-print line
  ]
  
  file-print "**coeff-fh-myAge_cusum*"
  set turtListHere [who] of turtles with [coeff-fh-myAge_cusum >=  threshold-low ];and coeff-fh-myAge_cusum < threshold-mid-low]
  file-print length turtListHere
  foreach turtListHere [
    let line (word ? ","  [coeff-fh-myAge_cusum] of turtle ?)
    file-print line
  ]
  
  file-print "**coeff-fs-myAge_cusum*"
  set turtListHere [who] of turtles with [coeff-fs-myAge_cusum >=  threshold-low ];and coeff-fs-myAge_cusum < threshold-mid-low]
  file-print length turtListHere
  foreach turtListHere [
    let line (word ? ","  [coeff-fs-myAge_cusum] of turtle ?)
    file-print line
  ]
  
  file-print "**coeff-tm-myAge_cusum*"
  set turtListHere [who] of turtles with [coeff-tm-myAge_cusum >=  threshold-low ];and coeff-fs-myS_cusum < threshold-mid-low]
  file-print length turtListHere
  foreach turtListHere [
    let line (word ? ","  [coeff-tm-myAge_cusum] of turtle ?)
    file-print line
  ]
  
  file-print "**coeff-dm-myAge_cusum*"
  set turtListHere [who] of turtles with [coeff-dm-myAge_cusum >=  threshold-low ];and coeff-fs-myS_cusum < threshold-mid-low]
  file-print length turtListHere
  foreach turtListHere [
    let line (word ? ","  [coeff-dm-myAge_cusum] of turtle ?)
    file-print line
  ]
  file-print "**coeff-bn-myVision_cusum*"
  set turtListHere [who] of turtles with [coeff-bn-myVision_cusum >=  threshold-low ];and coeff-bn-myVision_cusum < threshold-mid-low]
  file-print length turtListHere
  foreach turtListHere [
    let line (word ? ","  [coeff-bn-myVision_cusum] of turtle ?)
    file-print line
  ]
  
  file-print "**coeff-dn-myVision_cusum*"
  set turtListHere [who] of turtles with [coeff-dn-myVision_cusum >=  threshold-low ];and coeff-dn-myVision_cusum < threshold-mid-low]
  file-print length turtListHere
  foreach turtListHere [
    let line (word ? ","  [coeff-dn-myVision_cusum] of turtle ?)
    file-print line
  ]
  
  file-print "**coeff-fh-myVision_cusum*"
  set turtListHere [who] of turtles with [coeff-fh-myVision_cusum >=  threshold-low ];and coeff-fh-myVision_cusum < threshold-mid-low]
  file-print length turtListHere
  foreach turtListHere [
    let line (word ? ","  [coeff-fh-myVision_cusum] of turtle ?)
    file-print line
  ]
  
  file-print "**coeff-fs-myVision_cusum*"
  set turtListHere [who] of turtles with [coeff-fs-myVision_cusum >=  threshold-low ];and coeff-fs-myVision_cusum < threshold-mid-low]
  file-print length turtListHere
  foreach turtListHere [
    let line (word ? ","  [coeff-fs-myVision_cusum] of turtle ?)
    file-print line
  ]
  
  file-print "**coeff-tm-myVision_cusum*"
  set turtListHere [who] of turtles with [coeff-tm-myVision_cusum >=  threshold-low ];and coeff-fs-myS_cusum < threshold-mid-low]
  file-print length turtListHere
  foreach turtListHere [
    let line (word ? ","  [coeff-tm-myVision_cusum] of turtle ?)
    file-print line
  ]
  
  file-print "**coeff-dm-myVision_cusum*"
  set turtListHere [who] of turtles with [coeff-dm-myVision_cusum >=  threshold-low ];and coeff-fs-myS_cusum < threshold-mid-low]
  file-print length turtListHere
  foreach turtListHere [
    let line (word ? ","  [coeff-dm-myVision_cusum] of turtle ?)
    file-print line
  ]
  
  
  file-print "**coeff-Rsq-myS_cusum*"
  ;set turtListHere [who] of turtles with [coeff-Rsq-myS_cusum >=  threshold-low ];and coeff-Rsq-myS_cusum < threshold-mid-low]
  ;file-print length turtListHere
  ;foreach turtListHere [
  ; let line (word ? ","  [coeff-Rsq-myS_cusum] of turtle ?)
  ; file-print line
  ;]
  
  ;file-print "**coeff-Rsq-myH_cusum*"
  ;set turtListHere [who] of turtles with [coeff-Rsq-myH_cusum >=  threshold-low ];and coeff-Rsq-myH_cusum < threshold-mid-low]
  ;file-print length turtListHere
  ;foreach turtListHere [
  ; let line (word ? ","  [coeff-Rsq-myH_cusum] of turtle ?)
  ; file-print line
  ;]
  
  file-print "**Turles-With-Emerge Belief**"
  let turtEmerge [who] of turtles with [emergeBelief?]
  file-print length turtEmerge
  foreach turtEmerge [
    let line (word ? "," [localV] of turtle ? "," [emergeBelief?] of turtle ? )
    file-print line 
  ]
  
  file-close
end

to setParams
  set car-number 150
  set minNeighSize 8
  set maxNeighSize 30
  set changeMemLen 10
  set aggThreshold 0.25
  set run-name "RunnTraffic"
end


; *** NetLogo 4.1beta3 Model Copyright Notice ***
;
; This model was created as part of the project: CONNECTED MATHEMATICS:
; MAKING SENSE OF COMPLEX PHENOMENA THROUGH BUILDING OBJECT-BASED PARALLEL
; MODELS (OBPML).  The project gratefully acknowledges the support of the
; National Science Foundation (Applications of Advanced Technologies
; Program) -- grant numbers RED #9552950 and REC #9632612.
;
; Copyright 1998 by Uri Wilensky.  All rights reserved.
;
; Permission to use, modify or redistribute this model is hereby granted,
; provided that both of the following requirements are followed:
; a) this copyright notice is included.
; b) this model will not be redistributed for profit without permission
;    from Uri Wilensky.
; Contact Uri Wilensky for appropriate licenses for redistribution for
; profit.
;
; This model was converted to NetLogo as part of the projects:
; PARTICIPATORY SIMULATIONS: NETWORK-BASED DESIGN FOR SYSTEMS LEARNING
; IN CLASSROOMS and/or INTEGRATED SIMULATION AND MODELING ENVIRONMENT.
; The project gratefully acknowledges the support of the
; National Science Foundation (REPP & ROLE programs) --
; grant numbers REC #9814682 and REC-0126227.
; Converted from StarLogoT to NetLogo, 2002.
;
; To refer to this model in academic publications, please use:
; Wilensky, U. (1998).  NetLogo Flocking model.
; http://ccl.northwestern.edu/netlogo/models/Flocking.
; Center for Connected Learning and Computer-Based Modeling,
; Northwestern University, Evanston, IL.
;
; In other publications, please use:
; Copyright 1998 Uri Wilensky.  All rights reserved.
; See http://ccl.northwestern.edu/netlogo/models/Flocking
; for terms of use.
;
; *** End of NetLogo 4.1beta3 Model Copyright Notice ***
@#$#@#$#@
GRAPHICS-WINDOW
250
10
617
398
25
25
7.0
1
10
1
1
1
0
1
1
1
-25
25
-25
25
1
1
1
ticks
30.0

BUTTON
39
93
116
126
NIL
setup
NIL
1
T
OBSERVER
NIL
NIL
NIL
NIL
1

BUTTON
122
93
203
126
NIL
go
T
1
T
OBSERVER
NIL
NIL
NIL
NIL
1

SLIDER
9
51
232
84
population
population
1.0
1000.0
300
1.0
1
NIL
HORIZONTAL

SLIDER
4
217
237
250
max-align-turn
max-align-turn
0.0
20.0
0
0.25
1
degrees
HORIZONTAL

SLIDER
4
251
237
284
max-cohere-turn
max-cohere-turn
0.0
20.0
3
0.25
1
degrees
HORIZONTAL

SLIDER
4
285
237
318
max-separate-turn
max-separate-turn
0.0
20.0
1.5
0.25
1
degrees
HORIZONTAL

SLIDER
9
135
232
168
vision
vision
0.0
10.0
5
0.5
1
patches
HORIZONTAL

SLIDER
9
169
232
202
minimum-separation
minimum-separation
0.0
5.0
1
0.25
1
patches
HORIZONTAL

SLIDER
15
395
230
428
random-acceleration
random-acceleration
0
1
0.1
0.01
1
NIL
HORIZONTAL

SWITCH
15
435
230
468
allow-move-backward?
allow-move-backward?
1
1
-1000

TEXTBOX
70
365
220
383
velocity controls
12
0.0
1

@#$#@#$#@
## WHAT IS IT?

This model is an attempt to mimic the flocking of birds.  (The resulting motion also resembles schools of fish.)  The flocks that appear in this model are not created or led in any way by special leader birds.  Rather, each bird is following exactly the same set of rules, from which flocks emerge.

The birds follow three rules: "alignment", "separation", and "cohesion".  "Alignment" means that a bird tends to turn so that it is moving in the same direction that nearby birds are moving.  "Separation" means that a bird will turn to avoid another bird which gets too close.  "Cohesion" means that a bird will move towards other nearby birds (unless another bird is too close).  When two birds are too close, the "separation" rule overrides the other two, which are deactivated until the minimum separation is achieved.

The three rules affect only the bird's heading.  Each bird always moves forward at the same constant speed.

## HOW TO USE IT

First, determine the number of birds you want in the simulation and set the POPULATION slider to that value.  Press SETUP to create the birds, and press GO to have them start flying around.

The default settings for the sliders will produce reasonably good flocking behavior.  However, you can play with them to get variations:

Three TURN-ANGLE sliders control the maximum angle a bird can turn as a result of each rule.

VISION is the distance that each bird can see 360 degrees around it.

## THINGS TO NOTICE

Central to the model is the observation that flocks form without a leader.

There are no random numbers used in this model, except to position the birds initially.  The fluid, lifelike behavior of the birds is produced entirely by deterministic rules.

Also, notice that each flock is dynamic.  A flock, once together, is not guaranteed to keep all of its members.  Why do you think this is?

After running the model for a while, all of the birds have approximately the same heading.  Why?

Sometimes a bird breaks away from its flock.  How does this happen?  You may need to slow down the model or run it step by step in order to observe this phenomenon.

## THINGS TO TRY

Play with the sliders to see if you can get tighter flocks, looser flocks, fewer flocks, more flocks, more or less splitting and joining of flocks, more or less rearranging of birds within flocks, etc.

You can turn off a rule entirely by setting that rule's angle slider to zero.  Is one rule by itself enough to produce at least some flocking?  What about two rules?  What's missing from the resulting behavior when you leave out each rule?

Will running the model for a long time produce a static flock?  Or will the birds never settle down to an unchanging formation?  Remember, there are no random numbers used in this model.

## EXTENDING THE MODEL

Currently the birds can "see" all around them.  What happens if birds can only see in front of them?  The IN-CONE primitive can be used for this.

Is there some way to get V-shaped flocks, like migrating geese?

What happens if you put walls around the edges of the world that the birds can't fly into?

Can you get the birds to fly around obstacles in the middle of the world?

What would happen if you gave the birds different velocities?  For example, you could make birds that are not near other birds fly faster to catch up to the flock.  Or, you could simulate the diminished air resistance that birds experience when flying together by making them fly faster when in a group.

Are there other interesting ways you can make the birds different from each other?  There could be random variation in the population, or you could have distinct "species" of bird.

## NETLOGO FEATURES

Notice the need for the SUBTRACT-HEADINGS primitive and special procedure for averaging groups of headings.  Just subtracting the numbers, or averaging the numbers, doesn't give you the results you'd expect, because of the discontinuity where headings wrap back to 0 once they reach 360.

## CREDITS AND REFERENCES

This model is inspired by the Boids simulation invented by Craig Reynolds.  The algorithm we use here is roughly similar to the original Boids algorithm, but it is not the same.  The exact details of the algorithm tend not to matter very much -- as long as you have alignment, separation, and cohesion, you will usually get flocking behavior resembling that produced by Reynolds' original model.  Information on Boids is available at http://www.red3d.com/cwr/boids/.

To refer to this model in academic publications, please use:  Wilensky, U. (1998).  NetLogo Flocking model.  http://ccl.northwestern.edu/netlogo/models/Flocking.  Center for Connected Learning and Computer-Based Modeling, Northwestern University, Evanston, IL.

In other publications, please use:  Copyright 1998 Uri Wilensky.  All rights reserved.  See http://ccl.northwestern.edu/netlogo/models/Flocking for terms of use.
@#$#@#$#@
default
true
0
Polygon -7500403 true true 150 5 40 250 150 205 260 250

airplane
true
0
Polygon -7500403 true true 150 0 135 15 120 60 120 105 15 165 15 195 120 180 135 240 105 270 120 285 150 270 180 285 210 270 165 240 180 180 285 195 285 165 180 105 180 60 165 15

arrow
true
0
Polygon -7500403 true true 150 0 0 150 105 150 105 293 195 293 195 150 300 150

box
false
0
Polygon -7500403 true true 150 285 285 225 285 75 150 135
Polygon -7500403 true true 150 135 15 75 150 15 285 75
Polygon -7500403 true true 15 75 15 225 150 285 150 135
Line -16777216 false 150 285 150 135
Line -16777216 false 150 135 15 75
Line -16777216 false 150 135 285 75

bug
true
0
Circle -7500403 true true 96 182 108
Circle -7500403 true true 110 127 80
Circle -7500403 true true 110 75 80
Line -7500403 true 150 100 80 30
Line -7500403 true 150 100 220 30

butterfly
true
0
Polygon -7500403 true true 150 165 209 199 225 225 225 255 195 270 165 255 150 240
Polygon -7500403 true true 150 165 89 198 75 225 75 255 105 270 135 255 150 240
Polygon -7500403 true true 139 148 100 105 55 90 25 90 10 105 10 135 25 180 40 195 85 194 139 163
Polygon -7500403 true true 162 150 200 105 245 90 275 90 290 105 290 135 275 180 260 195 215 195 162 165
Polygon -16777216 true false 150 255 135 225 120 150 135 120 150 105 165 120 180 150 165 225
Circle -16777216 true false 135 90 30
Line -16777216 false 150 105 195 60
Line -16777216 false 150 105 105 60

car
false
0
Polygon -7500403 true true 300 180 279 164 261 144 240 135 226 132 213 106 203 84 185 63 159 50 135 50 75 60 0 150 0 165 0 225 300 225 300 180
Circle -16777216 true false 180 180 90
Circle -16777216 true false 30 180 90
Polygon -16777216 true false 162 80 132 78 134 135 209 135 194 105 189 96 180 89
Circle -7500403 true true 47 195 58
Circle -7500403 true true 195 195 58

circle
false
0
Circle -7500403 true true 0 0 300

circle 2
false
0
Circle -7500403 true true 0 0 300
Circle -16777216 true false 30 30 240

cow
false
0
Polygon -7500403 true true 200 193 197 249 179 249 177 196 166 187 140 189 93 191 78 179 72 211 49 209 48 181 37 149 25 120 25 89 45 72 103 84 179 75 198 76 252 64 272 81 293 103 285 121 255 121 242 118 224 167
Polygon -7500403 true true 73 210 86 251 62 249 48 208
Polygon -7500403 true true 25 114 16 195 9 204 23 213 25 200 39 123

cylinder
false
0
Circle -7500403 true true 0 0 300

dot
false
0
Circle -7500403 true true 90 90 120

face happy
false
0
Circle -7500403 true true 8 8 285
Circle -16777216 true false 60 75 60
Circle -16777216 true false 180 75 60
Polygon -16777216 true false 150 255 90 239 62 213 47 191 67 179 90 203 109 218 150 225 192 218 210 203 227 181 251 194 236 217 212 240

face neutral
false
0
Circle -7500403 true true 8 7 285
Circle -16777216 true false 60 75 60
Circle -16777216 true false 180 75 60
Rectangle -16777216 true false 60 195 240 225

face sad
false
0
Circle -7500403 true true 8 8 285
Circle -16777216 true false 60 75 60
Circle -16777216 true false 180 75 60
Polygon -16777216 true false 150 168 90 184 62 210 47 232 67 244 90 220 109 205 150 198 192 205 210 220 227 242 251 229 236 206 212 183

fish
false
0
Polygon -1 true false 44 131 21 87 15 86 0 120 15 150 0 180 13 214 20 212 45 166
Polygon -1 true false 135 195 119 235 95 218 76 210 46 204 60 165
Polygon -1 true false 75 45 83 77 71 103 86 114 166 78 135 60
Polygon -7500403 true true 30 136 151 77 226 81 280 119 292 146 292 160 287 170 270 195 195 210 151 212 30 166
Circle -16777216 true false 215 106 30

flag
false
0
Rectangle -7500403 true true 60 15 75 300
Polygon -7500403 true true 90 150 270 90 90 30
Line -7500403 true 75 135 90 135
Line -7500403 true 75 45 90 45

flower
false
0
Polygon -10899396 true false 135 120 165 165 180 210 180 240 150 300 165 300 195 240 195 195 165 135
Circle -7500403 true true 85 132 38
Circle -7500403 true true 130 147 38
Circle -7500403 true true 192 85 38
Circle -7500403 true true 85 40 38
Circle -7500403 true true 177 40 38
Circle -7500403 true true 177 132 38
Circle -7500403 true true 70 85 38
Circle -7500403 true true 130 25 38
Circle -7500403 true true 96 51 108
Circle -16777216 true false 113 68 74
Polygon -10899396 true false 189 233 219 188 249 173 279 188 234 218
Polygon -10899396 true false 180 255 150 210 105 210 75 240 135 240

house
false
0
Rectangle -7500403 true true 45 120 255 285
Rectangle -16777216 true false 120 210 180 285
Polygon -7500403 true true 15 120 150 15 285 120
Line -16777216 false 30 120 270 120

leaf
false
0
Polygon -7500403 true true 150 210 135 195 120 210 60 210 30 195 60 180 60 165 15 135 30 120 15 105 40 104 45 90 60 90 90 105 105 120 120 120 105 60 120 60 135 30 150 15 165 30 180 60 195 60 180 120 195 120 210 105 240 90 255 90 263 104 285 105 270 120 285 135 240 165 240 180 270 195 240 210 180 210 165 195
Polygon -7500403 true true 135 195 135 240 120 255 105 255 105 285 135 285 165 240 165 195

line
true
0
Line -7500403 true 150 0 150 300

line half
true
0
Line -7500403 true 150 0 150 150

pentagon
false
0
Polygon -7500403 true true 150 15 15 120 60 285 240 285 285 120

person
false
0
Circle -7500403 true true 110 5 80
Polygon -7500403 true true 105 90 120 195 90 285 105 300 135 300 150 225 165 300 195 300 210 285 180 195 195 90
Rectangle -7500403 true true 127 79 172 94
Polygon -7500403 true true 195 90 240 150 225 180 165 105
Polygon -7500403 true true 105 90 60 150 75 180 135 105

plant
false
0
Rectangle -7500403 true true 135 90 165 300
Polygon -7500403 true true 135 255 90 210 45 195 75 255 135 285
Polygon -7500403 true true 165 255 210 210 255 195 225 255 165 285
Polygon -7500403 true true 135 180 90 135 45 120 75 180 135 210
Polygon -7500403 true true 165 180 165 210 225 180 255 120 210 135
Polygon -7500403 true true 135 105 90 60 45 45 75 105 135 135
Polygon -7500403 true true 165 105 165 135 225 105 255 45 210 60
Polygon -7500403 true true 135 90 120 45 150 15 180 45 165 90

square
false
0
Rectangle -7500403 true true 30 30 270 270

square 2
false
0
Rectangle -7500403 true true 30 30 270 270
Rectangle -16777216 true false 60 60 240 240

star
false
0
Polygon -7500403 true true 151 1 185 108 298 108 207 175 242 282 151 216 59 282 94 175 3 108 116 108

target
false
0
Circle -7500403 true true 0 0 300
Circle -16777216 true false 30 30 240
Circle -7500403 true true 60 60 180
Circle -16777216 true false 90 90 120
Circle -7500403 true true 120 120 60

tree
false
0
Circle -7500403 true true 118 3 94
Rectangle -6459832 true false 120 195 180 300
Circle -7500403 true true 65 21 108
Circle -7500403 true true 116 41 127
Circle -7500403 true true 45 90 120
Circle -7500403 true true 104 74 152

triangle
false
0
Polygon -7500403 true true 150 30 15 255 285 255

triangle 2
false
0
Polygon -7500403 true true 150 30 15 255 285 255
Polygon -16777216 true false 151 99 225 223 75 224

truck
false
0
Rectangle -7500403 true true 4 45 195 187
Polygon -7500403 true true 296 193 296 150 259 134 244 104 208 104 207 194
Rectangle -1 true false 195 60 195 105
Polygon -16777216 true false 238 112 252 141 219 141 218 112
Circle -16777216 true false 234 174 42
Rectangle -7500403 true true 181 185 214 194
Circle -16777216 true false 144 174 42
Circle -16777216 true false 24 174 42
Circle -7500403 false true 24 174 42
Circle -7500403 false true 144 174 42
Circle -7500403 false true 234 174 42

turtle
true
0
Polygon -10899396 true false 215 204 240 233 246 254 228 266 215 252 193 210
Polygon -10899396 true false 195 90 225 75 245 75 260 89 269 108 261 124 240 105 225 105 210 105
Polygon -10899396 true false 105 90 75 75 55 75 40 89 31 108 39 124 60 105 75 105 90 105
Polygon -10899396 true false 132 85 134 64 107 51 108 17 150 2 192 18 192 52 169 65 172 87
Polygon -10899396 true false 85 204 60 233 54 254 72 266 85 252 107 210
Polygon -7500403 true true 119 75 179 75 209 101 224 135 220 225 175 261 128 261 81 224 74 135 88 99

wheel
false
0
Circle -7500403 true true 3 3 294
Circle -16777216 true false 30 30 240
Line -7500403 true 150 285 150 15
Line -7500403 true 15 150 285 150
Circle -7500403 true true 120 120 60
Line -7500403 true 216 40 79 269
Line -7500403 true 40 84 269 221
Line -7500403 true 40 216 269 79
Line -7500403 true 84 40 221 269

x
false
0
Polygon -7500403 true true 270 75 225 30 30 225 75 270
Polygon -7500403 true true 30 75 75 30 270 225 225 270

@#$#@#$#@
NetLogo 5.1.0
@#$#@#$#@
set population 200
setup
repeat 200 [ go ]
@#$#@#$#@
@#$#@#$#@
@#$#@#$#@
@#$#@#$#@
default
0.0
-0.2 0 0.0 1.0
0.0 1 1.0 0.0
0.2 0 0.0 1.0
link direction
true
0
Line -7500403 true 150 150 90 180
Line -7500403 true 150 150 210 180

@#$#@#$#@
1
@#$#@#$#@
