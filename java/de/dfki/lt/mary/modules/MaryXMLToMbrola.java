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
package de.dfki.lt.mary.modules;

// DOM classes
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Vector;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.traversal.DocumentTraversal;
import org.w3c.dom.traversal.NodeFilter;
import org.w3c.dom.traversal.NodeIterator;

import de.dfki.lt.mary.MaryData;
import de.dfki.lt.mary.MaryDataType;
import de.dfki.lt.mary.MaryXML;
import de.dfki.lt.mary.modules.synthesis.MBROLAPhoneme;
import de.dfki.lt.mary.modules.synthesis.MbrolaVoice;
import de.dfki.lt.mary.modules.synthesis.Voice;
import de.dfki.lt.mary.util.MaryUtils;
import de.dfki.lt.mary.util.dom.MaryDomUtils;
import de.dfki.lt.mary.util.dom.NameNodeFilter;

/**
 * Transforms a full MaryXML document into an MBROLA format string
 *
 * @author Marc Schr&ouml;der
 */

public class MaryXMLToMbrola extends InternalModule
{
    public MaryXMLToMbrola()
    {
        super("MaryXMLToMbrola",
              MaryDataType.get("ACOUSTPARAMS"),
              MaryDataType.get("MBROLA")
              );
    }

    public MaryData process(MaryData d)
    throws Exception
    {
        Document doc = d.getDocument();
        NodeIterator it = ((DocumentTraversal)doc).createNodeIterator
            (doc, NodeFilter.SHOW_ELEMENT,
             new NameNodeFilter(new String[]{MaryXML.PHONE, MaryXML.BOUNDARY}),
             false);
        List elements = new ArrayList();
        Element element = null;
        Voice currentVoice = d.getDefaultVoice();
        if (currentVoice == null) {
            Locale locale = MaryUtils.string2locale(doc.getDocumentElement().getAttribute("xml:lang"));
            currentVoice = Voice.getDefaultVoice(locale);
            logger.info("No default voice associated with data. Assuming locale " + locale + " default " +
                         currentVoice.getName());
        }
        Element currentVoiceElement = null;
        StringBuffer buf = new StringBuffer();
        while ((element = (Element) it.nextNode()) != null) {
            Element v = (Element) MaryDomUtils.getAncestor(element, MaryXML.VOICE);
            if (v == null) {
                if (currentVoiceElement != null) {
                    // We have just left a voice section
                    if (!elements.isEmpty()) {
                        buf.append(convertToMbrola(elements, currentVoice));
                        elements.clear();
                    }
                    currentVoice = d.getDefaultVoice();
                    currentVoiceElement = null;
                }
            } else if (v != currentVoiceElement) {
                // We have just entered a new voice section
                if (!elements.isEmpty()) {
                    buf.append(convertToMbrola(elements, currentVoice));
                    elements.clear();
                }
                Voice newVoice = Voice.getVoice(v);
                if (newVoice != null &&
                    (buf.length() == 0 || newVoice != currentVoice)) {
                    buf.append("; voice name=");
                    buf.append(newVoice.getName());
                    buf.append("\n");
                    currentVoice = newVoice;
                }
                currentVoiceElement = v;
            }
            elements.add(element);
        }
        if (!elements.isEmpty()) {
            buf.append(convertToMbrola(elements, currentVoice));
        }
        MaryData result = new MaryData(outputType());
        result.setPlainText(buf.toString());
        return result;
    }

