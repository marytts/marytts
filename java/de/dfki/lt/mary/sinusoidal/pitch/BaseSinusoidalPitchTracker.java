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

package de.dfki.lt.mary.sinusoidal.pitch;

import de.dfki.lt.mary.sinusoidal.SinusoidalSpeechFrame;
import de.dfki.lt.mary.sinusoidal.SinusoidalSpeechSignal;
import de.dfki.lt.signalproc.util.MathUtils;
import de.dfki.lt.signalproc.util.SignalProcUtils;

/**
 * @author oytun.turk
 *
 */
public class BaseSinusoidalPitchTracker {

    public BaseSinusoidalPitchTracker()
    {
        
    }
    
    public float [] pitchTrack(SinusoidalSpeechSignal sinSignal, int samplingRate, float searchStepInHz, float minFreqInHz, float maxFreqInHz)
    {
        float [] f0s = null;
        
        if (sinSignal.framesSins.length>0)
        {
            f0s = new float[sinSignal.framesSins.length];
            
            int i;
            for (i = 0; i<sinSignal.framesSins.length; i++)
            {
                f0s[i] = pitchAnalyzeFrame(sinSignal.framesSins[i], samplingRate, searchStepInHz, minFreqInHz, maxFreqInHz);
                System.out.println("f0=" + String.valueOf(f0s[i]) + " " + String.valueOf(i+1) + " of " + String.valueOf(sinSignal.framesSins.length));
            }
        }
        
        return f0s;
    }
    
    public float pitchAnalyzeFrame(SinusoidalSpeechFrame sinFrame, int samplingRate, float searchStepInHz, float minFreqInHz, float maxFreqInHz)
    {
        float f0 = 0.0f;
        
        int i, k;
        int numCandidates = (int)Math.floor((maxFreqInHz-minFreqInHz)/searchStepInHz + 1 + 0.5);
        float w0, w0InRadians;
        double [] Q = new double[numCandidates];
        double maxQ = 0.0;
        int stInd, enInd;
        float minFreqInRadians = SignalProcUtils.hz2radian(minFreqInHz, samplingRate);
        float maxFreqInRadians = SignalProcUtils.hz2radian(maxFreqInHz, samplingRate);
        
        for (i=0; i<numCandidates; i++)
        {
            w0 = i*searchStepInHz+minFreqInHz;
            w0InRadians = SignalProcUtils.hz2radian(w0, samplingRate);
            Q[i] = 0.0f;
            
            stInd = 0;
            while (sinFrame.sinusoids[stInd].freq<minFreqInRadians)
            {
                stInd++;
            
                if (stInd>=sinFrame.sinusoids.length-1)
                {
                    stInd=sinFrame.sinusoids.length-1;
                    break;
                }
            }
            
            enInd = stInd;
            while (sinFrame.sinusoids[enInd].freq<maxFreqInRadians)
            {
                enInd++;
            
                if (enInd>=sinFrame.sinusoids.length-1)
                {
                    enInd=sinFrame.sinusoids.length-1;
                    break;
                }
            }
            
            Q[i] = performanceCriterion(sinFrame, stInd, enInd, w0, samplingRate);
            
            if (i==0 || Q[i]>maxQ)
            {
                f0 = w0;
                maxQ = Q[i];
            }
        }

        return f0;
    }
    
    //Baseline version that does nothing, implement functionality in derived classes
    public double performanceCriterion(SinusoidalSpeechFrame sinFrame, int startIndex, int endIndex, float f0Candidate, int samplingRate)
    {
        return -1.0f;
    }

}
