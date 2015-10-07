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
package marytts.signalproc.adaptation;

/**
 * Flags for outliers in source and target training data
 * 
 * @author Oytun T&uuml;rk
 */
public class OutlierStatus {
	public static final int NON_OUTLIER = Integer.parseInt("0000000000", 2);
	public static final int LSF_OUTLIER = Integer.parseInt("0000000001", 2);
	public static final int F0_OUTLIER = Integer.parseInt("0000000010", 2);
	public static final int DURATION_OUTLIER = Integer.parseInt("0000000100", 2);
	public static final int ENERGY_OUTLIER = Integer.parseInt("0000001000", 2);
	public static final int GENERAL_OUTLIER = Integer.parseInt("0000010000", 2);
	public static final int ONE2MANY_OUTLIER = Integer.parseInt("0000100000", 2);
	public static final int MANY2ONE_OUTLIER = Integer.parseInt("0001000000", 2);
	public static final int MANY2MANY_OUTLIER = Integer.parseInt("0010000000", 2);

	public int totalNonOutliers;
	public int totalLsfOutliers;
	public int totalF0Outliers;
	public int totalDurationOutliers;
	public int totalEnergyOutliers;
	public int totalGeneralOutliers;

	public OutlierStatus() {
		init();
	}

	public void init() {
		totalNonOutliers = 0;
		totalLsfOutliers = 0;
		totalF0Outliers = 0;
		totalDurationOutliers = 0;
		totalEnergyOutliers = 0;
		totalGeneralOutliers = 0;
	}
}
