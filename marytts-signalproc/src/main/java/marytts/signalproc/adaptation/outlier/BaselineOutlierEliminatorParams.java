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
package marytts.signalproc.adaptation.outlier;

import marytts.signalproc.adaptation.BaselineParams;

/**
 * 
 * Baseline class for outlier elimination parameters
 * 
 * @author Oytun T&uuml;rk
 */
public class BaselineOutlierEliminatorParams extends BaselineParams {
	public boolean isActive; // Is outlier elimination process being used? If false, the below parameters have no effect.

	public boolean isCheckLsfOutliers; // Use LSF distance distributions for finding outliers?
	public boolean isCheckF0Outliers; // Use F0 difference distributions for finding outliers?
	public boolean isCheckDurationOutliers; // Use duration difference distributions for finding outliers?
	public boolean isCheckEnergyOutliers; // Use energy difference distributions for finding outliers?

	public BaselineOutlierEliminatorParams() {
		isCheckLsfOutliers = true;
		isCheckF0Outliers = true;
		isCheckDurationOutliers = true;
		isCheckEnergyOutliers = true;

		isActive = true;
	}

	public BaselineOutlierEliminatorParams(BaselineOutlierEliminatorParams existing) {
		isCheckLsfOutliers = existing.isCheckLsfOutliers;
		isCheckF0Outliers = existing.isCheckF0Outliers;
		isCheckDurationOutliers = existing.isCheckDurationOutliers;
		isCheckEnergyOutliers = existing.isCheckEnergyOutliers;

		isActive = existing.isActive;
	}
}
