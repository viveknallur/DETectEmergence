package com.graphhopper.netlogo;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.nlogo.api.Argument;
import org.nlogo.api.Context;
import org.nlogo.api.DefaultClassManager;
import org.nlogo.api.DefaultCommand;
import org.nlogo.api.DefaultReporter;
import org.nlogo.api.Dump;
import org.nlogo.api.ExtensionException;
import org.nlogo.api.LogoException;
import org.nlogo.api.LogoList;
import org.nlogo.api.LogoListBuilder;
import org.nlogo.api.PrimitiveManager;
import org.nlogo.api.Syntax;


public class GhExtension extends DefaultClassManager{
	private static GraphHopperRouter graphRouter;
	private static HashMap<String, Navigator> cars;
	
	@Override
	public void load(PrimitiveManager primitiveManager) throws ExtensionException {
		// TODO Auto-generated method stub
		primitiveManager.addPrimitive("find-first-route", new RouteFinder());           //Done
		primitiveManager.addPrimitive("find-route", new FirstRouteFinder()); 
		primitiveManager.addPrimitive("next-street-name", new NextStreet());
		primitiveManager.addPrimitive("next-instruction", new NextInstruction());
		primitiveManager.addPrimitive("next-coords", new NextCoordinates());
		primitiveManager.addPrimitive("next-destination", new NextDestination());
		primitiveManager.addPrimitive("current-street", new CurrentStreet());
		primitiveManager.addPrimitive("current-osm", new CurrentOsm());
		primitiveManager.addPrimitive("next-osm", new NextOsm());
		primitiveManager.addPrimitive("osm-check", new OsmCheck());
		primitiveManager.addPrimitive("connect-gh", new ConnectGh()); 
		primitiveManager.addPrimitive("import-data", new ImportData());
		primitiveManager.addPrimitive("car-list", new CarList());
		primitiveManager.addPrimitive("trip-info", new TripInfo());
	}
	
	public static class RouteFinder extends DefaultReporter{
		
		public Syntax getSyntax(){
			return Syntax.reporterSyntax(new int[] {Syntax.StringType(), Syntax.NumberType(), 
					Syntax.NumberType(), Syntax.NumberType(), Syntax.NumberType()}, 
					Syntax.WildcardType());
		}
		
		@Override
		public Object report(Argument[] args, Context context)
						throws ExtensionException, LogoException {
			// TODO Auto-generated method stub
			String carName = args[0].getString();
			double startLat = args[1].getDoubleValue();
			double startLong = args[2].getDoubleValue();
			double endLat = args[3].getDoubleValue();
			double endLong = args[4].getDoubleValue();
			
			Navigator navigator = cars.get(carName);
			ArrayList<Long> edgeList = null;
			if(navigator != null){
				edgeList = navigator.parseRoute(graphRouter.generateNewRoute(startLat, startLong, endLat, endLong));
				cars.put(carName, navigator);
				if (edgeList == null) {
					edgeList = new ArrayList<Long>();
				}
			}
			return convertArrayToLogoList(edgeList, carName);
		}
		
		private LogoList convertArrayToLogoList (ArrayList<Long> in, String carName){
			LogoListBuilder returnList = new LogoListBuilder();
			//System.out.println(in.size());
			for(int i = 0; i < in.size(); i++){
				//System.out.println(i);
				returnList.add(in.get(i).toString());
			}
			//System.out.println("Done: " + carName);
			//System.out.println("Returning: " + l.toString());
			return returnList.toLogoList();
		}

	}
	
	public static class FirstRouteFinder extends DefaultCommand{
		
		public Syntax getSyntax(){
			return Syntax.commandSyntax(new int[] {Syntax.StringType(), Syntax.NumberType(), 
					Syntax.NumberType(), Syntax.NumberType(), Syntax.NumberType()});
			/*return Syntax.reporterSyntax(new int[] {Syntax.WildcardType(), Syntax.NumberType(), 
					Syntax.NumberType(), Syntax.NumberType(), Syntax.NumberType()},
	                   Syntax.NumberType());*/
		}
		
