/**
 * Copyright 2002 DFKI GmbH.
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

package marytts.language.de;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.SortedMap;
import java.util.StringTokenizer;
import java.util.TreeMap;

import marytts.exceptions.MaryConfigurationException;
import marytts.fst.FSTLookup;
import marytts.language.de.phonemiser.Inflection;
import marytts.language.de.phonemiser.PhonemiseDenglish;
import marytts.language.de.phonemiser.Result;
import marytts.modules.synthesis.PAConverter;
import marytts.config.MaryProperties;
import marytts.util.MaryUtils;

import marytts.data.Utterance;
import marytts.data.Sequence;
import marytts.data.Relation;
import marytts.data.SupportedSequenceType;
import marytts.data.utils.IntegerPair;
import marytts.data.item.linguistic.Word;
import marytts.data.item.phonology.Phoneme;
import marytts.data.item.phonology.Syllable;
import marytts.data.item.phonology.Accent;

import com.google.common.base.Splitter;

import org.apache.commons.io.FileUtils;
import org.w3c.dom.Document;

import org.apache.logging.log4j.core.Appender;
/**
 * The phonemiser module -- java implementation.
 *
 * @author Marc Schr&ouml;der
 */

public class JPhonemiser extends marytts.modules.nlp.JPhonemiser {
    private Inflection inflection;
    private FSTLookup usEnglishLexicon = null;
    private String logUnknownFileName = null;
    private Map<String, Integer> unknown2Frequency = null;
    private String logEnglishFileName = null;
    private Map<String, Integer> english2Frequency = null;
    private PhonemiseDenglish phonemiseDenglish;

    public JPhonemiser() throws IOException, MaryConfigurationException {
        super("JPhonemiser_de", "de.allophoneset", "de.userdict", "de.lexicon", "de.lettertosound");
    }

    public void startup() throws Exception {
        super.startup();
        phonemiseDenglish = new PhonemiseDenglish(this);
        inflection = new Inflection();

        if (MaryProperties.getBoolean("de.phonemiser.logunknown")) {
            String logBasepath = MaryProperties.maryBase() + File.separator + "log" + File.separator;
            File logDir = new File(logBasepath);
            try {
                if (!logDir.isDirectory()) {
                    logger.info("Creating log directory " + logDir.getCanonicalPath());
                    FileUtils.forceMkdir(logDir);
                }
                logUnknownFileName = MaryProperties.getFilename("de.phonemiser.logunknown.filename",
                                     logBasepath + "de_unknown.txt");
                unknown2Frequency = new HashMap<String, Integer>();
                logEnglishFileName = MaryProperties.getFilename("de.phonemiser.logenglish.filename",
                                     logBasepath + "de_english-words.txt");
                english2Frequency = new HashMap<String, Integer>();
            } catch (IOException e) {
                logger.info("Could not create log directory " + logDir.getCanonicalPath() + " Logging disabled!",
                            e);
            }
        }
        if (MaryProperties.getBoolean("de.phonemiser.useenglish")) {
            InputStream usLexStream = MaryProperties.getStream("en_US.lexicon");
            if (usLexStream != null) {
                try {
                    usEnglishLexicon = new FSTLookup(usLexStream, MaryProperties.getProperty("en_US.lexicon"));
                } catch (Exception e) {
                    logger.info("Cannot load English lexicon '" + MaryProperties.getProperty("en_US.lexicon") + "'", e);
                }
            }
        }
    }

    public void shutdown() {
        if (logUnknownFileName != null || logEnglishFileName != null) {
            try {
                /* print unknown words */

                // open file
                PrintWriter logUnknown = new PrintWriter(
                    new OutputStreamWriter(new FileOutputStream(logUnknownFileName), "UTF-8"));
                // sort the words
                Set<String> unknownWords = unknown2Frequency.keySet();
                SortedMap<Integer, List<String>> freq2Unknown = new TreeMap<Integer, List<String>>();

                for (String nextUnknown : unknownWords) {
                    int nextFreq = unknown2Frequency.get(nextUnknown);
                    // logUnknown.println(nextFreq+" "+nextUnknown);
                    if (freq2Unknown.containsKey(nextFreq)) {
                        List<String> unknowns = freq2Unknown.get(nextFreq);
                        unknowns.add(nextUnknown);
                    } else {
                        List<String> unknowns = new ArrayList<String>();
                        unknowns.add(nextUnknown);
                        freq2Unknown.put(nextFreq, unknowns);
                    }
                }
                // print the words
                for (int nextFreq : freq2Unknown.keySet()) {
                    List<String> unknowns = freq2Unknown.get(nextFreq);
                    for (int i = 0; i < unknowns.size(); i++) {
                        String unknownWord = (String) unknowns.get(i);
                        logUnknown.println(nextFreq + " " + unknownWord);
                    }

                }
                // close file
                logUnknown.flush();
                logUnknown.close();

                /* print english words */
                // open the file
                PrintWriter logEnglish = new PrintWriter(
                    new OutputStreamWriter(new FileOutputStream(logEnglishFileName), "UTF-8"));
                // sort the words
                SortedMap<Integer, List<String>> freq2English = new TreeMap<Integer, List<String>>();
                for (String nextEnglish : english2Frequency.keySet()) {
                    int nextFreq = english2Frequency.get(nextEnglish);
                    if (freq2English.containsKey(nextFreq)) {
                        List<String> englishWords = freq2English.get(nextFreq);
                        englishWords.add(nextEnglish);
                    } else {
                        List<String> englishWords = new ArrayList<String>();
                        englishWords.add(nextEnglish);
                        freq2English.put(nextFreq, englishWords);
                    }

                }
                // print the words
                for (int nextFreq : freq2English.keySet()) {
                    List<String> englishWords = freq2English.get(nextFreq);
                    for (int i = 0; i < englishWords.size(); i++) {
                        logEnglish.println(nextFreq + " " + englishWords.get(i));
                    }
                }
                // close file
                logEnglish.flush();
                logEnglish.close();

            } catch (Exception e) {
                logger.info("Error printing log files for english and unknown words", e);
            }
        }
    }

