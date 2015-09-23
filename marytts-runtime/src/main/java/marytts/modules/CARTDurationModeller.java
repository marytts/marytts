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
import marytts.cart.DirectedGraph;
import marytts.cart.StringPredictionTree;
import marytts.cart.io.DirectedGraphReader;
import marytts.datatypes.MaryData;
import marytts.datatypes.MaryDataType;
import marytts.datatypes.MaryXML;
import marytts.features.FeatureDefinition;
import marytts.features.FeatureProcessorManager;
import marytts.features.FeatureRegistry;
import marytts.features.TargetFeatureComputer;
import marytts.modules.synthesis.Voice;
import marytts.server.MaryProperties;
import marytts.unitselection.select.Target;
import marytts.unitselection.select.UnitSelector;
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

public class CARTDurationModeller extends InternalModule {
	protected DirectedGraph cart = new CART();
	// TODO: use a simple regression tree, with FloatLeafNode, for pausetree:
	protected StringPredictionTree pausetree;
	protected TargetFeatureComputer featureComputer;
	protected TargetFeatureComputer pauseFeatureComputer;
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
	public CARTDurationModeller(String locale, String propertyPrefix) throws Exception {
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
	 *            the prefix to be used when looking up entries in the config files, e.g. "english.duration"
	 * @param featprocClassInfo
	 *            a package name for an instance of FeatureProcessorManager, e.g. "marytts.language.en.FeatureProcessorManager"
	 * @throws Exception
	 *             Exception
	 */
	public CARTDurationModeller(String locale, String propertyPrefix, String featprocClassInfo) throws Exception {
		this(MaryUtils.string2locale(locale), propertyPrefix, (FeatureProcessorManager) MaryRuntimeUtils
				.instantiateObject(featprocClassInfo));
	}

	/**
	 * Constructor to be called with instantiated objects.
	 * 
	 * @param locale
	 *            locale
	 * @param propertyPrefix
	 *            the prefix to be used when looking up entries in the config files, e.g. "english.duration"
	 * @param featureProcessorManager
	 *            the manager to use when looking up feature processors.
	 */
	protected CARTDurationModeller(Locale locale, String propertyPrefix, FeatureProcessorManager featureProcessorManager) {
		super("CARTDurationModeller", MaryDataType.ALLOPHONES, MaryDataType.DURATIONS, locale);
		if (propertyPrefix.endsWith("."))
			this.propertyPrefix = propertyPrefix;
		else
			this.propertyPrefix = propertyPrefix + ".";
		this.featureProcessorManager = featureProcessorManager;
	}

	public void startup() throws Exception {
		super.startup();
		String cartFilename = MaryProperties.getFilename(propertyPrefix + "cart");
		if (cartFilename != null) { // there is a default model for the language
			File cartFile = new File(cartFilename);
			cart = new DirectedGraphReader().load(cartFile.getAbsolutePath());
			featureComputer = FeatureRegistry.getTargetFeatureComputer(featureProcessorManager, cart.getFeatureDefinition()
					.getFeatureNames());
		} else {
			cart = null;
		}

		String pauseFilename = MaryProperties.getFilename(propertyPrefix + "pausetree");
		if (pauseFilename != null) {
			File pauseFile = new File(pauseFilename);

			File pauseFdFile = new File(MaryProperties.needFilename(propertyPrefix + "pausefeatures"));
			FeatureDefinition pauseFeatureDefinition = new FeatureDefinition(new BufferedReader(new FileReader(pauseFdFile)),
					false);
			pauseFeatureComputer = FeatureRegistry.getTargetFeatureComputer(featureProcessorManager,
					pauseFeatureDefinition.getFeatureNames());
			pausetree = new StringPredictionTree(new BufferedReader(new FileReader(pauseFile)), pauseFeatureDefinition);
		} else {
			this.pausetree = null;
		}
	}

	public MaryData process(MaryData d) throws Exception {
		Document doc = d.getDocument();
		NodeIterator sentenceIt = MaryDomUtils.createNodeIterator(doc, MaryXML.SENTENCE);
		Element sentence = null;
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

			DirectedGraph currentCart = cart;
			TargetFeatureComputer currentFeatureComputer = featureComputer;
			if (maryVoice != null) {
				DirectedGraph voiceCart = maryVoice.getDurationGraph();
				if (voiceCart != null) {
					currentCart = voiceCart;
					logger.debug("Using voice duration graph");
					FeatureDefinition voiceFeatDef = voiceCart.getFeatureDefinition();
					currentFeatureComputer = FeatureRegistry.getTargetFeatureComputer(featureProcessorManager,
							voiceFeatDef.getFeatureNames());
				}
			}

			if (currentCart == null) {
				throw new NullPointerException("No cart for predicting duration");
			}

			// cumulative duration from beginning of sentence, in seconds:
			float end = 0;

			TreeWalker tw = MaryDomUtils.createTreeWalker(sentence, MaryXML.PHONE, MaryXML.BOUNDARY);
			Element segmentOrBoundary;
			Element previous = null;
			while ((segmentOrBoundary = (Element) tw.nextNode()) != null) {
				String phone = UnitSelector.getPhoneSymbol(segmentOrBoundary);
				Target t = new Target(phone, segmentOrBoundary);
				t.setFeatureVector(currentFeatureComputer.computeFeatureVector(t));
				float durInSeconds;
				if (segmentOrBoundary.getTagName().equals(MaryXML.BOUNDARY)) { // a pause
					durInSeconds = enterPauseDuration(segmentOrBoundary, previous, pausetree, pauseFeatureComputer);
				} else {
					float[] dur = (float[]) currentCart.interpret(t);
					assert dur != null : "Null duration";
					assert dur.length == 2 : "Unexpected duration length: " + dur.length;
					durInSeconds = dur[1];
					float stddevInSeconds = dur[0];
				}
				end += durInSeconds;
				int durInMillis = (int) (1000 * durInSeconds);
				if (segmentOrBoundary.getTagName().equals(MaryXML.BOUNDARY)) {
					segmentOrBoundary.setAttribute("duration", String.valueOf(durInMillis));
				} else { // phone
					segmentOrBoundary.setAttribute("d", String.valueOf(durInMillis));
					segmentOrBoundary.setAttribute("end", String.valueOf(end));
				}
				previous = segmentOrBoundary;
			}
		}
		MaryData output = new MaryData(outputType(), d.getLocale());
		output.setDocument(doc);
		return output;
	}

