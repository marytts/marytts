package de.dfki.lt.mary.modules.synthesis;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.Vector;

import javax.sound.sampled.AudioFormat;


import de.dfki.lt.mary.modules.synthesis.Voice.Gender;

public class MbrolaVoice extends Voice
{
    private String path;
    private Set missingDiphones;
    private List knownVoiceQualities;


    public MbrolaVoice(String path, String[] nameArray, Locale locale,
            AudioFormat dbAudioFormat, WaveformSynthesizer synthesizer,
            Gender gender, int topStart, int topEnd, int baseStart,
            int baseEnd, String[] knownVoiceQualities,
            String missingDiphonesPath) {
        super(nameArray, locale, dbAudioFormat, synthesizer, gender,
                topStart, topEnd, baseStart, baseEnd);
        
        this.path = path;

        this.knownVoiceQualities = new ArrayList();
        if (knownVoiceQualities != null)
            for (int j=0; j<knownVoiceQualities.length; j++)
                this.knownVoiceQualities.add(knownVoiceQualities[j]);
        
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
    }

    public String path() { return path; }

    public boolean hasVoiceQuality(String vq)
    { return knownVoiceQualities.contains(vq); }

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
        Vector<MBROLAPhoneme> phonemes = new Vector<MBROLAPhoneme>();
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
    
}
