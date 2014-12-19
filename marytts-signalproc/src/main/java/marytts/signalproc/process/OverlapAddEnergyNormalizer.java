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

import java.util.Arrays;

import marytts.signalproc.window.Window;
import marytts.util.math.MathUtils;
import marytts.util.signal.SignalProcUtils;

/**
 * @author oytun.turk
 * 
 */
public class OverlapAddEnergyNormalizer {
	public static double[] normalize(double[] x, int samplingRate, double windowSizeInSeconds, double skipSizeInSeconds,
			int windowType, double targetEnergy) {
		double[] y = new double[x.length];
		double[] w = new double[x.length];
		Arrays.fill(y, 0.0);
		Arrays.fill(w, 0.0);

		int ws = (int) Math.floor(windowSizeInSeconds * samplingRate + 0.5);
		if (ws % 2 != 0)
			ws++;
		int half_ws = (int) (ws / 2.0);
		int ss = (int) Math.floor(skipSizeInSeconds * samplingRate + 0.5);

		Window win = Window.get(windowType, ws);
		win.normalize(1.0f); // Normalize to sum up to unity
		double[] wgt = new double[ws];
		double[] frm = new double[ws];

		int start = 0;
		boolean bLastFrame = false;
		double frmEn, gain;
		int i;
		while (true) {
			if (start + ws - 1 >= x.length - 1)
				bLastFrame = true;

			wgt = win.getCoeffs();
			if (start == 0) // First frame
				Arrays.fill(wgt, 0, half_ws - 1, 1.0);
			else if (bLastFrame)
				Arrays.fill(wgt, half_ws, ws - 1, 1.0);

			Arrays.fill(frm, 0.0);
			System.arraycopy(x, start, frm, 0, Math.min(ws, x.length - start));

			frm = MathUtils.multiply(frm, wgt);

			frmEn = SignalProcUtils.energy(frm);
			gain = Math.sqrt(targetEnergy / frmEn);
			System.out.println(String.valueOf(gain));

			frm = MathUtils.multiply(frm, gain);
			frm = MathUtils.multiply(frm, wgt);

			for (i = 0; i < ws; i++) {
				if (i + start >= y.length) {
					bLastFrame = true;
					break;
				}

				y[i + start] += frm[i];
				w[i + start] += wgt[i] * wgt[i];
			}

			if (bLastFrame)
				break;

			start += ss;
		}

		for (i = 0; i < y.length; i++) {
			if (w[i] > 0.0)
				y[i] /= w[i];
		}

		return y;
	}
}
