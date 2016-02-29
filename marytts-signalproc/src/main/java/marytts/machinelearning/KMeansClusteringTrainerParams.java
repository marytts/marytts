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

/**
 * Wrapper class for K-Means clustering training parameters
 * 
 * @author Oytun T&uuml;rk
 */
public class KMeansClusteringTrainerParams {

	// A set of default values for K-Means training parameters
	public static final int KMEANS_MAX_ITERATIONS_DEFAULT = 200;
	public static final double KMEANS_MIN_CLUSTER_CHANGE_PERCENT_DEFAULT = 0.0001;
	public static final boolean KMEANS_IS_DIAGONAL_COVARIANCE_DEFAULT = true;
	public static final int KMEANS_MIN_SAMPLES_IN_ONE_CLUSTER_DEFAULT = 10;
	private static final double KMEANS_MIN_COVARIANCE_ALLOWED_DEFAULT = 1e-5;
	//

	public int numClusters; // Number of clusters to be trained
	public int maxIterations; // Maximum iterations to stop K-means training
	public double minClusterChangePercent; // Minimum percent change in cluster assignments to stop K-Means iterations
	public boolean isDiagonalOutputCovariance; // Estimate diagonal cluster covariances finally?
	public int minSamplesInOneCluster; // Minimum number of observations allowed in one cluster
	public double minCovarianceAllowed; // Minimum covariance value allowed for final cluster covariance matrices
	public double[] globalVariances; // Global variance vector of whole data

	// Default constructor
	public KMeansClusteringTrainerParams() {
		numClusters = 0;
		maxIterations = KMEANS_MAX_ITERATIONS_DEFAULT;
		minClusterChangePercent = KMEANS_MIN_CLUSTER_CHANGE_PERCENT_DEFAULT;
		isDiagonalOutputCovariance = KMEANS_IS_DIAGONAL_COVARIANCE_DEFAULT;
		minSamplesInOneCluster = KMEANS_MIN_SAMPLES_IN_ONE_CLUSTER_DEFAULT;
		minCovarianceAllowed = KMEANS_MIN_COVARIANCE_ALLOWED_DEFAULT;
		globalVariances = null;
	}

	// Constructor using GMM training parameters
	public KMeansClusteringTrainerParams(GMMTrainerParams gmmParams) {
		numClusters = gmmParams.totalComponents;
		maxIterations = gmmParams.kmeansMaxIterations;
		minClusterChangePercent = gmmParams.kmeansMinClusterChangePercent;
		isDiagonalOutputCovariance = gmmParams.isDiagonalCovariance;
		minSamplesInOneCluster = gmmParams.kmeansMinSamplesInOneCluster;
		minCovarianceAllowed = gmmParams.minCovarianceAllowed;
		globalVariances = null;
	}

	// Constructor using an existing parameter set
	public KMeansClusteringTrainerParams(KMeansClusteringTrainerParams existing) {
		numClusters = existing.numClusters;
		maxIterations = existing.maxIterations;
		minClusterChangePercent = existing.minClusterChangePercent;
		isDiagonalOutputCovariance = existing.isDiagonalOutputCovariance;
		minSamplesInOneCluster = existing.minSamplesInOneCluster;

		setGlobalVariances(existing.globalVariances);
	}

	// Set global variance values
	public void setGlobalVariances(double[] globalVariancesIn) {
		if (globalVariancesIn != null) {
			if (globalVariances == null || globalVariancesIn.length != globalVariances.length)
				globalVariances = new double[globalVariancesIn.length];

			System.arraycopy(globalVariancesIn, 0, globalVariances, 0, globalVariancesIn.length);
		}
	}
}
