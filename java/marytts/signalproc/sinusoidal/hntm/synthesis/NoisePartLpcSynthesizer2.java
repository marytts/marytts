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

import java.util.Arrays;

import marytts.signalproc.sinusoidal.hntm.analysis.HntmSpeechSignal;
import marytts.signalproc.window.Window;
import marytts.util.MaryUtils;
import marytts.util.io.FileUtils;
import marytts.util.math.ArrayUtils;
import marytts.util.math.MathUtils;
import marytts.util.signal.SignalProcUtils;

/**
 * @author oytun.turk
 *
 */
public class NoisePartLpcSynthesizer2 {
  //LPC based noise model + OLA approach + Gain normalization according to generated harmonic part gain
    public static double[] synthesize(HntmSpeechSignal hnmSignal, HntmSynthesizerParams synthesisParams)
    {    
        double[][] allLpcs = hnmSignal.getLpcsAll();
        FileUtils.toTextFile(allLpcs, "d:\\lpcs.txt");
        double[] allGains = hnmSignal.getLpcGainsAll();
        FileUtils.toTextFile(allGains, "d:\\gains.txt");
        float[] analysisTimes = hnmSignal.getAnalysisTimes();
        int[] pitchMarks = SignalProcUtils.time2sample(analysisTimes, hnmSignal.samplingRateInHz);
        FileUtils.toTextFile(pitchMarks, "d:\\pitchMarks.txt");
        float[] stds = hnmSignal.getOrigNoiseStds();
        FileUtils.toTextFile(stds, "d:\\origNoiseStds.txt");
        double[] maxFreqOfVoicings = hnmSignal.getMaximumFrequencyOfVoicings();
        FileUtils.toTextFile(maxFreqOfVoicings, "d:\\maxFreqVoicings.txt");
        
        double[] noisePart = null;
        boolean isPrevNoised, isNoised, isNextNoised;
        boolean isVoiced, isNextVoiced;
        float tsi = 0.0f;
        float tsiNext; //Time in seconds
        int i, n, j;
        int startIndex = 0;
        int startIndexNext;
        int outputLen = SignalProcUtils.time2sample(hnmSignal.originalDurationInSeconds, hnmSignal.samplingRateInHz);
        int lpOrder = 0;
        
        for (i=0; i<hnmSignal.frames.length; i++)
        {
            isNoised = ((hnmSignal.frames[i].maximumFrequencyOfVoicingInHz<0.5f*hnmSignal.samplingRateInHz) ? true : false);
            if (isNoised)
            {
                lpOrder = hnmSignal.frames[i].lpCoeffs.length;
                break;
            }
        }

        if (lpOrder>0) //At least one noisy frame with LP coefficients exist
        {
            noisePart = new double[outputLen]; //In fact, this should be prosody scaled length when you implement prosody modifications
            Arrays.fill(noisePart, 0.0);
            
            boolean bFirst = true;
            int pmInd = 0;
            int pmIndNext = 0;
            int start = 0;
            double[] tmpy = null;
            double[] tmpalpha = null;
            double tmp;
            int count;
            
            for (i=0; i<hnmSignal.frames.length; i++)
            {
                if (i>0)
                {
                    pmInd = SignalProcUtils.time2sample(hnmSignal.frames[i].tAnalysisInSeconds, hnmSignal.samplingRateInHz);
                    start = pmInd;
                }
                
                if (i<hnmSignal.frames.length-1)
                    pmIndNext = SignalProcUtils.time2sample(hnmSignal.frames[i+1].tAnalysisInSeconds, hnmSignal.samplingRateInHz);
                else
                    pmIndNext = outputLen-1;

                isNoised = ((hnmSignal.frames[i].maximumFrequencyOfVoicingInHz<0.5f*hnmSignal.samplingRateInHz) ? true : false);
  
                if (isNoised && hnmSignal.frames[i].lpCoeffs!=null)
                {
                    if (i<hnmSignal.frames.length-1)
                    {
                        for (n=0; n<=pmIndNext-pmInd; n++)
                        {
                            tmpy = new double[Math.max(start-1,0)-Math.max(start-lpOrder,0)+1];
                            count=0;
                            for (j=Math.max(start-1,0); j>=Math.max(start-lpOrder,0); j--)
                                tmpy[count++] = noisePart[j];

                            tmpalpha = new double[tmpy.length];
                            for (j=0; j<tmpy.length; j++)
                                tmpalpha[j] = hnmSignal.frames[i].lpCoeffs[j];

                            tmp = 0.0;
                            for (j=0; j<tmpalpha.length; j++)
                                tmp += tmpalpha[j]*tmpy[j];

                            noisePart[start] = hnmSignal.frames[i].lpGain*(Math.random()-0.5) - tmp;
                            start++;
                        }  
                    }
                    else //for the last frame
                    {
                        for  (n=0; n<outputLen-pmInd; n++)
                        {
                            tmpy = new double[Math.max(start-1,0)-Math.max(start-lpOrder,0)+1];
                            count=0;
                            for (j=Math.max(start-1,0); j>=Math.max(start-lpOrder,0); j--)
                                tmpy[count++] = noisePart[j];

                            tmpalpha = new double[tmpy.length];
                            for (j=0; j<tmpy.length; j++)
                                tmpalpha[j] = hnmSignal.frames[i].lpCoeffs[j];

                            tmp = 0.0;
                            for (j=0; j<tmpalpha.length; j++)
                                tmp += tmpalpha[j]*tmpy[j];

                            noisePart[start] = hnmSignal.frames[i].lpGain*(Math.random()-0.5) - tmp;
                            start++;
                        }  
                    }
                }
            } 
        }
            
        //Now, apply the triangular noise envelope for voiced parts
        if (synthesisParams.applyTriangularNoiseEnvelopeForVoicedParts)
        {
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

                        l1 = SignalProcUtils.time2sample(0.15*(tsiNext-tsi), hnmSignal.samplingRateInHz);
                        l2 = SignalProcUtils.time2sample(0.85*(tsiNext-tsi), hnmSignal.samplingRateInHz);
                        lMid = (int)Math.floor(0.5*(l1+l2)+0.5);
                        for (n=0; n<l1; n++)
                            enEnv[n] = synthesisParams.energyTriangleLowerValue;
                        for (n=l1; n<lMid; n++)
                            enEnv[n] = (n-l1)*(synthesisParams.energyTriangleUpperValue-synthesisParams.energyTriangleLowerValue)/(lMid-l1)+synthesisParams.energyTriangleLowerValue;
                        for (n=lMid; n<l2; n++)
                            enEnv[n] = (n-lMid)*(synthesisParams.energyTriangleLowerValue-synthesisParams.energyTriangleUpperValue)/(l2-lMid)+synthesisParams.energyTriangleUpperValue;
                        for (n=l2; n<enEnvLen; n++)
                            enEnv[n] = synthesisParams.energyTriangleLowerValue;

                        for (n=startIndex; n<=Math.min(noisePart.length-1, startIndexNext); n++)
                            noisePart[n] *= enEnv[n-startIndex];
                    }
                }
            }
        }
        
        if (hnmSignal.preCoefNoise>0.0f)
            noisePart = SignalProcUtils.removePreemphasis(noisePart, hnmSignal.preCoefNoise);
        
        MaryUtils.plot(noisePart);
        MathUtils.adjustMean(noisePart, 0.0);
        
        return noisePart;
    }
}
