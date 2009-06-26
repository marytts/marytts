/**
 * Copyright 2004-2006 DFKI GmbH.
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

/**
 * A collection of static helper functions for dealing with arrays.
 *  * @author Marc Schr&ouml;der
 *
 */
public class ArrayUtils
{
    public static double[] copyFloat2Double(float[] orig)
    {
        if (orig!=null)
            return subarrayd(orig, 0, orig.length);
        else
            return null;
    }
    
    public static float[] copyDouble2Float(double[] orig)
    {
        if (orig!=null)
            return subarrayf(orig, 0, orig.length);
        else
            return null;
    }
    
    public static double[] copyShort2Double(short[] orig)
    {
        if (orig!=null)
            return subarrays(orig, 0, orig.length);
        else
            return null;
    }
    
    public static double[] copy(double[] orig)
    {
        if (orig!=null)
            return subarray(orig, 0, orig.length);
        else
            return null;
    }
    
    public static float[] copy(float[] orig)
    {
        if (orig!=null)
            return subarray(orig, 0, orig.length);
        else
            return null;
    }
    
    public static int[] copy(int[] orig)
    {
        if (orig!=null)
            return subarray(orig, 0, orig.length);
        else
            return null;
    }
    
    public static float[] copyf(double[] orig)
    {
        if (orig!=null)
            return subarrayf(orig, 0, orig.length);
        else
            return null;
    }
    
    public static ComplexNumber[] copy(ComplexNumber[] orig)
    {
        ComplexNumber[] out = null;
        
        if (orig!=null)
        {
            out = new ComplexNumber[orig.length];
            for (int i=0; i<orig.length; i++)
                out[i] = new ComplexNumber(orig[i]);
        }
        
        return out;
    }
    
    public static double[] subarrays(short[] orig, int off, int len)
    {
        if (off+len>orig.length) throw new IllegalArgumentException("requested subarray exceeds array length");
        double[] sub = new double[len];
        for (int i=0; i<len; i++)
            sub[i] = orig[i+off];
        
        return sub;
    }
    
    public static double[] subarrayd(float[] orig, int off, int len)
    {
        if (off+len>orig.length) throw new IllegalArgumentException("requested subarray exceeds array length");
        double[] sub = new double[len];
        for (int i=0; i<len; i++)
            sub[i] = orig[i+off];
        
        return sub;
    }
    
    public static double[] subarray(double[] orig, int off, int len)
    {
        if (off+len>orig.length) throw new IllegalArgumentException("requested subarray exceeds array length");
        double[] sub = new double[len];
        System.arraycopy(orig, off, sub, 0, len);
        return sub;
    }
    
    public static float[] subarrayf(double[] orig, int off, int len)
    {
        if (off+len>orig.length) throw new IllegalArgumentException("requested subarray exceeds array length");
        float[] sub = new float[len];
        for (int i=0; i<len; i++)
            sub[i] = (float)orig[i+off];

        return sub;
    }
    
    public static float[] subarray(float[] orig, int off, int len)
    {
        if (off+len>orig.length) throw new IllegalArgumentException("requested subarray exceeds array length");
        float[] sub = new float[len];
        for (int i=0; i<len; i++)
            sub[i] = orig[i+off];

        return sub;
    }
    
    public static int[] subarray(int[] orig, int off, int len)
    {
        if (off+len>orig.length) throw new IllegalArgumentException("requested subarray exceeds array length");
        int[] sub = new int[len];
        for (int i=0; i<len; i++)
            sub[i] = orig[i+off];

        return sub;
    }
    
    //Returns true if val is at least once contained in array
    //Otherwise returns false
    public static boolean isOneOf(int[] array, int val)
    {
        boolean ret = false;
        for (int i=0; i<array.length; i++)
        {
            if (array[i]==val)
            {
                ret = true;
                break;
            }
        }
        
        return ret;
    }

    //Appends val to the beginning of array
    public static int[] appendToStart(int[] array, int val)
    {
        int len = 1;
        if (array!=null)
            len += array.length;

        int[] arrayOut = new int[len];
        arrayOut[0] = val;
        if (array!=null)
            System.arraycopy(array, 0, arrayOut, 1, array.length);
        
        return arrayOut;
    }
    
    //Appends val to the end of array
    public static int[] appendToEnd(int[] array, int val)
    {
        int len = 1;
        if (array!=null)
            len += array.length;

        int[] arrayOut = new int[len];
        arrayOut[len-1] = val;
        if (array!=null)
            System.arraycopy(array, 0, arrayOut, 0, array.length);
        
        return arrayOut;
    }
    
    public static double[] toDoubleArray(float[] x)
    {
        double[] xd = null;

        if (x!=null && x.length>0)
        {
            xd = new double[x.length];
            for (int i=0; i<x.length; i++)
                xd[i] = x[i];
        }
        
        return xd;
    }
    
    public static float[] toFloatArray(double[] x)
    {
        float[] xf = null;

        if (x!=null && x.length>0)
        {
            xf = new float[x.length];
            for (int i=0; i<x.length; i++)
                xf[i] = (float)x[i];
        }
        
        return xf;
    }
    
    //Returns the vector [x y]
    public static float[] combine(float[] x, float[] y)
    {
        int len = 0;
        if (x!=null)
            len += x.length;
        if (y!=null)
            len += y.length;
        
        float[] z = null;
        
        if (len>0)
        {
            z = new float[len];

            int currentPos = 0;
            if (x!=null)
            {
                System.arraycopy(x, 0, z, currentPos, x.length);
                currentPos = x.length;
            }

            if (y!=null)
                System.arraycopy(y, 0, z, currentPos, y.length);
        }
        
        return z;
    }

    public static boolean isZero(float[] array)
    {
        boolean isZero = true;
        for (int j=0; j<array.length; j++) {
            if (array[j] != 0) {
                isZero = false;
                break;
            }
        }
        return isZero;
    }

    public static boolean isZero(double[] array)
    {
        boolean isZero = true;
        for (int j=0; j<array.length; j++) {
            if (array[j] != 0) {
                isZero = false;
                break;
            }
        }
        return isZero;
    }

}



