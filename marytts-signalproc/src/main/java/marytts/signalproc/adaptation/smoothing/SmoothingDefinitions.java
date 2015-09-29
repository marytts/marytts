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

/**
 * @author Oytun T&uuml;rk
 * 
 */
public class SmoothingDefinitions {
	// Smoothing status
	public static final int NONE = 0;
	public static final int ESTIMATING_SMOOTHED_VOCAL_TRACT = 1; // No output generation except a smoothed vocal tract spectrum
	public static final int TRANSFORMING_TO_SMOOTHED_VOCAL_TRACT = 2; // No mapping/transformation but use pre-computed values
	//

	// Smoothing methods: These are active when ESTIMATING_SMOOTHED_VOCAL_TRACT
	public static final int NO_SMOOTHING = 0; // Do nothing about smoothing
	public static final int TRANSFORMATION_FILTER_SMOOTHING = 1; // Store target/source filters to smooth them
	public static final int OUTPUT_LSFCONTOUR_SMOOTHING = 2; // Store target(output) lsf contours to smooth them
	public static final int OUTPUT_VOCALTRACTSPECTRUM_SMOOTHING = 3; // Store target(output) vocal tract spectra to smooth them
	//

	public static final int DEFAULT_NUM_NEIGHBOURS = 5;
}
