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
package marytts.language.en;

import java.util.Locale;

import marytts.datatypes.MaryData;
import marytts.datatypes.MaryDataType;
import marytts.datatypes.MaryXML;
import marytts.util.MaryUtils;
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

	/**
     * 
     */
	public JTokeniser() {
		super(MaryDataType.RAWMARYXML, MaryDataType.TOKENS, Locale.ENGLISH);
	}

	public MaryData process(MaryData d) throws Exception {
		MaryData result = super.process(d);
		normaliseToAscii(result);
		return result;
	}

	protected void normaliseToAscii(MaryData d) {
		Document doc = d.getDocument();
		NodeIterator ni = ((DocumentTraversal) doc).createNodeIterator(doc, NodeFilter.SHOW_ELEMENT, new NameNodeFilter(
				MaryXML.TOKEN), false);
		Element t = null;
		while ((t = (Element) ni.nextNode()) != null) {
			String s = MaryDomUtils.tokenText(t);
			String normalised = MaryUtils.normaliseUnicodeLetters(s, Locale.ENGLISH);
			if (!s.equals(normalised)) {
				MaryDomUtils.setTokenText(t, normalised);
			}
		}
	}

	/**
	 * In current FreeTTS code, prosody elements get lost. So remember at least the force-accent element on individual tokens:
	 * 
	 * @param d
	 *            d
	 * @deprecated FreeTTS is no longer used, so this method no longer serves a purpose.
	 */
	@Deprecated
	protected void propagateForceAccent(MaryData d) {
		Document doc = d.getDocument();
		NodeIterator prosodyNI = ((DocumentTraversal) doc).createNodeIterator(doc, NodeFilter.SHOW_ELEMENT, new NameNodeFilter(
				MaryXML.PROSODY), false);
		Element prosody = null;
		while ((prosody = (Element) prosodyNI.nextNode()) != null) {
			if (prosody.hasAttribute("force-accent")) {
				String forceAccent = prosody.getAttribute("force-accent");
				String accent = null;
				if (forceAccent.equals("none")) {
					accent = "none";
				} else {
					accent = "unknown";
				}
				NodeIterator tNI = ((DocumentTraversal) doc).createNodeIterator(prosody, NodeFilter.SHOW_ELEMENT,
						new NameNodeFilter(MaryXML.TOKEN), false);
				Element t = null;
				while ((t = (Element) tNI.nextNode()) != null) {
					if (!t.hasAttribute("accent")) {
						t.setAttribute("accent", accent);
					}
				} // while t
			}
		} // while prosody
	}
}
