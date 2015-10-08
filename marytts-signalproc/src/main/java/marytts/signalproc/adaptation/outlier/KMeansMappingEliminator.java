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

import java.io.IOException;
import java.util.Arrays;

import marytts.machinelearning.KMeansClusteringTrainer;
import marytts.machinelearning.KMeansClusteringTrainerParams;
import marytts.signalproc.adaptation.BaselineFeatureExtractor;
import marytts.signalproc.adaptation.OutlierStatus;
import marytts.signalproc.adaptation.codebook.WeightedCodebook;
import marytts.signalproc.adaptation.codebook.WeightedCodebookFile;
import marytts.signalproc.adaptation.codebook.WeightedCodebookFileHeader;
import marytts.signalproc.analysis.distance.DistanceComputer;
import marytts.util.math.MathUtils;

/**
 * K-Means clustering and mapping based outlier elimination. Clusters the source and target LSF, f0, energy, and duration features
 * either jointly or separately. Uses cluster assignments of matched source-target pairs to detect and eliminate pairs which
 * reside in outlier clusters.
 * 
 * @author Oytun T&uuml;rk
 */
public class KMeansMappingEliminator {
	// Separate clusterers
	KMeansClusteringTrainer sourceLsfClusterer;
	KMeansClusteringTrainer sourceF0Clusterer;
	KMeansClusteringTrainer sourceEnergyClusterer;
	KMeansClusteringTrainer sourceDurationClusterer;
	KMeansClusteringTrainer targetLsfClusterer;
	KMeansClusteringTrainer targetF0Clusterer;
	KMeansClusteringTrainer targetEnergyClusterer;
	KMeansClusteringTrainer targetDurationClusterer;
	//

	// Joint clusterers
	KMeansClusteringTrainer sourceClusterer;
	KMeansClusteringTrainer targetClusterer;

	//

