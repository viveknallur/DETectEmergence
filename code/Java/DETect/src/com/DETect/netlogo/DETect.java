package com.DETect.netlogo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.stat.correlation.Covariance;
import org.apache.commons.math3.stat.regression.OLSMultipleLinearRegression;
import org.nlogo.api.LogoList;
import org.nlogo.api.LogoListBuilder;
import org.rosuda.JRI.REXP;
import org.rosuda.JRI.Rengine;

/**
 * @author Eamonn
 * 
 */
/**
 * @author eamonn
 *
 */
/**
 * @author eamonn
 *
 */
public class DETect {

	private Map<String, InternalVariable> internalVariables;
	private Map<String, Variable> externalVariables;
	private List<String> internalNames;
	private List<String> externalNames;
	private Set<String> internalUsed;
	private Rengine re;
	private Map<String, ArrayList<String>> variablesUsed;
	private Map<String, Map<String, ArrayList<Double>>> coefficients;
	private int countInt;
	private int countExt;
	private double avgRsq;
	private boolean init;
	private List<Double> u;
	private int WinSize = 20;
	private int signifChange;
	private String name;
	private boolean variablesChosen;
	private int cusumMin = 0;
	private int cusumWindow = 0;

	/**
	 * Constructor
	 * 
	 * @param intCount How many internal variables
	 * @param extCount How many external variables
	 * @param name The name of the car/agent using this object
	 */
	public DETect(int intCount, int extCount, String name) {
		internalUsed = new HashSet<String>();
		internalNames = new ArrayList<String>();
		externalNames = new ArrayList<String>();
		internalVariables = new HashMap<String, InternalVariable>();
		externalVariables = new HashMap<String, Variable>();
		coefficients = new HashMap<String, Map<String, ArrayList<Double>>>();
		countInt = intCount;
		countExt = extCount;
		init = true;
		u = new ArrayList<Double>();
		variablesChosen = false;
		cusumMin = Integer.parseInt(System.getProperty("CusumMin"));
		cusumWindow = Integer.parseInt(System.getProperty("CusumWindow"));

		variablesUsed = new HashMap<String, ArrayList<String>>();

		this.name = name;
		re = null;
	}

