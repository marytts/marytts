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

import marytts.signalproc.adaptation.prosody.BasicProsodyModifierParams;
import marytts.signalproc.analysis.RegularizedCepstralEnvelopeEstimator;
import marytts.signalproc.process.TDPSOLAInstants;
import marytts.signalproc.process.TDPSOLAProcessor;
import marytts.signalproc.sinusoidal.hntm.analysis.HntmAnalyzer;
import marytts.signalproc.sinusoidal.hntm.analysis.HntmPlusTransientsSpeechSignal;
import marytts.signalproc.sinusoidal.hntm.analysis.HntmSpeechFrame;
import marytts.signalproc.sinusoidal.hntm.analysis.HntmSpeechSignal;
import marytts.signalproc.sinusoidal.hntm.analysis.TransientSegment;
import marytts.signalproc.sinusoidal.hntm.synthesis.HntmSynthesizedSignal;
import marytts.signalproc.sinusoidal.hntm.synthesis.HntmSynthesizer;
import marytts.util.MaryUtils;
import marytts.util.math.ArrayUtils;
import marytts.util.math.MathUtils;
import marytts.util.signal.SignalProcUtils;

/**
 * @author oytun.turk
 *
 */
public class HntmProsodyModifier {
    
    //Note that pmodParams are changed as well
    public static HntmSpeechSignal modify(HntmSpeechSignal hntmSignal, BasicProsodyModifierParams pmodParams)
    {
        int i, j;    
        int currentHarmonicNo;
        HntmSpeechSignal hntmSignalMod = null;

        if (!pmodParams.willProsodyBeModified())
            return hntmSignal;
        else
        {
            //Pre-process tScales and tScaleTimes to make sure transients are not duration modified but only shifted
            //Sort input time scales if required
            int[] sortedIndices;

            if (pmodParams.tScalesTimes!=null)
            {  
                sortedIndices = MathUtils.quickSort(pmodParams.tScalesTimes);
                pmodParams.tScales = MathUtils.sortAs(pmodParams.tScales, sortedIndices);
            }

            float[] tScalesMod = new float[hntmSignal.frames.length+1];
            float[] allScalesTimes = new float[hntmSignal.frames.length+1];
            float[] pScalesMod = new float[hntmSignal.frames.length+1];
            for (i=0; i<hntmSignal.frames.length; i++)
                allScalesTimes[i] = hntmSignal.frames[i].tAnalysisInSeconds;
            allScalesTimes[hntmSignal.frames.length] = hntmSignal.frames[hntmSignal.frames.length-1].tAnalysisInSeconds+(hntmSignal.frames[hntmSignal.frames.length-1].tAnalysisInSeconds-hntmSignal.frames[hntmSignal.frames.length-2].tAnalysisInSeconds);

            if (pmodParams.tScalesTimes!=null)
            {
                if (pmodParams.tScales.length!=pmodParams.tScalesTimes.length)
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
                    scaleIndex = MathUtils.findClosest(pmodParams.tScalesTimes, allScalesTimes[i]);
                    if (allScalesTimes[i]>pmodParams.tScalesTimes[scaleIndex])
                    {
                        if (scaleIndex<pmodParams.tScalesTimes.length-1)
                        {
                            if ((pmodParams.tScalesTimes[scaleIndex+1]-pmodParams.tScalesTimes[scaleIndex])>1e-10)
                                alpha = (pmodParams.tScalesTimes[scaleIndex+1]-allScalesTimes[i])/(pmodParams.tScalesTimes[scaleIndex+1]-pmodParams.tScalesTimes[scaleIndex]);
                            else
                                alpha = 0.5f;

                            tScalesMod[i] = alpha*pmodParams.tScales[scaleIndex] + (1.0f-alpha)*pmodParams.tScales[scaleIndex+1];
                        }
                        else
                            tScalesMod[i] = pmodParams.tScales[scaleIndex];
                    }
                    else if (allScalesTimes[i]<pmodParams.tScalesTimes[scaleIndex])
                    {
                        if (scaleIndex>0)
                        {
                            if ((pmodParams.tScalesTimes[scaleIndex]-pmodParams.tScalesTimes[scaleIndex-1])>1e-10)
                                alpha = (pmodParams.tScalesTimes[scaleIndex]-allScalesTimes[i])/(pmodParams.tScalesTimes[scaleIndex]-pmodParams.tScalesTimes[scaleIndex-1]);
                            else
                                alpha = 0.5f;

                            tScalesMod[i] = alpha*pmodParams.tScales[scaleIndex-1] + (1.0f-alpha)*pmodParams.tScales[scaleIndex];        
                        }
                        else
                            tScalesMod[i] = pmodParams.tScales[scaleIndex];
                    }
                    else
                        tScalesMod[i] = pmodParams.tScales[scaleIndex];
                    //
                }
                //
            }
            else
                Arrays.fill(tScalesMod, pmodParams.tScales[0]);

            if (pmodParams.pScalesTimes!=null)
            {
                if (pmodParams.pScales.length!=pmodParams.pScalesTimes.length)
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
                    scaleIndex = MathUtils.findClosest(pmodParams.pScalesTimes, allScalesTimes[i]);
                    if (allScalesTimes[i]>pmodParams.pScalesTimes[scaleIndex])
                    {
                        if (scaleIndex<pmodParams.pScalesTimes.length-1)
                        {
                            if ((pmodParams.pScalesTimes[scaleIndex+1]-pmodParams.pScalesTimes[scaleIndex])>1e-10)
                                alpha = (pmodParams.pScalesTimes[scaleIndex+1]-allScalesTimes[i])/(pmodParams.pScalesTimes[scaleIndex+1]-pmodParams.pScalesTimes[scaleIndex]);
                            else
                                alpha = 0.5f;

                            pScalesMod[i] = alpha*pmodParams.pScales[scaleIndex] + (1.0f-alpha)*pmodParams.pScales[scaleIndex+1];
                        }
                        else
                            pScalesMod[i] = pmodParams.pScales[scaleIndex];
                    }
                    else if (allScalesTimes[i]<pmodParams.pScalesTimes[scaleIndex])
                    {
                        if (scaleIndex>0)
                        {
                            if ((pmodParams.pScalesTimes[scaleIndex]-pmodParams.pScalesTimes[scaleIndex-1])>1e-10)
                                alpha = (pmodParams.pScalesTimes[scaleIndex]-allScalesTimes[i])/(pmodParams.pScalesTimes[scaleIndex]-pmodParams.pScalesTimes[scaleIndex-1]);
                            else
                                alpha = 0.5f;

                            pScalesMod[i] = alpha*pmodParams.pScales[scaleIndex-1] + (1.0f-alpha)*pmodParams.pScales[scaleIndex];
                        }
                        else
                            pScalesMod[i] = pmodParams.pScales[scaleIndex];
                    }
                    else
                        pScalesMod[i] = pmodParams.pScales[scaleIndex];
                    //
                }
                //
            }
            else
                Arrays.fill(pScalesMod, pmodParams.pScales[0]);

