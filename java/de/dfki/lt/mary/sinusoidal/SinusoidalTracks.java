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
 */
public class SinusoidalTracks {
    public SinusoidalTrack [] tracks;
    public int totalTracks;
    public int currentIndex;
    public int fs; //Sampling rate in Hz, you can change this using setSamplingRate to synthesize speech at a different sampling rate
    public float origDur; //Original duration of the signal modeled by sinusoidal tracks in seconds

    public SinusoidalTracks(int len, int samplingRate)
    {
        initialize(len, samplingRate);
    }
    
    public SinusoidalTracks(SinusoidalTracks sinTrks)
    {
        initialize(sinTrks.totalTracks, sinTrks.fs);
        copy(sinTrks);
    }
    
    public void setSamplingRate(int samplingRate)
    {
        fs = samplingRate;
    }
    
    public void initialize(int len, int samplingRate)
    {
        if (len>0)
        {
            totalTracks = len;
            tracks = new SinusoidalTrack[totalTracks];
        }
        else
        {
            totalTracks = 0;
            tracks = null;
        }
        
        currentIndex = -1;
        origDur = 0.0f;
        
        setSamplingRate(samplingRate);
    }
    
    // Copy part of the existing tracks in srcTracks into the current tracks
    //  starting from startSinIndex and ending at endSinIndex
    //  including startSinIndex and endSinIndex
    public void copy(SinusoidalTracks srcTracks, int startTrackIndex, int endTrackIndex)
    {
        if (startTrackIndex<0)
            startTrackIndex=0;
        if (endTrackIndex<0)
            endTrackIndex=0;
        
        if (endTrackIndex>srcTracks.totalTracks-1)
            endTrackIndex=srcTracks.totalTracks-1;
        if (startTrackIndex>endTrackIndex)
            startTrackIndex = endTrackIndex;
        
        if (totalTracks<endTrackIndex-startTrackIndex+1)
            initialize(endTrackIndex-startTrackIndex+1, srcTracks.fs);
        
        if (totalTracks>0)
        {
            for (int i=startTrackIndex; i<=endTrackIndex; i++)
            {
                tracks[i] = new SinusoidalTrack(srcTracks.tracks[i].totalSins);
                tracks[i].copy(srcTracks.tracks[i]);
            }
            
            currentIndex = endTrackIndex-startTrackIndex;
            
            if (srcTracks.origDur>origDur)
                origDur = srcTracks.origDur;
        }   
    }
    
    // Copy existing tracks (srcTracks) into the current tracks
    public void copy(SinusoidalTracks srcTracks)
    {
        copy(srcTracks, 0, srcTracks.totalTracks-1);
    }
    
    //Add a new track to the tracks
    public void add(SinusoidalTrack track)
    {
        if (currentIndex+1>=totalTracks) //Expand the current track twice its length and then add
        {
            if (totalTracks>0)
            {
                SinusoidalTracks tmpTracks = new SinusoidalTracks(this);
                if (tmpTracks.totalTracks<10)
                    initialize(2*tmpTracks.totalTracks, fs);
                else if (tmpTracks.totalTracks<100)
                    initialize(tmpTracks.totalTracks+20, fs);
                else if (tmpTracks.totalTracks<1000)
                    initialize(tmpTracks.totalTracks+200, fs);
                else
                    initialize(tmpTracks.totalTracks+2000, fs);
                
                this.copy(tmpTracks);
            }
            else
                initialize(1, fs);
        }

        currentIndex++;

        tracks[currentIndex] = new SinusoidalTrack(1);
        tracks[currentIndex].copy(track);
        
        if (origDur<track.times[track.totalSins-1])
            origDur = track.times[track.totalSins-1];
    }
    
    public void add(float time, Sinusoid [] sins, int state)
    {
        for (int i=0; i<sins.length; i++)
        {
            SinusoidalTrack tmpTrack = new SinusoidalTrack(time, sins[i], state);
            add(tmpTrack);
            
            if (time>origDur)
                origDur = time;
        }
    }
    
    //Update parameters of <index>th track
    public void update(int index, SinusoidalTrack track)
    {
        if (index<totalTracks)          
            tracks[index].copy(track);
    }
    
    public void getTrackStatistics()
    {
        getTrackStatistics(-1.0f, -1.0f);
    }
    
    public void getTrackStatistics(float windowSizeInSeconds, float skipSizeInSeconds)
    {
        int longest;
        double average;
        int numShorts;
        int shortLim = 5;
        
        int numLongs;
        int longLim = 15;
        
        int i, j;
        
        longest = 0;
        numShorts = 0;
        numLongs = 0;
        average = 0.0;
        for (i=0; i<totalTracks; i++)
        {
            if (tracks[i].totalSins>longest)
                longest = tracks[i].totalSins;
            
            if (tracks[i].totalSins<shortLim)
                numShorts++;
            
            if (tracks[i].totalSins>longLim)
                numLongs++;
            
            average += tracks[i].totalSins;
        }
        
        average /= totalTracks;
        
        System.out.println("Total tracks = " + String.valueOf(totalTracks));
        if (windowSizeInSeconds>0 && skipSizeInSeconds>0)
            System.out.println("Longest track = " + String.valueOf(longest) + " ("+ String.valueOf(longest*skipSizeInSeconds+0.5*windowSizeInSeconds) + " sec.)");
        else
            System.out.println("Longest track = " + String.valueOf(longest));
        
        if (windowSizeInSeconds>0 && skipSizeInSeconds>0)
            System.out.println("Mean track length = " + String.valueOf(average) + " ("+ String.valueOf(average*skipSizeInSeconds+0.5*windowSizeInSeconds) + " sec.)");
        else
            System.out.println("Mean track length = " + String.valueOf(average));
            
        System.out.println("Total tracks shorter than " + String.valueOf(shortLim) + " speech frames = " + String.valueOf(numShorts));
        System.out.println("Total tracks longer than " + String.valueOf(longLim) + " speech frames = " + String.valueOf(numLongs));
 
        for (i=0; i<totalTracks; i++)
            tracks[i].getStatistics(true, true, fs);
    }
    
    public float getOriginalDuration()
    {
        return origDur;
    }
}
