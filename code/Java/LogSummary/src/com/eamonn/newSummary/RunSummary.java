package com.eamonn.newSummary;

import java.util.HashMap;
import java.util.Set;

public class RunSummary {
	/*	These three hashmaps give a running record of the progress/count
		of the object measure of congestion, cars detecting change and cars
		with emergence belief. They contain an integer, which gives the 
		timestamp of the recording, and a Set giving the unique IDs of 
		streets or cars that fired at that timestep.
	 */
	private HashMap <Integer,Set<String>> objectMeasure;
	private HashMap <Integer,Set<String>> carChange; 
	private HashMap <Integer,Set<String>> emergeBelief;
	private HashMap <Integer,Double> neighSize;
	

	private double[] objMArray;
	private double[] carCArray;
	private double[] emeBArray;
	private double[] neSzArray;
	
	public RunSummary(){
		objectMeasure = new HashMap<Integer, Set<String>>();
		carChange = new HashMap<Integer,Set<String>>();
		emergeBelief = new HashMap<Integer, Set<String>>();
		neighSize = new HashMap<Integer, Double>();;
	}
	
	
	public void addToObjectMeasure(int i, Set<String>s) {
		objectMeasure.put(i, s);
	}
	
	public void addToCarChange(int i, Set<String>s) {
		carChange.put(i, s);
	}
	
	public void addToEmergeBelief(int i, Set<String>s) {
		emergeBelief.put(i, s);
	}
	
	public void addToNeighSize(int i, Double s) {
		neighSize.put(i, s);
	}
	
	/**
	 * Simple Getter method to return the objectMesure Object
	 * @return
	 */
	public HashMap<Integer, Set<String>> getObjectMeasure() {
		return objectMeasure;
	}

	public void setObjectMeasure(HashMap<Integer, Set<String>> objectMeasure) {
		this.objectMeasure = objectMeasure;
	}

	public HashMap<Integer, Set<String>> getCarChange() {
		return carChange;
	}

	public void setCarChange(HashMap<Integer, Set<String>> carChange) {
		this.carChange = carChange;
	}

	public HashMap<Integer, Set<String>> getEmergeBelief() {
		return emergeBelief;
	}

	public void setEmergeBelief(HashMap<Integer, Set<String>> emergeBelief) {
		this.emergeBelief = emergeBelief;
	}
	
	public HashMap<Integer, Double> getNeighSize() {
		return neighSize;
	}


	public void setNeighSize(HashMap<Integer, Double> neighSize) {
		this.neighSize = neighSize;
	}
}
