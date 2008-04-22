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

import java.io.IOException;
import java.util.Arrays;

import de.dfki.lt.machinelearning.KMeansClusteringTester;
import de.dfki.lt.machinelearning.KMeansClusteringTrainer;
import de.dfki.lt.mary.unitselection.adaptation.OutlierStatus;
import de.dfki.lt.mary.unitselection.adaptation.codebook.WeightedCodebook;
import de.dfki.lt.mary.unitselection.adaptation.BaselineFeatureExtractor;
import de.dfki.lt.mary.unitselection.adaptation.codebook.WeightedCodebookFile;
import de.dfki.lt.mary.unitselection.adaptation.codebook.WeightedCodebookFileHeader;
import de.dfki.lt.signalproc.util.MathUtils;
import de.dfki.lt.signalproc.util.distance.DistanceComputer;

/**
 * @author oytun.turk
 *
 */
public class KMeansMappingEliminator {
    //Separate clusterers
    KMeansClusteringTrainer sourceLsfClusterer;
    KMeansClusteringTrainer sourceF0Clusterer;
    KMeansClusteringTrainer sourceEnergyClusterer;
    KMeansClusteringTrainer sourceDurationClusterer;
    KMeansClusteringTrainer targetLsfClusterer;
    KMeansClusteringTrainer targetF0Clusterer;
    KMeansClusteringTrainer targetEnergyClusterer;
    KMeansClusteringTrainer targetDurationClusterer;
    //

    //Joint clusterers
    KMeansClusteringTrainer sourceClusterer;
    KMeansClusteringTrainer targetClusterer;
    //

    public void eliminate(KMeansMappingEliminatorParams params,
            String codebookFileIn, 
            String codebookFileOut)
    {    
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
//          TODO Auto-generated catch block
            e.printStackTrace();
        }

        int[] acceptanceStatus = new int[codebookIn.header.totalLsfEntries];
        Arrays.fill(acceptanceStatus, OutlierStatus.NON_OUTLIER);

        int totalLsfOutliers = 0;
        int totalDurationOutliers = 0;
        int totalF0Outliers = 0;
        int totalEnergyOutliers = 0;
        int totalOutliers = 0;

