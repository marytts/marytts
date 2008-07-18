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

import java.io.IOException;

import javax.sound.sampled.UnsupportedAudioFileException;

import marytts.machinelearning.ContextualGMMParams;
import marytts.signalproc.adaptation.BaselineAdaptationItem;
import marytts.signalproc.adaptation.BaselineAdaptationSet;
import marytts.signalproc.adaptation.BaselineFeatureExtractor;
import marytts.signalproc.adaptation.BaselinePostprocessor;
import marytts.signalproc.adaptation.BaselinePreprocessor;
import marytts.signalproc.adaptation.BaselineTransformer;
import marytts.signalproc.adaptation.FdpsolaAdapter;
import marytts.signalproc.adaptation.MfccAdapter;
import marytts.signalproc.adaptation.codebook.WeightedCodebookFile;
import marytts.signalproc.adaptation.prosody.PitchMapping;
import marytts.signalproc.adaptation.prosody.PitchMappingFile;
import marytts.signalproc.adaptation.prosody.PitchStatistics;
import marytts.signalproc.adaptation.prosody.PitchTransformationData;
import marytts.signalproc.adaptation.prosody.ProsodyTransformerParams;
import marytts.signalproc.adaptation.smoothing.SmoothingDefinitions;
import marytts.signalproc.analysis.LsfFileHeader;
import marytts.signalproc.analysis.MfccFileHeader;
import marytts.tools.voiceimport.BasenameList;
import marytts.util.io.FileUtils;
import marytts.util.string.StringUtils;

/**
 * 
 * @author oytun.turk
 * 
 * Voice conversion transformation using Joint-GMM approach
 *
 */
public class JointGMMTransformer extends BaselineTransformer {

    public JointGMMTransformerParams params;
    
    public JointGMMMapper mapper;
    public JointGMMSet jointGmmSet;
    
    private PitchMappingFile pitchMappingFile;
    public PitchMapping pitchMapping;

    public JointGMMTransformer(BaselinePreprocessor pp,
            BaselineFeatureExtractor fe,
            BaselinePostprocessor po,
            JointGMMTransformerParams pa) 
    {
        super(pp, fe, po, pa);
        params = new JointGMMTransformerParams(pa);
        
        jointGmmSet = null;
        mapper = null;
    }
    
