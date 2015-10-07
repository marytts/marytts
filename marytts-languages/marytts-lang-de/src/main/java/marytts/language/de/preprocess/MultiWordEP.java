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

package marytts.language.de.preprocess;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

import marytts.datatypes.MaryXML;
import marytts.util.MaryUtils;
import marytts.util.dom.MaryDomUtils;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * An expansion pattern implementation for abbreviation patterns.
 *
 * @author Marc Schr&ouml;der
 */

public class MultiWordEP extends ExpansionPattern {
	private final String[] _knownTypes = { "multiword" };
	private final List<String> knownTypes = Arrays.asList(_knownTypes);

	public List<String> knownTypes() {
		return knownTypes;
	}

	private static final Map<String, String> multiWordDict = new HashMap<String, String>();
	private static final Set<String> constituentWordSet = new HashSet<String>();

	// We don't use sMatchingChars here, but override isCandidate().
	private final Pattern reMatchingChars = null;

	public Pattern reMatchingChars() {
		return reMatchingChars;
	}

	private static final Logger logger = MaryUtils.getLogger("MultiWordEP");

	static {
		try {
			loadMultiWordDict();
		} catch (FileNotFoundException e) {
			logger.warn("Could not load abbreviation file", e);
		} catch (IOException e) {
			logger.warn("Could not load abbreviation file", e);
		}

	}

	public MultiWordEP() {
		super();
	}

	protected boolean isCandidate(Element t) {
		String str = MaryDomUtils.tokenText(t);
		return constituentWordSet.contains(str);
	}

	protected int canDealWith(String s, int type) {
		return match(s, type);
	}

	protected int match(String s, int type) {
		if (s.length() > 0)
			return type;
		return -1;
	}

	/**
	 * Expand multiwords and eventually replace them with <code>mtu</code> structures.
	 * 
	 * @param tokens
	 *            tokens
	 * @param s
	 *            s
	 * @param type
	 *            type
	 * @return expanded
	 */
	protected List<Element> expand(List<Element> tokens, String s, int type) {
		if (tokens == null)
			throw new NullPointerException("Received null argument");
		if (tokens.isEmpty())
			throw new IllegalArgumentException("Received empty list");
		// Expand the list of potential multi-token words.
		// First, try to find longest entries in database, then shorter.
		List<Element> expanded = new ArrayList<Element>();
		ArrayList<Element> match = new ArrayList<Element>(tokens);
		StringBuilder sb = new StringBuilder();
		String multiword = null;
		while (!match.isEmpty()) {
			sb.setLength(0);
			Iterator<Element> it = match.iterator();
			while (it.hasNext()) {
				sb.append(MaryDomUtils.tokenText((Element) it.next()));
				sb.append(" ");
			}
			String lookup = sb.toString().trim();
			logger.debug("Looking up multiword in dictionary: `" + lookup + "'");
			if (multiWordDict.containsKey(lookup)) {
				multiword = lookup;
				break; // OK, found a match
			}
			match.remove(match.size() - 1); // remove last in list
		}
		if (multiword != null) { // found a multiWordDict entry
			expanded.addAll(dictionaryExpandMultiWord(match, multiword));
			logger.debug("Have found multiword in dictionary: `" + multiword + "'");
		}
		if (logger.getEffectiveLevel().equals(Level.DEBUG)) {
			StringBuilder logBuf = new StringBuilder();
			for (Iterator<Element> it = expanded.iterator(); it.hasNext();) {
				Element elt = (Element) it.next();
				if (elt.getTagName().equals(MaryXML.TOKEN)) {
					logBuf.append(MaryDomUtils.tokenText(elt));
				} else {
					logBuf.append(elt.getTagName());
				}
				logBuf.append(" ");
			}
			logger.debug("Expanded multiword: " + logBuf.toString());
		}
		if (!expanded.isEmpty())
			replaceTokens(match, expanded);
		return expanded;
	}

	/**
	 * Expand a recognised multiword from the dictionary. <code>match</code> is the list of token elements forming the multiword;
	 * <code>abbrev</code> is a string representation of that multiword. Tokens for the expanded form are created, but not yet
	 * attached to the dom tree.
	 * 
	 * @param match
	 *            match
	 * @param multiword
	 *            multiword
	 * @return exp
	 */
	private List<Element> dictionaryExpandMultiWord(List<Element> match, String multiword) {
		Document doc = ((Element) match.get(0)).getOwnerDocument();
		ArrayList<Element> exp = new ArrayList<Element>();
		String graph = (String) multiWordDict.get(multiword);
		// graph = expanded form, possibly with pronunciation
		exp.addAll(makeNewTokens(doc, graph, true, multiword));
		return exp;
	}

	private static void loadMultiWordDict() throws FileNotFoundException, IOException {
		InputStream mwStream = MultiWordEP.class.getResourceAsStream("multiword.dat");
		BufferedReader br = new BufferedReader(new InputStreamReader(mwStream, "UTF-8"));
		String line;
		while ((line = br.readLine()) != null) {
			if (Pattern.compile("^\\#").matcher(line).find() || REPattern.emptyLine.matcher(line).find()) {
				// comment or empty line, ignore
				continue;
			}
			// Fields separated by a slash (/):
			StringTokenizer st = new StringTokenizer(line, "/");
			// Each line contains two fields,
			// key (the abbreviation),
			// and graph (the graphemic (and possibly phonemic) expanded form.
			// Remove leading/trailing whitespace from each field.
			String key = st.nextToken().trim();
			String graph = st.nextToken().trim();
			// In addition, replace all whitespace in key and graph by a single blank
			key = key.replaceAll("\\s+", " ");
			graph = graph.replaceAll("\\s+", " ");
			multiWordDict.put(key, graph);
			// In addition, make a note of all constituent words of key:
			constituentWordSet.addAll(Arrays.asList(key.split(" ")));
		}
	}

}
