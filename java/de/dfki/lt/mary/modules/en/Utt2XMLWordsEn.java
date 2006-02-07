/**
 * Copyright 2000-2006 DFKI GmbH.
 * All Rights Reserved.  Use is subject to license terms.
 * 
 * Permission is hereby granted, free of charge, to use and distribute
 * this software and its documentation without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of this work, and to
 * permit persons to whom this work is furnished to do so, subject to
 * the following conditions:
 * 
 * 1. The code must retain the above copyright notice, this list of
 *    conditions and the following disclaimer.
 * 2. Any modifications must be clearly marked as such.
 * 3. Original authors' names are not deleted.
 * 4. The authors' names are not used to endorse or promote products
 *    derived from this software without specific prior written
 *    permission.
 *
 * DFKI GMBH AND THE CONTRIBUTORS TO THIS WORK DISCLAIM ALL WARRANTIES WITH
 * REGARD TO THIS SOFTWARE, INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS, IN NO EVENT SHALL DFKI GMBH NOR THE
 * CONTRIBUTORS BE LIABLE FOR ANY SPECIAL, INDIRECT OR CONSEQUENTIAL
 * DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR
 * PROFITS, WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS
 * ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE OF
 * THIS SOFTWARE.
 */
package de.dfki.lt.mary.modules.en;

import org.w3c.dom.Element;

import com.sun.speech.freetts.Item;
import com.sun.speech.freetts.Relation;
import com.sun.speech.freetts.Utterance;

import de.dfki.lt.mary.MaryDataType;
import de.dfki.lt.mary.modules.Utt2XMLBase;


/**
 * Convert FreeTTS utterances into MaryXML format
 * (Words, English).
 *
 * @author Marc Schr&ouml;der
 */

public class Utt2XMLWordsEn extends Utt2XMLBase
{
    public Utt2XMLWordsEn()
    {
        super("Utt2XML WordsEn",
              MaryDataType.get("FREETTS_WORDS_EN"),
              MaryDataType.get("WORDS_EN")
              );
    }

    /**
     * Depending on the data type, find the right information in the utterance
     * and insert it into the sentence.
     */
    protected void fillSentence(Element sentence, Utterance utterance)
    {
        Relation tokenRelation = utterance.getRelation(Relation.TOKEN);
        if (tokenRelation == null) return;
        Item tokenItem = tokenRelation.getHead();
        while (tokenItem != null) {
            insertToken(tokenItem, sentence);
            tokenItem = tokenItem.getNext();
        }
    }




}
