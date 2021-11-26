package com.DETect.netlogo;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.stat.correlation.Covariance;
import org.apache.commons.math3.stat.regression.OLSMultipleLinearRegression;
import org.nlogo.api.*;

/**
 * @author Eamonn O'Toole
 * This class is used to connect NetLogo with the underlying functionality that makes up
 * DETect, found in the DETect class. This is necessitated by the way in which NetLogo 
 * extensions are built. For more information or to extend this work, please check the NetLogo website.
 */
public class NetLogoApi extends DefaultClassManager {
	/**
	 * All agents in the simulation have a Detect Object associated with them. These are stored in a 
	 * Hashmap and retrieved when each individual agent needs to use DETect.
	 */
	private static Map<String, DETect> detectObjects;

	/* (non-Javadoc)
	 * @see org.nlogo.api.DefaultClassManager#load(org.nlogo.api.PrimitiveManager)
	 * The load function is used to load the primitives or commands that can be used
	 * in a NetLogo script to call specific DETect functionality. Each primitive is
	 * associated with a sub-class.
	 */
	@Override
	public void load(PrimitiveManager primitiveManager)
			throws ExtensionException {
		// These are the list of commands used in NetLogo.
		primitiveManager.addPrimitive("new-data", new NewDETectObject());
		primitiveManager.addPrimitive("add-var", new AddVariables());
		primitiveManager.addPrimitive("update-var", new UpdateVariable());
		primitiveManager.addPrimitive("run-regress", new RunRegression());
		primitiveManager.addPrimitive("report-Cusum", new GetCusum());
		primitiveManager.addPrimitive("initialise", new Initialise());
		primitiveManager.addPrimitive("itest", new ITest());
		primitiveManager.addPrimitive("printVariables", new PrintVariable());
		primitiveManager.addPrimitive("runLasso", new RunLasso());
		primitiveManager.addPrimitive("startR", new StartR());
		primitiveManager.addPrimitive("stopR", new StopR());
		primitiveManager.addPrimitive("getRandom", new GetRandom());
	}
	
	/**
	 * @author Eamonn
	 * This is used to Initialise this class during a simulation run. All it does is create a new empty
	 * Hashmap for detectObjects
	 */
	public static class Initialise extends DefaultCommand {
		public Syntax getSyntax() {
			return Syntax.commandSyntax(new int[] {});
		}
		@Override
		public void perform(Argument[] args, Context context)
				throws ExtensionException, LogoException {
			detectObjects = new HashMap<String, DETect>();
		}
	}
	
	/**
	 * @author Eamonn
	 * The NewDETectObject class is used to initialise a new DETect object for an agent.
	 * The first argument passed in is the number of Internal Variables.
	 * The Second argument is the number of External Variables.
	 * Finally the third argument is name of the agent. This is used when saving the DETect
	 * object in the HashMap detectObjects. 
	 */
	public static class NewDETectObject extends DefaultCommand {
		public Syntax getSyntax() {
			return Syntax.commandSyntax(new int[] { Syntax.StringType(),
					Syntax.StringType(), Syntax.StringType() });
		}
		@Override
		public void perform(Argument[] arg0, Context arg1)
				throws ExtensionException, LogoException {
			// TODO Auto-generated method stub
			int intCount = Integer.parseInt(arg0[0].getString());
			int extCount = Integer.parseInt(arg0[1].getString());
			String name = arg0[2].getString();
			//System.out.println("Here");
			DETect lData = new DETect(intCount, extCount, name);
			detectObjects.put(name, lData);
		}

	}

	/**
	 * @author Eamonn
	 * This is used to add a new variable to DETect. 
	 * The first argument is the name of the agent.
	 */
	public static class AddVariables extends DefaultCommand {
		public Syntax getSyntax() {
			return Syntax.commandSyntax(new int[] { Syntax.StringType(),
					Syntax.StringType(), Syntax.StringType() });
		}

		@Override
		public void perform(Argument[] args, Context context)
				throws ExtensionException, LogoException {
			// Get the arguments passed from NetLogo and
			// retrieve the agent's DETect object
			String argName = args[0].get().toString();
			DETect data = detectObjects.get(argName);

			// Is it an internal or external variable being added
			String type = args[1].get().toString();

			// Now get the variable name
			String name = args[2].get().toString();
			
			// Call the DETect functionality and put the object back in the Map.
			data.addVariable(type, name);
			detectObjects.put(argName, data);
		}

	}

