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
import de.dfki.lt.signalproc.util.MathUtils.Complex;
import de.dfki.lt.signalproc.util.SignalProcUtils;

/**
 * @author Marc Schr&ouml;der
 *
 */
public class LPCAnalyser extends FrameBasedAnalyser
{    
    public static int lpOrder = 0;
    public static float preemphasisCoefficient = 0.0f;
    
    public LPCAnalyser(DoubleDataSource signal, int framelength, int samplingRate)
    {
        this(signal, Window.get(Defaults.getWindowType(), framelength), framelength, samplingRate);
    }

    public LPCAnalyser(DoubleDataSource signal, int framelength, int frameShift, int samplingRate)
    {
        this(signal, Window.get(Defaults.getWindowType(),framelength), frameShift, samplingRate);
    }
    
    public LPCAnalyser(DoubleDataSource signal, int framelength, int frameShift, int samplingRate, int order, int windowType)
    {
        this(signal, Window.get(windowType,framelength), frameShift, samplingRate, order);
    }
    
    public LPCAnalyser(DoubleDataSource signal, int framelength, int frameShift, int samplingRate, int order, int windowType, float preCoef)
    {
        this(signal, Window.get(windowType,framelength), frameShift, samplingRate, order, preCoef);
    }

    public LPCAnalyser(DoubleDataSource signal, Window window, int frameShift, int samplingRate)
    {
        this(signal, window, frameShift, samplingRate, SignalProcUtils.getLPOrder(samplingRate));
    }
    
    public LPCAnalyser(DoubleDataSource signal, Window window, int frameShift, int samplingRate, int order)
    {
        this(signal, window, frameShift, samplingRate, order, 0.0f);
    }
    
    public LPCAnalyser(DoubleDataSource signal, Window window, int frameShift, int samplingRate, int order, float preCoef)
    {
        super(signal, window, frameShift, samplingRate);
        lpOrder = order;
        preemphasisCoefficient = preCoef;
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
        
        return calcLPC(frame, lpOrder, preemphasisCoefficient);
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
        return calcLPC(x, p, 0.0f);
    }
    
    public static LPCoeffs calcLPC(double[] x, int p, float preCoef)
    {
        if (p<=0)
            p = Integer.getInteger("signalproc.lpcorder", 24).intValue();
        
        if (preCoef>0.0)
            x = SignalProcUtils.applyPreemphasis(x, preCoef);
        
        int i;
        for (i=0; i<x.length; i++)
            x[i] += Math.random()*1e-50;
        
        double[] autocorr = FFT.autoCorrelateWithZeroPadding(x);
        double[] r;
        if (2*(p+1)<autocorr.length) { // normal case: frame long enough
            r = ArrayUtils.subarray(autocorr, autocorr.length/2, p+1);
        } else { // absurdly short frame
            // still compute LPC coefficients, by zero-padding the r
            r = new double[p+1];
            System.arraycopy(autocorr, autocorr.length/2, r, 0, autocorr.length-autocorr.length/2);
        }
        double[] coeffs = MathUtils.levinson(r, p);
        // gain factor:
        double g = Math.sqrt(MathUtils.sum(MathUtils.multiply(coeffs, r)));

        return new LPCoeffs(coeffs, g);
    }
    
    //Computes LP smoothed spectrum of a windowed speech frame (linear)
    public static double [] calcSpecFrame(double [] windowedFrame, int p)
    {
        return calcSpecFrame(windowedFrame, p, windowedFrame.length);
    }
    
    //Computes LP smoothed spectrum of a windowed speech frame
    public static double [] calcSpecFrame(double [] windowedFrame, int p, int fftSize)
    {
        return calcSpecFrame(windowedFrame, p, fftSize, null);
    }
    
