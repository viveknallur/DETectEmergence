package com.DETect.netlogo;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;


/**
 * @author Eamonn
 * This class is used for internal variables. It inherits from Variable and adds some functionality specific to 
 * internal variables that are not needed by external variables.
 */
public class InternalVariable  extends Variable{
	
	/**
	 * This Hashmap can be used to store the Regression coefficients associated for each external variable associated 
	 * with this Internal variable in the DETect chosen model. These coefficients are not used as part of DETect 
	 * however.
	 */
	private Map<String, double[]> myCoefficients;
	/**
	 * This Hashmap holds the CUSUM parameters associated with the Internal variables relationship with each
	 * external variable that is being tracked as part of DETect's model. This is retrieved during the CUSUM process
	 * and used to calculate the new CUSUM. The updated parameters are then returned.
	 */
	private Map<String, double[]> myMbCusums;
	/**
	 * Stores the names of the external variables whose relationship with this variable is being monitored as part
	 * of the model. If this Set is empty it means the Internal variable is not part of the selected model. Each
	 * mbCusum is of length 8. Here is what each index value holds.
	 * [0] - the latest pValue for the relationship.
	 * [1] - the current estimated mean of the P Values
	 * [2] - the current estimated Standard deviation of the P Values
	 * [3] - the CUSUM that tracks if the mean of the PValues is increasing
	 * [4] - the CUSUM that tracks if the mean of the PValues is decreasing
	 * [5] - the CUSUM that tracks if the standard deviation of the PValues is increasing
	 * [6] - the CUSUM that tracks if the standard deviation of the PValues is decreasing
	 * [7] - the number of observations that make up the baseline
	 */
	public Set<String> externalUsed;
	/**
	 * This Map stores the baseline set of PValues that represent the historical significance of the Internal
	 * variable's relationship with each relevant external variable in the model. This baseline is used during
	 * the CUSUM calculation for each relationship.
	 */
	private Map<String, ArrayList<Double>> mylastPValues;
	
	/**
	 * A boolean variable to indicate whether the CUSUM process has been initialised or not.
	 */
	private boolean cusumInit;

	
	/**
	 * Constructor
	 * @param name the name of the internal variable
	 * @param type the type of the variable, set to internal
	 */
	public InternalVariable(String name, String type) {
		super(name, type);
		this.cusumInit = true;
		this.externalUsed = new HashSet<String>();
	}

	/**
	 * Returns the value of isCusumInit
	 * @return
	 */
	public boolean isCusumInit() {
		return cusumInit;
	}
	
	// Setters and Getters
	public Set<String> getExternalUsed() {
		return externalUsed;
	}

	public void setExternalUsed(Set<String> externalUsed) {
		this.externalUsed = externalUsed;
	}

	public void setCusumInit(boolean cusumInit) {
		this.cusumInit = cusumInit;
	}

	public Map<String, double[]> getMyMbCusums() {
		return myMbCusums;
	}

	public void setMyMbCusums(Map<String, double[]> myMbCusums) {
		this.myMbCusums = myMbCusums;
	}

	public Map<String, double[]> getMyCoefficients() {
		return myCoefficients;
	}

	public void setMyCoefficients(Map<String, double[]> myCoefficients) {
		this.myCoefficients = myCoefficients;
	}
	
	public Map<String, ArrayList<Double>> getMylastPValues() {
		return mylastPValues;
	}

	public void setMylastPValues(Map<String, ArrayList<Double>> mylastPValues) {
		this.mylastPValues = mylastPValues;
	}
}