		@Override
		public void perform(Argument[] args, Context context)
						throws ExtensionException, LogoException {
			// TODO Auto-generated method stub
			String carName = args[0].getString();
			double startLat = args[1].getDoubleValue();
			double startLong = args[2].getDoubleValue();
			double endLat = args[3].getDoubleValue();
			double endLong = args[4].getDoubleValue();
			
			Navigator navigator = cars.get(carName);
			if(navigator != null){
				navigator.parseRoute(graphRouter.generateNewRoute(startLat, startLong, endLat, endLong));
				cars.put(carName, navigator);
			}
		}

	}
	
	
	public static class NextStreet extends DefaultReporter{
		
		public Syntax getSyntax(){
			return Syntax.reporterSyntax(new int[] {Syntax.StringType()},
	                   Syntax.StringType());
		}

		@Override
		public Object report(Argument[] args, Context arg1)
				throws ExtensionException, LogoException {
			// TODO Auto-generated method stub
			String carName = args[0].getString();
			
			Navigator navigator = cars.get(carName);
			String nextStreet = "";
			if(navigator != null){
				nextStreet = navigator.reportNextStreet();
				cars.put(carName, navigator);
			}
			return nextStreet;
		}	
	}


	public static class NextInstruction extends DefaultReporter{
	
		public Syntax getSyntax(){
			return Syntax.reporterSyntax(new int[] {Syntax.StringType()},
	                   Syntax.NumberType());
		}

		@Override
		public Object report(Argument[] args, Context arg1)
				throws ExtensionException, LogoException {
			// TODO Auto-generated method stub
			String carName = args[0].getString();
			
			Navigator navigator = cars.get(carName);
			int nextTurn = 0;
			if(navigator != null){
				nextTurn = navigator.reportNextInstruction();
				cars.put(carName, navigator);
			}
			
			return Double.valueOf(nextTurn);
		}
			
	}

	public static class NextCoordinates extends DefaultReporter{
		
		public Syntax getSyntax(){
			return Syntax.reporterSyntax(new int[] {Syntax.StringType()},
	                   Syntax.WildcardType());
		}

		@Override
		public Object report(Argument[] args, Context arg1)
				throws ExtensionException, LogoException {
			// TODO Auto-generated method stub
			
			String carName = args[0].getString();
			double[] point = new double[0];
			Navigator navigator = cars.get(carName);

			if(navigator != null){
				point = navigator.reportNextCoordinate();
				cars.put(carName, navigator);
			}
			 
			return convertArrayToLogoList(point);
		}
		
		private LogoList convertArrayToLogoList (double[] in){
			LogoListBuilder returnList = new LogoListBuilder();
			for(int i = 0; i < in.length; i++){
				returnList.add(in[i]);
			}
			//System.out.println("Returning: " + l.toString());
			return returnList.toLogoList();
		}
			
	}
	
	public static class TripInfo extends DefaultReporter{
		
		public Syntax getSyntax(){
			return Syntax.reporterSyntax(new int[] {Syntax.StringType()},
	                   Syntax.WildcardType());
		}

		@Override
		public Object report(Argument[] args, Context arg1)
				throws ExtensionException, LogoException {
			// TODO Auto-generated method stub
			
			String carName = args[0].getString();
			double[] info = new double[0];
			Navigator navigator = cars.get(carName);

			if(navigator != null){
				info = navigator.tripInfo();
				cars.put(carName, navigator);
			}
			 
			return convertArrayToLogoList(info);
		}
		
		private LogoList convertArrayToLogoList (double[] in){
			LogoListBuilder returnList = new LogoListBuilder();
			for(int i = 0; i < in.length; i++){
				returnList.add(in[i]);
			}
			//System.out.println("Returning: " + l.toString());
			return returnList.toLogoList();
		}
			
	}
	
	
	public static class NextDestination extends DefaultReporter{
		
		public Syntax getSyntax(){
			return Syntax.reporterSyntax(new int[] {Syntax.StringType()},
	                   Syntax.WildcardType());
		}

		@Override
		public Object report(Argument[] args, Context arg1)
				throws ExtensionException, LogoException {
			// TODO Auto-generated method stub
			
			String carName = args[0].getString();
			double[] point = new double[2];
			Navigator navigator = cars.get(carName);

			if(navigator != null){
				point = navigator.getNextDestination();
			//	System.out.println(point[0] + ", " + point[1]);
				cars.put(carName, navigator);
			}//else{
			//	System.out.println("Car not found: " + carName);
			//}
			return convertArrayToLogoList(point);
		}
		
