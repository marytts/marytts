/**
 * Copyright 2000-2006 DFKI GmbH.
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

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;
import java.util.Vector;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;

import marytts.cart.DirectedGraph;
import marytts.cart.io.DirectedGraphReader;
import marytts.config.MaryConfig;
import marytts.config.VoiceConfig;
import marytts.datatypes.MaryData;
import marytts.datatypes.MaryDataType;
import marytts.datatypes.MaryXML;
import marytts.exceptions.MaryConfigurationException;
import marytts.exceptions.NoSuchPropertyException;
import marytts.exceptions.SynthesisException;
import marytts.features.FeatureProcessorManager;
import marytts.features.FeatureRegistry;
import marytts.modules.MaryModule;
import marytts.modules.ModuleRegistry;
import marytts.modules.acoustic.BoundaryModel;
import marytts.modules.acoustic.CARTModel;
import marytts.modules.acoustic.HMMModel;
import marytts.modules.acoustic.Model;
import marytts.modules.acoustic.ModelType;
import marytts.modules.acoustic.SoPModel;
import marytts.modules.phonemiser.Allophone;
import marytts.modules.phonemiser.AllophoneSet;
import marytts.server.MaryProperties;
import marytts.unitselection.data.FeatureFileReader;
import marytts.unitselection.interpolation.InterpolatingSynthesizer;
import marytts.unitselection.interpolation.InterpolatingVoice;
import marytts.util.MaryRuntimeUtils;
import marytts.util.MaryUtils;
import marytts.vocalizations.VocalizationSynthesizer;

import org.apache.log4j.Logger;
import org.w3c.dom.Element;

/**
 * A helper class for the synthesis module; each Voice object represents one available voice database.
 * 
 * @author Marc Schr&ouml;der
 */

