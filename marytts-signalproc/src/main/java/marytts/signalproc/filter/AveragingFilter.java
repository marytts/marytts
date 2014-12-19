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
package marytts.signalproc.filter;

import marytts.util.math.MathUtils;

/**
 * @author Marc Schr&ouml;der
 * 
 */
public class AveragingFilter extends FIRFilter {
	public AveragingFilter(double lengthInSeconds, int samplingRate) {
		this((int) (lengthInSeconds * samplingRate));
	}

	public AveragingFilter(int lengthInSamples) {
		int fftLength = MathUtils.closestPowerOfTwoAbove(lengthInSamples);
		if (fftLength == lengthInSamples)
			fftLength *= 2;
		double[] impulseResponse = new double[lengthInSamples];
		// Straight impulse response:
		for (int i = 0; i < lengthInSamples; i++) {
			impulseResponse[i] = 1. / lengthInSamples;
		}
		initialise(impulseResponse, fftLength - lengthInSamples);
	}

}
