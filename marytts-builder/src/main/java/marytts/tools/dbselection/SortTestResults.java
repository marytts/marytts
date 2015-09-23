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
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Sorts the test results according to the four coverage measures
 * 
 * @author Anna Hunecke
 * 
 */
public class SortTestResults {

	/**
	 * 
	 * 
	 * @param args
	 *            comand line arguments
	 * @throws Exception
	 *             Exception
	 */
	public static void main(String[] args) throws Exception {
		File logFile = new File(args[0]);
		boolean justSettings = false;
		boolean shortResults = false;
		if (args.length > 1) {
			if (args[1].equals("justSettings")) {
				justSettings = true;
			}
			if (args[1].equals("shortResults")) {
				shortResults = true;
			}
		}

		/*
		 * there is one file for each coverage measure and one for the settings which led to the same results
		 */
		File simpleDiphoneSortFile = new File("./simpleDiphoneSort.txt");
		File clusteredDiphoneSortFile = new File("./clusteredDiphoneSort.txt");
		File simpleProsodySortFile = new File("./simpleProsodySort.txt");
		File clusteredProsodySortFile = new File("./clusteredProsodySort.txt");
		File sameResultsFile = new File("./sameResults.txt");
		File numSentencesFile = new File("./numSentencesSort.txt");

		List<TestResult> resultList = new ArrayList<TestResult>();
		BufferedReader logIn = new BufferedReader(new InputStreamReader(new FileInputStream(logFile), "UTF-8"));
		String line;
		while ((line = logIn.readLine()) != null) {
			if (line.equals(""))
				continue;
			if (line.startsWith("***")) {
				resultList.add(new TestResult(logIn));
			}
		}

		/* sort */
		SortedMap<Double, TestResult> simpleDiphone2Results = new TreeMap<Double, TestResult>(Collections.reverseOrder());
		SortedMap<Double, TestResult> clusteredDiphone2Results = new TreeMap<Double, TestResult>(Collections.reverseOrder());
		SortedMap<Double, TestResult> simpleProsody2Results = new TreeMap<Double, TestResult>(Collections.reverseOrder());
		SortedMap<Double, TestResult> clusteredProsody2Results = new TreeMap<Double, TestResult>(Collections.reverseOrder());
		SortedMap<Integer, TestResult> numSentences2Results = new TreeMap<Integer, TestResult>();
		double[] sdCovArray = new double[resultList.size()];
		double[] soCovArray = new double[resultList.size()];
		double[] cdCovArray = new double[resultList.size()];
		double[] coCovArray = new double[resultList.size()];
		int index = 0;
		for (Iterator it = resultList.iterator(); it.hasNext();) {
			TestResult nextResult = (TestResult) it.next();
			// sort according to simpleDiphone coverage
			double cov = nextResult.getSimpleDiphoneCoverage();
			sdCovArray[index] = cov;
			Double coverage = new Double(cov);
			sort(simpleDiphone2Results, nextResult, coverage);

			// sort according to simpleProsody coverage
			cov = nextResult.getSimpleProsodyCoverage();
			soCovArray[index] = cov;
			coverage = new Double(cov);
			sort(simpleProsody2Results, nextResult, coverage);

			// sort according to clusteredDiphone coverage
			cov = nextResult.getClusteredDiphoneCoverage();
			cdCovArray[index] = cov;
			coverage = new Double(cov);
			sort(clusteredDiphone2Results, nextResult, coverage);

			// sort according to clusteredProsody coverage
			cov = nextResult.getClusteredProsodyCoverage();
			coCovArray[index] = cov;
			coverage = new Double(cov);
			sort(clusteredProsody2Results, nextResult, coverage);

			// sort according to number of sentences
			Integer numSents = new Integer(nextResult.getNumSentences());
			sort(numSentences2Results, nextResult, numSents);

			index++;
		}

		/* print out results */
		// simpleDiphoneCoverage
		PrintWriter out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(simpleDiphoneSortFile), "UTF-8"), true);
		TestResult result1 = (TestResult) resultList.get(0);
		double maxCoverage = result1.getMaxSimpleDiphoneCoverage();
		print(simpleDiphone2Results, out, shortResults, justSettings, "simple diphone coverage (max: " + maxCoverage + ")");

		// simpleProsodyCoverage
		out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(simpleProsodySortFile), "UTF-8"), true);
		maxCoverage = result1.getMaxSimpleProsodyCoverage();
		print(simpleProsody2Results, out, shortResults, justSettings, "simple Prosody coverage (max: " + maxCoverage + ")");

		// clusteredDiphoneCoverage
		out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(clusteredDiphoneSortFile), "UTF-8"), true);
		maxCoverage = result1.getMaxClusteredDiphoneCoverage();
		print(clusteredDiphone2Results, out, shortResults, justSettings, "clustered diphone coverage (max: " + maxCoverage + ")");

		// clusteredProsodyCoverage
		out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(clusteredProsodySortFile), "UTF-8"), true);
		maxCoverage = result1.getMaxClusteredProsodyCoverage();
		print(clusteredProsody2Results, out, shortResults, justSettings, "clustered Prosody coverage (max: " + maxCoverage + ")");

		// same Results
		out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(sameResultsFile), "UTF-8"), true);
		printSameResults(out, resultList, sdCovArray, soCovArray, cdCovArray, coCovArray, justSettings);

		// numSentences
		out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(numSentencesFile), "UTF-8"), true);
		printNumSentences(out, resultList, numSentences2Results, shortResults, justSettings);

	}

	private static void sort(SortedMap sortMap, TestResult result, Object coverage) {
		if (sortMap.containsKey(coverage)) {
			List nextResultList = (List) sortMap.get(coverage);
			nextResultList.add(result);
		} else {
			List nextResultList = new ArrayList();
			nextResultList.add(result);
			sortMap.put(coverage, nextResultList);
		}
	}

	private static void print(SortedMap sortMap, PrintWriter out, boolean shortResults, boolean justSettings, String whichCoverage) {
		int index = 1;
		if (justSettings) {
			out.println(whichCoverage + "\n");
		}
		DecimalFormat df = new DecimalFormat("0.00");
		for (Iterator it = sortMap.keySet().iterator(); it.hasNext();) {
			Double nextCoverage = (Double) it.next();
			List nextResultList = (List) sortMap.get(nextCoverage);
			if (justSettings) {
				for (Iterator it2 = nextResultList.iterator(); it2.hasNext();) {
					TestResult nextResult = (TestResult) it2.next();
					if (index < 10) {
						out.println("0" + index + ": " + nextCoverage + "\t" + nextResult.getSettings());
					} else {
						out.println(index + ": " + nextCoverage + "\t" + nextResult.getSettings());
					}

				}
			} else {
				out.println("*** " + index + " ***\n");
				for (Iterator it2 = nextResultList.iterator(); it2.hasNext();) {
					if (shortResults) {
						out.println(((TestResult) it2.next()).getShortText() + "\n");
					} else {
						out.println(((TestResult) it2.next()).getText() + "\n");
					}
				}
				out.println();
			}
			index++;
		}
		out.close();
	}

	private static void printSameResults(PrintWriter out, List resultList, double[] sdCovArray, double[] soCovArray,
			double[] cdCovArray, double[] coCovArray, boolean justSettings) {
		List sameResults = new ArrayList();
		for (int i = 0; i < resultList.size(); i++) {
			TestResult nextResult = (TestResult) resultList.get(i);
			double sdCov = sdCovArray[i];
			if (sdCov == -1)
				continue;
			double soCov = soCovArray[i];
			double cdCov = cdCovArray[i];
			double coCov = coCovArray[i];
			List nextSameResults = new ArrayList();
			for (int j = 0; j < sdCovArray.length; j++) {
				if (i == j)
					continue;
				double nextSDCov = sdCovArray[j];
				double nextSOCov = soCovArray[j];
				double nextCDCov = cdCovArray[j];
				double nextCOCov = coCovArray[j];
				if (sdCov == nextSDCov && soCov == nextSOCov && cdCov == nextCDCov && coCov == nextCOCov) {
					// every coverage is the same
					nextSameResults.add(resultList.get(j));
					sdCovArray[j] = -1;
				}
			}
			if (nextSameResults.size() > 0) {
				nextSameResults.add(0, nextResult);
				sdCovArray[i] = -1;
				sameResults.add(nextSameResults);
			}
		}

		// print the list of same results
		for (Iterator it = sameResults.iterator(); it.hasNext();) {
			List nextResultList = (List) it.next();
			if (justSettings) {
				for (Iterator it2 = nextResultList.iterator(); it2.hasNext();) {
					out.println(((TestResult) it2.next()).getSettings());
				}
			} else {
				for (Iterator it2 = nextResultList.iterator(); it2.hasNext();) {
					out.println(((TestResult) it2.next()).getText() + "\n");
				}
			}
			out.println("*******************");
		}
	}

	private static void printNumSentences(PrintWriter out, List resultList, Map numSentences2Results, boolean shortResults,
			boolean justSettings) {
		int index = 1;

		DecimalFormat df = new DecimalFormat("0.00");
		for (Iterator it = numSentences2Results.keySet().iterator(); it.hasNext();) {
			Integer numSentences = (Integer) it.next();
			List nextResultList = (List) numSentences2Results.get(numSentences);
			if (justSettings) {
				for (Iterator it2 = nextResultList.iterator(); it2.hasNext();) {
					TestResult nextResult = (TestResult) it2.next();
					if (index < 10) {
						out.println("0" + index + ": " + numSentences + "\t" + nextResult.getSettings() + "\n\t\t"
								+ nextResult.getCoverageString() + "\n");
					} else {
						out.println(index + ": " + numSentences + "\t" + nextResult.getSettings() + "\n\t\t"
								+ nextResult.getCoverageString() + "\n");
					}
				}
			} else {
				out.println("*** " + index + " ***\n");
				for (Iterator it2 = nextResultList.iterator(); it2.hasNext();) {
					if (shortResults) {
						out.println(((TestResult) it2.next()).getShortText() + "\n");
					} else {
						out.println(((TestResult) it2.next()).getText() + "\n");
					}
				}
				out.println();
			}
			index++;
		}
		out.close();
	}

}
