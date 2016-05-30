/**
 * Copyright 2002-2008 DFKI GmbH.
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

package marytts.language.ru;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import javax.xml.parsers.ParserConfigurationException;

import marytts.datatypes.MaryData;
import marytts.datatypes.MaryDataType;
import marytts.datatypes.MaryXML;
import marytts.exceptions.MaryConfigurationException;
import marytts.fst.FSTLookup;
import marytts.modules.InternalModule;
import marytts.modules.nlp.phonemiser.AllophoneSet;
import marytts.server.MaryProperties;
import marytts.util.MaryRuntimeUtils;
import marytts.util.dom.MaryDomUtils;

import org.w3c.dom.Document;


import marytts.io.XMLSerializer;
import marytts.data.Utterance;
import marytts.data.item.linguistic.Word;
import marytts.data.item.phonology.Phoneme;


/**
 * Russian phonemiser module
 *
 * @author Nickolay V. Shmyrev, Marc Schr&ouml;der, Sathish
 */

public class Phonemiser extends InternalModule {
	// TODO: This reads the userdict only. Replace with a mechanism that can deal with FST lexicon and LTS rules

	protected Map<String, List<String>> userdict;
	protected FSTLookup lexicon;

	protected AllophoneSet allophoneSet;

	public Phonemiser(String propertyPrefix) throws IOException, ParserConfigurationException,
        MaryConfigurationException {
		this("Phonemiser", MaryDataType.PARTSOFSPEECH, MaryDataType.PHONEMES, propertyPrefix + "allophoneset", propertyPrefix
             + "userdict");
	}

	/**
	 * Constructor providing the individual filenames of files that are required.
	 *
	 * @param componentName
	 *            componentName
	 * @param inputType
	 *            inputType
	 * @param outputType
	 *            outputType
	 * @param allophonesProperty
	 *            allophonesProperty
	 * @param userdictProperty
	 *            userdictProperty
	 * @throws IOException
	 *             IOException
	 * @throws ParserConfigurationException
	 *             ParserConfigurationException
	 * @throws MaryConfigurationException
	 *             MaryConfigurationException
	 */
	public Phonemiser(String componentName, MaryDataType inputType, MaryDataType outputType, String allophonesProperty,
                      String userdictProperty)
        throws IOException, ParserConfigurationException, MaryConfigurationException
    {
		super(componentName, inputType, outputType, MaryRuntimeUtils.needAllophoneSet(allophonesProperty).getLocale());
		allophoneSet = MaryRuntimeUtils.needAllophoneSet(allophonesProperty);
		// userdict is optional
		// Actually here, the user dict is the only source of information we have, so it is not optional:
		String userdictFilename = MaryProperties.needFilename(userdictProperty);
		if (userdictFilename != null)
			userdict = readLexicon(userdictFilename);
	}

	public MaryData process(MaryData d)
        throws Exception
    {

        XMLSerializer xml_ser = new XMLSerializer();
        Utterance utt = xml_ser.unpackDocument(d.getDocument());

        for (Word w: utt.getAllWords())
        {
            String text;


            // Do not touch tokens for which a transcription is already
            // given (exception: transcription contains a '*' character:
            if (w.getPhonemes().size() > 0)
                continue;

            if (w.soundsLike() != null)
                text = w.soundsLike();
            else
                text = w.getText();

            // use part-of-speech if available
            String pos = w.getPOS();


            ArrayList<Phoneme> new_phonemes = new ArrayList<Phoneme>();

            // If text consists of several parts (e.g., because that was
            // inserted into the sounds_like attribute), each part
            // is transcribed separately.
            StringBuilder ph = new StringBuilder();
            String g2p_method = null;
            StringTokenizer st = new StringTokenizer(text, " -");
            while (st.hasMoreTokens()) {
                String graph = st.nextToken();
                StringBuilder helper = new StringBuilder();
                if (pos.equals("$PUNCT")) {
                    continue;
                }

                String phon = phonemise(graph, pos, helper);


                // FIXME what does it mean : null result should not be processed
                if (phon == null)
                    continue;

                if (ph.length() == 0)
                    g2p_method = helper.toString();

                // FIXME: hardcoded here, not really good
                String stress = null;
                if (phon.contains("'"))
                {
                    stress = ",";
                    phon.replaceAll("'", "");
                }

                new_phonemes.add(new Phoneme(phon, stress));
            }


            if (new_phonemes.size() > 0)
            {
                // Adapt phoneme
                w.setPhonemes(new_phonemes);

                // Adapt G2P method
                w.setG2PMethod(g2p_method);
            }
        }


        MaryData result = new MaryData(outputType(), d.getLocale());
        result.setDocument(xml_ser.generateDocument(utt));
        return result;
    }

