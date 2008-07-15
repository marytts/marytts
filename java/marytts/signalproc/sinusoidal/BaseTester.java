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

import java.io.File;
import java.io.IOException;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;

import marytts.signalproc.analysis.F0ReaderWriter;
import marytts.util.data.BufferedDoubleDataSource;
import marytts.util.data.audio.DDSAudioInputStream;
import marytts.util.io.FileUtils;
import marytts.util.math.MathUtils;


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
    public static float DEFAULT_WINDOW_SIZE_FOR_PITCH_CONTOUR = 0.020f;
    public static float DEFAULT_SKIP_SIZE_FOR_PITCH_CONTOUR = 0.010f;
    public double [] signal;
    public int [] pitchMarks;
    public double [] f0s; 
    public int fs;
    public float ws; //Window size in seconds
    public float ss; //Skip size in seconds
    
    public BaseTester()
    {
        ws = DEFAULT_WINDOW_SIZE_FOR_PITCH_CONTOUR;
        ss = DEFAULT_SKIP_SIZE_FOR_PITCH_CONTOUR;
    }
    
    public void write(String outWavFile, String outPtcFile) throws IOException
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
            
            if (pitchMarks != null && (outPtcFile!=null || outPtcFile!=""))
            {
                F0ReaderWriter.write_pitch_file(outPtcFile, f0s, ws, ss, fs);
                //FileUtils.writeToBinaryFile(pitchMarks, outPtcFile); //Pitch mark file
            }
 
        }
    }
}
