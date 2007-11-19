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
package de.dfki.lt.mary.modules;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Level;

import com.sun.speech.freetts.Item;
import com.sun.speech.freetts.Relation;
import com.sun.speech.freetts.Utterance;

import de.dfki.lt.mary.MaryData;
import de.dfki.lt.mary.MaryDataType;
import de.dfki.lt.mary.MaryProperties;
import de.dfki.lt.mary.modules.synthesis.FreeTTSVoices;
import de.dfki.lt.mary.modules.synthesis.Voice;
import de.dfki.lt.mary.unitselection.Target;
import de.dfki.lt.mary.unitselection.featureprocessors.FeatureProcessorManager;
import de.dfki.lt.mary.unitselection.featureprocessors.FeatureVector;
import de.dfki.lt.mary.unitselection.featureprocessors.TargetFeatureComputer;


/**
 * Read a simple phoneme string and generate default acoustic parameters.
 *
 * @author Marc Schr&ouml;der
 */

public class TargetFeatureLister extends InternalModule
{

    public TargetFeatureLister(MaryDataType outputType)
    {
        super("TargetFeatureLister",
                MaryDataType.get("FREETTS_ACOUSTPARAMS"), 
                outputType);
    }


    public TargetFeatureLister()
    {
        this(MaryDataType.get("TARGETFEATURES"));
    }
    
    public MaryData process(MaryData d)
    throws Exception
    {
        Voice voice = d.getDefaultVoice();
        if (voice == null) {
            throw new NullPointerException("Need default voice to be set in input data, but it is null");
        }
        TargetFeatureComputer featureComputer = getTargetFeatureComputer(voice);
        assert featureComputer != null : "Voice provides null instead of a feature computer!";
        List uttList = d.getUtterances();
        String header = featureComputer.getAllFeatureProcessorNamesAndValues();
        StringBuffer text = new StringBuffer();
        StringBuffer bin = new StringBuffer();
        for (int i=0, len=uttList.size(); i<len; i++) {
            Utterance utt = (Utterance)uttList.get(i);
            if (logger.getEffectiveLevel().equals(Level.DEBUG)) {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                utt.dump(pw, 2, name(), true); // padding, justRelations
                logger.debug("Converting the following Utterance to target features:\n"+sw.toString());
            }
            // Create target chain for the utterance
            Relation segs = utt.getRelation(Relation.SEGMENT);
            List targets = overridableCreateTargetsWithPauses(segs);
            //List targets = createTargets(segs);
            // create target feature string for the target chain
            for (int j=0, nTargets = targets.size(); j<nTargets; j++) {
                Target target = (Target) targets.get(j);
                FeatureVector features = featureComputer.computeFeatureVector(target);
                text.append(featureComputer.toStringValues(features));
                text.append("\n");
                bin.append(features.toString());
                bin.append("\n");
            }
        }
        // Leave an empty line between sections:
        String out = header + "\n" + text + "\n" + bin;
        MaryData result = new MaryData(outputType());
        result.setPlainText(out);
        return result;
    }
    
    /**
     * Get the appropriate target feature computer for this target feature lister from voice
     * @param v
     * @return
     */
    protected TargetFeatureComputer getTargetFeatureComputer(Voice v)
    {
        return v.getTargetFeatureComputer();
    }

    /**
     * Create the list of targets from the Segments in the utterance.
     * @param segs the Segment relation
     * @return a list of Target objects
     */
    protected List<Target> createTargets(Relation segs) {
        List<Target> targets = new ArrayList<Target>();
        for (Item s = segs.getHead(); s != null; s = s.getNext()) {
            String segName = s.getFeatures().getString("name");;
            targets.add(new Target(segName, s));
        }
        return targets;
    }
    
    
    /**
     * Access the code from within the our own code so that a subclass
     * can override it. Use this rather than the public static method in local code.
     * @param segs
     * @return
     */
    protected List<Target> overridableCreateTargetsWithPauses(Relation segs)
    {
        return TargetFeatureLister.createTargetsWithPauses(segs);
    }
    
    /**
     * Create the list of targets from the Segments in the utterance.
     * Make sure that first item is a pause
     * @param segs the Segment relation
     * @return a list of Target objects
     */
    public static List<Target> createTargetsWithPauses(Relation segs) {
        List<Target> targets = new ArrayList<Target>();

        boolean first = true;
        Item s = segs.getHead();
        Voice v = FreeTTSVoices.getMaryVoice(s.getUtterance().getVoice());
        String silenceSymbol = v.sampa2voice("_");
        Item lastItem = s;
        Target lastTarget = null;
        for (; s != null; s = s.getNext()) {
            //create next target
            String segName = s.getFeatures().getString("name");
            Target nextTarget = new Target(segName, s);
            //if first target is not a pause, add one
            if (first){
                first = false;
                if (! nextTarget.isSilence()){
                   //System.out.println("Adding pause target "
                     //           +silenceSymbol);
                   //build new pause item
                   Item newPauseItem = s.prependItem(null);
                   newPauseItem.getFeatures().setString("name", silenceSymbol);
                   
                   //add new target for item
                   targets.add(new Target(silenceSymbol, newPauseItem)); 
                }
            }
            targets.add(nextTarget);
            lastItem=s;
            lastTarget=nextTarget;
        }        
        if (! lastTarget.isSilence()){
                   //System.out.println("Adding pause target "
                     //           +silenceSymbol);
                   //build new pause item
                   Item newPauseItem = lastItem.appendItem(null);
                   newPauseItem.getFeatures().setString("name", silenceSymbol);
                   
                   //add new target for item
                   targets.add(new Target(silenceSymbol, newPauseItem)); 
                }
        return targets;
    }
}
