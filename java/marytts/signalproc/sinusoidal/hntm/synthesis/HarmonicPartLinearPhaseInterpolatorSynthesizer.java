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

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import marytts.signalproc.analysis.LpcAnalyser;
import marytts.signalproc.analysis.RegularizedCepstrumEstimator;
import marytts.signalproc.analysis.RegularizedPostWarpedCepstrumEstimator;
import marytts.signalproc.analysis.RegularizedPreWarpedCepstrumEstimator;
import marytts.signalproc.sinusoidal.PitchSynchronousSinusoidalAnalyzer;
import marytts.signalproc.sinusoidal.hntm.analysis.HntmAnalyzer;
import marytts.signalproc.sinusoidal.hntm.analysis.HntmAnalyzerParams;
import marytts.signalproc.sinusoidal.hntm.analysis.HntmSpeechSignal;
import marytts.signalproc.window.Window;
import marytts.util.data.BufferedDoubleDataSource;
import marytts.util.data.audio.DDSAudioInputStream;
import marytts.util.io.FileUtils;
import marytts.util.math.MathUtils;
import marytts.util.signal.SignalProcUtils;
import marytts.util.string.StringUtils;
import marytts.util.MaryUtils;

/**
 * @author oytun.turk
 *
 */
public class HarmonicPartLinearPhaseInterpolatorSynthesizer {
    
    public static double[] synthesize(HntmSpeechSignal hnmSignal, HntmAnalyzerParams analysisParams, HntmSynthesizerParams synthesisParams)
    {
        return synthesize(hnmSignal, analysisParams, synthesisParams, null);
    }
    
