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
package marytts.signalproc.sinusoidal.hnm;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import marytts.signalproc.analysis.RegularizedCepstralEnvelopeEstimator;
import marytts.signalproc.filter.HighPassFilter;
import marytts.signalproc.sinusoidal.SinusoidalTrack;
import marytts.signalproc.window.Window;
import marytts.util.MaryUtils;
import marytts.util.data.BufferedDoubleDataSource;
import marytts.util.data.audio.AudioDoubleDataSource;
import marytts.util.data.audio.DDSAudioInputStream;
import marytts.util.math.MathUtils;
import marytts.util.signal.SignalProcUtils;

/**
 * Synthesis using harmonics plus noise model
 * 
 * @author Oytun T&uumlrk
 *
 */
public class HnmSynthesizer {
    
    public static final double ENERGY_TRIANGLE_LOWER_VALUE = 0.5;
    public static final double ENERGY_TRIANGLE_UPPER_VALUE = 1.0;

    public HnmSynthesizer()
    {

    }

    public HnmSynthesizedSignal syntesize(HnmSpeechSignal hnmSignal, 
            float[] tScales,
            float[] tScalesTimes,
            float[] pScales,
            float[] pScalesTimes)
    {
        HnmSynthesizedSignal s = new HnmSynthesizedSignal();
        
        s.harmonicPart = synthesizeHarmonicPartLinearPhaseInterpolation(hnmSignal, tScales, tScalesTimes, pScales, pScalesTimes);
        
        if (hnmSignal.frames[0].n instanceof FrameNoisePartLpc)
        {
            s.noisePart = synthesizeNoisePartLpc(hnmSignal, tScales, tScalesTimes, pScales, pScalesTimes);
        }
        else if (hnmSignal.frames[0].n instanceof FrameNoisePartPseudoHarmonic)
            s.noisePart = synthesizeNoisePartPseudoHarmonic(hnmSignal, tScales, tScalesTimes, pScales, pScalesTimes);

        return s;
    }

