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

package marytts.signalproc.sinusoidal.hntm.synthesis;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import marytts.signalproc.analysis.ReflectionCoefficients;
import marytts.signalproc.sinusoidal.hntm.analysis.FrameNoisePartWaveform;
import marytts.signalproc.sinusoidal.hntm.analysis.HntmAnalyzer;
import marytts.signalproc.sinusoidal.hntm.analysis.HntmSpeechSignal;
import marytts.signalproc.window.Window;
import marytts.util.MaryUtils;
import marytts.util.data.audio.AudioDoubleDataSource;
import marytts.util.math.ArrayUtils;
import marytts.util.math.MathUtils;
import marytts.util.signal.SignalProcUtils;

/**
 * @author oytun.turk
 *
 */
public class NoisePartWaveformSynthesizer {
    
    //LPC based noise model + OLA approach + Gain normalization according to generated harmonic part gain
    public static double[] synthesize(HntmSpeechSignal hnmSignal)
    {  
        double[] noisePart = null;
        int i;
        boolean isPrevNoised, isNoised, isNextNoised;
        boolean isVoiced, isNextVoiced;
        float t;
        float tsi = 0.0f;
        int startIndex = 0;
        int outputLen = SignalProcUtils.time2sample(hnmSignal.originalDurationInSeconds, hnmSignal.samplingRateInHz);
        
        isNoised = false;
        for (i=0; i<hnmSignal.frames.length; i++)
        {
            if (hnmSignal.frames[i].maximumFrequencyOfVoicingInHz<0.5f*hnmSignal.samplingRateInHz && ((FrameNoisePartWaveform)hnmSignal.frames[i].n).waveform!=null)
            {
                isNoised = true;
                break;
            }
        }
        
        if (isNoised) //At least one noisy frame with LP coefficients exist
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
            int n;
            int fftSizeNoise = SignalProcUtils.getDFTSize(hnmSignal.samplingRateInHz);

            boolean isDisplay = false;

            //Noise source of full length
            double[] noiseSourceHpf = null;

            int transitionOverlapLen = SignalProcUtils.time2sample(HntmSynthesizer.NOISE_SYNTHESIS_TRANSITION_OVERLAP_IN_SECONDS, hnmSignal.samplingRateInHz);
            
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

                if (i==0)
                    tsi = 0.0f;
                else
                    tsi = Math.max(0.0f, hnmSignal.frames[i].tAnalysisInSeconds-0.5f*HntmAnalyzer.NOISE_ANALYSIS_WINDOW_DURATION_IN_SECONDS);

                startIndex = SignalProcUtils.time2sample(tsi, hnmSignal.samplingRateInHz);

                if (isNoised && hnmSignal.frames[i].n!=null)
                {       
                    if (((FrameNoisePartWaveform)hnmSignal.frames[i].n).waveform!=null)
                    {
                        y = ArrayUtils.copy(((FrameNoisePartWaveform)hnmSignal.frames[i].n).waveform);
                        
                        //Compute window
                        winNoise = Window.get(windowType, y.length);
                        winNoise.normalizePeakValue(1.0f);
                        wgt = winNoise.getCoeffs();
                        //
                        
                        if (!HntmAnalyzer.HIGHPASS_FILTER_PRIOR_TO_NOISE_ANALYSIS)
                            y = SignalProcUtils.fdFilter(y, hnmSignal.frames[i].maximumFrequencyOfVoicingInHz, 0.5f*hnmSignal.samplingRateInHz, hnmSignal.samplingRateInHz, fftSizeNoise);

                        //Overlap-add
                        for (n=startIndex; n<Math.min(startIndex+y.length, noisePart.length); n++)
                        {
                            noisePart[n] += y[n-startIndex]*wgt[n-startIndex]; 
                            winWgtSum[n] += wgt[n-startIndex];
                        }
                    }
                    //
                }

                System.out.println("Waveform noise synthesis complete at " + String.valueOf(hnmSignal.frames[i].tAnalysisInSeconds) + "s. for frame " + String.valueOf(i+1) + " of " + String.valueOf(hnmSignal.frames.length) + "..."); 
            }
            
            for (i=0; i<winWgtSum.length; i++)
            {
                if (winWgtSum[i]>0.0)
                    noisePart[i] /= winWgtSum[i];
            }
        }
        
        if (hnmSignal.preCoefNoise>0.0f)
            noisePart = SignalProcUtils.removePreemphasis(noisePart, hnmSignal.preCoefNoise);
        
        //MathUtils.adjustMean(noisePart, 0.0);
        
        return noisePart;
    }
}

