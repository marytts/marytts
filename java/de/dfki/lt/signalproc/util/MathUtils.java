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

package de.dfki.lt.signalproc.util;

import java.util.Vector;


/**
 * @author Marc Schr&ouml;der
 * 
 * An uninstantiable class, containing static utility methods in the Math domain.
 *
 */
public class MathUtils {
    protected static final double PASCAL = 2E-5;
    protected static final double PASCALSQUARE = 4E-10;
    protected static final double LOG10 = Math.log(10);
    
    public static final double TWOPI = 2*Math.PI;
    
    public static final int EQUALS = 0;
    public static final int GREATER_THAN = 1;
    public static final int GREATER_THAN_OR_EQUALS = 2;
    public static final int LESS_THAN = 3;
    public static final int LESS_THAN_OR_EQUALS = 4;
    public static final int NOT_EQUALS = 5;
    
    public static boolean isPowerOfTwo(int N)
    {
        final int maxBits = 32;
        int n=2;
        for (int i=2; i<=maxBits; i++) {
            if (n==N) return true;
            n<<=1;
        }
        return false;
    }
    
    public static int closestPowerOfTwoAbove(int N)
    {
        return 1<<(int) Math.ceil(Math.log(N)/Math.log(2));
    }

    public static int findNextValleyLocation(double[] data, int startIndex)
    {
        for (int i=startIndex+1; i<data.length; i++) {
            if (data[i-1]<data[i]) return i-1;
        }
        return data.length-1;
    }

    public static int findNextPeakLocation(double[] data, int startIndex)
    {
        for (int i=startIndex+1; i<data.length; i++) {
            if (data[i-1]>data[i]) return i-1;
        }
        return data.length-1;
    }
    
    /**
     * Find the maximum of all elements in the array, ignoring elements that are NaN.
     * @param data
     * @return the index number of the maximum element
     */
    public static int findGlobalPeakLocation(double[] data)
    {
        double max = Double.NaN;
        int imax = -1;
        for (int i=0; i<data.length; i++) {
            if (Double.isNaN(data[i])) continue;
            if (Double.isNaN(max)|| data[i] > max) {
                max = data[i];
                imax = i;
            }
        }
        return imax;
    }
    
    /**
     * Find the minimum of all elements in the array, ignoring elements that are NaN.
     * @param data
     * @return the index number of the minimum element
     */
    public static int findGlobalValleyLocation(double[] data)
    {
        double min = Double.NaN;
        int imin = -1;
        for (int i=0; i<data.length; i++) {
            if (Double.isNaN(data[i])) continue;
            if (Double.isNaN(min)|| data[i] < min) {
                min = data[i];
                imin = i;
            }
        }
        return imin;
    }
    
    /**
     * Build the sum of all elements in the array, ignoring elements that are NaN.
     * @param data
     * @return
     */
    public static double sum(double[] data)
    {
        double sum = 0;
        for (int i=0; i<data.length; i++) {
            if (Double.isNaN(data[i])) continue; 
            sum += data[i];
        }
        return sum;
    }
    
    /**
     * Find the maximum of all elements in the array, ignoring elements that are NaN.
     * @param data
     * @return
     */
    public static double max(double[] data)
    {
        double max = Double.NaN;
        for (int i=0; i<data.length; i++) {
            if (Double.isNaN(data[i])) continue;
            if (Double.isNaN(max)|| data[i] > max) max = data[i];
        }
        return max;
    }
    
    public static int max(int[] data)
    {
        int max = data[0];
        for (int i=1; i<data.length; i++) {
            if (data[i] > max) 
                max = data[i];
        }
        return max;
    }

    /**
     * Find the maximum of the absolute values of all elements in the array, ignoring elements that are NaN.
     * @param data
     * @return
     */
    public static double absMax(double[] data)
    {
        double max = Double.NaN;
        for (int i=0; i<data.length; i++) {
            if (Double.isNaN(data[i])) continue;
            double abs = Math.abs(data[i]);
            if (Double.isNaN(max)|| abs > max) max = abs;
        }
        return max;
    }

    /**
     * Find the minimum of all elements in the array, ignoring elements that are NaN.
     * @param data
     * @return
     */
    public static double min(double[] data)
    {
        double min = Double.NaN;
        for (int i=0; i<data.length; i++) {
            if (Double.isNaN(data[i])) continue;
            if (Double.isNaN(min)|| data[i] < min) min = data[i];
        }
        return min;
    }
    
