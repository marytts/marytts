/**
 * Copyright 2000-2008 DFKI GmbH.
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
package marytts.language.de.features;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import marytts.exceptions.MaryConfigurationException;
import marytts.features.MaryGenericFeatureProcessors;
import marytts.features.MaryLanguageFeatureProcessors;
import marytts.modules.phonemiser.AllophoneSet;
import marytts.modules.synthesis.Voice;
import marytts.server.MaryProperties;
import marytts.util.MaryRuntimeUtils;

public class FeatureProcessorManager extends marytts.features.FeatureProcessorManager {

	/**
	 * Builds a new manager. This manager uses the english phoneset of FreeTTS and a PoS conversion file if the english PoS tagger
	 * is used. All feature processors loaded are language specific.
	 */
	public FeatureProcessorManager() {
		super();
		setupAdditionalFeatureProcessors();
	}

	/**
	 * Constructor called from a Voice in Locale DE that has its own acoustic models
	 * 
	 * @param voice
	 *            voice
	 * @throws MaryConfigurationException
	 *             MaryConfigurationException
	 */
	public FeatureProcessorManager(Voice voice) throws MaryConfigurationException {
		super(voice.getLocale());
		setupAdditionalFeatureProcessors();
		registerAcousticModels(voice);
	}

	/**
	 * specific stuff, moved here so that it can be called by more than one constructor without unnecessary code duplication
	 */
	private void setupAdditionalFeatureProcessors() {
		try {

			String[] pos = { "0", "NN", "NE", "NNm", "ADJA", "ADJD", "CARD", "CARDj", "ORD", "ITJ", "PDS", "PPOSS", "TRUNC",
					"ADV", "ADVq", "ADVf", "APPR", "APPRbis", "APPRART", "APPO", "APZR", "ART", "ARTdna", "ARTg", "ARTngd", "FM",
					"KOUI", "KOUS", "KON", "KOKOM", "KOKOMa", "PDAT", "PIS", "PIAT", "PIDAT", "PIDAT2", "PPER", "PPOSAT",
					"PRELS", "PRELAT", "PRF", "PWS", "PWAT", "PWAV", "PAV", "PTKZU", "PTKNEG", "PTKVZ", "PTKANT", "PTKA", "SGML",
					"SPELL", "VVFIN", "VVIMP", "VVINF", "VVIZU", "VVPP", "VV", "VAFIN", "VAIMP", "VAINF", "VAPP", "VMFIN",
					"VMINF", "VMPP", "XY" };
			addFeatureProcessor(new MaryLanguageFeatureProcessors.Pos(pos));

			// TODO: dummy code, replace with something useful:
			Map<String, String> posConverter = new HashMap<String, String>();
			addFeatureProcessor(new MaryLanguageFeatureProcessors.Gpos(posConverter));

			// property is set in de.config
			AllophoneSet allophoneSet = MaryRuntimeUtils.needAllophoneSet("de.allophoneset");

			// Phonetic features of the current segment:
			String[] phones = allophoneSet.getAllophoneNames().toArray(new String[0]);
			String[] phoneValues = new String[phones.length + 1];
			phoneValues[0] = "0";
			System.arraycopy(phones, 0, phoneValues, 1, phones.length);

			setupHardcodedPhoneFeatureValues();
			// Adding uvular consonant place for German:
			// cplace: 0-n/a l-labial a-alveolar p-palatal b-labio_dental d-dental v-velar u-uvular g-?
			String[] cplaceValues = new String[] { "0", "l", "a", "p", "b", "d", "v", "u", "g" };
			phonefeatures2values.put("cplace", cplaceValues);

			String pauseSymbol = allophoneSet.getSilence().name();
			setupPhoneFeatureProcessors(allophoneSet, phoneValues, pauseSymbol, phonefeatures2values);

			String wordFrequencyFilename = MaryProperties.getProperty("de.wordFrequency.fst");
			InputStream wordFrequencyStream = MaryProperties.getStream("de.wordFrequency.fst");
			String wordFrequencyEncoding = MaryProperties.getProperty("de.wordFrequency.encoding");
			addFeatureProcessor(new MaryLanguageFeatureProcessors.WordFrequency(wordFrequencyStream, wordFrequencyFilename,
					wordFrequencyEncoding));

			/* for database selection */
			String[] phoneClasses = new String[] { "0", "c_labial", "c_alveolar", "c_palatal", "c_labiodental", "c_dental",
					"c_velar", "c_glottal", "c_uvular", "v_i", "v_u", "v_O", "v_E", "v_EI", "v_@", "v_aU", "v_6", "v_~", "v_a",
					"v_y", "v_2", "v_e", "v_o", "v_9", "v_OY", "v_Ya", "v_aI" };
			// map from phones to their classes
			Map<String, String> phone2Classes = new HashMap<String, String>();
			// put in vowels
			phone2Classes.put("I", "v_i");
			phone2Classes.put("i", "v_i");
			phone2Classes.put("i:", "v_i");
			phone2Classes.put("U", "v_u");
			phone2Classes.put("u", "v_u");
			phone2Classes.put("u:", "v_u");
			phone2Classes.put("O", "v_O");
			phone2Classes.put("E", "v_E");
			phone2Classes.put("E:", "v_E");
			phone2Classes.put("EI", "v_EI");
			phone2Classes.put("@", "v_@");
			phone2Classes.put("aU", "v_aU");
			phone2Classes.put("6", "v_6");
			phone2Classes.put("a~", "v_~");
			phone2Classes.put("e~", "v_~");
			phone2Classes.put("o~", "v_~");
			phone2Classes.put("9~", "v_~");
			phone2Classes.put("a", "v_a");
			phone2Classes.put("a:", "v_a");
			phone2Classes.put("y", "v_y");
			phone2Classes.put("y:", "v_y");
			phone2Classes.put("Y", "v_y");
			phone2Classes.put("2", "v_2");
			phone2Classes.put("2:", "v_2");
			phone2Classes.put("e", "v_e");
			phone2Classes.put("e:", "v_e");
			phone2Classes.put("o", "v_o");
			phone2Classes.put("o:", "v_o");
			phone2Classes.put("9", "v_9");
			phone2Classes.put("OY", "v_OY");
			phone2Classes.put("Ya", "v_Ya");
			phone2Classes.put("aI", "v_aI");

			// put in consonants
			phone2Classes.put("b", "c_labial");
			phone2Classes.put("m", "c_labial");
			phone2Classes.put("p", "c_labial");
			phone2Classes.put("w", "c_labial");
			phone2Classes.put("pf", "c_labial");
			phone2Classes.put("d", "c_alveolar");
			phone2Classes.put("l", "c_alveolar");
			phone2Classes.put("n", "c_alveolar");
			phone2Classes.put("r", "c_alveolar");
			phone2Classes.put("s", "c_alveolar");
			phone2Classes.put("t", "c_alveolar");
			phone2Classes.put("z", "c_alveolar");
			phone2Classes.put("ts", "c_alveolar");
			phone2Classes.put("tS", "c_palatal");
			phone2Classes.put("S", "c_palatal");
			phone2Classes.put("j", "c_palatal");
			phone2Classes.put("Z", "c_palatal");
			phone2Classes.put("f", "c_labiodental");
			phone2Classes.put("v", "c_labiodental");
			phone2Classes.put("D", "c_dental");
			phone2Classes.put("T", "c_dental");
			phone2Classes.put("g", "c_velar");
			phone2Classes.put("k", "c_velar");
			phone2Classes.put("N", "c_velar");
			phone2Classes.put("C", "c_velar");
			phone2Classes.put("x", "c_uvular");
			phone2Classes.put("R", "c_uvular");
			phone2Classes.put("h", "c_glottal");
			phone2Classes.put("?", "c_glottal");
			phone2Classes.put("_", "0");

			MaryGenericFeatureProcessors.TargetElementNavigator nextSegment = new MaryGenericFeatureProcessors.NextSegmentNavigator();
			MaryGenericFeatureProcessors.TargetElementNavigator nextWord = new MaryGenericFeatureProcessors.NextWordNavigator();
			MaryGenericFeatureProcessors.TargetElementNavigator firstSegNextWord = new MaryGenericFeatureProcessors.FirstSegmentNextWordNavigator();

			addFeatureProcessor(new MaryLanguageFeatureProcessors.Selection_PhoneClass(phone2Classes, phoneClasses, nextSegment));

			addFeatureProcessor(new MaryLanguageFeatureProcessors.Pos("next_pos", pos, nextWord));

			// features of first segment in next word
			addFeatureProcessor(new MaryLanguageFeatureProcessors.PhoneFeature(allophoneSet, "next_wordbegin_cplace", "cplace",
					cplaceValues, "_", firstSegNextWord));
			addFeatureProcessor(new MaryLanguageFeatureProcessors.PhoneFeature(allophoneSet, "next_wordbegin_ctype", "ctype",
					phonefeatures2values.get("ctype"), "_", firstSegNextWord));
			/*
			 * processors_en.put("seg_coda_fric", new LanguageFeatureProcessors.SegCodaFric(phoneSet));
			 * processors_en.put("seg_onset_fric", new LanguageFeatureProcessors.SegOnsetFric(phoneSet));
			 * 
			 * processors_en.put("seg_coda_stop", new LanguageFeatureProcessors.SegCodaStop(phoneSet));
			 * processors_en.put("seg_onset_stop", new LanguageFeatureProcessors.SegOnsetStop(phoneSet));
			 * 
			 * processors_en.put("seg_coda_nasal", new LanguageFeatureProcessors.SegCodaNasal(phoneSet));
			 * processors_en.put("seg_onset_nasal", new LanguageFeatureProcessors.SegOnsetNasal(phoneSet));
			 * 
			 * processors_en.put("seg_coda_glide", new LanguageFeatureProcessors.SegCodaGlide(phoneSet));
			 * processors_en.put("seg_onset_glide", new LanguageFeatureProcessors.SegOnsetGlide(phoneSet));
			 * 
			 * processors_en.put("syl_codasize", new LanguageFeatureProcessors.SylCodaSize(phoneSet));
			 * processors_en.put("syl_onsetsize", new LanguageFeatureProcessors.SylOnsetSize(phoneSet));
			 * processors_en.put("accented", new GenericFeatureProcessors.Accented());
			 * 
			 * processors_en.put("token_pos_guess", new LanguageFeatureProcessors.TokenPosGuess());
			 */
		} catch (Exception e) {
			e.printStackTrace();
			throw new Error("Problem building Pos or PhoneSet");
		}
	}

	@Override
	public Locale getLocale() {
		return Locale.GERMAN;
	}

}
