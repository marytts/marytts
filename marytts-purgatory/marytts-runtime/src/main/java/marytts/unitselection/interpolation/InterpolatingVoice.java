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
package marytts.unitselection.interpolation;

import java.util.Locale;

import javax.sound.sampled.AudioFormat;

import marytts.exceptions.MaryConfigurationException;
import marytts.modules.phonemiser.Allophone;
import marytts.modules.phonemiser.AllophoneSet;
import marytts.modules.synthesis.Voice;

/**
 * @author marc
 * 
 */
public class InterpolatingVoice extends Voice {

	public static boolean isInterpolatingVoiceName(String name) {
		if (name == null)
			return false;
		String[] parts = name.split("\\s+");
		if (parts.length != 4)
			return false;
		if (!parts[1].equals("with"))
			return false;
		if (!parts[2].endsWith("%"))
			return false;
		int percent;
		try {
			percent = Integer.parseInt(parts[2].substring(0, parts[2].length() - 1));
		} catch (NumberFormatException nfe) {
			return false;
		}
		if (Voice.getVoice(parts[0]) == null)
			return false;
		if (Voice.getVoice(parts[3]) == null)
			return false;
		return true;
	}

	protected Voice firstVoice = null;

	public InterpolatingVoice(InterpolatingSynthesizer is, String name) throws MaryConfigurationException {
		super(name, null, null, is, null);
		if (isInterpolatingVoiceName(name)) {
			String[] parts = name.split("\\s+");
			firstVoice = Voice.getVoice(parts[0]);
		}
	}

	/**
	 * Determine whether this voice has the given name. For the InterpolatingVoice, the meaning of the name is different from a
	 * "normal" voice. It is a specification of how to interpolate two voices. The syntax is: &lt;br&frasl;&gt;
	 * <code>voice1 with XY% voice2</code>&lt;br&frasl;&gt; &lt;br&frasl;&gt; where voice1 and voice2 must be existing voices, and
	 * XY is an integer between 0 and 100.
	 * 
	 * @return true if name matches the specification, false otherwise
	 */
	/*
	 * public boolean hasName(String name) { if (name == null) return false; String[] parts = name.split("\\s+"); if (parts.length
	 * != 4) return false; if (!parts[1].equals("with")) return false; if (!parts[2].endsWith("%")) return false; int percent; try
	 * { percent = Integer.parseInt(parts[2].substring(0, parts[2].length()-1)); } catch (NumberFormatException nfe) { return
	 * false; } if (Voice.getVoice(parts[0]) == null) return false; if (Voice.getVoice(parts[3]) == null) return false; return
	 * true; }
	 */

	// Forward most of the public methods which are meaningful in a unit selection context to firstVoice:

	public AllophoneSet getAllophoneSet() {
		if (firstVoice == null)
			return null;
		return firstVoice.getAllophoneSet();
	}

	public Allophone getAllophone(String phoneSymbol) {
		if (firstVoice == null)
			return null;
		return firstVoice.getAllophone(phoneSymbol);
	}

	public Locale getLocale() {
		if (firstVoice == null)
			return null;
		return firstVoice.getLocale();
	}

	public AudioFormat dbAudioFormat() {
		if (firstVoice == null)
			return null;
		return firstVoice.dbAudioFormat();
	}

	public Gender gender() {
		if (firstVoice == null)
			return null;
		return firstVoice.gender();
	}

}
