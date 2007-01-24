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
 * Implements the conversion between the Linear
 * Prediction Coefficients (LPC) and the corresponding
 * Reflection Coefficients.
 * 
 * @author sacha
 *
 */
public class ReflectionCoefficients {

    /**
     * Converts from LPC coefficients to reflection coefficients.
     * 
     * @param oneMinusA A(z) = a0 - sum { ai * z^-i } . a0 = 1.
     * 
     * @return The reflection coefficents. If the LPC order is N,
     * there are N reflection coefficients and (N+1) LPC coefficients
     * (if you count a0==1).
     * 
     * @author sacha
     */
    public static double[] lpc2lprefc(double[] oneMinusA) {
        
        int lpcOrder = oneMinusA.length - 1;
        double[] k = new double[lpcOrder];
        double a1, a2, ki;
        double[] a = (double[]) oneMinusA.clone();
        
        /* For each prediction coeff */
        for ( int i = lpcOrder; i > 0; i-- ) {
            /* Current reflection coeff equals last prediction coeff */
            ki = a[i];
            k[i-1] = ki;
            /* Then update the remaining predictor coeffs */
            /* Recurse between 2nd and before-last LPC coeffs, going from border to
               middle position of the equation array. */
            for( int j = 1; j <= (i>>1); j++ ) {
                /* Note: in the above, (i>>1) is to be understood
                 * as the integer division by 2. */
                a1 = a[j];
                a2 = a[i-j];
                a[j]   = ( a1 + ki * a2 ) / (1 - ki*ki);
                a[i-j] = ( a2 + ki * a1 ) / (1 - ki*ki);
            }
        }

        
        return( k );
    }

    /**
     * Converts from the reflection coefficients to
     * the corresponding LPC coefficients.
     * 
     * @param k the vector of p reflection coefficients [k_0 ... k_p-1].
     * 
     * @return The LPC coefficents [1 -a_1 -a_2 ... -a_p].
     * 
     * @author sacha
     */
    public static double[] lprefc2lpc( double[] k ) {
        
        int lpcOrder = k.length;
        double[] a = new double[lpcOrder+1];
        double a1,a2;
        
        /* Set first LPC coefficient to 1, and never touch it again. */
        a[0] = 1.0;

        /* Then recurse for each reflection coefficient */
        for( int i = 1; i <= lpcOrder; i++ ) {
            /* WARNING: k[0] is the first reflection coefficient, leading from
               a_j^(0) to a_j^(1). There is one more a_j than the
               number of k_i. There are (lpcOrder) k_i and (lpcOrder+1) a_j. */

            /* Recurse between 2nd and before-last LPC coeffs, going from border to
               middle position of the equation array. */
            for( int j = 1; j <= (i>>1); j++ ) {
                /* Note: in the above, (i>>1) is to be understood
                 * as the integer division by 2. */
                a1 = a[j];
                a2 = a[i-j];
                a[j]   = a1 - k[i-1] * a2;
                a[i-j] = a2 - k[i-1] * a1;
            }
            /* The last a_j equals the current reflection coeff. */
            a[i] = k[i-1];
        }

        return( a );
    }

}
