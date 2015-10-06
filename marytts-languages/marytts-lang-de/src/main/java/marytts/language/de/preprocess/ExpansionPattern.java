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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import marytts.datatypes.MaryXML;
import marytts.util.MaryUtils;
import marytts.util.dom.MaryDomUtils;

import org.apache.log4j.Logger;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * For preprocessing, serve as a base class for the different types of possible expansion patterns. For simplicity's sake, it is
 * implemented in a "greedy" way: As soon as an expansion pattern matches, it is applied, i.e. the matched tokens are expanded
 * according to the expansion rules in the pattern.
 *
 * @author Marc Schr&ouml;der
 */

public abstract class ExpansionPattern {
	protected static MultiWordEP multiword;
	protected static CompositeEP composite;
	protected static NetEP net;
	protected static DateEP date;
	protected static TimeEP time;
	protected static DurationEP duration;
	protected static CurrencyEP currency;
	protected static MeasureEP measure;
	protected static TelephoneEP telephone;
	protected static NumberEP number;
	protected static AbbrevEP abbrev;
	protected static SpecialCharEP specialChar;

	private static List<ExpansionPattern> expansionPatterns;
	private static Map<String, ExpansionPattern> patternTable;

	/**
	 * Initialize the various patterns. Notice that the order in which they are added to List expansionPatterns is most important:
	 * If several patterns potentially would match a given input, the one first found in the list will be applied. Therefore,
	 * frequent and well-identifiable cases should come first, while exotic or fall-back cases (like simple integer expansion)
	 * should come last in the list.
	 */
	static {
		expansionPatterns = new ArrayList<ExpansionPattern>();
		patternTable = new HashMap<String, ExpansionPattern>();
		Iterator<String> it;
		multiword = new MultiWordEP();
		expansionPatterns.add(multiword);
		for (it = multiword.knownTypes().iterator(); it.hasNext();)
			patternTable.put(it.next(), multiword);
		net = new NetEP();
		expansionPatterns.add(net);
		for (it = net.knownTypes().iterator(); it.hasNext();)
			patternTable.put(it.next(), net);
		composite = new CompositeEP();
		expansionPatterns.add(composite);
		for (it = composite.knownTypes().iterator(); it.hasNext();)
			patternTable.put(it.next(), composite);
		date = new DateEP();
		expansionPatterns.add(date);
		for (it = date.knownTypes().iterator(); it.hasNext();)
			patternTable.put(it.next(), date);
		time = new TimeEP();
		expansionPatterns.add(time);
		for (it = time.knownTypes().iterator(); it.hasNext();)
			patternTable.put(it.next(), time);
		// Putting duration after time means that duration patterns,
		// which have the same form as a subset of time patterns,
		// will actually never match without being explicitly requested
		// through <say-as> annotation.
		duration = new DurationEP();
		expansionPatterns.add(duration);
		for (it = duration.knownTypes().iterator(); it.hasNext();)
			patternTable.put(it.next(), duration);
		currency = new CurrencyEP();
		expansionPatterns.add(currency);
		for (it = currency.knownTypes().iterator(); it.hasNext();)
			patternTable.put(it.next(), currency);
		measure = new MeasureEP();
		expansionPatterns.add(measure);
		for (it = measure.knownTypes().iterator(); it.hasNext();)
			patternTable.put(it.next(), measure);
		telephone = new TelephoneEP();
		expansionPatterns.add(telephone);
		for (it = telephone.knownTypes().iterator(); it.hasNext();)
			patternTable.put(it.next(), telephone);
		abbrev = new AbbrevEP();
		expansionPatterns.add(abbrev);
		for (it = abbrev.knownTypes().iterator(); it.hasNext();)
			patternTable.put(it.next(), abbrev);
		number = new NumberEP();
		expansionPatterns.add(number);
		for (it = number.knownTypes().iterator(); it.hasNext();)
			patternTable.put(it.next(), number);
		specialChar = new SpecialCharEP();
		expansionPatterns.add(specialChar);
		for (it = specialChar.knownTypes().iterator(); it.hasNext();)
			patternTable.put(it.next(), specialChar);
	}

