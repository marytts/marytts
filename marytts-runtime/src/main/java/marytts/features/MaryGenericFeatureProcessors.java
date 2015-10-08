/**
 * Portions Copyright 2006-2007 DFKI GmbH.
 * Portions Copyright 2001 Sun Microsystems, Inc.
 * Portions Copyright 1999-2001 Language Technologies Institute, 
 * Carnegie Mellon University.
 * All Rights Reserved.  Use is subject to license terms.
 * 
 * Permission is hereby granted, free of charge, to use and distribute
 * this software and its documentation without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of this work, and to
 * permit persons to whom this work is furnished to do so, subject to
 * the following conditions:
 * 
 * 1. The code must retain the above copyright notice, this list of
 *    conditions and the following disclaimer.
 * 2. Any modifications must be clearly marked as such.
 * 3. Original authors' names are not deleted.
 * 4. The authors' names are not used to endorse or promote products
 *    derived from this software without specific prior written
 *    permission.
 *
 * DFKI GMBH AND THE CONTRIBUTORS TO THIS WORK DISCLAIM ALL WARRANTIES WITH
 * REGARD TO THIS SOFTWARE, INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS, IN NO EVENT SHALL DFKI GMBH NOR THE
 * CONTRIBUTORS BE LIABLE FOR ANY SPECIAL, INDIRECT OR CONSEQUENTIAL
 * DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR
 * PROFITS, WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS
 * ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE OF
 * THIS SOFTWARE.
 */

package marytts.features;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;

import marytts.datatypes.MaryXML;
import marytts.unitselection.select.DiphoneTarget;
import marytts.unitselection.select.HalfPhoneTarget;
import marytts.unitselection.select.Target;
import marytts.util.dom.MaryDomUtils;
import marytts.util.string.ByteStringTranslator;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.traversal.TreeWalker;

/**
 * A collection of feature processors that operate on Target objects.
 * 
 * @author schroed
 *
 */
public class MaryGenericFeatureProcessors {
	/**
	 * Navigate from a target to an item. Classes implementing this interface will retrieve meaningful items given the target.
	 * 
	 * @author Marc Schr&ouml;der
	 */
	public static interface TargetElementNavigator {
		/**
		 * Given the target, retrieve an XML Element.
		 * 
		 * @param target
		 *            target
		 * @return an item selected according to this navigator, or null if there is no such item.
		 */
		public Element getElement(Target target);
	}

	/**
	 * Retrieve the segment belonging to this target.
	 * 
	 * @author Marc Schr&ouml;der
	 *
	 */
	public static class SegmentNavigator implements TargetElementNavigator {
		public Element getElement(Target target) {
			return target.getMaryxmlElement();
		}
	}

	/**
	 * Retrieve the segment preceding the segment which belongs to this target.
	 * 
	 * @author Marc Schr&ouml;der
	 *
	 */
	public static class PrevSegmentNavigator implements TargetElementNavigator {
		public Element getElement(Target target) {
			Element segment = target.getMaryxmlElement();
			if (segment == null)
				return null;
			Element sentence = (Element) MaryDomUtils.getAncestor(segment, MaryXML.SENTENCE);
			if (sentence == null)
				return null;
			TreeWalker tw = MaryDomUtils.createTreeWalker(sentence, MaryXML.PHONE, MaryXML.BOUNDARY);
			tw.setCurrentNode(segment);
			Element previous = (Element) tw.previousNode();
			return previous;
		}
	}

	/**
	 * Retrieve the segment two before the segment which belongs to this target.
	 * 
	 * @author Marc Schr&ouml;der
	 *
	 */
	public static class PrevPrevSegmentNavigator implements TargetElementNavigator {
		public Element getElement(Target target) {
			Element segment = target.getMaryxmlElement();
			if (segment == null)
				return null;
			Element sentence = (Element) MaryDomUtils.getAncestor(segment, MaryXML.SENTENCE);
			if (sentence == null)
				return null;
			TreeWalker tw = MaryDomUtils.createTreeWalker(sentence, MaryXML.PHONE, MaryXML.BOUNDARY);
			tw.setCurrentNode(segment);
			Element previous = (Element) tw.previousNode();
			Element pp = (Element) tw.previousNode();
			return pp;
		}
	}

	/**
	 * Retrieve the segment following the segment which belongs to this target.
	 * 
	 * @author Marc Schr&ouml;der
	 *
	 */
	public static class NextSegmentNavigator implements TargetElementNavigator {
		public Element getElement(Target target) {
			Element segment = target.getMaryxmlElement();
			if (segment == null)
				return null;
			Element sentence = (Element) MaryDomUtils.getAncestor(segment, MaryXML.SENTENCE);
			if (sentence == null)
				return null;
			TreeWalker tw = MaryDomUtils.createTreeWalker(sentence, MaryXML.PHONE, MaryXML.BOUNDARY);
			tw.setCurrentNode(segment);
			Element next = (Element) tw.nextNode();
			return next;
		}
	}

	/**
	 * Retrieve the segment two after the segment which belongs to this target.
	 * 
	 * @author Marc Schr&ouml;der
	 *
	 */
	public static class NextNextSegmentNavigator implements TargetElementNavigator {
		public Element getElement(Target target) {
			Element segment = target.getMaryxmlElement();
			if (segment == null)
				return null;
			Element sentence = (Element) MaryDomUtils.getAncestor(segment, MaryXML.SENTENCE);
			if (sentence == null)
				return null;
			TreeWalker tw = MaryDomUtils.createTreeWalker(sentence, MaryXML.PHONE, MaryXML.BOUNDARY);
			tw.setCurrentNode(segment);
			Element next = (Element) tw.nextNode();
			Element nn = (Element) tw.nextNode();
			return nn;
		}
	}

	/**
	 * Retrieve the first segment in the word to which this target belongs.
	 *
	 */
	public static class FirstSegmentInWordNavigator implements TargetElementNavigator {
		public Element getElement(Target target) {
			Element segment = target.getMaryxmlElement();
			if (segment == null)
				return null;
			Element word = (Element) MaryDomUtils.getAncestor(segment, MaryXML.TOKEN);
			if (word == null)
				return null;
			TreeWalker tw = MaryDomUtils.createTreeWalker(word, MaryXML.PHONE);
			Element first = (Element) tw.firstChild();
			if (first != null) {
				assert first.getTagName().equals(MaryXML.PHONE) : "Unexpected tag name: expected " + MaryXML.PHONE + ", got "
						+ first.getTagName();
			}
			return first;
		}
	}

	/**
	 * Retrieve the last segment in the word to which this target belongs.
	 *
	 */
	public static class LastSegmentInWordNavigator implements TargetElementNavigator {
		public Element getElement(Target target) {
			Element segment = target.getMaryxmlElement();
			if (segment == null)
				return null;
			Element word = (Element) MaryDomUtils.getAncestor(segment, MaryXML.TOKEN);
			if (word == null)
				return null;
			TreeWalker tw = MaryDomUtils.createTreeWalker(word, MaryXML.PHONE);
			Element last = (Element) tw.lastChild();
			if (last != null) {
				assert last.getTagName().equals(MaryXML.PHONE) : "Unexpected tag name: expected " + MaryXML.PHONE + ", got "
						+ last.getTagName();
			}
			return last;
		}
	}

	/**
	 * Retrieve the first syllable in the word to which this target belongs.
	 *
	 */
	public static class FirstSyllableInWordNavigator implements TargetElementNavigator {
		public Element getElement(Target target) {
			Element segment = target.getMaryxmlElement();
			if (segment == null)
				return null;
			Element word = (Element) MaryDomUtils.getAncestor(segment, MaryXML.TOKEN);
			if (word == null)
				return null;
			TreeWalker tw = MaryDomUtils.createTreeWalker(word, MaryXML.SYLLABLE);
			Element first = (Element) tw.firstChild();
			if (first != null) {
				assert first.getTagName().equals(MaryXML.SYLLABLE) : "Unexpected tag name: expected " + MaryXML.SYLLABLE
						+ ", got " + first.getTagName();
			}
			return first;
		}
	}

	/**
	 * Retrieve the last syllable in the word to which this target belongs.
	 *
	 */
	public static class LastSyllableInWordNavigator implements TargetElementNavigator {
		public Element getElement(Target target) {
			Element segment = target.getMaryxmlElement();
			if (segment == null)
				return null;
			Element word = (Element) MaryDomUtils.getAncestor(segment, MaryXML.TOKEN);
			if (word == null)
				return null;
			TreeWalker tw = MaryDomUtils.createTreeWalker(word, MaryXML.SYLLABLE);
			Element last = (Element) tw.lastChild();
			if (last != null) {
				assert last.getTagName().equals(MaryXML.SYLLABLE) : "Unexpected tag name: expected " + MaryXML.SYLLABLE
						+ ", got " + last.getTagName();
			}
			return last;
		}
	}

	/**
	 * Retrieve the syllable belonging to this target.
	 * 
	 * @author Marc Schr&ouml;der
	 *
	 */
	public static class SyllableNavigator implements TargetElementNavigator {
		public Element getElement(Target target) {
			Element segment = target.getMaryxmlElement();
			if (segment == null)
				return null;
			if (!segment.getTagName().equals(MaryXML.PHONE))
				return null;
			Element syllable = (Element) segment.getParentNode();
			if (syllable != null) {
				assert syllable.getTagName().equals(MaryXML.SYLLABLE) : "Unexpected tag name: expected " + MaryXML.SYLLABLE
						+ ", got " + syllable.getTagName();
			}
			return syllable;
		}
	}

	/**
	 * Retrieve the syllable before the syllable belonging to this target.
	 * 
	 * @author Marc Schr&ouml;der
	 *
	 */
	public static class PrevSyllableNavigator implements TargetElementNavigator {
		public Element getElement(Target target) {
			Element segment = target.getMaryxmlElement();
			if (segment == null)
				return null;
			Element current;
			if (segment.getTagName().equals(MaryXML.PHONE)) {
				Element syllable = (Element) segment.getParentNode();
				if (syllable == null)
					return null;
				current = syllable;
			} else { // boundary
				current = segment;
			}
			Element sentence = (Element) MaryDomUtils.getAncestor(segment, MaryXML.SENTENCE);
			if (sentence == null)
				return null;
			TreeWalker tw = MaryDomUtils.createTreeWalker(sentence, MaryXML.SYLLABLE);
			tw.setCurrentNode(current);
			Element previous = (Element) tw.previousNode();
			if (previous != null) {
				assert previous.getTagName().equals(MaryXML.SYLLABLE) : "Unexpected tag name: expected " + MaryXML.SYLLABLE
						+ ", got " + previous.getTagName();
			}
			return previous;
		}
	}

	/**
	 * Retrieve the syllable two before the syllable belonging to this target.
	 * 
	 * @author Marc Schr&ouml;der
	 *
	 */
	public static class PrevPrevSyllableNavigator implements TargetElementNavigator {
		public Element getElement(Target target) {
			Element segment = target.getMaryxmlElement();
			if (segment == null)
				return null;
			Element current;
			if (segment.getTagName().equals(MaryXML.PHONE)) {
				Element syllable = (Element) segment.getParentNode();
				if (syllable == null)
					return null;
				current = syllable;
			} else { // boundary
				current = segment;
			}
			Element sentence = (Element) MaryDomUtils.getAncestor(segment, MaryXML.SENTENCE);
			if (sentence == null)
				return null;
			TreeWalker tw = MaryDomUtils.createTreeWalker(sentence, MaryXML.SYLLABLE);
			tw.setCurrentNode(current);
			Element previous = (Element) tw.previousNode();
			Element pp = (Element) tw.previousNode();
			if (pp != null) {
				assert pp.getTagName().equals(MaryXML.SYLLABLE) : "Unexpected tag name: expected " + MaryXML.SYLLABLE + ", got "
						+ pp.getTagName();
			}
			return pp;
		}
	}

