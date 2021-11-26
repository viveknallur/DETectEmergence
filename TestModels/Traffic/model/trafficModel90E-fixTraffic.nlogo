extensions [ gis table graphhopper detect ]

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; Global Set up ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

; Declare the global variables forming the model
globals [
  projection countries-dataset acceleration deceleration speed-max speed-min junctions-with-lights 
  xmin xmax ymin ymax xscale yscale envelope cou carList speed-limit legal-streets all-ids roundabouts routeCount car-nums 
  file-objective file-specific file-cars file-cars_cusum file-cars_cusum-rsq
  
  global_flowRate
  global_avgSpeed
  global_congmetric
  global_conNeighMetric  
  
  globalCarsStopped
  
  osms-congested
  
  first-list                 ; List of common convergence destinations for the first period.
  second-list                ; List of common convergence destinations for the second period.
  
  globalCongestion?
  
  streetsJammedNames        ; Used to track names of the streets with congestion.
  streetsJammed             ; Use to store the number of streets currently with gridlock
  streetsJammedMemory       ; Use to track the recent history of streetsJammed to determine if it is remaining high and static indicating gridlock
  
  congestionTicks   ; This variable is used to count how long a high level of congestion remains static. Static congestion suggests deadlock in the traffic system and 
                    ; once this is detected a global adaptation occurs. See function update-GridLock
  
  car-vision        ; how far a car can see
  
  lasso-win         ; The size of the Sliding Observation window before lasso. When full it triggers the model selection.
  regWinL           ; The size of the Regression Sliding Observation Window. When full it triggers a regression analysis.
  smoothL           ; The size of the smoothing average window. When full the window is averaged and the average is added to the regression Sliding Observation window. Typically 5.

  run-name          ; The name of the simulation run. This is passed into Netlogo from the Java library that initiates the simulation.
  car-number        ; The number of agents in the model. TODO, change this name so it is generic
  minNeighSize      ; The minimum number of flockmates and agent must have before it can gossip.
  maxNeighSize      ; The maximum number of flockmates an agent will consider as potential gossiping partner
  changeMemLen      ; How long an agent will remember a change detected by a CUSUM. This is measured in subsequent Regression/CUSUM analysis runs.
  aggThreshold      ; What the gossiping average must be over before an agent will conclude an emergent event.
  threshold-low     ; What value the CUSUM must exceed before a change is detected. Typically set to 4. (h parameter in CUSUM (see thesis)
  scaler            ; What percent an agent will scale the gossiping average to their own local belief. Typically 5% of the difference.
  
  global-temperature ; A randomly variable simulation of temperature in the system.
  
]

; Declare the different types of agents that are used in the model
breed [postCodes postCode]
breed [junctions junction]
breed [joints joint]
breed [cars car]
breed [builders builder]
breed [lightMakers lightMaker]
breed [streets street] 
breed [lights light]
breed [manholes manhole]
directed-link-breed [ roads road ]

links-own [road-name one-way max-speed osm-id down-Stream high-way]
patches-own [status typeOf is-road? maxspeed intersection]
junctions-own [headings mylights phases current-phase street-names latitude longitude junction? lastSet lastSetO downStream]
builders-own [junc-count source target name osm-name downStream hi-way]
lightMakers-own [junc-count source target name osm-name]
streets-own [target source street-name osm-name highway forward-osm done? id downStream congested? congestedNeigh? congCount congNeighCount proc? depot?]
manholes-own [
  osm-name target source distFrom carCount carSpeeds flowRate avgSpeed used? neighborscore congMetric 
  memCarName 
  congestedNeigh?
  mypostCode
  avgSpMem
  flRtMem
  ]
lights-own [target source onPhase parent osm-name gridLock?]
postCodes-own [address carsList-low carsList-mid-low carsList-mid-high carsList-high mHListSome mhListLots mhListNeigh myDepot]
cars-own [
  id target  destinationx destinationy nextx nexty happy? destFound? futureOsm current-street lastRoad nextTurn nextStreet lost? lostx losty current-osm nextOsm  destCount osmList
  homeDepot
  depotList
  lastManhole
  arriveTime
  checkx
  checky
  manHoleMem
  manHoleTimeMem
  
  atJunction?
  canPass?
  nextTarget
  
  ;;Internal Variables
  speed
  myHeading ;heading
  age
  ; The smoothing averaging windows for each variable
  speed-mem
  heading-mem
  age-mem
  ; The sliding observation windows for each variable
  mem-int-speed
  mem-int-heading
  mem-int-age
  
  ;;External Variables
  cars-near        ; Cars in radius 1.5
  dist-near-car    
  cars-head
  cars-speed
  temperature
  distDest
  ; The smoothing averaging windows for each variable
  cars-near-mem
  dist-near-car-mem
  cars-head-mem
  cars-speed-mem
  temperature-mem
  distDest-mem
  ; The sliding observation windows for each variable
  mem-ext-cn
  mem-ext-dn
  mem-ext-ch
  mem-ext-cs
  mem-ext-temperature
  mem-ext-distDest
  
  ; These store the CUSUM values for each internal-external variable pair.
  ; First my speed against all external variables
  coeff-cn-myS
  coeff-dn-myS
  coeff-ch-myS
  coeff-cs-myS
  
  coeff-cn-myH
  coeff-dn-myH
  coeff-ch-myH
  coeff-cs-myH
  
  coeff-cn-myS_cusum
  coeff-dn-myS_cusum
  coeff-ch-myS_cusum
  coeff-cs-myS_cusum
  coeff-tm-myS_cusum
  coeff-dd-myS_cusum
  
  coeff-cn-myH_cusum
  coeff-dn-myH_cusum
  coeff-ch-myH_cusum
  coeff-cs-myH_cusum
  coeff-tm-myH_cusum
  coeff-dd-myH_cusum
  
  coeff-cn-myAge_cusum
  coeff-dn-myAge_cusum
  coeff-ch-myAge_cusum
  coeff-cs-myAge_cusum
  coeff-tm-myAge_cusum
  coeff-dd-myAge_cusum

  memoryint          ;; Memory of internal variables, an array
  memoryext          ;; Memory of external variables, an array
  memoryintshort     ;; Short term memory of internal variables
  memoryextshort     ;; Short term memory of external variables
  
  firstlist          ; The list that stores the common depots that cars should go to during 1st convergence periods
  secondlist         ; The list that stores the common depots that cars should go to during 2nd convergence periods
  
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
  
  flockmates        ; agent set of other cars in the cars area
  
  altMode?          ; used to make the car choose a different depot when there is gridlock
  nextDest          ; next destination depot of the car
  ]

;;GH EXtension commands
;  find-route
;  next-street-name
;  next-instruction
;  next-coords
;  current-street
;  current-osm
;  next-osm
;  osm-chec
;  connect-gh
;  import-data
;  car-list

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; Setup Procedures ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; Initialize the display by creating the map
;; Create num-cars of turtles if there are enough road patches for one turtle to
;; be created per road patch. Set up the plots.
to setup-movie
  user-message "First, save your new movie file (choose a name ending with .mov)"
  let path user-new-file
  movie-start path
  movie-grab-view
end

to setup
  ca
  set threshold-low 4.0
  setup-globals

end

to setup-part2
  print (word "Car number is " car-number)
  setup-map
  random-five-hundred
  make-manholes 
  setup-postal-areas
  
  setup-cars
  
  print count cars with [xcor = -300 and ycor = 300]
  
  set legal-streets streets with [distance target > 3 and distance source > 3 and count other streets in-radius 3 with [osm-name != [osm-name] of myself] = 0  and highway != "motorway" and highway != "motorway_link" and highway != "primary_linK"]
  setup-depot 
  setupCommonDestinations
  
  
  ask postcodes with [myDepot != nobody][
   ask myDepot [
    let counter 0
    let mywho [who] of myself
    let cars-here (car-number / 194)
    while [counter < cars-here] [
     ask one-of cars with [xcor = -300 and ycor = 300] [
      move-to myself 
      set homeDepot myself
      
      set depotList [who] of legal-streets with [depot? = true and who != mywho]
     ]
     set counter counter + 1 
    ] 
   ] 
  ]
  print (word "First: " first-list)
  print (word "Second: " second-list)
  
  ask cars [
    find-free-spot
    locate-car-first
  ]
  detect:initialise
  
  ask cars  [
   set happy? false
   setup-stats
  ]
  detect:itest
  
  reset-ticks
end

;;Set up Files

to setup-cars
  graphhopper:import-data "20"
  set carList[] 
  set carList graphhopper:car-list
  let num-cars 500
 foreach  car-Nums [ 
    let carName item ? carList
    create-cars 1 [
      set shape "car"
      set color yellow
      set size 1
      set destCount 0
      set id carName;?
      
      set xcor -300
      set ycor 300
      
      set destinationx nobody 
      set destinationy nobody 
      
      set lastRoad nobody
      set lost? false
      set happy? false
      set destFound? false
      set signChange -10
      
      set osmList[]
      set nextx nobody
      set nexty nobody
      
      set atJunction? false
      set canPass? false
      set nextTarget nobody
      set lastManhole nobody
      set manHoleMem []
      set manHoleTimeMem[]
      
      set homeDepot nobody
      set depotList[]
      
      
      set varsAdded? false
      
      set localV 0
      set Vp 0
      set changeDecay 0
      
      set emergeBelief? false
      set emergeRelent? true
      
      set altMode? false
      set relsChosen? false
      
      set age 5
      set age age + (random-float 10 - 4)
    ]
  ]
  graphhopper:connect-gh
end 

; This function selects the common destinations that the cars will be prompted to select from during convergence periods.
to setupCommonDestinations
  set first-list []
  let depotName2 [who] of one-of legal-streets with [depot?]
  set first-list lput depotName2 first-list 
  
  set depotName2 [who] of one-of legal-streets with [depot?]
  
  while [member? depotName2 first-list] [
    set depotName2 [who] of one-of legal-streets with [depot?]
  ]
  set first-list lput depotName2 first-list
  
  set second-list []
  set depotName2 [who] of one-of legal-streets with [depot?]
  set second-list lput depotName2 second-list
  
  set depotName2 [who] of one-of legal-streets with [depot?]
  
  while [member? depotName2 second-list] [
    set depotName2 [who] of one-of legal-streets with [depot?]
  ]
  set second-list lput depotName2 second-list
  
  ask cars [
    set secondlist second-list 
    set firstlist second-list 
  ]
end

; Selects random cars from the full list of possible cars.
to random-five-hundred
  print word "Hey" car-number
  set car-nums[]
  while [length car-nums < car-number] [
   let thisOne random 6499 
   if member? thisOne car-nums = false [
    set car-nums lput thisOne car-nums 
   ] 
  ]
end

; Sets up the depots on the map.
to setup-depot 
  ask postcodes [
    set myDepot nobody
    let counter 5
    let countNear count legal-streets in-radius counter
    while [countNear = 0 and counter <= 20] [
     set counter counter + 5 
     set countNear count legal-streets in-radius counter
    ]
    if countNear > 0 [
      set myDepot min-one-of legal-streets in-radius counter [distance myself]
      ask myDepot
      [
        set color red
        set size 5
        set hidden? false
        set depot? true
      ]
    ]
  ]
end

; set up global parameters
to setup-globals
  set xmin -74.02
  set xmax -73.92
  set ymin 40.7 
  set ymax 40.88
  set speed-max 1
  set speed-min 0
  set acceleration 0.11
  set deceleration 0.1
  set roundabouts[]
  set routeCount 0
  set global_flowRate 0
  set global_avgSpeed 0
  set global_congmetric 0
  set global_conNeighMetric 0
  set osms-congested[]

  ;These are passed in by the simulation starter
  set run-name ""
  set car-number 0
  set minNeighSize 0
  set maxNeighSize 0
  set changeMemLen 0
  set aggThreshold 0
  
  set scaler 0.05
  
  set car-vision 10
  
  set streetsJammedMemory[]
  set streetsJammed 0
  set streetsJammedNames[]
  set globalCongestion? false
  set congestionTicks 0
  
  set global-temperature 72
end

; This function is used to set up the DETect stats parameters owned by each agent. This basically initialses all the empty arrays that will be used, outlines the number of internal and 
; external variables and adds them to the DETect object.
to setup-stats
  set myHeading 0
  
  set speed-mem[]
  set heading-mem[]
  set age-mem[]
  
  set mem-int-speed[]
  set mem-int-heading[]
  set mem-int-age[]
  
  ;;External Variables
  
  set cars-near 0        ; Cars in radius 1.5
  set dist-near-car  0  
  set cars-head 0
  set cars-speed 0
  set distDest 0
  set distDest-mem[]
  set temperature-mem[]
  
  
  set coeff-cn-myS 0
  set coeff-dn-myS 0
  set coeff-ch-myS 0
  set coeff-cs-myS 0
  
  set coeff-cn-myH 0 
  set coeff-dn-myH 0
  set coeff-ch-myH 0
  set coeff-cs-myH 0
  
  set cars-near-mem[]
  set dist-near-car-mem[]
  set cars-head-mem[]
  set cars-speed-mem[]
  set age-mem[]
  
  set mem-ext-cn[]
  set mem-ext-dn[]
  set mem-ext-ch[]
  set mem-ext-cs[]
  set mem-ext-temperature[]
  set mem-ext-distDest[]
  
  detect:new-data "3" "6" id
  
  detect:add-var id "ext" "carsNear"
  detect:add-var id "ext" "distNear"
  detect:add-var id "ext" "carsHead"
  detect:add-var id "ext" "carsSpeed"
  detect:add-var id "ext" "temperature"
  detect:add-var id "ext" "distDest"
      
      ;Add internal variables
  detect:add-var id "int" "myHead"
  detect:add-var id "int" "speed"
  detect:add-var id "int" "age"
  
  set lasso-Win 500
end

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;Map Building Procedures;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

to setup-gis
  show "Loading patches..."
  gis:set-coverage-minimum-threshold 0.01
  gis:set-coverage-maximum-threshold 0.99
  gis:apply-coverage countries-dataset "MAXSPEED" typeOf
end

; This function is used to initiate the map building to create the street network underlying the model. 
to setup-map
  set projection "WGS_84_Geographic"
  ask patches [
    set pcolor black
  ]
  gis:load-coordinate-system (word "//home/eamonn/DETectPackage/TestModels/Traffic/gisMaps/data/" projection ".prj")
  set countries-dataset gis:load-dataset "//home/eamonn/DETectPackage/TestModels/Traffic/gisMaps/data2/manhattan_AtLast6.shp"
  gis:set-world-envelope-ds [-74.02 -73.9285 40.699743 40.8265]
  set envelope gis:world-envelope
  set xscale (max-pxcor - min-pxcor) / (item 1 envelope - item 0 envelope)
  set yscale (max-pycor - min-pycor) / (item 3 envelope - item 2 envelope)
  
  ; Drawing country boundaries from a shapefile -- as per Uri Wilensky
  gis:set-drawing-color black

  create-road-links
  global-position
  update-out-heads
  try-to-fill
 
  create-r 
  
  while [count builders > 0] [
    build-r
  ]
  create-l
  build-l
  setup-lights
  setup-joints
end

; This GIS map is passed to netlogo in the form of line segments, a start and end point. This function draws these lines on the map. The start and end of each line is
; represented by a junction. If two or more lines have overlapping start or end points, then existing junctions are updated to take the new street segment information.
; Each junction is a type of turtle. Connections between them are made using links.
to create-road-links
  set cou 0
  let thisOne? true
  foreach gis:feature-list-of countries-dataset
    [ 
      let road-n gis:property-value ? "NAME"
      let one-w gis:property-value ? "ONEWAY"
      let Road? true
      if Road? [
        let max-s gis:property-value ? "MAXSPEED"
        let osm gis:property-value ? "ID"
        let hway gis:property-value ? "HIGHWAY"
        
        let mylist[]
        let mylist2[]
        let visited[]
        if osm != "-6568" [  ; A hack here to ignore one specific OSM
          
          foreach gis:vertex-lists-of ?
          [ 
            let previous-turtle nobody
            foreach ?
            [ 
              
              let location gis:location-of ?
              if not empty? location
              [
                set mylist lput location mylist
                let xpos item 0 location
                let ypos item 1 location
                let existJunc one-of junctions with [xcor = xpos and ycor = ypos]
                ifelse existJunc = nobody[
                  create-junctions 1
                  [ 
                    set xcor item 0 location
                    set ycor item 1 location
                    set headings[]
                    set mylights[]
                    set street-names[]
                    set lastSet[]
                    set lastSetO[]
                    set downStream[]
                    set visited lput who visited
                    ifelse one-w = "-1" [ ;;SO if it's one way and reverse order...stupid OSM!
                      if previous-turtle != nobody
                      [ 
                        set street-names lput road-n street-names
                        create-road-to previous-turtle [
                          set road-name road-n
                          set one-way one-w
                          set high-way hway
                          set max-speed max-s
                          set osm-id osm
                          set down-Stream[]
                        ]
                      ]
                    ][ ;;If Else
                    if previous-turtle != nobody
                    [ 
                      ask previous-turtle [
                        create-road-to myself [
                          set road-name road-n
                          set one-way one-w
                          set high-way hway
                          set max-speed max-s
                          set osm-id osm
                          set down-Stream[]
                        ]
                          set street-names lput road-n street-names
                      ]
                      if one-w = "no" or one-w = "1" or one-w = "" or one-w = "NO" or one-w = "mno" [
                        set street-names lput road-n street-names
                        create-road-to previous-turtle [
                          set road-name road-n
                          set one-way one-w
                          set high-way hway
                          set max-speed max-s
                          set osm-id osm
                          set down-Stream[]
                        ]
                      ]
                    ]
                    ]
                    set hidden? true
                    set previous-turtle self 
                  ] ;;End Create Junction
                ][
                set visited lput [who] of existJunc visited
                
                ask existJunc [
                  ifelse one-w = "-1" [ ;;SO if it's one way and reverse order...stupid OSM!
                    if previous-turtle != nobody and previous-turtle != self
                    [ 
                      set street-names lput road-n street-names
                      create-road-to previous-turtle [
                        set road-name road-n
                        set one-way one-w
                        set high-way hway
                        set max-speed max-s
                        set osm-id osm
                        set down-Stream[]
                      ]
                    ]
                  ][ ;;If Else
                  ifelse previous-turtle != nobody and previous-turtle != self
                  [ 
                    ask previous-turtle [
                      create-road-to myself [
                        set road-name road-n
                        set one-way one-w
                        set high-way hway
                        set max-speed max-s
                        set osm-id osm
                        set down-Stream[]
                      ]
                        set street-names lput road-n street-names
                    ]
                    if one-w = "no" or one-w = "1" or one-w = "" or one-w = "NO" or one-w = "mno" [
                      set street-names lput road-n street-names
                      create-road-to previous-turtle [
                        set road-name road-n
                        set one-way one-w
                        set max-speed max-s
                        set high-way hway
                        set osm-id osm
                        set down-Stream[]
                      ]
                    ]
                    ;set lastSet lput osm lastSet
                  ] [
                  
                  ]
                  ]
                  set hidden? true
                  set previous-turtle self
                ]
                ]
              ]
            ]
          ]
          
          ;;
          if item 0 visited = last visited [
            set roundabouts lput osm roundabouts 
          ]
          let visit2 remove-duplicates visited 
          if length visit2 != length visited [
            set roundabouts lput osm roundabouts 
          ]
          if osm = "-20912" [
            ;print visited
          ]
          
          ifelse one-w = "-1" [
            let junName item 0 visited
            ask junction junName [
              set lastSet lput osm lastSet 
            ]
          ][
          let junName last visited
          ask junction junName [
            set lastSet lput osm lastSet 
          ]
          if one-w = "no" or one-w = "1" or one-w = "" or one-w = "NO" or one-w = "mno" [
            set junName item 0 visited
            ask junction junName [
              set lastSet lput osm lastSet 
            ]
          ]
          
          ]
        ]
      ]
    ] 
end

; Once the junctions and links are in place, the road between them is "paved" with "street" turtles. First a builder is spawned for each street at each starting junction. This builder is pointed at the destination junction.
to create-r
  ask junctions [
    let myx xcor
    let myy ycor
    foreach headings [
      let nxcor [xcor] of ?
      let nycor [ycor] of ?
      let lname [road-name] of link-with ?
      let osm [osm-id] of link-with ?
      let ds [down-Stream] of link-with ?
      let hway [high-way] of link-with ?
      let s self
      ask patch-here [
        sprout-builders 1[
          set color yellow
          set shape "turtle"
          set heading towardsxy nxcor nycor
          set xcor myx
          set ycor myy 
          set junc-count 0
          set source s
          set target ?
          set name lname
          set osm-name osm
          set hi-way hway
          set downStream ds
        ] 
      ]
    ]
  ]
end

; The builder then moves along the street "paving" a street turtle every 1 patch as it goes. These streets allow cars to find out what street they are on and what direction to point if they get lost when 
; the simulation is running.
to build-r
  ask builders [
    set heading towards target
    let myname name
    let tar target
    let osm osm-name
    let hway hi-way
    let sou source
    let ds downStream
    if distance source < 0.5 [
      forward 0.5
    ]
    ifelse distance target <= 0.7 [
      let head towards target
      ;move-to target
      let myx xcor
      let myy ycor
      let roads-in [my-in-links] of target
      let roads-name [road-name] of roads-in
      set roads-name remove-duplicates roads-name
      ask patch-here [
        sprout-streets 1[
          set color yellow
          set shape "line half"
          set xcor myx
          set ycor myy
          set target tar
          set heading towards target
          set hidden? true
          set street-name myname
          set osm-name osm
          set source sou
          set done? false
          set id (word osm-name "-" [who] of target)
          set downStream ds
          set congCount 0
          set congNeighCount 0
          set congested? false
          set CongestedNeigh? false
          set depot? false
          set highway hway
        ]
;      ]
      ]
      die 
    ][
    let myx xcor
    let myy ycor
    let head towards target
    ask patch-here [
      sprout-streets 1[
        set color yellow
        set shape "line half"
        set heading head
        set xcor myx
        set ycor myy 
        set target tar
        set heading towards target
        set hidden? true
        set street-name myname
        set osm-name osm
        set highway hway
        set source sou
        set done? false
        set id (word osm-name "-" [who] of target)
        set downStream ds
        set congCount 0
        set congNeighCount 0
        set congested? false
        set CongestedNeigh? false
        set depot? false
      ] 
    ]
    forward 1 
    ]
  ]
