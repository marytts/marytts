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

/**
 * @author oytun.turk
 *
 * This class generates the sinusoidal tracks given individual peak amplitudes measured from the DFT spectrum
 * 
 */
public class TrackGenerator {
    public static float ZERO_AMP_SHIFT_IN_SECONDS = 0.005f; //Time instant before/after current time to insert a turning-on/off event
                                                            //The amplitudes and synthesis freqs/phases are accordingly interpolated to provide a smooth transition
    
    public TrackGenerator()
    {
        
    }
    
    /*
     * Group individual sinusoids into tracks by considering closeness in frequency
     * Current version is a simple implementation of checking the frequency difference between neighbouring
     * sinusoids and assigning them to same track if the absolute difference is less than a threshold
     * Possible ways to improve this process would be to employ:
     * - constraints on amplitude continuity
     * - constraints on phase continuity (i.e. the phase difference between two consecutive sinusoids 
     *   should not be larger or smaller than some percent of the period
     *
     * framesSins[i][] : Array of sinusoidal parameters (amps, freqs, phases) extracted from ith speech frame
     * framesSins[i][j]:  Sinusoidal parameters of the jth peak sinusoid in the DFT spectrum of speech frame i
     * Returns a number of sinusoidal tracks
     * 
     * This version uses a simple search mechanism to compare a current sinusoid frequecny with the previous and if the difference is smaller than
     * +-deltaInHz, assigns the new sinusoid to the previous sinusoid´s track
     * In the assignment, longer previous paths are favoured in a wighted manner, i.e. the longer a candidate track, 
     * the more likely the current sinusoid gets assigned to that track
     * 
     */
    public SinusoidalTracks generateTracks(SinusoidalSpeechSignal sinSignal, float deltaInHz, int samplingRate)
    {
        int numFrames = sinSignal.framesSins.length;
        
        SinusoidalTracks tr = null;
        int i;
        Sinusoid zeroAmpSin;
        
        if (numFrames>0)
        {   
            int j, k;
            float tmpDist, minDist;
            int trackInd;
            boolean [] bSinAssigneds = null;
    
            for (i=0; i<numFrames; i++)
            {   
                if (tr==null) //If no tracks yet, assign the current sinusoids to new tracks
                {
                    tr = new SinusoidalTracks(sinSignal.framesSins[i].sinusoids.length, samplingRate);
                    tr.setSysAmpsAndTimes(sinSignal.framesSins);
                    
                    for (j=0; j<sinSignal.framesSins[i].sinusoids.length; j++)
                    {
                        //First add a zero amplitude sinusoid at previous time instant to allow smooth synthesis (i.e. "turning on" the track)
                        zeroAmpSin = new Sinusoid(0.0f, sinSignal.framesSins[i].sinusoids[j].freq, 0.0f, Sinusoid.NON_EXISTING_FRAME_INDEX);
                        tr.add(new SinusoidalTrack(sinSignal.framesSins[i].time-ZERO_AMP_SHIFT_IN_SECONDS, zeroAmpSin,  SinusoidalTrack.TURNED_ON));
                        //
                    
                        tr.tracks[tr.currentIndex].add(sinSignal.framesSins[i].time, sinSignal.framesSins[i].sinusoids[j], SinusoidalTrack.ACTIVE);
                    }
                }
                else //If there are tracks, first check "continuations" by checking whether a given sinusoid is in the +-deltaInHz neighbourhood of the previous track. 
                     // Those tracks that do not continue are "turned off". 
                     // All sinusoids of the current frame that are not assigned to any of the "continuations" or "turned off" are "birth"s of new tracks.
                {
                    for (j=0; j<tr.currentIndex+1; j++)
                    {
                        if (tr.tracks[j] != null)
                            tr.tracks[j].resetCandidate();
                    }
                    
                    bSinAssigneds = new boolean[sinSignal.framesSins[i].sinusoids.length];
                    
                    //Continuations:
                    for (k=0; k<sinSignal.framesSins[i].sinusoids.length; k++)
                    {
                        minDist = Math.abs(sinSignal.framesSins[i].sinusoids[k].freq-tr.tracks[0].freqs[tr.tracks[0].currentIndex]);
                        if (minDist<deltaInHz)
                            trackInd = 0;
                        else
                            trackInd = -1;
                        
                        for (j=1; j<tr.currentIndex+1; j++)
                        {
                            tmpDist = Math.abs(sinSignal.framesSins[i].sinusoids[k].freq-tr.tracks[j].freqs[tr.tracks[j].currentIndex]);
                            
                            if (tmpDist<deltaInHz && (trackInd==-1 || tmpDist<minDist))
                            {
                                minDist = tmpDist;
                                trackInd = j;
                            }   
                        }
                        
                        if (trackInd>-1)
                        {
                            if (tr.tracks[trackInd].newCandidateInd>-1)
                                bSinAssigneds[tr.tracks[trackInd].newCandidateInd] = false;
                            
                            tr.tracks[trackInd].newCandidate = new Sinusoid(sinSignal.framesSins[i].sinusoids[k]);
                            tr.tracks[trackInd].newCandidateInd = k;
                            
                            bSinAssigneds[k] = true; //The sinusoid might be assigned to an existing track provided that a closer sinusoid is not found
                        }
                        else
                            bSinAssigneds[k] = false; //This is the birth of a new track since it does not match any existing tracks
                    }
                    
                    //Here is the actual assignment of sinusoids to existing tracks 
                    for (j=0; j<tr.currentIndex+1; j++)
                    {
                        if (tr.tracks[j].newCandidate != null)
                        {
                            Sinusoid tmpSin = new Sinusoid(tr.tracks[j].newCandidate);

                            if (tr.tracks[j].states[tr.tracks[j].currentIndex]!=SinusoidalTrack.ACTIVE)
                            {
                                zeroAmpSin = new Sinusoid(0.0f, tr.tracks[j].freqs[tr.tracks[j].totalSins-1], 0.0f, Sinusoid.NON_EXISTING_FRAME_INDEX);
                                tr.tracks[j].add(sinSignal.framesSins[i].time-ZERO_AMP_SHIFT_IN_SECONDS, zeroAmpSin, SinusoidalTrack.TURNED_ON);
                            }
                            
                            tr.tracks[j].add(sinSignal.framesSins[i].time, tmpSin, SinusoidalTrack.ACTIVE); 
                        }
                        else //Turn off tracks that are not assigned any new sinusoid
                        {
                            if (tr.tracks[j].states[tr.tracks[j].currentIndex]!=SinusoidalTrack.TURNED_OFF)
                            {
                                zeroAmpSin = new Sinusoid(0.0f, tr.tracks[j].freqs[tr.tracks[j].totalSins-1], 0.0f, Sinusoid.NON_EXISTING_FRAME_INDEX);
                                tr.tracks[j].add(sinSignal.framesSins[i].time+ZERO_AMP_SHIFT_IN_SECONDS, zeroAmpSin, SinusoidalTrack.TURNED_OFF);
                            }  
                        } 
                    }
                    
                    //Births: Create new tracks from sinusoids that are not assigned to existing tracks
                    for (k=0; k<bSinAssigneds.length; k++)
                    {
                        if (!bSinAssigneds[k])
                        {
                            //First add a zero amplitude sinusoid to previous frame to allow smooth synthesis (i.e. "turning on" the track)
                            zeroAmpSin = new Sinusoid(0.0f, sinSignal.framesSins[i].sinusoids[k].freq, 0.0f, Sinusoid.NON_EXISTING_FRAME_INDEX);
                            tr.add(new SinusoidalTrack(sinSignal.framesSins[i].time-ZERO_AMP_SHIFT_IN_SECONDS, zeroAmpSin, SinusoidalTrack.TURNED_ON));
                            //
                            
                            tr.tracks[tr.currentIndex].add(sinSignal.framesSins[i].time, sinSignal.framesSins[i].sinusoids[k], SinusoidalTrack.ACTIVE);
                        }
                    }

                    System.out.println("Track generation using frame " + String.valueOf(i+1) + " of " + String.valueOf(numFrames));
                } 
                
                //Turn-off all active tracks after the last speech frame
                if (i==numFrames-1)
                {
                    for (j=0; j<tr.currentIndex+1; j++)
                    {
                        if (Math.abs(sinSignal.framesSins[i].time-tr.tracks[j].times[tr.tracks[j].totalSins-1])<ZERO_AMP_SHIFT_IN_SECONDS)
                        {
                            if (tr.tracks[j].states[tr.tracks[j].currentIndex]==SinusoidalTrack.ACTIVE)
                            {
                                zeroAmpSin = new Sinusoid(0.0f, tr.tracks[j].freqs[tr.tracks[j].totalSins-1], 0.0f, Sinusoid.NON_EXISTING_FRAME_INDEX);
                                tr.tracks[j].add(sinSignal.framesSins[i].time+ZERO_AMP_SHIFT_IN_SECONDS, zeroAmpSin, SinusoidalTrack.TURNED_OFF);
                            }
                        }
                    }
                }
                //
            }
        }
        
        for (i=0; i<=tr.currentIndex; i++)
            tr.tracks[i].correctTrack();
        
        tr.setOriginalDurationManual(sinSignal.originalDurationInSeconds);
        
        return new SinusoidalTracks(tr, 0, tr.currentIndex);
    }
}
