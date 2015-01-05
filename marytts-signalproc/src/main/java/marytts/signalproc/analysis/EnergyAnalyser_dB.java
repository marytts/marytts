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
package marytts.signalproc.analysis;

import marytts.util.data.DoubleDataSource;
import marytts.util.math.MathUtils;

/**
 * 
 * @author Marc Schr&ouml;der
 * 
 *         A class that analyses the energy distribution, and computes a silence cutoff threshold, in the dB energy domain.
 * 
 */
public class EnergyAnalyser_dB extends EnergyAnalyser {
	public EnergyAnalyser_dB(DoubleDataSource signal, int framelength, int samplingRate) {
		super(signal, framelength, samplingRate);
	}

	public EnergyAnalyser_dB(DoubleDataSource signal, int framelength, int frameShift, int samplingRate) {
		super(signal, framelength, frameShift, samplingRate);
	}

	public EnergyAnalyser_dB(DoubleDataSource signal, int framelength, int frameShift, int samplingRate, int maxSize) {
		super(signal, framelength, frameShift, samplingRate, maxSize);
	}

	/**
	 * Apply this FrameBasedAnalyser to the given data.
	 * 
	 * @param frame
	 *            the data to analyse, which must be of the length prescribed by this FrameBasedAnalyser, i.e. by
	 *            {@link #getFrameLengthSamples()}.
	 * @return a Double representing the total energy in the frame.
	 * @throws IllegalArgumentException
	 *             if frame does not have the prescribed length
	 */
	public Double analyse(double[] frame) {
		if (frame.length != getFrameLengthSamples())
			throw new IllegalArgumentException("Expected frame of length " + getFrameLengthSamples() + ", got " + frame.length);
		double totalEnergy = 0;
		for (int i = 0; i < frame.length; i++) {
			if (frame[i] != 0)
				totalEnergy += MathUtils.db(frame[i] * frame[i]);
			// for energy 0, ignore
		}
		rememberFrameEnergy(totalEnergy);
		return new Double(totalEnergy);
	}
}