    public double[] synthesizeHarmonicPartLinearPhaseInterpolation(HnmSpeechSignal hnmSignal, 
            float[] tScales,
            float[] tScalesTimes,
            float[] pScales,
            float[] pScalesTimes)
    {
        double[] harmonicPart = null;

        int i, k, n;
        float t, tsi, tsiPlusOne; //Time in seconds
        int startIndex, endIndex;
        double akt;
        int numHarmonicsCurrentFrame;
        int maxNumHarmonics = 0;
        for (i=0; i<hnmSignal.frames.length; i++)
        {
            if (hnmSignal.frames[i].maximumFrequencyOfVoicingInHz>0.0f && hnmSignal.frames[i].h.phases!=null)
            {
                numHarmonicsCurrentFrame = hnmSignal.frames[i].h.phases.length;
                if (numHarmonicsCurrentFrame>maxNumHarmonics)
                    maxNumHarmonics = numHarmonicsCurrentFrame;
            }  
        }

        double[] aksis = null;
        double[] aksiPlusOnes = null;
        float[] phasekis = null;
        float[] phasekiPlusOnes = null;
        if (maxNumHarmonics>0)
        {
            aksis = new double[maxNumHarmonics];
            Arrays.fill(aksis, 0.0);
            aksiPlusOnes = new double[maxNumHarmonics];
            Arrays.fill(aksis, 0.0);
            phasekis = new float[maxNumHarmonics];
            Arrays.fill(phasekis, 0.0f);
            phasekiPlusOnes = new float[maxNumHarmonics];
            Arrays.fill(phasekiPlusOnes, 0.0f);
        }

        float f0InHz, f0InHzNext, f0InHzPrev, f0av;
        f0InHzPrev = 0.0f;
        int lastPeriodInSamples = 0;
        double ht;
        float phasekt = 0.0f;

        float phasekiPlusOneEstimate = 0.0f;
        int Mk;
        boolean isVoiced, isNextVoiced, isPrevVoiced;
        int origLen = SignalProcUtils.time2sample(hnmSignal.originalDurationInSeconds, hnmSignal.samplingRateInHz);
        harmonicPart = new double[origLen]; //In fact, this should be prosody scaled length when you implement prosody modifications
        Arrays.fill(harmonicPart, 0.0);

        for (i=0; i<hnmSignal.frames.length; i++)
        {
            isVoiced = ((hnmSignal.frames[i].maximumFrequencyOfVoicingInHz>0.0f) ? true : false);
            if (i<hnmSignal.frames.length-1)
                isNextVoiced = ((hnmSignal.frames[i+1].maximumFrequencyOfVoicingInHz>0.0f) ? true : false);
            else
                isNextVoiced = false;

            if (i>0)
                isPrevVoiced = ((hnmSignal.frames[i-1].maximumFrequencyOfVoicingInHz>0.0f) ? true : false);
            else
                isPrevVoiced = false;

            if (i==0)
                tsi = 0.0f;
            else
                tsi = hnmSignal.frames[i].tAnalysisInSeconds;
            startIndex = SignalProcUtils.time2sample(tsi, hnmSignal.samplingRateInHz);
            if (isNextVoiced)
            {
                if (i==hnmSignal.frames.length-1)
                    tsiPlusOne = hnmSignal.originalDurationInSeconds;
                else
                    tsiPlusOne = hnmSignal.frames[i+1].tAnalysisInSeconds;
                endIndex = SignalProcUtils.time2sample(tsiPlusOne, hnmSignal.samplingRateInHz);
            }
            else
            {
                if (i==hnmSignal.frames.length-1)
                {
                    tsiPlusOne = hnmSignal.originalDurationInSeconds;
                    endIndex = SignalProcUtils.time2sample(tsiPlusOne, hnmSignal.samplingRateInHz);
                }
                else
                {
                    endIndex = startIndex + lastPeriodInSamples;
                    tsiPlusOne = SignalProcUtils.sample2time(endIndex, hnmSignal.samplingRateInHz);
                }
            }

            if (hnmSignal.frames[i].h.phases!=null)
                numHarmonicsCurrentFrame = hnmSignal.frames[i].h.phases.length;
            else
                numHarmonicsCurrentFrame = 0;

            for (n=startIndex; n<endIndex; n++)
            {
                t = SignalProcUtils.sample2time(n, hnmSignal.samplingRateInHz);

                f0InHz = hnmSignal.frames[i].h.f0InHz;
                if (isNextVoiced &&  hnmSignal.frames[i+1].h.phases!=null)
                    f0InHzNext = hnmSignal.frames[i+1].h.f0InHz;
                else
                    f0InHzNext = f0InHz;
                
                if (i>0)
                    f0InHzPrev = hnmSignal.frames[i-1].h.f0InHz;

                f0av = 0.5f*(f0InHz+f0InHzNext);
                
                /*
                TO DO: Synthesis times has to change with prosody modifications, also check amplitude and phase envelope resampling
                double pscale = 1.5;
                f0InHz *= pscale;
                f0InHzNext *= pscale;
                f0av *= pscale;
                */
                
                harmonicPart[n] = 0.0;
                for (k=0; k<Math.min(numHarmonicsCurrentFrame,maxNumHarmonics); k++)
                {
                    //Estimate amplitude
                    if (i==0)
                    {
                        aksis[k] = RegularizedCepstralEnvelopeEstimator.cepstrum2linearSpectrumValue(hnmSignal.frames[i].h.ceps, (k+1)*f0InHz, hnmSignal.samplingRateInHz);
                        //aksis[k] = hnmSignal.frames[i].h.ceps[k]; //Use amplitudes directly without cepstrum method
                    }
                    else
                    {
                        if (isPrevVoiced)
                            aksis[k] = aksiPlusOnes[k]; //from previous
                        else
                            aksis[k] = 0.0;
                    }

                    if (isNextVoiced && hnmSignal.frames[i+1].h.ceps!=null)
                    {
                        if ((k+1)*f0InHzNext<hnmSignal.frames[i+1].maximumFrequencyOfVoicingInHz)
                        {     
                            aksiPlusOnes[k] = RegularizedCepstralEnvelopeEstimator.cepstrum2linearSpectrumValue(hnmSignal.frames[i+1].h.ceps , (k+1)*f0InHzNext, hnmSignal.samplingRateInHz);
                            //aksiPlusOnes[k] = hnmSignal.frames[i+1].h.ceps[k]; //Use amplitudes directly without cepstrum method
                        }
                        else
                            aksiPlusOnes[k] = 0.0;
                    }
                    else
                        aksiPlusOnes[k] = 0.0;

                    akt = aksis[k] + (aksiPlusOnes[k]-aksis[k])*(t-tsi)/(tsiPlusOne-tsi);
                    //

                    //Estimate phase                     
                    if (isVoiced)
                        phasekis[k] = hnmSignal.frames[i].h.phases[k];

                    if (isNextVoiced && hnmSignal.frames[i+1].h.phases!=null && k<hnmSignal.frames[i+1].h.phases.length)
                        phasekiPlusOnes[k] = hnmSignal.frames[i+1].h.phases[k];
                    else
                        phasekiPlusOnes[k] = (float)(MathUtils.TWOPI*(Math.random()-0.5));

                    if (!isVoiced)
                        phasekis[k] = (float)( phasekiPlusOnes[k] - (k+1)*MathUtils.TWOPI*f0InHzNext*(tsiPlusOne-tsi) ); //Equation (3.54)

                    if (!(isNextVoiced && hnmSignal.frames[i+1].h.phases!=null && k<hnmSignal.frames[i+1].h.phases.length))
                        phasekiPlusOnes[k] = (float)( phasekis[k] + (k+1)*MathUtils.TWOPI*f0InHz*(tsiPlusOne-tsi) ); //Equation (3.55)

                    phasekiPlusOneEstimate = (float)( phasekis[k] + (k+1)*MathUtils.TWOPI*f0av*(tsiPlusOne-tsi));
                    Mk = (int)Math.floor((phasekiPlusOneEstimate-phasekiPlusOnes[k])/MathUtils.TWOPI + 0.5);
                    phasekt = (float)( phasekis[k] + (phasekiPlusOnes[k]+MathUtils.TWOPI*Mk-phasekis[k])*(t-tsi)/(tsiPlusOne-tsi) );
                    //

                    harmonicPart[n] += akt*Math.cos(phasekt);
                }
            }

            lastPeriodInSamples = SignalProcUtils.time2sample(tsiPlusOne - tsi, hnmSignal.samplingRateInHz);
        }

        harmonicPart = MathUtils.divide(harmonicPart, 32768.0);
        
        return harmonicPart;
    }
    
