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
import marytts.signalproc.analysis.RegularizedCepstrumEstimator;
import marytts.signalproc.analysis.RegularizedPostWarpedCepstrumEstimator;
import marytts.signalproc.analysis.RegularizedPreWarpedCepstrumEstimator;
import marytts.signalproc.process.TDPSOLAInstants;
import marytts.signalproc.process.TDPSOLAProcessor;
import marytts.signalproc.sinusoidal.hntm.analysis.HntmAnalyzer;
import marytts.signalproc.sinusoidal.hntm.analysis.HntmAnalyzerNoisePartWaveformSynthesizer;
import marytts.signalproc.sinusoidal.hntm.analysis.HntmAnalyzerParams;
import marytts.signalproc.sinusoidal.hntm.analysis.HntmPlusTransientsSpeechSignal;
import marytts.signalproc.sinusoidal.hntm.analysis.HntmSpeechFrame;
import marytts.signalproc.sinusoidal.hntm.analysis.HntmSpeechSignal;
import marytts.signalproc.sinusoidal.hntm.analysis.TransientSegment;
import marytts.signalproc.sinusoidal.hntm.synthesis.HntmSynthesizedSignal;
import marytts.signalproc.sinusoidal.hntm.synthesis.HntmSynthesizer;
import marytts.signalproc.sinusoidal.hntm.synthesis.NoisePartWaveformSynthesizer;
import marytts.signalproc.window.Window;
import marytts.util.MaryUtils;
import marytts.util.math.ArrayUtils;
import marytts.util.math.ComplexNumber;
import marytts.util.math.MathUtils;
import marytts.util.signal.SignalProcUtils;

/**
 * @author oytun.turk
 *
 */
public class HntmProsodyModifier {
    
