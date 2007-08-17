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
package de.dfki.lt.mary.modules;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.sun.speech.freetts.Item;
import com.sun.speech.freetts.Relation;
import com.sun.speech.freetts.Utterance;
import com.sun.speech.freetts.UtteranceProcessor;

import de.dfki.lt.freetts.mbrola.ParametersToMbrolaConverter;
import de.dfki.lt.mary.MaryData;
import de.dfki.lt.mary.MaryDataType;
import de.dfki.lt.mary.MaryProperties;
import de.dfki.lt.mary.modules.InternalModule;
import de.dfki.lt.mary.modules.synthesis.FreeTTSVoices;
import de.dfki.lt.mary.modules.synthesis.Voice;
import de.dfki.lt.mary.unitselection.Target;
import de.dfki.lt.mary.unitselection.UnitSelectionVoice;
import de.dfki.lt.mary.unitselection.cart.CART;
import de.dfki.lt.mary.unitselection.cart.RegressionTree;
import de.dfki.lt.mary.unitselection.featureprocessors.FeatureDefinition;
import de.dfki.lt.mary.unitselection.featureprocessors.FeatureProcessorManager;
import de.dfki.lt.mary.unitselection.featureprocessors.TargetFeatureComputer;


/**
 * Predict phoneme durations using a CART.
 *
 * @author Marc Schr&ouml;der
 */

public class CARTDurationModeller extends InternalModule
{
    protected CART cart;
    protected TargetFeatureComputer featureComputer;
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
               String propertyPrefix, FeatureProcessorManager featureProcessorManager)
    {
        super(name, inputType, outputType);
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
        cart = new RegressionTree(new BufferedReader(new FileReader(cartFile)), featureDefinition);
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
                    durInSeconds = 0.4f; // TODO: distinguish types of boundaries?
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
        MaryData output = new MaryData(outputType());
        output.setUtterances(utterances);
        return output;
    }




}
