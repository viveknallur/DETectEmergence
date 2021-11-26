extensions[detect]

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;                                                      Define global and agent/turtle based variables
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

globals [
  ; These variables are from the original model, Don't touch
  llenmax
  lcountmax
  cur_llenall
  cur_llenmax
  cur_lcount
  
  stop_edit?
  
  dsx_all_avg
  dsx_all_sum
  
  dsx_all_avg_min
  dsx_all_avg_max
  
  wt_count 
  wt_total
  wt_avg
  wt_avg_max
  wt_avg_min
  
  lml1 lml2 
  bcl1 bcl2
  
  ; probability lists
  problist1 
  problist2 
  problist11
  problist22
  entropy1
  entropy2
  entropy3
  entropy004
  
  favg_entropy1    ; floating average
  favg_entropy2    ; floating average  
  favg_sum1
  favg_sum2
  favg_count1
  favg_count2
  
  dprobs ; list for double probabilities
  probs
  
  probs004
  dprobs004
  
  ; These variables were added for DETect
  
  objM              ; Records the value of the objective measure (objM) of emergence in the system. This is a centralised count. In this model it is the 
                    ; number of patches with no turtles within 2 patches from its center.
  changPer          ; The percentage of agents who have detected a change using CUSUM recently.
  emPercent         ; The percentage of agents who believe there is an emergent event.
  lanePercent       ; The percentage of agents who are in lanes
  
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

turtles-own[ 
  ; These variables are from the original model
  dir col cur_speed orig_speed lane_calculated? in_lane? obstacle? accelerating? decelerating? last_speed last_heading dh dv1 dv2 ds lastds lastx lasty dsx speed_measurement? LM_calculated?
  l_member?
  cw  
  
  ; DETECT variables ;
  ;;;;;;;;;;;;;;;;;;Internal Variables
  myHeading           ; The direction the agent is facing
  mySpeed             ; The agents speed....remains constant
  age                 ; The agents speed....remains constant
  height              ; The agents speed....remains constant
  weight              ; The agents speed....remains constant
  ; The smoothing averaging windows for each variable
  speed-mem
  heading-mem
  age-mem
  height-mem
  weight-mem
  ; The sliding observation windows for each variable
  mem-int-speed
  mem-int-heading
  mem-int-age
  mem-int-height
  mem-int-weight
  
  ;;;;;;;;;;;;;;;;;;External Variables
  flockHead        ; Average heading of nearby pedestrians
  flockSpeed       ; Average speed of nearby pedestrians
  distNear         ; Distance to the nearest pedestrian
  flockCount       ; Number of nearby pedestrians
  temperature      ; The current temperature
  ; The smoothing averaging windows for each variable
  persons-near-mem
  dist-near-person-mem
  flock-head-mem
  flock-speed-mem
  temperature-mem
  ; The sliding observation windows for each variable
  mem-ext-bn
  mem-ext-dn
  mem-ext-fh
  mem-ext-fs
  mem-ext-temperature
   ; These store the CUSUM values for each internal-external variable pair.
  ; First my speed against all external variables
  coeff-pn-myS_cusum    ; persons near (count)
  coeff-dn-myS_cusum    ; distance to nearest
  coeff-fh-myS_cusum    ; heading
  coeff-fs-myS_cusum    ; speed
  coeff-tm-myS_cusum    ; temperature
  ; My Heading
  coeff-pn-myH_cusum
  coeff-dn-myH_cusum 
  coeff-fh-myH_cusum
  coeff-fs-myH_cusum
  coeff-tm-myH_cusum
  ; My age
  coeff-pn-myAge_cusum
  coeff-dn-myAge_cusum
  coeff-fh-myAge_cusum
  coeff-fs-myAge_cusum
  coeff-tm-myAge_cusum
  ; My Weight
  coeff-pn-myWeight_cusum
  coeff-dn-myWeight_cusum
  coeff-fh-myWeight_cusum
  coeff-fs-myWeight_cusum
  coeff-tm-myWeight_cusum
  ; My height
  coeff-pn-myHeight_cusum
  coeff-dn-myHeight_cusum
  coeff-fh-myHeight_cusum
  coeff-fs-myHeight_cusum
  coeff-tm-myHeight_cusum
  
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
]
; Patches properties, these were already in the model.
patches-own[ oc  random_walk_passed1? random_walk_passed2?]   

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;                              Pre-existing functions that were in the model before edit for DETect
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
to setup
  clear-all
  reset-ticks
  set lml1 [] set lml2 [] 
  set bcl1 [] set bcl2 []
  set probs [
    0.009981697
    0.020093178
    0.019938436
    0.019657238
    0.020267887
    0.02052579
    0.020447587
    0.020381032
    0.019865225
    0.020129784
    0.028742097
    0.03752579
    0.037099834
    0.037462562
    0.037396007
    0.037806988
    0.038049917
    0.038011647
    0.038088186
    0.037482529
    0.031735441
    0.025384359
    0.025432612
    0.025660566
    0.025895175
    0.026427621
    0.02602995
    0.026118136
    0.026143095
    0.025951747
    0.018209651
    0.010740433
    0.010760399
    0.01115807
    0.010900166
    0.011242928
    0.011109817
    0.01093178
    0.010918469
    0.010966722
    0.007361065
    0.003675541
    0.003517471
    0.003732113
    0.00374376
    0.003950083
    0.003905158
    0.003903494
    0.003898502
    0.003790349
    0.002381032
    0.00122629
    0.001144759
    0.001111481
    0.001139767
    0.001249584
    0.0011797
    0.001168053
    0.001202995
    0.001246256
    0.000758735
    0.000311148
    0.000336106
    0.000289517
    0.000299501
    0.000339434
    0.000336106
    0.000366057
    0.000399334
    0.000357737
    0.000216306
    5.15807E-05
    8.48586E-05
    4.6589E-05
    4.99168E-05
    6.65557E-05
    8.81864E-05
    7.65391E-05
    9.65058E-05
    7.8203E-05
    4.49251E-05
    1.83028E-05
    1.66389E-05
    1.33111E-05
    6.65557E-06
    4.99168E-06
    6.65557E-06
    6.65557E-06
    1.16473E-05
    9.98336E-06
    3.32779E-06
    0
    1.66389E-06
    1.66389E-06
    1.66389E-06
    0
    0
    0
    1.66389E-06
    3.32779E-06  
  ]
  
  ; list of 100 items, the quicker solution then cycle
  set dprobs [
    0 0 0 0 0 0 0 0 0 0 ; 1 
    0 0 0 0 0 0 0 0 0 0 ; 2
    0 0 0 0 0 0 0 0 0 0 ; 3
    0 0 0 0 0 0 0 0 0 0 ; 4
    0 0 0 0 0 0 0 0 0 0 ; 5
    0 0 0 0 0 0 0 0 0 0 ; 6
    0 0 0 0 0 0 0 0 0 0 ; 7
    0 0 0 0 0 0 0 0 0 0 ; 8
    0 0 0 0 0 0 0 0 0 0 ; 9
    0 0 0 0 0 0 0 0 0 0 ; 10
  ]
  
  set dprobs004 [
    0 0 0 0
    0 0 0 0
    0 0 0 0
    0 0 0 0
  ]
  
  set probs004 [
    0.207548173
    0.209091362
    0.207561462
    0.212340532
    0.039400332
    0.039807309
    0.039780731
    0.04057309
    0.000921927
    0.000958472
    0.000973422
    0.001034884
    3.32226E-06
    1.66113E-06
    0
    3.32226E-06
  ]
  
  
  
  ;----------------------------------------------------------------
  ; environment split 100 segments (10 x 10) of areas 4 x 4 patches
  
  ;observer> show lml1
  ;observer: [0       1      2     3    4    5   6 7]
  ;observer> show lml2
  ;observer: [1674956 254040 59908 9880 1245 132 5 1]
  set problist11[
    0.837408076
    0.127009395
    0.029951499
    0.004939588
    0.000622448
    6.59945E-05
    2.49979E-06
    4.99958E-07
  ]
  
  
  ;observer> show bcl1
  ;observer: [0       1      2      3     4    5   6  9 8 7]
  ;observer> show bcl2
  ;observer: [1093910 663932 197750 38383 5511 620 51 1 1 8]
  set problist22 [
    0.546909333
    0.331938283
    0.098866745
    0.019189898
    0.00275527
    0.000309974
    2.54979E-05
    3.99967E-06
    4.99958E-07
    4.99958E-07
  ]
  
  
  ;----------------------------------------------------------------
  ; environment split to 16 segments (4 x 4) of areas 10 x 10 patches
  
  ;observer: [0      2     7    4     1     3     6    5    8   11 9   10 12]
  ;observer> show lml2
  ;observer: [132012 49868 1414 15930 79276 29319 3562 7908 504 16 169 37 1]
  set problist1 [
    0.412516874
    0.247725114
    0.155829709
    0.091617294
    0.049778761
    0.024711264
    0.011130693
    0.004418529
    0.001574921
    0.000528099
    0.000115619
    4.99975E-05
    3.12484E-06
  ]
  
  ;observer: [2     1     0    8    7     5     4     3     6     9    10  11  12 13]
  ;observer> show bcl2
  ;observer: [52599 26613 6592 6647 15303 47442 64332 67399 29380 2557 841 235 59 17]
  set problist2 [
    0.02059897
    0.083161467
    0.164363657
    0.210611344
    0.201027449
    0.148248838
    0.09180791
    0.047819484
    0.020770836
    0.007990225
    0.002627994
    0.000734338
    0.000184366
    5.31223E-05
  ]
  
  set entropy1 0
  set entropy2 0  
  set favg_entropy1 0
  set favg_entropy2 0  
  set favg_sum1 0    
  set favg_sum2 0    
  set favg_count1 0  
  set favg_count2 0  
  
  ask patches [set random_walk_passed1? false set random_walk_passed2? false]
  ;file-open "patch_statistics.txt"
  ; 
  
  set red_blue_ratio 0.5
  
  crt population [
    set size t_size
    set shape "circle"
    set orig_speed min_speed + random (max_speed - min_speed) 
    move-to one-of patches with [count turtles-here = 0]
  ]
  ; decision about walker's direction
  ask n-of (population * red_blue_ratio) turtles [set dir 90 set col blue]
  ask turtles with [col != blue][set dir 270 set col red]
  ; other walkers settings 
  ask turtles [
    set heading dir 
    set color col
    reset_turtle_other_settings
  ]
  set llenmax 0
  set lcountmax 0
  set dsx_all_avg 0
  set dsx_all_sum 0 
  set dsx_all_avg_min 1000
  set dsx_all_avg_max 0 
  set stop_edit? false
  
  
  set wt_count  0
  set wt_total 0
  set wt_avg 0
  set wt_avg_max 0
  set wt_avg_min 1000
  
  file-close-all
  
  ; EMERGENCE SET UP
  set show_blue_lanes? true
  set show_red_lanes? true
  set min_lane_len 15
  
  set random_walk? true
  ;
  setup-globals
  ;let fileName (word "entropy_measurement-" runName ".csv") 
  ;if file-exists? fileName [file-delete fileName]
  ;file-open "entropy_measurement.csv"
end

to reset_turtle_other_settings
  set cur_speed orig_speed
  set last_speed cur_speed
  set last_heading dir
  set obstacle? false
  set accelerating? false
  set decelerating? false    
  set lastx xcor set lasty ycor
  set dh 0 set dv1 0 set dv2 0 set ds 0 ; heading, speed, trajetrody differential (one tick change)
  set lastds 0  
  set speed_measurement? false
  set l_member? false
end

to update_turtle_statistics ; turtle's procedure
  set dh abs(heading - last_heading) set last_heading heading
  set dv1 (cur_speed - last_speed) set last_speed cur_speed 
  let difx abs(xcor - lastx) let dify abs(ycor - lasty) set lastx xcor set lasty ycor
  set ds (sqrt((difx * difx) + (dify * dify)))
  set dv2 (ds - lastds) ; speed diferential as dif of trajectory difs
  set accelerating? (dv2 > 0) set decelerating? (dv2 < 0)
  set lastds ds
  set dsx difx; trajectory in direction of x coordinate
end

to insert_obstacle
  if stop_edit? [set stop_edit? false stop]
  if mouse-down? [
    ask patch mouse-xcor mouse-ycor [
      ask patches in-radius onstacle_radius [
        sprout 1 [
          set color yellow
          set size 1
          set shape "square"
          reset_turtle_other_settings
          set obstacle? true
        ]
      ]
    ]
  ]
end

to counter_flow
  ask turtles with [dir != 90][set dir 270]
end

to crossing_flow
  ask turtles with [dir != 90][set dir 180]
end

;to update_oc[a u d] ; turtle's procedure
;  ask patch-set patch-at-heading-and-distance (dir - a) d [set oc oc - u]
;  if a != 0 [ask patch-set patch-at-heading-and-distance (dir + a) d [set oc oc - u] ]
;end

to-report updated_oc[_oc t] ; patch function, t is turtle
  
  if self = [patch-here] of t [report oc] ; patch of the caller walker
  
  let rep 0
  let ocupd 0
  
  ; angel diference from ideal direction
  let p self
  let htp 0
  ask t [set htp towards p]
  let a_dif abs(subtract-headings htp ([dir] of t))
  
  ; There are several possible methods how to calculate occupation update,
  ; currently used is logaritmic function
  
  ; Discrete scale with angle intervales 
  ; ifelse a_dif = 0 [set ocupd b0][
  ; ifelse a_dif <= 10 [set ocupd b10][
  ; ifelse a_dif <= 45 [set ocupd b45][
  ; if a_dif <= 90 [set ocupd b90]]]]
  
  ; Hyperbolic function with don't go backward condition
  ; if a_dif < 90 and (a_dif + b0 != 0)[set ocupd ( b0 / (a_dif + b0) )]]
  
  ; Logaritmic function (it reverse the movement direction)
  if a_dif / ocupd_koef  >= adif_max [set a_dif adif_max * ocupd_koef - 1] ; -1 to avoid log 0
                                                                           ;  set ocupd ocupd_koef * log ( 1 / (1 + adif_max - a_dif) ) 10 ; 
  set ocupd ocupd_koef * log (adif_max - a_dif / ocupd_koef ) 10 ; 90
  
  set rep (_oc - ocupd)
  
  ifelse show_individual_update? [
    set plabel precision rep 1
    set plabel-color brown
  ][set plabel ""]
  
  report rep
