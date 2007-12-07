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

package de.dfki.lt.signalproc.filter;

import java.io.File;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import de.dfki.lt.signalproc.FFT;
import de.dfki.lt.signalproc.display.FunctionGraph;
import de.dfki.lt.signalproc.display.MultiDisplay;
import de.dfki.lt.signalproc.window.BlackmanWindow;
import de.dfki.lt.signalproc.window.Window;
import de.dfki.lt.signalproc.process.Robotiser;
import de.dfki.lt.signalproc.util.AudioDoubleDataSource;
import de.dfki.lt.signalproc.util.BufferedDoubleDataSource;
import de.dfki.lt.signalproc.util.DDSAudioInputStream;
import de.dfki.lt.signalproc.util.DoubleDataSource;
import de.dfki.lt.signalproc.util.MathUtils;

/**
 * @author Marc Schr&ouml;der
 *
 */
public class LowPassFilter extends FIRFilter {
    public static double DEFAULT_TRANSITIONBANDWIDTH = 0.01;

    /**
     * Create a new lowpass filter with the given normalised cutoff frequency and
     * a default transition band width.
     * @param normalisedCutoffFrequency the cutoff frequency of the lowpass filter,
     * expressed as a fraction of the sampling rate. It must be in the range ]0, 0.5[.
     * For example, with a sampling rate of 16000 Hz and a desired cutoff frequency of 
     * 4000 Hz, the normalisedCutoffFrequency would have to be 0.25. 
     */
    public LowPassFilter(double normalisedCutoffFrequency) {
        this(normalisedCutoffFrequency, DEFAULT_TRANSITIONBANDWIDTH);
    }
    
    /**
     * Create a new lowpass filter with the given normalised cutoff frequency and
     * the given normalised transition band width.
     * @param normalisedCutoffFrequency the cutoff frequency of the lowpass filter,
     * expressed as a fraction of the sampling rate. It must be in the range ]0, 0.5[.
     * For example, with a sampling rate of 16000 Hz and a desired cutoff frequency of 
     * 4000 Hz, the normalisedCutoffFrequency would have to be 0.25.
     * @param  normalisedTransitionBandwidth indicates the desired quality of the filter.
     * The smaller the bandwidth, the more abrupt the cutoff at the cutoff frequency,
     * but also the larger the filter kernel (impulse response) and computationally costly the filter.
     * Usual range of this parameter is [0.002, 0.2].
     */
    public LowPassFilter(double normalisedCutoffFrequency, double normalisedTransitionBandwidth) {
        this(normalisedCutoffFrequency, bandwidth2kernelLength(normalisedTransitionBandwidth));
    }
    
    /**
     * Create a new lowpass filter with the given normalised cutoff frequency and
     * the given length of the filter kernel.
     * @param normalisedCutoffFrequency the cutoff frequency of the lowpass filter,
     * expressed as a fraction of the sampling rate. It must be in the range ]0, 0.5[.
     * For example, with a sampling rate of 16000 Hz and a desired cutoff frequency of 
     * 4000 Hz, the normalisedCutoffFrequency would have to be 0.25.
     * @param  kernelLength length of the filter kernel (the impulse response). The
     * kernel length must be an odd number. The longer
     * the kernel, the sharper the cutoff, i.e. the narrower the transition band. Typical
     * lengths are in the range of 10-1000.
     * @throws IllegalArgumentException if the kernel length is not a positive, odd number,
     * or if normalisedCutoffFrequency is not in the range between 0 and 0.5.
     */
    public LowPassFilter(double normalisedCutoffFrequency, int kernelLength) {
        super();
        if (kernelLength <= 0 || kernelLength%2==0) {
            throw new IllegalArgumentException("Kernel length must be an odd positive number, got " + kernelLength);
        }
        if (normalisedCutoffFrequency <= 0 || normalisedCutoffFrequency >= 0.5) {
            throw new IllegalArgumentException("Normalised cutoff frequency must be between 0 and 0.5, got " + normalisedCutoffFrequency);
        }
        double[] kernel = getKernel(normalisedCutoffFrequency, kernelLength);
        // determine the length of the slices by which the signal will be consumed:
        // this is the distance to the second next power of two, so that the slice
        // will be at least as long as the kernel.
        sliceLength = MathUtils.closestPowerOfTwoAbove(2*kernelLength) - kernelLength;
        initialise(kernel, sliceLength);
    }
    
