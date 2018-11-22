/**
 * Copyright 2000-2016 DFKI GmbH.
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
package marytts.io;

// List
import java.util.ArrayList;

import java.io.IOException;


import org.apache.log4j.BasicConfigurator;

import marytts.data.utils.IntegerPair;
import marytts.data.Utterance;
import marytts.data.Sequence;
import marytts.data.Relation;
import marytts.data.item.linguistic.Word;
import marytts.data.item.phonology.Phoneme;
import marytts.data.item.phonology.Syllable;
import marytts.io.serializer.ROOTSJSONSerializer;

/* Assert/test */
import org.testng.Assert;
import org.testng.annotations.*;



/**
 * TODO: think about a real test....
 */
public class ROOTSJSONSerializerTest {

    public Utterance generateUTT() throws Exception {

	// Generate dummy utterance
	Sequence<Word> seq_word = new Sequence<Word>();
	Word w = new Word("hello");
	w.setAlternativeLocale("en_US");
	seq_word.add(w);
	w = new Word("test");
	seq_word.add(w);
	w = new Word("!");
	seq_word.add(w);

	Sequence<Syllable> seq_syl = new Sequence<Syllable>();
	Syllable syl = new Syllable();
	seq_syl.add(syl);
	syl = new Syllable();
	seq_syl.add(syl);
	syl = new Syllable();
	seq_syl.add(syl);
	syl = new Syllable();
	seq_syl.add(syl);

	Sequence<Phoneme> seq_phone = new Sequence<Phoneme>();
	Phoneme ph = new Phoneme("a");
	seq_phone.add(ph);
	ph = new Phoneme("b");
	seq_phone.add(ph);
	ph = new Phoneme("c");
	seq_phone.add(ph);
	ph = new Phoneme("d");
	seq_phone.add(ph);


        ArrayList<IntegerPair> alignment_word_phone = new ArrayList<IntegerPair>();
	alignment_word_phone.add(new IntegerPair(0, 0));
	alignment_word_phone.add(new IntegerPair(0, 1));
	alignment_word_phone.add(new IntegerPair(1, 2));
	alignment_word_phone.add(new IntegerPair(2, 2));


        ArrayList<IntegerPair> alignment_syl_phone = new ArrayList<IntegerPair>();
	alignment_syl_phone.add(new IntegerPair(0, 0));
	alignment_syl_phone.add(new IntegerPair(0, 1));
	alignment_syl_phone.add(new IntegerPair(1, 2));
	alignment_syl_phone.add(new IntegerPair(2, 2));



	Utterance utt = new Utterance();
	utt.addSequence("WORDS", seq_word);
	utt.addSequence("SYLLABLES", seq_syl);
	utt.addSequence("PHONES", seq_phone);

	Relation rel = new Relation(seq_word, seq_phone, alignment_word_phone);
	utt.setRelation("WORDS", "PHONES", rel);

	rel = new Relation(seq_syl, seq_phone, alignment_syl_phone);
	utt.setRelation("SYLLABLES", "PHONES", rel);

	return utt;
    }

    @Test
    public void testExport() throws Exception {
	ROOTSJSONSerializer ser = new ROOTSJSONSerializer();
	System.out.println(ser.export(generateUTT()));
    }

    @Test
    public void testImport() throws Exception {
	ROOTSJSONSerializer ser = new ROOTSJSONSerializer();

	// Try to export....
	Utterance origin_utt = generateUTT();
	String output = (String) ser.export(origin_utt );

	// Reload and assert !
	Utterance utt = ser.load(output);
	Assert.assertEquals(origin_utt, utt);
    }
}
