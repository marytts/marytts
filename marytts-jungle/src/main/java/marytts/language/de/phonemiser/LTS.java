/**
 * Copyright 2002 DFKI GmbH.
 * All Rights Reserved.  Use is subject to license terms.
 *
 * This file is part of MARY TTS.
 *
 * MARY TTS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package marytts.language.de.phonemiser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.LinkedList;
import java.util.ListIterator;

import marytts.exceptions.MaryConfigurationException;
import marytts.modules.phonemiser.Allophone;
import marytts.modules.phonemiser.AllophoneSet;
import marytts.modules.phonemiser.Syllabifier;
import marytts.server.MaryProperties;
import marytts.util.MaryUtils;

import org.apache.log4j.Logger;

import com.sun.speech.freetts.lexicon.LetterToSound;
import com.sun.speech.freetts.lexicon.LetterToSoundImpl;


public class LTS
{
    private static Logger logger = MaryUtils.getLogger("LTS");
    LetterToSound letterToSound;
    Syllabifier syllabifier;
    boolean lowerCaseOnly;
    boolean knowUmlauts;
    AllophoneSet allophoneSet;

    public LTS()
    throws IOException, MaryConfigurationException
    {
        //        URL letterToSoundURL = new URL("file:///project/cl/mary/work/lts_with_stress/flite/mary1_lts.txt");
        URL letterToSoundURL = this.getClass().getResource("mary_lts.txt");
        boolean isBinary = false;
        letterToSound = new LetterToSoundImpl(letterToSoundURL, isBinary);
        String[] test = letterToSound.getPhones("A", null);
        if (test == null || test.length == 0 || test[0].equals(""))
            lowerCaseOnly = true;
        else
            lowerCaseOnly = false;
        test = letterToSound.getPhones("ä", null);
        if (test == null || test.length == 0 || test[0].equals(""))
            knowUmlauts = false;
        else
            knowUmlauts = true;
        allophoneSet = AllophoneSet.getAllophoneSet(MaryProperties.needFilename("german.allophoneset"));
        syllabifier = new Syllabifier(allophoneSet);
    }



    /**
     * Take a graphemic representation of a word and convert it
     * into a phonemic representation using the sampa alphabet,
     * including syllable boundaries and word stress.
     */
    public String convert(String text)
    {
    	//testing
    	logger.debug("Text to convert: "+text);
        if (!knowUmlauts)
            text = replaceUmlauts(text);
        if (lowerCaseOnly)
            text = text.toLowerCase();
        String[] phones = letterToSound.getPhones(text, null);
        //logger.debug("Array phones: "+phones);
        //Warum geht hier kein logger.debug?
       // logger.debug("Here!!!!!!: "+letterToSound.getPhones(text, null));
       	//for (int i=0; i<phones.length; i++)
       	//	System.out.println("Phone "+i+" is now: "+phones[i]);
       	
        if (phones == null) return "";
        LinkedList phoneList = new LinkedList();
        for (int i=0; i<phones.length; i++)
            // !!remove "if"
            if (!phones[i].equals("?"))
            phoneList.add(phones[i]);
        /**for (int i = 0; i<phones.length; i++)
        	if (phones[i].equals("m"))
        	System.out.println("yeahhh");**/
            
        syllabifier.syllabify(phoneList);
        insertGlottalStops(phoneList);
        StringBuilder buf = new StringBuilder();
        ListIterator it = phoneList.listIterator(0);
        while (it.hasNext()) buf.append((String) it.next());
        return buf.toString();
    }


    /**
     * For those syllables containing a "1" character, remove that "1"
     * character and add a stress marker ' at the beginning of the syllable.
     */
    private LinkedList correctStressSymbol(LinkedList phoneList)
    {
        boolean stressFound = false;
        ListIterator it = phoneList.listIterator(0);
        while (it.hasNext()) {
            String s = (String) it.next();
            //testing
            //logger.debug("String at this place: "+s);
            if (s.endsWith("1")) {
                it.set(s.substring(0, s.length()-1)); // delete "1"
                if (!stressFound) {
                    // Only add a stress marker for first occurrence of "1":
                    // Search backwards for syllable boundary or beginning of word:
                    int steps = 0;
                    while (it.hasPrevious()) {
                        steps++;
                        String t = (String) it.previous();
                        if (t.equals("-") || t.equals("_")) { // syllable boundary
                            it.next();
                            steps--;
                            break;
                        }
                    }
                    it.add("'");
                    while (steps > 0) {
                        it.next();
                        steps--;
                    }
                    stressFound = true;
                }
            }
        }
        // No stressed vowel in word?
        if (!stressFound) {
            // Stress first non-schwa syllable
            it = phoneList.listIterator(0);
            while (it.hasNext()) {
                String s = (String) it.next();
                //testing
                //logger.debug("String at next place: "+s);
                Allophone ph = allophoneSet.getAllophone(s);
                //testing
                //logger.debug("Phone at next place: "+ph);
                if (ph != null && ph.sonority() >= 5) { // non-schwa vowel
                    // Search backwards for syllable boundary or beginning of word:
                    int steps = 0;
                    while (it.hasPrevious()) {
                        steps++;
                        String t = (String) it.previous();
                        if (t.equals("-") || t.equals("_")) { // syllable boundary
                            it.next();
                            steps--;
                            break;
                        }
                    }
                    it.add("'");
                    //testing
                    //logger.debug("It at next place: "+it);
                    while (steps > 0) {
                        it.next();
                        steps--;
                    }
                    break; // OK, that's it.
                }
            }
        }
		logger.debug("phoneList: "+phoneList);
        return phoneList;
    	
    }

    /**
     * Insert a glottal stop before a stressed syllable-initial vowel.
     */
    private LinkedList insertGlottalStops(LinkedList phoneList)
    {
        ListIterator it = phoneList.listIterator(0);
        while (it.hasNext()) {
            String s = (String) it.next();
            if (s.equals("'") && it.hasNext()) {
                String n = (String) it.next();
                Allophone ph = allophoneSet.getAllophone(n);
                if (ph != null && ph.isVowel()) { // ' followed by a vowel
                    it.previous();
                    it.add("?");
                    it.next();
                }
            }
        }
        return phoneList;
    }

    /**
     * Replace umlauts by their ASCII transcription (&ouml; => oe etc.).
     */
    private String replaceUmlauts(String text)
    {
        text = text.replaceAll("ä", "ae");
        text = text.replaceAll("ö", "oe");
        text = text.replaceAll("ü", "ue");
        text = text.replaceAll("ß", "ss");
        text = text.replaceAll("Ä", "Ae");
        text = text.replaceAll("Ö", "Oe");
        text = text.replaceAll("Ü", "Ue");
        return text;
    }

    /**
     * Apply the LTS rules to stdin. Read a file from stdin, one word per line,
     * transcribe each word, and write the result to stdout.
     */
    public static void main(String[] args) throws Exception
    {
        MaryProperties.readProperties();
        LTS ltsTest = new LTS();
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        String line = null;
        while ((line = br.readLine()) != null) {
            System.out.println(ltsTest.convert(line));
           	//logger.debug("Line: "+line);
        }
    }
}
