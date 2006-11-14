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

package de.dfki.lt.signalproc.analysis;

import java.io.File;
import java.util.Arrays;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import de.dfki.lt.signalproc.FFT;
import de.dfki.lt.signalproc.display.FunctionGraph;
import de.dfki.lt.signalproc.display.SignalGraph;
import de.dfki.lt.signalproc.filter.FIRFilter;
import de.dfki.lt.signalproc.window.Window;
import de.dfki.lt.util.ArrayUtils;
import de.dfki.lt.signalproc.util.AudioDoubleDataSource;
import de.dfki.lt.signalproc.util.Defaults;
import de.dfki.lt.signalproc.util.DoubleDataSource;
import de.dfki.lt.signalproc.util.MathUtils;

/**
 * @author Marc Schr&ouml;der
 *
 */
public class LPCAnalyser extends FrameBasedAnalyser
{
    public LPCAnalyser(DoubleDataSource signal, int framelength, int samplingRate)
    {
        this(signal, Window.get(Defaults.getWindowType(), framelength), framelength, samplingRate);
    }

    public LPCAnalyser(DoubleDataSource signal, int framelength, int frameShift, int samplingRate)
    {
        this(signal, Window.get(Defaults.getWindowType(),framelength), frameShift, samplingRate);
    }

    public LPCAnalyser(DoubleDataSource signal, Window window, int frameShift, int samplingRate)
    {
        super(signal, window, frameShift, samplingRate);
    }
    
    /**
     * Apply this FrameBasedAnalyser to the given data.
     * @param frame the data to analyse, which must be of the length prescribed by this
     * FrameBasedAnalyser, i.e. by @see{#getFrameLengthSamples()}.
     * @return an LPCoeffs object representing the lpc coefficients and gain factor of the frame.
     * @throws IllegalArgumentException if frame does not have the prescribed length 
     */
    public Object analyse(double[] frame)
    {
        if (frame.length != getFrameLengthSamples())
            throw new IllegalArgumentException("Expected frame of length " + getFrameLengthSamples()
                    + ", got " + frame.length);
        return calcLPC(frame);
    }

    /**
     * Calculate LPC parameters for a given input signal, with the default
     * prediction order as defined by the System property 
     * <code>signalproc.lpcorder</code> (defaults to 24).
     * @param x input signal
     * @param p prediction order
     * @return an LPCoeffs object encapsulating the LPC coefficients, a = [1, -a_1, -a_2, ... -a_p],
     * and the gain factor
     */
    public static LPCoeffs calcLPC(double[] x) {
        int p = Integer.getInteger("signalproc.lpcorder", 24).intValue();
        return calcLPC(x, p);
    }

    /**
     * Calculate LPC parameters for a given input signal.
     * @param x input signal
     * @param p prediction order
     * @return an LPCoeffs object encapsulating the LPC coefficients, a = [1, -a_1, -a_2, ... -a_p],
     * and the gain factor
     */
    public static LPCoeffs calcLPC(double[] x, int p)
    {
        double[] autocorr = FFT.autoCorrelateWithZeroPadding(x);
        double[] r = ArrayUtils.subarray(autocorr, autocorr.length/2, p+1);
        double[] coeffs = MathUtils.levinson(r, p);
        // gain factor:
        double g = Math.sqrt(MathUtils.sum(MathUtils.multiply(coeffs, r)));
        return new LPCoeffs(coeffs, g);
    }