    public static int min(int[] data)
    {
        int min = data[0];
        for (int i=1; i<data.length; i++) {
            if (data[i] < min) 
                min = data[i];
        }
        return min;
    }
    
    /**
     * Compute the mean of all elements in the array. No missing values (NaN) are allowed.
     * @throws IllegalArgumentException if the array contains NaN values. 
     */
    public static double mean(double[] data)
    {
        double mean = 0;
        for (int i=0; i<data.length; i++) {
            if (Double.isNaN(data[i]))
                throw new IllegalArgumentException("NaN not allowed in mean calculation");
            mean += data[i];
        }
        mean /= data.length;
        return mean;
    }
    
    /**
     * Compute the mean of all elements in the array with given indices. No missing values (NaN) are allowed.
     * @throws IllegalArgumentException if the array contains NaN values. 
     */
    public static double mean(double[] data, int [] inds)
    {
        double mean = 0;
        for (int i=0; i<inds.length; i++) {
            if (Double.isNaN(data[inds[i]]))
                throw new IllegalArgumentException("NaN not allowed in mean calculation");

            mean += data[inds[i]];
        }
        mean /= inds.length;
        return mean;
    }

    /**
     * Convert energy from linear scale to db SPL scale (comparing energies to  
     * the minimum audible energy, one Pascal squared).
     * @param energy in time or frequency domain, on a linear energy scale
     * @return energy on a db scale, or NaN if energy is less than or equal to 0.
     */
    public static double dbSPL(double energy)
    {
        if (energy <= 0) return Double.NaN;
        else return 10 * log10(energy/PASCALSQUARE);
    }

    public static double[] dbSPL(double[] energies)
    {
        return multiply(log10(divide(energies, PASCALSQUARE)), 10);
    }

    /**
     * Convert energy from linear scale to db scale.
     * @param energy in time or frequency domain, on a linear energy scale
     * @return energy on a db scale, or NaN if energy is less than or equal to 0.
     */
    public static double db(double energy)
    {
        if (energy <= 0) return Double.NaN;
        else return 10 * log10(energy);
    }

    public static double[] db(double[] energies)
    {
        return multiply(log10(energies), 10);
    }

    /**
     * Convert energy from db scale to linear scale.
     * @param energy in time or frequency domain, on a db energy scale
     * @return energy on a linear scale.
     */
    public static double db2linear(double dbEnergy)
    {
        if (Double.isNaN(dbEnergy)) return 0.;
        else return exp10(dbEnergy/10);
    }

    public static double[] db2linear(double[] dbEnergies)
    {
        return exp10(divide(dbEnergies, 10));
    }

    /**
     * Build the sum of the squared difference of all elements 
     * with the same index numbers in the arrays.
     * @param a
     * @param b
     * @return
     */
    public static double sumSquaredError(double[] a, double[] b)
    {
        if (a.length != b.length) {
            throw new IllegalArgumentException("Arrays must be equal length");
        }
        double sum = 0;
        for (int i=0; i<a.length; i++) {
            double delta = a[i] - b[i];
            sum += delta*delta;
        }
        return sum;
    }

    public static double[] add(double[] a, double[] b)
    {
        if (a.length != b.length) {
            throw new IllegalArgumentException("Arrays must be equal length");
        }
        double[] c = new double[a.length];
        for (int i=0; i<a.length; i++) {
            c[i] = a[i] + b[i];
        }
        return c;        
    }

    public static double[] add(double[] a, double b)
    {
        double[] c = new double[a.length];
        for (int i=0; i<a.length; i++) {
            c[i] = a[i] + b;
        }
        return c;        
    }

    public static double[] substract(double[] a, double[] b)
    {
        if (a.length != b.length) {
            throw new IllegalArgumentException("Arrays must be equal length");
        }
        double[] c = new double[a.length];
        for (int i=0; i<a.length; i++) {
            c[i] = a[i] - b[i];
        }
        return c;
    }
    
    public static double[] substract(double[] a, double b)
    {
        double[] c = new double[a.length];
        for (int i=0; i<a.length; i++) {
            c[i] = a[i] - b;
        }
        return c;
    }
    
    public static double[] multiply(double[] a, double[] b)
    {
        if (a.length != b.length) {
            throw new IllegalArgumentException("Arrays must be equal length");
        }
        double[] c = new double[a.length];
        for (int i=0; i<a.length; i++) {
            c[i] = a[i] * b[i];
        }
        return c;        
    }

