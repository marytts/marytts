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
package marytts.machinelearning;

import java.io.IOException;

import marytts.util.MaryUtils;
import marytts.util.io.FileUtils;
import marytts.util.io.MaryRandomAccessFile;
import marytts.util.math.DoubleMatrix;
import marytts.util.math.MathUtils;
import marytts.util.string.StringUtils;

/**
 * 
 * Expectation-Maximization (EM) based GMM training
 * 
 * Reference: A. P. Dempster, N. M. Laird, and D. B. Rubin. Maximum likelihood from in- complete data via the em algorithm.
 * Journal of the Royal Statistical Society: Series B, 39(1):1–38, November 1977.
 * 
 * @author Oytun T&uuml;rk
 */
public class GMMTrainer {

	public double[] logLikelihoods;

	public GMMTrainer() {
		logLikelihoods = null;
	}

	// This function calls the Expectation-Maximization (EM) algorithm
	// to fit a Gaussian Mixture Model (GMM) to multi-dimensional data in x.
	// Each row of x, i.e. x[0], x[1], ... corresponds to an observation vector.
	// The dimension of each vector should be identical.
	// Either a java implementation or a native C implementation (in Windows OS only) can be used.
	// Note that native C implementation (GMMTrainer.exe) works 5 to 10 times faster.
	// All training parameters are given by gmmParams (See GMMTrainerParams.java for details)
	// Training consists of two steps:
	// (a) Initialization using K-Means clustering
	// (b) EM iterations to increase total log-likelihood of the model given the data
	public GMM train(double[][] x, GMMTrainerParams gmmParams) {
		long startTime, endTime;

		/*
		 * //For testing Java and native C versions with identical data String dataFile0 = "d:/gmmTester2.dat"; DoubleData d0 =
		 * null; if (FileUtils.exists(dataFile0)) { d0 = new DoubleData(dataFile0); x = new double[d0.numVectors][]; for (int i=0;
		 * i<d0.numVectors; i++) { x[i] = new double[d0.dimension]; System.arraycopy(d0.vectors[i], 0, x[i], 0, d0.dimension); } }
		 * else { d0 = new DoubleData(x); d0.write(dataFile0); } //
		 */

		startTime = System.currentTimeMillis();

		GMM gmm = null;
		if (x != null && gmmParams.totalComponents > 0) {
			if (!MaryUtils.isWindows())
				gmmParams.useNativeCLibTrainer = false;

			if (!gmmParams.useNativeCLibTrainer) // Java based training
			{
				int featureDimension = x[0].length;
				int i;
				for (i = 1; i < x.length; i++)
					assert x[i].length == featureDimension;

				// Initialize components with KMeans clustering
				KMeansClusteringTrainerParams kmeansParams = new KMeansClusteringTrainerParams(gmmParams);
				KMeansClusteringTrainer kmeansClusterer = new KMeansClusteringTrainer();
				kmeansClusterer.train(x, kmeansParams);

				// Create initial GMM according to KMeans clustering results
				GMM initialGmm = new GMM(kmeansClusterer);

				// Update model parameters with Expectation-Maximization
				gmm = expectationMaximization(x, initialGmm, gmmParams.emMinIterations, gmmParams.emMaxIterations,
						gmmParams.isUpdateCovariances, gmmParams.tinyLogLikelihoodChangePercent, gmmParams.minCovarianceAllowed);
			} else // native C library based training (only available for Windows OS)
			{
				String strIsBigEndian = "1";
				String dataFile = StringUtils.getRandomFileName("d:/gmmTemp_", 8, ".dat");
				DoubleMatrix d = new DoubleMatrix(x);
				d.write(dataFile);

				String gmmFile = StringUtils.modifyExtension(dataFile, ".gmm");
				String logFile = StringUtils.modifyExtension(dataFile, ".log");
				String strCommand = "GMMTrainer.exe " + "\"" + dataFile + "\" " + "\"" + gmmFile + "\" "
						+ String.valueOf(gmmParams.totalComponents) + " " + strIsBigEndian + " "
						+ String.valueOf(gmmParams.isDiagonalCovariance ? 1 : 0) + " "
						+ String.valueOf(gmmParams.kmeansMaxIterations) + " "
						+ String.valueOf(gmmParams.kmeansMinClusterChangePercent) + " "
						+ String.valueOf(gmmParams.kmeansMinSamplesInOneCluster) + " "
						+ String.valueOf(gmmParams.emMinIterations) + " " + String.valueOf(gmmParams.emMaxIterations) + " "
						+ String.valueOf(gmmParams.isUpdateCovariances ? 1 : 0) + " "
						+ String.valueOf(gmmParams.tinyLogLikelihoodChangePercent) + " "
						+ String.valueOf(gmmParams.minCovarianceAllowed) + " " + "\"" + logFile + "\"";

				int exitVal = MaryUtils.shellExecute(strCommand, true);

				if (exitVal == 0) {
					System.out.println("GMM training with native C library done...");
					gmm = new GMM(gmmFile);
					FileUtils.delete(gmmFile);
				} else
					System.out.println("Error executing native C library with exit code " + exitVal);

				FileUtils.delete(dataFile);
			}
		}

		endTime = System.currentTimeMillis();
		System.out.println("GMM training took " + String.valueOf((endTime - startTime) / 1000.0) + " seconds...");

		return gmm;
	}

