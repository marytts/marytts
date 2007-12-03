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
import java.net.URL;
import java.util.*;
import java.io.*;

import org.apache.log4j.Logger;

import com.sun.speech.freetts.VoiceManager;
import com.sun.speech.freetts.en.us.CMULexicon;
import com.sun.speech.freetts.lexicon.Lexicon;
//import de.dfki.lt.mary.modules.en.*;

import de.dfki.lt.mary.MaryProperties;
import de.dfki.lt.mary.modules.phonemiser.PhonemeSet;
import de.dfki.lt.mary.modules.synthesis.Voice;
import de.dfki.lt.mary.modules.synthesis.WaveformSynthesizer;
import de.dfki.lt.mary.modules.synthesis.Voice.Gender;
import de.dfki.lt.mary.unitselection.cart.CART;
import de.dfki.lt.mary.unitselection.cart.RegressionTree;
import de.dfki.lt.mary.unitselection.featureprocessors.FeatureDefinition;
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
    private Map<String,FeatureProcessorManager> featureProcessors;
	
	public UnitSelectionVoiceBuilder(WaveformSynthesizer synth){
        this.synth = synth;  
       featureProcessors = new HashMap<String,FeatureProcessorManager>();
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
	        String gender = MaryProperties.needProperty(header+".gender");
	        Gender voiceGender =  new Gender(gender);
	        String locale = MaryProperties.needProperty(header+".locale");
	        Locale voiceLocale = MaryUtils.string2locale(locale);
	        String domain = MaryProperties.needProperty(header+".domain");
	        String exampleTextFile = null;
	        if (!domain.equals("general")){
	            exampleTextFile = MaryProperties.needFilename(header+".exampleTextFile");
	        }
	        
            
            // build the feature processors if not already built	
	        String featureProcessorsClass = MaryProperties.needProperty(header+".featureProcessorsClass");
	        FeatureProcessorManager featProcManager;
	        if (featureProcessors.containsKey(locale)) {
	            featProcManager = featureProcessors.get(locale);
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
            float wSignal = Float.parseFloat(MaryProperties.getProperty(header+".joincostfunction.wSignal", "1.0"));
            joinFunction.load(joinFileName, joinWeightFile, precomputedJoinCostFileName, wSignal);
            
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
	        float targetCostWeights = Float.parseFloat(MaryProperties.getProperty(header+".viterbi.wTargetCosts", "0.5"));
	        unitSelector.load(unitDatabase,targetCostWeights);
            
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
	        
			
            // see if there are any voice-specific duration and f0 models to load
            CART durationCart = null;
            FeatureDefinition durationCartFeatDef = null;
            String durationCartFile = MaryProperties.getFilename(header+".duration.cart");
            if (durationCartFile != null) {
                logger.debug("...loading duration tree...");
                File fdFile = new File(MaryProperties.needFilename(header+".duration.featuredefinition"));
                durationCartFeatDef = new FeatureDefinition(new BufferedReader(new FileReader(fdFile)), true);
                durationCart = new RegressionTree(new BufferedReader(new FileReader(durationCartFile)), durationCartFeatDef);
            }
            CART[] f0Carts = null;
            FeatureDefinition f0CartsFeatDef = null;
            String leftF0CartFile = MaryProperties.getFilename(header+".f0.cart.left");
            if (leftF0CartFile != null) {
                logger.debug("...loading f0 trees...");
                File fdFile = new File(MaryProperties.needFilename(header+".f0.featuredefinition"));
                f0CartsFeatDef = new FeatureDefinition(new BufferedReader(new FileReader(fdFile)), true);
                f0Carts = new CART[3];
                // left cart:
                f0Carts[0] = new RegressionTree(new BufferedReader(new FileReader(leftF0CartFile)), f0CartsFeatDef);
                String midF0CartFile = MaryProperties.needFilename(header+".f0.cart.mid");
                // mid cart:
                f0Carts[1] = new RegressionTree(new BufferedReader(new FileReader(midF0CartFile)), f0CartsFeatDef);
                String rightF0CartFile = MaryProperties.needFilename(header+".f0.cart.right");
                // right cart:
                f0Carts[2] = new RegressionTree(new BufferedReader(new FileReader(rightF0CartFile)), f0CartsFeatDef);
            }

	        //build the voice
            logger.debug("...instantiating voice...");
	        Voice v = new UnitSelectionVoice(unitDatabase, unitSelector, 
	                    unitConcatenator,
                    nameArray, voiceLocale, unitConcatenator.getAudioFormat(), 
                    synth, voiceGender,
                    topStart, topEnd, baseStart, baseEnd,
                    domain,
                    exampleTextFile, durationCart, f0Carts,
                    durationCartFeatDef, f0CartsFeatDef);
	        return v;
	    }catch(Exception e){
	        e.printStackTrace();
	        throw new Error("Could not build voice "+voice, e);}
	    
	}

}