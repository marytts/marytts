/**
 * Copyright 2007 DFKI GmbH.
 * All Rights Reserved.  Use is subject to license terms.
 *
 * This file is part of MARY TTS.
 *
 * MARY TTS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package marytts.tools.dbselection;

import java.io.BufferedReader;

/**
 * Represents the result of one test run
 * 
 * @author Anna Hunecke
 * 
 */

public class TestResult {

	private String resultText;
	private String shortResultText;
	private String settingText;
	private String date;
	private double phoneCoverage;
	private double simpleDiphoneCoverage;
	private double simpleProsodyCoverage;
	private double clusteredDiphoneCoverage;
	private double clusteredProsodyCoverage;
	private double maxPhoneCoverage;
	private double maxSimpleDiphoneCoverage;
	private double maxSimpleProsodyCoverage;
	private double maxClusteredDiphoneCoverage;
	private double maxClusteredProsodyCoverage;
	private double averageSentenceLength;
	private int maximumSentenceLength;
	private int minimumSentenceLength;
	private int numSentences;

	public TestResult(BufferedReader resultIn) throws Exception {
		StringBuilder textBuf = new StringBuilder();
		StringBuilder shortTextBuf = new StringBuilder();
		StringBuilder settingBuf = new StringBuilder();
		// Results for 30_06_2007_18_55_49:
		String line = resultIn.readLine();
		String[] lineSplit = line.split(" ");
		date = lineSplit[2].substring(0, lineSplit[2].length() - 1);
		textBuf.append(date + "\n");
		shortTextBuf.append(date + "\n");
		// Number of basenames 5000
		line = resultIn.readLine();
		textBuf.append(line + "\n");
		lineSplit = line.split(" ");
		settingBuf.append(lineSplit[3] + ";");
		shortTextBuf.append("Num basenames " + lineSplit[3] + "\n");
		// Stop criterion numSentences 100
		line = resultIn.readLine();
		textBuf.append(line + "\n");
		lineSplit = line.split(" ");
		if (lineSplit[2].equals("numSentences")) {
			settingBuf.append(lineSplit[3] + "sents;");
			shortTextBuf.append("Stop " + lineSplit[3] + " sents\n");
		} else {
			settingBuf.append(lineSplit[2] + ";");
			shortTextBuf.append("Stop " + lineSplit[2] + "\n");
		}
		// simpleDiphones true
		line = resultIn.readLine();
		textBuf.append(line + "\n");
		shortTextBuf.append(line + "\n");
		lineSplit = line.split(" ");
		if (lineSplit[1].equals("false")) {
			settingBuf.append("CD;");
		} else {
			settingBuf.append("SD;");
		}
		// frequency none
		line = resultIn.readLine();
		textBuf.append(line + "\n");
		shortTextBuf.append(line + "\n");
		lineSplit = line.split(" ");
		settingBuf.append(lineSplit[1] + ";");
		// considerSentenceLength false
		line = resultIn.readLine();
		textBuf.append(line + "\n");
		lineSplit = line.split(" ");
		boolean considerSentenceLength = false;
		if (lineSplit[1].equals("true")) {
			considerSentenceLength = true;
		}
		// phoneLevelWeight 10.0
		line = resultIn.readLine();
		textBuf.append(line + "\n");
		shortTextBuf.append(line + "\n");
		lineSplit = line.split(" ");
		settingBuf.append(lineSplit[1].substring(0, lineSplit[1].length() - 2) + "/");
		// diphoneLevelWeight 5.0
		line = resultIn.readLine();
		textBuf.append(line + "\n");
		shortTextBuf.append(line + "\n");
		lineSplit = line.split(" ");
		settingBuf.append(lineSplit[1].substring(0, lineSplit[1].length() - 2) + "/");
		// prosodyLevelWeight 1.0
		line = resultIn.readLine();
		textBuf.append(line + "\n");
		shortTextBuf.append(line + "\n");
		lineSplit = line.split(" ");
		settingBuf.append(lineSplit[1].substring(0, lineSplit[1].length() - 2) + ";");
		// maxSentenceLength 0
		line = resultIn.readLine();
		textBuf.append(line + "\n");
		if (considerSentenceLength) {
			lineSplit = line.split(" ");
			settingBuf.append("minSL " + lineSplit[1] + ";");
			shortTextBuf.append(line + "\n");
		}
		// minSentenceLength 0
		line = resultIn.readLine();
		textBuf.append(line + "\n");
		if (considerSentenceLength) {
			lineSplit = line.split(" ");
			settingBuf.append("maxSL " + lineSplit[1] + ";");
			shortTextBuf.append(line + "\n");
		}
		// divideWantedWeightBy 2.0
		line = resultIn.readLine();
		textBuf.append(line + "\n");
		shortTextBuf.append(line + "\n");
		lineSplit = line.split(" ");
		settingBuf.append(lineSplit[1].substring(0, lineSplit[1].length() - 2) + ";");
		//
		// textBuf.append(resultIn.readLine()+"\n");
		resultIn.readLine();
		// Num sent in cover : 5000
		line = resultIn.readLine();
		textBuf.append(line + "\n");
		lineSplit = line.split(" ");
		shortTextBuf.append("Num sents: " + lineSplit[5] + "\n");
		numSentences = Integer.parseInt(lineSplit[5]);
		settingBuf.append(lineSplit[5]);
		// Avg sent length : 107.92880
		line = resultIn.readLine();
		textBuf.append(line + "\n");
		lineSplit = line.split(" ");
		averageSentenceLength = Double.parseDouble(lineSplit[4]);
		shortTextBuf.append("Avg sent length: " + lineSplit[4] + "\n");
		// Max sent length : 924
		line = resultIn.readLine();
		textBuf.append(line + "\n");
		lineSplit = line.split(" ");
		maximumSentenceLength = Integer.parseInt(lineSplit[4]);
		shortTextBuf.append("Max sent length: " + lineSplit[4] + "\n");
		// Min sent length : 4
		line = resultIn.readLine();
		textBuf.append(line + "\n");
		lineSplit = line.split(" ");
		minimumSentenceLength = Integer.parseInt(lineSplit[4]);
		shortTextBuf.append("Min sent length: " + lineSplit[4] + "\n");
		// phones: 0.96364 (0.96364)
		line = resultIn.readLine();
		textBuf.append(line + "\n");
		lineSplit = line.split(" ");
		shortTextBuf.append(line + "\n");
		phoneCoverage = Double.parseDouble(lineSplit[1]);
		maxPhoneCoverage = Double.parseDouble(lineSplit[2].substring(1, lineSplit[2].length() - 1));
		// Simple Coverage:
		textBuf.append(resultIn.readLine() + "\n");
		// diphones: 0.50347 (0.50347)
		line = resultIn.readLine();
		textBuf.append(line + "\n");
		lineSplit = line.split(" ");
		shortTextBuf.append("simple Diphones: " + lineSplit[1] + " " + lineSplit[2] + "\n");
		simpleDiphoneCoverage = Double.parseDouble(lineSplit[1]);
		maxSimpleDiphoneCoverage = Double.parseDouble(lineSplit[2].substring(1, lineSplit[2].length() - 1));
		// overall: 0.30793 (0.30793)
		line = resultIn.readLine();
		textBuf.append(line + "\n");
		lineSplit = line.split(" ");
		shortTextBuf.append("simple Prosody: " + lineSplit[1] + " " + lineSplit[2] + "\n");
		simpleProsodyCoverage = Double.parseDouble(lineSplit[1]);
		maxSimpleProsodyCoverage = Double.parseDouble(lineSplit[2].substring(1, lineSplit[2].length() - 1));
		// Clustered Coverage:
		textBuf.append(resultIn.readLine() + "\n");
		// diphones: 0.53872 (0.53872)
		line = resultIn.readLine();
		textBuf.append(line + "\n");
		lineSplit = line.split(" ");
		shortTextBuf.append("clustered Diphones: " + lineSplit[1] + " " + lineSplit[2] + "\n");
		clusteredDiphoneCoverage = Double.parseDouble(lineSplit[1]);
		maxClusteredDiphoneCoverage = Double.parseDouble(lineSplit[2].substring(1, lineSplit[2].length() - 1));
		// overall: 0.35926 (0.35926)
		line = resultIn.readLine();
		textBuf.append(line + "\n");
		lineSplit = line.split(" ");
		shortTextBuf.append("clustered Prosody: " + lineSplit[1] + " " + lineSplit[2] + "\n");
		clusteredProsodyCoverage = Double.parseDouble(lineSplit[1]);
		maxClusteredProsodyCoverage = Double.parseDouble(lineSplit[2].substring(1, lineSplit[2].length() - 1));
		resultText = textBuf.toString();
		shortResultText = shortTextBuf.toString();
		settingText = settingBuf.toString();
	}

