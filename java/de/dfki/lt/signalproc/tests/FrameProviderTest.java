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

import de.dfki.lt.signalproc.process.FrameProvider;
import de.dfki.lt.signalproc.util.BufferedDoubleDataSource;
import de.dfki.lt.signalproc.util.MathUtils;
import junit.framework.TestCase;

public class FrameProviderTest extends TestCase
{
    public void testIdentity1()
    {
        // Signal length not a multiple of the frame length/shift:
        double[] signal = FFTTest.getSampleSignal(10000);
        int samplingRate = 8000;
        // Set stopWhenTouchingEnd to false: always read only the first frameShift samples in each frame:
        FrameProvider fp = new FrameProvider(new BufferedDoubleDataSource(signal), null, 2048, 512, samplingRate, false);
        double[] result = new double[signal.length];
        int resultPos = 0;
        while (fp.hasMoreData()) {
            double[] frame = fp.getNextFrame();
            int toCopy = fp.validSamplesInFrame() >= fp.getFrameShiftSamples() ?
                    fp.getFrameShiftSamples() : fp.validSamplesInFrame();
            System.arraycopy(frame, 0, result, resultPos, toCopy);
            resultPos += toCopy;
        }
        assertTrue("Got back "+resultPos+", expected "+signal.length, resultPos==signal.length);
        double err = MathUtils.sumSquaredError(signal, result);
        assertTrue("Error: " + err, err < 1.E-20);
    }

    public void testIdentity2()
    {
        // Signal length not a multiple of the frame length/shift:
        double[] signal = FFTTest.getSampleSignal(10000);
        int samplingRate = 8000;
        // Set stopWhenTouchingEnd to true: read only the first frameShift samples in each frame,
        // except in the last frame, which is read in full:
        FrameProvider fp = new FrameProvider(new BufferedDoubleDataSource(signal), null, 2048, 512, samplingRate, true);
        double[] result = new double[signal.length];
        int resultPos = 0;
        while (fp.hasMoreData()) {
            double[] frame = fp.getNextFrame();
            int toCopy = fp.validSamplesInFrame() == fp.getFrameLengthSamples() ?
                    fp.getFrameShiftSamples() : fp.validSamplesInFrame();
            System.arraycopy(frame, 0, result, resultPos, toCopy);
            resultPos += toCopy;
        }
        assertTrue("Got back "+resultPos+", expected "+signal.length, resultPos==signal.length);
        double err = MathUtils.sumSquaredError(signal, result);
        assertTrue("Error: " + err, err < 1.E-20);
    }

    public void testIdentity3()
    {
        // Signal length a multiple of the frame length/shift:
        double[] signal = FFTTest.getSampleSignal(10240);
        int samplingRate = 8000;
        // Set stopWhenTouchingEnd to false: always read only the first frameShift samples in each frame:
        FrameProvider fp = new FrameProvider(new BufferedDoubleDataSource(signal), null, 2048, 512, samplingRate, false);
        double[] result = new double[signal.length];
        int resultPos = 0;
        while (fp.hasMoreData()) {
            double[] frame = fp.getNextFrame();
            int toCopy = fp.validSamplesInFrame() >= fp.getFrameShiftSamples() ?
                    fp.getFrameShiftSamples() : fp.validSamplesInFrame();
            System.arraycopy(frame, 0, result, resultPos, toCopy);
            resultPos += toCopy;
        }
        assertTrue("Got back "+resultPos+", expected "+signal.length, resultPos==signal.length);
        double err = MathUtils.sumSquaredError(signal, result);
        assertTrue("Error: " + err, err < 1.E-20);
    }

    public void testIdentity4()
    {
        // Signal length a multiple of the frame length/shift:
        double[] signal = FFTTest.getSampleSignal(10240);
        int samplingRate = 8000;
        // Set stopWhenTouchingEnd to true: read only the first frameShift samples in each frame,
        // except in the last frame, which is read in full:
        FrameProvider fp = new FrameProvider(new BufferedDoubleDataSource(signal), null, 2048, 512, samplingRate, true);
        double[] result = new double[signal.length];
        int resultPos = 0;
        while (fp.hasMoreData()) {
            double[] frame = fp.getNextFrame();
            int toCopy = fp.hasMoreData() ?
                    fp.getFrameShiftSamples() : fp.validSamplesInFrame();
            System.arraycopy(frame, 0, result, resultPos, toCopy);
            resultPos += toCopy;
        }
        assertTrue("Got back "+resultPos+", expected "+signal.length, resultPos==signal.length);
        double err = MathUtils.sumSquaredError(signal, result);
        assertTrue("Error: " + err, err < 1.E-20);
    }

}
