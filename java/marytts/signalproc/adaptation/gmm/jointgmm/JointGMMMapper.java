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

package marytts.signalproc.adaptation.gmm.jointgmm;

import java.util.Arrays;

import marytts.signalproc.adaptation.gmm.GMMMapper;
import marytts.signalproc.adaptation.gmm.GMMMatch;
import marytts.util.math.MathUtils;


/**
 * 
 * Implements joint-GMM based speaker feature transformation from source acoustic space to target acoustic space
 *
 * @author Oytun T&uumlrk
 */
public class JointGMMMapper extends GMMMapper {
    
    public JointGMMMapper()
    {
        
    }
    
    //Weights should sum up to unity
    public GMMMatch transform(double[] inputLsfs, JointGMMSet jointGMMSet, double[] weights, boolean isVocalTractMatchUsingTargetModel)
    {
        JointGMMMatch jointGMMMatch = new JointGMMMatch(inputLsfs.length);
        JointGMMMatch tmpGMMMatch = new JointGMMMatch(inputLsfs.length);
        
        int i, n;
        Arrays.fill(jointGMMMatch.mappedSourceFeatures, 0.0);
        Arrays.fill(jointGMMMatch.outputFeatures, 0.0);
        
        for (i=0; i<jointGMMSet.gmms.length; i++)
        {
            if (jointGMMSet.gmms[i]==null)
                weights[i] = 0.0;
        }
        
        weights = MathUtils.normalizeToSumUpTo(weights, 1.0);
        
        for (i=0; i<jointGMMSet.gmms.length; i++)
        {
            if (jointGMMSet.gmms[i]!=null && weights[i]>0.0)
            {
                tmpGMMMatch = transform(inputLsfs, jointGMMSet.gmms[i], isVocalTractMatchUsingTargetModel);
            
                for (n=0; n<inputLsfs.length; n++)
                {
                    jointGMMMatch.mappedSourceFeatures[n] += weights[i]*tmpGMMMatch.mappedSourceFeatures[n];
                    jointGMMMatch.outputFeatures[n] += weights[i]*tmpGMMMatch.outputFeatures[n];
                }
            }
            
        }
        
        return jointGMMMatch;
    }
    
    public JointGMMMatch transform(double[] inputLsfs, JointGMM jointGMM, boolean isVocalTractMatchUsingTargetModel)
    {
        JointGMMMatch jointGMMMatch = new JointGMMMatch(inputLsfs.length);
        
        int i, n;
        double[] h = new double[jointGMM.source.totalComponents];
        double totalP = 0.0;
        for (i=0; i<jointGMM.source.totalComponents; i++)
        {
            h[i] = jointGMM.source.components[i].probability(inputLsfs);
            totalP += h[i];
        }
        
        for (i=0; i<jointGMM.source.totalComponents; i++)
            h[i] = h[i]/totalP;
        
        if (jointGMM.covarianceTerms.isDiagonalCovariance) //Diagonal covariance, covariance terms are just vectors
        {
            Arrays.fill(jointGMMMatch.mappedSourceFeatures, 0.0);
            Arrays.fill(jointGMMMatch.outputFeatures, 0.0);
            
            for (n=0; n<inputLsfs.length; n++)
            {   
                for (i=0; i<jointGMM.source.totalComponents; i++)
                {
                    jointGMMMatch.mappedSourceFeatures[n] += h[i]*jointGMM.source.components[i].meanVector[n];
                    jointGMMMatch.outputFeatures[n] += h[i]*(jointGMM.targetMeans.components[i].meanVector[n] + jointGMM.covarianceTerms.components[i].covMatrix[0][n]*(inputLsfs[n]-jointGMM.source.components[i].meanVector[n]));
                }
            }
        }
        else //Full covariance
        {
            Arrays.fill(jointGMMMatch.mappedSourceFeatures, 0.0);
            Arrays.fill(jointGMMMatch.outputFeatures, 0.0);
            
            double [] tmpMappedSourceLsfs = new double[inputLsfs.length];
            double [] tmpOutputLsfs = new double[inputLsfs.length];

            double[] inputMeanNormalized;
            double[] covarianceTransformed;
            double[] targetMeanAdded;
            
            for (i=0; i<jointGMM.source.totalComponents; i++)
            {
                tmpMappedSourceLsfs = MathUtils.multiply(jointGMM.source.components[i].meanVector, h[i]);
                
                inputMeanNormalized = MathUtils.substract(inputLsfs, jointGMM.source.components[i].meanVector);
                covarianceTransformed = MathUtils.matrixProduct(jointGMM.covarianceTerms.components[i].covMatrix, inputMeanNormalized);
                targetMeanAdded = MathUtils.add(jointGMM.targetMeans.components[i].meanVector, covarianceTransformed);
                tmpOutputLsfs = MathUtils.multiply(targetMeanAdded, h[i]);
            
                for (n=0; n<inputLsfs.length; n++)
                {
                    jointGMMMatch.mappedSourceFeatures[n] += tmpMappedSourceLsfs[n];
                    jointGMMMatch.outputFeatures[n] += tmpOutputLsfs[n];
                }
            }
        }
        
        return jointGMMMatch;
    }
}
