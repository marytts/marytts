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
    private double[] bestMatchDists;
    private int[] bestMatchIndices;
    private int[] sortedIndicesOfBestMatchIndices;
    double[] weights;
    public WeightedCodebookEntry returnedEntry;
    
    public WeightedCodebookMapper(WeightedCodebookMapperParams mp)
    {
        mapperParams = new WeightedCodebookMapperParams(mp);
        
        if (mapperParams.numBestMatches>0)
        {
            bestMatchDists = new double[mapperParams.numBestMatches];
            bestMatchIndices = new int[mapperParams.numBestMatches];
            sortedIndicesOfBestMatchIndices = new int[mapperParams.numBestMatches];
            returnedEntry = new WeightedCodebookEntry(mapperParams.lpOrder);
        }
        else
        {
            bestMatchDists = null;
            bestMatchIndices = null;
            sortedIndicesOfBestMatchIndices = null;
            returnedEntry = null;
        }
    }
    
    public WeightedCodebookEntry transform(double[] inputLsfs, WeightedCodebook codebook)
    {
        double currentDist;
        double worstBestDist = -1.0;
        int worstBestDistInd = 0;
        int i;

        if (mapperParams.distanceMeasure==WeightedCodebookMapperParams.LSF_EUCLIDEAN_DISTANCE)
        {
            for (i=0; i<codebook.totalEntries; i++)
            {
                if (i>=mapperParams.numBestMatches)
                {
                    currentDist = DistanceComputer.getEuclideanDistance(inputLsfs, codebook.entries[i].sourceItem.lsfs);
                    
                    if (currentDist<worstBestDist)
                    {
                        bestMatchDists[worstBestDistInd] = currentDist;
                        bestMatchIndices[worstBestDistInd] = i;
                        worstBestDistInd = MathUtils.getMaxIndex(bestMatchDists);
                        worstBestDist = bestMatchDists[worstBestDistInd];
                    }
                }
                else
                {
                    bestMatchDists[i] = DistanceComputer.getEuclideanDistance(inputLsfs, codebook.entries[i].sourceItem.lsfs);
                    bestMatchIndices[i] = i; 
                    
                    if (i==0 || bestMatchDists[i]>worstBestDist)
                    {
                        worstBestDist =  bestMatchDists[i];
                        worstBestDistInd = i;
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
                    currentDist = DistanceComputer.getLsfInverseHarmonicDistance(inputLsfs, codebook.entries[i].sourceItem.lsfs);
                    
                    if (currentDist<worstBestDist)
                    {
                        bestMatchDists[worstBestDistInd] = currentDist;
                        bestMatchIndices[worstBestDistInd] = i;
                        worstBestDistInd = MathUtils.getMaxIndex(bestMatchDists);
                        worstBestDist = bestMatchDists[worstBestDistInd];
                    }
                }
                else
                {
                    bestMatchDists[i] = DistanceComputer.getLsfInverseHarmonicDistance(inputLsfs, codebook.entries[i].sourceItem.lsfs);
                    bestMatchIndices[i] = i; 
                    
                    if (i==0 || bestMatchDists[i]>worstBestDist)
                    {
                        worstBestDist =  bestMatchDists[i];
                        worstBestDistInd = i;
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
                    currentDist = DistanceComputer.getLsfInverseHarmonicDistanceSymmetric(inputLsfs, codebook.entries[i].sourceItem.lsfs, mapperParams.alphaForSymmetric);
                    
                    if (currentDist<worstBestDist)
                    {
                        bestMatchDists[worstBestDistInd] = currentDist;
                        bestMatchIndices[worstBestDistInd] = i;
                        worstBestDistInd = MathUtils.getMaxIndex(bestMatchDists);
                        worstBestDist = bestMatchDists[worstBestDistInd];
                    }
                }
                else
                {
                    bestMatchDists[i] = DistanceComputer.getLsfInverseHarmonicDistanceSymmetric(inputLsfs, codebook.entries[i].sourceItem.lsfs, mapperParams.alphaForSymmetric);
                    bestMatchIndices[i] = i; 
                    
                    if (i==0 || bestMatchDists[i]>worstBestDist)
                    {
                        worstBestDist =  bestMatchDists[i];
                        worstBestDistInd = i;
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
                    currentDist = DistanceComputer.getMahalanobisDistance(inputLsfs, codebook.entries[i].sourceItem.lsfs, inverseCovarianceMatrix);
                    
                    if (currentDist<worstBestDist)
                    {
                        bestMatchDists[worstBestDistInd] = currentDist;
                        bestMatchIndices[worstBestDistInd] = i;
                        worstBestDistInd = MathUtils.getMaxIndex(bestMatchDists);
                        worstBestDist = bestMatchDists[worstBestDistInd];
                    }
                }
                else
                {
                    bestMatchDists[i] = DistanceComputer.getMahalanobisDistance(inputLsfs, codebook.entries[i].sourceItem.lsfs, inverseCovarianceMatrix);
                    bestMatchIndices[i] = i; 
                    
                    if (i==0 || bestMatchDists[i]>worstBestDist)
                    {
                        worstBestDist =  bestMatchDists[i];
                        worstBestDistInd = i;
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
                    currentDist = DistanceComputer.getAbsoluteValueDistance(inputLsfs, codebook.entries[i].sourceItem.lsfs);
                    
                    if (currentDist<worstBestDist)
                    {
                        bestMatchDists[worstBestDistInd] = currentDist;
                        bestMatchIndices[worstBestDistInd] = i;
                        worstBestDistInd = MathUtils.getMaxIndex(bestMatchDists);
                        worstBestDist = bestMatchDists[worstBestDistInd];
                    }
                }
                else
                {
                    bestMatchDists[i] = DistanceComputer.getAbsoluteValueDistance(inputLsfs, codebook.entries[i].sourceItem.lsfs);
                    bestMatchIndices[i] = i; 
                    
                    if (i==0 || bestMatchDists[i]>worstBestDist)
                    {
                        worstBestDist =  bestMatchDists[i];
                        worstBestDistInd = i;
                    }
                }
            } 
        }
        else
            return null;
        
        //Get sorted indices of best distances (i.e. lowests) and perform weighting
        //Note that bestMatchDists is not actually sorted, only sorted indices returned!
        sortedIndicesOfBestMatchIndices = MathUtils.quickSort(bestMatchDists, 0, Math.min(mapperParams.numBestMatches, codebook.totalEntries)-1);
        
        //bestMatchIndices[sortedIndicesOfBestMatchIndices[0]] is the best matching codebook entry index
        //bestMatchDists[sortedIndicesOfBestMatchIndices[0]] is the best score
        //codebook.entries[bestMatchIndices[sortedIndicesOfBestMatchIndices[0]]] are the source and target lsf set of the best match
        
        double[] sortedBestMatchDists = new double[Math.min(mapperParams.numBestMatches, codebook.totalEntries)];
        for (i=0; i<Math.min(mapperParams.numBestMatches, codebook.totalEntries); i++)
            sortedBestMatchDists[i] = bestMatchDists[sortedIndicesOfBestMatchIndices[i]];
        
        weights = getWeights(sortedBestMatchDists, Math.min(mapperParams.numBestMatches, codebook.totalEntries), mapperParams.weightingMethod, mapperParams.weightingSteepness);

        int j;
        String strBestIndices = "";
        String strWeights = "";
        Arrays.fill(returnedEntry.sourceItem.lsfs, 0.0);
        Arrays.fill(returnedEntry.targetItem.lsfs, 0.0);
        
        for (i=0; i<Math.min(mapperParams.numBestMatches, codebook.totalEntries); i++)
        {
            for (j=0; j<mapperParams.lpOrder; j++)
            {
                returnedEntry.sourceItem.lsfs[j] += weights[i]*codebook.entries[bestMatchIndices[sortedIndicesOfBestMatchIndices[i]]].sourceItem.lsfs[j];
                returnedEntry.targetItem.lsfs[j] += weights[i]*codebook.entries[bestMatchIndices[sortedIndicesOfBestMatchIndices[i]]].targetItem.lsfs[j];
            }
            
            strBestIndices += String.valueOf(bestMatchIndices[sortedIndicesOfBestMatchIndices[i]]) + " ";
            strWeights += String.valueOf(weights[i]) + " ";
        }
        
        System.out.println("Best entry indices = " + strBestIndices + " with weights = " + strWeights);
        
        return returnedEntry;
    }
    
    public static double[] getWeights(double[] bestDistances, int numBestDistances, int weightingMethod, double steepness)
    {
        double[] outputWeights = MathUtils.normalizeToRange(bestDistances, numBestDistances, 0.0, Math.max(1.0, steepness+1.0));
        
        if (weightingMethod==WeightedCodebookMapperParams.EXPONENTIAL_HALF_WINDOW)
        {
            for (int i=0; i<outputWeights.length; i++)
                outputWeights[i] = Math.exp(-steepness*outputWeights[i]);
        }
        else if (weightingMethod==WeightedCodebookMapperParams.TRIANGLE_HALF_WINDOW)
        {
            for (int i=0; i<outputWeights.length; i++)
                outputWeights[i] = 1.0/Math.pow(outputWeights[i],i*steepness) + (1.0+steepness); 
        }
        
        return MathUtils.normalizeToSumUpTo(outputWeights, 1.0);
    }
    
    public static void main(String[] args)
    {
       
    }
}
