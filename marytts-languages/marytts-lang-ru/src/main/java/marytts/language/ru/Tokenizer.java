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
package marytts.language.ru;

import java.util.Locale;

import marytts.datatypes.MaryData;
import marytts.datatypes.MaryDataType;
import marytts.datatypes.MaryXML;
import marytts.util.dom.MaryDomUtils;
import marytts.util.dom.NameNodeFilter;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.traversal.DocumentTraversal;
import org.w3c.dom.traversal.NodeFilter;
import org.w3c.dom.traversal.NodeIterator;

/**
 * 
 * @author Marc Schr&ouml;der
 */
public class Tokenizer extends marytts.modules.JTokeniser {

	/**
     * 
     */
	public Tokenizer() {
		super(MaryDataType.RAWMARYXML, MaryDataType.TOKENS, new Locale("ru"));
		setTokenizerLanguage("en");
	}

	public MaryData process(MaryData d) throws Exception {
		MaryData result = super.process(d);
		splitOffDots(result);
		return result;
	}

	/**
	 * For Russian, treat all dots as standalone tokens that trigger end of sentence.
	 * 
	 * @param d
	 *            d
	 */
	protected void splitOffDots(MaryData d) {
		Document doc = d.getDocument();
		NodeIterator ni = ((DocumentTraversal) doc).createNodeIterator(doc, NodeFilter.SHOW_ELEMENT, new NameNodeFilter(
				MaryXML.TOKEN), false);
		Element t = null;
		while ((t = (Element) ni.nextNode()) != null) {
			String s = MaryDomUtils.tokenText(t);
			// Any dots to be split off?
			if (s.length() > 1 && s.endsWith(".")) {
				String s1 = s.substring(0, s.length() - 1);

				MaryDomUtils.setTokenText(t, s1);
				Element sentence = (Element) MaryDomUtils.getAncestor(t, MaryXML.SENTENCE);
				assert sentence != null;
				if (!MaryDomUtils.isLastOfItsKindIn(t, sentence)) {
					// need to manually add a sentence break
					Element firstInSentence = MaryDomUtils.getFirstElementByTagName(sentence, MaryXML.TOKEN);
					Element newSentence = MaryDomUtils.encloseNodesWithNewElement(firstInSentence, t, MaryXML.SENTENCE);
					sentence.getParentNode().insertBefore(newSentence, sentence);
					sentence = newSentence;
				}
				// And actually, we still need to add a token '.'
				Element newT = MaryXML.appendChildElement(sentence, MaryXML.TOKEN);
				MaryDomUtils.setTokenText(newT, ".");
			}
		}
	}
}
