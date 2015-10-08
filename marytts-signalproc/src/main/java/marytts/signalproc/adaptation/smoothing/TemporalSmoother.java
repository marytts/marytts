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
package marytts.signalproc.adaptation.smoothing;

import marytts.signalproc.window.DynamicWindow;
import marytts.signalproc.window.Window;

/**
 * 
 * Temporal smoother can be used to smooth acoustic feature vectors along a desired axis. This class is used in voice conversion
 * to smooth the frequency bins of the vocal tract transformation filter as described in:
 * 
 * T&uuml;rk, O., B&uuml;y&uuml;k, O., Haznedaroglu, A., and Arslan, L. M., “Application of Voice Conversion for Cross-Language
 * Rap Singing Transformation”, “Application of voice conversion for crosslanguage rap singing transformation,” in Proc. of the
 * IEEE ICASSP 2009, Taipei, Taiwan, April 2009.
 * 
 * @author Oytun T&uuml;rk
 */
public class TemporalSmoother {
	public static final int DEFAULT_NUM_NEIGHBOURS = 4; // Default neighbours to use on the left and on the right (separately)
	public static final int DEFAULT_SMOOTHING_WINDOW = Window.HAMMING;

	public static double[] smooth(double[] x, int neighbours) {
		return smooth(x, neighbours, DEFAULT_SMOOTHING_WINDOW);
	}

	public static double[] smooth(double[] x, int neighbours, int windowType) {
		int i;
		double[][] xx = new double[x.length][1];
		for (i = 0; i < x.length; i++)
			xx[i][0] = x[i];
		xx = smooth(xx, neighbours, windowType);

		double[] y = new double[x.length];
		for (i = 0; i < x.length; i++)
			y[i] = xx[i][0];

		return y;
	}

	public static double[][] smooth(double[][] x, int neighbours) {
		return smooth(x, neighbours, DEFAULT_SMOOTHING_WINDOW);
	}

	// Smooth along each column
	// i.e. each row corresponds to acoustic features of one frame at a specific instant of time
	// Windowing based weighting is used
	public static double[][] smooth(double[][] x, int neighbours, int windowType) {
		if (neighbours <= 0)
			return x;
		else {
			double[][] y = new double[x.length][x[0].length];
			int i, j, k;
			int windowSize = 2 * neighbours + 1;
			DynamicWindow w = new DynamicWindow(windowType);
			double[] weights = w.values(windowSize);
			double weightSum;

			for (i = 1; i < x.length; i++)
				assert x[i].length == x[0].length;

			for (i = 0; i < x[0].length; i++) {
				for (j = 0; j < x.length; j++) {
					y[j][i] = 0.0;
					weightSum = 0.0;
					for (k = -neighbours; k <= neighbours; k++) {
						if (j + k >= 0 && j + k < x.length) {
							y[j][i] += weights[k + neighbours] * x[j + k][i];
							weightSum += weights[k + neighbours];
						}
					}

					if (weightSum > 0.0)
						y[j][i] /= weightSum;
				}
			}

			return y;
		}
	}

	public static void main(String[] args) {
		double[][] xx = new double[10][2];
		xx[0][0] = 100.0;
		xx[0][1] = 220.0;
		xx[1][0] = 110.0;
		xx[1][1] = 210.0;
		xx[2][0] = 150.0;
		xx[2][1] = 230.0;
		xx[3][0] = 90.0;
		xx[3][1] = 220.0;
		xx[4][0] = 80.0;
		xx[4][1] = 250.0;
		xx[5][0] = 120.0;
		xx[5][1] = 260.0;
		xx[6][0] = 140.0;
		xx[6][1] = 290.0;
		xx[7][0] = 180.0;
		xx[7][1] = 300.0;
		xx[8][0] = 150.0;
		xx[8][1] = 340.0;
		xx[9][0] = 120.0;
		xx[9][1] = 320.0;

		double[][] y = TemporalSmoother.smooth(xx, 3);

		System.out.println("Finished");
	}
}