	/**
	 * Retrieve the syllable following the syllable belonging to this target.
	 * 
	 * @author Marc Schr&ouml;der
	 *
	 */
	public static class NextSyllableNavigator implements TargetElementNavigator {
		public Element getElement(Target target) {
			Element segment = target.getMaryxmlElement();
			if (segment == null)
				return null;
			Element current;
			if (segment.getTagName().equals(MaryXML.PHONE)) {
				Element syllable = (Element) segment.getParentNode();
				if (syllable == null)
					return null;
				current = syllable;
			} else { // boundary
				current = segment;
			}
			Element sentence = (Element) MaryDomUtils.getAncestor(segment, MaryXML.SENTENCE);
			if (sentence == null)
				return null;
			TreeWalker tw = MaryDomUtils.createTreeWalker(sentence, MaryXML.SYLLABLE);
			tw.setCurrentNode(current);
			Element next = (Element) tw.nextNode();
			if (next != null) {
				assert next.getTagName().equals(MaryXML.SYLLABLE) : "Unexpected tag name: expected " + MaryXML.SYLLABLE
						+ ", got " + next.getTagName();
			}
			return next;
		}
	}

	/**
	 * Retrieve the syllable two after the syllable belonging to this target.
	 * 
	 * @author Marc Schr&ouml;der
	 *
	 */
	public static class NextNextSyllableNavigator implements TargetElementNavigator {
		public Element getElement(Target target) {
			Element segment = target.getMaryxmlElement();
			if (segment == null)
				return null;
			Element current;
			if (segment.getTagName().equals(MaryXML.PHONE)) {
				Element syllable = (Element) segment.getParentNode();
				if (syllable == null)
					return null;
				current = syllable;
			} else { // boundary
				current = segment;
			}
			Element sentence = (Element) MaryDomUtils.getAncestor(segment, MaryXML.SENTENCE);
			if (sentence == null)
				return null;
			TreeWalker tw = MaryDomUtils.createTreeWalker(sentence, MaryXML.SYLLABLE);
			tw.setCurrentNode(current);
			Element next = (Element) tw.nextNode();
			Element nn = (Element) tw.nextNode();
			if (nn != null) {
				assert nn.getTagName().equals(MaryXML.SYLLABLE) : "Unexpected tag name: expected " + MaryXML.SYLLABLE + ", got "
						+ nn.getTagName();
			}
			return nn;
		}
	}

	/**
	 * Retrieve the word belonging to this target.
	 * 
	 * @author Marc Schr&ouml;der
	 *
	 */
	public static class WordNavigator implements TargetElementNavigator {
		public Element getElement(Target target) {
			Element segment = target.getMaryxmlElement();
			if (segment == null)
				return null;
			Element word = (Element) MaryDomUtils.getAncestor(segment, MaryXML.TOKEN);
			if (word != null) {
				assert word.getTagName().equals(MaryXML.TOKEN) : "Unexpected tag name: expected " + MaryXML.TOKEN + ", got "
						+ word.getTagName();
			}
			return word;
		}
	}

	/** Last syllable in phrase. */
	public static class LastSyllableInPhraseNavigator implements TargetElementNavigator {
		public Element getElement(Target target) {
			Element segment = target.getMaryxmlElement();
			if (segment == null)
				return null;
			Element phrase = (Element) MaryDomUtils.getAncestor(segment, MaryXML.PHRASE);
			if (phrase == null)
				return null;
			TreeWalker tw = MaryDomUtils.createTreeWalker(phrase, MaryXML.SYLLABLE);
			Element last = (Element) tw.lastChild();
			if (last != null) {
				assert last.getTagName().equals(MaryXML.SYLLABLE) : "Unexpected tag name: expected " + MaryXML.SYLLABLE
						+ ", got " + last.getTagName();
			}
			return last;
		}
	}

	public static class NextWordNavigator implements TargetElementNavigator {
		public Element getElement(Target target) {
			Element segment = target.getMaryxmlElement();
			if (segment == null)
				return null;
			Element current;
			if (segment.getTagName().equals(MaryXML.PHONE)) {
				Element word = (Element) MaryDomUtils.getAncestor(segment, MaryXML.TOKEN);
				if (word == null)
					return null;
				current = word;
			} else { // boundary
				current = segment;
			}
			Element sentence = (Element) MaryDomUtils.getAncestor(segment, MaryXML.SENTENCE);
			if (sentence == null)
				return null;
			TreeWalker tw = MaryDomUtils.createTreeWalker(sentence, MaryXML.TOKEN);
			tw.setCurrentNode(current);
			// The next word is the next token with a "ph" attribute:
			Element nextWord = null;
			Element nextToken;
			while ((nextToken = (Element) tw.nextNode()) != null) {
				if (nextToken.hasAttribute("ph")) {
					nextWord = nextToken;
					break;
				}
			}
			if (nextWord != null) {
				assert nextWord.getTagName().equals(MaryXML.TOKEN) : "Unexpected tag name: expected " + MaryXML.TOKEN + ", got "
						+ nextWord.getTagName();
			}
			return nextWord;
		}
	}

	public static class PrevWordNavigator implements TargetElementNavigator {
		public Element getElement(Target target) {
			Element segment = target.getMaryxmlElement();
			if (segment == null)
				return null;
			Element current;
			if (segment.getTagName().equals(MaryXML.PHONE)) {
				Element word = (Element) MaryDomUtils.getAncestor(segment, MaryXML.TOKEN);
				if (word == null)
					return null;
				current = word;
			} else { // boundary
				current = segment;
			}
			Element sentence = (Element) MaryDomUtils.getAncestor(segment, MaryXML.SENTENCE);
			if (sentence == null)
				return null;
			TreeWalker tw = MaryDomUtils.createTreeWalker(sentence, MaryXML.TOKEN);
			tw.setCurrentNode(current);
			// The next word is the next token with a "ph" attribute:
			Element prevWord = null;
			Element prevToken;
			while ((prevToken = (Element) tw.previousNode()) != null) {
				if (prevToken.hasAttribute("ph")) {
					prevWord = prevToken;
					break;
				}
			}
			if (prevWord != null) {
				assert prevWord.getTagName().equals(MaryXML.TOKEN) : "Unexpected tag name: expected " + MaryXML.TOKEN + ", got "
						+ prevWord.getTagName();
			}
			return prevWord;
		}
	}

	public static class FirstSegmentNextWordNavigator implements TargetElementNavigator {
		public Element getElement(Target target) {
			Element segment = target.getMaryxmlElement();
			if (segment == null)
				return null;
			Element current;
			if (segment.getTagName().equals(MaryXML.PHONE)) {
				Element word = (Element) MaryDomUtils.getAncestor(segment, MaryXML.TOKEN);
				if (word == null)
					return null;
				current = word;
			} else { // boundary
				current = segment;
			}
			Element sentence = (Element) MaryDomUtils.getAncestor(segment, MaryXML.SENTENCE);
			if (sentence == null)
				return null;
			TreeWalker tw = MaryDomUtils.createTreeWalker(sentence, MaryXML.TOKEN);
			tw.setCurrentNode(current);
			// The next word is the next token with a "ph" attribute:
			Element nextWord = null;
			Element nextToken;
			while ((nextToken = (Element) tw.nextNode()) != null) {
				if (nextToken.hasAttribute("ph")) {
					nextWord = nextToken;
					break;
				}
			}
			if (nextWord == null) {
				return null;
			}
			assert nextWord.getTagName().equals(MaryXML.TOKEN) : "Unexpected tag name: expected " + MaryXML.TOKEN + ", got "
					+ nextWord.getTagName();
			TreeWalker sw = MaryDomUtils.createTreeWalker(nextWord, MaryXML.PHONE);
			Element first = (Element) sw.firstChild();
			if (first != null) {
				assert first.getTagName().equals(MaryXML.PHONE) : "Unexpected tag name: expected " + MaryXML.PHONE + ", got "
						+ first.getTagName();
			}
			return first;
		}
	}

	public static class LastWordInSentenceNavigator implements TargetElementNavigator {
		public Element getElement(Target target) {
			Element segment = target.getMaryxmlElement();
			if (segment == null)
				return null;
			Element sentence = (Element) MaryDomUtils.getAncestor(segment, MaryXML.SENTENCE);
			if (sentence == null)
				return null;
			TreeWalker tw = MaryDomUtils.createTreeWalker(sentence, MaryXML.TOKEN);
			Element lastWord = null;
			Element lastToken = (Element) tw.lastChild();
			// The last word is the lastToken which has a "ph" attribute:
			while (lastToken != null) {
				if (lastToken.hasAttribute("ph")) {
					lastWord = lastToken;
					break;
				}
				lastToken = (Element) tw.previousNode();
			}

			if (lastWord != null) {
				assert lastWord.getTagName().equals(MaryXML.TOKEN) : "Unexpected tag name: expected " + MaryXML.TOKEN + ", got "
						+ lastWord.getTagName();
			}
			return lastWord;
		}
	}

	// no instances
	protected MaryGenericFeatureProcessors() {
	}

	/**
	 * flite never returns an int more than 19 from a feature processor, we duplicate that behavior in the processors so that our
	 * tests will match. let's keep this number as a constant for better overview
	 */
	private static final int RAIL_LIMIT = 19;

	private static final String[] ZERO_TO_NINETEEN = new String[] { "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11",
			"12", "13", "14", "15", "16", "17", "18", "19" };

	/**
	 * Indicate whether a unit is an edge unit, which is never the case for a target.
	 */
	public static class Edge implements ByteValuedFeatureProcessor {
		public String getName() {
			return "edge";
		}

		public String[] getValues() {
			return new String[] { "0", "start", "end" };
		}

		/**
		 * This processor always returns 0 for targets.
		 */
		public byte process(Target target) {
			return (byte) 0;
		}

	}

	/**
	 * Is the given half phone target a left or a right half?
	 * 
	 * @author Marc Schr&ouml;der
	 *
	 */
	public static class HalfPhoneLeftRight implements ByteValuedFeatureProcessor {
		protected ByteStringTranslator values;

		/**
		 * Initialise a HalfPhoneLeftRight feature processor.
		 */
		public HalfPhoneLeftRight() {
			this.values = new ByteStringTranslator(new String[] { "0", "L", "R" });
		}

		public String getName() {
			return "halfphone_lr";
		}

		public String[] getValues() {
			return values.getStringValues();
		}

		public byte process(Target target) {
			if (!(target instanceof HalfPhoneTarget))
				return 0;
			HalfPhoneTarget hpTarget = (HalfPhoneTarget) target;
			String value = (hpTarget.isLeftHalf() ? "L" : "R");
			return values.get(value);
		}
	}

	/**
	 * Sentence Style for the given target
	 * 
	 * @author Sathish Chandra Pammi
	 *
	 */

	public static class Style implements ByteValuedFeatureProcessor {
		protected ByteStringTranslator values;
		protected TargetElementNavigator navigator;
		protected final String styleTagName = "style";