	public static List<ExpansionPattern> allPatterns() {
		return expansionPatterns;
	}

	public static ExpansionPattern getPattern(String typeString) {
		return (ExpansionPattern) patternTable.get(typeString);
	}

	/**
	 * A regular expression matching the characters at which a token should be split into parts before any preprocessing patterns
	 * are applied.
	 * 
	 * @return specialChar.getRESplitAtChars()
	 * @see SpecialCharEP#getRESplitAtChars
	 */
	public static Pattern reSplitAtChars() {
		return specialChar.getRESplitAtChars();
	}

	/**
	 * A string containing the characters at which a token should be split into parts before any preprocessing patterns are
	 * applied.
	 * 
	 * @return specialChar.splitAtChars()
	 * @see SpecialCharEP#splitAtChars
	 */
	public static String getSplitAtChars() {
		return specialChar.splitAtChars();
	}

	private static Logger logger = MaryUtils.getLogger("ExpansionPattern");

	public ExpansionPattern() {
	}

	/**
	 * Whether patterns of this type can be composed of several tokens.
	 * 
	 * @return true
	 */
	protected boolean allowMultipleTokens() {
		return true;
	}

	/**
	 * Inform whether this module performs a full expansion of the input, or whether other patterns should be applied after this
	 * one.
	 * 
	 * @return true
	 */
	protected boolean doesFullExpansion() {
		return true;
	}

	/**
	 * Returns the types known by this ExpansionPattern. These are possible values of the <code>type</code> attribute to the
	 * <code>say-as</code> element, as defined in MaryXML.dtd. Each subclass needs to override this to return something
	 * meaningful.
	 * 
	 * @return known types
	 */
	public abstract List<String> knownTypes();

	/**
	 * Returns the regular expression object matching any of the chars occurring in the pattern. Each subclass needs to override
	 * this to return something meaningful.
	 * 
	 * @return reMatchingChars
	 */
	public abstract Pattern reMatchingChars();

	/**
	 * Try to match this pattern starting at token <code>t</code>. If successful, replace the matched tokens with the replaced
	 * form.
	 * 
	 * @param t
	 *            the element to expand. After processing, this Element will still exist and be a valid Element, but possibly with
	 *            a different content, and possibly enclosed by an &lt;mtu&gt; element. In addition, &lt;t&gt; may have new
	 *            right-hand neighbors.
	 * @param expanded
	 *            an empty list into which the expanded Elements are placed if an expansion occurred. The list will remain empty
	 *            if no expansion was performed. Elements placed in the list are not guaranteed to be only t elements, but may be
	 *            elements enclosing the expanded t elements, such as mtu elements, as well as non-t empty elements (such as
	 *            boundary elements). If the list is non-empty, it is guaranteed to contain (either directly or as descendants of
	 *            the list items) at least one t element.
	 * @return true if this pattern is confident to have fully expanded this list of tokens, false if nothing could be done or
	 *         more expansion may be necessary.
	 */
	public boolean process(Element t, final List<Element> expanded) {
		if (t == null || expanded == null)
			throw new NullPointerException("Received null argument");
		if (!t.getTagName().equals(MaryXML.TOKEN))
			throw new DOMException(DOMException.INVALID_ACCESS_ERR, "Expected t element");
		if (!expanded.isEmpty())
			throw new IllegalArgumentException("Expected empty list, but list has " + expanded.size() + " elements.");
		StringBuilder sb = new StringBuilder();
		int matchedType = -1;
		ArrayList<Element> candidates = new ArrayList<Element>();
		if (allowMultipleTokens()) {
			Element n = t;
			// Do a look-forward preselection in order to find possible
			// candidates for tokens forming a pattern with t: They need to be
			// siblings and contain at least one of the characters occurring in
			// the pattern (as represented by the regular expression
			// reMatchingChars).
			while (n != null && n.getTagName().equals(MaryXML.TOKEN) && !n.hasAttribute("ph") && !n.hasAttribute("sounds_like")
					&& isCandidate(n)) {
				// System.err.println("Found candidate \"" + MaryDomUtils.tokenText(n) + "\" for " + this.getClass().getName());
				candidates.add(n);
				n = MaryDomUtils.getNextSiblingElement(n);
			}
			if (candidates.isEmpty()) // t itself is not a candidate
				return false; // quick exit for non-candidates
			// Now candidates contains the list of tokens that are worth
			// looking at more closely.
			while (!candidates.isEmpty()) {
				sb.setLength(0);
				Iterator<Element> it = candidates.iterator();
				while (it.hasNext()) {
					sb.append(MaryDomUtils.tokenText((Element) it.next()));
				}
				// System.err.println(this.getClass().getName() + ", trying to match: " + sb.toString() + "(t=" +
				// MaryDomUtils.tokenText(t) + ", candidates.size()=" + candidates.size() + ")");
				matchedType = match(sb.toString(), 0); // 0 == most general type
				if (matchedType != -1)
					break; // OK, found a match
				candidates.remove(candidates.size() - 1); // remove last in list
			}
		} else { // only a single token allowed
			if (!t.hasAttribute("ph") && !t.hasAttribute("sounds_like") && isCandidate(t)) {
				sb.setLength(0);
				sb.append(MaryDomUtils.tokenText(t));
				matchedType = match(sb.toString(), 0); // 0 == most general type
				candidates.add(t);
			}
		}
		if (matchedType != -1) { // found a match
			logger.debug("Found match, type " + knownTypes().get(matchedType) + ": " + sb.toString() + " (" + candidates.size()
					+ " tokens)");
			expanded.addAll(expand(candidates, sb.toString(), matchedType));
			if (expanded.isEmpty() && !knownTypes().get(matchedType).equals("specialChar")) {
				logger.info("Could match, but not expand string \"" + sb + "\" as type " + knownTypes().get(matchedType));
			}
			return !expanded.isEmpty() && doesFullExpansion();
		} else { // no match found
			return false;
		}
	}