    //LPC based noise model + OLA approach
    public double[] synthesizeNoisePartLpc(HnmSpeechSignal hnmSignal, 
            float[] tScales,
            float[] tScalesTimes,
            float[] pScales,
            float[] pScalesTimes)
    {
        double[] noisePart = null;
        int i;
        boolean isNoised, isNextNoised;
        boolean isVoiced;
        int lpOrder = 0;
        float t;
        float tsi = 0.0f;
        float tsiNext; //Time in seconds
        int startIndex = 0;
        int startIndexNext;
        int origLen = SignalProcUtils.time2sample(hnmSignal.originalDurationInSeconds, hnmSignal.samplingRateInHz);
        
        for (i=0; i<hnmSignal.frames.length; i++)
        {
            isNoised = ((hnmSignal.frames[i].maximumFrequencyOfVoicingInHz<0.5f*hnmSignal.samplingRateInHz) ? true : false);
            if (isNoised)
            {
                lpOrder = ((FrameNoisePartLpc)hnmSignal.frames[i].n).lpCoeffs.length;
                break;
            }
        }

        if (lpOrder>0) //At least one noisy frame with LP coefficients exist
        {
            noisePart = new double[origLen]; //In fact, this should be prosody scaled length when you implement prosody modifications
            Arrays.fill(noisePart, 0.0);
            double[] winWgtSum = new double[origLen]; //In fact, this should be prosody scaled length when you implement prosody modifications
            Arrays.fill(winWgtSum, 0.0);

            Window winNoise;
            int windowType = Window.HAMMING;
            double[] x;
            double[] xWindowed;
            double[] y;
            double[] yWindowed;
            double[] yFiltered;
            double[] wgt;
            double[] yInitial = new double[lpOrder];
            Arrays.fill(yInitial, 0.0); //Start with zero initial conditions
            int n;
            int fftSizeNoise = SignalProcUtils.getDFTSize(hnmSignal.samplingRateInHz);

            int wsNoise = 0;
            int wsNoiseNext = 0;

            boolean bFirstSyntheticFrame = true;
            boolean bLastSyntheticFrame = false;
            boolean isDisplay = false;
            int T0 = 0;
            int T0Next = 0;
            
            //Noise source of full length
            double[] noiseSourceHpf = null;
            //noiseSource = SignalProcUtils.getNoise(HnmAnalyzer.FIXED_MAX_FREQ_OF_VOICING_FOR_QUICK_TEST, 0.5f*hnmSignal.samplingRateInHz, HnmAnalyzer.HPF_TRANSITION_BANDWIDTH_IN_HZ,  hnmSignal.samplingRateInHz, (int)(1.1*origLen)); //Pink noise full signal length, works OK
            if (HnmAnalyzer.FIXED_MAX_FREQ_OF_VOICING_FOR_QUICK_TEST<0.5*hnmSignal.samplingRateInHz)
                noiseSourceHpf = SignalProcUtils.getNoise(HnmAnalyzer.FIXED_MAX_FREQ_OF_VOICING_FOR_QUICK_TEST, HnmAnalyzer.FIXED_MAX_FREQ_OF_NOISE_FOR_QUICK_TEST, HnmAnalyzer.HPF_TRANSITION_BANDWIDTH_IN_HZ,  hnmSignal.samplingRateInHz, (int)(1.1*origLen)); //Pink noise full signal length, works OK
            if (noiseSourceHpf!=null)
                MathUtils.adjustMeanVariance(noiseSourceHpf, 0.0, 1.0);
            double[] noiseSourceFull = SignalProcUtils.getWhiteNoise((int)(1.1*origLen), 1.0); //White noise full signal length, works OK
            MathUtils.adjustMeanVariance(noiseSourceFull, 0.0, 1.0);
            /*
            //Write the noise source to a wav file for checking
            AudioInputStream inputAudio = null;
            try {
                inputAudio = AudioSystem.getAudioInputStream(new File("d:\\hn.wav"));
            } catch (UnsupportedAudioFileException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            DDSAudioInputStream outputAudio = new DDSAudioInputStream(new BufferedDoubleDataSource(noiseSource), inputAudio.getFormat());
            try {
                AudioSystem.write(outputAudio, AudioFileFormat.Type.WAVE, new File("d:\\noiseSource.wav"));
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            */
            //
            
            for (i=0; i<hnmSignal.frames.length; i++)
            {
                isVoiced = ((hnmSignal.frames[i].maximumFrequencyOfVoicingInHz>0.0f) ? true : false);
                isNoised = ((hnmSignal.frames[i].maximumFrequencyOfVoicingInHz<0.5f*hnmSignal.samplingRateInHz) ? true : false);
                if (i<hnmSignal.frames.length-1)
                    isNextNoised = ((hnmSignal.frames[i+1].maximumFrequencyOfVoicingInHz<0.5f*hnmSignal.samplingRateInHz) ? true : false);
                else
                    isNextNoised = true;
                
                if (i==0)
                    tsi = 0.0f;
                else
                    tsi = Math.max(0.0f, hnmSignal.frames[i].tAnalysisInSeconds-0.5f*hnmSignal.windowDurationInSecondsNoise);
                
                /*
                if (tsi>0.250 && tsi<0.26)
                    isDisplay = true;
                else if (tsi>0.600 && tsi<0.61)
                    isDisplay = true;
                else
                    isDisplay = false;
                    */

                if (hnmSignal.frames[i].maximumFrequencyOfVoicingInHz>0.0f)
                    T0 = SignalProcUtils.time2sample(1.0/hnmSignal.frames[i].h.f0InHz, hnmSignal.samplingRateInHz);
                else
                    T0 = SignalProcUtils.time2sample(1.0/HnmAnalyzer.NOISE_F0_IN_HZ, hnmSignal.samplingRateInHz);

                wsNoise = 2*T0;
                if (wsNoise%2==0) //Always use an odd window size to have a zero-phase analysis window
                    wsNoise++; 

                startIndex = SignalProcUtils.time2sample(tsi, hnmSignal.samplingRateInHz);

                if (i<hnmSignal.frames.length-1)
                {
                    tsiNext = Math.max(0.0f, hnmSignal.frames[i+1].tAnalysisInSeconds-0.5f*hnmSignal.windowDurationInSecondsNoise);
                    
                    if (hnmSignal.frames[i+1].maximumFrequencyOfVoicingInHz>0.0f)
                        T0Next = SignalProcUtils.time2sample(1.0/hnmSignal.frames[i+1].h.f0InHz, hnmSignal.samplingRateInHz);
                    else
                        T0Next = SignalProcUtils.time2sample(1.0/HnmAnalyzer.NOISE_F0_IN_HZ, hnmSignal.samplingRateInHz);
                    
                    wsNoiseNext = 2*T0Next;
                    if (wsNoiseNext%2==0) //Always use an odd window size to have a zero-phase analysis window
                        wsNoiseNext++; 
                    
                    startIndexNext = SignalProcUtils.time2sample(tsiNext, hnmSignal.samplingRateInHz);
                }
                else
                {
                    T0Next = T0;
                    wsNoiseNext = wsNoise;
                    startIndexNext = origLen-1;
                    tsiNext = SignalProcUtils.sample2time(startIndexNext, hnmSignal.samplingRateInHz); 
                }
                
                //Compute window
                winNoise = Window.get(windowType, wsNoise);
                winNoise.normalizeSquaredSum(1.0f);
                wgt = winNoise.getCoeffs();
                //

                if (isNoised)
                {
                    //x = SignalProcUtils.getWhiteNoiseOfVariance(wsNoise, 1.0); //Variance specified white noise
                    //x = SignalProcUtils.getWhiteNoise(wsNoise, 0.5); //Absolute value limited white noise
                    //x = SignalProcUtils.getNoise(hnmSignal.frames[i].maximumFrequencyOfVoicingInHz, 0.5f*hnmSignal.samplingRateInHz, hnmSignal.samplingRateInHz, wsNoise); //Pink noise
                    //x = SignalProcUtils.getNoise(hnmSignal.frames[i].maximumFrequencyOfVoicingInHz, 4000.0f, hnmSignal.samplingRateInHz, wsNoise); //Pink noise
                    
                    if (isVoiced)
                    {
                        //Get pink noise from continuous buffer
                        x = new double[wsNoise]; 
                        System.arraycopy(noiseSourceHpf, startIndex, x, 0, wsNoise); 
                        //
                    }
                    else
                    {
                        //Get white noise from continuous buffer
                        x = new double[wsNoise]; 
                        System.arraycopy(noiseSourceFull, startIndex, x, 0, wsNoise);   
                        //
                    }
                        
                    if (isDisplay)
                    {
                        SignalProcUtils.displayDFTSpectrumInDBNoWindowing(x, fftSizeNoise); 
                        SignalProcUtils.displayDFTSpectrumInDB(x, fftSizeNoise); 
                        SignalProcUtils.displayLPSpectrumInDB(((FrameNoisePartLpc)hnmSignal.frames[i].n).lpCoeffs, ((FrameNoisePartLpc)hnmSignal.frames[i].n).gain, fftSizeNoise);
                    } 

                    //y = new double[wsNoise]; System.arraycopy(x, 0, y, 0, wsNoise); //What if we directly copy noise into output (i.e. does overlap-add work OK?)
                    //y = SignalProcUtils.arFilter(x, ((FrameNoisePartLpc)hnmSignal.frames[i].n).lpCoeffs, ((FrameNoisePartLpc)hnmSignal.frames[i].n).gain, yInitial);
                    xWindowed = winNoise.apply(x, 0);
                    y = SignalProcUtils.arFilterFreqDomain(xWindowed, ((FrameNoisePartLpc)hnmSignal.frames[i].n).lpCoeffs, ((FrameNoisePartLpc)hnmSignal.frames[i].n).gain);
                    
                    if (isDisplay)
                        SignalProcUtils.displayDFTSpectrumInDBNoWindowing(y, fftSizeNoise); 

                    /*
                    //Highpass filtering
                    if (hnmSignal.frames[i].maximumFrequencyOfVoicingInHz>0.0f)
                        y = SignalProcUtils.fdFilter(y, hnmSignal.frames[i].maximumFrequencyOfVoicingInHz, 0.5f*hnmSignal.samplingRateInHz, hnmSignal.samplingRateInHz, fftSizeNoise);
                    //
                    */

                    if (isDisplay)
                    {
                        SignalProcUtils.displayDFTSpectrumInDBNoWindowing(y, fftSizeNoise); 
                        MaryUtils.plot(x);
                        MaryUtils.plot(y);
                    }

                    if (i==hnmSignal.frames.length-1)
                        bLastSyntheticFrame = true;

                    //Overlap-add
                    for (n=startIndex; n<Math.min(startIndex+wsNoise, noisePart.length); n++)
                    {
                        noisePart[n] += y[n-startIndex]*wgt[n-startIndex]; 
                        winWgtSum[n] += wgt[n-startIndex]*wgt[n-startIndex];
                    }
                    //
                    
                    /*
                    //Save last lpOrder filter outputs to use as initial conditions for the next frame, provided that next frame is noised, otherwise set them to zero again
                    if (isNextNoised)
                    {
                        int count = 0;
                        for (n=startIndexNext-lpOrder; n<=Math.min(startIndex+wsNoise, noisePart.length); n++)
                        {
                            if (n<0)
                                yInitial[count++] = 0.0;
                            else
                                yInitial[count++] = noisePart[n]/winWgtSum[n];
                            
                            if (count==lpOrder)
                                break;
                        }
                    }
                    else
                        Arrays.fill(yInitial, 0.0);
                    //
                    */
                }

                System.out.println("LPC noise synthesis complete at " + String.valueOf(hnmSignal.frames[i].tAnalysisInSeconds) + "s. for frame " + String.valueOf(i+1) + " of " + String.valueOf(hnmSignal.frames.length) + "..." + String.valueOf(startIndex) + "-" + String.valueOf(startIndex+wsNoise)); 
            }

            //Normalize according to total window weights per sample
            for (n=0; n<origLen; n++)
            {
                if (winWgtSum[n]>0.0)
                    noisePart[n] /= winWgtSum[n];
            }
            //
        }
        
        //Now, apply the triangular noise envelope for voiced parts
        double[] enEnv;
        int enEnvLen;
        tsiNext = 0;
        int l1, lMid, l2;
        for (i=0; i<hnmSignal.frames.length; i++)
        {
            isVoiced = ((hnmSignal.frames[i].maximumFrequencyOfVoicingInHz>0.0f) ? true : false);
            if (isVoiced)
            {
                if (i==0)
                    tsi = 0.0f; 
                else
                    tsi = hnmSignal.frames[i].tAnalysisInSeconds;

                startIndex = SignalProcUtils.time2sample(tsi, hnmSignal.samplingRateInHz);
                
                if (i<hnmSignal.frames.length-1)
                {
                    tsiNext = Math.max(0.0f, hnmSignal.frames[i+1].tAnalysisInSeconds);
                    startIndexNext = SignalProcUtils.time2sample(tsiNext, hnmSignal.samplingRateInHz);
                }
                else
                {
                    startIndexNext = origLen-1;
                    tsiNext = SignalProcUtils.sample2time(startIndexNext, hnmSignal.samplingRateInHz);
                }

                enEnvLen = startIndexNext-startIndex+1;
                enEnv = new double[enEnvLen];
                
                int n;
                l1 = SignalProcUtils.time2sample(0.15*(tsiNext-tsi), hnmSignal.samplingRateInHz);
                l2 = SignalProcUtils.time2sample(0.85*(tsiNext-tsi), hnmSignal.samplingRateInHz);
                lMid = (int)Math.floor(0.5*(l1+l2)+0.5);
                for (n=0; n<l1; n++)
                    enEnv[n] = ENERGY_TRIANGLE_LOWER_VALUE;
                for (n=l1; n<lMid; n++)
                    enEnv[n] = (n-l1)*(ENERGY_TRIANGLE_UPPER_VALUE-ENERGY_TRIANGLE_LOWER_VALUE)/(lMid-l1)+ENERGY_TRIANGLE_LOWER_VALUE;
                for (n=lMid; n<l2; n++)
                    enEnv[n] = (n-lMid)*(ENERGY_TRIANGLE_LOWER_VALUE-ENERGY_TRIANGLE_UPPER_VALUE)/(l2-lMid)+ENERGY_TRIANGLE_UPPER_VALUE;
                for (n=l2; n<enEnvLen; n++)
                    enEnv[n] = ENERGY_TRIANGLE_LOWER_VALUE;
                
                for (n=startIndex; n<=startIndexNext; n++)
                    noisePart[n] *= enEnv[n-startIndex];
            }
        }
        
        //We need to get rid of this normalization and make sure the noise part gain level matches the original signal without modifications
        noisePart = MathUtils.divide(noisePart, 32768.0);
        //
        
        return noisePart;
    }

