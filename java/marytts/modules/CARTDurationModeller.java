/**
 * Copyright 2007 DFKI GmbH.
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
package marytts.modules;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import weka.classifiers.trees.j48.StringPredictionTree;

import marytts.cart.CART;
// old: import marytts.cart.RegressionTree;
import marytts.cart.io.WagonCARTReader;
import marytts.datatypes.MaryData;
import marytts.datatypes.MaryDataType;
import marytts.features.FeatureDefinition;
import marytts.features.FeatureProcessorManager;
import marytts.features.TargetFeatureComputer;
import marytts.modules.InternalModule;
import marytts.modules.synthesis.FreeTTSVoices;
import marytts.modules.synthesis.Voice;
import marytts.server.MaryProperties;
import marytts.unitselection.UnitSelectionVoice;
import marytts.unitselection.select.Target;

import com.sun.speech.freetts.Item;
import com.sun.speech.freetts.Relation;
import com.sun.speech.freetts.Utterance;
import com.sun.speech.freetts.UtteranceProcessor;

import de.dfki.lt.freetts.mbrola.ParametersToMbrolaConverter;


/**
 * Predict phoneme durations using a CART.
 *
 * @author Marc Schr&ouml;der
 */

public class CARTDurationModeller extends InternalModule
{
    // old: protected CART cart;
    protected CART cart = new CART();
    protected StringPredictionTree pausetree;
    protected TargetFeatureComputer featureComputer;
    protected TargetFeatureComputer pauseFeatureComputer;
    private String propertyPrefix;
    private FeatureProcessorManager featureProcessorManager;

    /**
     * Constructor to be called from subclasses.
     * @param name the module's name (for logging)
     * @param inputType the input type
     * @param outputType the output type
     * @param propertyPrefix the prefix to be used when looking up entries in the config files, e.g. "english.duration"
     */
    protected CARTDurationModeller(String name, MaryDataType inputType, MaryDataType outputType,
            Locale locale,
               String propertyPrefix, FeatureProcessorManager featureProcessorManager)
    {
        super(name, inputType, outputType, locale);
        if (propertyPrefix.endsWith(".")) this.propertyPrefix = propertyPrefix;
        else this.propertyPrefix = propertyPrefix + ".";
        this.featureProcessorManager = featureProcessorManager;
    }

    public void startup() throws Exception
    {
        super.startup();
        File fdFile = new File(MaryProperties.needFilename(propertyPrefix+"featuredefinition"));
        FeatureDefinition featureDefinition = new FeatureDefinition(new BufferedReader(new FileReader(fdFile)), true);
        File cartFile = new File(MaryProperties.needFilename(propertyPrefix+"cart"));
        
        //cart = new RegressionTree(new BufferedReader(new FileReader(cartFile)), featureDefinition);
        //CART cart = new CART();
        WagonCARTReader wagonRegReader = new WagonCARTReader("RegressionTree");
        cart.setRootNode(wagonRegReader.load(new BufferedReader(new FileReader(cartFile)), featureDefinition));
        

        //String pausefileName = "/project/mary/pavoque/Voice_Building/DFKI_German_Poker/durations.tree";        
        
        if ( null != MaryProperties.getFilename(propertyPrefix+"pausetree")){
            String pausefileName = MaryProperties.needFilename(propertyPrefix+"pausetree");
            

            File pauseFdFile = new File(MaryProperties.needFilename(propertyPrefix+"pausefeatures"));

            
            
            FeatureDefinition pauseFeatureDefinition = new FeatureDefinition(new BufferedReader(new FileReader(pauseFdFile)), false);
            pauseFeatureComputer = new TargetFeatureComputer(featureProcessorManager, pauseFeatureDefinition.getFeatureNames());

            
            File pauseFile = new File(pausefileName);
            
            this.pausetree = new StringPredictionTree(new BufferedReader(new FileReader(pauseFile)), pauseFeatureDefinition );
            
            
        } else {
            
            this.pausetree = null;
        }
        
        featureComputer = new TargetFeatureComputer(featureProcessorManager, featureDefinition.getFeatureNames());
    }

