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
 * A representation of any type of mary data, be it input, intermediate or
 * output data. The "technical" representation of the read data is hidden from
 * the caller, but can be accessed on request. Internally, the data is
 * appropriately represented according to this data's type, i.e. as a String
 * containing plain text, an XML DOM tree, or an input stream containing audio
 * data.
 *
 * @author Marc Schr&ouml;der
 */
public class MaryData {
	private Locale locale;

	Utterance utt;
	public MaryData(Locale locale) {
		this(locale, null);
	}

	public MaryData(Locale locale, Utterance utt) {
		this.locale = locale;
		this.utt = utt;
	}

	public Locale getLocale() {
		return locale;
	}

	public void setData(Utterance utt) {
		this.utt = utt;
	}

	public Utterance getData() {
		return this.utt;
	}
}
