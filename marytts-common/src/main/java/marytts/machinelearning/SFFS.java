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

import marytts.util.math.MathUtils;
import marytts.util.math.Regression;
import Jama.Matrix;

/***
 * Sequential Floating Forward Search(SFFS) for selection of features Ref: Pudil, P., J. Novovičová, and J. Kittler. 1994.
 * Floating search methods in feature selection. Pattern Recogn. Lett. 15, no. 11: 1119-1125.
 * (http://staff.utia.cas.cz/novovic/files/PudNovKitt_PRL94-Floating.pdf)
 * 
 * @author marcela
 */
public class SFFS {

	protected boolean interceptTerm = true;
	protected boolean logSolution = false;
	protected int solutionSize = 1;

	/**
	 * Sequential Floating Forward Search(SFFS) for selection of features
	 * 
	 * @param solSize
	 *            : size of the solution (default = 1)
	 * @param b0
	 *            : if true include interceptTerm or b0 in the linear equation (default = true)
	 * @param logSol
	 *            : if true use log(independent variable) (default = false)
	 */
	public SFFS(int solSize, boolean b0, boolean logSol) {
		interceptTerm = b0;
		logSolution = logSol;
		solutionSize = solSize;
	}

	public void trainModel(String[] lingFactors, String featuresFile, int numFeatures, double percentToTrain, SoP sop)
			throws Exception {

		int d = solutionSize; // desired size of the solution
		int D = 0; // maximum deviation allowed with respect to d
		int cols = lingFactors.length;
		int indVariable = cols; // the last column is the independent variable
		int rows = numFeatures;
		int rowIniTrain = 0;
		int percentVal = (int) (Math.floor((numFeatures * percentToTrain)));
		int rowEndTrain = percentVal - 1;
		int rowIniTest = percentVal;
		int rowEndTest = percentVal + (numFeatures - percentVal - 1) - 1;

		System.out.println("Number of points: " + rows + "\nNumber of points used for training from " + rowIniTrain + " to "
				+ rowEndTrain + "(Total train=" + (rowEndTrain - rowIniTrain) + ")\nNumber of points used for testing from "
				+ rowIniTest + " to " + rowEndTest + "(Total test=" + (rowEndTest - rowIniTest) + ")");
		System.out.println("Number of linguistic factors: " + cols);
		System.out.println("Max number of selected features in SFFS: " + (d + D));
		if (interceptTerm)
			System.out.println("Using intercept Term for regression");
		else
			System.out.println("No intercept Term for regression");
		if (logSolution)
			System.out.println("Using log(val) as independent variable" + "\n");
		else
			System.out.println("Using independent variable without log()" + "\n");
		// copy indexes of column features
		int Y[] = new int[lingFactors.length];
		int X[] = {};
		for (int j = 0; j < lingFactors.length; j++)
			Y[j] = j;

		// we need to remove from Y the column features that have mean 0.0
		System.out.println("Checking and removing columns with mean=0.0");
		Y = checkMeanColumns(featuresFile, Y, lingFactors);

		int selectedCols[] = sequentialForwardFloatingSelection(featuresFile, indVariable, lingFactors, X, Y, d, D, rowIniTrain,
				rowEndTrain, sop);

		sop.printCoefficients();
		System.out.println("Correlation original val / predicted val = " + sop.getCorrelation()
				+ "\nRMSE (root mean square error) = " + sop.getRMSE());
		Regression reg = new Regression();
		reg.setCoeffs(sop.getCoeffs());
		System.out.println("\nNumber points used for training=" + (rowEndTrain - rowIniTrain));
		reg.predictValues(featuresFile, cols, selectedCols, interceptTerm, rowIniTrain, rowEndTrain);
		System.out.println("\nNumber points used for testing=" + (rowEndTest - rowIniTest));
		reg.predictValues(featuresFile, cols, selectedCols, interceptTerm, rowIniTest, rowEndTest);

	}

