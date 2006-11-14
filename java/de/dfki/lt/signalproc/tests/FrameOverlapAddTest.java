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

import de.dfki.lt.signalproc.process.FrameOverlapAddSource;
import de.dfki.lt.signalproc.process.NaiveVocoder;
import de.dfki.lt.signalproc.util.BufferedDoubleDataSource;
import de.dfki.lt.signalproc.util.MathUtils;
import junit.framework.TestCase;


public class FrameOverlapAddTest extends TestCase
{

    public void testIdentity()
    {
        double[] signal =  FFTTest.getSampleSignal(16000);
        int samplingRate = 8000;
        FrameOverlapAddSource ola = new FrameOverlapAddSource(new BufferedDoubleDataSource(signal), 2048, samplingRate, null);
        double[] result = ola.getAllData();
        double err = MathUtils.sumSquaredError(signal, result);
        assertTrue("Error: "+err, err<1.E-19);
    }

    public void testStretch1()
    {
        double[] signal =  FFTTest.getSampleSignal(2048+128);
        int samplingRate = 8000;
        double rateFactor = 0.5;
        NaiveVocoder nv = new NaiveVocoder(new BufferedDoubleDataSource(signal), samplingRate, rateFactor);
        double[] result = nv.getAllData();
        int expectedLength = nv.computeOutputLength(signal.length);
        assertTrue("Expected result length: " + expectedLength +", found: "+result.length,
                result.length == expectedLength);
    }

    public void testStretch2()
    {
        double[] signal =  FFTTest.getSampleSignal(16000);
        int samplingRate = 8000;
        double rateFactor = 0.5;
        NaiveVocoder nv = new NaiveVocoder(new BufferedDoubleDataSource(signal), samplingRate, rateFactor);
        double[] result = nv.getAllData();
        double meanSignalEnergy = MathUtils.mean(MathUtils.multiply(signal, signal));
        double meanResultEnergy = MathUtils.mean(MathUtils.multiply(result, result));
        double percentDifference = Math.abs(meanSignalEnergy-meanResultEnergy)/meanSignalEnergy*100;
        assertTrue("Stretching changed signal energy by  "+percentDifference+"%",
                percentDifference<6);
    }

}
