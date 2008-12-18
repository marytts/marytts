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

package marytts.util.math;

/**
 * A collection of static helper functions for dealing with arrays.
 *  * @author Marc Schr&ouml;der
 *
 */
public class ArrayUtils
{
    public static double[] subarray(double[] orig, int off, int len)
    {
        if (off+len>orig.length) throw new IllegalArgumentException("requested subarray exceeds array length");
        double[] sub = new double[len];
        System.arraycopy(orig, off, sub, 0, len);
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
}
