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
    public SinusoidalTracks generateTracksFreqOnly(Sinusoid [][] framesSins, float [] times, float deltaInHz)
    {
        int numFrames = framesSins.length;
        
        SinusoidalTracks tr = null;
        int [] livingTrackInds = null;
        int [] newLivingTrackInds = null;
        int newTotalLiving;
        int tmpInd;
        int i;
        
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
                    tr = new SinusoidalTracks(framesSins[0].length);
                    livingTrackInds = new int[framesSins[0].length];
                    tr.add(times[i], framesSins[i]);

                    for (j=0; j<tr.totalTracks; j++)
                        livingTrackInds[j] = j;
                }
                else //If there are tracks, first check "continuations" by checking whether a given sinusoid is in the +-deltaInHz neighbourhood of the previous track. 
                     // Those tracks that do not continue are "dead". 
                     // All sinusoids of the current frame that are not assigned to any of the "continuations" or "deaths" are "birth"s of new tracks.
                {
                    for (j=0; j<tr.totalTracks; j++)
                    {
                        if (tr.tracks[j] != null)
                        {
                            tr.tracks[j].resetCandidate();
                            tr.tracks[j].bLiving = false;
                        }
                    }
                    
                    bSinAssigneds = new boolean[framesSins[i].length];
                    
                    //Continuations:
                    for (k=0; k<framesSins[i].length; k++)
                    {
                        minDist = Math.abs(framesSins[i][k].freq-tr.tracks[livingTrackInds[0]].freqs[tr.tracks[livingTrackInds[0]].currentIndex]);
                        if (minDist<deltaInHz)
                            trackInd = 0;
                        else
                            trackInd = -1;
                        
                        for (j=1; j<livingTrackInds.length; j++)
                        {
                            tmpDist = Math.abs(framesSins[i][k].freq-tr.tracks[livingTrackInds[j]].freqs[tr.tracks[livingTrackInds[j]].currentIndex]);
                            
                            if (tmpDist<deltaInHz && (trackInd==-1 || tmpDist<minDist))
                            {
                                minDist = tmpDist;
                                trackInd = livingTrackInds[j];
                            }   
                        }
                        
                        if (trackInd>-1)
                        {
                            if (tr.tracks[trackInd].newCandidateInd>-1)
                                bSinAssigneds[tr.tracks[trackInd].newCandidateInd] = false;
                            
                            tr.tracks[trackInd].newCandidate = new Sinusoid(framesSins[i][k]);
                            tr.tracks[trackInd].newCandidateInd = k;
                            
                            bSinAssigneds[k] = true; //The sinusoid might be assigned to an existing track provided that a closer sinusoid is not found
                        }
                        else
                            bSinAssigneds[k] = false; //This is the birth of a new track since it does not match any existing tracks
                    }
                    
                    //Here is the actual assignment of sinusoids to existing tracks
                    newTotalLiving = 0;
                    for (j=0; j<livingTrackInds.length; j++)
                    {
                        if (tr.tracks[livingTrackInds[j]].newCandidate != null)
                            newTotalLiving++;
                    }
                    for (k=0; k<bSinAssigneds.length; k++)
                    {
                        if (!bSinAssigneds[k])
                            newTotalLiving++;
                    }
                    
                    newLivingTrackInds = new int[newTotalLiving];
                    
                    tmpInd = 0;
                    for (j=0; j<livingTrackInds.length; j++)
                    {
                        if (tr.tracks[livingTrackInds[j]].newCandidate != null)
                        {
                            tr.tracks[livingTrackInds[j]].add(times[i], tr.tracks[livingTrackInds[j]].newCandidate);
                            tr.tracks[livingTrackInds[j]].bLiving = true;
                            
                            newLivingTrackInds[tmpInd] = livingTrackInds[j];
                            tmpInd++;
                        }
                    }
                    
                    //Deaths: Kill tracks that are not assigned any new sinusoid
                    //No need to do anything since at the end all tracks will be dead:)
                    
                    //Births: Create new tracks from sinusoids that are not assigned to existing tracks
                    for (k=0; k<bSinAssigneds.length; k++)
                    {
                        if (!bSinAssigneds[k])
                        {
                            tr.add(new SinusoidalTrack(times[i], framesSins[i][k]));
                            newLivingTrackInds[tmpInd] = tr.currentIndex;
                            tmpInd++;
                        }
                    }

                    //Update livingTrackInds
                    livingTrackInds = new int[newLivingTrackInds.length];
                    System.arraycopy(newLivingTrackInds, 0, livingTrackInds, 0, newLivingTrackInds.length);
                    
                    //System.out.println("Track generation using frame " + String.valueOf(i+1) + " of " + String.valueOf(numFrames));
                } 
            }
        }
        
        SinusoidalTracks tr2 = new SinusoidalTracks(tr.currentIndex+1);
        for (i=0; i<tr.currentIndex+1; i++)  
            tr2.add(tr.tracks[i]);
        
        return tr2;
    }
}
