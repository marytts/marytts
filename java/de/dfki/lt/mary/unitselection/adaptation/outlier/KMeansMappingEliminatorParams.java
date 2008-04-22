/**
 * Copyright 2007 DFKI GmbH.
 * All Rights Reserved.  Use is subject to license terms.
 * 
 * Permission is hereby granted, free of charge, to use and distribute
 * this software and its documentation without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of this work, and to
 * permit persons to whom this work is furnished to do so, subject to
 * the following conditions:
 * 
 * 1. The code must retain the above copyright notice, this list of
 *    conditions and the following disclaimer.
 * 2. Any modifications must be clearly marked as such.
 * 3. Original authors' names are not deleted.
 * 4. The authors' names are not used to endorse or promote products
 *    derived from this software without specific prior written
 *    permission.
 *
 * DFKI GMBH AND THE CONTRIBUTORS TO THIS WORK DISCLAIM ALL WARRANTIES WITH
 * REGARD TO THIS SOFTWARE, INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS, IN NO EVENT SHALL DFKI GMBH NOR THE
 * CONTRIBUTORS BE LIABLE FOR ANY SPECIAL, INDIRECT OR CONSEQUENTIAL
 * DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR
 * PROFITS, WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS
 * ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE OF
 * THIS SOFTWARE.
 */

package de.dfki.lt.mary.unitselection.adaptation.outlier;

import de.dfki.lt.machinelearning.KMeansClusteringTrainer;
import de.dfki.lt.signalproc.util.distance.DistanceComputer;

/**
 * @author oytun.turk
 *
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
    
    public int maximumIterations; 
    public double minClusterPercent;
    public boolean isDiagonalCovariance;
    
    public KMeansMappingEliminatorParams()
    {
        super();
        
        numClusters = DEFAULT_NUM_CLUSTERS;
        numClustersLsf = DEFAULT_NUM_CLUSTERS;
        numClustersF0 = DEFAULT_NUM_CLUSTERS;
        numClustersDuration = DEFAULT_NUM_CLUSTERS;
        numClustersEnergy = DEFAULT_NUM_CLUSTERS;
        
        maximumIterations = KMeansClusteringTrainer.MAXIMUM_ITERATIONS_DEFAULT;
        minClusterPercent = KMeansClusteringTrainer.MIN_CLUSTER_PERCENT_DEFAULT;
        isDiagonalCovariance = KMeansClusteringTrainer.IS_DIAGONAL_COVARIANCE_DEFAULT;
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
        
        maximumIterations = existing.maximumIterations;
        minClusterPercent = existing.minClusterPercent;
        isDiagonalCovariance = existing.isDiagonalCovariance;
        isSeparateClustering = existing.isSeparateClustering;
        eliminationAlgorithm = existing.eliminationAlgorithm;
        eliminationLikelihood = existing.eliminationLikelihood;
        totalStandardDeviations = new TotalStandardDeviations(existing.totalStandardDeviations);
        distanceType = existing.distanceType;
        isGlobalVariance = existing.isGlobalVariance;
    }
}
