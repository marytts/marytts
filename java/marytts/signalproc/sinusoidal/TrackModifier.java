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

package marytts.signalproc.sinusoidal;

import marytts.signalproc.analysis.PitchMarks;
import marytts.util.MaryUtils;
import marytts.util.math.MathUtils;
import marytts.util.signal.SignalProcUtils;

/**
 * @author Oytun T&uumlrk
 */
public class TrackModifier {
    
    public static float DEFAULT_MODIFICATION_SKIP_SIZE = 0.005f; //Default skip size (in seconds) to be used in sinusoidal analysis, modification, and synthesis
                                                                 //Note that lower skip sizes might be required in order to obtain better performance for 
                                                                 // large duration modification factors or to realize more accurate final target lengths
                                                                 // because the time scaling resolution will only be as low as the skip size
    
    public static final int FROM_ORIGINAL = 1;
    public static final int FROM_RESAMPLED = 2;
    public static final int FROM_CEPSTRUM = 3;
    public static final int FROM_INTERPOLATED = 4; //This is only available for phase
    
    public static SinusoidalTracks modifyTimeScale(SinusoidalTracks trIn, 
                                                    double[] f0s, 
                                                    float f0_ss, float f0_ws,
                                                    int[] pitchMarks,
                                                    float[] voicings, 
                                                    float numPeriods,
                                                    boolean isVoicingAdaptiveTimeScaling, 
                                                    float timeScalingVoicingThreshold, 
                                                    boolean isVoicingAdaptivePitchScaling, 
                                                    float tScale,
                                                    int offset,
                                                    int sysAmpModMethod,
                                                    int sysPhaseModMethod)
    {  
        float [] tScales = new float[1];
        float [] tScalesTimes = new float[1];
        tScales[0] = tScale;
        tScalesTimes[0] = 0.02f;
        
        return modify(trIn, 
                               f0s, 
                               f0_ss,  f0_ws,
                               pitchMarks,
                               voicings, 
                               numPeriods,
                               isVoicingAdaptiveTimeScaling, 
                               timeScalingVoicingThreshold, 
                               isVoicingAdaptivePitchScaling,
                               tScales,
                               tScalesTimes,
                               null,
                               null,
                               offset,
                               sysAmpModMethod,
                               sysPhaseModMethod);
    }