		/**
		 * Initialize a speaking Style feature processor.
		 */
		public Style() {
			this.values = new ByteStringTranslator(new String[] { "0", "neutral", "poker", "happy", "sad", "angry", "excited" });
			this.navigator = new SegmentNavigator();
		}

		public String getName() {
			return styleTagName;
		}

		public String[] getValues() {
			return values.getStringValues();
		}

		public byte process(Target target) {
			String style = null;
			Element segment = target.getMaryxmlElement();
			if (segment != null) {
				Element prosody = (Element) MaryDomUtils.getClosestAncestorWithAttribute(segment, MaryXML.PROSODY, styleTagName);
				if (prosody != null) {
					style = prosody.getAttribute(styleTagName);
				}
			}
			if (style == null || style.equals(""))
				style = "0";
			if (values.contains(style)) {
				return values.get(style);
			} else { // silently ignore unknown values
				return 0;
			}
		}
	}

	/**
	 * Checks to see if the given syllable is accented.
	 */
	public static class Accented implements ByteValuedFeatureProcessor {
		protected String name;
		protected TargetElementNavigator navigator;

		public Accented(String name, TargetElementNavigator syllableNavigator) {
			this.name = name;
			this.navigator = syllableNavigator;
		}

		public String getName() {
			return name;
		}

		public String[] getValues() {
			return new String[] { "0", "1" };
		}

		/**
		 * Performs some processing on the given item.
		 * 
		 * @param target
		 *            the target to process
		 * @return "1" if the syllable is accented; otherwise "0"
		 */
		public byte process(Target target) {
			Element syllable = navigator.getElement(target);
			if (syllable != null && syllable.hasAttribute("accent")) {
				return (byte) 1;
			} else {
				return (byte) 0;
			}
		}
	}

	/**
	 * Checks to see if the given syllable is stressed.
	 */
	public static class Stressed implements ByteValuedFeatureProcessor {
		protected String name;
		protected TargetElementNavigator navigator;

		public Stressed(String name, TargetElementNavigator syllableNavigator) {
			this.name = name;
			this.navigator = syllableNavigator;
		}

		public String getName() {
			return name;
		}

		public String[] getValues() {
			return new String[] { "0", "1" };
		}

		/**
		 * Performs some processing on the given item.
		 * 
		 * @param target
		 *            the target to process
		 * @return "1" if the syllable is stressed; otherwise "0"
		 */
		public byte process(Target target) {
			Element syllable = navigator.getElement(target);
			if (syllable == null)
				return 0;
			String value = syllable.getAttribute("stress");
			if (value.equals(""))
				return 0;
			byte stressValue = Byte.parseByte(value);
			if (stressValue > 1) {
				// out of range, set to 1
				stressValue = 1;
			}
			return stressValue;
		}
	}

	/**
	 * Syllable tone for the given target
	 * 
	 * @author sathish pammi
	 *
	 */
	public static class SyllableTone implements ByteValuedFeatureProcessor {
		protected String name;
		protected TargetElementNavigator navigator;

		public SyllableTone(String name, TargetElementNavigator syllableNavigator) {
			this.name = name;
			this.navigator = syllableNavigator;
		}

		public String getName() {
			return name;
		}

		public String[] getValues() {
			return new String[] { "0", "1", "2", "3", "4" };
		}

		/**
		 * Performs some processing on the given item.
		 * 
		 * @param target
		 *            the target to process
		 * @return tone value
		 */
		public byte process(Target target) {
			Element syllable = navigator.getElement(target);
			if (syllable == null)
				return 0;
			String value = syllable.getAttribute("tone");
			if (value.equals(""))
				return 0;
			byte toneValue = Byte.parseByte(value);
			if (toneValue > 4 || toneValue < 1) {
				// out of range, set to 0
				toneValue = 0;
			}
			return toneValue;
		}
	}

	/**
	 * Returns as a byte the number of phrases in the current sentence.
	 */
	public static class SentenceNumPhrases implements ByteValuedFeatureProcessor {
		public SentenceNumPhrases() {
		}

		public String getName() {
			return "sentence_numphrases";
		}

		public String[] getValues() {
			return ZERO_TO_NINETEEN;
		}

		/**
		 * 
		 * @return the number of phrases in the sentence
		 */
		public byte process(Target target) {
			Element segment = target.getMaryxmlElement();
			if (segment == null)
				return (byte) 0;
			Element sentence = (Element) MaryDomUtils.getAncestor(segment, MaryXML.SENTENCE);
			if (sentence == null)
				return (byte) 0;
			TreeWalker tw = MaryDomUtils.createTreeWalker(sentence, MaryXML.PHRASE);
			int count = 0;
			Element e;
			while ((e = (Element) tw.nextNode()) != null && count < RAIL_LIMIT) {
				count++;
			}
			return (byte) count;
		}
	}

	/**
	 * Returns as an Integer the number of words in the current sentence. This is a feature processor. A feature processor takes
	 * an item, performs some sort of processing on the item and returns an object.
	 */
	public static class SentenceNumWords implements ByteValuedFeatureProcessor {
		public SentenceNumWords() {
		}

		public String getName() {
			return "sentence_numwords";
		}

		public String[] getValues() {
			return ZERO_TO_NINETEEN;
		}

		/**
		 * @return the number of words in the sentence
		 */
		public byte process(Target target) {
			Element segment = target.getMaryxmlElement();
			if (segment == null)
				return (byte) 0;
			Element sentence = (Element) MaryDomUtils.getAncestor(segment, MaryXML.SENTENCE);
			if (sentence == null)
				return (byte) 0;
			TreeWalker tw = MaryDomUtils.createTreeWalker(sentence, MaryXML.TOKEN);
			int count = 0;
			Element e;
			while ((e = (Element) tw.nextNode()) != null && count < RAIL_LIMIT) {
				// only tokens with a "ph" attribute count as words:
				if (e.hasAttribute("ph"))
					count++;
			}
			return (byte) count;
		}
	}

	/**
	 * Returns as a byte the number of phrases in the current sentence.
	 */
	public static class PhraseNumSyls implements ByteValuedFeatureProcessor {
		public PhraseNumSyls() {
		}

		public String getName() {
			return "phrase_numsyls";
		}

		public String[] getValues() {
			return ZERO_TO_NINETEEN;
		}

		/**
		 * @return the number of words in the phrase
		 */
		public byte process(Target target) {
			Element segment = target.getMaryxmlElement();
			if (segment == null)
				return (byte) 0;
			Element phrase = (Element) MaryDomUtils.getAncestor(segment, MaryXML.PHRASE);
			if (phrase == null)
				return (byte) 0;
			TreeWalker tw = MaryDomUtils.createTreeWalker(phrase, MaryXML.SYLLABLE);
			int count = 0;
			Element e;
			while ((e = (Element) tw.nextNode()) != null && count < RAIL_LIMIT) {
				count++;
			}
			return (byte) count;
		}
	}

	/**
	 * Returns as a byte the number of words in the current phrase.
	 */
	public static class PhraseNumWords implements ByteValuedFeatureProcessor {
		public PhraseNumWords() {
		}

		public String getName() {
			return "phrase_numwords";
		}

		public String[] getValues() {
			return ZERO_TO_NINETEEN;
		}

		/**
		 * Performs some processing on the given item.
		 * 
		 * @param target
		 *            the item to process
		 * 
		 * @return the number of words in the phrase
		 */
		public byte process(Target target) {
			Element segment = target.getMaryxmlElement();
			if (segment == null)
				return (byte) 0;
			Element phrase = (Element) MaryDomUtils.getAncestor(segment, MaryXML.PHRASE);
			if (phrase == null)
				return (byte) 0;
			TreeWalker tw = MaryDomUtils.createTreeWalker(phrase, MaryXML.TOKEN);
			int count = 0;
			Element e;
			while ((e = (Element) tw.nextNode()) != null && count < RAIL_LIMIT) {
				count++;
			}
			return (byte) count;
		}
	}

	/**
	 * Returns as an Integer the number of syllables in the given word. This is a feature processor. A feature processor takes an
	 * item, performs some sort of processing on the item and returns an object.
	 */
	public static class WordNumSyls implements ByteValuedFeatureProcessor {
		public WordNumSyls() {
		}

		public String getName() {
			return "word_numsyls";
		}

		public String[] getValues() {
			return ZERO_TO_NINETEEN;
		}

		/**
		 * @return the number of syllables in the given word
		 */
		public byte process(Target target) {
			Element segment = target.getMaryxmlElement();
			if (segment == null)
				return (byte) 0;
			Element word = (Element) MaryDomUtils.getAncestor(segment, MaryXML.TOKEN);
			if (word == null)
				return (byte) 0;
			TreeWalker tw = MaryDomUtils.createTreeWalker(word, MaryXML.SYLLABLE);
			int count = 0;
			Element e;
			while ((e = (Element) tw.nextNode()) != null && count < RAIL_LIMIT) {
				count++;
			}
			return (byte) count;
		}
	}

	/**
	 * Returns as a byte the number of segments in the given word.
	 */
	public static class WordNumSegs implements ByteValuedFeatureProcessor {
		public WordNumSegs() {
		}

		public String getName() {
			return "word_numsegs";
		}

		public String[] getValues() {
			return ZERO_TO_NINETEEN;
		}

		/**
		 * @return the number of segments in the given word
		 */
		public byte process(Target target) {
			Element segment = target.getMaryxmlElement();
			if (segment == null)
				return (byte) 0;
			Element word = (Element) MaryDomUtils.getAncestor(segment, MaryXML.TOKEN);
			if (word == null)
				return (byte) 0;
			TreeWalker tw = MaryDomUtils.createTreeWalker(word, MaryXML.PHONE);
			int count = 0;
			Element e;
			while ((e = (Element) tw.nextNode()) != null && count < RAIL_LIMIT) {
				count++;
			}
			return (byte) count;
		}
	}

	/**
	 * Returns as an Integer the number of segments in the current syllable. This is a feature processor. A feature processor
	 * takes an item, performs some sort of processing on the item and returns an object.
	 */
	public static class SylNumSegs implements ByteValuedFeatureProcessor {
		public SylNumSegs() {
		}

		public String getName() {
			return "syl_numsegs";
		}

		public String[] getValues() {
			return ZERO_TO_NINETEEN;
		}

		/**
		 * Performs some processing on the given item.
		 * 
		 * @param target
		 *            the item to process
		 * 
		 * @return the number of segments in the given word
		 */
		public byte process(Target target) {
			Element segment = target.getMaryxmlElement();
			if (segment == null)
				return (byte) 0;
			if (!segment.getTagName().equals(MaryXML.PHONE))
				return 0;
			Element syllable = (Element) segment.getParentNode();
			if (syllable == null)
				return (byte) 0;
			TreeWalker tw = MaryDomUtils.createTreeWalker(syllable, MaryXML.PHONE);
			int count = 0;
			Element e;
			while ((e = (Element) tw.nextNode()) != null && count < RAIL_LIMIT) {
				count++;
			}
			return (byte) count;
		}
	}

	/**
	 * @deprecated use SegsFromSylStart instead
	 */
	public static class PosInSyl extends SegsFromSylStart {
		public PosInSyl() {
			super();
		}

		public String getName() {
			return "pos_in_syl";
		}
	}

	/**
	 * Finds the position of the phone in the syllable.
	 */
	public static class SegsFromSylStart implements ByteValuedFeatureProcessor {
		public SegsFromSylStart() {
		}

		public String getName() {
			return "segs_from_syl_start";
		}

		public String[] getValues() {
			return ZERO_TO_NINETEEN;
		}

