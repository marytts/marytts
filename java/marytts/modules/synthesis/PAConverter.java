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
package marytts.modules.synthesis;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;

import marytts.modules.phonemiser.Allophone;
import marytts.modules.phonemiser.AllophoneSet;
import marytts.server.MaryProperties;

import org.apache.log4j.Logger;



/**
 * Phonetic Alphabet converter. Converts individual phonetic symbols between
 * different phonetic alphabets.
 * 
 * @author Marc Schr&ouml;der
 */

public class PAConverter
{
    private static Logger logger = Logger.getLogger("PAConverter");

    // The following map has as its keys Locales and as its values PhonemeSets.
    private static Map<Locale, AllophoneSet> sampa;
    
    private static Map<String,String> sampaEn2sampaDe;
 
    // Static constructor:
    static {
        sampa = new HashMap<Locale, AllophoneSet>();
        try {
            AllophoneSet usenSampa = AllophoneSet.getAllophoneSet
                (MaryProperties.needFilename("english.allophoneset"));
            sampa.put(Locale.US, usenSampa);
        } catch (Exception e) {
            logger.warn("Cannot load US English allophone set", e);
        }
        try {
            AllophoneSet deSampa = AllophoneSet.getAllophoneSet
                (MaryProperties.needFilename("german.allophoneset"));
            sampa.put(Locale.GERMAN, deSampa);
        } catch (Exception e) {
            logger.warn("Cannot load German allophone set", e);
        }



        //English Sampa to German Sampa
        sampaEn2sampaDe = new HashMap<String, String>();
        sampaEn2sampaDe.put("p_h", "p");
        sampaEn2sampaDe.put("t_h", "t");
        sampaEn2sampaDe.put("4", "t");
        sampaEn2sampaDe.put("k_h", "k");
        sampaEn2sampaDe.put("r=","6" );
        sampaEn2sampaDe.put("i", "i:");
        sampaEn2sampaDe.put("u", "u:");
        sampaEn2sampaDe.put("A", "a:");
        sampaEn2sampaDe.put("E", "E");
        sampaEn2sampaDe.put("{", "E");
        sampaEn2sampaDe.put("V", "a");
        sampaEn2sampaDe.put("AI", "aI");
        sampaEn2sampaDe.put("OI", "OY");
    }

    
    /**Converts a single phonetic symbol in English sampa representation
     * into its equivalent in German sampa representation.
     * @return original English symbol if no known conversion exists.
     */
    public static String sampaEn2sampaDe(String En)
    {
    	String result = En;
    	if(sampaEn2sampaDe.containsKey(En)){
    		result = (String)sampaEn2sampaDe.get(En);
    	}
    	return result;
    }
    
    /**Converts an english sampa string into a german sampa string, 
     * keeping syllable boundaries and stress markers
     */
    public static String sampaEnString2sampaDeString(String sEn)
    {
    	StringBuffer result = new StringBuffer();
    	StringTokenizer st = new StringTokenizer(sEn, "-");
    	while(st.hasMoreTokens()){
    		boolean stressed = false;
    		String syl = st.nextToken();
    		if(syl.startsWith("'")){
    			result.append("'");
    			stressed = true;
    		}
    		Allophone[] phon = sampa(Locale.US).splitIntoAllophones(syl);
    		for(int i=0; i<phon.length; i++){
    			String eng = phon[i].name();
    			String sDe = sampaEn2sampaDe(eng);
    			if(sDe.equals("6")&& stressed){
    				sDe = "96";
    			}
      			result.append(sDe); 
    		}
    		if(st.hasMoreTokens()){
    			result.append("-");
    		}
    	
    	}
    	return result.toString();
    	
    }

    public static AllophoneSet sampa(Locale locale)
    {
        return sampa.get(locale);
    }



}
