/**
 * Copyright 2010 DFKI GmbH.
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

package marytts.modules.acoustic;

/**
 * List of known model types as constants; can be extended but needs to mesh with Classes extending {@link Model} and switch
 * statement in
 * {@linkplain marytts.modules.synthesis.Voice#Voice(String, java.util.Locale, javax.sound.sampled.AudioFormat, marytts.modules.synthesis.WaveformSynthesizer, marytts.modules.synthesis.Voice.Gender)
 * <code>Voice()</code>}:
 * 
 * @author steiner
 * 
 */
public enum ModelType {
	// enumerate model types here:
	CART, SOP, HMM;

	// get the appropriate model type from a string (which can be lower or mixed case):
	// adapted from http://www.xefer.com/2006/12/switchonstring
	public static ModelType fromString(String string) {
		try {
			ModelType modelString = valueOf(string.toUpperCase());
			return modelString;
		} catch (Exception e) {
			return null;
		}
	}
}
