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

import marytts.util.io.FileUtils;

/**
 * @author Oytun T&uuml;rk
 */
public class ComparativeStatisticsItem {
	public StatisticsItem referenceVsMethod1;
	public StatisticsItem referenceVsMethod2;

	public ComparativeStatisticsItem(double[] x1, double[] x2) {
		referenceVsMethod1 = new StatisticsItem(x1);
		referenceVsMethod2 = new StatisticsItem(x2);
	}

	public void writeToTextFile(String textFile) {
		double[] tmpOut = new double[5];
		tmpOut[0] = referenceVsMethod1.mean;
		tmpOut[1] = referenceVsMethod1.std;
		tmpOut[2] = referenceVsMethod2.mean;
		tmpOut[3] = referenceVsMethod2.std;
		tmpOut[4] = referenceVsMethod1.mean - referenceVsMethod2.mean;

		FileUtils.writeToTextFile(tmpOut, textFile);
	}
}