		/**
		 * @param target
		 *            the target to process
		 * @return the position of the phone in the syllable
		 */
		public byte process(Target target) {
			Element segment = target.getMaryxmlElement();
			if (segment == null)
				return (byte) 0;
			if (!segment.getTagName().equals(MaryXML.PHONE))
				return 0;
			int count = 0;
			Element e = segment;
			while ((e = MaryDomUtils.getPreviousSiblingElement(e)) != null && count < RAIL_LIMIT) {
				count++;
			}
			return (byte) count;
		}
	}

	/**
	 * Finds the position of the phone from the end of the syllable.
	 */
	public static class SegsFromSylEnd implements ByteValuedFeatureProcessor {
		public SegsFromSylEnd() {
		}

		public String getName() {
			return "segs_from_syl_end";
		}

		public String[] getValues() {
			return ZERO_TO_NINETEEN;
		}

		/**
		 * @param target
		 *            the target to process
		 * @return the position of the phone in the syllable
		 */
		public byte process(Target target) {
			Element segment = target.getMaryxmlElement();
			if (segment == null)
				return (byte) 0;
			if (!segment.getTagName().equals(MaryXML.PHONE))
				return 0;
			int count = 0;
			Element e = segment;
			while ((e = MaryDomUtils.getNextSiblingElement(e)) != null && count < RAIL_LIMIT) {
				count++;
			}
			return (byte) count;
		}
	}

	/**
	 * Finds the position of the segment from the start of the word.
	 */
	public static class SegsFromWordStart implements ByteValuedFeatureProcessor {
		public SegsFromWordStart() {
		}

		public String getName() {
			return "segs_from_word_start";
		}

		public String[] getValues() {
			return ZERO_TO_NINETEEN;
		}

		/**
		 * @param target
		 *            the target to process
		 * @return the position of the phone in the syllable
		 */
		public byte process(Target target) {
			Element segment = target.getMaryxmlElement();
			if (segment == null)
				return (byte) 0;
			Element word = (Element) MaryDomUtils.getAncestor(segment, MaryXML.TOKEN);
			if (word == null)
				return (byte) 0;
			TreeWalker tw = MaryDomUtils.createTreeWalker(word, MaryXML.PHONE);
			tw.setCurrentNode(segment);
			int count = 0;
			Element e;
			while ((e = (Element) tw.previousNode()) != null && count < RAIL_LIMIT) {
				count++;
			}
			return (byte) count;
		}
	}

	/**
	 * Finds the position of the segment from the end of the word.
	 */
	public static class SegsFromWordEnd implements ByteValuedFeatureProcessor {
		public SegsFromWordEnd() {
		}

		public String getName() {
			return "segs_from_word_end";
		}

		public String[] getValues() {
			return ZERO_TO_NINETEEN;
		}

		/**
		 * Performs some processing on the given item.
		 * 
		 * @param target
		 *            the target to process
		 * @return the position of the phone in the syllable
		 */
		public byte process(Target target) {
			Element segment = target.getMaryxmlElement();
			if (segment == null)
				return (byte) 0;
			Element word = (Element) MaryDomUtils.getAncestor(segment, MaryXML.TOKEN);
			if (word == null)
				return (byte) 0;
			TreeWalker tw = MaryDomUtils.createTreeWalker(word, MaryXML.PHONE);
			tw.setCurrentNode(segment);
			int count = 0;
			Element e;
			while ((e = (Element) tw.nextNode()) != null && count < RAIL_LIMIT) {
				count++;
			}
			return (byte) count;
		}
	}

	/**
	 * Finds the position of the syllable from the start of the word.
	 */
	public static class SylsFromWordStart implements ByteValuedFeatureProcessor {
		public SylsFromWordStart() {
		}

		public String getName() {
			return "syls_from_word_start";
		}

		public String[] getValues() {
			return ZERO_TO_NINETEEN;
		}

		/**
		 * @param target
		 *            the target to process
		 * @return the position of the syllable in the word
		 */
		public byte process(Target target) {
			Element segment = target.getMaryxmlElement();
			if (segment == null)
				return (byte) 0;
			if (!segment.getTagName().equals(MaryXML.PHONE))
				return 0;
			Element syllable = (Element) segment.getParentNode();
			if (syllable == null)
				return (byte) 0;
			int count = 0;
			Element e = syllable;
			while ((e = MaryDomUtils.getPreviousSiblingElement(e)) != null && count < RAIL_LIMIT) {
				count++;
			}
			return (byte) count;
		}
	}

	/**
	 * Finds the position of the syllable from the end of the word.
	 */
	public static class SylsFromWordEnd implements ByteValuedFeatureProcessor {
		public SylsFromWordEnd() {
		}

		public String getName() {
			return "syls_from_word_end";
		}

		public String[] getValues() {
			return ZERO_TO_NINETEEN;
		}

		/**
		 * @param target
		 *            the target to process
		 * @return the position of the phone in the syllable
		 */
		public byte process(Target target) {
			Element segment = target.getMaryxmlElement();
			if (segment == null)
				return (byte) 0;
			if (!segment.getTagName().equals(MaryXML.PHONE))
				return 0;
			Element syllable = (Element) segment.getParentNode();
			if (syllable == null)
				return (byte) 0;
			int count = 0;
			Element e = syllable;
			while ((e = MaryDomUtils.getNextSiblingElement(e)) != null && count < RAIL_LIMIT) {
				count++;
			}
			return (byte) count;
		}
	}

	/**
	 * Determines the break level after this syllable.
	 */
	public static class SylBreak implements ByteValuedFeatureProcessor {
		protected String name;
		protected TargetElementNavigator navigator;

		public SylBreak(String name, TargetElementNavigator syllableNavigator) {
			this.name = name;
			this.navigator = syllableNavigator;
		}

		public String getName() {
			return name;
		}

		/**
		 * "4" for a big break, "3" for a break; "1" = word-final; "0" = within-word
		 */
		public String[] getValues() {
			return new String[] { "0", "1", "unused", "3", "4" };
		}

		/**
		 * @param target
		 *            the target to process
		 * @return the break level after the syllable returned by syllableNavigator
		 */
		public byte process(Target target) {
			Element syllable = navigator.getElement(target);
			if (syllable == null)
				return 0;
			// is there another syllable following in the token?
			if (MaryDomUtils.getNextSiblingElement(syllable) != null)
				return 0;
			// else, it is at least word-final.
			Element word = (Element) syllable.getParentNode();
			if (word == null)
				return 0;
			assert word.getTagName().equals(MaryXML.TOKEN) : "Unexpected tag name: expected " + MaryXML.TOKEN + ", got "
					+ word.getTagName();
			Element sentence = (Element) MaryDomUtils.getAncestor(word, MaryXML.SENTENCE);
			if (sentence == null)
				return 0;
			TreeWalker tw = MaryDomUtils.createTreeWalker(sentence, MaryXML.TOKEN, MaryXML.BOUNDARY);
			tw.setCurrentNode(word);
			// The next word is the next token with a "ph" attribute:
			Element e;
			while ((e = (Element) tw.nextNode()) != null) {
				if (e.getTagName().equals(MaryXML.BOUNDARY) || e.getTagName().equals(MaryXML.TOKEN) && e.hasAttribute("ph"))
					break;
			}
			if (e == null) {
				// we are the last token in the sentence, but there is no boundary...
				// OK, let's say it is sentence-final anyway:
				return 4;
			}
			if (e.getTagName().equals(MaryXML.TOKEN)) {
				// a word follows
				return 1;
			}
			assert e.getTagName().equals(MaryXML.BOUNDARY) : "Unexpected tag name: expected " + MaryXML.BOUNDARY + ", got "
					+ e.getTagName();
			String bi = e.getAttribute("breakindex");
			if (bi.equals("")) {
				// no breakindex
				return 1;
			}
			try {
				int ibi = Integer.parseInt(bi);
				if (ibi >= 4)
					return 4;
				if (ibi == 3)
					return 3;
			} catch (NumberFormatException nfe) {
			}
			// default: a word boundary
			return 1;
		}
	}

	/**
	 * Classifies the the syllable as single, initial, mid or final.
	 */
	public static class PositionType implements ByteValuedFeatureProcessor {
		protected TargetElementNavigator navigator;
		protected ByteStringTranslator values;

		public PositionType() {
			values = new ByteStringTranslator(new String[] { "0", "single", "final", "initial", "mid" });
			navigator = new SyllableNavigator();
		}

		public String getName() {
			return "position_type";
		}

		public String[] getValues() {
			return values.getStringValues();
		}

		/**
		 * @param target
		 *            the target to process
		 * @return classifies the syllable as "single", "final", "initial" or "mid"
		 */
		public byte process(Target target) {
			Element syllable = navigator.getElement(target);
			if (syllable == null)
				return 0;
			String type;
			if (MaryDomUtils.getNextSiblingElement(syllable) == null) {
				if (MaryDomUtils.getPreviousSiblingElement(syllable) == null) {
					type = "single";
				} else {
					type = "final";
				}
			} else if (MaryDomUtils.getPreviousSiblingElement(syllable) == null) {
				type = "initial";
			} else {
				type = "mid";
			}
			return values.get(type);
		}
	}

	/**
	 * Checks if segment is a pause.
	 */
	public static class IsPause implements ByteValuedFeatureProcessor {
		protected TargetElementNavigator navigator;
		protected String name;

		public IsPause(String name, TargetElementNavigator segmentNavigator) {
			this.name = name;
			this.navigator = segmentNavigator;
		}

		public String getName() {
			return name;
		}

		public String[] getValues() {
			return new String[] { "0", "1" };
		}

		/**
		 * Check if segment is a pause
		 * 
		 * @param target
		 *            the target to process
		 * @return 0 if false, 1 if true
		 */
		public byte process(Target target) {
			Element seg = navigator.getElement(target);
			if (seg == null)
				return 0;
			if (seg.getTagName().equals(MaryXML.BOUNDARY))
				return 1;
			return 0;
		}
	}

	public static class BreakIndex implements ByteValuedFeatureProcessor {
		protected ByteStringTranslator values;

		public BreakIndex() {
			values = new ByteStringTranslator(new String[] { "0", "1", "2", "3", "4", "5", "6" });
		}

		public String getName() {
			return "breakindex";
		}

		public String[] getValues() {
			return values.getStringValues();
		}

		public byte process(Target target) {
			Element segment = target.getMaryxmlElement();
			if (segment == null)
				return 0;
			Element word = (Element) MaryDomUtils.getAncestor(segment, MaryXML.TOKEN);
			if (word == null)
				return 0;
			// is there another segment following in the token?
			TreeWalker tww = MaryDomUtils.createTreeWalker(word, MaryXML.PHONE);
			tww.setCurrentNode(segment);
			if (tww.nextNode() != null)
				return 0;
			// else, it is at least word-final.
			Element sentence = (Element) MaryDomUtils.getAncestor(word, MaryXML.SENTENCE);
			if (sentence == null)
				return 0;
			TreeWalker tw = MaryDomUtils.createTreeWalker(sentence, MaryXML.TOKEN, MaryXML.BOUNDARY);
			tw.setCurrentNode(word);
			// The next word is the next token with a "ph" attribute:
			Element e;
			while ((e = (Element) tw.nextNode()) != null) {
				if (e.getTagName().equals(MaryXML.BOUNDARY) || e.getTagName().equals(MaryXML.TOKEN) && e.hasAttribute("ph"))
					break;
			}
			if (e == null) {
				// we are the last token in the sentence, but there is no boundary...
				// OK, let's say it is sentence-final anyway:
				return 4;
			}
			if (e.getTagName().equals(MaryXML.TOKEN)) {
				// a word follows
				return 1;
			}
			assert e.getTagName().equals(MaryXML.BOUNDARY) : "Unexpected tag name: expected " + MaryXML.BOUNDARY + ", got "
					+ e.getTagName();
			String bi = e.getAttribute("breakindex");
			if (bi.equals("")) {
				// no breakindex
				return 1;
			}
			try {
				int ibi = Integer.parseInt(bi);
				if (ibi > 6)
					ibi = 6;
				if (ibi < 2)
					ibi = 2;
				return (byte) ibi;
			} catch (NumberFormatException nfe) {
			}
			// default: a word boundary
			return 1;
		}
	}