	public int[] sequentialForwardFloatingSelection(String dataFile, int indVariable, String[] features, int X[], int Y[], int d,
			int D, int rowIni, int rowEnd, SoP sop) throws Exception {

		int indVarColNumber = features.length; // the last column is the independent variable

		int ms; // most significant
		int ls; // least significant

		double forwardJ[] = new double[3];
		forwardJ[0] = 0.0; // least significance X_k+1
		forwardJ[1] = 0.0; // significance of X_k
		forwardJ[2] = 0.0; // most significance of X_k+1

		double backwardJ[] = new double[3];
		backwardJ[0] = 0.0; // least significance X_k-1
		backwardJ[1] = 0.0; // significance X_k
		backwardJ[2] = 0.0; // most significance of X_k-1

		int k = X.length;
		boolean condSFS = true; // Forward condition to be able to select from Y a most new significant feature
		boolean condSBS = true; // Backward condition: X has to have at least two elements to be able to select the least
								// significant in X
		double corX = 0.0;
		double improvement;
		while (k < d + D && condSFS) {
			// we need at least 1 feature in Y to continue
			if (Y.length > 1) {
				// Step 1. (Inclusion)
				// given X_k create X_k+1 : add the most significant feature of Y to X
				System.out.println("ForwardSelection k=" + k + " remaining features=" + Y.length);
				ms = sequentialForwardSelection(dataFile, features, indVarColNumber, X, Y, forwardJ, rowIni, rowEnd);
				System.out.format("corXplusy=%.4f  corX=%.4f\n", forwardJ[2], forwardJ[1]);
				corX = forwardJ[2];
				System.out.println("Most significant new feature to add: " + features[ms]);
				// add index to selected and remove it form Y
				X = MathUtils.addIndex(X, ms);
				Y = MathUtils.removeIndex(Y, ms);
				k = k + 1;

				// continue with a SBG step
				condSBS = true;

				// is this the best (k-1) subset so far
				while (condSBS && (k <= d + D) && k > 1) {
					if (X.length > 1) {
						// Step 3. (Continuation of conditional exclusion)
						// Find the least significant feature x_s in the reduced X'
						System.out.println(" BackwardSelection k=" + k);
						// get the least significant and check if removing it the correlation is better with or without this
						// feature
						ls = sequentialBackwardSelection(dataFile, features, indVarColNumber, X, backwardJ, rowIni, rowEnd);
						corX = backwardJ[1];
						improvement = Math.abs(backwardJ[0] - backwardJ[1]);
						System.out.format(" corXminusx=%.4f  corX=%.4f  difference=%.4f : ", backwardJ[0], backwardJ[1],
								improvement);
						System.out.println("Least significant feature to remove: " + features[ls]);

						// is this the best (k-1)-subset so far?
						// if corXminusx > corX
						// if the improvement is greater than 0.001 then keep the value
						if ((backwardJ[0] > backwardJ[1]) || (improvement < 0.0001)) { // J(X_k - x_s) <= J(X_k-1)
							// exclude xs from X'_k and set k = k-1
							System.out
									.println(" better without least significant feature or improvement < 0.0001 : (removing feature)");
							X = MathUtils.removeIndex(X, ls);
							k = k - 1;
							corX = backwardJ[0];
							condSBS = true;
						} else {
							System.out.println(" better with least significant feature (keeping feature)\n");
							condSBS = false;
						}
					} else {
						System.out.println("X has one feature, can not execute a SBS step");
						condSBS = false;
					}
				} // while SBG
				System.out.format("k=%d corX=%.4f   ", k, corX);
				printSelectedFeatures(X, features);
				System.out.println("-------------------------\n");
			} else { // so X.length == 0
				System.out.println("No more elements in Y for selection");
				condSFS = false;
			}
		} // while SFG
			// return the set of selected features

		// get the final equation coefficients
		Regression reg = new Regression();
		reg.multipleLinearRegression(dataFile, indVariable, X, features, interceptTerm, rowIni, rowEnd);

		// copy the coefficients and selected factors in SoP
		sop.setCoeffsAndFactors(reg.getCoeffs(), X, features, interceptTerm);
		sop.setCorrelation(reg.getCorrelation());
		sop.setRMSE(reg.getRMSE());

		return X;
	}