end

; This function creates traffic lights at junctions with more than one possible out street. It creates one for each street link that enters the function.
to create-l
  ask junctions [
    let in-streets [road-name] of my-in-links
    set in-streets remove-duplicates in-streets
    let counter length in-streets
    if counter > 1
    [
      let myx xcor
      let myy ycor
      let neighs [who] of in-link-neighbors
      foreach neighs [
        let lname [road-name] of link-with junction ?
        let osm [osm-id] of link-with junction ?
        let s self
        ask patch-here [
          sprout-lightMakers 1[
            set color yellow
            set shape "turtle"
            set heading towards junction ?
            set xcor myx
            set ycor myy
            set target s
            set source junction ?
            set name lname
            set osm-name osm
          ] 
        ]
      ]
    ]
  ]
end

; This function builds the light by giving it specific parameters.
to build-l
 ask lightMakers [
  forward 0.5 
  let myx xcor
  let myy ycor
  let tar target
  let osm osm-name
  let sou source
  ask patch-here [
    sprout-lights 1[
      set color red
      set shape "circle"
      set size 0.5
      set heading towards tar
      set xcor myx
      set ycor myy
      set target tar
      set source sou
      set osm-name osm
      set gridLock? false
    ]
  ]
  die       
]
end

; This function sets up the round robin pattern for lights that are at the same junction.
to setup-lights
  set junctions-with-lights junctions with [count my-in-links > 1] 
  ask junctions-with-lights[
    set myLights lights in-radius 0.6 with [target = myself]
    set phases count myLights
    let yourPhase 1
    let par self
    ask myLights[
      set parent par
    ]
    set current-phase 1
  ]
  ask lights with [parent != 0] [
    set onPhase [current-phase] of parent 
    ask parent  [
      set current-phase current-phase + 1 
    ]
  ]
  update-lights
