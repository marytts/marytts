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


import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.apache.log4j.Logger;

import com.sun.speech.freetts.lexicon.Lexicon;

import de.dfki.lt.mary.MaryProperties;
import de.dfki.lt.mary.modules.phonemiser.PhonemeSet;
import de.dfki.lt.mary.modules.synthesis.Voice;
import de.dfki.lt.mary.modules.synthesis.WaveformSynthesizer;
import de.dfki.lt.mary.modules.synthesis.Voice.Gender;
import de.dfki.lt.mary.unitselection.cart.CART;
import de.dfki.lt.mary.unitselection.featureprocessors.FeatureProcessorManager;
import de.dfki.lt.mary.util.MaryUtils;



/**
 * Builds the arctic voices
 * replaces ArcticVoiceDirectory, together 
 * with the .config files for the voices
 * 
 * @author Anna Hunecke
 *
 */

public class UnitSelectionVoiceBuilder
{ 
	
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
	public Voice buildVoice(String voice)
    {
	    try{
	        String header ="voice."+voice;
	        logger.info("Loading voice "+voice+"...");
	        
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
	        
	        //build the lexicon of not already built
            String lexiconClass = MaryProperties.getProperty(header+".lexiconClass");
            Lexicon lexicon = null;
            if (lexiconClass != null) {
                String lexiconName = MaryProperties.getProperty(header+".lexicon");
                if (lexicons.containsKey(lexiconClass+lexiconName)) {
                    lexicon = (Lexicon) lexicons.get(lexiconClass+lexiconName);
                } else { // need to create a new lexicon instance
                    logger.debug("...loading lexicon...");
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
            
            // build the feature processors if not already built	
	        String featureProcessorsClass = MaryProperties.needProperty(header+".featureProcessorsClass");
	        FeatureProcessorManager featProcManager;
	        if (featureProcessors.containsKey(locale)) {
	            featProcManager = (FeatureProcessorManager) featureProcessors.get(locale);
            } else {
                logger.debug("...instantiating feature processors...");
	            featProcManager = (FeatureProcessorManager) Class.forName(featureProcessorsClass).newInstance();
	            featureProcessors.put(locale,featProcManager);
	        }
            
            // build and load targetCostFunction
            logger.debug("...loading target cost function...");
            String featureFileName = MaryProperties.needFilename(header+".featureFile");
            String targetWeightFile = MaryProperties.getFilename(header + ".targetCostWeights");
            String targetCostClass = MaryProperties.needProperty(header+".targetCostClass");
            TargetCostFunction targetFunction = (TargetCostFunction) Class.forName(targetCostClass).newInstance();
            targetFunction.load(featureFileName, targetWeightFile, featProcManager);
            
            // build joinCostFunction
            logger.debug("...loading join cost function...");
            String joinFileName = MaryProperties.needFilename(header+".joinCostFile");
            String joinWeightFile = MaryProperties.getFilename(header + ".joinCostWeights");
            String joinCostClass = MaryProperties.needProperty(header+".joinCostClass");
            String precomputedJoinCostFileName = MaryProperties.getFilename(header+".precomputedJoinCostFile");
            JoinCostFunction joinFunction = (JoinCostFunction) Class.forName(joinCostClass).newInstance();
            joinFunction.load(joinFileName, joinWeightFile, precomputedJoinCostFileName);
            
	        // Build the various file readers
            logger.debug("...loading units file...");
            String unitReaderClass = MaryProperties.needProperty(header+".unitReaderClass");
            String unitsFile = MaryProperties.needFilename(header+".unitsFile");
            UnitFileReader unitReader = (UnitFileReader) Class.forName(unitReaderClass).newInstance();
            unitReader.load(unitsFile);
            
            logger.debug("...loading cart file...");
            String cartReaderClass = MaryProperties.needProperty(header+".cartReaderClass");
            String cartFile = MaryProperties.getFilename(header+".cartFile");
            CART cart = (CART) Class.forName(cartReaderClass).newInstance();
            cart.load(cartFile, targetFunction.getFeatureDefinition(),null);
            
            logger.debug("...loading audio time line...");
            String timelineReaderClass = MaryProperties.needProperty(header+".audioTimelineReaderClass");
            String timelineFile = MaryProperties.needFilename(header+".audioTimelineFile");
            TimelineReader timelineReader = (TimelineReader) Class.forName(timelineReaderClass).newInstance();
            timelineReader.load(timelineFile);
                
            //get the backtrace information
            String backtraceString = MaryProperties
                    .getProperty(header+".cart.backtrace");
            int backtrace = 100; // default backtrace value
            if (backtraceString != null) {
                backtrace = Integer.parseInt(backtraceString.trim());
            }
            
            // optionally, get basename timeline
            String basenameTimelineFile = MaryProperties.getFilename(header+".basenameTimeline");
            TimelineReader basenameTimelineReader = null;
            if (basenameTimelineFile != null) {
                logger.debug("...loading basename time line...");
                basenameTimelineReader = new TimelineReader(basenameTimelineFile);
            }
            
            //build and load database
            logger.debug("...instantiating database...");
            String databaseClass = MaryProperties.needProperty(header+".databaseClass");
            UnitDatabase unitDatabase = (UnitDatabase) Class.forName(databaseClass).newInstance();
            unitDatabase.load(targetFunction, joinFunction, unitReader, cart, timelineReader, basenameTimelineReader, backtrace);
	        
	        //build Selector
            logger.debug("...instantiating unit selector...");
            String selectorClass = MaryProperties.needProperty(header+".selectorClass");
	        UnitSelector unitSelector = (UnitSelector) Class.forName(selectorClass).newInstance();
	        unitSelector.load(unitDatabase);
            
	        //samplingRate -> bin, audioformat -> concatenator
	        //build Concatenator
            logger.debug("...instantiating unit concatenator...");
	        String concatenatorClass = MaryProperties.needProperty(header+".concatenatorClass");
	        UnitConcatenator unitConcatenator = (UnitConcatenator) Class.forName(concatenatorClass).newInstance();
	        unitConcatenator.load(unitDatabase);
            
	        //standard values for some parameters
	        String[] nameArray = new String[1];
	        nameArray[0] = voice;
	        int topStart = MaryProperties.getInteger(header+".topline.start", -1);
	        int topEnd = MaryProperties.getInteger(header+".topline.end", -1);
	        int baseStart = MaryProperties.getInteger(header+".baseline.start", -1);
	        int baseEnd = MaryProperties.getInteger(header+".baseline.end", -1);
	        
	        //dummy values for the rest of the parameters
	        String[] knownVoiceQualities = null;
	        String path = null;
			
	        //build the voice
            logger.debug("...instantiating voice...");
	        Voice v = new UnitSelectionVoice(unitDatabase, unitSelector, 
	                    unitConcatenator, path, 
                    nameArray, voiceLocale, unitConcatenator.getAudioFormat(), 
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