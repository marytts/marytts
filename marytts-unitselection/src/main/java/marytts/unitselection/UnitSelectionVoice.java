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
package marytts.unitselection;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.util.Locale;

import java.util.HashMap;
import java.util.StringTokenizer;


import javax.sound.sampled.AudioFormat;

import marytts.cart.CART;
import marytts.cart.io.MaryCARTReader;
import marytts.exceptions.MaryConfigurationException;
import marytts.exceptions.NoSuchPropertyException;
import marytts.features.FeatureDefinition;
import marytts.features.FeatureProcessorManager;
import marytts.features.FeatureRegistry;
import marytts.modules.synthesis.Voice;
import marytts.modules.synthesis.WaveformSynthesizer;
import marytts.server.MaryProperties;
import marytts.unitselection.concat.FdpsolaUnitConcatenator;
import marytts.unitselection.concat.UnitConcatenator;
import marytts.unitselection.data.TimelineReader;
import marytts.unitselection.data.UnitDatabase;
import marytts.unitselection.data.UnitFileReader;
import marytts.unitselection.select.JoinCostFunction;
import marytts.unitselection.select.JoinModelCost;
import marytts.unitselection.select.StatisticalCostFunction;
import marytts.unitselection.select.TargetCostFunction;
import marytts.unitselection.select.UnitSelector;

import marytts.modules.acoustic.Model;
import marytts.modules.acoustic.BoundaryModel;
import marytts.modules.acoustic.ModelType;
import marytts.modules.acoustic.CARTModel;

/**
 * A Unit Selection Voice
 *
 */
public class UnitSelectionVoice extends Voice {

	protected UnitDatabase database;
	protected UnitSelector unitSelector;
	protected UnitConcatenator concatenator;
	protected UnitConcatenator modificationConcatenator;
	protected String name;
	protected CART[] f0Carts;

	public UnitSelectionVoice(String name, WaveformSynthesizer synthesizer) throws MaryConfigurationException {
		super(name, synthesizer);

		try {
			this.name = name;
			String header = "voice." + name;

			FeatureProcessorManager featProcManager = FeatureRegistry.getFeatureProcessorManager(this);
			if (featProcManager == null)
				featProcManager = FeatureRegistry.getFeatureProcessorManager(getLocale());
			if (featProcManager == null)
				throw new MaryConfigurationException("No feature processor manager for voice '" + name + "' (locale "
                                                     + getLocale() + ")");

			// build and load targetCostFunction
			logger.debug("...loading target cost function...");
			String featureFileName = MaryProperties.needFilename(header + ".featureFile");
			InputStream targetWeightStream = MaryProperties.getStream(header + ".targetCostWeights");
			String targetCostClass = MaryProperties.needProperty(header + ".targetCostClass");
			TargetCostFunction targetFunction = (TargetCostFunction) Class.forName(targetCostClass).newInstance();
			targetFunction.load(featureFileName, targetWeightStream, featProcManager);

			// build joinCostFunction
			logger.debug("...loading join cost function...");
			String joinCostClass = MaryProperties.needProperty(header + ".joinCostClass");
			JoinCostFunction joinFunction = (JoinCostFunction) Class.forName(joinCostClass).newInstance();
			if (joinFunction instanceof JoinModelCost) {
				((JoinModelCost) joinFunction).setFeatureDefinition(targetFunction.getFeatureDefinition());
			}
			joinFunction.init(header);

			// build sCost function
			StatisticalCostFunction sCostFunction = null;
			boolean useSCost = MaryProperties.getBoolean(header + ".useSCost", false);
			if (useSCost) {
				logger.debug("...loading scost function...");
				String sCostClass = MaryProperties.needProperty(header + ".sCostClass");
				sCostFunction = (StatisticalCostFunction) Class.forName(sCostClass).newInstance();
				sCostFunction.init(header);
			}

			// Build the various file readers
			logger.debug("...loading units file...");
			String unitReaderClass = MaryProperties.needProperty(header + ".unitReaderClass");
			String unitsFile = MaryProperties.needFilename(header + ".unitsFile");
			UnitFileReader unitReader = (UnitFileReader) Class.forName(unitReaderClass).newInstance();
			unitReader.load(unitsFile);

			logger.debug("...loading cart file...");
			// String cartReaderClass = MaryProperties.needProperty(header+".cartReaderClass");
			InputStream cartStream = MaryProperties.needStream(header + ".cartFile");
			CART cart = new MaryCARTReader().loadFromStream(cartStream);
			cartStream.close();
			// get the backtrace information
			int backtrace = MaryProperties.getInteger(header + ".cart.backtrace", 100);

			logger.debug("...loading audio time line...");
			String timelineReaderClass = MaryProperties.needProperty(header + ".audioTimelineReaderClass");
			String timelineFile = MaryProperties.needFilename(header + ".audioTimelineFile");
			Class<? extends TimelineReader> theClass = Class.forName(timelineReaderClass).asSubclass(TimelineReader.class);
			// Now invoke Constructor with one String argument
			Class<String>[] constructorArgTypes = new Class[] { String.class };
			Object[] args = new Object[] { timelineFile };
			Constructor<? extends TimelineReader> constructor = (Constructor<? extends TimelineReader>) theClass
                .getConstructor(constructorArgTypes);
			TimelineReader timelineReader = constructor.newInstance(args);

			// optionally, get basename timeline
			String basenameTimelineFile = MaryProperties.getFilename(header + ".basenameTimeline");
			TimelineReader basenameTimelineReader = null;
			if (basenameTimelineFile != null) {
				logger.debug("...loading basename time line...");
				basenameTimelineReader = new TimelineReader(basenameTimelineFile);
			}

			// build and load database
			logger.debug("...instantiating database...");
			String databaseClass = MaryProperties.needProperty(header + ".databaseClass");
			database = (UnitDatabase) Class.forName(databaseClass).newInstance();
			if (useSCost) {
				database.load(targetFunction, joinFunction, sCostFunction, unitReader, cart, timelineReader,
                              basenameTimelineReader, backtrace);
			} else {
				database.load(targetFunction, joinFunction, unitReader, cart, timelineReader, basenameTimelineReader, backtrace);
			}

			// build Selector
			logger.debug("...instantiating unit selector...");
			String selectorClass = MaryProperties.needProperty(header + ".selectorClass");
			unitSelector = (UnitSelector) Class.forName(selectorClass).newInstance();
			float targetCostWeights = Float.parseFloat(MaryProperties.getProperty(header + ".viterbi.wTargetCosts", "0.33"));
			int beamSize = MaryProperties.getInteger(header + ".viterbi.beamsize", 100);
			if (!useSCost) {
				unitSelector.load(database, targetCostWeights, beamSize);
			} else {
				float sCostWeights = Float.parseFloat(MaryProperties.getProperty(header + ".viterbi.wSCosts", "0.33"));
				unitSelector.load(database, targetCostWeights, sCostWeights, beamSize);
			}

			// samplingRate -> bin, audioformat -> concatenator
			// build Concatenator
			logger.debug("...instantiating unit concatenator...");
			String concatenatorClass = MaryProperties.needProperty(header + ".concatenatorClass");
			concatenator = (UnitConcatenator) Class.forName(concatenatorClass).newInstance();
			concatenator.load(database);

			// TODO: this can be deleted at the same time as CARTF0Modeller
			// see if there are any voice-specific duration and f0 models to load
			f0Carts = null;
			InputStream leftF0CartStream = MaryProperties.getStream(header + ".f0.cart.left");
			if (leftF0CartStream != null) {
				logger.debug("...loading f0 trees...");
				f0Carts = new CART[3];
				f0Carts[0] = new MaryCARTReader().loadFromStream(leftF0CartStream);
				leftF0CartStream.close();
				// mid cart:
				InputStream midF0CartStream = MaryProperties.needStream(header + ".f0.cart.mid");
				f0Carts[1] = new MaryCARTReader().loadFromStream(midF0CartStream);
				midF0CartStream.close();
				// right cart:
				InputStream rightF0CartStream = MaryProperties.needStream(header + ".f0.cart.right");
				f0Carts[2] = new MaryCARTReader().loadFromStream(rightF0CartStream);
				rightF0CartStream.close();
			}
		} catch (MaryConfigurationException mce) {
			throw mce;
		} catch (Exception ex) {
			throw new MaryConfigurationException("Cannot build unit selection voice '" + name + "'", ex);
		}

	}

