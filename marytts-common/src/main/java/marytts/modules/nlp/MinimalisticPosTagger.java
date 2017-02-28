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

import marytts.datatypes.MaryData;
import marytts.datatypes.MaryXML;
import marytts.fst.FSTLookup;
import marytts.server.MaryProperties;
import marytts.util.MaryUtils;
import marytts.util.dom.MaryDomUtils;

import marytts.modules.InternalModule;

import marytts.io.XMLSerializer;
import marytts.data.Utterance;
import marytts.data.item.linguistic.Word;

import org.w3c.dom.Document;

/**
 * Minimalistic part-of-speech tagger, using only function word tags as marked in the Transcription GUI.
 *
 * @author Sathish Pammi
 * @author Marc Schr&ouml;der
 */

public class MinimalisticPosTagger extends InternalModule {
	private String propertyPrefix;
	private FSTLookup posFST = null;
	private String punctuationList;

	/**
	 * Constructor which can be directly called from init info in the config file. Different languages can call this code with
	 * different settings.
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
		if (!propertyPrefix.endsWith("."))
			propertyPrefix = propertyPrefix + ".";
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

	public MaryData process(MaryData d) throws Exception {
		Utterance utt = d.getData();

        for (Word w: utt.getAllWords())
        {
            String pos = "content";
            String tokenText = w.getText();
            if (punctuationList.contains(tokenText)) {
                pos = "$PUNCT";
            } else if (posFST != null) {
                String[] result = posFST.lookup(tokenText);
                if (result.length != 0)
                    pos = "function";
            }
            w.setPOS(pos);
        }

        MaryData result = new MaryData( d.getLocale(), utt);
        return result;
	}

}