    //Note that pmodParams are changed as well
    public static HntmSpeechSignal modify(HntmSpeechSignal hntmSignal, 
                                          BasicProsodyModifierParams pmodParams, 
                                          HntmAnalyzerParams analysisParams)
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
                                                                   numTransientSegments);

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
                hntmSignalMod = new HntmSpeechSignal(hntmSignal.frames.length, hntmSignal.samplingRateInHz, hntmSignal.originalDurationInSeconds);
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
            
            //Time scale noise part if it is based on any waveform representation
            if (analysisParams.noiseModel==HntmAnalyzerParams.WAVEFORM ||
                analysisParams.noiseModel==HntmAnalyzerParams.VOICEDNOISE_LPC_UNVOICEDNOISE_WAVEFORM ||
                analysisParams.noiseModel==HntmAnalyzerParams.UNVOICEDNOISE_LPC_VOICEDNOISE_WAVEFORM)
            {   
                //Synthesize original noise waveform
                double[] noisePartWaveform = NoisePartWaveformSynthesizer.synthesize(hntmSignal);
                
                //Time scale noise waveform using TD-PSOLA
                int noiseWaveformLenMod = SignalProcUtils.time2sample(hntmSignalMod.originalDurationInSeconds, hntmSignalMod.samplingRateInHz);
                double[] noisePartWaveformMod = new double[noiseWaveformLenMod];
                double[] winWgtSum = new double[noiseWaveformLenMod];
                Arrays.fill(noisePartWaveformMod, 0.0);
                Arrays.fill(winWgtSum, 0.0);
                Window winNoise = null;
                double[] wgt = null;
                int wsNoise, halfWsNoise;
                int analysisStartInd, analysisEndInd;
                int synthesisMidInd, synthesisStartInd, synthesisEndInd;
                currentSynthesisIndex = 0;
                bBroke = false;                

                int k, kStart;
                boolean invert = false;
                for (i=1; i<synthesisInstants.repeatSkipCounts.length-1; i++) //TO DO: handle first and last noise wwaform frames
                {
                    for (j=0; j<=synthesisInstants.repeatSkipCounts[i]; j++)
                    {
                        analysisStartInd = SignalProcUtils.time2sample(hntmSignal.frames[i-1].tAnalysisInSeconds, hntmSignal.samplingRateInHz);
                        if (i<hntmSignal.frames.length-1)
                            analysisEndInd = SignalProcUtils.time2sample(hntmSignal.frames[i+1].tAnalysisInSeconds, hntmSignal.samplingRateInHz);
                        else
                            analysisEndInd = noisePartWaveform.length-1;

                        wsNoise = analysisEndInd-analysisStartInd+1;
                        if (wsNoise>0)
                        {
                            halfWsNoise = (int)Math.floor(0.5*wsNoise+0.5);

                            winNoise = Window.get(analysisParams.harmonicAnalysisWindowType, wsNoise);
                            wgt = winNoise.getCoeffs(); 

                            synthesisMidInd = SignalProcUtils.time2sample(hntmSignalMod.frames[currentSynthesisIndex].tAnalysisInSeconds, hntmSignalMod.samplingRateInHz);
                            synthesisStartInd = synthesisMidInd - halfWsNoise;
                            synthesisEndInd = synthesisStartInd + wsNoise - 1;

                            kStart = Math.max(0, synthesisStartInd);
                            
                            if (!invert)
                            {
                                for (k=kStart; k<=Math.min(synthesisEndInd, noiseWaveformLenMod-1); k++)
                                {
                                    noisePartWaveformMod[k] += noisePartWaveform[k-kStart+analysisStartInd]*wgt[k-kStart];
                                    winWgtSum[k] += wgt[k-kStart];
                                }
                            }
                            else
                            {
                                for (k=kStart; k<=Math.min(synthesisEndInd, noiseWaveformLenMod-1); k++)
                                {
                                    noisePartWaveformMod[k] += noisePartWaveform[analysisEndInd-(k-kStart)]*wgt[k-kStart];
                                    winWgtSum[k] += wgt[k-kStart];
                                }
                            }
                        }
                        
                        currentSynthesisIndex++;
                        invert = !invert;

                        if (currentSynthesisIndex>=hntmSignalMod.frames.length)
                        {
                            bBroke = true;
                            break;
                        }
                    }

                    if (bBroke)
                        break;
                }
                
                for (i=0; i<winWgtSum.length; i++)
                {
                    if (winWgtSum[i]>0.0)
                        noisePartWaveformMod[i] /= winWgtSum[i];
                }
                
                HntmAnalyzer.packNoisePartWaveforms(hntmSignalMod, noisePartWaveformMod);
            }
            //
            
            
            //NOT EFFECTIVE SINCE we do not use the newPhases!
            //Synthesis uses complexAmps only!
            //Phase envelope estimation and unwrapping to ensure phase continuity in frequency domain
            double[][] modifiedPhases = null;
            if (HntmAnalyzerParams.UNWRAP_PHASES_ALONG_HARMONICS_AFTER_TIME_SCALING)
                modifiedPhases = HntmAnalyzer.unwrapPhasesAlongHarmonics(hntmSignalMod);
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
                int leftHarmonicInd, rightHarmonicInd;
                float[] currentCeps = null;
                for (i=0; i<hntmSignalMod.frames.length; i++)
                {
                    isVoiced = false;
                    if (hntmSignalMod.frames[i].h!=null && hntmSignalMod.frames[i].h.complexAmps!=null && hntmSignalMod.frames[i].h.complexAmps.length>0)
                        isVoiced = true;

                    if (isVoiced)
                    {
                        if (!analysisParams.useHarmonicAmplitudesDirectly)
                            currentCeps = hntmSignalMod.frames[i].h.getCeps(hntmSignalMod.frames[i].f0InHz, hntmSignalMod.samplingRateInHz, analysisParams);
                        
                        pScaleInd = MathUtils.findClosest(allScalesTimes, hntmSignalMod.frames[i].tAnalysisInSeconds);
                        pScale = pScalesMod[pScaleInd];

                        newTotalHarmonics = (int)Math.floor(hntmSignalMod.frames[i].h.complexAmps.length/pScale+0.5);
                        
                        if (newTotalHarmonics>0)
                        { 
                            harmonicEnergyOrig = 0.0f;
                            double[] amps = new double[hntmSignalMod.frames[i].h.complexAmps.length];
                            
                            for (k=0; k<hntmSignalMod.frames[i].h.complexAmps.length; k++)
                            {
                                currentHarmonicNo = (k+1);

                                if (!analysisParams.useHarmonicAmplitudesDirectly)
                                {
                                    if (analysisParams.regularizedCepstrumWarpingMethod == RegularizedCepstrumEstimator.REGULARIZED_CEPSTRUM_WITH_PRE_BARK_WARPING)
                                        amps[k] = RegularizedPreWarpedCepstrumEstimator.cepstrum2linearSpectrumValue(currentCeps, currentHarmonicNo*hntmSignalMod.frames[i].f0InHz, hntmSignalMod.samplingRateInHz);
                                    else if (analysisParams.regularizedCepstrumWarpingMethod == RegularizedCepstrumEstimator.REGULARIZED_CEPSTRUM_WITH_POST_MEL_WARPING)
                                        amps[k] = RegularizedPostWarpedCepstrumEstimator.cepstrum2linearSpectrumValue(currentCeps, currentHarmonicNo*hntmSignalMod.frames[i].f0InHz, hntmSignalMod.samplingRateInHz);
                                }
                                else
                                {
                                    //Linear interpolation using neighbouring harmonic amplitudes
                                    leftHarmonicInd = (int)Math.floor(currentHarmonicNo*pScale)-1;
                                    if (leftHarmonicInd<0)
                                        amps[k] = MathUtils.magnitudeComplex(hntmSignalMod.frames[i].h.complexAmps[0]);
                                    else
                                    {
                                        rightHarmonicInd = leftHarmonicInd+1;
                                        if (rightHarmonicInd>hntmSignalMod.frames[i].h.complexAmps.length-1)
                                            amps[k] = MathUtils.magnitudeComplex(hntmSignalMod.frames[i].h.complexAmps[hntmSignalMod.frames[i].h.complexAmps.length-1]);
                                        else
                                            amps[k] = MathUtils.interpolatedSample((leftHarmonicInd+1)*hntmSignalMod.frames[i].f0InHz, 
                                                                                   currentHarmonicNo*pScale*hntmSignalMod.frames[i].f0InHz, 
                                                                                   (rightHarmonicInd+1)*hntmSignalMod.frames[i].f0InHz, 
                                                                                   MathUtils.magnitudeComplex(hntmSignalMod.frames[i].h.complexAmps[leftHarmonicInd]), 
                                                                                   MathUtils.magnitudeComplex(hntmSignalMod.frames[i].h.complexAmps[rightHarmonicInd]));
                                    }
                                }
                                
                                harmonicEnergyOrig += amps[k]*amps[k];
                            }

                            //1. Resample complex amplitude envelopes
                            hntmSignalMod.frames[i].h.complexAmps = MathUtils.interpolate(hntmSignalMod.frames[i].h.complexAmps, newTotalHarmonics);

                            //2. Scale f0
                            hntmSignalMod.frames[i].f0InHz *= pScale; 
                            
                            double[] linearAmps = new double[newTotalHarmonics];
                            double[] freqsInHz = new double [newTotalHarmonics];
                            harmonicEnergyMod = 0.0f;
                            double[] ampsMod = new double[newTotalHarmonics];
                            
                            if (!analysisParams.useHarmonicAmplitudesDirectly)
                                currentCeps = hntmSignalMod.frames[i].h.getCeps(hntmSignalMod.frames[i].f0InHz, hntmSignalMod.samplingRateInHz, analysisParams);
                            
                            for (k=0; k<newTotalHarmonics; k++)
                            {
                                currentHarmonicNo = (k+1);

                                if (!analysisParams.useHarmonicAmplitudesDirectly)
                                {
                                    if (analysisParams.regularizedCepstrumWarpingMethod == RegularizedPreWarpedCepstrumEstimator.REGULARIZED_CEPSTRUM_WITH_PRE_BARK_WARPING)
                                        ampsMod[k] = RegularizedPreWarpedCepstrumEstimator.cepstrum2linearSpectrumValue(currentCeps, currentHarmonicNo*hntmSignalMod.frames[i].f0InHz, hntmSignalMod.samplingRateInHz);
                                    else if (analysisParams.regularizedCepstrumWarpingMethod == RegularizedPostWarpedCepstrumEstimator.REGULARIZED_CEPSTRUM_WITH_POST_MEL_WARPING)
                                        ampsMod[k] = RegularizedPostWarpedCepstrumEstimator.cepstrum2linearSpectrumValue(currentCeps, currentHarmonicNo*hntmSignalMod.frames[i].f0InHz, hntmSignalMod.samplingRateInHz);
                                }
                                else
                                {
                                    //Linear interpolation using neighbouring harmonic amplitudes
                                    leftHarmonicInd = (int)Math.floor(currentHarmonicNo*pScale)-1;
                                    if (leftHarmonicInd<0)
                                        ampsMod[k] = MathUtils.magnitudeComplex(hntmSignalMod.frames[i].h.complexAmps[0]);
                                    else
                                    {
                                        rightHarmonicInd = leftHarmonicInd+1;
                                        if (rightHarmonicInd>hntmSignalMod.frames[i].h.complexAmps.length-1)
                                            ampsMod[k] = MathUtils.magnitudeComplex(hntmSignalMod.frames[i].h.complexAmps[hntmSignalMod.frames[i].h.complexAmps.length-1]);
                                        else
                                            ampsMod[k] = MathUtils.interpolatedSample((leftHarmonicInd+1)*hntmSignalMod.frames[i].f0InHz, 
                                                                                      currentHarmonicNo*pScale*hntmSignalMod.frames[i].f0InHz, 
                                                                                      (rightHarmonicInd+1)*hntmSignalMod.frames[i].f0InHz, 
                                                                                      MathUtils.magnitudeComplex(hntmSignalMod.frames[i].h.complexAmps[leftHarmonicInd]), 
                                                                                      MathUtils.magnitudeComplex(hntmSignalMod.frames[i].h.complexAmps[rightHarmonicInd]));
                                    }
                                }
                                
                                harmonicEnergyMod += ampsMod[k]*ampsMod[k];
                                linearAmps[k] = ampsMod[k]; //Not energy scaled yet
                                freqsInHz[k] = currentHarmonicNo*hntmSignalMod.frames[i].f0InHz;
                            }
                            
                            //double[] vocalTractDBOrig = RegularizedPreWarpedCepstrumEstimator.cepstrum2dbSpectrumValues(hntmSignalMod.frames[i].h.ceps, SignalProcUtils.halfSpectrumSize(4096)-1, hntmSignalMod.samplingRateInHz);
                            //MaryUtils.plot(vocalTractDBOrig);
 
                            //double[] vocalTractDBMod = RegularizedPreWarpedCepstrumEstimator.cepstrum2dbSpectrumValues(hntmSignalMod.frames[i].h.ceps, SignalProcUtils.halfSpectrumSize(4096)-1, hntmSignalMod.samplingRateInHz);
                            //MaryUtils.plot(vocalTractDBMod);
                        }
                        else
                            hntmSignalMod.frames[i].h.complexAmps = null;
                    }
                }
                // 

                //Phase envelope estimation and unwrapping to ensure phase continuity in frequency domain
                if (HntmAnalyzerParams.UNWRAP_PHASES_ALONG_HARMONICS_AFTER_PITCH_SCALING)
                    HntmAnalyzer.unwrapPhasesAlongHarmonics(hntmSignalMod);
                //
            }

            return hntmSignalMod;
        }
    }
}
