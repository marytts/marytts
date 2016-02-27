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

import java.awt.Color;
import java.util.Arrays;

import javax.swing.JFrame;

import marytts.signalproc.display.FunctionGraph;
import marytts.util.math.MathUtils;
import marytts.util.math.Polynomial;

/**
 * K-Means clustering training algorithm
 * 
 * Reference: J. MacQueen, 1967, "Some methods for classification and analysis of multivariate observations", Proc. Fifth Berkeley
 * Symp. on Math. Statist. and Prob., Vol. 1 (Univ. of Calif. Press, 1967), pp. 281-297.
 * 
 * This version is adapted to work with a distance function between polynomials.
 * 
 * @author Oytun T&uuml;rk, Marc Schröder
 */
public class PolynomialKMeansClusteringTrainer {
	/**
	 * This function clusters polynomials using K-Means clustering procedure, using a polynomial distance function. Training
	 * consists of four steps: (a) Initialization (random assignment of cluster means using data points that are far away from
	 * each other + slight random shifts) (b) Hard clustering of samples according to new cluster means (c) Update of cluster
	 * means using assigned samples (d) Re-iteration of (b) and (c) until convergence, i.e. when overall cluster occupancy does
	 * not change much
	 * 
	 * @param polynomials
	 *            the observations to cluster
	 * @param kmeansParams
	 *            All training parameters are given by kmeansParams (See KMeansClusteringTrainerParams.java for details)
	 * @return the clusters trained
	 */
	public static PolynomialCluster[] train(Polynomial[] polynomials, KMeansClusteringTrainerParams kmeansParams) {

		int[] totalObservationsInClusters; // Total number of observations in each cluster
		int[] clusterIndices; // Assigned cluster for each observation vector

		int observations = polynomials.length;
		int polynomialOrder = polynomials[0].getOrder();

		// Intermediate representations for computing updated cluster means:
		Polynomial[] m_new = new Polynomial[kmeansParams.numClusters];

		// b[i][j] == true if observation i belongs to cluster j
		boolean[][] b = new boolean[observations][kmeansParams.numClusters];
		boolean[][] b_old = new boolean[observations][kmeansParams.numClusters];

		Polynomial[] clusterMeans = new Polynomial[kmeansParams.numClusters];
		for (int k = 0; k < kmeansParams.numClusters; k++)
			clusterMeans[k] = new Polynomial(polynomialOrder);

		for (int t = 1; t <= observations; t++) {
			Arrays.fill(b[t - 1], false);
		}

		// Select initial cluster centers
		Polynomial mAll = Polynomial.mean(polynomials);

		double[] dists = new double[observations];
		double[] tmp = new double[kmeansParams.numClusters + 1];

		for (int k = 1; k <= kmeansParams.numClusters; k++) {
			// For each cluster, initiate it with the observation that is most distant from the clusters initiated so far
			for (int t = 1; t <= observations; t++) {
				if (k > 1) {
					for (int i = 1; i <= k - 1; i++) {
						tmp[i - 1] = clusterMeans[i - 1].polynomialDistance(polynomials[t - 1]);
					}
					tmp[k - 1] = mAll.polynomialDistance(polynomials[t - 1]);
					dists[t - 1] = MathUtils.mean(tmp, 0, k - 1);
				} else {
					dists[t - 1] = mAll.polynomialDistance(polynomials[t - 1]);
				}
			}

			double maxD = Double.MIN_VALUE;
			int maxInd = -1;
			for (int t = 1; t <= observations; t++) {
				if (dists[t - 1] > maxD) {
					maxD = dists[t - 1];
					maxInd = t;
				}
			}

			clusterMeans[k - 1].copyCoeffs(polynomials[maxInd - 1]);

			// System.out.println("Cluster center " + String.valueOf(k) + " initialized...");
		}
		//

		int[] tinyClusterInds = new int[kmeansParams.numClusters];
		int numTinyClusters = 0;
		totalObservationsInClusters = new int[kmeansParams.numClusters];
		clusterIndices = new int[observations];

		int iter = 0;
		boolean bCont = true;
		while (bCont) {
			// Associate each observation with the nearest cluster
			for (int t = 1; t <= observations; t++) { // Over all observations
				double minDist = Double.MAX_VALUE;
				int ind = -1;
				for (int i = 1; i <= kmeansParams.numClusters; i++) { // Over all clusters
					double tmpDist = clusterMeans[i - 1].polynomialDistance(polynomials[t - 1]);
					b[t - 1][i - 1] = false;
					if (tmpDist < minDist) {
						minDist = tmpDist;
						ind = i;
					}
				}
				// associate the observation with the cluster to which it has minimum distance:
				b[t - 1][ind - 1] = true;
			}

			// Prepare means per cluster based on new cluster members:
			for (int i = 1; i <= kmeansParams.numClusters; i++) {
				totalObservationsInClusters[i - 1] = 0;
				tinyClusterInds[i - 1] = 0;
			}

			int c = 1; // count tiny clusters
			for (int i = 1; i <= kmeansParams.numClusters; i++) {
				m_new[i - 1] = new Polynomial(polynomialOrder);

				for (int t = 1; t <= observations; t++) {
					if (b[t - 1][i - 1]) { // observation t is associated with cluster i
						for (int d = 0; d <= polynomialOrder; d++)
							m_new[i - 1].coeffs[d] += polynomials[t - 1].coeffs[d];

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

			// Update the means of clusters if these are big enough,
			// and replace tiny clusters with random variations of big ones:
			c = 0;
			// need doubles to use quicksort:
			double[] tmps = new double[totalObservationsInClusters.length];
			for (int a = 0; a < tmps.length; a++) {
				tmps[a] = totalObservationsInClusters[a];
			}
			int[] inds = MathUtils.quickSort(tmps, 0, kmeansParams.numClusters - 1);
			for (int i = 1; i <= kmeansParams.numClusters; i++) {
				if (totalObservationsInClusters[i - 1] >= kmeansParams.minSamplesInOneCluster) { // a normal-sized cluster --
																									// update mean
					for (int d = 0; d <= polynomialOrder; d++) {
						clusterMeans[i - 1].coeffs[d] = m_new[i - 1].coeffs[d] / totalObservationsInClusters[i - 1];
					}
				} else { // a tiny cluster -- reinitialise with a random variation of one of the big clusters
					for (int d = 0; d <= polynomialOrder; d++) {
						double rnd = 2 * (Math.random() - 0.5) /* a random number between -1 and 1 */
								* clusterMeans[inds[kmeansParams.numClusters - c - 1]].coeffs[d] * 0.01;
						clusterMeans[i - 1].coeffs[d] = clusterMeans[inds[kmeansParams.numClusters - c - 1]].coeffs[d] + rnd;
					}
					c++;
				}
			}

			int[] prev_totals = totalObservationsInClusters.clone();

			iter++;
			// Count number of observations that have changed cluster:
			int totChanged = 0;
			if (iter > 1) {
				if (iter >= kmeansParams.maxIterations) {
					bCont = false;
				}

				for (int t = 1; t <= observations; t++) {
					for (int i = 1; i <= kmeansParams.numClusters; i++) {
						if (b_old[t - 1][i - 1] != b[t - 1][i - 1]) {
							totChanged++;
							break; // Count each difference once
						}
					}
				}

				double changedPerc = (double) totChanged / observations * 100.0;
				if (changedPerc < kmeansParams.minClusterChangePercent) { // stop if number of clusters changed is less than
																			// %MIN_CHANGE_PERCENT of total observation
					bCont = false;
				}
				// System.out.println("K-Means iteration: " + String.valueOf(iter) + " with " + String.valueOf(changedPerc) +
				// " percent of cluster assignments updated");
			}
			// else
			// System.out.println("K-Means iteration: " + String.valueOf(iter) + " K-means initialized");

			for (int t = 1; t <= observations; t++) {
				System.arraycopy(b[t - 1], 0, b_old[t - 1], 0, b[t - 1].length);
			}
		}

		// We do not compute covariances here, because we are unidimensional only.

		// Now fill the custers with their means and members:
		PolynomialCluster[] clusters = new PolynomialCluster[kmeansParams.numClusters];
		for (int i = 1; i <= kmeansParams.numClusters; i++) {
			Polynomial[] members = new Polynomial[totalObservationsInClusters[i - 1]];
			int m = 0;
			for (int t = 1; t <= observations; t++) {
				if (b[t - 1][i - 1]) {
					members[m] = polynomials[t - 1];
					m++;
				}
			}
			assert m == members.length;
			clusters[i - 1] = new PolynomialCluster(clusterMeans[i - 1], members);
		}

		return clusters;
		// System.out.println("K-Means clustering completed...");
	}

	public static void main(String[] args) {
		// Test clustering with random polynomials, and visualise result
		int order = 3;
		int numPolynomials = 1000;
		int numClusters = 50;

		// Initialise with random data:
		Polynomial[] ps = new Polynomial[numPolynomials];
		for (int i = 0; i < numPolynomials; i++) {
			double[] coeffs = new double[order + 1];
			for (int c = 0; c < coeffs.length; c++) {
				coeffs[c] = Math.random();
			}
			ps[i] = new Polynomial(coeffs);
		}
		KMeansClusteringTrainerParams params = new KMeansClusteringTrainerParams();
		params.numClusters = numClusters;

		// Train:
		PolynomialCluster[] clusters = PolynomialKMeansClusteringTrainer.train(ps, params);

		// Visualise:
		FunctionGraph clusterGraph = new FunctionGraph(0, 1, new double[1]);
		clusterGraph.setYMinMax(0, 5);
		clusterGraph.setPrimaryDataSeriesStyle(Color.BLUE, FunctionGraph.DRAW_DOTS, FunctionGraph.DOT_FULLCIRCLE);
		JFrame jf = clusterGraph.showInJFrame("", false, true);
		for (int i = 0; i < clusters.length; i++) {
			double[] meanValues = clusters[i].getMeanPolynomial().generatePolynomialValues(100, 0, 1);
			clusterGraph.updateData(0, 1. / meanValues.length, meanValues);

			Polynomial[] members = clusters[i].getClusterMembers();
			for (int m = 0; m < members.length; m++) {
				double[] pred = members[m].generatePolynomialValues(meanValues.length, 0, 1);
				clusterGraph.addDataSeries(pred, Color.GRAY, FunctionGraph.DRAW_LINE, -1);
				jf.repaint();
			}

			jf.setTitle("Cluster " + (i + 1) + " of " + clusters.length + ": " + members.length + " members");
			jf.repaint();

			try {
				Thread.sleep(500);
			} catch (InterruptedException ie) {
			}
		}
		System.exit(0);
	}

}
