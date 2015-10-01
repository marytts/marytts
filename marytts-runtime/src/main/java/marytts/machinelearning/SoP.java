/**
 * Copyright 2010 DFKI GmbH.
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

package marytts.machinelearning;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.util.Scanner;

import marytts.features.FeatureDefinition;
import marytts.unitselection.select.Target;

/**
 * Contains the coefficients and factors of an equation of the form: if interceptTterm = TRUE solution = coeffs[0] +
 * coeffs[1]*factors[0] + coeffs[2]*factors[1] + ... + coeffs[n]*factors[n-1] if interceptterm = FALSE solution =
 * coeffs[0]*factors[0] + coeffs[1]*factors[1] + ... + coeffs[n]*factors[n]
 * 
 * @author marcela
 * 
 */
public class SoP {

	private double coeffs[]; // coefficients of the multiple linear equation
	private String factors[]; // variables in the multiple linear
	private int factorsIndex[]; // indices in featureDefinition
	boolean interceptTerm;
	double correlation;
	double rmse;
	double solution;
	FeatureDefinition featureDefinition = null;

	public void setCorrelation(double val) {
		correlation = val;
	}

	public void setRMSE(double val) {
		rmse = val;
	}

	public double[] getCoeffs() {
		return coeffs;
	}

	public double getCorrelation() {
		return correlation;
	}

	public double getRMSE() {
		return rmse;
	}

	public int[] getFactorsIndex() {
		return factorsIndex;
	}

	public SoP() {
	}

	/**
	 * Build a new empty sop with the given feature definition.
	 * 
	 * @param featDef
	 *            featDef
	 */
	public SoP(FeatureDefinition featDef) {
		this.featureDefinition = featDef;
	}

	/***
	 * if b0=true then the number of selected factors 0 numCoeffs-1 (there is one coeff more) if b0=false then the number of
	 * selected factor is the same as the number of coeffs When setting the factors, it checks to which indexes correspond
	 * according to the featureDefinition.
	 * 
	 * @param coeffsVal
	 *            coeffsVal
	 * @param selectedFactorsIndex
	 *            selectedFactorsIndex
	 * @param allFactorsList
	 *            allFactorsList
	 * @param b0
	 *            b0
	 * @throws Exception
	 *             Exception
	 */
	public void setCoeffsAndFactors(double coeffsVal[], int selectedFactorsIndex[], String allFactorsList[], boolean b0)
			throws Exception {
		if (featureDefinition == null) {
			throw new Exception("FeatureDefinition not defined in SoP");
		} else {
			interceptTerm = b0;
			int numFactors = selectedFactorsIndex.length;
			if (interceptTerm) {
				// there is one coefficient more than factors
				coeffs = new double[numFactors + 1];
				factors = new String[numFactors + 1];
				factorsIndex = new int[numFactors + 1];
				coeffs[0] = coeffsVal[0];
				factors[0] = "_"; // if there is intercept term then the first factor is empty here indicated with _
				factorsIndex[0] = -1;
				for (int i = 1; i < (numFactors + 1); i++) {
					coeffs[i] = coeffsVal[i];
					factors[i] = allFactorsList[selectedFactorsIndex[i - 1]];
					factorsIndex[i] = featureDefinition.getFeatureIndex(factors[i]);
				}
			} else {
				coeffs = new double[numFactors];
				factors = new String[numFactors];
				factorsIndex = new int[numFactors];
				for (int i = 0; i < numFactors; i++) {
					coeffs[i] = coeffsVal[i];
					factors[i] = allFactorsList[selectedFactorsIndex[i]];
					factorsIndex[i] = featureDefinition.getFeatureIndex(factors[i]);
				}
			}
		}
	}