    public static void main(String[] args) throws Exception
    {
        int windowSize = Defaults.getWindowSize();
        int windowType = Defaults.getWindowType();
        int fftSize = Defaults.getFFTSize();
        int frameShift = Defaults.getFrameShift();
        int p = Integer.getInteger("signalproc.lpcorder", 24).intValue();
        int pre = p;
        AudioInputStream inputAudio = AudioSystem.getAudioInputStream(new File(args[0]));
        int samplingRate = (int)inputAudio.getFormat().getSampleRate();
        AudioDoubleDataSource signal = new AudioDoubleDataSource(inputAudio);
        double[] signalData = signal.getAllData();
        int position = 6000;
        Window w = Window.get(windowType, windowSize);
        double[] sliceToAnalyse = w.apply(signalData, position);
        LPCoeffs lpc = calcLPC(sliceToAnalyse, p);
        double g_db = 2*MathUtils.db(lpc.getGain()); // *2 because g is signal, not energy
        double[] signalPowerSpectrum = FFT.computeLogPowerSpectrum(sliceToAnalyse);
        double[] a = lpc.getOneMinusA();
        double[] fftA = new double[fftSize];
        System.arraycopy(a, 0, fftA, 0, a.length);
        double[] lpcPowerSpectrum = FFT.computeLogPowerSpectrum(fftA);

        double offset = 0; //2*MathUtils.db(2./Nfft);
        for (int i=0; i<lpcPowerSpectrum.length; i++) {
            lpcPowerSpectrum[i] = -lpcPowerSpectrum[i] + offset + g_db;
        }
        for (int i=0; i<signalPowerSpectrum.length; i++) {
            signalPowerSpectrum[i] += offset;
        }
        double[] lsp = LineSpectralFrequencies.lpc2lsf(a,1);
        System.out.println("Line spectral frequencies:");
        for (int i=0; i<lsp.length; i++) {
            System.out.println(i+": "+lsp[i]+" = "+lsp[i]/(2*Math.PI)*samplingRate);
        }
        
        double deltaF = (double) samplingRate / fftSize;
        FunctionGraph signalSpectrumGraph = new FunctionGraph(0, deltaF, signalPowerSpectrum);
        signalSpectrumGraph.showInJFrame("signal spectrum", true, true);
        FunctionGraph lpcSpectrumGraph = new FunctionGraph(0, deltaF, lpcPowerSpectrum);
        lpcSpectrumGraph.showInJFrame("lpc spectrum", true, true);
        
        FIRFilter whiteningFilter = new FIRFilter(a);
        double[] testSlice = new double[fftSize+p];
        System.arraycopy(signalData, position-p, testSlice, 0, testSlice.length);
        double[] residuum = whiteningFilter.apply(testSlice);
        double[] usableSignal = ArrayUtils.subarray(testSlice, p, fftSize);
        double[] usableResiduum = ArrayUtils.subarray(residuum, p, fftSize);
        FunctionGraph signalGraph = new SignalGraph(usableSignal, samplingRate);
        signalGraph.showInJFrame("signal", true, true);
        FunctionGraph residuumGraph = new SignalGraph(usableResiduum, samplingRate);
        residuumGraph.showInJFrame("residual", true, true);
        double predictionGain = MathUtils.db(MathUtils.sum(MathUtils.multiply(usableSignal, usableSignal))
                / MathUtils.sum(MathUtils.multiply(usableResiduum, usableResiduum)));
        System.out.println("Prediction gain: " + predictionGain + " dB");
    }
    
    public static class LPCoeffs
    {
        protected double[] oneMinusA;
        protected double gain;

        /**
         * Create a set of LPC coefficients
         * @param oneMinusA the coefficients, a = [1, -a_1, -a_2, ... -a_p], where p = prediction order
         * @param gain the gain factor, i.e. the square root of the total energy or the prediction error.
         */
        public LPCoeffs(double[] oneMinusA, double gain)
        {
            this.oneMinusA = oneMinusA;
            this.gain = gain;
        }
        // The following are the preferred ways to read/write the lpc coefficients:
        public final double getOneMinusA(int i) { return oneMinusA[i]; }
        public double getA(int i) { return -oneMinusA[i+1]; }
        public void setOneMinusA(int i, double value) { oneMinusA[i] = value; }
        public void setA(int i, double value) { oneMinusA[i+1] = -value; }
        
        /**
         * Return a clone of the internal representation of the LPC coefficients.
         * @return
         */
        public double[] getOneMinusA()
        {
            return (double[]) oneMinusA.clone();
        }
        
        public void setOneMinusA(double[] oneMinusA)
        {
            this.oneMinusA = (double[]) oneMinusA.clone();
        }
        
        /**
         * Get the gain, i.e. the square root of the total energy or the prediction error.
         * @return the gain
         */
        public double getGain()
        {
            return gain;
        }
        
        public void setGain(double gain)
        {
            this.gain = gain;
        }
        
        public int getOrder()
        {
            return oneMinusA.length - 1;
        }
        
        public double[] getA()
        {
            double[] a = new double[getOrder()];
            for (int i=0; i<a.length; i++)
                a[i] = -oneMinusA[i+1];
            return a;
        }
        
        public void setA(double[] a)
        {
            this.oneMinusA = new double[a.length+1];
            oneMinusA[0] = 1;
            for (int i=0; i<a.length; i++)
                oneMinusA[i+1] = -a[i];
        }
        
        /**
         * Convert these LPC coefficients into Line spectral frequencies.
         * @return the LSFs.
         */
        public double[] getLSF()
        {
            return LineSpectralFrequencies.lpc2lsf(oneMinusA, 1);
        }
        
        public void setLSF(double[] lsf)
        {
            oneMinusA = LineSpectralFrequencies.lsf2lpc(lsf);
        }
    }
}