end

; This function updates the lights on off status during the simulation.
to update-lights 
  ask lights with [parent != 0] [
    let curPhase [current-phase] of parent
    ifelse curPhase = onPhase [
      set color green
    ][
    set color red
    ]
  ]
  ask junctions [
    set current-phase current-phase + 1
    if current-phase > phases [
      set current-phase 1 
    ]
  ] 
end

to setup-joints 
  ask junctions [
    let roads-name [road-name] of my-in-links
    set roads-name remove-duplicates roads-name
    ifelse length roads-name > 1 [
      set junction? true
    ]
    [
      set junction? false
    ]
  ]
end

; This function is critical to navigation in the model. Its purpose is to allow each junction to work out what OSM-Id (street id) can be reached if an agent takes each and any of the streets that begin at that junction.
; Cars store their directions to the destination as a list of OSM-Ids to visit in order. So the junction can then present a menu of targets for the car agent to choose from. If the cars next OSM id is on the menu they select that
; direction
to try-to-fill
  ask junctions [
   set downStream [osm-id] of my-out-links 
  ]
  let impJuncts junctions with [length lastSet > 0] 
  ask impJuncts [ 
     
   foreach lastSet [
    let visited[]
    
    let myOuts [osm-id] of my-out-links
    let alink one-of my-in-links with [osm-id = ?]
    let s self
    let t [other-end] of alink
    set visited lput self visited
    ask alink [
      set down-Stream sentence myOuts down-Stream
    ]
    
    while [t != nobody and member? t visited = false] [
      ask t [
        set myOuts sentence myOuts [osm-id] of my-out-links
        set myOuts remove-duplicates myOuts
        set downStream sentence myOuts downStream
        set downStream remove-duplicates downStream 
        
        set visited lput self visited
        set alink one-of my-in-links with [osm-id = ? and other-end != s]
        set s self
        
        ifelse alink = nobody [
         set t nobody
        ][
        ask alink [
          set down-Stream sentence myOuts down-Stream
        ]
        set t [other-end] of alink 
        ]
      ]
    ] 
   ] 
  ]
  set impJuncts junctions with [length lastSetO > 0] 
  ask impJuncts [
   foreach lastSetO [
    let visited[]
    let myOuts [osm-id] of my-out-links
    let alink one-of my-in-links with [osm-id = ?]
    let s self
    let t [other-end] of alink
    set visited lput self visited
    ask alink [
      set down-Stream sentence myOuts down-Stream
    ]
    while [t != nobody and member? t visited = false] [
      ask t [
        set myOuts sentence myOuts [osm-id] of my-out-links
        set myOuts remove-duplicates myOuts
        set downStream sentence myOuts downStream
        set downStream remove-duplicates downStream 

        set visited lput self visited
        set alink one-of my-in-links with [osm-id = ? and other-end != s]
        set s self
        ifelse alink = nobody [
         set t nobody
        ][
        ask alink [
          set down-Stream sentence myOuts down-Stream
        ]
        set t [other-end] of alink 
        ]
      ]
    ] 
   ] 
  ]
  foreach roundabouts [
   let allDS [down-Stream] of roads with [osm-id = ?]
   let combinedDS[]
   foreach allDS [
    set combinedDS sentence combinedDS ? 
   ]
   set combinedDS remove-duplicates combinedDS
   ask roads with [osm-id = ?] [
    set down-Stream combinedDS 
   ]
      
  ]
end

; Each street segment that is greater than 8 patches in length has a "manhole" placed on it at the halfway point.
; The manhole is used to monitor cars that pass along the street and monitor the flow rate as a function of average speed and
; volume of cars passing. The objective measure of emergence is the number of manholes with a low flowrate when their neighbouring manholes also
; have a low flowrate.
; This function sets up the manhole and initialises each manhole which is a breed of turtle.
to make-manholes 
    let theseJunctions [who] of junctions with [count my-out-links with [link-length >= 8] > 0]
    foreach theseJunctions [
      ask junction ? [
        let longLinks [other-end] of my-out-links with [link-length >= 5]
        foreach longLinks [
          let osm [osm-id] of link-with ?
          let tar ?
          let sou self 
          let dist ([link-length] of link-with ? / 2)
          ask patch-here [
            sprout-manholes 1 [
              set target tar
              set source sou
              set distFrom dist
              set osm-name osm
              set heading towards tar 
              set shape "circle"
              set size 3
              set color blue
              set  carCount 0
              set carSpeeds[]
              set flowRate 0
              set avgSpeed 0.5
              set used? true
              set memCarName []
              set hidden? true
              set avgSpMem[]
              set flRtMem[]
            ] 
          ]
        ]
      ]
    ]
    ask manholes [
     forward distFrom
    ]
end

; This function is used to draw the post-code areas on the map and to place a depot on the street agent closest to the center of the depot.
to setup-postal-areas
  ask patches [
    set pcolor black
  ]
  let counter 0
  let counter2 0
  let targetH 5
  let county min-pycor
  let countx 0
  
  let thisMaxX 80
  let thisMinX min-pxcor
  
  while [county <= max-pycor] [
   set counter counter + 1

   set countx thisMinX 
   let c 1
   while [countX <= thisMaxX] [
    
   ask patches with [pxcor = (countX + 20) and pycor = (county + 20) and pxcor >= thisMinX] [
      sprout-postcodes 1   [
        set shape "circle"
        set color red
        set size 2
        set address (word pycor "-" pxcor)
        set hidden? true
        set carsList-low[]
        set mhListLots[]
        set mhListNeigh[]
      ]
    ] 
    set countX countX + 40
   ]
   
   set county county + 40
   
   if counter = targetH [
    set counter 0 
    set thisMaxX thisMaxX + 40
    set thisMinX thisMinX + 40
    set counter2 counter2 + 1
   ]
   
   if counter2 = 1 [
    ifelse targetH = 5 [
     set targetH  4
     set counter2 0
    ][
      set targetH 1
    ]
   ]
  ]
  
  ask postcodes with [distance min-one-of junctions [distance myself] > 20] [die]  
  ask manholes [
   set mypostCode min-one-of postcodes [distance myself] 
  ]
end

to update-out-heads 
  ask junctions [
    set headings [other-end] of my-out-links
    set street-names remove-duplicates street-names
  ]    
end



;;GH EXtension commands
;  find-route
;  next-street-name
;  next-instruction
;  next-coords
;  current-street
;  current-osm
;  next-osm
;  osm-chec
;  connect-gh
;  import-data
;  car-list

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;   Car Procedures  ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;; Navigation ;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

; This function is used at the start of the simulation. Each depot has 13 cars spawn on it. The cars then move to a free spot nearby. This
; function is used to find the free spot.
to find-free-spot
  ; Figure out the X,Y coordinates of the agents next destination. If no destination left, die.
  let myNextCords graphhopper:next-destination id   
  ifelse item 0 myNextCords = 0 [
    die 
  ][
  ; Find the nearest street segment (a turtle of the street breed) with no other turtle on it and move there.
  ; Gradually work out from currently location to a max distance of 200
  let nearestSt nobody 
  let counter 1
  while [nearestSt = nobody and counter <  200] [
    let options legal-streets in-radius counter with [count other cars in-radius 0.4 = 0 and distance myself > counter - 5]
    set nearestSt min-one-of options [distance myself] 
    set counter counter + 1
  ]
  ; Move to free spot.
  move-to  nearestSt
  ]
end

; THis fucnction is used to allow the car agent to locate itself and get its route to destination. This is called
; the first time the agent looks for a route. All subsequent route calculations call locate-car-during-run
to locate-car-first
  ;1 . Move to the nearest Street
  let nearestSt nobody
  
  ;2. Get a direction from there
  let currLat convert-ycor ycor
  let currLong convert-xcor xcor
  
  let destLat 0
  let destLong 0
  set nextTurn 4
  let myNextCords[]
  
  set nextDest nobody
  
  ; The destination must be further than 2 osm links away. If not, get a different loction.
  while [length osmList < 2][
    let nextDepot one-of depotList
    set nextDest one-of legal-streets with [who = nextDepot]
    
    set destinationx [xcor] of nextDest
    set destinationy [ycor] of nextDest
    
    set destLong convert-xcor destinationx
    set destLat convert-ycor destinationy
    
    set osmList graphhopper:find-first-route id currLat currLong destLat destLong
    let trip-info graphhopper:trip-info id
  ]
  set depotList remove [who] of nextDest depotList
  
  set nextOsm item 0 osmList
  set osmList remove-item 0 osmList
  set nextOsm item 0 osmList
  set osmList remove-item 0 osmList
  ifelse length osmList > 0 [
  set futureOsm item 0 osmList
  set osmList remove-item 0 osmList
  ][
   set futureOsm "0"
  ]
  
  ; If the agent cant figure out what street its on, it dies. This happens for about
  ; 4-5 agents at the start of each simulation.
  let nearSts [who] of streets in-radius 1 with [member? [nextOsm] of myself downStream]
  ifelse length nearSts = 0 [
    print (word who " I think i'll just die so " nextOsm)
    die
  ]
  [
    ; I know what my nearest street is, so fact it and move along to the next
    ; junction.
    set nearestSt street one-of nearSts 
    set heading [heading] of nearestSt
    set target [target] of nearestSt
    set lastRoad [source] of nearestSt
    set current-osm [osm-name] of nearestSt
    set lost? false
  ]