	public int isSignifChange() {
		return this.signifChange;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setSignifChange(int signifChange) {
		this.signifChange = signifChange;
	}

	public double getAvgRSq() {
		return this.avgRsq;
	}

	public void clearInternal() {
		internalVariables = new HashMap<String, InternalVariable>();
	}
	
	public void RengineStart() {
		re = Rengine.getMainEngine();
		if (re == null)
			re = new Rengine(new String[] { "--vanilla" }, false, null);
	}
	
	public void RengineStop() {
		if (re != null){
			re.eval("rm(list = ls())");
			re.end();
			re = null;
		}
	}
	
	public Map<String, ArrayList<String>> getVariablesUsed() {
		return variablesUsed;
	}

	public void setVariablesUsed(Map<String, ArrayList<String>> variablesUsed) {
		this.variablesUsed = variablesUsed;
	}
	

	/************************ Variable Based Methods *********************************/

	/**
	 * This method adds new variables to be tracked. External Variables should
	 * all be added before internal variables are I will return to make this
	 * more robust in the future
	 * 
	 * @param type The type of the variable, internal or external
	 * @param name The name of the variable.
	 */
	public void addVariable(String type, String name) {
		if (type.toLowerCase().equals("int")) {
			InternalVariable var = new InternalVariable(name, type);
			if (internalVariables.size() == 0
					|| !internalVariables.containsKey(name)) {
				internalNames.add(name);
				Map<String, double[]> myCoefficients = makeCoefficientMap();
				var.setMyCoefficients(myCoefficients);
				var.setMylastPValues(makeLasPvaluesMap());

				Map<String, double[]> myMbCusums = new HashMap<String, double[]>();
				for (int e = 0; e < externalNames.size(); e++) {
					String extName = externalNames.get(e);
					double[] MbCusum = new double[8];
					MbCusum[0] = 0;
					MbCusum[1] = 0;
					MbCusum[2] = 0;
					MbCusum[3] = 0;
					MbCusum[4] = 0;
					MbCusum[5] = 0;
					MbCusum[6] = 0;
					MbCusum[7] = 0;
					myMbCusums.put(extName, MbCusum);
				}
				/*double[] MbCusum = new double[8];
				MbCusum[0] = 0;
				MbCusum[1] = 0;
				MbCusum[2] = 0;
				MbCusum[3] = 0;
				MbCusum[4] = 0;
				MbCusum[5] = 0;
				MbCusum[6] = 0;
				MbCusum[7] = 0;
				myMbCusums.put("Rsq", MbCusum);*/
				var.setMyMbCusums(myMbCusums);

				internalVariables.put(name, var);
			}
		} else {
			Variable var = new Variable(name, type);
			if (externalVariables.size() == 0) {
				externalNames.add(name);
				externalVariables.put(name, var);
			} else if (!externalVariables.containsKey(name)) {
				// externalVariables.put(name, variable);
				externalNames.add(name);
				externalVariables.put(name, var);
			}
		}
	}

	/**
	 * This method updates an internal or external variable with a new full
	 * window of observed data points. This should be done just before
	 * regression analysis is carried out
	 * 
	 * @param type Internal or external
	 * @param name Name of the variable
	 * @param variable The most recent values
	 */
	public void updateVariable(String type, String name, LogoList variable) {
		if (type.toLowerCase().equals("int")) {
			InternalVariable var = null;
			if (internalVariables.containsKey(name)) {
				var = internalVariables.get(name);
				double[] input = convertListToArray(variable);
				var.setMyValues(input);
				internalVariables.put(name, var);
			}
		} else {
			Variable var = null;
			if (externalVariables.containsKey(name)) {
				var = externalVariables.get(name);
				double[] input = convertListToArray(variable);
				var.setMyValues(input);
				externalVariables.put(name, var);
			}
		}
	}
	
	/*********************************** LASSO model Selection **********************************************/
	public int runLassoNew() {
		ArrayList<String> intVext = new ArrayList<String>();
		ArrayList<String> extVInt = new ArrayList<String>();

		int countVarChosen = 0;

		// Begin by initialising internal and external
		String intName = internalNames.get(0);
		InternalVariable intVar = internalVariables.get(intName);
		double[] y = intVar.getMyValues();
		
		if(re == null){
			RengineStart();
		}
		re.eval("library(DETectRPackage)");
		re.assign(intName, y);
		re.eval("internal2 = as.data.frame(" + intName + ")");
		re.eval("internal = as.data.frame(" + intName + ")");
		
		String extName = externalNames.get(0);
		Variable extVar = externalVariables.get(extName);
		y = extVar.getMyValues();
		re.assign(extName, y);
		re.eval("external2 = as.data.frame(" + extName + ")");
		re.eval("external = as.data.frame(" + extName + ")");
		
		
		for (int i = 1; i < internalNames.size(); i++) {
			intName = internalNames.get(i);
			intVar = internalVariables.get(intName);
			y = intVar.getMyValues();

			re.assign(intName, y);
			String command = "internal = cbind(internal," + intName + ")";
			re.eval(command);
		}
		
		
		for (int i = 1; i < externalNames.size(); i++) {
			extName = externalNames.get(i);
			extVar = externalVariables.get(extName);
			y = extVar.getMyValues();

			re.assign(extName, y);
			re.eval("external = cbind(external," + extName + ")");
		}
		
		re.eval("theChosen <- chooseVariables(internal,external)");
		int i = 1;
		
		REXP result = re.eval("theChosen["+i+"]");
		while (result.asString() != null){
			String relName = result.asString();
			String parts[] = relName.split(":");
			String internalName = parts[0];
			String externalName = parts[1];
			intVar = internalVariables.get(internalName);
			intVar.externalUsed.add(externalName);
			internalVariables.put(internalName, intVar);
			internalUsed.add(internalName);
			i = i + 1;
			result = re.eval("theChosen["+i+"]");
		}

		// House keeping part
		Iterator<String> intUsedIterator = internalUsed.iterator();
		while (intUsedIterator.hasNext()) {
			ArrayList<String> extUsedList = new ArrayList<String>();
			String thisIName = intUsedIterator.next();
			InternalVariable varReport = internalVariables.get(thisIName);
			Iterator<String> extUsedIterator = varReport.externalUsed
					.iterator();
			while (extUsedIterator.hasNext()) {
				String extUsedName = extUsedIterator.next();
				extUsedList.add(extUsedName);
			}
			variablesUsed.put(thisIName, extUsedList);
		}
		
		return(internalUsed.size());
	}

	

	/*********************************** Regression Analysis Methods ****************************************/

	/**
	 * This method is used to trigger the regression analysis algorithm If the
	 * variables have not yet been chosen then a regression using all variables
	 * is done
	 */
	public void runRegressions() {
		if (internalUsed.size() > 0)
			runRegressionsSelect();
	}

	
	/**
	 * This method runs a regression analysis on all internal variables against
	 * all external variables.
	 */
	public void runRegressionsAll() {
		String intName = internalNames.get(0);
		InternalVariable var = internalVariables.get(intName);

		for (int i = 0; i < internalNames.size(); i++) {
			intName = internalNames.get(i);
			var = internalVariables.get(intName);

			double[] y = var.getMyValues();

			double[] e0 = externalVariables.get(externalNames.get(0))
					.getMyValues();
			double[] e1 = externalVariables.get(externalNames.get(1))
					.getMyValues();
			double[] e2 = externalVariables.get(externalNames.get(2))
					.getMyValues();
			double[] e3 = externalVariables.get(externalNames.get(3))
					.getMyValues();
			
			if(re == null)
				RengineStart();

			re.assign("y", y);
			re.assign("e0", e0);
			re.assign("e1", e1);
			re.assign("e2", e2);
			re.assign("e3", e3);

			re.eval("Mreg<-lm(y ~ e0 + e1 + e2 + e3)");

			double[] pvalues = new double[4];
			double[] coeffs = new double[4];

			REXP result = re.eval("coef(summary(Mreg))['e0','Pr(>|t|)']");
			pvalues[0] = result.asDouble();
			result = re.eval("coef(summary(Mreg))['e1','Pr(>|t|)']");
			pvalues[1] = result.asDouble();

			result = re.eval("coef(summary(Mreg))['e2','Pr(>|t|)']");
			pvalues[2] = result.asDouble();

			result = re.eval("coef(summary(Mreg))['e3','Pr(>|t|)']");
			pvalues[3] = result.asDouble();

			result = re.eval("summary(Mreg)$r.squared");
			double rsq = result.asDouble();


			Map<String, double[]> myCoefficients = var.getMyCoefficients();
			Map<String, double[]> myMbCusums = var.getMyMbCusums();

			double[] oldCoeffRsq = myCoefficients.get("Rsq");
			int oldCoeffLenRsq = 0;
			if (oldCoeffRsq != null) {
				oldCoeffLenRsq = oldCoeffRsq.length;
			}
			int newLenRsq = oldCoeffLenRsq + 1;
			if (oldCoeffLenRsq == WinSize) {
				newLenRsq = WinSize;
			}
			double[] newCoeffRsq = new double[newLenRsq];
			if (oldCoeffLenRsq < WinSize) {
				for (int len = 0; len < oldCoeffLenRsq; len++) {
					newCoeffRsq[len] = oldCoeffRsq[len];
				}
				newCoeffRsq[oldCoeffLenRsq] = rsq;
			} else {
				for (int len = 0; len < (WinSize - 1); len++) {
					newCoeffRsq[len] = oldCoeffRsq[len + 1];
				}
				newCoeffRsq[WinSize - 1] = rsq;
			}

			myCoefficients.put("Rsq", newCoeffRsq);

			/*double[] RMbCusums = myMbCusums.get("Rsq");

			RMbCusums[0] = rsq;
			RMbCusums[7] = RMbCusums[7] + 1;

			myMbCusums.put("Rsq", RMbCusums);*/

			for (int e = 0; e < externalNames.size(); e++) {
				String name = externalNames.get(e);
				double[] oldCoeff = myCoefficients.get(name);
				int oldCoeffLen = 0;
				if (oldCoeff != null) {
					oldCoeffLen = oldCoeff.length;
				}
				int newLen = oldCoeffLen + 1;
				if (oldCoeffLen == WinSize) {
					newLen = WinSize;
				}
				double[] newCoeff = new double[newLen];
				if (oldCoeffLen < WinSize) {
					for (int len = 0; len < oldCoeffLen; len++) {
						newCoeff[len] = oldCoeff[len];
					}
					newCoeff[oldCoeffLen] = pvalues[e];
				} else {
					for (int len = 0; len < (WinSize - 1); len++) {
						newCoeff[len] = oldCoeff[len + 1];
					}
					newCoeff[WinSize - 1] = pvalues[e];
				}
				myCoefficients.put(name, newCoeff);

				double[] MbCusums = myMbCusums.get(name);

				MbCusums[0] = pvalues[e];
				MbCusums[7] = Math.min(MbCusums[7] + 1, cusumWindow);
				myMbCusums.put(name, MbCusums);
			}
			var.setMyMbCusums(myMbCusums);
			var.setMyCoefficients(myCoefficients);
			internalVariables.put(intName, var);
		}
		runCusumAnalysis();
	}

	/**
	 * This method runs a regression analysis on variables that have been
	 * selected as having note worthy relationships As such, it should only be
	 * used after model selection has already been run
	 */
	public void runRegressionsSelect() {
		Iterator<String> internalUsedIterator = internalUsed.iterator();

		while (internalUsedIterator.hasNext()) {
			String intName = internalUsedIterator.next();
			InternalVariable var = internalVariables.get(intName);

			double[] y = var.getMyValues();
			
			if(re == null){
				RengineStart();
			}
			re.eval("library(DETectRPackage)");
			re.assign("y", y);
			re.eval("y <- injectNoiseVector(y)");
			String regressionStatement = "Mreg<-lm(y ~";

			
			Set<String> externalUsed = var.getExternalUsed();
			
			int regressionSize = externalUsed.size();

			Iterator<String> iteratorUsedExternal = externalUsed.iterator();
			int counter = 0;
			while (iteratorUsedExternal.hasNext()) {

				String extName = iteratorUsedExternal.next();

				String varCount = "e" + counter;
				counter = counter + 1;
				double[] e5 = externalVariables.get(extName).getMyValues();
				re.assign(varCount, e5);
				re.eval(varCount + " <- injectNoiseVector(" + varCount +")");
				regressionStatement = regressionStatement + " " + varCount
						+ " + ";
			}

			regressionStatement = regressionStatement.substring(0,
					regressionStatement.lastIndexOf("+") - 1);
			regressionStatement = regressionStatement + ")";
			re.eval(regressionStatement);
            
			double[] pvalues = new double[externalNames.size()];
			for (int i = 0; i < externalNames.size(); i++) {
				pvalues[i] = 0;
			}
			iteratorUsedExternal = externalUsed.iterator();
			counter = 0;
			while (iteratorUsedExternal.hasNext()) {
				String extName = iteratorUsedExternal.next();
				int index = externalNames.indexOf(extName);
				REXP result = re.eval("coef(summary(Mreg))['e" + counter
						+ "','Pr(>|t|)']"); // result.asDouble()
				pvalues[index] = result.asDouble();
				counter = counter + 1;
			}


			REXP result = re.eval("summary(Mreg)$r.squared");
			double rsq = result.asDouble();

			Map<String, double[]> myCoefficients = var.getMyCoefficients();
			Map<String, double[]> myMbCusums = var.getMyMbCusums();

			double[] oldCoeffRsq = myCoefficients.get("Rsq");
			int oldCoeffLenRsq = 0;
			if (oldCoeffRsq != null) {
				oldCoeffLenRsq = oldCoeffRsq.length;
			}
			int newLenRsq = oldCoeffLenRsq + 1;
			if (oldCoeffLenRsq == WinSize) {
				newLenRsq = WinSize;
			}
			double[] newCoeffRsq = new double[newLenRsq];
			if (oldCoeffLenRsq < WinSize) {
				for (int len = 0; len < oldCoeffLenRsq; len++) {
					newCoeffRsq[len] = oldCoeffRsq[len];
				}
				newCoeffRsq[oldCoeffLenRsq] = rsq;
			} else {
				for (int len = 0; len < (WinSize - 1); len++) {
					newCoeffRsq[len] = oldCoeffRsq[len + 1];
				}
				newCoeffRsq[WinSize - 1] = rsq;
			}

			myCoefficients.put("Rsq", newCoeffRsq);

			/*double[] RMbCusums = myMbCusums.get("Rsq");

			RMbCusums[0] = rsq;
			RMbCusums[7] = RMbCusums[7] + 1;

			myMbCusums.put("Rsq", RMbCusums);*/

			for (int e = 0; e < externalNames.size(); e++) {
				String name = externalNames.get(e);
				double[] oldCoeff = myCoefficients.get(name);
				int oldCoeffLen = 0;
				if (oldCoeff != null) {
					oldCoeffLen = oldCoeff.length;
				}
				int newLen = oldCoeffLen + 1;
				if (oldCoeffLen == WinSize) {
					newLen = WinSize;
				}
				double[] newCoeff = new double[newLen];
				if (oldCoeffLen < WinSize) {
					for (int len = 0; len < oldCoeffLen; len++) {
						newCoeff[len] = oldCoeff[len];
					}
					newCoeff[oldCoeffLen] = pvalues[e];
				} else {
					for (int len = 0; len < (WinSize - 1); len++) {
						newCoeff[len] = oldCoeff[len + 1];
					}
					newCoeff[WinSize - 1] = pvalues[e];
				}
				myCoefficients.put(name, newCoeff);

				double[] MbCusums = myMbCusums.get(name);
				//System.out.println(name + "," + e);
				MbCusums[0] = pvalues[e];
				MbCusums[7] = Math.min(MbCusums[7] + 1, cusumWindow);
				myMbCusums.put(name, MbCusums);
			}
			var.setMyMbCusums(myMbCusums);
			var.setMyCoefficients(myCoefficients);
			internalVariables.put(intName, var);
		}
		runCusumAnalysis();
	}

	

	/***************************** CUSUM ANALYSIS METHODS *******************************************/

	/**
	 * This method returns the latest results from CUSUM
	 * 
	 * @param varNum
	 * @return a LogoList of Coefficients from the latest CUSUM
	 */
	public LogoList getCusum(int varNum) {
		String intVar = internalNames.get(varNum);
		InternalVariable var = internalVariables.get(intVar);
		Map<String, double[]> myMBCusums = var.getMyMbCusums();
		LogoListBuilder returnList = new LogoListBuilder();
		for (int i = 0; i < externalNames.size(); i++) {
			double[] coeffs = myMBCusums.get(externalNames.get(i));
			double lastVal = Math.abs(coeffs[3]);
			lastVal = Math.max(lastVal, Math.abs(coeffs[4]));
			lastVal = Math.max(lastVal, Math.abs(coeffs[5]));
			lastVal = Math.max(lastVal, Math.abs(coeffs[6]));
			//System.out.println("Returning: " + lastVal);
			returnList.add(lastVal);
		}
		/*double[] coeffs = myMBCusums.get("Rsq");
		double lastVal = Math.abs(coeffs[3]);
		lastVal = Math.max(lastVal, Math.abs(coeffs[4]));
		lastVal = Math.max(lastVal, Math.abs(coeffs[5]));
		lastVal = Math.max(lastVal, Math.abs(coeffs[6]));
		returnList.add(lastVal);*/
		return returnList.toLogoList();
	}

	/**
	 * This method cycles through the internal variables and calls the CUSUM
	 * algorithm for each
	 */
	public void runCusumAnalysis() {
		Iterator<String> internalUsedIterator = internalUsed.iterator();
		while (internalUsedIterator.hasNext()) {
			String intName = internalUsedIterator.next();
			InternalVariable var = internalVariables.get(intName);
			newCusum(var);
		}
	}

	/**
	 * This method contains the CUSUM algortihm. It takes InternalVariable as
	 * input and then runs CUSUM for each relevant external variable. The CUSUM algorithm is an extension of
	 * the self-starting CUSUM algorithm outlined in Hawkins paper. (Check thesis)
	 * The values that are needed for the CUSUM are stored in the MbCusum contained in each internal variable.
	 * Check that object for details of what each individual index in this array contains.
	 * 
	 * @param var the Internal variable to be analysed.
	 */
	private void newCusum(InternalVariable var) {
		Map<String, double[]> myExtVarCusums = var.getMyMbCusums();
		Map<String, ArrayList<Double>> mylastPValues = var.getMylastPValues();
		// Get the names of the external variables that are being tracked for this internal variable.
		Set<String> coefNames = myExtVarCusums.keySet();
		Iterator<String> it = coefNames.iterator();
		// For each external variable, update the CUSUM.
		while (it.hasNext()) {
			String name = it.next();
			double[] coefficients = new double[8]; 
			if (var.getExternalUsed().contains(name)) { 
				coefficients = myExtVarCusums.get(name);
				ArrayList<Double> thisPValues = mylastPValues.get(name);

				double newXval = coefficients[0];
				double count = coefficients[7];
				double oldXmean = coefficients[1];
				double oldSerr = coefficients[2];

				// If the baseline is full, slide it by removing the oldest value.
				if (thisPValues.size() == cusumWindow)
					thisPValues.remove(0);

				// Add the newest Pvalue to the baseline.
				thisPValues.add(newXval);

				// Calculate the deviation
				double dev = newXval - oldXmean;

				double fir = (count - 1) / count;

				// IF the baseline is full, update the Cusums.
				if (count > cusumMin) {

					double aj = Math.sqrt(fir);
					double dof = (count - 2);
					double sqnum = dev * dev * fir;
					double sigd = 0.0;
					if (dev > 0)
						sigd = 1.0;
					else
						sigd = -1.0;

					double Fmult = ((8 * dof) + 1) / ((8 * dof) + 3);

					double Uj2 = Fmult
							* Math.sqrt(dof * Math.log(1 + sqnum / oldSerr))
							* sigd;

					double winsr = 3;
					if (Math.abs(Uj2) >= winsr) {
						double exter = ((winsr / Fmult) * (winsr / Fmult))
								/ dof;
						dev = sigd * Math.sqrt(oldSerr * (Math.exp(exter) - 1)
										/ fir);
						Uj2 = winsr * sigd;
					}
					double K = 1;
					double Vj = (Math.sqrt(Math.abs(Uj2)) - .822) / 0.349;
					double oldLp = coefficients[3];
					double newLp = oldLp + Uj2 - K;
					double oldLm = coefficients[4];
					double newLm = oldLm + Uj2 + K;
					double oldSp = coefficients[5];
					double newSp = oldSp + Vj - K;
					double oldSm = coefficients[6];
					double newSm = oldSm + Vj + K;

					coefficients[3] = Math.max(0, newLp);
					coefficients[4] = Math.min(0, newLm);
					coefficients[5] = Math.max(0, newSp);
					coefficients[6] = Math.min(0, newSm);

				}

				double[] target = new double[thisPValues.size()];
				for (int i = 0; i < target.length; i++) {
					target[i] = thisPValues.get(i); 
				}

				double newXMean = getMean(target);

				double newSerr = 0;
				for (int i = 0; i < target.length; i++) {
					double deviation = thisPValues.get(i) - newXMean; 
					newSerr = newSerr + (deviation * deviation);
				}
				
				coefficients[1] = newXMean;
				coefficients[2] = newSerr;
				mylastPValues.put(name, thisPValues);
			}

			else { 
				for (int i = 0; i < 8; i++) {
					coefficients[i] = 1;
				}
			}
			myExtVarCusums.put(name, coefficients);

		}
		var.setMyMbCusums(myExtVarCusums);

	}

	/******************** HELPER FUNCTIONS *****************************************/

	/**
	 * Helper function create a new Coefficients Map
	 * 
	 * @return
	 */
	private Map<String, double[]> makeCoefficientMap() {
		Map<String, double[]> myCoefficients = new HashMap<String, double[]>();
		for (int e = 0; e < externalNames.size(); e++) {
			String extName = externalNames.get(e);
			double[] coef = new double[1];
			coef[0] = 0;
			myCoefficients.put(extName, coef);
		}
		double[] coef = new double[1];
		coef[0] = 0;
		myCoefficients.put("Rsq", coef);
		return myCoefficients;
	}

	private Map<String, ArrayList<Double>> makeLasPvaluesMap() {
		Map<String, ArrayList<Double>> myLastPValues = new HashMap<String, ArrayList<Double>>();
		for (int e = 0; e < externalNames.size(); e++) {
			String extName = externalNames.get(e);
			ArrayList<Double> values = new ArrayList<Double>();

			myLastPValues.put(extName, values);
		}

		return myLastPValues;
	}

	/**
	 * Function to calculate the WFunc as part of the CUSUM process
	 * 
	 * @param x1
	 * @return
	 * 
	 */
	private double wFunc(double x1) {
		double wz = 1 / (Math.sqrt(2 * 3.14));
		double power = -(Math.pow(Math.abs(x1), 2)) / (2);

		wz = wz * (Math.exp(power));

		return wz;
	}

	/**
	 * Little helper function to calculate the SD of a array of double
	 * 
	 * @param x
	 * @return
	 */
	private double calcSD(double[] x) {
		double sd = 0;
		double mean = calcMean(x);
		double sum = 0;

		for (int i = 0; i < x.length; i++) {
			sum = sum + Math.pow((x[i] - mean), 2);
		}

		sd = Math.sqrt(sum / x.length);

		return sd;
	}

	/**
	 * Little helper function to calc the mean of a array of double
	 * 
	 * @param x
	 * @return
	 */
	private double calcMean(double[] x) {
		double mean = 0;

		double sum = 0;
		for (int i = 0; i < x.length; i++) {
			sum = sum + x[i];
		}
		mean = sum / x.length;
		return mean;
	}

	/**
	 * Little helper function to print a Double[][] array
	 * 
	 * @param input
	 * @param row
	 * @param col
	 */
	private void printDouble(double[][] input, int row, int col) {
		for (int r = 0; r < row; r++) {
			for (int c = 0; c < col; c++) {
				System.out.print(input[r][c] + " ");
			}
			System.out.print("\n");
		}
		System.out.print("\n");
	}

	/**
	 * Little helper function to print a Double[] array
	 * 
	 * @param input
	 * @param size
	 */
	private void printArray(double[] input, int size) {
		for (int r = 0; r < size; r++) {
			System.out.print(input[r] + " ");
		}
		System.out.print("\n");
	}

	/**
	 * Helper function to convert a LogoList to an Array
	 * 
	 * @param l
	 * @return
	 */
	private double[] convertListToArray(LogoList l) {
		int l_size = l.size();
		double[] y = new double[l_size];
		for (int x = 0; x < l_size; x++) {
			y[x] = Double.parseDouble(l.get(x).toString());
		}
		return y;
	}

	/**
	 * Helper Function to convert external map
	 * 
	 * @param size
	 * @return
	 * 
	 */
	private double[][] convertExternalToMap(int size) {
		int numOfExt = externalNames.size();
		double[][] x = new double[size][numOfExt];

		for (int e = 0; e < numOfExt; e++) {
			String name = externalNames.get(e);
			Variable var = externalVariables.get(name);
			double[] y = var.getMyValues();
			for (int i = 0; i < size; i++) {
				x[i][e] = y[i];
			}
		}
		return x;
	}

	/**
	 * Helper function to calculate the mean of an array
	 * @param data the array that you want to get the mean value of.
	 * @return the mean value
	 */
	private double getMean(double[] data) {
		double sum = 0.0;
		for (double a : data)
			sum += a;
		return sum / data.length;
	}

	/**
	 * Helper function to contact R and generate a random number.
	 * This random number is then returned
	 * @return the random number.
	 */
	public double getARandom(){
		double theError = 0.0;
		if(re != null){
			re.eval("library(DETectRPackage)");
			REXP result = re.eval("returnARandom()");
			String stringError = result.asString();
			theError = result.asDouble();
			//System.out.println("Error:" + theError);
		}
		return theError;
	}

}


/****************************************************************/

/*
 *  This section down contains old code that is no longer used.
 * 
 * 
	//This method is used to decide what variables to use in future regression analysis.
	public void decideVariablesOld() {

		String intName = internalNames.get(0);
		InternalVariable var = internalVariables.get(intName);

		// System.out.println("********************Start******************************");
		// System.out.println(this.name);
		Map<String, Map<String, Double>> values = new HashMap<String, Map<String, Double>>();

		// Step 1 - Get all the Pvalues from the first run
		for (int i = 0; i < internalNames.size(); i++) {
			intName = internalNames.get(i);
			var = internalVariables.get(intName);
			Map<String, double[]> coefficients = var.getMyCoefficients();
			Map<String, Double> extVarValues = new HashMap<String, Double>();
			for (int e = 0; e < externalNames.size(); e++) {
				String name = externalNames.get(e);
				extVarValues.put(name, coefficients.get(name)[1]);
				// System.out.println("Adding Hello " + intName + " : " + name +
				// " : " + coefficients.get(name)[1]);
			}
			values.put(intName, extVarValues);
			internalVariables.put(intName, var);
		}

		// Step 2 - Find the smallest PValue and add it to the watch list
		// This is done as you should monitor at least 1 relationship
		String smallestInt = "";
		String smallestExt = "";
		double smallP = Double.POSITIVE_INFINITY;

		for (int i = 0; i < internalNames.size(); i++) {
			intName = internalNames.get(i);
			var = internalVariables.get(intName);
			Map<String, Double> relationshipValues = values.get(intName);
			// System.out.println("Relationship Values size for " + intName +
			// " : " + values.size());
			for (int e = 0; e < externalNames.size(); e++) {
				String extName = externalNames.get(e);
				// System.out.println("Looking for " + extName);
				double currValue = relationshipValues.get(extName);
				if (currValue < smallP) {
					smallestInt = intName;
					smallestExt = extName;
					smallP = currValue;
				}
			}
			internalVariables.put(intName, var);
		}

		internalUsed.add(smallestInt);
		var = internalVariables.get(smallestInt);
		Set<String> extUsed = var.getExternalUsed();
		extUsed.add(smallestExt);
		var.setExternalUsed(extUsed);
		internalVariables.put(smallestInt, var);
		// System.out.println("Smallest is: " + smallestInt + " : " +
		// smallestExt);

		double pthreshold = Double
				.parseDouble(System.getProperty("PThreshold"));

		// Step 3 - Go back and add all others where the P-Value is inside the
		// threshold
		for (int i = 0; i < internalNames.size(); i++) {
			intName = internalNames.get(i);
			var = internalVariables.get(intName);
			Map<String, Double> relationshipValues = values.get(intName);
			Set<String> currentExtUsed = var.getExternalUsed();
			for (int e = 0; e < externalNames.size(); e++) {
				String extName = externalNames.get(e);
				double currValue = relationshipValues.get(extName);
				// System.out.println("Deciding if I should add: " + intName +
				// " : " + extName + " ? " + currValue);
				if (currValue <= pthreshold) {
					// System.out.println("Added");
					internalUsed.add(intName);
					currentExtUsed.add(extName);
				}
			}
			var.setExternalUsed(currentExtUsed);
			internalVariables.put(intName, var);
		}

		// Step 4 - Clear P-Values
		for (int i = 0; i < internalNames.size(); i++) {
			intName = internalNames.get(i);
			var = internalVariables.get(intName);
			Map<String, double[]> myCoefficients = makeCoefficientMap();
			var.setMyCoefficients(myCoefficients);
			internalVariables.put(intName, var);
		}

		// Step 5 - Report how i got on.
		// System.out.println("I selected " + internalUsed.size() +
		// "internal variables");

		Iterator<String> intUsedIterator = internalUsed.iterator();
		while (intUsedIterator.hasNext()) {
			ArrayList<String> extUsedList = new ArrayList<String>();
			String thisIName = intUsedIterator.next();
			InternalVariable varReport = internalVariables.get(thisIName);
			// System.out.println("Size of external Variables for " + thisIName
			// + " is " + varReport.externalUsed.size());
			Iterator<String> extUsedIterator = varReport.externalUsed
					.iterator();
			while (extUsedIterator.hasNext()) {
				String extUsedName = extUsedIterator.next();
				extUsedList.add(extUsedName);
				// System.out.println("Relationship between : " + thisIName +
				// " and " + extUsedName);
			}
			variablesUsed.put(thisIName, extUsedList);
		}
		// System.out.println("********************End******************************");

	}
 
 
 public void decideVariablesNew() {

		String intName = internalNames.get(0);
		InternalVariable var = internalVariables.get(intName);

		// System.out.println("********************Start******************************");
		// System.out.println(this.name);
		Map<String, Map<String, Double>> values = new HashMap<String, Map<String, Double>>();

		// Step 1 - Get all the Pvalues from the first run
		for (int i = 0; i < internalNames.size(); i++) {
			intName = internalNames.get(i);
			var = internalVariables.get(intName);
			Map<String, double[]> coefficients = var.getMyCoefficients();
			Map<String, Double> extVarValues = new HashMap<String, Double>();
			for (int e = 0; e < externalNames.size(); e++) {
				String name = externalNames.get(e);
				extVarValues.put(name, coefficients.get(name)[1]);
				// System.out.println("Adding Hello " + intName + " : " + name +
				// " : " + coefficients.get(name)[1]);
			}
			values.put(intName, extVarValues);
			internalVariables.put(intName, var);
		}

		double pthresholdLow = 0.2; // Double.parseDouble(System.getProperty("PThreshold"));
		double pthresholdHigh = 0.5;

		// Step 3 - Go back and add all others where the P-Value is inside the
		// threshold
		for (int i = 0; i < internalNames.size(); i++) {
			intName = internalNames.get(i);
			var = internalVariables.get(intName);
			Map<Integer, double[]> trackinglist = var.getSelectedVarTracker();
			double[] newValsLow = new double[countExt];
			double[] newValsHigh = new double[countExt];
			Map<String, Double> relationshipValues = values.get(intName);
			Set<String> currentExtUsed = var.getExternalUsed();

			for (int e = 0; e < externalNames.size(); e++) {
				String extName = externalNames.get(e);
				double currValue = relationshipValues.get(extName);
				// System.out.println("Deciding if I should add: " + intName +
				// " : " + extName + " ? " + currValue);
				if (currValue <= pthresholdLow) {
					// System.out.println("Added");
					// internalUsed.add(intName);
					// currentExtUsed.add(extName);
					newValsLow[e] = 1;
				} else {
					newValsLow[e] = 0;
				}

				if (currValue <= pthresholdHigh) {
					newValsHigh[e] = 1;
				} else {
					newValsHigh[e] = 0;
				}

			}
			trackinglist.put(1, newValsLow);
			trackinglist.put(2, newValsHigh);
			var.setSelectedVarTracker(trackinglist);

			var.setExternalUsed(currentExtUsed);
			internalVariables.put(intName, var);
		}

		// Step 4 - Clear P-Values
		for (int i = 0; i < internalNames.size(); i++) {
			intName = internalNames.get(i);
			var = internalVariables.get(intName);
			Map<String, double[]> myCoefficients = makeCoefficientMap();
			var.setMyCoefficients(myCoefficients);
			internalVariables.put(intName, var);
		}

		// System.out.println("********************End******************************");

	}
 

	public void runLasso() {
		ArrayList<String> intVext = new ArrayList<String>();
		ArrayList<String> extVInt = new ArrayList<String>();
		String intName = internalNames.get(0);
		InternalVariable var = internalVariables.get(intName);
		int countVarChosen = 0;
		for (int i = 0; i < internalNames.size(); i++) {
			intName = internalNames.get(i);
			var = internalVariables.get(intName);
			double[] y = var.getMyValues();
			double[] e0 = externalVariables.get(externalNames.get(0))
					.getMyValues();
			double[] e1 = externalVariables.get(externalNames.get(1))
					.getMyValues();
			double[] e2 = externalVariables.get(externalNames.get(2))
					.getMyValues();
			double[] e3 = externalVariables.get(externalNames.get(3))
					.getMyValues();
			double[] e4 = externalVariables.get(externalNames.get(4))
					.getMyValues();

			ArrayList<String> turtlesToPrint = new ArrayList<String>();
			turtlesToPrint.add("turtle-245");
			turtlesToPrint.add("turtle-144");
			turtlesToPrint.add("turtle-343");
			turtlesToPrint.add("turtle-180");
			turtlesToPrint.add("turtle-312");
			turtlesToPrint.add("turtle-291");
			turtlesToPrint.add("turtle-120");
			turtlesToPrint.add("turtle-152");
			turtlesToPrint.add("turtle-80");
			turtlesToPrint.add("turtle-219");

			if (turtlesToPrint.contains(this.name)) {
				System.out.println(this.name);
				System.out.println("IntName");
				printArray(y, y.length);
				System.out.println(externalNames.get(0));
				printArray(e0, e0.length);
				System.out.println(externalNames.get(1));
				printArray(e1, e1.length);
				System.out.println(externalNames.get(2));
				printArray(e2, e2.length);
				System.out.println(externalNames.get(3));
				printArray(e3, e3.length);
				System.out.println(externalNames.get(4));
				printArray(e4, e4.length);
			}
			re.assign("y", y);
			re.assign("e0", e0);
			re.assign("e1", e1);
			re.assign("e2", e2);
			re.assign("e3", e3);
			re.assign("e4", e4);

			re.eval("library(glmnet)");

			re.eval("XGroup<-cbind(e0,e1,e2,e3,e4)");
			re.eval("lassoRun.cv<-cv.glmnet(XGroup,y)");
			re.eval("lassofit<-glmnet(XGroup,y,alpha=1,nlambda=500)");
			re.eval("lassopred<-predict(lassofit,XGroup,s=lassoRun.cv$lambda.min)");
			re.eval("lassocoef<-predict(lassofit,s=lassoRun.cv$lambda.min,type=\"coefficients\")");

			double[] pvalues = new double[5];
			REXP result = re.eval("lassocoef[2,1]");
			// System.out.println(result);
			pvalues[0] = result.asDouble();
			result = re.eval("lassocoef[3,1]");
			pvalues[1] = result.asDouble();
			result = re.eval("lassocoef[4,1]");
			pvalues[2] = result.asDouble();
			result = re.eval("lassocoef[5,1]");
			pvalues[3] = result.asDouble();
			result = re.eval("lassocoef[6,1]");
			pvalues[4] = result.asDouble();

			Map<Integer, double[]> trackinglist = var.getSelectedVarTracker();
			double[] newValsLasso = new double[countExt];

			for (int c = 0; c < 5; c++) {
				if (Math.abs(pvalues[c]) != 0.0) {
					newValsLasso[c] = 1;
					// internalUsed.add(intName);
					int relNumber = 1 + (i * 5) + c;
					String relName = internalNames.get(i) + ":"
							+ externalNames.get(c);
					intVext.add(relName);
					// var.externalUsed.add(externalNames.get(c));
				} else {
					newValsLasso[c] = 0;
				}

			}
			trackinglist.put(3, newValsLasso);
			var.setSelectedVarTracker(trackinglist);

		}

		// This be used to be where the housekeeping part was
		// Now run External against Internal

		intName = externalNames.get(0);
		Variable varExt = externalVariables.get(intName);
		// Now run for external versus interal
		for (int i = 0; i < externalNames.size(); i++) {
			intName = externalNames.get(i);
			varExt = externalVariables.get(intName);
			double[] y = varExt.getMyValues();
			double[] e0 = internalVariables.get(internalNames.get(0))
					.getMyValues();
			double[] e1 = internalVariables.get(internalNames.get(1))
					.getMyValues();
			double[] e2 = internalVariables.get(internalNames.get(2))
					.getMyValues();
			double[] e3 = internalVariables.get(internalNames.get(3))
					.getMyValues();
			double[] e4 = internalVariables.get(internalNames.get(4))
					.getMyValues();


			re.assign("y", y);
			re.assign("e0", e0);
			re.assign("e1", e1);
			re.assign("e2", e2);
			re.assign("e3", e3);
			re.assign("e4", e4);

			re.eval("library(glmnet)");

			re.eval("XGroup<-cbind(e0,e1,e2,e3,e4)");
			re.eval("lassoRun.cv<-cv.glmnet(XGroup,y)");
			re.eval("lassofit<-glmnet(XGroup,y,alpha=1,nlambda=500)");
			re.eval("lassopred<-predict(lassofit,XGroup,s=lassoRun.cv$lambda.min)");
			re.eval("lassocoef<-predict(lassofit,s=lassoRun.cv$lambda.min,type=\"coefficients\")");

			double[] pvalues = new double[5];
			REXP result = re.eval("lassocoef[2,1]");
			// System.out.println(result);
			pvalues[0] = result.asDouble();
			result = re.eval("lassocoef[3,1]");
			pvalues[1] = result.asDouble();
			result = re.eval("lassocoef[4,1]");
			pvalues[2] = result.asDouble();
			result = re.eval("lassocoef[5,1]");
			pvalues[3] = result.asDouble();
			result = re.eval("lassocoef[6,1]");
			pvalues[4] = result.asDouble();

			for (int c = 0; c < 5; c++) {
				if (Math.abs(pvalues[c]) != 0.0) {
					int relNumber = 1 + (c * 5) + i;
					String relName = internalNames.get(c) + ":"
							+ externalNames.get(i);
					extVInt.add(relName);
				}
			}

		}
		// End
		String line = this.name;

		// Figure out what relationships were in both and record them
		Iterator<String> thisInt = intVext.iterator();
		while (thisInt.hasNext()) {
			String tester = thisInt.next();
			if (extVInt.contains(tester)) {
				// newValsLasso[c] = 1;
				String parts[] = tester.split(":");
				internalUsed.add(parts[0]);
				var = internalVariables.get(parts[0]);
				var.externalUsed.add(parts[1]);

				internalVariables.put(parts[0], var);
			}
		}

		// House keeping part
		Iterator<String> intUsedIterator = internalUsed.iterator();
		while (intUsedIterator.hasNext()) {
			ArrayList<String> extUsedList = new ArrayList<String>();
			String thisIName = intUsedIterator.next();
			InternalVariable varReport = internalVariables.get(thisIName);
			Iterator<String> extUsedIterator = varReport.externalUsed
					.iterator();
			while (extUsedIterator.hasNext()) {
				String extUsedName = extUsedIterator.next();
				extUsedList.add(extUsedName);
			}
			variablesUsed.put(thisIName, extUsedList);
		}

	

		// System.out.println(line);

		// System.out.println("**************************End***********************************");
		// System.out.println(this.name + ",Lasso," + internalUsed.size());
	}
	
	// Little Function to return the tracking flags for each ext var for
	// a specified internal variable (varNum). The level specifies whether it is
	// 1) p 0.2 2) p 0.5 or 3) lasso
	public LogoList getTrack(int varNum, int level) {
		String intVar = internalNames.get(varNum);
		InternalVariable var = internalVariables.get(intVar);
		Map<Integer, double[]> myTrackVals = var.getSelectedVarTracker();

		double[] requestVals = myTrackVals.get(level);

		LogoListBuilder returnList = new LogoListBuilder();
		for (int i = 0; i < countExt; i++) {
			returnList.add(requestVals[i]);
		}
		return returnList.toLogoList();
	}

	public double runCorrelationTest(LogoList variable1, LogoList variable2) {

		double[] input1 = convertListToArray(variable1);
		double[] input2 = convertListToArray(variable2);

		Rengine re = Rengine.getMainEngine();
		if (re == null)
			re = new Rengine(new String[] { "--vanilla" }, false, null);

		re.assign("x", input1);
		re.assign("y", input2);

		re.eval("z<-cor.test(x,y)");

		// re.eval("Mreg<-lm(y ~ e0 + e1 + e2 + e3)");

		double pvalue = 1.0;
		double[] coeffs = new double[4];

		REXP result = re.eval("z$p.value");
		pvalue = result.asDouble();
		re.end();

		return pvalue;
	}
	
	public LogoList getCoefficients(int varNum) {
		String intVar = internalNames.get(varNum);
		InternalVariable var = internalVariables.get(intVar);
		Map<String, double[]> myCoefficients = var.getMyCoefficients();
		LogoListBuilder returnList = new LogoListBuilder();
		for (int i = 0; i < externalNames.size(); i++) {
			double[] coeffs = myCoefficients.get(externalNames.get(i));
			double lastVal = coeffs[coeffs.length - 1];
			// System.out.println("Got: " + lastVal + " for : " +
			// externalNames.get(i));
			returnList.add(lastVal);
		}
		double[] coeffs = myCoefficients.get("Rsq");
		double lastVal = coeffs[coeffs.length - 1];
		returnList.add(lastVal);
		return returnList.toLogoList();
	}
	
	public String getExternSigVar() {
		String varName = "";
		if (internalUsed.size() > 0) {
			Iterator<String> intUsed_it = internalUsed.iterator();

			String intVarName = intUsed_it.next();

			InternalVariable intVar = internalVariables.get(intVarName);

			Set<String> extUsed = intVar.getExternalUsed();
			Iterator<String> extUsed_it = extUsed.iterator();
			String extVarName = extUsed_it.next();
			varName = extVarName;
		} else {
			varName = "empty";
		}
		System.out.println("Done");
		return varName;

	}
 
 */

