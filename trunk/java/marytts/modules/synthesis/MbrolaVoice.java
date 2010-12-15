/**
 * Copyright 2000-2009 DFKI GmbH.
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
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.sound.sampled.AudioFormat;

import marytts.exceptions.MaryConfigurationException;
import marytts.server.MaryProperties;



public class MbrolaVoice extends Voice
{
    private String path;
    private Set<String> missingDiphones;
    private List<String> knownVoiceQualities;
    private int topStart;
    private int topEnd;
    private int baseStart;
    private int baseEnd;
    private Map<String, String> sampa2voiceMap;
    private Map<String, String> voice2sampaMap;



    public MbrolaVoice(String path, String[] nameArray, Locale locale,
            AudioFormat dbAudioFormat, WaveformSynthesizer synthesizer,
            Gender gender, int topStart, int topEnd, int baseStart,
            int baseEnd, String[] knownVoiceQualities,
            String missingDiphonesPath) 
    throws MaryConfigurationException
    {
        super(nameArray, locale, dbAudioFormat, synthesizer, gender);
        
        this.topStart = topStart;
        this.topEnd = topEnd;
        this.baseStart = baseStart;
        this.baseEnd = baseEnd;

        fillSampaMap();

        this.path = path;

        this.knownVoiceQualities = new ArrayList<String>();
        if (knownVoiceQualities != null)
            for (int j=0; j<knownVoiceQualities.length; j++)
                this.knownVoiceQualities.add(knownVoiceQualities[j]);
        
        if (missingDiphonesPath != null) {
            File missingDiphonesFile = new File(missingDiphonesPath);
            try {
                BufferedReader br =
                    new BufferedReader(new FileReader(missingDiphonesFile));
                String diphone = null;
                missingDiphones = new HashSet<String>();
                while ((diphone = br.readLine()) != null) {
                    missingDiphones.add(diphone);
                }
            } catch (IOException e) {
                //e.printStackTrace();
                missingDiphones = null;
            }
        }
    }

    public String path() { return path; }

    public int topStart() { return topStart; }
    public int topEnd() { return topEnd; }
    public int baseStart() { return baseStart; }
    public int baseEnd() { return baseEnd; }

    
    public boolean hasVoiceQuality(String vq) {
        return knownVoiceQualities.contains(vq);
    }
    
    
    
    private void fillSampaMap()
    {
        // Any phone inventory mappings?
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
        // For one-to-many mappings, '+' can be used to group phone symbols.
        // E.g., the line "EI->E:+I" would map "EI" to "E:" and "I" 
        entry = entry.replace('+', ' ');
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
            if (sampa2voiceMap == null) sampa2voiceMap = new HashMap<String, String>();
            sampa2voiceMap.put(parts[0].trim(), parts[1].trim());
        }
        if (v2s) {
            if (voice2sampaMap == null) voice2sampaMap = new HashMap<String, String>();
            voice2sampaMap.put(parts[1].trim(), parts[0].trim());
        }
    }

    /** Converts a single phonetic symbol in the voice phonetic alphabet representation
     * representation into its equivalent in MARY sampa representation.
     * @return the converted phone, or the input string if no known conversion exists.
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
     * @return the converted phone, or the input string if no known conversion exists.
     */
    public String sampa2voice(String sampaPhoneme)
    {
        if (sampa2voiceMap != null && sampa2voiceMap.containsKey(sampaPhoneme))
            return (String) sampa2voiceMap.get(sampaPhoneme);
        else
            return sampaPhoneme;
    }



    /** Convert the SAMPA dialect used in MARY into the SAMPA version
     * used in this voice. Allow for one-to-many translations,
     * taking care of duration and f0 target adjustments.
     * @return a vector of MBROLAPhoneme objects realising this phone
     * for this voice.
     */
    public Vector<MBROLAPhoneme> convertSampa(MBROLAPhoneme maryPhoneme)
    {
        Vector<MBROLAPhoneme> phones = new Vector<MBROLAPhoneme>();
        String marySampa = maryPhoneme.getSymbol();
        if (sampa2voiceMap != null && sampa2voiceMap.containsKey(marySampa)) {
            String newSampa = (String) sampa2voiceMap.get(marySampa);
            // Check if more than one phone:
            Vector<String> newSampas = new Vector<String>();
            StringTokenizer st = new StringTokenizer(newSampa);
            while (st.hasMoreTokens()) {
                newSampas.add(st.nextToken());
            }
            // Now, how many new phones do we have:
            int n = newSampas.size();
            int totalDur = maryPhoneme.getDuration();
            Vector<int []> allTargets = maryPhoneme.getTargets();
            // Distribute total duration evenly across the phones
            // and put the targets where they belong:
            for (int i=0; i<newSampas.size(); i++) {
                String sampa = (String) newSampas.get(i);
                int dur = totalDur / n;
                Vector<int []> newTargets = null;
                // Percentage limit belonging to this phone
                int maxP = 100 * (i+1) / n;
                boolean ok = true;
                while (allTargets != null && allTargets.size() > 0 && ok) {
                    int[] oldTarget = (int[]) allTargets.get(0);
                    if (oldTarget[0] <= maxP) {
                        // this target falls into this phone
                        int[] newTarget = new int[2];
                        newTarget[0] = oldTarget[0] * n; // percentage
                        newTarget[1] = oldTarget[1]; // f0
                        if (newTargets == null) newTargets = new Vector<int []>();
                        newTargets.add(newTarget);
                        // Delete from original list:
                        allTargets.remove(0);
                    } else {
                        ok = false;
                    }
                }
                MBROLAPhoneme mp = new MBROLAPhoneme
                    (sampa, dur, newTargets, maryPhoneme.getVoiceQuality());
                phones.add(mp);
            }
        } else { // just return the thing itself
            phones.add(maryPhoneme);
        }
        return phones;
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

    public Vector<MBROLAPhoneme> replaceDiphone(MBROLAPhoneme p1, MBROLAPhoneme p2)
    {
        Vector<MBROLAPhoneme> phones = new Vector<MBROLAPhoneme>();
        String s1 = p1.getSymbol();
        String s2 = p2.getSymbol();
        boolean solved = false;
        // Would inserting a short silence help?
        if (hasDiphone(s1 + "-_") && hasDiphone("_-" + s2)) {
            phones.add(p1);
            phones.add(new MBROLAPhoneme("_", 10, null, null));
            phones.add(p2);
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
                    phones.add(p1);
                    phones.add(p2);
                    solved = true;
                } else if (hasDiphone(s1a + "-N") && hasDiphone("N-" + s2)) {
                    p1.setSymbol(s1a);
                    p1.setDuration(p1.getDuration()-30);
                    phones.add(p1);
                    phones.add(new MBROLAPhoneme("N", 30, null, null));
                    phones.add(p2);
                    solved = true;
                }                    
            } else {
                if (s2.equals("E~") || s2.equals("e~")) s2a = "E:";
                else if (s2.equals("9^") || s2.equals("9~")) s2a = "9";
                else if (s2.equals("a~")) s2a = "O";
                else if (s2.equals("o~")) s2a = "o:";
                if (s2a != null && hasDiphone(s1 + "-" + s2a)) {
                    p2.setSymbol(s2a);
                    phones.add(p1);
                    phones.add(p2);
                    solved = true;
                } else if (s1a != null && s2a != null &&
                           hasDiphone(s1a + "-" + s2a)) {
                    p1.setSymbol(s1a);
                    p2.setSymbol(s2a);
                    phones.add(p1);
                    phones.add(p2);
                    solved = true;
                }
            }
        }
        // replace first a: with a?
        if (!solved && s1.equals("a:") && hasDiphone("a-" + s2)) {
            p1.setSymbol("a");
            phones.add(p1);
            phones.add(p2);
            solved = true;
        }
        // replace second a: with a?
        if (!solved && s2.equals("a:") && hasDiphone(s1 + "-a")) {
            p2.setSymbol("a");
            phones.add(p1);
            phones.add(p2);
            solved = true;
        }
        // replace first j with i:?
        if (!solved && s1.equals("j") && hasDiphone("i:-" + s2)) {
            p1.setSymbol("i:");
            phones.add(p1);
            phones.add(p2);
            solved = true;
        }
        // replace second j with i:?
        if (!solved && s2.equals("j") && hasDiphone(s1 + "-i:")) {
            p2.setSymbol("i:");
            phones.add(p1);
            phones.add(p2);
            solved = true;
        }
        // insert g before N?
        if (!solved && s2.equals("N") &&
            hasDiphone(s1 + "-g") && hasDiphone("g-N")) {
            phones.add(p1);
            phones.add(new MBROLAPhoneme("g", 10, null, null));
            phones.add(p2);
            solved = true;
        }
        // insert 6 after 9 or after O?
        if (!solved && (s1.equals("9") || s1.equals("O")) &&
            hasDiphone(s1 + "-6") && hasDiphone("6-" + s2)) {
            phones.add(p1);
            phones.add(new MBROLAPhoneme("6", 10, null, null));
            phones.add(p2);
            solved = true;
        }
        // insert @?
        if (!solved && hasDiphone(s1 + "-@") && hasDiphone("@-" + s2)) {
            phones.add(p1);
            phones.add(new MBROLAPhoneme("@", 10, null, null));
            phones.add(p2);
            solved = true;
        }
        // No remedy... :-(
        if (!solved) {
            phones.add(p1);
            phones.add(p2);
        }
        return phones;
    }
    
}