end

to measure_speed 
  ask turtles with [not speed_measurement? and xcor > -5 and xcor < 5][
    set speed_measurement? true
  ]
  ask turtles with [speed_measurement? and (xcor > 10 or xcor < -10)][  
    set speed_measurement? false
    set wt_count wt_count + 1   
  ]    
  if ticks mod avg_dsx_ticks = 0 [
    set wt_avg wt_count / avg_dsx_ticks
    set wt_total wt_total + wt_count
    set wt_count 0   
    if wt_avg > wt_avg_max [set wt_avg_max wt_avg]   
    if wt_avg < wt_avg_min [set wt_avg_min wt_avg]   
  ]
end

to show_patches_oc 
  ask turtles [
    set size 1
  ]
  let min_oc min [oc] of patches
  let max_oc max [oc] of patches 
  ask patches [
    set plabel-color black
    set plabel precision oc 1
    set pcolor scale-color green oc  max_oc min_oc
  ] end

to speed_up set cur_speed cur_speed + speed_inc if cur_speed > max_speed [set cur_speed max_speed] end  ;orig_speed is the individual max speed
to speed_down set cur_speed cur_speed - speed_dec if cur_speed < min_speed [set cur_speed min_speed] end

to guided_walk
  ask patches [set oc 0]
  ask turtles [
    ;    set shape "default" 
    ask other patches in-radius information_radius [set oc oc + (information_radius - distance myself) * oc_multi_koef]
  ]
  ; ifelse show_patches_oc? [show_patches_oc][ask patches with [pcolor != black][set pcolor black set plabel ""]]
  
  ask turtles with [not obstacle?][
    let me self
    let new_patch min-one-of patches in-radius decision_radius with [not any? other turtles-here][updated_oc oc me]
    if new_patch != nobody [
      face new_patch
      let coliding_turtle one-of other turtles in-cone colision_angle cur_speed
      ifelse control_speed? [
        ifelse coliding_turtle = nobody [
          speed_up         
        ][
        set cur_speed [cur_speed] of coliding_turtle
        speed_down
        ]
        fd cur_speed
      ][
      if coliding_turtle = nobody [set cur_speed orig_speed fd cur_speed]
      ]     
    ]
    update_turtle_statistics
  ]  
end

to random_walk
  ask turtles with [not obstacle?][
    set shape "default"
    rt random 20 - random 20 
    fd orig_speed
  ]
  if micro_level_entropy? [
    ; recalculate patches occupation 
    ask patches [set oc 0]
    ask turtles [ask other patches in-radius information_radius [set oc oc + (information_radius - distance myself) * oc_multi_koef]]
  ]
end

to find_lane_formations[c]
  let toi turtles with [not obstacle? and col = c] ; turtles of interest
  ask toi [
    set color col
    set lane_calculated? false 
    set in_lane? false
  ]
  let x min-pxcor
  while[x <= max-pxcor][
    ask toi with [([pxcor] of patch-here = x) and (not lane_calculated?)][
      set lane_calculated? true
      find_lane
    ]
    set x x + 1
  ]
  ask toi with [in_lane?][set shape "default"]
end

to find_lane ; turtle procedurer
  no-display
  let lane_as turtle-set self
  set lane_calculated? true
  let next_tl get_next_tl self
  while[next_tl != nobody][
    set lane_as (turtle-set lane_as next_tl)
    ask next_tl [set next_tl (get_next_tl next_tl)]
  ]
  let llen count lane_as
  if lane_as != nobody [
    if (llen >= min_lane_len) [
      ; this is an identified lane
      ask lane_as [set in_lane? true]
      if llen > cur_llenmax [set cur_llenmax llen]
      if llen > llenmax [set llenmax llen]
      set cur_lcount cur_lcount + 1
      if cur_lcount > lcountmax [ set lcountmax cur_lcount ]
    ]
  ]
  display
end

to-report get_next_tl[t]
  let h heading
  set heading dir
  let rep nobody
  let possible_nlt other turtles in-cone le_max_dist lane_pair_cone_angle with [
    (col = [col] of t) and ; the same color
    ( 
      (col = blue and xcor > [xcor] of t) or    ; borders check for blue
      (col = red and xcor < [xcor] of t) 
      )    
  ]
  if (possible_nlt != nobody) [
    ask possible_nlt [set lane_calculated? true]
    set rep min-one-of possible_nlt with [not oposite_turtles_between? t][distance t]
  ]
  set heading h
  report rep
end

to-report value_or_min [x minx]
  report ifelse-value (x >= minx)[x][minx]   
end

to-report oposite_turtles_between?[t] ; turtle's procedure
  let h heading
  face t
  let dist value_or_min((distance t) - 1) 0
  let result (0 < count turtles in-cone dist walker_in_between_angle with [col != [col] of t])
  set heading h
  report result
end

