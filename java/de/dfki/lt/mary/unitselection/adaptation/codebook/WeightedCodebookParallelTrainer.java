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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;

import javax.sound.sampled.UnsupportedAudioFileException;

import de.dfki.lt.mary.unitselection.adaptation.AdaptationUtils;
import de.dfki.lt.mary.unitselection.adaptation.BaselineAdaptationItem;
import de.dfki.lt.mary.unitselection.adaptation.BaselineAdaptationSet;
import de.dfki.lt.mary.unitselection.adaptation.IndexMap;
import de.dfki.lt.mary.unitselection.adaptation.outlier.KMeansMappingEliminatorParams;
import de.dfki.lt.mary.unitselection.adaptation.prosody.PitchTrainer;
import de.dfki.lt.mary.unitselection.voiceimport.BasenameList;
import de.dfki.lt.mary.util.FileUtils;
import de.dfki.lt.mary.util.StringUtil;
import de.dfki.lt.signalproc.analysis.Lsfs;
import de.dfki.lt.signalproc.util.MaryRandomAccessFile;
import de.dfki.lt.signalproc.window.Window;

/**
 * @author oytun.turk
 *
 * This class implements training for weighted codebook mapping based voice conversion
 * using parallel training data (i.e. source and target data in pairs of audio recordings which have identical content)
 */
public class WeightedCodebookParallelTrainer extends WeightedCodebookTrainer {
    
    public WeightedCodebookParallelTrainer(WeightedCodebookPreprocessor pp,
                                           WeightedCodebookFeatureExtractor fe,
                                           WeightedCodebookTrainerParams pa) 
    {
        super(pp, fe, pa);
        
        outlierEliminator = new WeightedCodebookOutlierEliminator();
    }
      
    public void run() throws IOException, UnsupportedAudioFileException
    {
        if (checkParams())
        {
            BaselineAdaptationSet sourceTrainingSet = getTrainingSet(params.sourceTrainingFolder);
            BaselineAdaptationSet targetTrainingSet = getTrainingSet(params.targetTrainingFolder);

            train(sourceTrainingSet, targetTrainingSet);
        }
    }
    
    public boolean checkParams()
    {
        boolean bContinue = true;
        
        params.trainingBaseFolder = StringUtil.checkLastSlash(params.trainingBaseFolder);
        params.sourceTrainingFolder = StringUtil.checkLastSlash(params.sourceTrainingFolder);
        params.targetTrainingFolder = StringUtil.checkLastSlash(params.targetTrainingFolder);
        
        FileUtils.createDirectory(params.trainingBaseFolder);
        
        if (!FileUtils.exists(params.trainingBaseFolder) || !FileUtils.isDirectory(params.trainingBaseFolder))
        {
            System.out.println("Error! Training base folder " + params.trainingBaseFolder + " not found.");
            bContinue = false;
        }
        
        if (!FileUtils.exists(params.sourceTrainingFolder) || !FileUtils.isDirectory(params.sourceTrainingFolder))
        {
            System.out.println("Error! Source training folder " + params.sourceTrainingFolder + " not found.");
            bContinue = false;
        }
        
        if (!FileUtils.exists(params.targetTrainingFolder) || !FileUtils.isDirectory(params.targetTrainingFolder))
        {
            System.out.println("Error! Target training folder " + params.targetTrainingFolder + " not found.");
            bContinue = false;
        }
        
        params.temporaryCodebookFile = params.codebookFile + ".temp";
        
        return bContinue;
    }
    
    //Create list of files
    public BaselineAdaptationSet getTrainingSet(String trainingFolder)
    {   
        BasenameList b = new BasenameList(trainingFolder, wavExt);
        
        BaselineAdaptationSet trainingSet = new BaselineAdaptationSet(b.getListAsVector().size());
        
        for (int i=0; i<trainingSet.items.length; i++)
            trainingSet.items[i].setFromWavFilename(trainingFolder + b.getName(i) + wavExt);
        
        return trainingSet;
    }
    //
    
    public void train(BaselineAdaptationSet sourceTrainingSet, BaselineAdaptationSet targetTrainingSet) throws IOException, UnsupportedAudioFileException
    {
        int numItems = Math.min(sourceTrainingSet.items.length, targetTrainingSet.items.length);
        if (numItems>0)
        {
            int [] map = new int[numItems]; 
            int i;
            
            for (i=0; i<numItems; i++)
                map[i] = i;
            
            train(sourceTrainingSet, targetTrainingSet, map);
        }
    }
    
