package com.eamonn.TrafficChangeSpec;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.rosuda.JRI.Rengine;



import scala.actors.threadpool.Arrays;

public class Main {
	public static void main(String[] args) {
		int firstFolder = 100;
		while (firstFolder < 101) {
			// listFilesForFolder(folder);
			//System.out.println("VM " + firstFolder);
			int runCount = 1;
			while (runCount < 2) {
				//System.out.println("Run " + runCount);
				final File folder = new File("/home/eamonn/netlogo/EmergenceTests/SecondRuns/Consensus2/Traffic"
						+ "/Run00" + runCount);
				executeRun(folder, runCount);
				//listFilesInOrderOld(folder);
				runCount = runCount + 1;
			}
			runCount = 10;
			while (runCount < 10) {
				//System.out.println("Run " + runCount);
				final File folder = new File("/home/eamonn/netlogo/EmergenceTests/SecondRuns/Consensus2/Traffic"
						+ "/Run0" + runCount);
				executeRun(folder, runCount);
				//listFilesInOrderOld(folder);
				runCount = runCount + 1;
			}
			
			firstFolder = firstFolder + 1;
		}
		System.out.println("Done");
	}

	public static void listFilesForFolder(final File folder) {
		for (final File fileEntry : folder.listFiles()) {
			if (fileEntry.isDirectory()) {
				listFilesForFolder(fileEntry);
			} else {
				System.out.println(fileEntry.getName());
			}
		}
	}
	
