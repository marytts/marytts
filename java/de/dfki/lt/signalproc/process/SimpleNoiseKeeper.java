/**
 * Copyright 2004-2006 DFKI GmbH.
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

package de.dfki.lt.signalproc.process;

import java.io.File;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import de.dfki.lt.signalproc.window.Window;
import de.dfki.lt.signalproc.util.AudioDoubleDataSource;
import de.dfki.lt.signalproc.util.BufferedDoubleDataSource;
import de.dfki.lt.signalproc.util.DDSAudioInputStream;

/**
 * A simple implementation of a separator of periodic and aperiodic components, based on an attenuation function
 * for signals below a given threshold.
 * This class provides the aperiodic (noise) components, as the exact complement of @see SimpleNoiseRemover.
 * @author Marc Schr&ouml;der
 *
 */
public class SimpleNoiseKeeper extends PolarFrequencyProcessor
{
    protected double threshold;

    public SimpleNoiseKeeper(int fftSize, double threshold)
    {
        super(fftSize);
        this.threshold = threshold;
    }

    /**
     * Perform the attenuation of low-intensity frequency components.
     * @param r amplitude of FFT
     * @param phi phase of FFT
     */
    protected void processPolar(double[] r, double[] phi)
    {
        int halfWinLength = r.length / 2;
        for (int i=0; i<r.length; i++) {
            double rNorm = r[i] / halfWinLength;
            double factor = rNorm / (rNorm+threshold);
            //System.out.println("Threshold "+threshold+", rNorm "+rNorm+", factor "+factor);
            r[i] *= 1 - factor;
        }
    }
    
    public static void main(String[] args) throws Exception
    {
        for (int i=0; i<args.length; i++) {
            AudioInputStream inputAudio = AudioSystem.getAudioInputStream(new File(args[i]));
            int samplingRate = (int)inputAudio.getFormat().getSampleRate();
            AudioDoubleDataSource signal = new AudioDoubleDataSource(inputAudio);
            int frameLength = Integer.getInteger("signalproc.simplenoisekeeper.framelength", 1024).intValue();
            double threshold = Double.parseDouble(System.getProperty("signalproc.simplenoisekeeper.threshold", "50.0"));
            FrameOverlapAddSource foas = new FrameOverlapAddSource(signal, Window.HANN, true, frameLength, samplingRate, new SimpleNoiseKeeper(frameLength, threshold));
            DDSAudioInputStream outputAudio = new DDSAudioInputStream(new BufferedDoubleDataSource(foas), inputAudio.getFormat());
            String outFileName = args[i].substring(0, args[i].length()-4) + "_noiseonly.wav";
            AudioSystem.write(outputAudio, AudioFileFormat.Type.WAVE, new File(outFileName));
        }

    }

}
