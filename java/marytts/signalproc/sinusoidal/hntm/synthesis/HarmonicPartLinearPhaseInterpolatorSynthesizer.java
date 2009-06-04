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

import marytts.signalproc.analysis.RegularizedCepstralEnvelopeEstimator;
import marytts.signalproc.sinusoidal.hntm.analysis.HntmAnalyzer;
import marytts.signalproc.sinusoidal.hntm.analysis.HntmSpeechSignal;
import marytts.signalproc.window.Window;
import marytts.util.data.BufferedDoubleDataSource;
import marytts.util.data.audio.DDSAudioInputStream;
import marytts.util.io.FileUtils;
import marytts.util.math.MathUtils;
import marytts.util.signal.SignalProcUtils;
import marytts.util.MaryUtils;

/**
 * @author oytun.turk
 *
 */
public class HarmonicPartLinearPhaseInterpolatorSynthesizer {
    
    public static double[] synthesize(HntmSpeechSignal hnmSignal)
    {
        return synthesize(hnmSignal, null, null, null);
    }
    
    public static double[] synthesize(HntmSpeechSignal hnmSignal, float[] pScales, float[] pScalesTimes, String referenceFile)
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
            if (hnmSignal.frames[i].maximumFrequencyOfVoicingInHz>0.0f && hnmSignal.frames[i].h!=null && hnmSignal.frames[i].h.phases!=null)
            {
                numHarmonicsCurrentFrame = hnmSignal.frames[i].h.phases.length;
                if (numHarmonicsCurrentFrame>maxNumHarmonics)
                    maxNumHarmonics = numHarmonicsCurrentFrame;
            }  
        }

        double aksi;
        double aksiPlusOne;
        
        float phaseki;
        float phasekiPlusOne;

        float f0InHzPrev, f0InHz, f0InHzNext;
        float f0Average, f0AverageNext;
        f0InHzPrev = 0.0f;
        double ht;
        float phasekt = 0.0f;

        float phasekiEstimate = 0.0f;
        float phasekiPlusOneEstimate = 0.0f;
        int Mk;
        boolean isPrevVoiced, isVoiced, isNextVoiced;
        boolean isTrackVoiced, isNextTrackVoiced, isPrevTrackVoiced;
        int outputLen = SignalProcUtils.time2sample(hnmSignal.originalDurationInSeconds, hnmSignal.samplingRateInHz);
        
        harmonicPart = new double[outputLen]; //In fact, this should be prosody scaled length when you implement prosody modifications
        Arrays.fill(harmonicPart, 0.0);
        
        //Write separate tracks to output
        double[][] harmonicTracks = null;
        
        double[] dphasek = null;

        double[][] allSynthAmps = null;
        double[][] allSynthPhases = null;
        if (maxNumHarmonics>0)
        {
            harmonicTracks = new double[maxNumHarmonics][];
            for (k=0; k<maxNumHarmonics; k++)
            {
                harmonicTracks[k] = new double[outputLen];
                Arrays.fill(harmonicTracks[k], 0.0);
            }
            
            allSynthAmps = new double[maxNumHarmonics][hnmSignal.frames.length];
            allSynthPhases = new double[maxNumHarmonics][hnmSignal.frames.length];
        }
        //
        
        int transitionLen = SignalProcUtils.time2sample(HntmSynthesizer.UNVOICED_VOICED_TRACK_TRANSITION_IN_SECONDS, hnmSignal.samplingRateInHz);
        Window transitionWin = Window.get(Window.HANNING, transitionLen*2);
        transitionWin.normalizePeakValue(1.0f);
        double[] halfTransitionWinLeft = transitionWin.getCoeffsLeftHalf();
        double[] halfTransitionWinRight = transitionWin.getCoeffsRightHalf();
        
        int currentHarmonicNo;
        
        for (i=0; i<hnmSignal.frames.length; i++)
        {
            isPrevVoiced = false;
            isVoiced = false;
            isNextVoiced = false;
            
            if (i>0 && hnmSignal.frames[i-1].h!=null && hnmSignal.frames[i-1].h.phases!=null && hnmSignal.frames[i-1].h.phases.length>0)
                isPrevVoiced =  true;
            
            if (hnmSignal.frames[i].h!=null && hnmSignal.frames[i].h.phases!=null && hnmSignal.frames[i].h.phases.length>0)
                isVoiced = true;

            if (i<hnmSignal.frames.length-1 && hnmSignal.frames[i+1].h!=null && hnmSignal.frames[i+1].h.phases!=null && hnmSignal.frames[i+1].h.phases.length>0)
                isNextVoiced = true;
            
            if (isVoiced)
                numHarmonicsCurrentFrame = hnmSignal.frames[i].h.phases.length;
            else if (!isVoiced && isNextVoiced)
                numHarmonicsCurrentFrame = hnmSignal.frames[i+1].h.phases.length;
            else
                numHarmonicsCurrentFrame = 0;
            
            f0InHz = hnmSignal.frames[i].f0InHz;
            
            if (i>0)
                f0InHzPrev = hnmSignal.frames[i-1].f0InHz;
            else
                f0InHzPrev = f0InHz;
            
            if (isNextVoiced)
                f0InHzNext = hnmSignal.frames[i+1].f0InHz;
            else
                f0InHzNext = f0InHz;

            f0Average = 0.5f*(f0InHz+f0InHzNext);
            
            for (k=0; k<numHarmonicsCurrentFrame; k++)
            {
                if (HntmAnalyzer.INCLUDE_ZEROTH_HARMONIC)
                    currentHarmonicNo = k;
                else
                    currentHarmonicNo = k+1;
                
                aksi = 0.0;
                aksiPlusOne = 0.0;
                
                phaseki = 0.0f;
                phasekiPlusOne = 0.0f;
                
                isPrevTrackVoiced = false;
                isTrackVoiced = false;
                isNextTrackVoiced = false;
                
                if (i>0 && hnmSignal.frames[i-1].h!=null && hnmSignal.frames[i-1].h.phases!=null && hnmSignal.frames[i-1].h.phases.length>k)
                    isPrevTrackVoiced = true;
                
                if (hnmSignal.frames[i].h!=null && hnmSignal.frames[i].h.phases!=null && hnmSignal.frames[i].h.phases.length>k)
                    isTrackVoiced = true;

                if (i<hnmSignal.frames.length-1 && hnmSignal.frames[i+1].h!=null && hnmSignal.frames[i+1].h.phases!=null && hnmSignal.frames[i+1].h.phases.length>k)
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

                trackStartIndex = SignalProcUtils.time2sample(trackStartInSeconds, hnmSignal.samplingRateInHz);
                trackEndIndex = SignalProcUtils.time2sample(trackEndInSeconds, hnmSignal.samplingRateInHz);

                if (isTrackVoiced && trackEndIndex-trackStartIndex+1>0)
                {
                    //Amplitudes     
                    if (isTrackVoiced)
                    {
                        if (!HntmAnalyzer.USE_AMPLITUDES_DIRECTLY)
                            aksi = RegularizedCepstralEnvelopeEstimator.cepstrum2linearSpectrumValue(hnmSignal.frames[i].h.ceps, currentHarmonicNo*f0InHz, hnmSignal.samplingRateInHz);
                        else
                        {
                            if (k<hnmSignal.frames[i].h.ceps.length)
                                aksi = hnmSignal.frames[i].h.ceps[k]; //Use amplitudes directly without cepstrum method
                        }
                    }
                    
                    if (isNextTrackVoiced)
                    {
                        if (!HntmAnalyzer.USE_AMPLITUDES_DIRECTLY)
                            aksiPlusOne = RegularizedCepstralEnvelopeEstimator.cepstrum2linearSpectrumValue(hnmSignal.frames[i+1].h.ceps , currentHarmonicNo*f0InHzNext, hnmSignal.samplingRateInHz);
                        else
                        {
                            if (k<hnmSignal.frames[i+1].h.ceps.length)
                                aksiPlusOne = hnmSignal.frames[i+1].h.ceps[k]; //Use amplitudes directly without cepstrum method
                        }
                    }
                    //

                    //Phases
                    if (isTrackVoiced)
                    {
                        if (currentHarmonicNo==0)
                            phaseki = 0.0f;
                        else
                            phaseki = hnmSignal.frames[i].h.phases[k];
                    }
                    if (isNextTrackVoiced)
                    {
                        if (currentHarmonicNo==0)
                            phasekiPlusOne = 0.0f;
                        else
                            phasekiPlusOne = hnmSignal.frames[i+1].h.phases[k];
                    }
                    
                    //phaseki += MathUtils.degrees2radian(-4.0);
                    //phasekiPlusOne += MathUtils.degrees2radian(-4.0);
                    
                    if (!isTrackVoiced && isNextTrackVoiced)   
                        phaseki = (float)( phasekiPlusOne - currentHarmonicNo*MathUtils.TWOPI*f0InHzNext*(tsikPlusOne-tsik)); //Equation (3.54)
                    else if (isTrackVoiced && !isNextTrackVoiced)
                        phasekiPlusOne = (float)( phaseki + currentHarmonicNo*MathUtils.TWOPI*f0InHz*(tsikPlusOne-tsik)); //Equation (3.55)
                    
                    phasekiPlusOneEstimate = (float)( phaseki + currentHarmonicNo*MathUtils.TWOPI*f0Average*(tsikPlusOne-tsik));
                    //phasekiPlusOneEstimate = (float) (MathUtils.TWOPI*(Math.random()-0.5)); //Random phase
                    
                    //System.out.println(String.valueOf(f0Average) + " - " + String.valueOf(f0InHz) + " - " + String.valueOf(f0InHzNext));
                    
                    Mk = (int)Math.floor((phasekiPlusOneEstimate-phasekiPlusOne)/MathUtils.TWOPI + 0.5);
                    //
                    
                    if (!isPrevTrackVoiced)
                        trackStartIndex = Math.max(0, trackStartIndex-transitionLen);

                    for (n=trackStartIndex; n<=Math.min(trackEndIndex, outputLen-1); n++)
                    {
                        t = SignalProcUtils.sample2time(n, hnmSignal.samplingRateInHz);

                        //if (t>=tsik && t<tsikPlusOne)
                        {
                            //Amplitude estimate
                            if (t<tsik)
                                akt = MathUtils.interpolatedSample(0.0, t, tsik, 0.0, aksi);
                            else
                                akt = MathUtils.interpolatedSample(tsik, t, tsikPlusOne, aksi, aksiPlusOne);
                            //akt = 1.0;
                            //

                            //Phase estimate
                            phasekt = (float)( phaseki + (phasekiPlusOne+MathUtils.TWOPI*Mk-phaseki)*(t-tsik)/(tsikPlusOne-tsik) );
                            //

                            allSynthAmps[k][i] = akt;
                            allSynthPhases[k][i] = phasekt;
   
                            if (!isPrevTrackVoiced && n-trackStartIndex<transitionLen)
                                harmonicTracks[k][n] = halfTransitionWinLeft[n-trackStartIndex]*akt*Math.cos(phasekt);
                            else if (!isNextTrackVoiced && trackEndIndex-n<transitionLen)
                                harmonicTracks[k][n] = halfTransitionWinRight[halfTransitionWinRight.length-1-(trackEndIndex-n)]*akt*Math.cos(phasekt);
                            else
                                harmonicTracks[k][n] = akt*Math.cos(phasekt);
                        }
                    } 
                }
            }
        }
        
        //MaryUtils.plot(allSynthAmps[1]);
        //MaryUtils.plot(allSynthPhases[1]);
        
        if (harmonicTracks!=null)
        {
            for (k=0; k<harmonicTracks.length; k++)
            {
                for (n=0; n<harmonicPart.length; n++)
                    harmonicPart[n] += harmonicTracks[k][n];
            }

   
            if (referenceFile!=null && FileUtils.exists(referenceFile) && HntmSynthesizer.WRITE_SEPARATE_TRACKS_TO_OUTPUT)
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
                        harmonicTracks[k] = MathUtils.multiply(harmonicTracks[k], 1.0/MathUtils.getAbsMax(harmonicTracks[k]));

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
            }
            //
        }
        
        return harmonicPart;
    }
}
