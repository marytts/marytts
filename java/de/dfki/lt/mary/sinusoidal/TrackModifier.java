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

package de.dfki.lt.mary.sinusoidal;

import de.dfki.lt.signalproc.analysis.PitchMarker;
import de.dfki.lt.signalproc.util.MathUtils;
import de.dfki.lt.signalproc.util.SignalProcUtils;

/**
 * @author oytun.turk
 *
 */
public class TrackModifier {
    
    public static float DEFAULT_MODIFICATION_SKIP_SIZE = 0.005f; //Default skip size (in seconds) to be used in sinusoidal analysis, modification, and synthesis
                                                                 //Note that lower skip sizes might be required in order to obtain better performance for 
                                                                 // large duration modification factors or to realize more accurate final target lengths
                                                                 // because the time scaling resolution will only be as low as the skip size
    
    public static SinusoidalTracks modifyTimeScale(SinusoidalTracks trIn, 
                                                    double [] f0s, 
                                                    float f0_ss, float f0_ws,
                                                    int [] pitchMarks,
                                                    float [] voicings, 
                                                    float skipSizeInSeconds,
                                                    float numPeriods,
                                                    boolean isVoicingAdaptiveTimeScaling, 
                                                    float timeScalingVoicingThreshold, 
                                                    boolean isVoicingAdaptivePitchScaling, 
                                                    float tScale)
    {  
        float [] tScales = new float[1];
        float [] tScalesTimes = new float[1];
        tScales[0] = tScale;
        tScalesTimes[0] = skipSizeInSeconds;
        
        return modify(trIn, 
                               f0s, 
                               f0_ss,  f0_ws,
                               pitchMarks,
                               voicings, 
                               skipSizeInSeconds,
                               numPeriods,
                               isVoicingAdaptiveTimeScaling, 
                               timeScalingVoicingThreshold, 
                               isVoicingAdaptivePitchScaling,
                               tScales,
                               tScalesTimes,
                               null,
                               null);
    }
    
