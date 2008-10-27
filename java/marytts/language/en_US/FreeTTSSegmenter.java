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
package marytts.language.en_US;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import marytts.datatypes.MaryData;
import marytts.language.en.Segmenter;
import marytts.language.en_US.datatypes.USEnglishDataTypes;
import marytts.modules.InternalModule;
import marytts.modules.synthesis.FreeTTSVoices;
import marytts.server.MaryProperties;

import com.sun.speech.freetts.Relation;
import com.sun.speech.freetts.Utterance;
import com.sun.speech.freetts.UtteranceProcessor;



/**
 * Use an individual FreeTTS module for English synthesis.
 *
 * @author Marc Schr&ouml;der
 */

public class FreeTTSSegmenter extends InternalModule
{
    private UtteranceProcessor processor;
    private static Map<String, String> mrpa2sampa;

    public FreeTTSSegmenter()
    {
        super("Segmenter",
              // use this if you want to use the FreeTTS phraser:
              //USEnglishDataTypes.FREETTS_PHRASES,
              USEnglishDataTypes.FREETTS_POS,
              USEnglishDataTypes.FREETTS_SEGMENTS,
              Locale.US
              );
    }

    public void startup() throws Exception
    {
        super.startup();
        // Initialise FreeTTS
        FreeTTSVoices.load();
        processor = new Segmenter();
        mrpa2sampa = new HashMap<String, String>();
        fillSampaMap();
    }

    public MaryData process(MaryData d)
    throws Exception
    {
        List<Utterance> utterances = d.getUtterances();
        Iterator<Utterance> it = utterances.iterator();
        while (it.hasNext()) {
            Utterance utterance = (Utterance) it.next();
            processor.processUtterance(utterance);
            // convert to SAMPA format used in MARY:
            Relation segs = utterance.getRelation(Relation.SEGMENT);
            for (com.sun.speech.freetts.Item s = segs.getHead(); s != null; s = s.getNext()) {
                String mrpa = s.getFeatures().getString("name");
                String sampa = mrpa2sampa(mrpa);
                s.getFeatures().setString("name", sampa);
            }
        }
        MaryData output = new MaryData(outputType(), d.getLocale());
        output.setUtterances(utterances);
       
        return output;
    }
    
    
    private void fillSampaMap() throws Exception
    {
        // Any phoneme inventory mappings?
        String sampamapFilename = MaryProperties.needFilename("english.freetts.lexicon.sampamapfile");
        if (sampamapFilename != null) {
            logger.debug("For FreeTTSSegmenter, filling sampa map from file "+sampamapFilename);
            try {
                BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(sampamapFilename), "UTF-8"));
                String line;
                while ((line = br.readLine()) != null) {
                    line = line.trim();
                    if (line.equals("") || line.startsWith("#")) {
                        continue; // ignore empty and comment lines
                    }
                    try {
                        addSampaMapEntry(line);
                    } catch (IllegalArgumentException iae) {
                        throw new IllegalArgumentException("Ignoring invalid entry in sampa map file "+sampamapFilename, iae);
                    }
                }
            } catch (IOException ioe) {
                throw new IllegalArgumentException("Cannot open file '"+sampamapFilename+"'", ioe);
            }
            
        }
    }

    private void addSampaMapEntry(String entry) throws IllegalArgumentException
    {
        boolean s2v = false;
        boolean v2s = false;
        String[] parts = null;
        // For one-to-many mappings, '+' can be used to group phoneme symbols.
        // E.g., the line "EI->E:+I" would map "EI" to "E:" and "I" 
        entry.replace('+', ' ');
        if (entry.indexOf("<->") != -1) {
            parts = entry.split("<->");
            s2v = true;
            v2s = true;
        } else if (entry.indexOf("->") != -1) {
            parts = entry.split("->");
            s2v = true;
        } else if (entry.indexOf("<-") != -1) {
            parts = entry.split("<-");
            v2s = true;
        }
        if (parts == null || parts.length != 2) { // invalid entry
            throw new IllegalArgumentException();
        }
        if (v2s) {
            mrpa2sampa.put(parts[1].trim(), parts[0].trim());
        }
    }

    /** Converts a single phonetic symbol in MRPA representation
     * representation into its equivalent in MARY sampa representation.
     * @return the converted phoneme, or the input string if no known conversion exists.
     */
    public static String mrpa2sampa(String voicePhoneme)
    {
        if (mrpa2sampa.containsKey(voicePhoneme))
            return mrpa2sampa.get(voicePhoneme);
        else
            return voicePhoneme;
    }

    


    public void shutdown(){
        super.shutdown();
        try {
         //save addenda
       ((Segmenter)processor).saveAddenda();
        } catch (IOException ioe){
            ioe.printStackTrace();
            logger.warn("Could not save addenda");
        }
    }


}
