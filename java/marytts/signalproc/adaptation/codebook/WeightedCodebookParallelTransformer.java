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

import java.io.File;
import java.io.IOException;

import javax.sound.sampled.UnsupportedAudioFileException;

import marytts.signalproc.adaptation.BaselineAdaptationItem;
import marytts.signalproc.adaptation.BaselineAdaptationSet;
import marytts.signalproc.adaptation.BaselineFeatureExtractor;
import marytts.signalproc.adaptation.BaselinePostprocessor;
import marytts.signalproc.adaptation.BaselinePreprocessor;
import marytts.signalproc.adaptation.BaselineTransformerParams;
import marytts.signalproc.adaptation.FdpsolaAdapter;
import marytts.signalproc.adaptation.prosody.PitchMapping;
import marytts.signalproc.adaptation.prosody.PitchMappingFile;
import marytts.signalproc.adaptation.prosody.PitchStatistics;
import marytts.signalproc.adaptation.prosody.PitchTransformationData;
import marytts.signalproc.adaptation.prosody.ProsodyTransformerParams;
import marytts.signalproc.adaptation.smoothing.SmoothingDefinitions;
import marytts.signalproc.analysis.LsfFileHeader;
import marytts.tools.voiceimport.BasenameList;
import marytts.util.io.FileUtils;
import marytts.util.string.StringUtils;


/**
 * 
 * @author oytun.turk
 *
 * This class implements transformation for weighted codebook mapping based voice conversion
 * using parallel training data (i.e. source and target data in pairs of audio recordings which have identical content)
 *
 */
public class WeightedCodebookParallelTransformer extends WeightedCodebookTransformer {
    public WeightedCodebookTransformerParams pa;
    public WeightedCodebookMapper mapper;
    
    private WeightedCodebookFile codebookFile;
    public WeightedCodebook codebook;
    
    private PitchMappingFile pitchMappingFile;
    public PitchMapping pitchMapping;

    public WeightedCodebookParallelTransformer(BaselinePreprocessor pp,
                                               BaselineFeatureExtractor fe,
                                               BaselinePostprocessor po,
                                               WeightedCodebookTransformerParams pa) {
        super(pp, fe, po, pa);
               
        codebook = null;
        mapper = null;
    }
    
    public boolean checkParams() throws IOException
    {
        super.checkParams();
        
        params.inputFolder = StringUtils.checkLastSlash(params.inputFolder);
        params.outputBaseFolder = StringUtils.checkLastSlash(params.outputBaseFolder);
        codebookFile = null;
        
        //Read codebook header only
        if (!FileUtils.exists(params.codebookFile))
        {
            System.out.println("Error: Codebook file " + params.codebookFile + " not found!");
            return false;     
        }
        else //Read lsfParams from the codebook header
        {
            codebookFile = new WeightedCodebookFile(params.codebookFile, WeightedCodebookFile.OPEN_FOR_READ);
            codebook = new WeightedCodebook();
            
            codebook.header = codebookFile.readCodebookHeader();
            params.lsfParams = new LsfFileHeader(codebook.header.lsfParams);
            params.mapperParams.lpOrder = params.lsfParams.dimension;
        }
        //
        
        //Read pitch mapping file header
        if (!FileUtils.exists(params.pitchMappingFile))
        {
            System.out.println("Error: Pitch mapping file " + params.pitchMappingFile + " not found!");
            return false;     
        }
        else //Read pitch mapping info from the pitch mapping file header
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
                                  "_prosody" + String.valueOf(params.prosodyParams.pitchStatisticsType) + "x" + String.valueOf(params.prosodyParams.pitchTransformationMethod);
        }
        else
        {
            params.outputFolder = params.outputBaseFolder + 
                                  "_prosody" + String.valueOf(params.prosodyParams.pitchStatisticsType) + "x" + String.valueOf(params.prosodyParams.pitchTransformationMethod);
        }
            
        if (!FileUtils.isDirectory(params.outputFolder))
        {
            System.out.println("Creating output folder " + params.outputFolder + "...");
            FileUtils.createDirectory(params.outputFolder);
        }
        
        if (!params.isSeparateProsody)
            params.isSaveVocalTractOnlyVersion = false;
        