		private LogoList convertArrayToLogoList (double[] in){
			LogoListBuilder returnList = new LogoListBuilder();
			for(int i = 0; i < in.length; i++){
				returnList.add(in[i]);
			}
			//System.out.println("Returning: " + l.toString());
			return returnList.toLogoList();
		}
			
	}

	public static class CurrentStreet extends DefaultReporter{
		
		public Syntax getSyntax(){
			return Syntax.reporterSyntax(new int[] {Syntax.StringType()},
	                   Syntax.StringType());
		}

		@Override
		public Object report(Argument[] args, Context arg1)
				throws ExtensionException, LogoException {
			// TODO Auto-generated method stub
			String carName = args[0].getString();
			String currentStreet = "";
			Navigator navigator = cars.get(carName);

			if(navigator != null){
				currentStreet = navigator.reportCurrentStreet();
				cars.put(carName, navigator);
			}
			return currentStreet;
		}	
	}
	
	public static class CurrentOsm extends DefaultReporter{
		
		public Syntax getSyntax(){
			return Syntax.reporterSyntax(new int[] {Syntax.StringType()},
	                   Syntax.StringType());
		}

		@Override
		public Object report(Argument[] args, Context arg1)
				throws ExtensionException, LogoException {
			// TODO Auto-generated method stub
			String carName = args[0].getString();
			String currentOsm = "";
			Navigator navigator = cars.get(carName);

			if(navigator != null){
				currentOsm = Objects.toString(navigator.reportCurrentOsm(), null);
				cars.put(carName, navigator);
			}
			return currentOsm;
		}	
	}
	
	public static class NextOsm extends DefaultReporter{
		
		public Syntax getSyntax(){
			return Syntax.reporterSyntax(new int[] {Syntax.StringType()},
	                   Syntax.StringType());
		}

		@Override
		public Object report(Argument[] args, Context arg1)
				throws ExtensionException, LogoException {
			// TODO Auto-generated method stub
			String carName = args[0].getString();
			long nextOsm = 0;
			Navigator navigator = cars.get(carName);

			if(navigator != null){
				nextOsm = navigator.reportNextOsm();
				cars.put(carName, navigator);
			}
			return Objects.toString(nextOsm, null);
		}	
	}
	
	public static class OsmCheck extends DefaultReporter{
		
		public Syntax getSyntax(){
			return Syntax.reporterSyntax(new int[] {Syntax.StringType(), Syntax.StringType()},
	                   Syntax.BooleanType());
		}

		@Override
		public Object report(Argument[] args, Context arg1)
				throws ExtensionException, LogoException {
			// TODO Auto-generated method stub
			String carName = args[0].getString();
			boolean result = false;
			Navigator navigator = cars.get(carName);
			String osmID = args[1].getString();
			if(navigator != null){
				result = navigator.osmCheck(osmID);
				cars.put(carName, navigator);
			}
			

			return result;
		}	
	}
	
	public static class ConnectGh extends DefaultCommand{
		
		public Syntax getSyntax(){
			return Syntax.commandSyntax(new int[] {});
		}
		
		@Override
		public void perform(Argument[] args, Context context)
						throws ExtensionException, LogoException {
			// TODO Auto-generated method stub
			graphRouter = new GraphHopperRouter();
			graphRouter.initConnection();
		}

	}
	
	public static class CloseGh extends DefaultCommand{
		
		public Syntax getSyntax(){
			return Syntax.commandSyntax(new int[] {Syntax.StringType()});
		}
		
		@Override
		public void perform(Argument[] args, Context context)
						throws ExtensionException, LogoException {
			// TODO Auto-generated method stub
			graphRouter.closeConnection();
		}

	}
	
	public static class ImportData extends DefaultCommand{
		
		public Syntax getSyntax(){
			return Syntax.commandSyntax(new int[] {Syntax.StringType()});
		}
		
