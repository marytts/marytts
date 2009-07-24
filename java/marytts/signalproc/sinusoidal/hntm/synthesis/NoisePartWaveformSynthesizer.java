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
import marytts.signalproc.sinusoidal.hntm.analysis.HntmAnalyzerParams;
import marytts.signalproc.sinusoidal.hntm.analysis.HntmSpeechFrame;
import marytts.signalproc.sinusoidal.hntm.analysis.HntmSpeechSignal;
import marytts.signalproc.window.HammingWindow;
import marytts.signalproc.window.Window;
import marytts.util.MaryUtils;
import marytts.util.data.audio.AudioDoubleDataSource;
import marytts.util.math.ArrayUtils;
import marytts.util.math.MathUtils;
import marytts.util.signal.SignalProcUtils;

/**
 * Synthesizes noise part waveform from non-overlapping chunks of data.
 * This model is the most natural one since it involves no noise models.
 * 
 * @author oytun.turk
 *
 */
public class NoisePartWaveformSynthesizer 
{    
    //TO DO: This should use overlap add since the noise waveform will not be a continuous waveform in TTS
    public static double[] synthesize(HntmSpeechSignal hnmSignal, HntmSpeechFrame[] leftContexts, HntmSpeechFrame[] rightContexts, HntmAnalyzerParams analysisParams)
    {
        double[] noisePartWaveform = null;
        
        if (hnmSignal!=null && hnmSignal.frames!=null)
        {
            int outputLen = SignalProcUtils.time2sample(hnmSignal.originalDurationInSeconds, hnmSignal.samplingRateInHz);

            noisePartWaveform = new double[outputLen];
            double[] wgts = new double[outputLen];
            Arrays.fill(noisePartWaveform, 0.0);
            Arrays.fill(wgts, 0.0);
            int i, j;
            double[] frameWaveform = null;
            double[] leftContextWaveform = null;
            double[] rightContextWaveform = null;

            int waveformNoiseStartInd;
            
            //TO DO: Overlap waveform noise case! See analysis code!
            for (i=0; i<hnmSignal.frames.length; i++)
            {
                if (hnmSignal.frames[i].n!=null && (hnmSignal.frames[i].n instanceof FrameNoisePartWaveform))
                {
                    frameWaveform = ((FrameNoisePartWaveform)hnmSignal.frames[i].n).waveform2Doubles();
                    
                    if (leftContexts==null) //Take the previous frame parameters as left context (i.e. the HNM signal is a continuous one, not concatenated one
                    {
                        if (i>0)
                            leftContextWaveform = ((FrameNoisePartWaveform)hnmSignal.frames[i-1].n).waveform2Doubles();
                        else
                        {
                            leftContextWaveform = new double[frameWaveform.length];
                            Arrays.fill(leftContextWaveform, 0.0);
                        }
                    }
                    else
                    {
                        if (leftContexts[i]!=null)
                            leftContextWaveform = ArrayUtils.copy(((FrameNoisePartWaveform)leftContexts[i].n).waveform2Doubles());
                        else
                        {
                            leftContextWaveform = new double[frameWaveform.length];
                            Arrays.fill(leftContextWaveform, 0.0);
                        }   
                    }

                    waveformNoiseStartInd = SignalProcUtils.time2sample(hnmSignal.frames[i].tAnalysisInSeconds, hnmSignal.samplingRateInHz);
                    waveformNoiseStartInd -= leftContextWaveform.length;
                    
                    if (rightContexts==null) //Take the next frame parameters as right context (i.e. the HNM signal is a continuous one, not concatenated one
                    {
                        if (i<hnmSignal.frames.length-1)
                            rightContextWaveform = ((FrameNoisePartWaveform)hnmSignal.frames[i+1].n).waveform2Doubles();
                        else
                        {
                            rightContextWaveform = new double[frameWaveform.length];
                            Arrays.fill(rightContextWaveform, 0.0);
                        }
                    }
                    else
                    {
                        if (rightContexts[i]!=null)
                            rightContextWaveform = ArrayUtils.copy(((FrameNoisePartWaveform)rightContexts[i].n).waveform2Doubles());
                        else
                        {
                            rightContextWaveform = new double[frameWaveform.length];
                            Arrays.fill(rightContextWaveform, 0.0);
                        }
                    }

                    frameWaveform = ArrayUtils.combine(leftContextWaveform, frameWaveform);
                    frameWaveform = ArrayUtils.combine(frameWaveform, rightContextWaveform);
                    
                    if (frameWaveform!=null)
                    {
                        Window w = new HammingWindow(frameWaveform.length);
                        double[] wgt = w.getCoeffs();
                        for (j=waveformNoiseStartInd; j<Math.min(waveformNoiseStartInd+frameWaveform.length, outputLen); j++)
                        {
                            if (waveformNoiseStartInd+j>=0)
                            {
                                noisePartWaveform[j] += frameWaveform[j-waveformNoiseStartInd]*wgt[j-waveformNoiseStartInd];
                                wgts[j] += wgt[j-waveformNoiseStartInd];
                            }
                        }
                    }
                }
            }

            for (i=0; i<outputLen; i++)
            {
                if (wgts[i]>1.0e-10)
                    noisePartWaveform[i] /= wgts[i];
            }
        }
        
        return noisePartWaveform;
    }
}

