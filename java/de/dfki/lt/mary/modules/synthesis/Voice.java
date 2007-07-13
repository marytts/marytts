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
package de.dfki.lt.mary.modules.synthesis;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
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

import org.apache.log4j.Logger;
import org.w3c.dom.Element;

import de.dfki.lt.freetts.ClusterUnitVoice;
import de.dfki.lt.freetts.DiphoneVoice;
import de.dfki.lt.mary.Mary;
import de.dfki.lt.mary.MaryData;
import de.dfki.lt.mary.MaryDataType;
import de.dfki.lt.mary.MaryProperties;
import de.dfki.lt.mary.MaryXML;
import de.dfki.lt.mary.modules.MaryModule;
import de.dfki.lt.mary.modules.phonemiser.Phoneme;
import de.dfki.lt.mary.modules.phonemiser.PhonemeSet;
import de.dfki.lt.mary.modules.phonemiser.Syllabifier;
import de.dfki.lt.mary.unitselection.interpolation.InterpolatingSynthesizer;
import de.dfki.lt.mary.unitselection.interpolation.InterpolatingVoice;
import de.dfki.lt.mary.util.MaryUtils;

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
          System.getProperty("os.arch").equals("i386")) ? // byteorder
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
    private static Set allVoices = new TreeSet(new Comparator() {
    	public int compare(Object o1, Object o2) {
    		Voice v1 = (Voice) o1;
    		Voice v2 = (Voice) o2;
    		// Return negative number if v1 should be listed before v2
    		int desireDelta = v2.wantToBeDefault - v1.wantToBeDefault; 
    		if (desireDelta != 0) return desireDelta;
    		// same desire -- sort alphabetically
    		return v2.getName().compareTo(v1.getName());
    	}
    });
    /** This map associates a value de.dfki.lt.mary.modules.synthesis.Voice to
     * a key Locale: */
    private static Map defaultVoices = new HashMap();
    /** logger */
    private static Logger logger = Logger.getLogger("Voice");



    private String path;
    private List names; // all the names under which this voice is known
    private Locale locale;
    private AudioFormat dbAudioFormat = null;
    private WaveformSynthesizer synthesizer;
    private Gender gender;
    private int topStart;
    private int topEnd;
    private int baseStart;
    private int baseEnd;
    private List knownVoiceQualities;
    private Map sampa2voiceMap;
    private Map voice2sampaMap;
    private boolean useVoicePAInOutput;
    private Set missingDiphones;
    private int wantToBeDefault;
    private PhonemeSet phonemeSet;
    String preferredModulesClasses;
    private Vector preferredModules;

    
    public Voice(String path, String[] nameArray, Locale locale, 
                 AudioFormat dbAudioFormat,
                 WaveformSynthesizer synthesizer,
                 Gender gender,
                 int topStart, int topEnd, int baseStart, int baseEnd,
                 String[] knownVoiceQualities,
                 String missingDiphonesPath) 
    {
        this.path = path;
        this.names = new ArrayList();
        for (int i=0; i<nameArray.length; i++)
            names.add(nameArray[i]);
        this.locale = locale;
        this.dbAudioFormat = dbAudioFormat;
        this.synthesizer = synthesizer;
        this.gender = gender;
        this.topStart = topStart;
        this.topEnd = topEnd;
        this.baseStart = baseStart;
        this.baseEnd = baseEnd;
        this.knownVoiceQualities = new ArrayList();
        if (knownVoiceQualities != null)
            for (int j=0; j<knownVoiceQualities.length; j++)
                this.knownVoiceQualities.add(knownVoiceQualities[j]);
        fillSampaMap();
        this.useVoicePAInOutput = MaryProperties.getBoolean("voice."+getName()+".use.voicepa.in.output", true);
        if (missingDiphonesPath != null) {
            File missingDiphonesFile = new File(missingDiphonesPath);
            try {
                BufferedReader br =
                    new BufferedReader(new FileReader(missingDiphonesFile));
                String diphone = null;
                missingDiphones = new HashSet();
                while ((diphone = br.readLine()) != null) {
                    missingDiphones.add(diphone);
                }
            } catch (IOException e) {
                //e.printStackTrace();
                missingDiphones = null;
            }
        }
        this.wantToBeDefault = MaryProperties.getInteger("voice."+getName()+".wants.to.be.default", 0);
        String phonemesetFilename = MaryProperties.getFilename("voice."+getName()+".phonemeset");
        if (phonemesetFilename == null && getLocale() != null) {
            // No specific phoneme set for voice, use locale default
            phonemesetFilename = MaryProperties.getFilename(MaryProperties.localePrefix(getLocale())+".phonemeset");
        }
        if (phonemesetFilename == null) {
            phonemeSet = null;
        } else {
            try {
                phonemeSet = PhonemeSet.getPhonemeSet(phonemesetFilename);
            } catch (Exception e) {
                phonemeSet = null;
            }
        }
        preferredModulesClasses = MaryProperties.getProperty("voice."+getName()+".preferredModules");
    }

    /**
     * Constructor for creating a MARY voice from a FreeTTS voice.
     * @param freeTTSVoice an existing FreeTTS voice
     * @param synthesizer the freeTTS synthesizer working with this voice.
     */
    public Voice(com.sun.speech.freetts.Voice freeTTSVoice, WaveformSynthesizer synthesizer)
    {
        this.path = null;
        this.names = new ArrayList();
        String domain = freeTTSVoice.getDomain();
        String name;
        if (domain.equals("general")) name = freeTTSVoice.getName();
        else name = freeTTSVoice.getName() + "_" + domain;
        names.add(name);
        this.locale = freeTTSVoice.getLocale();
        int samplingRate = 16000; // fallback
        if (freeTTSVoice instanceof DiphoneVoice) {
            samplingRate = ((DiphoneVoice)freeTTSVoice).getSampleInfo().getSampleRate();
        } else if (freeTTSVoice instanceof ClusterUnitVoice) {
            samplingRate = ((ClusterUnitVoice)freeTTSVoice).getSampleInfo().getSampleRate();
        }
        this.dbAudioFormat = new AudioFormat
        (AudioFormat.Encoding.PCM_SIGNED,
                samplingRate, // samples per second
                16, // bits per sample
                1, // mono
                2, // nr. of bytes per frame
                samplingRate, // nr. of frames per second
                true); // big-endian;
        this.synthesizer = synthesizer;
        if (freeTTSVoice.getGender().equals(com.sun.speech.freetts.Gender.FEMALE)) {
            this.gender = FEMALE;
        } else {
            this.gender = MALE;
        }
        this.topStart  = (int) (freeTTSVoice.getPitch() + freeTTSVoice.getPitchRange());
        this.topEnd    = (int) freeTTSVoice.getPitch();
        this.baseStart = (int) freeTTSVoice.getPitch();
        this.baseEnd   = (int) (freeTTSVoice.getPitch() - freeTTSVoice.getPitchRange());
        this.knownVoiceQualities = new ArrayList(0);
        fillSampaMap();
        this.wantToBeDefault = MaryProperties.getInteger("voice."+getName()+".wants.to.be.default", 0);
    }
    
    private void fillSampaMap()
    {
        // Any phoneme inventory mappings?
        String sampamapFilename = MaryProperties.getFilename("voice."+getName()+".sampamapfile");
        if (sampamapFilename != null) {
            logger.debug("For voice "+getName()+", filling sampa map from file "+sampamapFilename);
            try {
                BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(sampamapFilename), "UTF-8"));
                String line;
                while ((line = br.readLine()) != null) {
                    line = line.trim();
                    if (line.equals("") || line.startsWith("#")) {
                        continue; // ignore empty and comment lines
                    }
                    try {
                        addSampaMapEntry(line);
                    } catch (IllegalArgumentException iae) {
                        logger.warn("Ignoring invalid entry in sampa map file "+sampamapFilename);
                    }
                }
            } catch (IOException ioe) {
                logger.warn("Cannot open file '"+sampamapFilename+"' referenced in mary config file field voice."+getName()+".sampamapfile");
            }
            
        }
        String sampamap = MaryProperties.getProperty("voice."+getName()+".sampamap");
        if (sampamap != null) {
            logger.debug("For voice "+getName()+", filling sampa map from config file");
            for (StringTokenizer sst=new StringTokenizer(sampamap); sst.hasMoreTokens(); ) {
                try {
                    addSampaMapEntry(sst.nextToken());
                } catch (IllegalArgumentException iae) {
                    logger.warn("Ignoring invalid entry in mary config file, field voice."+getName()+".sampamap");
                }
            }
        }
    }

    private void addSampaMapEntry(String entry) throws IllegalArgumentException
    {
        boolean s2v = false;
        boolean v2s = false;
        String[] parts = null;
        // For one-to-many mappings, '+' can be used to group phoneme symbols.
        // E.g., the line "EI->E:+I" would map "EI" to "E:" and "I" 
        entry.replace('+', ' ');
        if (entry.indexOf("<->") != -1) {
            parts = entry.split("<->");
            s2v = true;
            v2s = true;
        } else if (entry.indexOf("->") != -1) {
            parts = entry.split("->");
            s2v = true;
        } else if (entry.indexOf("<-") != -1) {
            parts = entry.split("<-");
            v2s = true;
        }
        if (parts == null || parts.length != 2) { // invalid entry
            throw new IllegalArgumentException();
        }
        if (s2v) {
            if (sampa2voiceMap == null) sampa2voiceMap = new HashMap();
            sampa2voiceMap.put(parts[0].trim(), parts[1].trim());
        }
        if (v2s) {
            if (voice2sampaMap == null) voice2sampaMap = new HashMap();
            voice2sampaMap.put(parts[1].trim(), parts[0].trim());
        }
    }

    /** Converts a single phonetic symbol in the voice phonetic alphabet representation
     * representation into its equivalent in MARY sampa representation.
     * @return the converted phoneme, or the input string if no known conversion exists.
     */
    public String voice2sampa(String voicePhoneme)
    {
        if (voice2sampaMap != null && voice2sampaMap.containsKey(voicePhoneme))
            return (String) voice2sampaMap.get(voicePhoneme);
        else
            return voicePhoneme;
    }

    /** Converts a single phonetic symbol in MARY sampa representation into its
     * equivalent in voice-specific phonetic alphabet representation.
     * @return the converted phoneme, or the input string if no known conversion exists.
     */
    public String sampa2voice(String sampaPhoneme)
    {
        if (sampa2voiceMap != null && sampa2voiceMap.containsKey(sampaPhoneme))
            return (String) sampa2voiceMap.get(sampaPhoneme);
        else
            return sampaPhoneme;
    }

    /** Converts a full phonetic string including stress markers from MARY sampa
     * into the voice-specific representation. Syllable boundaries, if
     * present, will be ignored. Stress markers, if present, will lead to a "1"
     * appended to the voice-specific versions of the vowels in the syllable.
     * @return a List of String objects.
     */
    public List sampaString2voicePhonemeList(String sampa)
    {
        List voicePhonemeList = new ArrayList();
        StringTokenizer st = new StringTokenizer(sampa, "-_");
        while (st.hasMoreTokens()) {
            String syllable = st.nextToken();
            boolean stressed = false;
            if (syllable.startsWith("'")) {
                stressed = true;
            }
            Phoneme[] phonemes = PAConverter.sampa(getLocale()).splitIntoPhonemes(syllable);
            for (int i=0; i<phonemes.length; i++) {
                String voicePhoneme = sampa2voice(phonemes[i].name());
                if (stressed && phonemes[i].isVowel()) voicePhoneme = voicePhoneme + "1";
                voicePhonemeList.add(voicePhoneme);
            }
        }
        return voicePhonemeList;
    }

    /** Converts an array of phoneme symbol strings in voice-specific representation
     *  into a single MARY sampa string.
     * If stress is marked on voice phoneme symbols ("1" appended), a crude
     * syllabification is done on the sampa string.
     */
    public String voicePhonemeArray2sampaString(String[] voicePhonemes)
    {
        StringBuffer sampaBuf = new StringBuffer();
        for (int i=0; i<voicePhonemes.length; i++) {
            String sampa;
            if (voicePhonemes[i].endsWith("1")) {
                sampa = voice2sampa(voicePhonemes[i].substring(0, voicePhonemes[i].length()-1)) + "1"; 
            } else {
                sampa = voice2sampa(voicePhonemes[i]);
            }
            assert sampa != null;
            sampaBuf.append(sampa);
        }
        PhonemeSet sampaPhonemeSet = PAConverter.sampa(getLocale());
        if (sampaPhonemeSet != null) {
            Syllabifier syllabifier = sampaPhonemeSet.getSyllabifier();
            return syllabifier.syllabify(sampaBuf.toString());
        }
        // Fallback if we have no syllabifier:
        return sampaBuf.toString();
    }

    /**
     * Get the SAMPA phoneme set associated with this voice.
     * @return
     */
    public PhonemeSet getSampaPhonemeSet()
    {
        return phonemeSet;
    }

    /**
     * If a phoneme set is available, return a Phoneme object
     * for the given sampa symbol.
     * @param sampaSymbol sampa symbol for one phoneme -- use voice2sampa() to
     * create this from a voice phoneme symbol.
     * @return a Phoneme object, or null.
     */
    public Phoneme getSampaPhoneme(String sampaSymbol)
    {
        if (phonemeSet == null) return null;
        return phonemeSet.getPhoneme(sampaSymbol);
    }
    
    
    public Vector getPreferredModulesAcceptingType(MaryDataType type)
    {
        if (preferredModules == null && preferredModulesClasses != null) {
            // need to initialise the list of modules
            preferredModules = new Vector();
            StringTokenizer st = new StringTokenizer(preferredModulesClasses, ", \t\n\r\f");
            while (st.hasMoreTokens()) {
                String className = st.nextToken();
                try {
                    MaryModule mm = Mary.getModule(Class.forName(className));
                    if (mm == null) {
                        // need to create our own:
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
            Vector v = new Vector();
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
    

    public String path() { return path; }
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
    public int topStart() { return topStart; }
    public int topEnd() { return topEnd; }
    public int baseStart() { return baseStart; }
    public int baseEnd() { return baseEnd; }

    public boolean hasVoiceQuality(String vq)
    { return knownVoiceQualities.contains(vq); }

    /**
     * Whether to use this voice's phonetic alphabet in the output.
     */
    public boolean useVoicePAInOutput() { return useVoicePAInOutput; }

    /** Convert the SAMPA dialect used in MARY into the SAMPA version
     * used in this voice. Allow for one-to-many translations,
     * taking care of duration and f0 target adjustments.
     * @return a vector of MBROLAPhoneme objects realising this phoneme
     * for this voice.
     */
    public Vector convertSampa(MBROLAPhoneme maryPhoneme)
    {
        Vector phonemes = new Vector();
        String marySampa = maryPhoneme.getSymbol();
        if (sampa2voiceMap != null && useVoicePAInOutput && sampa2voiceMap.containsKey(marySampa)) {
            String newSampa = (String) sampa2voiceMap.get(marySampa);
            // Check if more than one phoneme:
            Vector newSampas = new Vector();
            StringTokenizer st = new StringTokenizer(newSampa);
            while (st.hasMoreTokens()) {
                newSampas.add(st.nextToken());
            }
            // Now, how many new phonemes do we have:
            int n = newSampas.size();
            int totalDur = maryPhoneme.getDuration();
            Vector allTargets = maryPhoneme.getTargets();
            // Distribute total duration evenly across the phonemes
            // and put the targets where they belong:
            for (int i=0; i<newSampas.size(); i++) {
                String sampa = (String) newSampas.get(i);
                int dur = totalDur / n;
                Vector newTargets = null;
                // Percentage limit belonging to this phoneme
                int maxP = 100 * (i+1) / n;
                boolean ok = true;
                while (allTargets != null && allTargets.size() > 0 && ok) {
                    int[] oldTarget = (int[]) allTargets.get(0);
                    if (oldTarget[0] <= maxP) {
                        // this target falls into this phoneme
                        int[] newTarget = new int[2];
                        newTarget[0] = oldTarget[0] * n; // percentage
                        newTarget[1] = oldTarget[1]; // f0
                        if (newTargets == null) newTargets = new Vector();
                        newTargets.add(newTarget);
                        // Delete from original list:
                        allTargets.remove(0);
                    } else {
                        ok = false;
                    }
                }
                MBROLAPhoneme mp = new MBROLAPhoneme
                    (sampa, dur, newTargets, maryPhoneme.getVoiceQuality());
                phonemes.add(mp);
            }
        } else { // just return the thing itself
            phonemes.add(maryPhoneme);
        }
        return phonemes;
    }

    public boolean hasDiphone(MBROLAPhoneme p1, MBROLAPhoneme p2)
    {
        return hasDiphone(p1.getSymbol() + "-" + p2.getSymbol());
    }

    /**
     * Verify whether a diphone (p1-p2) is in the list of missing diphones.
     */
    public boolean hasDiphone(String diphone)
    {
        if (missingDiphones != null && missingDiphones.contains(diphone)) {
            return false;
        }
        return true;
    }

    public Vector replaceDiphone(MBROLAPhoneme p1, MBROLAPhoneme p2)
    {
        Vector phonemes = new Vector();
        String s1 = p1.getSymbol();
        String s2 = p2.getSymbol();
        boolean solved = false;
        // Would inserting a short silence help?
        if (hasDiphone(s1 + "-_") && hasDiphone("_-" + s2)) {
            phonemes.add(p1);
            phonemes.add(new MBROLAPhoneme("_", 10, null, null));
            phonemes.add(p2);
            solved = true;
        }
        // Would denasalising one of them help?
        if (!solved) {
            String s1a = null;
            String s2a = s2;
            if (s1.equals("E~") || s1.equals("e~")) s1a = "E";
            else if (s1.equals("9^") || s1.equals("9~")) s1a = "9";
            else if (s1.equals("a~")) s1a = "O";
            else if (s1.equals("o~")) s1a = "o:";
            if (s1a != null) {
                if (hasDiphone(s1a + "-" + s2)) {
                    p1.setSymbol(s1a);
                    phonemes.add(p1);
                    phonemes.add(p2);
                    solved = true;
                } else if (hasDiphone(s1a + "-N") && hasDiphone("N-" + s2)) {
                    p1.setSymbol(s1a);
                    p1.setDuration(p1.getDuration()-30);
                    phonemes.add(p1);
                    phonemes.add(new MBROLAPhoneme("N", 30, null, null));
                    phonemes.add(p2);
                    solved = true;
                }                    
            } else {
                if (s2.equals("E~") || s2.equals("e~")) s2a = "E:";
                else if (s2.equals("9^") || s2.equals("9~")) s2a = "9";
                else if (s2.equals("a~")) s2a = "O";
                else if (s2.equals("o~")) s2a = "o:";
                if (s2a != null && hasDiphone(s1 + "-" + s2a)) {
                    p2.setSymbol(s2a);
                    phonemes.add(p1);
                    phonemes.add(p2);
                    solved = true;
                } else if (s1a != null && s2a != null &&
                           hasDiphone(s1a + "-" + s2a)) {
                    p1.setSymbol(s1a);
                    p2.setSymbol(s2a);
                    phonemes.add(p1);
                    phonemes.add(p2);
                    solved = true;
                }
            }
        }
        // replace first a: with a?
        if (!solved && s1.equals("a:") && hasDiphone("a-" + s2)) {
            p1.setSymbol("a");
            phonemes.add(p1);
            phonemes.add(p2);
            solved = true;
        }
        // replace second a: with a?
        if (!solved && s2.equals("a:") && hasDiphone(s1 + "-a")) {
            p2.setSymbol("a");
            phonemes.add(p1);
            phonemes.add(p2);
            solved = true;
        }
        // replace first j with i:?
        if (!solved && s1.equals("j") && hasDiphone("i:-" + s2)) {
            p1.setSymbol("i:");
            phonemes.add(p1);
            phonemes.add(p2);
            solved = true;
        }
        // replace second j with i:?
        if (!solved && s2.equals("j") && hasDiphone(s1 + "-i:")) {
            p2.setSymbol("i:");
            phonemes.add(p1);
            phonemes.add(p2);
            solved = true;
        }
        // insert g before N?
        if (!solved && s2.equals("N") &&
            hasDiphone(s1 + "-g") && hasDiphone("g-N")) {
            phonemes.add(p1);
            phonemes.add(new MBROLAPhoneme("g", 10, null, null));
            phonemes.add(p2);
            solved = true;
        }
        // insert 6 after 9 or after O?
        if (!solved && (s1.equals("9") || s1.equals("O")) &&
            hasDiphone(s1 + "-6") && hasDiphone("6-" + s2)) {
            phonemes.add(p1);
            phonemes.add(new MBROLAPhoneme("6", 10, null, null));
            phonemes.add(p2);
            solved = true;
        }
        // insert @?
        if (!solved && hasDiphone(s1 + "-@") && hasDiphone("@-" + s2)) {
            phonemes.add(p1);
            phonemes.add(new MBROLAPhoneme("@", 10, null, null));
            phonemes.add(p2);
            solved = true;
        }
        // No remedy... :-(
        if (!solved) {
            phonemes.add(p1);
            phonemes.add(p2);
        }
        return phonemes;
    }
    

    /**
     * Synthesize a list of tokens and boundaries with the waveform synthesizer
     * providing this voice.
     */
    public AudioInputStream synthesize(List tokensAndBoundaries)
        throws SynthesisException
    {
        return synthesizer.synthesize(tokensAndBoundaries, this);
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
        Voice currentDefault = (Voice) defaultVoices.get(locale);
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
    public static Collection getAvailableVoices(Locale locale)
    {
        ArrayList list = new ArrayList();
        Iterator it = allVoices.iterator();
        while (it.hasNext()) {
            Voice v = (Voice) it.next();
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
    public static Collection getAvailableVoices(WaveformSynthesizer synth)
    {
        if (synth == null) {
            throw new NullPointerException("Got null WaveformSynthesizer");
        }
        ArrayList list = new ArrayList();
        Iterator it = allVoices.iterator();
        while (it.hasNext()) {
            Voice v = (Voice) it.next();
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
    public static Collection getAvailableVoices(WaveformSynthesizer synth, Locale locale)
    {
        ArrayList list = new ArrayList();
        Iterator it = allVoices.iterator();
        while (it.hasNext()) {
            Voice v = (Voice) it.next();
            if (v.synthesizer().equals(synth) && MaryUtils.subsumes(locale, v.getLocale())) {
                list.add(v);
            }
        }
        return list;
    }

    public static Voice getVoice(Locale locale, Gender gender)
    {
        for (Iterator it = allVoices.iterator(); it.hasNext(); ) {
            Voice v = (Voice) it.next();
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
        Voice v = (Voice) defaultVoices.get(locale);
        if (v == null) v = getVoice(locale, FEMALE);
        if (v == null) v = getVoice(locale, MALE);
        if (v == null) logger.warn("Could not find default voice for locale "+locale);
        return v;
    }


    public static Voice getSuitableVoice(MaryData d) {
        Locale docLocale = d.type().getLocale();
        if (docLocale == null && d.type().isXMLType() && d.getDocument() != null
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



    public static class Gender
    {
        String name;
        public Gender(String name) { this.name = name; }
        public String toString() { return name; }
        public boolean equals(Gender other) { return other.toString().equals(name); }
    }

}
