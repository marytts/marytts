/**
 * Copyright 2007 DFKI GmbH.
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
package marytts.modules.nlp;

import java.io.InputStream;

import marytts.data.Utterance;
import marytts.fst.FSTLookup;
import marytts.config.MaryProperties;
import marytts.util.MaryUtils;

import marytts.modules.MaryModule;

import marytts.config.MaryProperties;

import marytts.data.Utterance;
import marytts.data.SupportedSequenceType;
import marytts.data.Sequence;
import marytts.data.item.linguistic.Word;

import org.w3c.dom.Document;

import marytts.MaryException;

import org.apache.logging.log4j.core.Appender;

/**
 * Minimalistic part-of-speech tagger, using only function word tags as marked
 * in the Transcription GUI.
 *
 * @author Sathish Pammi
 * @author Marc Schr&ouml;der
 */

public class MinimalisticPosTagger extends MaryModule {
    private String propertyPrefix;
    private FSTLookup posFST = null;
    private String punctuationList;

    /**
     * Constructor which can be directly called from init info in the config
     * file. Different languages can call this code with different settings.
     *
     * @param locale
     *            a locale string, e.g. "en"
     * @param propertyPrefix
     *            propertyPrefix
     * @throws Exception
     *             Exception
     */
    public MinimalisticPosTagger(String locale, String propertyPrefix) throws Exception {
        super("OpenNLPPosTagger", MaryUtils.string2locale(locale));
        if (!propertyPrefix.endsWith(".")) {
            propertyPrefix = propertyPrefix + ".";
        }
        this.propertyPrefix = propertyPrefix + "partsofspeech.";
    }

    public void startup() throws Exception {
        super.startup();
        InputStream posFSTStream = MaryProperties.getStream(propertyPrefix + "fst");
        if (posFSTStream != null) {
            posFST = new FSTLookup(posFSTStream, MaryProperties.getProperty(propertyPrefix + "fst"));
        }
        punctuationList = MaryProperties.getProperty(propertyPrefix + "punctuation", ",.?!;");
    }

    /**
     *  Check if the input contains all the information needed to be
     *  processed by the module.
     *
     *  @param utt the input utterance
     *  @throws MaryException which indicates what is missing if something is missing
     */
    public void checkInput(Utterance utt) throws MaryException {
        if (!utt.hasSequence(SupportedSequenceType.WORD)) {
            throw new MaryException("Word sequence is missing", null);
        }
    }


    public Utterance process(Utterance utt, MaryProperties configuration, Appender app) throws Exception {

        for (Word w : (Sequence<Word>) utt.getSequence(SupportedSequenceType.WORD)) {
            String pos = "content";
            String tokenText = w.getText();
            if (punctuationList.contains(tokenText)) {
                pos = "$PUNCT";
            } else if (posFST != null) {
                String[] result = posFST.lookup(tokenText);
                if (result.length != 0) {
                    pos = "function";
                }
            }
            w.setPOS(pos);
        }

        return utt;
    }

}
