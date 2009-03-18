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
package marytts.signalproc.sinusoidal.hntm;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import marytts.signalproc.analysis.F0ReaderWriter;
import marytts.signalproc.analysis.Labels;
import marytts.signalproc.analysis.PitchMarks;
import marytts.signalproc.analysis.RegularizedCepstralEnvelopeEstimator;
import marytts.signalproc.filter.HighPassFilter;
import marytts.signalproc.sinusoidal.BaseSinusoidalSpeechSignal;
import marytts.signalproc.sinusoidal.SinusoidalTrack;
import marytts.signalproc.sinusoidal.pitch.HnmPitchVoicingAnalyzer;
import marytts.signalproc.window.HammingWindow;
import marytts.signalproc.window.HanningWindow;
import marytts.signalproc.window.Window;
import marytts.util.MaryUtils;
import marytts.util.data.BufferedDoubleDataSource;
import marytts.util.data.audio.AudioDoubleDataSource;
import marytts.util.data.audio.DDSAudioInputStream;
import marytts.util.io.FileUtils;
import marytts.util.math.ArrayUtils;
import marytts.util.math.MathUtils;
import marytts.util.signal.SignalProcUtils;
import marytts.util.string.StringUtils;

/**
 * Synthesis using harmonics plus noise (and possibly plus transients) model
 * 
 * @author Oytun T&uumlrk
 *
 */
public class HntmSynthesizer {

    public static final double ENERGY_TRIANGLE_LOWER_VALUE = 1.0;
    public static final double ENERGY_TRIANGLE_UPPER_VALUE = 0.5;
    public static final double NUM_PERIODS_NOISE = 2.0;
    public static final float NOISE_SYNTHESIS_WINDOW_DURATION_IN_SECONDS = 0.050f;
    public static final float NOISE_SYNTHESIS_TRANSITION_OVERLAP_IN_SECONDS = 0.010f;
    public static final float HARMONIC_SYNTHESIS_TRANSITION_OVERLAP_IN_SECONDS = 0.002f;
    public static final float UNVOICED_VOICED_TRACK_TRANSITION_IN_SECONDS = 0.005f;

    public HntmSynthesizer()
    {

    }

    public HntmSynthesizedSignal synthesize(HntmSpeechSignal hntmSignal, 
            float[] tScales,
            float[] tScalesTimes,
            float[] pScales,
            float[] pScalesTimes)
    {
        //Handle time scaling by adjusting synthesis times
        HntmSpeechSignal hntmSignalMod = HntmDurationModifier.modify(hntmSignal, tScales, tScalesTimes, pScales, pScalesTimes);
        //
        
        HntmSynthesizedSignal s = new HntmSynthesizedSignal();
        s.harmonicPart = synthesizeHarmonicPartLinearPhaseInterpolation(hntmSignalMod, pScales, pScalesTimes);
        
        float[] times = hntmSignalMod.getAnalysisTimes();
        //

        //Synthesize noise part
        if (hntmSignalMod.frames[0].n instanceof FrameNoisePartLpc)
            s.noisePart = synthesizeNoisePartLpc(hntmSignalMod);
        else if (hntmSignalMod.frames[0].n instanceof FrameNoisePartPseudoHarmonic)
            s.noisePart = synthesizeNoisePartPseudoHarmonic(hntmSignalMod, pScales, pScalesTimes);
        //
        
        //Synthesize transients
        if (hntmSignalMod instanceof HntmPlusTransientsSpeechSignal && ((HntmPlusTransientsSpeechSignal)hntmSignalMod).transients!=null)
            s.transientPart = synthesizeTransientPart((HntmPlusTransientsSpeechSignal)hntmSignalMod, tScales, tScalesTimes, pScales, pScalesTimes);
        //
        
        float[] averageSampleEnergyContourHarmonic = null;
        float[] averageSampleEnergyContourNoise = null;
        float[] averageSampleEnergyContourTransient = null;
        
        if (s.harmonicPart!=null)
           averageSampleEnergyContourHarmonic = SignalProcUtils.getAverageSampleEnergyContour(s.harmonicPart, times, hntmSignalMod.samplingRateInHz, NOISE_SYNTHESIS_WINDOW_DURATION_IN_SECONDS);
        if (s.noisePart!=null)
            averageSampleEnergyContourNoise = SignalProcUtils.getAverageSampleEnergyContour(s.noisePart, times, hntmSignalMod.samplingRateInHz, NOISE_SYNTHESIS_WINDOW_DURATION_IN_SECONDS);
        if (s.transientPart!=null)
            averageSampleEnergyContourTransient = SignalProcUtils.getAverageSampleEnergyContour(s.transientPart, times, hntmSignalMod.samplingRateInHz, NOISE_SYNTHESIS_WINDOW_DURATION_IN_SECONDS);

        return s;
    }

