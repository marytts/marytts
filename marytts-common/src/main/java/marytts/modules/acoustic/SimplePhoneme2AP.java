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
package marytts.modules.acoustic;

import java.util.Locale;
import java.util.StringTokenizer;
import java.util.ArrayList;

import marytts.config.MaryProperties;

import marytts.data.Utterance;
import marytts.data.Sequence;
import marytts.data.Relation;
import marytts.data.SupportedSequenceType;
import marytts.data.item.linguistic.*;
import marytts.data.item.prosody.*;
import marytts.data.item.phonology.*;
import marytts.data.utils.IntegerPair;
import marytts.data.utils.SequenceTypePair;

import marytts.data.Utterance;
import marytts.io.serializer.XMLSerializer;
import marytts.modules.nlp.phonemiser.Allophone;
import marytts.modules.nlp.phonemiser.AllophoneSet;
import marytts.config.MaryConfiguration;
import marytts.util.MaryRuntimeUtils;
import marytts.util.MaryUtils;

import marytts.modules.MaryModule;

import marytts.MaryException;

import org.apache.logging.log4j.core.Appender;
/**
 * Read a simple phone string and generate default acoustic parameters.
 *
 * @author Marc Schr&ouml;der
 */

public class SimplePhoneme2AP extends MaryModule {
    protected AllophoneSet allophoneSet;

    public SimplePhoneme2AP() {
	this(Locale.getDefault());
    }
    public SimplePhoneme2AP(String localeString) {
        this(MaryUtils.string2locale(localeString));
    }

    public SimplePhoneme2AP(Locale locale) {
        super("SimplePhoneme2AP", locale);
    }

    public void startup() throws Exception {
        allophoneSet = MaryRuntimeUtils.needAllophoneSet(MaryProperties.localePrefix(
                           getLocale()) + ".allophoneset");
        super.startup();
    }


    /**
     *  Check if the input contains all the information needed to be
     *  processed by the module.
     *
     *  @param utt the input utterance
     *  @throws MaryException which indicates what is missing if something is missing
     */
    public void checkInput(Utterance utt) throws MaryException {
    }

