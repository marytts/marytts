/**
 * Copyright 2002-2008 DFKI GmbH.
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
package marytts.modules.nlp;

// IO
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

// Collections
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Parsing
import java.util.StringTokenizer;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import com.google.common.base.Splitter;

// Locale
import java.util.Locale;

// Configuration
import marytts.config.MaryConfiguration;
import marytts.exceptions.MaryConfigurationException;

// Main mary
import marytts.MaryException;
import marytts.fst.FSTLookup;
import marytts.modules.nlp.phonemiser.AllophoneSet;
import marytts.modules.nlp.phonemiser.TrainedLTS;
import marytts.modules.MaryModule;
import marytts.util.MaryUtils;

// Data
import marytts.data.Utterance;
import marytts.data.Sequence;
import marytts.data.Relation;
import marytts.data.SupportedSequenceType;
import marytts.data.utils.IntegerPair;
import marytts.data.item.linguistic.Word;
import marytts.data.item.acoustic.Segment;
import marytts.data.item.phonology.Phoneme;
import marytts.data.item.phonology.NSS;
import marytts.data.item.phonology.Syllable;
import marytts.data.item.phonology.Accent;

import marytts.phonetic.AlphabetFactory;
import marytts.phonetic.converter.Alphabet;

/**
 * The phonemiser module -- java implementation.
 *
 * @author Marc Schr&ouml;der, Sathish
 * @author ingmar
 */

public abstract class JPhonemiser extends MaryModule {
    protected final String SYL_SEP = "-";
    protected final String FIRST_STRESS = "'";
    protected final String SECOND_STRESS = ",";

    protected Alphabet sampa2ipa;
    protected Map<String, List<String>> userdict;
    protected FSTLookup lexicon;
    protected TrainedLTS lts;
    protected boolean removeTrailingOneFromPhones = true;
    protected Locale locale;
    protected AllophoneSet allophoneSet;

    public Pattern punctuationPosRegex;
    protected Pattern unpronounceablePosRegex;


    protected JPhonemiser(Locale locale) throws MaryConfigurationException {
	super("phonemiser");

	try {
	    sampa2ipa = AlphabetFactory.getAlphabet("sampa");
	} catch (Exception ex) {
	    throw new MaryConfigurationException("Cannot instantiate sampa alphabet converter", ex);
	}

	String defaultRegex = "\\$PUNCT";
	punctuationPosRegex = Pattern.compile(defaultRegex);

	defaultRegex = "^[^a-zA-Z]+$";
	unpronounceablePosRegex = Pattern.compile(defaultRegex);

	setLocale(locale);
    }

    protected void setDescription() {
	this.description = "Default phonemiser of MaryTTS";
    }



    public void checkStartup() throws MaryConfigurationException {
	if (punctuationPosRegex == null)
	    throw new MaryConfigurationException("Problem as the regular expression for punctuation is not defined");

	if (unpronounceablePosRegex == null)
	    throw new MaryConfigurationException("Problem as the regular expression for unpronounceable tokens is not defined");

	if (lexicon == null)
	    throw new MaryConfigurationException("Problem as the lexicon is not defined");

	if (allophoneSet == null)
	    throw new MaryConfigurationException("Problem as the allophone set is not defined");

	if (lts == null)
	    throw new MaryConfigurationException("Problem as the lts model is not defined");
    }

    /**
     *  Check if the input contains all the information needed to be
     *  processed by the module.
     *
     *  @param utt the input utterance
     *  @throws MaryException which indicates what is missing if something is missing
     */
    public void checkInput(Utterance utt) throws MaryException {
        if (!utt.hasSequence(SupportedSequenceType.SENTENCE)) {
            throw new MaryException("Sentence sequence is missing", null);
        }
        if (!utt.hasSequence(SupportedSequenceType.WORD)) {
            throw new MaryException("Word sequence is missing", null);
        }
    }