    public static double[] multiply(double[] a, double b)
    {
        double[] c = new double[a.length];
        for (int i=0; i<a.length; i++) {
            c[i] = a[i] * b;
        }
        return c;        
    }

    public static double[] divide(double[] a, double[] b)
    {
        if (a.length != b.length) {
            throw new IllegalArgumentException("Arrays must be equal length");
        }
        double[] c = new double[a.length];
        for (int i=0; i<a.length; i++) {
            c[i] = a[i] / b[i];
        }
        return c;
    }
    
    public static double[] divide(double[] a, double b)
    {
        double[] c = new double[a.length];
        for (int i=0; i<a.length; i++) {
            c[i] = a[i] / b;
        }
        return c;
    }
    
    public static double log10(double x)
    {
        return Math.log(x)/LOG10;
    }
    
    public static double[] log(double[] a)
    {
        double[] c = new double[a.length];
        for (int i=0; i<a.length; i++) {
            c[i] = Math.log(a[i]);
        }
        return c;
    }

    public static double[] log10(double[] a)
    {
        double[] c = new double[a.length];
        for (int i=0; i<a.length; i++) {
            c[i] = log10(a[i]);
        }
        return c;
    }

    public static double exp10(double x)
    {
        return Math.exp(LOG10*x);
    }

    public static double[] exp(double[] a)
    {
        double[] c = new double[a.length];
        for (int i=0; i<a.length; i++) {
            c[i] = Math.exp(a[i]);
        }
        return c;
    }

    public static double[] exp10(double[] a)
    {
        double[] c = new double[a.length];
        for (int i=0; i<a.length; i++) {
            c[i] = exp10(a[i]);
        }
        return c;
    }

    /**
     * Convert a pair of arrays from cartesian (x, y) coordinates to 
     * polar (r, phi) coordinates. Phi will be in radians, i.e. a full circle is two pi.
     * @param x as input, the x coordinate; as output, the r coordinate;
     * @param y as input, the y coordinate; as output, the phi coordinate.
     */
    public static void toPolarCoordinates(double[] x, double[] y)
    {
        if (x.length != y.length) {
            throw new IllegalArgumentException("Arrays must be equal length");
        }
        for (int i=0; i<x.length; i++) {
            double r = Math.sqrt(x[i]*x[i] + y[i]*y[i]);
            double phi = Math.atan2(y[i], x[i]);
            x[i] = r;
            y[i] = phi;
        }
    }

    /**
     * Convert a pair of arrays from polar (r, phi) coordinates to
     * cartesian (x, y) coordinates. Phi is in radians, i.e. a whole circle is two pi.
     * @param r as input, the r coordinate; as output, the x coordinate;
     * @param phi as input, the phi coordinate; as output, the y coordinate.
     */
    public static void toCartesianCoordinates(double[] r, double[] phi)
    {
        if (r.length != phi.length) {
            throw new IllegalArgumentException("Arrays must be equal length");
        }
        for (int i=0; i<r.length; i++) {
            double x = r[i] * Math.cos(phi[i]);
            double y = r[i] * Math.sin(phi[i]);
            r[i] = x;
            phi[i] = y;
        }
    }

    /**
     * For a given angle in radians, return the equivalent angle in the range [-PI, PI].
     * @param angle
     * @return
     */
    public static double angleToDefaultAngle(double angle)
    {
        return (angle+Math.PI)%(-TWOPI)+Math.PI;
    }

    /**
     * For each of an array of angles (in radians), return the equivalent angle in the range [-PI, PI].
     * @param angle
     * @return
     */
    public static void angleToDefaultAngle(double[] angle)
    {
        for (int i=0; i<angle.length; i++) {
            angle[i] = angleToDefaultAngle(angle[i]);
        }
    }
    
