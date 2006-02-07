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

import java.util.StringTokenizer;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import de.dfki.lt.mary.MaryData;
import de.dfki.lt.mary.MaryDataType;
import de.dfki.lt.mary.MaryXML;
import de.dfki.lt.mary.modules.phonemiser.Phoneme;
import de.dfki.lt.mary.modules.phonemiser.PhonemeSet;
import de.dfki.lt.mary.modules.synthesis.Voice;


/**
 * Read a simple phoneme string and generate default acoustic parameters.
 *
 * @author Marc Schr&ouml;der
 */

public class SimplePhoneme2AP extends InternalModule
{
    private DocumentBuilderFactory factory = null;
    private DocumentBuilder docBuilder = null;
    protected PhonemeSet phonemeSet;

    public SimplePhoneme2AP(MaryDataType inputType, MaryDataType outputType)
    {
        super("SimplePhoneme2AP",
              inputType,
              outputType
              );
    }

    public void startup() throws Exception
    {
        if (phonemeSet == null) {
            throw new NullPointerException("Subclass needs to instantiate phonemeSet");
        }
        if (factory == null) {
            factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
        }
        if (docBuilder == null) {
            docBuilder = factory.newDocumentBuilder();
        }
        super.startup();
    }

    public MaryData process(MaryData d)
    throws Exception
    {
        String phonemeString = d.getPlainText();
        MaryData result = new MaryData(outputType(), true);
        Document doc = result.getDocument();
        Element root = doc.getDocumentElement();
        root.setAttribute("xml:lang", inputType().getLocale().toString());
        Element insertHere = root;
        Voice defaultVoice = d.getDefaultVoice();
        if (defaultVoice != null) {
            Element voiceElement = MaryXML.createElement(doc, MaryXML.VOICE);
            voiceElement.setAttribute("name", defaultVoice.getName());
            root.appendChild(voiceElement);
            insertHere = voiceElement;
        }
        int cumulDur = 0;
        boolean isFirst = true;
        StringTokenizer stTokens = new StringTokenizer(phonemeString);
        while (stTokens.hasMoreTokens()) {
            Element token = MaryXML.createElement(doc, MaryXML.TOKEN);
            insertHere.appendChild(token);
            String tokenPhonemes = stTokens.nextToken();
            token.setAttribute("sampa", tokenPhonemes);
            StringTokenizer stSyllables = new StringTokenizer(tokenPhonemes, "-_");
            while (stSyllables.hasMoreTokens()) { 
                Element syllable = MaryXML.createElement(doc, MaryXML.SYLLABLE);
                token.appendChild(syllable);
                String syllablePhonemes = stSyllables.nextToken();
                syllable.setAttribute("sampa", syllablePhonemes);
                int stress = 0;
                if (syllablePhonemes.startsWith("'")) stress = 1;
                else if (syllablePhonemes.startsWith(",")) stress = 2;
                if (stress != 0) {
                    // Simplified: Give a "pressure accent" do stressed syllables
                    syllable.setAttribute("accent", "*");
                    token.setAttribute("accent", "*");
                }
                Phoneme[] phonemes = phonemeSet.splitIntoPhonemes(syllablePhonemes);
                for (int i=0; i<phonemes.length; i++) {
                    Element ph = MaryXML.createElement(doc, MaryXML.PHONE);
                    ph.setAttribute("p", phonemes[i].name());
                    int dur = phonemes[i].inherentDuration();
                    if (phonemes[i].isVowel()) {
                        if (stress == 1) dur *= 1.5;
                        else if (stress == 2) dur *= 1.2;
                    }
                    ph.setAttribute("d", String.valueOf(dur));
                    cumulDur += dur;
                    ph.setAttribute("end", String.valueOf(cumulDur));
                    // Set top start for first and base end for last segment:
                    if (defaultVoice != null) {
                        if (isFirst) {
                            isFirst = false;
                            ph.setAttribute("f0", "(0," + defaultVoice.topStart() + ")");
                        } else if (i == phonemes.length - 1 &&
                                   !stTokens.hasMoreTokens() &&
                                   !stSyllables.hasMoreTokens()) {
                            ph.setAttribute("f0", "(100," + defaultVoice.baseEnd() + ")");
                        }
                    }
                    token.appendChild(ph);
                }
            }
        }
        Element boundary = MaryXML.createElement(doc, MaryXML.BOUNDARY);
        boundary.setAttribute("bi", "4");
        boundary.setAttribute("duration", "400");
        insertHere.appendChild(boundary);
        return result;
    }
}
