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

import marytts.modules.FreeTTS2FestivalUtt;

public class FestivalUttSectioner extends VoiceSectioner {
	public FestivalUttSectioner(String s, Voice defaultVoice) {
		super(s, defaultVoice);
	}

	public VoiceSection nextSection() {
		if (pos >= s.length())
			return null;
		if (s.startsWith(FreeTTS2FestivalUtt.UTTMARKER, pos)) {
			pos += FreeTTS2FestivalUtt.UTTMARKER.length();
		}
		int n = s.indexOf(FreeTTS2FestivalUtt.UTTMARKER, pos);
		if (n == -1)
			n = s.length();
		String section = s.substring(pos, n).trim();
		int endline = section.indexOf(System.getProperty("line.separator"));
		if (endline > -1 && section.startsWith(FreeTTS2FestivalUtt.VOICEMARKER)) {
			String voiceName = section.substring(FreeTTS2FestivalUtt.VOICEMARKER.length(), endline);
			Voice newVoice = Voice.getVoice(voiceName);
			if (newVoice == null) {
				logger.warn("Could not find voice named " + voiceName + ". Using " + currentVoice.getName() + "instead.");
			} else {
				currentVoice = newVoice;
			}
		}
		pos = n;
		logger.debug("Next voice section: voice = " + currentVoice.getName() + "\n" + section);
		return new VoiceSection(currentVoice, section);
	}
}
