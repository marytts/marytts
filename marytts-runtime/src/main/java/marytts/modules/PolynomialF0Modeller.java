/**
 * Copyright 2009 DFKI GmbH.
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

import java.util.Locale;

import marytts.cart.DirectedGraph;
import marytts.cart.io.DirectedGraphReader;
import marytts.datatypes.MaryData;
import marytts.datatypes.MaryDataType;
import marytts.datatypes.MaryXML;
import marytts.features.FeatureDefinition;
import marytts.features.FeatureProcessorManager;
import marytts.features.TargetFeatureComputer;
import marytts.modules.phonemiser.Allophone;
import marytts.modules.phonemiser.AllophoneSet;
import marytts.modules.synthesis.Voice;
import marytts.server.MaryProperties;
import marytts.unitselection.data.FeatureFileReader;
import marytts.unitselection.select.Target;
import marytts.util.MaryRuntimeUtils;
import marytts.util.MaryUtils;
import marytts.util.dom.MaryDomUtils;
import marytts.util.dom.NameNodeFilter;
import marytts.util.math.ArrayUtils;
import marytts.util.math.Polynomial;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.traversal.DocumentTraversal;
import org.w3c.dom.traversal.NodeFilter;
import org.w3c.dom.traversal.NodeIterator;
import org.w3c.dom.traversal.TreeWalker;

/**
 * Predict f0 contours using polynomial curves predicted from a directed graph per syllable.
 * 
 * @author Marc Schr&ouml;der
 */

