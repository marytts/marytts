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

import marytts.signalproc.analysis.distance.DistanceComputer;
import marytts.util.math.MathUtils;

/**
 * K-Means clustering training algorithm
 * 
 * Reference: J. MacQueen, 1967, "Some methods for classification and analysis of multivariate observations", Proc. Fifth Berkeley
 * Symp. on Math. Statist. and Prob., Vol. 1 (Univ. of Calif. Press, 1967), pp. 281-297.
 * 
 * @author Oytun T&uuml;rk
 */
public class KMeansClusteringTrainer {
	public Cluster[] clusters; // Parameters of each cluster
	public int[] totalObservationsInClusters; // Total number of observations in each cluster
	public int[] clusterIndices; // Assigned cluster for each observation vector
	public double[][] covMatrixGlobal; // Global covariance matrix of data
	public double[][] invCovMatrixGlobal; // Inverse of global covariance matrix of data

	// This function clusters multi-dimensional feature vectors using K-Means clustering procedure
	// Each row of x, i.e. x[0], x[1], ... corresponds to an observation vector.
	// The dimension of each vector should be identical.
	// All training parameters are given by kmeansParams (See KMeansClusteringTrainerParams.java for details)
	// Training consists of four steps:
	// (a) Initialization (random assignment of cluster means using data points that are far away from each other + slight random
	// shifts)
	// (b) Hard clustering of samples according to new cluster means
	// (c) Update of cluster means using assigned samples
	// (d) Re-iteration of (b) and (c) until convergence, i.e. when overall cluster occupancy does not change much
	public void train(double[][] x, KMeansClusteringTrainerParams kmeansParams) {
		if (kmeansParams.globalVariances == null) {
			double[] meanVector = MathUtils.mean(x, true);
			kmeansParams.globalVariances = MathUtils.variance(x, meanVector, true);
		}

		int observations = x.length;
		int dimension = x[0].length;

		int c, k, k2, d, t, iter, i, j, totChanged;
		int ind = -1;
		boolean bCont;
		double rnd, tmpDist;
		double minDist = Double.MIN_VALUE;

		double[][] m_new = new double[kmeansParams.numClusters][];
		for (k = 0; k < kmeansParams.numClusters; k++)
			m_new[k] = new double[dimension];

		int[][] b = new int[observations][];
		for (t = 0; t < observations; t++)
			b[t] = new int[kmeansParams.numClusters];

		int[][] b_old = new int[observations][];
		for (t = 0; t < observations; t++)
			b_old[t] = new int[kmeansParams.numClusters];

		int[] prev_totals = new int[kmeansParams.numClusters];
		double changedPerc;

		double[] mAll = new double[dimension];

		clusters = new Cluster[kmeansParams.numClusters];
		for (k = 0; k < kmeansParams.numClusters; k++)
			clusters[k] = new Cluster(dimension, kmeansParams.isDiagonalOutputCovariance);

		for (k = 1; k <= kmeansParams.numClusters; k++) {
			for (d = 1; d <= dimension; d++)
				clusters[k - 1].meanVector[d - 1] = 0.0;

			for (t = 1; t <= observations; t++)
				b[t - 1][k - 1] = 0;
		}

		// Select initial cluster centers
		mAll = MathUtils.mean(x, true);

		k = 1;
		double[] dists = new double[observations];
		double[] tmp = new double[kmeansParams.numClusters + 1];
		double maxD = Double.MAX_VALUE;
		int maxInd = -1;

		while (k <= kmeansParams.numClusters) {
			for (t = 1; t <= observations; t++) {
				if (k > 1) {
					for (i = 1; i <= k - 1; i++)
						tmp[i - 1] = DistanceComputer.getNormalizedEuclideanDistance(clusters[i - 1].meanVector, x[t - 1],
								kmeansParams.globalVariances);

					tmp[k - 1] = DistanceComputer.getNormalizedEuclideanDistance(mAll, x[t - 1], kmeansParams.globalVariances);
					dists[t - 1] = MathUtils.mean(tmp, 0, k - 1);
				} else {
					dists[t - 1] = DistanceComputer.getNormalizedEuclideanDistance(mAll, x[t - 1], kmeansParams.globalVariances);
				}
			}

			for (t = 1; t <= observations; t++) {
				if (t == 1 || dists[t - 1] > maxD) {
					maxD = dists[t - 1];
					maxInd = t;
				}
			}

			for (d = 0; d < dimension; d++)
				clusters[k - 1].meanVector[d] = x[maxInd - 1][d];

			// System.out.println("Cluster center " + String.valueOf(k) + " initialized...");
			k++;
		}
		//

		int[] tinyClusterInds = new int[kmeansParams.numClusters];
		int numTinyClusters = 0;
		double[] tmps = new double[kmeansParams.numClusters];
		int[] inds;
		totalObservationsInClusters = new int[kmeansParams.numClusters];
		clusterIndices = new int[observations];

		iter = 0;
		bCont = true;
		while (bCont) {
			for (t = 1; t <= observations; t++) // Overall observations
			{
				for (i = 1; i <= kmeansParams.numClusters; i++) // Overall classes
				{
					tmpDist = DistanceComputer.getNormalizedEuclideanDistance(clusters[i - 1].meanVector, x[t - 1],
							kmeansParams.globalVariances);
					b[t - 1][i - 1] = 0;
					if (i == 1 || tmpDist < minDist) {
						minDist = tmpDist;
						ind = i;
					}
				}
				for (i = 1; i <= kmeansParams.numClusters; i++) // Overall classes
				{
					if (i == ind)
						b[t - 1][i - 1] = 1;
				}
			}

			// Update means
			for (i = 1; i <= kmeansParams.numClusters; i++) {
				totalObservationsInClusters[i - 1] = 0;
				tinyClusterInds[i - 1] = 0;
			}

			c = 1;
			for (i = 1; i <= kmeansParams.numClusters; i++) {
				for (d = 1; d <= dimension; d++)
					m_new[i - 1][d - 1] = 0.0f;

				for (t = 1; t <= observations; t++) {
					if (b[t - 1][i - 1] == 1) {
						for (d = 1; d <= dimension; d++)
							m_new[i - 1][d - 1] = m_new[i - 1][d - 1] + x[t - 1][d - 1];

						clusterIndices[t - 1] = i - 1; // zero-based
						(totalObservationsInClusters[i - 1])++;
					}
				}

				// Do something if totalObservationsInClusters[i-1] is less than some value
				// (i.e. there are too few observations for the cluster)
				if ((double) totalObservationsInClusters[i - 1] < kmeansParams.minSamplesInOneCluster) {
					tinyClusterInds[c - 1] = i;
					numTinyClusters++;
					c++;
				}
			}
			//

			c = 0;
			for (i = 0; i < totalObservationsInClusters.length; i++)
				tmps[i] = totalObservationsInClusters[i];

			inds = MathUtils.quickSort(tmps, 0, kmeansParams.numClusters - 1);
			for (i = 1; i <= kmeansParams.numClusters; i++) {
				if (totalObservationsInClusters[i - 1] >= kmeansParams.minSamplesInOneCluster) {
					for (d = 1; d <= dimension; d++)
						clusters[i - 1].meanVector[d - 1] = m_new[i - 1][d - 1] / totalObservationsInClusters[i - 1];
				} else {
					for (d = 1; d <= dimension; d++) {
						rnd = Math.random() * Math.abs(clusters[inds[kmeansParams.numClusters - c - 1]].meanVector[d - 1]) * 0.01;
						clusters[i - 1].meanVector[d - 1] = clusters[inds[kmeansParams.numClusters - c - 1]].meanVector[d - 1]
								+ rnd;
					}
					c++;
				}
			}

			for (i = 1; i <= kmeansParams.numClusters; i++)
				prev_totals[i - 1] = totalObservationsInClusters[i - 1];

			iter++;
			totChanged = 0;
			if (iter > 1) {
				if (iter >= kmeansParams.maxIterations)
					bCont = false;

				for (t = 1; t <= observations; t++) {
					for (i = 1; i <= kmeansParams.numClusters; i++) {
						if (b_old[t - 1][i - 1] != b[t - 1][i - 1]) {
							totChanged++;
							break; // Count each difference once
						}
					}
				}

				changedPerc = (double) totChanged / observations * 100.0;
				if (changedPerc < kmeansParams.minClusterChangePercent) // stop if number of clusters changed is less than
																		// %MIN_CHANGE_PERCENT of total observation
					bCont = false;

				// System.out.println("K-Means iteration: " + String.valueOf(iter) + " with " + String.valueOf(changedPerc) +
				// " percent of cluster assignments updated");
			}
			// else
			// System.out.println("K-Means iteration: " + String.valueOf(iter) + " K-means initialized");

			for (t = 1; t <= observations; t++) {
				for (k2 = 1; k2 <= kmeansParams.numClusters; k2++)
					b_old[t - 1][k2 - 1] = b[t - 1][k2 - 1];
			}
		}

		// Finally, calculate the cluster covariances
		double[][] tmpCov = null;
		double[] diag = null;
		int d1, d2;
		for (i = 0; i < kmeansParams.numClusters; i++) {
			if (totalObservationsInClusters[i] > 0) {
				int[] indices = new int[totalObservationsInClusters[i]];
				int count = 0;
				for (t = 0; t < observations; t++) {
					if (clusterIndices[t] == i)
						indices[count++] = t;
				}

				if (kmeansParams.isDiagonalOutputCovariance) {
					tmpCov = MathUtils.covariance(x, clusters[i].meanVector, true, indices);
					diag = MathUtils.diagonal(tmpCov);
					for (d1 = 0; d1 < diag.length; d1++)
						diag[d1] = Math.max(diag[d1], kmeansParams.minCovarianceAllowed);
					System.arraycopy(diag, 0, clusters[i].covMatrix[0], 0, diag.length);
					clusters[i].invCovMatrix[0] = MathUtils.inverse(clusters[i].covMatrix[0]);
				} else {
					clusters[i].covMatrix = MathUtils.covariance(x, clusters[i].meanVector, true, indices);
					for (d1 = 0; d1 < clusters[i].covMatrix.length; d1++) {
						for (d2 = 0; d2 < clusters[i].covMatrix[d1].length; d2++)
							clusters[i].covMatrix[d1][d2] = Math.max(clusters[i].covMatrix[d1][d2],
									kmeansParams.minCovarianceAllowed);
					}

					clusters[i].invCovMatrix = MathUtils.inverse(clusters[i].covMatrix);
				}
			}
		}

		// There can be no observations for some clusters, i.e. when the number of clusters is large as compared to the actual
		// clusters in data
		// In this case, assign largest cluster´s mean, covariance, and inverse covariance to these empty clusters
		for (i = 0; i < kmeansParams.numClusters; i++)
			tmps[i] = totalObservationsInClusters[i];

		inds = MathUtils.quickSort(tmps, 0, kmeansParams.numClusters - 1);
		int largestClusterInd = inds[kmeansParams.numClusters - 1];
		for (i = 0; i < kmeansParams.numClusters; i++) {
			if (totalObservationsInClusters[i] < kmeansParams.minSamplesInOneCluster) {
				System.arraycopy(clusters[largestClusterInd].meanVector, 0, clusters[i].meanVector, 0, dimension);
				if (kmeansParams.isDiagonalOutputCovariance) {
					System.arraycopy(clusters[largestClusterInd].covMatrix[0], 0, clusters[i].covMatrix[0], 0, dimension);
					System.arraycopy(clusters[largestClusterInd].invCovMatrix[0], 0, clusters[i].invCovMatrix[0], 0, dimension);
				} else {
					for (j = 0; j < dimension; j++) {
						System.arraycopy(clusters[largestClusterInd].covMatrix[j], 0, clusters[i].covMatrix[j], 0, dimension);
						System.arraycopy(clusters[largestClusterInd].invCovMatrix[j], 0, clusters[i].invCovMatrix[j], 0,
								dimension);
					}
				}
			}
		}
		//

		if (kmeansParams.isDiagonalOutputCovariance) {
			tmpCov = MathUtils.covariance(x, true);
			covMatrixGlobal = new double[1][tmpCov.length];
			covMatrixGlobal[0] = MathUtils.diagonal(tmpCov);

			for (d1 = 0; d1 < covMatrixGlobal[0].length; d1++)
				covMatrixGlobal[0][d1] = Math.max(covMatrixGlobal[0][d1], kmeansParams.minCovarianceAllowed);

			invCovMatrixGlobal = new double[1][tmpCov.length];
			invCovMatrixGlobal[0] = MathUtils.inverse(covMatrixGlobal[0]);
		} else {
			covMatrixGlobal = MathUtils.covariance(x);

			for (d1 = 0; d1 < covMatrixGlobal[0].length; d1++) {
				for (d2 = 0; d2 < covMatrixGlobal[d1].length; d2++)
					covMatrixGlobal[d1][d2] = Math.max(covMatrixGlobal[d1][d2], kmeansParams.minCovarianceAllowed);
			}

			invCovMatrixGlobal = MathUtils.inverse(covMatrixGlobal);
		}

		// System.out.println("K-Means clustering completed...");
	}

	public int getFeatureDimension() {
		if (clusters != null && clusters[0].meanVector != null)
			return clusters[0].meanVector.length;
		else
			return 0;
	}

	public int getTotalClusters() {
		if (clusters != null)
			return clusters.length;
		else
			return 0;
	}

	public boolean isDiagonalCovariance() {
		if (clusters != null)
			return clusters[0].isDiagonalCovariance;
		else
			return false;
	}
}