	/*
	 * EM algorithm to fit a GMM to multi-dimensional data x: Data matrix (Each row is another observation vector) initialGMM:
	 * Initial GMM model (can be initialized using K-Means clustering (See function train) emMinimumIterations: Minimum number of
	 * EM iterations for which the algorithm will not quit even when the total likelihood does not change much with additional
	 * iterations) emMaximumIterations: Maximum number of EM iterations for which the algorithm will quit even when total
	 * likelihood has not settled yet isUpdateCovariances: Update covariance matrices in EM iterations?
	 * tinyLogLikelihoodChangePercent: Threshold to compare percent decrease in total log-likelihood to stop iterations
	 * automatically minimumCovarianceAllowed: Minimum covariance value allowed - should be a small positive number to avoid
	 * ill-conditioned training
	 * 
	 * Reference: A. P. Dempster, N. M. Laird, and D. B. Rubin. Maximum likelihood from incomplete data via the em algorithm.
	 * Journal of the Royal Statistical Society: Series B, 39(1):1–38, November 1977.
	 * 
	 * Many practical tutorials for EM training of GMMs exist on the web, i.e.:
	 * http://bengio.abracadoudou.com/lectures/old/tex_gmm.pdf
	 */
	public GMM expectationMaximization(double[][] x, GMM initialGmm, int emMinimumIterations, int emMaximumIterations,
			boolean isUpdateCovariances, double tinyLogLikelihoodChangePercent, double minimumCovarianceAllowed) {
		int i, j, k;
		int totalObservations = x.length;

		GMM gmm = new GMM(initialGmm);

		for (i = 0; i < totalObservations; i++)
			assert x[i].length == gmm.featureDimension;

		int numIterations = 1;

		double error = 0.0;
		double prevErr;

		for (k = 0; k < gmm.totalComponents; k++)
			gmm.weights[k] = 1.0f / gmm.totalComponents;

		boolean bContinue = true;

		double[] zDenum = new double[totalObservations];
		double P_xj_tetak;

		double[][] zNum = new double[totalObservations][gmm.totalComponents];
		double[][] z = new double[totalObservations][gmm.totalComponents];

		double[] num1 = new double[gmm.featureDimension];
		double[] tmpMean = new double[gmm.featureDimension];

		double[][] num2 = new double[gmm.featureDimension][gmm.featureDimension];

		double tmpSum;
		double mean_diff;
		double denum;
		double diffk;
		double tmpZeroMean;
		int d1, d2;
		logLikelihoods = new double[emMaximumIterations];

		long start, end;
		start = end = 0;

		// Main EM iteartions loop
		while (bContinue) {
			start = System.currentTimeMillis();
			// Expectation step
			// Find zjk's at time (s+1) using alphak's at time (s)
			for (j = 0; j < totalObservations; j++) {
				zDenum[j] = 0.0f;
				for (k = 0; k < gmm.totalComponents; k++) {
					// P(xj|teta_k)
					if (gmm.isDiagonalCovariance)
						P_xj_tetak = MathUtils.getGaussianPdfValue(x[j], gmm.components[k].meanVector,
								gmm.components[k].getCovMatrixDiagonal(), gmm.components[k].getConstantTerm());
					else
						P_xj_tetak = MathUtils.getGaussianPdfValue(x[j], gmm.components[k].meanVector,
								gmm.components[k].getInvCovMatrix(), gmm.components[k].getConstantTerm());

					/*
					 * if (P_xj_tetak<MathUtils.TINY_PROBABILITY) P_xj_tetak=MathUtils.TINY_PROBABILITY;
					 */

					zNum[j][k] = gmm.weights[k] * P_xj_tetak;
					zDenum[j] = zDenum[j] + zNum[j][k];
				}
			}

			// Find zjk's at time (s+1)
			for (j = 0; j < totalObservations; j++) {
				for (k = 0; k < gmm.totalComponents; k++)
					z[j][k] = zNum[j][k] / zDenum[j];
			}

			// Now update alphak's to find their values at time (s+1)
			for (k = 0; k < gmm.totalComponents; k++) {
				tmpSum = 0.0;
				for (j = 0; j < totalObservations; j++)
					tmpSum += z[j][k];

				gmm.weights[k] = tmpSum / totalObservations;
			}

			// Maximization step
			// Find the model parameters at time (s+1) using zjk's at time (s+1)
			mean_diff = 0.0;
			for (k = 0; k < gmm.totalComponents; k++) {
				for (d1 = 0; d1 < gmm.featureDimension; d1++) {
					num1[d1] = 0.0f;
					for (d2 = 0; d2 < gmm.featureDimension; d2++)
						num2[d1][d2] = 0.0f;
				}

				denum = 0.0;

				for (j = 0; j < totalObservations; j++) {
					denum += z[j][k];

					for (d1 = 0; d1 < gmm.featureDimension; d1++) {
						num1[d1] += x[j][d1] * z[j][k];

						tmpZeroMean = x[j][d1] - gmm.components[k].meanVector[d1];

						for (d2 = 0; d2 < gmm.featureDimension; d2++)
							num2[d1][d2] += z[j][k] * tmpZeroMean * (x[j][d2] - gmm.components[k].meanVector[d2]);
					}
				}

				for (d1 = 0; d1 < gmm.featureDimension; d1++)
					tmpMean[d1] = num1[d1] / denum;

				diffk = 0.0f;
				for (d1 = 0; d1 < gmm.featureDimension; d1++) {
					tmpZeroMean = tmpMean[d1] - gmm.components[k].meanVector[d1];
					diffk += tmpZeroMean * tmpZeroMean;
				}
				diffk = Math.sqrt(diffk);
				mean_diff += diffk;

				for (d1 = 0; d1 < gmm.featureDimension; d1++)
					gmm.components[k].meanVector[d1] = tmpMean[d1];

				if (isUpdateCovariances) {
					if (gmm.isDiagonalCovariance) {
						for (d1 = 0; d1 < gmm.featureDimension; d1++)
							gmm.components[k].covMatrix[0][d1] = Math.max(num2[d1][d1] / denum, minimumCovarianceAllowed);
					} else {
						for (d1 = 0; d1 < gmm.featureDimension; d1++) {
							for (d2 = 0; d2 < gmm.featureDimension; d2++)
								gmm.components[k].covMatrix[d1][d2] = Math.max(num2[d1][d2] / denum, minimumCovarianceAllowed);
						}
					}

					gmm.components[k].setDerivedValues();
				}
			}

			if (numIterations == 1)
				error = mean_diff;
			else {
				prevErr = error;
				error = mean_diff;
			}

			logLikelihoods[numIterations - 1] = 0.0;
			if (gmm.isDiagonalCovariance) {
				for (j = 0; j < totalObservations; j++) {
					double tmp = 0.0;
					for (k = 0; k < gmm.totalComponents; k++) {
						P_xj_tetak = MathUtils.getGaussianPdfValue(x[j], gmm.components[k].meanVector,
								gmm.components[k].getCovMatrixDiagonal(), gmm.components[k].getConstantTerm());

						/*
						 * if (P_xj_tetak<MathUtils.TINY_PROBABILITY) P_xj_tetak=MathUtils.TINY_PROBABILITY;
						 */

						tmp += gmm.weights[k] * P_xj_tetak;
					}

					logLikelihoods[numIterations - 1] += Math.log(tmp);
				}
			} else {
				for (j = 0; j < totalObservations; j++) {
					double tmp = 0.0;
					for (k = 0; k < gmm.totalComponents; k++) {
						P_xj_tetak = MathUtils.getGaussianPdfValue(x[j], gmm.components[k].meanVector,
								gmm.components[k].getInvCovMatrix(), gmm.components[k].getConstantTerm());

						/*
						 * if (P_xj_tetak<MathUtils.TINY_PROBABILITY) P_xj_tetak=MathUtils.TINY_PROBABILITY;
						 */

						tmp += gmm.weights[k] * P_xj_tetak;
					}

					logLikelihoods[numIterations - 1] += Math.log(tmp);
				}
			}

			end = System.currentTimeMillis();

			System.out.println("For " + String.valueOf(gmm.totalComponents) + " mixes - EM iteration no: "
					+ String.valueOf(numIterations) + " with avg. difference in means " + String.valueOf(error)
					+ " log-likelihood=" + String.valueOf(logLikelihoods[numIterations - 1]) + " in "
					+ String.valueOf((end - start) / 1000.0) + " sec");

			// Force iterations to stop if maximum number of iterations has been reached
			if (numIterations + 1 > emMaximumIterations)
				break;

			// Force iterations to stop if minimum number of iterations has been reached AND total log likelihood does not change
			// much
			if (numIterations > emMinimumIterations
					&& logLikelihoods[numIterations - 1] - logLikelihoods[numIterations - 2] < Math
							.abs(logLikelihoods[numIterations - 1] / 100 * tinyLogLikelihoodChangePercent))
				break;

			numIterations++;
		}

		double[] tmpLogLikelihoods = new double[numIterations - 1];
		System.arraycopy(logLikelihoods, 0, tmpLogLikelihoods, 0, numIterations - 1);
		logLikelihoods = new double[numIterations - 1];
		System.arraycopy(tmpLogLikelihoods, 0, logLikelihoods, 0, numIterations - 1);

		System.out.println("GMM training completed...");

		return gmm;
	}