    @Override

    public Utterance process(Utterance utt, MaryProperties configuration, Appender app) throws Exception {

        Sequence<Word> words = (Sequence<Word>) utt.getSequence(SupportedSequenceType.WORD);
        Sequence<Syllable> syllables = new Sequence<Syllable>();
        ArrayList<IntegerPair> alignment_word_syllable = new ArrayList<IntegerPair>();

        Sequence<Phoneme> phones = new Sequence<Phoneme>();
        ArrayList<IntegerPair> alignment_syllable_phone = new ArrayList<IntegerPair>();

        Relation rel_words_sent = utt.getRelation(SupportedSequenceType.SENTENCE,
                                  SupportedSequenceType.WORD)
                                  .getReverse();
        HashSet<IntegerPair> alignment_word_phrase = new HashSet<IntegerPair>();

        for (int i_word = 0; i_word < words.size(); i_word++) {
            Word w = words.get(i_word);

            String text;

            if (w.soundsLike() != null) {
                text = w.soundsLike();
            } else {
                text = w.getText();
            }

            // Get POS
            String pos = w.getPOS();

            boolean isEnglish = false;
            if ((w.getAlternativeLocale() != null) && (w.getAlternativeLocale().equals(Locale.ENGLISH))) {
                isEnglish = true;
            }

            // Ok adapt phonemes now
            ArrayList<String> phonetisation_string = new ArrayList<String>();
            if (maybePronounceable(text, pos)) {

                // If text consists of several parts (e.g., because that was
                // inserted into the sounds_like attribute), each part
                // is transcribed separately.
                StringBuilder ph = new StringBuilder();
                String g2p_method = null;
                StringTokenizer st = new StringTokenizer(text, " -");
                while (st.hasMoreTokens()) {
                    String graph = st.nextToken();
                    StringBuilder helper = new StringBuilder();
                    String phon = null;

                    if (isEnglish && usEnglishLexicon != null) {
                        phon = phonemiseEn(graph);
                        if (phon != null) {
                            helper.append("foreign:en");
                        }
                    }

                    if (phon == null) {
                        phon = phonemise(graph, pos, helper);
                    }

                    // FIXME: what does it mean : null result should not be
                    // processed
                    if (phon == null) {
                        continue;
                    }

                    if (ph.length() == 0) {
                        g2p_method = helper.toString();
                    }

                    phonetisation_string.add(phon);
                }

                if (phonetisation_string.size() > 0) {

                    createSubStructure(w, phonetisation_string, allophoneSet, syllables, phones,
                                       alignment_syllable_phone, i_word, alignment_word_syllable);

                    // Adapt G2P method
                    w.setG2PMethod(g2p_method);
                }
            }
        }

        // Relation word/syllable
        utt.addSequence(SupportedSequenceType.SYLLABLE, syllables);
        Relation rel_word_syllable = new Relation(words, syllables, alignment_word_syllable);
        utt.setRelation(SupportedSequenceType.WORD, SupportedSequenceType.SYLLABLE, rel_word_syllable);

        utt.addSequence(SupportedSequenceType.PHONE, phones);
        Relation rel_syllable_phone = new Relation(syllables, phones, alignment_syllable_phone);
        utt.setRelation(SupportedSequenceType.SYLLABLE, SupportedSequenceType.PHONE, rel_syllable_phone);

        return utt;
    }

