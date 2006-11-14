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

package de.dfki.lt.signalproc.tests;

import de.dfki.lt.signalproc.FFT;
import de.dfki.lt.signalproc.display.FunctionGraph;
import de.dfki.lt.signalproc.filter.FIRFilter;
import de.dfki.lt.signalproc.util.BufferedDoubleDataSource;
import de.dfki.lt.signalproc.util.DoubleDataSource;
import de.dfki.lt.signalproc.util.MathUtils;
import junit.framework.Assert;
import junit.framework.TestCase;

/**
 * @author Marc Schr&ouml;der
 *
 */
public class FFTTest extends TestCase
{
    protected int LEN=1024;
    protected int ONE=LEN/10;
    protected double[] x1;
    protected double[] x2;
    protected double[] y;
    
    protected FunctionGraph showGraph(double[] array, String title)
    {
        FunctionGraph graph = new FunctionGraph(400, 200, 0, 1./ONE, array);
        graph.showInJFrame(title,500, 300, true, false);
        try { Thread.sleep(1000); } catch (InterruptedException e) {}
        return graph;
    }

    public void setUp()
    {
        x1 = new double[ONE];
        for (int i=0; i<ONE; i++)
            x1[i] = 2*(1-(double)i/ONE);
        x2 = new double[LEN];
        for (int i=0; i<LEN; i++)
            x2[i] = (double)i/LEN;
        y = FFT.convolveWithZeroPadding(x1, x2, 1./ONE);

    }

    public void testTransform()
    {
        double[] signal = getSampleSignal(1024);
        double[] real = new double[signal.length];
        System.arraycopy(signal, 0, real, 0, signal.length);
        double[] imag = new double[signal.length];
        FFT.transform(real, imag, false);
        FFT.transform(real, imag, true);
        double err = MathUtils.sumSquaredError(signal, real);
        Assert.assertTrue("Error: "+err, err<1.E-16);
    }
    
    public void testConvolution()
    {
        Assert.assertTrue(y.length==x1.length+x2.length);
    }

    public void testFIRConvolution()
    {
        double[] signal = x2;
        double[] ir = x1;
        double[] resultingSignal = new double[signal.length+ir.length];
        int shift = LEN/2 - ir.length;
        FIRFilter filter = new FIRFilter(ir, shift);
        DoubleDataSource filtered = filter.apply(new BufferedDoubleDataSource(signal));
        int pos = 0;
        int iteration = 1;
        while (filtered.hasMoreData()) {
            int read = filtered.getData(resultingSignal, pos, shift);
            pos += read;
            //showGraph(resultingSignal, "resultingSignal after iteration " + (iteration++));
        }
        for (int i=0; i<resultingSignal.length; i++) {
            resultingSignal[i] *= 1./ONE;
        }
        double err = MathUtils.sumSquaredError(y, resultingSignal);
        Assert.assertTrue("Error: "+err, err<1.E-20);
    }
    
    public static double[] getSampleSignal(int length)
    {
        double[] signal = new double[length];
        for (int i=0; i<length; i++) {
            signal[i] = Math.round(10000 * Math.sin(2*Math.PI*i/length));
        }
        return signal;
    }

}