end

; This function is used by agents during the run to figure out where they are and to find a route to their next 
; target depot, their next destination.
to locate-car-during-run
  let currLat convert-ycor ycor
  let currLong convert-xcor xcor
  let destLat 0
  let destLong 0
  set nextDest nobody
  let myNextCords[]
  set osmList[]
  let shouldDie? false
  
  while [length osmList < 3 and shouldDie? = false][
    ifelse lost? [      
      let nextDepot one-of depotList
      set nextDest one-of legal-streets with [who = nextDepot]
      
      set destinationx [xcor] of nextDest
      set destinationy [ycor] of nextDest
    
      set destLong convert-xcor destinationx
      set destLat convert-ycor destinationy
    
      set osmList graphhopper:find-first-route id currLat currLong destLat destLong
    ][
    ; If the agent has visited all depots and is back at its home depot, then it should die.
    ifelse  length depotList = 0 and distance homeDepot < 2 [
      set shouldDie? true
    ]
    [
      ; Periodically, between certain timesteps, all agents are sent to the same
      ; set of depots. This causes agents to converge and causes traffic jams.
      ifelse ticks >= 15000 and ticks <= 25000 and length firstlist > 0 [
        ; go to set 1
        let nextDepot item 0 firstlist
        set nextDest one-of legal-streets with [who = nextDepot]
        set firstlist remove-item 0 firstlist
      ]
      [
        ifelse ticks >= 40000 and ticks <= 55000 and length secondlist > 0[
          ; go to set 2
          let nextDepot item 0 secondlist
          set nextDest one-of legal-streets with [who = nextDepot]
          set secondlist remove-item 0 secondlist
        ]
        [
          ; If not during one of the converge periods, just pick the next depot on the list.
          ; If no depots list, return to homeDepot.
          ifelse length depotList > 0 [
            let nextDepot one-of depotList
            set nextDest one-of legal-streets with [who = nextDepot]
          ][
          set nextDest homeDepot 
          ]
        ]
      ]
    ]
    ; Set destination to coordinates of nextDestination
    set destinationx [xcor] of nextDest
    set destinationy [ycor] of nextDest
    ; Convert from Long and Latitude to X, Y on the map.
    set destLong convert-xcor destinationx
    set destLat convert-ycor destinationy
    
    set routeCount routeCount + 1
    set osmList graphhopper:find-first-route id currLat currLong destLat destLong ; Get the route as a list of OSM ids to visit in order.
    ] 
  ]
  
  ; Remove next destination from the depotList
  if nextDest != nobody and nextDest != homeDepot [
    set depotList remove [who] of nextDest depotList
  ]
  
  ; Assuming the agent shouldn't die ....
  ifelse shouldDie? [
    die 
  ]
  [
    ; This figures out the direction the agent should move in and points them in that direction
    let nearestSt nobody
    let myS nobody
    let myO nobody
    
    set nextOsm item 0 osmList
    set osmList remove-item 0 osmList
    set nextOsm item 0 osmList
    set osmList remove-item 0 osmList
    set futureOsm item 0 osmList
    set osmList remove-item 0 osmList
    
    set myS current-street
    set myO current-Osm
    
    let nearSts legal-streets in-cone 1 180 with [osm-name = myO and member? [nextOsm] of myself downStream]
    
    if count nearSts = 0 [
      set nearSts streets in-cone 1 180 with [osm-name = myO and member? [nextOsm] of myself downStream]
    ]
    set nearestSt one-of nearSts
    set heading [heading] of nearestSt
    set target [target] of nearestSt
    set lastRoad [source] of nearestSt
    set current-street [street-name] of nearestSt
    set current-osm [osm-name] of nearestSt
    set speed 0.5
    
    set lost? false
  ]
end

to update-vars
  set nextTurn graphhopper:next-instruction id
  if nextTurn != 4 [
    set nextStreet graphhopper:next-street-name id
    let myNextCords graphhopper:next-coords id
    set nextx convert-long item 1 myNextCords
    set nexty convert-lat item 0 myNextCords
    set nextx precision nextx 4
    set nexty precision nexty 4
  ]
end

to update-Osm
  ifelse length osmList > 0 [
    set futureOsm item 0 osmList
    set osmList remove-item 0 osmList 
  ]
  [
    set futureOsm "0"
  ]
end



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;; ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;Movement ;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

; This function is used to get each car to move at each time step.
to move-cars
  ask cars [
    let triggerEnd? false
    if lost? [
      if distancexy lostx losty > 5 and (count junctions in-radius 5 = 0) and (count legal-streets in-cone 1 180 with [osm-name = [current-Osm] of myself] > 0) [
        locate-car-during-run
      ]
    ] 
    set-speed
    if speed > 0 [
      let nOsm nextOsm
      let fOsm futureOsm
      let lRoad lastRoad
      let cOsm current-Osm
      ifelse (distance target <= speed and speed > 0.5) or (distance target <= 0.5)[
        if atJunction? = false  or destFound? = true[
          find-next-road
          set atJunction? true
        ]
        ;; Can I move through junction?
        set canPass? false
        check-way
        ifelse canPass? [
          move-through-junction
          set canPass? false
          set atJunction? false
        ][
        if altMode? [   ;; Alt mode is turned on when there id Gridlock and the lights ask cars to just move and pick another depot to go to instead
          find-next-road-alt
          check-way
          if canPass? [
            move-through-junction
            set canPass? false
            set atJunction? false
          ]
        ] 
        ]
      ] ;; ifelse distance target < 0.5
      [ ;;Else
        set heading towards target
        forward speed
      ] ;; End Else
    ] ;; if speed > 0
    if (destFound? = false and (nextOsm = "0" or (current-Osm = nextOsm and futureOsm = "0"))) [  ;
      set destFound? true
      set nextTurn 0
      set destCount destCount + 1
    ]

    if destFound? and (distancexy destinationx destinationy > 5) and (count junctions in-radius 5 = 0) and (count legal-streets in-cone 1 180 with [osm-name = [current-Osm] of myself] > 0)[
      locate-car-during-run
      set destFound? false
    ]
    set altmode? false
  ] ;; ask cars  
  
end

; This function is used to find the correct street/road to take once a junction is encountered. This is picked from the set of all possible paths that can be taken from the particular junction.
to find-next-road
  let diff speed - distance target
  let nOsm nextOsm
  let fOsm futureOsm
  let lRoad lastRoad
  let cOsm current-Osm
  let targ nobody
  let myheaOpts [headings] of target
  ifelse destFound? = false [
    ifelse futureOsm  != "0" [
      ask target [
        ; First see if there is an OSM id out that is the same one you are looking for next and with a future OSM downstream
        if count my-out-links with [osm-id = nOsm and member? fOsm down-Stream and other-end != lRoad] > 0 [
          let options [other-end] of my-out-links with [osm-id = nOsm and member? fOsm down-Stream and other-end != lRoad]
          set targ one-of options
        ]
      ]
    ]
    [
      ; If future OSM = 0 then you are are you last one, so dont look for that
      ask target [
        if count my-out-links with [osm-id = nOsm and other-end != lRoad] > 0 [
          let options [other-end] of my-out-links with [osm-id = nOsm and other-end != lRoad]
          set targ one-of options
        ]
      ]     
    ]
    
    ifelse targ != nobody [
      ;set lastRoad target
      ;set current-Osm nextOsm
      set nextOsm futureOsm
      update-Osm
    ]
    [
      ; If here, this junciton does not have the OSM out im looking for so, just continue along current OSM
      ask target [
        ; Try to find a road out that is the same OSM i am on and is not the last section i have just traversed
        if count my-out-links with [osm-id = cOsm and other-end != lRoad] > 0 [
          let options [other-end] of my-out-links with [osm-id = cOsm and other-end != lRoad]
          set targ one-of options
        ]
      ]
      if targ = nobody [
        ; I couldnt find that so i must be lost
        if lost? = false and destfound? = false [
          set lost? true
          print (word who " : I'm lost @ " target " : looking for : " nextOsm " : " nextStreet " : Came from : " current-Osm) 
          set lostx xcor
          set losty ycor
          ;ask cars [set happy? true]
        ]
        ask target [
          ; So just find any exit, but try not to u turn
          if count my-out-links with [other-end != lRoad] > 0 [
            let options [other-end] of my-out-links with [other-end != lRoad]
            
            set targ one-of options ;[subtract-headings (towards myself + 180) [heading] of myself]
           
          ]
        ]
        if targ = nobody
        [
          ; Must be a cul de sac so do a u turn
          ask target [
            set targ [other-end] of one-of my-out-links
          ]
        ]
      ]
    ] 
  ]
  [ ; I am at a junction having recently found my destination, so I'm just looking for a road that i havent just come from
    ; IE i dont want to do a UTurn
    ask target [
      if count my-out-links with [other-end != lRoad] > 0 [
        let options [other-end] of my-out-links with [other-end != lRoad]
        set targ one-of options
      ]
    ]
    if targ = nobody 
    [
      ; If here i must do a UTurn
      ask target [
        set targ [other-end] of one-of my-out-links
      ]
    ]
  ]
  set nextTarget targ
end

; Same as above except alt-mode is turned on in the car
to find-next-road-alt
  let lRoad lastRoad
  let targ nobody
  
  let nextT nextTarget
  
  ask target [
    ; Cant pass the way i want, so see if there are other options that dont involve the same way or going backwards
    if count my-out-links with [other-end != nextT and other-end != lRoad] > 0 [
      let options [other-end] of my-out-links with [other-end != nextT and other-end != lRoad]
      set targ one-of options
    ]
  ]
  
  if targ != nobody [
   set lost? true
   set lostx xcor
   set losty ycor
   set nextTarget targ 
  ]
end

; This function checks whether a car can move through a junction in its intended target direction. Basically checks for enough space on the other side of the junction.
to check-way
  let thisX xcor
  let thisY ycor
  let head heading
  set speed 0.5
   
  ifelse speed >= distance target [
    let counter count other cars in-cone distance target 45 with [distance myself > 0 and target = [target] of myself and current-osm = [current-osm] of myself and lastRoad = [lastRoad] of myself]
    ifelse counter = 0 [
      
      move-to target
      set heading towards nextTarget
      set counter count other cars in-cone 0.05 45 with [distance myself > 0 and target = [nextTarget] of myself and lastRoad = [target] of myself]
      
      ifelse counter = 0 [
        forward 0.04
        set canPass? true
      ][
      set speed 0
      set xcor thisX
      set ycor thisY
      set heading head
      ]
    ][
    
    ]
  ][
  set speed 0
  ]
  set heading head
end

