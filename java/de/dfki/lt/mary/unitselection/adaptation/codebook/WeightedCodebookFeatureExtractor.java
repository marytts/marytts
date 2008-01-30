package de.dfki.lt.mary.unitselection.adaptation.codebook;

import java.io.File;
import java.io.IOException;
import java.util.BitSet;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import de.dfki.lt.mary.unitselection.adaptation.BaselineAdaptationSet;
import de.dfki.lt.mary.util.FileUtils;
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
    public static int LSF_ANALYSIS  =   Integer.parseInt("00000001", 2);
    public static int F0_ANALYSIS   =   Integer.parseInt("00000010", 2);
    
    public void run(BaselineAdaptationSet fileSet, WeightedCodebookBaselineParams params, int desiredFeatures) throws IOException, UnsupportedAudioFileException
    {
        LsfFileHeader lsfParams = null;
        if (params instanceof WeightedCodebookTrainerParams)
            lsfParams = new LsfFileHeader(((WeightedCodebookTrainerParams)params).codebookHeader.lsfParams);
        else if (params instanceof WeightedCodebookTransformerParams)
            lsfParams = new LsfFileHeader(((WeightedCodebookTransformerParams)params).lsfParams);
        
        PitchFileHeader ptcParams = null;
        if (params instanceof WeightedCodebookTrainerParams)
            ptcParams = new PitchFileHeader(((WeightedCodebookTrainerParams)params).codebookHeader.ptcParams);
        else if (params instanceof WeightedCodebookTransformerParams)
            ptcParams = new PitchFileHeader(((WeightedCodebookTransformerParams)params).ptcParams);
        
        boolean isForcedAnalysis = false;
        if (params instanceof WeightedCodebookTrainerParams)
            isForcedAnalysis = ((WeightedCodebookTrainerParams)params).isForcedAnalysis;
        else if (params instanceof WeightedCodebookTransformerParams)
            isForcedAnalysis = ((WeightedCodebookTransformerParams)params).isForcedAnalysis;
        
        String str1 = Integer.toBinaryString(desiredFeatures);
        int maxPos = 8;
        int pos = maxPos;
        while (str1.length()<maxPos)
            str1 = "0" + str1;
        
        String str2;
        
        pos--;
        str2 = Integer.toBinaryString(LSF_ANALYSIS);
        while (str2.length()<maxPos)
            str2 = "0" + str2;
        if (str1.charAt(pos)==str2.charAt(pos))
            lsfAnalysis(fileSet, lsfParams, isForcedAnalysis);

        pos--;
        str2 = Integer.toBinaryString(F0_ANALYSIS);
        while (str2.length()<maxPos)
            str2 = "0" + str2;
        if (str1.charAt(pos)==str2.charAt(pos))
            f0Analysis(fileSet, ptcParams, isForcedAnalysis);       
        
        //ADD more as necessary
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
   
    //To do: f0 analysis of wav files
    public void f0Analysis(BaselineAdaptationSet fileSet, PitchFileHeader ptcParams, boolean isForcedAnalysis) throws UnsupportedAudioFileException, IOException
    {
        System.out.println("Starting f0 analysis...");
        
        boolean bAnalyze;
        for (int i=0; i<fileSet.items.length; i++)
        {
            bAnalyze = true;
            
            if (!isForcedAnalysis && FileUtils.exists(fileSet.items[i].f0File)) //No f0 detection if ptc file already exists
                bAnalyze = false;
                
            if (bAnalyze)
            {
                PitchTrackerAutocorrelation.pitchAnalyzeWavFile(fileSet.items[i].audioFile, fileSet.items[i].f0File, ptcParams);
                
                System.out.println("Extracted f0s: " + fileSet.items[i].f0File);
            }
            else
                System.out.println("LSF file found with identical analysis parameters: " + fileSet.items[i].lsfFile);
        }
        
        System.out.println("f0 analysis completed...");
    }

}
