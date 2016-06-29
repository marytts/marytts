/**
 * Copyright 2010 DFKI GmbH.
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import marytts.datatypes.MaryData;
import marytts.datatypes.MaryDataType;
import marytts.datatypes.MaryXML;
import marytts.exceptions.MaryConfigurationException;
import marytts.exceptions.SynthesisException;
import marytts.features.FeatureProcessorManager;
import marytts.features.FeatureRegistry;
import marytts.modules.acoustic.Model;
import marytts.modules.acoustic.ProsodyElementHandler;
import marytts.modules.phonemiser.Allophone;
import marytts.modules.phonemiser.AllophoneSet;
import marytts.modules.synthesis.Voice;
import marytts.unitselection.select.UnitSelector;
import marytts.util.MaryRuntimeUtils;
import marytts.util.MaryUtils;
import marytts.util.dom.MaryDomUtils;

import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.traversal.TreeWalker;

/**
 * Predict duration and F0 using CARTs or other models
 *
 * @author steiner
 *
 */
public class AcousticModeller extends InternalModule {

	// three constructors adapted from DummyAllophones2AcoustParams (used if this is in modules.classes.list):

	public AcousticModeller() {
		this((Locale) null);
	}

	/**
	 * Constructor to be called with instantiated objects.
	 *
	 * @param locale
	 *            locale
	 */
	public AcousticModeller(String locale) {
		this(MaryUtils.string2locale(locale));
	}

	/**
	 * Constructor to be called with instantiated objects.
	 *
	 * @param locale
	 *            locale
	 */
	public AcousticModeller(Locale locale) {
		super("AcousticModeller", MaryDataType.ALLOPHONES, MaryDataType.ACOUSTPARAMS, locale);
	}

