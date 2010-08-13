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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
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
import marytts.datatypes.MaryData;
import marytts.datatypes.MaryDataType;
import marytts.datatypes.MaryXML;
import marytts.exceptions.MaryConfigurationException;
import marytts.exceptions.NoSuchPropertyException;
import marytts.exceptions.SynthesisException;
import marytts.features.FeatureProcessorManager;
import marytts.features.FeatureRegistry;
import marytts.features.MaryFeatureProcessor;
import marytts.features.FeatureVector.FeatureType;
import marytts.features.MaryGenericFeatureProcessors.GenericContinuousFeature;
import marytts.features.TargetFeatureComputer;
import marytts.modules.MaryModule;
import marytts.modules.ModuleRegistry;
import marytts.modules.acoustic.BoundaryModel;
import marytts.modules.acoustic.CARTModel;
import marytts.modules.acoustic.HMMModel;
import marytts.modules.acoustic.Model;
import marytts.modules.acoustic.ModelType;
import marytts.modules.acoustic.ProsodyModel;
import marytts.modules.acoustic.SoPModel;
import marytts.modules.phonemiser.Allophone;
import marytts.modules.phonemiser.AllophoneSet;
import marytts.nonverbal.BackchannelSynthesizer;
import marytts.server.MaryProperties;
import marytts.unitselection.data.FeatureFileReader;
import marytts.unitselection.interpolation.InterpolatingSynthesizer;
import marytts.unitselection.interpolation.InterpolatingVoice;
import marytts.util.MaryUtils;

import org.apache.log4j.Logger;
import org.w3c.dom.Element;

import com.sun.speech.freetts.FeatureProcessor;
import com.sun.speech.freetts.en.us.CMULexicon;
import com.sun.speech.freetts.lexicon.Lexicon;

/**
 * A helper class for the synthesis module; each Voice object represents one
 * available voice database.
 * @author Marc Schr&ouml;der
 */

public class Voice
{
    /** Gender: male. */
    public static final Gender MALE = new Gender("male");
    /** Gender: female. */
    public static final Gender FEMALE = new Gender("female");
    /** Audio format: 16kHz,16bit,mono, native byte order */
    public static final AudioFormat AF16000 = new AudioFormat
        (AudioFormat.Encoding.PCM_SIGNED,
         16000, // samples per second
         16, // bits per sample
         1, // mono
         2, // nr. of bytes per frame
         16000, // nr. of frames per second
         (System.getProperty("os.arch").equals("x86") ||
          System.getProperty("os.arch").equals("i386") ||
          System.getProperty("os.arch").equals("amd64")) ? // byteorder
         false // little-endian
         : true); // big-endian
    /** Audio format: 16kHz,16bit,mono, big endian */
    public static final AudioFormat AF16000BE = new AudioFormat
        (AudioFormat.Encoding.PCM_SIGNED,
         16000, // samples per second
         16, // bits per sample
         1, // mono
         2, // nr. of bytes per frame
         16000, // nr. of frames per second
         true); // big-endian
    /** Audio format: 22.05kHz,16bit,mono, native byte order */
    public static final AudioFormat AF22050 = new AudioFormat
        (AudioFormat.Encoding.PCM_SIGNED,
         22050, // samples per second
         16, // bits per sample
         1, // mono
         2, // nr. of bytes per frame
         22050, // nr. of frames per second
         (System.getProperty("os.arch").equals("x86") ||
          System.getProperty("os.arch").equals("i386")) ? // byteorder
         false // little-endian
         : true); // big-endian
    /** List all registered voices. This set will always return the voices in the order of their
     * wantToBeDefault value, highest first. */
    private static Set<Voice> allVoices = new TreeSet<Voice>(new Comparator<Voice>() {
    	public int compare(Voice v1, Voice v2) {
    		// Return negative number if v1 should be listed before v2
    		int desireDelta = v2.wantToBeDefault - v1.wantToBeDefault; 
    		if (desireDelta != 0) return desireDelta;
    		// same desire -- sort alphabetically
    		return v2.getName().compareTo(v1.getName());
    	}
    });

    private static Map<Locale,Voice> defaultVoices = new HashMap<Locale,Voice>();

    protected static Logger logger = Logger.getLogger("Voice");

    /** A local map of already-instantiated Lexicons */
    private static Map<String, Lexicon> lexicons = new HashMap<String, Lexicon>();


