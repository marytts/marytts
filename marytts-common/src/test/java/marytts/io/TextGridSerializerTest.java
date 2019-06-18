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

import java.util.ArrayList;

import org.testng.Assert;
import org.testng.annotations.*;

import java.io.IOException;

import marytts.data.utils.IntegerPair;
import marytts.data.Utterance;
import marytts.data.Sequence;
import marytts.data.Relation;
import marytts.data.item.linguistic.Word;
import marytts.data.item.phonology.Phoneme;
import marytts.data.item.phonology.Syllable;
import marytts.data.item.acoustic.Segment;
import marytts.io.serializer.TextGridSerializer;



/**
 * TODO: think about a real test....
 */
public class TextGridSerializerTest {

    public Utterance generateUTT() throws Exception {

        // Generate dummy utterance
        Sequence<Word> seq_word = new Sequence<Word>();
        Word w = new Word("w0");
        w.setAlternativeLocale("en_US");
        seq_word.add(w);
        w = new Word("w1");
        seq_word.add(w);
        w = new Word("w2");
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
        Sequence<Segment> seq_segment = new Sequence<Segment>();
        Segment seg = new Segment(0, 1);
        seq_segment.add(seg);
        Phoneme ph = new Phoneme("a");
        seq_phone.add(ph);
        seg = new Segment(1, 1);
        seq_segment.add(seg);
        ph = new Phoneme("b");
        seq_phone.add(ph);
        seg = new Segment(2, 1);
        seq_segment.add(seg);
        ph = new Phoneme("c");
        seq_phone.add(ph);
        seg = new Segment(3, 1);
        seq_segment.add(seg);
        ph = new Phoneme("d");
        seq_phone.add(ph);


        ArrayList<IntegerPair> alignment_word_phone = new ArrayList<IntegerPair>();
        alignment_word_phone.add(new IntegerPair(0, 0));
        alignment_word_phone.add(new IntegerPair(0, 1));
        alignment_word_phone.add(new IntegerPair(1, 2));
        alignment_word_phone.add(new IntegerPair(2, 3));


        ArrayList<IntegerPair> alignment_syl_phone = new ArrayList<IntegerPair>();
        alignment_syl_phone.add(new IntegerPair(0, 0));
        alignment_syl_phone.add(new IntegerPair(1, 1));
        alignment_syl_phone.add(new IntegerPair(1, 2));
        alignment_syl_phone.add(new IntegerPair(2, 2));
        alignment_syl_phone.add(new IntegerPair(2, 3));

        ArrayList<IntegerPair> alignment_phone_seg = new ArrayList<IntegerPair>();
        for (int i = 0; i < seq_phone.size(); i++) {
            alignment_phone_seg.add(new IntegerPair(i, i));
        }

        Utterance utt = new Utterance();
        utt.addSequence("WORD", seq_word);
        utt.addSequence("SYLLABLE", seq_syl);
        utt.addSequence("PHONE", seq_phone);
        utt.addSequence("SEGMENT", seq_segment);

        Relation rel = new Relation(seq_word, seq_phone, alignment_word_phone);
        utt.setRelation("WORD", "PHONE", rel);

        rel = new Relation(seq_syl, seq_phone, alignment_syl_phone);
        utt.setRelation("SYLLABLE", "PHONE", rel);

        rel = new Relation(seq_phone, seq_segment, alignment_phone_seg);
        utt.setRelation("PHONE", "SEGMENT", rel);

        return utt;
    }

    /** TODO */
    @Test
    public void testExport() throws Exception {
        TextGridSerializer ser = new TextGridSerializer();
        System.out.println(ser.export(generateUTT()));
    }
}