    /** Convert a given list of <ph> and <boundary> elements into the MBROLA
     * .pho format.
     * @throws IllegalArgumentException if one of the Elements in the List is
     * not a <ph> or <boundary> element or if the voice is not an MBROLA voice.
     */
    public String convertToMbrola(List<Element> phonemesAndBoundaries, Voice voice)
    {
        StringBuffer buf = new StringBuffer();
        // In order to test for missing diphones, we need to
        // look at two subsequent phonemes. General case:
        // A list of phonemes "in the cue":
        LinkedList<MBROLAPhoneme> mbrolaPhonemes = new LinkedList<MBROLAPhoneme>();
        for (Element element : phonemesAndBoundaries) {
            if (element.getTagName().equals(MaryXML.PHONE)) {
                // Assemble an MBROLAPhoneme object:
                String s = element.getAttribute("p");
                int dur = 0;
                try {
                    dur = Integer.parseInt(element.getAttribute("d"));
                } catch (NumberFormatException e) {}
                String f0string = element.getAttribute("f0");
                Vector<int[]> targets = new Vector<int []>();
                int i=0;
                while ((i = f0string.indexOf("(", i)) != -1) {
                    int j = f0string.indexOf(",", i);
                    int percent = 0;
                    try {
                        percent = Integer.parseInt(f0string.substring(i+1,j));
                    } catch (NumberFormatException e) {}
                    int k = f0string.indexOf(")", j);
                    int f0 = 0;
                    try {
                        f0 = Integer.parseInt(f0string.substring(j+1,k));
                    } catch (NumberFormatException e) {}
                    int[] target = new int[2];
                    target[0] = percent;
                    target[1] = f0;
                    targets.add(target);
                    i = k;
                }
                String vq = element.getAttribute("vq");
                if (vq.equals("") ||
                        !(voice instanceof MbrolaVoice && ((MbrolaVoice)voice).hasVoiceQuality(vq))) {
                    vq = null;
                }
                MBROLAPhoneme newP = new MBROLAPhoneme(s, dur, targets, vq);
                Vector<MBROLAPhoneme> p2vect = voice.convertSampa(newP);
                mbrolaPhonemes.addAll(p2vect);
                // Verify if diphone exists:
                while (mbrolaPhonemes.size() > 1) { // at least 2 phonemes
                    if (voice instanceof MbrolaVoice 
                            && !((MbrolaVoice)voice).hasDiphone
                                (mbrolaPhonemes.get(0), mbrolaPhonemes.get(1))) {
                        // Replace the first two phonemes:
                        MBROLAPhoneme p1 = mbrolaPhonemes.removeFirst();
                        MBROLAPhoneme p2 = mbrolaPhonemes.removeFirst();
                        Vector<MBROLAPhoneme> newPhones = ((MbrolaVoice)voice).replaceDiphone(p1, p2);
                        // Prepend them to list:
                        for (int l=newPhones.size()-1; l>=0; l--) {
                            MBROLAPhoneme mph = newPhones.get(l);
                            mbrolaPhonemes.addFirst(mph);
                        }
                    }
                    // And now consider the first in the list ready for output
                    MBROLAPhoneme p1 = mbrolaPhonemes.removeFirst();
                    buf.append(p1.toString());
                    buf.append("\n");
                }
            } else if (element.getTagName().equals(MaryXML.BOUNDARY)) {
                while (!mbrolaPhonemes.isEmpty()) {
                    MBROLAPhoneme p = mbrolaPhonemes.removeFirst();
                    buf.append(p.toString());
                    buf.append("\n");
                }
                String duration = element.getAttribute("duration");
                if (duration != null && !duration.equals("")) {
                    try {
                        Integer.parseInt(duration); // just to check it is an integer
                        buf.append("_ ");
                        buf.append(duration);
                        buf.append("\n");
                        // and insert a "flush" symbol after every boundary:
                        buf.append("#\n");
                    } catch (NumberFormatException nfe) {
                        logger.debug("Unexpected value for duration: '"+duration+"' -- ignoring boundary");
                    }
                }
            } else {
                throw new IllegalArgumentException
                    ("Expected only <ph> and <boundary> elements, got <" +
                     element.getTagName() + ">");
            }
        }
        while (!mbrolaPhonemes.isEmpty()) {
            MBROLAPhoneme p = mbrolaPhonemes.removeFirst();
            buf.append(p.toString());
            buf.append("\n");
        }
        return buf.toString();
    }

    /**
     * Perform a power-on self test by processing some example input data.
     * This module is actually tested as part of the MbrolaSynthesizer test,
     * for which reason this method does nothing.
     */
    public void powerOnSelfTest() throws Error
    {
    }

}
