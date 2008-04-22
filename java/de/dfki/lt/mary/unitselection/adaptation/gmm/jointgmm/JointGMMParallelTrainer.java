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
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Vector;

import javax.sound.sampled.UnsupportedAudioFileException;
import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import de.dfki.lt.machinelearning.ClusteredDataGenerator;
import de.dfki.lt.machinelearning.ContextualGMMParams;
import de.dfki.lt.machinelearning.GMM;
import de.dfki.lt.machinelearning.GMMTrainer;
import de.dfki.lt.machinelearning.GMMTrainerParams;
import de.dfki.lt.mary.MaryDataType;
import de.dfki.lt.mary.MaryProperties;
import de.dfki.lt.mary.NoSuchPropertyException;
import de.dfki.lt.mary.client.MaryClient;
import de.dfki.lt.mary.modules.phonemiser.PhonemeSet;
import de.dfki.lt.mary.unitselection.adaptation.codebook.WeightedCodebook;
import de.dfki.lt.mary.unitselection.adaptation.BaselineFeatureExtractor;
import de.dfki.lt.mary.unitselection.adaptation.codebook.WeightedCodebookFile;
import de.dfki.lt.mary.unitselection.adaptation.codebook.WeightedCodebookFileHeader;
import de.dfki.lt.mary.unitselection.adaptation.codebook.WeightedCodebookParallelTrainer;
import de.dfki.lt.mary.unitselection.adaptation.BaselinePreprocessor;
import de.dfki.lt.mary.unitselection.adaptation.codebook.WeightedCodebookTrainerParams;
import de.dfki.lt.mary.unitselection.adaptation.outlier.KMeansMappingEliminatorParams;
import de.dfki.lt.mary.unitselection.adaptation.outlier.TotalStandardDeviations;
import de.dfki.lt.mary.unitselection.adaptation.prosody.PitchMappingFile;
import de.dfki.lt.mary.util.FileUtils;
import de.dfki.lt.mary.util.StringUtil;
import de.dfki.lt.signalproc.util.MathUtils;
import de.dfki.lt.signalproc.util.distance.DistanceComputer;
import de.dfki.lt.signalproc.window.Window;

/**
 * @author oytun.turk
 *
 */
public class JointGMMParallelTrainer extends JointGMMTrainer {
    protected WeightedCodebookParallelTrainer wcpTrainer;
    protected JointGMMTrainerParams jgParams;
    
    public JointGMMParallelTrainer(BaselinePreprocessor pp,
                                   BaselineFeatureExtractor fe,
                                   WeightedCodebookTrainerParams pa,
                                   JointGMMTrainerParams gp,
                                   ContextualGMMParams cg) 
    {
        super(pp, fe, pa, gp, cg);
        
        wcpTrainer = new WeightedCodebookParallelTrainer(pp, fe, pa);
        jgParams = new JointGMMTrainerParams(gp);
    }

    public void run()
    {       
        train();
    }
    
