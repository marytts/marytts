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
 * This is a collection of (NonharmonicSinusoidalSpeechFrame)s
 * 
 * @author Oytun T&uuml;rk
 */
public class NonharmonicSinusoidalSpeechSignal {
	public NonharmonicSinusoidalSpeechFrame[] framesSins;
	public float originalDurationInSeconds;

	public NonharmonicSinusoidalSpeechSignal(int totalFrm) {
		if (totalFrm > 0)
			framesSins = new NonharmonicSinusoidalSpeechFrame[totalFrm];
		else
			framesSins = null;
	}
}
