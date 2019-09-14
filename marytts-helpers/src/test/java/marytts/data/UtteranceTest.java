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
package marytts.data;

import java.util.ArrayList;

import org.testng.Assert;
import org.testng.annotations.Test;

import marytts.MaryException;
import marytts.data.item.linguistic.Word;
import marytts.data.utils.IntegerPair;
import marytts.io.serializer.ROOTSJSONSerializer;

public class UtteranceTest {

    @Test
    public void testBaselineMerge() throws MaryException{

        // Create first utterance
        Utterance utt1 = new Utterance();
        Sequence<Word> seq1 = new Sequence<Word>();
        Word w = new Word("test1");
        seq1.add(w);
        w = new Word("test2");
        seq1.add(w);
        utt1.addSequence("word1", seq1);



        // Create first utterance
        Utterance utt2 = new Utterance();
        Sequence<Word> seq2 = new Sequence<Word>();
        w = new Word("test3");
        seq2.add(w);
        utt2.addSequence("word2", seq2);

        ArrayList<IntegerPair> al = new ArrayList<IntegerPair>();
        al.add(new IntegerPair(0, 0));
        al.add(new IntegerPair(1, 0));
        Relation linking_rel = new Relation(seq1, seq2, al);
        utt1.mergeInto(utt2, linking_rel);

        ROOTSJSONSerializer ser = new ROOTSJSONSerializer();
        System.out.println(ser.export(utt1));

        Assert.assertTrue(utt1.hasSequence("word2"));
    }
}
