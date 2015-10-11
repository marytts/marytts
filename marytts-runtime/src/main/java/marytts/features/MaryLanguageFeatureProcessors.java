/**
 * Portions Copyright 2006 DFKI GmbH.
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

import java.io.InputStream;
import java.util.Map;

import marytts.datatypes.MaryXML;
import marytts.fst.FSTLookup;
import marytts.modules.phonemiser.AllophoneSet;
import marytts.unitselection.select.HalfPhoneTarget;
import marytts.unitselection.select.Target;
import marytts.util.dom.MaryDomUtils;
import marytts.util.string.ByteStringTranslator;

import org.w3c.dom.Element;

/**
 * Provides the set of feature processors that are used by this language as part of the CART processing.
 */
public class MaryLanguageFeatureProcessors extends MaryGenericFeatureProcessors {

	// no instances
	private MaryLanguageFeatureProcessors() {
	}

	/**
	 * The phone symbol for the given target.
	 * 
	 * @author Marc Schr&ouml;der
	 *
	 */
	public static class Phone implements ByteValuedFeatureProcessor {
		protected String name;
		protected ByteStringTranslator values;
		protected String pauseSymbol;
		protected TargetElementNavigator navigator;

		/**
		 * Initialise a phone feature processor.
		 * 
		 * @param name
		 *            the name of the feature
		 * @param possibleValues
		 *            the list of possible phone values for the phonetic alphabet used, plus the value "0"=n/a.
		 * @param pauseSymbol
		 *            pauseSymbol
		 * @param segmentNavigator
		 *            a navigator returning a segment with respect to the target.
		 */
		public Phone(String name, String[] possibleValues, String pauseSymbol, TargetElementNavigator segmentNavigator) {
			this.name = name;
			this.values = new ByteStringTranslator(possibleValues);
			this.pauseSymbol = pauseSymbol;
			this.navigator = segmentNavigator;
		}

		public String getName() {
			return name;
		}

		public String[] getValues() {
			return values.getStringValues();
		}

		public byte process(Target target) {
			Element segment = navigator.getElement(target);
			if (segment == null)
				return values.get(pauseSymbol);
			if (!segment.getTagName().equals(MaryXML.PHONE))
				return values.get(pauseSymbol);
			String ph = segment.getAttribute("p");
			if (!values.contains(ph))
				return values.get("0");
			return values.get(ph);
		}

		public String getPauseSymbol() {
			return pauseSymbol;
		}
	}

	/**
	 * The unit name for the given half phone target.
	 * 
	 * @author Marc Schr&ouml;der
	 *
	 */
	public static class HalfPhoneUnitName implements ByteValuedFeatureProcessor {
		protected String name;
		protected ByteStringTranslator values;
		protected String pauseSymbol;

		/**
		 * Initialise a UnitName feature processor.
		 * 
		 * @param possiblePhonemes
		 *            the possible phonemes
		 * @param pauseSymbol
		 *            the pause symbol
		 */
		public HalfPhoneUnitName(String[] possiblePhonemes, String pauseSymbol) {
			this.name = "halfphone_unitname";
			this.pauseSymbol = pauseSymbol;
			String[] possibleValues = new String[2 * possiblePhonemes.length + 1];
			possibleValues[0] = "0"; // the "n/a" value
			for (int i = 0; i < possiblePhonemes.length; i++) {
				possibleValues[2 * i + 1] = possiblePhonemes[i] + "_L";
				possibleValues[2 * i + 2] = possiblePhonemes[i] + "_R";
			}
			this.values = new ByteStringTranslator(possibleValues);
		}

		public String getName() {
			return name;
		}

		public String[] getValues() {
			return values.getStringValues();
		}

		public byte process(Target target) {
			if (!(target instanceof HalfPhoneTarget))
				return 0;
			HalfPhoneTarget hpTarget = (HalfPhoneTarget) target;
			Element segment = target.getMaryxmlElement();
			String phoneLabel;
			if (segment == null) {
				phoneLabel = pauseSymbol;
			} else if (!segment.getTagName().equals(MaryXML.PHONE)) {
				phoneLabel = pauseSymbol;
			} else {
				phoneLabel = segment.getAttribute("p");
			}
			if (phoneLabel.equals(""))
				return values.get("0");
			String unitLabel = phoneLabel + (hpTarget.isLeftHalf() ? "_L" : "_R");
			return values.get(unitLabel);
		}
	}

