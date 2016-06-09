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


import marytts.data.item.linguistic.Word;
import marytts.data.item.Item;

public class SequenceTest
{

	@Test
	public void testSequence()
    {
        Sequence<Item> seq = new Sequence<Item>();

        Word w1 = new Word("Hello");
        Word w2 = new Word("You");

        Assert.assertTrue(seq.size() == 0);
        seq.add(w1);
        Assert.assertTrue(seq.size() == 1);
        seq.add(w2);
        Assert.assertTrue(seq.size() == 2);
    }

    @Test
    public void testReference()
    {
        Sequence<Item> seq = new Sequence<Item>();

        Word w1 = new Word("Hello");
        Word w2 = new Word("You");


        Assert.assertFalse(w1.isInSequence(seq));
        seq.add(w1);
        Assert.assertTrue(w1.isInSequence(seq));
        seq.add(w2);
        Assert.assertTrue(w2.isInSequence(seq));
    }
}