	public static void testEndianFileIO() throws IOException {
		boolean b1 = true;
		char c1 = 'c';
		short s1 = 111;
		int i1 = 222;
		double d1 = 33.3;
		float f1 = 44.4f;
		long l1 = 555;

		String javaFile = "d:/endianJava.tmp";
		MaryRandomAccessFile fp = new MaryRandomAccessFile(javaFile, "rw");
		if (fp != null) {
			fp.writeBooleanEndian(b1);
			fp.writeCharEndian(c1);
			fp.writeShortEndian(s1);
			fp.writeIntEndian(i1);
			fp.writeDoubleEndian(d1);
			fp.writeFloatEndian(f1);
			fp.writeLongEndian(l1);

			fp.close();
		}

		boolean b2;
		char c2;
		short s2;
		int i2;
		double d2;
		float f2;
		long l2;

		String cFile = "d:/endianC.tmp";

		if (FileUtils.exists(cFile)) {
			MaryRandomAccessFile fp2 = new MaryRandomAccessFile(cFile, "r");
			if (fp2 != null) {
				b2 = fp2.readBooleanEndian();
				c2 = fp2.readCharEndian();
				s2 = fp2.readShortEndian();
				i2 = fp2.readIntEndian();
				d2 = fp2.readDoubleEndian();
				f2 = fp2.readFloatEndian();
				l2 = fp2.readLongEndian();

				fp2.close();

				if (b1 != b2)
					System.out.println("Error in bool!\n");
				if (c1 != c2)
					System.out.println("Error in char!\n");
				if (s1 != s2)
					System.out.println("Error in short!\n");
				if (i1 != i2)
					System.out.println("Error in int!\n");
				if (d1 != d2)
					System.out.println("Error in double!\n");
				if (f1 != f2)
					System.out.println("Error in float!\n");
				if (l1 != l2)
					System.out.println("Error in long!\n");
			} else
				System.out.println("C generated file cannot be opened...\n");
		} else
			System.out.println("C generated file not found...\n");
	}

