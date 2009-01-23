/**
 * Copyright 2007 DFKI GmbH.
 * All Rights Reserved.  Use is subject to license terms.
 *
 * This file is part of MARY TTS.
 *
 * MARY TTS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package marytts.signalproc.sinusoidal.pitch;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import marytts.signalproc.sinusoidal.SinusoidalAnalyzer;
import marytts.signalproc.sinusoidal.NonharmonicSinusoidalSpeechFrame;
import marytts.signalproc.window.Window;
import marytts.util.MaryUtils;
import marytts.util.data.audio.AudioDoubleDataSource;
import marytts.util.math.ComplexArray;
import marytts.util.math.FFT;
import marytts.util.math.FFTMixedRadix;
import marytts.util.math.MathUtils;
import marytts.util.signal.SignalProcUtils;


/**
 * Initial pitch, voicing, maximum frequency of voicing, and refined pitch estimation
 * as described in:
 * Stylianou, Y., "A Pitch and Maximum Voiced Frequency Estimation Technique adapted to Harmonic Models of Speech".
 *
 * @author Oytun T&uumlrk
 */
public class HnmPitchVoicingAnalyzer {
    //Default search range for voicing detection, i.e. voicing criterion will be computed for frequency range:
    // [DEFAULT_VOICING_START_HARMONIC x f0, DEFAULT_VOICING_START_HARMONIC x f0] where f0 is the fundamental frequency estimate
    public static int NUM_HARMONICS_FOR_VOICING = 4;
    public static float HARMONICS_NEIGH = 0.3f; //Between 0.0 and 1.0: How much the search range for voicing detection will be extended beyond the first and the last harmonic
                                                //0.3 means the region [0.7xf0, 4.3xf0] will be considered in voicing decision
    //
    
    public static float NUM_PERIODS_AT_LEAST = 3.0f;
    
    //These are the three thresholds used in Stylianou for maximum voicing frequency estimation using the harmonic model
    public static double CUMULATIVE_AMP_THRESHOLD = 2.0;
    public static double MAXIMUM_AMP_THRESHOLD_IN_DB = 13.0;
    public static double HARMONIC_DEVIATION_PERCENT = 20.0;
    public static double SHARP_PEAK_AMP_DIFF_IN_DB = 1.0;
    public static int MINIMUM_TOTAL_HARMONICS = 20; //At least this much total harmonics will be included in voiced spectral region (effective only when f0>10.0)
    public static int MAXIMUM_TOTAL_HARMONICS = 50; //At most this much total harmonics will be included in voiced süpectral region (effective only when f0>10.0)
    //
    
    //For voicing detection
    public static double VUV_SEARCH_MIN_HARMONIC_MULTIPLIER = 0.7;
    public static double VUV_SEARCH_MAX_HARMONIC_MULTIPLIER = 4.3;
    //
    
    //public float[] f0s;
    //public float[] maxFrequencyOfVoicings;
    
    public HnmPitchVoicingAnalyzer()
    {

    }
    
    public static int getDefaultFFTSize(int samplingRate)
    { 
        if (samplingRate<10000)
            return 2048;
        else if (samplingRate<20000)
            return 4096;
        else
            return 8192;
    }
    
