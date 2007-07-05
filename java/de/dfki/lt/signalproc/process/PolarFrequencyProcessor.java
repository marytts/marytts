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

package de.dfki.lt.signalproc.process;

import de.dfki.lt.signalproc.util.MathUtils;

/**
 * @author Marc Schr&ouml;der
 *
 */
public class PolarFrequencyProcessor extends FrequencyDomainProcessor
{

    /**
     * @param fftSize
     */
    public PolarFrequencyProcessor(int fftSize, double amount)
    {
        super(fftSize, amount);
        //super(fftSize);
    }

    public PolarFrequencyProcessor(int fftSize)
    {
        this(fftSize, 1.0);
    }
    
    /**
     * Here the actual processing of the frequency-domain frame (in cartesian coordinates) happens.
     * This implementation converts to polar coordinates calls processPolar(), and
     * converts the result back to cartesian coordinates.
     * @param real
     * @param imag
     */
    protected final void process(double[] real, double[] imag)
    {
        MathUtils.toPolarCoordinates(real, imag);
        // for readability:
        double[] r = real;
        double[] phi = imag;
        // Now do something meaningful with the fourier transform
        processPolar(r, phi);
        // Convert back:
        MathUtils.toCartesianCoordinates(real, imag);
    }
    
    /**
     * Here the actual processing of the frequency-domain frame (in polar coordinates) happens.
     * This base implementation does nothing.
     * @param r
     * @param phi
     */
    protected void processPolar(double[] r, double[] phi)
    {
    }
}
