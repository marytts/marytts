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
package marytts.modules;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import marytts.datatypes.MaryData;
import marytts.datatypes.MaryDataType;
import marytts.datatypes.MaryXML;
import marytts.exceptions.MaryConfigurationException;
import marytts.fst.FSTLookup;
import marytts.modules.phonemiser.AllophoneSet;
import marytts.modules.phonemiser.TrainedLTS;
import marytts.server.MaryProperties;
import marytts.util.MaryRuntimeUtils;
import marytts.util.MaryUtils;
import marytts.util.dom.MaryDomUtils;

import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.traversal.NodeIterator;


/**
 * The phonemiser module -- java implementation.
 *
 * @author Marc Schr&ouml;der, Sathish
 */

public class JPhonemiser extends InternalModule
{

    protected Map<String, List<String>> userdict;
    protected FSTLookup lexicon;
    protected TrainedLTS lts;
    protected boolean removeTrailingOneFromPhones = true;

    protected AllophoneSet allophoneSet;

    public JPhonemiser(String propertyPrefix)
    throws IOException,  MaryConfigurationException
    {
        this("JPhonemiser", MaryDataType.PARTSOFSPEECH, MaryDataType.PHONEMES,
        		propertyPrefix+"allophoneset",
                propertyPrefix+"userdict",
                propertyPrefix+"lexicon",
                propertyPrefix+"lettertosound",
                propertyPrefix+"removeTrailingOneFromPhones");
    }
    
    
    /**
     * Constructor providing the individual filenames of files that are required.
     * @param allophonesFilename
     * @param userdictFilename
     * @param lexiconFilename
     * @param ltsFilename
     * @throws Exception
     */
    public JPhonemiser(String componentName, 
            MaryDataType inputType, MaryDataType outputType,
            String allophonesProperty, String userdictProperty, String lexiconProperty, String ltsProperty)
    throws IOException, MaryConfigurationException
    {
        this(componentName, inputType, outputType,
        		allophonesProperty, userdictProperty, lexiconProperty, ltsProperty, null);
    }

    /**
     * Constructor providing the individual filenames of files that are required.
     * @param allophonesFilename
     * @param userdictFilename
     * @param lexiconFilename
     * @param ltsFilename
     * @param removetrailingonefromphonesBoolean
     * @throws Exception
     */
    public JPhonemiser(String componentName, 
            MaryDataType inputType, MaryDataType outputType,
            String allophonesProperty, String userdictProperty, String lexiconProperty, String ltsProperty, String removetrailingonefromphonesProperty)
    throws IOException, MaryConfigurationException
    {
        super(componentName, inputType, outputType,
        		MaryRuntimeUtils.needAllophoneSet(allophonesProperty).getLocale());
        allophoneSet = MaryRuntimeUtils.needAllophoneSet(allophonesProperty);
        // userdict is optional
        String userdictFilename = MaryProperties.getFilename(userdictProperty); // may be null
        if (userdictFilename != null) {
        	if (new File(userdictFilename).exists()) {
        		userdict = readLexicon(userdictFilename);
        	} else {
        		logger.info("User dictionary '"+userdictFilename+"' for locale '"+getLocale()+"' does not exist. Ignoring.");
        	}
        }
        InputStream lexiconStream = MaryProperties.needStream(lexiconProperty);
        lexicon = new FSTLookup(lexiconStream, lexiconProperty);
        InputStream ltsStream = MaryProperties.needStream(ltsProperty);
        if(removetrailingonefromphonesProperty != null){
            this.removeTrailingOneFromPhones = MaryProperties.getBoolean(removetrailingonefromphonesProperty, true);
        }
        lts = new TrainedLTS(allophoneSet, ltsStream, this.removeTrailingOneFromPhones);
    }


	public MaryData process(MaryData d)
        throws Exception
    {
        Document doc = d.getDocument();
        NodeIterator it = MaryDomUtils.createNodeIterator(doc, doc, MaryXML.TOKEN);
        Element t = null;
        while ((t = (Element) it.nextNode()) != null) {
                String text;
                
                // Do not touch tokens for which a transcription is already
                // given (exception: transcription contains a '*' character:
                if (t.hasAttribute("ph") &&
                    !t.getAttribute("ph").contains("*")) {
                    continue;
                }
                if (t.hasAttribute("sounds_like"))
                    text = t.getAttribute("sounds_like");
                else
                    text = MaryDomUtils.tokenText(t);
                
                String pos = null;
                // use part-of-speech if available
                if (t.hasAttribute("pos")){
                    pos = t.getAttribute("pos");
                }
                
                if (text != null && !text.equals("")) {
                    // If text consists of several parts (e.g., because that was
                    // inserted into the sounds_like attribute), each part
                    // is transcribed separately.
                    StringBuilder ph = new StringBuilder();
                    String g2pMethod = null;
                    StringTokenizer st = new StringTokenizer(text, " -");
                    while (st.hasMoreTokens()) {
                        String graph = st.nextToken();
                        StringBuilder helper = new StringBuilder();
                        String phon = phonemise(graph, pos, helper);
                        if (ph.length() == 0) { // first part
                            // The g2pMethod of the combined beast is
                            // the g2pMethod of the first constituant.
                            g2pMethod = helper.toString();
                            ph.append(phon);
                        } else { // following parts
                            ph.append(" - ");
                            // Reduce primary to secondary stress:
                            ph.append(phon.replace('\'', ','));
                       }
                    }
                    
                    if (ph != null && ph.length() > 0) {
                        setPh(t, ph.toString());
                        t.setAttribute("g2p_method", g2pMethod);
                    }
            }
        }
        MaryData result = new MaryData(outputType(), d.getLocale());
        result.setDocument(doc);
        return result;
    }