    public boolean checkParams() throws IOException
    {
        params.inputFolder = StringUtils.checkLastSlash(params.inputFolder);
        params.outputBaseFolder = StringUtils.checkLastSlash(params.outputBaseFolder);
        
        //Read joint GMM file
        JointGMM nonNullGmm = null;
        if (!FileUtils.exists(params.jointGmmFile))
        {
            System.out.println("Error: Codebook file " + params.jointGmmFile + " not found!");
            return false;     
        }
        else //Read full GMM from the joint GMM file
        {
            jointGmmSet = new JointGMMSet(params.jointGmmFile);
            
            assert jointGmmSet.gmms!=null;
            
            for (int i=0; i<jointGmmSet.gmms.length; i++)
            {
                if (jointGmmSet.gmms[i]!=null)
                {
                    nonNullGmm = new JointGMM(jointGmmSet.gmms[i]);
                    break;
                }
            }
            
            if (nonNullGmm!=null)
            {
                if (nonNullGmm.featureType == BaselineFeatureExtractor.LSF_FEATURES)
                    params.lsfParams = new LsfFileHeader((LsfFileHeader)nonNullGmm.featureParams);
                else if (nonNullGmm.featureType == BaselineFeatureExtractor.MFCC_FEATURES_FROM_FILES)
                    params.mfccParams = new MfccFileHeader((MfccFileHeader)nonNullGmm.featureParams);
            }
        }
        
        if (nonNullGmm==null)
        {
            System.out.println("Error! All GMMs are null in " + params.jointGmmFile);
            return false;
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
                                  "_mixes" + String.valueOf(nonNullGmm.source.totalComponents) + 
                                  "_prosody" + String.valueOf(params.prosodyParams.pitchStatisticsType) + "x" + String.valueOf(params.prosodyParams.pitchTransformationMethod); 
        }
        else
        {
            params.outputFolder = params.outputBaseFolder + 
                                  "_mixes" + String.valueOf(nonNullGmm.source.totalComponents) + 
                                  "_prosody" + String.valueOf(params.prosodyParams.pitchStatisticsType) + "x" + String.valueOf(params.prosodyParams.pitchTransformationMethod);
        }
            
        if (!FileUtils.isDirectory(params.outputFolder))
        {
            System.out.println("Creating output folder " + params.outputFolder + "...");
            FileUtils.createDirectory(params.outputFolder);
        }
        
        return true;
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
    
  //Create list of input files
    public BaselineAdaptationSet getInputSet(String inputFolder)
    {   
        BasenameList b = new BasenameList(inputFolder, BaselineAdaptationSet.WAV_EXTENSION_DEFAULT);
        
        BaselineAdaptationSet inputSet = new BaselineAdaptationSet(b.getListAsVector().size());
        
        for (int i=0; i<inputSet.items.length; i++)
            inputSet.items[i].setFromWavFilename(inputFolder + b.getName(i) + BaselineAdaptationSet.WAV_EXTENSION_DEFAULT);
        
        return inputSet;
    }
    //
    
    //Create list of output files using input set
    public BaselineAdaptationSet getOutputSet(BaselineAdaptationSet inputSet, String outputFolder)
    {   
        BaselineAdaptationSet outputSet  = null;

        outputFolder = StringUtils.checkLastSlash(outputFolder);
        
        if (inputSet!=null && inputSet.items!=null)
        {
            outputSet = new BaselineAdaptationSet(inputSet.items.length);

            for (int i=0; i<inputSet.items.length; i++)
            {
                outputSet.items[i].audioFile = outputFolder + StringUtils.getFileName(inputSet.items[i].audioFile) + "_output" + BaselineAdaptationSet.WAV_EXTENSION_DEFAULT;
                outputSet.items[i].rawMfccFile = StringUtils.modifyExtension(outputSet.items[i].audioFile, BaselineAdaptationSet.RAWMFCC_EXTENSION_DEFAULT);
            }
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
                
                int desiredFeatures = BaselineFeatureExtractor.F0_FEATURES;
                
                try {
                    featureExtractor.run(inputSet, params, desiredFeatures);
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            
            //Read the pitch mapping file
            pitchMappingFile.readPitchMappingFileExcludingHeader(pitchMapping);
            
            //Create a mapper object
            mapper = new JointGMMMapper();
            
            //Do the transformations now
            for (int i=0; i<numItems; i++)
            {
                try {
                    transformOneItem(inputSet.items[i], outputSet.items[i], params, mapper, jointGmmSet, pitchMapping);
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
                                        JointGMMSet jgSet,
                                        PitchTransformationData pMap
                                       ) throws UnsupportedAudioFileException, IOException
    {  
        //LSF transformation is done fully from audio to audio
        if (jgSet.gmms[0].featureType==BaselineFeatureExtractor.LSF_FEATURES)
        {
            if (wctParams.isFixedRateVocalTractConversion)
                wctParams.isSeparateProsody = true;

            //Desired values should be specified in the following four parameters
            double[] pscales = {1.0};
            double[] tscales = {1.0};
            double[] escales = {1.0};
            double[] vscales = {1.0};

            //These are for fixed rate vocal tract transformation: Do not change these!!!
            double[] pscalesNone = {1.0};
            double[] tscalesNone = {1.0};
            double[] escalesNone = {1.0};
            double[] vscalesNone = {1.0};
            boolean noPscaleFromFestivalUttFile = false;
            boolean noTscaleFromFestivalUttFile = false;
            boolean noEscaleFromTargetWavFile = false;
            //

            FdpsolaAdapter adapter = null;
            JointGMMTransformerParams currentWctParams = new JointGMMTransformerParams(wctParams);

            String firstPassOutputWavFile = "";
            String smoothedVocalTractFile = "";

            if (currentWctParams.isTemporalSmoothing) //Need to do two pass for smoothing
                currentWctParams.isSeparateProsody = true;

            if (currentWctParams.isSeparateProsody) //First pass with no prosody modifications
            {
                firstPassOutputWavFile = StringUtils.getFolderName(outputItem.audioFile) + StringUtils.getFileName(outputItem.audioFile) + "_vt.wav";
                smoothedVocalTractFile = StringUtils.getFolderName(outputItem.audioFile) + StringUtils.getFileName(outputItem.audioFile) + "_vt.vtf";
                int tmpPitchTransformationMethod = currentWctParams.prosodyParams.pitchTransformationMethod;
                currentWctParams.prosodyParams.pitchTransformationMethod = ProsodyTransformerParams.NO_TRANSFORMATION;

                boolean tmpPscaleFromFestivalUttFile = currentWctParams.isPscaleFromFestivalUttFile;
                boolean tmpTscaleFromFestivalUttFile = currentWctParams.isTscaleFromFestivalUttFile;
                boolean tmpEscaleFromTargetWavFile = currentWctParams.isEscaleFromTargetWavFile;
                currentWctParams.isPscaleFromFestivalUttFile = noPscaleFromFestivalUttFile;
                currentWctParams.isTscaleFromFestivalUttFile = noTscaleFromFestivalUttFile;
                currentWctParams.isEscaleFromTargetWavFile = noEscaleFromTargetWavFile;

                if (currentWctParams.isTemporalSmoothing) //This estimates the vocal tract filter but performs no prosody and vocal tract transformations
                {
                    currentWctParams.smoothingState = SmoothingDefinitions.ESTIMATING_SMOOTHED_VOCAL_TRACT;
                    currentWctParams.smoothedVocalTractFile = smoothedVocalTractFile; //It is an output at first pass

                    adapter = new FdpsolaAdapter(inputItem, firstPassOutputWavFile, currentWctParams,
                                                 pscalesNone, tscalesNone, escalesNone, vscalesNone);

                    adapter.bSilent = !currentWctParams.isDisplayProcessingFrameCount;
                    adapter.fdpsolaOnline(jgMapper, jgSet, pMap); //Call voice conversion version

                    currentWctParams.smoothingState = SmoothingDefinitions.TRANSFORMING_TO_SMOOTHED_VOCAL_TRACT;
                    currentWctParams.smoothedVocalTractFile = smoothedVocalTractFile; //Now it is an input

                    adapter = new FdpsolaAdapter(inputItem, firstPassOutputWavFile, currentWctParams,
                            pscalesNone, tscalesNone, escalesNone, vscalesNone);
                }
                else
                {
                    currentWctParams.smoothingMethod = SmoothingDefinitions.NO_SMOOTHING;
                    currentWctParams.smoothingState = SmoothingDefinitions.NONE;
                    currentWctParams.smoothedVocalTractFile = "";

                    adapter = new FdpsolaAdapter(inputItem, firstPassOutputWavFile, currentWctParams,
                                                 pscalesNone, tscalesNone, escalesNone, vscalesNone);
                }

                currentWctParams.isPscaleFromFestivalUttFile = tmpPscaleFromFestivalUttFile;
                currentWctParams.isTscaleFromFestivalUttFile = tmpTscaleFromFestivalUttFile;
                currentWctParams.isEscaleFromTargetWavFile = tmpEscaleFromTargetWavFile;

                //Then second step: prosody modification (with possible additional vocal tract scaling)
                if (adapter!=null)
                {
                    adapter.bSilent = !currentWctParams.isDisplayProcessingFrameCount;
                    adapter.fdpsolaOnline(jgMapper, jgSet, pMap); //Call voice conversion version

                    if (isScalingsRequired(pscales, tscales, escales, vscales) || tmpPitchTransformationMethod!=ProsodyTransformerParams.NO_TRANSFORMATION)
                    {
                        System.out.println("Performing prosody modifications...");

                        currentWctParams.isVocalTractTransformation = false; //isVocalTractTransformation should be false 
                        currentWctParams.isFixedRateVocalTractConversion = false; //isFixedRateVocalTractConversion should be false to enable prosody modifications with FD-PSOLA
                        currentWctParams.isResynthesizeVocalTractFromSourceModel = false; //isResynthesizeVocalTractFromSourceCodebook should be false
                        currentWctParams.isVocalTractMatchUsingTargetModel = false; //isVocalTractMatchUsingTargetCodebook should be false
                        currentWctParams.prosodyParams.pitchTransformationMethod = tmpPitchTransformationMethod;
                        currentWctParams.smoothingMethod = SmoothingDefinitions.NO_SMOOTHING;
                        currentWctParams.smoothingState = SmoothingDefinitions.NONE;
                        currentWctParams.smoothedVocalTractFile = "";

                        String tmpInputWavFile = inputItem.audioFile;
                        inputItem.audioFile = firstPassOutputWavFile;

                        adapter = new FdpsolaAdapter(inputItem, outputItem.audioFile, currentWctParams,
                                                     pscales, tscales, escales, vscales);

                        inputItem.audioFile = tmpInputWavFile;

                        adapter.bSilent = true;
                        adapter.fdpsolaOnline(null, jgSet, pMap);
                    }
                    else //Copy output file
                        FileUtils.copy(firstPassOutputWavFile, outputItem.audioFile);

                    //Delete first pass output file
                    if (!currentWctParams.isSaveVocalTractOnlyVersion)
                        FileUtils.delete(firstPassOutputWavFile);

                    System.out.println("Done...");
                }
            }
            else //Single-pass prosody+vocal tract transformation and modification
            {
                currentWctParams.smoothingMethod = SmoothingDefinitions.NO_SMOOTHING;
                currentWctParams.smoothingState = SmoothingDefinitions.NONE;
                currentWctParams.smoothedVocalTractFile = "";

                adapter = new FdpsolaAdapter(inputItem, outputItem.audioFile, currentWctParams,
                        pscales, tscales, escales, vscales);

                adapter.bSilent = !wctParams.isDisplayProcessingFrameCount;
                adapter.fdpsolaOnline(jgMapper, jgSet, pMap); //Call voice conversion version
            }
        }
        else if (jgSet.gmms[0].featureType==BaselineFeatureExtractor.MFCC_FEATURES_FROM_FILES)
        {
            //MFCC transformation is done from MFCC file to MFCC file
            MfccAdapter adapter = null;
            JointGMMTransformerParams currentWctParams = new JointGMMTransformerParams(wctParams);

            adapter = new MfccAdapter(inputItem, outputItem.rawMfccFile, currentWctParams);

            //Then second step: vocal tract transformation
            if (adapter!=null)
            {
                adapter.bSilent = !currentWctParams.isDisplayProcessingFrameCount;
                adapter.transformOnline(jgMapper, jgSet); //Call voice conversion version

                System.out.println("Done...");
            }
        }
    }

    public static void main(String[] args) throws IOException, UnsupportedAudioFileException 
    {
        //mainInterspeech2008(args);
        
        mainHmmVoiceConversion(args);
    }
    
    public static void mainHmmVoiceConversion(String[] args) throws UnsupportedAudioFileException, IOException
    {
        String wavBaseFolder = "D:/Oytun/DFKI/voices/hmmVoiceConversionTest/hsmmMfcc_25Dimensional/";
        //String wavBaseFolder = "D:/Oytun/DFKI/voices/hmmVoiceConversionTest/lsp_21Dimensional/";
        //String wavBaseFolder = "D:/Oytun/DFKI/voices/hmmVoiceConversionTest/mellsp_21Dimensional/";
        //String baseFolder = "D:/Oytun/DFKI/voices/hmmVoiceConversionTest/mfcc_25Dimensional/";
        
        String sourceTag = "hmmSource";
        String targetTag = "origTarget";
        String method = "F";
        
        BaselinePreprocessor pp = new BaselinePreprocessor();
        BaselineFeatureExtractor fe = new BaselineFeatureExtractor();
        BaselinePostprocessor po = new BaselinePostprocessor();
        JointGMMTransformerParams pa = new JointGMMTransformerParams();
        
        int numTrainingFiles = 100;
        int i;
        
        boolean isContextualGMMs = false;
        int contextClassificationType = ContextualGMMParams.NO_PHONEME_CLASS; int[] numComponents = {8};
        //int contextClassificationType = ContextualGMMParams.SILENCE_SPEECH; int[] numComponents = {16, 128};
        //int contextClassificationType = ContextualGMMParams.VOWEL_SILENCE_CONSONANT; int[] numComponents = {128, 16, 128};
        //int contextClassificationType = ContextualGMMParams.PHONOLOGY_CLASS; int[] numComponents = {numMixes};
        //int contextClassificationType = ContextualGMMParams.FRICATIVE_GLIDELIQUID_NASAL_PLOSIVE_VOWEL_OTHER; int[] numComponents = {128, 128, 128, 128, 128, 16};
        //int contextClassificationType = ContextualGMMParams.PHONEME_IDENTITY; int[] numComponents = {128};
        
        String inputFolder = wavBaseFolder + "/" + sourceTag + "/test_39/";
        String outputBaseFolder;
        if (!isContextualGMMs)
        {
            outputBaseFolder = wavBaseFolder + "output/gmm" + method + "_" + String.valueOf(numTrainingFiles) + "_" + 
                               String.valueOf(numComponents[0]);
        }
        else
        {
            outputBaseFolder = wavBaseFolder + "output/gmm" + method + "_" + String.valueOf(numTrainingFiles) + "_" + 
                               "context" + String.valueOf(contextClassificationType);
            
            for (i=0; i<numComponents.length; i++)
                outputBaseFolder += "_" + String.valueOf(numComponents[i]);
        }
        
        String baseFile = wavBaseFolder + "output/" + sourceTag + "2" + targetTag + "/" + sourceTag + method + "_X_" + targetTag + method + "_" + String.valueOf(numTrainingFiles);
        
        pa.isForcedAnalysis = false;
        pa.isSourceVocalTractSpectrumFromModel = false;
        pa.isVocalTractTransformation = true;
        pa.isResynthesizeVocalTractFromSourceModel = false;
        pa.isVocalTractMatchUsingTargetModel= false;
        
        //Smoothing
        pa.isTemporalSmoothing = false;
        pa.smoothingNumNeighbours = 1;
        if (!pa.isTemporalSmoothing)
            pa.smoothingNumNeighbours = 0;
        
        //pa.smoothingMethod = SmoothingDefinitions.OUTPUT_LSFCONTOUR_SMOOTHING;
        //pa.smoothingMethod = SmoothingDefinitions.OUTPUT_VOCALTRACTSPECTRUM_SMOOTHING;
        pa.smoothingMethod = SmoothingDefinitions.TRANSFORMATION_FILTER_SMOOTHING;
        //
        
        pa.isDisplayProcessingFrameCount = true;
        
        pa.inputFolder = inputFolder;
        pa.outputBaseFolder = outputBaseFolder;

        if (!isContextualGMMs)
            pa.jointGmmFile = baseFile + "_" + String.valueOf(numComponents[0]) + JointGMMSet.DEFAULT_EXTENSION;
        else
        {
            pa.jointGmmFile = baseFile + "_context" + String.valueOf(contextClassificationType);
            for (i=0; i<numComponents.length; i++)
                pa.jointGmmFile += "_" + String.valueOf(numComponents[i]);
            
            pa.jointGmmFile += JointGMMSet.DEFAULT_EXTENSION;
        }
        
        pa.pitchMappingFile = baseFile + PitchMappingFile.DEFAULT_EXTENSION;
        
        pa.outputFolderInfoString = "isSrc" + String.valueOf(pa.isSourceVocalTractSpectrumFromModel ? 1:0) +
                                    "_smooth" + String.valueOf(pa.isTemporalSmoothing ? 1:0) + 
                                    "_" + String.valueOf(pa.smoothingNumNeighbours);
        
        pa.isSeparateProsody = true;
        pa.isSaveVocalTractOnlyVersion = true;
        pa.isFixedRateVocalTractConversion = true;
        
        //Prosody transformation
        pa.prosodyParams.pitchStatisticsType = PitchStatistics.STATISTICS_IN_HERTZ;
        //pa.prosodyParams.pitchStatisticsType = PitchStatistics.STATISTICS_IN_LOGHERTZ;
        
        //pa.prosodyParams.pitchTransformationMethod = ProsodyTransformerParams.GLOBAL_MEAN;
        pa.prosodyParams.pitchTransformationMethod = ProsodyTransformerParams.GLOBAL_STDDEV;
        
        pa.prosodyParams.isUseInputMean = false;
        pa.prosodyParams.isUseInputStdDev = false;
        pa.prosodyParams.isUseInputRange = false;
        pa.prosodyParams.isUseInputIntercept = false;
        pa.prosodyParams.isUseInputSlope = false;
        //
        
        //TTS tests
        pa.isPscaleFromFestivalUttFile = false;
        pa.isTscaleFromFestivalUttFile = false;
        pa.isEscaleFromTargetWavFile = false;
        //
        
        JointGMMTransformer t = new JointGMMTransformer(pp, fe, po, pa);
        t.run();
    }
    
    public static void mainInterspeech2008(String[] args) throws IOException, UnsupportedAudioFileException 
    {
        String emotion = "angry";
        String method = "F";
        int numTrainingFiles = 200; //2, 20, 200, 350
        int i;
        
        boolean isContextualGMMs = false;
        int contextClassificationType = ContextualGMMParams.NO_PHONEME_CLASS; int[] numComponents = {128};
        //int contextClassificationType = ContextualGMMParams.SILENCE_SPEECH; int[] numComponents = {16, 128};
        //int contextClassificationType = ContextualGMMParams.VOWEL_SILENCE_CONSONANT; int[] numComponents = {128, 16, 128};
        //int contextClassificationType = ContextualGMMParams.PHONOLOGY_CLASS; int[] numComponents = {numMixes};
        //int contextClassificationType = ContextualGMMParams.FRICATIVE_GLIDELIQUID_NASAL_PLOSIVE_VOWEL_OTHER; int[] numComponents = {128, 128, 128, 128, 128, 16};
        //int contextClassificationType = ContextualGMMParams.PHONEME_IDENTITY; int[] numComponents = {128}; 
        
        String inputFolder = "D:/Oytun/DFKI/voices/Interspeech08/neutral/test_tts_" + emotion;
        String outputBaseFolder;
        if (!isContextualGMMs)
        {
            outputBaseFolder = "D:/Oytun/DFKI/voices/Interspeech08_out/neutral2" + emotion + "/neutral2" + emotion + 
                               "Out_gmm" + method + "_" + String.valueOf(numTrainingFiles) + "_" + 
                               String.valueOf(numComponents[0]);
        }
        else
        {

            outputBaseFolder = "D:/Oytun/DFKI/voices/Interspeech08_out/neutral2" + emotion + "/neutral2" + emotion + 
                               "Out_gmm" + method + "_" + String.valueOf(numTrainingFiles) + "_" + 
                               "context" + String.valueOf(contextClassificationType);
            
            for (i=0; i<numComponents.length; i++)
                outputBaseFolder += "_" + String.valueOf(numComponents[i]);
        }
        
        String baseFile = "D:/Oytun/DFKI/voices/Interspeech08_out/neutral2"+ emotion + "/neutral" + method + "_X_" + emotion + method + "_" + String.valueOf(numTrainingFiles);
        
        boolean isSourceVocalTractSpectrumFromModel = false;
        boolean isTemporalSmoothing = false;
        int smoothingNumNeighbours = 1;
        
        //Note that these two can be true or false together, not yet implemented separate processing
        boolean isPscaleFromFestivalUttFile = true; //false=>mean std dev tfm of pitch, true=>from target CART
        boolean isTscaleFromFestivalUttFile = true;
        //
        
        String outputFolderInfoString = "isSrc" + String.valueOf(isSourceVocalTractSpectrumFromModel ? 1:0) +
                                        "_smooth" + String.valueOf(isTemporalSmoothing ? 1:0) + "_" + String.valueOf(smoothingNumNeighbours) +
                                        "_psUtt" + String.valueOf(isPscaleFromFestivalUttFile ? 1:0)+
                                        "_tsUtt" + String.valueOf(isTscaleFromFestivalUttFile ? 1:0);
        
        mainParametric(inputFolder, outputBaseFolder, baseFile, outputFolderInfoString,
                       isSourceVocalTractSpectrumFromModel,
                       isTemporalSmoothing, smoothingNumNeighbours, 
                       isPscaleFromFestivalUttFile, isTscaleFromFestivalUttFile,
                       isContextualGMMs, contextClassificationType, numComponents);
    }
    
    public static void mainParametric(String inputFolder, String outputBaseFolder, String baseFile, String outputFolderInfoString,
                                      boolean isSourceVocalTractSpectrumFromModel,
                                      boolean isTemporalSmoothing, int smoothingNumNeighbours, 
                                      boolean isPscaleFromFestivalUttFile, boolean isTscaleFromFestivalUttFile,
                                      boolean isContextualGMMs, int contextClassificationType, int[] numComponents) throws IOException, UnsupportedAudioFileException
    {
        BaselinePreprocessor pp = new BaselinePreprocessor();
        BaselineFeatureExtractor fe = new BaselineFeatureExtractor();
        BaselinePostprocessor po = new BaselinePostprocessor();
        JointGMMTransformerParams pa = new JointGMMTransformerParams();
        int i;
        
        pa.isDisplayProcessingFrameCount = true;
        
        pa.inputFolder = inputFolder;
        pa.outputBaseFolder = outputBaseFolder;

        if (!isContextualGMMs)
            pa.jointGmmFile = baseFile + "_" + String.valueOf(numComponents[0]) + JointGMMSet.DEFAULT_EXTENSION;
        else
        {
            pa.jointGmmFile = baseFile + "_context" + String.valueOf(contextClassificationType);
            for (i=0; i<numComponents.length; i++)
                pa.jointGmmFile += "_" + String.valueOf(numComponents[i]);
            
            pa.jointGmmFile += JointGMMSet.DEFAULT_EXTENSION;
        }
        
        pa.pitchMappingFile = baseFile + PitchMappingFile.DEFAULT_EXTENSION;
        
        pa.outputFolderInfoString = outputFolderInfoString;
        
        pa.isForcedAnalysis = false;
        pa.isSourceVocalTractSpectrumFromModel = isSourceVocalTractSpectrumFromModel;
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
        pa.isTemporalSmoothing = isTemporalSmoothing;
        pa.smoothingNumNeighbours = smoothingNumNeighbours;
        //pa.smoothingMethod = SmoothingDefinitions.OUTPUT_LSFCONTOUR_SMOOTHING;
        //pa.smoothingMethod = SmoothingDefinitions.OUTPUT_VOCALTRACTSPECTRUM_SMOOTHING;
        pa.smoothingMethod = SmoothingDefinitions.TRANSFORMATION_FILTER_SMOOTHING;
        //
        
        //TTS tests
        pa.isPscaleFromFestivalUttFile = isPscaleFromFestivalUttFile;
        pa.isTscaleFromFestivalUttFile = isTscaleFromFestivalUttFile;
        pa.isEscaleFromTargetWavFile = true;
        //
        
        JointGMMTransformer t = new JointGMMTransformer(pp, fe, po, pa);
        t.run();
    }

}
