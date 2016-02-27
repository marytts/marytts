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
package marytts.signalproc.analysis.distance;

import marytts.util.math.MathUtils;

/**
 * @author Oytun T&uuml;rk
 */
public class StatisticsItem {
	public double mean;
	public double std;

	public StatisticsItem(double meanIn, double stdIn) {
		mean = meanIn;
		std = stdIn;
	}

	public StatisticsItem(double[] x) {
		mean = MathUtils.mean(x);
		std = MathUtils.standardDeviation(x, mean);
	}
}