    public static SinusoidalTracks modify(SinusoidalTracks trIn, 
                                               double [] f0s, 
                                               float f0_ss, float f0_ws,
                                               int [] pitchMarks,
                                               float [] voicings, 
                                               float skipSizeInSeconds,
                                               float numPeriods,
                                               boolean isVoicingAdaptiveTimeScaling, 
                                               float timeScalingVoicingThreshold, 
                                               boolean isVoicingAdaptivePitchScaling, 
                                               float [] tScales,
                                               float [] tScalesTimes,
                                               float [] pScales,
                                               float [] pScalesTimes)
    {   
        int i, j, l, lShift;

       
        
        //Pitch scale pitch contour
        double [] f0sMod = SignalProcUtils.pitchScalePitchContour(f0s, f0_ws, f0_ss, pScales, pScalesTimes);
        
        //Time scale pitch contour
        f0sMod = SignalProcUtils.timeScalePitchContour(f0sMod, f0_ws, f0_ss, tScales, tScalesTimes);
        
        float maxDur = SignalProcUtils.timeScaledTime(trIn.origDur, tScales, tScalesTimes);
        
        //Find modified onsets
        PitchMarker pmMod = SignalProcUtils.pitchContour2pitchMarks(f0sMod, trIn.fs, (int)Math.floor(maxDur*trIn.fs+0.5), f0_ws, f0_ss, false);
        
        int L = (int)Math.floor(skipSizeInSeconds*trIn.fs+0.5);
        
        float tScaleCurrent;
        float pScaleCurrent;
        int LMod;
 
        float pVoicing;
        float bandwidth = (float)(0.5f*MathUtils.TWOPI);

        float excPhase, excPhaseMod;
        float sysPhase, sysPhaseMod;
        float excAmp, excAmpMod;
        float sysAmp, sysAmpMod;
        float freq, freqMod;
        
        int closestInd;
        int closestIndMod;
        int n0, n0Mod, n0Prev, n0ModPrev;
        int Pm;
        int J, JMod;
        
        int middleAnalysisSample;
        float middleSynthesisTime;
        int middleSynthesisSample;

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

        for (i=trackSt; i<=trackEn; i++)
        {
            if (bSingleTrackTest)
                trMod.add(trIn.tracks[i]);
            
            n0Prev = 0;
            n0ModPrev = 0;
            l = 0;
            
            for (j=0; j<trIn.tracks[i].totalSins; j++)
            {   
                if (trIn.tracks[i].states[j]==SinusoidalTrack.ACTIVE)
                {
                    middleAnalysisSample = SignalProcUtils.time2sample(trIn.tracks[i].times[j], trIn.fs);
                    
                    if (j>0 && trIn.tracks[i].states[j-1]==SinusoidalTrack.TURNED_ON) 
                        l = (int)Math.floor(((float)middleAnalysisSample)/L+0.5f)+1;
                    else
                        l++;

                    closestInd = MathUtils.findClosest(pitchMarks, middleAnalysisSample);

                    int pScaleInd = MathUtils.findClosest(pScalesTimes, trIn.tracks[i].times[j]);
                    pScaleCurrent = pScales[pScaleInd];
                    
                    //Voicing dependent pitch scale modification factor estimation
                    if (voicings!=null && isVoicingAdaptivePitchScaling)
                    {
                        pVoicing = voicings[Math.min(closestInd, voicings.length-1)];
                        float pitchScalingFreqThreshold = (float)(0.5f*pVoicing*MathUtils.TWOPI);
                        
                        if (trIn.tracks[i].freqs[j]>pitchScalingFreqThreshold)
                            pScaleCurrent = 1.0f;
                        else
                            pScaleCurrent = pScales[pScaleInd];
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
                    
                    LMod = (int)Math.floor(skipSizeInSeconds*tScaleCurrent*trIn.fs+0.5);
                    
                    if (closestInd<pitchMarks.length-1)
                        Pm = pitchMarks[closestInd+1] - pitchMarks[closestInd];
                    else
                        Pm = pitchMarks[closestInd] - pitchMarks[closestInd-1];
                    
                    if (j>0 && trIn.tracks[i].states[j-1]==SinusoidalTrack.TURNED_ON)
                    {
                        n0 = pitchMarks[closestInd];
                        n0Prev = 0;
                    }
                    else
                    {
                        /*
                        J = 1;
                        while (n0Prev+J*Pm<middleAnalysisSample)
                            J++;
                        
                        if (J>1 && Math.abs(n0Prev+J*Pm-middleAnalysisSample)>Math.abs(n0Prev+(J-1)*Pm-middleAnalysisSample))
                            J--;
                        
                        n0 = n0Prev + J*Pm;
                        */
                        
                        n0 = pitchMarks[closestInd];
                    }
                    
                    excPhase = -(middleAnalysisSample-n0)*trIn.tracks[i].freqs[j];
                    sysPhase = trIn.tracks[i].phases[j]-excPhase;
                    excAmp = trIn.tracks[i].amps[j]/trIn.tracks[i].sysAmps[j];
                    sysAmp = trIn.tracks[i].sysAmps[j];
                    freq = trIn.tracks[i].freqs[j];

                    //Estimate modified excitation phase
                    middleSynthesisTime = SignalProcUtils.timeScaledTime(trIn.tracks[i].times[j], tScales, tScalesTimes);
                    middleSynthesisSample = (int)SignalProcUtils.time2sample(middleSynthesisTime, trIn.fs);
                    closestIndMod = MathUtils.findClosest(pmMod.pitchMarks, middleSynthesisSample);
                    
                    if (j>0 && trIn.tracks[i].states[j-1]==SinusoidalTrack.TURNED_ON)
                    {
                        n0Mod = pmMod.pitchMarks[closestIndMod];
                        n0ModPrev = 0;
                    }
                    else
                    {
                        /*
                        JMod = 1;
                        while (n0ModPrev+JMod*Pm<middleSynthesisSample)
                            JMod++;
                        
                        if (JMod>1 && Math.abs(n0ModPrev+JMod*Pm-middleSynthesisSample)>Math.abs(n0ModPrev+(JMod-1)*Pm-middleSynthesisSample))
                            JMod--;
                        
                        n0Mod = n0ModPrev + JMod*Pm;
                        */
                        
                        n0Mod = pmMod.pitchMarks[closestIndMod];
                    }
                    
                    excPhaseMod = -(middleSynthesisSample-n0Mod)*pScaleCurrent*trIn.tracks[i].freqs[j]; 
                    excAmpMod = excAmp;
                    freqMod = (float)Math.min(pScaleCurrent*freq, 0.5f*MathUtils.TWOPI);
                    
                    if (pScaleCurrent==1.0f)
                    {
                        sysPhaseMod = sysPhase;
                        sysAmpMod = sysAmp;
                    }
                    else //Modify system phase and amplitude according to pitch scale modification factor
                    {
                        sysPhaseMod = sysPhase;
                        sysAmpMod = sysAmp;
                    }
                        
                    if (!bSingleTrackTest)
                    {
                        trMod.tracks[i].amps[j] = excAmpMod*sysAmpMod;
                        trMod.tracks[i].freqs[j] = freqMod;
                        trMod.tracks[i].phases[j] = sysPhaseMod + excPhaseMod;
                        trMod.tracks[i].times[j] = middleSynthesisTime;
                    
                        if (trMod.tracks[i].times[j]>maxDur)
                            maxDur = trMod.tracks[i].times[j];
                    
                        if (j>0 && trIn.tracks[i].states[j-1]==SinusoidalTrack.TURNED_ON)
                            trMod.tracks[i].times[j-1] = Math.max(0.0f, trMod.tracks[i].times[j]-TrackGenerator.ZERO_AMP_SHIFT_IN_SECONDS);
                    }
                    else
                    {
                        trMod.tracks[0].amps[j] = excAmpMod*sysAmpMod;
                        trMod.tracks[0].freqs[j] = freqMod;
                        trMod.tracks[0].phases[j] = sysPhaseMod + excPhaseMod;
                        trMod.tracks[0].times[j] = middleSynthesisTime;
                    
                        if (trMod.tracks[0].times[j]>maxDur)
                            maxDur = trMod.tracks[0].times[j];
                    
                        if (j>0 && trIn.tracks[i].states[j-1]==SinusoidalTrack.TURNED_ON)
                            trMod.tracks[0].times[j-1] = Math.max(0.0f, trMod.tracks[0].times[j]-TrackGenerator.ZERO_AMP_SHIFT_IN_SECONDS);
                    }
                    
                    n0Prev = n0;
                    n0ModPrev = n0Mod;
                }
                else if (trIn.tracks[i].states[j]==SinusoidalTrack.TURNED_OFF)
                {
                    if (!bSingleTrackTest)
                    {
                        trMod.tracks[i].times[j] = trMod.tracks[i].times[j-1]+TrackGenerator.ZERO_AMP_SHIFT_IN_SECONDS;
                    
                        if (trMod.tracks[i].times[j]>maxDur)
                            maxDur = trMod.tracks[i].times[j];
                    }
                    else
                    {
                        trMod.tracks[0].times[j] = trMod.tracks[0].times[j-1]+TrackGenerator.ZERO_AMP_SHIFT_IN_SECONDS;
                    
                        if (trMod.tracks[0].times[j]>maxDur)
                            maxDur = trMod.tracks[0].times[j];
                    }
                }
            }
        }
        
        trMod.origDur = maxDur;
        
        return trMod;
    }
}
