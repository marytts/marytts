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
package marytts.modules;

import java.util.ArrayList;
import java.util.List;

import marytts.datatypes.MaryData;
import marytts.datatypes.MaryDataType;
import marytts.datatypes.MaryXML;
import marytts.features.FeatureVector;
import marytts.features.TargetFeatureComputer;
import marytts.modules.synthesis.Voice;
import marytts.unitselection.select.Target;
import marytts.unitselection.select.UnitSelector;
import marytts.util.dom.MaryDomUtils;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.traversal.TreeWalker;



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
                MaryDataType.ACOUSTPARAMS, 
                outputType,
                null);
    }


    public TargetFeatureLister()
    {
        this(MaryDataType.TARGETFEATURES);
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
        Document doc = d.getDocument();
        // First, get the list of segments and boundaries in the current document
        TreeWalker tw = MaryDomUtils.createTreeWalker(doc, doc, MaryXML.PHONE, MaryXML.BOUNDARY);
        List<Element> segmentsAndBoundaries = new ArrayList<Element>();
        Element e;
        while ((e = (Element) tw.nextNode()) != null) {
            segmentsAndBoundaries.add(e);
        }
        // Second, construct targets
        String out = listTargetFeatures(featureComputer, segmentsAndBoundaries);
        MaryData result = new MaryData(outputType(), d.getLocale());
        result.setPlainText(out);
        return result;
    }


    /**
     * For the given elements and using the given feature computer, create a string representation of the 
     * target features.
     * @param featureComputer
     * @param segmentsAndBoundaries
     * @return a multi-line string.
     */
    public String listTargetFeatures(TargetFeatureComputer featureComputer, List<Element> segmentsAndBoundaries) 
    {
        List<Target> targets = overridableCreateTargetsWithPauses(segmentsAndBoundaries);
        // Third, compute the feature vectors and convert them to text
        String header = featureComputer.getAllFeatureProcessorNamesAndValues();
        StringBuilder text = new StringBuilder();
        StringBuilder bin = new StringBuilder();
        for (Target target : targets) {
            FeatureVector features = featureComputer.computeFeatureVector(target);
            text.append(featureComputer.toStringValues(features)).append("\n");
            bin.append(features.toString()).append("\n");
        }
        
        // Leave an empty line between sections:
        String out = header + "\n" + text + "\n" + bin;
        return out;
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
     * Access the code from within the our own code so that a subclass
     * can override it. Use this rather than the public static method in local code.
     * @param segs
     * @return
     */
    protected List<Target> overridableCreateTargetsWithPauses(List<Element> segmentsAndBoundaries)
    {
        return TargetFeatureLister.createTargetsWithPauses(segmentsAndBoundaries);
    }
    
    /**
     * Create the list of targets from the segments to be synthesized
     * Prepend and append pauses if necessary
     * @param segmentsAndBoundaries a list of MaryXML phone and boundary elements
     * @return a list of Target objects
     */
    public static List<Target> createTargetsWithPauses(List<Element> segmentsAndBoundaries) {
        List<Target> targets = new ArrayList<Target>();
        if (segmentsAndBoundaries.size() == 0) return targets;
        // TODO: how can we know the silence symbol here?
        String silenceSymbol = "_";
        Element first = segmentsAndBoundaries.get(0);
        if (!first.getTagName().equals(MaryXML.BOUNDARY)) {
            // need to insert a dummy silence target
            targets.add(new Target(silenceSymbol, null));
        }
        for (Element sOrB : segmentsAndBoundaries) {
            String phone = UnitSelector.getPhoneSymbol(sOrB);
            targets.add(new Target(phone, sOrB));
        }
        if (!targets.get(targets.size()-1).isSilence()) {
            targets.add(new Target(silenceSymbol, null)); 
        }
        return targets;
    }
}
