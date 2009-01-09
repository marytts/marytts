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
package marytts.modules.phonemiser;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import marytts.util.MaryUtils;
import marytts.util.dom.MaryDomUtils;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.traversal.NodeIterator;
import org.xml.sax.SAXException;


public class AllophoneSet
{
    private static Map<String, AllophoneSet> allophoneSets = new HashMap<String, AllophoneSet>();

    /** Return the allophone set specified by the given filename.
     * It will only be loaded if it was not loaded before.
     */
    public static AllophoneSet getAllophoneSet(String filename)
        throws SAXException, IOException, ParserConfigurationException
    {
        AllophoneSet as = allophoneSets.get(filename);
        if (as == null) {
            // Need to load it:
            as = new AllophoneSet(filename);
            allophoneSets.put(filename, as);
        }
        return as;
    }



    ////////////////////////////////////////////////////////////////////

    private String name; // the name of the allophone set
    private Locale locale; // the locale of the allophone set, e.g. US English
    private String[] featureNames;
    // The map of segment objects, indexed by their phonetic symbol:
    private Map<String, Allophone> allophones = null;
    private Allophone silence = null;

    private AllophoneSet(String filename)
    throws SAXException, IOException, ParserConfigurationException
    {
        allophones = new HashMap<String, Allophone>();
        // parse the xml file:
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setValidating(false);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(new File(filename));
        Element root = document.getDocumentElement();
        name = root.getAttribute("name");
        String xmlLang = root.getAttribute("xml:lang");
        locale = MaryUtils.string2locale(xmlLang);
        featureNames = root.getAttribute("features").split(" ");
        NodeIterator ni = MaryDomUtils.createNodeIterator(document, root, "vowel", "consonant", "silence");
        Element a;
        while ((a = (Element) ni.nextNode()) != null) {
            Allophone ap = new Allophone(a, featureNames);
            if (allophones.containsKey(ap.name()))
                throw new IllegalArgumentException("File "+filename+" contains duplicate definition of allophone '"+ap.name()+"'!");
            allophones.put(ap.name(), ap);
            if (ap.isPause()) {
                if (silence != null)
                    throw new IllegalArgumentException("File "+filename+" contains more than one silence symbol: '"+silence.name()+"' and '"+ap.name()+"'!");
                silence = ap;
            }
        }
        if (silence == null)
            throw new IllegalArgumentException("File "+filename+" does not contain a silence symbol");
    }

    public Locale getLocale()
    {
        return locale;
    }
    
    public Syllabifier getSyllabifier()
    {
        return new Syllabifier(this);
    }


    /**
     * Get the allophone with the given name, or null if there is no such allophone.
     * @param ph
     * @return
     */
    public Allophone getAllophone(String ph)
    {
        if (ph == null) return null;
        return allophones.get(ph);
    }
    
    /**
     * Obtain the silence allophone in this AllophoneSet
     * @return
     */
    public Allophone getSilence()
    {
        return silence;
    }
    
    /**
     * For the Allophone with name ph, return the value of the named feature.
     * @param ph
     * @param featureName
     * @return the allophone feature, or null if either the allophone or the feature does not exist.
     */
    public String getPhoneFeature(String ph, String featureName)
    {
        Allophone a = allophones.get(ph);
        if (a == null) return null;
        return a.getFeature(featureName);
    }
    
    /**
     * This returns the names of all allophones contained in this AllophoneSet,
     * as a Set of Strings
     */
    public Set<String> getAllophoneNames(){
        return this.allophones.keySet();
    }

    /**
     * Split a phonetic string into allophone symbols. Symbols representing
     * primary and secondary stress, syllable boundaries, and spaces, will be silently skipped.
     * @param allophoneString the phonetic string to split
     * @return an array of Allophone objects corresponding to the string given as input
     * @throws IllegalArgumentException if the allophoneString contains unknown symbols.
     */
    public Allophone[] splitIntoAllophones(String allophoneString)
    {
        List<Allophone> phones = new ArrayList<Allophone>();
        boolean haveSeenNucleus = false;
        for (int i=0; i<allophoneString.length(); i++) {
            String one = allophoneString.substring(i,i+1);
            // symbols to skip silently: primary and secondary stress, syllable boundary, and space:
            if ("',- ".contains(one))
                continue;
            // Try to cut off individual segments, 
            // starting with the longest prefixes:
            Allophone ph = null;
            String phString = "";
            if (i+2 <= allophoneString.length()) {
                String two = allophoneString.substring(i,i+2);
                // look up in phoneme list:
                ph = getAllophone(two);
                if (ph != null) {
                    // OK, a two-character segment
                    phString = two;
                    i++; // in addition to the i++ in the for loop
                }
            }
            if (ph == null) {
                ph = getAllophone(one);
                if (ph != null) {
                    // OK, a one-character segment
                    name = one;
                }
            }
            if (ph != null) {
                // have found a valid phoneme
                if (ph.isSyllabic())
                    haveSeenNucleus = true;
                else if (phString.equals("6") &&
                         !haveSeenNucleus) {
                    // This "6" is the nucleus, must be coded as "=6"
                    ph = getAllophone("=6");
                    haveSeenNucleus = true;
                }
                phones.add(ph);
            } else {
                throw new IllegalArgumentException("Found unknown symbol `" + one +
                            "' in phonetic string `" + allophoneString + "' -- ignoring.");
            }
        }
        return (Allophone[]) phones.toArray(new Allophone[0]);
    }
    
    
    public String checkAllophoneSyntax(String allophoneString)
    {
        List<Allophone> phones = new ArrayList<Allophone>();
        boolean haveSeenNucleus = false;
        for (int i=0; i<allophoneString.length(); i++) {
            String one = allophoneString.substring(i,i+1);
            // symbols to skip silently: primary and secondary stress, syllable boundary, and space:
            if ("',- ".contains(one))
                continue;
            // Try to cut off individual segments, 
            // starting with the longest prefixes:
            Allophone ph = null;
            String phString = "";
            if (i+2 <= allophoneString.length()) {
                String two = allophoneString.substring(i,i+2);
                // look up in phoneme list:
                ph = getAllophone(two);
                if (ph != null) {
                    // OK, a two-character segment
                    phString = two;
                    i++; // in addition to the i++ in the for loop
                }
            }
            if (ph == null) {
                ph = getAllophone(one);
                if (ph != null) {
                    // OK, a one-character segment
                    name = one;
                }
            }
            if (ph != null) {
                // have found a valid phoneme
                if (ph.isSyllabic())
                    haveSeenNucleus = true;
                else if (phString.equals("6") &&
                         !haveSeenNucleus) {
                    // This "6" is the nucleus, must be coded as "=6"
                    ph = getAllophone("=6");
                    haveSeenNucleus = true;
                }
                phones.add(ph);
            } else {
                return "Found unknown symbol `" + one +"' in phonetic string `" + allophoneString + "' -- ignoring.";
            }
        }
        return "OK";
    }
    
    
}
