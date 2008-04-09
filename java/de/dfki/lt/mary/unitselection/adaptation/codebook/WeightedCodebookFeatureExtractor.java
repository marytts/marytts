package de.dfki.lt.mary.unitselection.adaptation.codebook;

import java.io.File;
import java.io.IOException;
import java.util.BitSet;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import de.dfki.lt.mary.unitselection.adaptation.BaselineAdaptationSet;
import de.dfki.lt.mary.unitselection.adaptation.BaselineParams;
import de.dfki.lt.mary.unitselection.adaptation.gmm.jointgmm.JointGMMTransformerParams;
import de.dfki.lt.mary.util.FileUtils;
import de.dfki.lt.signalproc.analysis.EnergyAnalyserRms;
import de.dfki.lt.signalproc.analysis.EnergyFileHeader;
import de.dfki.lt.signalproc.analysis.F0ReaderWriter;
import de.dfki.lt.signalproc.analysis.F0Tracker;
import de.dfki.lt.signalproc.analysis.F0TrackerAutocorrelation;
import de.dfki.lt.signalproc.analysis.LineSpectralFrequencies;
import de.dfki.lt.signalproc.analysis.LsfFileHeader;
import de.dfki.lt.signalproc.analysis.Lsfs;
import de.dfki.lt.signalproc.analysis.PitchFileHeader;
import de.dfki.lt.signalproc.analysis.PitchTrackerAutocorrelation;
import de.dfki.lt.signalproc.util.AudioDoubleDataSource;

public class WeightedCodebookFeatureExtractor {
    //Add more as necessary & make sure you can discriminate each using AND(&) operator
    // from a single integer that represents desired analyses (See the function run())
    public static int LSF_FEATURES      =   Integer.parseInt("00000001", 2);
    public static int F0_FEATURES       =   Integer.parseInt("00000010", 2);
    public static int ENERGY_FEATURES   =   Integer.parseInt("00000100", 2);
    public static int DURATION_FEATURES =   Integer.parseInt("00001000", 2);
    public static final int FEATURE_DESCRIBER_STRING_LENGTH = 8;
    
    public static boolean isDesired(int featureDesired, int desiredFeatures)
    {
        boolean bRet = false;
        
        String str1 = Integer.toBinaryString(desiredFeatures);

        while (str1.length()<FEATURE_DESCRIBER_STRING_LENGTH)
            str1 = "0" + str1;
        
        String str2;
        int pos = FEATURE_DESCRIBER_STRING_LENGTH;
        
        //ADD more as necessary
        if (featureDesired==LSF_FEATURES)
            pos = FEATURE_DESCRIBER_STRING_LENGTH-1;
        else if (featureDesired==F0_FEATURES)
            pos = FEATURE_DESCRIBER_STRING_LENGTH-2;
        else if (featureDesired==ENERGY_FEATURES)
            pos = FEATURE_DESCRIBER_STRING_LENGTH-3;
        else if (featureDesired==DURATION_FEATURES)
            pos = FEATURE_DESCRIBER_STRING_LENGTH-4;
        //
           
        str2 = Integer.toBinaryString(featureDesired);
        while (str2.length()<FEATURE_DESCRIBER_STRING_LENGTH)
            str2 = "0" + str2;
        if (pos<FEATURE_DESCRIBER_STRING_LENGTH && pos>=0 && str1.charAt(pos)==str2.charAt(pos))
            bRet = true;
        
        return bRet;
    }
    
    public WeightedCodebookFeatureExtractor()
    {
        this(null);
    }
    
    public WeightedCodebookFeatureExtractor(WeightedCodebookFeatureExtractor fe)
    {
        if (fe!=null)
        {
            //Copy class members if you add any
        }
        else
        {
            //Set default class member values
        }
        
    }
    
