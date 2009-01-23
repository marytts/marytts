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
 * Parameters for K-Means clustering and mapping based outlier elimination
 *
 * @author Oytun T&uumlrk
 */
public class KMeansMappingEliminatorParams extends BaselineOutlierEliminatorParams {
    public int eliminationAlgorithm;
    //eliminationAlgorithm types and their parameters:
    public static final int ELIMINATE_LEAST_LIKELY_MAPPINGS = 1;            
    public double eliminationLikelihood;
    
    public static final int ELIMINATE_MEAN_DISTANCE_MISMATCHES = 2;         
    public TotalStandardDeviations totalStandardDeviations;
    public int distanceType; //This can be one of the distance measures in class DistanceComputer
    public boolean isGlobalVariance; //Use global variances when computing normalized Euclidean distance
                                     // If false, separate variance is computed for each cluster which may lead to inaccuracies when cluster size is small
    
    public static final int ELIMINATE_USING_SUBCLUSTER_MEAN_DISTANCES = 3;
    //
    
    public boolean isSeparateClustering; //separate cluster numbers are used only if this is set to true
    public int numClusters;
    public int numClustersLsf;
    public int numClustersF0;
    public int numClustersDuration;
    public int numClustersEnergy;
    public static final int DEFAULT_NUM_CLUSTERS = 30;
    
    public int maxIterations; 
    public double minClusterChangePercent;
    public boolean isDiagonalCovariance;
    
    public KMeansMappingEliminatorParams()
    {
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
    
    public KMeansMappingEliminatorParams(KMeansMappingEliminatorParams existing)
    {
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