	public static void executeRun(final File folder , final int runCount) {
		RunSummary runSumm = new RunSummary();
		String folderName = folder.getAbsolutePath() + "/";
		//System.out.println(folderName);
		File theFileList = new File(folderName + "LogFiles/");
		File[] listOfFiles = theFileList.listFiles();
		int printcount = 1;
		if (listOfFiles.length > 0) {
			String firstName = listOfFiles[0].getName();
			String fileNameStart = firstName.substring(0,
					firstName.lastIndexOf("-") + 1);
			String fileEnd = ".csv";
			Map<String, PostCode> postCodeRecords = new HashMap<String, PostCode>();
			
			BufferedReader inputStream = null;
			PrintWriter outputStream = null;
			

			Set<String> thisEmerged = new HashSet<String>();


			try {
				
				int counter = 50;
				File theFile = new File(folderName + "LogFiles/"
						+ fileNameStart + counter + fileEnd);

				while (theFile.exists()) {
					Set<String> congStreetsNow = new HashSet<String>();

					//System.out.println(theFile.getName());
					inputStream = new BufferedReader(new FileReader(folderName
							+ "LogFiles/" + theFile.getName()));

					inputStream.readLine(); // **Global Statistics**
					//runSumm.addToNeighSize(counter, Double.valueOf(inputStream.readLine().trim()));
					inputStream.readLine(); // **Headers
					inputStream.readLine(); // **Global Stats
					// Line now has global car stats
					inputStream.readLine(); // **Manhole Indiviuals
					inputStream.readLine(); // **Big Congestion**
					inputStream.readLine(); // Count

					// Cycle through the list of manholes with "Big" congestion
					String newLine = inputStream.readLine();
					while (!newLine.startsWith("**")) {
						String[] parts = newLine.split(",");
						//congStreetsNow.add(parts[0]);
						newLine = inputStream.readLine();
					}
					printFunction(printcount, newLine);
					printcount += 1;
					
					// Have found **Neighborhood Congestion
					newLine = inputStream.readLine(); // count
					
					// Cycle through the list of neighborhood congestion
					newLine = inputStream.readLine();
					while (!newLine.startsWith("**")) {
						String[] parts = newLine.split(",");
						double score = Double.parseDouble(parts[1]);
						// if(score > 0.0000)
						congStreetsNow.add(parts[0]);
						newLine = inputStream.readLine();
					}
					
					runSumm.addToObjectMeasure(counter, congStreetsNow);

					printFunction(printcount, newLine);
					printcount += 1;
					// 4. Cars Threshold Low
					
					// Have found 4. Cars Threshold Low**
					Set<String> switchedThisTime = new HashSet<String>();
					printFunction(printcount, inputStream.readLine());

					HashMap<String, Set<String>> switchTrackThisTime = new HashMap<String, Set<String>>();

					// Set<String> switchTracker = new HashSet<String>();
					switchTrackThisTime.put("cnMys", new HashSet<String>());
					switchTrackThisTime.put("dnMys", new HashSet<String>());
					switchTrackThisTime.put("chMys", new HashSet<String>());
					switchTrackThisTime.put("csMys", new HashSet<String>());
					switchTrackThisTime.put("cnMyh", new HashSet<String>());
					switchTrackThisTime.put("dnMyh", new HashSet<String>());
					switchTrackThisTime.put("chMyh", new HashSet<String>());
					switchTrackThisTime.put("csMyh", new HashSet<String>());
                    
					printcount += 1;
					inputStream.readLine(); // Count
					
					
					HashMap<Integer,Set<String>> specificRelsThisTime = new HashMap<Integer,Set<String>>();
					Set<String> specCars = new HashSet<String>();
					
					// This next loop, loops through coeff-cn-myS_cusum
					Set<String> uniqueCars = new HashSet<String>();
					newLine = inputStream.readLine();
					while (!newLine.startsWith("**")) {
						String[] parts = newLine.split(",");
						uniqueCars.add(parts[0]);
						switchedThisTime.add(parts[0]);
						newLine = inputStream.readLine();
						specCars.add(parts[0]);
					}
					
					specificRelsThisTime.put(1, specCars);
					specCars = new HashSet<String>();
					
					
					printFunction(printcount, newLine);
					printcount += 1;

					// Have found **coeff-dn-myS-cusum
					inputStream.readLine(); // Count
					// This next loop, loops through coeff-dn-myS_cusum
					newLine = inputStream.readLine();
					while (!newLine.startsWith("**")) {
						String[] parts = newLine.split(",");
						uniqueCars.add(parts[0]);
						switchedThisTime.add(parts[0]);
						newLine = inputStream.readLine();
						specCars.add(parts[0]);
					}
					
					specificRelsThisTime.put(2, specCars);
					specCars = new HashSet<String>();
					
					printFunction(printcount, newLine);
					printcount += 1;
					
					// Have found **coeff-ch-myS-cusum
					inputStream.readLine(); // Count
					// This next loop, loops through coeff-ch-myS_cusum
					newLine = inputStream.readLine();
					while (!newLine.startsWith("**")) {
						String[] parts = newLine.split(",");
						switchedThisTime.add(parts[0]);
						newLine = inputStream.readLine();
						specCars.add(parts[0]);
					}
					
					specificRelsThisTime.put(3, specCars);
					specCars = new HashSet<String>();
					printFunction(printcount, newLine);
					printcount += 1;
					
					// Have found **coeff-cs-myS-cusum
					inputStream.readLine(); // Count
					// This next loop, loops through coeff-cs-myS_cusum
					newLine = inputStream.readLine();
					while (!newLine.startsWith("**")) {
						String[] parts = newLine.split(",");
						uniqueCars.add(parts[0]);
						switchedThisTime.add(parts[0]);
						newLine = inputStream.readLine();
						specCars.add(parts[0]);
					}
					
					specificRelsThisTime.put(4, specCars);
					specCars = new HashSet<String>();
					printFunction(printcount, newLine);
					printcount += 1;
					
					// Have found **coeff-tm-myS-cusum
					inputStream.readLine(); // Count
					// This next loop, loops through coeff-cs-myS_cusum
					newLine = inputStream.readLine();
					while (!newLine.startsWith("**")) {
						String[] parts = newLine.split(",");
						uniqueCars.add(parts[0]);
						switchedThisTime.add(parts[0]);
						newLine = inputStream.readLine();
						specCars.add(parts[0]);
					}
					
					specificRelsThisTime.put(5, specCars);
					specCars = new HashSet<String>();
					printFunction(printcount, newLine);
					printcount += 1;
					
					// Have found **coeff-dd-myS-cusum
					inputStream.readLine(); // Count
					// This next loop, loops through coeff-cs-myS_cusum
					newLine = inputStream.readLine();
					while (!newLine.startsWith("**")) {
						String[] parts = newLine.split(",");
						uniqueCars.add(parts[0]);
						switchedThisTime.add(parts[0]);
						newLine = inputStream.readLine();
						
					}
					
					
					printFunction(printcount, newLine);
					printcount += 1;
					
					// Now MyHEADING
					// Have found **coeff-cn-myH-cusum
					inputStream.readLine(); // Count
					// This next loop, loops through coeff-cn-myH_cusum
					newLine = inputStream.readLine();
					while (!newLine.startsWith("**")) {
						String[] parts = newLine.split(",");
						uniqueCars.add(parts[0]);
						switchedThisTime.add(parts[0]);
						newLine = inputStream.readLine();
						specCars.add(parts[0]);
					}
					
					specificRelsThisTime.put(6, specCars);
					specCars = new HashSet<String>();
					printFunction(printcount, newLine);
					printcount += 1;
					// Have found **coeff-dn-myH-cusum
					inputStream.readLine(); // Count
					// This next loop, loops through coeff-dn-myH_cusum
					newLine = inputStream.readLine();
					while (!newLine.startsWith("**")) {
						String[] parts = newLine.split(",");
						uniqueCars.add(parts[0]);
						switchedThisTime.add(parts[0]);
						newLine = inputStream.readLine();
						specCars.add(parts[0]);
					}
					
					specificRelsThisTime.put(7, specCars);
					specCars = new HashSet<String>();
					printFunction(printcount, newLine);
					printcount += 1;
					// Have found **coeff-ch-myH-cusum
					inputStream.readLine(); // Count
					// This next loop, loops through coeff-ch-myH_cusum
					newLine = inputStream.readLine();
					while (!newLine.startsWith("**")) {
						String[] parts = newLine.split(",");
						uniqueCars.add(parts[0]);
						switchedThisTime.add(parts[0]);
						newLine = inputStream.readLine();
						specCars.add(parts[0]);
					}
					
					specificRelsThisTime.put(8, specCars);
					specCars = new HashSet<String>();

					printFunction(printcount, newLine);
					printcount += 1;
					
					// Have found **coeff-cs-myH-cusum
					inputStream.readLine(); // Count
					// This next loop, loops through coeff-cs-myH_cusum
					newLine = inputStream.readLine();
					while (!newLine.startsWith("**")) {
						String[] parts = newLine.split(",");
						uniqueCars.add(parts[0]);
						switchedThisTime.add(parts[0]);
						newLine = inputStream.readLine();
						specCars.add(parts[0]);
					}
					
					specificRelsThisTime.put(9, specCars);
					specCars = new HashSet<String>();
					
					printFunction(printcount, newLine);
					printcount += 1;
					
					// Have found **coeff-tm-myH-cusum
					inputStream.readLine(); // Count
					// This next loop, loops through coeff-cs-myH_cusum
					newLine = inputStream.readLine();
					while (!newLine.startsWith("**")) {
						String[] parts = newLine.split(",");
						uniqueCars.add(parts[0]);
						switchedThisTime.add(parts[0]);
						newLine = inputStream.readLine();
						specCars.add(parts[0]);
					}
					
					specificRelsThisTime.put(10, specCars);
					specCars = new HashSet<String>();
					
					printFunction(printcount, newLine);
					printcount += 1;
					
					// Have found **coeff-dd-myH-cusum
					inputStream.readLine(); // Count
					// This next loop, loops through coeff-cs-myH_cusum
					newLine = inputStream.readLine();
					while (!newLine.startsWith("**")) {
						String[] parts = newLine.split(",");
						uniqueCars.add(parts[0]);
						switchedThisTime.add(parts[0]);
						newLine = inputStream.readLine();
						
					}
					
					// NOW FOR AGE
					
					// Have found **coeff-cn-Age-cusum
					inputStream.readLine(); // Count
					// This next loop, loops through coeff-cn-Age_cusum
					newLine = inputStream.readLine();
					while (!newLine.startsWith("**")) {
						String[] parts = newLine.split(",");
						uniqueCars.add(parts[0]);
						switchedThisTime.add(parts[0]);
						newLine = inputStream.readLine();
						specCars.add(parts[0]);
					}
					
					specificRelsThisTime.put(11, specCars);
					specCars = new HashSet<String>();
					printFunction(printcount, newLine);
					printcount += 1;
					
					// Have found **coeff-dn-Age-cusum
					inputStream.readLine(); // Count
					// This next loop, loops through coeff-dn-Age_cusum
					newLine = inputStream.readLine();
					while (!newLine.startsWith("**")) {
						String[] parts = newLine.split(",");
						uniqueCars.add(parts[0]);
						switchedThisTime.add(parts[0]);
						newLine = inputStream.readLine();
						specCars.add(parts[0]);
					}
					
					specificRelsThisTime.put(12, specCars);
					specCars = new HashSet<String>();
					
					printFunction(printcount, newLine);
					printcount += 1;
					
					// Have found **coeff-ch-Age-cusum
					inputStream.readLine(); // Count
					// This next loop, loops through coeff-ch-Age_cusum
					newLine = inputStream.readLine();
					while (!newLine.startsWith("**")) {
						String[] parts = newLine.split(",");
						uniqueCars.add(parts[0]);
						switchedThisTime.add(parts[0]);
						newLine = inputStream.readLine();
						specCars.add(parts[0]);
					}
					
					specificRelsThisTime.put(13, specCars);
					specCars = new HashSet<String>();

					printFunction(printcount, newLine);
					printcount += 1;
					
					// Have found **coeff-cs-Age-cusum
					inputStream.readLine(); // Count
					// This next loop, loops through coeff-cs-Age_cusum
					newLine = inputStream.readLine();
					while (!newLine.startsWith("**")) {
						String[] parts = newLine.split(",");
						uniqueCars.add(parts[0]);
						switchedThisTime.add(parts[0]);
						newLine = inputStream.readLine();
						specCars.add(parts[0]);
					}
					
					specificRelsThisTime.put(14, specCars);
					specCars = new HashSet<String>();
					
					printFunction(printcount, newLine);
					printcount += 1;
					
					// Have found **coeff-tm-Age-cusum
					inputStream.readLine(); // Count
					// This next loop, loops through coeff-tm-Age_cusum
					newLine = inputStream.readLine();
					while (!newLine.startsWith("**")) {
						String[] parts = newLine.split(",");
						uniqueCars.add(parts[0]);
						switchedThisTime.add(parts[0]);
						newLine = inputStream.readLine();
						specCars.add(parts[0]);
					}
					
					specificRelsThisTime.put(15, specCars);
					specCars = new HashSet<String>();
					
					printFunction(printcount, newLine);
					printcount += 1;
					
					// Have found **coeff-dd-Age-cusum
					inputStream.readLine(); // Count
					// This next loop, loops through coeff-dd-Age_cusum
					newLine = inputStream.readLine();
					while (!newLine.startsWith("**")) {
						String[] parts = newLine.split(",");
						uniqueCars.add(parts[0]);
						switchedThisTime.add(parts[0]);
						newLine = inputStream.readLine();
					}
					
					runSumm.addToCarChange(counter, uniqueCars);
					runSumm.addToSetSpecificRels(counter, specificRelsThisTime);
					
					printFunction(printcount, newLine);
					printcount += 1;

					inputStream.readLine();
					// This next loop, loops through coeff-RsQ-myS_cusum
					newLine = inputStream.readLine();
					while (!newLine.startsWith("**")) {
						newLine = inputStream.readLine();
					}
					
					printFunction(printcount, newLine);
					printcount += 1;

					inputStream.readLine();
					// This next loop, loops through coeff-RsQ-myH_cusum
					newLine = inputStream.readLine();
					while (!newLine.startsWith("**")) {
						newLine = inputStream.readLine();
					}
					
					printFunction(printcount, newLine);
					printcount += 1;
					
					inputStream.readLine(); // Count
					// Emerge Belief
					uniqueCars = new HashSet<String>();
					thisEmerged = new HashSet<String>();
					List<Double> values = new ArrayList<Double>();
					
					newLine = inputStream.readLine();
					while (!newLine.startsWith("**")) {
						String[] parts = newLine.split(",");
						double thisSize = Double.parseDouble(parts[2]);
						
						thisEmerged.add(parts[0]);
						newLine = inputStream.readLine();
					}
					
					
					runSumm.addToEmergeBelief(counter, thisEmerged);
					
					newLine = inputStream.readLine();
					

					// High Threshold PostCodes
					newLine = inputStream.readLine();
					while (!newLine
							.startsWith("**Postal-Areas Cars low Threshold**")) {
						newLine = inputStream.readLine();
					}

					printFunction(printcount, newLine);
					printcount += 1;
					newLine = inputStream.readLine(); //count

					newLine = inputStream.readLine();
					// First do Cars
					String cvsSplitBy = ",";
					while (!newLine.startsWith("**")) {
						// System.out.println("14 " + newLine);
						String[] record = newLine.split(cvsSplitBy);
						String postName = record[0];
						String postCount = record[1];
						String carsList = record[2];
						String[] indivCars = carsList.split(" ");
						PostCode thisPostCode;
						if (!postCodeRecords.containsKey(postName)) {
							thisPostCode = new PostCode(postName);
						} else {
							thisPostCode = postCodeRecords.get(postName);
						}
						thisPostCode.addCarCountL(String.valueOf(counter),
								indivCars.length);
						postCodeRecords.put(postName, thisPostCode);
						newLine = inputStream.readLine(); // Count
					}
					printFunction(printcount, newLine);
					printcount += 1;
					
					// Should now be at : **Postal-Areas Manhole Lots**
					// Next do ManLots
					newLine = inputStream.readLine(); // Count
					newLine = inputStream.readLine(); // Count

					while (!newLine.startsWith("**")) {
						String[] record = newLine.split(cvsSplitBy);
						String postName = record[0];
						String postCount = record[1];
						PostCode thisPostCode;
						if (!postCodeRecords.containsKey(postName)) {
							thisPostCode = new PostCode(postName);
						} else {
							thisPostCode = postCodeRecords.get(postName);
						}
						thisPostCode.addManCountLots(String.valueOf(counter),
								Integer.parseInt(postCount));
						postCodeRecords.put(postName, thisPostCode);
						newLine = inputStream.readLine(); // Count
					}
					printFunction(printcount, newLine);
					printcount += 1;
					
					//Should now be at **Postal-Areas Manhole Neighborhood**
					// Next do ManNeigh
					//congStreetsNow = new HashSet<String>();
					newLine = inputStream.readLine(); // Count
					newLine = inputStream.readLine(); // Count
					while (newLine != null && !newLine.startsWith("**")) {
						String[] record = newLine.split(cvsSplitBy);
						//System.out.println(newLine);
						String postName = record[0];
						String postCount = record[1];
						if(Integer.parseInt(postCount) >= 3){
							//congStreetsNow.add(postName);
						}
						PostCode thisPostCode;
						if (!postCodeRecords.containsKey(postName)) {
							thisPostCode = new PostCode(postName);
						} else {
							thisPostCode = postCodeRecords.get(postName);
						}
						thisPostCode.addManCountNeigh(String.valueOf(counter),
								Integer.parseInt(postCount));
						postCodeRecords.put(postName, thisPostCode);
						newLine = inputStream.readLine(); // Count
					}
					printFunction(printcount, newLine);
					printcount += 1;
					//runSumm.addToObjectMeasure(counter, congStreetsNow);
					counter = counter + 50;
					theFile = new File(folderName + "LogFiles/" + fileNameStart
							+ counter + fileEnd);

				}
				printRunSumm(runSumm, fileNameStart, runCount);
				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} finally {
				try {
					if (inputStream != null) {
						inputStream.close();
					}
					if (outputStream != null) {
						outputStream.close();
					}
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}
	

	public static void listFilesInOrderOld(final File folder) {
		String folderName = folder.getAbsolutePath() + "/";
		//System.out.println(folderName);
		File theFileList = new File(folderName + "LogFiles/");
		File[] listOfFiles = theFileList.listFiles();
		int printcount = 1;
		if (listOfFiles.length > 0) {
			String firstName = listOfFiles[0].getName();
			String fileNameStart = firstName.substring(0,
					firstName.lastIndexOf("-") + 1);
			String fileEnd = ".csv";
			Map<String, PostCode> postCodeRecords = new HashMap<String, PostCode>();
			Set<String> congStreets = new HashSet<String>();
			BufferedReader inputStream = null;
			PrintWriter outputStream = null;
			
			Set<String> lastEmerged = new HashSet<String>();
			Set<String> thisEmerged = new HashSet<String>();


			try {
				outputStream = new PrintWriter(new FileWriter(folderName
						+ fileNameStart + "summary" + fileEnd));
				String line = "NeighSize,CarsStopped"
						+",man_big_congested,man_neigh_congested,Unique_Cars_Low," +
						"Unique_Change_Count,Unique_Change_Count2,"
						+ "cnMys,dnMys,chMys,csMys,cnMyh,dnMyh,chMyh,csMyh,"
						+ "cnMys2,dnMys2,chMys2,csMys2,cnMyh2,dnMyh2,chMyh2,csMyh2,"
						+ "streetsChanging,streetsGettingCong,streetsLosingCong,"
						+ "EmergeBelief,NewEmerge,LostEmerge,NewEmAvg,Median,Min,Max,"
						+ "post_withcarsL,"
						+ "post_withManLots,post_WithManNeigh";;
				/*String aline = "MatesF,global_coeff-cn-myS_cusum,global_coeff-dn-myS_cusum,global_coeff-ch-myS_cusum,"
						+ "global_coeff-cs-myS_cusum,global_coeff-cn-myH_cusum,global_coeff-dn-myH_cusum,"
						+ "global_coeff-ch-myH_cusum,global_coeff-cs-myH_cusum,global_coeff-Rsq-myS_cusum,"
						+ "global_coeff-Rsq-myH_cusum,man_some_congested,man_big_congested,man_neigh_congested,Unique_Cars_High," // Unique_Cars_Switch,"
						+ "Unique_Cars_Mid_High,Unique_Cars_Mid_Low,Unique_Cars_Low,Unique_Change_Count,Unique_Change_Count2,"
						+ "cnMys,dnMys,chMys,csMys,cnMyh,dnMyh,chMyh,csMyh,"
						+ "cnMys2,dnMys2,chMys2,csMys2,cnMyh2,dnMyh2,chMyh2,csMyh2,"
						+ "streetsChanging,streetsGettingCong,streetsLosingCong,"
						+ "EmergeBelief,NewEmerge,LostEmerge,NewEmAvg,Median,Min,Max,"
						+ "post_withcarsH,post_withcarsMH,post_withcarsML,post_withcarsL,post_withManSome,"
						+ "post_withManLots,post_WithManNeigh";*/
				outputStream.println(line);
				int counter = 50;
				File theFile = new File(folderName + "LogFiles/"
						+ fileNameStart + counter + fileEnd);
				Set<String> carCountsSwitch = new HashSet<String>();
				Set<String> carCountsNoSwitch = new HashSet<String>();

				HashMap<String, Set<String>> switchTrack = new HashMap<String, Set<String>>();

				Set<String> switchTracker = new HashSet<String>();
				switchTrack.put("cnMys", new HashSet<String>());
				switchTrack.put("dnMys", new HashSet<String>());
				switchTrack.put("chMys", new HashSet<String>());
				switchTrack.put("csMys", new HashSet<String>());
				switchTrack.put("cnMyh", new HashSet<String>());
				switchTrack.put("dnMyh", new HashSet<String>());
				switchTrack.put("chMyh", new HashSet<String>());
				switchTrack.put("csMyh", new HashSet<String>());

				while (theFile.exists()) {
					Set<String> congStreetsNow = new HashSet<String>();
					Set<String> meanNeiSizeNow = new HashSet<String>();
					//System.out.println(theFile.getName());
					inputStream = new BufferedReader(new FileReader(folderName
							+ "LogFiles/" + theFile.getName()));

					inputStream.readLine(); // **Global Statistics**
					line = inputStream.readLine();
					inputStream.readLine(); // **Headers
					line = line + "," + inputStream.readLine(); // **Global Stats
					// Line now has global car stats
					inputStream.readLine(); // **Manhole Indiviuals
					inputStream.readLine(); // **Big Congestion**
					String newLine = inputStream.readLine(); // Count

					line = line + "," + newLine;

					// Cycle through the list of manholes with "Big" congestion
					newLine = inputStream.readLine();
					while (!newLine.startsWith("**")) {
						newLine = inputStream.readLine();
					}
					printFunction(printcount, newLine);
					printcount += 1;
					// Have found **Neighborhood Congestion
					newLine = inputStream.readLine(); // count
					line = line + "," + newLine;
					// Cycle through the list of neighborhood congestion
					newLine = inputStream.readLine();
					while (!newLine.startsWith("**")) {
						String[] parts = newLine.split(",");
						double score = Double.parseDouble(parts[1]);
						// if(score > 0.0000)
						congStreetsNow.add(parts[0]);
						newLine = inputStream.readLine();
					}

					printFunction(printcount, newLine);
					printcount += 1;
					// 4. Cars Threshold Low
					
					// Have found 4. Cars Threshold Low**
					Set<String> switchedThisTime = new HashSet<String>();
					printFunction(printcount, inputStream.readLine());

					HashMap<String, Set<String>> switchTrackThisTime = new HashMap<String, Set<String>>();

					// Set<String> switchTracker = new HashSet<String>();
					switchTrackThisTime.put("cnMys", new HashSet<String>());
					switchTrackThisTime.put("dnMys", new HashSet<String>());
					switchTrackThisTime.put("chMys", new HashSet<String>());
					switchTrackThisTime.put("csMys", new HashSet<String>());
					switchTrackThisTime.put("cnMyh", new HashSet<String>());
					switchTrackThisTime.put("dnMyh", new HashSet<String>());
					switchTrackThisTime.put("chMyh", new HashSet<String>());
					switchTrackThisTime.put("csMyh", new HashSet<String>());
                    
					printcount += 1;
					inputStream.readLine(); // Count
					
					// This next loop, loops through coeff-cn-myS_cusum
					Set<String> uniqueCars = new HashSet<String>();
					newLine = inputStream.readLine();
					while (!newLine.startsWith("**")) {
						String[] parts = newLine.split(",");
						String currName = "cnMys";
						switchTrackThisTime = updateSet(switchTrackThisTime,
								currName, parts[0]);
						carCountsNoSwitch.add(parts[0]);
						uniqueCars.add(parts[0]);
						switchedThisTime.add(parts[0]);
						newLine = inputStream.readLine();
					}
					printFunction(printcount, newLine);
					printcount += 1;

					// Have found **coeff-dn-myS-cusum
					inputStream.readLine(); // Count
					// This next loop, loops through coeff-dn-myS_cusum
					newLine = inputStream.readLine();
					while (!newLine.startsWith("**")) {
						String[] parts = newLine.split(",");
						String currName = "dnMys";
						switchTrackThisTime = updateSet(switchTrackThisTime,
								currName, parts[0]);
						carCountsNoSwitch.add(parts[0]);
						uniqueCars.add(parts[0]);
						switchedThisTime.add(parts[0]);
						newLine = inputStream.readLine();
					}
					printFunction(printcount, newLine);
					printcount += 1;
					// Have found **coeff-ch-myS-cusum
					inputStream.readLine(); // Count
					// This next loop, loops through coeff-ch-myS_cusum
					newLine = inputStream.readLine();
					while (!newLine.startsWith("**")) {
						String[] parts = newLine.split(",");
						String currName = "chMys";
						switchTrackThisTime = updateSet(switchTrackThisTime,
								currName, parts[0]);
						carCountsNoSwitch.add(parts[0]);
						uniqueCars.add(parts[0]);
						switchedThisTime.add(parts[0]);
						newLine = inputStream.readLine();
					}
					printFunction(printcount, newLine);
					printcount += 1;
					// Have found **coeff-cs-myS-cusum
					inputStream.readLine(); // Count
					// This next loop, loops through coeff-cs-myS_cusum
					newLine = inputStream.readLine();
					while (!newLine.startsWith("**")) {
						String[] parts = newLine.split(",");
						String currName = "csMys";
						switchTrackThisTime = updateSet(switchTrackThisTime,
								currName, parts[0]);
						carCountsNoSwitch.add(parts[0]);
						uniqueCars.add(parts[0]);
						switchedThisTime.add(parts[0]);
						newLine = inputStream.readLine();
					}
					printFunction(printcount, newLine);
					printcount += 1;
					// Have found **coeff-cn-myH-cusum
					inputStream.readLine(); // Count
					// This next loop, loops through coeff-cn-myH_cusum
					newLine = inputStream.readLine();
					while (!newLine.startsWith("**")) {
						String[] parts = newLine.split(",");
						String currName = "cnMyh";
						switchTrackThisTime = updateSet(switchTrackThisTime,
								currName, parts[0]);
						carCountsNoSwitch.add(parts[0]);
						uniqueCars.add(parts[0]);
						switchedThisTime.add(parts[0]);
						newLine = inputStream.readLine();
					}
					printFunction(printcount, newLine);
					printcount += 1;
					// Have found **coeff-dn-myH-cusum
					inputStream.readLine(); // Count
					// This next loop, loops through coeff-dn-myH_cusum
					newLine = inputStream.readLine();
					while (!newLine.startsWith("**")) {
						String[] parts = newLine.split(",");
						String currName = "dnMyh";
						switchTrackThisTime = updateSet(switchTrackThisTime,
								currName, parts[0]);
						carCountsNoSwitch.add(parts[0]);
						uniqueCars.add(parts[0]);
						switchedThisTime.add(parts[0]);
						newLine = inputStream.readLine();
					}
					printFunction(printcount, newLine);
					printcount += 1;
					// Have found **coeff-ch-myH-cusum
					inputStream.readLine(); // Count
					// This next loop, loops through coeff-ch-myH_cusum
					newLine = inputStream.readLine();
					while (!newLine.startsWith("**")) {
						String[] parts = newLine.split(",");
						String currName = "chMyh";
						switchTrackThisTime = updateSet(switchTrackThisTime,
								currName, parts[0]);
						carCountsNoSwitch.add(parts[0]);
						uniqueCars.add(parts[0]);
						switchedThisTime.add(parts[0]);
						newLine = inputStream.readLine();
					}

					printFunction(printcount, newLine);
					printcount += 1;
					// Have found **coeff-cs-myH-cusum
					inputStream.readLine(); // Count
					// This next loop, loops through coeff-cs-myH_cusum
					newLine = inputStream.readLine();
					while (!newLine.startsWith("**")) {
						String[] parts = newLine.split(",");
						String currName = "csMyh";
						switchTrackThisTime = updateSet(switchTrackThisTime,
								currName, parts[0]);
						carCountsNoSwitch.add(parts[0]);

						uniqueCars.add(parts[0]);
						switchedThisTime.add(parts[0]);
						newLine = inputStream.readLine();
					}

					printFunction(printcount, newLine);
					printcount += 1;

					inputStream.readLine();
					// This next loop, loops through coeff-RsQ-myS_cusum
					newLine = inputStream.readLine();
					while (!newLine.startsWith("**")) {
						newLine = inputStream.readLine();
					}
					
					printFunction(printcount, newLine);
					printcount += 1;

					inputStream.readLine();
					// This next loop, loops through coeff-RsQ-myH_cusum
					newLine = inputStream.readLine();
					while (!newLine.startsWith("**")) {
						newLine = inputStream.readLine();
					}

					int countUniqueCarsL = uniqueCars.size();

					Set<String> relNames = switchTrack.keySet();
					Iterator<String> relNames_it = relNames.iterator();
					Set<String> superSet = new HashSet<String>();
					int uniqueCounterDetailed = 0;
					while (relNames_it.hasNext()) {
						String thisRel = relNames_it.next();
						Set<String> current = switchTrack.get(thisRel);
						superSet.addAll(current);
						// uniqueCounterDetailed += current.size();
					}
					uniqueCounterDetailed = superSet.size();

					/*
					 * switchTrack.put("cnMys", empty); switchTrack.put("dnMys",
					 * empty); switchTrack.put("chMys", empty);
					 * switchTrack.put("csMys", empty); switchTrack.put("cnMyh",
					 * empty); switchTrack.put("dnMyh", empty);
					 * switchTrack.put("chMyh", empty); switchTrack.put("csMyh",
					 * empty);
					 */

					Set<String> switchKey = switchTrackThisTime.keySet();
					Iterator<String> switchkey_it = switchKey.iterator();

					while (switchkey_it.hasNext()) {
						String keyName = switchkey_it.next();
						Set<String> carList = switchTrackThisTime.get(keyName);
						Iterator<String> carList_it = carList.iterator();
						while (carList_it.hasNext()) {
							String nextCar = carList_it.next();
							switchTrack = updateSetAll(switchTrack, keyName,
									nextCar);
						}
					}

					int cnMysCount = switchTrack.get("cnMys").size();
					;
					int dnMysCount = switchTrack.get("dnMys").size();
					int chMysCount = switchTrack.get("chMys").size();
					int csMysCount = switchTrack.get("csMys").size();
					int cnMyhCount = switchTrack.get("cnMyh").size();
					int dnMyhCount = switchTrack.get("dnMyh").size();
					int chMyhCount = switchTrack.get("chMyh").size();
					int csMyhCount = switchTrack.get("csMyh").size();

					int cnMysCount2 = switchTrackThisTime.get("cnMys").size();
					;
					int dnMysCount2 = switchTrackThisTime.get("dnMys").size();
					int chMysCount2 = switchTrackThisTime.get("chMys").size();
					int csMysCount2 = switchTrackThisTime.get("csMys").size();
					int cnMyhCount2 = switchTrackThisTime.get("cnMyh").size();
					int dnMyhCount2 = switchTrackThisTime.get("dnMyh").size();
					int chMyhCount2 = switchTrackThisTime.get("chMyh").size();
					int csMyhCount2 = switchTrackThisTime.get("csMyh").size();

					Iterator<String> switchedIt = switchedThisTime.iterator();
					while (switchedIt.hasNext()) {
						String carThatDid = switchedIt.next();
						if (switchTracker.contains(carThatDid)) {
							switchTracker.remove(carThatDid);
						} else {
							switchTracker.add(carThatDid);
						}
					}
					int secondCount = switchTracker.size();

					int streetChangeCount = 0;
					int streetGetCong = 0;
					int streetLoseCong = 0;

					
					Iterator<String> congStIt = congStreets.iterator();
					while (congStIt.hasNext()) {
						String thisOne = congStIt.next();
						if (congStreetsNow.contains(thisOne)) {

						} else {
							streetLoseCong = streetLoseCong + 1;
							
						}
					}
					
					congStIt = congStreetsNow.iterator();
					while (congStIt.hasNext()) {
						String thisOne = congStIt.next();
						if (congStreets.contains(thisOne)) {
						} else {
							streetGetCong = streetGetCong + 1;
						}
					}
					streetChangeCount = streetGetCong + streetLoseCong;

					congStreets = new HashSet<String>();
					congStIt = congStreetsNow.iterator();
					while (congStIt.hasNext()) {
						String thisOne = congStIt.next();
						congStreets.add(thisOne);
					}
					
					line = line + "," + countUniqueCarsL + "," + uniqueCounterDetailed
							+ "," + secondCount + "," + cnMysCount + ","
							+ dnMysCount + "," + chMysCount + "," + csMysCount
							+ "," + cnMyhCount + "," + dnMyhCount + ","
							+ chMyhCount + "," + csMyhCount + "," + cnMysCount2
							+ "," + dnMysCount2 + "," + chMysCount2 + ","
							+ csMysCount2 + "," + cnMyhCount2 + ","
							+ dnMyhCount2 + "," + chMyhCount2 + ","
							+ csMyhCount2 + "," + streetChangeCount + ","
							+ streetGetCong + "," + streetLoseCong;

					
					printFunction(printcount, newLine);
					printcount += 1;
					
					inputStream.readLine(); // Count
					// Emerge Belief
					uniqueCars = new HashSet<String>();
					thisEmerged = new HashSet<String>();
					Map<String,Double> flockRecord = new HashMap<String,Double>();
					List<Double> values = new ArrayList<Double>();
					
					newLine = inputStream.readLine();
					while (!newLine.startsWith("**")) {
						String[] parts = newLine.split(",");
						double thisSize = Double.parseDouble(parts[2]);
						flockRecord.put(parts[0], thisSize);
						thisEmerged.add(parts[0]);
						newLine = inputStream.readLine();
					}
					
					int newEmerge = 0;
					int lostEmerge = 0;
					
					double newAvg = 0;
					double lostAvg = 0;
					
					Iterator<String> thisEmerged_it = thisEmerged.iterator();
					Iterator<String> lastEmerged_it = lastEmerged.iterator();
					
					while(thisEmerged_it.hasNext()){
						String emergeCar = thisEmerged_it.next();
						if(lastEmerged.contains(emergeCar) == false){
							newEmerge = newEmerge + 1;
							values.add(flockRecord.get(emergeCar));
							newAvg = newAvg + flockRecord.get(emergeCar);
						}
					}
					
					while(lastEmerged_it.hasNext()){
						String emergeCar = lastEmerged_it.next();
						if(thisEmerged.contains(emergeCar) == false){
							lostEmerge = lostEmerge + 1;
							//lostAvg = lostAvg + flockRecord.get(emergeCar);
						}
					}
					
					
					
					lastEmerged = new HashSet<String>();// add them
					thisEmerged_it = thisEmerged.iterator();
					while(thisEmerged_it.hasNext()){
						String emergeCar = thisEmerged_it.next();
						lastEmerged.add(emergeCar);
					}
					
					newAvg = newAvg / newEmerge;
					//lostAvg = lostAvg / lostEmerge;
					
					double median = 0;
					double min = 0;
					double max = 0;
					if(values.size() > 0){
					double[] target = new double[values.size()];
					 for (int i = 0; i < target.length; i++) {
					   
					    target[i] = values.get(i);                // java 1.5+ style (outboxing)
					 }
					
					 Arrays.sort(target);
					 
					 if (target.length % 2 == 0)
					     median = ((double)target[target.length/2] + (double)target[target.length/2 - 1])/2;
					 else
					     median = (double) target[target.length/2];
					 
					 min = target[0];
					 max = target[target.length - 1];
					}
					int countEmergeBelief = thisEmerged.size();
					line = line + "," + countEmergeBelief + "," + newEmerge + "," + lostEmerge
							+ "," + newAvg + "," + median + "," + min + "," + max;//+ "," + lostAvg;
					
					newLine = inputStream.readLine();

					// High Threshold PostCodes
					newLine = inputStream.readLine();
					while (!newLine
							.startsWith("**Postal-Areas Cars low Threshold**")) {
						newLine = inputStream.readLine();
					}

					printFunction(printcount, newLine);
					printcount += 1;
					newLine = inputStream.readLine(); //count

					line = line + "," + newLine;
					newLine = inputStream.readLine();
					// First do Cars
					String cvsSplitBy = ",";
					while (!newLine.startsWith("**")) {
						// System.out.println("14 " + newLine);
						String[] record = newLine.split(cvsSplitBy);
						String postName = record[0];
						String postCount = record[1];
						String carsList = record[2];
						String[] indivCars = carsList.split(" ");
						PostCode thisPostCode;
						if (!postCodeRecords.containsKey(postName)) {
							thisPostCode = new PostCode(postName);
						} else {
							thisPostCode = postCodeRecords.get(postName);
						}
						thisPostCode.addCarCountL(String.valueOf(counter),
								indivCars.length);
						postCodeRecords.put(postName, thisPostCode);
						newLine = inputStream.readLine(); // Count
					}
					printFunction(printcount, newLine);
					printcount += 1;
					
					// Should now be at : **Postal-Areas Manhole Lots**
					// Next do ManLots
					newLine = inputStream.readLine(); // Count
					line = line + "," + newLine;
					newLine = inputStream.readLine(); // Count

					while (!newLine.startsWith("**")) {
						String[] record = newLine.split(cvsSplitBy);
						String postName = record[0];
						String postCount = record[1];
						PostCode thisPostCode;
						if (!postCodeRecords.containsKey(postName)) {
							thisPostCode = new PostCode(postName);
						} else {
							thisPostCode = postCodeRecords.get(postName);
						}
						thisPostCode.addManCountLots(String.valueOf(counter),
								Integer.parseInt(postCount));
						postCodeRecords.put(postName, thisPostCode);
						newLine = inputStream.readLine(); // Count
					}
					printFunction(printcount, newLine);
					printcount += 1;
					
					//Should now be at **Postal-Areas Manhole Neighborhood**
					// Next do ManNeigh
					
					newLine = inputStream.readLine(); // Count
					line = line + "," + newLine;
					newLine = inputStream.readLine(); // Count
					while (newLine != null && !newLine.startsWith("**")) {
						String[] record = newLine.split(cvsSplitBy);
						//System.out.println(newLine);
						String postName = record[0];
						String postCount = record[1];
						
						PostCode thisPostCode;
						if (!postCodeRecords.containsKey(postName)) {
							thisPostCode = new PostCode(postName);
						} else {
							thisPostCode = postCodeRecords.get(postName);
						}
						thisPostCode.addManCountNeigh(String.valueOf(counter),
								Integer.parseInt(postCount));
						postCodeRecords.put(postName, thisPostCode);
						newLine = inputStream.readLine(); // Count
					}
					printFunction(printcount, newLine);
					printcount += 1;
					outputStream.println(line);

					counter = counter + 50;
					theFile = new File(folderName + "LogFiles/" + fileNameStart
							+ counter + fileEnd);

				}
				// inputStream.close();
				outputStream.close();

				int max = counter - 50;

				int xval = -380;

				while (xval < 400) {
					outputStream = new PrintWriter(new FileWriter(folderName
							+ "XCuts/" + "PostX" + xval + "-summary" + fileEnd));
					outputStream
							.println("TimeStep,CarsL,ManHoleLots,ManHoleNeigh");
					counter = 50;
					while (counter < max) {
						int yval = -380;
						int carCountL = 0;
						int manLotsCount = 0;
						int manNeighCount = 0;
						while (yval < 400) {
							String postName = xval + "-" + String.valueOf(yval);
							if (postCodeRecords.containsKey(postName)) {
								PostCode thisOne = postCodeRecords
										.get(postName);
								carCountL = carCountL
										+ thisOne.returnCarCountL(String
												.valueOf(counter));
								manLotsCount = manLotsCount
										+ thisOne.returnManLotsCount(String
												.valueOf(counter));
								manNeighCount = manNeighCount
										+ thisOne.returnManNeighCount(String
												.valueOf(counter));
							}
							yval = yval + 40;
						}
						String newLine = counter + "," +  carCountL + "," 
								+ manLotsCount + "," + manNeighCount;
						outputStream.println(newLine);
						counter = counter + 50;
					}
					outputStream.close();
					xval = xval + 40;
				}

				int yval = -380;

				while (yval < 400) {
					outputStream = new PrintWriter(new FileWriter(folderName
							+ "YCuts/" + "PostY" + yval + "-summary" + fileEnd));
					outputStream
							.println("TimeStep,CarsL,ManHoleLots,ManHoleNeigh");

					counter = 50;
					while (counter < max) {
						xval = -380;
						int carCountL = 0;
						int manLotsCount = 0;
						int manNeighCount = 0;
						while (xval < 400) {
							String postName = xval + "-" + String.valueOf(yval);
							if (postCodeRecords.containsKey(postName)) {
								PostCode thisOne = postCodeRecords
										.get(postName);
								
								
								
								carCountL = carCountL
										+ thisOne.returnCarCountL(String
												.valueOf(counter));
								
								manLotsCount = manLotsCount
										+ thisOne.returnManLotsCount(String
												.valueOf(counter));
								manNeighCount = manNeighCount
										+ thisOne.returnManNeighCount(String
												.valueOf(counter));
							}
							xval = xval + 40;
						}
						String newLine = counter + "," + carCountL + "," 
								+ manLotsCount + "," + manNeighCount;
						outputStream.println(newLine);
						counter = counter + 50;
					}
					outputStream.close();
					yval = yval + 40;
				}

			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} finally {
				try {
					if (inputStream != null) {
						inputStream.close();
					}
					if (outputStream != null) {
						outputStream.close();
					}
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}
	
	private static void printRunSumm (RunSummary r, String RunName, int RunNum) {
		Rengine re = Rengine.getMainEngine();
		if (re == null)
			re = new Rengine(new String[] { "--vanilla" }, false, null);
		
		
		
		re.eval("library(DetectRPackage)");
		
		PrintWriter outputStream = null;
		try {
			/*outputStream = new PrintWriter(new FileWriter(
					"/home/eamonn/netlogo/EmergenceTests/SecondRuns/Consensus2/Traffic/Run00" + RunNum + ".csv"));
			outputStream.println("TimeStep,Object,Change,Emerge,NeighSize");*/
			
			HashMap<Integer,Set<String>> objM = r.getObjectMeasure();
			HashMap<Integer,Set<String>> carC = r.getCarChange();
			HashMap<Integer,Set<String>> emBe = r.getEmergeBelief();
			HashMap<Integer,Double> neSz = r.getNeighSize();
			HashMap <Integer,HashMap<Integer,Set<String>>> allSpecs = r.getSpecificRels();
			
			double[] yArray = new double[1300];
			int totalChanges = 0;
			int counter2 = 50;
			while(counter2 <= 65000){
				//System.out.println(counter2);
				yArray[(counter2 / 50) - 1] = objM.get(counter2).size();
				totalChanges += objM.get(counter2).size();
				counter2= counter2 + 50;
			}
			
			re.assign("y", yArray);
			re.eval("cp<-changeP(y)");
			re.eval("cpU<-matrix(cp$Ups,ncol=2)");
			re.eval("cpD<-matrix(cp$Downs,ncol=2)");
			org.rosuda.JRI.REXP result = re.eval("size(cpU[,1])[2]");
			double upRegions = result.asDouble();
			result = re.eval("size(cpD[,1])[2]");
			double downRegions = result.asDouble();
			
			double[] upStarts = new double[(int) upRegions];
			double[] upEnds = new double[(int) upRegions];
			double[] downStarts = new double[(int) downRegions];
			double[] downEnds = new double[(int) downRegions];
			
			int i = 0;
			
			while(i < upStarts.length){
				result = re.eval("cpU["+(i+1)+",1]");
				upStarts[i] =  result.asDouble();
				result = re.eval("cpU["+(i+1)+",2]");
				upEnds[i] =  result.asDouble();
				i = i + 1;
			}
			
			i = 0;
			
			while(i < downStarts.length){
				String command = "cpD["+(i+1)+",1]";
				//System.out.println(command);
				result = re.eval(command);
				downStarts[i] =  result.asDouble();
				command = "cpD["+(i+1)+",2]";
				result = re.eval(command);
				downEnds[i] =  result.asDouble();
				i = i + 1;
			}
			
			re.end();
			
			//check for overlap
			for(int k = 0; k < upStarts.length; k++){
				for(int l = 0; l < downStarts.length; l++){
					if(upStarts[k] < downStarts[l] && upEnds[k] >= downStarts[l]){
						downStarts[l] = upEnds[k] + 1;
						upEnds[k] = upEnds[k] - 4;
					}
				}
			}
			
			for(int k = 0; k < downStarts.length; k++){
				for(int l = 0; l < upStarts.length; l++){
					if(downStarts[k] < upStarts[l] && downEnds[k] >= upStarts[l]){
						upStarts[l] = downEnds[k] + 1;
						downEnds[k] = downEnds[k] - 4;
					}
				}
			}
			/*printArray(upStarts, upStarts.length);
			printArray(upEnds, upEnds.length);
			printArray(downStarts, downStarts.length);
			printArray(downEnds, downEnds.length);*/
			
			HashMap<Double, Double> periodDefns = new HashMap<Double, Double>();
			double[] allStarts = new double [upStarts.length + downStarts.length];
			for(int c = 0; c < upStarts.length; c++){
				allStarts[c] = upStarts[c];
				periodDefns.put(upStarts[c], upEnds[c]);
			}
			
			for(int c = 0; c < downStarts.length; c++){
				allStarts[c + upStarts.length] = downStarts[c];
				periodDefns.put(downStarts[c], downEnds[c]);
			}
			
			double[] orderedPeriodStarts = CrunchifyBubbleSortAsceMethod(allStarts);
			//Now go through and figure the results.
			int countChangeTime = 0;
			int countNoChangeTime = 0;
			
			int countChangeEvents = 0;
			int countNoChangeEvents = 0;
			
			HashMap<Integer,Integer> countSpecificChangeEvents = new HashMap<Integer,Integer>();
			HashMap<Integer,Integer> countSpecificNoChangeEvents = new HashMap<Integer,Integer>();
			
			int countEmergeEvents = 0;
			int countNoEmergeEvents = 0;
			
			int changeProcessed = 0;
			int simCount = 1;
			double nextChangeStart = 5;
			boolean inChangePeriod = false;
			
			while(nextChangeStart == 5){
				nextChangeStart = orderedPeriodStarts[changeProcessed];
				changeProcessed = changeProcessed + 1;
			}
			//System.out.println(nextChangeStart);
			double nextChangeEnd = periodDefns.get(nextChangeStart);
			//System.out.println(nextChangeEnd);
			//System.out.println(nextChangeStart);
			//nextChangeStart = nextChangeStart - 5;
			//nextChangeEnd = nextChangeEnd - 5;
			
			int countChangeTimeUp = 0;
			int countChangeTimeDown = 0;
			int countChangeTUEvents = 0;
			int countChangeTDEvents = 0;
			int countEmergeTUEvents = 0;
			int countEmergeTDEvents = 0;
			
			HashMap<Integer,Integer> countSpecificChangeTUEvents = new HashMap<Integer,Integer>();
			HashMap<Integer,Integer> countSpecificChangeTDEvents = new HashMap<Integer,Integer>();
			
			for(int f = 1; f <= 15; f ++){
				countSpecificNoChangeEvents.put(f,0);
				countSpecificChangeTUEvents.put(f,0);
				countSpecificChangeTDEvents.put(f,0);
			}
			
			boolean changeUp = true;
			
			
			while(simCount <= 1300){
				int getThis = simCount * 50;
				if(simCount >= nextChangeStart && simCount < nextChangeEnd){
					//System.out.println("Simcount: " + simCount + " Change Period");
					int carCount = carC.get(getThis).size();
					countChangeEvents = countChangeEvents + carCount;
					countEmergeEvents += emBe.get(getThis).size();
					countChangeTime += 1;
					if(simCount == nextChangeStart){
						int c = 0;
						changeUp = false;
						while(c < upStarts.length){
							if (upStarts[c] == simCount){
								changeUp = true;
							}
							c = c+ 1;
						}
					}
					if(changeUp){
						countChangeTimeUp += 1;
						countChangeTUEvents += carC.get(getThis).size();
						countEmergeTUEvents += emBe.get(getThis).size();
						
						countSpecificChangeTUEvents = updateSpecificCounts(countSpecificChangeTUEvents, allSpecs.get(getThis));  
						
					}else{
						countChangeTimeDown += 1;
						countChangeTDEvents += carC.get(getThis).size();
						countEmergeTDEvents += emBe.get(getThis).size();
						countSpecificChangeTDEvents = updateSpecificCounts(countSpecificChangeTDEvents, allSpecs.get(getThis));
					}
				}else{
					//System.out.println("Simcount: " + simCount + " Non-Change Period");
					if(simCount == (int) nextChangeEnd && changeProcessed < (orderedPeriodStarts.length )  ){
						nextChangeStart = orderedPeriodStarts[changeProcessed];
						changeProcessed = changeProcessed + 1;
						nextChangeEnd = periodDefns.get(nextChangeStart);
						//nextChangeStart = nextChangeStart - 5;
						//nextChangeEnd = nextChangeEnd - 5;
					}
					
					countNoChangeEvents += carC.get(getThis).size();
					countNoEmergeEvents += emBe.get(getThis).size();
					countSpecificNoChangeEvents = updateSpecificCounts(countSpecificNoChangeEvents, allSpecs.get(getThis));
					
					countNoChangeTime += 1;
				}
				//System.out.println(simCount + "," + countChangeEvents);
				simCount = simCount + 1;
			}
			//printArray(upStarts, upStarts.length);
			//printArray(upEnds, upEnds.length);
			//printArray(downStarts, downStarts.length);
			//printArray(downEnds, downEnds.length);
			String dataToPrint = RunName + "," + countChangeTime + "," + countChangeEvents + "," + countEmergeEvents 
					+ "," + countNoChangeTime + "," + countNoChangeEvents + "," + countNoEmergeEvents
					+ "," + countChangeTimeUp + "," + countChangeTUEvents + "," + countEmergeTUEvents
					+ "," + countChangeTimeDown + "," + countChangeTDEvents + "," + countEmergeTDEvents;
			
			for(int relCount = 1; relCount <= 15; relCount ++){
				dataToPrint = dataToPrint + "," + countSpecificNoChangeEvents.get(relCount);
			}
			for(int relCount = 1; relCount <= 15; relCount ++){
				dataToPrint = dataToPrint + "," + countSpecificChangeTUEvents.get(relCount);
			}
			for(int relCount = 1; relCount <= 15; relCount ++){
				dataToPrint = dataToPrint + "," + countSpecificChangeTDEvents.get(relCount);
			}
			
			System.out.println(dataToPrint);
			
			double[] orderedUpStarts = CrunchifyBubbleSortAsceMethod(upStarts);
			double[] orderedDownStarts = CrunchifyBubbleSortAsceMethod(downStarts);
			
			int countUps = orderedUpStarts.length;
			int countDowns = orderedDownStarts.length;
			
			int c3 = 0;
			while(orderedUpStarts[c3] == 5.0){
				c3 = c3 + 1;
			}
			
			//printArray(orderedUpStarts, orderedUpStarts.length);
			//printArray(orderedDownStarts, orderedDownStarts.length);
			countUps = countUps - c3;
			
			c3 = 0;
			while(orderedDownStarts[c3] == 5.0){
				c3 = c3 + 1;
			}
			countDowns = countDowns - c3;
			
			//System.out.println(RunName + "," + countUps + "," + countDowns);
			
			
			//Set<Integer> timeCount = objM.keySet();
			//Iterator<Integer> timeCount_It = timeCount.iterator();
			int counter = 50;
			/*while(counter <= 65000){
				//int counter = timeCount_It.next();
				outputStream.println(counter + "," + objM.get(counter).size() + "," + carC.get(counter).size() +
						"," + emBe.get(counter).size() + "," + neSz.get(counter));
				counter = counter + 50;
			}*/
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}finally {
			try {
				if (outputStream != null) {
					outputStream.close();
				}
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		
		
	}
	
	private static HashMap<Integer,Integer> updateSpecificCounts
						(HashMap<Integer,Integer> returnMap,HashMap<Integer,Set<String>> history){
		Set<Integer> relNames = history.keySet();
		Iterator<Integer> relNames_it = relNames.iterator();
		while(relNames_it.hasNext()){
			int relName = relNames_it.next();
			if(returnMap.containsKey(relName)){
				int relCount = returnMap.get(relName);
				relCount = relCount + history.get(relName).size();
				returnMap.put(relName, relCount);
			}else{
				int relCount = history.get(relName).size();
				returnMap.put(relName, relCount);
			}
		}
		return returnMap;
	}
	
	private static void printArray(double[] input, int size) {
		for (int r = 0; r < size; r++) {
			System.out.print(input[r] + " ");
		}
		System.out.print("\n");
	}
	
	public static double[] CrunchifyBubbleSortAsceMethod(double[] arr){
        double temp;
        for(int i=0; i < arr.length-1; i++){
 
            for(int j=1; j < arr.length-i; j++){
                if(arr[j-1] > arr[j]){
                    temp=arr[j-1];
                    arr[j-1] = arr[j];
                    arr[j] = temp;
                }
            }
            //System.out.println((i+1)+"th iteration result: "+Arrays.toString(arr));
        }
        return arr;
    }

	private static void printFunction(int count, String word) {
		 //System.out.println(count + " " + word);
	}

	private static HashMap<String, Set<String>> updateSet(
			HashMap<String, Set<String>> in, String nameKey, String nameCar) {
		Set<String> current = in.get(nameKey);
		in.remove(nameKey);
		current.add(nameCar);
		in.put(nameKey, current);
		return in;
	}

	private static HashMap<String, Set<String>> updateSetAll(
			HashMap<String, Set<String>> in, String nameKey, String nameCar) {
		Set<String> current = in.get(nameKey);
		if (current.contains(nameCar)) {
			current.remove(nameCar);
		} else {
			current.add(nameCar);
		}
		in.put(nameKey, current);
		return in;
	}

	public static class PostCode {
		String name;
		Map<String, Integer> carCountH;
		Map<String, Integer> carCountMH;
		Map<String, Integer> carCountML;
		Map<String, Integer> carCountL;
		Map<String, Integer> manCountSome;
		Map<String, Integer> manCountLots;
		Map<String, Integer> manCountNeighs;

		public PostCode(String name) {
			this.name = name;
			carCountH = new HashMap<String, Integer>();
			carCountMH = new HashMap<String, Integer>();
			carCountML = new HashMap<String, Integer>();
			carCountL = new HashMap<String, Integer>();
			manCountSome = new HashMap<String, Integer>();
			manCountLots = new HashMap<String, Integer>();
			manCountNeighs = new HashMap<String, Integer>();
		}

		public void addCarCountH(String count, int value) {
			carCountH.put(count, value);
		}

		public void addCarCountMH(String count, int value) {
			carCountMH.put(count, value);
		}

		public void addCarCountML(String count, int value) {
			carCountML.put(count, value);
		}

		public void addCarCountL(String count, int value) {
			carCountL.put(count, value);
		}

		public void addManCountSome(String count, int value) {
			manCountSome.put(count, value);
		}

		public void addManCountLots(String count, int value) {
			manCountLots.put(count, value);
		}

		public void addManCountNeigh(String count, int value) {
			manCountNeighs.put(count, value);
		}

		public int returnCarCountH(String count) {
			if (!carCountH.containsKey(count))
				return 0;
			else {
				return carCountH.get(count);
			}
		}

		public int returnCarCountMH(String count) {
			if (!carCountMH.containsKey(count))
				return 0;
			else {
				return carCountMH.get(count);
			}
		}

		public int returnCarCountML(String count) {
			if (!carCountML.containsKey(count))
				return 0;
			else {
				return carCountML.get(count);
			}
		}

		public int returnCarCountL(String count) {
			if (!carCountL.containsKey(count))
				return 0;
			else {
				return carCountL.get(count);
			}
		}

		public int returnManSomeCount(String count) {
			if (!manCountSome.containsKey(count))
				return 0;
			else {
				return manCountSome.get(count);
			}
		}

		public int returnManLotsCount(String count) {
			if (!manCountLots.containsKey(count))
				return 0;
			else {
				return manCountLots.get(count);
			}
		}

		public int returnManNeighCount(String count) {
			if (!manCountNeighs.containsKey(count))
				return 0;
			else {
				return manCountNeighs.get(count);
			}
		}
	}

}

/*
 
  f21 <- rep(1/21,21)
  c_lag <- filter(c,f21,sides=2)
  c_lag<- clag(11:1390)
  library(changepoint)
  mvalue = cpt.mean(c_lag, method="BinSeg")
  length(cpts(mvalue))
 
 */
