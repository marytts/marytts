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

/**
 * 
 * Class for keeping total standard deviations to be used in automatic thresholding in outlier elimation
 * 
 * @author Oytun T&uuml;rk
 */
public class TotalStandardDeviations {
	public double general; // Common total standard deviations to use unless the user does not want to specify the below
							// parameters
	public double lsf; // Standard deviation for LSFs
	public double f0; // Standard deviation for f0
	public double duration; // Standard deviation for duration
	public double energy; // Standard deviation for energy

	public static final double DEFAULT_TOTAL_STANDARD_DEVIATIONS = 1.5; // Default value of standard deviations to use

	public TotalStandardDeviations() {
		general = DEFAULT_TOTAL_STANDARD_DEVIATIONS;
		lsf = DEFAULT_TOTAL_STANDARD_DEVIATIONS;
		f0 = DEFAULT_TOTAL_STANDARD_DEVIATIONS;
		duration = DEFAULT_TOTAL_STANDARD_DEVIATIONS;
		energy = DEFAULT_TOTAL_STANDARD_DEVIATIONS;
	}

	public TotalStandardDeviations(TotalStandardDeviations existing) {
		general = existing.general;
		lsf = existing.lsf;
		f0 = existing.f0;
		duration = existing.duration;
		energy = existing.energy;
	}
}