	/**
	 * 
	 * This predicts and enters the pause duration for a pause segment.
	 * 
	 * @param s
	 * @param maryVoice
	 * @return pause duration, in seconds
	 */
	private float enterPauseDuration(Element boundary, Element previous, StringPredictionTree currentPauseTree,
			TargetFeatureComputer currentPauseFeatureComputer) {
		if (!boundary.getTagName().equals(MaryXML.BOUNDARY))
			throw new IllegalArgumentException("cannot call enterPauseDuration for non-pause element");

		// If there is already a duration, keep it:
		if (boundary.hasAttribute("duration")) {
			try {
				return Float.parseFloat(boundary.getAttribute("duration")) * 0.001f;
			} catch (NumberFormatException nfe) {
			}
		}

		float duration = 0.4f; // default value

		if (previous == null || !previous.getTagName().equals(MaryXML.PHONE))
			return duration;

		if (currentPauseTree == null)
			return duration;

		assert currentPauseFeatureComputer != null;
		String phone = previous.getAttribute("p");
		Target t = new Target(phone, previous);
		t.setFeatureVector(currentPauseFeatureComputer.computeFeatureVector(t));

		String durationString = currentPauseTree.getMostProbableString(t);
		// strip off "ms"
		durationString = durationString.substring(0, durationString.length() - 2);
		try {
			duration = Float.parseFloat(durationString);
		} catch (NumberFormatException nfe) {
		}

		if (duration > 2) {
			logger.debug("Cutting long duration to 2 s -- was " + duration);
			duration = 2;
		}
		return duration;
	}
}
