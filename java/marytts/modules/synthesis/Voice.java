/**
 * Copyright 2000-2006 DFKI GmbH.
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
package marytts.modules.synthesis;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
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

import marytts.datatypes.MaryData;
import marytts.datatypes.MaryDataType;
import marytts.datatypes.MaryXML;
import marytts.exceptions.MaryConfigurationException;
import marytts.exceptions.NoSuchPropertyException;
import marytts.exceptions.SynthesisException;
import marytts.features.FeatureProcessorManager;
import marytts.features.TargetFeatureComputer;
import marytts.modules.MaryModule;
import marytts.modules.ModuleRegistry;
import marytts.modules.phonemiser.Allophone;
import marytts.modules.phonemiser.AllophoneSet;
import marytts.nonverbal.BackchannelSynthesizer;
import marytts.server.MaryProperties;
import marytts.unitselection.interpolation.InterpolatingSynthesizer;
import marytts.unitselection.interpolation.InterpolatingVoice;
import marytts.util.MaryUtils;

import org.apache.log4j.Logger;
import org.w3c.dom.Element;

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

    /** A local map of already-instantiated target feature computers */
    private static Map<String,TargetFeatureComputer> knownTargetFeatureComputers = new HashMap<String,TargetFeatureComputer>();
    
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
    private TargetFeatureComputer targetFeatureComputer;
    private TargetFeatureComputer halfphoneTargetFeatureComputer;
    private Lexicon lexicon;
    private boolean backchannelSupport;
    private BackchannelSynthesizer backchannelSynthesizer;
    
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
            // No specific phoneme set for voice, use locale default
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
        
        // Determine target feature computer from config settings, if available
        targetFeatureComputer = initTargetFeatureProcessor("targetfeaturelister");
        halfphoneTargetFeatureComputer = initTargetFeatureProcessor("halfphone-targetfeaturelister");
        String lexiconClass = MaryProperties.getProperty(header+".lexiconClass");
        String lexiconName = MaryProperties.getProperty(header+".lexicon");
        lexicon = getLexicon(lexiconClass, lexiconName);
        backchannelSupport = MaryProperties.getBoolean(header+".backchannelSupport", false);
        if(backchannelSupport){
            backchannelSynthesizer = new BackchannelSynthesizer(this);
        }
    }

    /**
     * Try to determine a feature processor manager and a list of features for the 
     * given config setting. This will look in the voice-specific config settings
     * (prefix: <code>voice.(voicename).</code>) first, and then in the language-specific
     * settings (prefix: <code>(locale).</code>). If no feature processor manager or no 
     * list of features is found, null is returned.
     * @param configSetting "targetfeaturelister" for phone features, "halfphone-targetfeaturelister" 
     * for halfphone features
     * @return the target feature computer, or null if none is configered.
     */
    private TargetFeatureComputer initTargetFeatureProcessor(String configSetting) {
        // First, the feature processor manager to use:
        String keyVoiceFeatMgr = "voice."+getName()+"."+configSetting+".featuremanager";
        String featMgrClass = MaryProperties.getProperty(keyVoiceFeatMgr);
        if (featMgrClass == null) {
            String localePrefix = MaryProperties.localePrefix(locale);
            if (localePrefix == null) {
                throw new NoSuchPropertyException("Cannot determine config prefix for locale "+locale);
            }
            String keyLocaleFeatMgr = localePrefix+"."+configSetting+".featuremanager";
            featMgrClass = MaryProperties.getProperty(keyLocaleFeatMgr);
            if (featMgrClass == null) {
                logger.debug("No feature processor manager setting '"+keyVoiceFeatMgr+"' or '"+keyLocaleFeatMgr+"' -- will set to null.");
                return null;
            }
        }
        assert featMgrClass != null;
        // Now, the feature list to use:
        String keyVoiceFeatures = "voice."+getName()+"."+configSetting+".features";
        String features = MaryProperties.getProperty(keyVoiceFeatures);
        if (features == null) {
            String localePrefix = MaryProperties.localePrefix(locale);
            if (localePrefix == null) {
                throw new NoSuchPropertyException("Cannot determine config prefix for locale "+locale);
            }
            String keyLocaleFeatures = localePrefix+"."+configSetting+".features";
            features = MaryProperties.getProperty(keyLocaleFeatures);
            if (features == null) {
                logger.debug("No features setting '"+keyVoiceFeatures+"' or '"+keyLocaleFeatures+"' -- will set to null.");
                return null;
            }
        }
        assert features != null;
        String key = featMgrClass + "|" + features;
        TargetFeatureComputer tfc = knownTargetFeatureComputers.get(key);
        if (tfc == null) { // not known yet, initialise
            FeatureProcessorManager featMgr = null;
            try {
                featMgr = (FeatureProcessorManager) Class.forName(featMgrClass).newInstance();
            } catch (Exception e) {
                throw new IllegalArgumentException("Cannot initialise FeatureProcessorManager", e);
            }
            assert featMgr != null;
            tfc = new TargetFeatureComputer(featMgr, features);
            knownTargetFeatureComputers.put(key, tfc);
        }
        return tfc;
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
    
    
    public Vector<MaryModule> getPreferredModulesAcceptingType(MaryDataType type)
    {
        if (preferredModules == null && preferredModulesClasses != null) {
            // need to initialise the list of modules
            preferredModules = new Vector<MaryModule>();
            StringTokenizer st = new StringTokenizer(preferredModulesClasses, ", \t\n\r\f");
            while (st.hasMoreTokens()) {
                String className = st.nextToken();
                try {
                    MaryModule mm = ModuleRegistry.getModule(Class.forName(className));
                    if (mm == null) {
                        // need to create our own:
                        logger.warn("Module "+className+" is not in the standard list of modules -- will start our own, but will not be able to shut it down at the end.");
                        mm = (MaryModule) Class.forName(className).newInstance();
                        mm.startup();
                    }
                    preferredModules.add(mm);
                } catch (Exception e) {
                    logger.warn("Cannot initialise preferred module "+className+" for voice "+getName()+" -- skipping.");
                }
            }
        }
        if (preferredModules != null) {
            Vector<MaryModule> v = new Vector<MaryModule>();
            for (Iterator it = preferredModules.iterator(); it.hasNext(); ) {
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
     * Get the target feature computer to be used in conjunction with this voice
     * when computing target feature vectors,
     * e.g. for unit selection or HMM-based synthesis.
     * This can be voice-specific if there are config settings
     * <code>voice.(voicename).targetfeaturelister.featuremanager</code> and/or 
     * <code>voice.(voicename).targetfeaturelister.features</code>, or else this will default to the
     * locale specific versions
     * <code>(locale).targetfeaturelister.featuremanager</code> and
     * <code>(locale).targetfeaturelister.features</code>.
     * If there are no locale-specific versions either, null is returned.
     * @return a target feature computer object, or null.
     */
    public TargetFeatureComputer getTargetFeatureComputer()
    {
        return targetFeatureComputer;
    }

    /**
     * Get the target feature computer to be used in conjunction with this voice
     * when computing target feature vectors,
     * e.g. for unit selection or HMM-based synthesis.
     * This can be voice-specific if there are config settings
     * <code>voice.(voicename).halfphone-targetfeaturelister.featuremanager</code> and/or 
     * <code>voice.(voicename).halfphone-targetfeaturelister.features</code>, or else this will default to the
     * locale specific versions
     * <code>(locale).halfphone-targetfeaturelister.featuremanager</code> and
     * <code>(locale).halfphone-targetfeaturelister.features</code>.
     * If there are no locale-specific versions either, null is returned.
     * @return a target feature computer object, or null.
     */
    public TargetFeatureComputer getHalfphoneTargetFeatureComputer()
    {
        return halfphoneTargetFeatureComputer;
    }

    

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
            FreeTTSVoices.load(voice);
        }
        checkIfDefaultVoice(voice);
    }


    /**
     * Register the given voice along with the corresponding freetts voice. It will be contained in the list of available voices returned
     * by any subsequent calls to getAvailableVoices(). If the voice has the highest value of
     * <code>wantToBeDefault</code> for its locale it will be registered as the default voice for
     * its locale.
     * This value is set in the config file setting <code>voice.(name).want.to.be.default.voice</code>.
     */    
    public static void registerVoice(Voice maryVoice, com.sun.speech.freetts.Voice freettsVoice)
    {
        if (maryVoice == null || freettsVoice == null)
            throw new NullPointerException("Cannot register null voice.");
        if (!allVoices.contains(maryVoice)) {
            logger.info("Registering voice `" + maryVoice.getName() + "': " +
                         maryVoice.gender() + ", locale " + maryVoice.getLocale());
            allVoices.add(maryVoice);
            FreeTTSVoices.load(maryVoice, freettsVoice);
        }
        checkIfDefaultVoice(maryVoice);
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

    public static Voice getVoice(String name)
    {
        for (Iterator it = allVoices.iterator(); it.hasNext(); ) {
            Voice v = (Voice) it.next();
            if (v.hasName(name)) return v;
        }
        // Interpolating voices are created as needed:
        if (InterpolatingVoice.isInterpolatingVoiceName(name)) {
            InterpolatingSynthesizer interpolatingSynthesizer = null;
            for (Iterator it = allVoices.iterator(); it.hasNext(); ) {
                Voice v = (Voice) it.next();
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
    public static Collection getAvailableVoices() { return Collections.unmodifiableSet(allVoices); }

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