; Once a car encounters a manhole on the street it is travelling it uses this function to tell the manhole to observe it.
to checkIn-Manhole 
  ask cars [
    let manNear one-of manholes in-radius 2 with [osm-name = [current-osm] of myself and target = [target] of myself]
    ifelse lastManhole = nobody[
      if manNear != nobody [
        set lastManhole manNear
        ask lastManhole [
          set carCount carCount + 1
          set memCarName lput [id] of myself memCarName 
        ]
        if length manHoleMem = 50 [
         set manHoleMem remove-item 0 manholeMem 
         set manHoleTimeMem remove-item 0 manHoleTimeMem
        ]
        set manHoleMem lput [who] of manNear manHoleMem 
        set manHoleTimeMem lput ticks manHoleTimeMem
        set checkx xcor
        set checky ycor
        set arriveTime ticks
      ] 
    ]
    [
      ifelse manNear = nobody [
        let trav distancexy checkx checky
        let elaps ticks - arriveTime
        ask lastManhole [
          set carSpeeds lput (trav / elaps) carSpeeds
        ]
        ;]
        set lastManhole nobody
        set arriveTime nobody
      ]
      [
        if (ticks - arriveTime) mod 10 = 0 [
          let trav distancexy checkx checky
          ask lastManhole [
            set carSpeeds lput (trav / 10) carSpeeds
          ]
          set checkx xcor
          set checky ycor
          set arriveTime ticks
        ]
        
      ]
    ]
  ]
end

; This function moves the car through the target junction
to move-through-junction
  let nextT nextTarget
  let targ nobody
  let cOsm nobody
  ask target [
    set cOsm [osm-id] of link-with nextT
  ]
  set heading towards nextTarget
  set current-Osm cOsm
  set lastRoad target 
  set target nextTarget
end

; This function sets the cars speed based on other cars around it and traffic lights.
to set-speed 
  ;; get the turtles on the patch in front of the turtle
  let myHead heading
  let mytarg target 
  let sour lastRoad
  let myosm current-Osm
  let distTarg distance target
  let lightColor green

  let lightsAhead lights in-cone 1 90 with [osm-name = myosm and target = mytarg and source = sour and distance myself < distTarg]
  if any? lightsAhead[
  
    let lightAhead one-of lightsAhead
    set lightColor [color] of lightAhead
    if lightColor = red [
      set speed 0
    ]
  ]
  if lightColor != red [
  
  let car-ahead min-one-of other cars in-cone 1.5 30 with [target = mytarg and lastRoad = sour and distance myself > 0] [distance myself]
  ifelse car-ahead != nobody
  [
    slow-down-car car-ahead
  ]
  [ 
    speed-up-car ]
  ]
  if speed < speed-min [
    set speed speed-min 
    ]
  if speed  > speed-max [
    set speed speed-max]
end

; Function to slow down car
to slow-down-car [car-ahead]
  set speed [speed] of car-ahead - deceleration
end

; Function to speed up car
to speed-up-car
  set speed speed + acceleration
end

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;   General Run Procedures  ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


to go
  if all? cars [happy?] or ticks = 700005 [
    stop
  ]

  if ticks = 20000 [
 	detect:printVariables
  ]
  
  if ticks mod 3 = 0 [
    update-lights 
  ]
  checkIn-Manhole 
  lights-dictate ;; Lights tell cars to take alternate route
  move-cars
  ask cars [
    if any? other cars in-radius car-vision [
      get-Int-Variables
      get-Ext-Variables
      fill-memory
    ]
  ]
  ask cars [
    ifelse relsChosen? [
      run-Regressions
      
    ][
    choose-variables
    
    ]
  ]
  
  if ticks > 0 and ticks mod 50 = 0 [
    evaluate-congestion
    calculate-globals
    update-GridLock
    
    write-to-file-comprehensive
    ask manholes [
     set memCarName [] 
    ]
    ask postcodes [
     set carsList-low[]
     set mhListLots[]
     set mhListNeigh[]
    ]
  ]
  
  if ticks > 0 and ticks mod 20 = 0 [
    update-temp 
  ]
  
  swap-neigh
  tick
end


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;   Statistics Building  ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

; This function is used to determine the manholes on the map that have congestion. Manholes have a vision field with a radius of 2 all around them. For a car to "pass" it must enter and leave this
; field of vision with its time entering and leaving used to calculate average speed. It is also possible that a car has entered and not yet left the field of vision when this function is called.
; This second case is catered for by taking the cars current speed as an estimate of its average speed.
to evaluate-congestion
  ; Step 1: Ask manholes to update their "flowrate"
  ask manholes [
    set used? false
    set color 45                   ; update colour so manholes are visible
    set flowRate carCount * 20     ; This function is run every 50 time steps. 1000 timesteps is considered an hour. So X 20 gives "cars per hour".
     
    if length avgSpMem = 2 [
     set avgSpMem remove-item 0 avgSpMem 
     set flRtMem remove-item 0 flRtMem
    ]
    
    ; Calculate average speed.
    ; If some cars passed of there are cars in the field of vision.
    ifelse flowRate > 0 or count cars in-radius 2 > 0 [           
      ifelse length carSpeeds > 0 [
        set avgSpeed (sum carSpeeds / length carSpeeds) 
      ]
      [
        set avgSpeed 0                       ; there are cars but they have not passed through so they must be stopped. Average speed is thus 0.
      ]
      if carCount = 0 [
        set memCarName [id] of cars in-radius 2    ; Track the cars, for logging if needed.
      ]
      set carCount 0                          ; Clear memory.
      set carSpeeds[]
    ]
    [
      set avgSpeed 0
    ]
    
    ; Add the average speed and flowrate for this cycle to a short term memory.
    set avgSpMem lput avgSpeed avgSpMem
    set flRtMem lput flowRate flRtMem
    ; Calculate the flowrate from the last 2 observations.
    ifelse length avgSpMem = 2 [
      let aSpeed 0
      let counter 0
      while [counter < 2] [
        set aSpeed aSpeed + (item counter avgSpMem * item counter flRtMem) 
        set counter counter + 1
      ]
      
      ifelse sum flRtMem > 0 or count cars in-radius 2 > 0  [
        set used? true
        ifelse sum flRtMem > 0 [
          set aSpeed (aSpeed / sum flRtMem)
         
        ][
        set aSpeed 0
        
        ]
        let speedIn aSpeed
        let flowIn ((mean flRtMem) / 300)                  ; Weighted average
        set congmetric (speedIn + (flowIn * 15)) / 2       ; Weighted average
      ]
      [
        set used? false                                      ; manhole was not used
        set congmetric 1                                     ; 1 means no congestion  
      ]
    ]
    [
      set congmetric 1                                     ; 1 means no congestion
    ]
  ]
  
  ; 2. Update patch colour.
  ask patches [ 
    set pcolor black 
  ]
  
  ; 3. Ask streets to assume there is no congestion.
  ask streets [
    set congested? false 
    set congestedNeigh? false
    set proc? false
  ]
  
  ; 4. Get manholes to work out if they are in a congested area i.e. its not just them that has congestion but also their neighbours.
  ask manholes [
    ifelse used?[
      ifelse congmetric <= 0.5 [
        set hidden? false
        ifelse congmetric < 0.45 [   ; Lots of congestion on this manhole's street segement.
          set color red
          let myNeighs other manholes in-radius 10
          ask myPostCode [            ; tell the postcode (for logging)
            set mhListLots  lput [who] of myself mhListLots 
            set mhListLots remove-duplicates mhListLots
          ]
          if any? myNeighs [        ; Check if neighbouring manholes also have lots of congestion on average
            set neighborscore sum [congmetric] of myNeighs 
            set neighborscore neighborscore + congmetric
            set neighborscore neighborscore / (count myNeighs + 1)
            ; This manhole and its neighbours are congested, so the manhole tells the closest street agents that form the street its on
            ; that they are congested.
            if neighborscore < 0.45 [
              let tellSt one-of streets in-radius 1 with [target = [target] of myself and osm-name = [osm-name] of myself]
              if tellSt != nobody [
                ask tellSt [
                  set congested? true 
                ] 
              ]
            ]
          ]
        ]
        [ ; < 0.5 >= 0.45 so lightly congested.     
          set color orange
        ]
      ]
      [   ; > 0.5 so no congestion.
        set hidden? true
      ]
    ]
    [
      set hidden? true
    ]
  ]
  
  ; 5. Get streets without congestion to update.
  ask streets with [congested? = false] [
    set congCount 0
    set congNeighCount 0
  ]
  
  ; 6. Streets with congested inform all other agents that form the street to update their status.
  ask streets with [congested?] [   
    let myNeighbours other streets with [member? [osm-name] of myself downStream and congested?]
    ifelse count myNeighbours > 0 [
      
      let found? false
      let counter 0
      while [found? = false and counter < length downStream]
        [
          let osm item counter downStream
          let ds other streets with [osm-name = osm and congested?]
          if any? ds [
            set found? true
          ]
          set counter counter + 1 
        ]
      ifelse found? [
        set congestedNeigh? true
        set congNeighCount congNeighCount + 1
      ][
      set congestedNeigh? false
      set congNeighCount 0
      ]
    ][
    set congestedNeigh? false
    set congNeighCount 0
    ] 
    
    let sameRoads other streets with [osm-name = [osm-name] of myself and target = [target] of myself]
    let CN? congestedNeigh?
    let C? congested?
    let CNC congNeighCount
    let CC congCount
    ask sameRoads [
      set proc? true 
      set congestedNeigh? CN?
      set congested? C?
      set congNeighCount CNC
      set congCount CC 
    ]
  ]
  
  ; 7. Get streets to update their colour based on congestion levels so its visible on the map.
  ask streets [
    ifelse congestedNeigh? [
      set hidden? false
      ifelse congNeighCount >= 3[
        set color red 
      ][
      set color orange
      ]
    ][
    set hidden? true
    ] 
  ]
  
  ; 8. Ask manholes to update their congested status also.
  ask manholes [
    set  congestedNeigh? false
    if count streets in-radius 2 with [target = [target] of myself and congested?] > 0 [
      set  congestedNeigh? true 
      ask myPostCode [
        set mhListNeigh lput [who] of myself mhListNeigh
        set mhListNeigh remove-duplicates mhListNeigh
      ]
    ] 
  ]
end

; This function is used to calculate the states of global parameters in the model. The most 
; important of these is congestion and the number of congested streets.
to calculate-globals
  let conStreets streets with [hidden? = false and congestedneigh? and color = red]
  ifelse count conStreets > 0 [
    set osms-congested [osm-name] of conStreets
    set osms-congested remove-duplicates osms-congested
    set osms-congested sort-by < osms-congested
    set global_conNeighMetric length osms-congested
  ][
  set global_conNeighMetric 0
  set osms-congested []
  ]
  
  set globalCarsStopped count cars with [speed = 0] / count cars
  
  set streetsJammedNames[]
  set streetsJammedNames [osm-name] of manholes with [used? and congMetric < 0.4 and congestedNeigh?]
  set streetsJammedNames remove-duplicates streetsJammedNames
  set streetsJammed count manholes with [used? and congMetric < 0.4 and congestedNeigh?]
  
  ; This checks to see if the number of streets that are congested is high (above 25). If so, it checks
  ; to see if its remained static for a while (1000 time steps or 20 iterations of this function). If so, gridlock is assumed.
  ifelse streetsJammed > 25 [
    if globalCongestion? = false [
      ifelse length streetsJammedMemory = 0 [
        set streetsJammedMemory lput streetsJammed streetsJammedMemory
      ][
      let val item 0 streetsJammedMemory
      ifelse val = streetsJammed [
        set streetsJammedMemory lput streetsJammed streetsJammedMemory
        if length streetsJammedMemory = 20 [
          set globalCongestion? true
          set congestionTicks ticks 
        ] 
      ][
      set streetsJammedMemory[]
      set streetsJammedMemory lput streetsJammed streetsJammedMemory 
      ]
      ]
    ]
  ][
  if globalCongestion? and (ticks - congestionTicks) > 5000 [
    set globalCongestion? false
  ]
  ask lights [
    set gridLock? false
  ] 
  ]  
