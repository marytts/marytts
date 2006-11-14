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

import de.dfki.lt.signalproc.FFT;
import de.dfki.lt.util.ComplexArray;
import de.dfki.lt.signalproc.util.MathUtils;

/**
 * @author Marc Schr&ouml;der
 *
 */
public class CepstrumAnalyser
{
    public static double[] calcRealCepstrum(double[] frame)
    {
        return calcComplexCepstrum(frame).real;
    }
    
    public static ComplexArray calcComplexCepstrum(double[] frame)
    {
        int N = MathUtils.closestPowerOfTwoAbove(frame.length);
        double[] real = new double[N];
        double[] imag = new double[N];
        System.arraycopy(frame, 0, real, 0, frame.length);
        FFT.transform(real, imag, false);
        // Now real + j*imag is the complex spectrum
        MathUtils.toPolarCoordinates(real, imag);
        // now real = abs(X), imag = phi
        real = MathUtils.log(real);
        FFT.transform(real, imag, true);
        // Now real + j*imag is the complex cepstrum
        return new ComplexArray(real, imag);
    }

    public static double[] filterLowPass(double[] realCepstrum, int cutoffIndex)
    {
        double[] filtered = new double[realCepstrum.length];
        filtered[0] = realCepstrum[0];
        filtered[cutoffIndex] = realCepstrum[cutoffIndex];
        for (int i=1; i< cutoffIndex; i++) {
            filtered[i] = 2*realCepstrum[i];
        }
        return filtered;
    }
}
