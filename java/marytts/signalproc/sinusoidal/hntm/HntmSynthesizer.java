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
 * Synthesis using harmonics plus noise model
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
    public static final float HARMONIC_NONHARMONIC_TRANSITION_OVERLAP_IN_SECONDS = 0.005f;

    public HntmSynthesizer()
    {

    }

    public HntmSynthesizedSignal synthesize(HntmSpeechSignal hntmSignal, 
            float[] tScales,
            float[] tScalesTimes,
            float[] pScales,
            float[] pScalesTimes)
    {
        
        HntmSpeechSignal hntmSignalMod = HntmDurationModifier.modify(hntmSignal, tScales, tScalesTimes);
        
        HntmSynthesizedSignal s = new HntmSynthesizedSignal();
        s.harmonicPart = synthesizeHarmonicPartLinearPhaseInterpolation(hntmSignalMod, tScales, tScalesTimes, pScales, pScalesTimes);
        //

        //Synthesize noise part
        if (hntmSignalMod.frames[0].n instanceof FrameNoisePartLpc)
            s.noisePart = synthesizeNoisePartLpc(hntmSignalMod, tScales, tScalesTimes, pScales, pScalesTimes);
        else if (hntmSignalMod.frames[0].n instanceof FrameNoisePartPseudoHarmonic)
            s.noisePart = synthesizeNoisePartPseudoHarmonic(hntmSignalMod, tScales, tScalesTimes, pScales, pScalesTimes);
        //
        
        //Synthesize transients
        if (hntmSignalMod instanceof HntmPlusTransientsSpeechSignal && ((HntmPlusTransientsSpeechSignal)hntmSignalMod).transients!=null)
            s.transientPart = synthesizeTransientPart((HntmPlusTransientsSpeechSignal)hntmSignalMod, tScales, tScalesTimes, pScales, pScalesTimes);
        //
        
        return s;
    }

    public double[] synthesizeHarmonicPartLinearPhaseInterpolation(HntmSpeechSignal hnmSignal, 
            float[] tScales,
            float[] tScalesTimes,
            float[] pScales,
            float[] pScalesTimes)
    {
        double[] harmonicPart = null;
        //double[][] harmonicTracks = null;
        //double[][] trackAmps = null;
        //double[][] trackPhases = null;
        //double[] currentFrameTrack = null;
        int trackNoToExamine = 1;

        int overlapSize = SignalProcUtils.time2sample(HARMONIC_NONHARMONIC_TRANSITION_OVERLAP_IN_SECONDS, hnmSignal.samplingRateInHz);
        Window w = new HanningWindow(2*overlapSize+1);
        w.normalizePeakValue(1.0f);
        double[] wgtLeft = w.getCoeffsLeftHalf();
        double[] wgtRight = w.getCoeffsRightHalf();

        int i, k, n;
        double t, tsi, tsiPlusOne; //Time in seconds
        double frameStartInSeconds, frameEndInSeconds;
        int frameStartIndex, frameEndIndex;
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

        double[] aksis = null;
        double[] aksiPlusOnes = null;
        float[] phasekis = null;
        float[] phasekiPlusOnes = null;
        if (maxNumHarmonics>0)
        {
            //harmonicTracks = new double[maxNumHarmonics][];
            //trackAmps = new double[maxNumHarmonics][];
            //trackPhases = new double[maxNumHarmonics][];

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
        double lastPeriodInSeconds = 0.0;
        double ht;
        float phasekt = 0.0f;

        float phasekiPlusOneEstimate = 0.0f;
        int Mk;
        boolean isVoiced, isNextVoiced, isPrevVoiced;
        int origLen = SignalProcUtils.time2sample(hnmSignal.originalDurationInSeconds, hnmSignal.samplingRateInHz);
        float tscale = tScales[0];
        int outputLen = (int)Math.floor(tscale*origLen+0.5);
        
        harmonicPart = new double[outputLen]; //In fact, this should be prosody scaled length when you implement prosody modifications
        Arrays.fill(harmonicPart, 0.0);
        double currentWgt;
        
        /*
        for (k=0; k<maxNumHarmonics; k++)
        {
            harmonicTracks[k] = new double[outputLen];
            Arrays.fill(harmonicTracks[k], 0.0);
            trackAmps[k] = new double[outputLen];
            Arrays.fill(trackAmps[k], 0.0);
            trackPhases[k] = new double[outputLen];
            Arrays.fill(trackPhases[k], 0.0);
        }
        */

        for (i=0; i<hnmSignal.frames.length; i++)
        {
            if (hnmSignal.frames[i].h!=null && hnmSignal.frames[i].maximumFrequencyOfVoicingInHz>0.0f)
                isVoiced = true;
            else
                isVoiced = false;
            
            if (i<hnmSignal.frames.length-1 && hnmSignal.frames[i+1].h!=null && hnmSignal.frames[i+1].maximumFrequencyOfVoicingInHz>0.0f)
                isNextVoiced = true;
            else
                isNextVoiced = false;

            if (i>0 && hnmSignal.frames[i-1].h!=null && hnmSignal.frames[i-1].maximumFrequencyOfVoicingInHz>0.0f)
                isPrevVoiced =  true;
            else
                isPrevVoiced = false;

            if (i==0)
                tsi = 0.0f;
            else
                tsi = tscale*hnmSignal.frames[i].tAnalysisInSeconds;

            //System.out.println("Time="+String.valueOf(tsi)+ " " + (isPrevVoiced?"+":"-") + (isVoiced?"+":"-") + (isNextVoiced?"+":"-"));
                
            frameStartInSeconds = tsi;
            if (isNextVoiced)
            {
                if (i==hnmSignal.frames.length-1)
                    tsiPlusOne = tscale*hnmSignal.originalDurationInSeconds;
                else
                    tsiPlusOne = tscale*hnmSignal.frames[i+1].tAnalysisInSeconds;
                frameEndInSeconds = tsiPlusOne;
            }
            else
            {
                if (i==hnmSignal.frames.length-1)
                {
                    tsiPlusOne = tscale*hnmSignal.originalDurationInSeconds;
                    frameEndInSeconds = tsiPlusOne;
                }
                else
                {
                    frameEndInSeconds = tscale*(frameStartInSeconds + lastPeriodInSeconds);
                    tsiPlusOne = frameEndInSeconds;
                }
            }

            if (hnmSignal.frames[i].h!=null && hnmSignal.frames[i].h.phases!=null)
                numHarmonicsCurrentFrame = hnmSignal.frames[i].h.phases.length;
            else
                numHarmonicsCurrentFrame = 0;

            frameStartIndex = SignalProcUtils.time2sample(frameStartInSeconds, hnmSignal.samplingRateInHz);
            frameEndIndex = SignalProcUtils.time2sample(frameEndInSeconds, hnmSignal.samplingRateInHz);

            int tempStartIndex;
            if (!isPrevVoiced && isVoiced)
                tempStartIndex = Math.max(0,frameStartIndex-overlapSize);
            else
                tempStartIndex = frameStartIndex;

            int tempEndIndex;
            if (isVoiced && !isNextVoiced)
                tempEndIndex = Math.min(frameEndIndex+overlapSize, outputLen-1);
            else
                tempEndIndex = frameEndIndex;

            if (hnmSignal.frames[i].h!=null && tempEndIndex-tempStartIndex+1>0)
            {
                //currentFrameTrack = new double[tempEndIndex-tempStartIndex+1];

                for (n=tempStartIndex; n<=Math.min(tempEndIndex, outputLen-1); n++)
                {
                    if (!isPrevVoiced && isVoiced && n<frameStartIndex)
                        currentWgt = wgtLeft[n-tempStartIndex];
                    else if (isVoiced && !isNextVoiced && n>frameEndIndex)
                        currentWgt = wgtRight[n-frameEndIndex-1];
                    else
                        currentWgt = 1.0;

                    t = SignalProcUtils.sample2time(n, hnmSignal.samplingRateInHz);

                    f0InHz = hnmSignal.frames[i].f0InHz;
                    if (isNextVoiced)
                        f0InHzNext = hnmSignal.frames[i+1].f0InHz;
                    else
                        f0InHzNext = f0InHz;

                    if (i>0)
                        f0InHzPrev = hnmSignal.frames[i-1].f0InHz;

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
                        aksis[k] = RegularizedCepstralEnvelopeEstimator.cepstrum2linearSpectrumValue(hnmSignal.frames[i].h.ceps, (k+1)*f0InHz, hnmSignal.samplingRateInHz);
                        //aksis[k] = hnmSignal.frames[i].h.ceps[k]; //Use amplitudes directly without cepstrum method

                        aksiPlusOnes[k] = aksis[k];
                        if (isNextVoiced && i<hnmSignal.frames.length-1 && hnmSignal.frames[i+1].h!=null && hnmSignal.frames[i+1].h.ceps!=null)
                        {
                            if ((k+1)*f0InHzNext<hnmSignal.frames[i+1].maximumFrequencyOfVoicingInHz)
                            {     
                                aksiPlusOnes[k] = RegularizedCepstralEnvelopeEstimator.cepstrum2linearSpectrumValue(hnmSignal.frames[i+1].h.ceps , (k+1)*f0InHzNext, hnmSignal.samplingRateInHz);
                                //aksiPlusOnes[k] = hnmSignal.frames[i+1].h.ceps[k]; //Use amplitudes directly without cepstrum method
                            }
                        }

                        akt = aksis[k] + (aksiPlusOnes[k]-aksis[k])*(t-tsi)/(tsiPlusOne-tsi);
                        //

                        //Estimate phase                     
                        if (isVoiced)
                            phasekis[k] = hnmSignal.frames[i].h.phases[k];

                        if (isNextVoiced && hnmSignal.frames[i+1].h!=null && hnmSignal.frames[i+1].h.phases!=null && k<hnmSignal.frames[i+1].h.phases.length)
                            phasekiPlusOnes[k] = hnmSignal.frames[i+1].h.phases[k];
                        else
                            phasekiPlusOnes[k] = (float)(MathUtils.TWOPI*(Math.random()-0.5));

                        if (!isVoiced)
                            phasekis[k] = (float)( phasekiPlusOnes[k] - (k+1)*MathUtils.TWOPI*f0InHzNext*(tsiPlusOne-tsi)); //Equation (3.54)

                        if (!(isNextVoiced && hnmSignal.frames[i+1].h!=null && hnmSignal.frames[i+1].h.phases!=null && k<hnmSignal.frames[i+1].h.phases.length))
                            phasekiPlusOnes[k] = (float)( phasekis[k] + (k+1)*MathUtils.TWOPI*f0InHz*(tsiPlusOne-tsi)); //Equation (3.55)

                        phasekiPlusOneEstimate = (float)( phasekis[k] + (k+1)*MathUtils.TWOPI*f0av*(tsiPlusOne-tsi));
                        Mk = (int)Math.floor((phasekiPlusOneEstimate-phasekiPlusOnes[k])/MathUtils.TWOPI + 0.5);
                        phasekt = (float)( phasekis[k] + (phasekiPlusOnes[k]+MathUtils.TWOPI*Mk-phasekis[k])*(t-tsi)/(tsiPlusOne-tsi) );
                        //

                        harmonicPart[n] += currentWgt*akt*Math.cos(phasekt);
                        //harmonicTracks[k][n] = harmonicPart[n];
                        //trackAmps[k][n] = akt;
                        //trackPhases[k][n] = phasekt;

                        //if (trackNoToExamine==k+1)
                        //    currentFrameTrack[n-tempStartIndex] = harmonicPart[n];
                    }
                }

            }
               
            /*
            if (tsi>0.40 && tsi<0.45)
                MaryUtils.plot(currentFrameTrack);
            */

            lastPeriodInSeconds= tsiPlusOne - tsi;
        }

        harmonicPart = MathUtils.divide(harmonicPart, 32768.0);

        //Write individual tracks to file for examination
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
        
        /*
        if (inputAudio!=null)
        {
            //k=1;
            for (k=0; k<harmonicTracks.length; k++)
            {
                //MaryUtils.plot(trackAmps[k]);
                //MaryUtils.plot(trackPhases[k]);

                harmonicTracks[k] = MathUtils.divide(harmonicTracks[k], 32768.0);
                //MaryUtils.plot(harmonicTracks[k]);

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
        */

        return harmonicPart;
    }

    //LPC based noise model + OLA approach
    public double[] synthesizeNoisePartLpc(HntmSpeechSignal hnmSignal, 
            float[] tScales,
            float[] tScalesTimes,
            float[] pScales,
            float[] pScalesTimes)
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
        int origLen = SignalProcUtils.time2sample(hnmSignal.originalDurationInSeconds, hnmSignal.samplingRateInHz);

        float tscale = tScales[0];
        int outputLen = (int)Math.floor(tscale*origLen+0.5);
        
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

                wsNoise = SignalProcUtils.time2sample(tscale*(NOISE_SYNTHESIS_WINDOW_DURATION_IN_SECONDS), hnmSignal.samplingRateInHz);
                if (!isNextNoised)
                    wsNoise += transitionOverlapLen;
                if (!isPrevNoised)
                    wsNoise += transitionOverlapLen;
                
                if (wsNoise%2==0) //Always use an odd window size to have a zero-phase analysis window
                    wsNoise++; 

                if (i==0)
                    tsi = 0.0f;
                else
                    tsi = Math.max(0.0f, tscale*hnmSignal.frames[i].tAnalysisInSeconds-0.5f*NOISE_SYNTHESIS_WINDOW_DURATION_IN_SECONDS);

                //if (tsi>1.8 && tsi<1.82)
                //    System.out.println("Time=" + String.valueOf(tsi) + " " + (isPrevNoised?"+":"-") + (isNoised?"+":"-") + (isNextNoised?"+":"-"));

                startIndex = SignalProcUtils.time2sample(tsi, hnmSignal.samplingRateInHz);

                if (i<hnmSignal.frames.length-1)
                {
                    tsiNext = Math.max(0.0f, tscale*hnmSignal.frames[i].tAnalysisInSeconds+0.5f*NOISE_SYNTHESIS_WINDOW_DURATION_IN_SECONDS);
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
                    winNoise.normalizePeakValue(1.0f);
                    wgt = winNoise.getCoeffs();
                    //

                    //x = SignalProcUtils.getWhiteNoiseOfVariance(wsNoise, 1.0); //Variance specified white noise
                    //x = SignalProcUtils.getWhiteNoise(wsNoise, 0.5); //Absolute value limited white noise

                    //double[] tmpNoise = SignalProcUtils.getNoise(hnmSignal.frames[i].maximumFrequencyOfVoicingInHz, 0.5f*hnmSignal.samplingRateInHz, 50.0, hnmSignal.samplingRateInHz, 5*wsNoise); //Pink noise
                    //x = new double[wsNoise];
                    //System.arraycopy(tmpNoise, 2*wsNoise, x, 0, wsNoise); 

                    x = SignalProcUtils.getNoise(hnmSignal.frames[i].maximumFrequencyOfVoicingInHz, 0.5*hnmSignal.samplingRateInHz, 100.0, hnmSignal.samplingRateInHz, wsNoise); //Pink noise

                    /*
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
                     */

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
                    
                    //Overlap-add
                    for (n=startIndex; n<Math.min(startIndex+wsNoise, noisePart.length); n++)
                        noisePart[n] += y[n-startIndex]*wgt[n-startIndex]; 
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

            /*
            //Normalize according to total window weights per sample
            for (n=0; n<outputLen; n++)
            {
                if (winWgtSum[n]>0.0)
                {
                    noisePart[n] /= winWgtSum[n];
                    //if (Double.isNaN(noisePart[n]))
                    //   System.out.println("NaN!!!");
                }
            }
            //
            */
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
                    tsi = tscale*hnmSignal.frames[i].tAnalysisInSeconds;

                startIndex = SignalProcUtils.time2sample(tsi, hnmSignal.samplingRateInHz);

                if (i<hnmSignal.frames.length-1)
                {
                    tsiNext = Math.max(0.0f, tscale*hnmSignal.frames[i+1].tAnalysisInSeconds);
                    startIndexNext = SignalProcUtils.time2sample(tsiNext, hnmSignal.samplingRateInHz);
                }
                else
                {
                    startIndexNext = outputLen-1;
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

        return noisePart;
    }

    //Pseudo harmonics based noise generation for pseudo periods
    public static double[] synthesizeNoisePartPseudoHarmonic(HntmSpeechSignal hnmSignal, 
            float[] tScales,
            float[] tScalesTimes,
            float[] pScales,
            float[] pScalesTimes)
    {
        double[] noisePart = null;
        double[][] noiseTracks = null;
        double[][] trackAmps = null;
        double[][] trackPhases = null;
        double[] currentFrameTrack = null;
        int trackNoToExamine = 1;

        double overlapAmountInSeconds = 0.005;
        int overlapSize = SignalProcUtils.time2sample(overlapAmountInSeconds, hnmSignal.samplingRateInHz);
        Window w = new HanningWindow(2*overlapSize);
        double[] wgtLeft = w.getCoeffsLeftHalf();
        double[] wgtRight = w.getCoeffsRightHalf();

        int i, k, n;
        double t, tsi, tsiPlusOne; //Time in seconds
        double frameStartInSeconds, frameEndInSeconds;
        int frameStartIndex, frameEndIndex;

        double akt;
        int startPseudoHarmonicNo, startPseudoHarmonicNoNext;
        int endPseudoHarmonicNo = (int)Math.floor((0.5*hnmSignal.samplingRateInHz)/HntmAnalyzer.NOISE_F0_IN_HZ+0.5);

        int numNoiseHarmonicsCurrentFrame;
        int numNoiseHarmonicsNextFrame = 0;
        int maxNumHarmonics = 0;
        for (i=0; i<hnmSignal.frames.length; i++)
        {
            if (hnmSignal.frames[i].maximumFrequencyOfVoicingInHz>0.0f && hnmSignal.frames[i].maximumFrequencyOfVoicingInHz<0.5*hnmSignal.samplingRateInHz)
                startPseudoHarmonicNo = (int)(Math.floor((hnmSignal.frames[i].maximumFrequencyOfVoicingInHz+0.5*HntmAnalyzer.NOISE_F0_IN_HZ)/HntmAnalyzer.NOISE_F0_IN_HZ))-1; 
            else
                startPseudoHarmonicNo = 0;

            numNoiseHarmonicsCurrentFrame = endPseudoHarmonicNo-startPseudoHarmonicNo+1;
            if (numNoiseHarmonicsCurrentFrame>maxNumHarmonics)
                maxNumHarmonics = numNoiseHarmonicsCurrentFrame;
        }

        double[] aksis = null;
        double[] aksiPlusOnes = null;
        float[] phasekis = null;
        float[] phasekiPlusOnes = null;
        if (maxNumHarmonics>0)
        {
            noiseTracks = new double[endPseudoHarmonicNo+1][];
            trackAmps = new double[endPseudoHarmonicNo+1][];
            trackPhases = new double[endPseudoHarmonicNo+1][];

            aksis = new double[endPseudoHarmonicNo+1];
            Arrays.fill(aksis, 0.0);
            aksiPlusOnes = new double[endPseudoHarmonicNo+1];
            Arrays.fill(aksis, 0.0);
            phasekis = new float[endPseudoHarmonicNo+1];
            phasekiPlusOnes = new float[endPseudoHarmonicNo+1];
        }

        double lastPeriodInSeconds = 0.0;
        double ht;
        float phasekt = 0.0f;

        float phasekiPlusOneEstimate = 0.0f;
        int Mk;
        boolean isNoised, isNextNoised, isPrevNoised;
        int origLen = SignalProcUtils.time2sample(hnmSignal.originalDurationInSeconds, hnmSignal.samplingRateInHz);
        noisePart = new double[origLen]; //In fact, this should be prosody scaled length when you implement prosody modifications
        Arrays.fill(noisePart, 0.0);
        double currentWgt;

        for (k=0; k<endPseudoHarmonicNo+1; k++)
        {
            noiseTracks[k] = new double[origLen];
            Arrays.fill(noiseTracks[k], 0.0);
            trackAmps[k] = new double[origLen];
            Arrays.fill(trackAmps[k], 0.0);
            trackPhases[k] = new double[origLen];
            Arrays.fill(trackPhases[k], 0.0);
        }
        
        for (i=0; i<hnmSignal.frames.length; i++)
        {
            isNoised = ((hnmSignal.frames[i].maximumFrequencyOfVoicingInHz<0.5*hnmSignal.samplingRateInHz) ? true : false);
            if (i<hnmSignal.frames.length-1)
                isNextNoised = ((hnmSignal.frames[i+1].maximumFrequencyOfVoicingInHz<0.5*hnmSignal.samplingRateInHz) ? true : false);
            else
                isNextNoised = false;

            if (i>0)
                isPrevNoised = ((hnmSignal.frames[i-1].maximumFrequencyOfVoicingInHz<0.5*hnmSignal.samplingRateInHz) ? true : false);
            else
                isPrevNoised = true;

            if (i==0)
                tsi = 0.0f;
            else
                tsi = hnmSignal.frames[i].tAnalysisInSeconds;
            frameStartInSeconds = tsi;
            if (isNextNoised)
            {
                if (i==hnmSignal.frames.length-1)
                    tsiPlusOne = hnmSignal.originalDurationInSeconds;
                else
                    tsiPlusOne = hnmSignal.frames[i+1].tAnalysisInSeconds;
                frameEndInSeconds = tsiPlusOne;
            }
            else
            {
                if (i==hnmSignal.frames.length-1)
                {
                    tsiPlusOne = hnmSignal.originalDurationInSeconds;
                    frameEndInSeconds = tsiPlusOne;
                }
                else
                {
                    frameEndInSeconds = frameStartInSeconds + lastPeriodInSeconds;
                    tsiPlusOne = frameEndInSeconds;
                }
            }

            if (hnmSignal.frames[i].maximumFrequencyOfVoicingInHz>0.0f && hnmSignal.frames[i].maximumFrequencyOfVoicingInHz<0.5*hnmSignal.samplingRateInHz)
                startPseudoHarmonicNo = (int)(Math.floor((hnmSignal.frames[i].maximumFrequencyOfVoicingInHz+0.5*HntmAnalyzer.NOISE_F0_IN_HZ)/HntmAnalyzer.NOISE_F0_IN_HZ))-1;
            else
                startPseudoHarmonicNo = 0;

            if (i==0)
                numNoiseHarmonicsCurrentFrame = endPseudoHarmonicNo-startPseudoHarmonicNo+1;
            else
                numNoiseHarmonicsCurrentFrame = numNoiseHarmonicsNextFrame;

            if (i<hnmSignal.frames.length-1)
            {  
                if (hnmSignal.frames[i+1].maximumFrequencyOfVoicingInHz>0.0f && hnmSignal.frames[i+1].maximumFrequencyOfVoicingInHz<0.5*hnmSignal.samplingRateInHz)
                    startPseudoHarmonicNoNext = (int)(Math.floor((hnmSignal.frames[i+1].maximumFrequencyOfVoicingInHz+0.5*HntmAnalyzer.NOISE_F0_IN_HZ)/HntmAnalyzer.NOISE_F0_IN_HZ))-1;
                else
                    startPseudoHarmonicNoNext = 0;

                numNoiseHarmonicsNextFrame = endPseudoHarmonicNo-startPseudoHarmonicNoNext+1;
            }            

            frameStartIndex = SignalProcUtils.time2sample(frameStartInSeconds, hnmSignal.samplingRateInHz);
            frameEndIndex = SignalProcUtils.time2sample(frameEndInSeconds, hnmSignal.samplingRateInHz);

            int tempStartIndex;
            if (!isPrevNoised && isNoised)
                tempStartIndex = Math.max(0,frameStartIndex-overlapSize);
            else
                tempStartIndex = frameStartIndex;

            int tempEndIndex;
            if (isNoised && !isNextNoised)
                tempEndIndex = Math.max(0,frameEndIndex-overlapSize);
            else
                tempEndIndex = frameEndIndex;

            if (tempEndIndex-tempStartIndex+1>0)
            {
                currentFrameTrack = new double[tempEndIndex-tempStartIndex+1];

                for (n=tempStartIndex; n<=Math.min(tempEndIndex, origLen-1); n++)
                {
                    if (!isPrevNoised && isNoised && n<frameStartIndex)
                        currentWgt = wgtLeft[n-(frameStartIndex-overlapSize)];
                    else if (isNoised && !isNextNoised && n>Math.min(frameEndIndex,origLen-1))
                        currentWgt = wgtRight[n-(frameStartIndex-overlapSize)];
                    else
                        currentWgt = 1.0;

                    t = SignalProcUtils.sample2time(n, hnmSignal.samplingRateInHz);

                    noisePart[n] = 0.0;
                    for (k=startPseudoHarmonicNo; k<=endPseudoHarmonicNo; k++)
                    {
                        //Estimate amplitude
                        aksis[k] = RegularizedCepstralEnvelopeEstimator.cepstrum2linearSpectrumValue(((FrameNoisePartPseudoHarmonic)hnmSignal.frames[i].n).ceps, (k+1)*HntmAnalyzer.NOISE_F0_IN_HZ, hnmSignal.samplingRateInHz);
                        //aksis[k] = hnmSignal.frames[i].h.ceps[k]; //Use amplitudes directly without cepstrum method

                        aksiPlusOnes[k] = aksis[k];
                        if (isNextNoised && i<hnmSignal.frames.length-1 && ((FrameNoisePartPseudoHarmonic)hnmSignal.frames[i+1].n).ceps!=null)
                        {
                            if ((k+1)*HntmAnalyzer.NOISE_F0_IN_HZ<0.5*hnmSignal.samplingRateInHz)
                            {     
                                aksiPlusOnes[k] = RegularizedCepstralEnvelopeEstimator.cepstrum2linearSpectrumValue(((FrameNoisePartPseudoHarmonic)hnmSignal.frames[i+1].n).ceps , (k+1)*HntmAnalyzer.NOISE_F0_IN_HZ, hnmSignal.samplingRateInHz);
                                //aksiPlusOnes[k] = hnmSignal.frames[i+1].h.ceps[k]; //Use amplitudes directly without cepstrum method
                            }
                        }

                        akt = aksis[k] + (aksiPlusOnes[k]-aksis[k])*(t-tsi)/(tsiPlusOne-tsi);
                        //

                        //Estimate phase                        
                        phasekis[k] = (float)(MathUtils.TWOPI*(Math.random()-0.5));
                        phasekiPlusOnes[k] = (float)(MathUtils.TWOPI*(Math.random()-0.5));

                        if (!isNoised)
                            phasekis[k] = (float)( phasekiPlusOnes[k] - (k+1)*MathUtils.TWOPI*HntmAnalyzer.NOISE_F0_IN_HZ*(tsiPlusOne-tsi) ); //Equation (3.54)

                        if (!isNextNoised)
                            phasekiPlusOnes[k] = (float)( phasekis[k] + (k+1)*MathUtils.TWOPI*HntmAnalyzer.NOISE_F0_IN_HZ*(tsiPlusOne-tsi) ); //Equation (3.55)

                        phasekiPlusOneEstimate = (float)( phasekis[k] + (k+1)*MathUtils.TWOPI*HntmAnalyzer.NOISE_F0_IN_HZ*(tsiPlusOne-tsi));
                        Mk = (int)Math.floor((phasekiPlusOneEstimate-phasekiPlusOnes[k])/MathUtils.TWOPI + 0.5);
                        phasekt = (float)( phasekis[k] + (phasekiPlusOnes[k]+MathUtils.TWOPI*Mk-phasekis[k])*(t-tsi)/(tsiPlusOne-tsi) );
                        //

                        noisePart[n] += currentWgt*akt*Math.cos(phasekt);
                        noiseTracks[k][n] = noisePart[n];
                        trackAmps[k][n] = akt;
                        trackPhases[k][n] = phasekt;

                        if (trackNoToExamine==k+1)
                            currentFrameTrack[n-tempStartIndex] = noisePart[n];
                    }
                }
            }

            lastPeriodInSeconds= tsiPlusOne - tsi;

            System.out.println("Pseudo-harmonic noise synthesis complete at " + String.valueOf(hnmSignal.frames[i].tAnalysisInSeconds) + "s. for frame " + String.valueOf(i+1) + " of " + String.valueOf(hnmSignal.frames.length)); 
        }

        //noisePart = MathUtils.divide(noisePart, 32768.0);

        //Write individual tracks to file for examination
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
        
        /*
        if (inputAudio!=null)
        {
            //for (k=0; k<harmonicTracks.length; k++)
            k=60;
            {
                MaryUtils.plot(trackAmps[k]);
                MaryUtils.plot(trackPhases[k]);

                MaryUtils.plot(noiseTracks[k]);

                DDSAudioInputStream outputAudio = new DDSAudioInputStream(new BufferedDoubleDataSource(noiseTracks[k]), inputAudio.getFormat());
                String outFileName = "d:\\noiseTrack" + String.valueOf(k+1) + ".wav";
                try {
                    AudioSystem.write(outputAudio, AudioFileFormat.Type.WAVE, new File(outFileName));
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
        */
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
                if (hnmSignal.transients.segments[i]!=null && hnmSignal.transients.segments[i].waveform!=null && hnmSignal.transients.segments[i].startTime>=0.0f && hnmSignal.transients.segments[i].endTime>=0.0f)
                {
                    startInd = Math.min(SignalProcUtils.time2sample(hnmSignal.transients.segments[i].startTime, hnmSignal.samplingRateInHz), outputLen-1);
                    windowLeftEndInd = Math.min(startInd+winMidInd, outputLen-1);
                    endInd = Math.min(SignalProcUtils.time2sample(hnmSignal.transients.segments[i].endTime, hnmSignal.samplingRateInHz), outputLen-1);
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
        //

        //Analysis
        float windowSizeInSeconds = 0.050f;
        float skipSizeInSeconds = 0.010f;

        HntmAnalyzer ha = new HntmAnalyzer();
        
        int model = HntmAnalyzer.HARMONICS_PLUS_NOISE;
        //int model = HnmAnalyzer.HARMONICS_PLUS_TRANSIENTS_PLUS_NOISE;
        
        int noisePartRepresentation = HntmAnalyzer.LPC;
        //int noisePartRepresentation = HnmAnalyzer.PSEUDO_HARMONIC;

        HntmSpeechSignal hnmSignal = ha.analyze(wavFile, windowSizeInSeconds, skipSizeInSeconds, model, noisePartRepresentation);
        //

        if (hnmSignal!=null)
        {
            //Synthesis
            float[] tScales = {1.0f};
            float[] tScalesTimes = null;
            float[] pScales = {1.0f};
            float[] pScalesTimes = null;

            HntmSynthesizer hs = new HntmSynthesizer();
            HntmSynthesizedSignal xhat = hs.synthesize(hnmSignal, tScales, tScalesTimes, pScales, pScalesTimes);

            double hGain = 1.0;
            double nGain = 0.05/32768;
            double tGain = 0.3/32768;
            if (xhat.harmonicPart!=null)
            {
                xhat.harmonicPart = MathUtils.multiply(xhat.harmonicPart, hGain);
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
            y = MathUtils.multiply(y, MathUtils.absMax(x)/MathUtils.absMax(y));

            //xhat.noisePart = ArrayUtils.subarray(hpf, 0, hpf.length);
            //

            //File output
            DDSAudioInputStream outputAudio = null;
            String outFileName = null;
            String strExt = "";
            String modelName = "";
            if (model==HntmAnalyzer.HARMONICS_PLUS_NOISE)
                modelName = "hnm";
            else if (model==HntmAnalyzer.HARMONICS_PLUS_TRANSIENTS_PLUS_NOISE)
                modelName = "hnmt";

            outputAudio = new DDSAudioInputStream(new BufferedDoubleDataSource(y), inputAudio.getFormat());
            outFileName = wavFile.substring(0, wavFile.length()-4) + "_" + modelName + "Resynth" + strExt + ".wav";
            AudioSystem.write(outputAudio, AudioFileFormat.Type.WAVE, new File(outFileName));

            if (xhat.harmonicPart!=null)
            {
                outputAudio = new DDSAudioInputStream(new BufferedDoubleDataSource(xhat.harmonicPart), inputAudio.getFormat());
                outFileName = wavFile.substring(0, wavFile.length()-4) + "_" + modelName + "Harmonic" + strExt + ".wav";
                AudioSystem.write(outputAudio, AudioFileFormat.Type.WAVE, new File(outFileName));
            }
            
            if (xhat.noisePart!=null)
            {
                outputAudio = new DDSAudioInputStream(new BufferedDoubleDataSource(xhat.noisePart), inputAudio.getFormat());
                outFileName = wavFile.substring(0, wavFile.length()-4) + "_" + modelName + "Noise" + strExt + ".wav";
                AudioSystem.write(outputAudio, AudioFileFormat.Type.WAVE, new File(outFileName));
            }

            if (xhat.transientPart!=null)
            {
                outputAudio = new DDSAudioInputStream(new BufferedDoubleDataSource(xhat.transientPart), inputAudio.getFormat());
                outFileName = wavFile.substring(0, wavFile.length()-4) + "_" + modelName + "Transient" + strExt + ".wav";
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
