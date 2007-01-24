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

/**
 * Implements the conversion between the Linear Prediction
 * Coefficients (LPC) and the corresponding Linear Prediction
 * Cepstrum Coefficients.
 * 
 * @author sacha
 *
 */
public class LPCCepstrum {

    /**
     * Converts from LPC coefficients to LPCC coefficients.
     * 
     * @param oneMinusA A(z) = a0 - sum { ai * z^-i } . a0 = 1.
     * @param gain The LPC gain factor.
     * @param cepstrumOrder Cepstrum order (equal to the index of the last cepstrum coefficient).
     * 
     * @return The LPCC coefficents. c[0] is set to log(gain).
     * 
     * @author sacha
     */
    public static double[] lpc2lpcc(double[] oneMinusA, double gain, int cepstrumOrder) {
        
        // Check the cepstrum order
        if ( cepstrumOrder <= 0 ) {
            throw new RuntimeException( "The cepstrum order [" + cepstrumOrder + "] must be a positive integer." );
        }
        
        // Declare misc. useful variables
        int k,m;
        double acc;
        double[] c = new double[cepstrumOrder+1];
        int lpcOrder = oneMinusA.length - 1;

        /* First cepstral coeff */
        c[0] = Math.log( gain );
        /* Other coeffs: */
        for ( m = 1; m <= cepstrumOrder; m++ ) {
            /* Coeffs within the LPC order: */
            if ( m <= lpcOrder ) {
                acc = 0.0;
                for ( k=1; k < m; k++ ) acc += ( (double)(m-k) * oneMinusA[k] * c[m-k] );
                c[m] = acc/(double)(m) + oneMinusA[m];
            }
            /* Coeffs above the LPC order: */
            else {
                acc = 0.0;
                for ( k=1; k <= lpcOrder; k++ ) acc += ( (double)(m-k) * oneMinusA[k] * c[m-k] );
                c[m] = acc/(double)(m);
            }
        }
        
        return( c );
    }

    /**
     * Converts from LPCC coefficients to LPC coefficients.
     * 
     * @param c the vector of cepstral coefficients. Note: c[0] = log(gain).
     * @param lpcOrder The original LPC order (equal to the index of the last LPC coefficient).
     * 
     * @return The LPC coefficents [1 -a_1 -a_2 ... -a_p].
     * 
     * @note The gain is not returned, but it can be recovered as exp(c[0]).
     * 
     * @author sacha
     */
    public static double[] lpcc2lpc(double[] c, int lpcOrder) {
        
        // Check the LPC order
        if ( lpcOrder <= 0 ) {
            throw new RuntimeException( "The LPC order [" + lpcOrder + "] must be a positive integer." );
        }
        
        // Declare misc. useful variables
        int k,m;
        double acc;
        double[] a = new double[lpcOrder+1];
        int cepstrumOrder = c.length - 1;

        /* First lpc coeff */
        a[0] = 1.0;
        /* Other coeffs: */
        for ( m = 1; m <= lpcOrder; m++ ) {
            /* Coeffs within the Cepstrum order: */
            if ( m <= cepstrumOrder ) {
                acc = 0.0;
                for ( k=1; k < m; k++ ) acc += ( (double)(k) * a[m-k] * c[k] );
                a[m] = c[m] - acc/(double)(m);
            }
            /* Coeffs above the Cepstrum order: */
            else {
                acc = 0.0;
                for ( k=1; k <= cepstrumOrder; k++ ) acc += ( (double)(k) * a[m-k] * c[k] );
                a[m] = - acc/(double)(m);
            }
        }
        
        return( a );
    }

}
