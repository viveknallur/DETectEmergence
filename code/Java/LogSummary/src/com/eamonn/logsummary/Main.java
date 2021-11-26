package com.eamonn.logsummary;

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

import scala.actors.threadpool.Arrays;

public class Main {
	public static void main(String[] args) {
		int firstFolder = 120;
		while (firstFolder < 121) {
			// listFilesForFolder(folder);
			System.out.println("VM " + firstFolder);
			int runCount = 98;
			//while (runCount < 100) 
				System.out.println("Run " + runCount);
				final File folder = new File("//home/eamonn/netlogo/EmergenceTests/TrafficNorm/Run062");
//						+ firstFolder + "/Run0" + runCount);
				listFilesInOrder(folder);
				runCount = runCount + 1;
			//}
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

	public static void listFilesInOrder(final File folder) {
		String folderName = folder.getAbsolutePath() + "/";
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
			/*
			 * switchTrack.put("cnMys", empty); switchTrack.put("dnMys", empty);
			 * switchTrack.put("chMys", empty); switchTrack.put("csMys", empty);
			 * switchTrack.put("cnMyh", empty); switchTrack.put("dnMyh", empty);
			 * switchTrack.put("chMyh", empty); switchTrack.put("csMyh", empty);
			 */

			try {
				outputStream = new PrintWriter(new FileWriter(folderName
						+ fileNameStart + "summary" + fileEnd));
				String line = "MatesF,global_coeff-cn-myS_cusum,global_coeff-dn-myS_cusum,global_coeff-ch-myS_cusum,"
						+ "global_coeff-cs-myS_cusum,global_coeff-cn-myH_cusum,global_coeff-dn-myH_cusum,"
						+ "global_coeff-ch-myH_cusum,global_coeff-cs-myH_cusum,global_coeff-Rsq-myS_cusum,"
						+ "global_coeff-Rsq-myH_cusum,man_some_congested,man_big_congested,man_neigh_congested,Unique_Cars_High," // Unique_Cars_Switch,"
						+ "Unique_Cars_Mid_High,Unique_Cars_Mid_Low,Unique_Cars_Low,Unique_Change_Count,Unique_Change_Count2,"
						+ "cnMys,dnMys,chMys,csMys,cnMyh,dnMyh,chMyh,csMyh,"
						+ "cnMys2,dnMys2,chMys2,csMys2,cnMyh2,dnMyh2,chMyh2,csMyh2,"
						+ "streetsChanging,streetsGettingCong,streetsLosingCong,"
						+ "EmergeBelief,NewEmerge,LostEmerge,NewEmAvg,Median,Min,Max,"
						+ "post_withcarsH,post_withcarsMH,post_withcarsML,post_withcarsL,post_withManSome,"
						+ "post_withManLots,post_WithManNeigh";
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

					inputStream = new BufferedReader(new FileReader(folderName
							+ "LogFiles/" + theFile.getName()));

					line = inputStream.readLine(); // **Global Statistics**
					line = inputStream.readLine(); // **Headers
					line = inputStream.readLine(); // **Global Stats
					// Line now has global car stats
					inputStream.readLine(); // **Manhole Indiviuals
					inputStream.readLine(); // **Some Congestion**
					String newLine = inputStream.readLine(); // Count

					line = line + "," + newLine;

					// Cycle through the list of manholes with "Some" congestion
					newLine = inputStream.readLine();
					while (!newLine.startsWith("**")) {
						newLine = inputStream.readLine();
					}
					printFunction(printcount, newLine);
					printcount += 1;
					// Have found **Big Congeston**
					newLine = inputStream.readLine(); // Count
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

					// 1. Large Threshold for Cars
					// Have found **Cars
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

					int countUniqueCarsLarge = uniqueCars.size();

					printFunction(printcount, newLine);
					printcount += 1;

					// 2. Mid-High Threshold for Cars
					// Have found **Cars
					// inputStream.readLine(); // **coeff-cn-mySCusum
					printFunction(printcount, inputStream.readLine());
					printcount += 1;
					inputStream.readLine(); // Count

					// This next loop, loops through coeff-cn-myS_cusum
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

					// System.out.println ("Ignoring");
					printFunction(printcount, newLine);
					printcount += 1;

					inputStream.readLine();
					// This next loop, loops through coeff-RsQ-myS_cusum
					newLine = inputStream.readLine();
					while (!newLine.startsWith("**")) {
						newLine = inputStream.readLine();
					}
					// System.out.println ("Ignoring");
					printFunction(printcount, newLine);
					printcount += 1;

					inputStream.readLine();
					// This next loop, loops through coeff-RsQ-myH_cusum
					newLine = inputStream.readLine();
					while (!newLine.startsWith("**")) {
						newLine = inputStream.readLine();
					}

					int countUniqueCarsMh = uniqueCars.size();

					// 3. MidLow Threshold for Cars
					printFunction(printcount, newLine);
					printcount += 1;

					// Have found **Cars
					// inputStream.readLine(); // **coeff-cn-mySCusum
					printFunction(printcount, inputStream.readLine());
					printcount += 1;
					inputStream.readLine(); // Count

					// This next loop, loops through coeff-cn-myS_cusum
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

					// System.out.println ("Ignoring");
					printFunction(printcount, newLine);
					printcount += 1;

					inputStream.readLine();
					// This next loop, loops through coeff-RsQ-myS_cusum
					newLine = inputStream.readLine();
					while (!newLine.startsWith("**")) {
						newLine = inputStream.readLine();
					}
					// System.out.println ("Ignoring");
					printFunction(printcount, newLine);
					printcount += 1;

					inputStream.readLine();
					// This next loop, loops through coeff-RsQ-myH_cusum
					newLine = inputStream.readLine();
					while (!newLine.startsWith("**")) {
						newLine = inputStream.readLine();
					}

					int countUniqueCarsML = uniqueCars.size();

					// 4. Low Threshold for Cars
					// Have found **Cars

					// System.out.println ("Ignoring");
					printFunction(printcount, newLine);
					printcount += 1;

					printFunction(printcount, inputStream.readLine());
					printcount += 1;
					// inputStream.readLine(); // **coeff-cn-mySCusum
					inputStream.readLine(); // Count

					// This next loop, loops through coeff-cn-myS_cusum
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
					
					line = line + "," + countUniqueCarsLarge + ","
							+ countUniqueCarsMh + "," + countUniqueCarsML + ","
							+ countUniqueCarsL + "," + uniqueCounterDetailed
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
							.startsWith("**Postal-Areas Cars High Threshold**")) {

						newLine = inputStream.readLine();
					}

					printFunction(printcount, newLine);
					printcount += 1;
					newLine = inputStream.readLine();

					line = line + "," + newLine;

					newLine = inputStream.readLine();

					String cvsSplitBy = ",";
					while (!newLine.startsWith("**")) {
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
						thisPostCode.addCarCountH(String.valueOf(counter),
								indivCars.length);
						postCodeRecords.put(postName, thisPostCode);
						newLine = inputStream.readLine(); // Count
					}
					printFunction(printcount, newLine);
					printcount += 1;
					// Mid-High Threshold PostCodes
					newLine = inputStream.readLine(); // Count

					line = line + "," + newLine;
					newLine = inputStream.readLine();

					// First do Cars
					cvsSplitBy = ",";
					while (!newLine.startsWith("**")) {
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
						thisPostCode.addCarCountMH(String.valueOf(counter),
								indivCars.length);
						postCodeRecords.put(postName, thisPostCode);
						newLine = inputStream.readLine(); // Count
					}
					printFunction(printcount, newLine);
					printcount += 1;

					// Mid-Low Threshold PostCodes
					newLine = inputStream.readLine(); // Count
					line = line + "," + newLine;
					newLine = inputStream.readLine();

					// First do Cars
					cvsSplitBy = ",";
					while (!newLine.startsWith("**")) {
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
						thisPostCode.addCarCountML(String.valueOf(counter),
								indivCars.length);
						postCodeRecords.put(postName, thisPostCode);
						newLine = inputStream.readLine(); // Count
					}
					printFunction(printcount, newLine);
					printcount += 1;
					// Low Threshold PostCodes
					newLine = inputStream.readLine(); // Count
					line = line + "," + newLine;
					newLine = inputStream.readLine();
					// First do Cars
					cvsSplitBy = ",";
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
					// Next do ManSome

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
						thisPostCode.addManCountSome(String.valueOf(counter),
								Integer.parseInt(postCount));
						postCodeRecords.put(postName, thisPostCode);
						newLine = inputStream.readLine(); // Count
					}
					printFunction(printcount, newLine);
					printcount += 1;
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
					// Next do ManNeigh
					newLine = inputStream.readLine(); // Count
					line = line + "," + newLine;
					newLine = inputStream.readLine(); // Count
					while (newLine != null) {
						String[] record = newLine.split(cvsSplitBy);
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
							.println("TimeStep,CarsH,CarsMH,CarsML,CarsL,ManHoleSome,ManHoleLots,ManHoleNeigh");
					counter = 50;
					while (counter < max) {
						int yval = -380;
						int carCountH = 0;
						int carCountMH = 0;
						int carCountML = 0;
						int carCountL = 0;
						int manSomeCount = 0;
						int manLotsCount = 0;
						int manNeighCount = 0;
						while (yval < 400) {
							String postName = xval + "-" + String.valueOf(yval);
							if (postCodeRecords.containsKey(postName)) {
								PostCode thisOne = postCodeRecords
										.get(postName);
								carCountH = carCountH
										+ thisOne.returnCarCountH(String
												.valueOf(counter));
								carCountMH = carCountMH
										+ thisOne.returnCarCountMH(String
												.valueOf(counter));
								carCountML = carCountML
										+ thisOne.returnCarCountML(String
												.valueOf(counter));
								carCountL = carCountL
										+ thisOne.returnCarCountL(String
												.valueOf(counter));
								manSomeCount = manSomeCount
										+ thisOne.returnManSomeCount(String
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
						String newLine = counter + "," + carCountH + ","
								+ carCountMH + "," + carCountML + ","
								+ carCountL + "," + manSomeCount + ","
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
							.println("TimeStep,CarsH,CarsMH,CarsML,CarsL,ManHoleSome,ManHoleLots,ManHoleNeigh");

					counter = 50;
					while (counter < max) {
						xval = -380;
						int carCountH = 0;
						int carCountMH = 0;
						int carCountML = 0;
						int carCountL = 0;
						int manSomeCount = 0;
						int manLotsCount = 0;
						int manNeighCount = 0;
						while (xval < 400) {
							String postName = xval + "-" + String.valueOf(yval);
							if (postCodeRecords.containsKey(postName)) {
								PostCode thisOne = postCodeRecords
										.get(postName);
								carCountH = carCountH
										+ thisOne.returnCarCountH(String
												.valueOf(counter));
								carCountMH = carCountMH
										+ thisOne.returnCarCountMH(String
												.valueOf(counter));
								carCountML = carCountML
										+ thisOne.returnCarCountML(String
												.valueOf(counter));
								carCountL = carCountL
										+ thisOne.returnCarCountL(String
												.valueOf(counter));
								manSomeCount = manSomeCount
										+ thisOne.returnManSomeCount(String
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
						String newLine = counter + "," + carCountH + ","
								+ carCountMH + "," + carCountML + ","
								+ carCountL + "," + manSomeCount + ","
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

	private static void printFunction(int count, String word) {
		// System.out.println(count + " " + word);
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
