/**
 * Portions Copyright 2006 DFKI GmbH.
 * Portions Copyright 2001 Sun Microsystems, Inc.
 * Portions Copyright 1999-2001 Language Technologies Institute, 
 * Carnegie Mellon University.
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

package de.dfki.lt.mary.unitselection.featureprocessors.en;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.net.URL;
import java.util.*;

import de.dfki.lt.mary.MaryProperties;
import de.dfki.lt.mary.unitselection.featureprocessors.*;


public class UtteranceFeatProcManager_en extends UtteranceFeatProcManager{
    
    private Map processors_en;
    private Map processors_gen;
    
    /**
     * Builds a new manager. 
     * This manager uses the english phoneset of FreeTTS
     * and a PoS conversion file if the english PoS tagger is used.
     * All feature processors loaded are language specific.
     */
    public UtteranceFeatProcManager_en(){
        processors_gen = UtteranceFeatProcManager.getProcessors();
        processors_en = new HashMap();
        try{
            //property is set in english.config
            URL phoneSetURL = new URL("file:"
	            +MaryProperties.needFilename("english.freetts.phoneSetFile"));
        PhoneSet phoneSet  = new PhoneSetImpl(phoneSetURL);
        Map posConverter = loadPosConverter();
        processors_en.put("gpos", new LanguageFeatureProcessors.Gpos(posConverter));
        
        processors_en.put("ph_cplace", new LanguageFeatureProcessors.PH_CPlace(phoneSet));
        processors_en.put("ph_ctype", new LanguageFeatureProcessors.PH_CType(phoneSet));
        processors_en.put("ph_cvox", new LanguageFeatureProcessors.PH_CVox(phoneSet));
        processors_en.put("ph_vc", new LanguageFeatureProcessors.PH_VC(phoneSet));
        processors_en.put("ph_vfront", new LanguageFeatureProcessors.PH_VFront(phoneSet));
        processors_en.put("ph_vheight", new LanguageFeatureProcessors.PH_VHeight(phoneSet));
        processors_en.put("ph_vlng", new LanguageFeatureProcessors.PH_VLength(phoneSet));
        processors_en.put("ph_vrnd", new LanguageFeatureProcessors.PH_VRnd(phoneSet));

        processors_en.put("seg_coda_fric", 
                new LanguageFeatureProcessors.SegCodaFric(phoneSet));
        processors_en.put("seg_onset_fric", 
                new LanguageFeatureProcessors.SegOnsetFric(phoneSet));

        processors_en.put("seg_coda_stop", 
                new LanguageFeatureProcessors.SegCodaStop(phoneSet));
        processors_en.put("seg_onset_stop", 
                new LanguageFeatureProcessors.SegOnsetStop(phoneSet));

        processors_en.put("seg_coda_nasal", 
                new LanguageFeatureProcessors.SegCodaNasal(phoneSet));
        processors_en.put("seg_onset_nasal", 
                new LanguageFeatureProcessors.SegOnsetNasal(phoneSet));

        processors_en.put("seg_coda_glide", 
                new LanguageFeatureProcessors.SegCodaGlide(phoneSet));
        processors_en.put("seg_onset_glide", 
                new LanguageFeatureProcessors.SegOnsetGlide(phoneSet));

        processors_en.put("seg_onsetcoda", 
                new LanguageFeatureProcessors.SegOnsetCoda(phoneSet));
        processors_en.put("syl_codasize", 
                new LanguageFeatureProcessors.SylCodaSize(phoneSet));
        processors_en.put("syl_onsetsize", 
                new LanguageFeatureProcessors.SylOnsetSize(phoneSet));
        processors_en.put("accented", new GenericFeatureProcessors.Accented());
        
        processors_en.put("token_pos_guess", 
                new LanguageFeatureProcessors.TokenPosGuess());
    	
		}catch(Exception e){
		    e.printStackTrace();
		    throw new Error("Problem building Pos or PhoneSet");}
		}
    
    /**
     * Loads the PoS conversion file, if it is needed
     * @return the PoS conversion map
     */
    private Map loadPosConverter(){
	    try{
	        //property is set in english.shprot
	    String file = 
	        MaryProperties.getFilename("english.freetts.posConverterFile", "").trim();
	    if (!file.equals("")){
	        Map posConverter = new HashMap();
	        BufferedReader reader = 
	            new BufferedReader(new FileReader(new File (file)));
	        String line = reader.readLine();
	        while (line!=null){
	            if(!(line.startsWith("***"))){
	                
              	  StringTokenizer st = 
                    new StringTokenizer(line," ");
              	  String word = st.nextToken();
              	  String pos = st.nextToken();
              	  posConverter.put(word,pos);}
	            line = reader.readLine();
	        }
	        return posConverter;
	    }else{
	        //if file name is not given,
	        //the english tagger is not loaded
	        //and we do not need a conversion map
	        return new HashMap();}
	}catch(Exception e){
	    e.printStackTrace();
	    throw new Error("Error reading pos conversion map");
	}
	}
    
    /**
     * Gets the feature processor 
     * specified by name
     *@param name the name of the feature processor
     *@return the feature processor
     */
    public FeatureProcessor getFeatureProcessor(String name){
        if (processors_gen.containsKey(name)){
            return (FeatureProcessor)processors_gen.get(name);}
        else {
            return (FeatureProcessor)processors_en.get(name);}
    }
    
    }