public class PolynomialF0Modeller extends InternalModule {
	protected FeatureFileReader contourFeatures;
	protected DirectedGraph contourGraph;
	protected TargetFeatureComputer featureComputer;
	private String propertyPrefix;
	private FeatureProcessorManager featureProcessorManager;

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
	public PolynomialF0Modeller(String locale, String propertyPrefix, String featprocClassInfo) throws Exception {
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
	protected PolynomialF0Modeller(Locale locale, String propertyPrefix, FeatureProcessorManager featureProcessorManager) {
		super("PolynomialF0Modeller", MaryDataType.DURATIONS, MaryDataType.ACOUSTPARAMS, locale);
		if (propertyPrefix.endsWith("."))
			this.propertyPrefix = propertyPrefix;
		else
			this.propertyPrefix = propertyPrefix + ".";
		this.featureProcessorManager = featureProcessorManager;
	}

	public void startup() throws Exception {
		super.startup();

		contourFeatures = new FeatureFileReader(MaryProperties.needFilename(propertyPrefix + "contours"));
		contourGraph = new DirectedGraphReader().load(MaryProperties.needFilename(propertyPrefix + "graph"));
		featureComputer = new TargetFeatureComputer(featureProcessorManager, contourGraph.getFeatureDefinition()
				.getFeatureNames());
	}

	public MaryData process(MaryData d) throws Exception {
		Document doc = d.getDocument();
		NodeIterator sentenceIt = ((DocumentTraversal) doc).createNodeIterator(doc, NodeFilter.SHOW_ELEMENT, new NameNodeFilter(
				MaryXML.SENTENCE), false);
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
			FeatureFileReader currentContours = contourFeatures;
			DirectedGraph currentGraph = contourGraph;
			TargetFeatureComputer currentFeatureComputer = featureComputer;
			if (maryVoice != null) {
				DirectedGraph voiceGraph = maryVoice.getF0Graph();
				if (voiceGraph != null) {
					currentGraph = voiceGraph;
					logger.debug("Using voice graph");
					FeatureDefinition voiceFeatDef = voiceGraph.getFeatureDefinition();
					currentFeatureComputer = new TargetFeatureComputer(featureProcessorManager, voiceFeatDef.getFeatureNames());
					FeatureFileReader voiceContourFeatures = maryVoice.getF0ContourFeatures();
					currentContours = voiceContourFeatures;
				}
			}

			TreeWalker tw = ((DocumentTraversal) doc).createTreeWalker(sentence, NodeFilter.SHOW_ELEMENT, new NameNodeFilter(
					MaryXML.SYLLABLE), false);
			Element syllable;
			Element previous = null;
			while ((syllable = (Element) tw.nextNode()) != null) {
				Element vowel = null;
				float sylDur = 0;
				for (Element s = MaryDomUtils.getFirstChildElement(syllable); s != null; s = MaryDomUtils
						.getNextSiblingElement(s)) {
					assert s.getTagName().equals(MaryXML.PHONE) : "expected phone element, found " + s.getTagName();
					if (s.hasAttribute("d")) {
						sylDur += Float.parseFloat(s.getAttribute("d"));
					}
					String phone = s.getAttribute("p");
					if (allophoneSet == null) {
						allophoneSet = MaryRuntimeUtils.determineAllophoneSet(s);
					}
					assert allophoneSet != null;
					Allophone allophone = allophoneSet.getAllophone(phone);
					if (allophone.isVowel()) {
						// found a vowel
						vowel = s;
					}
				}
				// only predict F0 values if we have a vowel:
				if (vowel != null) {
					// Now predict the f0 values using the CARTs:ssh
					String phone = vowel.getAttribute("p");
					Target t = new Target(phone, vowel);
					t.setFeatureVector(currentFeatureComputer.computeFeatureVector(t));
					// double[] coeffs = ArrayUtils.toDoubleArray((float[]) currentGraph.interpret(t));
					int[] leafContours = (int[]) currentGraph.interpret(t);
					if (leafContours == null) { // no prediction :-(
						continue;
					}
					// At this stage, use the mean contour.
					double[] coeffs = getMeanContour(currentContours, leafContours);
					float posInSyl = 0;
					float relStart = 0;
					float relEnd = 0;
					for (Element s = MaryDomUtils.getFirstChildElement(syllable); s != null; s = MaryDomUtils
							.getNextSiblingElement(s)) {
						if (s.hasAttribute("d")) {
							float dur = Float.parseFloat(s.getAttribute("d"));
							relStart = posInSyl / sylDur;
							posInSyl += dur;
							relEnd = posInSyl / sylDur;
							// Now, predict three values for each phone, at beginning, mid, and end of phone.
							double initialLogF0 = Polynomial.getValueAt(coeffs, relStart);
							double midLogF0 = Polynomial.getValueAt(coeffs, (relStart + relEnd) / 2);
							double finalLogF0 = Polynomial.getValueAt(coeffs, relEnd);
							StringBuilder f0String = new StringBuilder();
							if (!Double.isNaN(initialLogF0)) {
								f0String.append("(0,").append((int) Math.exp(initialLogF0)).append(")");
							}
							if (!Double.isNaN(midLogF0)) {
								f0String.append("(50,").append((int) Math.exp(midLogF0)).append(")");
							}
							if (!Double.isNaN(finalLogF0)) {
								f0String.append("(100,").append((int) Math.exp(finalLogF0)).append(")");
							}
							if (f0String.length() > 0) {
								s.setAttribute("f0", f0String.toString());
							}
						}
					}
					assert posInSyl == sylDur;

				}
			}
		}
		MaryData output = new MaryData(outputType(), d.getLocale());
		output.setDocument(doc);
		return output;
	}

	protected double[] getMeanContour(FeatureFileReader currentContours, int[] contourIDs) {
		double[] coeffs = null;
		for (int i = 0; i < contourIDs.length; i++) {
			float[] oneCoeffs = currentContours.getFeatureVector(contourIDs[i]).getContinuousFeatures();
			assert !ArrayUtils.isZero(oneCoeffs) : "Feature vector " + contourIDs[i] + " is zero";
			if (coeffs == null)
				coeffs = new double[oneCoeffs.length];
			for (int j = 0; j < oneCoeffs.length; j++) {
				coeffs[j] += oneCoeffs[j];
			}
		}
		for (int j = 0; j < coeffs.length; j++) {
			coeffs[j] /= contourIDs.length;
		}
		return coeffs;
	}
}
