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

import de.dfki.lt.signalproc.util.DoubleDataSource;
import de.dfki.lt.signalproc.util.MathUtils;
import de.dfki.lt.signalproc.window.RectWindow;

/**
 * A class that analyses the energy distribution, and computes a silence cutoff threshold,
 * in the dB energy domain.
 * @author Marc Schr&ouml;der
 *
 */
public class EnergyAnalyser_dB extends EnergyAnalyser {
    public EnergyAnalyser_dB(DoubleDataSource signal, int framelength, int samplingRate)
    {
        super(signal, framelength, samplingRate);
    }

    public EnergyAnalyser_dB(DoubleDataSource signal, int framelength, int frameShift, int samplingRate)
    {
        super(signal, framelength, frameShift, samplingRate);
    }

    public EnergyAnalyser_dB(DoubleDataSource signal, int framelength, int frameShift, int samplingRate, int maxSize)
    {
        super(signal, framelength, frameShift, samplingRate, maxSize);
    }

    /**
     * Apply this FrameBasedAnalyser to the given data.
     * @param frame the data to analyse, which must be of the length prescribed by this
     * FrameBasedAnalyser, i.e. by @see{#getFrameLengthSamples()}.
     * @return a Double representing the total energy in the frame.
     * @throws IllegalArgumentException if frame does not have the prescribed length 
     */
    public Object analyse(double[] frame)
    {
        if (frame.length != getFrameLengthSamples())
            throw new IllegalArgumentException("Expected frame of length " + getFrameLengthSamples()
                    + ", got " + frame.length);
        double totalEnergy = 0;
        for (int i=0; i<frame.length; i++) {
            if (frame[i] != 0) 
                totalEnergy += MathUtils.db(frame[i] * frame[i]);
            // for energy 0, ignore
        }
        rememberFrameEnergy(totalEnergy);
        return new Double(totalEnergy);
    }
}