        if (codebookIn!=null)
        {
            int i;

            if (params.isSeparateClustering)
            {
                if (params.isCheckLsfOutliers)
                {
                    sourceLsfClusterer = clusterFeatures(codebookIn, BaselineFeatureExtractor.LSF_FEATURES, WeightedCodebook.SOURCE, params, params.numClustersLsf);
                    targetLsfClusterer = clusterFeatures(codebookIn, BaselineFeatureExtractor.LSF_FEATURES, WeightedCodebook.TARGET, params, params.numClustersLsf);
                }

                if (params.isCheckF0Outliers)
                {
                    sourceF0Clusterer = clusterFeatures(codebookIn, BaselineFeatureExtractor.F0_FEATURES, WeightedCodebook.SOURCE, params, params.numClustersF0);
                    targetF0Clusterer = clusterFeatures(codebookIn, BaselineFeatureExtractor.F0_FEATURES, WeightedCodebook.TARGET, params, params.numClustersF0);
                }

                if (params.isCheckEnergyOutliers)
                {
                    sourceEnergyClusterer = clusterFeatures(codebookIn, BaselineFeatureExtractor.ENERGY_FEATURES, WeightedCodebook.SOURCE, params, params.numClustersEnergy);
                    targetEnergyClusterer = clusterFeatures(codebookIn, BaselineFeatureExtractor.ENERGY_FEATURES, WeightedCodebook.TARGET, params, params.numClustersEnergy);
                }

                if (params.isCheckDurationOutliers)
                {
                    sourceDurationClusterer = clusterFeatures(codebookIn, BaselineFeatureExtractor.DURATION_FEATURES, WeightedCodebook.SOURCE, params, params.numClustersDuration);
                    targetDurationClusterer = clusterFeatures(codebookIn, BaselineFeatureExtractor.DURATION_FEATURES, WeightedCodebook.TARGET, params, params.numClustersDuration);
                }

                if (params.eliminationAlgorithm == KMeansMappingEliminatorParams.ELIMINATE_LEAST_LIKELY_MAPPINGS)
                {
                    if (params.isCheckLsfOutliers)
                    {
                        totalLsfOutliers = eliminateLeastLikelyMappings(sourceLsfClusterer, targetLsfClusterer, acceptanceStatus, BaselineFeatureExtractor.LSF_FEATURES, OutlierStatus.LSF_OUTLIER, params.eliminationLikelihood, params.distanceType);
                        System.out.println("Total lsf outliers = " + String.valueOf(totalLsfOutliers));
                    }
                    
                    if (params.isCheckF0Outliers)
                    {
                        totalF0Outliers = eliminateLeastLikelyMappings(sourceF0Clusterer, targetF0Clusterer, acceptanceStatus, BaselineFeatureExtractor.F0_FEATURES, OutlierStatus.F0_OUTLIER, params.eliminationLikelihood, params.distanceType);
                        System.out.println("Total f0 outliers = " + String.valueOf(totalF0Outliers));
                    }
                    
                    if (params.isCheckDurationOutliers)
                    {
                        totalDurationOutliers = eliminateLeastLikelyMappings(sourceDurationClusterer, targetDurationClusterer, acceptanceStatus, BaselineFeatureExtractor.DURATION_FEATURES, OutlierStatus.DURATION_OUTLIER, params.eliminationLikelihood, params.distanceType);
                        System.out.println("Total duration outliers = " + String.valueOf(totalDurationOutliers));
                    }
                    
                    if (params.isCheckEnergyOutliers)
                    {
                        totalEnergyOutliers = eliminateLeastLikelyMappings(sourceEnergyClusterer, targetEnergyClusterer, acceptanceStatus, BaselineFeatureExtractor.ENERGY_FEATURES, OutlierStatus.ENERGY_OUTLIER, params.eliminationLikelihood, params.distanceType);
                        System.out.println("Total energy outliers = " + String.valueOf(totalEnergyOutliers));
                    }
                }
                else if (params.eliminationAlgorithm == KMeansMappingEliminatorParams.ELIMINATE_MEAN_DISTANCE_MISMATCHES)
                {
                    if (params.isCheckLsfOutliers)
                    {
                        totalLsfOutliers = eliminateMeanDistanceMismatches(codebookIn, sourceLsfClusterer, targetLsfClusterer, acceptanceStatus, BaselineFeatureExtractor.LSF_FEATURES, OutlierStatus.LSF_OUTLIER, params.totalStandardDeviations.lsf, params.distanceType, params.isGlobalVariance);
                        System.out.println("Total lsf outliers = " + String.valueOf(totalLsfOutliers));
                    }
                    
                    if (params.isCheckF0Outliers)
                    {
                        totalF0Outliers = eliminateMeanDistanceMismatches(codebookIn, sourceF0Clusterer, targetF0Clusterer, acceptanceStatus, BaselineFeatureExtractor.F0_FEATURES, OutlierStatus.F0_OUTLIER, params.totalStandardDeviations.f0, params.distanceType, params.isGlobalVariance);
                        System.out.println("Total f0 outliers = " + String.valueOf(totalF0Outliers));
                    }
                    
                    if (params.isCheckDurationOutliers)
                    {
                        totalDurationOutliers = eliminateMeanDistanceMismatches(codebookIn, sourceDurationClusterer, targetDurationClusterer, acceptanceStatus, BaselineFeatureExtractor.DURATION_FEATURES, OutlierStatus.DURATION_OUTLIER, params.totalStandardDeviations.duration, params.distanceType, params.isGlobalVariance);
                        System.out.println("Total duration outliers = " + String.valueOf(totalDurationOutliers));
                    }
                    
                    if (params.isCheckEnergyOutliers)
                    {
                        totalEnergyOutliers = eliminateMeanDistanceMismatches(codebookIn, sourceEnergyClusterer, targetEnergyClusterer, acceptanceStatus, BaselineFeatureExtractor.ENERGY_FEATURES, OutlierStatus.ENERGY_OUTLIER, params.totalStandardDeviations.energy, params.distanceType, params.isGlobalVariance);
                        System.out.println("Total energy outliers = " + String.valueOf(totalEnergyOutliers));
                    }
                }
                else if (params.eliminationAlgorithm == KMeansMappingEliminatorParams.ELIMINATE_USING_SUBCLUSTER_MEAN_DISTANCES)
                {
                    if (params.isCheckLsfOutliers)
                    {
                        totalLsfOutliers = eliminateUsingSubclusterMeanDistances(codebookIn, sourceLsfClusterer, targetLsfClusterer, acceptanceStatus, BaselineFeatureExtractor.LSF_FEATURES, OutlierStatus.LSF_OUTLIER, params.totalStandardDeviations.lsf, params.distanceType);
                        System.out.println("Total lsf outliers = " + String.valueOf(totalLsfOutliers));
                    }
                    
                    if (params.isCheckF0Outliers)
                    {
                        totalF0Outliers = eliminateUsingSubclusterMeanDistances(codebookIn, sourceF0Clusterer, targetF0Clusterer, acceptanceStatus, BaselineFeatureExtractor.F0_FEATURES, OutlierStatus.F0_OUTLIER, params.totalStandardDeviations.f0, params.distanceType);
                        System.out.println("Total f0 outliers = " + String.valueOf(totalF0Outliers));
                    }
                    
                    if (params.isCheckDurationOutliers)
                    {
                        totalDurationOutliers = eliminateUsingSubclusterMeanDistances(codebookIn, sourceDurationClusterer, targetDurationClusterer, acceptanceStatus, BaselineFeatureExtractor.DURATION_FEATURES, OutlierStatus.DURATION_OUTLIER, params.totalStandardDeviations.duration, params.distanceType);
                        System.out.println("Total duration outliers = " + String.valueOf(totalDurationOutliers));
                    }
                    
                    if (params.isCheckEnergyOutliers)
                    {
                        totalEnergyOutliers = eliminateUsingSubclusterMeanDistances(codebookIn, sourceEnergyClusterer, targetEnergyClusterer, acceptanceStatus, BaselineFeatureExtractor.ENERGY_FEATURES, OutlierStatus.ENERGY_OUTLIER, params.totalStandardDeviations.energy, params.distanceType);
                        System.out.println("Total energy outliers = " + String.valueOf(totalEnergyOutliers)); 
                    }
                }
            }
            else
            {
                int desiredFeatures = 0;
                if (params.isCheckLsfOutliers)
                    desiredFeatures += BaselineFeatureExtractor.LSF_FEATURES;
                if (params.isCheckF0Outliers)
                    desiredFeatures += BaselineFeatureExtractor.F0_FEATURES;
                if (params.isCheckEnergyOutliers)
                    desiredFeatures += BaselineFeatureExtractor.ENERGY_FEATURES;
                if (params.isCheckDurationOutliers)
                    desiredFeatures += BaselineFeatureExtractor.DURATION_FEATURES;

                sourceClusterer = clusterFeatures(codebookIn, desiredFeatures, WeightedCodebook.SOURCE, params, params.numClusters);
                targetClusterer = clusterFeatures(codebookIn, desiredFeatures, WeightedCodebook.TARGET, params, params.numClusters);

                if (params.eliminationAlgorithm == KMeansMappingEliminatorParams.ELIMINATE_LEAST_LIKELY_MAPPINGS)
                    totalOutliers = eliminateLeastLikelyMappings(sourceClusterer, targetClusterer, acceptanceStatus, desiredFeatures, OutlierStatus.GENERAL_OUTLIER, params.eliminationLikelihood, params.distanceType);
                else if (params.eliminationAlgorithm == KMeansMappingEliminatorParams.ELIMINATE_MEAN_DISTANCE_MISMATCHES)
                    totalOutliers = eliminateMeanDistanceMismatches(codebookIn, sourceClusterer, targetClusterer, acceptanceStatus, BaselineFeatureExtractor.LSF_FEATURES, OutlierStatus.LSF_OUTLIER, params.totalStandardDeviations.general, params.distanceType, params.isGlobalVariance);
                else if (params.eliminationAlgorithm == KMeansMappingEliminatorParams.ELIMINATE_USING_SUBCLUSTER_MEAN_DISTANCES)
                    totalOutliers = eliminateUsingSubclusterMeanDistances(codebookIn, sourceClusterer, targetClusterer, acceptanceStatus, BaselineFeatureExtractor.LSF_FEATURES, OutlierStatus.LSF_OUTLIER, params.totalStandardDeviations.general, params.distanceType);
                
                System.out.println("Total outliers = " + String.valueOf(totalOutliers));
            }

            int newTotalEntries = 0;
            for (i=0; i<codebookIn.header.totalLsfEntries; i++)
            {
                if (acceptanceStatus[i]==OutlierStatus.NON_OUTLIER)
                    newTotalEntries++;
            }

            //Write the output codebook
            WeightedCodebookFile codebookOut = new WeightedCodebookFile(codebookFileOut, WeightedCodebookFile.OPEN_FOR_WRITE);

            WeightedCodebookFileHeader headerOut = new WeightedCodebookFileHeader(codebookIn.header);
            headerOut.resetTotalEntries();
            codebookOut.writeCodebookHeader(headerOut);

            for (i=0; i<codebookIn.header.totalLsfEntries; i++)
            {
                if (acceptanceStatus[i]==OutlierStatus.NON_OUTLIER)
                    codebookOut.writeLsfEntry(codebookIn.lsfEntries[i]);
            }

            codebookOut.close();
            //

            System.out.println("Outliers detected = " + String.valueOf(codebookIn.header.totalLsfEntries-newTotalEntries) + " of " + String.valueOf(codebookIn.header.totalLsfEntries));
        }
    }

    //Collect desired features from codebook and call k-means clustering
    private KMeansClusteringTrainer clusterFeatures(WeightedCodebook codebook, int desiredFeatures, int speakerType, KMeansMappingEliminatorParams params, int numClusters)
    {
        KMeansClusteringTrainer clusterer = null;

        double[][] features = codebook.getFeatures(speakerType, desiredFeatures);

        clusterer= new KMeansClusteringTrainer();
        double[] globalVariances = MathUtils.getVarianceCols(features);
        clusterer.cluster(features, numClusters, params.maximumIterations, params.minClusterPercent, params.isDiagonalCovariance, globalVariances);
        features = null; //Memory clean-up

        return clusterer;
    }

    //acceptance status should be initialized properly before calling this function,
    // i.e. it should have the same size as the number of lsf entries in the input codebook
    // all entries should be set to desired values (i.e. OutlierStatus.NON_OUTLIER for first call) since
    // elimination reasons are kept in these entries by summing up in this function
    // eliminationLikelihood should be between 0.0 and 1.0
    private int eliminateLeastLikelyMappings(KMeansClusteringTrainer srcClusterer, 
                                             KMeansClusteringTrainer tgtClusterer, 
                                             int[] acceptanceStatus, 
                                             int desiredFeatures,
                                             int desiredOutlierStatus,
                                             double eliminationLikelihood,
                                             int distanceType)
    {
        int totalOutliers = 0;
        int i, j, k;

        //Find total target clusters for each source cluster and eliminate non-frequent target clusters
        double[][] targetClusterCounts = new double[srcClusterer.clusters.length][]; //Each row correspond to another source cluster
        for (i=0; i<srcClusterer.clusters.length; i++)
        {
            targetClusterCounts[i] = new double[tgtClusterer.clusters.length];
            Arrays.fill(targetClusterCounts[i], 0.0);
        }

        for (i=0; i<srcClusterer.clusterIndices.length; i++)
            targetClusterCounts[srcClusterer.clusterIndices[i]][tgtClusterer.clusterIndices[i]] += 1.0;

        int[] sortedCountIndices = null;
        double threshold;
        double tempSum;
        int index;
        for (i=0; i<srcClusterer.clusters.length; i++)
        {
            sortedCountIndices = MathUtils.quickSort(targetClusterCounts[i]);
            threshold = eliminationLikelihood*MathUtils.sum(targetClusterCounts[i]);
            tempSum = 0.0;
            index = -1;
            for (j=0; j<targetClusterCounts[i].length; j++)
            {
                if (tempSum>=threshold)
                    break;

                tempSum += targetClusterCounts[i][j];
                index++;
            }

            if (index>-1)
            {
                for (j=0; j<=index; j++)
                {
                    for (k=0; k<tgtClusterer.clusterIndices.length; k++)
                    {
                        if (srcClusterer.clusterIndices[k]==i && tgtClusterer.clusterIndices[k]==sortedCountIndices[j])
                        {
                            acceptanceStatus[k] += desiredOutlierStatus;
                            totalOutliers++;
                        }
                    }
                }
            }
        }

        return totalOutliers;
    }

    private int eliminateMeanDistanceMismatches(WeightedCodebook codebookIn,
                                                KMeansClusteringTrainer srcClusterer, 
                                                KMeansClusteringTrainer tgtClusterer,  
                                                int[] acceptanceStatus, 
                                                int desiredFeatures,
                                                int desiredOutlierStatus,
                                                double totalStandardDeviations,
                                                int distanceType,
                                                boolean isGlobalVariance)
    {
        int totalOutliers = 0;
        int i, j, k;

        //If source codebook entry is close to cluster center but corresponding target is not --> One-to-many mapping
        //If target codebook entry is close to cluster center but corresponding source is not --> Many-to-one mapping
        double srcDist, tgtDist;
        double[][] srcFeatures = codebookIn.getFeatures(WeightedCodebook.SOURCE, desiredFeatures);
        double[][] tgtFeatures = codebookIn.getFeatures(WeightedCodebook.SOURCE, desiredFeatures);

        //Compute distance between a hypothetical source cluster boundary vector and the mean vector for thresholding
        double[] boundaryVector = new double[srcFeatures[0].length];
        double[] srcThresholds = new double[srcClusterer.clusters.length];
        for (i=0; i<srcClusterer.clusters.length; i++)
        { 
            for (j=0; j<boundaryVector.length; j++)
            {
                if (isGlobalVariance)
                    boundaryVector[j] = srcClusterer.clusters[i].meanVector[j] + totalStandardDeviations*srcClusterer.covMatrixGlobal[0][j];
                else
                    boundaryVector[j] = srcClusterer.clusters[i].meanVector[j] + totalStandardDeviations*srcClusterer.clusters[i].covMatrix[0][j];
            }

            if (distanceType==DistanceComputer.ABSOLUTE_VALUE_DISTANCE)
                srcThresholds[i] = DistanceComputer.getAbsoluteValueDistance(boundaryVector, srcClusterer.clusters[i].meanVector);
            else if (distanceType==DistanceComputer.EUCLIDEAN_DISTANCE)
                srcThresholds[i] = DistanceComputer.getEuclideanDistance(boundaryVector, srcClusterer.clusters[i].meanVector);
            else if (distanceType==DistanceComputer.NORMALIZED_EUCLIDEAN_DISTANCE)
                srcThresholds[i] = DistanceComputer.getNormalizedEuclideanDistance(boundaryVector, srcClusterer.clusters[i].meanVector, srcClusterer.clusters[i].covMatrix[0]);
            else if (distanceType==DistanceComputer.MAHALANOBIS_DISTANCE)
                srcThresholds[i] = DistanceComputer.getMahalanobisDistance(boundaryVector, srcClusterer.clusters[i].meanVector, srcClusterer.clusters[i].invCovMatrix);
        }

        //Compute distance between a hypothetical target cluster boundary vector and the mean vector for thresholding
        double[] tgtThresholds = new double[tgtClusterer.clusters.length];
        boundaryVector = new double[tgtFeatures[0].length];
        for (i=0; i<srcClusterer.clusters.length; i++)
        {
            for (j=0; j<boundaryVector.length; j++)
            {
                if (isGlobalVariance)
                    boundaryVector[j] = tgtClusterer.clusters[i].meanVector[j] + totalStandardDeviations*tgtClusterer.covMatrixGlobal[0][j];
                else
                    boundaryVector[j] = tgtClusterer.clusters[i].meanVector[j] + totalStandardDeviations*tgtClusterer.clusters[i].covMatrix[0][j];
            }

            if (distanceType==DistanceComputer.ABSOLUTE_VALUE_DISTANCE)
                tgtThresholds[i] = DistanceComputer.getAbsoluteValueDistance(boundaryVector, tgtClusterer.clusters[i].meanVector);
            else if (distanceType==DistanceComputer.EUCLIDEAN_DISTANCE)
                tgtThresholds[i] = DistanceComputer.getEuclideanDistance(boundaryVector, tgtClusterer.clusters[i].meanVector);
            else if (distanceType==DistanceComputer.NORMALIZED_EUCLIDEAN_DISTANCE)
                tgtThresholds[i] = DistanceComputer.getNormalizedEuclideanDistance(boundaryVector, tgtClusterer.clusters[i].meanVector, tgtClusterer.clusters[i].covMatrix[0]);
            else if (distanceType==DistanceComputer.MAHALANOBIS_DISTANCE)
                tgtThresholds[i] = DistanceComputer.getMahalanobisDistance(boundaryVector, tgtClusterer.clusters[i].meanVector, tgtClusterer.clusters[i].invCovMatrix);
        }

        int totalOne2Many = 0;
        int totalMany2One = 0;
        int totalMany2Many = 0;
        srcDist = 0.0;
        tgtDist = 0.0;
        for (i=0; i<srcClusterer.clusterIndices.length; i++)
        {
            if (distanceType==DistanceComputer.ABSOLUTE_VALUE_DISTANCE)
            {
                srcDist = DistanceComputer.getAbsoluteValueDistance(srcFeatures[i], 
                        srcClusterer.clusters[srcClusterer.clusterIndices[i]].meanVector);

                tgtDist = DistanceComputer.getAbsoluteValueDistance(tgtFeatures[i], 
                        tgtClusterer.clusters[tgtClusterer.clusterIndices[i]].meanVector);
            }
            else if (distanceType==DistanceComputer.EUCLIDEAN_DISTANCE)
            {
                srcDist = DistanceComputer.getEuclideanDistance(srcFeatures[i], 
                        srcClusterer.clusters[srcClusterer.clusterIndices[i]].meanVector);

                tgtDist = DistanceComputer.getEuclideanDistance(tgtFeatures[i], 
                        tgtClusterer.clusters[tgtClusterer.clusterIndices[i]].meanVector);
            }
            else if (distanceType==DistanceComputer.NORMALIZED_EUCLIDEAN_DISTANCE)
            {
                if (!isGlobalVariance)
                {
                    srcDist = DistanceComputer.getNormalizedEuclideanDistance(srcFeatures[i], 
                            srcClusterer.clusters[srcClusterer.clusterIndices[i]].meanVector, 
                            srcClusterer.clusters[srcClusterer.clusterIndices[i]].covMatrix[0]);

                    tgtDist = DistanceComputer.getNormalizedEuclideanDistance(tgtFeatures[i], 
                            tgtClusterer.clusters[tgtClusterer.clusterIndices[i]].meanVector, 
                            tgtClusterer.clusters[tgtClusterer.clusterIndices[i]].covMatrix[0]);
                }
                else
                {
                    srcDist = DistanceComputer.getNormalizedEuclideanDistance(srcFeatures[i], 
                            srcClusterer.clusters[srcClusterer.clusterIndices[i]].meanVector, 
                            srcClusterer.covMatrixGlobal[0]);

                    tgtDist = DistanceComputer.getNormalizedEuclideanDistance(tgtFeatures[i], 
                            tgtClusterer.clusters[tgtClusterer.clusterIndices[i]].meanVector, 
                            tgtClusterer.covMatrixGlobal[0]);
                }
            }
            else if (distanceType==DistanceComputer.MAHALANOBIS_DISTANCE)
            {
                if (!isGlobalVariance)
                {
                    srcDist = DistanceComputer.getMahalanobisDistance(srcFeatures[i], 
                            srcClusterer.clusters[srcClusterer.clusterIndices[i]].meanVector, 
                            srcClusterer.clusters[srcClusterer.clusterIndices[i]].invCovMatrix);

                    tgtDist = DistanceComputer.getMahalanobisDistance(tgtFeatures[i], 
                            tgtClusterer.clusters[tgtClusterer.clusterIndices[i]].meanVector, 
                            tgtClusterer.clusters[tgtClusterer.clusterIndices[i]].invCovMatrix);
                }
                else
                {
                    srcDist = DistanceComputer.getMahalanobisDistance(srcFeatures[i], 
                            srcClusterer.clusters[srcClusterer.clusterIndices[i]].meanVector, 
                            srcClusterer.invCovMatrixGlobal);

                    tgtDist = DistanceComputer.getMahalanobisDistance(tgtFeatures[i], 
                            tgtClusterer.clusters[tgtClusterer.clusterIndices[i]].meanVector, 
                            tgtClusterer.invCovMatrixGlobal);
                    
                }
            }

            if (srcDist<srcThresholds[srcClusterer.clusterIndices[i]]  
                                      && tgtDist>=tgtThresholds[tgtClusterer.clusterIndices[i]]) //One-to-many mappings
            {
                acceptanceStatus[i] += OutlierStatus.ONE2MANY_OUTLIER;
                totalOne2Many++;
                totalOutliers++;
            }
            else if (srcDist>=srcThresholds[srcClusterer.clusterIndices[i]] 
                                      && tgtDist<tgtThresholds[tgtClusterer.clusterIndices[i]]) //Many-to-one mapping
            {
                acceptanceStatus[i] += OutlierStatus.MANY2ONE_OUTLIER;
                totalMany2One++;
                totalOutliers++;
            }
            else if (srcDist>=srcThresholds[srcClusterer.clusterIndices[i]] 
                                      && tgtDist>=tgtThresholds[tgtClusterer.clusterIndices[i]]) //Many-to-many mapping
            {
                acceptanceStatus[i] += OutlierStatus.MANY2MANY_OUTLIER;
                totalMany2Many++;
                totalOutliers++;
            }
        }

        System.out.println("(One2Many=" + String.valueOf(totalOne2Many) + " Many2One=" + String.valueOf(totalMany2One) + " Many2Many=" + String.valueOf(totalMany2Many) + ")");

        return totalOutliers;
    }

    private int eliminateUsingSubclusterMeanDistances(WeightedCodebook codebookIn,
                                                      KMeansClusteringTrainer srcClusterer, 
                                                      KMeansClusteringTrainer tgtClusterer,  
                                                      int[] acceptanceStatus, 
                                                      int desiredFeatures,
                                                      int desiredOutlierStatus,
                                                      double totalStandardDeviations,
                                                      int distanceType)
    {
        int totalOutliers = 0;
        

        return totalOutliers;
    }
}