    public static double[] synthesize(HntmSpeechSignal hnmSignal, HntmAnalyzerParams analysisParams, HntmSynthesizerParams synthesisParams, String referenceFile)
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
            if (hnmSignal.frames[i].maximumFrequencyOfVoicingInHz>0.0f && hnmSignal.frames[i].h!=null && hnmSignal.frames[i].h.complexAmps!=null)
            {
                numHarmonicsCurrentFrame = hnmSignal.frames[i].h.complexAmps.length;
                if (numHarmonicsCurrentFrame>maxNumHarmonics)
                    maxNumHarmonics = numHarmonicsCurrentFrame;
            }  
        }

        double aksi;
        double aksiPlusOne;
        
        double phaseki;
        double phasekiPlusOne;

        float f0InHz, f0InHzNext;
        float f0Average, f0AverageNext;
        double ht;
        double phasekt = 0.0;

        double phasekiEstimate = 0.0;
        double phasekiPlusOneEstimate = 0.0;
        int Mk;
        boolean isPrevVoiced, isVoiced, isNextVoiced;
        boolean isTrackVoiced, isNextTrackVoiced, isPrevTrackVoiced;
        int outputLen = SignalProcUtils.time2sample(hnmSignal.originalDurationInSeconds, hnmSignal.samplingRateInHz);
        
        harmonicPart = new double[outputLen]; //In fact, this should be prosody scaled length when you implement prosody modifications
        Arrays.fill(harmonicPart, 0.0);
        
        //Write separate tracks to output
        double[][] harmonicTracks = null;
        double[][] winOverlapWgt = null;

        if (maxNumHarmonics>0)
        {
            harmonicTracks = new double[maxNumHarmonics][];
            winOverlapWgt = new double[maxNumHarmonics][];
            for (k=0; k<maxNumHarmonics; k++)
            {
                harmonicTracks[k] = new double[outputLen];
                Arrays.fill(harmonicTracks[k], 0.0);
                
                if (synthesisParams.overlappingHarmonicPartSynthesis)
                {
                    winOverlapWgt[k] = new double[outputLen];
                    Arrays.fill(winOverlapWgt[k], 0.0);
                }
            }
        }
        //
        
        int transitionLen = SignalProcUtils.time2sample(synthesisParams.unvoicedVoicedTrackTransitionInSeconds, hnmSignal.samplingRateInHz);
        Window transitionWin = Window.get(Window.HAMMING, transitionLen*2);
        transitionWin.normalizePeakValue(1.0f);
        double[] halfTransitionWinLeft = transitionWin.getCoeffsLeftHalf();
        double[] halfTransitionWinRight = transitionWin.getCoeffsRightHalf();
        
        int currentHarmonicNo;

        float[] currentCeps = null;
        float[] nextCeps = null;
        
        double currentOverlapWinWgt;
        
        for (i=0; i<hnmSignal.frames.length; i++)
        {
            isPrevVoiced = false;
            isVoiced = false;
            isNextVoiced = false;
            
            if (i>0 && hnmSignal.frames[i-1].h!=null && hnmSignal.frames[i-1].h.complexAmps!=null && hnmSignal.frames[i-1].h.complexAmps.length>0)
                isPrevVoiced =  true;
            
            if (hnmSignal.frames[i].h!=null && hnmSignal.frames[i].h.complexAmps!=null && hnmSignal.frames[i].h.complexAmps.length>0)
                isVoiced = true;

            if (i<hnmSignal.frames.length-1 && hnmSignal.frames[i+1].h!=null && hnmSignal.frames[i+1].h.complexAmps!=null && hnmSignal.frames[i+1].h.complexAmps.length>0)
                isNextVoiced = true;
            
            if (isVoiced)
                numHarmonicsCurrentFrame = hnmSignal.frames[i].h.complexAmps.length;
            else if (!isVoiced && isNextVoiced)
                numHarmonicsCurrentFrame = hnmSignal.frames[i+1].h.complexAmps.length;
            else
                numHarmonicsCurrentFrame = 0;
            
            f0InHz = hnmSignal.frames[i].f0InHz;
            
            if (isNextVoiced)
                f0InHzNext = hnmSignal.frames[i+1].f0InHz;
            else
                f0InHzNext = f0InHz;

            f0Average = 0.5f*(f0InHz+f0InHzNext);
            
            if (!analysisParams.useHarmonicAmplitudesDirectly)
            {
                currentCeps = hnmSignal.frames[i].h.getCeps(f0InHz, hnmSignal.samplingRateInHz, analysisParams);
                if (i+1<hnmSignal.frames.length)
                    nextCeps = hnmSignal.frames[i+1].h.getCeps(f0InHzNext, hnmSignal.samplingRateInHz, analysisParams);
                else
                    nextCeps = null;
            }
            
            for (k=0; k<numHarmonicsCurrentFrame; k++)
            {
                currentHarmonicNo = k+1;
                
                aksi = 0.0;
                aksiPlusOne = 0.0;
                
                phaseki = 0.0f;
                phasekiPlusOne = 0.0f;
                
                isPrevTrackVoiced = false;
                isTrackVoiced = false;
                isNextTrackVoiced = false;
                
                if (i>0 && hnmSignal.frames[i-1].h!=null && hnmSignal.frames[i-1].h.complexAmps!=null && hnmSignal.frames[i-1].h.complexAmps.length>k)
                    isPrevTrackVoiced = true;
                
                if (hnmSignal.frames[i].h!=null && hnmSignal.frames[i].h.complexAmps!=null && hnmSignal.frames[i].h.complexAmps.length>k)
                    isTrackVoiced = true;

                if (i<hnmSignal.frames.length-1 && hnmSignal.frames[i+1].h!=null && hnmSignal.frames[i+1].h.complexAmps!=null && hnmSignal.frames[i+1].h.complexAmps.length>k)
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
                
                if (synthesisParams.overlappingHarmonicPartSynthesis)
                {
                    trackStartInSeconds -= synthesisParams.harmonicSynthesisOverlapInSeconds;
                    trackEndInSeconds += synthesisParams.harmonicSynthesisOverlapInSeconds;
                }

                trackStartIndex = SignalProcUtils.time2sample(trackStartInSeconds, hnmSignal.samplingRateInHz);
                trackEndIndex = SignalProcUtils.time2sample(trackEndInSeconds, hnmSignal.samplingRateInHz);

                if (!synthesisParams.overlappingHarmonicPartSynthesis)
                {
                    if (!isPrevTrackVoiced)
                        trackStartIndex -= transitionLen;
                    if (!isNextTrackVoiced)
                        trackEndIndex += transitionLen;
                }
                
                Window overlapWin = null;
                double[] overlapWinWgt = null;
                if (synthesisParams.overlappingHarmonicPartSynthesis)
                {
                    overlapWin = Window.get(Window.HAMMING, trackEndIndex-trackStartIndex+1);
                    overlapWin.normalizePeakValue(1.0f);
                    overlapWinWgt = overlapWin.getCoeffs();
                }
                
                if (isTrackVoiced && trackEndIndex-trackStartIndex+1>0)
                {
                    //Amplitudes                       
                    if (isTrackVoiced)
                    {
                        if (!analysisParams.useHarmonicAmplitudesDirectly)
                        {
                            if (analysisParams.regularizedCepstrumWarpingMethod == RegularizedCepstrumEstimator.REGULARIZED_CEPSTRUM_WITH_PRE_BARK_WARPING)
                                aksi = RegularizedPreWarpedCepstrumEstimator.cepstrum2linearSpectrumValue(currentCeps, currentHarmonicNo*f0InHz, hnmSignal.samplingRateInHz);   
                            else if (analysisParams.regularizedCepstrumWarpingMethod == RegularizedCepstrumEstimator.REGULARIZED_CEPSTRUM_WITH_POST_MEL_WARPING)
                                aksi = RegularizedPostWarpedCepstrumEstimator.cepstrum2linearSpectrumValue(currentCeps, currentHarmonicNo*f0InHz, hnmSignal.samplingRateInHz);   
                        }
                        else
                        {
                            if (k<hnmSignal.frames[i].h.complexAmps.length)
                                aksi = MathUtils.magnitudeComplex(hnmSignal.frames[i].h.complexAmps[k]); //Use amplitudes directly without cepstrum method
                        }
                    }
                    else
                        aksi = 0.0;
                    
                    if (isNextTrackVoiced)
                    {
                        if (!analysisParams.useHarmonicAmplitudesDirectly)
                        {
                            if (analysisParams.regularizedCepstrumWarpingMethod == RegularizedCepstrumEstimator.REGULARIZED_CEPSTRUM_WITH_PRE_BARK_WARPING)  
                                aksiPlusOne = RegularizedPreWarpedCepstrumEstimator.cepstrum2linearSpectrumValue(nextCeps, currentHarmonicNo*f0InHzNext, hnmSignal.samplingRateInHz);
                            else if (analysisParams.regularizedCepstrumWarpingMethod == RegularizedCepstrumEstimator.REGULARIZED_CEPSTRUM_WITH_POST_MEL_WARPING)  
                                aksiPlusOne = RegularizedPostWarpedCepstrumEstimator.cepstrum2linearSpectrumValue(nextCeps, currentHarmonicNo*f0InHzNext, hnmSignal.samplingRateInHz);
                        }
                        else
                        {
                            if (k<hnmSignal.frames[i+1].h.complexAmps.length)
                                aksiPlusOne = MathUtils.magnitudeComplex(hnmSignal.frames[i+1].h.complexAmps[k]); //Use amplitudes directly without cepstrum method
                        }
                    }
                    else
                        aksiPlusOne = 0.0;
                    //

                    //Phases
                    if (isTrackVoiced)
                    {
                        if (currentHarmonicNo==0)
                            phaseki = 0.0f;
                        else
                            phaseki = MathUtils.phaseInRadians(hnmSignal.frames[i].h.complexAmps[k]);
                    }
                    if (isNextTrackVoiced)
                    {
                        if (currentHarmonicNo==0)
                            phasekiPlusOne = 0.0f;
                        else
                            phasekiPlusOne = MathUtils.phaseInRadians(hnmSignal.frames[i+1].h.complexAmps[k]);
                    }    
                    
                    //phaseki += MathUtils.degrees2radian(-4.0);
                    //phasekiPlusOne += MathUtils.degrees2radian(-4.0);
                    
                    if (!isTrackVoiced && isNextTrackVoiced)   
                    {
                        phaseki = (float)( phasekiPlusOne - currentHarmonicNo*MathUtils.TWOPI*f0InHzNext*(tsikPlusOne-tsik)); //Equation (3.54)
                        aksi = 0.0;
                    }
                    else if (isTrackVoiced && !isNextTrackVoiced)
                    {
                        phasekiPlusOne = phaseki + currentHarmonicNo*MathUtils.TWOPI*f0InHz*(tsikPlusOne-tsik); //Equation (3.55)
                        aksiPlusOne = 0.0;
                    }
                    
                    phasekiPlusOneEstimate = phaseki + currentHarmonicNo*MathUtils.TWOPI*f0Average*(tsikPlusOne-tsik);
                    //phasekiPlusOneEstimate = MathUtils.TWOPI*(Math.random()-0.5); //Random phase
                    
                    //System.out.println(String.valueOf(f0Average) + " - " + String.valueOf(f0InHz) + " - " + String.valueOf(f0InHzNext));
                    
                    Mk = (int)Math.floor((phasekiPlusOneEstimate-phasekiPlusOne)/MathUtils.TWOPI + 0.5);
                    //

                    for (n=Math.max(0, trackStartIndex); n<=Math.min(trackEndIndex, outputLen-1); n++)
                    {
                        t = SignalProcUtils.sample2time(n, hnmSignal.samplingRateInHz);
                        
                        //if (t>=tsik && t<tsikPlusOne)
                        {
                            //Amplitude estimate
                            if (t<tsik)
                                akt = MathUtils.interpolatedSample(tsik-synthesisParams.unvoicedVoicedTrackTransitionInSeconds, t, tsik, 0.0, aksi);
                            else if (t>tsikPlusOne)
                                akt = MathUtils.interpolatedSample(tsikPlusOne, t, tsikPlusOne+synthesisParams.unvoicedVoicedTrackTransitionInSeconds, aksiPlusOne, 0.0);
                            else
                                akt = MathUtils.interpolatedSample(tsik, t, tsikPlusOne, aksi, aksiPlusOne);
                            //

                            //Phase estimate
                            phasekt = phaseki + (phasekiPlusOne+MathUtils.TWOPI*Mk-phaseki)*(t-tsik)/(tsikPlusOne-tsik);
                            //
   
                            if (synthesisParams.overlappingHarmonicPartSynthesis)
                            {
                                currentOverlapWinWgt = overlapWinWgt[n-Math.max(0, trackStartIndex)];
                                winOverlapWgt[k][n] += currentOverlapWinWgt;
                            }
                            else
                                currentOverlapWinWgt = 1.0;
                            
                            if (!isPrevTrackVoiced && n-trackStartIndex<transitionLen)
                                harmonicTracks[k][n] = currentOverlapWinWgt*halfTransitionWinLeft[n-trackStartIndex]*akt*Math.cos(phasekt);
                            else if (!isNextTrackVoiced && trackEndIndex-n<transitionLen)
                                harmonicTracks[k][n] = currentOverlapWinWgt*halfTransitionWinRight[transitionLen-(trackEndIndex-n)-1]*akt*Math.cos(phasekt);
                            else
                                harmonicTracks[k][n] = currentOverlapWinWgt*akt*Math.cos(phasekt);
                        }
                    } 
                }
            }
        }
        
        if (harmonicTracks!=null)
        {
            if (!synthesisParams.overlappingHarmonicPartSynthesis)
            {
                for (k=0; k<harmonicTracks.length; k++)
                {
                    for (n=0; n<harmonicPart.length; n++)
                        harmonicPart[n] += harmonicTracks[k][n];
                }
            }
            else
            {
                for (k=0; k<harmonicTracks.length; k++)
                {
                    for (n=0; n<harmonicPart.length; n++)
                    {
                        if (winOverlapWgt[k][n]>0.0f)
                            harmonicPart[n] += harmonicTracks[k][n]/winOverlapWgt[k][n];
                        else
                            harmonicPart[n] += harmonicTracks[k][n];
                    }
                }
            }
            
            if (referenceFile!=null && FileUtils.exists(referenceFile) && synthesisParams.writeSeparateHarmonicTracksToOutputs)
            {
                //Write separate tracks to output
                AudioInputStream inputAudio = null;
                try {
                    inputAudio = AudioSystem.getAudioInputStream(new File(referenceFile));
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
                        harmonicTracks[k] = MathUtils.divide(harmonicTracks[k], 32767.0);

                        DDSAudioInputStream outputAudio = new DDSAudioInputStream(new BufferedDoubleDataSource(harmonicTracks[k]), inputAudio.getFormat());
                        String outFileName = StringUtils.getFolderName(referenceFile) + "harmonicTrack" + String.valueOf(k+1) + ".wav";
                        try {
                            AudioSystem.write(outputAudio, AudioFileFormat.Type.WAVE, new File(outFileName));
                        } catch (IOException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    }
                }
            }
            //
        }
        
        return harmonicPart;
    }
}