	/**
	 * Gets the database of this voice
	 *
	 * @return the database
	 */
	public UnitDatabase getDatabase() {
		return database;
	}

	/**
	 * Gets the unit selector of this voice
	 *
	 * @return the unit selector
	 */
	public UnitSelector getUnitSelector() {
		return unitSelector;
	}

	/**
	 * Gets the unit concatenator of this voice
	 *
	 * @return the unit selector
	 */
	public UnitConcatenator getConcatenator() {
		return concatenator;
	}

	/**
	 * Get the modification UnitConcatenator of this voice
	 *
	 * @return the modifying UnitConcatenator
	 */
	public UnitConcatenator getModificationConcatenator() {
		if (modificationConcatenator == null) {
			// get sensible minimum and maximum values:
			try {
				// initialize with values from properties:
				double minTimeScaleFactor = Double.parseDouble(MaryProperties.getProperty("voice." + name
                                                                                          + ".prosody.modification.duration.factor.minimum"));
				double maxTimeScaleFactor = Double.parseDouble(MaryProperties.getProperty("voice." + name
                                                                                          + ".prosody.modification.duration.factor.maximum"));
				double minPitchScaleFactor = Double.parseDouble(MaryProperties.getProperty("voice." + name
                                                                                           + ".prosody.modification.f0.factor.minimum"));
				double maxPitchScaleFactor = Double.parseDouble(MaryProperties.getProperty("voice." + name
                                                                                           + ".prosody.modification.f0.factor.maximum"));
				logger.debug("Initializing FD-PSOLA unit concatenator with the following parameter thresholds:");
				logger.debug("minimum duration modification factor: " + minTimeScaleFactor);
				logger.debug("maximum duration modification factor: " + maxTimeScaleFactor);
				logger.debug("minimum F0 modification factor: " + minPitchScaleFactor);
				logger.debug("maximum F0 modification factor: " + maxPitchScaleFactor);
				modificationConcatenator = new FdpsolaUnitConcatenator(minTimeScaleFactor, maxTimeScaleFactor,
                                                                       minPitchScaleFactor, maxPitchScaleFactor);
			} catch (Exception e) {
				// ignore -- defaults will be used
				logger.debug("Initializing FD-PSOLA unit concatenator with default parameter thresholds.");
				modificationConcatenator = new FdpsolaUnitConcatenator();
			}
			modificationConcatenator.load(database);
		}
		return modificationConcatenator;
	}

	public CART[] getF0Trees() {
		return f0Carts;
	}

	public FeatureDefinition getF0CartsFeatDef() {
		if (f0Carts == null || f0Carts.length < 1)
			return null;
		return f0Carts[0].getFeatureDefinition();
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
                    model = new CARTModel(symbolicFPM, getName(), modelDataStream, modelAttributeName, modelAttributeFormat,
                                          modelFeatureName, modelPredictFrom, modelApplyTo);

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
        return getName() + " " + getLocale() + " " + gender().toString() + " " + "unitselection" + " "
            + getDomain();
    }
}