	/**
	 * The ToBI accent of the current syllable.
	 */
	public static class TobiAccent implements ByteValuedFeatureProcessor {
		protected String name;
		protected TargetElementNavigator navigator;
		protected ByteStringTranslator values;

		public TobiAccent(String name, TargetElementNavigator syllableNavigator) {
			this.name = name;
			this.navigator = syllableNavigator;
			this.values = new ByteStringTranslator(new String[] { "0", "*", "H*", "!H*", "^H*", "L*", "L+H*", "L*+H", "L+!H*",
					"L*+!H", "L+^H*", "L*+^H", "H+L*", "H+!H*", "H+^H*", "!H+!H*", "^H+!H*", "^H+^H*", "H*+L", "!H*+L" });
		}

		public String getName() {
			return name;
		}

		public String[] getValues() {
			return values.getStringValues();
		}

		/**
		 * For the given syllable item, return its tobi accent, or 0 if there is none.
		 */
		public byte process(Target target) {
			Element syllable = navigator.getElement(target);
			if (syllable == null)
				return 0;
			String accent = syllable.getAttribute("accent");
			if (accent.equals("")) {
				return 0;
			}
			return values.get(accent);
		}
	}

	/**
	 * The ToBI endtone associated with the current syllable.
	 */
	public static class TobiEndtone implements ByteValuedFeatureProcessor {
		protected String name;
		protected TargetElementNavigator navigator;
		protected ByteStringTranslator values;

		public TobiEndtone(String name, TargetElementNavigator syllableNavigator) {
			this.name = name;
			this.navigator = syllableNavigator;
			this.values = new ByteStringTranslator(new String[] { "0", "H-", "!H-", "L-", "H-%", "!H-%", "H-^H%", "!H-^H%",
					"L-H%", "L-%", "L-L%", "H-H%", "H-L%" });
		}

		public String getName() {
			return name;
		}

		public String[] getValues() {
			return values.getStringValues();
		}

		/**
		 * For the given syllable item, return its tobi end tone, or 0 if there is none.
		 */
		public byte process(Target target) {
			Element syllable = navigator.getElement(target);
			if (syllable == null)
				return 0;
			Element sentence = (Element) MaryDomUtils.getAncestor(syllable, MaryXML.SENTENCE);
			if (sentence == null)
				return 0;
			TreeWalker tw = MaryDomUtils.createTreeWalker(sentence, MaryXML.SYLLABLE, MaryXML.BOUNDARY);
			tw.setCurrentNode(syllable);
			Element e = (Element) tw.nextNode();
			if (e == null)
				return 0;
			if (e.getTagName().equals(MaryXML.SYLLABLE))
				return 0;
			assert e.getTagName().equals(MaryXML.BOUNDARY) : "Unexpected tag name: expected " + MaryXML.BOUNDARY + ", got "
					+ e.getTagName();
			String endtone = e.getAttribute("tone");
			if (endtone.equals("")) {
				return 0;
			}
			return values.get(endtone);
		}
	}

	/**
	 * The next ToBI accent following the current syllable in the current phrase.
	 */
	public static class NextAccent extends TobiAccent {
		public NextAccent() {
			super("next_accent", null);
		}

		/**
		 * Search for an accented syllable, and return its tobi accent, or 0 if there is none.
		 */
		public byte process(Target target) {
			Element segment = target.getMaryxmlElement();
			if (segment == null)
				return 0;
			Element current;
			if (segment.getTagName().equals(MaryXML.PHONE)) {
				Element syllable = (Element) segment.getParentNode();
				if (syllable == null)
					return 0;
				current = syllable;
			} else { // boundary
				current = segment;
			}
			Element phrase = (Element) MaryDomUtils.getAncestor(current, MaryXML.PHRASE);
			if (phrase == null)
				return 0;
			TreeWalker tw = MaryDomUtils.createTreeWalker(phrase, MaryXML.SYLLABLE);
			tw.setCurrentNode(current);
			Element s;
			while ((s = (Element) tw.nextNode()) != null) {
				if (s.hasAttribute("accent")) {
					String accent = s.getAttribute("accent");
					return values.get(accent);
				}
			}
			return 0;
		}
	}

	/**
	 * The previous ToBI accent preceding the current syllable in the current phrase.
	 */
	public static class PrevAccent extends TobiAccent {
		public PrevAccent() {
			super("prev_accent", null);
		}

		/**
		 * Search for an accented syllable, and return its tobi accent, or 0 if there is none.
		 */
		public byte process(Target target) {
			Element segment = target.getMaryxmlElement();
			if (segment == null)
				return 0;
			Element current;
			if (segment.getTagName().equals(MaryXML.PHONE)) {
				Element syllable = (Element) segment.getParentNode();
				if (syllable == null)
					return 0;
				current = syllable;
			} else { // boundary
				current = segment;
			}
			Element phrase = (Element) MaryDomUtils.getAncestor(current, MaryXML.PHRASE);
			if (phrase == null)
				return 0;
			TreeWalker tw = MaryDomUtils.createTreeWalker(phrase, MaryXML.SYLLABLE);
			tw.setCurrentNode(current);
			Element s;
			while ((s = (Element) tw.previousNode()) != null) {
				if (s.hasAttribute("accent")) {
					String accent = s.getAttribute("accent");
					return values.get(accent);
				}
			}
			return 0;
		}
	}

	/**
	 * The ToBI endtone associated with the last syllable of the current phrase.
	 */
	public static class PhraseEndtone extends TobiEndtone {
		public PhraseEndtone() {
			super("phrase_endtone", new LastSyllableInPhraseNavigator());
		}
	}

	/**
	 * The ToBI endtone associated with the last syllable of the previous phrase.
	 */
	public static class PrevPhraseEndtone extends TobiEndtone {
		public PrevPhraseEndtone() {
			super("prev_phrase_endtone", null);
		}

		public byte process(Target target) {
			Element segment = target.getMaryxmlElement();
			if (segment == null)
				return 0;
			Element phrase = (Element) MaryDomUtils.getAncestor(segment, MaryXML.PHRASE);
			if (phrase == null)
				return 0;
			Document doc = phrase.getOwnerDocument();
			TreeWalker tw = MaryDomUtils.createTreeWalker(doc, doc, MaryXML.BOUNDARY);
			tw.setCurrentNode(phrase);
			Element boundary = (Element) tw.previousNode();
			if (boundary == null)
				return 0;
			String endtone = boundary.getAttribute("tone");
			if (endtone.equals("")) {
				return 0;
			}
			return values.get(endtone);
		}
	}

	/**
	 * Counts the number of syllables since the start of the phrase.
	 */
	public static class SylsFromPhraseStart implements ByteValuedFeatureProcessor {
		public SylsFromPhraseStart() {
		}

		public String getName() {
			return "syls_from_phrase_start";
		}

		public String[] getValues() {
			return ZERO_TO_NINETEEN;
		}

		/**
		 * @param target
		 *            the target to process
		 * @return the number of syllables since the last major break
		 */
		public byte process(Target target) {
			Element segment = target.getMaryxmlElement();
			if (segment == null)
				return 0;
			Element phrase = (Element) MaryDomUtils.getAncestor(segment, MaryXML.PHRASE);
			if (phrase == null)
				return 0;
			int count = 0;
			TreeWalker tw = MaryDomUtils.createTreeWalker(phrase, MaryXML.SYLLABLE);
			Element syllable = (Element) MaryDomUtils.getAncestor(segment, MaryXML.SYLLABLE);
			if (syllable != null) {
				tw.setCurrentNode(syllable);
			} else {
				tw.setCurrentNode(segment);
			}
			Element e;
			while ((e = (Element) tw.previousNode()) != null && count < RAIL_LIMIT) {
				count++;
			}
			return (byte) count;
		}
	}

	/**
	 * Counts the number of syllables until the end of the phrase.
	 */
	public static class SylsFromPhraseEnd implements ByteValuedFeatureProcessor {
		public SylsFromPhraseEnd() {
		}

		public String getName() {
			return "syls_from_phrase_end";
		}

		public String[] getValues() {
			return ZERO_TO_NINETEEN;
		}

		/**
		 * @param target
		 *            the target to process
		 * @return the number of accented syllables since the last major break
		 */
		public byte process(Target target) {
			Element segment = target.getMaryxmlElement();
			if (segment == null)
				return 0;
			Element phrase = (Element) MaryDomUtils.getAncestor(segment, MaryXML.PHRASE);
			if (phrase == null)
				return 0;
			int count = 0;
			TreeWalker tw = MaryDomUtils.createTreeWalker(phrase, MaryXML.SYLLABLE);
			tw.setCurrentNode(segment);
			Element e;
			while ((e = (Element) tw.nextNode()) != null && count < RAIL_LIMIT) {
				count++;
			}
			return (byte) count;
		}
	}

	/**
	 * Counts the number of stressed syllables since the start of the phrase.
	 */
	public static class StressedSylsFromPhraseStart implements ByteValuedFeatureProcessor {
		public StressedSylsFromPhraseStart() {
		}

		public String getName() {
			return "stressed_syls_from_phrase_start";
		}

		public String[] getValues() {
			return ZERO_TO_NINETEEN;
		}

		/**
		 * @param target
		 *            the target to process
		 * @return the number of stressed syllables since the last major break
		 */
		public byte process(Target target) {
			Element segment = target.getMaryxmlElement();
			if (segment == null)
				return 0;
			Element phrase = (Element) MaryDomUtils.getAncestor(segment, MaryXML.PHRASE);
			if (phrase == null)
				return 0;
			int count = 0;
			TreeWalker tw = MaryDomUtils.createTreeWalker(phrase, MaryXML.SYLLABLE);
			Element syllable = (Element) MaryDomUtils.getAncestor(segment, MaryXML.SYLLABLE);
			if (syllable != null) {
				tw.setCurrentNode(syllable);
			} else {
				tw.setCurrentNode(segment);
			}
			Element e;
			while ((e = (Element) tw.previousNode()) != null && count < RAIL_LIMIT) {
				String stress = e.getAttribute("stress");
				if (stress.equals("1"))
					count++;
			}
			return (byte) count;
		}
	}

	/**
	 * Counts the number of stressed syllables until the end of the phrase.
	 */
	public static class StressedSylsFromPhraseEnd implements ByteValuedFeatureProcessor {
		public StressedSylsFromPhraseEnd() {
		}

		public String getName() {
			return "stressed_syls_from_phrase_end";
		}

		public String[] getValues() {
			return ZERO_TO_NINETEEN;
		}