    public static float[] estimateInitialPitch(double[] x, int samplingRate, float windowSizeInSeconds, float skipSizeInSeconds, 
                                               float f0MinInHz, float f0MaxInHz, int windowType) 
    {
        int PMax = (int)Math.floor(samplingRate/f0MinInHz+0.5);
        int PMin = (int)Math.floor(samplingRate/f0MaxInHz+0.5);
        
        int ws = SignalProcUtils.time2sample(windowSizeInSeconds, samplingRate);
        ws = Math.max(ws, (int)Math.floor(NUM_PERIODS_AT_LEAST*PMin+0.5));
        
        int ss = (int)Math.floor(skipSizeInSeconds*samplingRate + 0.5);
        int numfrm = (int)Math.floor(((double)x.length-ws)/ss+0.5);

        int numCandidates = PMax-PMin+1;
        
        double[] E = new double[numCandidates];
        
        int P;
        int i, t, l, k;
        double term1, term2, term3, r;

        double[] frm = new double[ws];
        Window win = Window.get(windowType, ws);
        double[] wgt2 = win.getCoeffs();
        wgt2 = MathUtils.normalizeToSumUpTo(wgt2, 1.0);
        
        for (t=0; t<ws; t++)
            wgt2[t] = wgt2[t]*wgt2[t];
        
        double tmpSum = 0.0;
        for (t=0; t<ws; t++)
            tmpSum += wgt2[t];
        
        for (t=0; t<ws; t++)
            wgt2[t] = wgt2[t]/tmpSum;
        
        double[] wgt4 = new double[ws];
        System.arraycopy(wgt2, 0, wgt4, 0, ws);
        for (t=0; t<ws; t++)
            wgt4[t] = wgt4[t]*wgt4[t];
        
        double termTmp = 0.0;
        for (t=0; t<ws; t++)
            termTmp += wgt4[t];
        
        float[] initialF0s = new float[numfrm]; 
        int minInd;
        int startIndex, endIndex;
        for (i=0; i<numfrm; i++)
        {
            Arrays.fill(frm, 0.0);
            System.arraycopy(x, i*ss, frm, 0, Math.min(ws, x.length-i*ss));
            startIndex = i*ss;
            endIndex = startIndex+Math.min(ws, x.length-i*ss)-1;
            
            //MaryUtils.plot(frm);
            
            term1 = 0.0;
            for (t=0; t<ws; t++)
                term1 += frm[t]*frm[t]*wgt2[t];
            
            float lLim;
            for (P=PMin; P<=PMax; P++)
            {
                lLim = ((float)ws)/P;
                term2 = 0.0;
                for (l=(int)(Math.floor(-1.0f*lLim)+1); l<lLim; l++)
                {
                    r=0.0;
                    for (t=0; t<ws; t++)
                    {
                        if (t+l*P>=0 && t+l*P<ws)
                            r += frm[t]*wgt2[t]*frm[t+l*P]*wgt2[t+l*P];
                    }

                    term2 += r;
                }
                term2 *= P;

                term3 = 1.0-P*termTmp;

                E[P-PMin] = (term1-term2)/(term1*term3);
            }

            MaryUtils.plot(E);
            
            minInd = MathUtils.getMinIndex(E);
            if (E[minInd]<0.5)
                initialF0s[i] = 1.0f/SignalProcUtils.sample2time(minInd+PMin, samplingRate);
            else
                initialF0s[i] = 0.0f;
        } 
        
        return initialF0s;
    }
    
    public static  float[] analyzeVoicings(double[] x, int samplingRate, float windowSizeInSeconds, float skipSizeInSeconds,
                                        int fftSize, float[] initialF0s) 
    {
        int numfrm = initialF0s.length;
        
        double[] voicingErrors = new double[numfrm];
        
        int ws = SignalProcUtils.time2sample(windowSizeInSeconds, samplingRate);
        int ss = (int)Math.floor(skipSizeInSeconds*samplingRate + 0.5);

        int startIndex, endIndex;
        boolean isVoiced;
        float[] maxFrequencyOfVoicings = new float[numfrm];
        float currentTime;
        double [] frm = new double[ws];
        for (int i=0; i<numfrm; i++)
        {  
            Arrays.fill(frm, 0.0);
            System.arraycopy(x, i*ss, frm, 0, Math.min(ws, x.length-i*ss));
            startIndex = i*ss;
            endIndex = startIndex+Math.min(ws, x.length-i*ss)-1;
            
            NonharmonicSinusoidalSpeechFrame frameSins = null;

            if (fftSize<frm.length)
                fftSize *= 2;

            int maxFreq = (int) (Math.floor(0.5*fftSize+0.5)+1);
            ComplexArray Y = new ComplexArray(fftSize);

            //Perform circular buffering as described in (Quatieri, 2001) to provide correct phase estimates
            int midPoint = (int) Math.floor(0.5*frm.length+0.5);
            System.arraycopy(frm, midPoint, Y.real, 0, frm.length-midPoint);
            System.arraycopy(frm, 0, Y.real, fftSize-midPoint, midPoint);
            //

            //Compute DFT
            if (MathUtils.isPowerOfTwo(fftSize))
                FFT.transform(Y.real, Y.imag, false);
            else
                Y = FFTMixedRadix.fftComplex(Y);
            //
            
            //Compute magnitude spectrum in dB as peak frequency estimates tend to be more accurate
            double[] YAbs = MathUtils.abs(Y, 0, maxFreq-1);
            double[] YAbsDB = MathUtils.amp2db(YAbs);
            //
            
            isVoiced = false;
            if (initialF0s[i]>10.0f)
            {
                voicingErrors[i] = estimateVoicingFromFrameSpectrum(YAbs, samplingRate, initialF0s[i]);

                if (voicingErrors[i]<-15.0)
                    isVoiced = true;
            }
            
            VoicingAnalysisOutputData vo = null;
            if (isVoiced)
            {
                vo = estimateMaxFrequencyOfVoicingsFrame(YAbsDB, samplingRate, initialF0s[i], isVoiced);
                maxFrequencyOfVoicings[i] = vo.maxFreqOfVoicing;
            }
            else
                maxFrequencyOfVoicings[i] = 0.0f;
            
            currentTime = SignalProcUtils.sample2time((int)Math.floor(0.5*(startIndex+endIndex)+0.5), samplingRate);
            if (isVoiced)
                System.out.println("Time=" + String.valueOf(currentTime)+ " sec." + " f0=" + String.valueOf(initialF0s[i]) + " Hz." + " Voiced" + " MaxVFreq=" + String.valueOf(maxFrequencyOfVoicings[i]));
            else
                System.out.println("Time=" + String.valueOf(currentTime)+ " sec." + " f0=" + String.valueOf(initialF0s[i]) + " Hz." + " Unvoiced" + " MaxVFreq=" + String.valueOf(maxFrequencyOfVoicings[i]));
        }
        
        //MaryUtils.plot(voicingErrors);
        
        return maxFrequencyOfVoicings;
    }
    
