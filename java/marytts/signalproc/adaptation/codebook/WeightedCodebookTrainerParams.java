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
import marytts.signalproc.adaptation.BaselineTrainerParams;
import marytts.signalproc.adaptation.outlier.GaussianOutlierEliminator;
import marytts.signalproc.adaptation.outlier.GaussianOutlierEliminatorParams;
import marytts.signalproc.adaptation.outlier.KMeansMappingEliminatorParams;
import marytts.signalproc.analysis.LsfFileHeader;

/**
 * @author oytun.turk
 *
 */
public class WeightedCodebookTrainerParams extends BaselineTrainerParams {
    public static final int MAXIMUM_CONTEXT = 10;
    
    public WeightedCodebookFileHeader codebookHeader;
    
    public String trainingBaseFolder; //Training base directory
    public String sourceTrainingFolder; //Source training folder
    public String targetTrainingFolder; //Target training folder
    public String codebookFile; //Source and target codebook file
    public String temporaryCodebookFile; //Temporary codebook file
    
    public String pitchMappingFile; //Source and target pitch mapping file
    
    //Some filename extension for custom training file types
    public String indexMapFileExtension; //Index map file extensions
    
    public boolean isForcedAnalysis; //Set this to true if you want all acoustic features to be extracted even if their files exist
    
    public GaussianOutlierEliminatorParams gaussianEliminatorParams;
    public KMeansMappingEliminatorParams kmeansEliminatorParams;
    
    public int vocalTractFeature; //Feature to be used for representing vocal tract 
    
    public WeightedCodebookTrainerParams()
    {
        codebookHeader = new WeightedCodebookFileHeader();
        
        trainingBaseFolder = ""; //Training base directory
        sourceTrainingFolder = ""; //Source training folder
        targetTrainingFolder = ""; //Target training folder
        codebookFile = "";  //Source and target codebook file
        temporaryCodebookFile = ""; //Temporary codebook file
        
        pitchMappingFile = "";
        
        //Some filename extension for custom training file types
        indexMapFileExtension = ".imf";
        
        isForcedAnalysis = false;
        
        gaussianEliminatorParams = new GaussianOutlierEliminatorParams();
        kmeansEliminatorParams = new KMeansMappingEliminatorParams();
        
        vocalTractFeature = BaselineFeatureExtractor.NOT_DEFINED;
    }
    
    public WeightedCodebookTrainerParams(WeightedCodebookTrainerParams existing)
    {
        codebookHeader = new WeightedCodebookFileHeader(existing.codebookHeader);
        
        trainingBaseFolder = existing.trainingBaseFolder;
        sourceTrainingFolder = existing.sourceTrainingFolder;
        targetTrainingFolder = existing.targetTrainingFolder;
        codebookFile = existing.codebookFile;
        temporaryCodebookFile = existing.temporaryCodebookFile;
        
        pitchMappingFile = existing.pitchMappingFile;

        indexMapFileExtension = existing.indexMapFileExtension;
        
        isForcedAnalysis = existing.isForcedAnalysis;
        
        gaussianEliminatorParams = new GaussianOutlierEliminatorParams(existing.gaussianEliminatorParams);
        kmeansEliminatorParams = new KMeansMappingEliminatorParams(existing.kmeansEliminatorParams);
        
        vocalTractFeature = existing.vocalTractFeature;
    }
}
