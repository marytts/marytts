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

package marytts.signalproc.analysis;

import marytts.util.math.MathUtils;
import marytts.util.signal.SignalProcUtils;

/**
 * 
 * Implements the regularized cepstral envelope estimation in
 * 
 * Cappe, O., Laroche, J., and Moulines E., 1995, "Regularized estimation of cepstrum envelope from discrete frequency points", 
 *    in IEEE ASSP Workshop on app. of sig. proc. to audio and acoust.
 *    
 * This approach is used in Harmonic plus Noise (HNM) model for pitch modification
 * for the purpose of estimating amplitudes of harmonics at (new) pitch-modified locations.
 * (See, i.e. (Stylianou, et. al.,1995) or Stylianou´s PhD thesis for details)
 * 
 * Stylianou, Y, Laroche, J., and Moulines E., 1995, "High quality speech modification based on a Harmonic + Noise model", 
 *    in Proc. of the Europseech 1995.
 * 
 * Various other techniques are used by other researchers 
 * to keep the overall spectral shape unchanged under pitch sclae modifications.
 * For example, Quatieri uses SEEVOC approach (linear interpolation) to find amplitude values at modified frequencies.
 * Failing to estimate the modified amplitudes successfully will result in changes in overal spectral envelope
 * which may affect voice quality, presence, or even the identity of phonemes after pitch scaling.
 * 
 * @author Oytun T&uumlrk
 */
public class RegularizedCepstralEnvelopeEstimator 
{
    public static final double DEFAULT_LAMBDA = 5e-4;
   
    public static double[] spectralEnvelopeDB(double[] linearAmps, double[] freqsInHz, int samplingRateInHz, int cepsOrder, int fftSize)
    {
        double[] ceps = freqsLinearAmps2cepstrum(linearAmps, freqsInHz, samplingRateInHz, cepsOrder);

        return cepstrum2logAmpHalfSpectrum(ceps, fftSize, samplingRateInHz);
    }
    
    public static double[] spectralEnvelopeLinear(double[] linearAmps, double[] freqsInHz, int samplingRateInHz, int cepsOrder, int fftSize)
    {
        return MathUtils.db2amp(spectralEnvelopeDB(linearAmps, freqsInHz, samplingRateInHz, cepsOrder, fftSize));
    }
    
    public static double[] freqsLinearAmps2cepstrum(double[] linearAmps, double[] freqsInHz, int samplingRateInHz, int cepsOrder)
    {
        return freqsLinearAmps2cepstrum(linearAmps, freqsInHz, samplingRateInHz, cepsOrder, null, DEFAULT_LAMBDA);
    }
    
    //lambda: regularization term (typically on the order of 0.0001
    //Note that cepstrum is always computed using log amps, therefore the fitted spectrum computed from these cepstrum coeffs will be in log amp domain
    public static double[] freqsLinearAmps2cepstrum(double[] linearAmps, double[] freqsInHz, int samplingRateInHz, int cepsOrder, double[] weights, double lambda)
    { 
        assert linearAmps.length==freqsInHz.length;
        
        double[] logAmps = MathUtils.log10(linearAmps);
        double[] a = MathUtils.multiply(logAmps, 20.0);
        int L = linearAmps.length;
        int p = cepsOrder;
        double[][] M = new double[L][p+1];
        int i, j;
        double f;
        double denum = (2.0*SignalProcUtils.freq2barkNew(0.5*samplingRateInHz));
        for (i=0; i<L; i++)
        {
            M[i][0] = 1.0;
            //f = SignalProcUtils.hz2radian(freqsInHz[i], samplingRateInHz); //NO BARK CONVERSION
            f = SignalProcUtils.freq2barkNew(freqsInHz[i])/denum;
            
            for (j=1; j<p+1; j++)  
                M[i][j] = 2.0*Math.cos(MathUtils.TWOPI*f*j);
        }
        
        double[] diagR = new double[p+1];
        double tmp = 8.0*(0.5*MathUtils.TWOPI)*(0.5*MathUtils.TWOPI);
        for (i=0; i<p+1; i++)
            diagR[i] = tmp*i*i;
        double[][] R = MathUtils.toDiagonalMatrix(diagR);
        
        double[] ceps = null;
        if (weights!=null)
        {
            double[][] W = MathUtils.toDiagonalMatrix(weights);
            double[][] MTrans = MathUtils.transpoze(M);
            double[][] MTransW = MathUtils.matrixProduct(MTrans, W);
            double[][] MTransWM = MathUtils.matrixProduct(MTransW, M);
            double[][] lambdaR = MathUtils.multiply(lambda, R);
            double[] MTransWa = MathUtils.matrixProduct(MTransW, a);
            double[][] inverted = MathUtils.inverse(MathUtils.add(MTransWM, lambdaR));
            ceps = MathUtils.matrixProduct(inverted, MTransWa);
        }
        else //No weights given
        {
            double[][] MTrans = MathUtils.transpoze(M);
            double[][] MTransM = MathUtils.matrixProduct(MTrans, M);
            double[][] lambdaR = MathUtils.multiply(lambda, R);
            double[] MTransa = MathUtils.matrixProduct(MTrans, a);
            double[][] inverted = MathUtils.inverse(MathUtils.add(MTransM, lambdaR));
            ceps = MathUtils.matrixProduct(inverted, MTransa);
        }  
        
        return ceps;
    }
    
