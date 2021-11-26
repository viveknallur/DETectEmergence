package com.graphhopper.netlogo;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.graphhopper.GHRequest;
import com.graphhopper.GHResponse;
import com.graphhopper.GraphHopper;
import com.graphhopper.util.CmdArgs;
import com.graphhopper.util.shapes.GHPoint;

public class GraphHopperRouter {
	private String[] args;
	private GraphHopper gh;
	
	public GraphHopperRouter() {
		args = new String[4];
		/*args[0] = "graph.location=//home/eamonn/workspace/SMART-GH/maps/manhattan-gh"; 
		args[1] = "config=//home/eamonn/workspace/SMART-GH/config.properties";
		args[2] = "osm.reader=//home/eamonn/workspace/SMART-GH/maps/manhattan.osm";
		args[3] = "osmreader.osm=//home/eamonn/workspace/SMART-GH/maps/manhattan.osm";*/
		String folder = System.getProperty("InputFolder");
		args[0] = "graph.location="+folder+"/GH/maps/manhattan-gh"; 
		args[1] = "config="+folder+"/GH/config.properties";
		args[2] = "osm.reader="+folder+"/GH/maps/manhattan.osm";
		args[3] = "osmreader.osm="+folder+"/GH/maps/manhattan.osm";
	}
	
	public boolean initConnection() {
		try{
		CmdArgs c = CmdArgs.read(args);
        gh = new GraphHopper().init(c);
        gh.importOrLoad();
		}catch (Exception e){
			e.printStackTrace();
			return false;
		}
        return true;
	}
	
	public GHResponse generateNewRoute (double startLat, double startLong, double endLat, double endLong) {
		//Connect to gh and get the route
        //System.out.println("About to ask for new route: " + startLat + " , " + startLong + " to " + endLat + " , " + endLong);
        GHRequest gr = new GHRequest(startLat,startLong, endLat,endLong);
        double randDouble = new Random().nextDouble();
       if(randDouble > 0.5)
        	gr.setWeighting("FASTEST");
        else
        	gr.setWeighting("SHORTEST");
        gr.setVehicle("car");
        //System.out.println("About to calc route");
        if(gh == null) {
        	System.out.println("Spaghettios");
        }
        GHResponse ph = gh.route(gr);
        //System.out.println(gr.getWeighting() + " , " + ph.getInstructions().size());
        //Now parse the route
        return ph;
	}
	
	public GHResponse generateNewRoute (double startLat, double startLong, double dirLat, double dirLong, double endLat, double endLong) {
		//Connect to gh and get the route
        //System.out.println("About to ask for new route: " + startLat + " , " + startLong + " to " + endLat + " , " + endLong);
        List<GHPoint> pointList = new ArrayList<GHPoint>();
        GHPoint start = new GHPoint(startLat, startLong);
        GHPoint dir = new GHPoint(dirLat, dirLong);
        GHPoint end = new GHPoint(endLat, endLong);
        
        pointList.add(start);
        pointList.add(dir);
        pointList.add(end);
		
		GHRequest gr = new GHRequest(pointList);
        //gr.setWeighting("fastest");
        gr.setVehicle("car");
        //System.out.println("About to calc route");
        if(gh == null) {
        	System.out.println("Spaghettios");
        }
        GHResponse ph = gh.route(gr);
        //Now parse the route
        return ph;
	}
	
	public boolean closeConnection() {
		try{
			gh.close();  
		}catch(Exception e){
			e.printStackTrace();
			return false;
		}
		return true;
	}
}
