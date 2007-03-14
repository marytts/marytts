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
package de.dfki.lt.mary.modules.en;

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
import de.dfki.lt.mary.unitselection.cart.CART;
import de.dfki.lt.mary.unitselection.cart.RegressionTree;
import de.dfki.lt.mary.unitselection.featureprocessors.FeatureDefinition;
import de.dfki.lt.mary.unitselection.featureprocessors.TargetFeatureComputer;
import de.dfki.lt.mary.unitselection.featureprocessors.en.FeatureProcessorManager;


/**
 * Predict phoneme durations using a CART.
 *
 * @author Marc Schr&ouml;der
 */

public class CARTDurationModeller extends InternalModule
{
    protected CART cart;
    protected TargetFeatureComputer featureComputer;
    
    public CARTDurationModeller()
    {
        super("CARTDurationModeller",
              MaryDataType.get("FREETTS_POSTPROCESSED_EN"),
              MaryDataType.get("FREETTS_MBROLISED_DURATIONS_EN")
              );
    }

    public void startup() throws Exception
    {
        super.startup();
        File fdFile = new File(MaryProperties.needFilename("english.duration.featuredefinition"));
        FeatureDefinition featureDefinition = new FeatureDefinition(new BufferedReader(new FileReader(fdFile)), true);
        File cartFile = new File(MaryProperties.needFilename("english.duration.cart"));
        cart = new RegressionTree(new BufferedReader(new FileReader(cartFile)), featureDefinition);
        featureComputer = new TargetFeatureComputer(new FeatureProcessorManager(), featureDefinition.getFeatureNames());
    }

    public MaryData process(MaryData d)
    throws Exception
    {
        List utterances = d.getUtterances();
        Iterator it = utterances.iterator();
        while (it.hasNext()) {
            Utterance utterance = (Utterance) it.next();
            Voice maryVoice = FreeTTSVoices.getMaryVoice(utterance.getVoice());
            Relation segs = utterance.getRelation(Relation.SEGMENT);
            float end = 0; // end time of segment, in seconds
/*            for (Item s = segs.getHead(); s != null; s = s.getNext()) {
                String segName = s.getFeatures().getString("name");
                String sampaSegmentString = maryVoice.voice2sampa(segName);
                s.getFeatures().setString("name", sampaSegmentString);
            }*/
            for (Item s = segs.getHead(); s != null; s = s.getNext()) {
                String segName = s.getFeatures().getString("name");
                Target t = new Target(segName, s);
                t.setFeatureVector(featureComputer.computeFeatureVector(t));
                float[] dur = (float[])cart.interpret(t, 0);
                assert dur != null : "Null duration";
                assert dur.length == 2 : "Unexpected duration length: "+dur.length;
                float durInSeconds = dur[1];
                float stddevInSeconds = dur[0];
                end += durInSeconds;
                int durInMillis = (int) (1000 * durInSeconds);
                s.getFeatures().setFloat("end", end);
                s.getFeatures().setInt("mbr_dur", durInMillis);
                System.out.println("Duration predicted: ["+segName+"] "+durInSeconds);
            }
        }
        MaryData output = new MaryData(outputType());
        output.setUtterances(utterances);
        return output;
    }




}