    public void train()
    {
        if (!FileUtils.exists(codebookTrainerParams.codebookFile))
        {
            //Parallel codebook training
            try {
                wcpTrainer.run();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (UnsupportedAudioFileException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            //
        }
        
        //Read parallel codebook
        WeightedCodebookFile codebookFile = new WeightedCodebookFile(wcpTrainer.wcParams.codebookFile, WeightedCodebookFile.OPEN_FOR_READ);
        WeightedCodebook codebook = null;
        
        try {
            codebook = codebookFile.readCodebookFile();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        //
        
        //Get codebook entries in suitable format for GMM training and train joint GMMs
        if (cgParams==null || cgParams.phonemeClasses==null) //No context
        {
            JointGMMSet gmmSet = null;
            GMM gmm = null;
            if (codebook!=null)
            {
                double[][] xy = new double[codebook.lsfEntries.length][2*codebook.header.lsfParams.lpOrder];

                for (int i=0; i<codebook.lsfEntries.length; i++)
                {
                    System.arraycopy(codebook.lsfEntries[i].sourceItem.lsfs, 0, xy[i], 0, codebook.header.lsfParams.lpOrder);
                    System.arraycopy(codebook.lsfEntries[i].targetItem.lsfs, 0, xy[i], codebook.header.lsfParams.lpOrder, codebook.header.lsfParams.lpOrder);   
                }

                GMMTrainer g = new GMMTrainer();
                gmmSet = new JointGMMSet(1, cgParams);
                gmm = g.train(xy, jgParams.gmmEMTrainerParams);
                gmmSet.gmms[0] = new JointGMM(gmm, codebook.header.lsfParams);
            }
            
            //Convert joint GMM into a suitable format for using in transformation and save to a binary output file
            if (gmmSet!=null)
                gmmSet.write(jgParams.jointGMMFile);
        }
        else //Train contextual GMMs - a separate GMM will be trained for each phoneme class, all GMMs will be written to the same GMM file
        {
            double[][] xy = null;
            int[] totals = new int[cgParams.phonemeClasses.length+1];
            int[] classIndices = new int[codebook.lsfEntries.length];
            Arrays.fill(totals, 0);
            int i, n;
            JointGMMSet gmmSet = new JointGMMSet(totals.length, cgParams);

            if (codebook!=null)
            {
                for (i=0; i<codebook.lsfEntries.length; i++)
                {
                    classIndices[i] = cgParams.getClassIndex(codebook.lsfEntries[i].sourceItem.phn);
                    if (classIndices[i]<0)
                        classIndices[i] = totals.length-1;
                       
                    totals[classIndices[i]]++;
                }
            }

            for (n=0; n<totals.length; n++)
            {
                GMM gmm = null;
                int count = 0;
                if (totals[n]>0)
                {
                    xy = new double[totals[n]][2*codebook.header.lsfParams.lpOrder];

                    for (i=0; i<classIndices.length; i++)
                    {
                        if (count>=totals[n])
                            break;

                        if (classIndices[i]==n)
                        {
                            System.arraycopy(codebook.lsfEntries[i].sourceItem.lsfs, 0, xy[count], 0, codebook.header.lsfParams.lpOrder);
                            System.arraycopy(codebook.lsfEntries[i].targetItem.lsfs, 0, xy[count], codebook.header.lsfParams.lpOrder, codebook.header.lsfParams.lpOrder);
                            count++;
                        }
                    }

                    GMMTrainer g = new GMMTrainer();
                    gmm = g.train(xy, cgParams.classTrainerParams[n]);
                    if (n<totals.length-1)
                    {
                        gmm.info = "";
                        for (i=0; i<cgParams.phonemeClasses[n].length-1; i++)
                            gmm.info += cgParams.phonemeClasses[n][i] + " ";
                        
                        gmm.info += cgParams.phonemeClasses[n][cgParams.phonemeClasses[n].length-1];
                    }
                    else
                        gmm.info = "other";
                    
                    codebook.header.lsfParams.numfrm = totals[n];
                    gmmSet.gmms[n] = new JointGMM(gmm, codebook.header.lsfParams);
                }
            }
            
            //Convert joint GMM into a suitable format for using in transformation and save to a binary output file
            if (gmmSet!=null)
                gmmSet.write(jgParams.jointGMMFile);
            //
        }
        //
        
        System.out.println("Joint source-target GMM training completed...");
    }

    public static void main(String[] args) throws UnsupportedAudioFileException, IOException
    {   
        boolean isContextualGMMs = true;
        int numMixes = 4;
        
        mainParametric(numMixes, isContextualGMMs, "neutral", "angry");
        
        //mainParametric(numMixes, isContextualGMMs, "neutral", "happy");
        
        //mainParametric(numMixes, isContextualGMMs, "neutral", "sad");
    }
    
    public static void mainParametric(int numMixes, boolean isContextualGMMs, String sourceTag, String targetTag) throws UnsupportedAudioFileException, IOException
    {
        BaselinePreprocessor pp = new BaselinePreprocessor();
        BaselineFeatureExtractor fe = new BaselineFeatureExtractor();
        WeightedCodebookTrainerParams pa = new WeightedCodebookTrainerParams();
        JointGMMTrainerParams gp = new JointGMMTrainerParams();
        ContextualGMMParams cg = null;
        int numTrainingFiles = 200; //2, 20, 200, 350
        
        pa.codebookHeader.codebookType = WeightedCodebookFileHeader.FRAMES; //Frame-by-frame mapping of features
        //pa.codebookHeader.codebookType = WeightedCodebookFileHeader.FRAME_GROUPS; pa.codebookHeader.numNeighboursInFrameGroups = 3; //Mapping of frame average features (no label information but fixed amount of neighbouring frames is used)
        //pa.codebookHeader.codebookType = WeightedCodebookFileHeader.LABELS; //Mapping of label average features
        //pa.codebookHeader.codebookType = WeightedCodebookFileHeader.LABEL_GROUPS; pa.codebookHeader.numNeighboursInLabelGroups = 1; //Mapping of average features collected across label groups (i.e. vowels, consonants, etc)
        //pa.codebookHeader.codebookType = WeightedCodebookFileHeader.SPEECH; //Mapping of average features collected across all speech parts (i.e. like spectral equalization)

        pa.codebookHeader.sourceTag = sourceTag + "F"; //Source name tag (i.e. style or speaker identity)
        pa.codebookHeader.targetTag = targetTag + "F"; //Target name tag (i.e. style or speaker identity)
        
        pa.trainingBaseFolder = "D:\\Oytun\\DFKI\\voices\\Interspeech08_out\\" + sourceTag + "2" + targetTag; //Training base directory
        pa.sourceTrainingFolder = "D:\\Oytun\\DFKI\\voices\\Interspeech08\\" + sourceTag + "\\train_" + String.valueOf(numTrainingFiles); //Source training folder
        pa.targetTrainingFolder = "D:\\Oytun\\DFKI\\voices\\Interspeech08\\" + targetTag + "\\train_" + String.valueOf(numTrainingFiles); //Target training folder

        pa.indexMapFileExtension = ".imf"; //Index map file extensions
        
        pa.codebookHeader.lsfParams.lpOrder = 0; //Auto set
        pa.codebookHeader.lsfParams.preCoef = 0.97f;
        pa.codebookHeader.lsfParams.skipsize = 0.010f;
        pa.codebookHeader.lsfParams.winsize = 0.020f;
        pa.codebookHeader.lsfParams.windowType = Window.HAMMING;
        
        //Gaussian trainer params: commenting out results in using default value for each
        gp.gmmEMTrainerParams.totalComponents = numMixes;
        gp.gmmEMTrainerParams.isDiagonalCovariance = true; 
        gp.gmmEMTrainerParams.minimumIterations = 50;
        gp.gmmEMTrainerParams.maximumIterations = 150;
        gp.gmmEMTrainerParams.isUpdateCovariances = true;
        //gp.gmmEMTrainerParams.tinyLogLikelihoodChange = 1e-10;
        //gp.gmmEMTrainerParams.minimumCovarianceAllowed = 1e-5;
        
        if (isContextualGMMs)
        {
            String phonemeSetFile = "D:\\Mary TTS New\\lib\\modules\\de\\cap\\phoneme-list-de.xml";
            cg = getContextualGMMParams(phonemeSetFile, gp.gmmEMTrainerParams);
        }
        
        String baseFile = StringUtil.checkLastSlash(pa.trainingBaseFolder) + pa.codebookHeader.sourceTag + "_X_" + pa.codebookHeader.targetTag;
        //pa.codebookFile = baseFile + "_" + String.valueOf(gp.gmmEMTrainerParams.totalComponents) + WeightedCodebookFile.DEFAULT_EXTENSION;
        pa.codebookFile = baseFile + "_" + String.valueOf(numTrainingFiles) + WeightedCodebookFile.DEFAULT_EXTENSION;
        pa.pitchMappingFile = baseFile + "_" + String.valueOf(numTrainingFiles) + PitchMappingFile.DEFAULT_EXTENSION;
  
        if (!isContextualGMMs)
            gp.jointGMMFile = baseFile + "_" + String.valueOf(numTrainingFiles) + "_" + String.valueOf(gp.gmmEMTrainerParams.totalComponents) + JointGMMSet.DEFAULT_EXTENSION;
        else
            gp.jointGMMFile = baseFile + "_" + String.valueOf(numTrainingFiles) + "_context_" + String.valueOf(gp.gmmEMTrainerParams.totalComponents) + JointGMMSet.DEFAULT_EXTENSION;
            
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
        
        TotalStandardDeviations tsd = new TotalStandardDeviations();
        tsd.lsf = 1.5;
        tsd.f0 = 1.0;
        tsd.duration = 1.0;
        tsd.energy = 2.0;
        
        //Gaussian outlier eliminator
        //Decreasing totalStandardDeviations will lead to more outlier eliminations, i.e. smaller codebooks
        pa.gaussianEliminatorParams.isActive = true; //Set to false if you do not want to use this eliminator at all      
        pa.gaussianEliminatorParams.isCheckLsfOutliers = true;
        pa.gaussianEliminatorParams.isEliminateTooSimilarLsf = true;
        pa.gaussianEliminatorParams.isCheckF0Outliers = true; 
        pa.gaussianEliminatorParams.isCheckDurationOutliers = true;    
        pa.gaussianEliminatorParams.isCheckEnergyOutliers = true;
        pa.gaussianEliminatorParams.totalStandardDeviations = new TotalStandardDeviations(tsd);
        //
        
        //KMeans one-to-many and many-to-one mapping eliminator
        pa.kmeansEliminatorParams.isActive = true; //Set to false if you do not want to use this eliminator at all
        
        //pa.kmeansEliminatorParams.eliminationAlgorithm = KMeansMappingEliminatorParams.ELIMINATE_LEAST_LIKELY_MAPPINGS; 
        //pa.kmeansEliminatorParams.eliminationLikelihood = 0.20;
        
        pa.kmeansEliminatorParams.eliminationAlgorithm = KMeansMappingEliminatorParams.ELIMINATE_MEAN_DISTANCE_MISMATCHES; 
        pa.kmeansEliminatorParams.distanceType = DistanceComputer.NORMALIZED_EUCLIDEAN_DISTANCE;
        //pa.kmeansEliminatorParams.distanceType = DistanceComputer.EUCLIDEAN_DISTANCE;
        pa.kmeansEliminatorParams.isGlobalVariance = true;
        
        //pa.kmeansEliminatorParams.eliminationAlgorithm = KMeansMappingEliminatorParams.ELIMINATE_USING_SUBCLUSTER_MEAN_DISTANCES;
        
        pa.kmeansEliminatorParams.isSeparateClustering = false; //Cluster features separately(true) or together(false)?
        
        //Effective only when isSeparateClustering clustering is false
        tsd.general = 0.1;
        pa.kmeansEliminatorParams.numClusters = 30;
        
        //Effective only when isSeparateClustering clustering is true
        tsd.lsf = 1.0;
        tsd.f0 = 1.0;
        tsd.duration = 1.0;
        tsd.energy = 1.0;
        pa.kmeansEliminatorParams.numClustersLsf = 30;
        pa.kmeansEliminatorParams.numClustersF0 = 50;
        pa.kmeansEliminatorParams.numClustersDuration = 5;
        pa.kmeansEliminatorParams.numClustersEnergy = 5;
        
        pa.kmeansEliminatorParams.isCheckLsfOutliers = true;    
        pa.kmeansEliminatorParams.isCheckF0Outliers = false; 
        pa.kmeansEliminatorParams.isCheckDurationOutliers = false;  
        pa.kmeansEliminatorParams.isCheckEnergyOutliers = false;
        //
        
        pa.kmeansEliminatorParams.totalStandardDeviations = new TotalStandardDeviations(tsd);
        //

        JointGMMParallelTrainer t = new JointGMMParallelTrainer(pp, fe, pa, gp, cg);
        
        t.run();
    }
    
    public static ContextualGMMParams getContextualGMMParams(String phonemeSetFile, GMMTrainerParams commonParams)
    {
        ContextualGMMParams cg = null;
        PhonemeSet phonemeSet = null;
        
        try {
            try {
                phonemeSet = PhonemeSet.getPhonemeSet(phonemeSetFile);
            } catch (NoSuchPropertyException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (SAXException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (ParserConfigurationException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        if (phonemeSet!=null)
            cg = new ContextualGMMParams(phonemeSet, commonParams);

        return cg;
    }
}