    public Utterance process(Utterance utt, MaryConfiguration configuration, Appender app) throws Exception {
        // String phoneString = d.getPlainText();
        // Utterance utt = new Utterance(phoneString, d.getLocale());

        // Accent word_accent = null;
        // int cumulDur = 0;
        // boolean isFirst = true;
        // Sequence<Word> words = new Sequence<Word>();
        // Sequence<Syllable> syllables = new Sequence<Syllable>();
        // Sequence<Phoneme> phones = new Sequence<Phoneme>();
        // ArrayList<IntegerPair> alignment_word_syllable = new
        // ArrayList<IntegerPair>();
        // ArrayList<IntegerPair> alignment_syllable_phone = new
        // ArrayList<IntegerPair>();
        // int start_syl = 0;
        // int start_phone = 0;
        // StringTokenizer stTokens = new StringTokenizer(phoneString);
        // while (stTokens.hasMoreTokens())
        // {
        // String tokenPhonemes = stTokens.nextToken();
        // StringTokenizer stSyllables = new StringTokenizer(tokenPhonemes,
        // "-_");

        // int nb_syl = 0;
        // while (stSyllables.hasMoreTokens())
        // {
        // // Get syllable string
        // String syllablePhonemes = stSyllables.nextToken();

        // // Setting stress
        // int stress = 0;
        // if (syllablePhonemes.startsWith("'"))
        // stress = 1;
        // else if (syllablePhonemes.startsWith(","))
        // stress = 2;

        // // Simplified: Give a "pressure accent" do stressed syllables
        // Accent syllable_accent = null;
        // if (stress != 0) {
        // word_accent = new Accent("*");
        // syllable_accent = new Accent("*");
        // }

        // // Generate phones
        // Allophone[] allophones =
        // allophoneSet.splitIntoAllophones(syllablePhonemes);
        // for (int i = 0; i < allophones.length; i++) {
        // // Dealing with duration of the phone
        // int dur = 70;
        // if (allophones[i].isVowel()) {
        // dur = 100;
        // if (stress == 1)
        // dur *= 1.5;
        // else if (stress == 2)
        // dur *= 1.2;
        // }

        // // Creating the phone
        // Phone ph = new Phone(allophones[i].name(), cumulDur, dur);
        // phones.add(ph);

        // // We save the cumulative duration to know when the next phone is
        // starting
        // cumulDur += dur;
        // }

        // Syllable syl = new Syllable(stress);
        // syl.setAccent(syllable_accent);
        // syllables.add(syl);

        // for (int i=0; i<allophones.length; i++)
        // alignment_syllable_phone.add(new IntegerPair(syllables.size()-1,
        // start_phone+i));
        // start_phone = start_phone + allophones.length;

        // nb_syl++;
        // }

        // // Wrapping into a word
        // Word w = new Word("");
        // w.setAccent(word_accent);
        // words.add(w);

        // for (int i=0; i<nb_syl; i++)
        // alignment_word_syllable.add(new IntegerPair(words.size()-1,
        // start_syl+i));
        // start_syl = start_syl + nb_syl;
        // }

        // // Generate sequence and relations
        // utt.addSequence(SupportedSequenceType.PHONE, phones);

        // utt.addSequence(SupportedSequenceType.SYLLABLE, syllables);
        // utt.setRelation(SupportedSequenceType.SYLLABLE,
        // SupportedSequenceType.PHONE,
        // new Relation(syllables, phones, alignment_word_syllable));

        // utt.addSequence(SupportedSequenceType.WORD, words);
        // utt.setRelation(SupportedSequenceType.WORD,
        // SupportedSequenceType.SYLLABLE,
        // new Relation(words, syllables, alignment_word_syllable));

        // // Wrapping into a phrase
        // Boundary boundary = new Boundary(4, 400);
        // Phrase phrase = new Phrase(boundary);
        // Sequence<Phrase> phrases = new Sequence<Phrase>();
        // phrases.add(phrase);
        // utt.addSequence(SupportedSequenceType.PHRASE, phrases);
        // ArrayList<IntegerPair> alignment_phrase_word = new
        // ArrayList<IntegerPair>();
        // for (int i=0; i<words.size(); i++)
        // alignment_phrase_word.add(new IntegerPair(0, i));
        // utt.setRelation(SupportedSequenceType.PHRASE,
        // SupportedSequenceType.WORD,
        // new Relation(phrases, words, alignment_phrase_word));

        // // Wrapping into a sentence
        // Sentence sentence = new Sentence("");
        // Sequence<Sentence> sentences = new Sequence<Sentence>();
        // sentences.add(sentence);
        // utt.addSequence(SupportedSequenceType.SENTENCE, sentences);
        // ArrayList<IntegerPair> alignment_sentence_phrase = new
        // ArrayList<IntegerPair>();
        // alignment_sentence_phrase.add(new IntegerPair(0, 0));
        // utt.setRelation(SupportedSequenceType.SENTENCE,
        // SupportedSequenceType.PHRASE,
        // new Relation(sentences, phrases, alignment_sentence_phrase));

        // // Wrapping into a paragraph
        // Paragraph paragraph = new Paragraph("");
        // Sequence<Paragraph> paragraphs = new Sequence<Paragraph>();
        // paragraphs.add(paragraph);
        // utt.addSequence(SupportedSequenceType.PARAGRAPH, paragraphs);
        // ArrayList<IntegerPair> alignment_paragraph_sentence = new
        // ArrayList<IntegerPair>();
        // alignment_paragraph_sentence.add(new IntegerPair(0, 0));
        // utt.setRelation(SupportedSequenceType.PARAGRAPH,
        // SupportedSequenceType.SENTENCE,
        // new Relation(paragraphs, sentences, alignment_paragraph_sentence));

        // // Finally serialize and return
        // Utterance result = new Utterance(outputType(), d.getLocale(), utt);
        // return result;

        return utt;
    }
}