	/**
	 * Find the f feature in Y that maximise J(X+y)
	 * 
	 * @param dataFile
	 * @param features
	 * @return the index of Y that maximises J(X+y)
	 */
	private int sequentialForwardSelection(String dataFile, String[] features, int indVarColNumber, int X[], int Y[], double J[],
			int rowIni, int rowEnd) {
		double sig[] = new double[Y.length];
		int sigIndex[] = new int[Y.length]; // to keep track of the corresponding feature
		double corXplusy[] = new double[Y.length];

		// get J(X_k)
		double corX;
		if (X.length > 0) {
			Regression reg = new Regression();
			reg.multipleLinearRegression(dataFile, indVarColNumber, X, features, interceptTerm, rowIni, rowEnd);
			corX = reg.getCorrelation();
			// System.out.println("corX=" + corX);
		} else
			corX = 0.0;

		// Calculate the significance of a new feature y_j (y_j is not included in X)
		// S_k+1(y_j) = J(X_k + y_j) - J(X_k)
		for (int i = 0; i < Y.length; i++) {
			// get J(X_k + y_j)
			corXplusy[i] = correlationOfNewFeature(dataFile, features, indVarColNumber, X, Y[i], rowIni, rowEnd);
			sig[i] = corXplusy[i] - corX;
			sigIndex[i] = Y[i];
			// System.out.println("Significance of new feature[" + sigIndex[i] + "]: " + features[sigIndex[i]] + " = " + sig[i]);
		}
		// find min
		int minSig = MathUtils.getMinIndex(sig);
		J[0] = corXplusy[minSig];
		// J(X_k) = corX
		J[1] = corX;
		// find max
		int maxSig = MathUtils.getMaxIndex(sig);
		J[2] = corXplusy[maxSig];

		return sigIndex[maxSig];

	}

	/**
	 * Find the x feature in X that minimise J(X-x), find the least significant feature in X.
	 * 
	 * @param dataFile
	 * @param features
	 * @return the x (index) that minimises J(X-x)
	 */
	private int sequentialBackwardSelection(String dataFile, String[] features, int indVarColNumber, int X[], double J[],
			int rowIni, int rowEnd) {
		double sig[] = new double[X.length];
		double corXminusx[] = new double[X.length];
		int sigIndex[] = new int[X.length]; // to keep track of the corresponding feature

		// get J(X_k)
		double corX;
		if (X.length > 0) {
			Regression reg = new Regression();
			reg.multipleLinearRegression(dataFile, indVarColNumber, X, features, interceptTerm, rowIni, rowEnd);
			// reg.printCoefficients(X, features);
			corX = reg.getCorrelation();
			// System.out.println("corX=" + corX);
		} else
			corX = 0.0;

		// Calculate the significance a feature x_j (included in X)
		// S_k-1(x_j) = J(X_k) - J(X_k - x_i)
		for (int i = 0; i < X.length; i++) {
			// get J(X_k - x_i)
			corXminusx[i] = correlationOfFeature(dataFile, features, indVarColNumber, X, X[i], rowIni, rowEnd);
			sig[i] = corX - corXminusx[i];
			sigIndex[i] = X[i];
			// System.out.println("Significance of current feature[" + sigIndex[i] + "]: " + features[sigIndex[i]] + " = " +
			// sig[i]);
		}
		// find min
		int minSig = MathUtils.getMinIndex(sig);
		J[0] = corXminusx[minSig];
		// J(X_k) = corX
		J[1] = corX;
		// find max
		int maxSig = MathUtils.getMaxIndex(sig);
		J[2] = corXminusx[maxSig];

		return sigIndex[minSig];
	}