    public void run(BaselineAdaptationSet fileSet, BaselineParams params, int desiredFeatures) throws IOException, UnsupportedAudioFileException
    {
        LsfFileHeader lsfParams = null;
        if (params instanceof WeightedCodebookTrainerParams)
            lsfParams = new LsfFileHeader(((WeightedCodebookTrainerParams)params).codebookHeader.lsfParams);
        else if (params instanceof WeightedCodebookTransformerParams)
            lsfParams = new LsfFileHeader(((WeightedCodebookTransformerParams)params).lsfParams);
        else if (params instanceof JointGMMTransformerParams)
            lsfParams = new LsfFileHeader(((JointGMMTransformerParams)params).lsfParams);
        
        PitchFileHeader ptcParams = null;
        if (params instanceof WeightedCodebookTrainerParams)
            ptcParams = new PitchFileHeader(((WeightedCodebookTrainerParams)params).codebookHeader.ptcParams);
        else if (params instanceof WeightedCodebookTransformerParams)
            ptcParams = new PitchFileHeader(((WeightedCodebookTransformerParams)params).ptcParams);
        else if (params instanceof JointGMMTransformerParams)
            ptcParams = new PitchFileHeader(((JointGMMTransformerParams)params).ptcParams);
        
        EnergyFileHeader energyParams = null;
        if (params instanceof WeightedCodebookTrainerParams)
            energyParams = new EnergyFileHeader(((WeightedCodebookTrainerParams)params).codebookHeader.energyParams);
        else if (params instanceof WeightedCodebookTransformerParams)
            energyParams = new EnergyFileHeader(((WeightedCodebookTransformerParams)params).energyParams);
        else if (params instanceof JointGMMTransformerParams)
            energyParams = new EnergyFileHeader(((JointGMMTransformerParams)params).energyParams);
        
        boolean isForcedAnalysis = false;
        if (params instanceof WeightedCodebookTrainerParams)
            isForcedAnalysis = ((WeightedCodebookTrainerParams)params).isForcedAnalysis;
        else if (params instanceof WeightedCodebookTransformerParams)
            isForcedAnalysis = ((WeightedCodebookTransformerParams)params).isForcedAnalysis;
        else if (params instanceof JointGMMTransformerParams)
            isForcedAnalysis = ((JointGMMTransformerParams)params).isForcedAnalysis;
        
        //ADD more analyses as necessary
        if (isDesired(LSF_FEATURES, desiredFeatures))
            lsfAnalysis(fileSet, lsfParams, isForcedAnalysis);

        if (isDesired(F0_FEATURES, desiredFeatures))
            f0Analysis(fileSet, ptcParams, isForcedAnalysis);       
        
        if (isDesired(ENERGY_FEATURES, desiredFeatures))
            energyAnalysis(fileSet, energyParams, isForcedAnalysis); 
        //
    }
    
    public void lsfAnalysis(BaselineAdaptationSet fileSet, LsfFileHeader lsfParams, boolean isForcedAnalysis) throws IOException
    {
        System.out.println("Starting LSF analysis...");
        
        boolean bAnalyze;
        for (int i=0; i<fileSet.items.length; i++)
        {
            bAnalyze = true;
            if (!isForcedAnalysis && FileUtils.exists(fileSet.items[i].lsfFile))
            {
                LsfFileHeader tmpParams = new LsfFileHeader(fileSet.items[i].lsfFile);
                if (tmpParams.isIdenticalAnalysisParams(lsfParams))
                    bAnalyze = false;
            }
                
            if (bAnalyze)
            {
                LineSpectralFrequencies.lsfAnalyzeWavFile(fileSet.items[i].audioFile, fileSet.items[i].lsfFile, lsfParams);
                System.out.println("Extracted LSFs: " + fileSet.items[i].lsfFile);
            }
            else
                System.out.println("LSF file found with identical analysis parameters: " + fileSet.items[i].lsfFile);
        }
        
        System.out.println("LSF analysis completed...");
    }
   
    public void f0Analysis(BaselineAdaptationSet fileSet, PitchFileHeader ptcParams, boolean isForcedAnalysis) throws UnsupportedAudioFileException, IOException
    {
        System.out.println("Starting f0 analysis...");
        
        boolean bAnalyze;
        PitchTrackerAutocorrelation p = new PitchTrackerAutocorrelation(ptcParams);
        
        for (int i=0; i<fileSet.items.length; i++)
        {
            bAnalyze = true;
            
            if (!isForcedAnalysis && FileUtils.exists(fileSet.items[i].f0File)) //No f0 detection if ptc file already exists
                bAnalyze = false;
                
            if (bAnalyze)
            { 
                p.pitchAnalyzeWavFile(fileSet.items[i].audioFile, fileSet.items[i].f0File);
                
                System.out.println("Extracted f0 contour: " + fileSet.items[i].f0File);
            }
            else
                System.out.println("F0 file found with identical analysis parameters: " + fileSet.items[i].f0File);
        }
        
        System.out.println("f0 analysis completed...");
    }

    public void energyAnalysis(BaselineAdaptationSet fileSet, EnergyFileHeader energyParams, boolean isForcedAnalysis) throws UnsupportedAudioFileException, IOException
    {
        System.out.println("Starting energy analysis...");
        
        boolean bAnalyze;
        EnergyAnalyserRms e = null;
        
        for (int i=0; i<fileSet.items.length; i++)
        {
            bAnalyze = true;
            
            if (!isForcedAnalysis && FileUtils.exists(fileSet.items[i].energyFile)) //No f0 detection if ptc file already exists
                bAnalyze = false;
                
            if (bAnalyze)
            { 
                e = new EnergyAnalyserRms(fileSet.items[i].audioFile, fileSet.items[i].energyFile, energyParams.windowSizeInSeconds, energyParams.skipSizeInSeconds);
                
                System.out.println("Extracted energy contour: " + fileSet.items[i].energyFile);
            }
            else
                System.out.println("Energy file found with identical analysis parameters: " + fileSet.items[i].energyFile);
        }
        
        System.out.println("Energy analysis completed...");
    }

}
