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

/**
 * @author Marc Schr&ouml;der
 *
 *
 */
public class SSML_EN_Definer extends MaryDataType {
    static {
        define("SSML_EN", Locale.US, false, false, EXTERNAL_MARKUP, "speak", null,
            "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n" +
        "<speak version=\"1.0\" xmlns=\"http://www.w3.org/2001/10/synthesis\"\n" +
         "  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
         "  xsi:schemaLocation=\"http://www.w3.org/2001/10/synthesis http://www.w3.org/TR/speech-synthesis/synthesis.xsd\"\n" +
         "  xml:lang=\"en\">\n" +
         "Welcome<break/>to the world of speech synthesis!\n" +
         "</speak>\n");

    }
}
