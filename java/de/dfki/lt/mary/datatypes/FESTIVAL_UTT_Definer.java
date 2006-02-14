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

/**
 * @author Marc Schr&ouml;der
 *
 *
 */
public class FESTIVAL_UTT_Definer extends MaryDataType {
    static {
        define("FESTIVAL_UTT", null, true, true, PLAIN_TEXT, null, null,
                "===Utterance===\n"+
                "voice=kevin16\n"+
                "==Segment==\n"+
                "#\n"+
                "0.055000003 100 w\n"+
                "0.163 100 eh\n"+
                "0.255 100 l\n"+
                "0.341 100 k\n"+
                "0.394 100 ax\n"+
                "0.488 100 m\n"+
                "0.688 100 pau\n"+
                "==Target==\n"+
                "#\n"+
                "0.109 100 106.0\n"+
                "0.3675 100 93.0\n"+
                "0.488 100 89.0\n"+
                "0.688 100 89.0\n"+
                "==Syllable==\n"+
                "#\n"+
                "0.341 100 wehlk ; stress 1\n"+
                "0.488 100 axm ; stress 0\n"+
                "==Word==\n"+
                "#\n"+
                "0.488 100 welcome\n"+
                "==IntEvent==\n"+
                "#\n"+
                "0.488 100 L-L%\n"+
                "0.488 100 !H*\n"+
                "==Phrase==\n"+
                "#\n"+
                "0.488 100 4\n");

    }
}
