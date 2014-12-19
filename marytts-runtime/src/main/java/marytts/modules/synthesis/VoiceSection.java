/**
 * Copyright 2000-2006 DFKI GmbH.
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
package marytts.modules.synthesis;

/**
 * A helper class for the synthesis module, representing a section of synthesis input data to be spoken by the same voice.
 * 
 * @author Marc Schr&ouml;der
 */

public class VoiceSection {
	private Voice voice;
	private String text;

	public Voice voice() {
		return voice;
	}

	public String text() {
		return text;
	}

	public VoiceSection(Voice voice, String text) {
		this.voice = voice;
		this.text = text;
	}
}
