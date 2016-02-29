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
public class FilterBankAnalyserBase implements FilterBankAnalyser {
	public static final int FIR_BANDPASS_FILTERBANK = 1;
	public static final int FIR_WAVELET_FILTERBANK = 2;
	public static final int COMPLEMENTARY_FILTERBANK = 3;

	// The baseline version does nothing
	// Implement functionality in derived classes
	public Subband[] apply(double[] x) {
		return null;
	}
}