    /**
     * Phonemise the word text. This starts with a simple lexicon lookup,
     * followed by some heuristics, and finally applies letter-to-sound rules
     * if nothing else was successful. 
     * 
     * @param text the textual (graphemic) form of a word.
     * @param pos the part-of-speech of the word
     * @param g2pMethod This is an awkward way to return a second
     * String parameter via a StringBuilder. If a phonemisation of the text is
     * found, this parameter will be filled with the method of phonemisation
     * ("lexicon", ... "rules"). 
     * @return a phonemisation of the text if one can be generated, or
     * null if no phonemisation method was successful.
     */
    public String phonemise(String text, String pos, StringBuilder g2pMethod)
    {
		return this.phonemiseComplete(null, text, pos, g2pMethod);
	}

    /**
     * Phonemise the word text. This starts with a simple lexicon lookup,
     * followed by some heuristics, and finally applies letter-to-sound rules
     * if nothing else was successful. 
     * 
     * @param privatedict an additional lexicon for lookups (can be null).
     * @param text the textual (graphemic) form of a word.
     * @param pos the part-of-speech of the word
     * @param g2pMethod This is an awkward way to return a second
     * String parameter via a StringBuilder. If a phonemisation of the text is
     * found, this parameter will be filled with the method of phonemisation
     * ("lexicon", ... "rules"). 
     * @return a phonemisation of the text if one can be generated, or
     * null if no phonemisation method was successful.
     */
    public String phonemiseComplete(Map<String, List<String>> privatedict, String text, String pos, StringBuilder g2pMethod)
    {
        // First, try a simple userdict and lexicon lookup:

        String result = this.phonemiseLookupOnly(privatedict, text, pos, g2pMethod);
        if (result != null) {
            return result;
        }

        // Cannot find it in the lexicon -- apply letter-to-sound rules
        // to the normalised form

        String phones = lts.predictPronunciation(text);
        result = lts.syllabify(phones);
        if (result != null) {
            g2pMethod.append("rules");
            return result;
        }

        return null;
    }

	/**
	 * Phonemise the word text. This starts with a simple lexicon lookup,
	 * followed by some heuristics.
	 * 
     * @param privatedict an additional lexicon for lookups (can be null).
	 * @param text
	 *            the textual (graphemic) form of a word.
	 * @param pos
	 *            the part-of-speech of the word
	 * @param g2pMethod
	 *            This is an awkward way to return a second String parameter via
	 *            a StringBuilder. If a phonemisation of the text is found, this
	 *            parameter will be filled with the method of phonemisation
	 *            ("lexicon", ... "rules").
	 * @return a phonemisation of the text if one can be generated, or null if
	 *         no phonemisation method was successful.
	 */
	public String phonemiseLookupOnly(Map<String, List<String>> privatedict, String text, String pos,
			StringBuilder g2pMethod) {
		// First, try a simple userdict and lexicon lookup:

		String result = this.dictLookup(privatedict, text, pos);
		if (result != null) {
			g2pMethod.append("privatedict");
			return result;
		}

		result = this.dictLookup(this.userdict, text, pos);
		if (result != null) {
			g2pMethod.append("userdict");
			return result;
		}

		result = lexiconLookup(text, pos);
		if (result != null) {
			g2pMethod.append("lexicon");
			return result;
		}

		// Lookup attempts failed. Try normalising exotic letters
		// (diacritics on vowels, etc.), look up again:
		String normalised = MaryUtils
				.normaliseUnicodeLetters(text, getLocale());
		if (!normalised.equals(text)) {
			result = this.dictLookup(privatedict, normalised, pos);
			if (result != null) {
				g2pMethod.append("privatedict");
				return result;
			}
			result = this.dictLookup(this.userdict, normalised, pos);
			if (result != null) {
				g2pMethod.append("userdict");
				return result;
			}
			result = lexiconLookup(normalised, pos);
			if (result != null) {
				g2pMethod.append("lexicon");
				return result;
			}
		}

		return null;
	}
        
    
    