	/**
	 * A parametrisable class which can retrieve all sorts of phone features, given a phone set.
	 * 
	 * @author Marc Schr&ouml;der
	 *
	 */
	public static class PhoneFeature implements ByteValuedFeatureProcessor {
		protected AllophoneSet phoneSet;
		protected String name;
		protected String phonesetQuery;
		protected ByteStringTranslator values;
		protected String pauseSymbol;
		protected TargetElementNavigator navigator;

		public PhoneFeature(AllophoneSet phoneSet, String name, String phonesetQuery, String[] possibleValues,
				String pauseSymbol, TargetElementNavigator segmentNavigator) {
			this.phoneSet = phoneSet;
			this.name = name;
			this.phonesetQuery = phonesetQuery;
			this.values = new ByteStringTranslator(possibleValues);
			this.navigator = segmentNavigator;
		}

		public String getName() {
			return name;
		}

		public String[] getValues() {
			return values.getStringValues();
		}

		public byte process(Target target) {
			Element segment = navigator.getElement(target);
			if (segment == null)
				return values.get("0");
			String ph;
			if (!segment.getTagName().equals(MaryXML.PHONE)) {
				ph = pauseSymbol;
			} else {
				ph = segment.getAttribute("p");
			}
			String value = phoneSet.getPhoneFeature(ph, phonesetQuery);
			if (value == null)
				return values.get("0");
			return values.get(value);
		}
	}

	/**
	 * Returns the part-of-speech.
	 */
	public static class Pos implements ByteValuedFeatureProcessor {
		private ByteStringTranslator values;
		private TargetElementNavigator navigator;
		private String name;

		public String getName() {
			return this.name;
		}

		public String[] getValues() {
			return values.getStringValues();
		}

		public Pos(String[] posValues) {
			this.values = new ByteStringTranslator(posValues);
			this.navigator = new WordNavigator();
			this.name = "pos";
		}

		public Pos(String aName, String[] posValues, TargetElementNavigator wordNavi) {
			this.values = new ByteStringTranslator(posValues);
			this.navigator = wordNavi;
			this.name = aName;
		}

		/**
		 * @param target
		 *            the item to process
		 * @return a guess at the part-of-speech for the item
		 */
		public byte process(Target target) {
			Element word = navigator.getElement(target);
			if (word == null)
				return values.get("0");
			String pos = word.getAttribute("pos");
			if (pos == null)
				return values.get("0");
			pos = pos.trim();
			if (values.contains(pos))
				return values.get(pos);
			return values.get("0");
		}
	}

	/**
	 * Returns generalised part-of-speech.
	 */
	public static class Gpos implements ByteValuedFeatureProcessor {
		private Map<String, String> posConverter;
		private ByteStringTranslator values;
		private TargetElementNavigator navigator;

		public String getName() {
			return "gpos";
		}

		public String[] getValues() {
			return values.getStringValues();
		}

		public Gpos(Map<String, String> posConverter) {
			this.posConverter = posConverter;
			this.values = new ByteStringTranslator(new String[] { "0", "in", // Preposition or subordinating conjunction
					"to", // to
					"det", // determiner
					"md", // modal
					"cc", // coordinating conjunction
					"wp", // w-pronouns
					"pps", // possive pronouns
					"aux", // auxiliary verbs
					"punc", // punctuation
					"content" // content words
			});
			this.navigator = new WordNavigator();
		}

		/**
		 * Performs some processing on the given item.
		 * 
		 * @param target
		 *            the item to process
		 * @return a guess at the part-of-speech for the item
		 */
		public byte process(Target target) {
			Element word = navigator.getElement(target);
			if (word == null)
				return values.get("0");
			String pos = word.getAttribute("pos");
			if (pos == null)
				return values.get("0");
			pos = pos.trim();
			if (posConverter.containsKey(pos)) {
				pos = (String) posConverter.get(pos);
			}
			if (!values.contains(pos))
				return values.get("0");
			return values.get(pos);
		}
	}