end

; 
to update-GridLock
  ifelse globalCongestion? [
   foreach streetsJammedNames [
     let j one-of junctions with [member? ? lastSet]
     let l min-one-of lights with [osm-name = ? and (count cars in-radius 5 > 3)] [distance j]
    
     if l != nobody [
       ask l [
         if gridLock? = false [
           let r random-float 1
           if r >= 0.9 [
             set gridLock? true
           ]
         ]
       ]
     ]
   ]
  ][
  ask lights [
    set gridLock? false
  ] 
  ]
end
 
to lights-dictate 
  ask lights with [gridLock?] [
    let t target
    let carsNear cars in-radius 5 with [target = t]
    if count carsNear > 0 [
     let nearCar min-one-of carsNear [distance myself] 
     ask nearCar [
       set altMode? true 
     ]
    ]
  ]
end


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;                                         DETect procedures
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

; This updates the internal variables of the agent
to get-Int-Variables
  set myHeading heading
end

; This calculates and updates the external variables of the agent
to get-Ext-Variables
  set cars-near 0        
  set dist-near-car  0  
  set cars-head 0
  set cars-speed 0
  
  let flock other cars in-radius car-vision
  
  set cars-near count flock
  set dist-near-car distance min-one-of other cars [distance myself]
  set cars-head mean [heading] of flock
  set cars-speed mean [speed] of flock
  
  ifelse nextDest != nobody [
    set distDest distance nextDest
  ][
  set distDest 0
  ]
end

; The observations for all internal and external variables are stored in list/arrays. This function
; adds the observations calculated in the above 2 procedures the relevant short term list "varName-mem". If that is of length 5
; it is averaged and the result added to the relevant long term sliding observation window "mem-int-varName" for internal
; or "mem-ext-varName" for external.
to fill-memory
  ;; Internal Variables  
  set heading-mem lput (myHeading) heading-mem
  set speed-mem lput (speed) speed-mem
  set age-mem lput (age) age-mem
  
  ;; External Variables
  set cars-near-mem lput (cars-near) cars-near-mem
  set dist-near-car-mem lput (dist-near-car) dist-near-car-mem
  set cars-head-mem lput (cars-head) cars-head-mem
  set cars-speed-mem lput (cars-speed) cars-speed-mem
  set temperature-mem lput (global-temperature) temperature-mem
  set distDest-mem lput (distDest) distDest-mem
  
  ;; Every 5 we aggregate
  if length heading-mem = smoothL [
    set mem-int-speed lput mean speed-mem mem-int-speed
    set mem-int-heading lput mean heading-mem mem-int-heading
    set mem-int-age lput mean age-mem mem-int-age
    
    set mem-ext-cn lput mean cars-near-mem mem-ext-cn
    set mem-ext-dn lput mean dist-near-car-mem mem-ext-dn
    set mem-ext-ch lput mean cars-head-mem mem-ext-ch
    set mem-ext-cs lput mean cars-speed-mem mem-ext-cs
    set mem-ext-temperature lput mean temperature-mem mem-ext-temperature
    set mem-ext-distDest lput mean distDest-mem mem-ext-distDest
    
    ;; Now clear memory
    set heading-mem[]
    set speed-mem[]
    set age-mem[]
    
    set cars-near-mem[]
    set dist-near-car-mem[]
    set cars-head-mem[]
    set cars-speed-mem[]
    set distDest-mem[]
    set temperature-mem[]
  ]
end

; This function is the gossiping function for DETect. 
to swap-neigh
  ; First ask each turtle to pick a random flockmate. If the flock is too small (belwo minNeighSize) the turtle does not gossip;
  ; If the flock is too big (above maxNeighSize) then only the closest maxNeighSize neighbours are potential gossip partners.
  ask cars [
    let rad 10
    let neigh-size 20
    set flockmates other cars in-radius car-vision
    
    ;Now check to see if you have at least the minimum neighbourhood size of neighbours
    ;If So find the MaxNeighSize nearest
    ifelse count flockmates >= minNeighSize [
      let flockmates-all sort-by [[distance myself] of ?1 < [distance myself] of ?2] flockmates
      
      while [length flockmates-all > maxNeighSize][
        let fmallSize length flockmates-all
        set flockmates-all remove-item (fmallSize - 1) flockmates-all
      ]
      
      let maxNum length flockmates-all
      
      let rand random maxNum
      let partner item rand flockmates-all
      ; Get partners Vp
      set Vq [Vp] of partner
      set Vp (Vp + Vq) / 2   ; average
      let newVp Vp
      ask partner [
        set Vp newVp   ; ask partner to update
      ]
    ][
    set Vp 0        ; If no partner, set Vp to 0, as agent is alone and thus not gossiping and no emergence present
    ]
  ]
  ; Next ask turtles to scale towards their own feedback state (localV). 
  ; Thus requires constant feedback from neighbours to change mind
  ask cars [
    let diff Vp - localV
    set Vp Vp - (diff * scaler)
  ]
  
end

