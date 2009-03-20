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

package marytts.signalproc.sinusoidal.hntm.modification;

import java.util.Arrays;
import java.util.Vector;

import marytts.signalproc.process.TDPSOLAInstants;
import marytts.signalproc.process.TDPSOLAProcessor;
import marytts.signalproc.sinusoidal.hntm.analysis.HntmPlusTransientsSpeechSignal;
import marytts.signalproc.sinusoidal.hntm.analysis.HntmSpeechFrame;
import marytts.signalproc.sinusoidal.hntm.analysis.HntmSpeechSignal;
import marytts.signalproc.sinusoidal.hntm.analysis.TransientSegment;
import marytts.signalproc.sinusoidal.hntm.synthesis.HntmSynthesizedSignal;
import marytts.util.MaryUtils;
import marytts.util.math.ArrayUtils;
import marytts.util.math.MathUtils;
import marytts.util.signal.SignalProcUtils;

/**
 * @author oytun.turk
 *
 */
public class HntmDurationModifier {
    
    public static HntmSpeechSignal modify(HntmSpeechSignal hntmSignal, float[] tScales, float[] tScalesTimes, float[] pScales, float[] pScalesTimes)
    {
        int i, j;                
        HntmSpeechSignal hntmSignalMod = null;
        
        //Pre-process tScales and tScaleTimes to make sure transients are not duration modified but only shifted#
        //Sort input time scales if required
        int[] sortedIndices;
        
        if (tScalesTimes!=null)
        {  
            sortedIndices = MathUtils.quickSort(tScalesTimes);
            tScales = MathUtils.sortAs(tScales, sortedIndices);
        }
        
        float[] tScalesMod = new float[hntmSignal.frames.length+1];
        float[] allScalesTimes = new float[hntmSignal.frames.length+1];
        float[] pScalesMod = new float[hntmSignal.frames.length+1];
        for (i=0; i<hntmSignal.frames.length; i++)
            allScalesTimes[i] = hntmSignal.frames[i].tAnalysisInSeconds;
        allScalesTimes[hntmSignal.frames.length] = hntmSignal.frames[hntmSignal.frames.length-1].tAnalysisInSeconds+(hntmSignal.frames[hntmSignal.frames.length-1].tAnalysisInSeconds-hntmSignal.frames[hntmSignal.frames.length-2].tAnalysisInSeconds);
        
        if (tScalesTimes!=null)
        {
            if (tScales.length!=tScalesTimes.length)
            {
                System.out.println("Error! Time scale array and associated instants should be of identical length");
                return null;
            }
            
            //Map tScalesTimes to the analysis time axis (which is now in allScalesTimes)
            int scaleIndex;
            float alpha;
            for (i=0; i<allScalesTimes.length; i++)
            {
                //For time scales
                scaleIndex = MathUtils.findClosest(tScalesTimes, allScalesTimes[i]);
                if (allScalesTimes[i]>tScalesTimes[scaleIndex])
                {
                    if (scaleIndex<tScalesTimes.length-1)
                    {
                        if ((tScalesTimes[scaleIndex+1]-tScalesTimes[scaleIndex])>1e-10)
                            alpha = (tScalesTimes[scaleIndex+1]-allScalesTimes[i])/(tScalesTimes[scaleIndex+1]-tScalesTimes[scaleIndex]);
                        else
                            alpha = 0.5f;
                        
                        tScalesMod[i] = alpha*tScales[scaleIndex] + (1.0f-alpha)*tScales[scaleIndex+1];
                    }
                    else
                        tScalesMod[i] = tScales[scaleIndex];
                }
                else if (allScalesTimes[i]<tScalesTimes[scaleIndex])
                {
                    if (scaleIndex>0)
                    {
                        if ((tScalesTimes[scaleIndex]-tScalesTimes[scaleIndex-1])>1e-10)
                            alpha = (tScalesTimes[scaleIndex]-allScalesTimes[i])/(tScalesTimes[scaleIndex]-tScalesTimes[scaleIndex-1]);
                        else
                            alpha = 0.5f;
                        
                        tScalesMod[i] = alpha*tScales[scaleIndex-1] + (1.0f-alpha)*tScales[scaleIndex];        
                    }
                    else
                        tScalesMod[i] = tScales[scaleIndex];
                }
                else
                    tScalesMod[i] = tScales[scaleIndex];
                //
            }
            //
        }
        else
            Arrays.fill(tScalesMod, tScales[0]);
        
        if (pScalesTimes!=null)
        {
            if (pScales.length!=pScalesTimes.length)
            {
                System.out.println("Error! Pitch scale array and associated instants should be of identical length");
                return null;
            }
            
            //Map pScalesTimes to the analysis time axis (which is now in allScalesTimes)
            int scaleIndex;
            float alpha;
            for (i=0; i<allScalesTimes.length; i++)
            {
                //For pitch scales
                scaleIndex = MathUtils.findClosest(pScalesTimes, allScalesTimes[i]);
                if (allScalesTimes[i]>pScalesTimes[scaleIndex])
                {
                    if (scaleIndex<pScalesTimes.length-1)
                    {
                        if ((pScalesTimes[scaleIndex+1]-pScalesTimes[scaleIndex])>1e-10)
                            alpha = (pScalesTimes[scaleIndex+1]-allScalesTimes[i])/(pScalesTimes[scaleIndex+1]-pScalesTimes[scaleIndex]);
                        else
                            alpha = 0.5f;
                        
                        pScalesMod[i] = alpha*pScales[scaleIndex] + (1.0f-alpha)*pScales[scaleIndex+1];
                    }
                    else
                        pScalesMod[i] = pScales[scaleIndex];
                }
                else if (allScalesTimes[i]<pScalesTimes[scaleIndex])
                {
                    if (scaleIndex>0)
                    {
                        if ((pScalesTimes[scaleIndex]-pScalesTimes[scaleIndex-1])>1e-10)
                            alpha = (pScalesTimes[scaleIndex]-allScalesTimes[i])/(pScalesTimes[scaleIndex]-pScalesTimes[scaleIndex-1]);
                        else
                            alpha = 0.5f;
                        
                        pScalesMod[i] = alpha*pScales[scaleIndex-1] + (1.0f-alpha)*pScales[scaleIndex];
                    }
                    else
                        pScalesMod[i] = pScales[scaleIndex];
                }
                else
                    pScalesMod[i] = pScales[scaleIndex];
                //
            }
            //
        }
        else
            Arrays.fill(pScalesMod, pScales[0]);
        
        if (hntmSignal instanceof HntmPlusTransientsSpeechSignal && ((HntmPlusTransientsSpeechSignal)hntmSignal).transients!=null)
        {   
            int numTransientSegments = ((HntmPlusTransientsSpeechSignal)hntmSignal).transients.segments.length;
            
            hntmSignalMod = new HntmPlusTransientsSpeechSignal(hntmSignal.frames.length, hntmSignal.samplingRateInHz, hntmSignal.originalDurationInSeconds, 
                                                               hntmSignal.f0WindowDurationInSeconds, hntmSignal.f0SkipSizeInSeconds,
                                                               hntmSignal.windowDurationInSecondsNoise, hntmSignal.preCoefNoise, numTransientSegments);

            if (numTransientSegments>0)
            {
                float[] tempScales = new float[4*numTransientSegments];
                float[] tempScalesTimes = new float[4*numTransientSegments];
                float[] tempScales2 = ArrayUtils.copy(tScalesMod);
                float[] tempScalesTimes2 = ArrayUtils.copy(allScalesTimes);
                
                int ind = 0;
                for (i=0; i<numTransientSegments; i++)
                {
                    tempScalesTimes[2*i] = ((HntmPlusTransientsSpeechSignal)hntmSignal).transients.segments[i].startTime;
                    tempScales[2*i] = 1.0f;
                    tempScalesTimes[2*i+1] = ((HntmPlusTransientsSpeechSignal)hntmSignal).transients.segments[i].getEndTime(hntmSignal.samplingRateInHz);
                    tempScales[2*i+1] = 1.0f;
                 
                    if (tempScalesTimes2!=null)
                    {
                        for (j=0; j<tempScalesTimes2.length; j++)
                        {
                            if (tempScalesTimes2[j]>=tempScalesTimes[2*i] && tempScalesTimes2[j]<=tempScalesTimes[2*i+1])
                                tempScales2[j] = 1.0f;
                        }
                    }
                }
                
                for (i=numTransientSegments; i<2*numTransientSegments; i++)
                {
                    tempScalesTimes[2*i] = ((HntmPlusTransientsSpeechSignal)hntmSignal).transients.segments[i-numTransientSegments].startTime-0.001f;
                    tempScales[2*i] = 1.0f;
                    for (j=0; j<allScalesTimes.length; j++)
                    {
                        if (tempScalesTimes[2*i]>allScalesTimes[j])
                        {
                            tempScales[2*i] = tScalesMod[j];
                            break;
                        }
                    }
                    
                    tempScalesTimes[2*i+1] = ((HntmPlusTransientsSpeechSignal)hntmSignal).transients.segments[i-numTransientSegments].getEndTime(hntmSignal.samplingRateInHz)+0.001f;
                    tempScales[2*i+1] = 1.0f;
                    
                    for (j=allScalesTimes.length-1; j>=0; j--)
                    {
                        if (tempScalesTimes[2*i+1]<allScalesTimes[j])
                        {
                            tempScales[2*i+1] = tScalesMod[j];
                            break;
                        }
                    }
                }
                
                tScalesMod = ArrayUtils.combine(tempScales, tempScales2);
                allScalesTimes = ArrayUtils.combine(tempScalesTimes, tempScalesTimes2);
                sortedIndices = MathUtils.quickSort(allScalesTimes);
                tScalesMod = MathUtils.sortAs(tScalesMod, sortedIndices);
                
                for (i=0; i<numTransientSegments; i++)
                {
                    ((HntmPlusTransientsSpeechSignal)hntmSignalMod).transients.segments[i] = new TransientSegment(((HntmPlusTransientsSpeechSignal)hntmSignal).transients.segments[i]);
                    ((HntmPlusTransientsSpeechSignal)hntmSignalMod).transients.segments[i].startTime = SignalProcUtils.timeScaledTime(((HntmPlusTransientsSpeechSignal)hntmSignal).transients.segments[i].startTime, tScalesMod, allScalesTimes);
                }
            }
        }
        else
        {
            hntmSignalMod = new HntmSpeechSignal(hntmSignal.frames.length, hntmSignal.samplingRateInHz, hntmSignal.originalDurationInSeconds, 
                                                 hntmSignal.f0WindowDurationInSeconds, hntmSignal.f0SkipSizeInSeconds,
                                                 hntmSignal.windowDurationInSecondsNoise, hntmSignal.preCoefNoise);
        }
        //
        
        /*
        for (i=0; i<hntmSignal.frames.length; i++)
        {
            hntmSignalMod.frames[i] = new HntmSpeechFrame(hntmSignal.frames[i]);
            hntmSignalMod.frames[i].tAnalysisInSeconds = SignalProcUtils.timeScaledTime(hntmSignal.frames[i].tAnalysisInSeconds, tScalesMod, tScalesTimesMod);
        }
        
        hntmSignalMod.originalDurationInSeconds = SignalProcUtils.timeScaledTime(hntmSignal.originalDurationInSeconds, tScalesMod, tScalesTimesMod);
        */
        
        float[] tAnalysis = new float[hntmSignal.frames.length+1];
        for (i=0; i<hntmSignal.frames.length; i++)
            tAnalysis[i] = hntmSignal.frames[i].tAnalysisInSeconds;
        tAnalysis[hntmSignal.frames.length] = hntmSignal.frames[hntmSignal.frames.length-1].tAnalysisInSeconds+(hntmSignal.frames[hntmSignal.frames.length-1].tAnalysisInSeconds-hntmSignal.frames[hntmSignal.frames.length-2].tAnalysisInSeconds);
        boolean[] vuvs = new boolean[hntmSignal.frames.length+1];
        for (i=0; i<hntmSignal.frames.length; i++)
        {
            if (hntmSignal.frames[i].f0InHz>10.0)
                vuvs[i] = true;
            else
                vuvs[i] = false;
        }
        vuvs[hntmSignal.frames.length] = vuvs[hntmSignal.frames.length-1];
        
        TDPSOLAInstants synthesisInstants = TDPSOLAProcessor.transformAnalysisInstants(tAnalysis, hntmSignal.samplingRateInHz, vuvs, tScalesMod, pScalesMod);

        hntmSignalMod.frames = new HntmSpeechFrame[synthesisInstants.synthesisInstantsInSeconds.length];
        int currentSynthesisIndex = 0;
        boolean bBroke = false;
        for (i=0; i<synthesisInstants.repeatSkipCounts.length; i++) //This is of the same length with total analysis frames 
        {
            for (j=0; j<=synthesisInstants.repeatSkipCounts[i]; j++)
            {
                if (i<hntmSignal.frames.length)
                    hntmSignalMod.frames[currentSynthesisIndex] = new HntmSpeechFrame(hntmSignal.frames[i]);
                else
                    hntmSignalMod.frames[currentSynthesisIndex] = new HntmSpeechFrame(hntmSignal.frames[hntmSignal.frames.length-1]);
                
                hntmSignalMod.frames[currentSynthesisIndex].tAnalysisInSeconds = synthesisInstants.synthesisInstantsInSeconds[currentSynthesisIndex];
                currentSynthesisIndex++;
                
                if (currentSynthesisIndex>=hntmSignalMod.frames.length)
                {
                    bBroke = true;
                    break;
                }
            }
            
            if (bBroke)
                break;
        }
        
        float[] tSynthesis = new float[hntmSignalMod.frames.length];
        for (i=0; i<hntmSignalMod.frames.length; i++)
            tSynthesis[i] = hntmSignalMod.frames[i].tAnalysisInSeconds;
        
        hntmSignalMod.originalDurationInSeconds = hntmSignalMod.frames[hntmSignalMod.frames.length-1].tAnalysisInSeconds;
        
        //MaryUtils.plot(tAnalysis);
        //MaryUtils.plot(tSynthesis);
        
        return hntmSignalMod;
    }
}
