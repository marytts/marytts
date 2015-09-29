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
package marytts.signalproc.process;

import marytts.util.signal.SignalProcUtils;

/**
 * @author Oytun T&uuml;rk
 * 
 */
public class VoiceModificationParameters {

	public int fs; // Sampling rate in Hz
	public int lpOrder; // LP order

	protected double[] pscales;
	protected double[] tscales;
	protected double[] escales;
	protected double[] vscales;

	/**
     * 
     */
	public VoiceModificationParameters() {
		this(16000, 18, null, null, null, null);
	}

	public VoiceModificationParameters(int samplingRate, int LPOrder, double[] pscalesIn, double[] tscalesIn, double[] escalesIn,
			double[] vscalesIn) {
		initialise(samplingRate, LPOrder, pscalesIn, tscalesIn, escalesIn, vscalesIn);
	}

	public VoiceModificationParameters(int samplingRate, int LPOrder, double pscaleIn, double tscaleIn, double escaleIn,
			double vscaleIn) {
		double[] pscalesIn = new double[1];
		double[] tscalesIn = new double[1];
		double[] escalesIn = new double[1];
		double[] vscalesIn = new double[1];
		pscalesIn[0] = pscaleIn;
		tscalesIn[0] = tscaleIn;
		escalesIn[0] = escaleIn;
		vscalesIn[0] = vscaleIn;

		initialise(samplingRate, LPOrder, pscalesIn, tscalesIn, escalesIn, vscalesIn);
	}

	private void initialise(int samplingRate, int LPOrder, double[] pscalesIn, double[] tscalesIn, double[] escalesIn,
			double[] vscalesIn) {
		if (pscalesIn != null) {
			pscales = new double[pscalesIn.length];
			System.arraycopy(pscalesIn, 0, pscales, 0, pscalesIn.length);
		}

		if (tscalesIn != null) {
			tscales = new double[tscalesIn.length];
			System.arraycopy(tscalesIn, 0, tscales, 0, tscalesIn.length);
		}

		if (escalesIn != null) {
			escales = new double[escalesIn.length];
			System.arraycopy(escalesIn, 0, escales, 0, escalesIn.length);
		}

		if (vscalesIn != null) {
			vscales = new double[vscalesIn.length];
			System.arraycopy(vscalesIn, 0, vscales, 0, vscalesIn.length);
		}

		fs = samplingRate;
		lpOrder = SignalProcUtils.getLPOrder(fs);
	}
}
