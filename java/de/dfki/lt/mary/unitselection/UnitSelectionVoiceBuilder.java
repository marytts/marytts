/**
 * Copyright 2006 DFKI GmbH.
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

package de.dfki.lt.mary.unitselection;


import de.dfki.lt.mary.modules.synthesis.Voice;
import de.dfki.lt.mary.modules.synthesis.Voice.Gender;

import de.dfki.lt.mary.MaryProperties;
import de.dfki.lt.mary.modules.synthesis.WaveformSynthesizer;

import de.dfki.lt.mary.unitselection.clunits.ClusterUnitSelector;
import de.dfki.lt.mary.unitselection.clunits.ClusterUnitConcatenator;
import de.dfki.lt.mary.unitselection.featureprocessors.UnitSelectionFeatProcManager;
import de.dfki.lt.mary.util.MaryUtils;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.sun.speech.freetts.lexicon.Lexicon;

import java.lang.reflect.Constructor;
import java.util.*;

import javax.sound.sampled.AudioFormat;

/**
 * Builds the arctic voices
 * replaces ArcticVoiceDirectory, together 
 * with the .config files for the voices
 * 
 * @author Anna Hunecke
 *
 */

public class UnitSelectionVoiceBuilder{ 
	
    private Logger logger;

    private Map lexicons; // administrate lexicons by name
    private WaveformSynthesizer synth;
    private Map featureProcessors;
	
	public UnitSelectionVoiceBuilder(WaveformSynthesizer synth){
        this.synth = synth;  
       featureProcessors = new HashMap();
       lexicons = new HashMap();
       logger = Logger.getLogger("UnitSelectionVoiceBuilder");
	}
	
	/**
	 * Builds a voice by reading the voice properties 
	 * from the .config file of the voice
	 * 
	 * @param voice the name of the voice
	 * @return the voice
	 */
	public Voice buildVoice(String voice){
	    try{
	        String header ="voice."+voice;
	        logger.debug("Loading voice "+voice+"...");
	        //read in the parameters from the .config file
	        String gender = MaryProperties.getProperty(header+".gender");
	        Gender voiceGender =  new Gender(gender);
		
	        String locale = MaryProperties.getProperty(header+".locale");
	       
	        Locale voiceLocale = MaryUtils.string2locale(locale);
		
	        String domain = MaryProperties.getProperty(header+".domain");
	        String exampleTextFile = null;
	        if (!domain.equals("general")){
	            exampleTextFile = MaryProperties.getFilename(header+".exampleTextFile");
	        }
	        
            String lexiconClass = MaryProperties.getProperty(header+".lexiconClass");
            Lexicon lexicon = null;
            if (lexiconClass != null) {
                String lexiconName = MaryProperties.getProperty(header+".lexicon");
                if (lexicons.containsKey(lexiconClass+lexiconName)) {
                    lexicon = (Lexicon) lexicons.get(lexiconClass+lexiconName);
                } else { // need to create a new lexicon instance
                    if (lexiconName == null) {
                        lexicon = (Lexicon) Class.forName(lexiconClass).newInstance();
                    } else { // lexiconName is String argument to constructor 
                        Class lexCl = Class.forName(lexiconClass);
                        Constructor lexConstr = lexCl.getConstructor(new Class[] {String.class});
                        // will throw a NoSuchMethodError if constructor does not exist
                        lexicon = (Lexicon) lexConstr.newInstance(new Object[] {lexiconName});
                    }
                    lexicons.put(lexiconClass+lexiconName, lexicon);
                }
            }
            			
	        String featureProcessorsClass = 
	            MaryProperties.getFilename(header+".featureProcessorsClass");
	        UnitSelectionFeatProcManager featProcManager;
	        if (featureProcessors.containsKey(locale)){
	            featProcManager = 
	                (UnitSelectionFeatProcManager)featureProcessors.get(locale);}
	        else {
	            featProcManager = 
	                (UnitSelectionFeatProcManager) Class.forName(featureProcessorsClass).newInstance();
	            featureProcessors.put(locale,featProcManager);}
	            
	        String databaseFile = 
	            MaryProperties.getFilename(header+".databaseFile");
	        String databaseClass = 
	            MaryProperties.getProperty(header+".databaseClass");
	        UnitDatabase unitDatabase = 
	            (UnitDatabase) Class.forName(databaseClass).newInstance();
	        
	        unitDatabase.load(databaseFile, featProcManager, voice);
	        
	        //overwrite target cost weights if defined
	        String targetCostWeights = 
	            MaryProperties.getFilename(header+".targetCostWeights");
	        if (targetCostWeights != null){
	            unitDatabase.overwriteWeights(targetCostWeights);
	        }
	        
	        String targetCostClass = 
	            MaryProperties.getProperty(header+".targetCostClass");
	        TargetCostFunction targetFunction = 
		        (TargetCostFunction) Class.forName(targetCostClass).newInstance();
	        targetFunction.setFeatsAndWeights(unitDatabase.getFeats(),
	                						unitDatabase.getWeights(),
	                						featProcManager);
	        String joinCostClass = 
	            MaryProperties.getProperty(header+".joinCostClass");
	        JoinCostFunction joinFunction = 
		        (JoinCostFunction) Class.forName(joinCostClass).newInstance();
	        String selectorClass = 
	            MaryProperties.getProperty(header+".selectorClass");
	        UnitSelector unitSelector = null;
	        if (selectorClass.equals("de.dfki.lt.mary.unitselection.clunits.ClusterUnitSelector")){
	            unitSelector = 
	                new ClusterUnitSelector(targetFunction,joinFunction);}
	       
		
	        String[] nameArray = new String[1];
	        nameArray[0] = voice;
	        
	       
	        int samplingRate = 
	            Integer.parseInt(MaryProperties.getProperty(header+".samplingRate"));
	        AudioFormat dbAudioFormat = 
	            new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,
	                    samplingRate, // samples per second
	                    16, // bits per sample
	                    1, // mono
	                    2, // nr. of bytes per frame
	                    samplingRate, // nr. of frames per second
	                    true); // big-endian;
	        String concatenatorClass = 
	            MaryProperties.getFilename(header+".concatenatorClass");
	        UnitConcatenator unitConcatenator = null;
	        if (concatenatorClass.equals("de.dfki.lt.mary.unitselection.clunits.ClusterUnitConcatenator")){
	            unitConcatenator = 
	                new ClusterUnitConcatenator(unitDatabase, dbAudioFormat);}
	        
	        //dummy values for the other parameters
	        String path = null;
	        int topStart = -1;
	        int topEnd = -1;
	        int baseStart = -1;
	        int baseEnd = -1;
	        String[] knownVoiceQualities = null;
			
	        Voice v = 
	            new UnitSelectionVoice(unitDatabase, unitSelector, 
	                    unitConcatenator, path, 
                    nameArray, voiceLocale, dbAudioFormat, 
                    synth, voiceGender,
                    topStart, topEnd, baseStart, 
                    baseEnd, knownVoiceQualities,lexicon,domain,
                    exampleTextFile);
	        return v;
	    }catch(Exception e){
	        e.printStackTrace();
	        throw new Error("Could not build voice "+voice, e);}
	    
	}

}