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
     * @throws IllegalArgumentException if coeffs has length 0
     * @throws IllegalArgumentException if numSamples is <= 0
     * @throws IllegalArgumentException if a is not less than b.
     */
    public static double[] generatePolynomialValues(double[] coeffs, int numSamples, double a, double b)
    {
        if (numSamples <= 0) throw new IllegalArgumentException("Need positive number of samples");
        if (a >= b) throw new IllegalArgumentException("Not a valid interval: ["+a+","+b+"[");
        
        double[] pred = new double[numSamples];
        double step = (b-a)/numSamples;
        double t = a;
        for (int i=0; i<numSamples; i++) {
            pred[i] = getValueAt(coeffs, t);
            t += step;
        }
        return pred;
    }
    
    /**
     * For a polynomial with the given coefficients, compute the value at the given position.
     * @param coeffs the polynomial coefficients. The code assumes that the polynomial is 
     * <code>a_order t^order + a_(order-1) t^(order-1) + ... + a_1 t + a_0</code>,
     * and will interpret coeffs as <code>a_order, a_(order-1), ..., a_1, a_0</code>,
     * where <code>order</code> is <code>coeffs.length-1</code>.
     * @param x the position where to compute the value
     * @return the predicted value
     * @throws NullPointerException if coeffs is null
     * @throws IllegalArgumentException if coeffs has length 0
     */

    public static double getValueAt(double[] coeffs, double x)
    {
        if (coeffs == null) throw new NullPointerException("Received null coeffs");
        if (coeffs.length == 0) throw new IllegalArgumentException("Received empty coeffs");
        double val = 0;
        int order = coeffs.length - 1;
        for (int j=0; j<=order; j++) {
            val += coeffs[j] * Math.pow(x, order-j);
        }
        return val;
    }

    /**
     * Compute the integrated distance between two polynomials of same order.
     * More precisely, this will return the absolute value of 
     * the integral from 0 to 1 of 
     * the difference between the two functions. 
     * 
     * @param coeffs1 polynomial coefficients, [a_order, a_(order-1), ..., a_1, a_0]
     * @param coeffs2 polynomial coefficients, [a_order, a_(order-1), ..., a_1, a_0]
     * @return
     */
    public static double polynomialDistance(double[] coeffs1, double[] coeffs2)
    {
        if (coeffs1 == null || coeffs2 == null)
            throw new NullPointerException("Received null argument");
        if (coeffs1.length != coeffs2.length)
            throw new IllegalArgumentException("Can only compare polynomials with same order");
        double dist = 0;
        int order = coeffs1.length - 1;
        for (int i=0; i<= order; i++) {
            dist += (coeffs1[order - i] - coeffs2[order - i]) / (i+1);
        }
        return Math.abs(dist);
    }
 
    /**
     * Compute the integrated distance between two polynomials of same order.
     * More precisely, this will return the absolute value of 
     * the integral from 0 to 1 of 
     * the difference between the two functions. 
     * 
     * @param coeffs1 polynomial coefficients, [a_order, a_(order-1), ..., a_1, a_0]
     * @param coeffs2 polynomial coefficients, [a_order, a_(order-1), ..., a_1, a_0]
     * @return
     */
    public static double polynomialDistance(float[] coeffs1, float[] coeffs2)
    {
        if (coeffs1 == null || coeffs2 == null)
            throw new NullPointerException("Received null argument");
        if (coeffs1.length != coeffs2.length)
            throw new IllegalArgumentException("Can only compare polynomials with same order");
        double dist = 0;
        int order = coeffs1.length - 1;
        for (int i=0; i<= order; i++) {
            dist += ((double)coeffs1[order - i] - coeffs2[order - i]) / (i+1);
        }
        return Math.abs(dist);
    }

    /**
     * Compute the integral of the squared difference between two polynomials of same order.
     * More precisely, this will return the  
     * the integral from 0 to 1 of 
     * the square of
     * the difference between the two functions.
     * <p>
     * This implements the algebraic solution proposed by Maxima from
     * the following command:
     * <code>expand(integrate((sum(a[i]*x**i, i, 0, order))**2, x, 0, 1));</code>,
     * with order varied from 0 to 4. Increasing order by 1 adds (order+1) summands.
     * 
     * @param coeffs1 polynomial coefficients, [a_order, a_(order-1), ..., a_1, a_0]
     * @param coeffs2 polynomial coefficients, [a_order, a_(order-1), ..., a_1, a_0]
     * @return
     */
    public static double polynomialSquaredDistance(double[] coeffs1, double[] coeffs2)
    {
        if (coeffs1 == null || coeffs2 == null)
            throw new NullPointerException("Received null argument");
        if (coeffs1.length != coeffs2.length)
            throw new IllegalArgumentException("Can only compare polynomials with same order");
        int order = coeffs1.length - 1;
        double[] a = new double[coeffs1.length];
        for (int i=0; i<a.length; i++) {
            a[i] = coeffs1[order-i] - coeffs2[order-i];
        }
        return integrateSquared(order, a);
    }

    /**
     * Compute the integral of the squared difference between two polynomials of same order.
     * More precisely, this will return the  
     * the integral from 0 to 1 of 
     * the square of
     * the difference between the two functions.
     * <p>
     * This implements the algebraic solution proposed by Maxima from
     * the following command:
     * <code>expand(integrate((sum(a[i]*x**i, i, 0, order))**2, x, 0, 1));</code>,
     * with order varied from 0 to 4. Increasing order by 1 adds (order+1) summands.
     * 
     * @param coeffs1 polynomial coefficients, [a_order, a_(order-1), ..., a_1, a_0]
     * @param coeffs2 polynomial coefficients, [a_order, a_(order-1), ..., a_1, a_0]
     * @return
     */
    public static double polynomialSquaredDistance(float[] coeffs1, float[] coeffs2)
    {
        if (coeffs1 == null || coeffs2 == null)
            throw new NullPointerException("Received null argument");
        if (coeffs1.length != coeffs2.length)
            throw new IllegalArgumentException("Can only compare polynomials with same order");
        int order = coeffs1.length - 1;
        double[] a = new double[coeffs1.length];
        for (int i=0; i<a.length; i++) {
            a[i] = coeffs1[order-i] - coeffs2[order-i];
        }
        return integrateSquared(order, a);
    }

    private static double integrateSquared(int order, double[] a) {
        double dist = 0;
        // Order 0 terms: a0^2
        dist += a[0]*a[0];
        if (order == 0) return dist;
        // Order 1 terms: a0 a1 + 1/3 a1^2
        dist += a[0]*a[1] + a[1]*a[1]/3;
        if (order == 1) return dist;
        // Order 2 terms: 2/3 a0 a2 + 1/2 a1 a2 + 1/5 a2^2
        dist += 2./3*a[0]*a[2] + a[1]*a[2]/2 + a[2]*a[2]/5;
        if (order == 2) return dist;
        // Order 3 terms: 1/2 a0 a3 + 2/5 a1 a3 + 1/3 a2 a3 + 1/7 a3^2
        dist += a[0]*a[3]/2 + 2./5*a[1]*a[3] + a[2]*a[3]/3 + a[3]*a[3]/7;
        if (order == 3) return dist;
        // Order 4 terms: 2/5 a0 a4 + 1/3 a1 a4 + 2/7 a2 a4 + 1/4 a3 a4 + 1/9 a4^2
        dist += 2./5*a[0]*a[4] + a[1]*a[4]/3 + 2./7*a[2]*a[4] + a[3]*a[4]/4 + a[4]*a[4]/9;
        if (order == 4) return dist;
        throw new IllegalArgumentException("Order greater than 4 not supported");
    }

}