    public static double[][] precomputeM(double[] freqsInHz, int samplingRateInHz, int cepsOrder)
    {
        int L = freqsInHz.length;
        int p = cepsOrder;
        double[][] M = new double[L][p+1];
        int i, j;
        double f;
        double denum = (2.0*SignalProcUtils.freq2barkNew(0.5*samplingRateInHz));
        for (i=0; i<L; i++)
        {
            M[i][0] = 1.0;
            //f = SignalProcUtils.hz2radian(freqsInHz[i], samplingRateInHz); //NO BARK CONVERSION
            f = SignalProcUtils.freq2barkNew(freqsInHz[i])/denum;

            for (j=1; j<p+1; j++)  
                M[i][j] = 2.0*Math.cos(MathUtils.TWOPI*f*j);
        }

        return M;
    }
    
    public static double[][] precomputeMTransW(double[][] M, double[] weights)
    {
        double[][] MTransW = null;
        if (weights!=null)
        {
            double[][] W = MathUtils.toDiagonalMatrix(weights);
            double[][] MTrans = MathUtils.transpoze(M);
            MTransW = MathUtils.matrixProduct(MTrans, W);
        }
        else //No weights given
            MTransW = MathUtils.transpoze(M); 
        
        return MTransW;
    }
    
    public static double[][] precomputeMTransWM(double[][] MTransW, double[][] M)
    {
        double[][] MTransWM = MathUtils.matrixProduct(MTransW, M);
        
        return MTransWM;
    }
    
    public static double[][] precomputeLambdaR(double lambda, int cepsOrder)
    {
        int p = cepsOrder;
        double[] diagR = new double[p+1];
        double tmp = 8.0*(0.5*MathUtils.TWOPI)*(0.5*MathUtils.TWOPI);
        for (int i=0; i<p+1; i++)
            diagR[i] = tmp*i*i;
        double[][] R = MathUtils.toDiagonalMatrix(diagR);
        
        double[][] lambdaR = MathUtils.multiply(lambda, R);
        
        return lambdaR;
    }
    
    public static double[][] precomputeInverted(double[][] MTransWM, double[][] lambdaR)
    {        
        double[][] inverted = MathUtils.inverse(MathUtils.add(MTransWM, lambdaR));
        
        return inverted;
    }
    
    //Another version when frequencies are fixed and all precomputations were done by calling precomputeForCepstrum with these fixed values
    //Note that cepstrum is always computed using log amps, therefore the fitted spectrum computed from these cepstrum coeffs will be in log amp domain
    public static double[] freqsLinearAmps2cepstrum(double[] linearAmps, double[][] MTransW, double[][] inverted)
    {  
        double[] logAmps = MathUtils.log10(linearAmps);
        double[] a = MathUtils.multiply(logAmps, 20.0);
        
        double[] ceps = null;
        
        double[] MTransWa = MathUtils.matrixProduct(MTransW, a);
        ceps = MathUtils.matrixProduct(inverted, MTransWa);

        return ceps;
    }
    
    public static double[] cepstrum2logAmpHalfSpectrum(double[] ceps, int fftSize, int samplingRateInHz)
    {
        int maxFreq = SignalProcUtils.halfSpectrumSize(fftSize);
        double[] halfAbsSpectrum = new double[maxFreq];
        int p = ceps.length-1;
        int i, k;

        double f;
        double denum = (2.0*SignalProcUtils.freq2barkNew(0.5*samplingRateInHz));
        for (k=0; k<maxFreq; k++)
        {
            f = SignalProcUtils.freq2barkNew(((double)k/(maxFreq-1.0))*0.5*samplingRateInHz)/denum;
            
            halfAbsSpectrum[k] = ceps[0];
            for (i=1; i<=p; i++)
                halfAbsSpectrum[k] += 2.0*ceps[i]*Math.cos(MathUtils.TWOPI*f*i);   
        }
        
        return halfAbsSpectrum;
    }
    
    public static double cepstrum2linearSpectrumValue(double[] ceps, double freqInHz, int samplingRateInHz)
    {
        return MathUtils.db2amp(cepstrum2dbSpectrumValue(ceps, freqInHz, samplingRateInHz));
    }
    
    public static double cepstrum2dbSpectrumValue(double[] ceps, double freqInHz, int samplingRateInHz)
    {
        int p = ceps.length-1;

        double denum = (2.0*SignalProcUtils.freq2barkNew(0.5*samplingRateInHz));
        double f = SignalProcUtils.freq2barkNew(freqInHz)/denum;

        double val = ceps[0];
        for (int i=1; i<=p; i++)
            val += 2.0*ceps[i]*Math.cos(MathUtils.TWOPI*f*i);   

        return val;
    }
    
    public static double[] cepstrum2linearSpectrumValues(double[] ceps, double[] freqsInHz, int samplingRateInHz)
    {
        return MathUtils.db2amp(cepstrum2dbSpectrumValues(ceps, freqsInHz, samplingRateInHz));
    }
    
    public static double[] cepstrum2dbSpectrumValues(double[] ceps, double[] freqsInHz, int samplingRateInHz)
    {
        double[] vals = new double[freqsInHz.length];
        for (int i=0; i<freqsInHz.length; i++)
            vals[i] = cepstrum2dbSpectrumValue(ceps, freqsInHz[i], samplingRateInHz);
        
        return vals;
    }
}
