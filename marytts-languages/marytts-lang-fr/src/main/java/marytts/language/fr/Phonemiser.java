/**
 * Copyright 2011 DFKI GmbH.
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
package marytts.language.fr;

import java.io.IOException;

import marytts.exceptions.MaryConfigurationException;
import marytts.util.MaryUtils;

public class Phonemiser extends marytts.modules.JPhonemiser {

	public Phonemiser() throws IOException, MaryConfigurationException {
		super("fr.");
	}

	@Override
	public String phonemise(String text, String pos, StringBuilder g2pMethod) {
		// First, try a simple userdict and lexicon lookup:

		text = text.replaceAll("[0-9]+", "");
		String result = userdictLookup(text, pos);
		if (result != null) {
			g2pMethod.append("userdict");
			return result;
		}

		result = lexiconLookup(text, pos);
		if (result != null) {
			g2pMethod.append("lexicon");
			return result;
		}

		// Lookup attempts failed. Try normalising exotic letters
		// (diacritics on vowels, etc.), look up again:
		String normalised = MaryUtils.normaliseUnicodeLetters(text, getLocale());
		if (!normalised.equals(text)) {
			result = userdictLookup(normalised, pos);
			if (result != null) {
				g2pMethod.append("userdict");
				return result;
			}
			result = lexiconLookup(normalised, pos);
			if (result != null) {
				g2pMethod.append("lexicon");
				return result;
			}
		}

		// Cannot find it in the lexicon -- apply letter-to-sound rules
		// to the normalised form

		String phones = lts.predictPronunciation(text);
		try {
			result = lts.syllabify(phones);
		} catch (IllegalArgumentException e) {
			logger.error(String.format("Problem with token <%s> [%s]: %s", text, phones, e.getMessage()));
		}
		if (result != null) {
			g2pMethod.append("rules");
			return result;
		}

		return null;
	}

}
