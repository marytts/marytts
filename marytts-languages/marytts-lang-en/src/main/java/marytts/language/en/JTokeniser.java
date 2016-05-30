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
package marytts.language.en;

import java.util.Locale;

import marytts.datatypes.MaryData;
import marytts.datatypes.MaryDataType;
import marytts.datatypes.MaryXML;
import marytts.util.MaryUtils;
import marytts.util.dom.MaryDomUtils;
import marytts.util.dom.NameNodeFilter;

import marytts.data.Utterance;
import marytts.data.item.linguistic.Word;
import marytts.io.XMLSerializer;

/**
 *
 * @author Marc Schr&ouml;der
 */
public class JTokeniser extends marytts.modules.nlp.JTokeniser {

	/**
     *
     */
	public JTokeniser()
    {
		super(MaryDataType.RAWMARYXML, MaryDataType.TOKENS, Locale.ENGLISH);
	}

	public MaryData process(MaryData d) throws Exception
    {
		MaryData super_result = super.process(d);

        XMLSerializer xml_ser = new XMLSerializer();
        Utterance utt = xml_ser.unpackDocument(super_result.getDocument());

		normaliseToAscii(utt);

        MaryData result = new MaryData(outputType(), d.getLocale());
        result.setDocument(xml_ser.generateDocument(utt));
        return result;
	}

	protected void normaliseToAscii(Utterance utt)
    {
        for (Word w: utt.getAllWords())
        {
            String s = w.getText();
            String normalised = MaryUtils.normaliseUnicodeLetters(s, Locale.ENGLISH);
            if (!s.equals(normalised)) {
                w.setText(normalised);
            }
        }
    }
}
