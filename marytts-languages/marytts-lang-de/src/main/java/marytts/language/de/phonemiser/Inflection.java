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
package marytts.language.de.phonemiser;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import marytts.datatypes.MaryXML;
import marytts.util.MaryUtils;
import marytts.util.dom.MaryDomUtils;
import marytts.util.dom.NameNodeFilter;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.traversal.DocumentTraversal;
import org.w3c.dom.traversal.NodeFilter;
import org.w3c.dom.traversal.NodeIterator;
import org.w3c.dom.traversal.TreeWalker;

/**
 * Add inflection endings to expanded abbreviations and ordinals.
 * 
 * @author Marc Schr&ouml;der
 *
 *
 */
public class Inflection {
	private final Map<String, String> endingTable;
	private Logger logger;

	public Inflection() throws IOException {
		endingTable = Collections.synchronizedMap(new HashMap<String, String>());
		// Ending class applicable to:
		// masc singular nom
		endingTable.put("1d", ""); // with definite determiner
		endingTable.put("1i", "r"); // with indefinite determiner
		endingTable.put("1", "r"); // without determiner
		// Ending class applicable to:
		// masc singular gen/acc, neutrum singular gen
		endingTable.put("2d", "n"); // with definite determiner
		endingTable.put("2i", "n"); // with indefinite determiner
		endingTable.put("2", "n"); // without determiner
		// Ending class applicable to:
		// masc/neutrum singular dat
		endingTable.put("3d", "n"); // with definite determiner
		endingTable.put("3i", "n"); // with indefinite determiner
		endingTable.put("3", "m"); // without determiner
		// Ending class applicable to:
		// masc/fem/neutrum plural nom/acc
		endingTable.put("4d", "n"); // with definite determiner
		endingTable.put("4", ""); // without determiner
		// Ending class applicable to:
		// masc/fem/neutrum plural gen
		endingTable.put("5d", "n"); // with definite determiner
		endingTable.put("5", "r"); // without determiner
		// Ending class applicable to:
		// masc/fem/neutrum plural dat
		endingTable.put("6d", "n"); // with definite determiner
		endingTable.put("6", "n"); // without determiner
		// Ending class applicable to:
		// fem singular nom/acc
		endingTable.put("7d", ""); // with definite determiner
		endingTable.put("7i", ""); // with indefinite determiner
		endingTable.put("7", ""); // without determiner
		// Ending class applicable to:
		// fem singular gen/dat
		endingTable.put("8d", "n"); // with definite determiner
		endingTable.put("8i", "n"); // with indefinite determiner
		endingTable.put("8", "r"); // without determiner
		// Ending class applicable to:
		// neutrum singular nom/acc
		endingTable.put("9d", ""); // with definite determiner
		endingTable.put("9i", "s"); // with indefinite determiner
		endingTable.put("9", "s"); // without determiner
		logger = MaryUtils.getLogger("Inflection");
	}