; returtns [j,i] square patchset from the environment divided to segments of squares with edge m  
; shwn show? is true the square is highlighted 
to-report get_square_patchset[i j m show?]  
  let ret patches with [
    pxcor >= (min-pxcor + i * m) and 
    pxcor < (min-pxcor + (i + 1) * m) and 
    pycor >= (min-pycor + j * m) and 
    pycor < (min-pycor + (j + 1) * m)]
  if show? [
    let c green + random 5
    ask ret [set pcolor c] 
  ]
  report ret
end

to-report similar_heading?[h]
  report abs(subtract-headings heading h) < 45
end

to-report is_lane_turtle?[t]
  let p1? (any? other turtles in-cone le_max_dist lane_pair_cone_angle with [col = [col] of t and not oposite_turtles_between? t])
  rt 180
  let p2? (any? other turtles in-cone le_max_dist lane_pair_cone_angle with [col = [col] of t and not oposite_turtles_between? t]) 
  rt 180
  report p1? and p2?  
end


to test_LM
  clear-all
  let P nobody
  ask patch ((min-pxcor + max-pxcor) / 2) ((min-pycor + max-pycor) / 2) [
    sprout 1 [
      set color blue
      set P self 
      set shape "default"
      set label who
    ] 
  ]
  
  let U nobody
  
  ask one-of turtles [
    ask patch-here [
      ask n-of 3 other patches with [distance P < le_max_dist][
        sprout 1 [
          set color blue
          set shape "default"
          set label who
        ]    
      ]
    ] 
    set U other turtles in-radius 7
  ]
  
  ask U [set color cyan]
  
  let plist sort U
  show plist
  show LMl P plist 120
  
end

to-report LMl[p plist min_dif]
  no-display
  
  let ret false
  let c length plist
  let j 0
  
  let mp1 nobody let mp2 nobody
  let max_dif 0 ; min value as init value
  let dif 0
  let h [heading] of p
  let continue? true
  let o nobody
  
  while[j < (c - 1)][
    let p1 item j plist
    
    ask p [
      face p1
      set o min-one-of other turtles in-cone distance p1 walker_in_between_angle [distance p]
      set continue? ((o = nobody) or (o = p1))  
      ;show word o word "," word p1 word "," continue? 
    ]
    
    if continue? [
      let i (j + 1)   
      while[i < c][
        let p2 item i plist
        
        ask p [
          face p2
          set o min-one-of other turtles in-cone distance p2 walker_in_between_angle [distance p]
          set continue? (o = nobody or o = p2)   
        ]
        
        if continue? [
          ask p [set dif abs(subtract-headings towards p1 towards p2)]
          ;print word i word "," word j word "," word p1 word "," word p2 word "," dif
          if dif > min_dif and dif > max_dif [set max_dif dif set mp1 p1 set mp2 p2]   
        ]
        
        set i i + 1
      ]        
    ]
    
    set j j + 1
  ] 
  
  if (mp1 != nobody) and (mp2 != nobody) [
    set ret true
    ;ask (turtle-set mp1 mp2) [set color red]
  ]
  
  ask p [set heading h]
  display 
  
  report ret   
end

; Lane Membe rship function
to-report is_lane_member?[rmin rmax] ; turtle's function
  let ret 0
  
  let s self
  
  let U other turtles 
  in-radius rmax ; round neighbourhood 
  with [distance s >= rmin ; not round but ring neighbourhood
    and col = [col] of s] ; pedestrians with the same primary direction
  
                          ; reset calculation flag
  ask U [set LM_calculated? false]
  
  ; example:
  ; for 4 pedestrians in U we need the following x values
  ; it is a triangle matrix
  ;   1 2 3 4
  ; 1 0 x x x
  ; 2   0 x x
  ; 3     0 x
  ; 4       0
  ;
  ; stands for possible lane elements:
  ;  1 s 2
  ;  1 s 3
  ;  1 s 4
  ;  2 s 3
  ;  2 s 4
  ;  3 s 4  
  
  ;cycle   
  ;1 2-4  
  ;2 3-4
  ;3 4-4
  
  let plist sort U ; agentset as list sorted by who
  report LMl self plist 120
end


to check_lane_membersip   
  ask turtles with [col = blue][set l_member? is_lane_member? le_min_dist  le_max_dist]
  ;  ask turtles with [l_member?][set shape "circle"]
end


to count_lane_members_on_square_areas_10_4[visualize?]
  if visualize? [ask patches [set plabel ""]]
  let ent 0
  let ent2 0
  let ctotal sum lml2 ; totsl count  
  ask turtles with [shape = "square"][set shape "default"]
  let j 0
  let i 0
  no-display
  while [j < 10][
    while[i < 10][
      let ps get_square_patchset j i 4 false 
      
      let as (turtles-on ps) with [col = blue]       
      let bpc count as
      
      let lm as with [is_lane_member? le_min_dist le_max_dist]
      let lmc count lm 
      
      
      if visualize? [
        ask lm [set shape "square"]
        let cc ifelse-value ((i mod 2 = 0 and j mod 2 = 0) or ( (i + 1) mod 2 = 0 and (j + 1) mod 2 = 0) ) [green + 4][green + 2]
        ask ps [set pcolor cc]
        ask min-one-of ps [pxcor + pycor][ask patch-at 0 1 [set plabel-color black set plabel lmc]]
      ]
      
      let posb position lmc lml1 
      let posbc position bpc bcl1 
      
      if collect_probabilities? [
        ifelse posb = false [set lml1 lput lmc lml1 set lml2 lput 1 lml2 set posb (length lml1 - 1)][set lml2 replace-item posb lml2 ((item posb lml2) + 1) ]
        ifelse posbc = false [set bcl1 lput bpc bcl1 set bcl2 lput 1 bcl2 set posbc (length bcl1 - 1)][set bcl2 replace-item posbc bcl2 ((item posbc bcl2) + 1) ]
      ]
      
      ; total sum of all counts occurences 
      let totalc sum lml2   
      
      if calculate_entropy? [ 
        ; entropy from precalculated probabilities - based on lane membersip
        if (length problist11 > lmc) [ ; exists item of the given index (bc count) in the list?
          let p item lmc problist11 ; find the probability for this count
          if p > 0 [set ent (ent + p * (log p 2) )]
        ]
        
        ; entropy from precalculated probabilities - based on count of pedestrians in square
        if (length problist22 > bpc) [ ; exists item of the given index (bc count) in the list?
          let pp item bpc problist22 ; find the probability for this count
          if pp > 0 [set ent2 (ent2 + pp * (log pp 2) )]
        ]                
        
      ]
      
      set i i + 1
    ]
    set i 0
    set j j + 1
  ]
  
  if calculate_entropy? [
    set entropy1 -1 * ent
    set entropy2 -1 * ent2
    
    set favg_sum1 favg_sum1 + entropy1
    set favg_count1 favg_count1 + 1
    
    set favg_sum2 favg_sum2 + entropy2
    set favg_count2 favg_count2 + 1
    
    if favg_count1 = favg_step [
      set favg_entropy1 favg_sum1 / favg_step  
      set favg_count1 0 
      set favg_sum1 0
      
      set favg_entropy2 favg_sum2 / favg_step  
      set favg_count2 0 
      set favg_sum2 0      
    ]
    
    ;    file-print word ticks word ";" word entropy1 word ";" word favg_entropy1 word ";" cur_lcount
    ;    file-print word ticks word ";" word entropy1 word ";" word favg_entropy1 word ";" cur_lcount
    
  ]
  
  display
  
  save_statistics_in_file  
  
end


to count_lane_members_on_square_areas_4_10[visualize?]
  if visualize? [ask patches [set plabel ""]]
  let ent 0
  let ent2 0
  let ctotal sum lml2 ; totsl count  
  ask turtles with [shape = "square"][set shape "circle 2"]
  let j 0
  let i 0
  no-display
  while [j < 4][
    
    while[i < 4][
      let ps get_square_patchset j i 10 false 
      
      let as (turtles-on ps) with [col = blue]       
      let bpc count as
      
      let lm as with [is_lane_member? le_min_dist le_max_dist]
      let lmc count lm 
      
      if visualize? [
        ask lm [set shape "square"]
        let cc ifelse-value ((i mod 2 = 0 and j mod 2 = 0) or ( (i + 1) mod 2 = 0 and (j + 1) mod 2 = 0) ) [green + 4][green + 2]
        ask ps [set pcolor cc]
        ask min-one-of ps [pxcor + pycor][ask patch-at 0 1 [set plabel-color black set plabel lmc]]
      ]
      
      let posb position lmc lml1 
      let posbc position bpc bcl1 
      
      if collect_probabilities? [
        ifelse posb = false [set lml1 lput lmc lml1 set lml2 lput 1 lml2 set posb (length lml1 - 1)][set lml2 replace-item posb lml2 ((item posb lml2) + 1) ]
        ifelse posbc = false [set bcl1 lput bpc bcl1 set bcl2 lput 1 bcl2 set posbc (length bcl1 - 1)][set bcl2 replace-item posbc bcl2 ((item posbc bcl2) + 1) ]
      ]
      
      ; total sum of all counts occurences 
      let totalc sum lml2   
      
      if calculate_entropy? [ 
        ; entropy from precalculated probabilities - based on lane membersip
        if (length problist1 > lmc) [ ; exists item of the given index (bc count) in the list?
          let p item lmc problist1 ; find the probability for this count
          if p > 0 [set ent (ent + p * (log p 2) )]
        ]
        
        ; entropy from precalculated probabilities - based on count of pedestrians in square
        if (length problist2 > bpc) [ ; exists item of the given index (bc count) in the list?
          let pp item bpc problist2 ; find the probability for this count
          if pp > 0 [set ent2 (ent2 + pp * (log pp 2) )]
        ]                
        
      ]
      
      set i i + 1
    ]
    set i 0
    set j j + 1
  ]
  
  if calculate_entropy? [
    set entropy1 -1 * ent
    set entropy2 -1 * ent2
    
    set favg_sum1 favg_sum1 + entropy1
    set favg_count1 favg_count1 + 1
    
    set favg_sum2 favg_sum2 + entropy2
    set favg_count2 favg_count2 + 1
    
    if favg_count1 = favg_step [
      set favg_entropy1 favg_sum1 / favg_step  
      set favg_count1 0 
      set favg_sum1 0
      
      set favg_entropy2 favg_sum2 / favg_step  
      set favg_count2 0 
      set favg_sum2 0      
    ]
    
    
  ]
  
  save_statistics_in_file
  
  display
  
