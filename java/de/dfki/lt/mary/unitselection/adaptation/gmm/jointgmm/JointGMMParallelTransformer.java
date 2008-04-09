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

package de.dfki.lt.mary.unitselection.adaptation.gmm.jointgmm;

import java.io.IOException;

import javax.sound.sampled.UnsupportedAudioFileException;

import de.dfki.lt.mary.unitselection.adaptation.BaselineAdaptationItem;
import de.dfki.lt.mary.unitselection.adaptation.BaselineAdaptationSet;
import de.dfki.lt.mary.unitselection.adaptation.FdpsolaAdapter;
import de.dfki.lt.mary.unitselection.adaptation.codebook.WeightedCodebookFeatureExtractor;
import de.dfki.lt.mary.unitselection.adaptation.codebook.WeightedCodebookPostprocessor;
import de.dfki.lt.mary.unitselection.adaptation.codebook.WeightedCodebookPreprocessor;
import de.dfki.lt.mary.unitselection.adaptation.prosody.PitchMapping;
import de.dfki.lt.mary.unitselection.adaptation.prosody.PitchMappingFile;
import de.dfki.lt.mary.unitselection.adaptation.prosody.PitchStatistics;
import de.dfki.lt.mary.unitselection.adaptation.prosody.PitchTransformationData;
import de.dfki.lt.mary.unitselection.adaptation.prosody.ProsodyTransformerParams;
import de.dfki.lt.mary.unitselection.adaptation.smoothing.SmoothingDefinitions;
import de.dfki.lt.mary.unitselection.voiceimport.BasenameList;
import de.dfki.lt.mary.util.FileUtils;
import de.dfki.lt.mary.util.StringUtil;

/**
 * @author oytun.turk
 *
 */
public class JointGMMParallelTransformer extends JointGMMTransformer {
    
    public JointGMMMapper mapper;
    public JointGMM jointGmm;
    
    private PitchMappingFile pitchMappingFile;
    public PitchMapping pitchMapping;

    public JointGMMParallelTransformer(WeightedCodebookPreprocessor pp,
            WeightedCodebookFeatureExtractor fe,
            WeightedCodebookPostprocessor po,
            JointGMMTransformerParams pa) {
        super(pp, fe, po, pa);
        
        jointGmm = null;
        mapper = null;
    }
    
    public void run() throws IOException, UnsupportedAudioFileException
    {
        if (checkParams())
        {
            BaselineAdaptationSet inputSet = getInputSet(params.inputFolder);
            if (inputSet==null)
                System.out.println("No input files found in " + params.inputFolder);
            else
            {
                BaselineAdaptationSet outputSet = getOutputSet(inputSet, params.outputFolder);

                transform(inputSet, outputSet);
            }
        }
    }
    
    public boolean checkParams() throws IOException
    {
        params.inputFolder = StringUtil.checkLastSlash(params.inputFolder);
        params.outputBaseFolder = StringUtil.checkLastSlash(params.outputBaseFolder);
        
        //Read joint GMM file
        if (!FileUtils.exists(params.jointGmmFile))
        {
            System.out.println("Error: Codebook file " + params.jointGmmFile + " not found!");
            return false;     
        }
        else //Read full GMM from the joint GMM file
        {
            jointGmm = new JointGMM(params.jointGmmFile);
            
            //params.lsfParams = new LsfFileHeader(jointGmm.header.lsfParams);
            //params.mapperParams.lpOrder = params.lsfParams.lpOrder;
        }
        //
        
        //Read pitch mapping file
        if (!FileUtils.exists(params.pitchMappingFile))
        {
            System.out.println("Error: Pitch mapping file " + params.pitchMappingFile + " not found!");
            return false;     
        }
        else //Read lsfParams from the codebook header
        {
            pitchMappingFile = new PitchMappingFile(params.pitchMappingFile, PitchMappingFile.OPEN_FOR_READ);
            pitchMapping = new PitchMapping();
            
            pitchMapping.header = pitchMappingFile.readPitchMappingHeader();
        }
        //
            
        if (!FileUtils.exists(params.inputFolder) || !FileUtils.isDirectory(params.inputFolder))
        {
            System.out.println("Error: Input folder " + params.inputFolder + " not found!");
            return false; 
        }
        
        if (!FileUtils.isDirectory(params.outputBaseFolder))
        {
            System.out.println("Creating output base folder " + params.outputBaseFolder + "...");
            FileUtils.createDirectory(params.outputBaseFolder);
        }
        
        if (params.outputFolderInfoString!="")
        {
            params.outputFolder = params.outputBaseFolder + params.outputFolderInfoString + 
                                  "_mixes" + String.valueOf(jointGmm.source.totalComponents) + 
                                  "_prosody" + String.valueOf(params.prosodyParams.pitchStatisticsType) + "x" + String.valueOf(params.prosodyParams.pitchTransformationMethod);
        }
        else
        {
            params.outputFolder = params.outputBaseFolder + 
                                  "_mixes" + String.valueOf(jointGmm.source.totalComponents) + 
                                  "_prosody" + String.valueOf(params.prosodyParams.pitchStatisticsType) + "x" + String.valueOf(params.prosodyParams.pitchTransformationMethod);
        }
            
        if (!FileUtils.isDirectory(params.outputFolder))
        {
            System.out.println("Creating output folder " + params.outputFolder + "...");
            FileUtils.createDirectory(params.outputFolder);
        }
        
        return true;
    }
    
