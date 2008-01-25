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

package de.dfki.lt.mary.unitselection.adaptation.codebook;

import java.util.Arrays;

import de.dfki.lt.mary.unitselection.adaptation.DistanceComputer;
import de.dfki.lt.signalproc.util.MathUtils;
import de.dfki.lt.signalproc.util.SignalProcUtils;

/**
 * @author oytun.turk
 *
 * This class performs mapping of acoustic features to be transformed to the codebook
 * entries
 * 
 */
public class WeightedCodebookMapper {
    public WeightedCodebookMapperParams mapperParams;
    private double[] bestMatchScores;
    private int[] bestMatchIndices;
    private int[] sortedIndices;
    double[] weights;
    
    public WeightedCodebookMapper(WeightedCodebookMapperParams mp)
    {
        mapperParams = new WeightedCodebookMapperParams(mp);
        
        if (mapperParams.numBestMatches>0)
        {
            bestMatchScores = new double[mapperParams.numBestMatches];
            bestMatchIndices = new int[mapperParams.numBestMatches];
            sortedIndices = new int[mapperParams.numBestMatches];
        }
        else
        {
            bestMatchScores = null;
            bestMatchIndices = null;
            sortedIndices = null;
        }
    }
    
    public double[] transform(double[] inputLsfs, WeightedCodebook codebook)
    {
        double matchScore;
        double minBestScore = -1.0;
        int minBestScoreInd = 0;
        int i;

        if (mapperParams.distanceMeasure==WeightedCodebookMapperParams.LSF_EUCLIDEAN_DISTANCE)
        {
            for (i=0; i<codebook.totalEntries; i++)
            {
                if (i>=mapperParams.numBestMatches)
                {
                    matchScore = DistanceComputer.getEuclideanDistance(inputLsfs, codebook.entries[i].sourceItem.lsfs);
                    
                    if (matchScore>minBestScore)
                    {
                        bestMatchScores[minBestScoreInd] = matchScore;
                        bestMatchIndices[minBestScoreInd] = i;
                        minBestScoreInd = MathUtils.getMinIndex(bestMatchScores);
                        minBestScore = bestMatchIndices[minBestScoreInd];
                    }
                }
                else
                {
                    bestMatchScores[i] = DistanceComputer.getEuclideanDistance(inputLsfs, codebook.entries[i].sourceItem.lsfs);
                    bestMatchIndices[i] = i; 
                    
                    if (i==0 || minBestScore>bestMatchScores[i])
                    {
                        minBestScore = bestMatchScores[i];
                        minBestScoreInd = i;
                    }
                }
            }
        }
        else if (mapperParams.distanceMeasure==WeightedCodebookMapperParams.LSF_INVERSE_HARMONIC_DISTANCE)
        {
            for (i=0; i<codebook.totalEntries; i++)
            {
                if (i>=mapperParams.numBestMatches)
                {
                    matchScore = DistanceComputer.getLsfInverseHarmonicDistance(inputLsfs, codebook.entries[i].sourceItem.lsfs);
                    
                    if (matchScore>minBestScore)
                    {
                        bestMatchScores[minBestScoreInd] = matchScore;
                        bestMatchIndices[minBestScoreInd] = i;
                        minBestScoreInd = MathUtils.getMinIndex(bestMatchScores);
                        minBestScore = bestMatchIndices[minBestScoreInd];
                    }
                }
                else
                {
                    bestMatchScores[i] = DistanceComputer.getLsfInverseHarmonicDistance(inputLsfs, codebook.entries[i].sourceItem.lsfs);
                    bestMatchIndices[i] = i; 
                    
                    if (i==0 || minBestScore>bestMatchScores[i])
                    {
                        minBestScore = bestMatchScores[i];
                        minBestScoreInd = i;
                    }
                }
            }
        }
        else if (mapperParams.distanceMeasure==WeightedCodebookMapperParams.LSF_INVERSE_HARMONIC_DISTANCE_SYMMETRIC)
        {
            for (i=0; i<codebook.totalEntries; i++)
            {
                if (i>=mapperParams.numBestMatches)
                {
                    matchScore = DistanceComputer.getLsfInverseHarmonicDistanceSymmetric(inputLsfs, codebook.entries[i].sourceItem.lsfs, mapperParams.alphaForSymmetric);
                    
                    if (matchScore>minBestScore)
                    {
                        bestMatchScores[minBestScoreInd] = matchScore;
                        bestMatchIndices[minBestScoreInd] = i;
                        minBestScoreInd = MathUtils.getMinIndex(bestMatchScores);
                        minBestScore = bestMatchIndices[minBestScoreInd];
                    }
                }
                else
                {
                    bestMatchScores[i] = DistanceComputer.getLsfInverseHarmonicDistance(inputLsfs, codebook.entries[i].sourceItem.lsfs);
                    bestMatchIndices[i] = i; 
                    
                    if (i==0 || minBestScore>bestMatchScores[i])
                    {
                        minBestScore = bestMatchScores[i];
                        minBestScoreInd = i;
                    }
                }
            }
        }
        else if (mapperParams.distanceMeasure==WeightedCodebookMapperParams.LSF_MAHALANOBIS_DISTANCE)
        {
            double[][] inverseCovarianceMatrix = null;
            for (i=0; i<codebook.totalEntries; i++)
            {
                if (i>=mapperParams.numBestMatches)
                {
                    matchScore = DistanceComputer.getMahalanobisDistance(inputLsfs, codebook.entries[i].sourceItem.lsfs, inverseCovarianceMatrix);
                    
                    if (matchScore>minBestScore)
                    {
                        bestMatchScores[minBestScoreInd] = matchScore;
                        bestMatchIndices[minBestScoreInd] = i;
                        minBestScoreInd = MathUtils.getMinIndex(bestMatchScores);
                        minBestScore = bestMatchIndices[minBestScoreInd];
                    }
                }
                else
                {
                    bestMatchScores[i] = DistanceComputer.getMahalanobisDistance(inputLsfs, codebook.entries[i].sourceItem.lsfs, inverseCovarianceMatrix);
                    bestMatchIndices[i] = i; 
                    
                    if (i==0 || minBestScore>bestMatchScores[i])
                    {
                        minBestScore = bestMatchScores[i];
                        minBestScoreInd = i;
                    }
                }
            }
        }
        else if (mapperParams.distanceMeasure==WeightedCodebookMapperParams.LSF_ABSOLUTE_VALUE_DISTANCE)
        {
            for (i=0; i<codebook.totalEntries; i++)
            {
                if (i>=mapperParams.numBestMatches)
                {
                    matchScore = DistanceComputer.getAbsoluteValueDistance(inputLsfs, codebook.entries[i].sourceItem.lsfs);
                    
                    if (matchScore>minBestScore)
                    {
                        bestMatchScores[minBestScoreInd] = matchScore;
                        bestMatchIndices[minBestScoreInd] = i;
                        minBestScoreInd = MathUtils.getMinIndex(bestMatchScores);
                        minBestScore = bestMatchIndices[minBestScoreInd];
                    }
                }
                else
                {
                    bestMatchScores[i] = DistanceComputer.getAbsoluteValueDistance(inputLsfs, codebook.entries[i].sourceItem.lsfs);
                    bestMatchIndices[i] = i; 
                    
                    if (i==0 || minBestScore>bestMatchScores[i])
                    {
                        minBestScore = bestMatchScores[i];
                        minBestScoreInd = i;
                    }
                }
            }
        }
        else
            return null;
        
        //Sort the best scores and perform weighting
        sortedIndices = MathUtils.quickSort(bestMatchScores);
        
        //bestMatchIndices[sortedIndices[0]] is the best matching codebook entry index and
        //bestMatchScores[0] is the best score
        
        weights = getWeights(bestMatchScores, mapperParams.weightingMethod, mapperParams.weightingSteepness);

        int j;
        double[] targetLsfs = new double[mapperParams.lpOrder];
        Arrays.fill(targetLsfs, 0.0);
        for (i=0; i<mapperParams.numBestMatches; i++)
        {
            for (j=0; j<mapperParams.lpOrder; j++)
                targetLsfs[j] += weights[i]*codebook.entries[bestMatchIndices[sortedIndices[i]]].targetItem.lsfs[j];
        }
        
        return targetLsfs;
    }
    
    public static double[] getWeights(double[] bestScores, int weightingMethod, double steepness)
    {
        double[] outputWeights = MathUtils.normalizeToSumUpTo(bestScores, 1.0);
        
        if (weightingMethod==WeightedCodebookMapperParams.EXPONENTIAL_HALF_WINDOW)
        {
            for (int i=0; i<outputWeights.length; i++)
                outputWeights[i] = Math.exp(-10.0*steepness*outputWeights[i]);
        }
        else if (weightingMethod==WeightedCodebookMapperParams.GAUSSIAN_HALF_WINDOW)
        {
            for (int i=0; i<outputWeights.length; i++)
                outputWeights[i] = Math.exp(-steepness*outputWeights[i]*outputWeights[i]);   
        }
        else if (weightingMethod==WeightedCodebookMapperParams.TRIANGLE_HALF_WINDOW)
        {
            for (int i=0; i<outputWeights.length; i++)
                outputWeights[i] = 5.0*steepness*outputWeights[i] + (1.0+steepness); 
        }
        
        return MathUtils.normalizeToSumUpTo(outputWeights, 1.0);
    }
    
    public static void main(String[] args)
    {
       
    }
}
