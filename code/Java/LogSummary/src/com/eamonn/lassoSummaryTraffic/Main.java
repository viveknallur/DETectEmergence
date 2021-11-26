package com.eamonn.lassoSummaryTraffic;

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
		final File folder = new File(
				"//home/eamonn/netlogo/EmergenceTests/VariableSelection/Traffic");
		processFile(folder);
		System.out.println("Done");
	}

	public static void processFile(final File folder) {

		File[] listOfFiles = folder.listFiles();
		String folderName = folder.getAbsolutePath() + "/";
		System.out.println(listOfFiles.length);
		BufferedReader inputStream = null;

		double globalAgentCount = 0;
		double globalRelCountSelected = 0;

		int globalAgeFH = 0;
		int globalAgeFS = 0;
		int globalAgeDN = 0;
		int globalAgeBN = 0;
		int globalAgeTM = 0;

		int globalMySpeedFH = 0;
		int globalMySpeedFS = 0;
		int globalMySpeedDN = 0;
		int globalMySpeedBN = 0;
		int globalMySpeedTM = 0;

		int globalMyHeadFH = 0;
		int globalMyHeadFS = 0;
		int globalMyHeadDN = 0;
		int globalMyHeadBN = 0;
		int globalMyHeadTM = 0;

		// Now Count
		// Internal
		int ageRels = 0;
		int speedRels = 0;
		int myHeadRels = 0;

		int reallyBad = 0;
		int reallyBadExt = 0;

		int goodInt = 0;
		int goodExt = 0;
		int great = 0;
		// External
		int flockHeadRels = 0;
		int flockSpeedRels = 0;
		int tempRels = 0;
		int birdsNearRels = 0;
		int distNearRels = 0;
		// None
		int noRels = 0;

		for (File file : listOfFiles) {
			if (file.isFile()) {
				Map<String, Turtle> turtleSet = new HashMap<String, Turtle>();
				try {
					inputStream = new BufferedReader(new FileReader(folderName
							+ file.getName()));

					String line = inputStream.readLine();
					while (line != null) {

						globalAgentCount = globalAgentCount + 1;

						String[] parts = line.split(",");
						String name = parts[0].trim();
						Turtle t = null;
						if (turtleSet.containsKey(name)) {
							t = turtleSet.get(name);
						} else {
							t = new Turtle(name);
						}
						// parts[1] the count of it.
						String intVarName = "";
						String extVarName = "";
						for (int i = 2; i < parts.length; i++) {
							String thisPart = parts[i].trim();
							if (thisPart.indexOf(":") > 0) {
								// new internal Variable
								String[] subParts = thisPart.split(":");
								String iName = subParts[0];
								String eName = subParts[1].substring(1);
								// Should be an external Variable
								if (eName.endsWith("]")) {
									eName = eName.substring(0,
											eName.length() - 1);
								}
								extVarName = eName;
								intVarName = iName;
							} else {
								// Should be an external Variable
								if (thisPart.endsWith("]")) {
									thisPart = thisPart.substring(0,
											thisPart.length() - 1);
								}
								extVarName = thisPart;
							}
							String relName = intVarName + ":" + extVarName;
							// System.out.println(name + " " + relName);
							if (relName.toLowerCase()
									.compareTo("age:carshead") == 0) {
								globalAgeFH = globalAgeFH + 1;
							} else if (relName.toLowerCase().compareTo(
									"age:carsspeed") == 0) {
								globalAgeFS = globalAgeFS + 1;
							} else if (relName.toLowerCase().compareTo(
									"age:distnear") == 0) {
								globalAgeDN = globalAgeDN + 1;
							} else if (relName.toLowerCase().compareTo(
									"age:carsnear") == 0) {
								globalAgeBN = globalAgeBN + 1;
							} else if (relName.toLowerCase().compareTo(
									"age:temperature") == 0) {
								globalAgeTM = globalAgeTM + 1;
							}  else if (relName.toLowerCase().compareTo(
									"speed:carshead") == 0) {
								globalMySpeedFH = globalMySpeedFH + 1;
							} else if (relName.toLowerCase().compareTo(
									"speed:carsspeed") == 0) {
								globalMySpeedFS = globalMySpeedFS + 1;
							} else if (relName.toLowerCase().compareTo(
									"speed:distnear") == 0) {
								globalMySpeedDN = globalMySpeedDN + 1;
							} else if (relName.toLowerCase().compareTo(
									"speed:carsnear") == 0) {
								globalMySpeedBN = globalMySpeedBN + 1;
							} else if (relName.toLowerCase().compareTo(
									"speed:temperature") == 0) {
								globalMySpeedTM = globalMySpeedTM + 1;
							} else if (relName.toLowerCase().compareTo(
									"myhead:carshead") == 0) {
								globalMyHeadFH = globalMyHeadFH + 1;
							} else if (relName.toLowerCase().compareTo(
									"myhead:carsspeed") == 0) {
								globalMyHeadFS = globalMyHeadFS + 1;
							} else if (relName.toLowerCase().compareTo(
									"myhead:distnear") == 0) {
								globalMyHeadDN = globalMyHeadDN + 1;
							} else if (relName.toLowerCase().compareTo(
									"myhead:carsnear") == 0) {
								globalMyHeadBN = globalMyHeadBN + 1;
							} else if (relName.toLowerCase().compareTo(
									"myhead:temperature") == 0) {
								globalMyHeadTM = globalMyHeadTM + 1;
							}

							t.setNoRels(false);
							String[] relParts = relName.trim().split(":");
							if (relParts[0].toLowerCase().startsWith("myhead")) {
								ArrayList<String> rels = t
										.getMyRelationshipsHeading();
								rels.add(parts[i].trim());
								t.setMyRelationshipsHeading(rels);
							} else if (relParts[0].toLowerCase().startsWith(
									"speed")) {
								ArrayList<String> rels = t
										.getMyRelationshipsSpeed();
								rels.add(parts[i].trim());
								t.setMyRelationshipsSpeed(rels);
							} else if (relParts[0].toLowerCase().startsWith(
									"age")) {
								ArrayList<String> rels = t
										.getMyRelationshipsAge();
								rels.add(parts[i].trim());
								t.setMyRelationshipsAge(rels);
							} 

							// Second Part
							if (relParts[1].toLowerCase().startsWith(
									"carshead")) {
								ArrayList<String> rels = t
										.getMyRelationshipsFlockHead();
								rels.add(parts[i].trim());
								t.setMyRelationshipsFlockHead(rels);
							} else if (relParts[1].toLowerCase().startsWith(
									"carsspeed")) {
								ArrayList<String> rels = t
										.getMyRelationshipsFlockSpeed();
								rels.add(parts[i].trim());
								t.setMyRelationshipsFlockSpeed(rels);
							} else if (relParts[1].toLowerCase().startsWith(
									"carsnear")) {
								ArrayList<String> rels = t
										.getMyRelationshipsBiredNear();
								rels.add(parts[i].trim());
								t.setMyRelationshipsBiredNear(rels);
							} else if (relParts[1].toLowerCase().startsWith(
									"distnear")) {
								ArrayList<String> rels = t
										.getMyRelationshipsDistNear();
								rels.add(parts[i].trim());
								t.setMyRelationshipsDistNear(rels);
							} else if (relParts[1].toLowerCase().startsWith(
									"temperature")) {
								ArrayList<String> rels = t
										.getMyRelationshipsTemperature();
								rels.add(parts[i].trim());
								t.setMyRelationshipsTemperature(rels);
							}
							globalRelCountSelected = globalRelCountSelected + 1;
						}

						turtleSet.put(name, t);

						line = inputStream.readLine();
					}

				} catch (Exception e) {

				} finally {
					try {
						if (inputStream != null) {
							inputStream.close();
						}
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}

				Iterator<String> turtleNames = turtleSet.keySet().iterator();
				while (turtleNames.hasNext()) {
					String name = turtleNames.next();
					Turtle t = turtleSet.get(name);
					if (t.isNoRels()) {
						noRels = noRels + 1;
					} else {

						if (t.getMyRelationshipsAge().size() > 0) {
							ageRels = ageRels + 1;
						}
						if (t.getMyRelationshipsSpeed().size() > 0) {
							speedRels = speedRels + 1;
						}
						if (t.getMyRelationshipsHeading().size() > 0) {
							myHeadRels = myHeadRels + 1;
						}
						if (t.getMyRelationshipsSpeed().size() == 0
								&& t.getMyRelationshipsHeading().size() == 0) {
							reallyBad = reallyBad + 1;
						}

						if (t.getMyRelationshipsAge().size() == 0) {
							goodInt = goodInt + 1;
							if (t.getMyRelationshipsTemperature().size() == 0) {
								great = great + 1;
							}
						}

						// Externals
						if (t.getMyRelationshipsFlockHead().size() > 0) {
							flockHeadRels = flockHeadRels + 1;
						}
						if (t.getMyRelationshipsFlockSpeed().size() > 0) {
							flockSpeedRels = flockSpeedRels + 1;
						}
						if (t.getMyRelationshipsDistNear().size() > 0) {
							distNearRels = distNearRels + 1;
						}
						if (t.getMyRelationshipsBiredNear().size() > 0) {
							birdsNearRels = birdsNearRels + 1;
						}
						if (t.getMyRelationshipsTemperature().size() > 0) {
							tempRels = tempRels + 1;
						} else {
							goodExt = goodExt + 1;
						}

						if (t.getMyRelationshipsFlockHead().size() == 0
								&& t.getMyRelationshipsFlockSpeed().size() == 0
								&& t.getMyRelationshipsDistNear().size() == 0
								&& t.getMyRelationshipsBiredNear().size() == 0) {
							reallyBadExt = reallyBadExt + 1;
						}
					}
				}
			}
		}

		// Now Print
		// Now Print
		System.out.println("Number of Agents: " + globalAgentCount);
		System.out
				.println("Number of Relationships: " + globalRelCountSelected);
		System.out.println("Average per agent: "
				+ (globalRelCountSelected / globalAgentCount));
		System.out.println("No Relationships: " + noRels);
		System.out.println("*******Internal***************");
		System.out.println("Age: " + ageRels);
		System.out.println("My Speed: " + speedRels);
		System.out.println("My Heading: " + myHeadRels);

		System.out.println("*******External***************");
		System.out.println("Flock Heading: " + flockHeadRels);
		System.out.println("Flock Speed: " + flockSpeedRels);
		System.out.println("Distance: " + distNearRels);
		System.out.println("Agents Near: " + birdsNearRels);
		System.out.println("Temperature: " + tempRels);

		System.out.println("Really Bad: " + reallyBad);
		System.out.println("Really Bad External: " + reallyBadExt);
		System.out.println("Really Good Internal: " + goodInt);
		System.out.println("Really Good External: " + goodExt);
		System.out.println("Really Great: " + great);

		System.out.println("***********Global***************");
		System.out.println("age-flock-head: " + globalAgeFH);
		System.out.println("age-flock-speed: " + globalAgeFS);
		System.out.println("age-distnear: " + globalAgeDN);
		System.out.println("age-birdnear: " + globalAgeBN);
		System.out.println("age-temperature: " + globalAgeTM);
		System.out.println("speed-flock-head: " + globalMySpeedFH);
		System.out.println("speed-flock-speed: " + globalMySpeedFS);
		System.out.println("speed-distnear: " + globalMySpeedDN);
		System.out.println("speed-birdnear: " + globalMySpeedBN);
		System.out.println("speed-temperature: " + globalMySpeedTM);
		System.out.println("myhead-flock-head: " + globalMyHeadFH);
		System.out.println("myhead-flock-speed: " + globalMyHeadFS);
		System.out.println("myhead-distnear: " + globalMyHeadDN);
		System.out.println("myhead-birdnear: " + globalMyHeadBN);
		System.out.println("myhead-temperature: " + globalMyHeadTM);
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

	private static class Turtle {
		private boolean noRels;

		public boolean isNoRels() {
			return noRels;
		}

		public void setNoRels(boolean noRels) {
			this.noRels = noRels;
		}

		private ArrayList<String> myRelationshipsAge;

		public ArrayList<String> getMyRelationshipsAge() {
			return myRelationshipsAge;
		}

		public void setMyRelationshipsAge(ArrayList<String> myRelationshipsAge) {
			this.myRelationshipsAge = myRelationshipsAge;
		}

		public ArrayList<String> getMyRelationshipsWeight() {
			return myRelationshipsWeight;
		}

		public void setMyRelationshipsWeight(
				ArrayList<String> myRelationshipsWeight) {
			this.myRelationshipsWeight = myRelationshipsWeight;
		}

		public ArrayList<String> getMyRelationshipsHeight() {
			return myRelationshipsHeight;
		}

		public void setMyRelationshipsHeight(
				ArrayList<String> myRelationshipsHeight) {
			this.myRelationshipsHeight = myRelationshipsHeight;
		}

		public ArrayList<String> getMyRelationshipsSpeed() {
			return myRelationshipsSpeed;
		}

		public void setMyRelationshipsSpeed(
				ArrayList<String> myRelationshipsSpeed) {
			this.myRelationshipsSpeed = myRelationshipsSpeed;
		}

		public ArrayList<String> getMyRelationshipsHeading() {
			return myRelationshipsHeading;
		}

		public void setMyRelationshipsHeading(
				ArrayList<String> myRelationshipsHeading) {
			this.myRelationshipsHeading = myRelationshipsHeading;
		}

		public ArrayList<String> getMyRelationshipsFlockSpeed() {
			return myRelationshipsFlockSpeed;
		}

		public void setMyRelationshipsFlockSpeed(
				ArrayList<String> myRelationshipsFlockSpeed) {
			this.myRelationshipsFlockSpeed = myRelationshipsFlockSpeed;
		}

		public ArrayList<String> getMyRelationshipsFlockHead() {
			return myRelationshipsFlockHead;
		}

		public void setMyRelationshipsFlockHead(
				ArrayList<String> myRelationshipsFlockHead) {
			this.myRelationshipsFlockHead = myRelationshipsFlockHead;
		}

		public ArrayList<String> getMyRelationshipsBiredNear() {
			return myRelationshipsBiredNear;
		}

		public void setMyRelationshipsBiredNear(
				ArrayList<String> myRelationshipsBiredNear) {
			this.myRelationshipsBiredNear = myRelationshipsBiredNear;
		}

		public ArrayList<String> getMyRelationshipsDistNear() {
			return myRelationshipsDistNear;
		}

		public void setMyRelationshipsDistNear(
				ArrayList<String> myRelationshipsDistNear) {
			this.myRelationshipsDistNear = myRelationshipsDistNear;
		}

		public ArrayList<String> getMyRelationshipsTemperature() {
			return myRelationshipsTemperature;
		}

		public void setMyRelationshipsTemperature(
				ArrayList<String> myRelationshipsTemperature) {
			this.myRelationshipsTemperature = myRelationshipsTemperature;
		}

		private ArrayList<String> myRelationshipsWeight;
		private ArrayList<String> myRelationshipsHeight;
		private ArrayList<String> myRelationshipsSpeed;
		private ArrayList<String> myRelationshipsHeading;
		private ArrayList<String> myRelationshipsFlockSpeed;
		private ArrayList<String> myRelationshipsFlockHead;
		private ArrayList<String> myRelationshipsBiredNear;
		private ArrayList<String> myRelationshipsDistNear;
		private ArrayList<String> myRelationshipsTemperature;
		private String myName;

		public Turtle(String name) {
			myName = name;
			noRels = true;
			myRelationshipsAge = new ArrayList<String>();
			myRelationshipsWeight = new ArrayList<String>();
			myRelationshipsHeight = new ArrayList<String>();
			myRelationshipsSpeed = new ArrayList<String>();
			myRelationshipsHeading = new ArrayList<String>();
			myRelationshipsFlockSpeed = new ArrayList<String>();
			myRelationshipsFlockHead = new ArrayList<String>();
			myRelationshipsBiredNear = new ArrayList<String>();
			myRelationshipsDistNear = new ArrayList<String>();
			myRelationshipsTemperature = new ArrayList<String>();
		}
	}

}
