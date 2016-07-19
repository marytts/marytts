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
public class RelationGraphTest
{
	@Test
	public void testDirectRelation()
    {
        Word w1 = new Word("Hello");
        Word w2 = new Word("You");

        Sequence<Word> seq1 = new Sequence<Word>();
        seq1.add(w1);
        seq1.add(w2);


        Sequence<Word> seq2 = new Sequence<Word>();
        seq2.add(w1);
        seq2.add(w2);

        Relation rel = new Relation(seq1, seq2);

        RelationGraph rg = new RelationGraph();
        rg.addRelation(rel);

        Assert.assertNotNull(rg.getRelation(seq1, seq2));
    }


	@Test
	public void testUndirectRelation()
    {
        Word w1 = new Word("Hello");
        Word w2 = new Word("You");

        Sequence<Word> seq1 = new Sequence<Word>();
        seq1.add(w1);
        seq1.add(w2);


        Sequence<Word> seq2 = new Sequence<Word>();
        seq2.add(w1);
        seq2.add(w2);


        Sequence<Word> seq3 = new Sequence<Word>();
        seq3.add(w1);
        seq3.add(w2);

        Relation rel1 = new Relation(seq1, seq2);
        Relation rel2 = new Relation(seq2, seq3);

        RelationGraph rg = new RelationGraph();
        rg.addRelation(rel1);
        rg.addRelation(rel2);

        Assert.assertNotNull(rg.getRelation(seq1, seq2));
        Assert.assertNotNull(rg.getRelation(seq2, seq3));
        Assert.assertNotNull(rg.getRelation(seq1, seq3));
    }

	@Test
	public void testUndirectRelationWithReverse()
    {
        Word w1 = new Word("Hello");
        Word w2 = new Word("You");

        Sequence<Word> seq1 = new Sequence<Word>();
        seq1.add(w1);
        seq1.add(w2);


        Sequence<Word> seq2 = new Sequence<Word>();
        seq2.add(w1);
        seq2.add(w2);


        Sequence<Word> seq3 = new Sequence<Word>();
        seq3.add(w1);
        seq3.add(w2);

        Relation rel1 = new Relation(seq2, seq1);
        Relation rel2 = new Relation(seq2, seq3);

        RelationGraph rg = new RelationGraph();
        rg.addRelation(rel1);
        rg.addRelation(rel2);

        Assert.assertNotNull(rg.getRelation(seq2, seq1));
        Assert.assertNotNull(rg.getRelation(seq2, seq3));
        Assert.assertNotNull(rg.getRelation(seq1, seq3));
    }
}