end

to save_statistics_in_file 
  ;file-print word ticks word ";" word entropy1 word ";" word favg_entropy1 word ";" word entropy2 word ";" word favg_entropy2 word "," cur_lcount
end

to calculate_micro_level_entropy
  
end

to-report crowd_part_patch? ; patch function
  let ps (patch-set self patch-at 1 0 patch-at 1 1 patch-at 0 1)
  if ((count turtles-on ps) > 0)[report true]
end

to measure001
  ask turtles [
    let c get_coworker_distance_category                                                                                                              
    let h get_heading_category
    let index c * 10 + h ; transform two indexes to one index 0 - 99 (they are both 0 - 9 so we can produce two digit number from them)
    let curval item index dprobs   
    set dprobs replace-item index dprobs (curval + 1) 
  ]    
end

to-report get_coworker_distance_category ; turtle's function
  let me self 
  ; reports 0 - 9, 9 is max also when distance is bigger
  let rep 9
  ask min-one-of other turtles with [col = [col] of me][distance me][
    set rep floor (distance me)
    set rep ifelse-value(rep > 10)[9][rep]
  ]
  report rep
end

to-report get_heading_category ; turtle's function 
                               ; 0 - 360 transfromed to categories 0 - 9 as folllowing: 0 - 36 -> 0, 36 - 72 -> 2 .... 324 - 360 -> 9
  report round (heading / 36)    
end

to measure_entropy3
  let ent 0
  ask turtles [
    let c get_coworker_distance_category                                                                                                              
    let h get_heading_category
    let index c * 10 + h ; transform two indexes to one index 0 - 99 (they are both 0 - 9 so we can produce two digit number from them)
    let p item index probs   
    if p > 0 [set ent (ent + p * (log p 2) )]
  ]
  set entropy3 -1 * ent  
end

;------------------------------------------------------------------------------------------
to measure_entropy004
  let ent 0
  ask turtles [
    let c get_cowolker_distance_cat004                                                                                                              
    let h get_cowalker_heading_dif_cat004
    let index c * 4 + h ; transform two indexes to one index 0 - 99 (they are both 0 - 9 so we can produce two digit number from them)
    let p item index probs004   
    if p > 0 [set ent (ent + p * (log p 2) )]
  ]
  set entropy004 -1 * ent  
end

to measure004
  ask turtles [
    let c get_cowolker_distance_cat004                                                                                                              
    let h get_cowalker_heading_dif_cat004
    ;show word c word "," h
    let index c * 4 + h ; transform two indexes to one index 0 - 99 (they are both 0 - 9 so we can produce two digit number from them)
    let curval item index dprobs004   
    set dprobs004 replace-item index dprobs004 (curval + 1) 
  ]      
end

to-report get_cowolker_distance_cat004 ; turtle's function
  let me self 
  ; reports 0 - 9, 9 is max also when distance is bigger
  let rep 3
  ask min-one-of other turtles with [col = [col] of me][distance me][
    set rep floor (distance me / 3) ; 0 - 3 -> 0; 3 - 6 -> 1; 6 - 9 -> 2; 9 - 12 -> 3 
    set rep ifelse-value(rep > 3)[3][rep]
    let mee self
    ask me [set cw mee]
  ]
  report rep
end

to-report get_cowalker_heading_dif_cat004
  let hd abs(subtract-headings heading [heading] of cw)
  ; possible values 0 - 180; 0 - 45 -> 0, 45 - 90 -> 1; 90 - 135 -> 2; 135 - 180 -> 3
  report ifelse-value (hd = 180)[3][floor (hd / 45)]
end

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; This go function is what is called every timestep. It is used to prompt the agents to either random walk or walk in counter flow.
; relevant DETect associated functionality like choosing variables, running regression and gossiping.
to go
  if ticks mod 10000 = 0 and ticks > 0 [set random_walk? not random_walk?] ; change style every 10k ticks
  
  ifelse random_walk? [random_walk][guided_walk]
  
  if ticks mod 10 = 0 [
    measure_entropy004
  ] 
  
  
  if ticks mod 10 = 0 [
    ifelse _10_4? [count_lane_members_on_square_areas_10_4 false][count_lane_members_on_square_areas_4_10 false]
    ;    
    if micro_level_entropy? [ calculate_micro_level_entropy ]
    
  ]
  
  if show_blue_lanes? or show_red_lanes? [
    set cur_llenmax 0
    set cur_lcount 0
    ask turtles with [shape = "circle"][set shape "default"]
    if show_blue_lanes? [find_lane_formations blue]
    ;   if show_red_lanes? [find_lane_formations red]
  ]     
  
  ;;EMERGENCE & DETect
  
  set scaler 0.05
  if ticks mod 100 = 0 [
    print (word run-name " is at " ticks) 
  ]
  
  if ticks = 20000 [
    print "Printing Vars"
    detect:printVariables
  ]
  ask turtles [get-Int-Variables]
  ask turtles [get-Ext-Variables]
  ask turtles [fill-memory-em]
  
  ask turtles [
    ifelse relsChosen? [
      run-Regressions
      
    ][
    choose-variables
    
    ]
  ]
  swap-neigh
  
  if ticks > 0 and ticks mod 50 = 0 [
    cal-globals
    write-to-file-comprehensive
  ]
  
  if ticks > 0 and ticks mod 20 = 0 [
    update-temp 
  ]
  
  ;;END EMERGENCE
  tick
  
end

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;                                         DETect procedures
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

; This updates the internal variables of the agent
to get-Int-Variables
  set myHeading heading
  set mySpeed cur_speed
end

; This calculates and updates the external variables of the agent
to get-Ext-Variables
  set flockHead mean [myHeading] of other turtles in-radius le_max_dist
  set flockSpeed mean [mySpeed] of other turtles in-radius le_max_dist
  set distNear distance min-one-of other turtles in-radius le_max_dist [distance myself]
  set flockCount count other turtles in-radius le_max_dist
  set temperature global-temperature
end

; The observations for all internal and external variables are stored in list/arrays. This function
; adds the observations calculated in the above 2 procedures the relevant short term list "varName-mem". If that is of length 5
; it is averaged and the result added to the relevant long term sliding observation window "mem-int-varName" for internal
; or "mem-ext-varName" for external.
to fill-memory-em
  ;; Internal Variables  
  set heading-mem lput(myHeading) heading-mem
  set speed-mem lput (mySpeed) speed-mem
  set age-mem lput (age) age-mem
  set height-mem lput (height) height-mem 
  set weight-mem lput (weight ) weight-mem 
  
  ;; External Variables
  set persons-near-mem lput (flockCount) persons-near-mem 
  set dist-near-person-mem lput (distNear) dist-near-person-mem 
  set flock-head-mem lput (flockHead ) flock-head-mem 
  set flock-speed-mem lput (flockSpeed) flock-speed-mem 
  set temperature-mem lput (temperature) temperature-mem 
  
  ;; Every 5 we aggregate by calculating the mean
  if length heading-mem = smoothL [
    set mem-int-speed lput mean speed-mem mem-int-speed
    set mem-int-heading lput mean heading-mem mem-int-heading
    set mem-int-age lput mean age-mem mem-int-age
    set mem-int-height lput mean height-mem mem-int-height
    set mem-int-weight lput mean weight-mem mem-int-weight
    
    set mem-ext-bn lput mean persons-near-mem mem-ext-bn
    set mem-ext-dn lput mean dist-near-person-mem mem-ext-dn
    set mem-ext-fh lput mean flock-head-mem mem-ext-fh
    set mem-ext-fs lput mean flock-speed-mem mem-ext-fs
    set mem-ext-temperature lput mean temperature-mem mem-ext-temperature
    
    ;; Now clear memory
    ;; Internal
    set heading-mem[]
    set speed-mem[]
    set age-mem[]
    set height-mem[]
    set weight-mem[]
    
    ; External
    set persons-near-mem[]
    set dist-near-person-mem[]
    set flock-head-mem[]
    set flock-speed-mem[]
    set temperature-mem[]
  ]
end 

