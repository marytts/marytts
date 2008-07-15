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

package marytts.signalproc.analysis;

import java.util.Arrays;

import marytts.util.math.ComplexArray;
import marytts.util.math.FFT;
import marytts.util.math.FFTMixedRadix;
import marytts.util.math.Hilbert;
import marytts.util.math.MathUtils;
import marytts.util.signal.SignalProcUtils;

/**
 * @author Marc Schr&ouml;der, oytun.turk
 *
 * Computes real and complex cepstrum directly from speech spectrum
 * Note that this is different than LP cepstrum 
 *   which is implemented in CepstrumLPCAnalyser
 */
public class CepstrumSpeechAnalyser
{
    public static double[] realCepstrum(double[] frame)
    {
        return realCepstrum(frame, frame.length);
    }
    
    public static double[] realCepstrum(double[] frame, int fftSize)
    {
        return complexCepstrum(frame, fftSize).real;
    }
    
    public static ComplexArray complexCepstrum(double[] frame, int fftSize)
    {
        double[] real = new double[fftSize];
        double[] imag = new double[fftSize];
        Arrays.fill(real, 0.0);
        Arrays.fill(imag, 0.0);
        System.arraycopy(frame, 0, real, 0, Math.min(frame.length, fftSize));
        
        if (MathUtils.isPowerOfTwo(fftSize))
            FFT.transform(real, imag, false);
        else
            FFTMixedRadix.fftComplex(real, imag);
        
        // Now real + j*imag is the complex spectrum
        MathUtils.toPolarCoordinates(real, imag);
        // now real = abs(X), imag = phi
        real = MathUtils.log(real);
        
        // Now real + j*imag is the complex cepstrum
        if (MathUtils.isPowerOfTwo(fftSize))
        {
            FFT.transform(real, imag, true);
            return new ComplexArray(real, imag);
        }
        else
             return FFTMixedRadix.ifft(real, imag); 
    }

    public static double[] filterLowPass(double[] realCepstrum, int cutoffIndex)
    {
        double[] filtered = new double[realCepstrum.length];
        filtered[0] = realCepstrum[0];
        filtered[cutoffIndex] = realCepstrum[cutoffIndex];
        for (int i=1; i< cutoffIndex; i++) {
            filtered[i] = 2*realCepstrum[i];
        }
        return filtered;
    }
    
    //Estimates the phase response (in Radians) of the vocal tract transfer function
    // using the minimum phase assumption using real cepstrum based spectral smoothing
    public static double [] systemPhaseResponse(double [] x, int fftSize, int lifterOrder)
    {
        double [] systemAmpsInNeper = cepstralSmoothedSpectrumInNeper(x, fftSize, lifterOrder);
        
        return minimumPhaseResponseInRadians(systemAmpsInNeper);
    }
    
    //Estimates the phase response (in Radians) of the vocal tract transfer function
    // using the minimum phase assumption using real cepstrum based spectral smoothing
    public static double [] systemPhaseResponse(double [] x, int fs)
    {   
        double [] systemAmpsInNeper = cepstralSmoothedSpectrumInNeper(x, fs);
        
        return minimumPhaseResponseInRadians(systemAmpsInNeper);
    }
    
    //Returns the phase response(in radians) of a minimum phase system given the system amplitudes in dB
    public static double [] minimumPhaseResponseInRadians(double[] systemAmpsInNeper)
    {
        ComplexArray phaseResponse = minimumPhaseResponse(systemAmpsInNeper);
        
        //Perform in-place conversion from complex values to radians
        for (int w=0; w<phaseResponse.real.length; w++)
            phaseResponse.real[w] = Math.atan2(phaseResponse.imag[w], phaseResponse.real[w]);
        
        return phaseResponse.real;
    }
    
    //Returns the phase response of a minimum phase system given the system amplitudes in dB
    public static ComplexArray minimumPhaseResponse(double[] systemAmpsInNeper)
    {
        int w;
    
        ComplexArray phaseResponse = Hilbert.transform(systemAmpsInNeper);
        for (w=0; w<phaseResponse.real.length; w++)
        {
            phaseResponse.real[w] *= -1.0;
            phaseResponse.imag[w] *= -1.0;
        }
        
        return phaseResponse;
    }
    
    //Returns the cepstral smoothed amplitude spectrum in dB
    public static double [] cepstralSmoothedSpectrumInNeper(double [] x, int fs)
    {
        int lifterOrder = SignalProcUtils.getLifterOrder(fs);
        int fftSize = SignalProcUtils.getDFTSize(fs);
        return  cepstralSmoothedSpectrumInNeper(x, fftSize, lifterOrder);
    }

    public static double [] cepstralSmoothedSpectrumInNeper(double [] x, int fftSize, int lifterOrder)
    {
        double[] rceps = realCepstrum(x, fftSize);
        double[] w = new double[rceps.length];
        
        int i;
        for (i=0; i<lifterOrder; i++)
            w[i] = 1.0;
        for (i=lifterOrder; i<w.length; i++)
            w[i] = 0.0;
        
        for (i=0; i<w.length; i++)
            rceps[i] *= w[i];
        
        //Inverse cepstrum step
        ComplexArray y = FFTMixedRadix.fftReal(rceps, rceps.length);
        
        return y.real;
        //
    }
}
