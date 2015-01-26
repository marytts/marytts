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

package marytts.language.it;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import marytts.datatypes.MaryData;
import marytts.datatypes.MaryDataType;
import marytts.datatypes.MaryXML;
import marytts.language.it.preprocess.ExpansionPattern;
import marytts.modules.InternalModule;
import marytts.util.dom.MaryDomUtils;
import marytts.util.dom.NameNodeFilter;

import org.apache.log4j.Level;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.traversal.DocumentTraversal;
import org.w3c.dom.traversal.NodeFilter;
import org.w3c.dom.traversal.TreeWalker;

/**
 * The preprocessing module.
 * 
 * @author Marc Schr&ouml;der
 */

public class Preprocess extends InternalModule {

	public Preprocess() {
		super("Preprocess", MaryDataType.TOKENS, MaryDataType.WORDS, Locale.ITALIAN);
	}

	public MaryData process(MaryData d) throws Exception {
		Document doc = d.getDocument();
		logger.info("Expanding say-as elements...");
		expandSayasElements(doc);
		logger.info("Matching and expanding patterns...");
		matchAndExpandPatterns(doc);
		logger.info("Done.");
		MaryData result = new MaryData(outputType(), d.getLocale());
		result.setDocument(doc);
		return result;
	}

	private void expandSayasElements(Document doc) {
		NodeList sayasElements = doc.getElementsByTagName(MaryXML.SAYAS);
		for (int i = 0; i < sayasElements.getLength(); i++) {
			Element sayas = (Element) sayasElements.item(i);
			String type = sayas.getAttribute("type");
			ExpansionPattern ep = ExpansionPattern.getPattern(type);
			if (ep != null) {
				if (logger.getEffectiveLevel().equals(Level.DEBUG)) {
					logger.debug("Expanding say-as element of type " + type + ", containing text `"
							+ MaryDomUtils.getPlainTextBelow(sayas) + "'");
				}
				ep.match(sayas, type);
			} else {
				// Don't know how to handle type -- ignore
				logger.info("Don't know how to expand say-as type=\"" + type + "\"");
			}
		}
	}

	private void matchAndExpandPatterns(Document doc) {
		TreeWalker tw = ((DocumentTraversal) doc).createTreeWalker(doc, NodeFilter.SHOW_ELEMENT,
				new NameNodeFilter(MaryXML.TOKEN), false);
		Element t = null;
		while ((t = (Element) tw.nextNode()) != null) {
			// System.err.println("matching and expanding " + MaryDomUtils.tokenText(t));
			// Skip tokens inside say-as tags, as well as tokens
			// for which a pronunciation is given:
			if (MaryDomUtils.hasAncestor(t, MaryXML.SAYAS) || t.hasAttribute("ph") || t.hasAttribute("sounds_like")) {
				// ignore token
				continue;
			}
			Iterator it = ExpansionPattern.allPatterns().iterator();
			boolean fullyExpanded = false;
			while (!fullyExpanded && it.hasNext()) {
				ExpansionPattern ep = (ExpansionPattern) it.next();
				logger.debug("Now applying ep " + ep + " to token " + MaryDomUtils.getPlainTextBelow(t));
				List expanded = new ArrayList();
				fullyExpanded = ep.process(t, expanded);
				// Element replacements may have been caused by ep.process());
				// Update t and tw accordingly: the next position to look at is
				// - if fully expanded, the token after the last expanded token
				// - else,
				// -- if no expansion occurred, the same position as before;
				// -- if partial expansion occurred, the first of the expanded tokens.
				if (fullyExpanded) {
					logger.debug("fully expanded");
					assert !expanded.isEmpty();
					// need to correct tw
					Element lastToken = getLastToken(expanded);
					assert lastToken != null;
					tw.setCurrentNode(lastToken);
					logger.debug("set treewalker position:" + MaryDomUtils.getPlainTextBelow((Element) tw.getCurrentNode()));
				} else { // not fully expanded
					if (!expanded.isEmpty()) { // partial expansion
						logger.debug("non-final expansion");
						// need to set t
						t = getFirstToken(expanded);
						assert t != null;
						// set tw as if fully expanded, just in case no further expansions occur
						// Element lastToken = getLastToken(expanded);
						// assert lastToken != null;
						tw.setCurrentNode(t);
					}
				}

			} // all patterns
		} // all tokens
	}

	/**
	 * Find the last token in the list of elements l. Starting from the last element in the list, if the element itself is a
	 * token, return it; else, if it has a direct or indirect descendant which is a token, return that one; else, go backwards in
	 * the list.
	 * 
	 * @param l
	 *            a list of elements
	 * @return the last token, or null if no such token can be found
	 */
	private Element getLastToken(List l) {
		if (l == null)
			throw new NullPointerException("Received null argument");
		if (l.isEmpty())
			throw new IllegalArgumentException("Received empty list");
		for (int i = l.size() - 1; i >= 0; i--) {
			Element e = (Element) l.get(i);
			Element t = null;
			if (e.getTagName().equals(MaryXML.TOKEN)) {
				t = e;
			} else {
				t = MaryDomUtils.getLastElementByTagName(e, MaryXML.TOKEN);
			}
			if (t != null)
				return t;
		}
		return null;
	}

	/**
	 * Find the first token in the list of elements l. Starting from the first element in the list, if the element itself is a
	 * token, return it; else, if it has a direct or indirect descendant which is a token, return that one; else, go forward in
	 * the list.
	 * 
	 * @param l
	 *            a list of elements
	 * @return the first token, or null if no such token can be found
	 */
	private Element getFirstToken(List l) {
		if (l == null)
			throw new NullPointerException("Received null argument");
		if (l.isEmpty())
			throw new IllegalArgumentException("Received empty list");
		for (int i = 0; i < l.size(); i++) {
			Element e = (Element) l.get(i);
			Element t = null;
			if (e.getTagName().equals(MaryXML.TOKEN)) {
				t = e;
			} else {
				t = MaryDomUtils.getFirstElementByTagName(e, MaryXML.TOKEN);
			}
			if (t != null)
				return t;
		}
		return null;
	}

}
