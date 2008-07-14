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

package marytts.signalproc.adaptation.codebook;

import marytts.signalproc.adaptation.BaselineFeatureExtractor;
import marytts.signalproc.adaptation.outlier.GMMOutlierEliminator;
import marytts.signalproc.adaptation.outlier.GaussianOutlierEliminator;
import marytts.signalproc.adaptation.outlier.KMeansMappingEliminator;
import marytts.util.io.FileUtils;

/**
 * @author oytun.turk
 *
 */
public class WeightedCodebookOutlierEliminator {
    private GaussianOutlierEliminator gaussian;
    private KMeansMappingEliminator kmeans;
    private GMMOutlierEliminator gmm;
    
    public void run(WeightedCodebookTrainerParams params)
    {
        String tempIn = params.temporaryCodebookFile;
        String tempOut = params.temporaryCodebookFile + "2";
        
        if (!params.gaussianEliminatorParams.isCheckDurationOutliers && 
            !params.gaussianEliminatorParams.isCheckEnergyOutliers && 
            !params.gaussianEliminatorParams.isCheckF0Outliers && 
            !params.gaussianEliminatorParams.isCheckLsfOutliers)
            params.gaussianEliminatorParams.isActive = false;
        
        if (!params.kmeansEliminatorParams.isCheckDurationOutliers && 
            !params.kmeansEliminatorParams.isCheckEnergyOutliers && 
            !params.kmeansEliminatorParams.isCheckF0Outliers && 
            !params.kmeansEliminatorParams.isCheckLsfOutliers)
            params.kmeansEliminatorParams.isActive = false;
            
        if (params.gaussianEliminatorParams.isActive)
        {
            if (!params.kmeansEliminatorParams.isActive)
                tempOut = params.codebookFile;
                
            gaussian = new GaussianOutlierEliminator();
            
            if (params.codebookHeader.vocalTractFeature!=BaselineFeatureExtractor.LSF_FEATURES)
                params.gaussianEliminatorParams.isCheckLsfOutliers = false;
            
            gaussian.eliminate(params.gaussianEliminatorParams, tempIn, tempOut);
        }
        
        if (params.kmeansEliminatorParams.isActive)
        {
            if (params.gaussianEliminatorParams.isActive)
                tempIn = tempOut;

            tempOut = params.codebookFile; //This should be changed if you add more eliminators below
                
            kmeans = new KMeansMappingEliminator();
            
            if (params.codebookHeader.vocalTractFeature!=BaselineFeatureExtractor.LSF_FEATURES)
                params.kmeansEliminatorParams.isCheckLsfOutliers = false;
            
            kmeans.eliminate(params.kmeansEliminatorParams, tempIn, tempOut);
            
            if (params.gaussianEliminatorParams.isActive)
                FileUtils.delete(tempIn);
        }
       
        /*
        if (params.gmmEliminatorParams.isActive)
        {
            gmm = new KMeansOutlierEliminator(params.totalStandardDeviations);
            gmm.eliminate();
        }
        */
        
        //If no outlier elimintor was run, just rename the temporary input file to final codebook file
        if (!params.gaussianEliminatorParams.isActive && !params.kmeansEliminatorParams.isActive)
            FileUtils.rename(tempIn, params.codebookFile);
    }

}
