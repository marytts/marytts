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

import de.dfki.lt.machinelearning.ClusteredDataGenerator;
import de.dfki.lt.machinelearning.GMM;
import de.dfki.lt.machinelearning.GMMTrainer;
import de.dfki.lt.mary.unitselection.adaptation.codebook.WeightedCodebook;
import de.dfki.lt.mary.unitselection.adaptation.codebook.WeightedCodebookFeatureExtractor;
import de.dfki.lt.mary.unitselection.adaptation.codebook.WeightedCodebookFile;
import de.dfki.lt.mary.unitselection.adaptation.codebook.WeightedCodebookFileHeader;
import de.dfki.lt.mary.unitselection.adaptation.codebook.WeightedCodebookParallelTrainer;
import de.dfki.lt.mary.unitselection.adaptation.codebook.WeightedCodebookPreprocessor;
import de.dfki.lt.mary.unitselection.adaptation.codebook.WeightedCodebookTrainerParams;
import de.dfki.lt.mary.unitselection.adaptation.outlier.KMeansMappingEliminatorParams;
import de.dfki.lt.mary.unitselection.adaptation.outlier.TotalStandardDeviations;
import de.dfki.lt.mary.util.FileUtils;
import de.dfki.lt.mary.util.StringUtil;
import de.dfki.lt.signalproc.util.DistanceComputer;
import de.dfki.lt.signalproc.util.MathUtils;
import de.dfki.lt.signalproc.window.Window;

/**
 * @author oytun.turk
 *
 */
public class JointGMMParallelTrainer extends JointGMMTrainer {
    protected WeightedCodebookParallelTrainer wcpTrainer;
    protected JointGMMTrainerParams jgParams;
    
    public JointGMMParallelTrainer(WeightedCodebookPreprocessor pp,
                           WeightedCodebookFeatureExtractor fe,
                           WeightedCodebookTrainerParams pa,
                           JointGMMTrainerParams gp) 
    {
        wcpTrainer = new WeightedCodebookParallelTrainer(pp, fe, pa);
        jgParams = new JointGMMTrainerParams(gp);
    }

    public void run()
    {       
        train();
    }
    
    public void train()
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
        
        //Read codebook entries in suitable format for GMM training and train joint GMMs
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
            gmm = g.train(xy, jgParams.gmmEMTrainerParams);
        }
        //
        
        //Convert joint GMM into a suitable format for using in transformation and save to a binary output file
        if (gmm!=null)
        {
            JointGMM jointGMM = new JointGMM(gmm);
            
            jointGMM.write(jgParams.jointGMMFile);
            
            JointGMM jointGMM2 = new JointGMM(jgParams.jointGMMFile); //Check if writing/reading identical
            
            System.out.println("Write/read test complete...");
        }
        //
        
        //Delete temporary codebook file
        FileUtils.delete(wcpTrainer.wcParams.codebookFile);
        //
    }
    
    public static void main(String[] args) throws UnsupportedAudioFileException, IOException
    {
        WeightedCodebookPreprocessor pp = new WeightedCodebookPreprocessor();
        WeightedCodebookFeatureExtractor fe = new WeightedCodebookFeatureExtractor();
        WeightedCodebookTrainerParams pa = new WeightedCodebookTrainerParams();
        JointGMMTrainerParams gp = new JointGMMTrainerParams();
        
        pa.codebookHeader.codebookType = WeightedCodebookFileHeader.FRAMES; //Frame-by-frame mapping of features
        //pa.codebookHeader.codebookType = WeightedCodebookFileHeader.FRAME_GROUPS; pa.codebookHeader.numNeighboursInFrameGroups = 3; //Mapping of frame average features (no label information but fixed amount of neighbouring frames is used)
        //pa.codebookHeader.codebookType = WeightedCodebookFileHeader.LABELS; //Mapping of label average features
        //pa.codebookHeader.codebookType = WeightedCodebookFileHeader.LABEL_GROUPS; pa.codebookHeader.numNeighboursInLabelGroups = 1; //Mapping of average features collected across label groups (i.e. vowels, consonants, etc)
        //pa.codebookHeader.codebookType = WeightedCodebookFileHeader.SPEECH; //Mapping of average features collected across all speech parts (i.e. like spectral equalization)

        pa.codebookHeader.sourceTag = "neutralF"; //Source name tag (i.e. style or speaker identity)
        pa.codebookHeader.targetTag = "angryF"; //Target name tag (i.e. style or speaker identity)
        
        pa.trainingBaseFolder = "d:\\1\\neutral_X_angry_50_jointGMM"; //Training base directory
        pa.sourceTrainingFolder = "d:\\1\\neutral50\\train"; //Source training folder
        pa.targetTrainingFolder = "d:\\1\\angry50\\train"; //Target training folder

        pa.indexMapFileExtension = ".imf"; //Index map file extensions
        
        pa.codebookHeader.lsfParams.lpOrder = 0; //Auto set
        pa.codebookHeader.lsfParams.preCoef = 0.97f;
        pa.codebookHeader.lsfParams.skipsize = 0.010f;
        pa.codebookHeader.lsfParams.winsize = 0.020f;
        pa.codebookHeader.lsfParams.windowType = Window.HAMMING;
        
        pa.codebookFile = StringUtil.checkLastSlash(pa.trainingBaseFolder) + pa.codebookHeader.sourceTag + "_X_" + pa.codebookHeader.targetTag + WeightedCodebookFile.DEFAULT_FILE_EXTENSION;

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
        
        //Gaussian trainer params: commenting out results in using default value for each
        gp.gmmEMTrainerParams.totalComponents = 16;
        gp.gmmEMTrainerParams.isDiagonalCovariance = true; 
        gp.gmmEMTrainerParams.minimumIterations = 100;
        gp.gmmEMTrainerParams.maximumIterations = 200;
        gp.gmmEMTrainerParams.isUpdateCovariances = true;
        //gp.gmmEMTrainerParams.tinyLogLikelihoodChange = 1e-10;
        //gp.gmmEMTrainerParams.minimumCovarianceAllowed = 1e-5;
        
        gp.jointGMMFile = StringUtil.checkLastSlash(pa.trainingBaseFolder) + pa.codebookHeader.sourceTag + "_X_" + pa.codebookHeader.targetTag + JointGMM.DEFAULT_FILE_EXTENSION;
        //
        
        JointGMMParallelTrainer t = new JointGMMParallelTrainer(pp, fe, pa, gp);
        
        t.run();
        
        System.out.println("End of joint GMM training test...");
    }
}
