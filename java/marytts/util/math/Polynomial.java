/**
 * Copyright 2009 DFKI GmbH.
 * All Rights Reserved.  Use is subject to license terms.
 *
 * This file is part of MARY TTS.
 *
 * MARY TTS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package marytts.util.math;

import Jama.Matrix;

/**
 * @author marc
 *
 */
public class Polynomial
{
    /**
     * Fit a polynomial of the given order to the given data.
     * @param data the data points, assumed to be in the interval [0, 1[
     * @param order the order of the polynomial. Must be non-negative.
     * @return the polynomial coefficients, highest power first.
     * In other words, if the polynomial is 
     * <code>a_order t^order + a_(order-1) t^(order-1) + ... + a_1 t + a_0</code>,
     * then the array returned contains <code>a_order, a_(order-1), ..., a_1, a_0</code>. 
     * throws NullPointerException if data is null
     * throws IllegalArgumentException if data.length < order or if order < 0.
     */
    public static double[] fitPolynomial(double[] data, int order)
    {
        if (data == null) throw new NullPointerException("Null data");
        if (order < 0) throw new IllegalArgumentException("Polynomial order < 0 not supported");
        //if (data.length < order) throw new IllegalArgumentException("Data length must be at least order");

        double[][] A = new double[data.length][order+1];
        double[][] b = new double[data.length][1];
        for (int i=0; i<A.length; i++) {
            if (Double.isNaN(data[i])) { // not a number -- ignore this data point
                // set value and coeffs to zero
                b[i][0] = 0;
                for (int j=0; j<=order; j++) {
                    A[i][j] = 0;
                }
            } else { // normal case: valid data point
                b[i][0] = data[i];
                // We write the polynomial as:
                // a_order t^order + a_(order-1) t^(order-1) + ... + a_1 t + a_0
                double t = ((double) i) / data.length;
                for (int j=0; j<=order; j++) {
                    A[i][j] = Math.pow(t, order-j);
                }
            }
        }
        // Least-square solution A x = b, where
        // x = (a_order, a_(order-1), ..., a_1, a_0)
        try {
            Matrix x = new Matrix(A).solve(new Matrix(b));
            double[] coeffs = new double[order+1];
            for (int j=0; j<=order; j++) {
                coeffs[j] = x.get(j, 0);
            }
            return coeffs;
        } catch (RuntimeException re) {
            return null;
        }
    }
    
    /**
     * For a polynomial with the given coefficients, compute <code>numSamples</code>
     * values, equally spaced in the interval [a, b[.
     * @param coeffs the polynomial coefficients. The code assumes that the polynomial is 
     * <code>a_order t^order + a_(order-1) t^(order-1) + ... + a_1 t + a_0</code>,
     * and will interpret coeffs as <code>a_order, a_(order-1), ..., a_1, a_0</code>,
     * where <code>order</code> is <code>coeffs.length-1</code>.
     * @param numSamples
     * @param a lower bound (inclusive)
     * @param b upper bound (exclusive)
     * @return the predicted samples.
     * @throws NullPointerException if coeffs is null
     * @throws IllegalArgumentException if numSamples is <= 0
     * @throws IllegalArgumentException if a is not less than b.
     */
    public static double[] generatePolynomialValues(double[] coeffs, int numSamples, double a, double b)
    {
        if (coeffs == null) throw new NullPointerException("Received null coeffs");
        if (numSamples <= 0) throw new IllegalArgumentException("Need positive number of samples");
        if (a >= b) throw new IllegalArgumentException("Not a valid interval: ["+a+","+b+"[");
        
        int order = coeffs.length-1;
        double[] pred = new double[numSamples];
        double step = (b-a)/numSamples;
        double t = a;
        for (int i=0; i<numSamples; i++) {
            for (int j=0; j<=order; j++) {
                pred[i] += coeffs[j] * Math.pow(t, order-j);
            }
            t += step;
        }
        return pred;
    }

}
