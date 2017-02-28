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
		super(new Locale("ru"));
		setTokenizerLanguage("en");
	}
}