	protected boolean isCandidate(Element t) {
		return reMatchingChars().matcher(MaryDomUtils.tokenText(t)).find();
	}

	/**
	 * Try to match and expand the entirety of tokens enclosed by the say-as tag <code>sayas</code>. The <code>type</code> of data
	 * to expand is given. If the tokens can be matched according to <code>type</code>, they are expanded. Throws DOMException if
	 * <code>sayas</code>'s tag name is not "say-as".
	 * 
	 * @param sayas
	 *            sayas
	 * @param typeString
	 *            typeString
	 * @throws DOMException
	 *             DOMException
	 */
	public void match(Element sayas, String typeString) throws DOMException {
		if (!sayas.getTagName().equals(MaryXML.SAYAS))
			throw new DOMException(DOMException.INVALID_ACCESS_ERR, "Expected " + MaryXML.SAYAS + " element, got "
					+ sayas.getTagName());
		List<Node> tokenNodes = MaryDomUtils.getNodeListAsList(sayas.getElementsByTagName(MaryXML.TOKEN));
		List<Element> tokens = new ArrayList<Element>();
		for (Node n : tokenNodes) {
			tokens.add((Element) n);
		}
		StringBuilder sb = new StringBuilder();
		for (Iterator<Element> it = tokens.iterator(); it.hasNext();) {
			sb.append(MaryDomUtils.tokenText((Element) it.next()));
		}
		int type = knownTypes().indexOf(typeString);
		int expandType = canDealWith(sb.toString(), type);
		if (expandType != -1) { // OK, we can expand this
			// System.err.println("Say-as requested type \"" + knownTypes().get(type) + "\" for text \"" + sb.toString() +
			// "\": can expand.");
			List<Element> expanded = expand(tokens, sb.toString(), expandType);
			if (expanded.isEmpty())
				logger.info("Failure expanding string \"" + sb + "\" as type \"" + knownTypes().get(expandType) + "\"");
		} else { // cannot expand according to sayas wish
			logger.info("Cannot expand string \"" + sb.toString() + "\" as requested type \"" + typeString + "\"");
		}
	}

