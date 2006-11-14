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

package de.dfki.lt.signalproc.display;

import java.io.File;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import de.dfki.lt.signalproc.AudioUtils;
import de.dfki.lt.signalproc.FFT;
import de.dfki.lt.signalproc.util.MathUtils;

/**
 * @author Marc Schr&ouml;der
 *
 */
public class SignalSpectrum  extends FunctionGraph
{
    public SignalSpectrum(AudioInputStream ais) {
        this(ais, DEFAULT_WIDTH, DEFAULT_HEIGHT);
    }
            
    public SignalSpectrum(AudioInputStream ais, int width, int height) {
        super();
        if (!ais.getFormat().getEncoding().equals(AudioFormat.Encoding.PCM_SIGNED)) {
            ais = AudioSystem.getAudioInputStream(AudioFormat.Encoding.PCM_SIGNED, ais);
        }
        if (ais.getFormat().getChannels() > 1) {
            throw new IllegalArgumentException("Can only deal with mono audio signals");
        }
        int samplingRate = (int) ais.getFormat().getSampleRate();
        double[] signal = AudioUtils.getSamplesAsDoubleArray(ais);
        initialise(signal, samplingRate, width, height);
    }
    
    public SignalSpectrum(final double[] signal, int samplingRate)
    {
        this(signal, samplingRate, DEFAULT_WIDTH, DEFAULT_HEIGHT);
    }
    
    public SignalSpectrum(final double[] signal, int samplingRate, int width, int height)
    {
        initialise(signal, samplingRate, width, height);
    }
    
    protected void initialise(final double[] signal, int samplingRate, int width, int height)
    {
        int N = signal.length;
        if (!MathUtils.isPowerOfTwo(N)) {
            N = MathUtils.closestPowerOfTwoAbove(N);
        }
        double[] ar = new double[N];
        System.arraycopy(signal, 0, ar, 0, signal.length);
        // Transform:
        FFT.realTransform(ar, false);
        double[] freqs = FFT.computeAmplitudeSpectrum_FD(ar);
        process(freqs);
        double deltaF = (double) samplingRate / N;
        super.initialise(width, height, 0, deltaF, freqs);
    }

    /**
     * Subclass can use this to compute power or log spectrum
     * @param freqs the frequencies that come out of the FFT.
     */
    protected void process(double[] freqs)
    {
    }
    
    public static void main(String[] args) throws Exception
    {
        for (int i=0; i<args.length; i++) {
            AudioInputStream ais = AudioSystem.getAudioInputStream(new File(args[i]));
            SignalSpectrum signalSpectrum = new SignalSpectrum(ais);
            signalSpectrum.showInJFrame(args[i], true, false);
        }
    }
    
}
