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
package marytts.datatypes;


import marytts.data.Utterance;
import java.util.Locale;


import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**
 * A representation of any type of mary data, be it input, intermediate or output data. The "technical" representation of the read
 * data is hidden from the caller, but can be accessed on request. Internally, the data is appropriately represented according to
 * this data's type, i.e. as a String containing plain text, an XML DOM tree, or an input stream containing audio data.
 *
 * @author Marc Schr&ouml;der
 */
public class MaryData {
	private MaryDataType type;
	private Locale locale;

    Utterance utt;
	public MaryData(MaryDataType type, Locale locale) {
        this(type, locale, null);
	}

	public MaryData(MaryDataType type, Locale locale, Utterance utt) {
        this.type = type;
        this.locale = locale;
        this.utt = utt;

	}

	public MaryDataType getType() {
		return type;
	}

	public Locale getLocale() {
		return locale;
	}

	/**
	 * Set the content data of this MaryData object from the given String. For XML data ({@link MaryDataType#isXMLType()}), parse
	 * the String representation of the data into a DOM tree.
	 *
	 * @param dataString
	 *            string representation of the input data.
	 * @throws ParserConfigurationException
	 *             ParserConfigurationException
	 * @throws IOException
	 *             IOException
	 * @throws SAXException
	 *             SAXException
	 * @throws IllegalArgumentException
	 *             if this method is called for MaryDataTypes that are neither text nor XML.
	 */
	public void setData(Utterance utt)
    {
        this.utt = utt;
	}

    public Utterance getData()
    {
        return this.utt;
    }
}
