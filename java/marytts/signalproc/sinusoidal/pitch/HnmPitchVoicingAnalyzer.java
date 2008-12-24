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
    
    //These are the three thresholds used in Stylianou for maximum voicing frequency estimation using the harmonic model
    public static double CUMULATIVE_AMP_THRESHOLD = 2.0;
    public static double MAXIMUM_AMP_THRESHOLD_IN_DB = 13.0;
    public static double HARMONIC_DEVIATION_PERCENT = 20.0;
    public static double SHARP_PEAK_AMP_DIFF_IN_DB = 1.0;
    public static int MINIMUM_TOTAL_HARMONICS = 20; //At least this much total harmonics will be included in voiced spectral region (effective only when f0>10.0)
    public static int MAXIMUM_TOTAL_HARMONICS = 50; //At most this much total harmonics will be included in voiced süpectral region (effective only when f0>10.0)
    //
    
    public float [] initialF0s;
    public float [] f0s;
    public boolean [] voicings;
    public float [] maxFrequencyOfVoicings;
    public int [][] frmInds;
    
    public HnmPitchVoicingAnalyzer()
    {
        initialF0s = null;
        f0s = null;
        voicings = null;
        maxFrequencyOfVoicings = null;
        frmInds = null;
    }
    
    public void estimateInitialPitch(double [] x, int samplingRate, float windowSizeInSeconds, float skipSizeInSeconds, 
                                     float f0MinInHz, float f0MaxInHz, float searchStepInHz) 
    {
        int PMax = (int)Math.floor(samplingRate/f0MinInHz+0.5);
        int PMin = (int)Math.floor(samplingRate/f0MaxInHz+0.5);
        
        int ws = (int)Math.floor(windowSizeInSeconds*samplingRate + 0.5);
        ws = Math.max(ws, (int)Math.floor(2.5*PMin+0.5));
        int ss = (int)Math.floor(skipSizeInSeconds*samplingRate + 0.5);
        int numfrm = (int)Math.floor(((double)x.length-ws)/ss+0.5);

        int numCandidates = PMax-PMin+1;
        
        double [] E = new double[numCandidates];
        int [][] minInds = new int[numfrm][];
        double [][] minEs = new double[numfrm][];
        
        int P;
        int i, t, l, k;
        double term1, term2, term3, r;

        double [] frm = new double[ws];
        Window win = Window.get(Window.HANNING, ws);
        double [] wgt2 = win.getCoeffs();
        
        for (t=0; t<ws; t++)
            wgt2[t] = wgt2[t]*wgt2[t];
        
        double tmpSum = 0.0;
        for (t=0; t<ws; t++)
            tmpSum += wgt2[t];
        
        for (t=0; t<ws; t++)
            wgt2[t] = wgt2[t]/tmpSum;
        
        double [] wgt4 = new double[ws];
        System.arraycopy(wgt2, 0, wgt4, 0, ws);
        for (t=0; t<ws; t++)
            wgt4[t] = wgt4[t]*wgt4[t];
        
        double termTmp = 0.0;
        for (t=0; t<ws; t++)
            termTmp += wgt4[t];
        
        initialF0s = new float[numfrm]; 
        frmInds = new int[numfrm][2];
        voicings = new boolean[numfrm];
        maxFrequencyOfVoicings = new float[numfrm];
        
        for (i=0; i<numfrm; i++)
        {
            Arrays.fill(frm, 0.0);
            System.arraycopy(x, i*ss, frm, 0, Math.min(ws, x.length-i*ss));
            frmInds[i][0] = i*ss;
            frmInds[i][1] = frmInds[i][0]+Math.min(ws, x.length-i*ss)-1;
            
            //MaryUtils.plot(frm);
            
            term1 = 0.0;
            for (t=0; t<ws; t++)
                term1 += frm[t]*frm[t]*wgt2[t];
            
            for (P=PMin; P<=PMax; P++)
            {
                term2 = 0.0;
                for (l=0; l<ws; l++)
                {
                    r=0.0;
                    for (t=0; t<ws-l*P; t++)
                        r += frm[t]*wgt2[t]*frm[t+l*P]*wgt2[t+l*P];

                    term2 += r;
                }
                term2 *= P;

                term3 = 1.0-P*termTmp;

                E[P-PMin] = (term1-term2)/(term1*term3);
            }

            //MaryUtils.plot(E, true, 1000);
            
            minInds[i] = MathUtils.getExtrema(E, 2, 2, false);
            if (minInds[i]!=null)
            {
                minEs[i] = new double[minInds[i].length];
                for (t=0; t<minInds[i].length; t++)
                    minEs[i][t] = E[minInds[i][t]];
            }
            else
                minEs[i] = null;
        }

        //Search for local minimum error paths to assign pitch values
        //Previous and next <neigh> neighbors are used for searching of the local total minimum
        int neigh = 0;
        int [] totalNodes = new int[2*neigh+1];
        int minLocalEInd;
        int [][] pathInds;
        double [] localEs;
        
        for (i=0; i<numfrm; i++)
        {
            if (minEs!=null && minInds[i]!=null)
            {
                for (t=-neigh; t<=neigh; t++)
                {
                    if (i+t>=0 && i+t<minEs.length && minEs[i+t]!=null)
                        totalNodes[t+neigh] = minEs[i+t].length;
                    else
                        totalNodes[t+neigh] = 1;
                }

                //Here is a factorial design of all possible paths
                pathInds = MathUtils.factorialDesign(totalNodes);
                localEs = new double[pathInds.length];

                for (k=0; k<pathInds.length; k++)
                {
                    localEs[k] = 0.0;
                    for (t=0; t<pathInds[k].length; t++)
                    {
                        //System.out.println(String.valueOf(i) + " " + String.valueOf(k) + " " + String.valueOf(t) + " ");
                        
                        if (minEs!=null)
                            if (i-neigh+t>=0)
                                if (i-neigh+t<minEs.length)
                                    if (minEs[i-neigh+t]!=null)
                                        if (pathInds[k][t]<minEs[i-neigh+t].length)
                                            localEs[k] += minEs[i-neigh+t][pathInds[k][t]];
                    }
                }

                minLocalEInd = MathUtils.getMinIndex(localEs);
                //System.out.println(String.valueOf(minLocalEInd));
                initialF0s[i] = ((float)samplingRate)/(minInds[i][pathInds[minLocalEInd][neigh]]+PMin);
            }
            else
                initialF0s[i] = 0.0f;
            
            System.out.println(String.valueOf(initialF0s[i]));
        }
    }
    
    public void analyzeVoicings(double [] x, int samplingRate) 
    {
        double [] frm;

        for (int n=0; n<initialF0s.length; n++)
        {
            frm = new double[frmInds[n][1]-frmInds[n][0]+1];
            
            Arrays.fill(frm, 0.0);
            System.arraycopy(x, frmInds[n][0], frm, 0, frmInds[n][1]-frmInds[n][0]);
            
            NonharmonicSinusoidalSpeechFrame frameSins = null;

            int fftSize = SinusoidalAnalyzer.getDefaultFFTSize(samplingRate);
            if (fftSize<frm.length)
                fftSize = frm.length;

            if (fftSize % 2 == 1)
                fftSize++;

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
            double[] YAbsDB = MathUtils.dft2ampdb(Y, 0, maxFreq-1);
            //
            
            int [] peakInds = MathUtils.getExtrema(YAbsDB, 3, 3, true);
            int [] valleyInds = MathUtils.getExtrema(YAbsDB, 1, 1, false);
            voicings[n] = estimateVoicingFromFrameSpectrum(YAbsDB, samplingRate, initialF0s[n], peakInds, valleyInds);

            if (voicings[n]==false)
                initialF0s[n] = 0.0f;
            
            VoicingAnalysisOutputData vo = null;
            if (voicings[n])
            {
                vo = estimateMaxFrequencyOfVoicingsFrame(YAbsDB, samplingRate, initialF0s[n], voicings[n]);
                maxFrequencyOfVoicings[n] = vo.maxFreqOfVoicing;
            }
            else
                maxFrequencyOfVoicings[n] = 0.0f;
            
            if (voicings[n])
                System.out.println("Time=" + String.valueOf(0.5*(frmInds[n][1]+frmInds[n][0])/samplingRate)+ " sec." + " f0=" + String.valueOf(initialF0s[n]) + " Hz." + " Voiced" + " MaxVFreq=" + String.valueOf(maxFrequencyOfVoicings[n]));
            else
                System.out.println("Time=" + String.valueOf(0.5*(frmInds[n][1]+frmInds[n][0])/samplingRate)+ " sec." + " f0=" + String.valueOf(initialF0s[n]) + " Hz." + " Unvoiced" + " MaxVFreq=" + String.valueOf(maxFrequencyOfVoicings[n]));
        }
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
    
    public static boolean estimateVoicingFromFrameSpectrum(double [] absSpec, int samplingRate, float initialF0, int [] peakInds, int [] valleyInds) 
    {
        boolean bVoicing = false;
        int maxFreq = absSpec.length-1;
        double numTerm = 0.0;
        double denTerm = 0.0;
        
        if (peakInds!=null)
        {
            double f0StartHz = (1.0 - HARMONICS_NEIGH)*initialF0;
            double f0EndHz = (NUM_HARMONICS_FOR_VOICING + HARMONICS_NEIGH)*initialF0;
            double f0NeighHz = 10.0f;
            int freqStartInd = SignalProcUtils.freq2index(f0StartHz, samplingRate, maxFreq);
            int freqEndInd = SignalProcUtils.freq2index(f0EndHz, samplingRate, maxFreq);
            int f0NeighInd = SignalProcUtils.freq2index(f0NeighHz, samplingRate, maxFreq);

            int i, j;
            int freqInd;

            //Signal energy around peaks
            for (i=0; i<peakInds.length; i++)
            {
                if (peakInds[i]>=freqStartInd)
                {
                    if (peakInds[i]<=freqEndInd)
                    {
                        for (j=peakInds[i]-f0NeighInd; j<=peakInds[i]+f0NeighInd; j++)
                            numTerm += absSpec[j]*absSpec[j];
                    }
                    else
                        break;
                }
            }
            //

            //Signal energy around valleys
            if (valleyInds!=null)
            {
                for (i=0; i<valleyInds.length; i++)
                {
                    if (valleyInds[i]>=freqStartInd)
                    {
                        if (valleyInds[i]<=freqEndInd)
                        {
                            for (j=valleyInds[i]-f0NeighInd; j<=valleyInds[i]+f0NeighInd; j++)
                                denTerm += absSpec[j]*absSpec[j];
                        }
                        else
                            break;
                    }
                }
            }
            //
        }
        
        double E = MathUtils.db((numTerm+1e-20)/(denTerm+1e-20)); 
        if (E>6.0)
            bVoicing = true;

        //System.out.println(String.valueOf(E));

        return bVoicing;
    }
    
    public void estimateRefinedPitch() 
    {
        
    }

    public void analyze(double [] x, int samplingRate, float windowSizeInSeconds, float skipSizeInSeconds,
                        int windowType, float f0MinInHz, float f0MaxInHz, float searchStepInHz) 
    {
        estimateInitialPitch(x, samplingRate, windowSizeInSeconds, skipSizeInSeconds, f0MinInHz, f0MaxInHz, searchStepInHz);
        analyzeVoicings(x, samplingRate);
        estimateRefinedPitch();
    }   
    
    public static void main(String[] args) throws UnsupportedAudioFileException, IOException
    {
        AudioInputStream inputAudio = AudioSystem.getAudioInputStream(new File(args[0]));
        int samplingRate = (int)inputAudio.getFormat().getSampleRate();
        AudioDoubleDataSource signal = new AudioDoubleDataSource(inputAudio);
        double [] x = signal.getAllData();
        
        float windowSizeInSeconds = 0.040f;
        float skipSizeInSeconds = 0.005f;
        int windowType = Window.HANNING;
        float f0MinInHz = 60.0f;
        float f0MaxInHz = 500.0f;
        float searchStepInHz = 0.5f;
        
        HnmPitchVoicingAnalyzer h = new HnmPitchVoicingAnalyzer();
        h.analyze(x, samplingRate, windowSizeInSeconds, skipSizeInSeconds, 
                  windowType, f0MinInHz, f0MaxInHz, searchStepInHz);
        
        /*
        for (int i=0; i<h.initialF0s.length; i++)
            System.out.println(String.valueOf(i*skipSizeInSeconds+0.5f*windowSizeInSeconds) + " sec. = " + String.valueOf(h.initialF0s[i]));
            */
    }
}
