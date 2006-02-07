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

import de.dfki.lt.mary.MaryDataType;
import de.dfki.lt.mary.MaryXML;

/**
 * @author Marc Schr&ouml;der
 *
 *
 */
public class ACOUSTPARAMS_Definer extends MaryDataType {
    static {
        define("ACOUSTPARAMS", null, true, true, MARYXML, MaryXML.MARYXML, null,
                         "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                         "<maryxml xmlns=\"http://mary.dfki.de/2002/MaryXML\"\n" +
                         "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" version=\"0.4\"\n" +
                         "xml:lang=\"de\">\n" +
                         "<s>\n" +
                         "<phrase>\n" +
                         "<t accent=\"H+L*\" g2p_method=\"lexicon\" pos=\"ADJD\" sampa=\"vIl-\'kO-m@n\" syn_attach=\"1\" syn_phrase=\"_\">\n" +
                         "Willkommen\n" +
                         "<syllable sampa=\"vIl\">\n" +
                         "<ph d=\"65\" end=\"65\" f0=\"(0,106)\" p=\"v\"/>\n" +
                         "<ph d=\"52\" end=\"117\" p=\"I\"/>\n" +
                         "<ph d=\"46\" end=\"163\" f0=\"(61,113)\" p=\"l\"/>\n" +
                         "</syllable>\n" +
                         "<syllable accent=\"H+L*\" sampa=\"kO\" stress=\"1\">\n" +
                         "<ph d=\"88\" end=\"251\" p=\"k\"/>\n" +
                         "<ph d=\"80\" end=\"331\" f0=\"(50,80)\" p=\"O\"/>\n" +
                         "</syllable>\n" +
                         "<syllable sampa=\"m@n\">\n" +
                         "<ph d=\"70\" end=\"401\" p=\"m\"/>\n" +
                         "<ph d=\"99\" end=\"500\" p=\"@\"/>\n" +
                         "<ph d=\"71\" end=\"571\" f0=\"(100,74)\" p=\"n\"/>\n" +
                         "</syllable>\n" +
                         "</t>\n" +
                         "<t pos=\"$.\" syn_attach=\"2\" syn_phrase=\"_\">\n" +
                         "!\n" +
                         "</t>\n" +
                         "<boundary breakindex=\"6\" duration=\"410\" tone=\"L-%\"/>\n" +
                         "</phrase>\n" +
                         "</s>\n" +
                         "</maryxml>\n");

    }
}