    public double[] synthesizeHarmonicPartLinearPhaseInterpolation(HntmSpeechSignal hnmSignal, float[] pScales, float[] pScalesTimes)
    {
        double[] harmonicPart = null;
        int trackNoToExamine = 1;

        int i, k, n;
        double t; //Time in seconds
        
        double tsik = 0.0; //Synthesis time in seconds
        double tsikPlusOne = 0.0; //Synthesis time in seconds
        
        double trackStartInSeconds, trackEndInSeconds;
        //double lastPeriodInSeconds = 0.0;
        int trackStartIndex, trackEndIndex;
        double akt;
        int numHarmonicsCurrentFrame;
        int maxNumHarmonics = 0;
        for (i=0; i<hnmSignal.frames.length; i++)
        {
            if (hnmSignal.frames[i].maximumFrequencyOfVoicingInHz>0.0f && hnmSignal.frames[i].h!=null && hnmSignal.frames[i].h.phases!=null)
            {
                numHarmonicsCurrentFrame = hnmSignal.frames[i].h.phases.length;
                if (numHarmonicsCurrentFrame>maxNumHarmonics)
                    maxNumHarmonics = numHarmonicsCurrentFrame;
            }  
        }

        double aksi;
        double aksiPlusOne;
        
        float phaseki;
        float phasekiPlusOne;

        float f0InHzPrev, f0InHz, f0InHzNext;
        float f0av, f0avNext;
        f0InHzPrev = 0.0f;
        double ht;
        float phasekt = 0.0f;

        float phasekiEstimate = 0.0f;
        float phasekiPlusOneEstimate = 0.0f;
        int Mk;
        boolean isPrevVoiced, isVoiced, isNextVoiced;
        boolean isTrackVoiced, isNextTrackVoiced, isPrevTrackVoiced;
        int outputLen = SignalProcUtils.time2sample(hnmSignal.originalDurationInSeconds, hnmSignal.samplingRateInHz);
        
        harmonicPart = new double[outputLen]; //In fact, this should be prosody scaled length when you implement prosody modifications
        Arrays.fill(harmonicPart, 0.0);
        
        //Write separate tracks to output
        double[][] harmonicTracks = null;

        if (maxNumHarmonics>0)
        {
            harmonicTracks = new double[maxNumHarmonics][];
            for (k=0; k<maxNumHarmonics; k++)
            {
                harmonicTracks[k] = new double[outputLen];
                Arrays.fill(harmonicTracks[k], 0.0);
            }
        }
        //
        
        int transitionLen = SignalProcUtils.time2sample(UNVOICED_VOICED_TRACK_TRANSITION_IN_SECONDS, hnmSignal.samplingRateInHz);
        Window transitionWin = Window.get(Window.HAMMING, transitionLen*2);
        transitionWin.normalizePeakValue(1.0f);
        double[] halfTransitionWinLeft = transitionWin.getCoeffsLeftHalf();

        float[] targetContour = new float[hnmSignal.frames.length];
        Arrays.fill(targetContour, 0.0f);
        float[] times = new float[hnmSignal.frames.length];
        Arrays.fill(times, -1.0f);
        
        for (i=0; i<hnmSignal.frames.length; i++)
        {
            isPrevVoiced = false;
            isVoiced = false;
            isNextVoiced = false;
            
            if (i>0 && hnmSignal.frames[i-1].h!=null && hnmSignal.frames[i-1].h.phases!=null && hnmSignal.frames[i-1].h.phases.length>0)
                isPrevVoiced =  true;
            
            if (hnmSignal.frames[i].h!=null && hnmSignal.frames[i].h.phases!=null && hnmSignal.frames[i].h.phases.length>0)
                isVoiced = true;

            if (i<hnmSignal.frames.length-1 && hnmSignal.frames[i+1].h!=null && hnmSignal.frames[i+1].h.phases!=null && hnmSignal.frames[i+1].h.phases.length>0)
                isNextVoiced = true;
            
            if (isVoiced)
                numHarmonicsCurrentFrame = hnmSignal.frames[i].h.phases.length;
            else if (!isVoiced && isNextVoiced)
                numHarmonicsCurrentFrame = hnmSignal.frames[i+1].h.phases.length;
            else
                numHarmonicsCurrentFrame = 0;
            
            f0InHz = hnmSignal.frames[i].f0InHz;
            
            if (i>0)
                f0InHzPrev = hnmSignal.frames[i-1].f0InHz;
            else
                f0InHzPrev = f0InHz;
            
            if (isNextVoiced)
                f0InHzNext = hnmSignal.frames[i+1].f0InHz;
            else
                f0InHzNext = f0InHz;

            f0av = 0.5f*(f0InHz+f0InHzNext);
            
            times[i] = hnmSignal.frames[i].tAnalysisInSeconds;
            targetContour[i] = (1.0f-hnmSignal.frames[i].noiseTotalEnergyRatio)*hnmSignal.frames[i].totalSampleEnergy;

            for (k=0; k<numHarmonicsCurrentFrame; k++)
            {
                aksi = 0.0;
                aksiPlusOne = 0.0;
                
                phaseki = 0.0f;
                phasekiPlusOne = 0.0f;
                
                isPrevTrackVoiced = false;
                isTrackVoiced = false;
                isNextTrackVoiced = false;
                
                if (i>0 && hnmSignal.frames[i-1].h!=null && hnmSignal.frames[i-1].h.phases!=null && hnmSignal.frames[i-1].h.phases.length>k)
                    isPrevTrackVoiced = true;
                
                if (hnmSignal.frames[i].h!=null && hnmSignal.frames[i].h.phases!=null && hnmSignal.frames[i].h.phases.length>k)
                    isTrackVoiced = true;

                if (i<hnmSignal.frames.length-1 && hnmSignal.frames[i+1].h!=null && hnmSignal.frames[i+1].h.phases!=null && hnmSignal.frames[i+1].h.phases.length>k)
                    isNextTrackVoiced = true;

                tsik = hnmSignal.frames[i].tAnalysisInSeconds;

                if (i==0)
                    trackStartInSeconds = 0.0;
                else
                    trackStartInSeconds = tsik;
                
                if (i==hnmSignal.frames.length-1)
                    tsikPlusOne = hnmSignal.originalDurationInSeconds;
                else
                    tsikPlusOne = hnmSignal.frames[i+1].tAnalysisInSeconds;

                trackEndInSeconds = tsikPlusOne;

                trackStartIndex = SignalProcUtils.time2sample(trackStartInSeconds, hnmSignal.samplingRateInHz);
                trackEndIndex = SignalProcUtils.time2sample(trackEndInSeconds, hnmSignal.samplingRateInHz);

                if (isTrackVoiced && trackEndIndex-trackStartIndex+1>0)
                {
                    //Amplitudes     
                    if (isTrackVoiced)
                    {
                        aksi = RegularizedCepstralEnvelopeEstimator.cepstrum2linearSpectrumValue(hnmSignal.frames[i].h.ceps, (k+1)*f0InHz, hnmSignal.samplingRateInHz);
                        //aksi = hnmSignal.frames[i].h.ceps[k]; //Use amplitudes directly without cepstrum method
                    }
                    
                    if (isNextTrackVoiced)
                    {
                        aksiPlusOne = RegularizedCepstralEnvelopeEstimator.cepstrum2linearSpectrumValue(hnmSignal.frames[i+1].h.ceps , (k+1)*f0InHzNext, hnmSignal.samplingRateInHz);
                        //aksiPlusOne = hnmSignal.frames[i+1].h.ceps[k]; //Use amplitudes directly without cepstrum method
                    }
                    //

                    //Phases
                    if (isTrackVoiced)
                        phaseki = hnmSignal.frames[i].h.phases[k];
                    if (isNextTrackVoiced)
                        phasekiPlusOne = hnmSignal.frames[i+1].h.phases[k];
                    
                    if (!isTrackVoiced && isNextTrackVoiced)   
                        phaseki = (float)( phasekiPlusOne - (k+1)*MathUtils.TWOPI*f0InHzNext*(tsikPlusOne-tsik)); //Equation (3.54)
                    else if (isTrackVoiced && !isNextTrackVoiced)
                        phasekiPlusOne = (float)( phaseki + (k+1)*MathUtils.TWOPI*f0InHz*(tsikPlusOne-tsik)); //Equation (3.55)
                    
                    phasekiPlusOneEstimate = (float)( phaseki + (k+1)*MathUtils.TWOPI*f0av*(tsikPlusOne-tsik));
                    //phasekiPlusOneEstimate = (float) (MathUtils.TWOPI*(Math.random()-0.5)); //Random phase
                    
                    Mk = (int)Math.floor((phasekiPlusOneEstimate-phasekiPlusOne)/MathUtils.TWOPI + 0.5);
                    //
                    
                    if (!isPrevTrackVoiced)
                        trackStartIndex  = Math.max(0, trackStartIndex-transitionLen);

                    for (n=trackStartIndex; n<=Math.min(trackEndIndex, outputLen-1); n++)
                    {
                        t = SignalProcUtils.sample2time(n, hnmSignal.samplingRateInHz);

                        //if (t>=tsik && t<tsikPlusOne)
                        {
                            //Amplitude estimate
                            akt = MathUtils.interpolatedSample(tsik, t, tsikPlusOne, aksi, aksiPlusOne);
                            //

                            //Phase estimate
                            phasekt = (float)( phaseki + (phasekiPlusOne+MathUtils.TWOPI*Mk-phaseki)*(t-tsik)/(tsikPlusOne-tsik) );
                            //

                            if (!isPrevTrackVoiced && n-trackStartIndex<transitionLen)
                                harmonicTracks[k][n] = halfTransitionWinLeft[n-trackStartIndex]*akt*Math.cos(phasekt);
                            else
                                harmonicTracks[k][n] = akt*Math.cos(phasekt);
                        }
                    } 
                }
            }
        }
        
        if (harmonicTracks!=null)
        {
            for (k=0; k<harmonicTracks.length; k++)
            {
                for (n=0; n<harmonicPart.length; n++)
                    harmonicPart[n] += harmonicTracks[k][n];
            }

            //Write separate tracks to output
            AudioInputStream inputAudio = null;
            try {
                inputAudio = AudioSystem.getAudioInputStream(new File("d:\\i.wav"));
            } catch (UnsupportedAudioFileException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            if (inputAudio!=null)
            {
                //k=1;
                for (k=0; k<harmonicTracks.length; k++)
                {
                    harmonicTracks[k] = MathUtils.divide(harmonicTracks[k], 32768.0);

                    DDSAudioInputStream outputAudio = new DDSAudioInputStream(new BufferedDoubleDataSource(harmonicTracks[k]), inputAudio.getFormat());
                    String outFileName = "d:\\harmonicTrack" + String.valueOf(k+1) + ".wav";
                    try {
                        AudioSystem.write(outputAudio, AudioFileFormat.Type.WAVE, new File(outFileName));
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }
            //
            
            //Energy contour normalization
            float[] currentContour = SignalProcUtils.getAverageSampleEnergyContour(harmonicPart, times, hnmSignal.samplingRateInHz, NOISE_SYNTHESIS_WINDOW_DURATION_IN_SECONDS);
            MaryUtils.plot(currentContour);
            MaryUtils.plot(harmonicPart);
            
            MaryUtils.plot(targetContour);
            
            harmonicPart = SignalProcUtils.normalizeAverageSampleEnergyContour(harmonicPart, times, currentContour, targetContour, hnmSignal.samplingRateInHz, NOISE_SYNTHESIS_WINDOW_DURATION_IN_SECONDS);
            float[] currentContour2 = SignalProcUtils.getAverageSampleEnergyContour(harmonicPart, times, hnmSignal.samplingRateInHz, NOISE_SYNTHESIS_WINDOW_DURATION_IN_SECONDS);
            MaryUtils.plot(currentContour2);
            //
        }
        
        return harmonicPart;
    }

    //LPC based noise model + OLA approach + Gain normalization according to generated harmonic part gain
    public double[] synthesizeNoisePartLpc(HntmSpeechSignal hnmSignal)
    { 
        double[] noisePart = null;
        int i;
        boolean isNoised, isPrevNoised, isNextNoised;
        boolean isVoiced;
        int lpOrder = 0;
        float t;
        float tsi = 0.0f;
        float tsiNext; //Time in seconds
        int startIndex = 0;
        int startIndexNext;
        int outputLen = SignalProcUtils.time2sample(hnmSignal.originalDurationInSeconds, hnmSignal.samplingRateInHz);
        
        for (i=0; i<hnmSignal.frames.length; i++)
        {
            isNoised = ((hnmSignal.frames[i].maximumFrequencyOfVoicingInHz<0.5f*hnmSignal.samplingRateInHz) ? true : false);
            if (isNoised)
            {
                lpOrder = ((FrameNoisePartLpc)hnmSignal.frames[i].n).lpCoeffs.length;
                break;
            }
        }

        float noiseWindowDurationInSeconds;
        float[] targetContour = new float[hnmSignal.frames.length];
        Arrays.fill(targetContour, 0.0f);
        float[] times = new float[hnmSignal.frames.length];
        Arrays.fill(times, -1.0f);
        
        if (lpOrder>0) //At least one noisy frame with LP coefficients exist
        {
            noisePart = new double[outputLen]; //In fact, this should be prosody scaled length when you implement prosody modifications
            Arrays.fill(noisePart, 0.0);
            double[] winWgtSum = new double[outputLen]; //In fact, this should be prosody scaled length when you implement prosody modifications
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

            boolean isDisplay = false;

            //Noise source of full length
            double[] noiseSourceHpf = null;
            //noiseSource = SignalProcUtils.getNoise(HnmAnalyzer.FIXED_MAX_FREQ_OF_VOICING_FOR_QUICK_TEST, 0.5f*hnmSignal.samplingRateInHz, HnmAnalyzer.HPF_TRANSITION_BANDWIDTH_IN_HZ,  hnmSignal.samplingRateInHz, (int)(1.1*outputLen)); //Pink noise full signal length, works OK
            /*
            if (HnmAnalyzer.FIXED_MAX_FREQ_OF_VOICING_FOR_QUICK_TEST<0.5*hnmSignal.samplingRateInHz)
                noiseSourceHpf = SignalProcUtils.getNoise(HnmAnalyzer.FIXED_MAX_FREQ_OF_VOICING_FOR_QUICK_TEST, HnmAnalyzer.FIXED_MAX_FREQ_OF_NOISE_FOR_QUICK_TEST, HnmAnalyzer.HPF_TRANSITION_BANDWIDTH_IN_HZ,  hnmSignal.samplingRateInHz, (int)(1.1*outputLen)); //Pink noise full signal length, works OK
            if (noiseSourceHpf!=null)
                MathUtils.adjustMeanVariance(noiseSourceHpf, 0.0, 1.0);
            double[] noiseSourceFull = SignalProcUtils.getWhiteNoise((int)(1.1*outputLen), 1.0); //White noise full signal length, works OK
            MathUtils.adjustMeanVariance(noiseSourceFull, 0.0, 1.0);
            */
            
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

            int transitionOverlapLen = SignalProcUtils.time2sample(NOISE_SYNTHESIS_TRANSITION_OVERLAP_IN_SECONDS, hnmSignal.samplingRateInHz);
            
            for (i=0; i<hnmSignal.frames.length; i++)
            {
                if (hnmSignal.frames[i].h!=null && hnmSignal.frames[i].maximumFrequencyOfVoicingInHz>0.0f)
                    isVoiced = true;
                else
                    isVoiced = false;
                if (hnmSignal.frames[i].n!=null && hnmSignal.frames[i].maximumFrequencyOfVoicingInHz<0.5f*hnmSignal.samplingRateInHz)
                    isNoised = true;
                else
                    isNoised = false;
                if (i<hnmSignal.frames.length-1 && hnmSignal.frames[i+1].maximumFrequencyOfVoicingInHz<0.5f*hnmSignal.samplingRateInHz && hnmSignal.frames[i+1].n!=null)
                    isNextNoised = true;
                else
                    isNextNoised = false;
                if (i>0 && hnmSignal.frames[i-1].maximumFrequencyOfVoicingInHz<0.5f*hnmSignal.samplingRateInHz && hnmSignal.frames[i-1].n!=null)
                    isPrevNoised = true;
                else
                    isPrevNoised = false;

                if (i<hnmSignal.frames.length-1 && isNextNoised)
                    noiseWindowDurationInSeconds = Math.max(NOISE_SYNTHESIS_WINDOW_DURATION_IN_SECONDS, 2*(hnmSignal.frames[i+1].tAnalysisInSeconds-hnmSignal.frames[i].tAnalysisInSeconds));
                else
                    noiseWindowDurationInSeconds = NOISE_SYNTHESIS_WINDOW_DURATION_IN_SECONDS;
                wsNoise = SignalProcUtils.time2sample(noiseWindowDurationInSeconds, hnmSignal.samplingRateInHz);
                if (!isNextNoised)
                    wsNoise += transitionOverlapLen;
                if (!isPrevNoised)
                    wsNoise += transitionOverlapLen;
                
                if (wsNoise%2==0) //Always use an odd window size to have a zero-phase analysis window
                    wsNoise++; 

                if (i==0)
                    tsi = 0.0f;
                else
                    tsi = Math.max(0.0f, hnmSignal.frames[i].tAnalysisInSeconds-0.5f*noiseWindowDurationInSeconds);

                //if (tsi>1.8 && tsi<1.82)
                //    System.out.println("Time=" + String.valueOf(tsi) + " " + (isPrevNoised?"+":"-") + (isNoised?"+":"-") + (isNextNoised?"+":"-"));

                startIndex = SignalProcUtils.time2sample(tsi, hnmSignal.samplingRateInHz);

                if (i<hnmSignal.frames.length-1)
                {
                    tsiNext = Math.max(0.0f, hnmSignal.frames[i].tAnalysisInSeconds+0.5f*noiseWindowDurationInSeconds);
                    startIndexNext = SignalProcUtils.time2sample(tsiNext, hnmSignal.samplingRateInHz);
                }
                else
                {
                    startIndexNext = outputLen-1;
                    tsiNext = SignalProcUtils.sample2time(startIndexNext, hnmSignal.samplingRateInHz); 
                }

                if (isNoised && hnmSignal.frames[i].n!=null)
                {       
                    //Compute window
                    winNoise = Window.get(windowType, wsNoise);
                    wgt = winNoise.getCoeffs();
                    //

                    //x = SignalProcUtils.getWhiteNoiseOfVariance(wsNoise, 1.0); //Variance specified white noise
                    //x = SignalProcUtils.getWhiteNoise(wsNoise, 0.5); //Absolute value limited white noise

                    //double[] tmpNoise = SignalProcUtils.getNoise(hnmSignal.frames[i].maximumFrequencyOfVoicingInHz, 0.5f*hnmSignal.samplingRateInHz, 50.0, hnmSignal.samplingRateInHz, 5*wsNoise); //Pink noise
                    //x = new double[wsNoise];
                    //System.arraycopy(tmpNoise, 2*wsNoise, x, 0, wsNoise); 

                    x = SignalProcUtils.getNoise(hnmSignal.frames[i].maximumFrequencyOfVoicingInHz, 0.5*hnmSignal.samplingRateInHz, 100.0, hnmSignal.samplingRateInHz, wsNoise); //Pink noise
                    
                    y = SignalProcUtils.arFilterFreqDomain(x, ((FrameNoisePartLpc)hnmSignal.frames[i].n).lpCoeffs, 1.0);
                    
                    y = SignalProcUtils.normalizeAverageSampleEnergy(y, hnmSignal.frames[i].noiseTotalEnergyRatio*hnmSignal.frames[i].totalSampleEnergy);

                    y = winNoise.apply(y, 0);
                    
                    times[i] = hnmSignal.frames[i].tAnalysisInSeconds;
                    targetContour[i] = hnmSignal.frames[i].noiseTotalEnergyRatio*hnmSignal.frames[i].totalSampleEnergy;

                    //Overlap-add
                    for (n=startIndex; n<Math.min(startIndex+wsNoise, noisePart.length); n++)
                    {
                        noisePart[n] += y[n-startIndex]*wgt[n-startIndex]; 
                        winWgtSum[n] += wgt[n-startIndex]*wgt[n-startIndex];
                    }
                    //
                }

                System.out.println("LPC noise synthesis complete at " + String.valueOf(hnmSignal.frames[i].tAnalysisInSeconds) + "s. for frame " + String.valueOf(i+1) + " of " + String.valueOf(hnmSignal.frames.length) + "..." + String.valueOf(startIndex) + "-" + String.valueOf(startIndex+wsNoise)); 
            }
            
            for (i=0; i<winWgtSum.length; i++)
            {
                if (winWgtSum[i]>0.0)
                    noisePart[i] /= winWgtSum[i];
            }
        }
        
        //Energy contour normalization
        float[] currentContour = SignalProcUtils.getAverageSampleEnergyContour(noisePart, times, hnmSignal.samplingRateInHz, NOISE_SYNTHESIS_WINDOW_DURATION_IN_SECONDS);
        MaryUtils.plot(currentContour);
        MaryUtils.plot(noisePart);
        
        MaryUtils.plot(targetContour);
        
        noisePart = SignalProcUtils.normalizeAverageSampleEnergyContour(noisePart, times, currentContour, targetContour, hnmSignal.samplingRateInHz, NOISE_SYNTHESIS_WINDOW_DURATION_IN_SECONDS);
        float[] currentContour2 = SignalProcUtils.getAverageSampleEnergyContour(noisePart, times, hnmSignal.samplingRateInHz, NOISE_SYNTHESIS_WINDOW_DURATION_IN_SECONDS);
        MaryUtils.plot(currentContour2);
        //

        /*
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
                    startIndexNext = outputLen-1;
                    tsiNext = SignalProcUtils.sample2time(startIndexNext, hnmSignal.samplingRateInHz);
                }
                
                enEnvLen = startIndexNext-startIndex+1;
                if (enEnvLen>0)
                {
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

                    for (n=startIndex; n<=Math.min(noisePart.length-1, startIndexNext); n++)
                        noisePart[n] *= enEnv[n-startIndex];
                }
            }
        }
        */

        return noisePart;
    }

    //Pseudo harmonics based noise generation for pseudo periods
    public static double[] synthesizeNoisePartPseudoHarmonic(HntmSpeechSignal hnmSignal, float[] pScales, float[] pScalesTimes)
    {
        double[] noisePart = null;
        int trackNoToExamine = 1;

        int i, k, n;
        double t; //Time in seconds
        
        double tsik = 0.0; //Synthesis time in seconds
        double tsikPlusOne = 0.0; //Synthesis time in seconds
        
        double trackStartInSeconds, trackEndInSeconds;
        //double lastPeriodInSeconds = 0.0;
        int trackStartIndex, trackEndIndex;
        double akt;
        int numHarmonicsCurrentFrame, numHarmonicsPrevFrame, numHarmonicsNextFrame;
        int harmonicIndexShiftPrev, harmonicIndexShiftCurrent, harmonicIndexShiftNext;
        int maxNumHarmonics = 0;
        for (i=0; i<hnmSignal.frames.length; i++)
        {
            if (hnmSignal.frames[i].maximumFrequencyOfVoicingInHz>0.0f && hnmSignal.frames[i].n!=null)
            {
                numHarmonicsCurrentFrame = (int)Math.floor((hnmSignal.samplingRateInHz-hnmSignal.frames[i].maximumFrequencyOfVoicingInHz)/HntmAnalyzer.NOISE_F0_IN_HZ+0.5);
                numHarmonicsCurrentFrame = Math.max(0, numHarmonicsCurrentFrame);
                if (numHarmonicsCurrentFrame>maxNumHarmonics)
                    maxNumHarmonics = numHarmonicsCurrentFrame;
            }  
        }

        double aksi;
        double aksiPlusOne;
        
        float[] phasekis = null;
        float phasekiPlusOne;

        double ht;
        float phasekt = 0.0f;

        float phasekiEstimate = 0.0f;
        float phasekiPlusOneEstimate = 0.0f;
        int Mk;
        boolean isPrevNoised, isNoised, isNextNoised;
        boolean isTrackNoised, isNextTrackNoised, isPrevTrackNoised;
        int outputLen = SignalProcUtils.time2sample(hnmSignal.originalDurationInSeconds, hnmSignal.samplingRateInHz);
        
        noisePart = new double[outputLen]; //In fact, this should be prosody scaled length when you implement prosody modifications
        Arrays.fill(noisePart, 0.0);
        
        //Write separate tracks to output
        double[][] noiseTracks = null;

        if (maxNumHarmonics>0)
        {
            noiseTracks = new double[maxNumHarmonics][];
            for (k=0; k<maxNumHarmonics; k++)
            {
                noiseTracks[k] = new double[outputLen];
                Arrays.fill(noiseTracks[k], 0.0);
            }
            
            phasekis = new float[maxNumHarmonics];
            for (k=0; k<maxNumHarmonics; k++)
                phasekis[k] = (float) (MathUtils.TWOPI*(Math.random()-0.5));
        }
        //
        
        int transitionLen = SignalProcUtils.time2sample(UNVOICED_VOICED_TRACK_TRANSITION_IN_SECONDS, hnmSignal.samplingRateInHz);
        Window transitionWin = Window.get(Window.HAMMING, transitionLen*2);
        transitionWin.normalizePeakValue(1.0f);
        double[] halfTransitionWinLeft = transitionWin.getCoeffsLeftHalf();
        float halfFs = hnmSignal.samplingRateInHz;
        
        for (i=0; i<hnmSignal.frames.length; i++)
        {
            isPrevNoised = false;
            isNoised = false;
            isNextNoised = false;
            
            if (i>0 && hnmSignal.frames[i-1].n!=null && hnmSignal.frames[i-1].maximumFrequencyOfVoicingInHz<halfFs && ((FrameNoisePartPseudoHarmonic)hnmSignal.frames[i-1].n).ceps!=null)
                isPrevNoised =  true;
            
            if (i>0 && hnmSignal.frames[i].n!=null && hnmSignal.frames[i].maximumFrequencyOfVoicingInHz<halfFs && ((FrameNoisePartPseudoHarmonic)hnmSignal.frames[i].n).ceps!=null)
                isNoised =  true;

            if (i<hnmSignal.frames.length-1 && hnmSignal.frames[i+1].maximumFrequencyOfVoicingInHz<halfFs && hnmSignal.frames[i+1].n!=null && ((FrameNoisePartPseudoHarmonic)hnmSignal.frames[i+1].n).ceps!=null)
                isNextNoised = true;
            
            numHarmonicsPrevFrame = 0;
            numHarmonicsCurrentFrame = 0;
            numHarmonicsNextFrame = 0;
            harmonicIndexShiftPrev = 0;
            harmonicIndexShiftCurrent = 0;
            harmonicIndexShiftNext = 0;
            
            if (isPrevNoised)
            {
                numHarmonicsPrevFrame = (int)Math.floor((hnmSignal.samplingRateInHz-hnmSignal.frames[i-1].maximumFrequencyOfVoicingInHz)/HntmAnalyzer.NOISE_F0_IN_HZ+0.5);
                numHarmonicsPrevFrame = Math.max(0, numHarmonicsPrevFrame);
                harmonicIndexShiftPrev = (int)Math.floor(hnmSignal.frames[i-1].maximumFrequencyOfVoicingInHz/HntmAnalyzer.NOISE_F0_IN_HZ+0.5);
                harmonicIndexShiftPrev = Math.max(1, harmonicIndexShiftPrev);
            }
            
            if (isNoised)
            {
                numHarmonicsCurrentFrame = (int)Math.floor((hnmSignal.samplingRateInHz-hnmSignal.frames[i].maximumFrequencyOfVoicingInHz)/HntmAnalyzer.NOISE_F0_IN_HZ+0.5);
                numHarmonicsCurrentFrame = Math.max(0, numHarmonicsCurrentFrame);
                harmonicIndexShiftCurrent = (int)Math.floor(hnmSignal.frames[i].maximumFrequencyOfVoicingInHz/HntmAnalyzer.NOISE_F0_IN_HZ+0.5);
                harmonicIndexShiftCurrent = Math.max(1, harmonicIndexShiftCurrent);
            }
            else if (!isNoised && isNextNoised)
            {
                numHarmonicsCurrentFrame = (int)Math.floor((hnmSignal.samplingRateInHz-hnmSignal.frames[i+1].maximumFrequencyOfVoicingInHz)/HntmAnalyzer.NOISE_F0_IN_HZ+0.5);
                numHarmonicsCurrentFrame = Math.max(0, numHarmonicsCurrentFrame);
                harmonicIndexShiftCurrent = (int)Math.floor(hnmSignal.frames[i+1].maximumFrequencyOfVoicingInHz/HntmAnalyzer.NOISE_F0_IN_HZ+0.5);
                harmonicIndexShiftCurrent = Math.max(1, harmonicIndexShiftCurrent);
            }
              
            if (isNextNoised)
            {
                numHarmonicsNextFrame = (int)Math.floor((hnmSignal.samplingRateInHz-hnmSignal.frames[i+1].maximumFrequencyOfVoicingInHz)/HntmAnalyzer.NOISE_F0_IN_HZ+0.5);
                numHarmonicsNextFrame = Math.max(0, numHarmonicsNextFrame);
                harmonicIndexShiftNext = (int)Math.floor(hnmSignal.frames[i+1].maximumFrequencyOfVoicingInHz/HntmAnalyzer.NOISE_F0_IN_HZ+0.5);
                harmonicIndexShiftNext = Math.max(1, harmonicIndexShiftNext);
            }

            for (k=0; k<numHarmonicsCurrentFrame; k++)
            {
                aksi = 0.0;
                aksiPlusOne = 0.0;
                
                phasekiPlusOne = 0.0f;
                
                isPrevTrackNoised = false;
                isTrackNoised = false;
                isNextTrackNoised = false;
                
                if (i>0 && hnmSignal.frames[i-1].n!=null && numHarmonicsPrevFrame>k)
                    isPrevTrackNoised = true;
                
                if (hnmSignal.frames[i].n!=null && numHarmonicsCurrentFrame>k)
                    isTrackNoised = true;

                if (i<hnmSignal.frames.length-1 && hnmSignal.frames[i+1].n!=null && numHarmonicsNextFrame>k)
                    isNextTrackNoised = true;

                tsik = hnmSignal.frames[i].tAnalysisInSeconds;

                if (i==0)
                    trackStartInSeconds = 0.0;
                else
                    trackStartInSeconds = tsik;
                
                if (i==hnmSignal.frames.length-1)
                    tsikPlusOne = hnmSignal.originalDurationInSeconds;
                else
                    tsikPlusOne = hnmSignal.frames[i+1].tAnalysisInSeconds;

                trackEndInSeconds = tsikPlusOne;

                trackStartIndex = SignalProcUtils.time2sample(trackStartInSeconds, hnmSignal.samplingRateInHz);
                trackEndIndex = SignalProcUtils.time2sample(trackEndInSeconds, hnmSignal.samplingRateInHz);

                if (isTrackNoised && trackEndIndex-trackStartIndex+1>0)
                {
                    //Amplitudes     
                    if (isTrackNoised)
                    {
                        aksi = RegularizedCepstralEnvelopeEstimator.cepstrum2linearSpectrumValue(((FrameNoisePartPseudoHarmonic)hnmSignal.frames[i].n).ceps, (k+harmonicIndexShiftCurrent)*HntmAnalyzer.NOISE_F0_IN_HZ, hnmSignal.samplingRateInHz);
                        //aksi = ((FrameNoisePartPseudoHarmonic)hnmSignal.frames[i].n).ceps[k]; //Use amplitudes directly without cepstrum method
                    }
                    
                    if (isNextTrackNoised)
                    {
                        aksiPlusOne = RegularizedCepstralEnvelopeEstimator.cepstrum2linearSpectrumValue(((FrameNoisePartPseudoHarmonic)hnmSignal.frames[i+1].n).ceps , (k+harmonicIndexShiftNext)*HntmAnalyzer.NOISE_F0_IN_HZ, hnmSignal.samplingRateInHz);
                        //aksiPlusOne = ((FrameNoisePartPseudoHarmonic)hnmSignal.frames[i+1].n).ceps[k]; //Use amplitudes directly without cepstrum method
                    }
                    //

                    //Phases
                    if (!isPrevTrackNoised && isTrackNoised)
                        phasekis[k] = (float) (MathUtils.TWOPI*(Math.random()-0.5));
                    
                    phasekiPlusOne = (float)( phasekis[k] + (k+harmonicIndexShiftCurrent)*MathUtils.TWOPI*HntmAnalyzer.NOISE_F0_IN_HZ*(tsikPlusOne-tsik)); //Equation (3.55)
                    //
                    
                    if (!isPrevTrackNoised)
                        trackStartIndex  = Math.max(0, trackStartIndex-transitionLen);

                    for (n=trackStartIndex; n<=Math.min(trackEndIndex, outputLen-1); n++)
                    {
                        t = SignalProcUtils.sample2time(n, hnmSignal.samplingRateInHz);

                        //if (t>=tsik && t<tsikPlusOne)
                        {
                            //Amplitude estimate
                            akt = MathUtils.interpolatedSample(tsik, t, tsikPlusOne, aksi, aksiPlusOne);
                            //

                            //Phase estimate
                            phasekt = (float)(phasekiPlusOne*(t-tsik)/(tsikPlusOne-tsik) );
                            //

                            if (!isPrevTrackNoised && n-trackStartIndex<transitionLen)
                                noiseTracks[k][n] = halfTransitionWinLeft[n-trackStartIndex]*akt*Math.cos(phasekt);
                            else
                                noiseTracks[k][n] = akt*Math.cos(phasekt);
                        }
                    } 
                    
                    phasekis[k] = phasekiPlusOne;
                }
            }
        }

        for (k=0; k<noiseTracks.length; k++)
        {
            for (n=0; n<noisePart.length; n++)
               noisePart[n] += noiseTracks[k][n];
        }
        
        //Write separate tracks to output
        AudioInputStream inputAudio = null;
        try {
            inputAudio = AudioSystem.getAudioInputStream(new File("d:\\i.wav"));
        } catch (UnsupportedAudioFileException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        if (inputAudio!=null)
        {
            //k=1;
            for (k=0; k<noiseTracks.length; k++)
            {
                noiseTracks[k] = MathUtils.divide(noiseTracks[k], 32768.0);

                DDSAudioInputStream outputAudio = new DDSAudioInputStream(new BufferedDoubleDataSource(noiseTracks[k]), inputAudio.getFormat());
                String outFileName = "d:\\harmonicTrack" + String.valueOf(k+1) + ".wav";
                try {
                    AudioSystem.write(outputAudio, AudioFileFormat.Type.WAVE, new File(outFileName));
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
        //

        return noisePart;
    }
    
    public double[] synthesizeTransientPart(HntmPlusTransientsSpeechSignal hnmSignal, 
            float[] tScales,
            float[] tScalesTimes,
            float[] pScales,
            float[] pScalesTimes)
    {
        int outputLen = SignalProcUtils.time2sample(hnmSignal.originalDurationInSeconds, hnmSignal.samplingRateInHz);
        
        double[] transientPart = new double[outputLen];
        Arrays.fill(transientPart, 0.0);
        
        if (hnmSignal.transients!=null)
        {
            int i, j;
            int startInd, endInd;
            int windowLeftEndInd, windowRightStartInd;
            int ws = SignalProcUtils.time2sample(2*HntmAnalyzer.OVERLAP_BETWEEN_TRANSIENT_AND_NONTRANSIENT_REGIONS_IN_SECONDS, hnmSignal.samplingRateInHz);
            if (ws%2==0)
                ws++;
            Window win = new HammingWindow(ws);
            win.normalizePeakValue(1.0f);
            int winMidInd = (ws-1)/2;
            for (i=0; i<hnmSignal.transients.segments.length; i++)
            {
                if (hnmSignal.transients.segments[i]!=null && hnmSignal.transients.segments[i].waveform!=null && hnmSignal.transients.segments[i].waveform.length>0 && hnmSignal.transients.segments[i].startTime>=0.0f)
                {
                    startInd = Math.min(SignalProcUtils.time2sample(hnmSignal.transients.segments[i].startTime, hnmSignal.samplingRateInHz), outputLen-1);
                    windowLeftEndInd = Math.min(startInd+winMidInd, outputLen-1);
                    endInd = Math.min(SignalProcUtils.time2sample(hnmSignal.transients.segments[i].startTime,hnmSignal.samplingRateInHz)+hnmSignal.transients.segments[i].waveform.length-1, outputLen-1);
                    windowRightStartInd = endInd-winMidInd;
                    
                    for (j=startInd; j<=windowLeftEndInd; j++)
                        transientPart[j] = hnmSignal.transients.segments[i].waveform[j-startInd]*win.value(j-startInd);
                    for (j=windowLeftEndInd+1; j<windowRightStartInd; j++)
                        transientPart[j] = hnmSignal.transients.segments[i].waveform[j-startInd];
                    for (j=windowRightStartInd; j<=endInd; j++)
                        transientPart[j] = hnmSignal.transients.segments[i].waveform[j-startInd]*win.value((j-endInd)+ws-1);
                }
            }
        }
        
        return transientPart;
    }

    public static void mainSingleFile(String wavFile) throws UnsupportedAudioFileException, IOException
    {
        //File input
        AudioInputStream inputAudio = AudioSystem.getAudioInputStream(new File(wavFile));
        int samplingRate = (int)inputAudio.getFormat().getSampleRate();
        AudioDoubleDataSource signal = new AudioDoubleDataSource(inputAudio);
        double[] x = signal.getAllData();
        x = MathUtils.multiply(x, 32768.0);
        //

        //Analysis
        float windowSizeInSeconds = 0.035f;
        float skipSizeInSeconds = 0.010f;

        HntmAnalyzer ha = new HntmAnalyzer();
        
        int model = HntmAnalyzer.HARMONICS_PLUS_NOISE;
        //int model = HntmAnalyzer.HARMONICS_PLUS_TRANSIENTS_PLUS_NOISE;
        
        int noisePartRepresentation = HntmAnalyzer.LPC;
        //int noisePartRepresentation = HntmAnalyzer.PSEUDO_HARMONIC;

        F0ReaderWriter f0 = null;
        String strPitchFile = StringUtils.modifyExtension(wavFile, ".ptc");
        if (FileUtils.exists(strPitchFile))
        {
            f0 = new F0ReaderWriter(strPitchFile);
            //Arrays.fill(f0.contour, 100.0);
        }
        
        Labels labels = null;
        if (model == HntmAnalyzer.HARMONICS_PLUS_TRANSIENTS_PLUS_NOISE)
        {
            String strLabFile = StringUtils.modifyExtension(wavFile, ".lab"); 
            if (FileUtils.exists(strLabFile)!=true) //Labels required for transients analysis (unless we design an automatic algorithm)
            {
                System.out.println("Error! Labels required for transient analysis...");
                System.exit(1);
            }
            labels = new Labels(strLabFile);
        }
         
        int fftSize = 4096;
        int harmonicPartAnalysisMethod = HntmAnalyzer.TIME_DOMAIN_CORRELATION_HARMONICS_ANALYSIS;
        //int harmonicPartAnalysisMethod = HntmAnalyzer.FREQUENCY_DOMAIN_PEAK_PICKING_HARMONICS_ANALYSIS;
        
        HntmSpeechSignal hnmSignal = ha.analyze(x, samplingRate, f0, labels, windowSizeInSeconds, skipSizeInSeconds, fftSize, model, noisePartRepresentation, harmonicPartAnalysisMethod);
        //

        if (hnmSignal!=null)
        {
            //Synthesis
            //float[] tScales = {0.3f, 0.5f, 1.0f, 1.5f, 2.5f};
            float[] tScales = {1.0f};
            //float[] tScalesTimes = {0.5f, 1.0f, 1.5f, 2.0f, 2.5f};
            float[] tScalesTimes = null;
            
            float[] pScales = {1.0f};
            float[] pScalesTimes = null;

            HntmSynthesizer hs = new HntmSynthesizer();
            HntmSynthesizedSignal xhat = hs.synthesize(hnmSignal, tScales, tScalesTimes, pScales, pScalesTimes);

            double hGain = 1.0;
            double nGain = 1.0;
            double tGain = 1.0;
            if (xhat.harmonicPart!=null)
            {
                xhat.harmonicPart = MathUtils.multiply(xhat.harmonicPart, hGain);
                //xhat.harmonicPart = MathUtils.multiply(xhat.harmonicPart, MathUtils.absMax(x)/MathUtils.absMax(xhat.harmonicPart));
                //MaryUtils.plot(xhat.harmonicPart);
            }
            
            if (xhat.noisePart!=null)
            {
                xhat.noisePart = MathUtils.multiply(xhat.noisePart, nGain);
              //MaryUtils.plot(xhat.noisePart);
            }
            
            if (xhat.transientPart!=null)
            {
                xhat.transientPart = MathUtils.multiply(xhat.transientPart, tGain);
                //MaryUtils.plot(xhat.transientPart);
            }

            double[] y = SignalProcUtils.addSignals(xhat.harmonicPart, xhat.noisePart);
            y = SignalProcUtils.addSignals(y, xhat.transientPart);
            //y = MathUtils.multiply(y, MathUtils.absMax(x)/MathUtils.absMax(y));
            MaryUtils.plot(x);
            MaryUtils.plot(xhat.harmonicPart);
            MaryUtils.plot(xhat.noisePart);
            MaryUtils.plot(xhat.transientPart);
            MaryUtils.plot(y);
            
            //double[] d = SignalProcUtils.addSignals(x, 1.0f, xhat.harmonicPart, -1.0f);
            
            /*
            for (int i=0; i<300; i+=100)
            {
                int startIndex = i;
                int len = 100;
                double[] xPart = ArrayUtils.subarray(x, startIndex, len);
                double[] hPart = ArrayUtils.subarray(xhat.harmonicPart, startIndex, len);
                double[] dPart = SignalProcUtils.addSignals(xPart, 1.0f, hPart, -1.0f);
                MaryUtils.plot(xPart);
                MaryUtils.plot(hPart);
                MaryUtils.plot(dPart);
            }
            */

            //xhat.noisePart = ArrayUtils.subarray(hpf, 0, hpf.length);
            //

            //File output
            DDSAudioInputStream outputAudio = null;
            String outFileName = null;
            String strExt = "";
            String modelName = "";
            if (model==HntmAnalyzer.HARMONICS_PLUS_NOISE)
                modelName = "hnml";
            else if (model==HntmAnalyzer.HARMONICS_PLUS_TRANSIENTS_PLUS_NOISE)
                modelName = "hntml";

            //y = MathUtils.multiply(y, MathUtils.absMax(x)/MathUtils.absMax(y));
            outputAudio = new DDSAudioInputStream(new BufferedDoubleDataSource(MathUtils.divide(y,32768.0)), inputAudio.getFormat());
            outFileName = wavFile.substring(0, wavFile.length()-4) + "_" + modelName + "Resynth" + strExt + ".wav";
            AudioSystem.write(outputAudio, AudioFileFormat.Type.WAVE, new File(outFileName));

            if (xhat.harmonicPart!=null)
            {
                outputAudio = new DDSAudioInputStream(new BufferedDoubleDataSource(MathUtils.divide(xhat.harmonicPart, 32768.0)), inputAudio.getFormat());
                outFileName = wavFile.substring(0, wavFile.length()-4) + "_" + modelName + "Harmonic" + strExt + ".wav";
                AudioSystem.write(outputAudio, AudioFileFormat.Type.WAVE, new File(outFileName));
            }
            
            if (xhat.noisePart!=null)
            {
                outputAudio = new DDSAudioInputStream(new BufferedDoubleDataSource(MathUtils.divide(xhat.noisePart, 32768.0)), inputAudio.getFormat());
                outFileName = wavFile.substring(0, wavFile.length()-4) + "_" + modelName + "Noise" + strExt + ".wav";
                AudioSystem.write(outputAudio, AudioFileFormat.Type.WAVE, new File(outFileName));
            }

            if (xhat.transientPart!=null)
            {
                outputAudio = new DDSAudioInputStream(new BufferedDoubleDataSource(MathUtils.divide(xhat.transientPart, 32768.0)), inputAudio.getFormat());
                outFileName = wavFile.substring(0, wavFile.length()-4) + "_" + modelName + "Transient" + strExt + ".wav";
                AudioSystem.write(outputAudio, AudioFileFormat.Type.WAVE, new File(outFileName));
            }
            
            if (xhat.harmonicPart!=null)
            {
                outputAudio = new DDSAudioInputStream(new BufferedDoubleDataSource(MathUtils.divide(SignalProcUtils.addSignals(x, 1.0, xhat.harmonicPart, -1.0), 32768.0)), inputAudio.getFormat());
                outFileName = wavFile.substring(0, wavFile.length()-4) + "_" + modelName + "OrigMinusHarmonic" + strExt + ".wav";
                AudioSystem.write(outputAudio, AudioFileFormat.Type.WAVE, new File(outFileName));
            }

            //MaryUtils.plot(xhat.harmonicPart);
            //MaryUtils.plot(xhat.noisePart);
            //MaryUtils.plot(y);

            //if (nEstimate!=null)
            //{
            //    outputAudio = new DDSAudioInputStream(new BufferedDoubleDataSource(nEstimate), inputAudio.getFormat());
            //    outFileName = args[0].substring(0, args[0].length()-4) + "_" + modelName + "Diff.wav";
            //    AudioSystem.write(outputAudio, AudioFileFormat.Type.WAVE, new File(outFileName));
            //}
            //
        }
    }
    
    public static void main(String[] args) throws UnsupportedAudioFileException, IOException
    {
        if (FileUtils.isDirectory(args[0])) //Process folder
        {
            String[] fileList = FileUtils.getFileList(args[0], "wav");
            if (fileList!=null)
            {
                for (int i=0; i<fileList.length; i++)
                {
                    mainSingleFile(fileList[i]);
                    System.out.println("HNM processing completed for file " + String.valueOf(i+1) + " of " + String.valueOf(fileList.length));
                }
            }
            else
                System.out.println("No wav files found!");
        }
        else //Process file
            mainSingleFile(args[0]);
    }
}