	/**
	 * Correlation of X minus a feature x which is part of the set X: J(X_k - x_i)
	 * 
	 * @param dataFile
	 *            one column per feature
	 * @param features
	 *            string array with the list of feature names
	 * @param indVarColNumber
	 *            number of the column that corresponds to the independent variable
	 * @param X
	 *            set of current feature indexes
	 * @param x
	 *            one feature index in X
	 * @return corXminusx
	 */
	private double correlationOfFeature(String dataFile, String[] features, int indVarColNumber, int[] X, int x, int rowIni,
			int rowEnd) {

		double corXminusx;
		Regression reg = new Regression();

		// get J(X_k - x_i)
		// we need to remove the index x from X
		int j = 0;
		int[] Xminusx = new int[X.length - 1];
		for (int i = 0; i < X.length; i++)
			if (X[i] != x)
				Xminusx[j++] = X[i];
		reg.multipleLinearRegression(dataFile, indVarColNumber, Xminusx, features, interceptTerm, rowIni, rowEnd);
		// reg.printCoefficients(Xminusx, features);
		corXminusx = reg.getCorrelation();
		// System.out.println("corXminusx=" + corXminusx);
		// System.out.println("significance of x[" + x + "]: " + features[x] + " = " + (corX-corXminusx));

		return corXminusx;

	}

	/**
	 * Correlation of X plus the new feature y (y is not included in X): J(X_k + y_j)
	 * 
	 * @param dataFile
	 *            one column per feature
	 * @param features
	 *            string array with the list of feature names
	 * @param indVarColNumber
	 *            number of the column that corresponds to the independent variable
	 * @param X
	 *            set of current feature indexes
	 * @param y
	 *            a feature index that is not in X, new feature
	 * @return corXplusy
	 */
	private double correlationOfNewFeature(String dataFile, String[] features, int indVarColNumber, int[] X, int y, int rowIni,
			int rowEnd) {

		double corXplusy;
		Regression reg = new Regression();

		// get J(X_k + y_j)
		// we need to add the index y to X
		int j = 0;
		int[] Xplusf = new int[X.length + 1];
		for (int i = 0; i < X.length; i++)
			Xplusf[i] = X[i];
		Xplusf[X.length] = y;

		reg.multipleLinearRegression(dataFile, indVarColNumber, Xplusf, features, interceptTerm, rowIni, rowEnd);
		// reg.printCoefficients(Xplusf, features);
		corXplusy = reg.getCorrelation();
		// System.out.println("corXplusf=" + corXplusy);
		// System.out.println("significance of x[" + f + "]: " + features[f] + " = " + (corXplusf-corX));

		return corXplusy;

	}

	static private void printSelectedFeatures(int X[], String[] features) {
		System.out.print("Features: ");
		for (int i = 0; i < X.length; i++)
			System.out.print(features[X[i]] + "  ");
		System.out.println();
	}

	static private void printSelectedFeatures(int X[], String[] features, PrintWriter file) {
		file.print("Features: ");
		for (int i = 0; i < X.length; i++)
			file.print(features[X[i]] + "  ");
		file.println();
	}

	// remove the columns with mean = 0.0
	private int[] checkMeanColumns(String dataFile, int Y[], String[] features) {
		try {
			BufferedReader reader = new BufferedReader(new FileReader(dataFile));
			Matrix data = Matrix.read(reader);
			reader.close();
			data = data.transpose(); // then I have easy access to the columns
			int rows = data.getRowDimension() - 1;
			int cols = data.getColumnDimension() - 1;

			data = data.getMatrix(0, rows, 1, cols); // dataVowels(:,1:cols) -> dependent variables
			int M = data.getRowDimension();
			double mn;
			for (int i = 0; i < M; i++) {
				mn = MathUtils.mean(data.getArray()[i]);
				if (mn == 0.0) {
					System.out.println("Removing feature: " + features[i] + " from list of features because it has mean=0.0");
					Y = MathUtils.removeIndex(Y, i);
				}
			}
		} catch (Exception e) {
			throw new RuntimeException("Problem reading file " + dataFile, e);
		}
		System.out.println();
		return Y;
	}

}
