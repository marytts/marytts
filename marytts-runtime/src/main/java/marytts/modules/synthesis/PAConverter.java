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

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;

import marytts.modules.phonemiser.Allophone;
import marytts.modules.phonemiser.AllophoneSet;
import marytts.util.MaryRuntimeUtils;
import marytts.util.MaryUtils;

import org.apache.log4j.Logger;

/**
 * Phonetic Alphabet converter. Converts individual phonetic symbols between different phonetic alphabets.
 * 
 * @author Marc Schr&ouml;der
 */

public class PAConverter {
	private static Logger logger = MaryUtils.getLogger("PAConverter");

	// The following map has as its keys Locales and as its values PhonemeSets.
	private static Map<Locale, AllophoneSet> sampa;

	private static Map<String, String> sampaEn2sampaDe;

	// Static constructor:
	static {
		sampa = new HashMap<Locale, AllophoneSet>();
		try {
			AllophoneSet usenSampa = MaryRuntimeUtils.needAllophoneSet("en_US.allophoneset");
			sampa.put(Locale.US, usenSampa);
		} catch (Exception e) {
			logger.warn("Cannot load US English allophone set", e);
		}
		try {
			AllophoneSet deSampa = MaryRuntimeUtils.needAllophoneSet("de.allophoneset");
			sampa.put(Locale.GERMAN, deSampa);
		} catch (Exception e) {
			logger.warn("Cannot load German allophone set", e);
		}

		// English Sampa to German Sampa
		sampaEn2sampaDe = new HashMap<String, String>();
		sampaEn2sampaDe.put("p_h", "p");
		sampaEn2sampaDe.put("t_h", "t");
		sampaEn2sampaDe.put("4", "t");
		sampaEn2sampaDe.put("k_h", "k");
		sampaEn2sampaDe.put("r=", "6");
		sampaEn2sampaDe.put("i", "i:");
		sampaEn2sampaDe.put("u", "u:");
		sampaEn2sampaDe.put("A", "a:");
		sampaEn2sampaDe.put("E", "E");
		sampaEn2sampaDe.put("{", "E");
		sampaEn2sampaDe.put("V", "a");
		sampaEn2sampaDe.put("AI", "aI");
		sampaEn2sampaDe.put("OI", "OY");
	}

	/**
	 * Converts a single phonetic symbol in English sampa representation into its equivalent in German sampa representation.
	 * 
	 * @param En
	 *            En
	 * @return original English symbol if no known conversion exists.
	 */
	public static String sampaEn2sampaDe(String En) {
		String result = En;
		if (sampaEn2sampaDe.containsKey(En)) {
			result = (String) sampaEn2sampaDe.get(En);
		}
		return result;
	}

	/**
	 * Converts an english sampa string into a german sampa string, keeping syllable boundaries and stress markers
	 * 
	 * @param sEn
	 *            sEn
	 * @return result.toString
	 */
	public static String sampaEnString2sampaDeString(String sEn) {
		StringBuilder result = new StringBuilder();
		StringTokenizer st = new StringTokenizer(sEn, "-");
		while (st.hasMoreTokens()) {
			boolean stressed = false;
			String syl = st.nextToken();
			if (syl.startsWith("'")) {
				result.append("'");
				stressed = true;
			}
			Allophone[] phon = sampa(Locale.US).splitIntoAllophones(syl);
			for (int i = 0; i < phon.length; i++) {
				String eng = phon[i].name();
				String sDe = sampaEn2sampaDe(eng);
				if (sDe.equals("6") && stressed) {
					sDe = "96";
				}
				result.append(sDe);
			}
			if (st.hasMoreTokens()) {
				result.append("-");
			}

		}
		return result.toString();

	}

	public static AllophoneSet sampa(Locale locale) {
		return sampa.get(locale);
	}

}
