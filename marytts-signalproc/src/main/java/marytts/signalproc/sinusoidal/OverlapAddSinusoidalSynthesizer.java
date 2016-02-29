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
package marytts.signalproc.sinusoidal;

/**
 * A basic implementation of overlap-add sinusoidal synthesis. Phase interpolation seems problematic, so we are not yet able to
 * use this class in applications.
 * 
 * @author Oytun T&uuml;rk
 * 
 */
public class OverlapAddSinusoidalSynthesizer extends BaseSinusoidalSynthesizer {

	public OverlapAddSinusoidalSynthesizer(int samplingRate) {
		super(samplingRate);
		// TODO Auto-generated constructor stub
	}

	public double[] synthesize(SinusoidalTracks[] sts) {
		return synthesize(sts, false);
	}

	public double[] synthesize(SinusoidalTracks[] sts, boolean isSilentSynthesis) {
		double[] y = null;

		NonharmonicSinusoidalSpeechFrame[] frameSins = SinusoidalUtils.tracks2frameSins(sts);

		return y;
	}
}
