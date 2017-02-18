/* ----------------------------------------------------------------- */
/*           The HMM-Based Speech Synthesis Engine "hts_engine API"  */
/*           developed by HTS Working Group                          */
/*           http://hts-engine.sourceforge.net/                      */
/* ----------------------------------------------------------------- */
/*                                                                   */
/*  Copyright (c) 2001-2010  Nagoya Institute of Technology          */
/*                           Department of Computer Science          */
/*                                                                   */
/*                2001-2008  Tokyo Institute of Technology           */
/*                           Interdisciplinary Graduate School of    */
/*                           Science and Engineering                 */
/*                                                                   */
/* All rights reserved.                                              */
/*                                                                   */
/* Redistribution and use in source and binary forms, with or        */
/* without modification, are permitted provided that the following   */
/* conditions are met:                                               */
/*                                                                   */
/* - Redistributions of source code must retain the above copyright  */
/*   notice, this list of conditions and the following disclaimer.   */
/* - Redistributions in binary form must reproduce the above         */
/*   copyright notice, this list of conditions and the following     */
/*   disclaimer in the documentation and/or other materials provided */
/*   with the distribution.                                          */
/* - Neither the name of the HTS working group nor the names of its  */
/*   contributors may be used to endorse or promote products derived */
/*   from this software without specific prior written permission.   */
/*                                                                   */
/* THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND            */
/* CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,       */
/* INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF          */
/* MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE          */
/* DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS */
/* BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,          */
/* EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED   */
/* TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,     */
/* DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON */
/* ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,   */
/* OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY    */
/* OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE           */
/* POSSIBILITY OF SUCH DAMAGE.                                       */
/* ----------------------------------------------------------------- */
/**
 * Copyright 2011 DFKI GmbH.
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

package marytts.modules.synthesis;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.StringTokenizer;
import marytts.data.Relation;
import marytts.data.SupportedSequenceType;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;

import marytts.datatypes.MaryData;
import marytts.datatypes.MaryDataType;
import marytts.datatypes.MaryXML;
import marytts.exceptions.SynthesisException;
import marytts.modeling.features.FeatureRegistry;
import marytts.modeling.features.FeatureVector;
import marytts.modeling.features.TargetFeatureComputer;
import marytts.htsengine.HMMVoice;
import marytts.modules.HTSEngine;
import marytts.modules.MaryModule;
import marytts.modules.ModuleRegistry;
import marytts.modules.acoustic.TargetFeatureLister;
import marytts.modules.synthesis.Voice.Gender;
import marytts.server.MaryProperties;
import marytts.util.MaryUtils;
import marytts.util.dom.MaryDomUtils;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.traversal.NodeIterator;
import org.w3c.dom.traversal.TreeWalker;

import marytts.data.Utterance;
import marytts.data.Relation;
import marytts.data.Sequence;
import marytts.data.item.Item;
import marytts.features.FeatureMap;
import marytts.features.FeatureComputer;

/**
 * HTS-HMM synthesiser.
 *
 * Java port and extension of HTS engine version 2.0 Extension: mixed excitation
 *
 * @author Marc Schr&ouml;der, Marcela Charfuelan
 */
public class HMMSynthesizer implements WaveformSynthesizer {
    public static final MaryDataType HTSCONTEXT = new MaryDataType("HTSCONTEXT", true, true, MaryDataType.PLAIN_TEXT);
	private TargetFeatureLister targetFeatureLister;
	private HTSEngine htsEngine;
	private Logger logger;

	// private TargetFeatureComputer comp;

	public HMMSynthesizer() {
	}

	public void startup() throws Exception {
		logger = MaryUtils.getLogger(this.toString());
		// Try to get instances of our tools from Mary; if we cannot get them,
		// instantiate new objects.

		try {
			targetFeatureLister = (TargetFeatureLister) ModuleRegistry.getModule(TargetFeatureLister.class);
		} catch (NullPointerException npe) {
			targetFeatureLister = null;
		}
		if (targetFeatureLister == null) {
			logger.info("Starting my own TargetFeatureLister");
			targetFeatureLister = new TargetFeatureLister();
			targetFeatureLister.startup();
		} else if (targetFeatureLister.getState() == MaryModule.MODULE_OFFLINE) {
			targetFeatureLister.startup();
		}

		try {
			htsEngine = (HTSEngine) ModuleRegistry.getModule(HTSEngine.class);
		} catch (NullPointerException npe) {
			htsEngine = null;
		}
		if (htsEngine == null) {
			logger.info("Starting my own HTSEngine");
			htsEngine = new HTSEngine();
			htsEngine.startup();
		} else if (htsEngine.getState() == MaryModule.MODULE_OFFLINE) {
			htsEngine.startup();
		}

		// Register HMM voices:
		List<String> voiceNames = MaryProperties.getList("hmm.voices.list");
		for (String voiceName : voiceNames) {
			logger.debug("Voice '" + voiceName + "'");

			/**
			 * When creating a HMMVoice object it should create and initialise a TreeSet ts, a ModelSet ms and load the context
			 * feature list used in this voice.
			 */

			HMMVoice v = new HMMVoice(voiceName, this);
			Voice.registerVoice(v);
		}
		logger.info("started.");

	}

	public String toString() {
		return "HMMSynthesizer";
	}

	/**
	 * {@inheritDoc}
	 */
	public AudioInputStream synthesize(Utterance utt, int sentence_index, Voice voice, String outputParams)
        throws SynthesisException
    {

		if (!voice.synthesizer().equals(this)) {
			throw new IllegalArgumentException("Voice " + voice.getName() + " is not an HMM voice.");
		}
		logger.info("Synthesizing one sentence.");

		try {
			assert voice instanceof HMMVoice : "Expected voice to be a HMMVoice, but it is a " + voice.getClass().toString();

			// -- This can be done just once when powerOnSelfTest() of this voice
			// -- mmmmmm it did not work, it takes the comp from the default voice
			// -- CHECK: do we need to do this for every call???
			String features = ((HMMVoice) voice).getHMMData().getFeatureDefinition().getFeatureNames();

			//TargetFeatureComputer comp = FeatureRegistry.getTargetFeatureComputer(voice, features);
            FeatureComputer the_feature_computer = FeatureComputer.the_feature_computer;


			MaryData d = new MaryData(targetFeatureLister.outputType(), voice.getLocale());
			d.setDefaultVoice(voice);

            Relation rel_sent_phones = utt.getRelation(SupportedSequenceType.SENTENCE, SupportedSequenceType.PHONE);
            ArrayList<Item> items = (ArrayList<Item>) rel_sent_phones.getRelatedItems(sentence_index);
			List<FeatureMap> targetFeaturesList = targetFeatureLister.getListTargetFeatures(the_feature_computer, utt, items);

			// the actual durations are already fixed in the htsEngine.process()
			// here i pass segements and boundaries to update the realised acoustparams, dur and f0
			MaryData audio = htsEngine.process(d, targetFeaturesList, utt);

			return audio.getAudio();

		} catch (Exception e) {
			throw new SynthesisException("HMM Synthesiser could not synthesise: ", e);
		}
	}

}
