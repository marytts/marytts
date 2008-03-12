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

import de.dfki.lt.mary.unitselection.adaptation.prosody.ProsodyTransformerParams;
import de.dfki.lt.mary.unitselection.adaptation.smoothing.SmoothingDefinitions;
import de.dfki.lt.signalproc.analysis.EnergyFileHeader;
import de.dfki.lt.signalproc.analysis.LsfFileHeader;
import de.dfki.lt.signalproc.analysis.PitchFileHeader;

/**
 * @author oytun.turk
 *
 */
public class WeightedCodebookTransformerParams extends WeightedCodebookBaselineParams {
    public String inputFolder; //Folder of input files to be transformed
    public String outputBaseFolder; //Base folder of output files
    public String outputFolder; //Individual folder of output files (Note that this is automatically generated using parameter values)
    public String outputFolderInfoString; //An information string to be appended as a prefix to the output folder
    public String codebookFile; //Codebook file
    public boolean isSourceToTarget; //if true source is transformed to target, else target is transformed to source
    public boolean isDisplayProcessingFrameCount; //Display processed frame indices while transforming?
    
    public WeightedCodebookMapperParams mapperParams;
    public ProsodyTransformerParams prosodyParams;
    
    public LsfFileHeader lsfParams;
    public PitchFileHeader ptcParams;
    public EnergyFileHeader energyParams;
    
    public boolean isForcedAnalysis;
    public boolean isSourceVocalTractSpectrumFromCodebook;
    public boolean isVocalTractTransformation;
    public boolean isResynthesizeVocalTractFromSourceCodebook;
    public boolean isVocalTractMatchUsingTargetCodebook;
    
    public boolean isSeparateProsody;
    public boolean isSaveVocalTractOnlyVersion;
    public boolean isFixedRateVocalTractConversion;
    
    public boolean isContextBasedPreselection;
    public int totalContextNeighbours;
    
    public boolean isTemporalSmoothing;
    public int smoothingMethod;
    public int smoothingNumNeighbours;
    
    public int smoothingState;
    public String smoothedVocalTractFile;
    
    public WeightedCodebookTransformerParams()
    {
        inputFolder = "";
        outputBaseFolder = "";
        outputFolder = "";
        outputFolderInfoString = "";
        codebookFile = "";
        isSourceToTarget = true;
        isDisplayProcessingFrameCount = false;
            
        mapperParams = new WeightedCodebookMapperParams();
        prosodyParams = new ProsodyTransformerParams();
        lsfParams = new LsfFileHeader();
        ptcParams = new PitchFileHeader();
        energyParams = new EnergyFileHeader();
        
        isForcedAnalysis = false;
        isSourceVocalTractSpectrumFromCodebook = true;
        isVocalTractTransformation = true;
        isResynthesizeVocalTractFromSourceCodebook = false;
        isVocalTractMatchUsingTargetCodebook = false;
        
        isSeparateProsody = true;
        isSaveVocalTractOnlyVersion = true;
        isFixedRateVocalTractConversion = true;
        
        isContextBasedPreselection = false;
        totalContextNeighbours = 0;
        
        isTemporalSmoothing = false;
        smoothingMethod = SmoothingDefinitions.NO_SMOOTHING;
        smoothingNumNeighbours = SmoothingDefinitions.DEFAULT_NUM_NEIGHBOURS;
        
        smoothingState = SmoothingDefinitions.NONE; 
        smoothedVocalTractFile = "";
    }
    
    public WeightedCodebookTransformerParams(WeightedCodebookTransformerParams existing)
    {
        inputFolder = existing.inputFolder;
        outputBaseFolder = existing.outputBaseFolder;
        outputFolder = existing.outputFolder;
        outputFolderInfoString = existing.outputFolderInfoString;
        codebookFile = existing.codebookFile;
        isSourceToTarget = existing.isSourceToTarget;
        isDisplayProcessingFrameCount = existing.isDisplayProcessingFrameCount;
            
        mapperParams = new WeightedCodebookMapperParams(existing.mapperParams);
        prosodyParams = new ProsodyTransformerParams(existing.prosodyParams);
        lsfParams = new LsfFileHeader(existing.lsfParams);
        ptcParams = new PitchFileHeader(existing.ptcParams);
        energyParams = new EnergyFileHeader(existing.energyParams);
        
        isForcedAnalysis = existing.isForcedAnalysis;
        isSourceVocalTractSpectrumFromCodebook = existing.isSourceVocalTractSpectrumFromCodebook;
        isVocalTractTransformation = existing.isVocalTractTransformation;
        isResynthesizeVocalTractFromSourceCodebook = existing.isResynthesizeVocalTractFromSourceCodebook;
        isVocalTractMatchUsingTargetCodebook = existing.isVocalTractMatchUsingTargetCodebook;
        
        isSeparateProsody = existing.isSeparateProsody;
        isSaveVocalTractOnlyVersion = existing.isSaveVocalTractOnlyVersion;
        isFixedRateVocalTractConversion = existing.isFixedRateVocalTractConversion;
        
        isContextBasedPreselection = existing.isContextBasedPreselection;
        totalContextNeighbours = existing.totalContextNeighbours;
        
        isTemporalSmoothing = existing.isTemporalSmoothing;
        smoothingMethod = existing.smoothingMethod;
        smoothingNumNeighbours = existing.smoothingNumNeighbours;
        
        smoothingState = existing.smoothingState;
        smoothedVocalTractFile = existing.smoothedVocalTractFile;
    }
}

