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

package marytts.signalproc.adaptation;

import marytts.signalproc.adaptation.prosody.ProsodyTransformerParams;
import marytts.signalproc.adaptation.smoothing.SmoothingDefinitions;
import marytts.signalproc.analysis.EnergyFileHeader;
import marytts.signalproc.analysis.LsfFileHeader;
import marytts.signalproc.analysis.MfccFileHeader;
import marytts.signalproc.analysis.PitchFileHeader;

/**
 * Baseline class for voice conversion transformation parameters
 * All specific implementations of transformation stage of a given voice conversion algorithm should use
 * a parameter set that is derived from this class
 *
 * @author Oytun T&uumlrk
 */
public class BaselineTransformerParams extends BaselineParams {
    public String inputFolder; //Folder of input files to be transformed
    public String outputBaseFolder; //Base folder of output files
    public String outputFolder; //Individual folder of output files (Note that this is automatically generated using parameter values)
    public String outputFolderInfoString; //An information string to be appended as a prefix to the output folder
    public boolean isSourceToTarget; //if true source is transformed to target, else target is transformed to source
    public boolean isDisplayProcessingFrameCount; //Display processed frame indices while transforming?
   
    public ProsodyTransformerParams prosodyParams;
    
    public LsfFileHeader lsfParams;
    public PitchFileHeader ptcParams;
    public EnergyFileHeader energyParams;
    public MfccFileHeader mfccParams;
    
    public boolean isForcedAnalysis;
    public boolean isVocalTractTransformation;

    public boolean isSeparateProsody;
    public boolean isSaveVocalTractOnlyVersion;
    public boolean isFixedRateVocalTractConversion;
    
    public boolean isTemporalSmoothing;
    public int smoothingMethod;
    public int smoothingNumNeighbours;
    public int smoothingState;
    public String smoothedVocalTractFile;
    
    public boolean isSourceVocalTractSpectrumFromModel;
    public boolean isResynthesizeVocalTractFromSourceModel;
    public boolean isVocalTractMatchUsingTargetModel;
    
    public String pitchMappingFile;
    
    public boolean isPscaleFromFestivalUttFile;
    public boolean isTscaleFromFestivalUttFile;
    public boolean isEscaleFromTargetWavFile;

    public BaselineTransformerParams()
    {
        inputFolder = "";
        outputBaseFolder = "";
        outputFolder = "";
        outputFolderInfoString = "";
        isSourceToTarget = true;
        isDisplayProcessingFrameCount = false;
   
        prosodyParams = new ProsodyTransformerParams();
        lsfParams = new LsfFileHeader();
        ptcParams = new PitchFileHeader();
        energyParams = new EnergyFileHeader();
        mfccParams = new MfccFileHeader();
        
        isForcedAnalysis = false;
        isSourceVocalTractSpectrumFromModel = true;
        isVocalTractTransformation = true;
        
        isResynthesizeVocalTractFromSourceModel = false;
        isVocalTractMatchUsingTargetModel = false;
        
        isSeparateProsody = true;
        isSaveVocalTractOnlyVersion = true;
        isFixedRateVocalTractConversion = true;
        
        isTemporalSmoothing = false;
        smoothingMethod = SmoothingDefinitions.NO_SMOOTHING;
        smoothingNumNeighbours = SmoothingDefinitions.DEFAULT_NUM_NEIGHBOURS;
        smoothingState = SmoothingDefinitions.NONE; 
        smoothedVocalTractFile = "";
        
        isSourceVocalTractSpectrumFromModel = false;
        isResynthesizeVocalTractFromSourceModel = false;
        isVocalTractMatchUsingTargetModel = false;
        
        pitchMappingFile = "";
        
        isPscaleFromFestivalUttFile = false;
        isTscaleFromFestivalUttFile = false;
        isEscaleFromTargetWavFile = false;
    }
    
    public BaselineTransformerParams(BaselineTransformerParams existing)
    {
        inputFolder = existing.inputFolder;
        outputBaseFolder = existing.outputBaseFolder;
        outputFolder = existing.outputFolder;
        outputFolderInfoString = existing.outputFolderInfoString;
        isSourceToTarget = existing.isSourceToTarget;
        isDisplayProcessingFrameCount = existing.isDisplayProcessingFrameCount;
        
        prosodyParams = new ProsodyTransformerParams(existing.prosodyParams);
        lsfParams = new LsfFileHeader(existing.lsfParams);
        ptcParams = new PitchFileHeader(existing.ptcParams);
        energyParams = new EnergyFileHeader(existing.energyParams);
        mfccParams = new MfccFileHeader(existing.mfccParams);
        
        isForcedAnalysis = existing.isForcedAnalysis;
        isVocalTractTransformation = existing.isVocalTractTransformation;
        
        isSeparateProsody = existing.isSeparateProsody;
        isSaveVocalTractOnlyVersion = existing.isSaveVocalTractOnlyVersion;
        isFixedRateVocalTractConversion = existing.isFixedRateVocalTractConversion;
        
        isTemporalSmoothing = existing.isTemporalSmoothing;
        smoothingMethod = existing.smoothingMethod;
        smoothingNumNeighbours = existing.smoothingNumNeighbours;
        smoothingState = existing.smoothingState;
        smoothedVocalTractFile = existing.smoothedVocalTractFile;
        
        isSourceVocalTractSpectrumFromModel = existing.isSourceVocalTractSpectrumFromModel;
        isResynthesizeVocalTractFromSourceModel = existing.isResynthesizeVocalTractFromSourceModel;
        isVocalTractMatchUsingTargetModel = existing.isVocalTractMatchUsingTargetModel; 
        
        pitchMappingFile = existing.pitchMappingFile;
        
        isPscaleFromFestivalUttFile = existing.isPscaleFromFestivalUttFile;
        isTscaleFromFestivalUttFile = existing.isTscaleFromFestivalUttFile;
        isEscaleFromTargetWavFile = existing.isEscaleFromTargetWavFile;
    }
}