    //Computes LP smoothed spectrum of a windowed speech frame
    public static double [] calcSpecFrame(double [] windowedFrame, int p, int fftSize, Complex expTerm)
    {
        LPCoeffs c = calcLPC(windowedFrame, p);
        
        if (expTerm==null || expTerm.real == null)
            return calcSpec(c.getA(), c.getGain(), fftSize, null);
        else
            return calcSpec(c.getA(), c.getGain(), fftSize, expTerm);
    }
    
    public static double [] calcSpecFromOneMinusA(double [] oneMinusA,  float gain, int fftSize, Complex expTerm)
    {  
        double[] alpha = new double[oneMinusA.length-1];
        for (int i=1; i<oneMinusA.length; i++)
            alpha[i-1] = -1*oneMinusA[i];
        return calcSpec(alpha, gain, fftSize, expTerm);
    }
    
    //Computes LP smoothed spectrum from LP coefficients
    public static double [] calcSpec(double [] alpha, int fftSize, Complex expTerm)
    {  
        return calcSpec(alpha, 1.0f, fftSize, expTerm);
    }
    
    //Computes LP smoothed spectrum from LP coefficients
    public static double [] calcSpec(double [] alpha, double gain, int fftSize, Complex expTerm)
    {
        int p = alpha.length;
        int maxFreq = SignalProcUtils.halfSpectrumSize(fftSize);
        double [] vtSpectrum = new double[maxFreq];
        
        if (expTerm==null || expTerm.real == null || expTerm.real.length != p*maxFreq)
            expTerm = calcExpTerm(fftSize, p);
        
        int w, i, fInd;
        Complex tmp = new Complex(1);

        for (w=0; w<=maxFreq-1; w++)
        {
            tmp.real[0] = 1.0;
            tmp.imag[0] = 0.0;
            for (i=0; i<=p-1; i++)       
            {  
                fInd = i*maxFreq+w;
                tmp.real[0] -= alpha[i]*expTerm.real[fInd];  
                tmp.imag[0] -= alpha[i]*expTerm.imag[fInd];
            }
            
            vtSpectrum[w] = gain/Math.sqrt(tmp.real[0]*tmp.real[0]+tmp.imag[0]*tmp.imag[0]);
        }
        
        return vtSpectrum;
    }
  
    public static Complex calcExpTerm(int fftSize, int p)
    {
        int maxFreq = SignalProcUtils.halfSpectrumSize(fftSize);
        Complex expTerm = new Complex(p*maxFreq);
        int i, w;
        double r;

        for (w=0; w<=maxFreq-1; w++)
        {
            r = (MathUtils.TWOPI/fftSize)*w;
            
            for (i=0; i<=p-1; i++)
            {
                expTerm.real[i*maxFreq+w] = Math.cos(r*(i+1));
                expTerm.imag[i*maxFreq+w] = -1*Math.sin(r*(i+1));
            }
        }
     
        return expTerm;
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
        protected double[] oneMinusA = null;
        protected double gain = 1.0;

        protected double[] lsf    = null;
        protected double[] lpcc   = null;
        protected double[] lprefc = null;
        
        /**
         * Create a set of LPC coefficients
         * @param oneMinusA the coefficients, a = [1, -a_1, -a_2, ... -a_p], where p = prediction order
         * @param gain the gain factor, i.e. the square root of the total energy or the prediction error.
         */
        public LPCoeffs(double[] oneMinusA, double gain)
        {
            this.oneMinusA = oneMinusA;
            this.gain = gain;
            
            this.lsf = null;
            this.lpcc = null;
            this.lprefc = null;
        }
        
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
            // Reset the cache:
            this.lsf = null;
            this.lpcc = null;
            this.lprefc = null;
        }
        
//      The following are the preferred ways to read/write the individual lpc coefficients:
        public final double getOneMinusA(int i) { return oneMinusA[i]; }
        public double getA(int i) { return -oneMinusA[i+1]; }
        
        public void setOneMinusA(int i, double value) {
            oneMinusA[i] = value;
            // Clean the cache:
            this.lsf = null;
            this.lpcc = null;
            this.lprefc = null;
        }
        public void setA(int i, double value) {
            oneMinusA[i+1] = -value;
            // Clean the cache:
            this.lsf = null;
            this.lpcc = null;
            this.lprefc = null;
        }
        