	/**
	 * Checks for onset coda This is a feature processor. A feature processor takes an item, performs some sort of processing on
	 * the item and returns an object.
	 */
	public static class SegOnsetCoda implements ByteValuedFeatureProcessor {
		protected ByteStringTranslator values;
		private AllophoneSet phoneSet;

		public SegOnsetCoda(AllophoneSet phoneSet) {
			this.phoneSet = phoneSet;
			this.values = new ByteStringTranslator(new String[] { "0", "onset", "coda" });
		}

		public String getName() {
			return "onsetcoda";
		}

		public String[] getValues() {
			return values.getStringValues();
		}

		public byte process(Target target) {
			Element s = target.getMaryxmlElement();
			if (s == null) {
				return 0;
			}
			if (!s.getTagName().equals(MaryXML.PHONE))
				return 0;

			while ((s = MaryDomUtils.getNextSiblingElement(s)) != null) {
				String ph = s.getAttribute("p");
				if ("+".equals(phoneSet.getPhoneFeature(ph, "vc"))) {
					return values.get("onset");
				}
			}
			return values.get("coda");
		}
	}

	/**
	 * The phone class for the given target.
	 * 
	 * @author Anna Hunecke
	 *
	 */
	public static class Selection_PhoneClass implements ByteValuedFeatureProcessor {
		protected String name;
		protected Map<String, String> phones2Classes;
		protected ByteStringTranslator values;
		protected TargetElementNavigator navigator;

		/**
		 * Initialise the feature processor.
		 * 
		 * @param phones2Classes
		 *            the mapping of phones to their classes
		 * @param classes
		 *            the available phone classes
		 * @param segmentNavigator
		 *            a navigator returning a segment with respect to the target.
		 */
		public Selection_PhoneClass(Map<String, String> phones2Classes, String[] classes, TargetElementNavigator segmentNavigator) {
			this.name = "selection_next_phone_class";
			this.phones2Classes = phones2Classes;
			this.values = new ByteStringTranslator(classes);
			this.navigator = segmentNavigator;
		}

		public String getName() {
			return name;
		}

		public String[] getValues() {
			return values.getStringValues();
		}

		/**
		 * Give back the phone class of the target
		 * 
		 * @param target
		 *            target
		 * @return the phone class of the target
		 */
		public byte process(Target target) {
			Element segment = navigator.getElement(target);
			if (segment == null)
				return values.get("0");
			if (!segment.getTagName().equals(MaryXML.PHONE))
				return 0;
			String ph = segment.getAttribute("p");
			String phoneClass = phones2Classes.get(ph);
			if (phoneClass == null) {
				return values.get("0");
			}
			return values.get(phoneClass);
		}
	}

	public static class WordFrequency implements ByteValuedFeatureProcessor {
		protected TargetElementNavigator navigator;
		protected ByteStringTranslator values;
		protected FSTLookup wordFrequencies;

		public WordFrequency(InputStream inStream, String identifier, String encoding) {
			this.navigator = new WordNavigator();
			try {
				if (inStream != null)
					this.wordFrequencies = new FSTLookup(inStream, identifier, encoding);
				else
					this.wordFrequencies = null;
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
			this.values = new ByteStringTranslator(new String[] { "0", "1", "2", "3", "4", "5", "6", "7", "8", "9" });
		}

		public String getName() {
			return "word_frequency";
		}

		public String[] getValues() {
			return values.getStringValues();
		}

		/**
		 * Performs some processing on the given item.
		 * 
		 * @param target
		 *            the target to process
		 * @return the frequency of the current word, on a ten-point scale from 0=unknown=very rare to 9=very frequent.
		 */
		public byte process(Target target) {
			Element word = navigator.getElement(target);
			if (word == null)
				return (byte) 0;
			String wordString = MaryDomUtils.tokenText(word);
			if (wordFrequencies != null) {
				String[] result = wordFrequencies.lookup(wordString);
				if (result.length > 0) {
					String freq = result[0];
					if (values.contains(freq))
						return values.get(freq);
				}

			}
			return (byte) 0; // unknown word
		}
	}

}