		/**
		 * @param target
		 *            the target to process
		 * @return the number of stressed syllables since the last major break
		 */
		public byte process(Target target) {
			Element segment = target.getMaryxmlElement();
			if (segment == null)
				return 0;
			Element phrase = (Element) MaryDomUtils.getAncestor(segment, MaryXML.PHRASE);
			if (phrase == null)
				return 0;
			int count = 0;
			TreeWalker tw = MaryDomUtils.createTreeWalker(phrase, MaryXML.SYLLABLE);
			tw.setCurrentNode(segment);
			Element e;
			while ((e = (Element) tw.nextNode()) != null && count < RAIL_LIMIT) {
				String stress = e.getAttribute("stress");
				if (stress.equals("1"))
					count++;
			}
			return (byte) count;
		}
	}

	/**
	 * Counts the number of accented syllables since the start of the phrase.
	 */
	public static class AccentedSylsFromPhraseStart implements ByteValuedFeatureProcessor {
		public AccentedSylsFromPhraseStart() {
		}

		public String getName() {
			return "accented_syls_from_phrase_start";
		}

		public String[] getValues() {
			return ZERO_TO_NINETEEN;
		}

		/**
		 * @param target
		 *            the target to process
		 * @return the number of accented syllables since the last major break
		 */
		public byte process(Target target) {
			Element segment = target.getMaryxmlElement();
			if (segment == null)
				return 0;
			Element phrase = (Element) MaryDomUtils.getAncestor(segment, MaryXML.PHRASE);
			if (phrase == null)
				return 0;
			int count = 0;
			TreeWalker tw = MaryDomUtils.createTreeWalker(phrase, MaryXML.SYLLABLE);
			Element syllable = (Element) MaryDomUtils.getAncestor(segment, MaryXML.SYLLABLE);
			if (syllable != null) {
				tw.setCurrentNode(syllable);
			} else {
				tw.setCurrentNode(segment);
			}
			Element e;
			while ((e = (Element) tw.previousNode()) != null && count < RAIL_LIMIT) {
				String accent = e.getAttribute("accent");
				if (!accent.equals(""))
					count++;
			}
			return (byte) count;
		}
	}

	/**
	 * Counts the number of accented syllables until the end of the phrase.
	 */
	public static class AccentedSylsFromPhraseEnd implements ByteValuedFeatureProcessor {
		public AccentedSylsFromPhraseEnd() {
		}

		public String getName() {
			return "accented_syls_from_phrase_end";
		}

		public String[] getValues() {
			return ZERO_TO_NINETEEN;
		}

		/**
		 * @param target
		 *            the target to process
		 * @return the number of accented syllables since the last major break
		 */
		public byte process(Target target) {
			Element segment = target.getMaryxmlElement();
			if (segment == null)
				return 0;
			Element phrase = (Element) MaryDomUtils.getAncestor(segment, MaryXML.PHRASE);
			if (phrase == null)
				return 0;
			int count = 0;
			TreeWalker tw = MaryDomUtils.createTreeWalker(phrase, MaryXML.SYLLABLE);
			tw.setCurrentNode(segment);
			Element e;
			while ((e = (Element) tw.nextNode()) != null && count < RAIL_LIMIT) {
				String accent = e.getAttribute("accent");
				if (!accent.equals(""))
					count++;
			}
			return (byte) count;
		}
	}

	/**
	 * Counts the number of words since the start of the phrase.
	 */
	public static class WordsFromPhraseStart implements ByteValuedFeatureProcessor {
		public WordsFromPhraseStart() {
		}

		public String getName() {
			return "words_from_phrase_start";
		}

		public String[] getValues() {
			return ZERO_TO_NINETEEN;
		}

		/**
		 * @param target
		 *            the target to process
		 * @return the number of words since the last major break
		 */
		public byte process(Target target) {
			Element segment = target.getMaryxmlElement();
			if (segment == null)
				return 0;
			Element phrase = (Element) MaryDomUtils.getAncestor(segment, MaryXML.PHRASE);
			if (phrase == null)
				return 0;
			int count = 0;
			TreeWalker tw = MaryDomUtils.createTreeWalker(phrase, MaryXML.TOKEN);
			Element word = (Element) MaryDomUtils.getAncestor(segment, MaryXML.TOKEN);
			if (word != null) {
				tw.setCurrentNode(word);
			} else {
				tw.setCurrentNode(segment);
			}
			Element e;
			while ((e = (Element) tw.previousNode()) != null && count < RAIL_LIMIT) {
				// only count tokens that have a "ph" attribute:
				if (e.hasAttribute("ph"))
					count++;
			}
			return (byte) count;
		}
	}

	/**
	 * Counts the number of words until the end of the phrase.
	 */
	public static class WordsFromPhraseEnd implements ByteValuedFeatureProcessor {
		public WordsFromPhraseEnd() {
		}

		public String getName() {
			return "words_from_phrase_end";
		}

		public String[] getValues() {
			return ZERO_TO_NINETEEN;
		}

		/**
		 * @param target
		 *            the target to process
		 * @return the number of words until the next major break
		 */
		public byte process(Target target) {
			Element segment = target.getMaryxmlElement();
			if (segment == null)
				return 0;
			Element phrase = (Element) MaryDomUtils.getAncestor(segment, MaryXML.PHRASE);
			if (phrase == null)
				return 0;
			int count = 0;
			TreeWalker tw = MaryDomUtils.createTreeWalker(phrase, MaryXML.TOKEN);
			tw.setCurrentNode(segment);
			Element e;
			while ((e = (Element) tw.nextNode()) != null && count < RAIL_LIMIT) {
				// only count tokens that have a "ph" attribute
				if (e.hasAttribute("ph"))
					count++;
			}
			return (byte) count;
		}
	}

	/**
	 * Counts the number of words since the start of the sentence.
	 */
	public static class WordsFromSentenceStart implements ByteValuedFeatureProcessor {
		public WordsFromSentenceStart() {
		}

		public String getName() {
			return "words_from_sentence_start";
		}

		public String[] getValues() {
			return ZERO_TO_NINETEEN;
		}

		/**
		 * @param target
		 *            the target to process
		 * @return the number of words since the beginning of the sentence
		 */
		public byte process(Target target) {
			Element segment = target.getMaryxmlElement();
			if (segment == null)
				return 0;
			Element sentence = (Element) MaryDomUtils.getAncestor(segment, MaryXML.SENTENCE);
			if (sentence == null)
				return 0;
			int count = 0;
			TreeWalker tw = MaryDomUtils.createTreeWalker(sentence, MaryXML.TOKEN);
			Element word = (Element) MaryDomUtils.getAncestor(segment, MaryXML.TOKEN);
			if (word != null) {
				tw.setCurrentNode(word);
			} else {
				tw.setCurrentNode(segment);
			}
			Element e;
			while ((e = (Element) tw.previousNode()) != null && count < RAIL_LIMIT) {
				// only count tokens that have a "ph" attribute:
				if (e.hasAttribute("ph"))
					count++;
			}
			return (byte) count;
		}
	}

	/**
	 * Counts the number of words until the end of the sentence.
	 */
	public static class WordsFromSentenceEnd implements ByteValuedFeatureProcessor {
		public WordsFromSentenceEnd() {
		}

		public String getName() {
			return "words_from_sentence_end";
		}

		public String[] getValues() {
			return ZERO_TO_NINETEEN;
		}

		/**
		 * @param target
		 *            the target to process
		 * @return the number of words until the end of the sentence
		 */
		public byte process(Target target) {
			Element segment = target.getMaryxmlElement();
			if (segment == null)
				return 0;
			Element sentence = (Element) MaryDomUtils.getAncestor(segment, MaryXML.SENTENCE);
			if (sentence == null)
				return 0;
			int count = 0;
			TreeWalker tw = MaryDomUtils.createTreeWalker(sentence, MaryXML.TOKEN);
			tw.setCurrentNode(segment);
			Element e;
			while ((e = (Element) tw.nextNode()) != null && count < RAIL_LIMIT) {
				// only count tokens that have a "ph" attribute:
				if (e.hasAttribute("ph"))
					count++;
			}
			return (byte) count;
		}
	}

	/**
	 * Counts the number of phrases since the start of the sentence.
	 */
	public static class PhrasesFromSentenceStart implements ByteValuedFeatureProcessor {
		public PhrasesFromSentenceStart() {
		}

		public String getName() {
			return "phrases_from_sentence_start";
		}

		public String[] getValues() {
			return ZERO_TO_NINETEEN;
		}

		/**
		 * @param target
		 *            the target to process
		 * @return the number of phrases since the start of the sentence
		 */
		public byte process(Target target) {
			Element segment = target.getMaryxmlElement();
			if (segment == null)
				return 0;
			Element sentence = (Element) MaryDomUtils.getAncestor(segment, MaryXML.SENTENCE);
			if (sentence == null)
				return 0;
			int count = 0;
			TreeWalker tw = MaryDomUtils.createTreeWalker(sentence, MaryXML.PHRASE);
			Element phrase = (Element) MaryDomUtils.getAncestor(segment, MaryXML.PHRASE);
			if (phrase != null) {
				tw.setCurrentNode(phrase);
			} else {
				tw.setCurrentNode(segment);
			}
			Element e;
			while ((e = (Element) tw.previousNode()) != null && count < RAIL_LIMIT) {
				count++;
			}
			return (byte) count;
		}
	}

	/**
	 * Counts the number of phrases until the end of the sentence.
	 */
	public static class PhrasesFromSentenceEnd implements ByteValuedFeatureProcessor {
		public PhrasesFromSentenceEnd() {
		}

		public String getName() {
			return "phrases_from_sentence_end";
		}

		public String[] getValues() {
			return ZERO_TO_NINETEEN;
		}

		/**
		 * @param target
		 *            the target to process
		 * @return the number of phrases until the end of the sentence.
		 */
		public byte process(Target target) {
			Element segment = target.getMaryxmlElement();
			if (segment == null)
				return 0;
			Element sentence = (Element) MaryDomUtils.getAncestor(segment, MaryXML.SENTENCE);
			if (sentence == null)
				return 0;
			int count = 0;
			TreeWalker tw = MaryDomUtils.createTreeWalker(sentence, MaryXML.PHRASE);
			tw.setCurrentNode(segment);
			Element e;
			while ((e = (Element) tw.nextNode()) != null && count < RAIL_LIMIT) {
				count++;
			}
			return (byte) count;
		}
	}

	/**
	 * Counts the number of syllables since the last accent in the current phrase.
	 */
	public static class SylsFromPrevAccent implements ByteValuedFeatureProcessor {
		public SylsFromPrevAccent() {
		}

		public String getName() {
			return "syls_from_prev_accent";
		}

		public String[] getValues() {
			return ZERO_TO_NINETEEN;
		}

		/**
		 * @param target
		 *            the target to process
		 * @return the number of syllables since the last accent
		 */
		public byte process(Target target) {
			Element segment = target.getMaryxmlElement();
			if (segment == null)
				return 0;
			Element phrase = (Element) MaryDomUtils.getAncestor(segment, MaryXML.PHRASE);
			if (phrase == null)
				return 0;
			int count = 0;
			TreeWalker tw = MaryDomUtils.createTreeWalker(phrase, MaryXML.SYLLABLE);
			Element syllable = (Element) MaryDomUtils.getAncestor(segment, MaryXML.SYLLABLE);
			if (syllable != null) {
				tw.setCurrentNode(syllable);
			} else {
				tw.setCurrentNode(segment);
			}
			Element e;
			while ((e = (Element) tw.previousNode()) != null && count < RAIL_LIMIT) {
				count++;
				String accent = e.getAttribute("accent");
				if (!accent.equals(""))
					break;
			}
			return (byte) count;
		}
	}