            //Handle transient part by time shifting segments as necessary
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

            //Time scaling
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

            hntmSignalMod.originalDurationInSeconds = hntmSignalMod.frames[hntmSignalMod.frames.length-1].tAnalysisInSeconds;
            
            //Re-calculate phases considering the time shifts due to PSOLA-like repeatings/skipping of frame parameters
            if (HntmSynthesizer.ADJUST_PHASES_AFTER_TIME_SCALING)
            {
                float deltaPhase;
                for (i=0; i<hntmSignalMod.frames.length; i++)
                {
                    if (hntmSignalMod.frames[i].h.phases!=null)
                    {  
                        //MaryUtils.plot(hntmSignalMod.frames[i].h.phases);

                        for (j=0; j<hntmSignalMod.frames[i].h.phases.length; j++)
                        {
                            if (HntmAnalyzer.INCLUDE_ZEROTH_HARMONIC)
                                currentHarmonicNo = j;
                            else
                                currentHarmonicNo = j+1;
                            deltaPhase = (float)MathUtils.TWOPI*hntmSignalMod.frames[i].f0InHz*(synthesisInstants.analysisInstantsInSeconds[i]-synthesisInstants.synthesisInstantsInSeconds[i]);
                            hntmSignalMod.frames[i].h.phases[j] -= deltaPhase;
                        }

                        //MaryUtils.plot(hntmSignalMod.frames[i].h.phases);
                    }
                }
            }
            //
            
