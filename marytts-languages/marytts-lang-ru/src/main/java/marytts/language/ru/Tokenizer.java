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
package marytts.language.ru;

import java.util.ArrayList;
import java.util.Locale;

import marytts.datatypes.MaryData;
import marytts.datatypes.MaryDataType;
import marytts.datatypes.MaryXML;
import marytts.util.dom.MaryDomUtils;
import marytts.util.dom.NameNodeFilter;

import marytts.data.Utterance;
import marytts.data.Sequence;
import marytts.data.item.linguistic.Sentence;
import marytts.data.item.linguistic.Word;
import marytts.io.XMLSerializer;

/**
 *
 * @author Marc Schr&ouml;der
 */
public class Tokenizer extends marytts.modules.nlp.JTokenizer {

	/**
     *
     */
	public Tokenizer()
    {
		super(MaryDataType.RAWMARYXML, MaryDataType.TOKENS, new Locale("ru"));
		setTokenizerLanguage("en");
	}

	public MaryData process(MaryData d)
        throws Exception
    {
		MaryData result = super.process(d);
		result = splitOffDots(result);
		return result;
	}

	/**
	 * For Russian, treat all dots as standalone tokens that trigger end of sentence.
	 *
	 * @param d
	 *            d
	 */
	protected MaryData splitOffDots(MaryData d)
        throws Exception
    {

        XMLSerializer xml_ser = new XMLSerializer();
        Utterance utt = xml_ser.unpackDocument(d.getDocument());

        for (Sentence sent: (Sequence<Sentence>) utt.getSequence(Utterance.SupportedSequenceType.SENTENCE))
        {
            // Create a complete new list of word
            ArrayList<Word> words = new ArrayList<Word>();
            for (Word w:sent.getWords())
            {
                String s = w.getText();

                // Any dots to be split off?
                if (s.length() > 1 && s.endsWith("."))
                {
                    String s1 = s.substring(0, s.length() - 1);

                    Word w1 = new Word(s1);
                    words.add(w1);
                    words.add(new Word("."));
                }
                else
                {
                    words.add(w);
                }
            }

            sent.setWords(words);
        }

        MaryData result = new MaryData(outputType(), d.getLocale());
        result.setDocument(xml_ser.generateDocument(utt));
        return result;
    }
}
