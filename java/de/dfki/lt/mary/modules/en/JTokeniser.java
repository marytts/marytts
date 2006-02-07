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

import java.util.Locale;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.traversal.DocumentTraversal;
import org.w3c.dom.traversal.NodeFilter;
import org.w3c.dom.traversal.NodeIterator;

import de.dfki.lt.mary.MaryData;
import de.dfki.lt.mary.MaryDataType;
import de.dfki.lt.mary.MaryXML;
import de.dfki.lt.mary.util.MaryUtils;
import de.dfki.lt.mary.util.dom.MaryDomUtils;
import de.dfki.lt.mary.util.dom.NameNodeFilter;

/**
 *
 * @author Marc Schr&ouml;der
 */
public class JTokeniser extends de.dfki.lt.mary.modules.JTokeniser {

    /**
     * 
     */
    public JTokeniser() {
        super(MaryDataType.get("RAWMARYXML_EN"), MaryDataType.get("TOKENS_EN"));
    }

    public MaryData process(MaryData d) throws Exception
    {
        MaryData result = super.process(d);
        normaliseToAscii(result);
        propagateForceAccent(result);
        return result;
    }
    
    protected void normaliseToAscii(MaryData d)
    {
        Document doc = d.getDocument();
        NodeIterator ni = ((DocumentTraversal) doc).createNodeIterator(doc,
            NodeFilter.SHOW_ELEMENT, new NameNodeFilter(MaryXML.TOKEN), false);
        Element t = null;
        while ((t = (Element) ni.nextNode()) != null) {
            String s = MaryDomUtils.tokenText(t);
            String normalised = MaryUtils.normaliseUnicodeLetters(s, Locale.ENGLISH);
            if (!s.equals(normalised)) {
                MaryDomUtils.setTokenText(t, normalised);
            }
        }
    }

    /**
     * In current FreeTTS code, prosody elements get lost. So remember
     * at least the force-accent element on individual tokens:
     * @param d
     */
    protected void propagateForceAccent(MaryData d)
    {
        Document doc = d.getDocument();
        NodeIterator prosodyNI = ((DocumentTraversal)doc).createNodeIterator(doc,
                NodeFilter.SHOW_ELEMENT, new NameNodeFilter(MaryXML.PROSODY), false);
        Element prosody = null;
        while ((prosody = (Element) prosodyNI.nextNode()) != null) {
            if (prosody.hasAttribute("force-accent")) {
                String forceAccent = prosody.getAttribute("force-accent");
                String accent = null;
                if (forceAccent.equals("none")) {
                    accent = "none";
                } else {
                    accent = "unknown";
                }
                NodeIterator tNI = ((DocumentTraversal)doc).createNodeIterator(prosody,
                    NodeFilter.SHOW_ELEMENT, new NameNodeFilter(MaryXML.TOKEN), false);
                Element t = null;
                while ((t = (Element) tNI.nextNode()) != null) {
                    if (!t.hasAttribute("accent")) {
                        t.setAttribute("accent", accent);
                    }
                } // while t
            }
        } // while prosody
    }
}