	/**
	 * Decide whether we can expand a string according to type <code>typeCode</code>. This is important in cases where a
	 * particular expansion is requested via a <code>say-as</code> element. As a default, reply that a string can be expanded if
	 * it would be matched by the pattern recogniser. Subclasses may wish to override this with less strict requirements. Returns
	 * the type as which it can be expanded, or -1 if expansion is not possible.
	 * 
	 * @param input
	 *            input
	 * @param typeCode
	 *            typeCode
	 * @return true if it can deal with input and typecode
	 */
	protected abstract int canDealWith(String input, int typeCode);

	// formerly: {return match(input, typeCode); }

	/**
	 * Subclasses do their matching in this class.
	 * 
	 * @param input
	 *            is the String to be matched,
	 * @param typeCode
	 *            is the index in <code>knownTypes</code> to match with.
	 * @return type actually matched on successful match with this type (if <code>typeCode</code> is a general type (
	 *         <code>typeCode == 0</code>), it may have matched with a more specific subtype). On failure, <code>-1</code> is
	 *         returned.
	 */
	protected abstract int match(String input, int typeCode);

	/**
	 * Subclasses do their expansion in this class.
	 * 
	 * @param tokens
	 *            is a list of token Elements to be replaced with their expanded form. The expanded forms are inserted into the
	 *            DOM tree at the same positions as the tokens in List <code>tokens</code>. If there are more new tokens than old
	 *            tokens, the rest are inserted as siblings at the position of the last old token.
	 * @param text
	 *            is the String to be expanded,
	 * @param typeCode
	 *            is the index in <code>knownTypes</code> this string has matched with before.
	 * @return the list of expanded (=new) tokens.
	 */
	protected abstract List<Element> expand(List<Element> tokens, String text, int typeCode);

	/**
	 * The default way to create new token DOM elements from whitespace-separated tokens in a string. String tokens have the form<br>
	 * <code>graph</code> or <code>graph[phon]</code>, where the optional <code>phon</code>, if present, is set as value to the
	 * <code>sampa</code> attribute of the <code>t</code> element.
	 * <p>
	 * All expansion patterns that do not require any special attribute settings should create their new tokens using this method.
	 * <p>
	 * Returns a list of token elements created from Document <code>doc</code>, but not yet attached in the tree.
	 * 
	 * @param doc
	 *            doc
	 * @param newText
	 *            newText
	 * @return makeNewTokens(doc, newText, false, null)
	 */
	protected List<Element> makeNewTokens(Document doc, String newText) {
		return makeNewTokens(doc, newText, false, null);
	}

	protected List<Element> makeNewTokens(Document doc, String newText, boolean createMtu, String origText) {
		return makeNewTokens(doc, newText, createMtu, origText, false);
	}

	protected List<Element> makeNewTokens(Document doc, String newText, boolean createMtu, String origText, boolean forceAccents) {
		if (newText == null || newText.length() == 0) {
			// unusable input
			return null; // failure
		}
		Pattern rePron = Pattern.compile("\\[(.*)\\]"); // pronunciation in square brackets
		StringTokenizer st = new StringTokenizer(newText);
		ArrayList<Element> newTokens = new ArrayList<Element>();
		while (st.hasMoreTokens()) {
			// Create new token element:
			String text = st.nextToken();
			Element newT = MaryXML.createElement(doc, MaryXML.TOKEN);
			Matcher remPron = rePron.matcher(text);
			if (remPron.find()) {
				String pron = remPron.group(1); // would be $1 in perl
				text = rePron.matcher(text).replaceFirst(""); // delete pronunciation from word
				newT.setAttribute("ph", pron);
			}
			MaryDomUtils.setTokenText(newT, text);
			if (forceAccents)
				newT.setAttribute("accent", "unknown");
			newTokens.add(newT);
		}
		if (createMtu) {
			// create mtu element enclosing the expanded tokens:
			Element mtu = MaryXML.createElement(doc, MaryXML.MTU);
			mtu.setAttribute("orig", origText);
			mtu.setAttribute("accent", "last");
			for (Iterator<Element> it = newTokens.iterator(); it.hasNext();) {
				mtu.appendChild((Element) it.next());
			}
			List<Element> result = new ArrayList<Element>();
			result.add(mtu);
			return result;
		} else {
			return newTokens;
		}
	}