	public void determineEndings(Document doc) {
		// Search for <t> tags with an "ending" attribute:
		NodeIterator ni = ((DocumentTraversal) doc).createNodeIterator(doc, NodeFilter.SHOW_ELEMENT, new NodeFilter() {
			public short acceptNode(Node n) {
				if (!(n instanceof Element))
					return NodeFilter.FILTER_SKIP;
				Element e = (Element) n;
				if (e.getTagName().equals(MaryXML.TOKEN) && e.hasAttribute("ending"))
					return NodeFilter.FILTER_ACCEPT;
				return NodeFilter.FILTER_SKIP;
			}
		}, true);
		Element toInflect = null;
		while ((toInflect = (Element) ni.nextNode()) != null) {
			logger.debug("Token `" + MaryDomUtils.tokenText(toInflect) + "' needs an inflection ending.");
			// If it has an "ending" attribute, it must also have a "sounds_like"
			// attribute.
			if (!toInflect.hasAttribute("sounds_like")) {
				logger.warn("Token `" + MaryDomUtils.tokenText(toInflect)
						+ "' has an `ending' attribute, but no `sounds_like' attribute. Ignoring.");
				continue;
			}
			// For adverbial use, simply append "-ns":
			if (toInflect.getAttribute("ending").equals("ordinal") && toInflect.getAttribute("pos").equals("ADV")) {
				toInflect.setAttribute("sounds_like", toInflect.getAttribute("sounds_like") + "ns");
				logger.debug("...added adverbial ending.");
				continue;
			}
			// Otherwise, it is an adjective, so we need to analyse the NP/PP:
			// Start with the fullest possible set of ending classes, then
			// reduce ambiguity my means of the context.
			Set<String> endingClasses = new HashSet<String>(Arrays.asList(new String[] { "1", "2", "3", "4", "5", "6", "7", "8",
					"9" }));
			// Also need the determiner type:
			String detType = null;
			TreeWalker tw = ((DocumentTraversal) doc).createTreeWalker(doc, NodeFilter.SHOW_ELEMENT, new NameNodeFilter(
					MaryXML.TOKEN), true);
			tw.setCurrentNode(toInflect);
			// If toInflect is not the first token in the NP/PP,
			// search to the left:
			if (!toInflect.getAttribute("syn_attach").equals("1")) {
				boolean foundStart = false;
				Element t;
				while (!foundStart && (t = (Element) tw.previousNode()) != null) {
					String synAttach = t.getAttribute("syn_attach");
					if (synAttach.equals("1") || t.getAttribute("syn_phrase").equals("CNP")
							|| t.getAttribute("syn_phrase").equals("CPP")) {
						// Found the start of the NP/PP
						foundStart = true;
					}
					if (!synAttach.equals("2")) {
						// And try to find the determiner type:
						if (detType == null) {
							detType = getDeterminerType(t);
						}
					}
				}
			}
			// Then search to the right:
			tw.setCurrentNode(toInflect);
			Element t;
			boolean haveSeenNoun = false;
			while ((t = (Element) tw.nextNode()) != null && !t.getAttribute("syn_attach").equals("1") && // Stop at conjunction in
																											// coordinated noun
																											// phrases
																											// if the left part
																											// already has its own
																											// noun
																											// (as in:
																											// "der 2. Mann und die 3. Frau",
																											// as opposed to
																											// "der 2. und der 3. Mann").
					!((t.getAttribute("syn_phrase").equals("CNP") || t.getAttribute("syn_phrase").equals("CPP")) && haveSeenNoun)) {
				if (!t.getAttribute("syn_attach").equals("2")) {
					if (t.getAttribute("pos").equals("NN"))
						haveSeenNoun = true;
				}
			}
			// Now the disambiguation is complete.
			Set<String> endings = new HashSet<String>();
			Iterator<String> it = endingClasses.iterator();
			while (it.hasNext()) {
				String endingClass = it.next();
				String key = (detType == null ? endingClass : endingClass + detType);
				String ending = (String) endingTable.get(key);
				assert (ending != null);
				endings.add(ending);
				logger.debug("...ending class " + endingClass + " with "
						+ (detType == null ? "no" : (detType.equals("d") ? "definite" : "indefinite")) + " determiner: Ending `e"
						+ ending + "'");
			}
			// If there is exactly one ending in the endings Set, then we can use it:
			if (endings.size() == 1) {
				String ending = (String) endings.iterator().next();
				logger.debug("...correct ending should be `e" + ending + "'");
				StringBuilder soundsLike = new StringBuilder(toInflect.getAttribute("sounds_like"));
				// abbreviations don't have an "e" at the end, so add it:
				if (toInflect.getAttribute("ending").equals("adjadv"))
					soundsLike.append("e");
				soundsLike.append(ending);
				toInflect.setAttribute("sounds_like", soundsLike.toString());
			} else {
				logger.debug("...cannot determine right ending, using default `e'.");
			}
		}
	}

	/**
	 * For a given token t, try to determine whether it is a definite or indefinite determiner.
	 * 
	 * @param t
	 *            a token to verify.
	 * @return "d" for definite determiner, "i" for indefinite determiner, and null if the token is not a determiner.
	 */
	private String getDeterminerType(Element t) {
		// special case: APPRART (zum, hinters, ...) is a definite determiner
		// (not correctly treated in mmorph).
		if (t.getAttribute("pos").equals("APPRART"))
			return "d";
		return null;
	}

}