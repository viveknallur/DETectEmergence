This folder contains the graphHopper extension that is used by agents in the traffic model to navigate the map and get directions from A to B.

--- Content ---
- src -
This folder contains the source code of the graphhopper extension. It is composed of an extension class, that defines the new commands that agents call in netlogo, and the underlying logic, GraphHopperRouter, that interfaces with the GraphHopper application.

- libs - 
This folder contains the jar libraries used by this extension. These should be copied to the extension folder in netlogo to ensure they are present there.

- manifest.txt - 
This is required to compile the graphhopper jar file

- runCommands.txt -
This file contains three commands that should be executed in order in a terminal to compile the library, create the jar and then copy the jar to the netlogo extension folder. Please change the paths as required.