	/**
	 * Counts the number of syllables until the next accent in the current phrase.
	 */
	public static class SylsToNextAccent implements ByteValuedFeatureProcessor {
		public SylsToNextAccent() {
		}

		public String getName() {
			return "syls_to_next_accent";
		}

		public String[] getValues() {
			return ZERO_TO_NINETEEN;
		}

		/**
		 * @param target
		 *            the target to process
		 * @return the number of syllables until the next accent
		 */
		public byte process(Target target) {
			Element segment = target.getMaryxmlElement();
			if (segment == null)
				return 0;
			Element phrase = (Element) MaryDomUtils.getAncestor(segment, MaryXML.PHRASE);
			if (phrase == null)
				return 0;
			int count = 0;
			TreeWalker tw = MaryDomUtils.createTreeWalker(phrase, MaryXML.SYLLABLE);
			tw.setCurrentNode(segment);
			Element e;
			while ((e = (Element) tw.nextNode()) != null && count < RAIL_LIMIT) {
				count++;
				String accent = e.getAttribute("accent");
				if (!accent.equals(""))
					break;
			}
			return (byte) count;
		}
	}

	/**
	 * Counts the number of syllables since the last stressed syllable in the current phrase.
	 */
	public static class SylsFromPrevStressed implements ByteValuedFeatureProcessor {
		public SylsFromPrevStressed() {
		}

		public String getName() {
			return "syls_from_prev_stressed";
		}

		public String[] getValues() {
			return ZERO_TO_NINETEEN;
		}

		/**
		 * @param target
		 *            the target to process
		 * @return the number of syllables since the last accent
		 */
		public byte process(Target target) {
			Element segment = target.getMaryxmlElement();
			if (segment == null)
				return 0;
			Element phrase = (Element) MaryDomUtils.getAncestor(segment, MaryXML.PHRASE);
			if (phrase == null)
				return 0;
			int count = 0;
			TreeWalker tw = MaryDomUtils.createTreeWalker(phrase, MaryXML.SYLLABLE);
			Element syllable = (Element) MaryDomUtils.getAncestor(segment, MaryXML.SYLLABLE);
			if (syllable != null) {
				tw.setCurrentNode(syllable);
			} else {
				tw.setCurrentNode(segment);
			}
			Element e;
			while ((e = (Element) tw.previousNode()) != null && count < RAIL_LIMIT) {
				count++;
				String stress = e.getAttribute("stress");
				if (stress.equals("1"))
					break;
			}
			return (byte) count;
		}
	}

	/**
	 * Counts the number of syllables until the next stressed syllable in the current phrase.
	 */
	public static class SylsToNextStressed implements ByteValuedFeatureProcessor {
		public SylsToNextStressed() {
		}

		public String getName() {
			return "syls_to_next_stressed";
		}

		public String[] getValues() {
			return ZERO_TO_NINETEEN;
		}

		/**
		 * @param target
		 *            the target to process
		 * @return the number of syllables until the next stressed syllable
		 */
		public byte process(Target target) {
			Element segment = target.getMaryxmlElement();
			if (segment == null)
				return 0;
			Element phrase = (Element) MaryDomUtils.getAncestor(segment, MaryXML.PHRASE);
			if (phrase == null)
				return 0;
			int count = 0;
			TreeWalker tw = MaryDomUtils.createTreeWalker(phrase, MaryXML.SYLLABLE);
			tw.setCurrentNode(segment);
			Element e;
			while ((e = (Element) tw.nextNode()) != null && count < RAIL_LIMIT) {
				count++;
				String stress = e.getAttribute("stress");
				if (stress.equals("1"))
					break;
			}
			return (byte) count;
		}
	}

	/**
	 * Determines the word punctuation.
	 */
	public static class WordPunc implements ByteValuedFeatureProcessor {
		protected String name;
		protected TargetElementNavigator navigator;
		protected ByteStringTranslator values;

		/**
		 * @param name
		 *            name of this feature processor
		 * @param wordNavigator
		 *            a navigator which returns a word for a target. This navigator decides the word for which the punctuation
		 *            will be computed.
		 */
		public WordPunc(String name, TargetElementNavigator wordNavigator) {
			this.name = name;
			this.navigator = wordNavigator;
			this.values = new ByteStringTranslator(new String[] { "0", ".", ",", ";", ":", "(", ")", "?", "!", "\"" });
		}

		public String getName() {
			return name;
		}

		public String[] getValues() {
			return values.getStringValues();
		}

		public byte process(Target target) {
			Element word = navigator.getElement(target);
			if (word == null)
				return 0;
			Element sentence = (Element) MaryDomUtils.getAncestor(word, MaryXML.SENTENCE);
			if (sentence == null)
				return 0;
			TreeWalker tw = MaryDomUtils.createTreeWalker(sentence, MaryXML.TOKEN, MaryXML.BOUNDARY);
			tw.setCurrentNode(word);
			Element next = (Element) tw.nextNode();
			if (next == null || !next.getTagName().equals(MaryXML.TOKEN) || next.hasAttribute("ph"))
				return 0;
			String text = MaryDomUtils.tokenText(next);
			if (values.contains(text)) {
				return values.get(text);
			}
			// unknown or no punctuation: return "0"
			return values.get("0");
		}
	}

	/**
	 * Determines the next word punctuation in the sentence.
	 */
	public static class NextPunctuation extends WordPunc {
		public NextPunctuation() {
			super("next_punctuation", new WordNavigator());
		}

		public byte process(Target target) {
			Element word = navigator.getElement(target);
			if (word == null)
				return 0;
			Element sentence = (Element) MaryDomUtils.getAncestor(word, MaryXML.SENTENCE);
			if (sentence == null)
				return 0;
			TreeWalker tw = MaryDomUtils.createTreeWalker(sentence, MaryXML.TOKEN);
			tw.setCurrentNode(word);
			Element e;
			while ((e = (Element) tw.nextNode()) != null) {
				if (e.hasAttribute("ph")) // a word
					continue;
				// potentially a punctuation
				String text = MaryDomUtils.tokenText(e);
				if (values.contains(text)) {
					return values.get(text);
				}
			}
			// no next punctuation: return "0"
			return values.get("0");
		}
	}

	/**
	 * Determines the previous word punctuation in the sentence.
	 */
	public static class PrevPunctuation extends WordPunc {
		public PrevPunctuation() {
			super("prev_punctuation", new WordNavigator());
		}

		public byte process(Target target) {
			Element word = navigator.getElement(target);
			if (word == null)
				return 0;
			Element sentence = (Element) MaryDomUtils.getAncestor(word, MaryXML.SENTENCE);
			if (sentence == null)
				return 0;
			TreeWalker tw = MaryDomUtils.createTreeWalker(sentence, MaryXML.TOKEN);
			tw.setCurrentNode(word);
			Element e;
			while ((e = (Element) tw.previousNode()) != null) {
				if (e.hasAttribute("ph")) // a word
					continue;
				// potentially a punctuation
				String text = MaryDomUtils.tokenText(e);
				if (values.contains(text)) {
					return values.get(text);
				}
			}
			// no next punctuation: return "0"
			return values.get("0");
		}
	}

	/**
	 * Determines the distance in words to the next word punctuation in the sentence.
	 */
	public static class WordsToNextPunctuation extends WordPunc {
		public WordsToNextPunctuation() {
			super("words_to_next_punctuation", new WordNavigator());
		}

		public String[] getValues() {
			return ZERO_TO_NINETEEN;
		}

		public byte process(Target target) {
			Element word = navigator.getElement(target);
			if (word == null)
				return 0;
			Element sentence = (Element) MaryDomUtils.getAncestor(word, MaryXML.SENTENCE);
			if (sentence == null)
				return 0;
			TreeWalker tw = MaryDomUtils.createTreeWalker(sentence, MaryXML.TOKEN);
			tw.setCurrentNode(word);
			Element e;
			int count = 0;
			while ((e = (Element) tw.nextNode()) != null && count < RAIL_LIMIT) {
				count++;
				if (e.hasAttribute("ph")) // a word
					continue;
				// potentially a punctuation
				String text = MaryDomUtils.tokenText(e);
				if (values.contains(text)) {
					break;
				}
			}
			// found punctuation or end of sentence:
			return (byte) count;
		}
	}

	/**
	 * Determines the distance in words from the previous word punctuation in the sentence.
	 */
	public static class WordsFromPrevPunctuation extends WordPunc {
		public WordsFromPrevPunctuation() {
			super("words_from_prev_punctuation", new WordNavigator());
		}

		public String[] getValues() {
			return ZERO_TO_NINETEEN;
		}

		public byte process(Target target) {
			Element word = navigator.getElement(target);
			if (word == null)
				return 0;
			Element sentence = (Element) MaryDomUtils.getAncestor(word, MaryXML.SENTENCE);
			if (sentence == null)
				return 0;
			TreeWalker tw = MaryDomUtils.createTreeWalker(sentence, MaryXML.TOKEN);
			tw.setCurrentNode(word);
			Element e;
			int count = 0;
			while ((e = (Element) tw.previousNode()) != null && count < RAIL_LIMIT) {
				count++;
				if (e.hasAttribute("ph")) // a word
					continue;
				// potentially a punctuation
				String text = MaryDomUtils.tokenText(e);
				if (values.contains(text)) {
					break;
				}
			}
			// found punctuation or start of sentence:
			return (byte) count;
		}
	}

	/**
	 * Determine the prosodic property of a target
	 * 
	 * @author Anna Hunecke
	 *
	 */
	public static class Selection_Prosody implements ByteValuedFeatureProcessor {

		protected TargetElementNavigator navigator;
		protected ByteStringTranslator values = new ByteStringTranslator(new String[] { "0", "stressed", "pre-nuclear",
				"nuclear", "finalHigh", "finalLow", "final" });
		private Set<String> lowEndtones = new HashSet<String>(Arrays.asList(new String[] { "L-", "L-%", "L-L%" }));
		private Set<String> highEndtones = new HashSet<String>(Arrays.asList(new String[] { "H-", "!H-", "H-%", "H-L%", "!H-%",
				"H-^H%", "!H-^H%", "L-H%", "H-H%" }));

		public Selection_Prosody(TargetElementNavigator syllableNavigator) {
			this.navigator = syllableNavigator;
		}

		public String getName() {
			return "selection_prosody";
		}

		public String[] getValues() {
			return values.getStringValues();
		}

