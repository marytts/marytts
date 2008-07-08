/**
 * Copyright 2000-2008 DFKI GmbH.
 * All Rights Reserved.  Use is subject to license terms.
 * 
 * Permission is hereby granted, free of charge, to use and distribute
 * this software and its documentation without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of this work, and to
 * permit persons to whom this work is furnished to do so, subject to
 * the following conditions:
 * 
 * 1. The code must retain the above copyright notice, this list of
 *    conditions and the following disclaimer.
 * 2. Any modifications must be clearly marked as such.
 * 3. Original authors' names are not deleted.
 * 4. The authors' names are not used to endorse or promote products
 *    derived from this software without specific prior written
 *    permission.
 *
 * DFKI GMBH AND THE CONTRIBUTORS TO THIS WORK DISCLAIM ALL WARRANTIES WITH
 * REGARD TO THIS SOFTWARE, INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS, IN NO EVENT SHALL DFKI GMBH NOR THE
 * CONTRIBUTORS BE LIABLE FOR ANY SPECIAL, INDIRECT OR CONSEQUENTIAL
 * DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR
 * PROFITS, WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS
 * ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE OF
 * THIS SOFTWARE.
 */
package marytts.language.en_US.datatypes;

import java.util.Locale;

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
