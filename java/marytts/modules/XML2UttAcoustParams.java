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

import java.util.List;

import marytts.datatypes.MaryDataType;
import marytts.exceptions.MaryConfigurationException;
import marytts.modules.synthesis.FreeTTSVoices;
import marytts.modules.synthesis.Voice;

import org.w3c.dom.Element;

import com.sun.speech.freetts.Item;
import com.sun.speech.freetts.Relation;
import com.sun.speech.freetts.Utterance;



/**
 * Convert a MaryXML DOM tree into FreeTTS utterances
 * (durations, English).
 *
 * @author Marc Schr&ouml;der
 */

public class XML2UttAcoustParams extends XML2UttBase
{
    public XML2UttAcoustParams() {
        super("XML2Utt AcoustParams",
              MaryDataType.ACOUSTPARAMS,
              MaryDataType.FREETTS_ACOUSTPARAMS,
              null);
    }

    public void powerOnSelfTest() throws Error
    {
    }
    
    public Utterance convert(List<Element> tokensAndBoundaries, Voice maryVoice)
    {
        com.sun.speech.freetts.Voice freettsVoice =
            FreeTTSVoices.getFreeTTSVoice(maryVoice);
        if (freettsVoice == null) {
            throw new NullPointerException("No FreeTTS voice for mary voice " + maryVoice.getName());
        }
        Utterance utterance = new Utterance(freettsVoice);
        utterance.createRelation(Relation.TOKEN);
        utterance.createRelation(Relation.WORD);
        utterance.createRelation(Relation.SYLLABLE_STRUCTURE);
        utterance.createRelation(Relation.SYLLABLE);
        utterance.createRelation(Relation.SEGMENT);
        utterance.createRelation(Relation.TARGET);
        utterance.createRelation(Relation.PHRASE);
        for (Element el : tokensAndBoundaries) {
            addOneElement(utterance, el,
                          true, // create word relation
                          true, // create sylstruct relation
                          true); // create target relation
        }
        // Check if default name needs to be added to last phrase item:
        Item phraseItem = utterance.getRelation(Relation.PHRASE).getTail(); 
        if (phraseItem != null && !phraseItem.getFeatures().isPresent("name")) {
            phraseItem.getFeatures().setString("name", "BB");
        }
        // Append a last target at the very end of the utterance, because
        // the FreeTTS diphone code only creates audio up to the last target:
        Relation segmentRelation = utterance.getRelation(Relation.SEGMENT);
        Relation targetRelation = utterance.getRelation(Relation.TARGET);
        assert segmentRelation != null;
        assert targetRelation != null;
        Item lastSegment = segmentRelation.getTail();
        if (lastSegment != null && lastSegment.getFeatures().isPresent("end")) {
            float pos = lastSegment.getFeatures().getFloat("end");
            float f0;
            Item lastTarget = targetRelation.getTail();
            if (lastTarget != null) f0 = lastTarget.getFeatures().getFloat("f0");
            else f0 = 100;
            Item finalTarget = targetRelation.appendItem();
            finalTarget.getFeatures().setFloat("pos", pos);
            finalTarget.getFeatures().setFloat("f0", f0);
        }
        return utterance;
    }


    /**
     * Depending on the data type, find the right information in the sentence
     * and insert it into the utterance.
     */
    protected void fillUtterance(Utterance utterance, Element sentence)
    {
        fillUtterance(utterance, sentence,
                      true, // create word relation
                      true, // create sylstruct relation
                      true); // create target relation
    }


}

