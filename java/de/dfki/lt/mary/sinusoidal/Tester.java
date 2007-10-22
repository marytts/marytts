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

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import de.dfki.lt.mary.util.FileUtils;
import de.dfki.lt.signalproc.util.BufferedDoubleDataSource;
import de.dfki.lt.signalproc.util.DDSAudioInputStream;
import de.dfki.lt.signalproc.util.MathUtils;

/**
 * @author oytun.turk
 *
 * This class can be used to generate various sinusoid signals and writing them to wav and ptc files
 * to be used in testing the analysis/synthesis algorithms
 * 
 */
public class Tester {
    public static float DEFAULT_AMP = 0.8f;
    public static float DEFAULT_PHASE = 0.0f;
    public static float DEFAULT_DUR = 1.0f;
    public static int DEFAULT_FS = 16000;
    public double [] signal;
    public int [] pitchMarks;
    public int fs;
    
    public Tester(float freqInHz)
    {
        this(freqInHz, DEFAULT_AMP);
    }
    
    public Tester(float freqInHz, float amp)
    {
        this(freqInHz, amp, DEFAULT_PHASE);
    }
    
    public Tester(float freqInHz, float amp, float phaseInDegrees)
    {
        this(freqInHz, amp, phaseInDegrees, DEFAULT_DUR);
    }
    
    public Tester(float freqInHz, float amp, float phaseInDegrees, float durationInSeconds)
    {
        this(freqInHz, amp, phaseInDegrees, durationInSeconds, DEFAULT_FS);
    }
    
    public Tester(Sinusoid sin)
    {
        this(sin, DEFAULT_DUR);
    }
    
    public Tester(Sinusoid sin, float durationInSeconds)
    {
        this(sin, durationInSeconds, DEFAULT_FS);
    }
    
    public Tester(float freqInHz, float amp, float phaseInDegrees, float durationInSeconds, int samplingRateInHz)
    {
        Sinusoid [] tmpSins = new Sinusoid[1];
        tmpSins[0] = new Sinusoid(amp, freqInHz, phaseInDegrees);
        init(tmpSins, durationInSeconds, samplingRateInHz);
    }
    
    public Tester(Sinusoid sin, float durationInSeconds, int samplingRateInHz)
    {
        Sinusoid [] tmpSins = new Sinusoid[1];
        tmpSins[0] = new Sinusoid(sin);
        init(tmpSins, durationInSeconds, samplingRateInHz);
    }
    
    public Tester(Sinusoid [] sinsIn)
    {
        this(sinsIn, DEFAULT_DUR);
    }
    
    public Tester(Sinusoid [] sinsIn, float durationInSeconds)
    {
        this(sinsIn, durationInSeconds, DEFAULT_FS);
    }
    
    public Tester(Sinusoid [] sinsIn, float durationInSeconds, int samplingRateInHz)
    {
        init(sinsIn, durationInSeconds, samplingRateInHz);
    }
    
    //These constructors can be used to create tracks starting and terminating at desired time instants    
    public Tester(Sinusoid [] sinsIn, float [] startTimesInSeconds, float [] endTimesInSeconds)
    {
        this(sinsIn, startTimesInSeconds, endTimesInSeconds, DEFAULT_FS);
    }
    
    public Tester(Sinusoid [] sinsIn, float [] startTimesInSeconds, float [] endTimesInSeconds, int samplingRateInHz)
    {
        init(sinsIn, startTimesInSeconds, endTimesInSeconds, samplingRateInHz);
    }
    //
    
    public void init(Sinusoid [] sinsIn, float durationInSeconds, int samplingRateInHz)
    {
        if (sinsIn.length>0)
        {
            float [] startTimesInSeconds = new float[sinsIn.length];
            float [] endTimesInSeconds = new float[sinsIn.length];
            for (int i=0; i<sinsIn.length; i++)
            {
                startTimesInSeconds[i] = 0.0f;
                endTimesInSeconds[i] = durationInSeconds;
            }
            
            init(sinsIn, startTimesInSeconds, endTimesInSeconds, samplingRateInHz);
        } 
    }
    
