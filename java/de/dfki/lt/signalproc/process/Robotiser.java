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
import de.dfki.lt.signalproc.util.DoubleDataSource;
import de.dfki.lt.signalproc.util.SignalProcUtils;

/**
 * Create a robot-like impression on the output, by setting all phases to zero in each frame. This effectively
 * creates a pitch equalling the output frame shift.
 * @author Marc Schr&ouml;der
 *
 */
public class Robotiser extends FrameOverlapAddSource
{
    /**
     * @param inputSource
     * @param frameLength
     * @param samplingRate
     * @param rateChangeFactor the factor by which to speed up or slow down the source.
     * Values greater than one will speed up, values smaller than one will slow down the original.
     */
    public Robotiser(DoubleDataSource inputSource, int samplingRate, float amount)
    {
        //int frameLength = Integer.getInteger("signalproc.robotiser.framelength", 256).intValue();
        int frameLength = SignalProcUtils.getDFTSize(samplingRate);
        initialise(inputSource, Window.HANN, true, frameLength, samplingRate, new PhaseRemover(frameLength, amount));
    }
    
    public Robotiser(DoubleDataSource inputSource, int samplingRate)
    {
        this(inputSource, samplingRate, 1.0f);
    }
    
    public static class PhaseRemover extends PolarFrequencyProcessor
    {
        public PhaseRemover(int fftSize, double amount)
        {
            super(fftSize, amount);
        }
        
        public PhaseRemover(int fftSize)
        {
            this(fftSize, 1.0);
        }

        /**
         * Perform the random manipulation.
         * @param r
         * @param phi
         */
        protected void processPolar(double[] r, double[] phi)
        {
            for (int i=0; i<phi.length; i++) {
                phi[i] = 0;
            }
        }

    }
    
    public static void main(String[] args) throws Exception
    {
        for (int i=0; i<args.length; i++) {
            AudioInputStream inputAudio = AudioSystem.getAudioInputStream(new File(args[i]));
            int samplingRate = (int)inputAudio.getFormat().getSampleRate();
            AudioDoubleDataSource signal = new AudioDoubleDataSource(inputAudio);
            Robotiser pv = new Robotiser(signal, samplingRate);
            DDSAudioInputStream outputAudio = new DDSAudioInputStream(new BufferedDoubleDataSource(pv), inputAudio.getFormat());
            String outFileName = args[i].substring(0, args[i].length()-4) + "_robotised.wav";
            AudioSystem.write(outputAudio, AudioFileFormat.Type.WAVE, new File(outFileName));
        }
    }
}
