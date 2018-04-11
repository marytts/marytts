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

import org.testng.Assert;
import org.testng.annotations.*;

import marytts.data.item.Item;
import marytts.data.item.linguistic.Word;

/**
 * TODO: compare relations & sequences
 *
 */
public class RelationTest {
    @Test
    public void testBaselineRelation() {
        Word w1 = new Word("Hello");
        Word w2 = new Word("You");

        Sequence<Word> seq1 = new Sequence<Word>();
        seq1.add(w1);
        seq1.add(w2);

        Sequence<Word> seq2 = new Sequence<Word>();
        seq2.add(w1);
        seq2.add(w2);

        Relation rel = new Relation(seq1, seq2);
    }

    @Test
    public void testBackwardReferencesNominalCase() {
        Word w1 = new Word("Hello");
        Word w2 = new Word("You");

        Sequence<Word> seq1 = new Sequence<Word>();
        seq1.add(w1);
        seq1.add(w2);

        Sequence<Word> seq2 = new Sequence<Word>();
        seq2.add(w1);
        seq2.add(w2);

        Relation rel = new Relation(seq1, seq2);

        Sequence<Word> seq3 = new Sequence<Word>();
        seq3.add(w1);
        seq3.add(w2);

        // Check double direction relation
        Assert.assertTrue(seq1.isRelatedWith(seq2));
        Assert.assertTrue(seq2.isRelatedWith(seq1));

        // Check non relation
        Assert.assertFalse(seq1.isRelatedWith(seq3));

        rel.addRelation(1, 0);
	rel.addRelation(1, 1);
	rel.addRelation(0, 1);
	seq1.remove(1);
	seq1.remove(0);

        seq1.add(w2);
        rel.addRelation(0, 1);

        seq1.add(w2);
        seq2.add(w2);

        // Assert.assertFalse(true);
    }

    @Test(expectedExceptions = AssertionError.class)
    public void testRemoveOutBoundaries1() {
        Word w1 = new Word("Hello");
        Word w2 = new Word("You");

        Sequence<Word> seq1 = new Sequence<Word>();
        seq1.add(w1);
        seq1.add(w2);

        Sequence<Word> seq2 = new Sequence<Word>();
        seq2.add(w1);
        seq2.add(w2);

        Relation rel = new Relation(seq1, seq2);
        seq1.remove(10);
    }

    @Test(expectedExceptions = AssertionError.class)
    public void testRemoveOutBoundaries2() {
        Word w1 = new Word("Hello");
        Word w2 = new Word("You");

        Sequence<Word> seq1 = new Sequence<Word>();
        seq1.add(w1);
        seq1.add(w2);

        Sequence<Word> seq2 = new Sequence<Word>();
        seq2.add(w1);
        seq2.add(w2);

        Relation rel = new Relation(seq1, seq2);
        seq2.remove(10);
    }
}