    //Pseudo harmonics based noise generation for pseudo periods
    public static double[] synthesizeNoisePartPseudoHarmonic(HnmSpeechSignal hnmSignal, 
            float[] tScales,
            float[] tScalesTimes,
            float[] pScales,
            float[] pScalesTimes)
    {
        int i, k, n;
        float t, tsi, tsiPlusOne; //Time in seconds
        int startIndex, endIndex;

        double[] noisePart = null;
        int origLen = SignalProcUtils.time2sample(hnmSignal.originalDurationInSeconds, hnmSignal.samplingRateInHz);
        noisePart = new double[origLen]; //In fact, this should be prosody scaled length when you implement prosody modifications
        Arrays.fill(noisePart, 0.0);

        double akt;
        int startPseudoHarmonicNo, startPseudoHarmonicNoNext;
        int endPseudoHarmonicNo = (int)Math.floor((0.5*hnmSignal.samplingRateInHz)/HnmAnalyzer.NOISE_F0_IN_HZ+0.5);

        double[] aksis = null;
        double[] aksiPlusOnes = null;
        float[] phasekis = null;

        int numNoiseHarmonicsCurrentFrame;
        int numNoiseHarmonicsNextFrame = 0;
        int maxNumHarmonics = 0;
        for (i=0; i<hnmSignal.frames.length; i++)
        {
            if (hnmSignal.frames[i].maximumFrequencyOfVoicingInHz>0.0f && hnmSignal.frames[i].maximumFrequencyOfVoicingInHz<0.5*hnmSignal.samplingRateInHz)
                startPseudoHarmonicNo = (int)(Math.floor((hnmSignal.frames[i].maximumFrequencyOfVoicingInHz+0.5*HnmAnalyzer.NOISE_F0_IN_HZ)/HnmAnalyzer.NOISE_F0_IN_HZ))-1; 
            else
                startPseudoHarmonicNo = 0;
            
            numNoiseHarmonicsCurrentFrame = endPseudoHarmonicNo-startPseudoHarmonicNo+1;
            if (numNoiseHarmonicsCurrentFrame>maxNumHarmonics)
                maxNumHarmonics = numNoiseHarmonicsCurrentFrame;
        }

        if (maxNumHarmonics>0)
        {
            aksis = new double[maxNumHarmonics];
            Arrays.fill(aksis, 0.0);
            aksiPlusOnes = new double[maxNumHarmonics];
            Arrays.fill(aksis, 0.0);
            phasekis = new float[maxNumHarmonics];
            for (i=0; i<maxNumHarmonics; i++)
                phasekis[i] = (float)(MathUtils.TWOPI*Math.random()-0.5*MathUtils.TWOPI); 
        }

        int lastPeriodInSamples = 0;
        double ht;
        float phasekt = 0.0f;

        float phasekiPlusOneEstimate = 0.0f;
        int Mk;
        boolean isNoised, isNextNoised, isPrevNoised;

        for (i=0; i<hnmSignal.frames.length; i++)
        {
            if (hnmSignal.frames[i].maximumFrequencyOfVoicingInHz>0.0f && hnmSignal.frames[i].maximumFrequencyOfVoicingInHz<0.5*hnmSignal.samplingRateInHz)
                startPseudoHarmonicNo = (int)(Math.floor((hnmSignal.frames[i].maximumFrequencyOfVoicingInHz+0.5*HnmAnalyzer.NOISE_F0_IN_HZ)/HnmAnalyzer.NOISE_F0_IN_HZ))-1;
            else
                startPseudoHarmonicNo = 0;

            if (i==0)
                numNoiseHarmonicsCurrentFrame = endPseudoHarmonicNo-startPseudoHarmonicNo+1;
            else
                numNoiseHarmonicsCurrentFrame = numNoiseHarmonicsNextFrame;

            if (i<hnmSignal.frames.length-1)
            {  
                if (hnmSignal.frames[i+1].maximumFrequencyOfVoicingInHz>0.0f && hnmSignal.frames[i+1].maximumFrequencyOfVoicingInHz<0.5*hnmSignal.samplingRateInHz)
                    startPseudoHarmonicNoNext = (int)(Math.floor((hnmSignal.frames[i+1].maximumFrequencyOfVoicingInHz+0.5*HnmAnalyzer.NOISE_F0_IN_HZ)/HnmAnalyzer.NOISE_F0_IN_HZ))-1;
                else
                    startPseudoHarmonicNoNext = 0;
                
                numNoiseHarmonicsNextFrame = endPseudoHarmonicNo-startPseudoHarmonicNoNext+1;
            }

            isNoised = ((hnmSignal.frames[i].maximumFrequencyOfVoicingInHz<0.5f*hnmSignal.samplingRateInHz) ? true : false);
            if (i<hnmSignal.frames.length-1)
                isNextNoised = ((hnmSignal.frames[i+1].maximumFrequencyOfVoicingInHz<0.5f*hnmSignal.samplingRateInHz) ? true : false);
            else
                isNextNoised = true;

            if (i>0)
                isPrevNoised = ((hnmSignal.frames[i-1].maximumFrequencyOfVoicingInHz<0.5f*hnmSignal.samplingRateInHz) ? true : false);
            else
                isPrevNoised = true;

            if (i==0)
                tsi = 0.0f;
            else
                tsi = hnmSignal.frames[i].tAnalysisInSeconds;
            startIndex = SignalProcUtils.time2sample(tsi, hnmSignal.samplingRateInHz);
            if (isNextNoised)
            {
                if (i==hnmSignal.frames.length-1)
                    tsiPlusOne = hnmSignal.originalDurationInSeconds;
                else
                    tsiPlusOne = hnmSignal.frames[i+1].tAnalysisInSeconds;
                endIndex = SignalProcUtils.time2sample(tsiPlusOne, hnmSignal.samplingRateInHz);
            }
            else
            {
                if (i==hnmSignal.frames.length-1)
                {
                    tsiPlusOne = hnmSignal.originalDurationInSeconds;
                    endIndex = SignalProcUtils.time2sample(tsiPlusOne, hnmSignal.samplingRateInHz);
                }
                else
                {
                    endIndex = startIndex + lastPeriodInSamples;
                    tsiPlusOne = SignalProcUtils.sample2time(endIndex, hnmSignal.samplingRateInHz);
                }
            }

            for (n=startIndex; n<endIndex; n++)
            {
                t = SignalProcUtils.sample2time(n, hnmSignal.samplingRateInHz);

                noisePart[n] = 0.0;
                for (k=startPseudoHarmonicNo; k<=endPseudoHarmonicNo; k++)
                {
                    //Estimate amplitude
                    if (i==0)
                    {
                        aksis[k-startPseudoHarmonicNo] = RegularizedCepstralEnvelopeEstimator.cepstrum2linearSpectrumValue(((FrameNoisePartPseudoHarmonic)hnmSignal.frames[i].n).ceps, (k+1)*HnmAnalyzer.NOISE_F0_IN_HZ, hnmSignal.samplingRateInHz);
                        //What if analysis sends noise amplitudes directly?
                        //aksis[k-startPseudoHarmonicNo] = ((FrameNoisePartPseudoHarmonic)hnmSignal.frames[i].n).ceps[k-startPseudoHarmonicNo];
                    }
                    else
                    {
                        if (isPrevNoised)
                            aksis[k-startPseudoHarmonicNo] = aksiPlusOnes[k-startPseudoHarmonicNo]; //from previous
                        else
                            aksis[k-startPseudoHarmonicNo] = 0.0;
                    }

                    if (isNextNoised && i<hnmSignal.frames.length-1)
                    {
                        aksiPlusOnes[k-startPseudoHarmonicNo] = RegularizedCepstralEnvelopeEstimator.cepstrum2linearSpectrumValue(((FrameNoisePartPseudoHarmonic)hnmSignal.frames[i+1].n).ceps , (k+1)*HnmAnalyzer.NOISE_F0_IN_HZ, hnmSignal.samplingRateInHz);
                        //What if analysis sends noise amplitudes directly?
                        //aksiPlusOnes[k-startPseudoHarmonicNo] = ((FrameNoisePartPseudoHarmonic)hnmSignal.frames[i+1].n).ceps[k-startPseudoHarmonicNo];
                    }
                    else
                        aksiPlusOnes[k-startPseudoHarmonicNo] = 0.0;

                    akt = aksis[k-startPseudoHarmonicNo] + (aksiPlusOnes[k-startPseudoHarmonicNo]-aksis[k-startPseudoHarmonicNo])*(t-tsi)/(tsiPlusOne-tsi);
                    //

                    //Estimate phase   
                    phasekiPlusOneEstimate = (float)( phasekis[k-startPseudoHarmonicNo] + (k+1)*MathUtils.TWOPI*HnmAnalyzer.NOISE_F0_IN_HZ*(tsiPlusOne-tsi));
                    phasekt = (float)(phasekiPlusOneEstimate*(t-tsi)/(tsiPlusOne-tsi) );
                    //

                    noisePart[n] += akt*Math.cos(phasekt);

                    if (!isNextNoised) //Set to random if next frame is fully voiced (which should be an extremely rare case!)
                        phasekis[k-startPseudoHarmonicNo] = (float)(MathUtils.TWOPI*Math.random()-0.5*MathUtils.TWOPI);
                }
            }

            lastPeriodInSamples = SignalProcUtils.time2sample(tsiPlusOne - tsi, hnmSignal.samplingRateInHz);

            System.out.println("Pseudo-harmonic noise synthesis complete at " + String.valueOf(hnmSignal.frames[i].tAnalysisInSeconds) + "s. for frame " + String.valueOf(i+1) + " of " + String.valueOf(hnmSignal.frames.length)); 
        }

        //We need to get rid of this normalization and make sure the noise part gain level matches the original signal without modifications
        noisePart = MathUtils.divide(noisePart, 32768.0);
        //
        
        return noisePart;
    }

