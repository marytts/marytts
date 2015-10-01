/**
 * Copyright 2007 DFKI GmbH.
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
package marytts.modules;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Locale;

import marytts.cart.CART;
import marytts.cart.io.MaryCARTReader;
import marytts.datatypes.MaryData;
import marytts.datatypes.MaryDataType;
import marytts.datatypes.MaryXML;
import marytts.exceptions.SynthesisException;
import marytts.features.FeatureDefinition;
import marytts.features.FeatureProcessorManager;
import marytts.features.FeatureRegistry;
import marytts.features.TargetFeatureComputer;
import marytts.modules.phonemiser.Allophone;
import marytts.modules.phonemiser.AllophoneSet;
import marytts.modules.synthesis.Voice;
import marytts.server.MaryProperties;
import marytts.unitselection.UnitSelectionVoice;
import marytts.unitselection.select.Target;
import marytts.util.MaryRuntimeUtils;
import marytts.util.MaryUtils;
import marytts.util.dom.MaryDomUtils;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.traversal.NodeIterator;
import org.w3c.dom.traversal.TreeWalker;

/**
 * Predict phone durations using a CART.
 * 
 * @author Marc Schr&ouml;der
 * @deprecated
 */

public class CARTF0Modeller extends InternalModule {
	protected CART leftCart;
	protected CART midCart;
	protected CART rightCart;
	protected TargetFeatureComputer featureComputer;
	private String propertyPrefix;
	private FeatureProcessorManager featureProcessorManager;

	/**
	 * Constructor which can be directly called from init info in the config file. This constructor will use the registered
	 * feature processor manager for the given locale.
	 * 
	 * @param locale
	 *            a locale string, e.g. "en"
	 * @param propertyPrefix
	 *            the prefix to be used when looking up entries in the config files, e.g. "english.duration"
	 * @throws Exception
	 *             Exception
	 */
	public CARTF0Modeller(String locale, String propertyPrefix) throws Exception {
		this(MaryUtils.string2locale(locale), propertyPrefix, FeatureRegistry.getFeatureProcessorManager(MaryUtils
				.string2locale(locale)));
	}

	/**
	 * Constructor which can be directly called from init info in the config file. Different languages can call this code with
	 * different settings.
	 * 
	 * @param locale
	 *            a locale string, e.g. "en"
	 * @param propertyPrefix
	 *            the prefix to be used when looking up entries in the config files, e.g. "english.f0"
	 * @param featprocClassInfo
	 *            a package name for an instance of FeatureProcessorManager, e.g. "marytts.language.en.FeatureProcessorManager"
	 * @throws Exception
	 *             Exception
	 */
	public CARTF0Modeller(String locale, String propertyPrefix, String featprocClassInfo) throws Exception {
		this(MaryUtils.string2locale(locale), propertyPrefix, (FeatureProcessorManager) MaryRuntimeUtils
				.instantiateObject(featprocClassInfo));
	}

	/**
	 * Constructor to be called with instantiated objects.
	 * 
	 * @param locale
	 *            locale
	 * @param propertyPrefix
	 *            the prefix to be used when looking up entries in the config files, e.g. "english.f0"
	 * @param featureProcessorManager
	 *            the manager to use when looking up feature processors.
	 */
	protected CARTF0Modeller(Locale locale, String propertyPrefix, FeatureProcessorManager featureProcessorManager) {
		super("CARTF0Modeller", MaryDataType.DURATIONS, MaryDataType.ACOUSTPARAMS, locale);
		if (propertyPrefix.endsWith("."))
			this.propertyPrefix = propertyPrefix;
		else
			this.propertyPrefix = propertyPrefix + ".";
		this.featureProcessorManager = featureProcessorManager;
	}

	public void startup() throws Exception {
		super.startup();

		String leftCartFilename = MaryProperties.getFilename(propertyPrefix + "cart.left");
		if (leftCartFilename != null) { // if we have this, we *need* all the rest
			File leftCartFile = new File(leftCartFilename);

			File fdFile = new File(MaryProperties.needFilename(propertyPrefix + "featuredefinition"));
			FeatureDefinition featureDefinition = new FeatureDefinition(new BufferedReader(new FileReader(fdFile)), true);

			// old: leftCart = new RegressionTree(new BufferedReader(new FileReader(leftCartFile)), featureDefinition);
			// leftCart = new CART();
			// leftCart.setRootNode(wagonReader.load(new BufferedReader(new FileReader(leftCartFile)), featureDefinition));
			leftCart = (new MaryCARTReader()).load(leftCartFile.getAbsolutePath());

			File midCartFile = new File(MaryProperties.needFilename(propertyPrefix + "cart.mid"));
			// old: midCart = new RegressionTree(new BufferedReader(new FileReader(midCartFile)), featureDefinition);
			// midCart = new CART();
			// midCart.setRootNode(wagonReader.load(new BufferedReader(new FileReader(midCartFile)), featureDefinition));
			midCart = (new MaryCARTReader()).load(midCartFile.getAbsolutePath());

			File rightCartFile = new File(MaryProperties.needFilename(propertyPrefix + "cart.right"));
			// old: rightCart = new RegressionTree(new BufferedReader(new FileReader(rightCartFile)), featureDefinition);
			// rightCart = new CART();
			// rightCart.setRootNode(wagonReader.load(new BufferedReader(new FileReader(rightCartFile)), featureDefinition));
			rightCart = (new MaryCARTReader()).load(rightCartFile.getAbsolutePath());
			featureComputer = new TargetFeatureComputer(featureProcessorManager, featureDefinition.getFeatureNames());
		}
	}