	/**
	 * @author Eamonn
	 * This class is used to update a variable with the most recent full Sliding Observation window
	 * for that variable. It is called for each variable being monitored on the agent.
	 * The first argument is the agent's name.
	 * The second argument is the variable type, (internal or external)
	 * The third argument is variable name.
	 * Finally the forth argument is the Sliding observation window, as a LogoList object (array).
	 */
	public static class UpdateVariable extends DefaultCommand {
		public Syntax getSyntax() {
			return Syntax
					.commandSyntax(new int[] { Syntax.StringType(),
							Syntax.StringType(), Syntax.StringType(),
							Syntax.ListType() });
		}

		@Override
		public void perform(Argument[] args, Context context)
				throws ExtensionException, LogoException {
			String agentName = args[0].get().toString();
			DETect data = detectObjects.get(agentName);
			// Is it an internal or external variable
			String type = args[1].get().toString();
			// Now get the variable name
			String variableName = args[2].get().toString();
			// Lastly the variable data
			Object variableWindow = args[3].get();
			LogoList variableList = (LogoList) variableWindow;
			data.updateVariable(type, variableName, variableList);
			detectObjects.put(agentName, data);
		}

	}

	/**
	 * @author Eamonn
	 * This is used to initiate the MLR analysis for the variable relationships in the selected model.
	 * The running of a regression analysis also triggers the CUSUM analysis
	 * The first and only argument passed is the agent name.
	 */
	public static class RunRegression extends DefaultCommand {

		public Syntax getSyntax() {
			return Syntax.commandSyntax(new int[] { Syntax.StringType() });
		}

		@Override
		public void perform(Argument[] args, Context context)
				throws ExtensionException, LogoException {
			String agentName = args[0].get().toString();
			DETect data = detectObjects.get(agentName);

			data.runRegressions();

			detectObjects.put(agentName, data);
		}

	}

	/**
	 * @author Eamonn
	 * This is used to retrieve the current value of the CUSUM for each variable relationship being monitored in
	 * by DETect for a specific internal variable. 
	 * The first argument is the agent name. The second argument is the index of the specific internal variable.
	 * TO DO: Change this so that it is not the index that is used but instead the variable name.
	 * The returned object is a LogoList (array) of each CUSUM for the specific internal variable.
	 */
	public static class GetCusum extends DefaultReporter {
		public Syntax getSyntax() {
			return Syntax.reporterSyntax(new int[] { Syntax.StringType(),
					Syntax.StringType() }, Syntax.WildcardType());
		}
		@Override
		public Object report(Argument[] arg0, Context arg1)
				throws ExtensionException, LogoException {

			String agentName = arg0[0].get().toString();
			DETect data = detectObjects.get(agentName);

			// Index of the variable
			int varIndex = Integer.parseInt(arg0[1].get().toString());
			LogoList returnList = data.getCusum(varIndex);
			detectObjects.put(agentName, data);
			return returnList;
		}
	}

	
	/**
	 * @author Eamonn
	 * This is used to simply test that everything is set up correctly before the simulation begins running.
	 * The check made is that there are objects in the hashmap detectObjects. If everything is not correct,
	 * calling this will cause the simulation to crash.
	 */
	public static class ITest extends DefaultCommand {
		public Syntax getSyntax() {
			return Syntax.commandSyntax(new int[] {});
		}

		@Override
		public void perform(Argument[] args, Context context)
				throws ExtensionException, LogoException {
			Set<String> names = detectObjects.keySet();
			Iterator<String> it = names.iterator();
			while (it.hasNext()) {
				String thisName = it.next();
				DETect l = detectObjects.get(thisName);
			}
			// detectObjects = new HashMap<String,LogoData>();

		}

	}

	/**
	 * @author Eamonn
	 * This is used to print the variable relationships being tracked by DETect for each agent in the simulation.
	 * It is typically called after 20,000 time-steps to ensure that all agents will have selected a model when
	 * it is run. The variables are printed in a csv file on the local machine with the filename passed from 
	 * NetLogo, typically being the run name.
	 */
	public static class PrintVariable extends DefaultCommand {

		public Syntax getSyntax() {
			return Syntax.commandSyntax(new int[] {});
		}

