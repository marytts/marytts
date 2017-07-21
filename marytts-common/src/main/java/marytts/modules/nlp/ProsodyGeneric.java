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

import java.util.Locale;
import java.util.ArrayList;
import marytts.data.Utterance;
import marytts.io.serializer.XMLSerializer;
import marytts.modules.MaryModule;
import marytts.config.MaryProperties;

import org.w3c.dom.Document;

import marytts.data.Utterance;
import marytts.data.Relation;
import marytts.data.Sequence;
import marytts.data.item.linguistic.Sentence;
import marytts.data.item.prosody.Phrase;
import marytts.data.item.prosody.Boundary;
import marytts.data.item.linguistic.Sentence;
import marytts.data.item.linguistic.Word;
import marytts.data.SupportedSequenceType;
import marytts.data.utils.IntegerPair;
import marytts.data.utils.SequenceTypePair;

/**
 * The generic prosody module.
 *
 * @author Stephanie Becker
 */

public class ProsodyGeneric extends MaryModule {
    public static final int DEFAULT_BREAKINDEX = 5;
    public static final int DEFAULT_DURATION = 400;

    public ProsodyGeneric() {
        this((Locale) null);
    }

    public ProsodyGeneric(Locale locale, String tobipredFileName, String accentPriorities,
                          String syllableAccents,
                          String paragraphDeclination) {
        super("Prosody", locale);

    }

    public ProsodyGeneric(String locale, String propertyPrefix) {
        this(new Locale(locale), propertyPrefix);
    }

    public ProsodyGeneric(Locale locale, String propertyPrefix) {
        super("Prosody", locale);
    }

    public ProsodyGeneric(String locale) {
        this(new Locale(locale), "fallback.prosody.");
    }

    public ProsodyGeneric(Locale locale) {
        this(locale, "fallback.prosody.");
    }

    public void startup() throws Exception {
        super.startup();
    }

    public Utterance process(Utterance utt, MaryProperties configuration) throws Exception {

        // Initialise sequences
        Sequence<Sentence> sentences = (Sequence<Sentence>) utt.getSequence(SupportedSequenceType.SENTENCE);
        Sequence<Word> words = (Sequence<Word>) utt.getSequence(SupportedSequenceType.WORD);
        Sequence<Phrase> phrases = new Sequence<Phrase>();

        // Initialise relations
        Relation rel_sent_wrd = utt.getRelation(SupportedSequenceType.SENTENCE, SupportedSequenceType.WORD);

        // Alignment initialisation
        ArrayList<IntegerPair> alignment_sentence_phrase = new ArrayList<IntegerPair>();
        ArrayList<IntegerPair> alignment_phrase_word = new ArrayList<IntegerPair>();

        // By default : 1 sentence = 1 phrase
        // FIXME (artificial breakindex = 5, artificial duration = 400)
        for (int sent_idx = 0; sent_idx < sentences.size(); sent_idx++) {
            int[] word_indexes = rel_sent_wrd.getRelatedIndexes(sent_idx);
            for (int i = 0; i < word_indexes.length; i++) {
                alignment_phrase_word.add(new IntegerPair(sent_idx, word_indexes[i]));
            }

            Phrase ph = new Phrase(new Boundary(DEFAULT_BREAKINDEX, DEFAULT_DURATION));
            phrases.add(ph);
            alignment_sentence_phrase.add(new IntegerPair(sent_idx, sent_idx));
        }

        utt.addSequence(SupportedSequenceType.PHRASE, phrases);

        // Create the relations and add them to the utterance
        Relation rel_sent_phrase = new Relation(utt.getSequence(SupportedSequenceType.SENTENCE),
                                                utt.getSequence(SupportedSequenceType.PHRASE), alignment_sentence_phrase);
        utt.setRelation(SupportedSequenceType.SENTENCE, SupportedSequenceType.PHRASE, rel_sent_phrase);
        Relation rel_phrase_wrd = new Relation(utt.getSequence(SupportedSequenceType.PHRASE),
                                               utt.getSequence(SupportedSequenceType.WORD), alignment_phrase_word);
        utt.setRelation(SupportedSequenceType.PHRASE, SupportedSequenceType.WORD, rel_phrase_wrd);

	return utt;
    }
}
