package com.graphhopper.netlogo;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;

import org.apache.commons.math3.util.Pair;

import com.graphhopper.GHRequest;
import com.graphhopper.GHResponse;
import com.graphhopper.GraphHopper;
import com.graphhopper.util.CmdArgs;
import com.graphhopper.util.Instruction;
import com.graphhopper.util.InstructionList;
import com.graphhopper.util.PointList;

public class Navigator implements Serializable{
	private String myName;
	private String currentStreet;
	private Long currentOsm;
	private ArrayList<Integer> instructionList;
	private ArrayList<String> streetList;
	private ArrayList<PointList> coordinateList;
	private ArrayList<Long> osmList;
	private ArrayList<double[]> destinations;
	private ArrayList<Double> durations;
	private ArrayList<Double> distances;
	//1. Constructor
	public Navigator(String myName){
		this.myName = myName;
		destinations = new ArrayList<double[]>();
		instructionList = new ArrayList<Integer>();
    	streetList = new ArrayList<String>();
    	coordinateList = new ArrayList<PointList>();
    	currentStreet = "";
    	osmList = new ArrayList<Long>();
    	currentOsm = (long) 0;
    	durations = new ArrayList<Double>();
    	distances = new ArrayList<Double>();
	}
	
	private void resetRouteLists(){
		instructionList = new ArrayList<Integer>();
    	streetList = new ArrayList<String>();
    	coordinateList = new ArrayList<PointList>();
	}
	
	public ArrayList<Long> parseRoute(GHResponse ph) {
		/*resetRouteLists();
		InstructionList l = ph.getInstructions();
        Iterator<Instruction> it = l.iterator();
        while(it.hasNext()){
            Instruction i = it.next();
            String street = i.getName();
            int instruct = i.getSign();
            if(instruct != 5){
            	if (instruct != 4) {
	            PointList p = i.getPoints();
	            instructionList.add(instruct);
	            streetList.add(street);
	            coordinateList.add(p); 
            	}else{
            		instructionList.add(instruct);
            	}
            }
        }*/
        osmList = ph.getOsmIds();
        return osmList;
	}
	
	public long reportNextOsm() {
		long reportOsm = 0;
		if(!osmList.isEmpty()){
			long nextOsm = osmList.get(0);
			currentOsm = nextOsm;
			reportOsm = nextOsm;
			osmList.remove(0);
		}
		return reportOsm;
	}
	
	public String reportNextStreet() {
		String reportStreet = "";
		if(!streetList.isEmpty()){
			String nextStreet = streetList.get(0);
			currentStreet = nextStreet;
			reportStreet = nextStreet;
			streetList.remove(0);
		}
		return reportStreet;
	}
	
	public int reportNextInstruction() {
		String reportStreet = "";
		int reportInstruction = -5;
		if(!instructionList.isEmpty()){
			reportInstruction = instructionList.get(0);
			instructionList.remove(0);
		}
		return reportInstruction;
	}
	
	public double[] reportNextCoordinate() {
		double[] nextCord = new double[2];
		if(!coordinateList.isEmpty()){
			PointList pl = coordinateList.get(0);
			double lat = pl.getLatitude(0);
			double lon = pl.getLongitude(0);
			nextCord[0] = lat;
			nextCord[1] = lon;
			coordinateList.remove(0);
		}
		return nextCord;
	}
	
	public String reportCurrentStreet() {
		return currentStreet;
	}
	
	public long reportCurrentOsm() {
		return currentOsm;
	}
	
	public boolean osmCheck (String osmID) {
		if (osmList.contains(osmID)){
			int index = osmList.indexOf(osmID);
			for(int i = 0; i < index; i++){
				osmList.remove(i);
			}
			return true;
		}
		return false;
	}
	
	public void addDestination(double latitude, double longitude) {
		double[] location = new double[2];
		location[0] = latitude;
		location[1] = longitude;
		destinations.add(location);
	}
	
	public double[] getNextDestination (){
		if (destinations.size() > 0){
			double[] returnLocation = destinations.get(0);
			destinations.remove(0);
			return returnLocation;
		}else{
			double[] returnLocation = new double [2];
			returnLocation[0] = Double.parseDouble("0.0");
			returnLocation[1] = Double.parseDouble("0.0");
			return returnLocation;
		}
	}
	
	public void addDistance (double distance) {
		distances.add(distance);
	}
	
	public void addDuration (double duration) {
		durations.add(duration);
	}
	
	public double[] tripInfo () {
		double info[] = new double[2];
		info[0] = distances.get(0);
		info[1] = durations.get(0);
		return info;
	}

}