    public static void main(String[] args) throws UnsupportedAudioFileException, IOException
    {
        //File input
        String wavFile = args[0];
        AudioInputStream inputAudio = AudioSystem.getAudioInputStream(new File(wavFile));
        int samplingRate = (int)inputAudio.getFormat().getSampleRate();
        AudioDoubleDataSource signal = new AudioDoubleDataSource(inputAudio);
        double[] x = signal.getAllData();
        //

        //Analysis
        float windowSizeInSeconds = 0.040f;
        float skipSizeInSeconds = 0.010f;

        HnmAnalyzer ha = new HnmAnalyzer();
        int noisePartRepresentation = HnmAnalyzer.LPC;
        //int noisePartRepresentation = HnmAnalyzer.PSEUDO_HARMONIC;

        HnmSpeechSignal hnmSignal = ha.analyze(wavFile, windowSizeInSeconds, skipSizeInSeconds, noisePartRepresentation);
        //

        //Synthesis
        float[] tScales = {1.0f};
        float[] tScalesTimes = null;
        float[] pScales = {1.0f};
        float[] pScalesTimes = null;

        HnmSynthesizer hs = new HnmSynthesizer();
        HnmSynthesizedSignal xhat = hs.syntesize(hnmSignal, tScales, tScalesTimes, pScales, pScalesTimes);
        double[] y = SignalProcUtils.addSignals(xhat.harmonicPart, xhat.noisePart);
        //

        //A small test here: What if we substract y from x: In case no noise part is synthesized, this should be like a noise signal
        double[] nEstimate = null;
        nEstimate = SignalProcUtils.subtractSignals(x, y); //Not good, still amplitude difference and phase difference

        //File output
        DDSAudioInputStream outputAudio = null;
        String outFileName = null;

        outputAudio = new DDSAudioInputStream(new BufferedDoubleDataSource(y), inputAudio.getFormat());
        outFileName = args[0].substring(0, args[0].length()-4) + "_hnmResynth.wav";
        AudioSystem.write(outputAudio, AudioFileFormat.Type.WAVE, new File(outFileName));

        outputAudio = new DDSAudioInputStream(new BufferedDoubleDataSource(xhat.harmonicPart), inputAudio.getFormat());
        outFileName = args[0].substring(0, args[0].length()-4) + "_hnmHarmonic.wav";
        AudioSystem.write(outputAudio, AudioFileFormat.Type.WAVE, new File(outFileName));

        outputAudio = new DDSAudioInputStream(new BufferedDoubleDataSource(xhat.noisePart), inputAudio.getFormat());
        outFileName = args[0].substring(0, args[0].length()-4) + "_hnmNoise.wav";
        AudioSystem.write(outputAudio, AudioFileFormat.Type.WAVE, new File(outFileName));

        //MaryUtils.plot(xhat.harmonicPart);
        //MaryUtils.plot(xhat.noisePart);
        
        /*
        if (nEstimate!=null)
        {
            outputAudio = new DDSAudioInputStream(new BufferedDoubleDataSource(nEstimate), inputAudio.getFormat());
            outFileName = args[0].substring(0, args[0].length()-4) + "_hnmDiff.wav";
            AudioSystem.write(outputAudio, AudioFileFormat.Type.WAVE, new File(outFileName));
        }
         */
        //
    }
}