    /**
     * This is the Java source code for a Levinson Recursion.
     * from http://www.nauticom.net/www/jdtaft/JavaLevinson.htm
     * @param r contains the autocorrelation lags as input [r(0)...r(m)].
     * @param m
     * @return the array of whitening coefficients
     */
    public static double[] levinson(double[] r, int m)
    {
        // The matrix l is unit lower triangular.
        // It's i-th row contains upon completion the i-th prediction error filter,
        // with the coefficients in reverse order. The vector e contains upon 
        // completion the prediction errors.
        // The last section extracts the maximum length whitening filter
        // coefficients from matrix l.
        int i;
        int j;
        int k;
        double gap;
        double gamma;
        double e[] = new double[m + 1];
        double l[][] = new double[m + 1][m + 1];
        double[] coeffs = new double[m+1];
        
        /* compute recursion  */
        for (i = 0; i <= m; i++) {
            for (j = i + 1; j <= m; j++) {
                l[i][j] = 0.;
            }
        }
        l[0][0] = 1.;
        l[1][1] = 1.;
        l[1][0] = -r[1] / r[0];
        e[0] = r[0];
        e[1] = e[0] * (1. - l[1][0] * l[1][0]);
        for (i = 2; i <= m; i++) {
            gap = 0.;
            for (k = 0; k <= i - 1; k++) {
                gap += r[k + 1] * l[i - 1][k];
            }
            gamma = gap / e[i - 1];
            l[i][0] = -gamma;
            for (k = 1; k <= i - 1; k++) {
                l[i][k] = l[i - 1][k - 1] - gamma * l[i - 1][i - 1 - k];
            }
            l[i][i] = 1.;
            e[i] = e[i - 1] * (1. - gamma * gamma);
        }
        /* extract length-m whitening filter coefficients  */
        coeffs[0] = 1.;
        for (i = 1; i <= m; i++) {
            coeffs[i] = l[m][m - i];
        }
/*        double sum = 0.;
        for (i = 0; i < m; i++) {
            sum += coeffs[i];
        }
        for (i = 0; i < m; i++) {
            coeffs[i] = coeffs[i] / sum;
        }
*/
        return coeffs;
    }

    public static class Complex {
        public double [] real;
        public double [] imag;
        
        public Complex(int len)
        {
            if (len>0)
            {
                this.real = new double[len];
                this.imag = new double[len];
            }
            else
            {
                this.real = null;
                this.imag = null;
            }
        }
    }
    
    /* Performs interpolation to increase or decrease the size of array x
       to newLength*/
    public static double [] interpolate(double [] x, int newLength)
    {
        double [] y = null;
        if (newLength>0)
        {
            int N = x.length;
            if (N==1)
            {
                y = new double[1];
                y[0]=x[0];
                return y;
            }
            else if (newLength==1)
            {
                y = new double[1];
                int ind = (int)Math.floor(N*0.5+0.5);
                ind = Math.max(1, ind);
                ind = Math.min(ind, N);
                y[0]= x[ind-1];
                return y;
            }
            else
            {
                y = new double[newLength];
                double Beta = ((float)newLength)/N;
                double newBeta = 1.0;
                
                if (newLength>2)
                    newBeta=(N-2.0)/(newLength-2.0);

                y[0] = x[0];
                y[1] = x[1];
                y[newLength-1] = x[N-1];
                
                double tmp, alpha;
                int i, j;
                for (i=2; i<=newLength-2; i++) 
                {
                    tmp = 1.0+(i-1)*newBeta;
                    j = (int)Math.floor(tmp);
                    alpha = tmp-j;
                    y[i] = (1.0-alpha)*x[Math.max(0,j)] + alpha*x[Math.min(N-1,j+1)];
                }
            }
        }
        
        return y;
    }
    
    public static int getMax(int [] x)
    {
        int maxx = x[0];
        for (int i=1; i<x.length; i++)
        {
            if (x[i]>maxx)
                maxx = x[i];
        }
        
        return maxx;
    }
    
    public static double getMax(double [] x)
    {
        double maxx = x[0];
        for (int i=1; i<x.length; i++)
        {
            if (x[i]>maxx)
                maxx = x[i];
        }
        
        return maxx;
    }
    
    public static double getAbsMax(double [] x)
    {
        double maxx = Math.abs(x[0]);
        for (int i=1; i<x.length; i++)
        {
            if (Math.abs(x[i])>maxx)
                maxx = Math.abs(x[i]);
        }
        
        return maxx;
    }
    
    //Return an array where each entry is set to val
    public static double [] filledArray(double val, int len)
    {
        double [] x = null;
        
        if (len>0)
        {
            x = new double[len];
            for (int i=0; i<len; i++)
                x[i] = val;
        }
        
        return x;
    }
    
    //Return an array where each entry is set to val
    public static int [] filledArray(int val, int len)
    {
        int [] x = null;
        
        if (len>0)
        {
            x = new int[len];
            for (int i=0; i<len; i++)
                x[i] = val;
        }
        
        return x;
    }
    
    //Return an array filled with 0´s
    public static double [] zeros(int len)
    {
        return filledArray(0.0, len);
    }
    