	public static void main(String[] args) {
		int numClusters = 20;
		int numSamplesInClusters = 2000;
		double[] variances = { 0.01 };
		int vectorDim = 10;
		ClusteredDataGenerator[] c = new ClusteredDataGenerator[vectorDim];
		int i, j, n;
		int totalVectors = 0;
		for (i = 0; i < vectorDim; i++) {
			if (i < variances.length)
				c[i] = new ClusteredDataGenerator(numClusters, numSamplesInClusters, 10.0 * (i + 1), variances[i]);
			else
				c[i] = new ClusteredDataGenerator(numClusters, numSamplesInClusters, 10.0 * (i + 1), variances[0]);
		}

		totalVectors = c[0].data.length;

		double[][] x = new double[totalVectors][vectorDim];
		int counter = 0;
		for (n = 0; n < c.length; n++) {
			for (i = 0; i < c[n].data.length; i++)
				x[i][n] = c[n].data[i];
		}

		x = MathUtils.randomSort(x);

		double[] m = MathUtils.mean(x);
		double[] v = MathUtils.variance(x, m);
		System.out.println(String.valueOf(m[0]) + " " + String.valueOf(v[0]));

		GMMTrainerParams gmmParams = new GMMTrainerParams();
		gmmParams.totalComponents = numClusters;
		gmmParams.isDiagonalCovariance = true;
		gmmParams.kmeansMaxIterations = 100;
		gmmParams.kmeansMinClusterChangePercent = 0.01;
		gmmParams.kmeansMinSamplesInOneCluster = 10;
		gmmParams.emMinIterations = 100;
		gmmParams.emMaxIterations = 2000;
		gmmParams.isUpdateCovariances = true;
		gmmParams.tinyLogLikelihoodChangePercent = 0.001;
		gmmParams.minCovarianceAllowed = 1e-5;
		gmmParams.useNativeCLibTrainer = true;

		GMMTrainer g = new GMMTrainer();
		GMM gmm = g.train(x, gmmParams);

		if (gmm != null) {
			for (i = 0; i < gmm.totalComponents; i++)
				System.out.println("Gaussian #" + String.valueOf(i + 1) + " mean="
						+ String.valueOf(gmm.components[i].meanVector[0]) + " variance="
						+ String.valueOf(gmm.components[i].covMatrix[0][0]) + " prior=" + gmm.weights[i]);
		}

		/*
		 * try { testEndianFileIO(); } catch (IOException e) { // TODO Auto-generated catch block e.printStackTrace(); }
		 */
	}
}
