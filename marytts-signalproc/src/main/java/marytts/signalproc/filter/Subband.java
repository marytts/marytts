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
package marytts.signalproc.filter;

/**
 * @author Oytun T&uuml;rk
 * 
 */
public class Subband {
	public double[] waveform;
	public double samplingRate;
	public double lowestFreqInHz;
	public double highestFreqInHz;

	public Subband(double[] waveformIn, double samplingRateIn) {
		this(waveformIn, samplingRateIn, 0.0);
	}

	public Subband(double[] waveformIn, double samplingRateIn, double lowestFreqInHzIn) {
		this(waveformIn, samplingRateIn, lowestFreqInHzIn, 0.5 * samplingRateIn);
	}

	public Subband(double[] waveformIn, double samplingRateIn, double lowestFreqInHzIn, double highestFreqInHzIn) {
		waveform = null;
		if (waveformIn != null) {
			waveform = new double[waveformIn.length];
			System.arraycopy(waveformIn, 0, waveform, 0, waveformIn.length);
		}

		samplingRate = samplingRateIn;
		lowestFreqInHz = lowestFreqInHzIn;
		highestFreqInHz = highestFreqInHzIn;
	}
}
