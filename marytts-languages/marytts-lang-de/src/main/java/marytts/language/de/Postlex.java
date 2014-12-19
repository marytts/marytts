/**
 * Copyright 2002 DFKI GmbH.
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

import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import marytts.datatypes.MaryData;
import marytts.datatypes.MaryXML;
import marytts.language.de.postlex.PhonologicalRules;
import marytts.modules.PronunciationModel;
import marytts.util.dom.MaryDomUtils;
import marytts.util.dom.NameNodeFilter;

import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.traversal.DocumentTraversal;
import org.w3c.dom.traversal.NodeFilter;
import org.w3c.dom.traversal.TreeWalker;

/**
 * The postlexical phonological processes module.
 * 
 * @author Marc Schr&ouml;der
 */

public class Postlex extends PronunciationModel {

	public Postlex() {
		super(Locale.GERMAN);
	}

	public MaryData process(MaryData d) throws Exception {
		Document doc = d.getDocument();
		mtuPostlex(doc);
		phonologicalRules(doc);
		return super.process(d);
	}

	private void mtuPostlex(Document doc) throws DOMException {
		TreeWalker tw = ((DocumentTraversal) doc).createTreeWalker(doc, NodeFilter.SHOW_ELEMENT, new NameNodeFilter(MaryXML.MTU),
				false);
		Element m = null;
		while ((m = (Element) tw.nextNode()) != null) {
			if (MaryDomUtils.hasAncestor(m, MaryXML.MTU)) // not highest-level
				continue;
			// Now m is a highest-level mtu element
			// Search for the token whose accent is retained;
			// all other accents will be deleted.
			Element c = m;
			while (c != null && !c.getTagName().equals(MaryXML.TOKEN)) {
				String whatToAccent = c.getAttribute("accent");
				if (whatToAccent != null && whatToAccent.equals("first"))
					c = MaryDomUtils.getFirstChildElement(c);
				else
					c = MaryDomUtils.getLastChildElement(c);
			}
			Element retainAccentToken = c;

			// Now all token below m except retainAccentToken get
			// their accent deleted.
			NodeList tokens = m.getElementsByTagName(MaryXML.TOKEN);
			for (int i = 0; i < tokens.getLength(); i++) {
				Element t = (Element) tokens.item(i);
				if (t != retainAccentToken) // not the same *Object*!
					t.removeAttribute("accent");
			}
		} // for all highest-level mtu elements
	}

	private void phonologicalRules(Document doc) {
		NodeList allTokens = doc.getElementsByTagName(MaryXML.TOKEN);
		for (int i = 0; i < allTokens.getLength(); i++) {
			Element t = (Element) allTokens.item(i);
			if (t.hasAttribute("ph")) { // otherwise there's no point
				String transcript = t.getAttribute("ph");
				// determine the pronunciation precision required:
				// The precision depends on two things:
				// a) the precision requested in the closest enclosing
				// <phonology> element
				int phonolPrecision = PhonologicalRules.NORMAL;
				Element phonolAncestor = (Element) MaryDomUtils.getAncestor(t, MaryXML.PHONOLOGY);
				if (phonolAncestor != null) {
					String phonolPrecisionString = phonolAncestor.getAttribute("precision");
					if (phonolPrecisionString != null) {
						if (phonolPrecisionString.equals("precise")) {
							phonolPrecision = PhonologicalRules.PRECISE;
						} else if (phonolPrecisionString.equals("sloppy")) {
							phonolPrecision = PhonologicalRules.SLOPPY;
						}
					}
				}
				// b) whether this token has an accent or not.
				boolean hasAccent = t.hasAttribute("accent") && !t.getAttribute("accent").equals("none");
				// Roughly, tokens that carry an accent are stepped
				// one step up in precision.
				int precision = phonolPrecision;
				if (hasAccent) {
					if (precision == PhonologicalRules.NORMAL)
						precision = PhonologicalRules.PRECISE;
					else if (precision == PhonologicalRules.SLOPPY)
						precision = PhonologicalRules.NORMAL;
				}
				List rules = PhonologicalRules.getRules();
				// for all rules
				for (Iterator it = rules.iterator(); it.hasNext();) {
					PhonologicalRules pr = (PhonologicalRules) it.next();
					// if a key matches
					if (pr.matches(transcript)) {
						// apply the rule and remember the result
						transcript = pr.apply(transcript, precision);
					}
					// apply more rules if more rules match
				}
				t.setAttribute("ph", transcript);
			} // if token has transcript
		} // for all tokens
	}

}