    public static SinusoidalTracks modify(SinusoidalTracks trIn, 
                                            double[] f0s, 
                                            float f0_ss, float f0_ws,
                                            int[] pitchMarks,
                                            float[] voicings, 
                                            float numPeriods,
                                            boolean isVoicingAdaptiveTimeScaling, 
                                            float timeScalingVoicingThreshold, 
                                            boolean isVoicingAdaptivePitchScaling, 
                                            float[] tScales,
                                            float[] tScalesTimes,
                                            float[] pScales,
                                            float[] pScalesTimes,
                                            int offset,
                                            int sysAmpModMethod,
                                            int sysPhaseModMethod)
    {   
        int i, j, lShift;
        
        if (tScalesTimes==null)
        {
            tScalesTimes = new float[tScales.length];
            for (i=0; i<tScales.length; i++)
                tScalesTimes[i] = (float)((i+0.5)/tScales.length*trIn.origDur);
        }
        
        if (pScalesTimes==null)
        {
            pScalesTimes = new float[pScales.length];
            for (i=0; i<pScales.length; i++)
                pScalesTimes[i] = (float)((i+0.5)/pScales.length*trIn.origDur);
        }

        //Pitch scale pitch contour
        double[] f0sMod = SignalProcUtils.pitchScalePitchContour(f0s, f0_ws, f0_ss, pScales, pScalesTimes);

        //Time scale pitch contour
        f0sMod = SignalProcUtils.timeScalePitchContour(f0sMod, f0_ws, f0_ss, tScales, tScalesTimes);

        float maxDur = SignalProcUtils.timeScaledTime(trIn.origDur, tScales, tScalesTimes);

        //Find modified onsets
        PitchMarks pmMod = SignalProcUtils.pitchContour2pitchMarks(f0sMod, trIn.fs, (int)Math.floor(maxDur*trIn.fs+0.5), f0_ws, f0_ss, false, offset);

        float tScaleCurrent;
        float pScaleCurrent;

        float pVoicing;
        float bandwidth = (float)(0.5f*MathUtils.TWOPI);

        float excPhase, excPhaseMod;
        float prevExcPhase, prevExcPhaseMod;
        float sysPhase, sysPhaseMod;
        float sysPhaseModReal;
        float sysPhaseModImag; 
        float excAmp, excAmpMod;
        float sysAmp, sysAmpMod;
        float freq, freqMod;

        int closestInd;
        int closestIndMod;
        int sysTimeInd, sysFreqInd, sysFreqIndMod;
        double sysFreqIndDouble;
        int currentInd;
        int n0, n0Mod, n0Prev, n0ModPrev;
        int Pm;
        int J, JMod;
        int tempIndex;
        
        int middleAnalysisSample;
        int prevMiddleAnalysisSample;
        float middleSynthesisTime;
        int middleSynthesisSample;
        int prevMiddleSynthesisSample;

        float freqInHz;
        
        int maxFreqInd;

        SinusoidalTracks trMod = null;
        int trackSt, trackEn;

        boolean bSingleTrackTest = false;
        //boolean bSingleTrackTest = true;

        if (bSingleTrackTest)  
        {
            trackSt = 7;
            trackEn = 7;
            trMod = new SinusoidalTracks(1, trIn.fs);
        }
        else
        {
            trackSt = 0;
            trackEn = trIn.totalTracks-1;
            trMod = new SinusoidalTracks(trIn);
        }
        
        prevExcPhase = 0.0f;
        prevExcPhaseMod = 0.0f;
        prevMiddleAnalysisSample = 0;
        prevMiddleSynthesisSample = 0;

        for (i=trackSt; i<=trackEn; i++)
        {
            if (bSingleTrackTest)
                trMod.add(trIn.tracks[i]);

            n0Prev = 0;
            n0ModPrev = 0;

            for (j=0; j<trIn.tracks[i].totalSins; j++)
            {   
                if (!bSingleTrackTest)
                    currentInd = i;
                else
                    currentInd = 0;

                if (trIn.tracks[i].states[j]==SinusoidalTrack.ACTIVE || trIn.tracks[i].states[j]==SinusoidalTrack.TURNED_OFF)
                {
                    middleAnalysisSample = SignalProcUtils.time2sample(trIn.tracks[i].times[j], trIn.fs);

                    closestInd = MathUtils.findClosest(pitchMarks, middleAnalysisSample);

                    sysTimeInd = MathUtils.findClosest(trIn.times, trIn.tracks[i].times[j]);
                    freqInHz = SignalProcUtils.radian2Hz(trIn.tracks[i].freqs[j], trIn.fs);
                    
                    int pScaleInd = MathUtils.findClosest(pScalesTimes, trIn.tracks[i].times[j]);
                    pScaleCurrent = pScales[pScaleInd];

                    //Voicing dependent pitch scale modification factor estimation
                    if (voicings!=null && isVoicingAdaptivePitchScaling)
                    {
                        pVoicing = voicings[Math.min(closestInd, voicings.length-1)];
                        float pitchScalingFreqThreshold = (float)(0.5f*pVoicing*MathUtils.TWOPI);

                        //Frequency limit for pitch scaling needs some elaboration
                        if (trIn.tracks[i].freqs[j]>pitchScalingFreqThreshold)
                            pScaleCurrent = 1.0f;
                        else
                            pScaleCurrent = pScales[pScaleInd];
                    }   
                    
                    //Apply triangular decreasing of pitch scale in voiced/unvoiced transition region
                    if (pScaleCurrent!=1.0f)
                    {
                        float modFreqLowerCutoffInHz = 3500.0f;
                        float modFreqUpperCutoffInHz = 4500.0f;
                        if (freqInHz>=modFreqLowerCutoffInHz && freqInHz<modFreqLowerCutoffInHz)
                            pScaleCurrent = (freqInHz-modFreqLowerCutoffInHz)*(pScaleCurrent-1.0f)/(modFreqUpperCutoffInHz-modFreqLowerCutoffInHz)+1.0f;
                        else if (freqInHz>=modFreqLowerCutoffInHz)
                            pScaleCurrent = 1.0f;
                    }
                    //

                    int tScaleInd = MathUtils.findClosest(tScalesTimes, trIn.tracks[i].times[j]);
                    tScaleCurrent = tScales[tScaleInd];

                    //Voicing dependent time scale modification factor estimation
                    if (voicings!=null && isVoicingAdaptiveTimeScaling)
                    {
                        pVoicing = voicings[Math.min(closestInd, voicings.length-1)];

                        if (pVoicing<timeScalingVoicingThreshold)
                            tScaleCurrent = 1.0f;
                        else
                            tScaleCurrent = (1.0f-pVoicing) + pVoicing*tScales[tScaleInd];
                    }     
                    
                    sysFreqInd = SignalProcUtils.freq2index(freqInHz, trIn.fs, trIn.sysAmps.get(sysTimeInd).length-1);
                    sysFreqIndDouble = SignalProcUtils.freq2indexDouble(freqInHz, trIn.fs, trIn.sysAmps.get(sysTimeInd).length-1);
                    sysAmp = (float)(trIn.sysAmps.get(sysTimeInd)[sysFreqInd]);
                    
                    //This is from Van Santen´s et.al.´s book - Chapter 5 
                    //(van Santen, et. al., Progress in Speech Synthesis)
                    //sysAmp = (float)SignalProcUtils.cepstrum2linearSpecAmp(trIn.sysCeps.get(sysTimeInd), trIn.tracks[i].freqs[j]);

                    excPhase = prevExcPhase + trIn.tracks[i].freqs[j]*(middleAnalysisSample-prevMiddleAnalysisSample);
                    sysPhase = trIn.tracks[i].phases[j]-excPhase;
                    excAmp = trIn.tracks[i].amps[j]/sysAmp;
                    //excAmp = 1.0f; //This should hold whenever an envelope that passes from spectral peaks is used, i.e. SEEVOC
                    freq = trIn.tracks[i].freqs[j];

                    //Estimate modified excitation phase
                    
                    if (trIn.tracks[i].states[j]!=SinusoidalTrack.TURNED_OFF)
                        middleSynthesisTime = SignalProcUtils.timeScaledTime(trIn.tracks[i].times[j], tScales, tScalesTimes);
                    else
                        middleSynthesisTime = trMod.tracks[currentInd].times[j-1]+TrackGenerator.ZERO_AMP_SHIFT_IN_SECONDS;

                    middleSynthesisSample = (int)SignalProcUtils.time2sample(middleSynthesisTime, trIn.fs);
                    closestIndMod = MathUtils.findClosest(pmMod.pitchMarks, middleSynthesisSample);

                    excPhaseMod = prevExcPhaseMod + pScaleCurrent*trIn.tracks[i].freqs[j]*(middleSynthesisSample-prevMiddleSynthesisSample);
                    excAmpMod = excAmp;
                    //excAmpMod = 1.0f; //This should hold whenever an envelope that passes from spectral peaks is used, i.e. SEEVOC
                    
                    freqMod = (float)pScaleCurrent*freq;
                    if (freqMod>0.5*MathUtils.TWOPI)
                        excAmpMod=0.0f;
                    while (freqMod>MathUtils.TWOPI)
                        freqMod -= MathUtils.TWOPI;

                    sysFreqIndMod = sysFreqInd;
                    sysPhaseMod = sysPhase;
                    sysAmpMod = sysAmp;
                    
                    if (pScaleCurrent!=1.0f) //Modify system phase and amplitude according to pitch scale modification factor
                    {
                        sysFreqIndMod = SignalProcUtils.freq2index(pScaleCurrent*freqInHz, trIn.fs, trIn.sysAmps.get(sysTimeInd).length-1);
                        sysFreqIndMod = Math.min(sysFreqIndMod, trIn.sysAmps.get(sysTimeInd).length-1);
                        sysFreqIndMod = Math.max(sysFreqIndMod, 0);

                        //System phase modification for pitch scaling
                        if (sysPhaseModMethod==FROM_ORIGINAL)
                            sysPhaseMod = sysPhase;
                        else if (sysPhaseModMethod==FROM_RESAMPLED)
                            sysPhaseMod = (float)(trIn.sysPhases.get(sysTimeInd)[sysFreqIndMod]); //This is wrong, create phase envelope for real and imaginary parts, and then resample
                        else if (sysPhaseModMethod==FROM_INTERPOLATED)
                        {
                            if (freqInHz<0.5*trIn.fs/pScaleCurrent-50.0f)
                            {
                                //This is from Quatieri´s paper "Shape Invariant..."
                                tempIndex = (int)Math.floor(pScaleCurrent*sysFreqIndDouble);
                                if (sysFreqInd<trIn.frameDfts.get(sysTimeInd).real.length-1)
                                {
                                    sysPhaseModReal = (float)MathUtils.interpolatedSample(tempIndex, sysFreqIndDouble, tempIndex+1, trIn.frameDfts.get(sysTimeInd).real[tempIndex], trIn.frameDfts.get(sysTimeInd).real[tempIndex+1]);
                                    sysPhaseModImag = (float)MathUtils.interpolatedSample(tempIndex, sysFreqIndDouble, tempIndex+1, trIn.frameDfts.get(sysTimeInd).imag[tempIndex], trIn.frameDfts.get(sysTimeInd).imag[tempIndex+1]);   
                                }
                                else
                                {
                                    sysPhaseModReal = (float)MathUtils.interpolatedSample(tempIndex-1, sysFreqIndDouble, tempIndex, trIn.frameDfts.get(sysTimeInd).real[tempIndex-1], trIn.frameDfts.get(sysTimeInd).real[tempIndex]);
                                    sysPhaseModImag = (float)MathUtils.interpolatedSample(tempIndex-1, sysFreqIndDouble, tempIndex, trIn.frameDfts.get(sysTimeInd).imag[tempIndex-1], trIn.frameDfts.get(sysTimeInd).imag[tempIndex]);   
                                }
                                sysPhaseMod = (float)Math.atan2(sysPhaseModImag, sysPhaseModReal);
                            }
                            else
                                sysPhaseMod = sysPhase;
                        }
                        else if (sysPhaseModMethod==FROM_CEPSTRUM)
                        {
                            //This is from Van Santen´s et.al.´s book - Chapter 5 
                            //(van Santen, et. al., Progress in Speech Synthesis)
                            sysPhaseMod = (float)SignalProcUtils.cepstrum2minimumPhase(trIn.sysCeps.get(sysTimeInd), pScaleCurrent*trIn.tracks[i].freqs[j]);
                        }
                        //
                        
                        //System amplitude modification for pitch scaling
                        if (sysAmpModMethod==FROM_ORIGINAL)
                        {
                            //This will make vocal tract scaled in proportion to pitch scale amount
                            sysAmpMod = sysAmp; 
                        }
                        else if (sysAmpModMethod==FROM_RESAMPLED)
                        {
                            //This is from Quatieri´s paper "Shape Invariant..."
                            //Get system amp from modified location
                            sysAmpMod = (float)(trIn.sysAmps.get(sysTimeInd)[sysFreqIndMod]); 
                        }
                        else if (sysAmpModMethod==FROM_CEPSTRUM)
                        {
                            //This is from Van Santen´s et.al.´s book - Chapter 5 
                            //(van Santen, et. al., Progress in Speech Synthesis)
                            sysAmpMod = (float)SignalProcUtils.cepstrum2linearSpecAmp(trIn.sysCeps.get(sysTimeInd), pScaleCurrent*trIn.tracks[i].freqs[j]);
                        }
                        //

                        //MaryUtils.plot(trIn.sysAmps.get(sysTimeInd));
                    }
                    
                    trMod.tracks[currentInd].amps[j] = excAmpMod*sysAmpMod;
                    trMod.tracks[currentInd].freqs[j] = freqMod;
                    trMod.tracks[currentInd].phases[j] = sysPhaseMod + excPhaseMod;
                    trMod.tracks[currentInd].times[j] = middleSynthesisTime;

                    if (trMod.tracks[currentInd].times[j]>maxDur)
                        maxDur = trMod.tracks[currentInd].times[j];

                    if (j>0 && trIn.tracks[i].states[j-1]==SinusoidalTrack.TURNED_ON)
                        trMod.tracks[currentInd].times[j-1] = Math.max(0.0f, trMod.tracks[currentInd].times[j]-TrackGenerator.ZERO_AMP_SHIFT_IN_SECONDS);

                    prevExcPhase = excPhase;
                    prevExcPhaseMod = excPhaseMod;
                    
                    prevMiddleSynthesisSample = middleSynthesisSample;
                    prevMiddleAnalysisSample = middleAnalysisSample;  
                }
                else if (trIn.tracks[i].states[j]==SinusoidalTrack.TURNED_ON)
                {    
                    prevMiddleAnalysisSample = SignalProcUtils.time2sample(trIn.tracks[i].times[j], trIn.fs);
                    middleSynthesisTime = SignalProcUtils.timeScaledTime(trIn.tracks[i].times[j], tScales, tScalesTimes);
                    prevMiddleSynthesisSample = (int)SignalProcUtils.time2sample(middleSynthesisTime, trIn.fs);
                    
                    prevExcPhase = 0.0f;
                    prevExcPhaseMod = 0.0f;
                }
            }
        }

        trMod.origDur = maxDur;
        
        if (trMod!=null)
        {
            System.out.println("--- Modified track statistics ---");
            trMod.getTrackStatistics();
            SinusoidalAnalyzer.getGrossStatistics(trMod);
        }

        return trMod;
    }
}
