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

package marytts.signalproc.sinusoidal.hntm;

import marytts.signalproc.sinusoidal.hntm.HntmSpeechFrame;
import marytts.signalproc.sinusoidal.hntm.HntmSpeechSignal;
import marytts.signalproc.sinusoidal.hntm.HntmSynthesizedSignal;
import marytts.util.signal.SignalProcUtils;

/**
 * @author oytun.turk
 *
 */
public class HntmDurationModifier {
    
    public static HntmSpeechSignal modify(HntmSpeechSignal hntmSignal, float[] tScales, float[] tScalesTimes)
    {
      //Contour preprocessing carried out here for time and pitch modification
        //Pitch scale pitch contour
        //double[] f0s = null;
        //double[] f0sMod = SignalProcUtils.pitchScalePitchContour(f0s, hnmSignal.f0WindowDurationInSeconds, hnmSignal.f0SkipSizeInSeconds, pScales, pScalesTimes);

        //Time scale pitch contour
        //f0sMod = SignalProcUtils.timeScalePitchContour(f0sMod, hnmSignal.f0WindowDurationInSeconds, hnmSignal.f0SkipSizeInSeconds, tScales, tScalesTimes);

        float maxDur = SignalProcUtils.timeScaledTime(hntmSignal.originalDurationInSeconds, tScales, tScalesTimes);

        //Find modified onsets
        //PitchMarks pmMod = SignalProcUtils.pitchContour2pitchMarks(f0sMod, hnmSignal.samplingRateInHz, (int)Math.floor(maxDur*hnmSignal.samplingRateInHz+0.5), hnmSignal.f0WindowDurationInSeconds, hnmSignal.f0SkipSizeInSeconds, false, 0);
        //
        //
        
        //Synthesize harmonic part
        HntmSpeechSignal hntmSignalMod = new HntmSpeechSignal(2*hntmSignal.frames.length, hntmSignal.samplingRateInHz, 2.0f*hntmSignal.originalDurationInSeconds, 
                                                              hntmSignal.f0WindowDurationInSeconds, hntmSignal.f0SkipSizeInSeconds,
                                                              hntmSignal.windowDurationInSecondsNoise, hntmSignal.preCoefNoise);
        
        float currentTime = 0.0f;
        for (int i=0; i<hntmSignal.frames.length; i++)
        {
            hntmSignalMod.frames[2*i] = new HntmSpeechFrame(hntmSignal.frames[i]);
            hntmSignalMod.frames[2*i+1] = new HntmSpeechFrame(hntmSignal.frames[i]);
            if (i==0)
                hntmSignalMod.frames[2*i].tAnalysisInSeconds = hntmSignal.frames[i].tAnalysisInSeconds;
            else
                hntmSignalMod.frames[2*i].tAnalysisInSeconds = currentTime + (hntmSignal.frames[i].tAnalysisInSeconds-hntmSignal.frames[i-1].tAnalysisInSeconds);
            
            currentTime = hntmSignalMod.frames[2*i].tAnalysisInSeconds;
            
            if (i==0)
                hntmSignalMod.frames[2*i+1].tAnalysisInSeconds = 2*currentTime;
            else
                hntmSignalMod.frames[2*i+1].tAnalysisInSeconds = currentTime + (hntmSignal.frames[i].tAnalysisInSeconds-hntmSignal.frames[i-1].tAnalysisInSeconds);
            
            currentTime = hntmSignalMod.frames[2*i+1].tAnalysisInSeconds;
        }
        
        return hntmSignalMod;
    }
}