	public String getText() {
		return resultText;
	}

	public String getShortText() {
		return shortResultText;
	}

	public String getSettings() {
		return settingText;
	}

	public String getDate() {
		return date;
	}

	public double getPhoneCoverage() {
		return phoneCoverage;
	}

	public double getMaxPhoneCoverage() {
		return maxPhoneCoverage;
	}

	public double getSimpleDiphoneCoverage() {
		return simpleDiphoneCoverage;
	}

	public double getMaxSimpleDiphoneCoverage() {
		return maxSimpleDiphoneCoverage;
	}

	public double getSimpleProsodyCoverage() {
		return simpleProsodyCoverage;
	}

	public double getMaxSimpleProsodyCoverage() {
		return maxSimpleProsodyCoverage;
	}

	public double getClusteredDiphoneCoverage() {
		return clusteredDiphoneCoverage;
	}

	public double getMaxClusteredDiphoneCoverage() {
		return maxClusteredDiphoneCoverage;
	}

	public double getClusteredProsodyCoverage() {
		return clusteredProsodyCoverage;
	}

	public double getMaxClusteredProsodyCoverage() {
		return maxClusteredProsodyCoverage;
	}

	public double getAverageSentenceLength() {
		return averageSentenceLength;
	}

	public int getMaxSentenceLength() {
		return maximumSentenceLength;
	}

	public int getMinSentenceLength() {
		return minimumSentenceLength;
	}

	public int getNumSentences() {
		return numSentences;
	}

	public String getCoverageString() {
		return "SD " + simpleDiphoneCoverage + " SP " + simpleProsodyCoverage + " CD " + clusteredDiphoneCoverage + " CP "
				+ clusteredProsodyCoverage;

	}

}
