package de.dfki.lt.mary.unitselection.adaptation.codebook;

import java.io.IOException;

import de.dfki.lt.mary.unitselection.adaptation.BaselineAdaptationSet;
import de.dfki.lt.mary.util.FileUtils;
import de.dfki.lt.signalproc.analysis.LineSpectralFrequencies;
import de.dfki.lt.signalproc.analysis.LsfFileHeader;
import de.dfki.lt.signalproc.analysis.Lsfs;

public class WeightedCodebookFeatureExtractor {
    //Add more as necessary & make sure you can discriminate each using AND(&) operator
    // from a single integer that represents desired analyses (See the function run())
    public static int LSF_ANALYSIS  =   Integer.parseInt("00000001", 2);
    public static int F0_ANALYSIS   =   Integer.parseInt("00000010", 2);
    
    public void run(BaselineAdaptationSet fileSet, WeightedCodebookBaselineParams params, int desiredFeatures) throws IOException
    {
        LsfFileHeader lsfParams = null;
        if (params instanceof WeightedCodebookTrainerParams)
            lsfParams = new LsfFileHeader(((WeightedCodebookTrainerParams)params).codebookHeader.lsfParams);
        else if (params instanceof WeightedCodebookTransformerParams)
            lsfParams = new LsfFileHeader(((WeightedCodebookTransformerParams)params).lsfParams);
        
        if ((desiredFeatures & LSF_ANALYSIS)==1)
            lsfAnalysis(fileSet, lsfParams);
        
        if ((desiredFeatures & F0_ANALYSIS)==1)
            f0Analysis(fileSet);       
    }
    
    public void lsfAnalysis(BaselineAdaptationSet fileSet, LsfFileHeader lsfParams) throws IOException
    {
        System.out.println("Starting LSF analysis...");
        
        boolean bAnalyze;
        for (int i=0; i<fileSet.items.length; i++)
        {
            bAnalyze = true;
            if (FileUtils.exists(fileSet.items[i].lsfFile))
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
    public void f0Analysis(BaselineAdaptationSet fileSet) throws IOException
    {
        System.out.println("Starting f0 analysis...");
        
        //To do: f0 analysis of wav files
        
        System.out.println("f0 analysis completed...");
    }

}
