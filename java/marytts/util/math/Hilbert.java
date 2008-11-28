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

package marytts.util.math;

/**
 * Computes the N-point Discrete Hilbert Transform of real valued vector x:
 * The algorithm consists of the following stages:
 * - X(w) = FFT(x) is computed
 * - H(w), DFT of a Hilbert transform filter h[n], is created:
 *   H[0]=H[N/2]=1
 *   H[w]=2 for w=1,2,...,N/2-1
 *   H[w]=0 for w=N/2+1,...,N-1
 * - x[n] and h[n] are convolved (i.e. X(w) and H(w) multiplied)
 * - y[n], the Discrete Hilbert Transform of x[n] is computed by y[n]=IFFT(X(w)H(w)) for n=0,...,N-1
 * 
 * @author Oytun T&uumlrk
 */
public class Hilbert {
    public static ComplexArray transform(double [] x)
    {
        return transform(x, x.length);
    }
    
    
    public static ComplexArray transform(double [] x, int N)
    {
        ComplexArray X = FFTMixedRadix.fftReal(x, N);
        double [] H = new double[N];
        
        int NOver2 = (int)Math.floor(N/2+0.5);
        int w;
        
        H[0] = 1.0;
        H[NOver2] = 1.0;
        
        for (w=1; w<=NOver2-1; w++)
            H[w] = 2.0;
        
        for (w=NOver2+1; w<=N-1; w++)
            H[w] = 0.0;
        
        for (w=0; w<N; w++)
        {
            X.real[w] *= H[w];
            X.imag[w] *= H[w];
        }
        
        return FFTMixedRadix.ifft(X);
    }

}
