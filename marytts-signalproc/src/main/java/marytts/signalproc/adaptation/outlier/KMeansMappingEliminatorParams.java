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
package marytts.signalproc.adaptation.outlier;

import marytts.machinelearning.KMeansClusteringTrainerParams;
import marytts.signalproc.analysis.distance.DistanceComputer;

/**
 * This class implements a K-Means clustering and mapping based outlier elimination procedure: - Step1: Cluster source and target
 * acoustic features either jointly or separately - Step2: For each feature, for each source cluster find the most likely target
 * cluster - Step3: For each feature, for each target cluster find the most likely source cluster - Step4: Determine outlier pairs
 * by checking the total number of source-target pairs assigned to clusters other than the most likely cluster which are
 * sufficiently "distant" from the most likely cluster
 * 
 * @author Oytun T&uuml;rk
 */
public class KMeansMappingEliminatorParams extends BaselineOutlierEliminatorParams {
	public int eliminationAlgorithm; // EliminationAlgorithm to use
	public static final int ELIMINATE_LEAST_LIKELY_MAPPINGS = 1;
	public static final int ELIMINATE_MEAN_DISTANCE_MISMATCHES = 2;
	public static final int ELIMINATE_USING_SUBCLUSTER_MEAN_DISTANCES = 3;

	// Parameters of ELIMINATE_LEAST_LIKELY_MAPPINGS algorithm:
	public double eliminationLikelihood;
	//

	// Parameters of ELIMINATE_MEAN_DISTANCE_MISMATCHES algorithm:
	public TotalStandardDeviations totalStandardDeviations;
	public int distanceType; // This can be one of the distance measures in class DistanceComputer
	public boolean isGlobalVariance; // Use global variances when computing normalized Euclidean distance
										// If false, separate variance is computed for each cluster which may lead to inaccuracies
										// when cluster size is small

	//

	public boolean isSeparateClustering; // Separate cluster numbers are used only if this is set to true
	public int numClusters; // Total clusters
	public int numClustersLsf; // Total clusters for LSFs
	public int numClustersF0; // Total clusters for f0
	public int numClustersDuration; // Total clusters for duration
	public int numClustersEnergy; // Total clusters for energy
	public static final int DEFAULT_NUM_CLUSTERS = 30; // Default number of clusters

	public int maxIterations; // Maximum K-means iterations
	public double minClusterChangePercent; // Minimum percentage change in clsuter means to terminate K-means iterations
	public boolean isDiagonalCovariance; // Use diagonal covariance matrix as cluster covariances

	public KMeansMappingEliminatorParams() {
		super();

		numClusters = DEFAULT_NUM_CLUSTERS;
		numClustersLsf = DEFAULT_NUM_CLUSTERS;
		numClustersF0 = DEFAULT_NUM_CLUSTERS;
		numClustersDuration = DEFAULT_NUM_CLUSTERS;
		numClustersEnergy = DEFAULT_NUM_CLUSTERS;

		maxIterations = KMeansClusteringTrainerParams.KMEANS_MAX_ITERATIONS_DEFAULT;
		minClusterChangePercent = KMeansClusteringTrainerParams.KMEANS_MIN_CLUSTER_CHANGE_PERCENT_DEFAULT;
		isDiagonalCovariance = KMeansClusteringTrainerParams.KMEANS_IS_DIAGONAL_COVARIANCE_DEFAULT;
		isSeparateClustering = false;
		eliminationAlgorithm = ELIMINATE_LEAST_LIKELY_MAPPINGS;
		eliminationLikelihood = 0.1;
		totalStandardDeviations = new TotalStandardDeviations();
		distanceType = DistanceComputer.NORMALIZED_EUCLIDEAN_DISTANCE;
		isGlobalVariance = true;
	}

	public KMeansMappingEliminatorParams(KMeansMappingEliminatorParams existing) {
		super(existing);

		numClusters = existing.numClusters;
		numClustersLsf = existing.numClustersLsf;
		numClustersF0 = existing.numClustersF0;
		numClustersDuration = existing.numClustersDuration;
		numClustersEnergy = existing.numClustersEnergy;

		maxIterations = existing.maxIterations;
		minClusterChangePercent = existing.minClusterChangePercent;
		isDiagonalCovariance = existing.isDiagonalCovariance;
		isSeparateClustering = existing.isSeparateClustering;
		eliminationAlgorithm = existing.eliminationAlgorithm;
		eliminationLikelihood = existing.eliminationLikelihood;
		totalStandardDeviations = new TotalStandardDeviations(existing.totalStandardDeviations);
		distanceType = existing.distanceType;
		isGlobalVariance = existing.isGlobalVariance;
	}
}
