/**
 * Copyright 2003 DFKI GmbH.
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
package marytts.language.de;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

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
public class JTokeniser extends marytts.modules.JTokeniser {

	protected static final Set<String> nonAbbrevs = new HashSet<String>(Arrays.asList(new String[] {
			// Measure symbols
			"m", "km", "dm", "cm", "mm", "g", "kg", "mg", "s", "sec", "ms", "min",
			// "A",
			// "V",
			// "K",
			"°C", "°F", "Hz", "kHz", "MHz", "GHz",
			// "N",
			"Pa", "J", "kJ",
			// "W",
			"kW", "MW", "GW", "mW", "l", "dl", "cl", "ml", "Bq", "EL", "TL", "kcal", "oz", "qm", "m²", "m³", "ccm", "%" }));

	/**
     * 
     */
	public JTokeniser() {
		super(MaryDataType.RAWMARYXML, MaryDataType.TOKENS, Locale.GERMAN);
	}

	public MaryData process(MaryData d) throws Exception {
		MaryData result = super.process(d);
		tokenizerFixes(result);
		return result;
	}

	protected void tokenizerFixes(MaryData d) {
		Document doc = d.getDocument();
		NodeIterator ni = ((DocumentTraversal) doc).createNodeIterator(doc, NodeFilter.SHOW_ELEMENT, new NameNodeFilter(
				MaryXML.TOKEN), false);
		Element t = null;
		while ((t = (Element) ni.nextNode()) != null) {
			String s = MaryDomUtils.tokenText(t);
			// Any dots taken to be part of an abbreviation that should not be one?
			if (s.endsWith(".")) {
				String s1 = s.substring(0, s.length() - 1);
				if (nonAbbrevs.contains(s1)) {
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

}
