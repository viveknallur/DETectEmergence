package com.eamonn.logsummary;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.eamonn.logsummary.Main.PostCode;
import org.rosuda.JRI.REXP;
import org.rosuda.JRI.Rengine;

public class SummaryProcess {
	public static void main(String[] args) {
		File folder = new File("//home/eamonn/netlogo/Results/Stage1SummFiles/");
		File[] listOfFiles = folder.listFiles();
		List<String> trafficLevels = new ArrayList<String>();
		if (listOfFiles.length > 0) {
			for (int i = 0; i < listOfFiles.length; i++) {
				trafficLevels.add(listOfFiles[i].getAbsolutePath());
			}
		}

		Iterator<String> trafficLevels_it = trafficLevels.iterator();
		while (trafficLevels_it.hasNext()) {
			String t_level = trafficLevels_it.next() + "/";
			File t_levelFolder = new File(t_level);
			listOfFiles = t_levelFolder.listFiles();
			List<String> runFolders = new ArrayList<String>();
			if (listOfFiles.length > 0) {
				for (int i = 0; i < listOfFiles.length; i++) {
					runFolders.add(listOfFiles[i].getAbsolutePath());
				}
			}

			Iterator<String> runFolders_it = runFolders.iterator();
			while (runFolders_it.hasNext()) {
				String run_Name = runFolders_it.next() + "/";
				processFolder(run_Name);
			}
		}

	}

	public static void processFolder(String folderName)  {
		BufferedReader inputStream = null;
		System.out.println(folderName);
		String run_Name = folderName + "/";
		File run_Name_Folder = new File(run_Name);
		File[] listOfFiles = run_Name_Folder.listFiles();
		Rengine re = Rengine.getMainEngine();
		if (re == null)
			re = new Rengine(new String[] { "--vanilla" }, false, null);
		if (listOfFiles.length > 0) {
			for (int i = 0; i < listOfFiles.length; i++) {
				try {
					inputStream = new BufferedReader(new FileReader(listOfFiles[i].getAbsolutePath()));
					String line = inputStream.readLine(); // Header
					line = inputStream.readLine();
					double[] traffic_ts = new double[600];
					int count = 0;
					while(line != null) {
						String[] splitline = line.split(",");
						double streetCount = Double.parseDouble(splitline[12]);
						traffic_ts[count] = streetCount;
						
						count = count + 1;
						line = inputStream.readLine();
					}
					int peakCount = 0;
					// Next is the magic with R
					
					REXP result;
					
					re.assign("traffic", traffic_ts);
					re.eval("traffic.df <- data.frame(traffic)");
					re.eval("traffic.ts <- ts(traffic.df)");
					re.eval("traffic.f10 <- filter(traffic.ts, rep(1/10,10), sides=2)");
					re.eval("traffic.df <- data.frame(traffic.f10)[5:595,]");
					re.eval("traffic.ts <- ts(traffic.df)");
					result = re.eval("library(cardidates)");
					//System.out.println(result.asString());
					re.eval("peak <- peakwindow(traffic.ts, mincut=0.9, minpeak=(120/max(traffic.ts)))");
					
					result = re.eval("peak$peaks[,1]");
					//printArray(result.asDoubleArray(),result.asDoubleArray().length);
					
					boolean checker = true;
					int c = 1;
					
					//result = re.eval("peak$peaks[,1]");
					peakCount = result.asDoubleArray().length;
					System.out.println(listOfFiles[i].getAbsolutePath() + "," + peakCount);
					
					
					
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} // **Global Statistics**
				
			}
		}
		re.end();
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

		}
	}
	
	private static void printArray(double[] input, int size) {
		for (int r = 0; r < size; r++) {
			System.out.print(input[r] + " ");
		}
		System.out.print("\n");
	}
}