		@Override
		public void perform(Argument[] args, Context context)
						throws ExtensionException, LogoException {
			// TODO Auto-generated method stub
			//System.out.println("About to load Cars!!");
			String date = args[0].getString();
			//String folder = "//home/eamonn/TaxiData/Manhattan/Finals/";
			String folder = System.getProperty("InputFolder");
			String filepath = folder + "/" + date + "-Map.ser";
			File f = new File(filepath);
			if(f.exists() && !f.isDirectory()){
				//System.out.println("It exists!!");
				try {
					readMyMap(filepath);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			else{
				//System.out.println("Nope gotta csv it");
				String csvFile = folder + "/" + date + "Dec_Count_Cut.csv";
				importAndProcessCsv(csvFile);
				try {
					writeMyMap(filepath);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
		}
		
		public void writeMyMap(String filepath) throws Exception{
	           FileOutputStream fos = new FileOutputStream(filepath);
	           ObjectOutputStream oos = new ObjectOutputStream(fos);
	           oos.writeObject(cars);
	           oos.close();
	    }
	    
	    @SuppressWarnings("unchecked")
		public void readMyMap(String filepath) throws Exception{
	    	 FileInputStream fis = new FileInputStream(filepath);
	         ObjectInputStream ois = new ObjectInputStream(fis);
	         cars = (HashMap<String, Navigator>) ois.readObject();
	         ois.close();
	    }
	    
	    public void importAndProcessCsv (String csvFile) {
	    	BufferedReader br = null;
	    	String line = "";
	    	String cvsSplitBy = ",";
	    	cars = new HashMap<String, Navigator>();
	    	try {
	    		br = new BufferedReader(new FileReader(csvFile));
	    		//Medallion,pickup_longitude,pickup_latitude,dropoff_longitude,dropoff_latitude
	    		line = br.readLine();
	    		String[] firstOne = line.split(cvsSplitBy);
	    		String currentMed = firstOne[0];
	    		Navigator thisNav = new Navigator(currentMed);
	    		double long1 = Double.parseDouble(firstOne[1]);
	    		double lat1 = Double.parseDouble(firstOne[2]);
	    		double long2 = Double.parseDouble(firstOne[3]);
	    		double lat2 = Double.parseDouble(firstOne[4]);
	    		double distance = Double.parseDouble(firstOne[5]);
	    		double duration = Double.parseDouble(firstOne[6]);
	    		
	    		thisNav.addDestination(lat1, long1);
	    		thisNav.addDestination(lat2, long2);
	    		thisNav.addDistance(distance);
	    		thisNav.addDuration(duration);
	    		
	    		while ((line = br.readLine()) != null) {
	    		        // use comma as separator
	    			String[] trip = line.split(cvsSplitBy);
	    			String thisMed = trip[0];
	    			long1 = Double.parseDouble(trip[1]);
		    		lat1 = Double.parseDouble(trip[2]);
		    		long2 = Double.parseDouble(trip[3]);
		    		lat2 = Double.parseDouble(trip[4]);
		    		distance = Double.parseDouble(trip[5]);
		    		duration = Double.parseDouble(trip[6]);
	    			if(!thisMed.equals(currentMed)) {
	    				//Different Medallion so store existing one
	    				cars.put(currentMed, thisNav);
	    				currentMed = thisMed;
	    				thisNav = new Navigator(currentMed);
	    			}
	    			//Add destinations
	    			thisNav.addDestination(lat1, long1);
    	    		thisNav.addDestination(lat2, long2);
    	    		thisNav.addDistance(distance);
    	    		thisNav.addDuration(duration);
	    		}
	     
	    	} catch (FileNotFoundException e) {
	    		e.printStackTrace();
	    	} catch (IOException e) {
	    		e.printStackTrace();
	    	} finally {
	    		if (br != null) {
	    			try {
	    				br.close();
	    			} catch (IOException e) {
	    				e.printStackTrace();
	    			}
	    		}
	    	}
	    }

	}
	
	public static class CarList extends DefaultReporter{
		
		public Syntax getSyntax(){
			return Syntax.reporterSyntax(new int[] {},
	                   Syntax.WildcardType());
		}

		@Override
		public Object report(Argument[] args, Context arg1)
				throws ExtensionException, LogoException {
			// TODO Auto-generated method st
			Set<String> carNames = cars.keySet();
			 
			return convertSetToLogoList(carNames);
		}
		
		private LogoList convertSetToLogoList (Set<String> in){
			LogoListBuilder returnList = new LogoListBuilder();
			Iterator<String> carIterator = in.iterator();
			while(carIterator.hasNext()){
				String car = carIterator.next();
				returnList.add(car);
			}
			//System.out.println("Returning: " + l.toString());
			return returnList.toLogoList();
		}
			
	}
	
	
}
