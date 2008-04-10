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

package de.dfki.lt.mary.unitselection.adaptation;

import java.io.IOException;

import javax.sound.sampled.UnsupportedAudioFileException;

import de.dfki.lt.mary.unitselection.adaptation.codebook.WeightedCodebookTrainerParams;
import de.dfki.lt.mary.unitselection.adaptation.codebook.WeightedCodebookTransformerParams;
import de.dfki.lt.mary.unitselection.adaptation.gmm.jointgmm.JointGMMTransformerParams;
import de.dfki.lt.mary.util.FileUtils;
import de.dfki.lt.signalproc.analysis.EnergyAnalyserRms;
import de.dfki.lt.signalproc.analysis.EnergyFileHeader;
import de.dfki.lt.signalproc.analysis.LineSpectralFrequencies;
import de.dfki.lt.signalproc.analysis.LsfFileHeader;
import de.dfki.lt.signalproc.analysis.PitchFileHeader;
import de.dfki.lt.signalproc.analysis.PitchTrackerAutocorrelation;

/**
 * @author oytun.turk
 *
 */
public class BaselineFeatureExtractor {
    //Add more as necessary & make sure you can discriminate each using AND(&) operator
    // from a single integer that represents desired analyses (See the function run())
    public static final int LSF_FEATURES      =   Integer.parseInt("00000001", 2);
    public static final int F0_FEATURES       =   Integer.parseInt("00000010", 2);
    public static final int ENERGY_FEATURES   =   Integer.parseInt("00000100", 2);
    public static final int DURATION_FEATURES =   Integer.parseInt("00001000", 2);
    public static final int FEATURE_DESCRIBER_STRING_LENGTH = 8;
    
    public BaselineFeatureExtractor()
    {
        this(null);
    }
    
    public BaselineFeatureExtractor(BaselineFeatureExtractor existing)
    {
        if (existing!=null)
        {
            //Copy class members if you add any
        }
        else
        {
            //Set default class member values
        }
        
    }
    
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
