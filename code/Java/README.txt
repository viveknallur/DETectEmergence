This directory contains all the java code needed to compile and run DETect simulations in NetLogo. Some of these are placed here for completeness and I suggest not changing anything in those projects, however I will specify which these are below.

-- Content -- 

1. DETect
This is the main project here. It contains the code for the NetLogo extension that allows netlogo agents to access DETect functionality. The majority of DETect's functionality is contained in this folder with the exception of 1) gossiping (contained in the netlogo models themselves) and 2) some R code that is called from this DETect extension. The code is extensively commented so it should hopefully be straighforward to figure out what is going on where.

2. ghNetlogo
The car agents in the traffic model use graphhopper to find their way from points A to B during simulations. This required that another NetLogo extension be created to allow agents to call graphhopper during each simulation. This is what this project does. You can change this if necessary but I don't know why that would be needed.

3. SMART-GH
This is the main graph hopper code base, so its basically the engine for ghNetlogo. The original code base was edited to change how routes are returned so that the OSM-id of each street is returned. This was necessary to remove ambiguity from routing in netlogo model which was causing problems. The compiled jar of this is used by ghNetlogo and I don't recommend trying to update this unless absolutely necessary.

4. NetlogoRunner
This contains a small project that is used to run NetLogo in headless mode. It loads the parameters specific to each simulation run and these are passed in via a csv file. The project should be compiled to a jar and then copied to the netlogo application folder. It can be run by calling the makeRun.sh file. This folder is specific to the Traffic model. 

5. NetlogoRunnerFlock
Same as above except this one is used for the Pedestrian or Flocking models.