    public static VoicingAnalysisOutputData estimateMaxFrequencyOfVoicingsFrame(double[] absDBSpec, int samplingRate, float f0, boolean isVoiced)
    {  
        //double[] ampSpec = MathUtils.db2amp(absDBSpec);
        VoicingAnalysisOutputData output = new VoicingAnalysisOutputData();
        int i, n;
        output.maxFreqOfVoicing = 0.0f; //Means the spectrum is completely unvoiced
        if (!isVoiced)
            f0=100.0f;
        
        int maxFreqIndex = absDBSpec.length-1;
        int numHarmonics = (int)Math.floor((0.5*samplingRate-1.5*f0)/f0+0.5); //Leave some space at the end of spectrum, i.e. 1.5*f0 to avoid very narrow bands there
        int[] bandInds = new int[numHarmonics+1]; //0.5f0 1.5f0 2.5f0 ... (numHarmonics-0.5)f0 (numHarmonics+0.5)f0
        for (i=0; i<bandInds.length; i++)
            bandInds[i] = SignalProcUtils.freq2index((i+0.5)*f0, samplingRate, maxFreqIndex);
        
        double[] voiceds = new double[numHarmonics];
        double[] vals1 = new double[numHarmonics];
        double[] vals2 = new double[numHarmonics];
        double[] vals3 = new double[numHarmonics];
        Arrays.fill(vals1, Double.NEGATIVE_INFINITY);
        Arrays.fill(vals2, Double.NEGATIVE_INFINITY);
        Arrays.fill(vals3, Double.NEGATIVE_INFINITY);
        
        Arrays.fill(voiceds, 0.0);
        int[] valleyInds = MathUtils.getExtrema(absDBSpec, 1, 1, false);
        int[] tmpPeakIndices = new int[numHarmonics];
        
        double bandExtremaVal;
        int fcIndex;
        for (i=0; i<numHarmonics; i++)
        {
            //Get local peak
            int[] fcIndices = MathUtils.getExtrema(absDBSpec, 1, 1, true, bandInds[i], bandInds[i+1]);
            if (fcIndices!=null)
            {
                fcIndex = fcIndices[0];
                bandExtremaVal = absDBSpec[fcIndex];
                for (n=1; n<fcIndices.length; n++)
                {
                    if (absDBSpec[fcIndices[n]]>bandExtremaVal)
                    {
                        fcIndex = fcIndices[n];
                        bandExtremaVal = absDBSpec[fcIndex];
                    }
                }
            }
            else
            {
                fcIndex = MathUtils.getAbsMaxInd(absDBSpec, bandInds[i], bandInds[i+1]);
                if (fcIndex==-1)
                    fcIndex = (int)Math.floor(0.5*(bandInds[i]+bandInds[i+1])+0.5);
            }
            
            double fc = SignalProcUtils.index2freq(fcIndex, samplingRate, maxFreqIndex);
            
            //From db spec
            double Am = absDBSpec[fcIndex];
            double Amc = computeAmc(absDBSpec, fcIndex, valleyInds);
            //
            
            /*
            //From linear spec
            double Am = ampSpec[fcIndex];
            double Amc = computeAmc(ampSpec, fcIndex, valleyInds);
            */
            
            //Search for other peaks and compute Ams and Amcs
            int startInd = SignalProcUtils.freq2index(fc-0.5*f0, samplingRate, maxFreqIndex);
            int endInd = SignalProcUtils.freq2index(fc+0.5*f0, samplingRate, maxFreqIndex);
            int[] peakInds = MathUtils.getExtrema(absDBSpec, 1, 1, true, startInd, endInd);
            //MaryUtils.plot(absDBSpec, startInd, endInd);
            double[] Ams = null;
            double[] Amcs = null;
            voiceds[i] = 0.0;
            double bandMedVal;
            if (peakInds!=null)
            {
                if (peakInds.length>1)
                {
                    int total = 0;
                    for (n=0; n<peakInds.length; n++)
                    {
                        if (peakInds[n]!=fcIndex)
                            total++;
                    }
                    
                    Ams = new double[total];
                    Amcs = new double[total];
                    int counter = 0;
                    for (n=0; n<peakInds.length; n++)
                    {
                        if (peakInds[n]!=fcIndex)
                        {
                            //From db spec
                            Ams[counter] = absDBSpec[peakInds[n]];
                            Amcs[counter] = computeAmc(absDBSpec, peakInds[n], valleyInds);
                            //
                            
                            /*
                            //From linear spec
                            Ams[counter] = ampSpec[peakInds[n]];
                            Amcs[counter] = computeAmc(ampSpec, peakInds[n], valleyInds); 
                            //
                            */
                            counter++;
                        }
                    }
                }
                else //A very sharp single peak might exist in this range, check by comparing with rage median
                {
                    bandMedVal = MathUtils.median(absDBSpec, startInd, endInd);
                    if (Am-bandMedVal>SHARP_PEAK_AMP_DIFF_IN_DB)
                    {
                        voiceds[i] = 1.0;
                        //MaryUtils.plot(absDBSpec, startInd, endInd);
                    }
                }
            }
            //
            
            //Now do harmonic tests
            if (voiceds[i]!=1.0 && Amcs!=null)
            {
                double meanAmcs = MathUtils.mean(Amcs);
                
                vals1[i] = Amc/meanAmcs;
                vals3[i] = (Math.abs(fc-(double)(i+1)*f0)/((double)(i+1)*f0));
                
                //if (vals1[i]>CUMULATIVE_AMP_THRESHOLD && vals3[i]<(HARMONIC_DEVIATION_PERCENT/100.0))
                if (vals1[i]>CUMULATIVE_AMP_THRESHOLD && vals3[i]<(HARMONIC_DEVIATION_PERCENT/100.0))
                        voiceds[i]=1.0;
            }
            
            if (voiceds[i]!=1.0 && Ams!=null)
            {
                double maxAms = MathUtils.max(Ams);
                
                vals2[i] = Am-maxAms;
                //val2 = (MathUtils.amp2db(Am)-MathUtils.amp2db(maxAms)); //in amp should be converted to db
                vals3[i] = (Math.abs(fc-(double)(i+1)*f0)/((double)(i+1)*f0));
                
                //if (vals2[i]>MAXIMUM_AMP_THRESHOLD_IN_DB && vals3[i]<(HARMONIC_DEVIATION_PERCENT/100.0))
                if (vals2[i]>MAXIMUM_AMP_THRESHOLD_IN_DB && vals3[i]<(HARMONIC_DEVIATION_PERCENT/100.0))
                    voiceds[i]=1.0;
            }
            //
            
            //Save for senidng peak indices to output
            tmpPeakIndices[i] = fcIndex;
        }
        
        //Median filter voicing decisions
        voiceds = SignalProcUtils.medianFilter(voiceds, 3);
        
        //Forward look
        int maxVoicedHarmonicBand = -1;   
        for (i=0; i<voiceds.length-2; i++)
        {
            if (voiceds[i]==1.0 && voiceds[i+1]==0.0 && voiceds[i+2]==0.0)
            {
                maxVoicedHarmonicBand=i;
                break;
            }    
        }
        
        //Get the max freq. of voicing
        if (maxVoicedHarmonicBand>-1)
            output.maxFreqOfVoicing = (float)Math.min((maxVoicedHarmonicBand+0.5)*f0, 0.5*samplingRate);
        else
            output.maxFreqOfVoicing = 0.0f;
       
        if (isVoiced)
            output.maxFreqOfVoicing = MathUtils.CheckLimits(output.maxFreqOfVoicing, (float)(MINIMUM_TOTAL_HARMONICS*f0), (float)(MAXIMUM_TOTAL_HARMONICS*f0)); //From hnm with some limiting depending on f0
        else
            output.maxFreqOfVoicing = 0.0f; //Meaning the spectrum is completely unvoiced
        
        //Put harmonic peak indices into output as well
        if (output.maxFreqOfVoicing>0.0f)
        {
            int count = (int)Math.floor(output.maxFreqOfVoicing/f0+0.5);
            count = MathUtils.CheckLimits(count, 0, numHarmonics);
            if (count>0)
            {
                output.peakIndices = new int[count];
                System.arraycopy(tmpPeakIndices, 0, output.peakIndices, 0, count);
            }
        }
        //
        
        /*
        String strDebug = "";
        for (i=0; i<voiceds.length; i++)
            System.out.println(String.valueOf(voiceds[i]) + " " + String.valueOf(vals1[i]) + " " + String.valueOf(vals2[i]) + " " + String.valueOf(vals3[i]) + " ");
        */
        
        System.out.println("Max req of voicing=" + String.valueOf(output.maxFreqOfVoicing));
        
        //MaryUtils.plot(absDBSpec);
        
        return output;
    }
    