    public void init(Sinusoid [] sinsIn, float [] startTimesInSeconds, float [] endTimesInSeconds, int samplingRateInHz)
    {
        fs = samplingRateInHz;
        signal = null;
        pitchMarks = null;
        int i, j;
        
        if (sinsIn!=null)
        {
            assert startTimesInSeconds.length==endTimesInSeconds.length;
            assert sinsIn.length==endTimesInSeconds.length;
            
            float minFreq = 2*fs;
            int minFreqInd = -1;
            for (i=0; i<sinsIn.length; i++)
            {
                if (sinsIn[i].freq>0.0f && sinsIn[i].freq<minFreq)
                {
                    minFreq = sinsIn[i].freq;
                    minFreqInd = i;
                }
            }
            
            int [] startSampleIndices = new int[sinsIn.length];
            int [] endSampleIndices = new int[sinsIn.length];
            
            for (i=0; i<startTimesInSeconds.length; i++)
            {
                if (startTimesInSeconds[i]<0.0f)
                    startTimesInSeconds[i]=0.0f;
                if (endTimesInSeconds[i]<0.0f)
                    endTimesInSeconds[i]=0.0f;
                if (startTimesInSeconds[i]>endTimesInSeconds[i])
                    startTimesInSeconds[i] = endTimesInSeconds[i];
                
                startSampleIndices[i] = (int)(Math.floor(startTimesInSeconds[i]*fs+0.5));
                endSampleIndices[i] = (int)(Math.floor(endTimesInSeconds[i]*fs+0.5));
            }
            
            int minStartSampleIndex = MathUtils.getMin(startSampleIndices);
            int maxEndSampleIndex = MathUtils.getMax(endSampleIndices);
            
            //Create pitch marks by finding the longest period
            int maxT0;
            
            if (minFreqInd>0)
                maxT0 = (int)(Math.floor(fs/minFreq+0.5));
            else //No non-zero Hz sinusoids found, therefore set maxT0 to a fixed number
                maxT0 = (int)Math.floor(0.010f*fs+0.5);
            
            int numPitchMarks = (int)(Math.floor(((double)(maxEndSampleIndex-minStartSampleIndex+1))/maxT0+0.5)) + 1; 
            pitchMarks = new int[numPitchMarks];
            for (i=0; i<numPitchMarks; i++)
                pitchMarks[i] = Math.min(i*maxT0+minStartSampleIndex, maxEndSampleIndex);
            //
            
            if (maxEndSampleIndex>0)
            {
                signal = new double[maxEndSampleIndex+1];
                Arrays.fill(signal, 0.0);

                //Synthesize sinusoids
                for (i=0; i<sinsIn.length; i++)
                {
                    for (j=startSampleIndices[i]; j<=endSampleIndices[i]; j++)
                        signal[j] += sinsIn[i].amp * Math.sin(MathUtils.TWOPI*((j-startSampleIndices[i])*(sinsIn[i].freq/fs) + sinsIn[i].phase/360.0));
                }  
            }
        }
    }
    
    public void write(String outWavFile, String outPmFile) throws IOException
    {
        if (signal != null)
        {
            if (signal!=null && (outWavFile!=null || outWavFile!=""))
            {
                double maxVal = MathUtils.getAbsMax(signal);
                for (int i=0; i<signal.length; i++)
                    signal[i] *= 0.8/maxVal;

                AudioFormat format = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,
                        fs, // samples per second
                        16, // bits per sample
                        1, // mono
                        2, // nr. of bytes per frame
                        fs, // nr. of frames per second
                        true); // big-endian;

                DDSAudioInputStream outputAudio = new DDSAudioInputStream(new BufferedDoubleDataSource(signal), format);
                AudioSystem.write(outputAudio, AudioFileFormat.Type.WAVE, new File(outWavFile));
            }
            
            if (pitchMarks != null && (outPmFile!=null || outPmFile!=""))
                FileUtils.writeToBinaryFile(pitchMarks, outPmFile);
        }
    }
    
    public static void main(String[] args) throws IOException
    {
        int i, numTracks;
        float [] tStarts, tEnds;
        Tester t = null;
        
        //Single sinusoid, tme-invariant
        //t = new Tester(200.0f);
        //

        /*
        //Several sinusoids, time-invariant
        numTracks = 3;
        Sinusoid [] sins = new Sinusoid[numTracks];
        sins[0] = new Sinusoid(100.0f, 120.0f, 0.0f);
        sins[1] = new Sinusoid(25.0f, 700.0f, 0.0f);
        sins[2] = new Sinusoid(6.5f, 4300.0f, 0.0f);
        t = new Tester(sins);
        //
        */
        
        //Fixed sinusoidal track with a gap
        numTracks = 4;
        Sinusoid [] sins = new Sinusoid[numTracks];
        tStarts = new float[numTracks];
        tEnds = new float[numTracks];
        
        sins[0] = new Sinusoid(0.0f, 0.0f, 0.0f);
        tStarts[0] = 0.0f;
        tEnds[0] = 0.1f; 
        sins[1] = new Sinusoid(100.0f, 200.0f, 0.0f);
        tStarts[1] = 0.1f;
        tEnds[1] = 0.2f; 
        sins[2] = new Sinusoid(100.0f, 300.0f, 0.0f);
        tStarts[2] = 0.3f;
        tEnds[2] = 0.4f;
        sins[3] = new Sinusoid(0.0f, 0.0f, 0.0f);
        tStarts[3] = 0.4f;
        tEnds[3] = 0.5f;
        t = new Tester(sins, tStarts, tEnds);
        //
        
        t.write(args[0], args[1]);
        
        int [] pitchMarks = FileUtils.readFromBinaryFile(args[1]);
        for (i=0; i<pitchMarks.length; i++)
            System.out.println(String.valueOf(pitchMarks[i]) + " ");
    }
}