        /**
         * Get the gain, i.e. the square root of the total energy of the prediction error.
         * @return the gain
         */
        public double getGain()
        {
            return gain;
        }
        
        public void setGain(double gain)
        {
            this.gain = gain;
            this.lpcc = null; /* Note: lpcc[0] is related to the gain value,
                               * whereas the LSFs and reflection coeffs are
                               * oblivious to the gain value. */
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
            // Clean the cache:
            this.lsf = null;
            this.lpcc = null;
            this.lprefc = null;
        }
        
        /**
         * Convert these LPC coefficients into Line spectral frequencies.
         * @return the LSFs.
         */
        public double[] getLSF()
        {
            if ( lsf == null ) lsf = LineSpectralFrequencies.lpc2lsf(oneMinusA, 1);
            return( (double[]) lsf.clone() );
        }
        
        public void setLSF(double[] someLsf)
        {
            this.lsf = (double[]) someLsf.clone();
            this.oneMinusA = LineSpectralFrequencies.lsf2lpc(lsf);
            // Clean the cache:          
            this.lpcc = null;
            this.lprefc = null;            
        }
        
        /**
         * Convert these LPC coefficients into LPC-Cesptrum coefficients.
         * @param cepstrumOrder The cepstrum order (i.e., the index of the last cepstrum coefficient).
         * @return the LPCCs. c[0] is set to log(gain).
         */
        public double[] getLPCC( int cepstrumOrder )
        {
            if ( lpcc == null ) lpcc = LPCCepstrum.lpc2lpcc( oneMinusA, gain, cepstrumOrder );
            return( (double[]) lpcc.clone() );
        }
        
        /**
         * Convert some LPC-Cepstrum coefficients into these LPC coefficients.
         * @param lpcOrder The LPC order (i.e., the index of the last LPC coefficient).
         * @note The gain is set to exp(c[0]) and the LPCs are represented in the oneMinusA format [1 -a_1 -a_2 ... -a_p].
         */
        public void setLPCC( double[] someLpcc, int LPCOrder )
        {
            this.lpcc = (double[]) someLpcc.clone();
            oneMinusA = LPCCepstrum.lpcc2lpc( lpcc, LPCOrder );
            gain = Math.exp( lpcc[0] );
            // Clean the cache:
            this.lsf = null;
            this.lprefc = null;
        }

        
        /**
         * Convert these LPC coefficients into reflection coefficients.
         * @return the reflection coefficients.
         */
        public double[] getLPRefc()
        {
            if ( lprefc == null ) lprefc = ReflectionCoefficients.lpc2lprefc( oneMinusA );
            return( (double[]) lprefc.clone() );
        }
        
        /**
         * Convert some reflection coefficients into these LPC coefficients.
         */
        public void setLPRefc( double[] someLprefc )
        {
            this.lprefc = (double[]) someLprefc.clone();
            oneMinusA = ReflectionCoefficients.lprefc2lpc( lprefc );
            // Clean the cache:
            this.lsf = null;
            this.lpcc = null;
        }
        
        /**
         * Check for the stability of the LPC filter.
         * 
         * @return true if the filter is stable, false otherwise.
         */
        public boolean isStable()
        {
            /* If the reflection coeffs have not been cached before, compute them: */
            if ( this.lprefc == null ) this.lprefc = ReflectionCoefficients.lpc2lprefc( oneMinusA );
            /* Check the stability condition: a LPC filter is stable if all its
             * reflection coefficients have values in the interval [-1.0 1.0].  */
            for ( int i = 0; i < this.lprefc.length; i++ ) {
                if (  ( this.lprefc[i] > 1.0 ) || ( this.lprefc[i] < -1.0 )  ) return( false );
            }
            return( true );
        }
        
    }
}