	protected void replaceTokens(List<Element> oldTokens, List<Element> newTokens) {
		if (oldTokens == null || oldTokens.isEmpty() || newTokens == null || newTokens.isEmpty()) {
			// unusable input
			throw new NullPointerException("Have received null or empty argument.");
		}
		Element oldT = null;
		Iterator<Element> itOld = oldTokens.iterator();
		Iterator<Element> itNew = newTokens.iterator();
		while (itNew.hasNext()) {
			Element newT = (Element) itNew.next();
			// Retrieve old token element:
			if (itOld.hasNext()) // this is true at least once
				oldT = (Element) itOld.next();
			oldT.getParentNode().insertBefore(newT, oldT);
			if (itOld.hasNext()) // only remove this old t if there is another one
				oldT.getParentNode().removeChild(oldT);
		}
		if (!itOld.hasNext()) { // only need to remove oldT
			oldT.getParentNode().removeChild(oldT);
		} else {
			// there were more old than new tokens
			while (itOld.hasNext()) {
				oldT = (Element) itOld.next();
				oldT.getParentNode().removeChild(oldT);
			}
		}
		// Now go through the new tokens again and see if there are any
		// useless mtu combinations. If so, the "inner" one wins.
		itNew = newTokens.iterator();
		while (itNew.hasNext()) {
			Element mtu = (Element) itNew.next();
			if (!mtu.getTagName().equals(MaryXML.MTU))
				continue;
			Element parent = (Element) mtu.getParentNode();
			if (!parent.getTagName().equals(MaryXML.MTU))
				continue;
			// OK, got an mtu inside an mtu
			if (MaryDomUtils.getPreviousSiblingElement(mtu) != null || MaryDomUtils.getNextSiblingElement(mtu) != null)
				continue;
			if (!parent.getAttribute("orig").equals(mtu.getAttribute("orig")))
				continue;
			// OK, mtu and parent are mtu tags, there is no other element in parent
			// than mtu, and both have the same orig value
			// => delete parent
			Element grandParent = (Element) parent.getParentNode();
			grandParent.insertBefore(mtu, parent);
			grandParent.removeChild(parent);
		}
	}

	/**
	 * Enclose token in a &lt;prosody rate="..."&gt; tag in order to slow the spelling down, and in a &lt;phonology&gt; tag in
	 * order to enforce precise pronunciation.
	 * 
	 * @param e
	 *            e
	 */
	protected void slowDown(Element e) {
		Document doc = e.getOwnerDocument();
		Element whereToInsert = e;
		Element prosody = null;
		Element phonol = null;
		if (whereToInsert.getParentNode().getNodeName().equals(MaryXML.PHONOLOGY)) {
			// There is already a phonology tag enclosing us.
			phonol = (Element) whereToInsert.getParentNode();
			if (phonol.getParentNode().getNodeName().equals(MaryXML.PROSODY)) {
				// And also a prosody tag enclosing us.
				prosody = (Element) phonol.getParentNode();
			}
		} else {
			phonol = MaryXML.createElement(doc, MaryXML.PHONOLOGY);
			prosody = MaryXML.createElement(doc, MaryXML.PROSODY);
			prosody.appendChild(phonol);
			whereToInsert.getParentNode().insertBefore(prosody, whereToInsert);
			phonol.appendChild(whereToInsert);
		}
		prosody.setAttribute("rate", "-20%");
		phonol.setAttribute("precision", "precise");
	}

	/**
	 * Enclose the elements' closest common ancestor.
	 * 
	 * @param first
	 *            first
	 * @param last
	 *            last
	 */
	protected void slowDown(Element first, Element last) {
		Element phonol = MaryDomUtils.encloseNodesWithNewElement(first, last, MaryXML.PHONOLOGY);
		phonol.setAttribute("precision", "precise");
		Document doc = phonol.getOwnerDocument();
		Element prosody = MaryXML.createElement(doc, MaryXML.PROSODY);
		prosody.setAttribute("rate", "-20%");
		phonol.getParentNode().insertBefore(prosody, phonol);
		prosody.appendChild(phonol);
	}
}