    //Create list of input files
    public BaselineAdaptationSet getInputSet(String inputFolder)
    {   
        BasenameList b = new BasenameList(inputFolder, wavExt);
        
        BaselineAdaptationSet inputSet = new BaselineAdaptationSet(b.getListAsVector().size());
        
        for (int i=0; i<inputSet.items.length; i++)
            inputSet.items[i].setFromWavFilename(inputFolder + b.getName(i) + wavExt);
        
        return inputSet;
    }
    //
    
    //Create list of output files using input set
    public BaselineAdaptationSet getOutputSet(BaselineAdaptationSet inputSet, String outputFolder)
    {   
        BaselineAdaptationSet outputSet  = null;

        outputFolder = StringUtil.checkLastSlash(outputFolder);
        
        if (inputSet!=null && inputSet.items!=null)
        {
            outputSet = new BaselineAdaptationSet(inputSet.items.length);

            for (int i=0; i<inputSet.items.length; i++)
                outputSet.items[i].audioFile = outputFolder + StringUtil.getFileName(inputSet.items[i].audioFile) + "_output" + wavExt;
        }

        return outputSet;
    }
    //
    
    
    public void transform(BaselineAdaptationSet inputSet, BaselineAdaptationSet outputSet) throws UnsupportedAudioFileException
    {
        System.out.println("Transformation started...");
        
        if (inputSet.items!=null && outputSet.items!=null)
        {
            int numItems = Math.min(inputSet.items.length, outputSet.items.length);
            
            if (numItems>0)
            {
                preprocessor.run(inputSet);
                
                int desiredFeatures = WeightedCodebookFeatureExtractor.F0_FEATURES;
                
                try {
                    featureExtractor.run(inputSet, params, desiredFeatures);
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            
            //Create a mapper object
            mapper = new JointGMMMapper();
            
            //Do the transformations now
            for (int i=0; i<numItems; i++)
            {
                try {
                    transformOneItem(inputSet.items[i], outputSet.items[i], params, mapper, jointGmm, pitchMapping);
                } catch (UnsupportedAudioFileException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                
                System.out.println("Transformed file " + String.valueOf(i+1) + " of "+ String.valueOf(numItems));
            }
        } 
        
        System.out.println("Transformation completed...");
    }
    
    //This function performs the actual voice conversion
    public static void transformOneItem(BaselineAdaptationItem inputItem, 
                                        BaselineAdaptationItem outputItem,
                                        JointGMMTransformerParams wctParams,
                                        JointGMMMapper jgMapper,
                                        JointGMM wCodebook,
                                        PitchTransformationData pMap
                                       ) throws UnsupportedAudioFileException, IOException
    {   
        if (wctParams.isFixedRateVocalTractConversion)
            wctParams.isSeparateProsody = true;

//      Desired values should be specified in the following four parameters
        double [] pscales = {1.0};
        double [] tscales = {1.0};
        double [] escales = {1.0};
        double [] vscales = {1.0};


//      These are for fixed rate vocal tract transformation: Do not change these!!!
        double [] pscalesTemp = {1.0};
        double [] tscalesTemp = {1.0};
        double [] escalesTemp = {1.0};
        double [] vscalesTemp = {1.0};


        FdpsolaAdapter adapter = null;
        JointGMMTransformerParams currentWctParams = new JointGMMTransformerParams(wctParams);

        String firstPassOutputWavFile = "";
        String smoothedVocalTractFile = "";

        if (currentWctParams.isTemporalSmoothing) //Need to do two pass for smoothing
            currentWctParams.isSeparateProsody = true;


        if (currentWctParams.isSeparateProsody)
        {
            firstPassOutputWavFile = StringUtil.getFolderName(outputItem.audioFile) + StringUtil.getFileName(outputItem.audioFile) + "_vt.wav";
            smoothedVocalTractFile = StringUtil.getFolderName(outputItem.audioFile) + StringUtil.getFileName(outputItem.audioFile) + "_vt.vtf";
            int tmpPitchTransformationMethod = currentWctParams.prosodyParams.pitchTransformationMethod;
            currentWctParams.prosodyParams.pitchTransformationMethod = ProsodyTransformerParams.NO_TRANSFORMATION;

            if (currentWctParams.isTemporalSmoothing) //This estimates the vocal tract filter but performs no prosody and vocal tract transformations
            {
                currentWctParams = new JointGMMTransformerParams(currentWctParams);
                currentWctParams.smoothingState = SmoothingDefinitions.ESTIMATING_SMOOTHED_VOCAL_TRACT;
                currentWctParams.smoothedVocalTractFile = smoothedVocalTractFile; //It is an output at first pass

                adapter = new FdpsolaAdapter(
                        inputItem.audioFile, inputItem.f0File, inputItem.labelFile,
                        firstPassOutputWavFile,
                        currentWctParams,
                        pscalesTemp, tscalesTemp, escalesTemp, vscalesTemp);

                adapter.bSilent = !currentWctParams.isDisplayProcessingFrameCount;
                adapter.fdpsolaOnline(jgMapper, wCodebook, pMap); //Call voice conversion version

                currentWctParams.smoothingState = SmoothingDefinitions.TRANSFORMING_TO_SMOOTHED_VOCAL_TRACT;
                currentWctParams.smoothedVocalTractFile = smoothedVocalTractFile; //Now it is an input
                adapter = new FdpsolaAdapter(
                        inputItem.audioFile, inputItem.f0File, inputItem.labelFile,
                        firstPassOutputWavFile, 
                        currentWctParams,
                        pscalesTemp, tscalesTemp, escalesTemp, vscalesTemp);
            }
            else
            {
                currentWctParams = new JointGMMTransformerParams(currentWctParams);
                currentWctParams.smoothingMethod = SmoothingDefinitions.NO_SMOOTHING;
                currentWctParams.smoothingState = SmoothingDefinitions.NONE;
                currentWctParams.smoothedVocalTractFile = "";

                adapter = new FdpsolaAdapter(
                        inputItem.audioFile, inputItem.f0File, inputItem.labelFile,
                        firstPassOutputWavFile, 
                        currentWctParams,
                        pscalesTemp, tscalesTemp, escalesTemp, vscalesTemp);
            }

//          Separate prosody modification
            if (adapter!=null)
            {
                adapter.bSilent = !currentWctParams.isDisplayProcessingFrameCount;
                adapter.fdpsolaOnline(jgMapper, wCodebook, pMap); //Call voice conversion version

                if (isScalingsRequired(pscales, tscales, escales, vscales) || tmpPitchTransformationMethod!=ProsodyTransformerParams.NO_TRANSFORMATION)
                {
                    System.out.println("Performing prosody modifications...");

                    currentWctParams = new JointGMMTransformerParams(currentWctParams);
                    currentWctParams.isVocalTractTransformation = false; //isVocalTractTransformation should be false 
                    currentWctParams.isFixedRateVocalTractConversion = false; //isFixedRateVocalTractConversion should be false to enable prosody modifications with FD-PSOLA
                    currentWctParams.isResynthesizeVocalTractFromSourceModel = false; //isResynthesizeVocalTractFromSourceCodebook should be false
                    currentWctParams.isVocalTractMatchUsingTargetModel = false; //isVocalTractMatchUsingTargetCodebook should be false
                    currentWctParams.prosodyParams.pitchTransformationMethod = tmpPitchTransformationMethod;
                    currentWctParams.smoothingMethod = SmoothingDefinitions.NO_SMOOTHING;
                    currentWctParams.smoothingState = SmoothingDefinitions.NONE;
                    currentWctParams.smoothedVocalTractFile = "";

                    adapter = new FdpsolaAdapter(
                            firstPassOutputWavFile, inputItem.f0File, inputItem.labelFile,
                            outputItem.audioFile, 
                            currentWctParams,
                            pscales, tscales, escales, vscales);

                    adapter.bSilent = true;
                    adapter.fdpsolaOnline(null, wCodebook, pMap);
                }
                else //Copy output file
                    FileUtils.copy(firstPassOutputWavFile, outputItem.audioFile);

//              Delete first pass output file
                if (!currentWctParams.isSaveVocalTractOnlyVersion)
                    FileUtils.delete(firstPassOutputWavFile);

                System.out.println("Done...");
            }
        }
        else
        {
            currentWctParams = new JointGMMTransformerParams(currentWctParams);
            currentWctParams.smoothingMethod = SmoothingDefinitions.NO_SMOOTHING;
            currentWctParams.smoothingState = SmoothingDefinitions.NONE;
            currentWctParams.smoothedVocalTractFile = "";

            adapter = new FdpsolaAdapter(
                    inputItem.audioFile, inputItem.f0File, inputItem.labelFile,
                    outputItem.audioFile, 
                    currentWctParams,
                    pscales, tscales, escales, vscales);

            adapter.bSilent = !wctParams.isDisplayProcessingFrameCount;
            adapter.fdpsolaOnline(jgMapper, wCodebook, pMap); //Call voice conversion version
        }
    }
    
    public static boolean isScalingsRequired(double[] pscales, double[] tscales, double[] escales, double[] vscales)
    {
        int i;
        for (i=0; i<pscales.length; i++)
        {
            if (pscales[i]!=1.0)
                return true;
        }
        
        for (i=0; i<tscales.length; i++)
        {
            if (tscales[i]!=1.0)
                return true;
        }
        
        for (i=0; i<escales.length; i++)
        {
            if (escales[i]!=1.0)
                return true;
        }
        
        for (i=0; i<vscales.length; i++)
        {
            if (vscales[i]!=1.0)
                return true;
        }
        
        return false;
    }

    public static void main(String[] args) throws IOException, UnsupportedAudioFileException {
        WeightedCodebookPreprocessor pp = new WeightedCodebookPreprocessor();
        WeightedCodebookFeatureExtractor fe = new WeightedCodebookFeatureExtractor();
        WeightedCodebookPostprocessor po = new WeightedCodebookPostprocessor();
        JointGMMTransformerParams pa = new JointGMMTransformerParams();
        
        pa.isDisplayProcessingFrameCount = true;
        
        pa.inputFolder = "d:\\1\\neutral50\\test1";
        pa.outputBaseFolder = "d:\\1\\neutral_X_angry_50\\neutral2angryOut_jointGMM";
        
        pa.jointGmmFile = "d:\\1\\neutral_X_angry_50\\neutralF_X_angryF.jgf";
        pa.outputFolderInfoString = "labelsGaussKmeans";
        
        //Set codebook mapper parameters
        //pa.mapperParams.numBestMatches = 3; // Number of best matches in codebook
        //pa.mapperParams.weightingSteepness = 1.0; // Steepness of weighting function in range [WeightedCodebookMapperParams.MIN_STEEPNESS, WeightedCodebookMapperParams.MAX_STEEPNESS]
        //pa.mapperParams.freqRange = 8000.0; //Frequency range to be considered around center freq when matching LSFs (note that center freq is estimated automatically as the middle of most closest LSFs)
        
        pa.isForcedAnalysis = false;
        pa.isSourceVocalTractSpectrumFromModel = false;
        pa.isVocalTractTransformation = true;
        pa.isResynthesizeVocalTractFromSourceModel = false;
        pa.isVocalTractMatchUsingTargetModel= false;
        
        pa.isSeparateProsody = true;
        pa.isSaveVocalTractOnlyVersion = true;
        pa.isFixedRateVocalTractConversion = true;
        
        //Prosody transformation
        pa.prosodyParams.pitchStatisticsType = PitchStatistics.STATISTICS_IN_HERTZ;
        //pa.prosodyParams.pitchStatisticsType = PitchStatistics.STATISTICS_IN_LOGHERTZ;
        
        pa.prosodyParams.pitchTransformationMethod = ProsodyTransformerParams.GLOBAL_MEAN;
        //pa.prosodyParams.pitchTransformationMethod = ProsodyTransformerParams.GLOBAL_STDDEV;
        //pa.prosodyParams.pitchTransformationMethod = ProsodyTransformerParams.GLOBAL_RANGE;
        //pa.prosodyParams.pitchTransformationMethod = ProsodyTransformerParams.GLOBAL_SLOPE;
        //pa.prosodyParams.pitchTransformationMethod = ProsodyTransformerParams.GLOBAL_INTERCEPT;
        //pa.prosodyParams.pitchTransformationMethod = ProsodyTransformerParams.GLOBAL_MEAN_STDDEV;
        //pa.prosodyParams.pitchTransformationMethod = ProsodyTransformerParams.GLOBAL_MEAN_SLOPE;
        //pa.prosodyParams.pitchTransformationMethod = ProsodyTransformerParams.GLOBAL_INTERCEPT_STDDEV;
        //pa.prosodyParams.pitchTransformationMethod = ProsodyTransformerParams.GLOBAL_INTERCEPT_SLOPE;
        //pa.prosodyParams.pitchTransformationMethod = ProsodyTransformerParams.SENTENCE_MEAN;
        //pa.prosodyParams.pitchTransformationMethod = ProsodyTransformerParams.SENTENCE_STDDEV;
        //pa.prosodyParams.pitchTransformationMethod = ProsodyTransformerParams.SENTENCE_RANGE;
        //pa.prosodyParams.pitchTransformationMethod = ProsodyTransformerParams.SENTENCE_SLOPE;
        //pa.prosodyParams.pitchTransformationMethod = ProsodyTransformerParams.SENTENCE_INTERCEPT;
        //pa.prosodyParams.pitchTransformationMethod = ProsodyTransformerParams.SENTENCE_MEAN_STDDEV;
        //pa.prosodyParams.pitchTransformationMethod = ProsodyTransformerParams.SENTENCE_MEAN_SLOPE;
        //pa.prosodyParams.pitchTransformationMethod = ProsodyTransformerParams.SENTENCE_INTERCEPT_STDDEV;
        //pa.prosodyParams.pitchTransformationMethod = ProsodyTransformerParams.SENTENCE_INTERCEPT_SLOPE;
        
        pa.prosodyParams.isUseInputMean = false;
        pa.prosodyParams.isUseInputStdDev = false;
        pa.prosodyParams.isUseInputRange = false;
        pa.prosodyParams.isUseInputIntercept = false;
        pa.prosodyParams.isUseInputSlope = false;
        //
        
        //Smoothing
        pa.isTemporalSmoothing = true;
        pa.smoothingNumNeighbours = 3;
        //pa.smoothingMethod = SmoothingDefinitions.OUTPUT_LSFCONTOUR_SMOOTHING;
        //pa.smoothingMethod = SmoothingDefinitions.OUTPUT_VOCALTRACTSPECTRUM_SMOOTHING;
        pa.smoothingMethod = SmoothingDefinitions.TRANSFORMATION_FILTER_SMOOTHING;
        //
        
        JointGMMParallelTransformer t = new JointGMMParallelTransformer(pp, fe, po, pa);
        t.run();
    }

}
