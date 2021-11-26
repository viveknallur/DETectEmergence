package com.DETect.netlogo;

/**
 * @author Eamonn
 * This is a class to store a generic variable that is being monitored by DETect from the agent.
 */
public class Variable {
	/**
	 * The name of the variable
	 */
	private String myName;
	
	/**
	 * Variable type, either internal or external (typically external)
	 */
	private String type;
	/**
	 * This is used to store the most recent sliding observation window for the variable.
	 */
	private double[] myValues;

	
	/**
	 * Constructor
	 * @param name the name of the variable
	 * @param type the type of the variable
	 */
	public Variable(String name, String type) {
		this.myName = name;
		this.type = type;
	}

	/**
	 * Getter function for myValues
	 * @return the array myValues
	 */
	public double[] getMyValues() {
		return myValues;
	}

	/**
	 * Setter function for myValues
	 * @param myValues the new set of values for myValues
	 */
	public void setMyValues(double[] myValues) {
		this.myValues = myValues;
	}
}