		@Override
		public void perform(Argument[] args, Context context)
				throws ExtensionException, LogoException {
			// TODO Auto-generated method stub
			Set<String> names = detectObjects.keySet();
			Iterator<String> it = names.iterator();

			String fileName = System.getProperty("RunName");

			PrintWriter outputStream = null;

			try {
				outputStream = new PrintWriter(new FileWriter(
						"/home/eamonn/netlogo/GISWork/" + fileName
								+ "-VariableInfo.csv"));
				while (it.hasNext()) {
					String thisName = it.next();
					DETect l = detectObjects.get(thisName);

					String newLine = thisName + ",";
					int count = 0;
					Map<String, ArrayList<String>> variablesUsed = l
							.getVariablesUsed();
					String variableDetails = "";

					Set<String> intVars = variablesUsed.keySet();
					Iterator<String> iteratorIntVars = intVars.iterator();

					while (iteratorIntVars.hasNext()) {
						String varName = iteratorIntVars.next();
						ArrayList<String> exVars = variablesUsed.get(varName);
						count += exVars.size();
						variableDetails = variableDetails + "," + varName + ":"
								+ exVars;
					}

					newLine = newLine + count + variableDetails;

					outputStream.println(newLine);
				}

				outputStream.close();

			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}

	}

	/**
	 * @author Eamonn
	 * This is used to initiate the model selection functionality which uses the Lasso to select the 
	 * Internal-External variable pairs that will be monitored by DETect.
	 * The argument passed in is the name of the agent.
	 * The return argument is the number of variable pairs that were selected. If this is zero, the agent
	 * will re-run this again in the future until at least one relationship is selected.
	 */
	public static class RunLasso extends DefaultReporter {
		public Syntax getSyntax() {
			return Syntax.reporterSyntax(new int[] { Syntax.StringType() },
					Syntax.NumberType());
		}
		@Override
		public Object report(Argument[] arg0, Context arg1)
				throws ExtensionException, LogoException {
			String argName = arg0[0].get().toString();

			DETect data = detectObjects.get(argName);

			int numb = data.runLassoNew();
			String returner = String.valueOf(numb);

			detectObjects.put(argName, data);

			return Double.parseDouble(returner);
		}
	}

	/**
	 * @author Eamonn
	 * This is used to create a connection to R.
	 * The only argument passed is the agent name.
	 */
	public static class StartR extends DefaultCommand {

		public Syntax getSyntax() {
			return Syntax.commandSyntax(new int[] { Syntax.StringType()});
		}

		@Override
		public void perform(Argument[] args, Context context)
				throws ExtensionException, LogoException {
			// TODO Auto-generated method stub
			String argName = args[0].get().toString();
			DETect data = detectObjects.get(argName);

			data.RengineStart();

			detectObjects.put(argName, data);
		}
	}

	/**
	 * @author Eamonn
	 * This is used to close a connection to R.
	 * The only argument passed is the name of the agent.
	 */
	public static class StopR extends DefaultCommand {

		public Syntax getSyntax() {
			return Syntax.commandSyntax(new int[] { Syntax.StringType()});
		}

		@Override
		public void perform(Argument[] args, Context context)
				throws ExtensionException, LogoException {
			// TODO Auto-generated method stub
			String argName = args[0].get().toString();
			DETect data = detectObjects.get(argName);

			data.RengineStop();

			detectObjects.put(argName, data);
		}

	}
	
	/**
	 * @author Eamonn
	 * This is used to generate a random number. This random number is used to inject noise to each observation
	 * of a variable. NetLogo random numbers dont appear to be entirely random!!
	 */
	public static class GetRandom extends DefaultReporter {
		public Syntax getSyntax() {
			return Syntax.reporterSyntax(new int[] { Syntax.StringType() },
					Syntax.NumberType());
		}

