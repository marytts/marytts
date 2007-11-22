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

import java.io.File;
import java.io.IOException;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;

import de.dfki.lt.mary.util.FileUtils;
import de.dfki.lt.signalproc.util.BufferedDoubleDataSource;
import de.dfki.lt.signalproc.util.DDSAudioInputStream;
import de.dfki.lt.signalproc.util.MathUtils;

/**
 * @author oytun.turk
 *
 * All tester classes should be derived from this baseline class
 * 
 */
public class BaseTester {
    public static float DEFAULT_AMP = 0.8f;
    public static float DEFAULT_DUR = 1.0f;
    public static int DEFAULT_FS = 16000;
    public double [] signal;
    public int [] pitchMarks;
    public int fs;
    
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
}