	public void eliminate(KMeansMappingEliminatorParams params, String codebookFileIn, String codebookFileOut) {
		sourceLsfClusterer = null;
		sourceF0Clusterer = null;
		sourceEnergyClusterer = null;
		sourceDurationClusterer = null;
		targetLsfClusterer = null;
		targetF0Clusterer = null;
		targetEnergyClusterer = null;
		targetDurationClusterer = null;
		sourceClusterer = null;
		targetClusterer = null;

		WeightedCodebookFile fileIn = new WeightedCodebookFile(codebookFileIn, WeightedCodebookFile.OPEN_FOR_READ);

		WeightedCodebook codebookIn = null;

		try {
			codebookIn = fileIn.readCodebookFile();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		int[] acceptanceStatus = new int[codebookIn.header.totalEntries];
		Arrays.fill(acceptanceStatus, OutlierStatus.NON_OUTLIER);

		int totalLsfOutliers = 0;
		int totalDurationOutliers = 0;
		int totalF0Outliers = 0;
		int totalEnergyOutliers = 0;
		int totalOutliers = 0;

		if (codebookIn != null) {
			int i;

			if (params.isSeparateClustering) {
				if (params.isCheckLsfOutliers) {
					sourceLsfClusterer = clusterFeatures(codebookIn, BaselineFeatureExtractor.LSF_FEATURES,
							WeightedCodebook.SOURCE, params, params.numClustersLsf);
					targetLsfClusterer = clusterFeatures(codebookIn, BaselineFeatureExtractor.LSF_FEATURES,
							WeightedCodebook.TARGET, params, params.numClustersLsf);
				}

				if (params.isCheckF0Outliers) {
					sourceF0Clusterer = clusterFeatures(codebookIn, BaselineFeatureExtractor.F0_FEATURES,
							WeightedCodebook.SOURCE, params, params.numClustersF0);
					targetF0Clusterer = clusterFeatures(codebookIn, BaselineFeatureExtractor.F0_FEATURES,
							WeightedCodebook.TARGET, params, params.numClustersF0);
				}

				if (params.isCheckEnergyOutliers) {
					sourceEnergyClusterer = clusterFeatures(codebookIn, BaselineFeatureExtractor.ENERGY_FEATURES,
							WeightedCodebook.SOURCE, params, params.numClustersEnergy);
					targetEnergyClusterer = clusterFeatures(codebookIn, BaselineFeatureExtractor.ENERGY_FEATURES,
							WeightedCodebook.TARGET, params, params.numClustersEnergy);
				}

				if (params.isCheckDurationOutliers) {
					sourceDurationClusterer = clusterFeatures(codebookIn, BaselineFeatureExtractor.DURATION_FEATURES,
							WeightedCodebook.SOURCE, params, params.numClustersDuration);
					targetDurationClusterer = clusterFeatures(codebookIn, BaselineFeatureExtractor.DURATION_FEATURES,
							WeightedCodebook.TARGET, params, params.numClustersDuration);
				}

				if (params.eliminationAlgorithm == KMeansMappingEliminatorParams.ELIMINATE_LEAST_LIKELY_MAPPINGS) {
					if (params.isCheckLsfOutliers) {
						totalLsfOutliers = eliminateLeastLikelyMappings(sourceLsfClusterer, targetLsfClusterer, acceptanceStatus,
								BaselineFeatureExtractor.LSF_FEATURES, OutlierStatus.LSF_OUTLIER, params.eliminationLikelihood,
								params.distanceType);
						System.out.println("Total lsf outliers = " + String.valueOf(totalLsfOutliers));
					}

					if (params.isCheckF0Outliers) {
						totalF0Outliers = eliminateLeastLikelyMappings(sourceF0Clusterer, targetF0Clusterer, acceptanceStatus,
								BaselineFeatureExtractor.F0_FEATURES, OutlierStatus.F0_OUTLIER, params.eliminationLikelihood,
								params.distanceType);
						System.out.println("Total f0 outliers = " + String.valueOf(totalF0Outliers));
					}

					if (params.isCheckDurationOutliers) {
						totalDurationOutliers = eliminateLeastLikelyMappings(sourceDurationClusterer, targetDurationClusterer,
								acceptanceStatus, BaselineFeatureExtractor.DURATION_FEATURES, OutlierStatus.DURATION_OUTLIER,
								params.eliminationLikelihood, params.distanceType);
						System.out.println("Total duration outliers = " + String.valueOf(totalDurationOutliers));
					}

					if (params.isCheckEnergyOutliers) {
						totalEnergyOutliers = eliminateLeastLikelyMappings(sourceEnergyClusterer, targetEnergyClusterer,
								acceptanceStatus, BaselineFeatureExtractor.ENERGY_FEATURES, OutlierStatus.ENERGY_OUTLIER,
								params.eliminationLikelihood, params.distanceType);
						System.out.println("Total energy outliers = " + String.valueOf(totalEnergyOutliers));
					}
				} else if (params.eliminationAlgorithm == KMeansMappingEliminatorParams.ELIMINATE_MEAN_DISTANCE_MISMATCHES) {
					if (params.isCheckLsfOutliers) {
						totalLsfOutliers = eliminateMeanDistanceMismatches(codebookIn, sourceLsfClusterer, targetLsfClusterer,
								acceptanceStatus, BaselineFeatureExtractor.LSF_FEATURES, OutlierStatus.LSF_OUTLIER,
								params.totalStandardDeviations.lsf, params.distanceType, params.isGlobalVariance);
						System.out.println("Total lsf outliers = " + String.valueOf(totalLsfOutliers));
					}

					if (params.isCheckF0Outliers) {
						totalF0Outliers = eliminateMeanDistanceMismatches(codebookIn, sourceF0Clusterer, targetF0Clusterer,
								acceptanceStatus, BaselineFeatureExtractor.F0_FEATURES, OutlierStatus.F0_OUTLIER,
								params.totalStandardDeviations.f0, params.distanceType, params.isGlobalVariance);
						System.out.println("Total f0 outliers = " + String.valueOf(totalF0Outliers));
					}

					if (params.isCheckDurationOutliers) {
						totalDurationOutliers = eliminateMeanDistanceMismatches(codebookIn, sourceDurationClusterer,
								targetDurationClusterer, acceptanceStatus, BaselineFeatureExtractor.DURATION_FEATURES,
								OutlierStatus.DURATION_OUTLIER, params.totalStandardDeviations.duration, params.distanceType,
								params.isGlobalVariance);
						System.out.println("Total duration outliers = " + String.valueOf(totalDurationOutliers));
					}

					if (params.isCheckEnergyOutliers) {
						totalEnergyOutliers = eliminateMeanDistanceMismatches(codebookIn, sourceEnergyClusterer,
								targetEnergyClusterer, acceptanceStatus, BaselineFeatureExtractor.ENERGY_FEATURES,
								OutlierStatus.ENERGY_OUTLIER, params.totalStandardDeviations.energy, params.distanceType,
								params.isGlobalVariance);
						System.out.println("Total energy outliers = " + String.valueOf(totalEnergyOutliers));
					}
				} else if (params.eliminationAlgorithm == KMeansMappingEliminatorParams.ELIMINATE_USING_SUBCLUSTER_MEAN_DISTANCES) {
					if (params.isCheckLsfOutliers) {
						totalLsfOutliers = eliminateUsingSubclusterMeanDistances(codebookIn, sourceLsfClusterer,
								targetLsfClusterer, acceptanceStatus, BaselineFeatureExtractor.LSF_FEATURES,
								OutlierStatus.LSF_OUTLIER, params.totalStandardDeviations.lsf, params.distanceType);
						System.out.println("Total lsf outliers = " + String.valueOf(totalLsfOutliers));
					}

					if (params.isCheckF0Outliers) {
						totalF0Outliers = eliminateUsingSubclusterMeanDistances(codebookIn, sourceF0Clusterer, targetF0Clusterer,
								acceptanceStatus, BaselineFeatureExtractor.F0_FEATURES, OutlierStatus.F0_OUTLIER,
								params.totalStandardDeviations.f0, params.distanceType);
						System.out.println("Total f0 outliers = " + String.valueOf(totalF0Outliers));
					}

					if (params.isCheckDurationOutliers) {
						totalDurationOutliers = eliminateUsingSubclusterMeanDistances(codebookIn, sourceDurationClusterer,
								targetDurationClusterer, acceptanceStatus, BaselineFeatureExtractor.DURATION_FEATURES,
								OutlierStatus.DURATION_OUTLIER, params.totalStandardDeviations.duration, params.distanceType);
						System.out.println("Total duration outliers = " + String.valueOf(totalDurationOutliers));
					}

					if (params.isCheckEnergyOutliers) {
						totalEnergyOutliers = eliminateUsingSubclusterMeanDistances(codebookIn, sourceEnergyClusterer,
								targetEnergyClusterer, acceptanceStatus, BaselineFeatureExtractor.ENERGY_FEATURES,
								OutlierStatus.ENERGY_OUTLIER, params.totalStandardDeviations.energy, params.distanceType);
						System.out.println("Total energy outliers = " + String.valueOf(totalEnergyOutliers));
					}
				}
			} else {
				int desiredFeatures = 0;
				if (params.isCheckLsfOutliers)
					desiredFeatures += BaselineFeatureExtractor.LSF_FEATURES;
				if (params.isCheckF0Outliers)
					desiredFeatures += BaselineFeatureExtractor.F0_FEATURES;
				if (params.isCheckEnergyOutliers)
					desiredFeatures += BaselineFeatureExtractor.ENERGY_FEATURES;
				if (params.isCheckDurationOutliers)
					desiredFeatures += BaselineFeatureExtractor.DURATION_FEATURES;

				if (desiredFeatures != 0) {
					sourceClusterer = clusterFeatures(codebookIn, desiredFeatures, WeightedCodebook.SOURCE, params,
							params.numClusters);
					targetClusterer = clusterFeatures(codebookIn, desiredFeatures, WeightedCodebook.TARGET, params,
							params.numClusters);

					if (params.eliminationAlgorithm == KMeansMappingEliminatorParams.ELIMINATE_LEAST_LIKELY_MAPPINGS)
						totalOutliers = eliminateLeastLikelyMappings(sourceClusterer, targetClusterer, acceptanceStatus,
								desiredFeatures, OutlierStatus.GENERAL_OUTLIER, params.eliminationLikelihood, params.distanceType);
					else if (params.eliminationAlgorithm == KMeansMappingEliminatorParams.ELIMINATE_MEAN_DISTANCE_MISMATCHES)
						totalOutliers = eliminateMeanDistanceMismatches(codebookIn, sourceClusterer, targetClusterer,
								acceptanceStatus, BaselineFeatureExtractor.LSF_FEATURES, OutlierStatus.LSF_OUTLIER,
								params.totalStandardDeviations.general, params.distanceType, params.isGlobalVariance);
					else if (params.eliminationAlgorithm == KMeansMappingEliminatorParams.ELIMINATE_USING_SUBCLUSTER_MEAN_DISTANCES)
						totalOutliers = eliminateUsingSubclusterMeanDistances(codebookIn, sourceClusterer, targetClusterer,
								acceptanceStatus, BaselineFeatureExtractor.LSF_FEATURES, OutlierStatus.LSF_OUTLIER,
								params.totalStandardDeviations.general, params.distanceType);
				}

				System.out.println("Total outliers = " + String.valueOf(totalOutliers));
			}

			int newTotalEntries = 0;
			for (i = 0; i < codebookIn.header.totalEntries; i++) {
				if (acceptanceStatus[i] == OutlierStatus.NON_OUTLIER)
					newTotalEntries++;
			}

			// Write the output codebook
			WeightedCodebookFile codebookOut = new WeightedCodebookFile(codebookFileOut, WeightedCodebookFile.OPEN_FOR_WRITE);

			WeightedCodebookFileHeader headerOut = new WeightedCodebookFileHeader(codebookIn.header);
			headerOut.resetTotalEntries();
			codebookOut.writeCodebookHeader(headerOut);

			for (i = 0; i < codebookIn.header.totalEntries; i++) {
				if (acceptanceStatus[i] == OutlierStatus.NON_OUTLIER)
					codebookOut.writeEntry(codebookIn.entries[i]);
			}

			codebookOut.close();
			//

			System.out.println("Outliers detected = " + String.valueOf(codebookIn.header.totalEntries - newTotalEntries) + " of "
					+ String.valueOf(codebookIn.header.totalEntries));
		}
	}

	// Collect desired features from codebook and call k-means clustering
	private KMeansClusteringTrainer clusterFeatures(WeightedCodebook codebook, int desiredFeatures, int speakerType,
			KMeansMappingEliminatorParams params, int numClusters) {
		KMeansClusteringTrainer clusterer = null;

		double[][] features = codebook.getFeatures(speakerType, desiredFeatures);

		clusterer = new KMeansClusteringTrainer();
		double[] globalVariances = MathUtils.getVarianceCols(features);

		KMeansClusteringTrainerParams kmeansParams = new KMeansClusteringTrainerParams();
		kmeansParams.numClusters = numClusters;
		kmeansParams.maxIterations = params.maxIterations;
		kmeansParams.minClusterChangePercent = params.minClusterChangePercent;
		kmeansParams.isDiagonalOutputCovariance = params.isDiagonalCovariance;
		kmeansParams.setGlobalVariances(globalVariances);

		clusterer.train(features, kmeansParams);
		features = null; // Memory clean-up

		return clusterer;
	}

	// acceptance status should be initialized properly before calling this function,
	// i.e. it should have the same size as the number of lsf entries in the input codebook
	// all entries should be set to desired values (i.e. OutlierStatus.NON_OUTLIER for first call) since
	// elimination reasons are kept in these entries by summing up in this function
	// eliminationLikelihood should be between 0.0 and 1.0
	private int eliminateLeastLikelyMappings(KMeansClusteringTrainer srcClusterer, KMeansClusteringTrainer tgtClusterer,
			int[] acceptanceStatus, int desiredFeatures, int desiredOutlierStatus, double eliminationLikelihood, int distanceType) {
		int totalOutliers = 0;
		int i, j, k;

		// Find total target clusters for each source cluster and eliminate non-frequent target clusters
		double[][] targetClusterCounts = new double[srcClusterer.clusters.length][]; // Each row correspond to another source
																						// cluster
		for (i = 0; i < srcClusterer.clusters.length; i++) {
			targetClusterCounts[i] = new double[tgtClusterer.clusters.length];
			Arrays.fill(targetClusterCounts[i], 0.0);
		}

		for (i = 0; i < srcClusterer.clusterIndices.length; i++)
			targetClusterCounts[srcClusterer.clusterIndices[i]][tgtClusterer.clusterIndices[i]] += 1.0;

		int[] sortedCountIndices = null;
		double threshold;
		double tempSum;
		int index;
		for (i = 0; i < srcClusterer.clusters.length; i++) {
			sortedCountIndices = MathUtils.quickSort(targetClusterCounts[i]);
			threshold = eliminationLikelihood * MathUtils.sum(targetClusterCounts[i]);
			tempSum = 0.0;
			index = -1;
			for (j = 0; j < targetClusterCounts[i].length; j++) {
				if (tempSum >= threshold)
					break;

				tempSum += targetClusterCounts[i][j];
				index++;
			}

			if (index > -1) {
				for (j = 0; j <= index; j++) {
					for (k = 0; k < tgtClusterer.clusterIndices.length; k++) {
						if (srcClusterer.clusterIndices[k] == i && tgtClusterer.clusterIndices[k] == sortedCountIndices[j]) {
							acceptanceStatus[k] += desiredOutlierStatus;
							totalOutliers++;
						}
					}
				}
			}
		}

		return totalOutliers;
	}

	private int eliminateMeanDistanceMismatches(WeightedCodebook codebookIn, KMeansClusteringTrainer srcClusterer,
			KMeansClusteringTrainer tgtClusterer, int[] acceptanceStatus, int desiredFeatures, int desiredOutlierStatus,
			double totalStandardDeviations, int distanceType, boolean isGlobalVariance) {
		int totalOutliers = 0;
		int i, j, k;

		// If source codebook entry is close to cluster center but corresponding target is not --> One-to-many mapping
		// If target codebook entry is close to cluster center but corresponding source is not --> Many-to-one mapping
		double srcDist, tgtDist;
		double[][] srcFeatures = codebookIn.getFeatures(WeightedCodebook.SOURCE, desiredFeatures);
		double[][] tgtFeatures = codebookIn.getFeatures(WeightedCodebook.SOURCE, desiredFeatures);

		// Compute distance between a hypothetical source cluster boundary vector and the mean vector for thresholding
		double[] boundaryVector = new double[srcFeatures[0].length];
		double[] srcThresholds = new double[srcClusterer.clusters.length];
		for (i = 0; i < srcClusterer.clusters.length; i++) {
			for (j = 0; j < boundaryVector.length; j++) {
				if (isGlobalVariance)
					boundaryVector[j] = srcClusterer.clusters[i].meanVector[j] + totalStandardDeviations
							* srcClusterer.covMatrixGlobal[0][j];
				else
					boundaryVector[j] = srcClusterer.clusters[i].meanVector[j] + totalStandardDeviations
							* srcClusterer.clusters[i].covMatrix[0][j];
			}

			if (distanceType == DistanceComputer.ABSOLUTE_VALUE_DISTANCE)
				srcThresholds[i] = DistanceComputer.getAbsoluteValueDistance(boundaryVector, srcClusterer.clusters[i].meanVector);
			else if (distanceType == DistanceComputer.EUCLIDEAN_DISTANCE)
				srcThresholds[i] = DistanceComputer.getEuclideanDistance(boundaryVector, srcClusterer.clusters[i].meanVector);
			else if (distanceType == DistanceComputer.NORMALIZED_EUCLIDEAN_DISTANCE)
				srcThresholds[i] = DistanceComputer.getNormalizedEuclideanDistance(boundaryVector,
						srcClusterer.clusters[i].meanVector, srcClusterer.clusters[i].covMatrix[0]);
			else if (distanceType == DistanceComputer.MAHALANOBIS_DISTANCE)
				srcThresholds[i] = DistanceComputer.getMahalanobisDistance(boundaryVector, srcClusterer.clusters[i].meanVector,
						srcClusterer.clusters[i].invCovMatrix);
		}

		// Compute distance between a hypothetical target cluster boundary vector and the mean vector for thresholding
		double[] tgtThresholds = new double[tgtClusterer.clusters.length];
		boundaryVector = new double[tgtFeatures[0].length];
		for (i = 0; i < srcClusterer.clusters.length; i++) {
			for (j = 0; j < boundaryVector.length; j++) {
				if (isGlobalVariance)
					boundaryVector[j] = tgtClusterer.clusters[i].meanVector[j] + totalStandardDeviations
							* tgtClusterer.covMatrixGlobal[0][j];
				else
					boundaryVector[j] = tgtClusterer.clusters[i].meanVector[j] + totalStandardDeviations
							* tgtClusterer.clusters[i].covMatrix[0][j];
			}

			if (distanceType == DistanceComputer.ABSOLUTE_VALUE_DISTANCE)
				tgtThresholds[i] = DistanceComputer.getAbsoluteValueDistance(boundaryVector, tgtClusterer.clusters[i].meanVector);
			else if (distanceType == DistanceComputer.EUCLIDEAN_DISTANCE)
				tgtThresholds[i] = DistanceComputer.getEuclideanDistance(boundaryVector, tgtClusterer.clusters[i].meanVector);
			else if (distanceType == DistanceComputer.NORMALIZED_EUCLIDEAN_DISTANCE)
				tgtThresholds[i] = DistanceComputer.getNormalizedEuclideanDistance(boundaryVector,
						tgtClusterer.clusters[i].meanVector, tgtClusterer.clusters[i].covMatrix[0]);
			else if (distanceType == DistanceComputer.MAHALANOBIS_DISTANCE)
				tgtThresholds[i] = DistanceComputer.getMahalanobisDistance(boundaryVector, tgtClusterer.clusters[i].meanVector,
						tgtClusterer.clusters[i].invCovMatrix);
		}

		int totalOne2Many = 0;
		int totalMany2One = 0;
		int totalMany2Many = 0;
		srcDist = 0.0;
		tgtDist = 0.0;
		for (i = 0; i < srcClusterer.clusterIndices.length; i++) {
			if (distanceType == DistanceComputer.ABSOLUTE_VALUE_DISTANCE) {
				srcDist = DistanceComputer.getAbsoluteValueDistance(srcFeatures[i],
						srcClusterer.clusters[srcClusterer.clusterIndices[i]].meanVector);

				tgtDist = DistanceComputer.getAbsoluteValueDistance(tgtFeatures[i],
						tgtClusterer.clusters[tgtClusterer.clusterIndices[i]].meanVector);
			} else if (distanceType == DistanceComputer.EUCLIDEAN_DISTANCE) {
				srcDist = DistanceComputer.getEuclideanDistance(srcFeatures[i],
						srcClusterer.clusters[srcClusterer.clusterIndices[i]].meanVector);

				tgtDist = DistanceComputer.getEuclideanDistance(tgtFeatures[i],
						tgtClusterer.clusters[tgtClusterer.clusterIndices[i]].meanVector);
			} else if (distanceType == DistanceComputer.NORMALIZED_EUCLIDEAN_DISTANCE) {
				if (!isGlobalVariance) {
					srcDist = DistanceComputer.getNormalizedEuclideanDistance(srcFeatures[i],
							srcClusterer.clusters[srcClusterer.clusterIndices[i]].meanVector,
							srcClusterer.clusters[srcClusterer.clusterIndices[i]].covMatrix[0]);

					tgtDist = DistanceComputer.getNormalizedEuclideanDistance(tgtFeatures[i],
							tgtClusterer.clusters[tgtClusterer.clusterIndices[i]].meanVector,
							tgtClusterer.clusters[tgtClusterer.clusterIndices[i]].covMatrix[0]);
				} else {
					srcDist = DistanceComputer.getNormalizedEuclideanDistance(srcFeatures[i],
							srcClusterer.clusters[srcClusterer.clusterIndices[i]].meanVector, srcClusterer.covMatrixGlobal[0]);

					tgtDist = DistanceComputer.getNormalizedEuclideanDistance(tgtFeatures[i],
							tgtClusterer.clusters[tgtClusterer.clusterIndices[i]].meanVector, tgtClusterer.covMatrixGlobal[0]);
				}
			} else if (distanceType == DistanceComputer.MAHALANOBIS_DISTANCE) {
				if (!isGlobalVariance) {
					srcDist = DistanceComputer.getMahalanobisDistance(srcFeatures[i],
							srcClusterer.clusters[srcClusterer.clusterIndices[i]].meanVector,
							srcClusterer.clusters[srcClusterer.clusterIndices[i]].invCovMatrix);

					tgtDist = DistanceComputer.getMahalanobisDistance(tgtFeatures[i],
							tgtClusterer.clusters[tgtClusterer.clusterIndices[i]].meanVector,
							tgtClusterer.clusters[tgtClusterer.clusterIndices[i]].invCovMatrix);
				} else {
					srcDist = DistanceComputer.getMahalanobisDistance(srcFeatures[i],
							srcClusterer.clusters[srcClusterer.clusterIndices[i]].meanVector, srcClusterer.invCovMatrixGlobal);

					tgtDist = DistanceComputer.getMahalanobisDistance(tgtFeatures[i],
							tgtClusterer.clusters[tgtClusterer.clusterIndices[i]].meanVector, tgtClusterer.invCovMatrixGlobal);

				}
			}

			if (srcDist < srcThresholds[srcClusterer.clusterIndices[i]]
					&& tgtDist >= tgtThresholds[tgtClusterer.clusterIndices[i]]) // One-to-many mappings
			{
				acceptanceStatus[i] += OutlierStatus.ONE2MANY_OUTLIER;
				totalOne2Many++;
				totalOutliers++;
			} else if (srcDist >= srcThresholds[srcClusterer.clusterIndices[i]]
					&& tgtDist < tgtThresholds[tgtClusterer.clusterIndices[i]]) // Many-to-one mapping
			{
				acceptanceStatus[i] += OutlierStatus.MANY2ONE_OUTLIER;
				totalMany2One++;
				totalOutliers++;
			} else if (srcDist >= srcThresholds[srcClusterer.clusterIndices[i]]
					&& tgtDist >= tgtThresholds[tgtClusterer.clusterIndices[i]]) // Many-to-many mapping
			{
				acceptanceStatus[i] += OutlierStatus.MANY2MANY_OUTLIER;
				totalMany2Many++;
				totalOutliers++;
			}
		}

		System.out.println("(One2Many=" + String.valueOf(totalOne2Many) + " Many2One=" + String.valueOf(totalMany2One)
				+ " Many2Many=" + String.valueOf(totalMany2Many) + ")");

		return totalOutliers;
	}

	private int eliminateUsingSubclusterMeanDistances(WeightedCodebook codebookIn, KMeansClusteringTrainer srcClusterer,
			KMeansClusteringTrainer tgtClusterer, int[] acceptanceStatus, int desiredFeatures, int desiredOutlierStatus,
			double totalStandardDeviations, int distanceType) {
		int totalOutliers = 0;

		return totalOutliers;
	}
}
