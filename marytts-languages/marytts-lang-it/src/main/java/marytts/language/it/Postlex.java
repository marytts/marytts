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

import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import marytts.datatypes.MaryData;
import marytts.datatypes.MaryDataType;
import marytts.datatypes.MaryXML;
import marytts.modules.InternalModule;
import marytts.modules.PronunciationModel;
import marytts.modules.phonemiser.AllophoneSet;
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
 * The postlexical phonological processes module. Used as first option to solve proclitics tokens and sillabification merging
 * 
 * @author Fabio Tesser
 */

public class Postlex extends PronunciationModel {

	public Postlex() {
		super(Locale.ITALIAN);
	}

	public MaryData process(MaryData d) throws Exception {
		// System.err.println("Italian Postlex START");
		Document doc = d.getDocument();
		mtuMergeTokenPostlex(doc);
		return super.process(d);
	}

	/*
	 * Return Quote space if quote present
	 */
	String returnQuoteIfStress(String lPhones) {
		if (lPhones.indexOf("'") != -1) {
			// System.out.println("there is ' in temp string");
			return "' ";
		} else {
			// System.out.println("there is no ' in temp string");
			return "";
		}
	}

	/*
	 * This method is used when proclitics are found in mtu proclitics is c'X (if there is in it_clitics.xml)
	 */
	private void mtuMergeTokenPostlex(Document doc) throws DOMException {
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
				// check for last-proclitics (c' t' d' X)
				if (whatToAccent != null && whatToAccent.equals("last-proclitics")) {
					// System.err.println("token to join!!! in " + c.getNodeName());
					Element c1 = MaryDomUtils.getFirstChildElement(c);
					// get the anchor as reference in order to delete the children after
					Element c_anchor = c;
					// set c as last (second) child element in this case
					// WARNING we treat the case with 2 token only!!!
					c = MaryDomUtils.getLastChildElement(c);
					// merge ph and POS?
					c.setAttribute("merged-token", "yes");
					c.setAttribute("g2p_method", c1.getAttribute("g2p_method") + "+" + c.getAttribute("g2p_method"));
					// TODO: accent= to merge? take the first or the second?
					// c.setAttribute("accent", c1.getAttribute("accent")); // + "+" + c.getAttribute("accent"));
					// c.removeAttribute("accent");
					c.setTextContent(c1.getTextContent() + "+" + c.getTextContent());
					// Merge the ph and write the quote if necessary
					c.setAttribute("ph",
							returnQuoteIfStress(c.getAttribute("ph")) + c1.getAttribute("ph") + " " + c.getAttribute("ph"));
					// TODO: POS are not merged if you want to merge the POS:
					// c.setAttribute("pos", c1.getAttribute("pos") + "+" +c.getAttribute("pos"));

					// c.setAttribute("pos",c1.getAttribute("pos");

					// remove child token
					c_anchor.removeChild(c1);
				} else
					c = MaryDomUtils.getLastChildElement(c);
			}

			/*
			 * Element retainAccentToken = c;
			 * 
			 * // Now all token below m except retainAccentToken get // their accent deleted. System.err.println("the olio" +
			 * m.getNodeName()); NodeList tokens = m.getElementsByTagName(MaryXML.TOKEN); System.err.println("OK number" +
			 * tokens.getLength()); for (int i=0; i<tokens.getLength(); i++) { Element t = (Element) tokens.item(i);
			 * System.err.println("OK" ); if (t == retainAccentToken) // not the same *Object*! {
			 * 
			 * System.err.println("VAI!!!!:" + t.getNodeName()); t.setNodeValue("aaaa"); //System.err.println("VAI!!!!:" + t.get);
			 * //Element syl = MaryDomUtils.getFirstChildElement(t); // System.err.println("VAI!!!!: " + syl.getLocalName()); } }
			 */

		} // for all highest-level mtu elements
	}

}