public class Voice {
	/** Gender: male. */
	public static final Gender MALE = new Gender("male");
	/** Gender: female. */
	public static final Gender FEMALE = new Gender("female");
	/** Audio format: 16kHz,16bit,mono, native byte order */
	public static final AudioFormat AF16000 = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 16000, // samples per second
			16, // bits per sample
			1, // mono
			2, // nr. of bytes per frame
			16000, // nr. of frames per second
			(System.getProperty("os.arch").equals("x86") || System.getProperty("os.arch").equals("i386") || System.getProperty(
					"os.arch").equals("amd64")) ? // byteorder
			false // little-endian
					: true); // big-endian
	/** Audio format: 16kHz,16bit,mono, big endian */
	public static final AudioFormat AF16000BE = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 16000, // samples per second
			16, // bits per sample
			1, // mono
			2, // nr. of bytes per frame
			16000, // nr. of frames per second
			true); // big-endian
	/** Audio format: 22.05kHz,16bit,mono, native byte order */
	public static final AudioFormat AF22050 = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 22050, // samples per second
			16, // bits per sample
			1, // mono
			2, // nr. of bytes per frame
			22050, // nr. of frames per second
			(System.getProperty("os.arch").equals("x86") || System.getProperty("os.arch").equals("i386")) ? // byteorder
			false // little-endian
					: true); // big-endian
	/**
	 * List all registered voices. This set will always return the voices in the order of their wantToBeDefault value, highest
	 * first.
	 */
	private static Set<Voice> allVoices = new TreeSet<Voice>(new Comparator<Voice>() {
		public int compare(Voice v1, Voice v2) {
			// Return negative number if v1 should be listed before v2
			int desireDelta = v2.wantToBeDefault - v1.wantToBeDefault;
			if (desireDelta != 0)
				return desireDelta;
			// same desire -- sort alphabetically
			return v2.getName().compareTo(v1.getName());
		}
	});

	private static Map<Locale, Voice> defaultVoices = new HashMap<Locale, Voice>();

	protected static Logger logger = MaryUtils.getLogger("Voice");

	private String voiceName;
	private Locale locale;
	private AudioFormat dbAudioFormat = null;
	private WaveformSynthesizer synthesizer;
	private Gender gender;
	private int wantToBeDefault;
	private AllophoneSet allophoneSet;
	String preferredModulesClasses;
	private Vector<MaryModule> preferredModules;
	private boolean vocalizationSupport;
	private VocalizationSynthesizer vocalizationSynthesizer;
	protected DirectedGraph durationGraph;
	protected DirectedGraph f0Graph;
	protected FeatureFileReader f0ContourFeatures;
	protected Map<String, Model> acousticModels;

	@Deprecated
	public Voice(String name, Locale locale, AudioFormat dbAudioFormat, WaveformSynthesizer synthesizer, Gender gender)
			throws MaryConfigurationException {
		this.voiceName = name;
		this.locale = locale;
		this.dbAudioFormat = dbAudioFormat;
		this.synthesizer = synthesizer;
		this.gender = gender;

		try {
			init();
		} catch (Exception n) {
			throw new MaryConfigurationException("Cannot instantiate voice '" + voiceName + "'", n);
		}
	}

	public Voice(String name, WaveformSynthesizer synthesizer) throws MaryConfigurationException {
		this.voiceName = name;
		this.synthesizer = synthesizer;
		VoiceConfig config = MaryConfig.getVoiceConfig(voiceName);
		if (config == null) {
			throw new MaryConfigurationException("Trying to load config for voice '" + voiceName + "' but cannot find it.");
		}
		this.locale = config.getLocale();
		int samplingRate = MaryProperties.getInteger("voice." + voiceName + ".samplingRate", 16000);
		this.dbAudioFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, samplingRate, // samples per second
				16, // bits per sample
				1, // mono
				2, // nr. of bytes per frame
				samplingRate, // nr. of frames per second
				false);

		this.gender = new Gender(MaryProperties.needProperty("voice." + voiceName + ".gender"));

		try {
			init();
		} catch (Exception n) {
			throw new MaryConfigurationException("Cannot instantiate voice '" + voiceName + "'", n);
		}
	}

	/**
	 * @throws MaryConfigurationException
	 *             MaryConfigurationException
	 * @throws NoSuchPropertyException
	 *             NoSuchPropertyException
	 * @throws IOException
	 *             IOException
	 */
	private void init() throws MaryConfigurationException, NoSuchPropertyException, IOException {
		// Read settings from config file:
		String header = "voice." + getName();
		this.wantToBeDefault = MaryProperties.getInteger(header + ".wants.to.be.default", 0);
		try {
			allophoneSet = MaryRuntimeUtils.needAllophoneSet(header + ".allophoneset");
		} catch (MaryConfigurationException e) {
			// no allophone set for voice, try for locale
			try {
				allophoneSet = MaryRuntimeUtils.needAllophoneSet(MaryProperties.localePrefix(getLocale()) + ".allophoneset");
			} catch (MaryConfigurationException e2) {
				throw new MaryConfigurationException("No allophone set specified -- neither for voice '" + getName()
						+ "' nor for locale '" + getLocale() + "'", e2);
			}
		}
		preferredModulesClasses = MaryProperties.getProperty(header + ".preferredModules");

		String lexiconClass = MaryProperties.getProperty(header + ".lexiconClass");
		String lexiconName = MaryProperties.getProperty(header + ".lexicon");
		vocalizationSupport = MaryProperties.getBoolean(header + ".vocalizationSupport", false);
		if (vocalizationSupport) {
			vocalizationSynthesizer = new VocalizationSynthesizer(this);
		}

		loadOldStyleProsodyModels(header);
		loadAcousticModels(header);
		// initialization of FeatureProcessorManager for this voice, if needed:
		initFeatureProcessorManager();
	}

	@Deprecated
	private void loadOldStyleProsodyModels(String header) throws MaryConfigurationException {
		// see if there are any voice-specific duration and f0 models to load
		durationGraph = null;
		String durationGraphFile = MaryProperties.getFilename(header + ".duration.cart");
		if (durationGraphFile != null) {
			logger.debug("...loading duration graph...");
			try {
				durationGraph = (new DirectedGraphReader()).load(durationGraphFile);
			} catch (IOException e) {
				throw new MaryConfigurationException("Cannot load duration graph file '" + durationGraphFile + "'", e);
			}
		}

		f0Graph = null;
		String f0GraphFile = MaryProperties.getFilename(header + ".f0.graph");
		if (f0GraphFile != null) {
			logger.debug("...loading f0 contour graph...");
			try {
				f0Graph = (new DirectedGraphReader()).load(f0GraphFile);
				// If we have the graph, we need the contour:
				String f0ContourFile = MaryProperties.needFilename(header + ".f0.contours");
				f0ContourFeatures = new FeatureFileReader(f0ContourFile);
			} catch (IOException e) {
				throw new MaryConfigurationException("Cannot load f0 contour graph file '" + f0GraphFile + "'", e);
			}
		}
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
	private void loadAcousticModels(String header) throws MaryConfigurationException, NoSuchPropertyException, IOException {
		// The feature processor manager that all acoustic models will use to predict their acoustics:
		FeatureProcessorManager symbolicFPM = FeatureRegistry.determineBestFeatureProcessorManager(getLocale());

		// Acoustic models:
		String acousticModelsString = MaryProperties.getProperty(header + ".acousticModels");
		if (acousticModelsString != null) {
			acousticModels = new HashMap<String, Model>();

			// add boundary "model" (which could of course be overwritten by appropriate properties in voice config):
			acousticModels.put("boundary", new BoundaryModel(symbolicFPM, voiceName, null, "duration", null, null, null,
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
					switch (possibleModelTypes) {
					case CART:
						model = new CARTModel(symbolicFPM, voiceName, modelDataStream, modelAttributeName, modelAttributeFormat,
								modelFeatureName, modelPredictFrom, modelApplyTo);
						break;

					case SOP:
						model = new SoPModel(symbolicFPM, voiceName, modelDataStream, modelAttributeName, modelAttributeFormat,
								modelFeatureName, modelPredictFrom, modelApplyTo);
						break;

					case HMM:
						// if we already have a HMM duration or F0 model, and if this is the other of the two, and if so,
						// and they use the same dataFile, then let them be the same instance:
						// if this is the case set the boolean variable predictDurAndF0 to true in HMMModel
						if (getDurationModel() != null && getDurationModel() instanceof HMMModel
								&& modelName.equalsIgnoreCase("F0") && voiceName.equals(getDurationModel().getVoiceName())) {
							model = getDurationModel();
							((HMMModel) model).setPredictDurAndF0(true);
						} else if (getF0Model() != null && getF0Model() instanceof HMMModel
								&& modelName.equalsIgnoreCase("duration") && voiceName.equals(getF0Model().getVoiceName())) {
							model = getF0Model();
							((HMMModel) model).setPredictDurAndF0(true);
						} else {
							model = new HMMModel(symbolicFPM, voiceName, modelDataStream, modelAttributeName,
									modelAttributeFormat, modelFeatureName, modelPredictFrom, modelApplyTo);
						}
						break;
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

	/**
	 * Try to determine a feature processor manager. This will look for the voice-specific config setting
	 * <code>voice.(voicename).featuremanager</code>. If a feature processor manager is found, it is initialised and entered into
	 * the {@link marytts.features.FeatureRegistry}.
	 * 
	 * @throws MaryConfigurationException
	 *             if the feature processor manager cannot be initialised.
	 */
	private void initFeatureProcessorManager() throws MaryConfigurationException {
		FeatureProcessorManager featMgr = null;

		// Any feature processor manager settings in the config file?
		String keyVoiceFeatMgr = "voice." + getName() + ".featuremanager";
		String featMgrClass = MaryProperties.getProperty(keyVoiceFeatMgr);
		if (featMgrClass != null) {
			try {
				featMgr = (FeatureProcessorManager) Class.forName(featMgrClass).newInstance();
			} catch (Exception e) {
				throw new MaryConfigurationException("Cannot initialise voice-specific FeatureProcessorManager " + featMgrClass
						+ " from config file", e);
			}
		} else if (getOtherModels() != null) {
			// Only if there is no feature manager setting in the config file,
			// we consider creating one from the acoustic features;
			// We need to do this only if we have any "other" acoustic models, beyond duration and F0:

			FeatureProcessorManager genericFPM = FeatureRegistry.determineBestFeatureProcessorManager(locale);
			// We attempt to create an FPM with same class as genericFPM via the Constructor FPM(Voice):
			Class<? extends FeatureProcessorManager> fpmClass = genericFPM.getClass();
			try {
				Constructor<? extends FeatureProcessorManager> fpmVoiceConstructor = fpmClass.getConstructor(Voice.class);
				featMgr = fpmVoiceConstructor.newInstance(this);
			} catch (NoSuchMethodException nsme) {
				throw new MaryConfigurationException("Cannot initialise voice-specific FeatureProcessorManager: Class "
						+ fpmClass.getName() + " has no constructor " + fpmClass.getSimpleName() + "(Voice)");
			} catch (Exception e) {
				throw new MaryConfigurationException("Cannot initialise voice-specific FeatureProcessorManager", e);
			}
		}
		// register the FeatureProcessorManager for this Voice:
		if (featMgr != null) {
			FeatureRegistry.setFeatureProcessorManager(this, featMgr);
		}
	}

	/**
	 * Get the allophone set associated with this voice.
	 * 
	 * @return allophoneSet
	 */
	public AllophoneSet getAllophoneSet() {
		return allophoneSet;
	}

	/**
	 * Get the Allophone set for the given phone symbol.
	 * 
	 * @param phoneSymbol
	 *            phoneSymbol
	 * @return an Allophone object if phoneSymbol is a known phone symbol in the voice's AllophoneSet.
	 * @deprecated use {@link AllophoneSet#getAllophone(String)} directly instead
	 */
	@Deprecated
	public Allophone getAllophone(String phoneSymbol) {
		return allophoneSet.getAllophone(phoneSymbol);
	}

	public synchronized Vector<MaryModule> getPreferredModulesAcceptingType(MaryDataType type) {
		if (preferredModules == null && preferredModulesClasses != null) {
			// need to initialise the list of modules
			preferredModules = new Vector<MaryModule>();
			StringTokenizer st = new StringTokenizer(preferredModulesClasses);
			while (st.hasMoreTokens()) {
				String moduleInfo = st.nextToken();
				try {
					MaryModule mm = null;
					if (!moduleInfo.contains("(")) { // no constructor info
						mm = ModuleRegistry.getModule(Class.forName(moduleInfo));
					}
					if (mm == null) {
						// need to create our own:
						logger.warn("Module "
								+ moduleInfo
								+ " is not in the standard list of modules -- will start our own, but will not be able to shut it down at the end.");
						mm = ModuleRegistry.instantiateModule(moduleInfo);
						mm.startup();
					}
					preferredModules.add(mm);
				} catch (Exception e) {
					logger.warn("Cannot initialise preferred module " + moduleInfo + " for voice " + getName() + " -- skipping.",
							e);
				}
			}
		}
		if (preferredModules != null) {
			Vector<MaryModule> v = new Vector<MaryModule>();
			for (Iterator<MaryModule> it = preferredModules.iterator(); it.hasNext();) {
				MaryModule m = (MaryModule) it.next();
				if (m.inputType().equals(type)) {
					v.add(m);
				}
			}
			if (v.size() > 0)
				return v;
			else
				return null;
		}
		return null;
	}

	public boolean hasName(String aName) {
		return voiceName.equals(aName);
	}

	/**
	 * Return the name of this voice. If the voice has several possible names, the first one is returned.
	 * 
	 * @return voiceName
	 */
	public String getName() {
		return voiceName;
	}

	/** Returns the return value of <code>getName()</code>. */
	public String toString() {
		return getName();
	}

	public Locale getLocale() {
		return locale;
	}

	public AudioFormat dbAudioFormat() {
		return dbAudioFormat;
	}

	public WaveformSynthesizer synthesizer() {
		return synthesizer;
	}

	public Gender gender() {
		return gender;
	}

	public boolean hasVocalizationSupport() {
		return vocalizationSupport;
	}

	public VocalizationSynthesizer getVocalizationSynthesizer() {
		return vocalizationSynthesizer;
	}

	/**
	 * Get any styles supported by this voice.
	 * 
	 * @return an array of style names supported by this voice, or null if styles are not supported.
	 */
	public String[] getStyles() {
		// TODO: read from config file
		if (voiceName.equals("dfki-pavoque-styles")) {
			return new String[] { "neutral", "poker", "happy", "angry", "sad" };
		}
		return null;
	}

	/**
	 * Synthesize a list of tokens and boundaries with the waveform synthesizer providing this voice.
	 * 
	 * @param tokensAndBoundaries
	 *            tokensAndBoundaries
	 * @param outputParams
	 *            outputParams
	 * @throws SynthesisException
	 *             SynthesisException
	 * @return f0ContourFeatures
	 */
	public AudioInputStream synthesize(List<Element> tokensAndBoundaries, String outputParams) throws SynthesisException {
		return synthesizer.synthesize(tokensAndBoundaries, this, outputParams);
	}

	public DirectedGraph getDurationGraph() {
		return durationGraph;
	}

	public DirectedGraph getF0Graph() {
		return f0Graph;
	}

	public FeatureFileReader getF0ContourFeatures() {
		return f0ContourFeatures;
	}

	// Several getters for acoustic models, returning null if undefined:

	/**
	 * Get the acoustic models defined for this voice.
	 * 
	 * @return a Map mapping model names to models, or null if there are no such models.
	 */
	public Map<String, Model> getAcousticModels() {
		return acousticModels;
	}

	/**
	 * Get the duration model for this voice.
	 * 
	 * @return the model, or null if no such model is defined.
	 */
	public Model getDurationModel() {
		if (acousticModels == null) {
			return null;
		}
		return acousticModels.get("duration");
	}

	/**
	 * Get the F0 model for this voice.
	 * 
	 * @return the model, or null if no such model is defined.
	 */
	public Model getF0Model() {
		if (acousticModels == null) {
			return null;
		}
		return acousticModels.get("F0");
	}

	/**
	 * Get the boundary duration model for this voice.
	 * 
	 * @return the model, or null if no such model is defined.
	 */
	public Model getBoundaryModel() {
		if (acousticModels == null) {
			return null;
		}
		return acousticModels.get("boundary");
	}

	/**
	 * Return any "other" acoustic models that we have. Other models are acoustic models beyond duration, F0 and boundary.
	 * 
	 * @return a Map mapping the model name to the model, or null if no other models exist.
	 */
	public Map<String, Model> getOtherModels() {
		if (acousticModels == null) {
			return null;
		}
		Map<String, Model> otherModels = new HashMap<String, Model>();
		for (String modelName : acousticModels.keySet()) {
			// ignore critical Models that have their own getters:
			if (!modelName.equals("duration") && !modelName.equals("F0") && !modelName.equals("boundary")) {
				otherModels.put(modelName, acousticModels.get(modelName));
			}
		}
		if (otherModels.size() == 0) {
			return null;
		}
		return otherModels;
	}

	// //////// static stuff //////////

	/**
	 * Register the given voice. It will be contained in the list of available voices returned by any subsequent calls to
	 * getAvailableVoices(). If the voice has the highest value of <code>wantToBeDefault</code> for its locale it will be
	 * registered as the default voice for its locale. This value is set in the config file setting
	 * <code>voice.(name).want.to.be.default.voice</code>.
	 * 
	 * @param voice
	 *            voicwe
	 */
	public static void registerVoice(Voice voice) {
		if (voice == null)
			throw new NullPointerException("Cannot register null voice.");
		if (!allVoices.contains(voice)) {
			logger.info("Registering voice `" + voice.getName() + "': " + voice.gender() + ", locale " + voice.getLocale());
			allVoices.add(voice);
		}
		checkIfDefaultVoice(voice);
	}

	/**
	 * Check if this voice should be registered as default.
	 * 
	 * @param voice
	 *            voice
	 */
	private static void checkIfDefaultVoice(Voice voice) {

		Locale locale = voice.getLocale();
		Voice currentDefault = defaultVoices.get(locale);
		if (currentDefault == null || currentDefault.wantToBeDefault < voice.wantToBeDefault) {
			logger.info("New default voice for locale " + locale + ": " + voice.getName() + " (desire " + voice.wantToBeDefault
					+ ")");
			defaultVoices.put(locale, voice);
		}
	}

	/**
	 * Get the voice with the given name, or null if there is no voice with that name.
	 * 
	 * @param name
	 *            name
	 * @return v if it has name
	 */
	public static Voice getVoice(String name) {
		for (Iterator<Voice> it = allVoices.iterator(); it.hasNext();) {
			Voice v = it.next();
			if (v.hasName(name))
				return v;
		}
		// Interpolating voices are created as needed:
		if (InterpolatingVoice.isInterpolatingVoiceName(name)) {
			InterpolatingSynthesizer interpolatingSynthesizer = null;
			for (Iterator<Voice> it = allVoices.iterator(); it.hasNext();) {
				Voice v = it.next();
				if (v instanceof InterpolatingVoice) {
					interpolatingSynthesizer = (InterpolatingSynthesizer) v.synthesizer();
					break;
				}
			}
			if (interpolatingSynthesizer == null)
				return null;
			try {
				Voice v = new InterpolatingVoice(interpolatingSynthesizer, name);
				registerVoice(v);
				return v;
			} catch (Exception e) {
				logger.warn("Could not create Interpolating voice:", e);
				return null;
			}
		}
		return null; // no such voice found
	}

	/**
	 * Get the list of all available voices. The iterator of the collection returned will return the voices in decreasing order of
	 * their "wantToBeDefault" value.
	 * 
	 * @return Collections.unmodifiableSet(allVoices)
	 */
	public static Collection<Voice> getAvailableVoices() {
		return Collections.unmodifiableSet(allVoices);
	}

	/**
	 * Get the list of all available voices for a given locale. The iterator of the collection returned will return the voices in
	 * decreasing order of their "wantToBeDefault" value.
	 * 
	 * @param locale
	 *            locale
	 * @return a collection of Voice objects, or an empty collection if no voice is available for the given locale.
	 */
	public static Collection<Voice> getAvailableVoices(Locale locale) {
		ArrayList<Voice> list = new ArrayList<Voice>();
		for (Voice v : allVoices) {
			if (MaryUtils.subsumes(locale, v.getLocale())) {
				list.add(v);
			}
		}
		return list;
	}

	/**
	 * Get the list of all available voices for a given waveform synthesizer. The iterator of the collection returned will return
	 * the voices in decreasing order of their "wantToBeDefault" value.
	 * 
	 * @param synth
	 *            synth
	 * @return a collection of Voice objects, or an empty collection if no voice is available for the given waveform synthesizer.
	 */
	public static Collection<Voice> getAvailableVoices(WaveformSynthesizer synth) {
		if (synth == null) {
			throw new NullPointerException("Got null WaveformSynthesizer");
		}
		ArrayList<Voice> list = new ArrayList<Voice>();
		for (Voice v : allVoices) {
			if (synth.equals(v.synthesizer())) {
				list.add(v);
			}
		}
		return list;
	}

	/**
	 * Get the list of all available voices for a given waveform synthesizer and locale. The iterator of the collection returned
	 * will return the voices in decreasing order of their "wantToBeDefault" value.
	 * 
	 * @param synth
	 *            synth
	 * @param locale
	 *            locale
	 * @return a collection of Voice objects, or an empty collection if no voice is available for the given locale.
	 */
	public static Collection<Voice> getAvailableVoices(WaveformSynthesizer synth, Locale locale) {
		ArrayList<Voice> list = new ArrayList<Voice>();
		for (Voice v : allVoices) {
			if (v.synthesizer().equals(synth) && MaryUtils.subsumes(locale, v.getLocale())) {
				list.add(v);
			}
		}
		return list;
	}

	public static Voice getVoice(Locale locale, Gender gender) {
		for (Voice v : allVoices) {
			if (MaryUtils.subsumes(locale, v.getLocale()) && v.gender().equals(gender))
				return v;
		}
		return null; // no such voice found
	}

	public static Voice getVoice(Element voiceElement) {
		if (voiceElement == null || !voiceElement.getTagName().equals(MaryXML.VOICE)) {
			return null;
		}

		Voice v = null;
		// Try to get the voice by name:
		String voiceName = voiceElement.getAttribute("name");
		if (!voiceName.equals("")) {
			v = Voice.getVoice(voiceName);
		}
		// Now if that didn't work, try getting a voice by gender:
		if (v == null) {
			String voiceGender = voiceElement.getAttribute("gender");
			// Try to get the locale for the voice Element.
			// Trust that the locale is encoded in the document root element.
			Locale locale = MaryUtils
					.string2locale(voiceElement.getOwnerDocument().getDocumentElement().getAttribute("xml:lang"));
			if (locale == null) {
				locale = Locale.GERMAN;
			}
			v = Voice.getVoice(locale, new Gender(voiceGender));
		}
		return v;
	}

	public static Voice getDefaultVoice(Locale locale) {
		Voice v = defaultVoices.get(locale);
		if (v == null)
			v = getVoice(locale, FEMALE);
		if (v == null)
			v = getVoice(locale, MALE);
		if (v == null)
			logger.debug("Could not find default voice for locale " + locale);
		return v;
	}

	public static Voice getSuitableVoice(MaryData d) {
		Locale docLocale = d.getLocale();
		if (docLocale == null && d.getType().isXMLType() && d.getDocument() != null
				&& d.getDocument().getDocumentElement().hasAttribute("xml:lang")) {
			docLocale = MaryUtils.string2locale(d.getDocument().getDocumentElement().getAttribute("xml:lang"));
		}
		Voice guessedVoice = null;
		if (docLocale != null) {
			guessedVoice = Voice.getDefaultVoice(docLocale);
		} else {
			// get any voice
			if (allVoices.size() != 0)
				guessedVoice = (Voice) allVoices.iterator().next();
		}
		if (guessedVoice != null)
			logger.debug("Guessing default voice `" + guessedVoice.getName() + "'");
		else
			logger.debug("Couldn't find any voice at all");

		return guessedVoice;
	}

	public static class Gender {
		String name;

		public Gender(String name) {
			this.name = name;
		}

		public String toString() {
			return name;
		}

		public boolean equals(Gender other) {
			return other.toString().equals(name);
		}
	}

}