    private static double computeAmc(double[] spec, int fcIndex, int[] valleyInds)
    {
        double Amc = spec[fcIndex];

        if (valleyInds!=null)
        {
            //Find closest valley indices
            int vLeftInd = -1;
            int vRightInd = -1;
            int counter = 0;
            vLeftInd = 0;
            while (fcIndex>valleyInds[counter])
            {
                vLeftInd = valleyInds[counter];
                if (counter==valleyInds.length-1)
                    break;

                counter++;
            }

            counter = valleyInds.length-1;
            vRightInd = spec.length-1;
            while (valleyInds[counter]>fcIndex)
            {
                vRightInd = valleyInds[counter];
                if (counter==0)
                    break;

                counter--;
            }

            for (int i=vLeftInd; i<=vRightInd; i++)
            {
                if (i!=fcIndex)
                    Amc += spec[i];
            }
        }
        
        return Amc;
    }
    
    public static double estimateVoicingFromFrameSpectrum(double[] absSpec, int samplingRate, float f0) 
    {
        boolean isVoiced = false;
        
        int maxFreq = absSpec.length;
        int minFreqInd = SignalProcUtils.freq2index(VUV_SEARCH_MIN_HARMONIC_MULTIPLIER*f0, samplingRate, maxFreq-1);
        int maxFreqInd = SignalProcUtils.freq2index(VUV_SEARCH_MAX_HARMONIC_MULTIPLIER*f0, samplingRate, maxFreq-1);
        int harmonicNoFirst = ((int)Math.floor(VUV_SEARCH_MIN_HARMONIC_MULTIPLIER))+1;
        int harmonicNoLast = ((int)Math.floor(VUV_SEARCH_MAX_HARMONIC_MULTIPLIER));
        int numHarmonicsInRange = harmonicNoLast-harmonicNoFirst+1;
        int currentHarmonicInd;
        int i, j;
        int[] harmonicsInds = new int[numHarmonicsInRange];
        for (i=harmonicNoFirst; i<=harmonicNoLast; i++)
            harmonicsInds[i-harmonicNoFirst] = SignalProcUtils.freq2index(i*f0, samplingRate, maxFreq-1);
        
        double num = 0.0;
        double denum = 0.0;
        
        for (j=minFreqInd; j<harmonicsInds[0]-2; j++)
            num += absSpec[j]*absSpec[j];
        
        for (i=0; i<numHarmonicsInRange-1; i++)
        {
            for (j=harmonicsInds[i]+1+2; j<harmonicsInds[i+1]-2; j++)
                num += absSpec[j]*absSpec[j];
        }
        
        for (j=harmonicsInds[numHarmonicsInRange-1]+1+2; j<=maxFreqInd; j++)
            num += absSpec[j]*absSpec[j];
        
        for (j=minFreqInd; j<=maxFreqInd; j++)
            denum += absSpec[j]*absSpec[j];
        
        double E = num/denum;
        E = MathUtils.db(E);
        
        return E;
    }
    