		/**
		 * Determine the prosodic property of the target
		 * 
		 * @param target
		 *            the target
		 * @return 0 - unstressed, 1 - stressed, 2 - pre-nuclear accent 3 - nuclear accent, 4 - phrase final high, 5 - phrase
		 *         final low, 6 - phrase final (with unknown high/low status).
		 */
		public byte process(Target target) {
			// first find out if syllable is stressed
			Element syllable = navigator.getElement(target);
			if (syllable == null)
				return (byte) 0;
			boolean stressed = false;
			if (syllable.getAttribute("stress").equals("1")) {
				stressed = true;
			}
			// find out if we have an accent
			boolean accented = syllable.hasAttribute("accent");
			boolean nuclear = true; // relevant only if accented == true
			// find out the position of the target
			boolean phraseFinal = false;
			String endtone = null;
			Element sentence = (Element) MaryDomUtils.getAncestor(syllable, MaryXML.SENTENCE);
			if (sentence == null)
				return 0;
			TreeWalker tw = MaryDomUtils.createTreeWalker(sentence, MaryXML.SYLLABLE, MaryXML.BOUNDARY);
			tw.setCurrentNode(syllable);
			Element e = (Element) tw.nextNode();
			if (e != null) {
				if (e.getTagName().equals(MaryXML.BOUNDARY)) {
					phraseFinal = true;
					endtone = e.getAttribute("tone");
				}
				if (accented) { // look forward for any accent
					while (e != null) {
						if (e.getTagName().equals(MaryXML.SYLLABLE) && e.hasAttribute("accent")) {
							nuclear = false;
							break;
						}
						e = (Element) tw.nextNode();
					}
				}
			}
			// Now, we know:
			// stressed or not
			// accented or not
			// if accented, nuclear or not
			// if final, the endtone

			if (accented) {
				if (nuclear) {
					return values.get("nuclear");
				} else {
					return values.get("pre-nuclear");
				}
			} else if (phraseFinal) {
				if (endtone != null && highEndtones.contains(endtone)) {
					return values.get("finalHigh");
				} else if (endtone != null && lowEndtones.contains(endtone)) {
					return values.get("finalLow");
				} else {
					return values.get("final");
				}
			} else if (stressed) {
				return values.get("stressed");
			}
			return (byte) 0;// return unstressed
		}
	}

	/**
	 * Returns the duration of the given segment, in seconds.
	 */
	public static class UnitDuration implements ContinuousFeatureProcessor {
		public String getName() {
			return "unit_duration";
		}

		public float process(Target target) {
			if (target instanceof DiphoneTarget) {
				DiphoneTarget diphone = (DiphoneTarget) target;
				return process(diphone.left) + process(diphone.right);
			}
			Element seg = target.getMaryxmlElement();
			if (seg == null) {
				return 0;
			}
			float phoneDuration = 0;
			String sDur;
			if (seg.getTagName().equals(MaryXML.PHONE))
				sDur = seg.getAttribute("d");
			else {
				assert seg.getTagName().equals(MaryXML.BOUNDARY) : "segment should be a phone or a boundary, but is a "
						+ seg.getTagName();
				sDur = seg.getAttribute("duration");
			}
			if (sDur.equals("")) {
				return 0;
			}
			try {
				// parse duration string, and convert from milliseconds into seconds:
				phoneDuration = Float.parseFloat(sDur) * 0.001f;
			} catch (NumberFormatException nfe) {
			}
			if (target instanceof HalfPhoneTarget)
				return phoneDuration / 2;
			return phoneDuration;
		}
	}

	/**
	 * Calculates the log of the fundamental frequency in the middle of a unit segment. This processor should be used by target
	 * items only -- for unit features during voice building, the actual measured values should be used.
	 */
	public static class UnitLogF0 implements ContinuousFeatureProcessor {
		public String getName() {
			return "unit_logf0";
		}

		public float process(Target target) {
			return process(target, false);
		}

		/**
		 * Compute log f0 and log f0 delta for the given target.
		 * 
		 * @param target
		 *            target
		 * @param delta
		 *            if true, return the delta, i.e. the logF0 slope; if false, return the log f0 value itself.
		 * @return 0 if seg is null, or if !seg.getTagName().equals(MaryXML.PHONE), 0 if lastPos == null or nextPos == null,
		 *         return lastF0 otherwise; if delta return slope, return f0 otherwise
		 */
		protected float process(Target target, boolean delta) {
			// Note: all variables in this method with "f0" in their name
			// actually represent log f0 values.
			if (target instanceof DiphoneTarget) {
				DiphoneTarget diphone = (DiphoneTarget) target;
				return (process(diphone.left) + process(diphone.right)) / 2;
			}
			// Idea: find the closest f0 targets in the current syllable, left and right of our middle;
			// linearly interpolate between them to find the value in the middle of this unit.
			Element seg = target.getMaryxmlElement();
			if (seg == null) {
				return 0;
			}
			if (!seg.getTagName().equals(MaryXML.PHONE)) {
				return 0;
			}
			// get mid position of segment wrt phone start (phone start = 0, phone end = phone duration)
			float mid;
			float phoneDuration = getDuration(seg);
			if (target instanceof HalfPhoneTarget) {
				if (((HalfPhoneTarget) target).isLeftHalf()) {
					mid = .25f;
				} else {
					mid = .75f;
				}
			} else { // phone target
				mid = .5f;
			}

			// Now mid is the middle of the unit relative to the phone start, in percent
			Float lastPos = null; // position relative to mid, in milliseconds (negative)
			float lastF0 = 0;
			Float nextPos = null; // position relative to mid, in milliseconds
			float nextF0 = 0;
			Float[] f0values = getLogF0Values(seg);

			assert f0values != null;
			// values are position, f0, position, f0, etc.;
			// position is in percent of phone duration between 0 and 1, f0 is in Hz
			for (int i = 0; i < f0values.length; i += 2) {
				float pos = f0values[i];
				if (pos <= mid) {
					lastPos = (pos - mid) * phoneDuration; // negative or zero
					lastF0 = f0values[i + 1];
				} else if (pos > mid) {
					nextPos = (pos - mid) * phoneDuration; // positive
					nextF0 = f0values[i + 1];
					break; // no point looking further to the right
				}
			}
			if (lastPos == null) { // need to look to the left
				float msBack = -mid * phoneDuration;
				Element e = seg;

				// get all phone units in the same phrase
				Element phraseElement = (Element) MaryDomUtils.getAncestor(seg, MaryXML.PHRASE);
				TreeWalker tw = MaryDomUtils.createTreeWalker(seg.getOwnerDocument(), phraseElement, MaryXML.PHONE);
				Element en;
				while ((en = (Element) tw.nextNode()) != null) {
					if (en == seg) {
						break;
					}
				}

				while ((e = (Element) tw.previousNode()) != null) {
					float dur = getDuration(e);
					f0values = getLogF0Values(e);
					if (f0values.length == 0) {
						msBack -= dur;
						continue;
					}
					assert f0values.length > 1;
					float pos = f0values[f0values.length - 2];
					lastPos = msBack - (1 - pos) * dur;
					lastF0 = f0values[f0values.length - 1];
					break;
				}
			}

			if (nextPos == null) { // need to look to the right
				float msForward = (1 - mid) * phoneDuration;
				Element e = seg;

				// get all phone units in the same phrase
				Element phraseElement = (Element) MaryDomUtils.getAncestor(seg, MaryXML.PHRASE);
				TreeWalker tw = MaryDomUtils.createTreeWalker(seg.getOwnerDocument(), phraseElement, MaryXML.PHONE);
				Element en;
				while ((en = (Element) tw.nextNode()) != null) {
					if (en == seg) {
						break;
					}
				}

				while ((e = (Element) tw.nextNode()) != null) {
					float dur = getDuration(e);
					f0values = getLogF0Values(e);
					if (f0values.length == 0) {
						msForward += dur;
						continue;
					}
					assert f0values.length > 1;
					float pos = f0values[0];
					nextPos = msForward + pos * dur;
					nextF0 = f0values[1];
					break;
				}
			}

			if (lastPos == null && nextPos == null) {
				// no info
				return 0;
			} else if (lastPos == null) {
				// have only nextF0;
				if (delta)
					return 0;
				else
					return nextF0;
			} else if (nextPos == null) {
				// have only lastF0
				if (delta)
					return 0;
				else
					return lastF0;
			}
			assert lastPos <= 0 && 0 <= nextPos : "unexpected: lastPos=" + lastPos + ", nextPos=" + nextPos;
			// build a linear function (f(x) = slope*x+intersectionYAxis)
			float f0;
			float slope;
			if (lastPos - nextPos == 0) {
				f0 = (lastF0 + nextF0) / 2;
				slope = 0;
			} else {
				slope = (nextF0 - lastF0) / (nextPos - lastPos);
				// calculate the pitch
				f0 = lastF0 + slope * (-lastPos);
			}
			assert !Float.isNaN(f0) : "f0 is not a number";
			assert lastF0 <= f0 && nextF0 >= f0 || lastF0 >= f0 && nextF0 <= f0 : "f0 should be between last and next values";

			if (delta)
				return slope;
			else
				return f0;
		}

		private Float[] getLogF0Values(Element ph) {
			String mbrTargets = ph.getAttribute("f0");
			if (mbrTargets.equals("")) {
				return new Float[0];
			}
			ArrayList<Float> values = new ArrayList<Float>();
			try {
				// mbrTargets contains one or more pairs of numbers,
				// either enclosed by (a,b) or just separated by whitespace.
				StringTokenizer st = new StringTokenizer(mbrTargets, " (,)");
				while (st.hasMoreTokens()) {
					String posString = "";
					while (st.hasMoreTokens() && posString.equals(""))
						posString = st.nextToken();
					String f0String = "";
					while (st.hasMoreTokens() && f0String.equals(""))
						f0String = st.nextToken();

					float pos = Float.parseFloat(posString) * 0.01f;
					assert 0 <= pos && pos <= 1 : "invalid position:" + pos + " (pos string was '" + posString
							+ "' coming from '" + mbrTargets + "')";
					float f0 = Float.parseFloat(f0String);
					float logF0 = (float) Math.log(f0);
					values.add(pos);
					values.add(logF0);
				}
			} catch (Exception e) {
				return new Float[0];
			}
			return values.toArray(new Float[0]);
		}

		private float getDuration(Element ph) {
			float phoneDuration = 0;
			String sDur = ph.getAttribute("d");
			if (!sDur.equals("")) {
				try {
					phoneDuration = Float.parseFloat(sDur);
				} catch (NumberFormatException nfe) {
				}
			}
			return phoneDuration;
		}
	}

	/**
	 * Calculates the slope of a linear approximation of the fundamental frequency, in the log domain. The slope is computed by
	 * linearly connecting the two log f0 values closest to the middle of the unit segment. This processor should be used by
	 * target items only -- for unit features during voice building, the actual measured values should be used.
	 */
	public static class UnitLogF0Delta extends UnitLogF0 {
		@Override
		public String getName() {
			return "unit_logf0delta";
		}

		public float process(Target target) {
			return process(target, true);
		}
	}

	/**
	 * Returns the value of the given feature for the given segment.
	 */
	public static class GenericContinuousFeature implements ContinuousFeatureProcessor {
		private String name;
		private String attributeName;

		public GenericContinuousFeature(String featureName, String attributeName) {
			this.name = featureName;
			this.attributeName = attributeName;
		}

		public String getName() {
			return name;
		}

		public float process(Target target) {
			if (target instanceof DiphoneTarget) {
				DiphoneTarget diphone = (DiphoneTarget) target;
				// return mean of left and right costs:
				return (process(diphone.left) + process(diphone.right)) / 2.0f;
			}
			Element seg = target.getMaryxmlElement();
			if (seg == null) {
				return 0;
			}
			float value = 0;
			String valueString;
			if (seg.getTagName().equals(MaryXML.PHONE)) {
				valueString = seg.getAttribute(attributeName);
			} else {
				assert seg.getTagName().equals(MaryXML.BOUNDARY) : "segment should be a phone or a boundary, but is a "
						+ seg.getTagName();
				valueString = seg.getAttribute(attributeName);
			}
			if (valueString.equals("")) {
				return 0;
			}
			try {
				value = Float.parseFloat(valueString);
			} catch (NumberFormatException nfe) {
				return 0;
			}
			return value;
		}
	}

}
