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
import de.dfki.lt.mary.modules.phonemiser.Phoneme;
import de.dfki.lt.mary.modules.synthesis.FreeTTSVoices;
import de.dfki.lt.mary.modules.synthesis.Voice;
import de.dfki.lt.mary.unitselection.Target;
import de.dfki.lt.mary.unitselection.UnitSelectionVoice;
import de.dfki.lt.mary.unitselection.cart.CART;
import de.dfki.lt.mary.unitselection.cart.RegressionTree;
import de.dfki.lt.mary.unitselection.featureprocessors.FeatureDefinition;
import de.dfki.lt.mary.unitselection.featureprocessors.MaryFeatureProcessor;
import de.dfki.lt.mary.unitselection.featureprocessors.TargetFeatureComputer;
import de.dfki.lt.mary.unitselection.featureprocessors.FeatureProcessorManager;


/**
 * Predict phoneme durations using a CART.
 *
 * @author Marc Schr&ouml;der
 */

public class CARTF0Modeller extends InternalModule
{
    protected CART leftCart;
    protected CART midCart;
    protected CART rightCart;
    protected TargetFeatureComputer featureComputer;
    private String propertyPrefix;
    private FeatureProcessorManager featureProcessorManager;
    
    /**
     * Constructor to be called from subclasses.
     * @param name the module's name (for logging)
     * @param inputType the input type
     * @param outputType the output type
     * @param propertyPrefix the prefix to be used when looking up entries in the config files, e.g. "english.f0"
     */
    protected CARTF0Modeller(String name, MaryDataType inputType, MaryDataType outputType,
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
        File leftCartFile = new File(MaryProperties.needFilename(propertyPrefix+"cart.left"));
        leftCart = new RegressionTree(new BufferedReader(new FileReader(leftCartFile)), featureDefinition);
        File midCartFile = new File(MaryProperties.needFilename(propertyPrefix+"cart.mid"));
        midCart = new RegressionTree(new BufferedReader(new FileReader(midCartFile)), featureDefinition);
        File rightCartFile = new File(MaryProperties.needFilename(propertyPrefix+"cart.right"));
        rightCart = new RegressionTree(new BufferedReader(new FileReader(rightCartFile)), featureDefinition);
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
            CART currentLeftCart  = leftCart;
            CART currentMidCart   = midCart;
            CART currentRightCart = rightCart;
            TargetFeatureComputer currentFeatureComputer = featureComputer;
            if (maryVoice instanceof UnitSelectionVoice) {
                CART[] voiceTrees = ((UnitSelectionVoice)maryVoice).getF0Trees();
                if (voiceTrees != null) {
                    currentLeftCart  = voiceTrees[0];
                    currentMidCart   = voiceTrees[1];
                    currentRightCart = voiceTrees[2];
                    logger.debug("Using voice carts");
                }
                FeatureDefinition voiceFeatDef = 
                    ((UnitSelectionVoice)maryVoice).getDurationCartFeatDef();
                if (voiceFeatDef != null){
                    currentFeatureComputer = 
                        new TargetFeatureComputer(featureProcessorManager, voiceFeatDef.getFeatureNames());
                    logger.debug("Using voice feature definition");
                }
            }
            Relation targets = utterance.createRelation(Relation.TARGET);
            Relation syls = utterance.getRelation(Relation.SYLLABLE);
            for (Item syl = syls.getHead(); syl != null; syl = syl.getNext()) {
                Item sylStruct = syl.getItemAs(Relation.SYLLABLE_STRUCTURE);
                Item firstVoiced = null;
                Item vowel = null;
                Item lastVoiced = null;
                for (Item s = sylStruct.getDaughter(); s != null; s = s.getNext()) {
                    String sampaString = maryVoice.voice2sampa(s.toString());
                    assert sampaString != null;
                    Phoneme sampaPhoneme = maryVoice.getSampaPhoneme(sampaString);
                    assert sampaPhoneme != null : "Unknown phoneme: ["+sampaString+"]";
                    if (sampaPhoneme.isVowel()) {
                        // found a vowel
                        if (firstVoiced == null) firstVoiced = s;
                        if (vowel == null) vowel = s;
                        lastVoiced = s; // last so far, at least
                    } else if (sampaPhoneme.isVoiced()) {
                        // voiced consonant
                        if (firstVoiced == null) firstVoiced = s;
                        lastVoiced = s;
                    }
                }
                // only predict F0 values if we have a vowel:
                if (vowel != null) {
                    assert firstVoiced != null : "First voiced should not be null";
                    assert lastVoiced != null : "Last voiced should not be null";
                    // Get the time information for the f0 targets:
                    float leftTime;
                    Item prev = firstVoiced.getItemAs(Relation.SEGMENT).getPrevious();
                    if (prev == null) leftTime = 0;
                    else leftTime = prev.getFeatures().getFloat("end");
                    float midTime;
                    prev = vowel.getItemAs(Relation.SEGMENT).getPrevious();
                    float vowelStart;
                    if (prev == null) vowelStart = 0;
                    else vowelStart = prev.getFeatures().getFloat("end");
                    float vowelEnd = vowel.getFeatures().getFloat("end");
                    midTime = (vowelStart + vowelEnd) / 2;
                    float rightTime = lastVoiced.getFeatures().getFloat("end");
                    // Now predict the f0 values using the CARTs:ssh 
                    String segName = vowel.getFeatures().getString("name");
                    assert segName != null;
                    String sampaName = maryVoice.voice2sampa(segName);
                    assert sampaName != null;
                    Target t = new Target(segName, vowel);
                    t.setFeatureVector(currentFeatureComputer.computeFeatureVector(t));
                    float[] left = (float[])currentLeftCart.interpret(t, 0);
                    assert left != null : "Null frequency";
                    assert left.length == 2 : "Unexpected frequency length: "+left.length;
                    float leftF0InHz = left[1];
                    float leftStddevInHz = left[0];
                    float[] mid = (float[])currentMidCart.interpret(t, 0);
                    assert mid != null : "Null frequency";
                    assert mid.length == 2 : "Unexpected frequency length: "+mid.length;
                    float midF0InHz = mid[1];
                    float midStddevInHz = mid[0];
                    float[] right = (float[])currentRightCart.interpret(t, 0);
                    assert right != null : "Null frequency";
                    assert right.length == 2 : "Unexpected frequency length: "+right.length;
                    float rightF0InHz = right[1];
                    float rightStddevInHz = right[0];
                    // Now set targets:
                    Item targetItem = targets.appendItem();
                    targetItem.getFeatures().setFloat("pos", leftTime);
                    targetItem.getFeatures().setFloat("f0", leftF0InHz);
                    targetItem = targets.appendItem();
                    targetItem.getFeatures().setFloat("pos", midTime);
                    targetItem.getFeatures().setFloat("f0", midF0InHz);
                    targetItem = targets.appendItem();
                    targetItem.getFeatures().setFloat("pos", rightTime);
                    targetItem.getFeatures().setFloat("f0", rightF0InHz);
                    // and in MBROLA format:
                    firstVoiced.getFeatures().setString("mbr_targets", "(0,"+((int)leftF0InHz)+")");
                    String mbrTargets = "(50,"+((int)midF0InHz)+")";
                    if (vowel.getFeatures().isPresent("mbr_targets")) { // e.g., because firstVoiced == vowel
                        mbrTargets = vowel.getFeatures().getString("mbr_targets")+mbrTargets;
                    }
                    vowel.getFeatures().setString("mbr_targets", mbrTargets);
                    mbrTargets = "(100,"+((int)rightF0InHz)+")";
                    if (lastVoiced.getFeatures().isPresent("mbr_targets")) { // e.g., because vowel == lastVoiced
                        mbrTargets = lastVoiced.getFeatures().getString("mbr_targets")+mbrTargets;
                    }
                    lastVoiced.getFeatures().setString("mbr_targets", mbrTargets);
                }
            }
        }
        MaryData output = new MaryData(outputType());
        output.setUtterances(utterances);
        return output;
    }




}