    public static float[] estimateRefinedPitch(int fftSize, int samplingRateInHz, float leftNeighInHz, float rightNeighInHz, float searchStepInHz, float[] initialF0s, float[] maxFrequencyOfVoicings)
    {
        float[] f0s = new float[initialF0s.length];
        for (int i=0; i<initialF0s.length; i++)
            f0s[i] = estimateRefinedFramePitch(initialF0s[i], maxFrequencyOfVoicings[i], fftSize, samplingRateInHz, leftNeighInHz, rightNeighInHz, searchStepInHz);
        
        return f0s;
    }
    
    //Searches for a refined f0 value by searching the range [f0InHz-leftNeighInHz, f0InHz+rightNeighInHz] in steps searchStepInHz
    //This is done by evaluating the error function
    // E(f0New) = sum_i=0^maxVoicedIndex |iInHz-i*f0New|^2 
    //    for f0New=f0InHz-leftNeighInHz,f0InHz-leftNeighInHz+searchStepInHz,f0InHz-leftNeighInHz+2*searchStepInHz, ...,f0InHz+rightNeighInHz
    // and finding the value of f0New that minimizes it
    public static float estimateRefinedFramePitch(float f0InHz, float maxFreqOfVoicingInHz, int fftSize, int samplingRateInHz, float leftNeighInHz, float rightNeighInHz, float searchStepInHz) 
    {
        float refinedF0InHz = 0.0f;
        double E, Emin;
        int maxFreqIndex = fftSize/2+1;
        int maxVoicedFreqInd = SignalProcUtils.freq2index(maxFreqOfVoicingInHz, samplingRateInHz, maxFreqIndex);
        
        float f0New;
        int i;
        
        float[] freqIndsInHz = new float[maxVoicedFreqInd];
        for (i=0; i<maxVoicedFreqInd; i++)
            freqIndsInHz[i] = (float)SignalProcUtils.index2freq(i, samplingRateInHz, maxFreqIndex-1);
        
        Emin = Double.MAX_VALUE;
        refinedF0InHz = f0InHz;
        for (f0New=f0InHz-leftNeighInHz; f0New<=f0InHz+rightNeighInHz; f0New+=searchStepInHz)
        {
            E = 0.0;
            for (i=0; i<maxVoicedFreqInd; i++)
                E += Math.abs(freqIndsInHz[i]-i*f0New);
            
            if (E<Emin)
            {
                Emin = E;
                refinedF0InHz = f0New;
            }
        }
        
        return refinedF0InHz;
    } 
    
