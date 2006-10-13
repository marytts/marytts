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
package de.dfki.lt.mary.modules.phonemiser;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;


public class PhonemeSet
{
    private static Map phonemeSets = new HashMap();

    /** Return the phoneme set specified by the given filename.
     * It will only be loaded if it was not loaded before.
     */
    public static PhonemeSet getPhonemeSet(String filename)
        throws SAXException, IOException, ParserConfigurationException
    {
        PhonemeSet ps = (PhonemeSet) phonemeSets.get(filename);
        if (ps == null) {
            // Need to load it:
            ps = new PhonemeSet(filename);
            phonemeSets.put(filename, ps);
        }
        return ps;
    }



    ////////////////////////////////////////////////////////////////////

    // The map of segment objects, indexed by their phonetic symbol:
    private Map phonemes = null;

    private PhonemeSet(String filename)
    throws SAXException, IOException, ParserConfigurationException
    {
        phonemes = new HashMap();
        // parse the xml file:
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setValidating(false);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(new File(filename));
        // In document, ignore everything that is not a segment element:
        NodeList segElements = document.getElementsByTagName("segment");
        for (int i=0; i<segElements.getLength(); i++) {
            Element seg = (Element) segElements.item(i);
            String name = seg.getAttribute("s");
            int inherentDuration = Integer.parseInt(seg.getAttribute("inh"));
            int minimalDuration = Integer.parseInt(seg.getAttribute("min"));
            String phonology = seg.getAttribute("phonology");
            int sonority = Integer.parseInt(seg.getAttribute("sonority"));
            String example = seg.getAttribute("ex");
    
            phonemes.put(name, new Phoneme(name, inherentDuration,
                                           minimalDuration, phonology,
                                           sonority, example));
        }
    }

    public Syllabifier getSyllabifier()
    {
        return new Syllabifier(this);
    }

    /**
     * Return the Phoneme object with the given name (phonetic symbol),
     * or <code>null</code> if no Phoneme has that name.
     */
    public Phoneme getPhoneme(String name)
    {
        if (name == null) return null;
        return (Phoneme) phonemes.get(name);
    }

    /**
     * Split a phoneme string into phonemes. Any non-phoneme characters in the
     * string will be ignored.
     * @param phonemeString the phoneme string to split
     */
    public Phoneme[] splitIntoPhonemes(String phonemeString)
    {
        Vector phones = new Vector();
        boolean haveSeenNucleus = false;
        for (int i=0; i<phonemeString.length(); i++) {
            // Try to cut off individual segments, 
            // starting with the longest prefixes:
            Phoneme ph = null;
            String name = "";
            if (i+2 <= phonemeString.length()) {
                String two = phonemeString.substring(i,i+2);
                // look up in phoneme list:
                ph = getPhoneme(two);
                if (ph != null) {
                    // OK, a two-character segment
                    name = two;
                    i++; // in addition to the i++ in the for loop
                }
            }
            if (ph == null) {
                String one = phonemeString.substring(i,i+1);
                ph = getPhoneme(one);
                if (ph != null) {
                    // OK, a one-character segment
                    name = one;
                }
            }
            if (ph != null) {
                // have found a valid phoneme
                if (ph.isSyllabic())
                    haveSeenNucleus = true;
                else if (name.equals("6") &&
                         !haveSeenNucleus) {
                    // This "6" is the nucleus, must be coded as "=6"
                    ph = getPhoneme("=6");
                    haveSeenNucleus = true;
                }
                phones.add(ph);
            } else {
                //logger.warn("Found unknown phoneme `" +
                //            phonemeString.substring(i,i+1) +
                //            "' in phoneme string `" + phonemeString +
                //            "' -- ignoring.");
            }
        }
        return (Phoneme[]) phones.toArray(new Phoneme[0]);
    }
}