; This function is the gossiping function for DETect. 
to swap-neigh
  ask turtles [
    let flockmatesGoss other turtles in-radius (le_max_dist)
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
    if varsAdded? = false [             ; If the agent hasn't already told DETect what the variables being monitored are, do it now.
      ;Add external variables
      detect:add-var id "ext" "personsNear"
      detect:add-var id "ext" "distNear"
      detect:add-var id "ext" "flockHead"
      detect:add-var id "ext" "flockSpeed"
      detect:add-var id "ext" "temperature"  
      
      ;Add internal variables
      detect:add-var id "int" "myHead"
      detect:add-var id "int" "speed"
      detect:add-var id "int" "weight"
      detect:add-var id "int" "age"
      detect:add-var id "int" "height"
      
      set varsAdded? true
    ]
    ; Pass the internal variable data to DETect
    detect:update-var id "int" "myHead" mem-int-heading
    detect:update-var id "int" "speed" mem-int-speed
    detect:update-var id "int" "weight" mem-int-weight
    detect:update-var id "int" "age" mem-int-age
    detect:update-var id "int" "height" mem-int-height
    
    ; Pass the external variable data to DETect
    detect:update-var id "ext" "personsNear" mem-ext-bn
    detect:update-var id "ext" "distNear" mem-ext-dn
    detect:update-var id "ext" "flockHead" mem-ext-fh
    detect:update-var id "ext" "flockSpeed" mem-ext-fs
    detect:update-var id "ext" "temperature" mem-ext-temperature
    
    ;Run the lasso. The return value is the number of internal variables in the model. If this is above 0 model selection was successful.
    let intChose detect:runLasso id
    
    detect:stopR id

    if intChose > 0 [                  ; Model select was successul, so delete all but the latest 20 observations.
      set relsChosen? true
      set mem-int-heading sublist mem-int-heading 480 500
      set mem-int-speed sublist mem-int-speed 480 500
      set mem-int-weight sublist mem-int-weight 480 500
      set mem-int-height sublist mem-int-height 480 500
      set mem-int-age sublist mem-int-age 480 500
    
      set mem-ext-bn sublist mem-ext-bn 480 500
      set mem-ext-dn sublist mem-ext-dn 480 500
      set mem-ext-fh sublist mem-ext-fh 480 500
      set mem-ext-fs sublist mem-ext-fs 480 500
      set mem-ext-temperature sublist mem-ext-temperature 480 500
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
  
  set mem-int-height remove-item 0 mem-int-height
  set mem-int-height remove-item 0 mem-int-height
  set mem-int-height remove-item 0 mem-int-height
  set mem-int-height remove-item 0 mem-int-height
  set mem-int-height remove-item 0 mem-int-height
  set mem-int-height remove-item 0 mem-int-height
  set mem-int-height remove-item 0 mem-int-height
  set mem-int-height remove-item 0 mem-int-height
  set mem-int-height remove-item 0 mem-int-height
  set mem-int-height remove-item 0 mem-int-height
  
  set mem-int-weight remove-item 0 mem-int-weight
  set mem-int-weight remove-item 0 mem-int-weight
  set mem-int-weight remove-item 0 mem-int-weight
  set mem-int-weight remove-item 0 mem-int-weight
  set mem-int-weight remove-item 0 mem-int-weight
  set mem-int-weight remove-item 0 mem-int-weight
  set mem-int-weight remove-item 0 mem-int-weight
  set mem-int-weight remove-item 0 mem-int-weight
  set mem-int-weight remove-item 0 mem-int-weight
  set mem-int-weight remove-item 0 mem-int-weight
  
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
end

; This function is used to initiate the regression analysis for DETect
to run-Regressions
  if length mem-int-speed = regWinL[  ; Is the observation window full? All will be of equal length as they are update concurrently.
    ; Start R and send the latest variable values.
    detect:startR id
    detect:update-var id "int" "myHead" mem-int-heading
    detect:update-var id "int" "speed" mem-int-speed
    detect:update-var id "int" "weight" mem-int-weight
    detect:update-var id "int" "age" mem-int-age
    detect:update-var id "int" "height" mem-int-height
    
    ;External
    detect:update-var id "ext" "personsNear" mem-ext-bn
    detect:update-var id "ext" "distNear" mem-ext-dn
    detect:update-var id "ext" "flockHead" mem-ext-fh
    detect:update-var id "ext" "flockSpeed" mem-ext-fs
    detect:update-var id "ext" "temperature" mem-ext-temperature
    
    ; Run the regression analysis and then stop R.
    detect:run-regress id
    detect:stopR id
    
    ; Retrieve the CUSUM scores for each internal variable. Each value in the CUSUM list contains the maximum CUSUM value for each external variable
    ; being monitored.
    let sCoeff_Cusum detect:report-Cusum id "1"
    let hCoeff_Cusum detect:report-Cusum id "0"
    let agCoeff_Cusum detect:report-Cusum id "2"
    let htCoeff_Cusum detect:report-Cusum id "3"
    let wtCoeff_Cusum detect:report-Cusum id "4"
   
    ; Next check to see if any of the CUSUMS are above the CUSUM threshold value? If so, set the local belief on feedback (localV) to 1.
    ; A change is remembered for a length of changeMemLen, and this "decays" with each subsequent Regression/CUSUM analysis.
    let trigger threshold-low
    set signChange 0
    ifelse (item 0 sCoeff_Cusum >= trigger or item 1 sCoeff_Cusum >= trigger or item 2 sCoeff_Cusum >= trigger or item 3 sCoeff_Cusum >= trigger or item 4 sCoeff_Cusum >= trigger) 
    [
      set signChange 1
      set localV 1
      set changeDecay changeMemLen
    ]
    [
      ifelse (item 0 hCoeff_Cusum >= trigger or item 1 hCoeff_Cusum >= trigger or item 2 hCoeff_Cusum >= trigger or item 3 hCoeff_Cusum >= trigger or item 4 hCoeff_Cusum >= trigger) 
      [
        set signChange 1
        set localV 1  ;;Set own Emergence belief to be 1 ie. yes
        set changeDecay changeMemLen
      ]
      [
        ifelse (item 0 agCoeff_Cusum >= trigger or item 1 agCoeff_Cusum >= trigger or item 2 agCoeff_Cusum >= trigger or item 3 agCoeff_Cusum >= trigger or item 4 agCoeff_Cusum >= trigger) 
        [
          set signChange 1
          set localV 1  ;;Set own Emergence belief to be 1 ie. yes
          set changeDecay changeMemLen
        ]
        [
          ifelse (item 0 htCoeff_Cusum >= trigger or item 1 htCoeff_Cusum >= trigger or item 2 htCoeff_Cusum >= trigger or item 3 htCoeff_Cusum >= trigger or item 4 htCoeff_Cusum >= trigger) 
          [
            set signChange 1
            set localV 1  ;;Set own Emergence belief to be 1 ie. yes
            set changeDecay changeMemLen
          ]
          [
            ifelse (item 0 wtCoeff_Cusum >= trigger or item 1 wtCoeff_Cusum >= trigger or item 2 wtCoeff_Cusum >= trigger or item 3 wtCoeff_Cusum >= trigger or item 4 wtCoeff_Cusum >= trigger) 
            [
              set signChange 1
              set localV 1  ;;Set own Emergence belief to be 1 ie. yes
              set changeDecay changeMemLen
            ]
            [
              set signChange 0
              if changeDecay > 0 [
                set changeDecay changeDecay - 1
              ]
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
    set coeff-pn-myS_Cusum item 0 sCoeff_Cusum
    set coeff-dn-myS_Cusum item 1 sCoeff_Cusum
    set coeff-fh-myS_Cusum item 2 sCoeff_Cusum
    set coeff-fs-myS_Cusum item 3 sCoeff_Cusum
    set coeff-tm-myS_Cusum item 4 sCoeff_Cusum
    
    set coeff-pn-myH_Cusum item 0 hCoeff_Cusum
    set coeff-dn-myH_Cusum item 1 hCoeff_Cusum
    set coeff-fh-myH_Cusum item 2 hCoeff_Cusum
    set coeff-fs-myH_Cusum item 3 hCoeff_Cusum
    set coeff-tm-myH_Cusum item 4 hCoeff_Cusum
    
    set coeff-pn-myAge_Cusum item 0 agCoeff_Cusum
    set coeff-dn-myAge_Cusum item 1 agCoeff_Cusum
    set coeff-fh-myAge_Cusum item 2 agCoeff_Cusum
    set coeff-fs-myAge_Cusum item 3 agCoeff_Cusum
    set coeff-tm-myAge_Cusum item 4 agCoeff_Cusum
    
    set coeff-pn-myWeight_Cusum item 0 wtCoeff_Cusum
    set coeff-dn-myWeight_Cusum item 1 wtCoeff_Cusum
    set coeff-fh-myWeight_Cusum item 2 wtCoeff_Cusum
    set coeff-fs-myWeight_Cusum item 3 wtCoeff_Cusum
    set coeff-tm-myWeight_Cusum item 4 wtCoeff_Cusum
    
    set coeff-pn-myHeight_Cusum item 0 htCoeff_Cusum
    set coeff-dn-myHeight_Cusum item 1 htCoeff_Cusum
    set coeff-fh-myHeight_Cusum item 2 htCoeff_Cusum
    set coeff-fs-myHeight_Cusum item 3 htCoeff_Cusum
    set coeff-tm-myHeight_Cusum item 4 htCoeff_Cusum
    
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
    ;let temp-change (random-float 0.4 - 0.2)
    set global-temperature global-temperature + changeTemp
  ]
end

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;                                                                       Setup Procedures
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; This function setups the inital value for the global parameters.
to setup-globals
  ;These are passed in by the simulation starter
  print "Setting globals"
  set run-name ""
  set minNeighSize 0
  set maxNeighSize 0
  set changeMemLen 0
  set aggThreshold 0
  set scaler 0.05
  set lasso-Win 500
  
  set objM 0
  set changPer 0
  set emPercent 0
  
  set global-temperature 72
end

; This function is used to set up the DETect stats parameters owned by each agent. This basically initialses all the empty arrays that will be used, outlines the number of internal and 
; external variables and adds them to the DETect object.
to setup-stats
  ; Internal Variables
  set myHeading 0
  set age 30
  set height 170 
  set weight 70
  
  set age age + (random-float 20 - 10)
  set height height + (random-float 30 - 15)
  set weight weight + (random-float 40 - 20)
  
  set speed-mem[]
  set heading-mem[]
  
  set age-mem[]
  set height-mem[]
  set weight-mem[]
  
  set mem-int-speed[]
  set mem-int-heading[]
  
  set mem-int-age[]
  set mem-int-height[]
  set mem-int-weight[]
  
  ;;External Variables
  
  set flockCount 0        ; Cars in radius 1.5
  set distNear  0  
  set flockHead 0
  set flockSpeed 0
  
  set persons-near-mem[]
  set dist-near-person-mem[]
  set flock-head-mem[]
  set flock-speed-mem[]
  set temperature-mem[]
  
  set mem-ext-bn[]
  set mem-ext-dn[]
  set mem-ext-fh[]
  set mem-ext-fs[]
  set mem-ext-temperature[]

  
  detect:new-data "5" "5" id        ; 5 internal variables,5 external variables
  
  detect:add-var id "ext" "personsNear"
  detect:add-var id "ext" "distNear"
  detect:add-var id "ext" "flockHead"
  detect:add-var id "ext" "flockSpeed"
  detect:add-var id "ext" "temperature"
  
  ;Add internal variables
  detect:add-var id "int" "myHead"
  detect:add-var id "int" "speed"
  detect:add-var id "int" "weight"
  detect:add-var id "int" "age"
  detect:add-var id "int" "height"
end

; This is the second part of the setup process managed by the Java run library. 
; It sets up the agents and initialses DETect.
to setup-part2
  
  set threshold-low 4.0
  setup-persons
  
  detect:initialise
  
  ask turtles  [
    setup-stats
  ]
  detect:itest
  
end

; This function asks the agents/persons to initialse personal parameters.
to setup-persons
  ask turtles[
    set id (word "turtle-" who)
    
    set varsAdded? false
    set relsChosen? false
    
    set localV 0
    set Vp 0
    set changeDecay 0
    
    set emergeBelief? false
    set emergeRelent? true
  ]
end

to setup-file
  let file run-name
  ;; We check to make sure we actually got a string just in case
  ;; the user hits the cancel button.
  if is-string? file
  [
    ;; If the file already exists, we begin by deleting it, otherwise
    ;; new data would be appended to the old contents.
    if file-exists? file
      [ file-delete file ]
    file-open file
    ;; record the initial turtle data
  ]
end



to cal-globals
  set objM ((count patches with [count turtles in-radius 2 = 0]) / count patches) * 100
  set changPer ((count turtles with [localV = 1]) / count turtles) * 100
  set emPercent ((count turtles with [emergeBelief?]) / count turtles) * 100 
  set lanePercent ((count turtles with [l_member?]) / count turtles) * 100 
end

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;                                                                 Log writing functions
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
to write-to-file-comprehensive
  let file-name (word run-name "-" ticks ".csv")
  file-open file-name
  file-print "** Global Statistics **"
  ;file-print (word "MeanFM,global_coeff-pn-myS_cusum,global_coeff-dn-myS_cusum,global_coeff-fh-myS_cusum,global_coeff-fs-myS_cusum,global_coeff-pn-myH_cusum,global_coeff-dn-myH_cusum,global_coeff-fh-myH_cusum,global_coeff-fs-myH_cusum,global_coeff-Rsq-myS_cusum,global_coeff-Rsq-myH_cusum")
  ;file-print (word  meanFM "," global_coeff-pn-myS_cusum "," global_coeff-dn-myS_cusum "," global_coeff-fh-myS_cusum "," global_coeff-fs-myS_cusum  "," global_coeff-pn-myH_cusum "," global_coeff-dn-myH_cusum "," global_coeff-fh-myH_cusum "," global_coeff-fs-myH_cusum "," global_coeff-Rsq-myS_cusum "," global_coeff-Rsq-myH_cusum)
  file-print (word "Time,ChangePercent,EmergePercent,LanePercent,Entropy1,Favg_Entropy1,Entropy2,Favg_Entropy2,LaneCount,Random_Walk")
  file-print (word ticks ","  changPer  ","  emPercent  ","  lanePercent ","  entropy1  "," favg_entropy1  ","  entropy2  "," favg_entropy2 
    ","  cur_lcount  ","  random_walk?  ","  min_lane_len)
  
  file-print "**Turles-With-Change**"
  let turtChange [who] of turtles with [localV = 1]
  file-print length turtChange
  foreach turtChange [
    let line (word ? "," [localV] of turtle ? "," [emergeBelief?] of turtle ? "," [l_member?] of turtle ?)
    file-print line 
  ]
  
  file-print "**4. Cars Threshold Low**"
  file-print "**coeff-pn-myS_cusum*"
  let turtListHere [who] of turtles with [coeff-pn-myS_cusum >=  threshold-low ];and coeff-pn-myS_cusum < threshold-mid-low]
  file-print length turtListHere
  foreach turtListHere [
    let line (word ? ","  [coeff-pn-myS_cusum] of turtle ?)
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
  set turtListHere [who] of turtles with [coeff-tm-myS_cusum >=  threshold-low ];and coeff-tm-myS_cusum < threshold-mid-low]
  file-print length turtListHere
  foreach turtListHere [
    let line (word ? ","  [coeff-tm-myS_cusum] of turtle ?)
    file-print line
  ]
  
  file-print "**coeff-pn-myH_cusum*"
  set turtListHere [who] of turtles with [coeff-pn-myH_cusum >=  threshold-low ];and coeff-pn-myH_cusum < threshold-mid-low]
  file-print length turtListHere
  foreach turtListHere [
    let line (word ? ","  [coeff-pn-myH_cusum] of turtle ?)
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
  set turtListHere [who] of turtles with [coeff-tm-myH_cusum >=  threshold-low ];and coeff-tm-myS_cusum < threshold-mid-low]
  file-print length turtListHere
  foreach turtListHere [
    let line (word ? ","  [coeff-tm-myH_cusum] of turtle ?)
    file-print line
  ]
  
  file-print "**coeff-pn-myAge_cusum*"
  set turtListHere [who] of turtles with [coeff-pn-myAge_cusum >=  threshold-low ];and coeff-pn-myAge_cusum < threshold-mid-low]
  file-print length turtListHere
  foreach turtListHere [
    let line (word ? ","  [coeff-pn-myAge_cusum] of turtle ?)
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
  set turtListHere [who] of turtles with [coeff-tm-myAge_cusum >=  threshold-low ];and coeff-tm-myS_cusum < threshold-mid-low]
  file-print length turtListHere
  foreach turtListHere [
    let line (word ? ","  [coeff-tm-myAge_cusum] of turtle ?)
    file-print line
  ]
  
  file-print "**coeff-pn-myWeight_cusum*"
  set turtListHere [who] of turtles with [coeff-pn-myWeight_cusum >=  threshold-low ];and coeff-pn-myWeight_cusum < threshold-mid-low]
  file-print length turtListHere
  foreach turtListHere [
    let line (word ? ","  [coeff-pn-myWeight_cusum] of turtle ?)
    file-print line
  ]
  
  file-print "**coeff-dn-myWeight_cusum*"
  set turtListHere [who] of turtles with [coeff-dn-myWeight_cusum >=  threshold-low ];and coeff-dn-myWeight_cusum < threshold-mid-low]
  file-print length turtListHere
  foreach turtListHere [
    let line (word ? ","  [coeff-dn-myWeight_cusum] of turtle ?)
    file-print line
  ]
  
  file-print "**coeff-fh-myWeight_cusum*"
  set turtListHere [who] of turtles with [coeff-fh-myWeight_cusum >=  threshold-low ];and coeff-fh-myWeight_cusum < threshold-mid-low]
  file-print length turtListHere
  foreach turtListHere [
    let line (word ? ","  [coeff-fh-myWeight_cusum] of turtle ?)
    file-print line
  ]
  
  file-print "**coeff-fs-myWeight_cusum*"
  set turtListHere [who] of turtles with [coeff-fs-myWeight_cusum >=  threshold-low ];and coeff-fs-myWeight_cusum < threshold-mid-low]
  file-print length turtListHere
  foreach turtListHere [
    let line (word ? ","  [coeff-fs-myWeight_cusum] of turtle ?)
    file-print line
  ]
  
  file-print "**coeff-tm-myWeight_cusum*"
  set turtListHere [who] of turtles with [coeff-tm-myWeight_cusum >=  threshold-low ];and coeff-tm-myS_cusum < threshold-mid-low]
  file-print length turtListHere
  foreach turtListHere [
    let line (word ? ","  [coeff-tm-myWeight_cusum] of turtle ?)
    file-print line
  ]
  
  file-print "**coeff-pn-myHeight_cusum*"
  set turtListHere [who] of turtles with [coeff-pn-myHeight_cusum >=  threshold-low ];and coeff-pn-myHeight_cusum < threshold-mid-low]
  file-print length turtListHere
  foreach turtListHere [
    let line (word ? ","  [coeff-pn-myHeight_cusum] of turtle ?)
    file-print line
  ]
  
  file-print "**coeff-dn-myHeight_cusum*"
  set turtListHere [who] of turtles with [coeff-dn-myHeight_cusum >=  threshold-low ];and coeff-dn-myHeight_cusum < threshold-mid-low]
  file-print length turtListHere
  foreach turtListHere [
    let line (word ? ","  [coeff-dn-myHeight_cusum] of turtle ?)
    file-print line
  ]
  
  file-print "**coeff-fh-myHeight_cusum*"
  set turtListHere [who] of turtles with [coeff-fh-myHeight_cusum >=  threshold-low ];and coeff-fh-myHeight_cusum < threshold-mid-low]
  file-print length turtListHere
  foreach turtListHere [
    let line (word ? ","  [coeff-fh-myHeight_cusum] of turtle ?)
    file-print line
  ]
  
  file-print "**coeff-fs-myHeight_cusum*"
  set turtListHere [who] of turtles with [coeff-fs-myHeight_cusum >=  threshold-low ];and coeff-fs-myHeight_cusum < threshold-mid-low]
  file-print length turtListHere
  foreach turtListHere [
    let line (word ? ","  [coeff-fs-myHeight_cusum] of turtle ?)
    file-print line
  ]
  
  file-print "**coeff-tm-myHeight_cusum*"
  set turtListHere [who] of turtles with [coeff-tm-myHeight_cusum >=  threshold-low ];and coeff-tm-myS_cusum < threshold-mid-low]
  file-print length turtListHere
  foreach turtListHere [
    let line (word ? ","  [coeff-tm-myHeight_cusum] of turtle ?)
    file-print line
  ]
  
  
  file-print "**coeff-Rsq-myS_cusum*"
  
  
  file-print "**Turles-With-Emerge Belief**"
  let turtEmerge [who] of turtles with [emergeBelief?]
  file-print length turtEmerge
  foreach turtEmerge [
    let line (word ? "," [localV] of turtle ? "," [emergeBelief?] of turtle ? "," [l_member?] of turtle ?)
    file-print line 
  ]
  
  file-print "**Turles-With-Lane Belief**"
  let turtLanes [who] of turtles with [l_member?]
  file-print length turtLanes
  foreach turtLanes [
    let line (word ? "," [localV] of turtle ? "," [emergeBelief?] of turtle ? "," [l_member?] of turtle ?)
    file-print line 
  ]
  
  file-close
