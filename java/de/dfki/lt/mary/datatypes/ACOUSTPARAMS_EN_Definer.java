/**
 * Copyright 2000-2006 DFKI GmbH.
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
package de.dfki.lt.mary.datatypes;

import java.util.Locale;

import de.dfki.lt.mary.MaryDataType;
import de.dfki.lt.mary.MaryXML;

/**
 * @author Marc Schr&ouml;der
 *
 *
 */
public class ACOUSTPARAMS_EN_Definer extends MaryDataType {
    static {
        define("ACOUSTPARAMS_EN", Locale.US, false, false, MARYXML, MaryXML.MARYXML, null,
	       "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
	       "<maryxml xmlns=\"http://mary.dfki.de/2002/MaryXML\"\n" +
	       "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" version=\"0.4\"\n" +
	       "xml:lang=\"en\">\n" +
	       "<s>\n" +
	       "<phrase>\n" +
	       "<t accent=\"H*\" pos=\"content\" sampa=\"'wElk-@m\">\n" +
	       "Welcome\n" +
	       "<syllable accent=\"H*\" sampa=\"'wElk\" stress=\"1\">\n" +
	       "<ph d=\"61\" end=\"61\" f0=\"(0,221)\" p=\"w\"/>\n" +
	       "<ph d=\"120\" end=\"182\" f0=\"(50,232)\" p=\"E\"/>\n" +
	       "<ph d=\"102\" end=\"284\" p=\"l\"/>\n" +
	       "<ph d=\"96\" end=\"380\" p=\"k\"/>\n" +
	       "</syllable>\n" +
	       "<syllable sampa=\"@m\">\n" +
	       "<ph d=\"59\" end=\"440\" f0=\"(50,176)\" p=\"@\"/>\n" +
	       "<ph d=\"104\" end=\"544\" f0=\"(100,156)\" p=\"m\"/>\n" +
	       "</syllable>\n" +
	       "</t>\n" +
	       "<boundary breakindex=\"4\" duration=\"200\" tone=\"L-L%\"/>\n" +
	       "<t pos=\"$PUNCT\">\n" +
	       "!\n" +
	       "</t>\n" +
	       "</phrase>\n" +
	       "</s>\n" +
	       "</maryxml>\n");
    }
}