    //Return an array filled with 1´s
    public static double [] ones(int len)
    {
        return filledArray(1.0, len);
    }
    
//  Return an array filled with 0´s
    public static int [] zerosInt(int len)
    {
        return filledArray(0, len);
    }
    
    //Return an array filled with 1´s
    public static int [] onesInt(int len)
    {
        return filledArray(1, len);
    }
    
    public static int [] find(double[] x, int comparator, double val)
    {
        int [] inds = null;
        int totalFound = 0;
        
        switch (comparator)
        {
        case EQUALS:
            for (int i=0; i<x.length; i++)
            {
                if (x[i]==val)
                    totalFound++;
            }
            break;
        case GREATER_THAN:
            for (int i=0; i<x.length; i++)
            {
                if (x[i]>val)
                    totalFound++;
            }
            break;
        case GREATER_THAN_OR_EQUALS:
            for (int i=0; i<x.length; i++)
            {
                if (x[i]>=val)
                    totalFound++;
            }
            break;
        case LESS_THAN:
            for (int i=0; i<x.length; i++)
            {
                if (x[i]<val)
                    totalFound++;
            }
            break;
        case LESS_THAN_OR_EQUALS:
            for (int i=0; i<x.length; i++)
            {
                if (x[i]<=val)
                    totalFound++;
            }
            break;
        case NOT_EQUALS:
            for (int i=0; i<x.length; i++)
            {
                if (x[i]!=val)
                    totalFound++;
            }
            break;
        }
        
        if (totalFound>0)
        {
            int currentInd = 0;
            inds = new int[totalFound];
            
            switch (comparator)
            {
            case EQUALS:
                for (int i=0; i<x.length; i++)
                {
                    if (x[i]==val)
                    {
                        inds[currentInd++] = i;
                        totalFound++;
                    }
                }
                break;
            case GREATER_THAN:
                for (int i=0; i<x.length; i++)
                {
                    if (x[i]>val)
                    {
                        inds[currentInd++] = i;
                        totalFound++;
                    }
                }
                break;
            case GREATER_THAN_OR_EQUALS:
                for (int i=0; i<x.length; i++)
                {
                    if (x[i]>=val)
                    {
                        inds[currentInd++] = i;
                        totalFound++;
                    }
                }
                break;
            case LESS_THAN:
                for (int i=0; i<x.length; i++)
                {
                    if (x[i]<val)
                    {
                        inds[currentInd++] = i;
                        totalFound++;
                    }
                }
                break;
            case LESS_THAN_OR_EQUALS:
                for (int i=0; i<x.length; i++)
                {
                    if (x[i]<=val)
                    {
                        inds[currentInd++] = i;
                        totalFound++;
                    }
                }
                break;
            case NOT_EQUALS:
                for (int i=0; i<x.length; i++)
                {
                    if (x[i]!=val)
                    {
                        inds[currentInd++] = i;
                        totalFound++;
                    }
                }
                break;
            }
        }
        
        return inds;
    }
    
    public static double [] interpolate_linear(int [] x, double [] y, int [] xi)
    {
        assert(x.length==y.length);
        
        double [] yi = new double[xi.length];
        int i, j;
        boolean bFound;
        double alpha;
        
        for (i=0; i<xi.length; i++)
        {
            bFound = false;
            for (j=0; j<x.length-1; j++)
            {
                if (xi[i]>=x[j] && xi[i]<x[j+1])
                {
                    bFound = true;
                    break;
                }
            }
            
            if (bFound)
            {
                alpha = (((double)xi[i])-x[j])/(x[j+1]-x[j]);
                yi[i] = (1-alpha)*y[j] + alpha*y[j+1];
            }
        }
        
        if (xi[xi.length-1]==x[x.length-1])
            yi[xi.length-1] = y[x.length-1];
        
        return yi;
    }
    
    public static int CheckLimits(int val, int minVal, int maxVal)
    {
        int ret = val;

        if (ret<minVal)
            ret = minVal;
        
        if (ret>maxVal)
            ret = maxVal;
        
        return ret;
    }
    
    public static double CheckLimits(double val, double minVal, double maxVal)
    {
        double ret = val;

        if (ret<minVal)
            ret = minVal;
        
        if (ret>maxVal)
            ret = maxVal;
        
        return ret;
    }
    
    public static float CheckLimits(float val, float minVal, float maxVal)
    {
        float ret = val;

        if (ret<minVal)
            ret = minVal;
        
        if (ret>maxVal)
            ret = maxVal;
        
        return ret;
    }
}