    //<map> is a vector of same length as sourceItems showing the index of the corresponding target item 
    //  for each source item. This allows to specify the target files in any order, i.e. file names are not required to be in alphabetical order
    public void train(BaselineAdaptationSet sourceTrainingSet, BaselineAdaptationSet targetTrainingSet, int [] map) throws IOException, UnsupportedAudioFileException
    {
        if (sourceTrainingSet.items!=null && targetTrainingSet.items!=null && map!=null)
        {
            int numItems = Math.min(sourceTrainingSet.items.length, sourceTrainingSet.items.length);
            numItems = Math.min(numItems, map.length);
            
            if (numItems>0)
            {
                preprocessor.run(sourceTrainingSet);
                preprocessor.run(targetTrainingSet);
                
                int desiredFeatures = WeightedCodebookFeatureExtractor.LSF_FEATURES +
                                      WeightedCodebookFeatureExtractor.F0_FEATURES + 
                                      WeightedCodebookFeatureExtractor.ENERGY_FEATURES;
                
                featureExtractor.run(sourceTrainingSet, params, desiredFeatures);
                featureExtractor.run(targetTrainingSet, params, desiredFeatures);
            }
            
            WeightedCodebookFeatureCollection fcol = collectFeatures(sourceTrainingSet, targetTrainingSet, map);

            learnMapping(fcol, sourceTrainingSet, targetTrainingSet, map);
            
            outlierEliminator.run(params);
            
            deleteTemporaryFiles(fcol, sourceTrainingSet, targetTrainingSet);
        }
    }