; This function is used to initiate the model selection process using Lasso.
to choose-variables
  if length mem-int-speed = lasso-win [ ; Are the observation windows full? All windows are updated concurrently so they are all the same size.
    detect:startR id
    if varsAdded? = false [             ; If the agent hasn't already told DETect what the variables being monitored are, do it now.
      ;Add internal variables
      detect:add-var id "ext" "carsNear"
      detect:add-var id "ext" "distNear"
      detect:add-var id "ext" "carsHead"
      detect:add-var id "ext" "carsSpeed"
      detect:add-var id "ext" "temperature"
      detect:add-var id "ext" "distDest"
      
      ;Add internal variables
      detect:add-var id "int" "myHead"
      detect:add-var id "int" "speed"
      detect:add-var id "int" "age"
      
      set varsAdded? true
    ]
    ; Pass the internal variable data to DETect
    detect:update-var id "int" "myHead" mem-int-heading
    detect:update-var id "int" "speed" mem-int-speed
    detect:update-var id "int" "age" mem-int-age
    
    ; Pass the external variable data to DETect
    detect:update-var id "ext" "carsNear" mem-ext-cn
    detect:update-var id "ext" "distNear" mem-ext-dn
    detect:update-var id "ext" "carsHead" mem-ext-ch
    detect:update-var id "ext" "carsSpeed" mem-ext-cs
    detect:update-var id "ext" "temperature" mem-ext-temperature
    detect:update-var id "int" "distDest" mem-ext-distDest
    
    ;Run the lasso. The return value is the number of internal variables in the model. If this is above 0 model selection was successful.
    let intChose detect:runLasso id

    detect:stopR id
    if intChose > 0 [  ; Model select was successul, so delete all but the latest 20 observations.
                       ; TODO change this so its not hardcoded numbers
      set relsChosen? true
      set mem-int-heading sublist mem-int-heading 480 500
      set mem-int-speed sublist mem-int-speed 480 500
      set mem-int-age sublist mem-int-age 480 500
      
      set mem-ext-cn sublist mem-ext-cn 480 500
      set mem-ext-dn sublist mem-ext-dn 480 500
      set mem-ext-ch sublist mem-ext-ch 480 500
      set mem-ext-cs sublist mem-ext-cs 480 500
      set mem-ext-temperature sublist mem-ext-temperature 480 500
      set mem-ext-distDest sublist mem-ext-distDest 480 500
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
  
  
  set mem-ext-cn remove-item 0 mem-ext-cn
  set mem-ext-cn remove-item 0 mem-ext-cn
  set mem-ext-cn remove-item 0 mem-ext-cn
  set mem-ext-cn remove-item 0 mem-ext-cn
  set mem-ext-cn remove-item 0 mem-ext-cn
  set mem-ext-cn remove-item 0 mem-ext-cn
  set mem-ext-cn remove-item 0 mem-ext-cn
  set mem-ext-cn remove-item 0 mem-ext-cn
  set mem-ext-cn remove-item 0 mem-ext-cn
  set mem-ext-cn remove-item 0 mem-ext-cn
  
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
  
  set mem-ext-ch remove-item 0 mem-ext-ch
  set mem-ext-ch remove-item 0 mem-ext-ch
  set mem-ext-ch remove-item 0 mem-ext-ch
  set mem-ext-ch remove-item 0 mem-ext-ch
  set mem-ext-ch remove-item 0 mem-ext-ch
  set mem-ext-ch remove-item 0 mem-ext-ch
  set mem-ext-ch remove-item 0 mem-ext-ch
  set mem-ext-ch remove-item 0 mem-ext-ch
  set mem-ext-ch remove-item 0 mem-ext-ch
  set mem-ext-ch remove-item 0 mem-ext-ch
  
  set mem-ext-cs remove-item 0 mem-ext-cs
  set mem-ext-cs remove-item 0 mem-ext-cs 
  set mem-ext-cs remove-item 0 mem-ext-cs
  set mem-ext-cs remove-item 0 mem-ext-cs
  set mem-ext-cs remove-item 0 mem-ext-cs
  set mem-ext-cs remove-item 0 mem-ext-cs
  set mem-ext-cs remove-item 0 mem-ext-cs
  set mem-ext-cs remove-item 0 mem-ext-cs
  set mem-ext-cs remove-item 0 mem-ext-cs
  set mem-ext-cs remove-item 0 mem-ext-cs
  
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
  
  set mem-ext-distDest remove-item 0 mem-ext-distDest
  set mem-ext-distDest remove-item 0 mem-ext-distDest
  set mem-ext-distDest remove-item 0 mem-ext-distDest
  set mem-ext-distDest remove-item 0 mem-ext-distDest
  set mem-ext-distDest remove-item 0 mem-ext-distDest
  set mem-ext-distDest remove-item 0 mem-ext-distDest
  set mem-ext-distDest remove-item 0 mem-ext-distDest
  set mem-ext-distDest remove-item 0 mem-ext-distDest
  set mem-ext-distDest remove-item 0 mem-ext-distDest
  set mem-ext-distDest remove-item 0 mem-ext-distDest
end

; This function is used to initiate the regression analysis for DETect
to run-Regressions
  if length mem-int-speed = regWinL[   ; Is the observation window full? All will be of equal length as they are update concurrently.
    ; Start R and send the latest variable values.
    detect:startR id
    detect:update-var id "int" "myHead" mem-int-heading
    detect:update-var id "int" "speed" mem-int-speed
    detect:update-var id "int" "age" mem-int-age
    
    ;External
    detect:update-var id "ext" "carsNear" mem-ext-cn
    detect:update-var id "ext" "distNear" mem-ext-dn
    detect:update-var id "ext" "carsHead" mem-ext-ch
    detect:update-var id "ext" "carsSpeed" mem-ext-cs
    detect:update-var id "ext" "temperature" mem-ext-temperature
    detect:update-var id "int" "distDest" mem-ext-distDest
    
    ; Run the regression analysis and then stop R.
    detect:run-regress id
    detect:stopR id
    
    ; Retrieve the CUSUM scores for each internal variable. Each value in the CUSUM list contains the maximum CUSUM value for each external variable
    ; being monitored.
    let sCoeff_Cusum detect:report-Cusum id "1"
    let hCoeff_Cusum detect:report-Cusum id "0"
    let agCoeff_Cusum detect:report-Cusum id "2"
    
    ; Next check to see if any of the CUSUMS are above the CUSUM threshold value? If so, set the local belief on feedback (localV) to 1.
    ; A change is remembered for a length of changeMemLen, and this "decays" with each subsequent Regression/CUSUM analysis.
    let trigger threshold-low
    set signChange 0
    
    ifelse (item 0 sCoeff_Cusum >= trigger or item 1 sCoeff_Cusum >= trigger or item 2 sCoeff_Cusum >= trigger 
      or item 3 sCoeff_Cusum >= trigger or item 4 sCoeff_Cusum >= trigger or item 5 sCoeff_Cusum >= trigger) 
    [
      set signChange 1
      set localV 1
      set changeDecay changeMemLen
    ]
    [
      ifelse (item 0 hCoeff_Cusum >= trigger or item 1 hCoeff_Cusum >= trigger or item 2 hCoeff_Cusum >= trigger 
        or item 3 hCoeff_Cusum >= trigger or item 4 hCoeff_Cusum >= trigger or item 5 hCoeff_Cusum >= trigger) 
      [
        set signChange 1
        set localV 1  ;;Set own Emergence belief to be 1 ie. yes
        set changeDecay changeMemLen
      ]
      [
        ifelse (item 0 agCoeff_Cusum >= trigger or item 1 agCoeff_Cusum >= trigger or item 2 agCoeff_Cusum >= trigger 
          or item 3 agCoeff_Cusum >= trigger or item 4 agCoeff_Cusum >= trigger or item 5 agCoeff_Cusum >= trigger) 
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
    ; If no memory of change, set local belief (local V) to 0.  
    if changeDecay = 0 [
      set localV 0
    ]
    ; Here the maximum CUSUM for each relationship for the agent is split into its own variable. This allows the number of agents who detect feedback
    ; for each relationship to be counted and recorded in the logs.
    set coeff-cn-myS_Cusum item 0 sCoeff_Cusum
    set coeff-dn-myS_Cusum item 1 sCoeff_Cusum
    set coeff-ch-myS_Cusum item 2 sCoeff_Cusum
    set coeff-cs-myS_Cusum item 3 sCoeff_Cusum
    set coeff-tm-myS_Cusum item 4 sCoeff_Cusum
    set coeff-dd-myS_Cusum item 5 sCoeff_Cusum
    
    set coeff-cn-myH_Cusum item 0 hCoeff_Cusum
    set coeff-dn-myH_Cusum item 1 hCoeff_Cusum
    set coeff-ch-myH_Cusum item 2 hCoeff_Cusum
    set coeff-cs-myH_Cusum item 3 hCoeff_Cusum
    set coeff-tm-myH_Cusum item 4 hCoeff_Cusum
    set coeff-dd-myH_Cusum item 5 hCoeff_Cusum
    
    set coeff-cn-myAge_Cusum item 0 agCoeff_Cusum
    set coeff-dn-myAge_Cusum item 1 agCoeff_Cusum
    set coeff-ch-myAge_Cusum item 2 agCoeff_Cusum
    set coeff-cs-myAge_Cusum item 3 agCoeff_Cusum
    set coeff-tm-myAge_Cusum item 4 agCoeff_Cusum
    set coeff-dd-myAge_Cusum item 5 agCoeff_Cusum
    
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
  ask one-of cars [
    detect:startR id
    let changeTemp detect:getRandom id
    detect:stopR id
    set global-temperature global-temperature + changeTemp
  ]
end

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; Helper Functions ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
to global-position
  ask junctions [
    set latitude (((ycor - min-pycor) / yscale) + item 2 envelope)
    set longitude (((xcor - min-pxcor) / xscale) + item 0 envelope)
  ]
end

to-report convert-lat [lat]
  let mycor (lat - item 2 envelope) * yscale + min-pycor 
  report mycor
end

to-report convert-long [long]
  let mxcor (long - item 0 envelope) * xscale + min-pxcor
  report mxcor
end

to-report convert-ycor [y]
  let mylat (y - min-pycor) / yscale + ( item 2 envelope)
  report mylat
end

to-report convert-xcor [x]
  let mxLong (x - min-pxcor) / xscale + (item 0 envelope)
  report mxLong
end

to-report cords [lat lon]
  let mycor (lat - item 2 envelope) * yscale + min-pycor 
  let mxcor (lon - item 0 envelope) * xscale + min-pxcor
  let reportList[]
  set reportList lput mxcor reportList
  set reportList lput mycor reportList
  report reportList
end

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;Write Files Procedures;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

to write-to-file-objective
  file-open file-objective
  file-print (word ticks "," global_flowRate "," global_avgSpeed ","  global_congmetric ","   global_conNeighMetric )
  file-close
end 

to write-to-file-comprehensive
  let file-name (word run-name "-" ticks ".csv")
  let meanFM 0
  file-open file-name
  file-print "** Global Statistics **"
  file-print (word "CarsStopped")
  file-print (word globalCarsStopped)
  file-print "**Manhole-Individuals**"
  file-print "**Big Congestion**"
  let manHoleList [who] of manholes with [used? and congMetric < 0.1]
  file-print length manHoleList
  foreach manHoleList [
   let manName ?
   let line (word ? ","  [congMetric] of manhole ?)
   let carPass [memCarName] of manhole ?
   set line (word line "," carPass)
   file-print line
  ]
  file-print "**Neighborhood Congestion**"
  set manHoleList [who] of manholes with [used? and congMetric < 0.1 and congestedNeigh?]
  file-print length manHoleList
  foreach manHoleList [
   let manName ?
   let line (word ? ","  [congMetric] of manhole ?)
   let carPass [memCarName] of manhole ?
   set line (word line "," carPass)
   file-print line
  ]

  file-print "**4. Cars Threshold Low**"
  file-print "**coeff-cn-myS_cusum*"
  let carListHere [who] of cars with [coeff-cn-myS_cusum >=  threshold-low ]
  file-print length carListHere
  foreach carListHere [
    let carName ?
    let line (word ? ","  [coeff-cn-myS_cusum] of car ?)
    let manPass [manHoleMem] of car ?
    set line (word line " : " manPass)
    file-print line
    set line (word ? ","  [coeff-cn-myS_cusum] of car ?)
    let manPassTime [manHoleTimeMem] of car ?
    set line (word line "," manPassTime)
    file-print line
  ]
  file-print "**coeff-dn-myS_cusum*"
  set carListHere [who] of cars with [coeff-dn-myS_cusum >=  threshold-low ]
  file-print length carListHere
  foreach carListHere [
    let carName ?
    let line (word ? ","  [coeff-dn-myS_cusum] of car ?)
    let manPass [manHoleMem] of car ?
    set line (word line " : " manPass)
    file-print line
    set line (word ? ","  [coeff-dn-myS_cusum] of car ?)
    let manPassTime [manHoleTimeMem] of car ?
    set line (word line "," manPassTime)
    file-print line
  ]
  file-print "**coeff-ch-myS_cusum*"
  set carListHere [who] of cars with [coeff-ch-myS_cusum >=  threshold-low ]
  file-print length carListHere
  foreach carListHere [
    let carName ?
    let line (word ? ","  [coeff-ch-myS_cusum] of car ?) 
    let manPass [manHoleMem] of car ?
    set line (word line "," manPass)
    file-print line
    set line (word ? ","  [coeff-ch-myS_cusum] of car ?)
    let manPassTime [manHoleTimeMem] of car ?
    set line (word line "," manPassTime)
    file-print line
  ]
  file-print "**coeff-cs-myS_cusum*"
  set carListHere [who] of cars with [coeff-cs-myS_cusum >=  threshold-low ]
  file-print length carListHere
  foreach carListHere [
    let carName ?
    let line (word ? ","  [coeff-cs-myS_cusum] of car ?) 
    let manPass [manHoleMem] of car ?
    set line (word line "," manPass)
    file-print line
    set line (word ? ","  [coeff-cs-myS_cusum] of car ?)
    let manPassTime [manHoleTimeMem] of car ?
    set line (word line "," manPassTime)
    file-print line
  ]
  file-print "**coeff-tm-myS_cusum*"
  set carListHere [who] of cars with [coeff-tm-myS_cusum >=  threshold-low ]
  file-print length carListHere
  foreach carListHere [
    let carName ?
    let line (word ? ","  [coeff-tm-myS_cusum] of car ?)
    let manPass [manHoleMem] of car ?
    set line (word line "," manPass)
    file-print line
    set line (word ? ","  [coeff-tm-myS_cusum] of car ?)
    let manPassTime [manHoleTimeMem] of car ?
    set line (word line "," manPassTime)
    file-print line
  ]
  file-print "**coeff-dd-myS_cusum*"
  set carListHere [who] of cars with [coeff-dd-myS_cusum >=  threshold-low ]
  file-print length carListHere
  foreach carListHere [
    let carName ?
    let line (word ? ","  [coeff-dd-myS_cusum] of car ?)
    let manPass [manHoleMem] of car ?
    set line (word line "," manPass)
    file-print line
    set line (word ? ","  [coeff-dd-myS_cusum] of car ?)
    let manPassTime [manHoleTimeMem] of car ?
    set line (word line "," manPassTime)
    file-print line
  ]
  file-print "**coeff-cn-myH_cusum*"
  set carListHere [who] of cars with [coeff-cn-myH_cusum >=  threshold-low ]
  file-print length carListHere
  foreach carListHere [
    let carName ?
    let line (word ? ","  [coeff-cn-myH_cusum] of car ?) 
    let manPass [manHoleMem] of car ?
    set line (word line "," manPass)
    file-print line
    set line (word ? ","  [coeff-cn-myH_cusum] of car ?)
    let manPassTime [manHoleTimeMem] of car ?
    set line (word line "," manPassTime)
    file-print line
  ]
  file-print "**coeff-dn-myH_cusum*"
  set carListHere [who] of cars with [coeff-dn-myH_cusum >=  threshold-low ]
  file-print length carListHere
  foreach carListHere [
    let carName ?
    let line (word ? ","  [coeff-dn-myH_cusum] of car ?)
    let manPass [manHoleMem] of car ?
    set line (word line "," manPass)
    file-print line
    set line (word ? ","  [coeff-dn-myH_cusum] of car ?)
    let manPassTime [manHoleTimeMem] of car ?
    set line (word line "," manPassTime)
    file-print line
  ]
  file-print "**coeff-ch-myH_cusum*"
  set carListHere [who] of cars with [coeff-ch-myH_cusum >=  threshold-low ]
  file-print length carListHere
  foreach carListHere [
    let carName ?
    let line (word ? ","  [coeff-ch-myH_cusum] of car ?)
    let manPass [manHoleMem] of car ?
    set line (word line "," manPass)
    file-print line
    set line (word ? ","  [coeff-ch-myH_cusum] of car ?)
    let manPassTime [manHoleTimeMem] of car ?
    set line (word line "," manPassTime)
    file-print line
  ]
  file-print "**coeff-cs-myH_cusum*"
  set carListHere [who] of cars with [coeff-cs-myH_cusum >=  threshold-low ]
  file-print length carListHere
  foreach carListHere [
    let carName ?
    let line (word ? ","  [coeff-cs-myH_cusum] of car ?)
    let manPass [manHoleMem] of car ?
    set line (word line "," manPass)
    file-print line
    set line (word ? ","  [coeff-cs-myH_cusum] of car ?)
    let manPassTime [manHoleTimeMem] of car ?
    set line (word line "," manPassTime)
    file-print line
  ]
  file-print "**coeff-tm-myH_cusum*"
  set carListHere [who] of cars with [coeff-tm-myH_cusum >=  threshold-low ]
  file-print length carListHere
  foreach carListHere [
    let carName ?
    let line (word ? ","  [coeff-tm-myH_cusum] of car ?)
    let manPass [manHoleMem] of car ?
    set line (word line "," manPass)
    file-print line
    set line (word ? ","  [coeff-tm-myH_cusum] of car ?)
    let manPassTime [manHoleTimeMem] of car ?
    set line (word line "," manPassTime)
    file-print line
  ]
  file-print "**coeff-dd-myH_cusum*"
  set carListHere [who] of cars with [coeff-dd-myH_cusum >=  threshold-low ]
  file-print length carListHere
  foreach carListHere [
    let carName ?
    let line (word ? ","  [coeff-dd-myH_cusum] of car ?)
    let manPass [manHoleMem] of car ?
    set line (word line "," manPass)
    file-print line
    set line (word ? ","  [coeff-dd-myH_cusum] of car ?)
    let manPassTime [manHoleTimeMem] of car ?
    set line (word line "," manPassTime)
    file-print line
  ]
  file-print "**coeff-cn-myAge_cusum*"
  set carListHere [who] of cars with [coeff-cn-myAge_cusum >=  threshold-low ]
  file-print length carListHere
  foreach carListHere [
    let carName ?
    let line (word ? ","  [coeff-cn-myAge_cusum] of car ?)
    let manPass [manHoleMem] of car ?
    set line (word line " : " manPass)
    file-print line
    set line (word ? ","  [coeff-cn-myAge_cusum] of car ?)
    let manPassTime [manHoleTimeMem] of car ?
    set line (word line "," manPassTime)
    file-print line
  ]
  file-print "**coeff-dn-myAge_cusum*"
  set carListHere [who] of cars with [coeff-dn-myAge_cusum >=  threshold-low ]
  file-print length carListHere
  foreach carListHere [
    let carName ?
    let line (word ? ","  [coeff-dn-myAge_cusum] of car ?)
    let manPass [manHoleMem] of car ?
    set line (word line " : " manPass)
    file-print line
    set line (word ? ","  [coeff-dn-myAge_cusum] of car ?)
    let manPassTime [manHoleTimeMem] of car ?
    set line (word line "," manPassTime)
    file-print line
  ]
  file-print "**coeff-ch-myAge_cusum*"
  set carListHere [who] of cars with [coeff-ch-myAge_cusum >=  threshold-low ]
  file-print length carListHere
  foreach carListHere [
    let carName ?
    let line (word ? ","  [coeff-ch-myAge_cusum] of car ?) 
    let manPass [manHoleMem] of car ?
    set line (word line "," manPass)
    file-print line
    set line (word ? ","  [coeff-ch-myAge_cusum] of car ?)
    let manPassTime [manHoleTimeMem] of car ?
    set line (word line "," manPassTime)
    file-print line
  ]
  file-print "**coeff-cs-myAge_cusum*"
  set carListHere [who] of cars with [coeff-cs-myAge_cusum >=  threshold-low ]
  file-print length carListHere
  foreach carListHere [
    let carName ?
    let line (word ? ","  [coeff-cs-myAge_cusum] of car ?) 
    let manPass [manHoleMem] of car ?
    set line (word line "," manPass)
    file-print line
    set line (word ? ","  [coeff-cs-myAge_cusum] of car ?)
    let manPassTime [manHoleTimeMem] of car ?
    set line (word line "," manPassTime)
    file-print line
  ]
  file-print "**coeff-tm-myAge_cusum*"
  set carListHere [who] of cars with [coeff-tm-myAge_cusum >=  threshold-low ]
  file-print length carListHere
  foreach carListHere [
    let carName ?
    let line (word ? ","  [coeff-tm-myAge_cusum] of car ?)
    let manPass [manHoleMem] of car ?
    set line (word line "," manPass)
    file-print line
    set line (word ? ","  [coeff-tm-myAge_cusum] of car ?)
    let manPassTime [manHoleTimeMem] of car ?
    set line (word line "," manPassTime)
    file-print line
  ]
  file-print "**coeff-dd-myAge_cusum*"
  set carListHere [who] of cars with [coeff-dd-myAge_cusum >=  threshold-low ]
  file-print length carListHere
  foreach carListHere [
    let carName ?
    let line (word ? ","  [coeff-dd-myAge_cusum] of car ?)
    let manPass [manHoleMem] of car ?
    set line (word line "," manPass)
    file-print line
    set line (word ? ","  [coeff-dd-myAge_cusum] of car ?)
    let manPassTime [manHoleTimeMem] of car ?
    set line (word line "," manPassTime)
    file-print line
  ]
  file-print "**Emergence Belief**"
  set carListHere [who] of cars with [emergeBelief? = true]
  file-print length carListHere
  foreach carListHere [
    let carName ?
    let line (word ? "," [Vp] of car ? ", 0" )
    file-print line
  ]
  file-print "**Postal-Areas**"
  let countFalseCarsL count postCodes with [length carsList-low > 0 and ( length mhListLots = 0 and length mhListNeigh = 0)]
  let countFalsemans count postCodes with [(length carsList-low = 0) and (length mhListLots > 0 or length mhListNeigh > 0)]
  
  file-print (word count postCodes with [length carsList-low > 0] "," count postCodes with [length mhListLots > 0] "," count postCodes with [length mhListNeigh > 0] "," countFalseCarsL "," countFalsemans)


  file-print "**Postal-Areas Cars low Threshold**"
  let postalCodeList [who] of postCodes with [length carsList-low > 0]
  file-print length postalCodeList
  foreach postalCodeList [
   let postName ?
   let line (word [address] of postCode ? ",")
   set line (word line [length carsList-low] of postCode ? ",")
   set line (word line [carsList-low] of postCode ?)
   file-print line
  ]

  file-print "**Postal-Areas Manhole Lots**"
  set postalCodeList [who] of postCodes with [length mhListLots > 0]
  file-print length postalCodeList
  foreach postalCodeList [
   let postName ?
   let line (word [address] of postCode ? ",")
   set line (word line [length mhListLots] of postCode ? ",")
   set line (word line [mhListLots] of postCode ?)
   file-print line
  ]
  file-print "**Postal-Areas Manhole Neighborhood**"
  set postalCodeList [who] of postCodes with [length mhListNeigh > 0]
  file-print length postalCodeList
  foreach postalCodeList [
   let postName ?
   let line (word [address] of postCode ? ",")
   set line (word line [length mhListNeigh] of postCode ? ",")
   set line (word line [mhListNeigh] of postCode ?)
   file-print line
  ]
  file-close
end
@#$#@#$#@
GRAPHICS-WINDOW
250
10
1101
882
420
420
1.0
1
10
1
1
1
0
1
1
1
-420
420
-420
420
0
0
1
ticks
30.0

BUTTON
15
10
135
43
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
15
50
135
83
NIL
update-lights
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
15
90
135
123
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

MONITOR
145
85
202
130
To Do
count cars with [xcor = -300 and ycor = 300]
17
1
11

BUTTON
15
130
132
163
NIL
setup-part2
NIL
1
T
OBSERVER
NIL
NIL
NIL
NIL
1

MONITOR
15
260
147
305
NIL
globalCongestion?
17
1
11

@#$#@#$#@
## WHAT IS IT?

(a general understanding of what the model is trying to show or explain)

## HOW IT WORKS

(what rules the agents use to create the overall behavior of the model)

## HOW TO USE IT

(how to use the model, including a description of each of the items in the Interface tab)

## THINGS TO NOTICE

(suggested things for the user to notice while running the model)

## THINGS TO TRY

(suggested things for the user to try to do (move sliders, switches, etc.) with the model)

## EXTENDING THE MODEL

(suggested things to add or change in the Code tab to make the model more complicated, detailed, accurate, etc.)

## NETLOGO FEATURES

(interesting or unusual features of NetLogo that the model uses, particularly in the Code tab; or where workarounds were needed for missing features)

## RELATED MODELS

(models in the NetLogo Models Library and elsewhere which are of related interest)

## CREDITS AND REFERENCES

(a reference to the model's URL on the web if it has one, as well as any other necessary credits, citations, and links)
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

sheep
false
15
Circle -1 true true 203 65 88
Circle -1 true true 70 65 162
Circle -1 true true 150 105 120
Polygon -7500403 true false 218 120 240 165 255 165 278 120
Circle -7500403 true false 214 72 67
Rectangle -1 true true 164 223 179 298
Polygon -1 true true 45 285 30 285 30 240 15 195 45 210
Circle -1 true true 3 83 150
Rectangle -1 true true 65 221 80 296
Polygon -1 true true 195 285 210 285 210 240 240 210 195 210
Polygon -7500403 true false 276 85 285 105 302 99 294 83
Polygon -7500403 true false 219 85 210 105 193 99 201 83

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

wolf
false
0
Polygon -16777216 true false 253 133 245 131 245 133
Polygon -7500403 true true 2 194 13 197 30 191 38 193 38 205 20 226 20 257 27 265 38 266 40 260 31 253 31 230 60 206 68 198 75 209 66 228 65 243 82 261 84 268 100 267 103 261 77 239 79 231 100 207 98 196 119 201 143 202 160 195 166 210 172 213 173 238 167 251 160 248 154 265 169 264 178 247 186 240 198 260 200 271 217 271 219 262 207 258 195 230 192 198 210 184 227 164 242 144 259 145 284 151 277 141 293 140 299 134 297 127 273 119 270 105
Polygon -7500403 true true -1 195 14 180 36 166 40 153 53 140 82 131 134 133 159 126 188 115 227 108 236 102 238 98 268 86 269 92 281 87 269 103 269 113

x
false
0
Polygon -7500403 true true 270 75 225 30 30 225 75 270
Polygon -7500403 true true 30 75 75 30 270 225 225 270

@#$#@#$#@
NetLogo 5.1.0
@#$#@#$#@
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