    public static void main(String[] args) throws UnsupportedAudioFileException, IOException
    {
        AudioInputStream inputAudio = AudioSystem.getAudioInputStream(new File(args[0]));
        int samplingRate = (int)inputAudio.getFormat().getSampleRate();
        AudioDoubleDataSource signal = new AudioDoubleDataSource(inputAudio);
        double [] x = signal.getAllData();
        
        float windowSizeInSeconds = 0.040f;
        float skipSizeInSeconds = 0.010f;
        int windowType = Window.BLACKMAN;
        float f0MinInHz = 60.0f;
        float f0MaxInHz = 500.0f;
        
        //Pitch refinement parameters
        float leftNeighInHz = 20.0f; 
        float rightNeighInHz = 20.0f;
        float searchStepInHz = 0.01f;
        //
        
        int fftSize = getDefaultFFTSize(samplingRate);

        float[] initialF0s = HnmPitchVoicingAnalyzer.estimateInitialPitch(x, samplingRate, windowSizeInSeconds, skipSizeInSeconds, f0MinInHz, f0MaxInHz, windowType);
        float[] maxFrequencyOfVoicings = HnmPitchVoicingAnalyzer.analyzeVoicings(x, samplingRate, windowSizeInSeconds, skipSizeInSeconds, fftSize, initialF0s);
        float[] f0s = HnmPitchVoicingAnalyzer.estimateRefinedPitch(fftSize, samplingRate, leftNeighInHz, rightNeighInHz, searchStepInHz, initialF0s, maxFrequencyOfVoicings);
        
        for (int i=0; i<f0s.length; i++)
            System.out.println(String.valueOf(i*skipSizeInSeconds+0.5f*windowSizeInSeconds) + " sec. InitialF0=" + String.valueOf(initialF0s[i]) + " RefinedF0="+ String.valueOf(f0s[i]));
        
        MaryUtils.plot(initialF0s);
        MaryUtils.plot(f0s);
    }
}

