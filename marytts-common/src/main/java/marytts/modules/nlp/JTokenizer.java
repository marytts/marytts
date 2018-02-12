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
package marytts.modules.nlp;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Properties;

import marytts.config.MaryConfiguration;

import marytts.data.Utterance;
import marytts.io.serializer.XMLSerializer;

import marytts.data.utils.IntegerPair;
import marytts.data.utils.SequenceTypePair;
import marytts.data.Utterance;
import marytts.data.Sequence;
import marytts.data.Relation;
import marytts.data.SupportedSequenceType;
import marytts.data.item.linguistic.Paragraph;
import marytts.data.item.linguistic.Sentence;
import marytts.data.item.linguistic.Word;
import marytts.modules.MaryModule;

import de.dfki.lt.tools.tokenizer.JTok;
import de.dfki.lt.tools.tokenizer.annotate.AnnotatedString;
import de.dfki.lt.tools.tokenizer.output.Outputter;
import de.dfki.lt.tools.tokenizer.output.Token;

import marytts.exceptions.MaryConfigurationException;
import marytts.MaryException;

import org.apache.logging.log4j.core.Appender;
/**
 * @author Marc Schr&ouml;der
 *
 *
 */
public class JTokenizer extends MaryModule {
    public static final int TOKEN_MAXLENGTH = 100;

    private JTok tokenizer = null;
    private String jtokLocale;

    public JTokenizer() throws MaryConfigurationException {
	super();
	setLocale("default");
    }

    public void checkStartup() throws MaryConfigurationException {
	if (tokenizer == null)
	    throw new MaryConfigurationException("The tokenize is null and should not be");

	if (jtokLocale == null)
	    throw new MaryConfigurationException("The locale is null and should not be");
    }

    /**
     * Set the tokenizer language to be different from the Locale of the module.
     * This can be useful when reusing another language's tokenizer data.
     *
     * @param languageCode
     *            the language-code to use, as a two-character string such as
     *            "de" or "en".
     */
    protected void setTokenizerLanguage(String languageCode) {
        jtokLocale = languageCode;
    }

    public void startup() throws MaryException {
        super.startup();
    }

    public void setLocale(String locale) throws MaryConfigurationException {

	try {
	    Properties jtokProperties = new Properties();
	    if (locale.equals("default")) {
		jtokProperties.setProperty(locale, "jtok/default"); // FIXME:
		// hardcoded
	    } else {
		// hardcoded
		jtokProperties.setProperty(locale, "marytts/modules/nlp/jtok/" + jtokLocale);
	    }
	    tokenizer = new JTok(jtokProperties);
	    jtokLocale = locale;
	} catch (Exception ex) {
	    throw new MaryConfigurationException("Cannot set locale of the tokenizer jtok", ex);
	}
    }

    /**
     *  Check if the input contains all the information needed to be
     *  processed by the module.
     *
     *  @param utt the input utterance
     *  @throws MaryException which indicates what is missing if something is missing
     */
    public void checkInput(Utterance utt) throws MaryException {
        if (!utt.hasSequence(SupportedSequenceType.PARAGRAPH)) {
            throw new MaryException("Paragraph sequence is missing", null);
        }
    }

    public Utterance process(Utterance utt, MaryConfiguration configuration) throws Exception {

        // Sequence initialisation
        Sequence<Sentence> sentences = new Sequence<Sentence>();
        Sequence<Word> words = new Sequence<Word>();

        // Alignment initialisation
        ArrayList<IntegerPair> alignment_paragraph_sentence = new ArrayList<IntegerPair>();
        ArrayList<IntegerPair> alignment_sentence_word = new ArrayList<IntegerPair>();

        // Indexes and temp variables
        int p_idx = 0, sent_idx = 0, tok_idx = 0;
        int sent_offset = 0;
        ArrayList<Integer> token_ids = new ArrayList<Integer>();
        String sent_text = "";

        // Tokenize everything
        for (Paragraph p : (Sequence<Paragraph>) utt.getSequence(SupportedSequenceType.PARAGRAPH)) {
            // Tokenize current paragraph
            AnnotatedString res = tokenizer.tokenize(p.getText(), jtokLocale);

            // Going through the tokens and create the proper MaryTTS
            // representation
            for (Token tok : Outputter.createTokens(res)) {
                String tok_string = tok.getImage();

                // Create the new token
                if (!(tok_string.equals("\"") || tok_string.equals("(") || tok_string.equals(")")
                        || tok_string.equals("[") || tok_string.equals("]") || tok_string.equals("{")
                        || tok_string.equals("}"))) {
                    sent_text += tok_string + " ";
                    token_ids.add(tok_idx);
                }

                if (((tok_string.charAt(0) == '\'') && (tok_string.length() == 1))
                        || ((tok_string.charAt(0) == '\'') && (tok_string.length() > 1)
                            && (tok_string.charAt(1) != '\''))
                        || ((tok_string.length() > 1) && (tok_string.charAt(1) == '\''))) {
                    Word prev = words.get(words.size() - 1);
                    prev.setText(prev.getText() + tok_string);
                } else {
                    Word w = new Word(tok_string);
                    words.add(w);
                    tok_idx++;

                    // Check if the token is ending the current sentence or not
                    if (tok.getType().equals("PERIOD") || tok.getType().equals("QUEST")
                            || tok.getType().equals("EXCLAM")) {
                        // Create and add the new sentence
                        Sentence s = new Sentence(sent_text);
                        sentences.add(s);

                        // Sent <=> Word relation
                        for (int i : token_ids) {
                            alignment_sentence_word.add(new IntegerPair(sent_idx, i));
                        }

                        // Paragraph <=> relation
                        alignment_paragraph_sentence.add(new IntegerPair(p_idx, sent_idx));

                        // Prepare the next sentence
                        sent_idx++;
                        sent_text = "";
                        token_ids.clear();
                    }
                }
            }

            p_idx++;
        }

        // Maybe something remains after there is no punctuation
        if (!token_ids.isEmpty()) {
            // Create and add the new sentence
            Sentence s = new Sentence(sent_text);
            sentences.add(s);

            // // Add a punctuation token(FIXME: somehow kind of patchy!)
            // Word w = new Word(".");
            // words.add(w);
            // tok_idx++;

            // Sent <=> Word relation
            for (int i : token_ids) {
                alignment_sentence_word.add(new IntegerPair(sent_idx, i));
            }

            // Paragraph <=> relation
            alignment_paragraph_sentence.add(new IntegerPair(p_idx - 1, sent_idx));
        }

        // Add the sequences to the utterance
        utt.addSequence(SupportedSequenceType.SENTENCE, sentences);
        utt.addSequence(SupportedSequenceType.WORD, words);

        // Create the relations and add them to the utterance
        Relation rel_par_sent = new Relation(utt.getSequence(SupportedSequenceType.PARAGRAPH),
                                             utt.getSequence(SupportedSequenceType.SENTENCE), alignment_paragraph_sentence);
        utt.setRelation(SupportedSequenceType.PARAGRAPH, SupportedSequenceType.SENTENCE, rel_par_sent);
        Relation rel_sent_wrd = new Relation(utt.getSequence(SupportedSequenceType.SENTENCE),
                                             utt.getSequence(SupportedSequenceType.WORD), alignment_sentence_word);
        utt.setRelation(SupportedSequenceType.SENTENCE, SupportedSequenceType.WORD, rel_sent_wrd);

        // Generate the result
        return utt;
    }
}
