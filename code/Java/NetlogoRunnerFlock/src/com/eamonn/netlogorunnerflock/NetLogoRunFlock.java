package com.eamonn.netlogorunnerflock;

import java.io.BufferedReader;
import java.io.FileReader;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.nlogo.headless.HeadlessWorkspace;

public class NetLogoRunFlock {

	public static void main(String[] argv) {
		String model = argv[0];
		String runInput = argv[1];
		String machineName = argv[2];

		BufferedReader inputStream = null;
		try {
			HeadlessWorkspace workspace = HeadlessWorkspace.newInstance();
			workspace.open(model);
			inputStream = new BufferedReader(new FileReader(runInput));

			String firstLine = inputStream.readLine(); // **Global Statistics**
			String newLine = inputStream.readLine();

			while (newLine != null) {
				String[] runDetails = newLine.split(",");

				String runNumber = runDetails[0];
				String repeat = runDetails[1];
				String carNum = runDetails[2];
				String cusMin = runDetails[3];
				String cusMax = runDetails[4];
				String PThreshold = runDetails[5];
				String neighMin = runDetails[6];
				String neighMax = runDetails[7];
				String changememLen = runDetails[8];
				String avgThreshold = runDetails[9];
				String regWin = runDetails[10];
				String smothL = runDetails[11];

				System.setProperty("PThreshold", PThreshold);
				System.setProperty("CusumMin", cusMin);
				System.setProperty("CusumWindow", cusMax);
				

				newLine = inputStream.readLine();

				DateFormat dateFormat = new SimpleDateFormat(
						"yyyy/MM/dd HH:mm:ss");
				Date date = new Date();
				
				PThreshold = System.getProperty("PThreshold");
				if (PThreshold.equals("1.0")) {
					PThreshold = "10";
				} else if (PThreshold.equals("0.5")) {
					PThreshold = "05";
				} else {
					PThreshold = "02";
				}
				
				int runCount = 1;
				
				while (runCount <= 1) {
					System.out.println("And so we begin!!");
					
					workspace.command("setup");
					
					String commandText3 = "set car-number " + carNum;
					workspace.command(commandText3);
					String commandText4 = "set minNeighSize " + neighMin;
					workspace.command(commandText4);
					String commandText5 = "set  maxNeighSize " + neighMax;
					workspace.command(commandText5);
					String commandText6 = "set changeMemLen " + changememLen;
					workspace.command(commandText6);
					String commandText7 = "set aggThreshold " + avgThreshold;
					workspace.command(commandText7);
					String commandText8 = "set regWinL " + regWin;
					workspace.command(commandText8);
					String commandText9 = "set  smoothL " + smothL;
					workspace.command(commandText9);
					
					// Construct the run name
					String runName = "Run0" + runNumber + "CN" + carNum + "CW"
							+ System.getProperty("CusumMin") + "VS" + PThreshold
							+ "NL" + neighMin
							+ "NH" + neighMax
							+ "ML" + changememLen
							+ "AT" + avgThreshold.substring(0, avgThreshold.indexOf(".")) + 
							avgThreshold.substring(avgThreshold.indexOf(".") + 1 )
							+ "RW" + regWin
							+ "MA" + machineName;
					
					System.setProperty("RunName", runName);
					
					System.out.println(dateFormat.format(date) + " : Starting : "
							+ runName);
					
					String commandText = "set run-name \"" + runName + "\"";
					workspace.command(commandText);
					workspace.command("setup-part2");
					workspace.command("repeat " + repeat + " [ go ]");
					workspace.command("file-close");
					System.out.println(dateFormat.format(date) + " : Ending : "
							+ runName);
					
					runCount += 1;
					
				}

			}
			workspace.dispose();
		} catch (Exception ex) {
			ex.printStackTrace();
		}

	}
}