    /**
     * Phonemise the word text. This starts with a simple lexicon lookup, followed by some heuristics, and finally applies
     * letter-to-sound rules if nothing else was successful.
     *
     * @param text
     *            the textual (graphemic) form of a word.
     * @param pos
     *            the part-of-speech of the word
     * @param g2pMethod
     *            This is an awkward way to return a second String parameter via a StringBuilder. If a phonemisation of the text
     *            is found, this parameter will be filled with the method of phonemisation ("lexicon", ... "rules").
     * @return a phonemisation of the text if one can be generated, or null if no phonemisation method was successful.
     * @throws IOException
     *             IOException
     */
    public String phonemise(String text, String pos, StringBuilder g2pMethod)
        throws IOException
    {
        // First, try a simple userdict lookup:

        String result = userdictLookup(text, pos);
        if (result != null) {
            g2pMethod.append("userdict");
            return result;
        }
        return null;
    }

    /**
     * look a given text up in the userdict. part-of-speech is used in case of ambiguity.
     *
     * @param text
     *            text
     * @param pos
     *            pos
     * @return null if userdict == null || text == null || text.length() == 0, null if entries == null, transcr otherwise
     */
    public String userdictLookup(String text, String pos)
    {
        if (userdict == null || text == null || text.length() == 0)
            return null;
        List<String> entries = userdict.get(text);
        // If entry is not found directly, try the following changes:
        // - lowercase the word
        // - all lowercase but first uppercase
        if (entries == null) {
            text = text.toLowerCase(getLocale());
            entries = userdict.get(text);
        }
        if (entries == null) {
            text = text.substring(0, 1).toUpperCase(getLocale()) + text.substring(1);
            entries = userdict.get(text);
        }

        if (entries == null)
            return null;

        String transcr = null;
        for (String entry : entries) {
            String[] parts = entry.split("\\|");
            transcr = parts[0];
            if (parts.length > 1 && pos != null) {
                StringTokenizer tokenizer = new StringTokenizer(entry);
                while (tokenizer.hasMoreTokens()) {
                    String onePos = tokenizer.nextToken();
                    if (pos.equals(onePos))
                        return transcr; // found
                }
            }
        }
        // no match of POS: return last entry
        return transcr;
    }

    /**
     * Read a lexicon. Lines must have the format
     *
     * graphemestring | phonestring | optional-parts-of-speech
     *
     * The pos-item is optional. Different pos's belonging to one grapheme chain may be separated by whitespace
     *
     *
     * @param lexiconFilename
     *            lexiconFilename
     * @throws IOException
     *             IOException
     * @return fLexicon
     */
    protected Map<String, List<String>> readLexicon(String lexiconFilename) throws IOException {
        String line;
        Map<String, List<String>> fLexicon = new HashMap<String, List<String>>();

        BufferedReader lexiconFile = new BufferedReader(new InputStreamReader(new FileInputStream(lexiconFilename), "UTF-8"));
        while ((line = lexiconFile.readLine()) != null) {
            // Ignore empty lines and comments:
            if (line.trim().equals("") || line.startsWith("#"))
                continue;

            String[] lineParts = line.split("\\s*\\|\\s*");
            String graphStr = lineParts[0];
            String phonStr = lineParts[1];
            try {
                allophoneSet.splitIntoAllophones(phonStr);
            } catch (RuntimeException re) {
                logger.warn("Lexicon '" + lexiconFilename + "': invalid entry for '" + graphStr + "'", re);
            }
            String phonPosStr = phonStr;
            if (lineParts.length > 2) {
                String pos = lineParts[2];
                if (!pos.trim().equals(""))
                    phonPosStr += "|" + pos;
            }

            List<String> transcriptions = fLexicon.get(graphStr);
            if (null == transcriptions) {
                transcriptions = new ArrayList<String>();
                fLexicon.put(graphStr, transcriptions);
            }
            transcriptions.add(phonPosStr);
        }
        lexiconFile.close();
        return fLexicon;
    }
}
