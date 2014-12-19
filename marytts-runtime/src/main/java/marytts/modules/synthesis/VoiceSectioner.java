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

import marytts.util.MaryUtils;

import org.apache.log4j.Logger;

/**
 * A helper class for the synthesis module, splitting synthesis input data into sections to be spoken by the same voice.
 * 
 * @author Marc Schr&ouml;der
 */

public abstract class VoiceSectioner {
	protected String s;
	protected int pos;
	protected Voice currentVoice;
	protected Logger logger = null;

	public VoiceSectioner(String s, Voice defaultVoice) {
		this.s = s;
		this.pos = 0;
		this.currentVoice = defaultVoice;
		this.logger = MaryUtils.getLogger("VoiceSectioner");
	}

	public abstract VoiceSection nextSection();
}
