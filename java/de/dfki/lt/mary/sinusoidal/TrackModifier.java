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
    
    public static SinusoidalTracks modifyTimeScaleConstant(SinusoidalTracks trIn, 
                                               double [] f0s, 
                                               float f0_ss, float f0_ws,
                                               int [] pitchMarks,
                                               float [] voicings, 
                                               float skipSizeInSeconds,
                                               float numPeriods,
                                               boolean isVoicingAdaptiveTimeScaling, 
                                               float timeScalingVoicingThreshold, 
                                               float tScale)
    {   
        //Time scale pitch contour
        double [] f0sMod = SignalProcUtils.interpolate_pitch_contour(f0s, tScale);
        
        //Find modified onsets
        PitchMarker pmMod = SignalProcUtils.pitchContour2pitchMarks(f0sMod, trIn.fs, (int)Math.floor(trIn.origDur*tScale*trIn.fs+0.5), f0_ws, f0_ss, false);
        
        int L = (int)Math.floor(skipSizeInSeconds*trIn.fs+0.5);
        int LMod = (int)Math.floor(skipSizeInSeconds*tScale*trIn.fs+0.5);
        
        int i, j, l, lShift;
        float excPhase, excPhaseMod;
        float sysPhase;
        int closestInd;
        int closestIndMod;
        int n0, n0Mod, n0Prev, n0ModPrev;
        int Pm;
        int J, JMod;
        int m;
        
        float maxDur = 0.0f;
        int middleAnalysisSample;
        int middleSynthesisSample;

        SinusoidalTracks trMod = null;
        int trackSt, trackEn;
        
        
        boolean bSingleTrackTest = true;
        

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
            m = 0;
            l = 0;
            
            for (j=0; j<trIn.tracks[i].totalSins; j++)
            {   
                System.out.println(String.valueOf(l) + " " + String.valueOf(m));
                
                if (trIn.tracks[i].states[j]==SinusoidalTrack.ACTIVE)
                {
                    middleAnalysisSample = SignalProcUtils.time2sample(trIn.tracks[i].times[j], trIn.fs);
                    
                    if (j>0 && trIn.tracks[i].states[j-1]==SinusoidalTrack.TURNED_ON) 
                        l = (int)Math.floor(((float)middleAnalysisSample)/L+0.5f)+1;
                    else
                        l++;

                    closestInd = MathUtils.findClosest(pitchMarks, middleAnalysisSample);
                    
                    if (closestInd<pitchMarks.length-1)
                        Pm = pitchMarks[closestInd+1] - pitchMarks[closestInd];
                    else
                        Pm = pitchMarks[closestInd] - pitchMarks[closestInd-1];
                    
                    if (j>0 && trIn.tracks[i].states[j-1]==SinusoidalTrack.TURNED_ON)
                    {
                        m = 0;
                        n0 = pitchMarks[closestInd];
                        n0Prev = 0;
                    }
                    else
                    {
                        m++;
                        
                        J = 1;
                        while (n0Prev+J*Pm<middleAnalysisSample)
                            J++;
                        
                        if (J>1 && Math.abs(n0Prev+J*Pm-middleAnalysisSample)>Math.abs(n0Prev+(J-1)*Pm-middleAnalysisSample))
                            J--;
                        
                        n0 = n0Prev + J*Pm;
                    }
                    
                    excPhase = -(m*L-n0)*trIn.tracks[i].freqs[j];
                    sysPhase = trIn.tracks[i].phases[j]-excPhase;

                    //Estimate modified excitation phase
                    middleSynthesisSample = l*LMod;
                    closestIndMod = MathUtils.findClosest(pmMod.pitchMarks, middleSynthesisSample);
                    
                    if (j>0 && trIn.tracks[i].states[j-1]==SinusoidalTrack.TURNED_ON)
                    {
                        n0Mod = pmMod.pitchMarks[closestIndMod];
                        n0ModPrev = 0;
                    }
                    else
                    {
                        JMod = 1;
                        while (n0ModPrev+JMod*Pm<middleSynthesisSample)
                            JMod++;
                        
                        if (JMod>1 && Math.abs(n0ModPrev+JMod*Pm-middleSynthesisSample)>Math.abs(n0ModPrev+(JMod-1)*Pm-middleSynthesisSample))
                            JMod--;
                        
                        n0Mod = n0ModPrev + JMod*Pm;
                    }
                    
                    excPhaseMod = -(m*LMod-n0Mod)*trIn.tracks[i].freqs[j]; 
                    
                    if (!bSingleTrackTest)
                    {
                        trMod.tracks[i].phases[j] = sysPhase + excPhaseMod;
                        trMod.tracks[i].times[j] = SignalProcUtils.sample2time(middleSynthesisSample, trIn.fs);
                    
                        if (trMod.tracks[i].times[j]>maxDur)
                            maxDur = trMod.tracks[i].times[j];
                    
                        if (j>0 && trIn.tracks[i].states[j-1]==SinusoidalTrack.TURNED_ON)
                            trMod.tracks[i].times[j-1] = Math.max(0.0f, trMod.tracks[i].times[j]-TrackGenerator.ZERO_AMP_SHIFT_IN_SECONDS);
                    }
                    else
                    {
                        trMod.tracks[0].phases[j] = sysPhase + excPhaseMod;
                        trMod.tracks[0].times[j] = SignalProcUtils.sample2time(l*LMod, trIn.fs);
                    
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