	public MaryData process(MaryData d) throws Exception {
		Document doc = d.getDocument();
		NodeIterator sentenceIt = MaryDomUtils.createNodeIterator(doc, MaryXML.SENTENCE);
		Element sentence = null;
		AllophoneSet allophoneSet = null;
		while ((sentence = (Element) sentenceIt.nextNode()) != null) {
			// Make sure we have the correct voice:
			Element voice = (Element) MaryDomUtils.getAncestor(sentence, MaryXML.VOICE);
			Voice maryVoice = Voice.getVoice(voice);
			if (maryVoice == null) {
				maryVoice = d.getDefaultVoice();
			}
			if (maryVoice == null) {
				// Determine Locale in order to use default voice
				Locale locale = MaryUtils.string2locale(doc.getDocumentElement().getAttribute("xml:lang"));
				maryVoice = Voice.getDefaultVoice(locale);
			}
			CART currentLeftCart = leftCart;
			CART currentMidCart = midCart;
			CART currentRightCart = rightCart;
			TargetFeatureComputer currentFeatureComputer = featureComputer;
			if (maryVoice instanceof UnitSelectionVoice) {
				CART[] voiceTrees = ((UnitSelectionVoice) maryVoice).getF0Trees();
				if (voiceTrees != null) {
					currentLeftCart = voiceTrees[0];
					currentMidCart = voiceTrees[1];
					currentRightCart = voiceTrees[2];
					logger.debug("Using voice carts");
				}
				FeatureDefinition voiceFeatDef = ((UnitSelectionVoice) maryVoice).getF0CartsFeatDef();
				if (voiceFeatDef != null) {
					currentFeatureComputer = new TargetFeatureComputer(featureProcessorManager, voiceFeatDef.getFeatureNames());
					logger.debug("Using voice feature definition");
				}
			}

			if (currentLeftCart == null) {
				throw new NullPointerException("Do not have f0 prediction tree");
			}

			TreeWalker tw = MaryDomUtils.createTreeWalker(sentence, MaryXML.SYLLABLE);
			Element syllable;
			Element previous = null;
			while ((syllable = (Element) tw.nextNode()) != null) {
				Element firstVoiced = null;
				Element vowel = null;
				Element lastVoiced = null;
				for (Element s = MaryDomUtils.getFirstChildElement(syllable); s != null; s = MaryDomUtils
						.getNextSiblingElement(s)) {
					assert s.getTagName().equals(MaryXML.PHONE) : "expected phone element, found " + s.getTagName();
					String phone = s.getAttribute("p");
					if (allophoneSet == null) {
						allophoneSet = MaryRuntimeUtils.determineAllophoneSet(s);
					}
					assert allophoneSet != null;
					Allophone allophone;
					try {
						allophone = allophoneSet.getAllophone(phone);
					} catch (IllegalArgumentException e) {
						throw new SynthesisException(e);
					}
					if (allophone.isVowel()) {
						// found a vowel
						if (firstVoiced == null)
							firstVoiced = s;
						if (vowel == null)
							vowel = s;
						lastVoiced = s; // last so far, at least
					} else if (allophone.isVoiced()) {
						// voiced consonant
						if (firstVoiced == null)
							firstVoiced = s;
						lastVoiced = s;
					}
				}
				// only predict F0 values if we have a vowel:
				if (vowel != null) {
					assert firstVoiced != null : "First voiced should not be null";
					assert lastVoiced != null : "Last voiced should not be null";
					// Now predict the f0 values using the CARTs:ssh
					String phone = vowel.getAttribute("p");
					Target t = new Target(phone, vowel);
					t.setFeatureVector(currentFeatureComputer.computeFeatureVector(t));
					float[] left = (float[]) currentLeftCart.interpret(t, 0);
					assert left != null : "Null frequency";
					assert left.length == 2 : "Unexpected frequency length: " + left.length;
					float leftF0InHz = left[1];
					float leftStddevInHz = left[0];
					float[] mid = (float[]) currentMidCart.interpret(t, 0);
					assert mid != null : "Null frequency";
					assert mid.length == 2 : "Unexpected frequency length: " + mid.length;
					float midF0InHz = mid[1];
					float midStddevInHz = mid[0];
					float[] right = (float[]) currentRightCart.interpret(t, 0);
					assert right != null : "Null frequency";
					assert right.length == 2 : "Unexpected frequency length: " + right.length;
					float rightF0InHz = right[1];
					float rightStddevInHz = right[0];
					// Now set targets:
					String leftTargetString = "(0," + ((int) leftF0InHz) + ")";
					String currentVal = firstVoiced.getAttribute("f0");
					String newVal;
					if (!currentVal.equals("")) {
						newVal = currentVal + " " + leftTargetString;
					} else {
						newVal = leftTargetString;
					}
					firstVoiced.setAttribute("f0", newVal);

					String midTargetString = "(50," + ((int) midF0InHz) + ")";
					currentVal = vowel.getAttribute("f0");
					// for example, if firstVoiced == vowel, then we have just set a first f0 value
					if (!currentVal.equals("")) {
						newVal = currentVal + " " + midTargetString;
					} else {
						newVal = midTargetString;
					}
					vowel.setAttribute("f0", newVal);

					String rightTargetString = "(100," + ((int) rightF0InHz) + ")";
					currentVal = lastVoiced.getAttribute("f0");
					// for example, if lastVoiced == vowel, then we have just set a first f0 value
					if (!currentVal.equals("")) {
						newVal = currentVal + " " + rightTargetString;
					} else {
						newVal = rightTargetString;
					}
					lastVoiced.setAttribute("f0", newVal);

				}
			}
		}
		MaryData output = new MaryData(outputType(), d.getLocale());
		output.setDocument(doc);
		return output;
	}
}
