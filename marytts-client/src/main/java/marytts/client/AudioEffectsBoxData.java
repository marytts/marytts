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
package marytts.client;

import marytts.util.string.StringUtils;

/**
 * Data for a set of audio effects, i.e. "an audio effects box".
 * 
 * @author Oytun T&uuml;rk
 * 
 */
public class AudioEffectsBoxData {

	private AudioEffectControlData[] effectControlsData;

	// availableEffects is one large string produced by the server in the following format:
	// <EffectSeparator>charEffectSeparator</EffectSeparator>
	// <Effect>
	// <Name>effect´s name</Name>
	// <SampleParam>example parameters string</SampleParam>
	// <HelpText>help text string</HelpText>
	// </Effect>
	// <Effect>
	// <Name>effect´s name</effectName>
	// <SampleParam>example parameters string</SampleParam>
	// <HelpText>help text string</HelpText>
	// </Effect>
	// ...
	// <Effect>
	// <Name>effect´s name</effectName>
	// <SampleParam>example parameters string</SampleParam>
	// <HelpText>help text string</HelpText>
	// </Effect>
	public AudioEffectsBoxData(String availableEffects) {
		effectControlsData = null;

		if (availableEffects != null && availableEffects.length() > 0)
			parseAvailableEffects(availableEffects);
	}

	public AudioEffectControlData getControlData(int index) {
		if (effectControlsData != null && index >= 0 && index < effectControlsData.length)
			return effectControlsData[index];
		else
			return null;
	}

	public boolean hasEffects() {
		return effectControlsData != null;
	}

	// Parse the XML-like full effect set string from the server
	protected int parseAvailableEffects(String availableEffects) {
		String[] effectLines = StringUtils.toStringArray(availableEffects);
		effectControlsData = new AudioEffectControlData[effectLines.length];
		for (int i = 0; i < effectLines.length; i++) {
			String strEffectName, strParams;
			int iSpace = effectLines[i].indexOf(' ');
			if (iSpace != -1) {
				strEffectName = effectLines[i].substring(0, iSpace);
				strParams = effectLines[i].substring(iSpace + 1);
			} else { // no params
				strEffectName = effectLines[i];
				strParams = "";
			}
			effectControlsData[i] = new AudioEffectControlData(strEffectName, strParams, null);
		}
		return getTotalEffects();
	}

	public int getTotalEffects() {
		if (effectControlsData != null)
			return effectControlsData.length;
		else
			return 0;
	}
}