	// three constructors adapted from CARTF0Modeller (used if this is in a voice's preferredModules):

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
	public AcousticModeller(String locale, String propertyPrefix) throws Exception {
		this(MaryUtils.string2locale(locale), propertyPrefix,
				FeatureRegistry.getFeatureProcessorManager(MaryUtils.string2locale(locale)));
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
	public AcousticModeller(String locale, String propertyPrefix, String featprocClassInfo) throws Exception {
		this(MaryUtils.string2locale(locale), propertyPrefix,
				(FeatureProcessorManager) MaryRuntimeUtils.instantiateObject(featprocClassInfo));
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
	protected AcousticModeller(Locale locale, String propertyPrefix, FeatureProcessorManager featureProcessorManager) {
		super("AcousticModeller", MaryDataType.ALLOPHONES, MaryDataType.ACOUSTPARAMS, locale);
	}

	public MaryData process(MaryData d) throws SynthesisException {
		Document doc = d.getDocument();
		MaryData output = new MaryData(outputType(), d.getLocale());

		// cascaded voice identification:
		Element voiceElement = (Element) doc.getElementsByTagName(MaryXML.VOICE).item(0);
		Voice voice = Voice.getVoice(voiceElement);
		if (voice == null) {
			voice = d.getDefaultVoice();
		}
		if (voice == null) {
			// Determine Locale in order to use default voice
			Locale locale = MaryUtils.string2locale(doc.getDocumentElement().getAttribute("xml:lang"));
			voice = Voice.getDefaultVoice(locale);
		}

		// if no voice can be found for the Locale
		if (voice == null) {
			logger.debug("No voice found for locale; could not process!");
			output.setDocument(doc);
			return output;
		}
		assert voice != null;

		// get models from voice, if they are defined:
		Map<String, Model> models = voice.getAcousticModels();
		if (models == null) {
			// unless voice provides suitable models, pass out unmodified MaryXML, just like DummyAllophones2AcoustParams:
			logger.debug("No acoustic models defined in " + voice.getName() + "; could not process!");
			output.setDocument(doc);
			return output;
		}
		assert models != null;

		/*
		 * Actual processing below here; applies only when Voice provides appropriate models:
		 */

		// parse the MaryXML Document to populate Lists of relevant Elements:
		Map<String, List<Element>> elementLists = parseDocument(doc);

		// apply critical Models to Elements:
		Model durationModel = voice.getDurationModel();
		if (durationModel == null) {
			throw new SynthesisException("No duration model available for voice " + voice);
		}
		List<Element> durationElements = elementLists.get(durationModel.getApplyTo());
		if (durationElements == null) {
			throw new SynthesisException("Could not determine to which Elements to apply duration model!");
		}
		try {
			durationModel.applyTo(durationElements); // Note that this assumes that Elements always predict their own duration!
		} catch (MaryConfigurationException e) {
			throw new SynthesisException("Duration model could not be applied", e);
		}

		// hack duration attributes:
		// IMPORTANT: this hack has to be done right after predict durations,
		// because the dur value is used by the HMMs, in case of prediction of f0.
		hackSegmentDurations(durationElements);

		// TODO this should be reduced further to the point where any HMM-specific stuff is handled opaquely within HMMModel
		// finally we can then pass elementLists into Model.apply and the Model will know which Element Lists to process
		/*
		 * Model f0Model = voice.getF0Model(); if (f0Model instanceof HMMModel) { ((HMMModel)
		 * f0Model).evaluate(elementLists.get(f0Model.getApplyTo())); } else {
		 * f0Model.applyFromTo(elementLists.get(f0Model.getPredictFrom()), elementLists.get(f0Model.getApplyTo())); }
		 */
		Model f0Model = voice.getF0Model();
		if (f0Model == null) {
			throw new SynthesisException("No F0 model available for voice " + voice);
		}
		try {
			List<Element> predictFromElements = elementLists.get(f0Model.getPredictFrom());
			List<Element> applyToElements = elementLists.get(f0Model.getApplyTo());
			if (predictFromElements == null || applyToElements == null) {
				throw new SynthesisException("Could not determine to which Elements to apply F0 model!");
			}
			f0Model.applyFromTo(predictFromElements, applyToElements);
		} catch (MaryConfigurationException e) {
			throw new SynthesisException("Could not apply F0 model", e);
		}

		Model boundaryModel = voice.getBoundaryModel();
		if (boundaryModel == null) {
			throw new SynthesisException("No boundary model available for voice " + voice);
		}
		try {
			List<Element> boundaryElements = elementLists.get(boundaryModel.getApplyTo());
			if (boundaryElements == null) {
				throw new SynthesisException("Could not determine to which Elements to apply boundary model!");
			}
			voice.getBoundaryModel().applyTo(boundaryElements);
		} catch (MaryConfigurationException e) {
			throw new SynthesisException("Could not apply boundary model", e);
		}

		// apply other Models, if applicable:
		Map<String, Model> otherModels = voice.getOtherModels();
		if (otherModels != null && !otherModels.isEmpty()) {
			for (String modelName : otherModels.keySet()) {
				Model model = models.get(modelName);
				if (model == null) {
					throw new SynthesisException("Cannot apply invalid model");
				}
				try {
					List<Element> predictFromElements = elementLists.get(model.getPredictFrom());
					List<Element> applyToElements = elementLists.get(model.getApplyTo());
					if (predictFromElements == null || applyToElements == null) {
						throw new SynthesisException("Could not determine to which Elements to apply model '" + modelName + "'");
					}
					// remember, the Model constructor will predict from, and apply the model to, "segments" by default
					model.applyFromTo(predictFromElements, applyToElements);
				} catch (MaryConfigurationException e) {
					throw new SynthesisException("Could not apply model '" + modelName + "'", e);
				}
			}
		}

		// Once prosody values are predicted apply modifications if any
		logger.debug("\nApplying prosody modification if any:");
		ProsodyElementHandler prosodyHandler = new ProsodyElementHandler();
		// TODO catch exceptions thrown by prosodyHandler:
		prosodyHandler.process(doc);

		output.setDocument(doc);

		return output;
	}

	/**
	 * Hack duration attributes so that <code>d</code> attribute values are in milliseconds, and add <code>end</code> attributes
	 * containing the cumulative end time.
	 *
	 * @param elements
	 *            a List of segment Elements
	 */
	private void hackSegmentDurations(List<Element> elements) {
		assert elements != null;
		float cumulEndInSeconds = 0;
		for (Element segment : elements) {
			float durationInSeconds = Float.parseFloat(segment.getAttribute("d"));
			cumulEndInSeconds += durationInSeconds;

			// cumulative end time in seconds:
			String endStr = Float.toString(cumulEndInSeconds);
			segment.setAttribute("end", endStr);

			// duration rounded to milliseconds:
			String durationInMilliseconds = String.format("%.0f", (durationInSeconds * 1000));
			segment.setAttribute("d", durationInMilliseconds);
		}
	}

	/**
	 * Parse the Document to populate the Lists of Elements
	 *
	 * @param doc
	 *            the Document to parse
	 * @return A Map of Lists of Elements, accessible by keys such as "segments", etc.
	 * @throws SynthesisException
	 *             if the Document or some of the relevant Elements cannot be parsed properly
	 */
	private Map<String, List<Element>> parseDocument(Document doc) throws SynthesisException {

		// initialize Element Lists:
		Map<String, List<Element>> elementLists = new HashMap<String, List<Element>>();
		List<Element> segments = new ArrayList<Element>();
		List<Element> boundaries = new ArrayList<Element>();
		List<Element> firstVoicedSegments = new ArrayList<Element>();
		List<Element> firstVowels = new ArrayList<Element>();
		List<Element> lastVoicedSegments = new ArrayList<Element>();
		List<Element> voicedSegments = new ArrayList<Element>();

		// walk over all syllables in MaryXML document:
		TreeWalker treeWalker = null;
		try {
			treeWalker = MaryDomUtils.createTreeWalker(doc, MaryXML.SYLLABLE, MaryXML.BOUNDARY);
		} catch (DOMException e) {
			throw new SynthesisException("Could not parse XML Document", e);
		}
		Node node;
		while ((node = treeWalker.nextNode()) != null) {
			assert node != null;
			Element element = (Element) node;

			// handle boundaries
			if (node.getNodeName().equals(MaryXML.BOUNDARY)) {
				boundaries.add(element);
				continue;
			}

			// from this point on, we should be dealing only with syllables:
			assert node.getNodeName().equals(MaryXML.SYLLABLE);

			// get AllophoneSet for syllable
			AllophoneSet allophoneSet = null; // TODO should this be here, or rather outside the loop?
			try {
				allophoneSet = MaryRuntimeUtils.determineAllophoneSet(element);
			} catch (MaryConfigurationException e) {
				throw new SynthesisException("Could not determine AllophoneSet", e);
			}
			assert allophoneSet != null;

			// initialize some variables:
			Element segment;
			Element firstVoicedSegment = null;
			Element firstVowel = null;
			Element lastVoicedSegment = null;

			// iterate over "ph" children of syllable
			for (segment = MaryDomUtils.getFirstElementByTagName(node, MaryXML.PHONE); segment != null; segment = MaryDomUtils
					.getNextOfItsKindIn(segment, element)) {
				assert segment != null;

				// in passing, append segment to segments List:
				segments.add(segment);

				// get "p" attribute...
				String phone = UnitSelector.getPhoneSymbol(segment);
				if (phone.length() == 0) {
					throw new SynthesisException("No phone found for segment " + segment);
				}

				// ...and get the corresponding allophone, which knows about its phonological features:
				Allophone allophone;
				try {
					allophone = allophoneSet.getAllophone(phone);
				} catch (IllegalArgumentException e) {
					throw new SynthesisException(e);
				}

				if (allophone.isVoiced()) { // all and only voiced segments are potential F0 anchors
					voicedSegments.add(segment);
					if (firstVoicedSegment == null) {
						firstVoicedSegment = segment;
					}
					if (firstVowel == null && allophone.isVowel()) {
						firstVowel = segment;
					}
					lastVoicedSegment = segment; // keep overwriting this; finally it's the last voiced segment
				}
			}

			// at this point, no TBU should be null:
			if (firstVoicedSegment == null || firstVowel == null || lastVoicedSegment == null) {
				logger.debug(
						"WARNING: could not identify F0 anchors in malformed syllable: '" + element.getAttribute("ph") + "'");
			} else {
				// we have what we need, append to Lists:
				firstVoicedSegments.add(firstVoicedSegment);
				firstVowels.add(firstVowel);
				lastVoicedSegments.add(lastVoicedSegment);
			}
		}

		// pack the Element Lists into the Map:
		elementLists.put("segments", segments);
		elementLists.put("voicedSegments", voicedSegments);
		elementLists.put("firstVoicedSegments", firstVoicedSegments);
		elementLists.put("firstVowels", firstVowels);
		elementLists.put("lastVoicedSegments", lastVoicedSegments);
		elementLists.put("boundaries", boundaries);
		return elementLists;
	}

}