    /**
     * Phonemise the word text. This starts with a simple lexicon lookup,
     * followed by some heuristics, and finally applies letter-to-sound rules if
     * nothing else was successful.
     *
     * @param text
     *            the textual (graphemic) form of a word.
     * @param pos
     *            pos
     * @param g2pMethod
     *            This is an awkward way to return a second String parameter via
     *            a StringBuilder. If a phonemisation of the text is found, this
     *            parameter will be filled with the method of phonemisation
     *            ("lexicon", ... "rules").
     * @return a phonemisation of the text if one can be generated, or null if
     *         no phonemisation method was successful.
     */
    @Override
    public String phonemise(String text, String pos, StringBuilder g2pMethod) {
        // First, try a simple userdict and lexicon lookup:
        String result = userdictLookup(text, pos);
        if (result != null) {
            g2pMethod.append("userdict");
            return result;
        }
        result = lexiconLookup(text, pos);
        if (result != null) {
            g2pMethod.append("lexicon");
            return result;
        }
        /**
         * // Not found? Try a compound "analysis": result =
         * compoundSearch(text); //logger.debug("Compound here:
         * "+compoundSearch(text)); if (result != null) {
         * g2pMethod.append("compound"); return result; }
         **/

        // Lookup attempts failed. Try normalising exotic letters
        // (diacritics on vowels, etc.), look up again:
        String normalised = MaryUtils.normaliseUnicodeLetters(text, Locale.GERMAN);
        if (!normalised.equals(text)) {
            // First, try a simple userdict and lexicon lookup:
            result = userdictLookup(normalised, pos);
            if (result != null) {
                g2pMethod.append("userdict");
                return result;
            }
            result = lexiconLookup(normalised, pos);
            if (result != null) {
                g2pMethod.append("lexicon");
                return result;
            }
            /**
             * // Not found? Try a compound "analysis": result =
             * compoundSearch(normalised); if (result != null) {
             * g2pMethod.append("compound"); return result; }
             **/
        }

        // plain English word must be looked up in English lexicon before
        // phonemiseDenglish starts
        if (usEnglishLexicon != null) {
            String englishTranscription = phonemiseEn(text);
            if (englishTranscription != null) {
                g2pMethod.append("foreign:en");
                logger.debug(text + " is English");
                if (logEnglishFileName != null) {
                    String englishText = text.trim();
                    if (english2Frequency.containsKey(englishText)) {
                        int textFreq = english2Frequency.get(englishText);
                        textFreq++;
                        english2Frequency.put(englishText, textFreq);
                    } else {
                        english2Frequency.put(englishText, 1);
                    }
                }
                return englishTranscription;
            }
        }
        Result resultingWord = null;
        boolean usedOtherLanguageToPhonemise = false;
        try {
            resultingWord = phonemiseDenglish.processWord(text, usEnglishLexicon != null);
            result = resultingWord.getTranscription();
            usedOtherLanguageToPhonemise = resultingWord.isUsedOtherLanguageToPhonemise();
        } catch (NullPointerException e) {
            logger.debug(String.format("Word is Null: ", e.getMessage()));
        }
        // logger.debug("input for PD: "+text);
        if (result != null) {
            result = allophoneSet.splitAllophoneString(result);
            if (usedOtherLanguageToPhonemise) {
                g2pMethod.append("phonemiseDenglish");
                return result;
            } else {
                g2pMethod.append("compound");
                return result;
            }
        }

        // Cannot find it in the lexicon -- apply letter-to-sound rules
        // to the normalised form

        String phones = ""; // added
        try {
            phones = lts.predictPronunciation(normalised); // added
            result = lts.syllabify(phones);
        } catch (IllegalArgumentException e) {
            logger.error(String.format("Problem with token <%s> [%s]: %s", normalised, phones, e.getMessage()));
        } catch (ClassCastException e) {
            logger.error(String.format("Problem with token <%s> : %s", normalised, e.getMessage())); // added
        }
        if (result != null) {
            if (logUnknownFileName != null) {
                String unknownText = text.trim();
                if (unknown2Frequency.containsKey(unknownText)) {
                    int textFreq = unknown2Frequency.get(unknownText);
                    textFreq++;
                    unknown2Frequency.put(unknownText, textFreq);
                } else {
                    unknown2Frequency.put(unknownText, new Integer(1));
                }
            }
            g2pMethod.append("rules");
            return result;
        }
        return null;
    }

    /**
     * Try to determine an English transcription of the text according to
     * English rules, but using German Sampa.
     *
     * @param text
     *            Word to transcribe
     * @return the transcription, or null if none could be determined.
     */
    public String phonemiseEn(String text) {
        assert usEnglishLexicon != null;
        // We get here only if there is an English lexicon
        String normalisedEn = MaryUtils.normaliseUnicodeLetters(text, Locale.US);
        normalisedEn = normalisedEn.toLowerCase();
        String[] transcriptions = usEnglishLexicon.lookup(normalisedEn);
        assert transcriptions != null; // if nothing is found, an array of
        // length 0 is returned.
        if (transcriptions.length == 0) {
            return null;
        }
        String usSampa = transcriptions[0];

        String deSampa = PAConverter.sampaEnString2sampaDeString(usSampa);
        // logger.debug("converted "+usSampa+" to "+deSampa);
        return deSampa;
    }
}