end

to test-rand
  print (random-float 0.1 - 0.05)
  print (random-float 0.1 - 0.05)
  print (random-float 0.1 - 0.05)
end
@#$#@#$#@
GRAPHICS-WINDOW
97
10
827
761
-1
-1
18.0
1
11
1
1
1
0
1
1
1
0
39
0
39
0
0
1
ticks
30.0

BUTTON
9
10
72
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
9
50
72
83
NIL
go
NIL
1
T
OBSERVER
NIL
NIL
NIL
NIL
1

SLIDER
973
10
1094
43
information_radius
information_radius
0
10
2
1
1
NIL
HORIZONTAL

SLIDER
834
10
955
43
population
population
0
500
382
1
1
NIL
HORIZONTAL

BUTTON
8
88
72
122
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
834
46
956
79
red_blue_ratio
red_blue_ratio
0
1
0.28
0.01
1
NIL
HORIZONTAL

SLIDER
1159
739
1331
772
b0
b0
0.1
50
16.3
0.1
1
NIL
HORIZONTAL

SLIDER
1313
290
1485
323
b45
b45
0
5
0.7
0.1
1
NIL
HORIZONTAL

SLIDER
1119
521
1291
554
b90
b90
0
5
0.4
0.1
1
NIL
HORIZONTAL