            //Phase envelope estimation and unwrapping to ensure phase continuity in frequency domain
            if (HntmAnalyzer.UNWRAP_PHASES_ALONG_HARMONICS_AFTER_TIME_SCALING)
                HntmAnalyzer.unwrapPhasesAlongHarmonics(hntmSignalMod);
            //
            
            //

            //float[] tSynthesis = new float[hntmSignalMod.frames.length];
            //for (i=0; i<hntmSignalMod.frames.length; i++)
            //    tSynthesis[i] = hntmSignalMod.frames[i].tAnalysisInSeconds;
            //MaryUtils.plot(tAnalysis);
            //MaryUtils.plot(tSynthesis);

            pmodParams = new BasicProsodyModifierParams(tScalesMod, allScalesTimes, pScalesMod, allScalesTimes);

            //Pitch scale modification
            if (pmodParams.pScales!=null)
            {            
                float pScale;
                int pScaleInd;
                boolean isVoiced;
                int newTotalHarmonics;
                float[] newPhases;
                float harmonicEnergyOrig;
                float harmonicEnergyMod;
                int k;
                double amp;
                for (i=0; i<hntmSignalMod.frames.length; i++)
                {
                    isVoiced = false;
                    if (hntmSignalMod.frames[i].h!=null && hntmSignalMod.frames[i].h.phases!=null && hntmSignalMod.frames[i].h.phases.length>0)
                        isVoiced = true;

                    if (isVoiced)
                    {
                        pScaleInd = MathUtils.findClosest(allScalesTimes, hntmSignalMod.frames[i].tAnalysisInSeconds);
                        pScale = pScalesMod[pScaleInd];

                        if (HntmAnalyzer.INCLUDE_ZEROTH_HARMONIC)
                            newTotalHarmonics = (int)Math.floor((hntmSignalMod.frames[i].h.phases.length-1)/pScale+0.5)+1;
                        else
                            newTotalHarmonics = (int)Math.floor(hntmSignalMod.frames[i].h.phases.length/pScale+0.5);
                        
                        if (newTotalHarmonics>0)
                        { 
                            harmonicEnergyOrig = 0.0f;
                            for (k=0; k<hntmSignalMod.frames[i].h.phases.length; k++)
                            {
                                if (HntmAnalyzer.INCLUDE_ZEROTH_HARMONIC)
                                    currentHarmonicNo = k;
                                else
                                    currentHarmonicNo = (k+1);

                                if (!HntmAnalyzer.USE_AMPLITUDES_DIRECTLY)
                                    amp = RegularizedCepstralEnvelopeEstimator.cepstrum2linearSpectrumValue(hntmSignalMod.frames[i].h.ceps, currentHarmonicNo*hntmSignalMod.frames[i].f0InHz, hntmSignalMod.samplingRateInHz);
                                else
                                    amp = hntmSignalMod.frames[i].h.ceps[k];
                                
                                harmonicEnergyOrig += amp*amp;
                            }

                            //1. Resample phase envelopes
                            newPhases = MathUtils.interpolate(hntmSignalMod.frames[i].h.phases, newTotalHarmonics);
                            //MaryUtils.plot(hntmSignalMod.frames[i].h.phases);
                            //MaryUtils.plot(newPhases);

                            //2. Change number of harmonics (only harmonics up to the original frequency of voicing will be kept)
                            hntmSignalMod.frames[i].h.phases = ArrayUtils.copy(newPhases);
                            //3. Scale f0
                            hntmSignalMod.frames[i].f0InHz *= pScale; 
                            
                            double[] linearAmps = new double[newTotalHarmonics];
                            double[] freqsInHz = new double [newTotalHarmonics];
                            harmonicEnergyMod = 0.0f;
                            for (k=0; k<newTotalHarmonics; k++)
                            {
                                if (HntmAnalyzer.INCLUDE_ZEROTH_HARMONIC)
                                    currentHarmonicNo = k;
                                else
                                    currentHarmonicNo = (k+1);

                                if (!HntmAnalyzer.USE_AMPLITUDES_DIRECTLY)
                                    amp = RegularizedCepstralEnvelopeEstimator.cepstrum2linearSpectrumValue(hntmSignalMod.frames[i].h.ceps, currentHarmonicNo*hntmSignalMod.frames[i].f0InHz, hntmSignalMod.samplingRateInHz);
                                else
                                {
                                    int closestHarmonicInd = (int)Math.floor(k/pScale+0.5);
                                    closestHarmonicInd = MathUtils.CheckLimits(closestHarmonicInd, 0, hntmSignalMod.frames[i].h.ceps.length-1);
                                    amp = hntmSignalMod.frames[i].h.ceps[closestHarmonicInd];
                                }
                                
                                harmonicEnergyMod += amp*amp;
                                linearAmps[k] = amp; //Not energy scaled yet
                                freqsInHz[k] = currentHarmonicNo*hntmSignalMod.frames[i].f0InHz;
                            }
                            
                            //Now, estimate modified cepstral envelope
                            for (j=0; j<newTotalHarmonics; j++)
                                linearAmps[j] *= Math.sqrt(harmonicEnergyOrig)/(1e-20+Math.sqrt(harmonicEnergyMod));

                            if (!HntmAnalyzer.USE_AMPLITUDES_DIRECTLY)
                                hntmSignalMod.frames[i].h.ceps = RegularizedCepstralEnvelopeEstimator.freqsLinearAmps2cepstrum(linearAmps, freqsInHz, hntmSignalMod.samplingRateInHz, hntmSignalMod.frames[i].h.ceps.length);
                            else
                            {
                                hntmSignalMod.frames[i].h.ceps = new double[newTotalHarmonics];
                                for (j=0; j<newTotalHarmonics; j++)
                                    hntmSignalMod.frames[i].h.ceps[j] = linearAmps[j];
                            }
                            //
                        }
                        else
                            hntmSignalMod.frames[i].h.phases = null;
                    }
                }
                // 

                //Re-calculate phases considering the time shifts due to PSOLA-like repeatings/skipping of frame parameters
                if (HntmSynthesizer.ADJUST_PHASES_AFTER_PITCH_SCALING)
                {
                    float deltaPhase;
                    for (i=0; i<hntmSignalMod.frames.length; i++)
                    {
                        pScaleInd = MathUtils.findClosest(allScalesTimes, hntmSignalMod.frames[i].tAnalysisInSeconds);
                        pScale = pScalesMod[pScaleInd];

                        if (hntmSignalMod.frames[i].h.phases!=null)
                        {  
                            //MaryUtils.plot(hntmSignalMod.frames[i].h.phases);

                            for (j=0; j<hntmSignalMod.frames[i].h.phases.length; j++)
                            {
                                if (HntmAnalyzer.INCLUDE_ZEROTH_HARMONIC)
                                    currentHarmonicNo = j;
                                else
                                    currentHarmonicNo = j+1;
                                deltaPhase = (float)MathUtils.TWOPI*hntmSignalMod.frames[i].f0InHz*(synthesisInstants.analysisInstantsInSeconds[i]-synthesisInstants.synthesisInstantsInSeconds[i]);
                                hntmSignalMod.frames[i].h.phases[j] -= deltaPhase;
                            }

                            //MaryUtils.plot(hntmSignalMod.frames[i].h.phases);
                        }
                    }
                }
                //

                //Phase envelope estimation and unwrapping to ensure phase continuity in frequency domain
                if (HntmAnalyzer.UNWRAP_PHASES_ALONG_HARMONICS_AFTER_PITCH_SCALING)
                    HntmAnalyzer.unwrapPhasesAlongHarmonics(hntmSignalMod);
                //
            }

            return hntmSignalMod;
        }
    }
}
