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

package marytts.htsengine;

import java.util.Locale;

import javax.sound.sampled.AudioFormat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;


import java.util.HashMap;
import java.util.StringTokenizer;

import marytts.config.MaryConfig;
import marytts.modules.synthesis.Voice;
import marytts.modules.synthesis.WaveformSynthesizer;
import marytts.server.MaryProperties;
import marytts.util.MaryUtils;
import marytts.modules.acoustic.BoundaryModel;
import marytts.modules.acoustic.Model;
import marytts.modules.acoustic.ModelType;
import marytts.modules.acoustic.HMMModel;
import marytts.exceptions.MaryConfigurationException;
import marytts.exceptions.NoSuchPropertyException;

import marytts.features.FeatureProcessorManager;
import marytts.features.FeatureRegistry;

import org.apache.log4j.Logger;

public class HMMVoice extends Voice {

	private HMMData htsData = new HMMData();
	private Logger logger = MaryUtils.getLogger("HMMVoice");

	/**
	 * constructor
	 *
	 * @param voiceName
	 *            voiceName
	 * @param synthesizer
	 *            synthesizer
	 * @throws Exception
	 *             Exception
	 */
	public HMMVoice(String voiceName, WaveformSynthesizer synthesizer) throws Exception {
		super(voiceName, synthesizer);

		htsData.initHMMData(voiceName);

	}

	public HMMData getHMMData() {
		return this.htsData;
	}

	/* set parameters for generation: f0Std, f0Mean and length, default values 1.0, 0.0 and 0.0 */
	/* take the values from audio effects component through a MaryData object */
	public void setF0Std(double dval) {
		htsData.setF0Std(dval);
	}

	public void setF0Mean(double dval) {
		htsData.setF0Mean(dval);
	}

	public void setLength(double dval) {
		htsData.setLength(dval);
	}

	public void setDurationScale(double dval) {
		htsData.setDurationScale(dval);
	}


	/**
	 * Load a flexibly configurable list of acoustic models as specified in the config file.
	 *
	 * @param header
	 *            header
	 * @throws MaryConfigurationException
	 *             MaryConfigurationException
	 * @throws NoSuchPropertyException
	 *             NoSuchPropertyException
	 * @throws IOException
	 *             IOException
	 */
	protected void loadAcousticModels(String header) throws MaryConfigurationException, NoSuchPropertyException, IOException {
		// The feature processor manager that all acoustic models will use to predict their acoustics:
		FeatureProcessorManager symbolicFPM = FeatureRegistry.determineBestFeatureProcessorManager(getLocale());

		// Acoustic models:
		String acousticModelsString = MaryProperties.getProperty(header + ".acousticModels");
		if (acousticModelsString != null) {
			acousticModels = new HashMap<String, Model>();

			// add boundary "model" (which could of course be overwritten by appropriate properties in voice config):
			acousticModels.put("boundary", new BoundaryModel(symbolicFPM, getName(), null, "duration", null, null, null,
                                                             "boundaries"));

			StringTokenizer acousticModelStrings = new StringTokenizer(acousticModelsString);
			do {
				String modelName = acousticModelStrings.nextToken();

				// get more properties from voice config, depending on the model name:
				String modelType = MaryProperties.needProperty(header + "." + modelName + ".model");

				InputStream modelDataStream = MaryProperties.getStream(header + "." + modelName + ".data"); // not used for hmm
																											// models
				String modelAttributeName = MaryProperties.needProperty(header + "." + modelName + ".attribute");

				// the following are null if not defined; this is handled in the Model constructor:
				String modelAttributeFormat = MaryProperties.getProperty(header + "." + modelName + ".attribute.format");
				String modelFeatureName = MaryProperties.getProperty(header + "." + modelName + ".feature");
				String modelPredictFrom = MaryProperties.getProperty(header + "." + modelName + ".predictFrom");
				String modelApplyTo = MaryProperties.getProperty(header + "." + modelName + ".applyTo");

				// consult the ModelType enum to find appropriate Model subclass...
				ModelType possibleModelTypes = ModelType.fromString(modelType);
				// if modelType is not in ModelType.values(), we don't know how to handle it:
				if (possibleModelTypes == null) {
					throw new MaryConfigurationException("Cannot handle unknown model type: " + modelType);
				}

				// ...and instantiate it in a switch statement:
				Model model = null;
				try {

                    // if we already have a HMM duration or F0 model, and if this is the other of the two, and if so,
                    // and they use the same dataFile, then let them be the same instance:
                    // if this is the case set the boolean variable predictDurAndF0 to true in HMMModel
                    if (getDurationModel() != null && getDurationModel() instanceof HMMModel
                        && modelName.equalsIgnoreCase("F0") && getName().equals(getDurationModel().getVoiceName())) {
                        model = getDurationModel();
                        ((HMMModel) model).setPredictDurAndF0(true);
                    } else if (getF0Model() != null && getF0Model() instanceof HMMModel
                               && modelName.equalsIgnoreCase("duration") && getName().equals(getF0Model().getVoiceName())) {
                        model = getF0Model();
                        ((HMMModel) model).setPredictDurAndF0(true);
                    } else {
                        model = new HMMModel(symbolicFPM, getName(), modelDataStream, modelAttributeName,
                                             modelAttributeFormat, modelFeatureName, modelPredictFrom, modelApplyTo);
                    }
                } catch (Throwable t) {
                    throw new MaryConfigurationException("Cannot instantiate model '" + modelName + "' of type '" + modelType
                                                         + "' from '" + MaryProperties.getProperty(header + "." + modelName + ".data") + "'", t);
                }

                // if we got this far, model should not be null:
                assert model != null;

                // put the model in the Model Map:
                acousticModels.put(modelName, model);
            } while (acousticModelStrings.hasMoreTokens());
        }
    }


    public String toString()
    {
        return getName() + " " + getLocale() + " " + gender().toString() + " " + "hmm";
    }


    public String getType()
    {
        return "hmm";
    }

} /* class HMMVoice */