BUTTON
841
417
945
450
NIL
counter_flow
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
842
460
948
493
NIL
crossing_flow
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
1439
514
1548
559
Not exlusive patches
count patches with [count turtles-here with [not obstacle?] > 1]
17
1
11

SLIDER
842
150
1015
183
le_max_dist
le_max_dist
0
20
5
1
1
NIL
HORIZONTAL

SLIDER
842
186
1015
219
lane_pair_cone_angle
lane_pair_cone_angle
0
180
80
1
1
NIL
HORIZONTAL

SLIDER
843
222
1015
255
walker_in_between_angle
walker_in_between_angle
0
90
30
1
1
NIL
HORIZONTAL

SLIDER
843
257
1015
290
min_lane_len
min_lane_len
0
50
4
1
1
NIL
HORIZONTAL

SWITCH
844
292
982
325
show_blue_lanes?
show_blue_lanes?
1
1
-1000

SWITCH
843
326
982
359
show_red_lanes?
show_red_lanes?
1
1
-1000

MONITOR
1532
13
1605
58
NIL
llenmax
17
1
11

MONITOR
1532
60
1605
105
NIL
cur_llenmax
17
1
11

MONITOR
1534
176
1607
221
NIL
lcountmax
17
1
11

MONITOR
1535
223
1608
268
NIL
cur_lcount
17
1
11

PLOT
1232
12
1529
170
Lanes length
NIL
NIL
0.0
10.0
0.0
10.0
true
false
"" ""
PENS
"default" 1.0 0 -16777216 true "" "plot llenmax"
"pen-1" 1.0 0 -7500403 true "" "plot cur_llenmax"

PLOT
213
1058
852
1224
Lanes count
NIL
NIL
0.0
10.0
0.0
10.0
true
false
"" ""
PENS
"default" 1.0 0 -16777216 true "" "plot lcountmax"
"pen-1" 1.0 0 -7500403 true "" "plot cur_lcount"

SLIDER
1160
775
1332
808
b10
b10
0
5
1.8
0.1
1
NIL
HORIZONTAL

BUTTON
837
707
1000
740
NIL
show_patches_oc
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
989
737
1148
770
NIL
ask turtles [hide-turtle]
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
989
772
1148
805
NIL
ask turtles [show-turtle]
NIL
1
T
OBSERVER
NIL
NIL
NIL
NIL
1

SWITCH
835
743
998
776
show_patches_oc?
show_patches_oc?
0
1
-1000

BUTTON
837
660
1007
693
NIL
ask turtles [set size t_size]
NIL
1
T
OBSERVER
NIL
NIL
NIL
NIL
1

SLIDER
836
624
1006
657
t_size
t_size
0
10
1.5
0.1
1
NIL
HORIZONTAL

SLIDER
974
46
1095
79
decision_radius
decision_radius
0
10
2
1
1
NIL
HORIZONTAL

SLIDER
1038
142
1210
175
min_speed
min_speed
0
1
0.2
0.1
1
NIL
HORIZONTAL

SLIDER
1444
231
1616
264
max_speed
max_speed
0
1
0.3
0.1
1
NIL
HORIZONTAL

SLIDER
1445
275
1617
308
speed_inc
speed_inc
0
0.5
0.1
0.01
1
NIL
HORIZONTAL

SLIDER
1042
274
1214
307
speed_dec
speed_dec
0
0.5
0
0.01
1
NIL
HORIZONTAL

SWITCH
1040
316
1214
349
control_speed?
control_speed?
0
1
-1000

SLIDER
1040
356
1212
389
colision_angle
colision_angle
0
90
5
1
1
NIL
HORIZONTAL

SLIDER
974
82
1095
115
oc_multi_koef
oc_multi_koef
0
5
1.01
0.01
1
NIL
HORIZONTAL

SLIDER
1109
13
1213
46
ocupd_koef
ocupd_koef
-10
10
1
0.1
1
NIL
HORIZONTAL

BUTTON
847
541
978
574
Insert obstacle
insert_obstacle
T
1
T
OBSERVER
NIL
NIL
NIL
NIL
1

BUTTON
847
578
979
611
Remove obstacles
ask turtles with [obstacle?][die]
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
1266
521
1318
566
ds total
sum [ds] of turtles
0
1
11

MONITOR
1419
616
1528
661
Accelerating walkers
count turtles with [accelerating?]
0
1
11

MONITOR
1419
663
1528
708
Decelerating walkers
count turtles with [decelerating?]
0
1
11

MONITOR
1474
566
1590
611
NIL
sum [dh] of turtles
0
1
11

PLOT
388
881
687
1031
Walkers heading change - total
NIL
NIL
0.0
10.0
0.0
10.0
true
false
"" ""
PENS
"default" 1.0 0 -16777216 true "" "plot sum [dh] of turtles"

SLIDER
845
501
977
534
onstacle_radius
onstacle_radius
0
50
1
1
1
NIL
HORIZONTAL

BUTTON
965
571
1097
604
Stop inserting obstacles
set stop_edit? true
NIL
1
T
OBSERVER
NIL
NIL
NIL
NIL
1

SLIDER
1040
405
1212
438
adif_max
adif_max
0
180
90
1
1
NIL
HORIZONTAL

MONITOR
1419
722
1481
767
dsx total
sum [dsx] of turtles
0
1
11

SLIDER
1570
524
1680
557
avg_dsx_ticks
avg_dsx_ticks
0
100
50
1
1
NIL
HORIZONTAL

MONITOR
1523
611
1636
656
NIL
dsx_all_avg
0
1
11

PLOT
1303
378
1599
528
All walkers avg_dsx (overall movement speed)
NIL
NIL
0.0
10.0
0.0
10.0
true
false
"" ""
PENS
"default" 1.0 0 -16777216 true "" "plot dsx_all_avg"

SWITCH
836
779
1010
812
show_individual_update?
show_individual_update?
1
1
-1000

MONITOR
849
788
958
833
NIL
dsx_all_avg_max
0
1
11

MONITOR
1527
711
1631
756
NIL
dsx_all_avg_min
0
1
11

MONITOR
1580
679
1642
724
NIL
wt_count
17
1
11

MONITOR
1582
731
1641
776
NIL
wt_total
17
1
11

MONITOR
1686
587
1746
632
NIL
wt_avg
2
1
11

MONITOR
1682
642
1762
687
NIL
wt_avg_min
2
1
11

MONITOR
1686
695
1771
740
NIL
wt_avg_max
2
1
11

SWITCH
1650
353
1784
386
random_walk?
random_walk?
1
1
-1000

MONITOR
1041
455
1178
500
Patches to walk through
count patches - count patches with [random_walk_passed1? and random_walk_passed2?]
0
1
11

