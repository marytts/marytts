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

import marytts.datatypes.MaryDataType;
import marytts.language.en_US.datatypes.USEnglishDataTypes;
import marytts.modules.Utt2XMLBase;

/**
 * Convert FreeTTS utterances into MaryXML format (Intonation, English).
 * 
 * @author Marc Schr&ouml;der
 */

public class Utt2XMLIntonationEn extends Utt2XMLBase {
	public Utt2XMLIntonationEn() {
		super("Utt2XML IntonationEn", USEnglishDataTypes.FREETTS_INTONATION, MaryDataType.INTONATION, Locale.ENGLISH);
	}

}
