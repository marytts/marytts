/**
 * Copyright 2000-2008 DFKI GmbH.
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
package marytts.language.en_US.datatypes;

import marytts.datatypes.MaryDataType;
import marytts.datatypes.MaryXML;

/**
 * This class will register the data types that are specific for the
 * US English synthesis modules.
 * @author marc
 *
 */
public class USEnglishDataTypes 
{
    public static final MaryDataType FREETTS_CONTOUR             = new MaryDataType("FREETTS_CONTOUR", false, false, MaryDataType.UTTERANCES);
    public static final MaryDataType FREETTS_DURATIONS           = new MaryDataType("FREETTS_DURATIONS", false, false, MaryDataType.UTTERANCES);
    public static final MaryDataType FREETTS_INTONATION          = new MaryDataType("FREETTS_INTONATION", false, false, MaryDataType.UTTERANCES);
    public static final MaryDataType FREETTS_MBROLISED_DURATIONS = new MaryDataType("FREETTS_MBROLISED_DURATIONS", false, false, MaryDataType.UTTERANCES);
    public static final MaryDataType FREETTS_PAUSES              = new MaryDataType("FREETTS_PAUSES", false, false, MaryDataType.UTTERANCES);
    public static final MaryDataType FREETTS_PHRASES             = new MaryDataType("FREETTS_PHRASES", false, false, MaryDataType.UTTERANCES);
    public static final MaryDataType FREETTS_POS                 = new MaryDataType("FREETTS_POS", false, false, MaryDataType.UTTERANCES);
    public static final MaryDataType FREETTS_POSTPROCESSED       = new MaryDataType("FREETTS_POSTPROCESSED", false, false, MaryDataType.UTTERANCES);
    public static final MaryDataType FREETTS_SEGMENTS            = new MaryDataType("FREETTS_SEGMENTS", false, false, MaryDataType.UTTERANCES);
    public static final MaryDataType FREETTS_TOKENS              = new MaryDataType("FREETTS_TOKENS", false, false, MaryDataType.UTTERANCES);
    public static final MaryDataType FREETTS_WORDS               = new MaryDataType("FREETTS_WORDS", false, false, MaryDataType.UTTERANCES);
    public static final MaryDataType PAUSES_US                   = new MaryDataType("PAUSES_US", true, true, MaryDataType.MARYXML, MaryXML.MARYXML);
    public static final MaryDataType PHRASES_US                  = new MaryDataType("PHRASES_US", true, true, MaryDataType.MARYXML, MaryXML.MARYXML);
}

