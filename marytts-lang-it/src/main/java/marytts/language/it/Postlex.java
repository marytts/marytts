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

import java.util.Locale;

import marytts.datatypes.MaryData;
import marytts.datatypes.MaryXML;
import marytts.modules.PronunciationModel;
import marytts.util.dom.MaryDomUtils;
import marytts.util.dom.NameNodeFilter;

import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.traversal.DocumentTraversal;
import org.w3c.dom.traversal.NodeFilter;
import org.w3c.dom.traversal.TreeWalker;

/**
 * The postlexical phonological processes module. Used as first option to solve
 * proclitics tokens and sillabification merging
 * 
 * @author Fabio Tesser and Giulio Paci
 */

public class Postlex extends PronunciationModel {

	public Postlex() {
		super(Locale.ITALIAN);
	}

	public MaryData process(MaryData d) throws Exception {
		Document doc = d.getDocument();
		mtuPostlex(doc);
		return super.process(d);
	}

	private static void mergeIntoLastElement(Element c1, Element c2) {
		if ((c1 != null) && (c1 != null)) {
			c2.setAttribute("merged-token", "yes");
			c2.setAttribute("g2p_method", "compound");
			c2.setTextContent(c1.getTextContent() + "+" + c2.getTextContent());
			// TODO fix accents and syllabification
			c2.setAttribute("ph", c1.getAttribute("ph") + " " + c2.getAttribute("ph"));
		}
	}

	/*
	 * This method is used when proclitics are found in mtu proclitics is c'X
	 * (if there is in it_clitics.xml)
	 */
	private void mtuPostlex(Document doc) throws DOMException {
		TreeWalker tw = ((DocumentTraversal) doc).createTreeWalker(doc, NodeFilter.SHOW_ELEMENT, new NameNodeFilter(MaryXML.MTU), false);
		Element m = null;
		while ((m = (Element) tw.nextNode()) != null) {
			if (MaryDomUtils.hasAncestor(m, MaryXML.MTU)) // not highest-level
				continue;
			// Now m is a highest-level mtu element
			Element c = m;
			while (c != null && !c.getTagName().equals(MaryXML.TOKEN)) {
				String whatToAccent = c.getAttribute("accent");
				// check for last-proclitics (c' t' d' X)
				if (whatToAccent != null
						&& whatToAccent.equals("last-proclitics")) {
					Element c1 = MaryDomUtils.getFirstChildElement(c);
					boolean done = false;
					if (c1 != null) {
						while (!done) {
							done = true;
							Element c2 = MaryDomUtils.getNextSiblingElement(c1);
							if ((c2 != null) && (MaryXML.TOKEN.equals(c1.getTagName()))) {
								if (MaryXML.TOKEN.equals(c2.getTagName())) {
									Postlex.mergeIntoLastElement(c1, c2);
									c.removeChild(c1);
									c1 = c2;
									done = false;
								} else if (MaryXML.MTU.equals(c2.getTagName())) {
									while ((c2 != null) && (!MaryXML.TOKEN.equals(c2.getTagName()))) {
										c2 = MaryDomUtils.getFirstChildElement(c2);
									}
									if (c2 != null) {
										Postlex.mergeIntoLastElement(c1, c2);
										c.removeChild(c1);
									}
								}
							}
						}
					}
				}
				c = MaryDomUtils.getLastChildElement(c);
			}
		} // for all highest-level mtu elements
	}

}
