This folder contains the Smart GraphHopper code that is used in the traffic simulation. This is the GraphHopper source code altered to make it work how was required to in the traffic simulation. 

As it relates to DETect, nothing significant was changed. Instead,  the return from a route request (how to get from A to B) was updated so that the list of OSM Ids could be returned. In the original GraphHopper the list of street names was returned but this lead to ambiguity in he implementation of routing in the model.

The compiled jar of this folder is included with the ghNetlogo package. I suggest not changing this code unless absolutely necessary. Just use the jar as is.

Changes I made are identified by //Eamonn at the start and end of the addition.
