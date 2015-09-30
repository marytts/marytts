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
package marytts.modules.phonemiser;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.Element;

public class Allophone {
	private String name;
	private final Map<String, String> features;

	/**
	 * Create a new Allophone object from the given XML Element
	 * 
	 * @param a
	 *            the allophone definition element
	 * @param featureNames
	 *            the names of features to use for defining the allophone
	 * @throws IllegalArgumentException
	 *             if a is not as expected
	 */
	public Allophone(Element a, String[] featureNames) {
		name = a.getAttribute("ph");
		String vc;
		String isTone;
		if (name.equals(""))
			throw new IllegalArgumentException("Element must have a 'ph' attribute");
		if (a.getTagName().equals("consonant")) {
			vc = "-";
			isTone = "-";
		} else if (a.getTagName().equals("vowel")) {
			vc = "+";
			isTone = "-";
		} else if (a.getTagName().equals("silence")) {
			vc = "0";
			isTone = "-";
		} else if (a.getTagName().equals("tone")) {
			vc = "0";
			isTone = "+";
		} else {
			throw new IllegalArgumentException("Element must be one of <vowel>, <consonant> and <silence>, but is <"
					+ a.getTagName() + ">");
		}
		Map<String, String> feats = new HashMap<String, String>();
		feats.put("vc", vc);
		feats.put("isTone", isTone);
		for (String f : featureNames) {
			feats.put(f, getAttribute(a, f));
		}
		this.features = Collections.unmodifiableMap(feats);
	}

	/**
	 * Return the requested attribute of e, or "0" if there is no such attribute.
	 * 
	 * @param e
	 * @param att
	 * @return "0" if val.equals(""), val otherwise
	 */
	private String getAttribute(Element e, String att) {
		String val = e.getAttribute(att);
		if (val.equals(""))
			return "0";
		return val;
	}

	public String name() {
		return name;
	}

	public String toString() {
		return name;
	}

	public boolean isVowel() {
		return "+".equals(features.get("vc"));
	}

	public boolean isDiphthong() {
		assert isVowel();
		return "d".equals(features.get("vlng"));
	}

	public boolean isSyllabic() {
		return isVowel();
	}

	public boolean isConsonant() {
		return "-".equals(features.get("vc"));
	}

	public boolean isVoiced() {
		return isVowel() || "+".equals(features.get("cvox"));
	}

	public boolean isSonorant() {
		return "lnr".contains(features.get("ctype"));
	}

	public boolean isLiquid() {
		return "l".equals(features.get("ctype"));
	}

	public boolean isNasal() {
		return "n".equals(features.get("ctype"));
	}

	public boolean isGlide() {
		return "r".equals(features.get("ctype")) && !isVowel();
	}

	public boolean isFricative() {
		return "f".equals(features.get("ctype"));
	}

	public boolean isPlosive() {
		return "s".equals(features.get("ctype"));
	}

	public boolean isAffricate() {
		return "a".equals(features.get("ctype"));
	}

	public boolean isPause() {
		return "0".equals(features.get("vc")) && "-".equals(features.get("isTone"));
	}

	/**
	 * Whether the Allophone object represents a tone symbol.
	 * 
	 * @return "+".equals(features.get("isTone"))
	 */
	public boolean isTone() {
		return "+".equals(features.get("isTone"));
	}

	public int sonority() {
		if (isVowel()) {
			String vlng = features.get("vlng");
			if (vlng == null)
				return 5; // language doesn't make a distinction between vowels of different length
			if ("ld".contains(vlng))
				return 6;
			else if ("s".equals(vlng))
				return 5;
			else if ("a".equals(vlng))
				return 4;
			else
				return 5; // unknown vowel length
		} else if (isSonorant())
			return 3;
		else if (isFricative())
			return 2;
		return 1;
	}

	/**
	 * Get the key-value map of features and feature values for this allophone.
	 * 
	 * @return an unmodifiable map.
	 */
	public Map<String, String> getFeatures() {
		return features;
	}

	/**
	 * Return the feature with name feat. Three types of values are possible: 1. an informative feature; 2. the value "0" to
	 * indicate that the feature exists but is not meaningful for this allophone; 3. null to indicate that the feature does not
	 * exist.
	 * 
	 * @param feat
	 *            feat
	 * @return the feature value, or null
	 */
	public String getFeature(String feat) {
		return features.get(feat);
	}
}