        return true;
    }
    
    public void transform(BaselineAdaptationSet inputSet, BaselineAdaptationSet outputSet) throws UnsupportedAudioFileException
    {
        System.out.println("Transformation started...");
        
        if (inputSet.items!=null && outputSet.items!=null)
        {
            int numItems = Math.min(inputSet.items.length, outputSet.items.length);
            
            if (numItems>0)
            {
                preprocessor.run(inputSet);
                
                int desiredFeatures = BaselineFeatureExtractor.F0_FEATURES + BaselineFeatureExtractor.ENERGY_FEATURES;
                
                try {
                    featureExtractor.run(inputSet, params, desiredFeatures);
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            
            //Read the codebook
            codebookFile.readCodebookFileExcludingHeader(codebook);
            
            //Read the pitch mapping file
            pitchMappingFile.readPitchMappingFileExcludingHeader(pitchMapping);
            
            //Create a mapper object
            mapper = new WeightedCodebookMapper(params.mapperParams);
            
            //Do the transformations now
            for (int i=0; i<numItems; i++)
            {
                try {
                    transformOneItem(inputSet.items[i], outputSet.items[i], params, mapper, codebook, pitchMapping);
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
                                        WeightedCodebookTransformerParams wctParams,
                                        WeightedCodebookMapper wcMapper,
                                        WeightedCodebook wCodebook,
                                        PitchTransformationData pMap
                                        ) throws UnsupportedAudioFileException, IOException
    {   
        if (wctParams.isFixedRateVocalTractConversion && wctParams.prosodyParams.pitchTransformationMethod!=ProsodyTransformerParams.NO_TRANSFORMATION)
            wctParams.isSeparateProsody = true;
            
        //Desired values should be specified in the following four parameters
        double [] pscales = {1.0};
        double [] tscales = {1.0};
        double [] escales = {1.0};
        double [] vscales = {1.0};
        //
        
        //These are for fixed rate vocal tract transformation: Do not change these!!!
        double [] pscalesNone = {1.0};
        double [] tscalesNone = {1.0};
        double [] escalesNone = {1.0};
        double [] vscalesNone = {1.0};
        boolean noPscaleFromFestivalUttFile = false;
        boolean noTscaleFromFestivalUttFile = false;
        boolean noEscaleFromTargetWavFile = false;
        //
        
        FdpsolaAdapter adapter = null;
        WeightedCodebookTransformerParams currentWctParams = new WeightedCodebookTransformerParams(wctParams);
        
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
                adapter.fdpsolaOnline(wcMapper, wCodebook, pMap); //Call voice conversion version
                
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
                adapter.fdpsolaOnline(wcMapper, wCodebook, pMap); //Call voice conversion version

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
                    currentWctParams.isContextBasedPreselection = false;
                    
                    String tmpInputWavFile = inputItem.audioFile;
                    inputItem.audioFile = firstPassOutputWavFile;
                    
                    adapter = new FdpsolaAdapter(inputItem, outputItem.audioFile, currentWctParams,
                                                 pscales, tscales, escales, vscales);

                    inputItem.audioFile = tmpInputWavFile;
                        
                    adapter.bSilent = true;
                    adapter.fdpsolaOnline(null, wCodebook, pMap);
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
            adapter.fdpsolaOnline(wcMapper, wCodebook, pMap); //Call voice conversion version
        }
    }

    public static void main(String[] args) throws IOException, UnsupportedAudioFileException {
        String emotion = "sad";
        String method = "F";
        String inputFolder = "D:/Oytun/DFKI/voices/Interspeech08/neutral/test_tts_" + emotion;
        String outputBaseFolder = "D:/Oytun/DFKI/voices/Interspeech08_out/neutral2" + emotion + "/neutral2" + emotion + "Out_codebook" + method + "3";
        String baseFile = "D:/Oytun/DFKI/voices/Interspeech08_out/neutral2"+ emotion + "/neutral"+ method + "_X_" + emotion + method + "_200";
        
        /*
        //for method: L
        boolean isSourceVocalTractSpectrumFromModel = true;
        int numBestMatches = 15; // Number of best matches in codebook
        boolean isTemporalSmoothing = false;
        int smoothingNumNeighbours = 1;
        boolean isContextBasedPreselection = false;
        int totalContextNeighbours = 2;
        boolean isPscaleFromFestivalUttFile = true; //false=>mean std dev tfm of pitch, true=>from target CART
        boolean isTscaleFromFestivalUttFile = true;
        */
        
        //for method: F
        boolean isSourceVocalTractSpectrumFromModel = true;
        int numBestMatches = 15; // Number of best matches in codebook
        boolean isTemporalSmoothing = true;
        int smoothingNumNeighbours = 1;
        boolean isContextBasedPreselection = true;
        int totalContextNeighbours = 5;
        
        //Note that these two can be true or false together, not yet implemented separate processing
        boolean isPscaleFromFestivalUttFile = true; //false=>mean std dev tfm of pitch, true=>from target CART
        boolean isTscaleFromFestivalUttFile = true;
        //
        
        String outputFolderInfoString = "isSrc" + String.valueOf(isSourceVocalTractSpectrumFromModel ? 1:0) +
                                        "_nBest" + String.valueOf(numBestMatches) + 
                                        "_smooth" + String.valueOf(isTemporalSmoothing ? 1:0) + "_" + String.valueOf(smoothingNumNeighbours) +
                                        "_context" + String.valueOf(isContextBasedPreselection ? 1:0) + "_" + String.valueOf(totalContextNeighbours) +
                                        "_psUtt" + String.valueOf(isPscaleFromFestivalUttFile ? 1:0)+
                                        "_tsUtt" + String.valueOf(isTscaleFromFestivalUttFile ? 1:0);
        
        mainParametric(inputFolder, outputBaseFolder, baseFile, outputFolderInfoString,
                       isSourceVocalTractSpectrumFromModel,
                       numBestMatches,
                       isTemporalSmoothing, smoothingNumNeighbours, 
                       isContextBasedPreselection, totalContextNeighbours,
                       isPscaleFromFestivalUttFile, isTscaleFromFestivalUttFile);
    }
    
    public static void mainParametric(String inputFolder, String outputBaseFolder, String baseFile, String outputFolderInfoString,
                                      boolean isSourceVocalTractSpectrumFromModel,
                                      int numBestMatches,
                                      boolean isTemporalSmoothing, int smoothingNumNeighbours, 
                                      boolean isContextBasedPreselection, int totalContextNeighbours,
                                      boolean isPscaleFromFestivalUttFile, boolean isTscaleFromFestivalUttFile) throws IOException, UnsupportedAudioFileException
    {
        BaselinePreprocessor pp = new BaselinePreprocessor();
        BaselineFeatureExtractor fe = new BaselineFeatureExtractor();
        BaselinePostprocessor po = new BaselinePostprocessor();
        WeightedCodebookTransformerParams pa = new WeightedCodebookTransformerParams();
        
        pa.isDisplayProcessingFrameCount = true;

        pa.inputFolder = inputFolder;
        pa.outputBaseFolder = outputBaseFolder;
        pa.codebookFile = baseFile + WeightedCodebookFile.DEFAULT_EXTENSION;
        pa.pitchMappingFile = baseFile + PitchMappingFile.DEFAULT_EXTENSION;
        
        pa.outputFolderInfoString = outputFolderInfoString;
        
        //Set codebook mapper parameters
        pa.mapperParams.numBestMatches = numBestMatches; // Number of best matches in codebook
        pa.mapperParams.weightingSteepness = 1.0; // Steepness of weighting function in range [WeightedCodebookMapperParams.MIN_STEEPNESS, WeightedCodebookMapperParams.MAX_STEEPNESS]
        pa.mapperParams.freqRange = 8000.0; //Frequency range to be considered around center freq when matching LSFs (note that center freq is estimated automatically as the middle of most closest LSFs)
        
        // Distance measure for comparing source training and transformation features
        //pa.mapperParams.distanceMeasure = WeightedCodebookMapperParams.LSF_INVERSE_HARMONIC_DISTANCE;
        pa.mapperParams.distanceMeasure = WeightedCodebookMapperParams.LSF_INVERSE_HARMONIC_DISTANCE_SYMMETRIC; pa.mapperParams.alphaForSymmetric = 0.5; //Weighting factor for using weights of two lsf vectors in distance computation relatively. The range is [0.0,1.0]
        //pa.mapperParams.distanceMeasure = WeightedCodebookMapperParams.LSF_EUCLIDEAN_DISTANCE;
        //pa.mapperParams.distanceMeasure = WeightedCodebookMapperParams.LSF_MAHALANOBIS_DISTANCE;
        //pa.mapperParams.distanceMeasure = WeightedCodebookMapperParams.LSF_ABSOLUTE_VALUE_DISTANCE;
        
        // Method for weighting best codebook matches
        pa.mapperParams.weightingMethod = WeightedCodebookMapperParams.EXPONENTIAL_HALF_WINDOW;
        //pa.mapperParams.weightingMethod = WeightedCodebookMapperParams.TRIANGLE_HALF_WINDOW;

        ////Mean and variance of a specific distance measure can be optionally kept in the following
        // two parameters for z-normalization
        pa.mapperParams.distanceMean = 0.0; 
        pa.mapperParams.distanceVariance = 1.0;
        //
        
        pa.isForcedAnalysis = false;
        pa.isSourceVocalTractSpectrumFromModel = isSourceVocalTractSpectrumFromModel;
        pa.isVocalTractTransformation = true;
        pa.isResynthesizeVocalTractFromSourceModel = false;
        pa.isVocalTractMatchUsingTargetModel= false;
        
        pa.isSeparateProsody = true;
        pa.isSaveVocalTractOnlyVersion = true;
        pa.isFixedRateVocalTractConversion = true;
        
        pa.isContextBasedPreselection = isContextBasedPreselection;
        pa.totalContextNeighbours = totalContextNeighbours;
        
        //Prosody transformation
        pa.prosodyParams.pitchStatisticsType = PitchStatistics.STATISTICS_IN_HERTZ;
        //pa.prosodyParams.pitchStatisticsType = PitchStatistics.STATISTICS_IN_LOGHERTZ;
        
        pa.prosodyParams.pitchTransformationMethod = ProsodyTransformerParams.GLOBAL_MEAN;
        pa.prosodyParams.pitchTransformationMethod = ProsodyTransformerParams.GLOBAL_STDDEV;
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
        
        WeightedCodebookParallelTransformer t = new WeightedCodebookParallelTransformer(pp, fe, po, pa);
        t.run();
    }
}