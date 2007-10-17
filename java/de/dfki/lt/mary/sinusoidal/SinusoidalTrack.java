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
 * A sinusoidal track is a collection of matched (amplitude,frequency,phase) triplets 
 * which represents a "relatively" stationary time-frequency partition of a signal
 * 
 */
public class SinusoidalTrack {

    float [] amps; //Amplitudes of the sinusoids
    float [] freqs; //Frequencies of the sinusoids in Hz
    float [] phases; //Phases of the sinusoids in radians
    float [] times; //Times of the sinusoids in seconds
    int [] states; //State of the track for a given index (one of the flags below, by default: LIVING
    
    public static int ACTIVE = 0; //The track has been turned on in a previous frame and has not been turned off until now
    public static int TURNED_ON = 1; //The track is turned on at the current time instant
    public static int TURNED_OFF = 2; //The track is turned off until next turning on event
    
    int currentIndex;
    int totalSins;
    
    //These two parameters are used for keeping temporary information 
    // on new sinusoid candidates to be appended to the current track during track generation
    Sinusoid newCandidate;
    int newCandidateInd;
    //
    
    public SinusoidalTrack(int len)
    {
        initialize(len);
    }
    
    //Create a track from a single sinusoid
    public SinusoidalTrack(float time, Sinusoid sin, int state)
    {
        add(time, sin.amp, sin.freq, sin.phase, state);
    }
    
  //Create a track from a track
    public SinusoidalTrack(SinusoidalTrack trk)
    {
        initialize(trk.totalSins);
        copy(trk);
    }
    
    public void initialize(int len)
    {
        if (len>0)
        {
            totalSins = len;
            times = new float[totalSins];
            amps = new float[totalSins];
            freqs = new float[totalSins];
            phases = new float[totalSins];
            states = new int[totalSins];
        }
        else
        {
            totalSins = 0;
            times = null;
            amps = null;
            freqs = null;
            phases = null;
            states = null;
        }
        
        currentIndex = -1;
        newCandidate = null;
        newCandidateInd = -1;
    }
    
    // Copy part of the existing track parameters in srcTrack into the current track 
    //  starting from startSinIndex and ending at endSinIndex
    //  including startSinIndex and endSinIndex
    public void copy(SinusoidalTrack srcTrack, int startSinIndex, int endSinIndex)
    {
        if (startSinIndex<0)
            startSinIndex=0;
        if (endSinIndex<0)
            endSinIndex=0;
        
        if (endSinIndex>srcTrack.totalSins-1)
            endSinIndex=srcTrack.totalSins-1;
        if (startSinIndex>endSinIndex)
            startSinIndex = endSinIndex;
        
        if (totalSins<endSinIndex-startSinIndex+1)
            initialize(endSinIndex-startSinIndex+1);
        
        if (totalSins>0)
        {
            System.arraycopy(srcTrack.times, startSinIndex, this.times, 0, endSinIndex-startSinIndex+1);
            System.arraycopy(srcTrack.amps, startSinIndex, this.amps, 0, endSinIndex-startSinIndex+1);
            System.arraycopy(srcTrack.freqs, startSinIndex, this.freqs, 0, endSinIndex-startSinIndex+1);
            System.arraycopy(srcTrack.phases, startSinIndex, this.phases, 0, endSinIndex-startSinIndex+1);
            System.arraycopy(srcTrack.states, startSinIndex, this.states, 0, endSinIndex-startSinIndex+1);
            currentIndex = endSinIndex-startSinIndex;
        }
    }
    
    // Copy an existing track (srcTrack) into the current track
    public void copy(SinusoidalTrack srcTrack)
    {
        copy(srcTrack, 0, srcTrack.totalSins-1);
    }
    
    //Add a new sinusoid to the track
    public void add(float time, float amp, float freq, float phase, int state)
    {
        if (currentIndex+1>=totalSins) //Expand the current track to twice its length and then add
        {
            SinusoidalTrack tmpTrack = new SinusoidalTrack(totalSins);
            if (totalSins>0)
            {
                tmpTrack.copy(this);

                initialize(tmpTrack.totalSins+1);

                this.copy(tmpTrack);
            }
            else
                initialize(1);
        }
        
        currentIndex++;

        times[currentIndex] = time;
        amps[currentIndex] = amp;
        freqs[currentIndex] = freq;
        phases[currentIndex] = phase;
        states[currentIndex] = state;
    }
    
    public void add(float time, Sinusoid newSin, int state)
    {
        add(time, newSin.amp, newSin.freq, newSin.phase, state);
    }
    
    //Update parameters of <index>th sinusoid in track
    public void update(int index, int time, float amp, float freq, float phase, int state)
    {
        if (index<totalSins)
        {            
            times[index] = time;
            amps[index] = amp;
            freqs[index] = freq;
            phases[index] = phase;
            states[index] = state;
        }
    }
    
    public void resetCandidate()
    {
        newCandidate = null;
        newCandidateInd = -1;
    }
}
