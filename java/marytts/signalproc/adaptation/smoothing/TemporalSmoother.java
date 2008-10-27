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

package marytts.signalproc.adaptation.smoothing;

import marytts.signalproc.window.DynamicWindow;
import marytts.signalproc.window.Window;

/**
 * @author oytun.turk
 *
 * Temporal smoother can be used to smooth LSF contours, spectral envelope, etc
 *
 */
public class TemporalSmoother {
    public static final int DEFAULT_NUM_NEIGHBOURS = 4;
    public static final int DEFAULT_SMOOTHING_WINDOW = Window.HAMMING;
    
    public static double[] smooth(double[]x, int neighbours)
    {
        return smooth(x, neighbours, DEFAULT_SMOOTHING_WINDOW);
    }
    
    public static double[] smooth(double[]x, int neighbours, int windowType)
    {
        int i;
        double[][] xx = new double[x.length][1];
        for (i=0; i<x.length; i++)
            xx[i][0] = x[i];
        xx = smooth(xx, neighbours, windowType);
        
        double[] y = new double[x.length];
        for (i=0; i<x.length; i++)
           y[i] = xx[i][0];
       
        return y;
    }
    
    public static double[][] smooth(double[][] x, int neighbours)
    {
        return smooth(x, neighbours, DEFAULT_SMOOTHING_WINDOW);
    }
    
    //Smooth along each column
    // i.e. each row corresponds to acoustic features of one frame at a specific instant of time
    // Windowing based weighting is used
    public static double[][] smooth(double[][] x, int neighbours, int windowType)
    {
        if (neighbours<=0)
            return x;
        else
        {
            double[][] y = new double[x.length][x[0].length];
            int i, j, k;
            int windowSize = 2*neighbours+1;
            DynamicWindow w = new DynamicWindow(windowType);
            double[] weights = w.values(windowSize);
            double weightSum;
            
            for (i=1; i<x.length; i++)
                assert x[i].length==x[0].length;
            
            for (i=0; i<x[0].length; i++)
            {
                for (j=0; j<x.length; j++)
                {
                    y[j][i] = 0.0;
                    weightSum = 0.0;
                    for (k=-neighbours; k<=neighbours; k++)
                    {
                        if (j+k>=0 && j+k<x.length)
                        {
                            y[j][i] += weights[k+neighbours]*x[j+k][i];
                            weightSum += weights[k+neighbours];
                        }
                    }
                    
                    if (weightSum>0.0)
                        y[j][i] /= weightSum;
                }
            }
            
            return y;
        }
    }
    
    public static void main(String[] args)
    {
        double[][] xx = new double[10][2];
        xx[0][0]=100.0; xx[0][1]=220.0;
        xx[1][0]=110.0; xx[1][1]=210.0;
        xx[2][0]=150.0; xx[2][1]=230.0;
        xx[3][0]=90.0;  xx[3][1]=220.0;
        xx[4][0]=80.0;  xx[4][1]=250.0;
        xx[5][0]=120.0; xx[5][1]=260.0;
        xx[6][0]=140.0; xx[6][1]=290.0;
        xx[7][0]=180.0; xx[7][1]=300.0;
        xx[8][0]=150.0; xx[8][1]=340.0;
        xx[9][0]=120.0; xx[9][1]=320.0;
        
        double[][] y = TemporalSmoother.smooth(xx, 3);
        
        System.out.println("Finished");
    }
}