    public Utterance process(Utterance utt, MaryConfiguration configuration) throws MaryException {

        Sequence<Word> words = (Sequence<Word>) utt.getSequence(SupportedSequenceType.WORD);

        // Prepare new sequences
        Sequence<Syllable> syllables = new Sequence<Syllable>();
        Sequence<Phoneme> phones = new Sequence<Phoneme>();
        Sequence<NSS> nss_seq = new Sequence<NSS>();
        Sequence<Segment> segments = new Sequence<Segment>();

        // Prepare alignment
        ArrayList<IntegerPair> alignment_syllable_phone = new ArrayList<IntegerPair>();
        ArrayList<IntegerPair> alignment_phone_seg = new ArrayList<IntegerPair>();
        ArrayList<IntegerPair> alignment_nss_seg = new ArrayList<IntegerPair>();
        ArrayList<IntegerPair> alignment_word_seg = new ArrayList<IntegerPair>();

        // FIXME: add pause de facto
        nss_seq.add(new NSS("start"));
        segments.add(new Segment(0));
        alignment_nss_seg.add(new IntegerPair(nss_seq.size()-1, segments.size()-1));

        for (int i_word = 0; i_word < words.size(); i_word++) {
            // Get word
            Word w = words.get(i_word);

            // Get the wanted text
            String text = w.getText();
            if (w.soundsLike() != null) {
                text = w.soundsLike();
            }

            // Get POS
            String pos = w.getPOS();

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
                    String phon = phonemise(graph, pos, helper);

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
                    createSubStructure(w, phonetisation_string,
                                       syllables, phones, segments,
                                       alignment_syllable_phone, alignment_phone_seg,
                                       alignment_word_seg, i_word);

                    // Adapt G2P method
                    w.setG2PMethod(g2p_method);
                }
            } else {
                nss_seq.add(new NSS("pau"));
                double new_start = segments.get(segments.size()-1).getStart() +
                    segments.get(segments.size()-1).getDuration();
                segments.add(new Segment(new_start));
                alignment_nss_seg.add(new IntegerPair(nss_seq.size()-1, segments.size()-1));
                alignment_word_seg.add(new IntegerPair(i_word, segments.size()-1));
            }
        }

        nss_seq.add(new NSS("end"));
        double new_start = segments.get(segments.size()-1).getStart() +
            segments.get(segments.size()-1).getDuration();
        segments.add(new Segment(new_start));
        alignment_nss_seg.add(new IntegerPair(nss_seq.size()-1, segments.size()-1));

        // Word => segment
        utt.addSequence(SupportedSequenceType.SEGMENT, segments);
        Relation tmp_rel = new Relation(words, segments, alignment_word_seg);
        utt.setRelation(SupportedSequenceType.WORD, SupportedSequenceType.SEGMENT, tmp_rel);

        // NSS => segment
        utt.addSequence(SupportedSequenceType.NSS, nss_seq);
        tmp_rel = new Relation(nss_seq, segments, alignment_nss_seg);
        utt.setRelation(SupportedSequenceType.NSS, SupportedSequenceType.SEGMENT, tmp_rel);

        // Phone => segment
        utt.addSequence(SupportedSequenceType.PHONE, phones);
        tmp_rel = new Relation(phones, segments, alignment_phone_seg);
        utt.setRelation(SupportedSequenceType.PHONE, SupportedSequenceType.SEGMENT, tmp_rel);

        // Syllable => phone
        utt.addSequence(SupportedSequenceType.SYLLABLE, syllables);
        tmp_rel = new Relation(syllables, phones, alignment_syllable_phone);
        utt.setRelation(SupportedSequenceType.SYLLABLE, SupportedSequenceType.PHONE, tmp_rel);

        return utt;
    }

    protected void createSubStructure(Word w, ArrayList<String> phonetisation_string,
                                      Sequence<Syllable> syllables, Sequence<Phoneme> phones, Sequence<Segment> segments,
                                      ArrayList<IntegerPair> alignment_syllable_phone, ArrayList<IntegerPair> alignment_phone_seg,
                                      ArrayList<IntegerPair> alignment_word_seg, int word_index) throws MaryException {

        int stress = 0;
        int phone_offset = phones.size();
        Accent accent = null;
        Phoneme tone = null;

        for (String syl_string : phonetisation_string) {
            if (syl_string.trim().isEmpty()) {
                continue;
            }

            logger.debug("Dealing with \"" + syl_string + "\"");
            Splitter syl_string_plitter = Splitter.on(' ').omitEmptyStrings().trimResults();

            Iterable<String> syl_tokens = syl_string_plitter.split(syl_string);
            for (String token : syl_tokens) {

                // Syllable separator
                if (token.equals(SYL_SEP)) {
                    // Create the syllable (FIXME: and the tone?)
                    syllables.add(new Syllable(tone, stress, accent));

                    // Update the phone/syllable relation
                    for (; phone_offset < phones.size(); phone_offset++) {
                        alignment_syllable_phone.add(new IntegerPair(syllables.size() - 1, phone_offset));
                    }

                    // Reinit for the next part
                    tone = null;
                    stress = 0;
                    accent = null;
                }
                // First stress
                else if (token.equals(FIRST_STRESS)) {
                    stress = 1;
                    accent = w.getAccent();
                }
                // Second stress
                else if (token.equals(SECOND_STRESS)) {
                    stress = 2;
                } else {
                    phones.add(generatePhonemeFromLabel(token));
                    double new_start = segments.get(segments.size()-1).getStart() +
                        segments.get(segments.size()-1).getDuration();
                    segments.add(new Segment(new_start));
                    alignment_phone_seg.add(new IntegerPair(phones.size()-1, segments.size()-1));
                    alignment_word_seg.add(new IntegerPair(word_index, segments.size()-1));
                }
            }

            // Create the syllable (FIXME: how to get the tone?)
            syllables.add(new Syllable(tone, stress, accent));

            // Update the phone/syllable relation
            for (; phone_offset < phones.size(); phone_offset++) {
                alignment_syllable_phone.add(new IntegerPair(syllables.size() - 1, phone_offset));
            }
        }
    }

    protected Phoneme generatePhonemeFromLabel(String label) throws MaryException {
	return new Phoneme(sampa2ipa.getCorrespondingIPA(label));
    }

    /**
     * Phonemise the word text. This starts with a simple lexicon lookup,
     * followed by some heuristics, and finally applies letter-to-sound rules if
     * nothing else was successful.
     *
     * @param text
     *            the textual (graphemic) form of a word.
     * @param pos
     *            the part-of-speech of the word
     * @param g2pMethod
     *            This is an awkward way to return a second String parameter via
     *            a StringBuilder. If a phonemisation of the text is found, this
     *            parameter will be filled with the method of phonemisation
     *            ("lexicon", ... "rules").
     * @return a phonemisation of the text if one can be generated, or null if
     *         no phonemisation method was successful.
     */
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

        // Lookup attempts failed. Try normalising exotic letters
        // (diacritics on vowels, etc.), look up again:
        String normalised = MaryUtils.normaliseUnicodeLetters(text, getLocale());
        if (!normalised.equals(text)) {
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
        }

        // Cannot find it in the lexicon -- apply letter-to-sound rules
        // to the normalised form

        String phones = lts.predictPronunciation(text);
        try {
            result = lts.syllabify(phones);
        } catch (IllegalArgumentException e) {
            logger.error(String.format("Problem with token <%s> [%s]: %s", text, phones, e.getMessage()));
        }
        if (result != null) {
            g2pMethod.append("rules");
            return result;
        }

        return null;
    }

    /**
     * Look a given text up in the (standard) lexicon. part-of-speech is used in
     * case of ambiguity.
     *
     * @param text
     *            text
     * @param pos
     *            pos
     * @return null if text == null or text.length is 0, null if entries.length
     *         is 0, entries[0] otherwise
     */
    public String lexiconLookup(String text, String pos) {
        if (text == null || text.length() == 0) {
            return null;
        }
        String[] entries;
        entries = lexiconLookupPrimitive(text, pos);
        // If entry is not found directly, try the following changes:
        // - lowercase the word
        // - all lowercase but first uppercase
        if (entries.length == 0) {
            text = text.toLowerCase(getLocale());
            entries = lexiconLookupPrimitive(text, pos);
        }
        if (entries.length == 0) {
            text = text.substring(0, 1).toUpperCase(getLocale()) + text.substring(1);
            entries = lexiconLookupPrimitive(text, pos);
        }

        if (entries.length == 0) {
            return null;
        }
        return entries[0];
    }

    private String[] lexiconLookupPrimitive(String text, String pos) {
        String[] entries;
        if (pos != null) { // look for pos-specific version first
            entries = lexicon.lookup(text + pos);
            if (entries.length == 0) { // not found -- lookup without pos
                entries = lexicon.lookup(text);
            }
        } else {
            entries = lexicon.lookup(text);
        }
        return entries;
    }

    /**
     * look a given text up in the userdict. part-of-speech is used in case of
     * ambiguity.
     *
     * @param text
     *            text
     * @param pos
     *            pos
     * @return null if userdict is null or text is null or text.length is 0,
     *         null if entries is null, transcr otherwise
     */
    public String userdictLookup(String text, String pos) {
        if (userdict == null || text == null || text.length() == 0) {
            return null;
        }

        List<String> entries = userdict.get(text);
        // If entry is not found directly, try the following changes:
        // - lowercase the word
        // - all lowercase but first uppercase
        if (entries == null) {
            text = text.toLowerCase(getLocale());
            entries = userdict.get(text);
        }
        if (entries == null) {
            text = text.substring(0, 1).toUpperCase(getLocale()) + text.substring(1);
            entries = userdict.get(text);
        }

        if (entries == null) {
            return null;
        }

        String transcr = null;
        for (String entry : entries) {
            String[] parts = entry.split("\\|");
            transcr = parts[0];
            if (parts.length > 1 && pos != null) {
                StringTokenizer tokenizer = new StringTokenizer(entry);
                while (tokenizer.hasMoreTokens()) {
                    String onePos = tokenizer.nextToken();
                    if (pos.equals(onePos)) {
                        return transcr;    // found
                    }
                }
            }
        }

        // no match of POS: return last entry
        return transcr;
    }

    /**
     * Access the allophone set underlying this phonemiser.
     *
     * @return allophoneSet
     */
    public AllophoneSet getAllophoneSet() {
        return allophoneSet;
    }

    public Locale getLocale() {
	return locale;
    }

    public void setLexicon(InputStream stream) throws MaryConfigurationException {
	try {
	    lexicon = new FSTLookup(stream, this.getClass().getName() + ".lexicon");
	} catch (Exception ex) {
	    throw new MaryConfigurationException("Cannot load lexicon", ex);
	}
    }


    public void setAllophoneSet(InputStream allophone_xml_stream) throws MaryConfigurationException {
        allophoneSet = AllophoneSet.getAllophoneSet(allophone_xml_stream, getLocale().toString());
	assert allophoneSet != null;
    }

    public void setLetterToSound(InputStream stream) throws MaryConfigurationException {
	try {
	    assert getAllophoneSet() != null;
	    lts = new TrainedLTS(getAllophoneSet(), stream, this.removeTrailingOneFromPhones);
	} catch (Exception ex) {
	    throw new MaryConfigurationException("Cannot load LTS model", ex);
	}
    }

    public void setLocale(Locale locale) {
	this.locale = locale;
    }

    /**
     * a lexicon. Lines must have the format
     *
     * graphemestring | phonestring | optional-parts-of-speech
     *
     * The pos-item is optional. Different pos's belonging to one grapheme chain
     * may be separated by whitespace
     *
     *
     * @param lexiconFilename
     *            lexiconFilename
     * @throws IOException
     *             IOException
     * @return fLexicon
     */
    protected Map<String, List<String>> readLexicon(String lexiconFilename) throws IOException {
        logger.debug(String.format("Reading lexicon from '%s'", lexiconFilename));
        String line;
        Map<String, List<String>> fLexicon = new HashMap<String, List<String>>();

        BufferedReader lexiconFile = new BufferedReader(
            new InputStreamReader(new FileInputStream(lexiconFilename), "UTF-8"));
        while ((line = lexiconFile.readLine()) != null) {
            // Ignore empty lines and comments:
            if (line.trim().equals("") || line.startsWith("#")) {
                continue;
            }

            String[] lineParts = line.split("\\s*\\|\\s*");
            String graphStr = lineParts[0];
            String phonStr = null;
            try {
                phonStr = lineParts[1];
            } catch (ArrayIndexOutOfBoundsException e) {
                logger.warn(String.format("Lexicon '%s': missing transcription for '%s'", lexiconFilename,
                                          graphStr));
                continue;
            }
            try {
                allophoneSet.splitIntoAllophones(phonStr);
            } catch (IllegalArgumentException e) {
                logger.warn(String.format("Lexicon '%s': invalid entry for '%s': %s", lexiconFilename, graphStr,
                                          e.getMessage()));
                continue;
            }
            String phonPosStr = phonStr;
            if (lineParts.length > 2) {
                String pos = lineParts[2];
                if (!pos.trim().equals("")) {
                    phonPosStr += "|" + pos;
                }
            }

            List<String> transcriptions = fLexicon.get(graphStr);
            if (null == transcriptions) {
                transcriptions = new ArrayList<String>();
                fLexicon.put(graphStr, transcriptions);
            }
            transcriptions.add(phonPosStr);
        }
        lexiconFile.close();
        return fLexicon;
    }

    /**
     * Compile a regex pattern used to determine whether tokens are processed as
     * punctuation or not, based on whether their <code>pos</code> attribute
     * matches the pattern.
     *
     */
    public void setPosPunctRegex(String regex) {
        try {
            punctuationPosRegex = Pattern.compile(regex);
        } catch (PatternSyntaxException e) {
            logger.error(String.format("Could not compile regex pattern /%s/, using default instead", regex));
        }
        logger.debug(String.format("Punctuation regex pattern set to /%s/", punctuationPosRegex));
    }

    /**
     * Compile a regex pattern used to determine whether tokens are processed as
     * unprounounceable or not, based on whether their <code>pos</code>
     * attribute matches the pattern.
     *
     */
    public void setUnpronounceablePosRegex(String regex) {
        try {
            unpronounceablePosRegex = Pattern.compile(regex);
        } catch (PatternSyntaxException e) {
            logger.error(String.format("Could not compile regex pattern /%s/, using default instead", regex));

        }
        logger.debug(String.format("Punctuation regex pattern set to /%s/", unpronounceablePosRegex));
    }

    /**
     * Based on the regex compiled in {@link #setPunctuationPosRegex()},
     * determine whether a given POS string is classified as punctuation
     *
     * @param pos
     *            the POS tag
     * @return <b>true</b> if the POS tag matches the regex pattern;
     *         <b>false</b> otherwise
     * @throws NullPointerException
     *             if the regex pattern is null (because it hasn't been set
     *             during module startup)
     *
     */
    public boolean isPosPunctuation(String pos) {
        if (pos != null && punctuationPosRegex.matcher(pos).matches()) {
            return true;
        }
        return false;
    }

    public boolean isUnpronounceable(String pos) {
        if (pos != null && unpronounceablePosRegex.matcher(pos).matches()) {
            return true;
        }
        return false;
    }

    /**
     * Determine whether token should be pronounceable, based on text and POS
     * tag.
     *
     * @param text
     *            the text of the token
     * @param pos
     *            the POS tag of the token
     * @return <b>false</b> if the text is empty, or if it contains no word
     *         characters <em>and</em> the POS tag indicates punctuation;
     *         <b>true</b> otherwise
     */
    public boolean maybePronounceable(String text, String pos) {
        // does text contain anything at all?
        if (text == null || text.isEmpty()) {
            return false;
        }

        // does text contain at least one word character?
        if (text.matches(".*\\w.*")) {
            return true;
        }

        // does POS tag indicate punctuation?
        if (isPosPunctuation(pos)) {
            return false;
        }

        // does POS tag indicate punctuation?
        if (isUnpronounceable(pos)) {
            return false;
        }

        // by default, just try to pronounce anyway
        return true;
    }
}
