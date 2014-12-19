/**
 * Copyright 2007 DFKI GmbH.
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
package marytts.signalproc.sinusoidal.hntm.synthesis;

import marytts.util.math.ArrayUtils;
import marytts.util.signal.SignalProcUtils;

/**
 * @author oytun.turk
 * 
 */
public class HntmSynthesizedSignal {
	public double[] harmonicPart;
	public double[] noisePart;
	public double[] transientPart;
	public double[] output;

	public HntmSynthesizedSignal() {
		harmonicPart = null;
		noisePart = null;
		transientPart = null;
		output = null;
	}

	public void concatToHarmonicPart(double[] newData) {
		harmonicPart = ArrayUtils.combine(harmonicPart, newData);
	}

	public void concatToNoisePart(double[] newData) {
		noisePart = ArrayUtils.combine(noisePart, newData);
	}

	public void concatToTransientPart(double[] newData) {
		transientPart = ArrayUtils.combine(transientPart, newData);
	}

	public void concat(HntmSynthesizedSignal newSignal) {
		concatToHarmonicPart(newSignal.harmonicPart);
		concatToNoisePart(newSignal.noisePart);
		concatToTransientPart(newSignal.transientPart);
	}

	public void generateOutput() {
		output = SignalProcUtils.addSignals(harmonicPart, noisePart);
		output = SignalProcUtils.addSignals(output, transientPart);
	}
}
