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

import de.dfki.lt.signalproc.util.DistanceComputer;
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
    private int[] bestMatchIndices; //indices in codebook
    private int[] sortedIndicesOfBestMatchIndices; //indices in array bestMatchIndices
    double[] weights;
    public WeightedCodebookMatch returnedMatch;
    
    public WeightedCodebookMapper(WeightedCodebookMapperParams mp)
    {
        mapperParams = new WeightedCodebookMapperParams(mp);
        
        if (mapperParams.numBestMatches>0)
        {
            bestMatchDists = new double[mapperParams.numBestMatches];
            bestMatchIndices = new int[mapperParams.numBestMatches];
            sortedIndicesOfBestMatchIndices = new int[mapperParams.numBestMatches];
        }
        else
        {
            bestMatchDists = null;
            bestMatchIndices = null;
            sortedIndicesOfBestMatchIndices = null;
        }
        
        returnedMatch = null;
    }
    
    //Simple phoneme based selection
    public int[] preselect(String phoneme, WeightedCodebook codebook, boolean isMatchUsingTargetCodebook)
    {
        int[] indices = null;
        int total = 0;
        int i;
        
        if (!isMatchUsingTargetCodebook)
        {
            for (i=0; i<codebook.lsfEntries.length; i++)
            {
                if (phoneme.compareTo(codebook.lsfEntries[i].sourceItem.phn)==0)
                    total++;
            }
        }
        else
        {
            for (i=0; i<codebook.lsfEntries.length; i++)
            {
                if (phoneme.compareTo(codebook.lsfEntries[i].targetItem.phn)==0)
                    total++;
            }
        }
        
        if (total>0)
        {
            indices = new int[total];
            int index = 0;
            if (!isMatchUsingTargetCodebook)
            {
                for (i=0; i<codebook.lsfEntries.length; i++)
                {
                    if (phoneme.compareTo(codebook.lsfEntries[i].sourceItem.phn)==0)
                    {
                        indices[index] = i;
                        index++;
                        
                        if (index>=total)
                            break;
                    }
                }
            }
            else
            {
                for (i=0; i<codebook.lsfEntries.length; i++)
                {
                    if (phoneme.compareTo(codebook.lsfEntries[i].targetItem.phn)==0)
                    {
                        indices[index] = i;
                        index++;
                        
                        if (index>=total)
                            break;
                    }
                }
            }
        }
       
        return indices;
    }
    
    public WeightedCodebookMatch transform(double[] inputLsfs, WeightedCodebook codebook, boolean isVocalTractMatchUsingTargetCodebook, int[] preselectedIndices)
    {
        double currentDist;
        double worstBestDist = -1.0;
        int worstBestDistInd = 0;
        int i;

        if (mapperParams.distanceMeasure==WeightedCodebookMapperParams.LSF_EUCLIDEAN_DISTANCE)
        {
            //for (i=0; i<codebook.header.totalLsfEntries; i++)
            for (i=0; i<preselectedIndices.length; i++)
            {
                if (i>=mapperParams.numBestMatches)
                {
                    if (!isVocalTractMatchUsingTargetCodebook)
                        currentDist = DistanceComputer.getEuclideanDistance(inputLsfs, codebook.lsfEntries[preselectedIndices[i]].sourceItem.lsfs);
                    else
                        currentDist = DistanceComputer.getEuclideanDistance(inputLsfs, codebook.lsfEntries[preselectedIndices[i]].targetItem.lsfs);
                    
                    if (currentDist<worstBestDist)
                    {
                        bestMatchDists[worstBestDistInd] = currentDist;
                        bestMatchIndices[worstBestDistInd] = preselectedIndices[i];
                        worstBestDistInd = MathUtils.getMaxIndex(bestMatchDists);
                        worstBestDist = bestMatchDists[worstBestDistInd];
                    }
                }
                else
                {
                    if (!isVocalTractMatchUsingTargetCodebook)
                        bestMatchDists[i] = DistanceComputer.getEuclideanDistance(inputLsfs, codebook.lsfEntries[preselectedIndices[i]].sourceItem.lsfs);
                    else
                        bestMatchDists[i] = DistanceComputer.getEuclideanDistance(inputLsfs, codebook.lsfEntries[preselectedIndices[i]].targetItem.lsfs);
                    
                    bestMatchIndices[i] = preselectedIndices[i]; 
                    
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
            for (i=0; i<preselectedIndices.length; i++)
            {
                if (i>=mapperParams.numBestMatches)
                {
                    if (!isVocalTractMatchUsingTargetCodebook)
                        currentDist = DistanceComputer.getLsfInverseHarmonicDistance(inputLsfs, codebook.lsfEntries[preselectedIndices[i]].sourceItem.lsfs, mapperParams.freqRange);
                    else
                        currentDist = DistanceComputer.getLsfInverseHarmonicDistance(inputLsfs, codebook.lsfEntries[preselectedIndices[i]].targetItem.lsfs, mapperParams.freqRange);
                    
                    if (currentDist<worstBestDist)
                    {
                        bestMatchDists[worstBestDistInd] = currentDist;
                        bestMatchIndices[worstBestDistInd] = preselectedIndices[i];
                        worstBestDistInd = MathUtils.getMaxIndex(bestMatchDists);
                        worstBestDist = bestMatchDists[worstBestDistInd];
                    }
                }
                else
                {
                    if (!isVocalTractMatchUsingTargetCodebook)
                        bestMatchDists[i] = DistanceComputer.getLsfInverseHarmonicDistance(inputLsfs, codebook.lsfEntries[preselectedIndices[i]].sourceItem.lsfs, mapperParams.freqRange);
                    else
                        bestMatchDists[i] = DistanceComputer.getLsfInverseHarmonicDistance(inputLsfs, codebook.lsfEntries[preselectedIndices[i]].targetItem.lsfs, mapperParams.freqRange);
                    
                    bestMatchIndices[i] = preselectedIndices[i]; 
                    
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
            for (i=0; i<preselectedIndices.length; i++)
            {
                if (i>=mapperParams.numBestMatches)
                {
                    if (!isVocalTractMatchUsingTargetCodebook)
                        currentDist = DistanceComputer.getLsfInverseHarmonicDistanceSymmetric(inputLsfs, codebook.lsfEntries[preselectedIndices[i]].sourceItem.lsfs, mapperParams.alphaForSymmetric, mapperParams.freqRange);
                    else
                        currentDist = DistanceComputer.getLsfInverseHarmonicDistanceSymmetric(inputLsfs, codebook.lsfEntries[preselectedIndices[i]].targetItem.lsfs, mapperParams.alphaForSymmetric, mapperParams.freqRange);
                    
                    if (currentDist<worstBestDist)
                    {
                        bestMatchDists[worstBestDistInd] = currentDist;
                        bestMatchIndices[worstBestDistInd] = preselectedIndices[i];
                        worstBestDistInd = MathUtils.getMaxIndex(bestMatchDists);
                        worstBestDist = bestMatchDists[worstBestDistInd];
                    }
                }
                else
                {
                    if (!isVocalTractMatchUsingTargetCodebook)
                        bestMatchDists[i] = DistanceComputer.getLsfInverseHarmonicDistanceSymmetric(inputLsfs, codebook.lsfEntries[preselectedIndices[i]].sourceItem.lsfs, mapperParams.alphaForSymmetric, mapperParams.freqRange);
                    else
                        bestMatchDists[i] = DistanceComputer.getLsfInverseHarmonicDistanceSymmetric(inputLsfs, codebook.lsfEntries[preselectedIndices[i]].targetItem.lsfs, mapperParams.alphaForSymmetric, mapperParams.freqRange);
                        
                    bestMatchIndices[i] = preselectedIndices[i]; 
                    
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
            
            for (i=0; i<preselectedIndices.length; i++)
            {
                if (i>=mapperParams.numBestMatches)
                {
                    if (!isVocalTractMatchUsingTargetCodebook)
                        currentDist = DistanceComputer.getMahalanobisDistance(inputLsfs, codebook.lsfEntries[preselectedIndices[i]].sourceItem.lsfs, inverseCovarianceMatrix);
                    else
                        currentDist = DistanceComputer.getMahalanobisDistance(inputLsfs, codebook.lsfEntries[preselectedIndices[i]].targetItem.lsfs, inverseCovarianceMatrix);
                    
                    if (currentDist<worstBestDist)
                    {
                        bestMatchDists[worstBestDistInd] = currentDist;
                        bestMatchIndices[worstBestDistInd] = preselectedIndices[i];
                        worstBestDistInd = MathUtils.getMaxIndex(bestMatchDists);
                        worstBestDist = bestMatchDists[worstBestDistInd];
                    }
                }
                else
                {
                    if (!isVocalTractMatchUsingTargetCodebook)
                        bestMatchDists[i] = DistanceComputer.getMahalanobisDistance(inputLsfs, codebook.lsfEntries[preselectedIndices[i]].sourceItem.lsfs, inverseCovarianceMatrix);
                    else
                        bestMatchDists[i] = DistanceComputer.getMahalanobisDistance(inputLsfs, codebook.lsfEntries[preselectedIndices[i]].targetItem.lsfs, inverseCovarianceMatrix);
                    
                    bestMatchIndices[i] = preselectedIndices[i]; 
                    
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
            
            for (i=0; i<preselectedIndices.length; i++)
            {
                if (i>=mapperParams.numBestMatches)
                {
                    if (!isVocalTractMatchUsingTargetCodebook)
                        currentDist = DistanceComputer.getAbsoluteValueDistance(inputLsfs, codebook.lsfEntries[preselectedIndices[i]].sourceItem.lsfs);
                    else
                        currentDist = DistanceComputer.getAbsoluteValueDistance(inputLsfs, codebook.lsfEntries[preselectedIndices[i]].targetItem.lsfs);
                    
                    if (currentDist<worstBestDist)
                    {
                        bestMatchDists[worstBestDistInd] = currentDist;
                        bestMatchIndices[worstBestDistInd] = preselectedIndices[i];
                        worstBestDistInd = MathUtils.getMaxIndex(bestMatchDists);
                        worstBestDist = bestMatchDists[worstBestDistInd];
                    }
                }
                else
                {
                    if (!isVocalTractMatchUsingTargetCodebook)
                        bestMatchDists[i] = DistanceComputer.getAbsoluteValueDistance(inputLsfs, codebook.lsfEntries[preselectedIndices[i]].sourceItem.lsfs);
                    else
                        bestMatchDists[i] = DistanceComputer.getAbsoluteValueDistance(inputLsfs, codebook.lsfEntries[preselectedIndices[i]].targetItem.lsfs);
                            
                    bestMatchIndices[i] = preselectedIndices[i]; 
                    
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
        sortedIndicesOfBestMatchIndices = MathUtils.quickSort(bestMatchDists, 0, Math.min(mapperParams.numBestMatches, codebook.header.totalLsfEntries)-1);
        
        //bestMatchIndices[sortedIndicesOfBestMatchIndices[0]] is the best matching codebook entry index
        //bestMatchDists[0] is the best score
        //codebook.entries[bestMatchIndices[sortedIndicesOfBestMatchIndices[0]]] are the source and target lsf set of the best match
        
        weights = getWeights(bestMatchDists, Math.min(mapperParams.numBestMatches, codebook.header.totalLsfEntries), mapperParams.weightingMethod, mapperParams.weightingSteepness);

        int j;
        String strBestIndices = "";
        String strWeights = "";
        
        returnedMatch = new WeightedCodebookMatch(Math.min(mapperParams.numBestMatches, codebook.header.totalLsfEntries), mapperParams.lpOrder);
        Arrays.fill(returnedMatch.entry.sourceItem.lsfs, 0.0);
        Arrays.fill(returnedMatch.entry.targetItem.lsfs, 0.0);
        
        for (i=0; i<returnedMatch.totalMatches; i++)
        {
            returnedMatch.weights[i] = weights[i];
            returnedMatch.indices[i] = bestMatchIndices[sortedIndicesOfBestMatchIndices[i]];
            
            for (j=0; j<mapperParams.lpOrder; j++)
            {
                returnedMatch.entry.sourceItem.lsfs[j] += returnedMatch.weights[i]*codebook.lsfEntries[returnedMatch.indices[i]].sourceItem.lsfs[j];
                returnedMatch.entry.targetItem.lsfs[j] += returnedMatch.weights[i]*codebook.lsfEntries[returnedMatch.indices[i]].targetItem.lsfs[j];
            }
            
            strBestIndices += String.valueOf(returnedMatch.indices[i]) + " ";
            strWeights += String.valueOf(returnedMatch.weights[i]) + " ";
            
            if (i>0 && weights[i]>weights[i-1])
                System.out.println("Weight should be less than prev weight!!!");
        }
        
        System.out.println("Best entry indices = " + strBestIndices + " with weights = " + strWeights);
        
        return returnedMatch;
    }
    
    public WeightedCodebookMatch transform(double[] inputLsfs, WeightedCodebook codebook, boolean isVocalTractMatchUsingTargetCodebook)
    {
        int[] allIndices = new int[codebook.lsfEntries.length];
        for (int i=0; i<allIndices.length; i++)
            allIndices[i] = i;
        
        return transform(inputLsfs, codebook, isVocalTractMatchUsingTargetCodebook, allIndices);
    }
    
    public WeightedCodebookMatch transformOld(double[] inputLsfs, WeightedCodebook codebook, boolean isVocalTractMatchUsingTargetCodebook)
    {
        double currentDist;
        double worstBestDist = -1.0;
        int worstBestDistInd = 0;
        int i;

        if (mapperParams.distanceMeasure==WeightedCodebookMapperParams.LSF_EUCLIDEAN_DISTANCE)
        {
            for (i=0; i<codebook.header.totalLsfEntries; i++)
            {
                if (i>=mapperParams.numBestMatches)
                {
                    if (!isVocalTractMatchUsingTargetCodebook)
                        currentDist = DistanceComputer.getEuclideanDistance(inputLsfs, codebook.lsfEntries[i].sourceItem.lsfs);
                    else
                        currentDist = DistanceComputer.getEuclideanDistance(inputLsfs, codebook.lsfEntries[i].targetItem.lsfs);
                    
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
                    if (!isVocalTractMatchUsingTargetCodebook)
                        bestMatchDists[i] = DistanceComputer.getEuclideanDistance(inputLsfs, codebook.lsfEntries[i].sourceItem.lsfs);
                    else
                        bestMatchDists[i] = DistanceComputer.getEuclideanDistance(inputLsfs, codebook.lsfEntries[i].targetItem.lsfs);
                    
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
            for (i=0; i<codebook.header.totalLsfEntries; i++)
            {
                if (i>=mapperParams.numBestMatches)
                {
                    if (!isVocalTractMatchUsingTargetCodebook)
                        currentDist = DistanceComputer.getLsfInverseHarmonicDistance(inputLsfs, codebook.lsfEntries[i].sourceItem.lsfs, mapperParams.freqRange);
                    else
                        currentDist = DistanceComputer.getLsfInverseHarmonicDistance(inputLsfs, codebook.lsfEntries[i].targetItem.lsfs, mapperParams.freqRange);
                    
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
                    if (!isVocalTractMatchUsingTargetCodebook)
                        bestMatchDists[i] = DistanceComputer.getLsfInverseHarmonicDistance(inputLsfs, codebook.lsfEntries[i].sourceItem.lsfs, mapperParams.freqRange);
                    else
                        bestMatchDists[i] = DistanceComputer.getLsfInverseHarmonicDistance(inputLsfs, codebook.lsfEntries[i].targetItem.lsfs, mapperParams.freqRange);
                    
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
            for (i=0; i<codebook.header.totalLsfEntries; i++)
            {
                if (i>=mapperParams.numBestMatches)
                {
                    if (!isVocalTractMatchUsingTargetCodebook)
                        currentDist = DistanceComputer.getLsfInverseHarmonicDistanceSymmetric(inputLsfs, codebook.lsfEntries[i].sourceItem.lsfs, mapperParams.alphaForSymmetric, mapperParams.freqRange);
                    else
                        currentDist = DistanceComputer.getLsfInverseHarmonicDistanceSymmetric(inputLsfs, codebook.lsfEntries[i].targetItem.lsfs, mapperParams.alphaForSymmetric, mapperParams.freqRange);
                    
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
                    if (!isVocalTractMatchUsingTargetCodebook)
                        bestMatchDists[i] = DistanceComputer.getLsfInverseHarmonicDistanceSymmetric(inputLsfs, codebook.lsfEntries[i].sourceItem.lsfs, mapperParams.alphaForSymmetric, mapperParams.freqRange);
                    else
                        bestMatchDists[i] = DistanceComputer.getLsfInverseHarmonicDistanceSymmetric(inputLsfs, codebook.lsfEntries[i].targetItem.lsfs, mapperParams.alphaForSymmetric, mapperParams.freqRange);
                        
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
            
            for (i=0; i<codebook.header.totalLsfEntries; i++)
            {
                if (i>=mapperParams.numBestMatches)
                {
                    if (!isVocalTractMatchUsingTargetCodebook)
                        currentDist = DistanceComputer.getMahalanobisDistance(inputLsfs, codebook.lsfEntries[i].sourceItem.lsfs, inverseCovarianceMatrix);
                    else
                        currentDist = DistanceComputer.getMahalanobisDistance(inputLsfs, codebook.lsfEntries[i].targetItem.lsfs, inverseCovarianceMatrix);
                    
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
                    if (!isVocalTractMatchUsingTargetCodebook)
                        bestMatchDists[i] = DistanceComputer.getMahalanobisDistance(inputLsfs, codebook.lsfEntries[i].sourceItem.lsfs, inverseCovarianceMatrix);
                    else
                        bestMatchDists[i] = DistanceComputer.getMahalanobisDistance(inputLsfs, codebook.lsfEntries[i].targetItem.lsfs, inverseCovarianceMatrix);
                    
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
            
            for (i=0; i<codebook.header.totalLsfEntries; i++)
            {
                if (i>=mapperParams.numBestMatches)
                {
                    if (!isVocalTractMatchUsingTargetCodebook)
                        currentDist = DistanceComputer.getAbsoluteValueDistance(inputLsfs, codebook.lsfEntries[i].sourceItem.lsfs);
                    else
                        currentDist = DistanceComputer.getAbsoluteValueDistance(inputLsfs, codebook.lsfEntries[i].targetItem.lsfs);
                    
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
                    if (!isVocalTractMatchUsingTargetCodebook)
                        bestMatchDists[i] = DistanceComputer.getAbsoluteValueDistance(inputLsfs, codebook.lsfEntries[i].sourceItem.lsfs);
                    else
                        bestMatchDists[i] = DistanceComputer.getAbsoluteValueDistance(inputLsfs, codebook.lsfEntries[i].targetItem.lsfs);
                            
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
        sortedIndicesOfBestMatchIndices = MathUtils.quickSort(bestMatchDists, 0, Math.min(mapperParams.numBestMatches, codebook.header.totalLsfEntries)-1);
        
        //bestMatchIndices[sortedIndicesOfBestMatchIndices[0]] is the best matching codebook entry index
        //bestMatchDists[0] is the best score
        //codebook.entries[bestMatchIndices[sortedIndicesOfBestMatchIndices[0]]] are the source and target lsf set of the best match
        
        weights = getWeights(bestMatchDists, Math.min(mapperParams.numBestMatches, codebook.header.totalLsfEntries), mapperParams.weightingMethod, mapperParams.weightingSteepness);

        int j;
        String strBestIndices = "";
        String strWeights = "";
        
        returnedMatch = new WeightedCodebookMatch(Math.min(mapperParams.numBestMatches, codebook.header.totalLsfEntries), mapperParams.lpOrder);
        Arrays.fill(returnedMatch.entry.sourceItem.lsfs, 0.0);
        Arrays.fill(returnedMatch.entry.targetItem.lsfs, 0.0);
        
        for (i=0; i<returnedMatch.totalMatches; i++)
        {
            returnedMatch.weights[i] = weights[i];
            returnedMatch.indices[i] = bestMatchIndices[sortedIndicesOfBestMatchIndices[i]];
            
            for (j=0; j<mapperParams.lpOrder; j++)
            {
                returnedMatch.entry.sourceItem.lsfs[j] += returnedMatch.weights[i]*codebook.lsfEntries[returnedMatch.indices[i]].sourceItem.lsfs[j];
                returnedMatch.entry.targetItem.lsfs[j] += returnedMatch.weights[i]*codebook.lsfEntries[returnedMatch.indices[i]].targetItem.lsfs[j];
            }
            
            strBestIndices += String.valueOf(returnedMatch.indices[i]) + " ";
            strWeights += String.valueOf(returnedMatch.weights[i]) + " ";
            
            if (i>0 && weights[i]>weights[i-1])
                System.out.println("Weight should be less than prev weight!!!");
        }
        
        System.out.println("Best entry indices = " + strBestIndices + " with weights = " + strWeights);
        
        return returnedMatch;
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