		@Override
		public Object report(Argument[] arg0, Context arg1)
				throws ExtensionException, LogoException {

			String argName = arg0[0].get().toString();

			DETect data = detectObjects.get(argName);

			double numb = data.getARandom();

			detectObjects.put(argName, data);

			return numb;
		}
	}
}


/******************************************************************************************/



/*
 *  This section contains code that is no longer used.
 *  
	primitiveManager.addPrimitive("cor-test", new GetCorTest());
	primitiveManager.addPrimitive("sig-test", new getSigTest());
	primitiveManager.addPrimitive("report-rsq", new GetRSquared());
	primitiveManager.addPrimitive("report-coef", new GetCoefficient());
	primitiveManager.addPrimitive("decide-variables", new DecideVariables());
	primitiveManager.addPrimitive("time-predict", new MyReporter());
	primitiveManager.addPrimitive("returnExtSign", new ReturnExtSignificant());
	primitiveManager.addPrimitive("report-Variable-Track", new GetTrack());

	public static class GetRSquared extends DefaultReporter {
		public Syntax getSyntax() {
			return Syntax.reporterSyntax(new int[] { Syntax.StringType() },
					Syntax.WildcardType());
		}

		@Override
		public Object report(Argument[] arg0, Context arg1)
				throws ExtensionException, LogoException {
			// TODO Auto-generated method stub

			String argName = arg0[0].get().toString();

			DETect data = detectObjects.get(argName);

			double returnR = data.getAvgRSq();

			detectObjects.put(argName, data);

			return returnR;
		}
	}

	public static class GetCoefficient extends DefaultReporter {
		public Syntax getSyntax() {
			return Syntax.reporterSyntax(new int[] { Syntax.StringType(),
					Syntax.StringType() }, Syntax.WildcardType());
		}

		@Override
		public Object report(Argument[] arg0, Context arg1)
				throws ExtensionException, LogoException {
			// TODO Auto-generated method stub

			String argName = arg0[0].get().toString();

			DETect data = detectObjects.get(argName);

			// Is it internal or external
			int var = Integer.parseInt(arg0[1].get().toString());

			LogoList returnList = data.getCoefficients(var);

			detectObjects.put(argName, data);

			return returnList;
		}
	}
	
	public static class DecideVariables extends DefaultCommand {

		public Syntax getSyntax() {
			return Syntax.commandSyntax(new int[] { Syntax.StringType() });
		}

		@Override
		public void perform(Argument[] args, Context context)
				throws ExtensionException, LogoException {
			// TODO Auto-generated method stub
			String argName = args[0].get().toString();

			DETect data = detectObjects.get(argName);

			detectObjects.put(argName, data);
			// data.decideVariables();

		}

	}

	public static class getSigTest extends DefaultReporter {
		public Syntax getSyntax() {
			return Syntax.reporterSyntax(new int[] { Syntax.StringType() },
					Syntax.NumberType());
		}

		@Override
		public Object report(Argument[] arg0, Context arg1)
				throws ExtensionException, LogoException {
			// TODO Auto-generated method stub

			String argName = arg0[0].get().toString();

			DETect data = detectObjects.get(argName);

			int numb = data.isSignifChange();
			String returner = String.valueOf(numb);

			detectObjects.put(argName, data);

			return Double.parseDouble(returner);
		}
	}
	
	public static class GetCorTest extends DefaultReporter {
		public Syntax getSyntax() {
			return Syntax.reporterSyntax(
					new int[] { Syntax.StringType(), Syntax.ListType(),
							Syntax.ListType(), Syntax.StringType() },
					Syntax.StringType());
		}

		@Override
		public Object report(Argument[] arg0, Context arg1)
				throws ExtensionException, LogoException {
			// TODO Auto-generated method stub

			String argName = arg0[0].get().toString();

			DETect data = detectObjects.get(argName);

			Object homeList = arg0[1].get();
			LogoList homeVariableList = (LogoList) homeList;

			Object neighbourList = arg0[2].get();
			LogoList neighbourVariableList = (LogoList) neighbourList;

			String threshold = arg0[3].getString();
			double thresDouble = Double.parseDouble(threshold);

			double pvalue = data.runCorrelationTest(homeVariableList,
					neighbourVariableList);

			detectObjects.put(argName, data);

			if (pvalue <= thresDouble)
				return "yes";
			else
				return "no";
		}
	}
	
	public static class ReturnExtSignificant extends DefaultReporter {
		public Syntax getSyntax() {
			return Syntax.reporterSyntax(new int[] { Syntax.StringType() },
					Syntax.StringType());
		}

		@Override
		public Object report(Argument[] arg0, Context arg1)
				throws ExtensionException, LogoException {
			// TODO Auto-generated method stub

			String argName = arg0[0].get().toString();

			DETect data = detectObjects.get(argName);

			String varName = data.getExternSigVar();

			detectObjects.put(argName, data);

			return varName;
		}
	}

	public static class GetTrack extends DefaultReporter {
		public Syntax getSyntax() {
			return Syntax.reporterSyntax(new int[] { Syntax.StringType(),
					Syntax.StringType(), Syntax.StringType() },
					Syntax.WildcardType());
		}

		@Override
		public Object report(Argument[] arg0, Context arg1)
				throws ExtensionException, LogoException {
			// TODO Auto-generated method stub

			String argName = arg0[0].get().toString();

			DETect data = detectObjects.get(argName);

			// Is it internal or external
			int var = Integer.parseInt(arg0[1].get().toString());
			int level = Integer.parseInt(arg0[2].get().toString());
			LogoList returnList = data.getTrack(var, level);

			detectObjects.put(argName, data);

			return returnList;
		}
	}

*/