	// Need to add a SoP.load() method to load the coeffs and features from a file
	// this means that we need a sop file for each type... left, mid and righ f0
	// for duration ???
	// this function also set the featureDefinition for this model
	public void load(String sopFile) {
		// System.out.println("sopFileName: " + sopFile);
		String nextLine;
		String strContext = "";
		Scanner s = null;
		try {
			s = new Scanner(new BufferedReader(new FileReader(sopFile)));

			// The first part contains the feature definition
			while (s.hasNext()) {
				nextLine = s.nextLine();
				if (nextLine.trim().equals(""))
					break;
				else
					strContext += nextLine + "\n";
			}
			// the featureDefinition is the same for vowel, consonant and Pause
			featureDefinition = new FeatureDefinition(new BufferedReader(new StringReader(strContext)), false);

			// next line should contain the coeffs and linguistic features
			// vowel line
			if (s.hasNext()) {
				nextLine = s.nextLine();
				// System.out.println("line vowel = " + nextLine);
				setCoeffsAndFactors(nextLine);
				// printCoefficients();
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			if (s != null)
				s.close();
		}

	}

	public void setCoeffsAndFactors(String line) {

		// features definition should be already set

		String word[] = line.split(" ");
		int j = 0;
		coeffs = new double[word.length / 2];
		factors = new String[word.length / 2];
		factorsIndex = new int[word.length / 2];
		interceptTerm = false;
		for (int i = 0; i < word.length; i++) {
			// System.out.println("w=" + word[i]);
			coeffs[j] = Double.parseDouble(word[i]);
			factors[j] = word[i + 1];
			if (word[i + 1].contentEquals("_")) {
				interceptTerm = true;
				factorsIndex[j] = -1;
			} else
				factorsIndex[j] = featureDefinition.getFeatureIndex(factors[j]);
			i++;
			j++;
		}
	}

	// this we will not need after using acoustic modeller
	public SoP(String line, FeatureDefinition feaDef) {

		this.featureDefinition = feaDef;

		String word[] = line.split(" ");
		int j = 0;
		coeffs = new double[word.length / 2];
		factors = new String[word.length / 2];
		factorsIndex = new int[word.length / 2];
		interceptTerm = false;
		for (int i = 0; i < word.length; i++) {
			// System.out.println("w=" + word[i]);
			coeffs[j] = Double.parseDouble(word[i]);
			factors[j] = word[i + 1];
			if (word[i + 1].contentEquals("_")) {
				interceptTerm = true;
				factorsIndex[j] = -1;
			} else
				factorsIndex[j] = featureDefinition.getFeatureIndex(factors[j]);
			i++;
			j++;
		}
	}

	public FeatureDefinition getFeatureDefinition() {
		return featureDefinition;
	}

	/**
	 * Solve the linear equation given the features (factors) in t and coeffs and factors in the SoP object * if interceptTterm =
	 * TRUE solution = coeffs[0] + coeffs[1]*factors[0] + coeffs[2]*factors[1] + ... + coeffs[n]*factors[n-1] if interceptterm =
	 * FALSE solution = coeffs[0]*factors[0] + coeffs[1]*factors[1] + ... + coeffs[n]*factors[n]
	 * 
	 * @param t
	 *            t
	 * @param feaDef
	 *            feaDef
	 * @param log
	 *            log
	 * @return solution
	 */
	public double solve(Target t, FeatureDefinition feaDef, boolean log) {
		solution = 0.0f;
		double lastPosSolution = 0.0;
		if (interceptTerm) {
			// the first factor is empty filled with "_" so it should not be used
			solution = coeffs[0];
			for (int i = 1; i < coeffs.length; i++) {
				solution = solution + (coeffs[i] * t.getFeatureVector().getByteFeature(factorsIndex[i]));
				if (solution > 0.0)
					lastPosSolution = solution;
				else
					System.out.println("WARNING: sop solution negative");
			}
		} else {
			for (int i = 0; i < coeffs.length; i++) {
				solution = solution + (coeffs[i] * t.getFeatureVector().getByteFeature(factorsIndex[i]));
				if (solution > 0.0)
					lastPosSolution = solution;
				else
					System.out.println("WARNING: sop solution negative");
			}
		}
		if (solution < 0.0)
			solution = lastPosSolution;

		if (log)
			return Math.exp(solution);
		else
			return solution;
	}

	public double solve(Target t, FeatureDefinition feaDef, boolean log, boolean debug) {
		solution = 0.0f;
		double lastPosSolution = 0.0;
		if (interceptTerm) {
			// the first factor is empty filled with "_" so it should not be used
			solution = coeffs[0];
			if (debug)
				System.out.format("   solution = %.3f (coeff[0])\n", coeffs[0]);
			for (int i = 1; i < coeffs.length; i++) {
				// check if the retrieved bytevalue is allowed for the kind of feature factor
				byte feaVal = t.getFeatureVector().getByteFeature(factorsIndex[i]);
				String feaValStr = t.getFeatureVector().getFeatureAsString(factorsIndex[i], feaDef);
				if (feaDef.hasFeatureValue(factorsIndex[i], feaValStr)) {
					if (debug)
						System.out.format("   %.3f + (%.3f * %d (%s) = ", solution, coeffs[i], feaVal, factors[i]);
					solution = solution + (coeffs[i] * feaVal);
					if (debug)
						System.out.format("%.3f  featureIndex=%d  feaValStr=%s \n", solution, factorsIndex[i], feaValStr);
				} else {
					System.out.format("WARNING: Feature value for %s = %s is not valid", coeffs[i], feaValStr);
				}
				if (solution > 0.0)
					lastPosSolution = solution;
			}
		} else {
			for (int i = 0; i < coeffs.length; i++)
				solution = solution + (coeffs[i] * t.getFeatureVector().getByteFeature(factorsIndex[i]));
			if (solution > 0.0)
				lastPosSolution = solution;

		}
		if (debug) {
			if (log)
				return Math.exp(lastPosSolution);
			else
				return lastPosSolution;
		} else {
			if (log)
				return Math.exp(solution);
			else
				return solution;
		}
	}

	public double interpret(Target t) {

		return solve(t, this.featureDefinition, false);

	}

	/***
	 * First line vowel coefficients plus factors, second line consonant coefficients plus factors
	 * 
	 * @param toSopFile
	 *            toSopFile
	 */
	public void saveSelectedFeatures(PrintWriter toSopFile) {
		for (int j = 0; j < coeffs.length; j++)
			toSopFile.print(coeffs[j] + " " + factors[j] + " ");
		toSopFile.println();
	}

	public void printCoefficients() {
		if (coeffs != null) {
			System.out.println("SoP coefficients (factor : factorIndex in FeatureDefinition)");
			for (int j = 0; j < coeffs.length; j++)
				System.out.format(" %.5f (%s : %d)\n", coeffs[j], factors[j], factorsIndex[j]);
		} else
			System.out.println("There is no coefficients to print (coeffs=null).");
	}

}
