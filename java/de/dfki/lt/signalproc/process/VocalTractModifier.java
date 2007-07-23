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

package de.dfki.lt.signalproc.process;

import java.util.Arrays;

import de.dfki.lt.signalproc.FFT;
import de.dfki.lt.signalproc.analysis.LPCAnalyser;
import de.dfki.lt.signalproc.analysis.LPCAnalyser.LPCoeffs;
import de.dfki.lt.signalproc.filter.FIRFilter;
import de.dfki.lt.signalproc.filter.RecursiveFilter;
import de.dfki.lt.signalproc.process.InlineDataProcessor;
import de.dfki.lt.signalproc.util.SignalProcUtils;
import de.dfki.lt.util.ArrayUtils;
import de.dfki.lt.signalproc.analysis.LPCAnalyser;
import de.dfki.lt.signalproc.analysis.LPCAnalyser.LPCoeffs;
import de.dfki.lt.signalproc.util.MathUtils.Complex;

/**
 * @author oytun.turk
 *
 */
public class VocalTractModifier implements InlineDataProcessor {

    protected int p;
    protected int fs;
    protected int fftSize;
    protected int maxFreq;
    protected double[] real;
    protected double[] imag;
    protected double [] vtSpectrum;
    private Complex expTerm;
    private boolean bAnalysisOnly;
    
    /**
     * 
     */
    
    //For derived classes which will call initialise on their own
    public VocalTractModifier() {
    
    }
    
    public VocalTractModifier(int pIn, int fsIn, int fftSizeIn) {
            initialise(pIn, fsIn, fftSizeIn);
    }
    
    public void initialise(int pIn, int fsIn, int fftSizeIn)
    {
        initialise(pIn, fsIn, fftSizeIn, false);
    }
    
    //If bAnalysisOnly is true, it will not process the spectrum and after each call to applyInline, you will obtain
    // the real valued vocal tract spectrum in vtSpectrum and the complex valued excitation spectrum in real and imag
    public void initialise(int pIn, int fsIn, int fftSizeIn, boolean bAnalysisOnlyIn)
    {
        this.p = pIn;
        this.fs = fsIn;
        this.fftSize = fftSizeIn;
        this.real = new double[fftSize];
        this.imag = new double[fftSize];
        this.maxFreq = SignalProcUtils.halfSpectrumSize(fftSize);
        this.vtSpectrum = new double[maxFreq];
        this.expTerm = new Complex(p*maxFreq);
        this.expTerm = LPCAnalyser.calcExpTerm(fftSize, p);
        this.bAnalysisOnly = bAnalysisOnlyIn;
    }
    
    public void applyInline(double [] data, int pos, int len) {
        int k;
        assert pos==0;
        assert len==data.length;
        if (len > fftSize)
            throw new IllegalArgumentException("Length must not be larger than FFT size");
        
        // Compute LPC coefficients
        LPCoeffs coeffs = LPCAnalyser.calcLPC(data, p);
        double sqrtGain = coeffs.getGain();
        
        System.arraycopy(data, 0, real, 0, Math.min(len, real.length));
        
        if (real.length > len)
            Arrays.fill(real, real.length-len, real.length-1, 0);
        
        Arrays.fill(imag, 0, imag.length-1, 0);
        
        // Convert to polar coordinates in frequency domain
        FFT.transform(real, imag, false);
        
        vtSpectrum = LPCAnalyser.calcSpec(coeffs.getA(), p, fftSize, expTerm);
        
        for (k=0; k<maxFreq; k++)
            vtSpectrum[k] *= sqrtGain;
        
        // Filter out vocal tract to obtain residual spectrum
        for (k=0; k<maxFreq; k++)
        {
            real[k] /= vtSpectrum[k];
            imag[k] /= vtSpectrum[k];
        }
        
        if (!bAnalysisOnly)
        {
            // Process vocal tract spectrum
            processSpectrum(vtSpectrum);

            //Apply modified vocal tract filter on the residual spectrum
            for (k=0; k<maxFreq; k++)
            {
                real[k] *= vtSpectrum[k];
                imag[k] *= vtSpectrum[k];
            }

            //Generate the complex conjugate part to make the output the DFT of a real-valued signal
            for (k=maxFreq; k<fftSize; k++)
            {
                real[k] = real[2*maxFreq-k];
                imag[k] = imag[2*maxFreq-k];
            }
            //

            FFT.transform(real, imag, true);
            System.arraycopy(real, 0, data, 0, len);
        }
    }

    //Overload this function in the derived classes to modify the vocal tract spectrum Px in anyway you wish
    protected void processSpectrum(double [] Px) {}
    
}
