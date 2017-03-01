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

package marytts.modules.acoustic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import marytts.datatypes.MaryData;
import marytts.datatypes.MaryXML;
import marytts.exceptions.MaryConfigurationException;
import marytts.exceptions.SynthesisException;
import marytts.modeling.features.FeatureProcessorManager;
import marytts.modeling.features.FeatureRegistry;
import marytts.modules.acoustic.model.Model;
import marytts.modules.nlp.phonemiser.Allophone;
import marytts.modules.nlp.phonemiser.AllophoneSet;
import marytts.modules.synthesis.Voice;
import marytts.util.MaryRuntimeUtils;
import marytts.util.MaryUtils;
import marytts.util.dom.MaryDomUtils;

import marytts.modules.InternalModule;

import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.traversal.TreeWalker;

import marytts.data.Utterance;
import marytts.data.SupportedSequenceType;
import marytts.io.XMLSerializer;

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
		super("AcousticModeller", locale);
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
		super("AcousticModeller", locale);
	}

	public MaryData process(MaryData d) throws SynthesisException {
        Utterance utt = d.getData();
		MaryData output = new MaryData(d.getLocale());
        try {

		// cascaded voice identification:
		Voice voice = null;
		if (voice == null) {
			// Determine Locale in order to use default voice
			Locale locale = utt.getLocale();
			voice = Voice.getDefaultVoice(locale);
		}

		// if no voice can be found for the Locale
		if (voice == null) {
			logger.debug("No voice found for locale; could not process!");
            return d;
		}
		assert voice != null;

		// get models from voice, if they are defined:
		Map<String, Model> models = voice.getAcousticModels();
		if (models == null) {
			// unless voice provides suitable models, pass out unmodified MaryXML, just like DummyAllophones2AcoustParams:
			logger.debug("No acoustic models defined in " + voice.getName() + "; could not process!");
            return d;
		}
		assert models != null;

        ArrayList<Integer> indexes = new ArrayList<Integer>();
        int size = utt.getSequence(SupportedSequenceType.PHONE).size();
        for (int i=0; i<size; i++)
            indexes.add(i);


		// apply critical Models to Elements:
		Model durationModel = voice.getDurationModel();
		if (durationModel == null) {
			throw new SynthesisException("No duration model available for voice " + voice);
		}
		try {
			durationModel.applyTo(utt, SupportedSequenceType.PHONE, indexes); // Note that this assumes that Elements always predict their own duration!
		} catch (MaryConfigurationException e) {
			throw new SynthesisException("Duration model could not be applied", e);
		}

		Model f0Model = voice.getF0Model();
		if (f0Model == null) {
			throw new SynthesisException("No F0 model available for voice " + voice);
		}
		try {
			f0Model.applyTo(utt, SupportedSequenceType.PHONE, indexes);
		} catch (MaryConfigurationException e) {
			throw new SynthesisException("Could not apply F0 model", e);
		}

		// Model boundaryModel = voice.getBoundaryModel();
		// if (boundaryModel == null) {
		// 	throw new SynthesisException("No boundary model available for voice " + voice);
		// }
		// try {
		// 	List<Element> boundaryElements = elementLists.get(boundaryModel.getApplyTo());
		// 	if (boundaryElements == null) {
		// 		throw new SynthesisException("Could not determine to which Elements to apply boundary model!");
		// 	}
		// 	voice.getBoundaryModel().applyTo(boundaryElements);
		// } catch (MaryConfigurationException e) {
		// 	throw new SynthesisException("Could not apply boundary model", e);
		// }

        output.setData(utt);

        } catch (Exception ex) {
            throw new SynthesisException(ex);
        }
		return output;
	}
}