PLOT
874
831
1588
1393
entropy1
NIL
NIL
0.0
10.0
0.0
10.0
true
false
"" ""
PENS
"default" 1.0 0 -7500403 true "" "plot entropy1"
"pen-1" 1.0 0 -16777216 true "" "plot favg_entropy1"

MONITOR
1629
10
1693
55
NIL
entropy1
17
1
11

MONITOR
1677
215
1785
260
NIL
round (ticks / 10)
17
1
11

BUTTON
12
220
94
253
areas_10_4
count_lane_members_on_square_areas_10_4 true
NIL
1
T
OBSERVER
NIL
NIL
NIL
NIL
1

SWITCH
1627
130
1796
163
collect_probabilities?
collect_probabilities?
1
1
-1000

SWITCH
1629
175
1790
208
calculate_entropy?
calculate_entropy?
0
1
-1000

SLIDER
1547
283
1719
316
favg_step
favg_step
1
100
50
1
1
NIL
HORIZONTAL

MONITOR
1698
10
1794
55
NIL
favg_entropy1
17
1
11

BUTTON
17
296
92
329
NIL
test_LM
NIL
1
T
OBSERVER
NIL
NIL
NIL
NIL
1

SLIDER
843
116
1015
149
le_min_dist
le_min_dist
0
10
1
1
1
NIL
HORIZONTAL

BUTTON
13
880
171
913
NIL
check_lane_membersip
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
1630
61
1694
106
NIL
entropy2
17
1
11

MONITOR
1699
61
1795
106
NIL
favg_entropy2
17
1
11

PLOT
1260
97
1634
377
entropy004
NIL
NIL
0.0
10.0
20.0
10.0
true
false
"" ""
PENS
"default" 1.0 0 -7500403 true "" "plot entropy004"

BUTTON
13
258
95
291
areas_4_10
count_lane_members_on_square_areas_4_10 true
NIL
1
T
OBSERVER
NIL
NIL
NIL
NIL
1

SWITCH
4
182
94
215
_10_4?
_10_4?
1
1
-1000

SWITCH
1084
196
1257
229
micro_level_entropy?
micro_level_entropy?
1
1
-1000

MONITOR
97
761
383
806
NIL
count turtles with [count other turtles-on neighbors > 0]
17
1
11

SLIDER
389
810
570
843
individual_radius
individual_radius
0
10
0
1
1
patches
HORIZONTAL

MONITOR
390
762
756
807
NIL
count turtles with [any? other turtles in-radius individual_radius]
17
1
11

MONITOR
840
366
985
411
NIL
count patches with [count turtles-here > 1]
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
<experiments>
  <experiment name="experiment" repetitions="1" runMetricsEveryStep="false">
    <setup>setup</setup>
    <go>go</go>
    <metric>dsx_all_avg_min</metric>
    <metric>dsx_all_avg_max</metric>
    <metric>wt_total</metric>
    <metric>wt_avg</metric>
    <metric>wt_avg_min</metric>
    <metric>wt_avg_max</metric>
    <enumeratedValueSet variable="t_size">
      <value value="1.5"/>
    </enumeratedValueSet>
    <enumeratedValueSet variable="speed_inc">
      <value value="0.1"/>
    </enumeratedValueSet>
    <enumeratedValueSet variable="min_lane_len">
      <value value="10"/>
    </enumeratedValueSet>
    <enumeratedValueSet variable="lane_pair_max_distance">
      <value value="7"/>
    </enumeratedValueSet>
    <enumeratedValueSet variable="avg_dsx_ticks">
      <value value="50"/>
    </enumeratedValueSet>
    <enumeratedValueSet variable="information_radius">
      <value value="2"/>
      <value value="3"/>
      <value value="4"/>
    </enumeratedValueSet>
    <enumeratedValueSet variable="population">
      <value value="200"/>
    </enumeratedValueSet>
    <enumeratedValueSet variable="b0">
      <value value="16.3"/>
    </enumeratedValueSet>
    <enumeratedValueSet variable="oc_multi_koef">
      <value value="0.7"/>
      <value value="1"/>
      <value value="1.3"/>
    </enumeratedValueSet>
    <enumeratedValueSet variable="show_patches_oc?">
      <value value="false"/>
    </enumeratedValueSet>
    <enumeratedValueSet variable="red_blue_ratio">
      <value value="0.3"/>
    </enumeratedValueSet>
    <enumeratedValueSet variable="speed_dec">
      <value value="0.08"/>
    </enumeratedValueSet>
    <enumeratedValueSet variable="control_speed?">
      <value value="true"/>
      <value value="false"/>
    </enumeratedValueSet>
    <enumeratedValueSet variable="colision_angle">
      <value value="15"/>
    </enumeratedValueSet>
    <enumeratedValueSet variable="max_speed">
      <value value="0.4"/>
    </enumeratedValueSet>
    <enumeratedValueSet variable="b90">
      <value value="0.4"/>
    </enumeratedValueSet>
    <enumeratedValueSet variable="show_individual_update?">
      <value value="false"/>
    </enumeratedValueSet>
    <enumeratedValueSet variable="decision_radius">
      <value value="2"/>
      <value value="3"/>
    </enumeratedValueSet>
    <enumeratedValueSet variable="min_speed">
      <value value="0.2"/>
    </enumeratedValueSet>
    <enumeratedValueSet variable="show_red_lanes?">
      <value value="false"/>
    </enumeratedValueSet>
    <enumeratedValueSet variable="adif_max">
      <value value="90"/>
    </enumeratedValueSet>
    <enumeratedValueSet variable="lane_pair_cone_angle">
      <value value="101"/>
    </enumeratedValueSet>
    <enumeratedValueSet variable="walker_in_between_angle">
      <value value="15"/>
    </enumeratedValueSet>
    <enumeratedValueSet variable="onstacle_radius">
      <value value="1"/>
    </enumeratedValueSet>
    <enumeratedValueSet variable="b45">
      <value value="0.7"/>
    </enumeratedValueSet>
    <enumeratedValueSet variable="b10">
      <value value="1.7"/>
    </enumeratedValueSet>
    <steppedValueSet variable="ocupd_koef" first="0.5" step="0.1" last="1.5"/>
    <enumeratedValueSet variable="show_blue_lanes?">
      <value value="true"/>
    </enumeratedValueSet>
  </experiment>
  <experiment name="experiment" repetitions="1" runMetricsEveryStep="true">
    <setup>setup</setup>
    <go>go</go>
    <metric>count turtles</metric>
    <enumeratedValueSet variable="t_size">
      <value value="1.5"/>
    </enumeratedValueSet>
    <enumeratedValueSet variable="speed_inc">
      <value value="0.1"/>
    </enumeratedValueSet>
    <enumeratedValueSet variable="min_lane_len">
      <value value="10"/>
    </enumeratedValueSet>
    <enumeratedValueSet variable="lane_pair_max_distance">
      <value value="7"/>
    </enumeratedValueSet>
    <enumeratedValueSet variable="avg_dsx_ticks">
      <value value="30"/>
    </enumeratedValueSet>
    <enumeratedValueSet variable="information_radius">
      <value value="3"/>
    </enumeratedValueSet>
    <enumeratedValueSet variable="population">
      <value value="250"/>
    </enumeratedValueSet>
    <enumeratedValueSet variable="b0">
      <value value="16.3"/>
    </enumeratedValueSet>
    <enumeratedValueSet variable="oc_multi_koef">
      <value value="1"/>
    </enumeratedValueSet>
    <enumeratedValueSet variable="show_patches_oc?">
      <value value="false"/>
    </enumeratedValueSet>
    <enumeratedValueSet variable="red_blue_ratio">
      <value value="0.3"/>
    </enumeratedValueSet>
    <enumeratedValueSet variable="speed_dec">
      <value value="0.08"/>
    </enumeratedValueSet>
    <enumeratedValueSet variable="control_speed?">
      <value value="true"/>
    </enumeratedValueSet>
    <enumeratedValueSet variable="colision_angle">
      <value value="15"/>
    </enumeratedValueSet>
    <enumeratedValueSet variable="max_speed">
      <value value="0.4"/>
    </enumeratedValueSet>
    <enumeratedValueSet variable="b90">
      <value value="0.4"/>
    </enumeratedValueSet>
    <enumeratedValueSet variable="show_individual_update?">
      <value value="false"/>
    </enumeratedValueSet>
    <enumeratedValueSet variable="decision_radius">
      <value value="2"/>
    </enumeratedValueSet>
    <enumeratedValueSet variable="min_speed">
      <value value="0.2"/>
    </enumeratedValueSet>
    <enumeratedValueSet variable="show_red_lanes?">
      <value value="false"/>
    </enumeratedValueSet>
    <enumeratedValueSet variable="adif_max">
      <value value="90"/>
    </enumeratedValueSet>
    <enumeratedValueSet variable="lane_pair_cone_angle">
      <value value="101"/>
    </enumeratedValueSet>
    <enumeratedValueSet variable="walker_in_between_angle">
      <value value="15"/>
    </enumeratedValueSet>
    <enumeratedValueSet variable="onstacle_radius">
      <value value="1"/>
    </enumeratedValueSet>
    <enumeratedValueSet variable="b45">
      <value value="0.7"/>
    </enumeratedValueSet>
    <enumeratedValueSet variable="b10">
      <value value="1.7"/>
    </enumeratedValueSet>
    <enumeratedValueSet variable="ocupd_koef">
      <value value="1.2"/>
    </enumeratedValueSet>
    <enumeratedValueSet variable="show_blue_lanes?">
      <value value="true"/>
    </enumeratedValueSet>
  </experiment>
</experiments>
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
0
@#$#@#$#@
