/**
 * Copyright 2008 DFKI GmbH.
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
package marytts.language.en;

import java.util.Locale;

import marytts.datatypes.MaryXML;
import marytts.modules.phonemiser.Allophone;
import marytts.modules.phonemiser.AllophoneSet;
import marytts.util.dom.MaryDomUtils;

import org.w3c.dom.Element;
import org.w3c.dom.traversal.TreeWalker;

/**
 * A pronunciation model that takes into account some English postlexical rules.
 * 
 * @author marc
 *
 */
public class PronunciationModel extends marytts.modules.PronunciationModel {
	/**
     * 
     */
	public PronunciationModel() {
		super(Locale.ENGLISH);
	}

	/**
	 * Optionally, a language-specific subclass can implement any postlexical rules on the document.
	 * 
	 * @param token
	 *            a &lt;t&gt; element with a &lt;syllable&gt; and &lt;ph&gt; substructure.
	 * @param allophoneSet
	 *            allophoneSet
	 * @return true if something was changed in the content of the &lt;ph&gt; elements for this &lt;t&gt;, false otherwise
	 */
	@Override
	protected boolean postlexicalRules(Element token, AllophoneSet allophoneSet) {
		String word = MaryDomUtils.tokenText(token);

		if (word.equals("'s") || word.equals("'ve") || word.equals("'ll") || word.equals("'d")) {
			Element sentence = (Element) MaryDomUtils.getAncestor(token, MaryXML.SENTENCE);
			if (sentence == null)
				return false;
			TreeWalker tw = MaryDomUtils.createTreeWalker(sentence, MaryXML.PHONE);
			tw.setCurrentNode(token);
			Element currentSegment = (Element) tw.nextNode();
			if (currentSegment == null)
				return false;
			Element prevSegment = (Element) tw.previousNode();
			if (prevSegment == null)
				return false;
			String pname = prevSegment.getAttribute("p");
			Allophone prev = allophoneSet.getAllophone(pname);

			if (word.equals("'s")) {
				if ("fa".contains(prev.getFeature("ctype")) && "ap".contains(prev.getFeature("cplace"))) {
					// an alveolar or palatal fricative or affricate: s,z,S,Z,tS,dZ
					prependSchwa(currentSegment);
					return true;
				} else if (prev.getFeature("cvox").equals("-")) {
					// any unvoiced consonant
					currentSegment.setAttribute("p", "s"); // devoice
					return true;
				}
			} else { // one of 've, 'll or 'd
				if (prev.isConsonant()) {
					prependSchwa(currentSegment);
					return true;
				}
			}
		}
		return false;

	}

	private void prependSchwa(Element currentSegment) {
		Element syllable = (Element) currentSegment.getParentNode();
		assert syllable != null;
		Element schwa = MaryXML.createElement(syllable.getOwnerDocument(), MaryXML.PHONE);
		schwa.setAttribute("p", "@");
		syllable.insertBefore(schwa, currentSegment);
	}
}