    private List<String> names; // all the names under which this voice is known
    private Locale locale;
    private AudioFormat dbAudioFormat = null;
    private WaveformSynthesizer synthesizer;
    private Gender gender;
    private int wantToBeDefault;
    private AllophoneSet allophoneSet;
    String preferredModulesClasses;
    private Vector<MaryModule> preferredModules;
    private Lexicon lexicon;
    private boolean backchannelSupport;
    private BackchannelSynthesizer backchannelSynthesizer;
    protected DirectedGraph durationGraph;
    protected DirectedGraph f0Graph;
    protected FeatureFileReader f0ContourFeatures;
    protected Map<String, Model> acousticModels;
    
    public Voice(String[] nameArray, Locale locale, 
                 AudioFormat dbAudioFormat,
                 WaveformSynthesizer synthesizer,
                 Gender gender)
    throws MaryConfigurationException
    {
        this.names = new ArrayList<String>();
        for (int i=0; i<nameArray.length; i++)
            names.add(nameArray[i]);
        this.locale = locale;
        this.dbAudioFormat = dbAudioFormat;
        this.synthesizer = synthesizer;
        this.gender = gender;
        
        // Read settings from config file:
        String header = "voice."+getName();
        this.wantToBeDefault = MaryProperties.getInteger(header+".wants.to.be.default", 0);
        String allphonesetFilename = MaryProperties.getFilename(header+".allophoneset");
        if (allphonesetFilename == null && getLocale() != null) {
            // No specific phone set for voice, use locale default
            allphonesetFilename = MaryProperties.getFilename(MaryProperties.localePrefix(getLocale())+".allophoneset");
        }
        if (allphonesetFilename == null) {
            throw new MaryConfigurationException("No allophone set specified -- neither for voice '"+getName()+"' nor for locale '"+getLocale()+"'");
        }
        try {
            allophoneSet = AllophoneSet.getAllophoneSet(allphonesetFilename);
        } catch (Exception e) {
            throw new MaryConfigurationException("Cannot load allophone set", e);
        }
        preferredModulesClasses = MaryProperties.getProperty(header+".preferredModules");
        
        initFeatureProcessorManager();
        String lexiconClass = MaryProperties.getProperty(header+".lexiconClass");
        String lexiconName = MaryProperties.getProperty(header+".lexicon");
        lexicon = getLexicon(lexiconClass, lexiconName);
        backchannelSupport = MaryProperties.getBoolean(header+".backchannelSupport", false);
        if(backchannelSupport) {
            backchannelSynthesizer = new BackchannelSynthesizer(this);
        }
        
        // see if there are any voice-specific duration and f0 models to load
        durationGraph = null;
        String durationGraphFile = MaryProperties.getFilename(header+".duration.cart");
        if (durationGraphFile != null) {
            logger.debug("...loading duration graph...");
            try {
                durationGraph = (new DirectedGraphReader()).load(durationGraphFile);
            } catch (IOException e) {
                throw new MaryConfigurationException("Cannot load duration graph file '"+durationGraphFile+"'", e);
            }
        }

        f0Graph = null;
        String f0GraphFile = MaryProperties.getFilename(header+".f0.graph");
        if (f0GraphFile != null) {
            logger.debug("...loading f0 contour graph...");
            try {
                f0Graph = (new DirectedGraphReader()).load(f0GraphFile);
                // If we have the graph, we need the contour:
                String f0ContourFile = MaryProperties.needFilename(header+".f0.contours");
                f0ContourFeatures = new FeatureFileReader(f0ContourFile);
            } catch (IOException e) {
                throw new MaryConfigurationException("Cannot load f0 contour graph file '"+f0GraphFile+"'", e);
            }
        }
        
        // Acoustic models:
        String acousticModelsString = MaryProperties.getProperty(header + ".acousticModels");
        if (acousticModelsString != null) {
            acousticModels = new HashMap<String, Model>();

            // add boundary "model" (which could of course be overwritten by appropriate properties in voice config):
            acousticModels.put("boundary", new BoundaryModel("boundary", null, "duration", null, null, null, "boundaries"));

            StringTokenizer acousticModelStrings = new StringTokenizer(acousticModelsString);
            do {
                String modelName = acousticModelStrings.nextToken();

                // get more properties from voice config, depending on the model name:
                String modelType = MaryProperties.needProperty(header + "." + modelName + ".model");

                String modelDataFileName = MaryProperties.needFilename(header + "." + modelName + ".data");
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
                    logger.warn("Cannot handle unknown model type: " + modelType);
                    throw new MaryConfigurationException();
                }

                // ...and instantiate it in a switch statement:
                Model model = null;
                switch (possibleModelTypes) {
                case CART:
                    model = new CARTModel(modelType, modelDataFileName, modelAttributeName, modelAttributeFormat,
                            modelFeatureName, modelPredictFrom, modelApplyTo);
                    break;

                case SOP:
                    model = new SoPModel(modelType, modelDataFileName, modelAttributeName, modelAttributeFormat,
                            modelFeatureName, modelPredictFrom, modelApplyTo);
                    break;

                case HMM:
                    model = new HMMModel(modelType, modelDataFileName, modelAttributeName, modelAttributeFormat,
                            modelFeatureName, modelPredictFrom, modelApplyTo);
                    break;
                }

                // if we got this far, model should not be null:
                assert model != null;

                // load dataFile and put the model in the Model Map:
                model.loadDataFile();
                acousticModels.put(modelName, model);
            } while (acousticModelStrings.hasMoreTokens());

            // initialization of FeatureProcessorManager for this voice:
            FeatureProcessorManager featureProcessorManager;
            // TODO somehow reconcile that German FPM class with the rest of the code...
            if (locale.equals(new Locale("de"))) {
                featureProcessorManager = new marytts.language.de.features.FeatureProcessorManager(this);
            } else {
                featureProcessorManager = new FeatureProcessorManager(this);
            }

            // (re-)register the FeatureProcessorManager for this Voice:
            FeatureRegistry.setFeatureProcessorManager(this, featureProcessorManager);
        }
    }

    /**
     * Try to determine a feature processor manager. This will look for the voice-specific config setting
     * <code>voice.(voicename).featuremanager</code>. If a feature processor manager 
     * is found, it is initialised and entered into the {@link marytts.features.FeatureRegistry}.
     */
    private void initFeatureProcessorManager() {
        // First, the feature processor manager to use:
        String keyVoiceFeatMgr = "voice."+getName()+".featuremanager";
        String featMgrClass = MaryProperties.getProperty(keyVoiceFeatMgr);
        if (featMgrClass != null) {
            try {
                FeatureProcessorManager featMgr = (FeatureProcessorManager) Class.forName(featMgrClass).newInstance();
                FeatureRegistry.setFeatureProcessorManager(this, featMgr);
            } catch (Exception e) {
                throw new IllegalArgumentException("Cannot initialise voice-specific FeatureProcessorManager", e);
            }
        }
    }
    


    /**
     * Get the allophone set associated with this voice.
     * @return
     */
    public AllophoneSet getAllophoneSet()
    {
        return allophoneSet;
    }

    /**
     * Get the Allophone set for the given phone symbol.
     * @param phoneSymbol
     * @return an Allophone object if phoneSymbol is a known phone symbol in the voice's AllophoneSet, or null.
     */
    public Allophone getAllophone(String phoneSymbol)
    {
        return allophoneSet.getAllophone(phoneSymbol);
    }
    
    
    public synchronized Vector<MaryModule> getPreferredModulesAcceptingType(MaryDataType type)
    {
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
                        logger.warn("Module "+moduleInfo+" is not in the standard list of modules -- will start our own, but will not be able to shut it down at the end.");
                        mm = ModuleRegistry.instantiateModule(moduleInfo);
                        mm.startup();
                    }
                    preferredModules.add(mm);
                } catch (Exception e) {
                    logger.warn("Cannot initialise preferred module "+moduleInfo+" for voice "+getName()+" -- skipping.", e);
                }
            }
        }
        if (preferredModules != null) {
            Vector<MaryModule> v = new Vector<MaryModule>();
            for (Iterator<MaryModule> it = preferredModules.iterator(); it.hasNext(); ) {
                MaryModule m = (MaryModule) it.next();
                if (m.inputType().equals(type)) {
                    v.add(m);
                }
            }
            if (v.size() > 0) return v;
            else return null;
        }
        return null;
    }
    

    public boolean hasName(String name) { return names.contains(name); }
    /** Return the name of this voice. If the voice has several possible names,
     * the first one is returned. */
    public String getName() { return (String) names.get(0); }
    /** Returns the return value of <code>getName()</code>. */
    public String toString() { return getName(); }
    public Locale getLocale() { return locale; }
    public AudioFormat dbAudioFormat() { return dbAudioFormat; }
    public WaveformSynthesizer synthesizer() { return synthesizer; }
    public Gender gender() { return gender; }
    public boolean hasBackchannelSupport() { return backchannelSupport; }
    public BackchannelSynthesizer getBackchannelSynthesizer() { return backchannelSynthesizer; }

    

    /**
     * Synthesize a list of tokens and boundaries with the waveform synthesizer
     * providing this voice.
     */
    public AudioInputStream synthesize(List<Element> tokensAndBoundaries)
        throws SynthesisException
    {
        return synthesizer.synthesize(tokensAndBoundaries, this);
    }
    
    /**
     * Return the lexicon associated to this voice
     * @return
     */
    public Lexicon getLexicon()
    {
        return lexicon;
    }

    public DirectedGraph getDurationGraph()
    {
        return durationGraph;
    }

    public DirectedGraph getF0Graph()
    {
        return f0Graph;
    }

    public FeatureFileReader getF0ContourFeatures()
    {
        return f0ContourFeatures;
    }
    
    // Several getters for acoustic models, returning null if undefined:
    
    public Map<String, Model> getAcousticModels() {
        return acousticModels;
    }
    
    public Model getDurationModel() {
        return acousticModels.get("duration");
    }
    
    public Model getF0Model() {
        return acousticModels.get("F0");
    }
      
    public Model getBoundaryModel() {
        return acousticModels.get("boundary");
    }
    

    
    public Map<String, Model> getOtherModels() {
        Map<String, Model> otherModels = new HashMap<String, Model>();
        for (String modelName : acousticModels.keySet()) {
            // ignore critical Models that have their own getters:
            if (!modelName.equals("duration") && !modelName.equals("F0") && !modelName.equals("boundary")) {
                otherModels.put(modelName, acousticModels.get(modelName));
            }
        }
        return otherModels;
    }

    ////////// static stuff //////////

    /**
     * Register the given voice. It will be contained in the list of available voices returned
     * by any subsequent calls to getAvailableVoices(). If the voice has the highest value of
     * <code>wantToBeDefault</code> for its locale it will be registered as the default voice for
     * its locale.
     * This value is set in the config file setting <code>voice.(name).want.to.be.default.voice</code>.
     */
    public static void registerVoice(Voice voice)
    {
        if (voice == null)
            throw new NullPointerException("Cannot register null voice.");
        if (!allVoices.contains(voice)) {
            logger.info("Registering voice `" + voice.getName() + "': " +
                         voice.gender() + ", locale " + voice.getLocale());
            allVoices.add(voice);
            try {
                FreeTTSVoices.load(voice);
            } catch (NoClassDefFoundError err) {
                // do nothing
            }
        }
        checkIfDefaultVoice(voice);
    }



    /**
     * Check if this voice should be registered as default.
     * @param voice
     */
	private static void checkIfDefaultVoice(Voice voice)
	{
		
        Locale locale = voice.getLocale();
        Voice currentDefault = defaultVoices.get(locale);
        if (currentDefault == null || currentDefault.wantToBeDefault < voice.wantToBeDefault) {
            logger.info("New default voice for locale " + locale + ": " + voice.getName() + " (desire " + voice.wantToBeDefault + ")");
        	defaultVoices.put(locale, voice);
        }
	}

	/**
	 * Get the voice with the given name, or null if there is no voice with that name.
	 * @param name
	 * @return
	 */
    public static Voice getVoice(String name)
    {
        for (Iterator<Voice> it = allVoices.iterator(); it.hasNext(); ) {
            Voice v = it.next();
            if (v.hasName(name)) return v;
        }
        // Interpolating voices are created as needed:
        if (InterpolatingVoice.isInterpolatingVoiceName(name)) {
            InterpolatingSynthesizer interpolatingSynthesizer = null;
            for (Iterator<Voice> it = allVoices.iterator(); it.hasNext(); ) {
                Voice v = it.next();
                if (v instanceof InterpolatingVoice) {
                    interpolatingSynthesizer = (InterpolatingSynthesizer) v.synthesizer();
                    break;
                }
            }
            if (interpolatingSynthesizer == null) return null;
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
     * Get the list of all available voices. The iterator of the collection returned
     * will return the voices in decreasing order of their "wantToBeDefault" value.
     */
    public static Collection<Voice> getAvailableVoices() { return Collections.unmodifiableSet(allVoices); }

    /**
     * Get the list of all available voices for a given locale. The iterator of the collection returned
     * will return the voices in decreasing order of their "wantToBeDefault" value.
     * @param locale
     * @return a collection of Voice objects, or an empty collection if no voice is available for the given locale.
     */
    public static Collection<Voice> getAvailableVoices(Locale locale)
    {
        ArrayList<Voice> list = new ArrayList<Voice>();
        for (Voice v : allVoices) {
            if (MaryUtils.subsumes(locale, v.getLocale())) {
                list.add(v);
            }
        }
        return list;
    }

    /**
     * Get the list of all available voices for a given waveform synthesizer. The iterator of the collection returned
     * will return the voices in decreasing order of their "wantToBeDefault" value.
     * @return a collection of Voice objects, or an empty collection if no voice is available for the given waveform synthesizer.
     */
    public static Collection<Voice> getAvailableVoices(WaveformSynthesizer synth)
    {
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
     * @return a collection of Voice objects, or an empty collection if no voice is available for the given locale.
     */
    public static Collection<Voice> getAvailableVoices(WaveformSynthesizer synth, Locale locale)
    {
        ArrayList<Voice> list = new ArrayList<Voice>();
        for (Voice v : allVoices) {
            if (v.synthesizer().equals(synth) && MaryUtils.subsumes(locale, v.getLocale())) {
                list.add(v);
            }
        }
        return list;
    }

    public static Voice getVoice(Locale locale, Gender gender)
    {
        for (Voice v : allVoices) {
            if (MaryUtils.subsumes(locale, v.getLocale()) && v.gender().equals(gender))
                return v;
        }
        return null; // no such voice found
    }

    public static Voice getVoice(Element voiceElement)
    {
        if (voiceElement == null ||
            !voiceElement.getTagName().equals(MaryXML.VOICE)) {
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
            Locale locale = MaryUtils.string2locale(
                voiceElement.getOwnerDocument().getDocumentElement()
                           .getAttribute("xml:lang"));
            if (locale == null) {
                locale = Locale.GERMAN;
            }
            v = Voice.getVoice(locale, new Gender(voiceGender));
        }
        return v;
    }


    public static Voice getDefaultVoice(Locale locale)
    {
        Voice v = defaultVoices.get(locale);
        if (v == null) v = getVoice(locale, FEMALE);
        if (v == null) v = getVoice(locale, MALE);
        if (v == null) logger.warn("Could not find default voice for locale "+locale);
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
            logger.debug("Guessing default voice `"+guessedVoice.getName()+"'");
        else
            logger.debug("Couldn't find any voice at all");

        return guessedVoice;
    }

    /**
     * Look up in the list of already-loaded lexicons whether the requested lexicon
     * is known; otherwise, load it.
     * @param lexiconClass
     * @param lexiconName
     * @return the requested lexicon, or null.
     */
    private static Lexicon getLexicon(String lexiconClass, String lexiconName)
    {
        if (lexiconClass == null) return null;
        // build the lexicon if not already built
        Lexicon lexicon = null;
        if (lexicons.containsKey(lexiconClass+lexiconName)) {
            return lexicons.get(lexiconClass+lexiconName);
        }
        // need to create a new lexicon instance
        try {
            logger.debug("...loading lexicon...");
            if (lexiconName == null) {
                lexicon = (Lexicon) Class.forName(lexiconClass).newInstance();
            } else { // lexiconName is String argument to constructor 
                Class lexCl = Class.forName(lexiconClass);
                Constructor lexConstr = lexCl.getConstructor(new Class[] {String.class});
                // will throw a NoSuchMethodError if constructor does not exist
                lexicon = (Lexicon) lexConstr.newInstance(new Object[] {lexiconName});
                
                // Apply our own custom addenda only for cmudict04:
                if (lexiconName.equals("cmudict04")) {
                    assert lexicon instanceof CMULexicon : "Expected lexicon to be a CMULexicon";
                    String customAddenda = MaryProperties.getFilename("english.lexicon.customAddenda");
                    if (customAddenda != null) {
                        //create lexicon with custom addenda
                        logger.debug("...loading custom addenda...");
                        lexicon.load();
                        //open addenda file
                        BufferedReader addendaIn = new BufferedReader(new InputStreamReader(
                                new FileInputStream(new File(customAddenda)),"UTF-8"));
                        String line;
                        while((line = addendaIn.readLine()) != null) {
                            if (!line.startsWith("#") && !line.equals("")) {
                                //add all words in addenda to lexicon
                                StringTokenizer tok = new StringTokenizer(line);
                                String word = tok.nextToken();
                                int numPhones = tok.countTokens();
                                String[] phones = new String[numPhones];
                                for (int i=0;i<phones.length;i++) {
                                    phones[i] = tok.nextToken();
                                }
                                ((CMULexicon) lexicon).addAddendum(word, null, phones);
                            }
                        }
                    }
                }
            }                    
        } catch (Exception ex) {
            logger.error("Could not load lexicon "+lexiconClass+"('"+lexiconName+"')", ex);
        }
        lexicons.put(lexiconClass+lexiconName, lexicon);
        return lexicon;
    }


    public static class Gender
    {
        String name;
        public Gender(String name) { this.name = name; }
        public String toString() { return name; }
        public boolean equals(Gender other) { return other.toString().equals(name); }
    }

}