    //Parallel training, sourceItems and targetItems should have at least map.length elements (ensured if this function is called through train)
    public WeightedCodebookFeatureCollection collectFeatures(BaselineAdaptationSet sourceTrainingSet, BaselineAdaptationSet targetTrainingSet, int [] map)
    {
        WeightedCodebookFeatureCollection fcol = new WeightedCodebookFeatureCollection(params, map.length);
        int i;
        IndexMap imap = null;
          
        if (params.codebookHeader.codebookType==WeightedCodebookFileHeader.FRAMES)
        {
            for (i=0; i<map.length; i++)
            {
                imap = AdaptationUtils.lsfFramesMapping(sourceTrainingSet.items[i].labelFile, targetTrainingSet.items[map[i]].labelFile, 
                                                        sourceTrainingSet.items[i].lsfFile, targetTrainingSet.items[map[i]].lsfFile);
                try {
                    imap.writeToFile(fcol.indexMapFiles[i]);
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
        else if (params.codebookHeader.codebookType==WeightedCodebookFileHeader.FRAME_GROUPS)
        {
            for (i=0; i<map.length; i++)
            {
                imap = AdaptationUtils.lsfFrameGroupsMapping(sourceTrainingSet.items[i].labelFile, targetTrainingSet.items[map[i]].labelFile, 
                                                             sourceTrainingSet.items[i].lsfFile, targetTrainingSet.items[map[i]].lsfFile,
                                                             params.codebookHeader.numNeighboursInFrameGroups);
                try {
                    imap.writeToFile(fcol.indexMapFiles[i]);
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
        else if (params.codebookHeader.codebookType==WeightedCodebookFileHeader.LABELS)
        {
            for (i=0; i<map.length; i++)
            {
                imap = AdaptationUtils.lsfLabelsMapping(sourceTrainingSet.items[i].labelFile, targetTrainingSet.items[map[i]].labelFile, 
                                                        sourceTrainingSet.items[i].lsfFile, targetTrainingSet.items[map[i]].lsfFile);
                try {
                    imap.writeToFile(fcol.indexMapFiles[i]);
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
        else if (params.codebookHeader.codebookType==WeightedCodebookFileHeader.LABEL_GROUPS)
        {
            for (i=0; i<map.length; i++)
            {
                imap = AdaptationUtils.lsfLabelGroupsMapping(sourceTrainingSet.items[i].labelFile, targetTrainingSet.items[map[i]].labelFile, 
                                                             sourceTrainingSet.items[i].lsfFile, targetTrainingSet.items[map[i]].lsfFile,
                                                             params.codebookHeader.numNeighboursInLabelGroups);
                try {
                    imap.writeToFile(fcol.indexMapFiles[i]);
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
        else if (params.codebookHeader.codebookType==WeightedCodebookFileHeader.SPEECH)
        {
            imap = AdaptationUtils.lsfSpeechMapping();
            try {
                imap.writeToFile(fcol.indexMapFiles[0]);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        
        return fcol;
    }
    
    //This function generates the codebooks from training pairs
    public void learnMapping(WeightedCodebookFeatureCollection fcol, BaselineAdaptationSet sourceTrainingSet, BaselineAdaptationSet targetTrainingSet, int [] map)
    {
        WeightedCodebookLsfMapper lsfMapper = new WeightedCodebookLsfMapper(params);
        WeightedCodebookFile temporaryCodebookFile = new WeightedCodebookFile(params.temporaryCodebookFile, WeightedCodebookFile.OPEN_FOR_WRITE);

        if (params.codebookHeader.codebookType==WeightedCodebookFileHeader.FRAMES)
            lsfMapper.learnMappingFrames(temporaryCodebookFile, fcol, sourceTrainingSet, targetTrainingSet, map);
        else if (params.codebookHeader.codebookType==WeightedCodebookFileHeader.FRAME_GROUPS)
            lsfMapper.learnMappingFrameGroups(temporaryCodebookFile, fcol, sourceTrainingSet, targetTrainingSet, map);
        else if (params.codebookHeader.codebookType==WeightedCodebookFileHeader.LABELS)
            lsfMapper.learnMappingLabels(temporaryCodebookFile, fcol, sourceTrainingSet, targetTrainingSet, map);
        else if (params.codebookHeader.codebookType==WeightedCodebookFileHeader.LABEL_GROUPS)
            lsfMapper.learnMappingLabelGroups(temporaryCodebookFile, fcol, sourceTrainingSet, targetTrainingSet, map);
        else if (params.codebookHeader.codebookType==WeightedCodebookFileHeader.SPEECH)
            lsfMapper.learnMappingSpeech(temporaryCodebookFile, fcol, sourceTrainingSet, targetTrainingSet, map);
        
        PitchTrainer ptcTrainer = new PitchTrainer(params);
        ptcTrainer.learnMapping(temporaryCodebookFile, fcol, sourceTrainingSet, targetTrainingSet, map);
        
        temporaryCodebookFile.close();
    }
    
    public void deleteTemporaryFiles(WeightedCodebookFeatureCollection fcol, BaselineAdaptationSet sourceTrainingSet, BaselineAdaptationSet targetTrainingSet)
    {
        FileUtils.delete(fcol.indexMapFiles, true);
        //FileUtils.delete(sourceTrainingSet.getLsfFiles(), true);
        //FileUtils.delete(targetTrainingSet.getLsfFiles(), true);
        //FileUtils.delete(sourceTrainingSet.getF0Files(), true);
        //FileUtils.delete(targetTrainingSet.getF0Files(), true);
        //FileUtils.delete(sourceTrainingSet.getEnergyFiles(), true);
        //FileUtils.delete(targetTrainingSet.getEnergyFiles(), true);
        
        FileUtils.delete(params.temporaryCodebookFile);
    }
    
    public static void main(String[] args) throws UnsupportedAudioFileException, IOException
    {
        WeightedCodebookPreprocessor pp = new WeightedCodebookPreprocessor();
        WeightedCodebookFeatureExtractor fe = new WeightedCodebookFeatureExtractor();
        
        WeightedCodebookTrainerParams pa = new WeightedCodebookTrainerParams();
        
        pa.codebookHeader.codebookType = WeightedCodebookFileHeader.FRAMES; //Frame-by-frame mapping of features
        //pa.codebookHeader.codebookType = WeightedCodebookFileHeader.FRAME_GROUPS; pa.codebookHeader.numNeighboursInFrameGroups = 3; //Mapping of frame average features (no label information but fixed amount of neighbouring frames is used)
        //pa.codebookHeader.codebookType = WeightedCodebookFileHeader.LABELS; //Mapping of label average features
        //pa.codebookHeader.codebookType = WeightedCodebookFileHeader.LABEL_GROUPS; pa.codebookHeader.numNeighboursInLabelGroups = 1; //Mapping of average features collected across label groups (i.e. vowels, consonants, etc)
        //pa.codebookHeader.codebookType = WeightedCodebookFileHeader.SPEECH; //Mapping of average features collected across all speech parts (i.e. like spectral equalization)

        pa.codebookHeader.sourceTag = "neutralF"; //Source name tag (i.e. style or speaker identity)
        pa.codebookHeader.targetTag = "angryF"; //Target name tag (i.e. style or speaker identity)
        
        pa.trainingBaseFolder = "d:\\1\\neutral_X_angry_50"; //Training base directory
        pa.sourceTrainingFolder = "d:\\1\\neutral50\\train"; //Source training folder
        pa.targetTrainingFolder = "d:\\1\\angry50\\train"; //Target training folder

        pa.indexMapFileExtension = ".imf"; //Index map file extensions
        
        pa.codebookHeader.lsfParams.lpOrder = 0; //Auto set
        pa.codebookHeader.lsfParams.preCoef = 0.97f;
        pa.codebookHeader.lsfParams.skipsize = 0.010f;
        pa.codebookHeader.lsfParams.winsize = 0.020f;
        pa.codebookHeader.lsfParams.windowType = Window.HAMMING;
        
        pa.codebookFile = StringUtil.checkLastSlash(pa.trainingBaseFolder) + pa.codebookHeader.sourceTag + "_X_" + pa.codebookHeader.targetTag + WeightedCodebookFile.defaultExtension;

        pa.isForcedAnalysis = false;
        
        pa.codebookHeader.ptcParams.ws = 0.040;
        pa.codebookHeader.ptcParams.ss = 0.005;
        pa.codebookHeader.ptcParams.voicingThreshold = 0.30;
        pa.codebookHeader.ptcParams.isDoublingCheck = false;
        pa.codebookHeader.ptcParams.isHalvingCheck = false;
        pa.codebookHeader.ptcParams.minimumF0 = 40.0f;
        pa.codebookHeader.ptcParams.maximumF0 = 400.0f;
        pa.codebookHeader.ptcParams.centerClippingRatio = 0.3;
        pa.codebookHeader.ptcParams.cutOff1 = pa.codebookHeader.ptcParams.minimumF0-20.0;
        pa.codebookHeader.ptcParams.cutOff2 = pa.codebookHeader.ptcParams.maximumF0+200.0;
        
        pa.codebookHeader.energyParams.windowSizeInSeconds = 0.020;
        pa.codebookHeader.energyParams.skipSizeInSeconds = 0.010;
        
        //Gaussian outlier eliminator
        //Decreasing totalStandardDeviations will lead to more outlier eliminations, i.e. smaller codebooks
        pa.gaussianEliminatorParams.isActive = true; //Set to false if you do not want to use this eliminator at all
        pa.gaussianEliminatorParams.totalStandardDeviationsLsf = 1.5;        
        pa.gaussianEliminatorParams.isCheckLsfOutliers = true;
        pa.gaussianEliminatorParams.isEliminateTooSimilarLsf = false;
        
        pa.gaussianEliminatorParams.totalStandardDeviationsF0 = 1.0;         
        pa.gaussianEliminatorParams.isCheckF0Outliers = true;
        
        pa.gaussianEliminatorParams.totalStandardDeviationsDuration = 1.0;   
        pa.gaussianEliminatorParams.isCheckDurationOutliers = true;
        
        pa.gaussianEliminatorParams.totalStandardDeviationsEnergy = 2.0;     
        pa.gaussianEliminatorParams.isCheckEnergyOutliers = true;
        //
        
        //KMeans one-to-many and many-to-one mapping eliminator
        pa.kmeansEliminatorParams.eliminationAlgorithm = KMeansMappingEliminatorParams.ELIMINATE_LEAST_LIKELY_MAPPINGS;
        //pa.kmeansEliminatorParams.eliminationAlgorithm = KMeansMappingEliminatorParams.ELIMINATE_MEAN_DISTANCE_MISMATCHES;
        //pa.kmeansEliminatorParams.eliminationAlgorithm = KMeansMappingEliminatorParams.ELIMINATE_USING_SUBCLUSTER_MEAN_DISTANCES;
        
        pa.kmeansEliminatorParams.eliminationLikelihood = 0.20;
        pa.kmeansEliminatorParams.numClusters = 40;
        pa.kmeansEliminatorParams.isActive = true; //Set to false if you do not want to use this eliminator at all
        pa.kmeansEliminatorParams.isSeparateClustering = true; //Cluster features separately(true) or together(false)?
        pa.kmeansEliminatorParams.isCheckLsfOutliers = true;    
        pa.kmeansEliminatorParams.isCheckF0Outliers = true; 
        pa.kmeansEliminatorParams.isCheckDurationOutliers = true;  
        pa.kmeansEliminatorParams.isCheckEnergyOutliers = true;
        //
        
        WeightedCodebookParallelTrainer t = new WeightedCodebookParallelTrainer(pp, fe, pa);
        
        t.run();
        
        System.out.println("Training completed...");
    }
}