    /**
     * For a given sampling rate, return the width of the transition band for this
     * filter, in Hertz.
     * @param samplingRate the sampling rate, in Hertz.
     */
    public double getTransitionBandWidth(int samplingRate)
    {
        return samplingRate * kernelLength2bandwidth(impulseResponseLength);
    }

    /**
     * Compute the low-pass filter kernel, using a Blackman window.
     */
    protected static double[] getKernel(double normalisedCutoffFrequency, int kernelLength)
    {
        double[] kernel = new double[kernelLength];
        int m = (kernelLength-1)/2;
        double fc = normalisedCutoffFrequency;
        double sum = 0.;
        Window window = new BlackmanWindow(kernelLength);
        for (int i=0; i<m; i++) {
            kernel[i] = Math.sin(2*Math.PI*fc*(i-m))/(i-m) * window.value(i);
            kernel[kernelLength-i-1] = kernel[i];
            sum += 2*kernel[i];
        }
        kernel[m] = 2*Math.PI*fc;
        sum += kernel[m];
        // Normalise to area 1:
        for (int i=0; i<kernelLength; i++) {
            kernel[i] /= sum;
        }
        return kernel;
    }
    
    /**
     * Convert from normalisedTransitionBandwidth to filter kernel length,
     * using the approximate formula l = 4/bw. 
     * @param normalisedTransitionBandwidth
     * @return the corresponding filter kernel length (guaranteed to be an odd number).
     */
    protected static int bandwidth2kernelLength(double normalisedTransitionBandwidth)
    {
        int l = (int) (4/normalisedTransitionBandwidth);
        // kernel length must be odd:
        if (l%2==0) l++;
        return l;
    }

    /**
     * Convert from filter kernel length to normalisedTransitionBandwidth,
     * using the approximate formula l = 4/bw. 
     * @param kernelLength
     * @return the corresponding normalised transition bandwidth.
     */
    protected static double kernelLength2bandwidth(int kernelLength)
    {
        double bw = (double) 4/kernelLength;
        return bw;
    }
    
    public String toString() { return "Lowpass filter"; }

    public static void main(String[] args) throws Exception
    {
        int cutoffFreq = Integer.valueOf(args[0]).intValue();
        
        AudioInputStream inputAudio = AudioSystem.getAudioInputStream(new File(args[1]));
        int samplingRate = (int)inputAudio.getFormat().getSampleRate();
        AudioDoubleDataSource source = new AudioDoubleDataSource(inputAudio);
        
        double normalisedCutoffFrequency = (double) cutoffFreq / samplingRate;
        LowPassFilter filter = new LowPassFilter(normalisedCutoffFrequency);
        System.err.println("Created " + filter.toString() + " with cutoff frequency " + cutoffFreq + " Hz and transition band width " + ((int)filter.getTransitionBandWidth(samplingRate)) + " Hz");

        // Display the filter kernel and log frequency response:
        double[] fftSignal = new double[filter.transformedIR.length];
        System.arraycopy(filter.transformedIR, 0, fftSignal, 0, filter.transformedIR.length);
        // inverse transform:
        FFT.realTransform(fftSignal, true);
        double[] kernel = new double[filter.impulseResponseLength];
        System.arraycopy(fftSignal, 0, kernel, 0, kernel.length);
        FunctionGraph timeGraph = new FunctionGraph(0, 1, kernel);
        timeGraph.showInJFrame(filter.toString() + " in time domain", true, false);

        double[] powerSpectrum = FFT.computePowerSpectrum_FD(filter.transformedIR);
        for (int i=0; i<powerSpectrum.length; i++) powerSpectrum[i] = MathUtils.db(powerSpectrum[i]);
        FunctionGraph freqGraph = new FunctionGraph(0, (double)samplingRate/filter.transformedIR.length, powerSpectrum);
        freqGraph.showInJFrame(filter.toString() + " log frequency response", true, false);

        // Filter the test signal and display it:
        DoubleDataSource filteredSignal = filter.apply(source);
        //MultiDisplay display = new MultiDisplay(filteredSignal.getAllData(), samplingRate, filter.toString() + " at " + cutoffFreq + " Hz applied to " + args[1],
        //        MultiDisplay.DEFAULT_WIDTH, MultiDisplay.DEFAULT_HEIGHT);
    
        DDSAudioInputStream outputAudio = new DDSAudioInputStream(new BufferedDoubleDataSource(filteredSignal), source.getAudioFormat());
        String outFileName = args[1].substring(0, args[1].length()-4) + "_lpf.wav";
        AudioSystem.write(outputAudio, AudioFileFormat.Type.WAVE, new File(outFileName));
    }
}