    public MaryData process(MaryData d)
    throws Exception
    {
        List utterances = d.getUtterances();
        Iterator it = utterances.iterator();
        while (it.hasNext()) {
            Utterance utterance = (Utterance) it.next();
            Voice maryVoice = FreeTTSVoices.getMaryVoice(utterance.getVoice());
            CART currentCart = cart;
            TargetFeatureComputer currentFeatureComputer = featureComputer;
            if (maryVoice instanceof UnitSelectionVoice) {
                CART voiceCart = ((UnitSelectionVoice)maryVoice).getDurationTree();
                if (voiceCart != null) {
                    currentCart  = voiceCart;
                    logger.debug("Using voice cart");
                }
                FeatureDefinition voiceFeatDef = 
                    ((UnitSelectionVoice)maryVoice).getDurationCartFeatDef();
                if (voiceFeatDef != null){
                    currentFeatureComputer = 
                        new TargetFeatureComputer(featureProcessorManager, voiceFeatDef.getFeatureNames());
                    logger.debug("Using voice feature definition");
                }
            }
            Relation segs = utterance.getRelation(Relation.SEGMENT);
            // Simple boundary treatment: Insert pause segments for boundaries
            Relation words = utterance.getRelation(Relation.WORD);
            for (Item w = words.getHead(); w != null; w = w.getNext()) {
                Item ss = w.getItemAs(Relation.SYLLABLE_STRUCTURE);
                Item lastSyl = ss.getLastDaughter();
                if (lastSyl != null && lastSyl.getFeatures().isPresent("endtone")) {
                    Item lastSeg = lastSyl.getLastDaughter();
                    assert lastSeg != null;
                    lastSeg = lastSeg.getItemAs(Relation.SEGMENT);
                    assert lastSeg != null;
                    Item pauseSeg = lastSeg.appendItem(null);
                    pauseSeg.getFeatures().setString("name", maryVoice.sampa2voice("_"));
                }
            }
            float end = 0; // end time of segment, in seconds
            for (Item s = segs.getHead(); s != null; s = s.getNext()) {
                String segName = s.getFeatures().getString("name");
                assert segName != null;
                String sampaName = maryVoice.voice2sampa(segName);
                assert sampaName != null;
                Target t = new Target(sampaName, s);
                t.setFeatureVector(currentFeatureComputer.computeFeatureVector(t));
                float durInSeconds;
                if (sampaName.equals("_")) { // a pause
                    //durInSeconds = 0.4f; // TODO: distinguish types of boundaries?
                    durInSeconds = enterPauseDuration(s, maryVoice);
                } else {
                    float[] dur = (float[])currentCart.interpret(t, 0);
                    assert dur != null : "Null duration";
                    assert dur.length == 2 : "Unexpected duration length: "+dur.length;
                    durInSeconds = dur[1];
                    float stddevInSeconds = dur[0];
                }
                end += durInSeconds;
                int durInMillis = (int) (1000 * durInSeconds);
                s.getFeatures().setFloat("end", end);
                s.getFeatures().setInt("mbr_dur", durInMillis);
                //System.out.println("Duration predicted: ["+segName+"] "+durInSeconds);
            }
        }
        MaryData output = new MaryData(outputType(), d.getLocale());
        output.setUtterances(utterances);
        return output;
    }

    /**
     * 
     * This predicts and enters the pause duration for a pause segment.
     * 
     * @param s
     * @param maryVoice 
     * @return
     */
    private float enterPauseDuration(Item s, Voice maryVoice) {
        // check that it is called for pauses only
        String segNameCheck = s.getFeatures().getString("name");
        assert segNameCheck != null;
        String sampaNameCheck = maryVoice.voice2sampa(segNameCheck);
        assert sampaNameCheck != null;
        if (!sampaNameCheck.equals("_"))
            throw new IllegalArgumentException("cannot call enterPauseDuration for non-pause item");
        
        // if there is no pause duration tree, treat pauses as done before
        if (null == this.pausetree)
            return .4f;
        
        //float duration = 0f;
        String duration = null;
        
        
        Item prevSegment = s.getPrevious();


        if (null != prevSegment){
            Item tokenItem = getSegmentToken(prevSegment);
            
            if (null == tokenItem)
                throw new IllegalArgumentException("Pause segments predecessor does not belong to a token.");
            
            // use already set duration if there is one
            if (tokenItem.getFeatures().isPresent("followingBoundaryDuration")){
                duration = tokenItem.getFeatures().getString("followingBoundaryDuration");
            } else {
                
                
                String segName = prevSegment.getFeatures().getString("name");
                assert segName != null;
                String sampaName = maryVoice.voice2sampa(segName);
                assert sampaName != null;
                Target t = new Target(sampaName, prevSegment);
                t.setFeatureVector(this.pauseFeatureComputer.computeFeatureVector(t));
                
                String durationString = this.pausetree.getMostProbableString(t);
                
                // strip off "ms"
                duration = durationString.substring(0, durationString.length() - 2);
                tokenItem.getFeatures().setString("followingBoundaryDuration", duration);
                
            }     
            

        }
                
        if (null == duration)
            // nothing set in token, so behave as before
            return .4f;
        else{
            float result = Float.parseFloat(duration) / 1000f;
            // TODO: make this configurable
            // return not more than duration of .4
            return Math.min(result, .4f);            
        }

    }

    private Item getSegmentToken(Item segment) {

        // segment -> getItemAs(Rel.Sylsstruct) -> getParent.getParent - Word-Item -> getItemas(Rel.Token) -> getParent 

        Item sylItem = segment.getItemAs(Relation.SYLLABLE_STRUCTURE).getParent();
        Item wordItem = sylItem.getParent();
        Item tokenItem = wordItem.getItemAs(Relation.TOKEN).getParent();
        

        return tokenItem;
    }




}
