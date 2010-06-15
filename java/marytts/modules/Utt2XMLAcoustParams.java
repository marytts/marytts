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
package marytts.modules;

import marytts.datatypes.MaryDataType;
import marytts.datatypes.MaryXML;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.sun.speech.freetts.Item;
import com.sun.speech.freetts.Relation;
import com.sun.speech.freetts.Utterance;



/**
 * Convert FreeTTS utterances into MaryXML format
 * (Acoustic Parameters, not language specific).
 *
 * @author Marc Schr&ouml;der
 */

public class Utt2XMLAcoustParams extends Utt2XMLBase
{
    public Utt2XMLAcoustParams()
    {
        super("Utt2XML AcoustParams",
              MaryDataType.FREETTS_ACOUSTPARAMS,
              MaryDataType.ACOUSTPARAMS,
              null);
    }

    /**
     * Depending on the data type, find the right information in the utterance
     * and insert it into the sentence.
     */
    protected void fillSentence(Element sentence, Utterance utterance)
    {
        Document doc = sentence.getOwnerDocument();
        Relation phraseRelation = utterance.getRelation(Relation.PHRASE);
        if (phraseRelation == null) return;
        Item phraseItem = phraseRelation.getHead();
        Relation tokenRelation = utterance.getRelation(Relation.TOKEN);
        if (tokenRelation == null) return;
        Item tokenItem = tokenRelation.getHead();
        // Challenge: Bring token and phrase relations together. They have
        // common children, which can be interpreted as Word or SylStructure
        // items. Algorithm: For a given phrase, look at tokens. If a token's
        // first child, interpreted in the phrase relation, has the phrase as
        // its parent, then insert the token and all its children, and move to
        // the next token. If not, move to the next phrase.
        while (phraseItem != null) {
            // The phrases:
            Element phrase = MaryXML.createElement(doc, MaryXML.PHRASE);
            sentence.appendChild(phrase);
            // Is this token part of this phrase?
            while (tokenItem != null &&
                   tokenItem.getDaughter().findItem("R:Phrase.parent").equals(phraseItem)) {
                insertToken(tokenItem, phrase, true); // create deep structure
                tokenItem = tokenItem.getNext();
            }
            phraseItem = phraseItem.getNext();
        }
    }




}