    /**
     * Look a given text up in the (standard) lexicon. part-of-speech is used 
     * in case of ambiguity.
     * 
     * @param text
     * @param pos
     * @return
     */
    public String lexiconLookup(String text, String pos)
    {
        if (text == null || text.length() == 0) return null;
        String[] entries;
        entries = lexiconLookupPrimitive(text, pos);
        // If entry is not found directly, try the following changes:
        // - lowercase the word
        // - all lowercase but first uppercase
        if (entries.length  == 0) {
            text = text.toLowerCase(getLocale());
            entries = lexiconLookupPrimitive(text, pos);
        }
        if (entries.length  == 0) {
            text = text.substring(0,1).toUpperCase(getLocale()) + text.substring(1);
            entries = lexiconLookupPrimitive(text, pos);
         }
         
         if (entries.length  == 0) return null;
         return entries[0];
    }

    private String[] lexiconLookupPrimitive(String text, String pos) {
        String[] entries;
        if (pos != null) { // look for pos-specific version first
            entries = lexicon.lookup(text+pos);
            if (entries.length == 0) {  // not found -- lookup without pos
                entries = lexicon.lookup(text);
            }
        } else {
            entries = lexicon.lookup(text);
        }
        return entries;
    }
    
    /**
     * look a given text up in the userdict. part-of-speech is used 
     * in case of ambiguity.
     * 
     * @param text
     * @param pos
     * @return
     */
    public String userdictLookup(String text, String pos)
    {
        return this.dictLookup(this.userdict,text,pos);
    }
    
    /**
     * look a given text up in the privatedict. part-of-speech is used 
     * in case of ambiguity.
     * 
     * @param text
     * @param pos
     * @return
     */
    public String dictLookup(Map<String,List<String>> privatedict, String text, String pos)
    {
        if (privatedict == null || text == null || text.length() == 0) return null;
        List<String> entries = privatedict.get(text);
        // If entry is not found directly, try the following changes:
        // - lowercase the word
        // - all lowercase but first uppercase
        if (entries  == null) {
            text = text.toLowerCase(this.getLocale());
            entries = privatedict.get(text);
         }
         if (entries == null) {
             text = text.substring(0,1).toUpperCase(this.getLocale()) + text.substring(1);
             entries = privatedict.get(text);
         }
         
         if (entries == null) return null;

         String transcr = null;
         for (String entry : entries) {
             String[] parts = entry.split("\\|");
             transcr = parts[0];
             if (parts.length > 1 && pos != null) {
                 StringTokenizer tokenizer = new StringTokenizer(entry);
                 while (tokenizer.hasMoreTokens()) {
                     String onePos = tokenizer.nextToken();
                     if (pos.equals(onePos)) return transcr; // found
                 }
             }
         }
         // no match of POS: return last entry
         return transcr;
    }
    
    /**
     * Access the allophone set underlying this phonemiser.
     * @return
     */
    public AllophoneSet getAllophoneSet()
    {
        return allophoneSet;
    }
    

    
    /**
     * Read a lexicon. Lines must have the format
     * 
     * graphemestring | phonestring | optional-parts-of-speech
     * 
     * The pos-item is optional. Different pos's belonging to one grapheme
     * chain may be separated by whitespace
     * 
     * 
     * @param lexiconFilename
     * @return
     */
    protected Map<String, List<String>> readLexicon(String lexiconFilename)
    throws IOException
    {
        String line;
        Map<String,List<String>> fLexicon = new HashMap<String,List<String>>();

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
                logger.warn("Lexicon '"+lexiconFilename+"': invalid entry for '"+graphStr+"'", re);
            }
            String phonPosStr = phonStr;
            if (lineParts.length > 2){
                String pos = lineParts[2];
                if (!pos.trim().equals(""))
                    phonPosStr += "|" + pos;
            }

            List<String> transcriptions = fLexicon.get(graphStr);
            if  (null == transcriptions) {
                transcriptions = new ArrayList<String>();
                fLexicon.put(graphStr, transcriptions);
            }
            transcriptions.add(phonPosStr);
        }
        lexiconFile.close();
        return fLexicon; 
    }

    
    protected void setPh(Element t, String ph)
    {
        if (!t.getTagName().equals(MaryXML.TOKEN))
            throw new DOMException(DOMException.INVALID_ACCESS_ERR,
                                   "Only t elements allowed, received " +
                                   t.getTagName() + ".");
        if (t.hasAttribute("ph")) {
            String prevPh = t.getAttribute("ph");
            // In previous sampa, replace star with sampa:
            String newPh = prevPh.replaceFirst("\\*", ph);
            t.setAttribute("ph", newPh);
        } else {
            t.setAttribute("ph", ph);
        }
    }

}

