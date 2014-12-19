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
 * @author oytun.turk
 * 
 */
public class SinusoidalUtils {

	// Collect each track´s sinusoids in speech frame sinusoids
	// This way, we will have a collection of sinusoids representing each speech frame
	// Then, overlap-add synthesis can be performed to avoid concatenation artifacts and smoothness problems
	// Quatieri mentions that even single sinusoids can be used using this approach, i.e.
	// each track starts and ends within one frame
	// However, the skip rate should be dense enough, i.e. at most 0.01 s.(at least 100 Hz)
	public static NonharmonicSinusoidalSpeechFrame[] tracks2frameSins(SinusoidalTracks[] sts) {
		NonharmonicSinusoidalSpeechFrame[] frameSins = null;

		return frameSins;
	}
}
