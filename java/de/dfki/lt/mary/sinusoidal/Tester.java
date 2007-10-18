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
    
    public void init(Sinusoid [] sinsIn, float durationInSeconds, int samplingRateInHz)
    {
        fs = samplingRateInHz;
        signal = null;
        pitchMarks = null;
        int i, j;
        
        if (sinsIn!=null)
        {
            int totalSamples = (int)(Math.floor(durationInSeconds*fs+0.5));
            
            if (totalSamples>0)
            {
                signal = new double[totalSamples];
                Arrays.fill(signal, 0.0);
                
              //Create pitch marks by finding the longest period
                float minFreq = sinsIn[0].freq;
                for (i=1; i<sinsIn.length; i++)
                {
                    if (sinsIn[i].freq<minFreq)
                        minFreq = sinsIn[i].freq;
                }

                int maxT0 = (int)(Math.floor(fs/minFreq+0.5));
                int numPitchMarks = (int)(Math.floor(((double)totalSamples)/maxT0+0.5)) + 1; 
                pitchMarks = new int[numPitchMarks];
                for (i=0; i<numPitchMarks; i++)
                    pitchMarks[i] = Math.min(i*maxT0, totalSamples);
                //
                
                //Synthesize sinusoids
                for (i=0; i<sinsIn.length; i++)
                {
                    for (j=0; j<totalSamples; j++)
                        signal[j] += sinsIn[i].amp * Math.sin(MathUtils.TWOPI*(j*(sinsIn[i].freq/fs) + sinsIn[i].phase/360.0));
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
        int i;
        Tester t = null;
        
        //t = new Tester(200.0f);
        
        Sinusoid [] sins = new Sinusoid[3];
        sins[0] = new Sinusoid(100.0f, 120.0f, 0.0f);
        sins[1] = new Sinusoid(25.0f, 700.0f, 0.0f);
        sins[2] = new Sinusoid(6.5f, 4300.0f, 0.0f);
        t = new Tester(sins);
        
        t.write(args[0], args[1]);
        
        int [] pitchMarks = FileUtils.readFromBinaryFile(args[1]);
        for (i=0; i<pitchMarks.length; i++)
            System.out.println(String.valueOf(pitchMarks[i]) + " ");
    }
}
