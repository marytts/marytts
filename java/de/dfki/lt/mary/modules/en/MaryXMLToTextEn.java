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
package de.dfki.lt.mary.modules.en;

// DOM classes
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import de.dfki.lt.mary.MaryData;
import de.dfki.lt.mary.MaryDataType;
import de.dfki.lt.mary.MaryXML;
import de.dfki.lt.mary.modules.InternalModule;
import de.dfki.lt.mary.modules.synthesis.Voice;
import de.dfki.lt.mary.util.dom.MaryDomUtils;

/**
 * Create simple text from a rawmaryxml document.
 *
 * @author Marc Schr&ouml;der
 */

public class MaryXMLToTextEn extends InternalModule
{
    public MaryXMLToTextEn()
    {
        super("MaryXMLToTextEn",
              MaryDataType.get("RAWMARYXML_EN"),
              MaryDataType.get("TEXT_EN")
              );
    }

    public void startup() throws Exception
    {
        // local startup goes here
        super.startup();
    }

    public MaryData process(MaryData d)
    throws Exception
    {
        Document doc = d.getDocument();

        MaryData result = new MaryData(outputType());
        result.setPlainText(MaryDomUtils.getPlainTextBelow(doc));
        Element voiceElement = MaryDomUtils.
            getFirstElementByTagName(doc.getDocumentElement(), MaryXML.VOICE);
        if (voiceElement != null) {
            Voice voice = Voice.getVoice(voiceElement.getAttribute("name"));
            if (voice != null)
                result.setDefaultVoice(voice);
        }
        return result;
    }
}
