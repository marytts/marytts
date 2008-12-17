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
import java.io.File;
import java.io.FileReader;
import java.util.Locale;

import marytts.cart.CART;
import marytts.cart.StringPredictionTree;
import marytts.cart.LeafNode.LeafType;
import marytts.cart.io.MaryCARTReader;
import marytts.cart.io.WagonCARTReader;
import marytts.datatypes.MaryData;
import marytts.datatypes.MaryDataType;
import marytts.datatypes.MaryXML;
import marytts.features.FeatureDefinition;
import marytts.features.FeatureProcessorManager;
import marytts.features.TargetFeatureComputer;
import marytts.modules.synthesis.Voice;
import marytts.server.MaryProperties;
import marytts.unitselection.UnitSelectionVoice;
import marytts.unitselection.select.Target;
import marytts.unitselection.select.UnitSelector;
import marytts.util.MaryUtils;
import marytts.util.dom.MaryDomUtils;
import marytts.util.dom.NameNodeFilter;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.traversal.DocumentTraversal;
import org.w3c.dom.traversal.NodeFilter;
import org.w3c.dom.traversal.NodeIterator;
import org.w3c.dom.traversal.TreeWalker;


/**
 * Predict phoneme durations using a CART.
 *
 * @author Marc Schr&ouml;der
 */

public class CARTDurationModeller extends InternalModule
{
    // old: protected CART cart;
    protected CART cart = new CART();
    // TODO: use a simple regression tree, with FloatLeafNode, for pausetree:
    protected StringPredictionTree pausetree;
    protected TargetFeatureComputer featureComputer;
    protected TargetFeatureComputer pauseFeatureComputer;
    private String propertyPrefix;
    private FeatureProcessorManager featureProcessorManager;

    /**
     * Constructor which can be directly called from init info in the config file.
     * Different languages can call this code with different settings.
     * @param locale a locale string, e.g. "en"
     * @param propertyPrefix the prefix to be used when looking up entries in the config files, e.g. "english.duration"
     * @param featprocClass a package name for an instance of FeatureProcessorManager, e.g. "marytts.language.en.FeatureProcessorManager"
     * @throws Exception
     */
    public CARTDurationModeller(String locale, String propertyPrefix, String featprocClass)
    throws Exception
    {
        this(MaryUtils.string2locale(locale), propertyPrefix,
                (FeatureProcessorManager)Class.forName(featprocClass).newInstance());
    }
    
    /**
     * Constructor to be called with instantiated objects.
     * @param locale
     * @param propertyPrefix the prefix to be used when looking up entries in the config files, e.g. "english.duration"
     * @praam featureProcessorManager the manager to use when looking up feature processors.
     */
    protected CARTDurationModeller(
            Locale locale,
               String propertyPrefix, FeatureProcessorManager featureProcessorManager)
    {
        super("CARTDurationModeller",
                MaryDataType.ALLOPHONES,
                MaryDataType.DURATIONS, locale);
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
        cart = (new MaryCARTReader()).load(cartFile.getAbsolutePath());
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
        Document doc = d.getDocument(); 
        NodeIterator sentenceIt = ((DocumentTraversal)doc).
            createNodeIterator(doc, NodeFilter.SHOW_ELEMENT,
                           new NameNodeFilter(MaryXML.SENTENCE), false);
        Element sentence = null;
        while ((sentence = (Element) sentenceIt.nextNode()) != null) {
            // Make sure we have the correct voice:
            Element voice = (Element) MaryDomUtils.getAncestor(sentence, MaryXML.VOICE);
            Voice maryVoice = Voice.getVoice(voice);
            if (maryVoice == null) {                
                maryVoice = d.getDefaultVoice();
            }
            if (maryVoice == null) {
                // Determine Locale in order to use default voice
                Locale locale = MaryUtils.string2locale(doc.getDocumentElement().getAttribute("xml:lang"));
                maryVoice = Voice.getDefaultVoice(locale);
            }
            assert maryVoice != null;

            CART currentCart = cart;
            TargetFeatureComputer currentFeatureComputer = featureComputer;
            // TODO: cleanup: shouldn't all voices have the option of including their own CART?
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
            
            // cumulative duration from beginning of sentence, in seconds:
            float end = 0;

            TreeWalker tw = ((DocumentTraversal)doc).createTreeWalker(sentence, 
                    NodeFilter.SHOW_ELEMENT, new NameNodeFilter(MaryXML.PHONE, MaryXML.BOUNDARY), false);
            Element segmentOrBoundary;
            Element previous = null;
            while ((segmentOrBoundary = (Element)tw.nextNode()) != null) {
                String phone = UnitSelector.getPhoneSymbol(segmentOrBoundary);
                Target t = new Target(phone, segmentOrBoundary);
                t.setFeatureVector(currentFeatureComputer.computeFeatureVector(t));
                float durInSeconds;
                if (segmentOrBoundary.getTagName().equals(MaryXML.BOUNDARY)) { // a pause
                    durInSeconds = enterPauseDuration(segmentOrBoundary, previous, maryVoice);
                } else {
                    float[] dur = (float[])currentCart.interpret(t, 0);
                    assert dur != null : "Null duration";
                    assert dur.length == 2 : "Unexpected duration length: "+dur.length;
                    durInSeconds = dur[1];
                    float stddevInSeconds = dur[0];
                }
                end += durInSeconds;
                int durInMillis = (int) (1000 * durInSeconds);
                if (segmentOrBoundary.getTagName().equals(MaryXML.BOUNDARY)) {
                    segmentOrBoundary.setAttribute("duration", String.valueOf(durInMillis));
                } else { // phone
                    segmentOrBoundary.setAttribute("d", String.valueOf(durInMillis));
                    segmentOrBoundary.setAttribute("end", String.valueOf(end));
                }
                previous = segmentOrBoundary;
            }
        }
        MaryData output = new MaryData(outputType(), d.getLocale());
        output.setDocument(doc);
        return output;
    }

    /**
     * 
     * This predicts and enters the pause duration for a pause segment.
     * 
     * @param s
     * @param maryVoice 
     * @return pause duration, in seconds
     */
    private float enterPauseDuration(Element boundary, Element previous, Voice maryVoice)
    {
        if (!boundary.getTagName().equals(MaryXML.BOUNDARY))
            throw new IllegalArgumentException("cannot call enterPauseDuration for non-pause element");
        
        // If there is already a duration, keep it:
        if (boundary.hasAttribute("duration")) {
            try {
                return Float.parseFloat(boundary.getAttribute("duration"))  * 0.001f;
            } catch (NumberFormatException nfe) {}
        }

        float duration = 0.4f; // default value

        if (previous == null || !previous.getTagName().equals(MaryXML.PHONE))
            return duration;
        
        if (null == this.pausetree)
            return duration;
        
        String phone = previous.getAttribute("p");
        Target t = new Target(phone, previous);
        t.setFeatureVector(this.pauseFeatureComputer.computeFeatureVector(t));
                
        String durationString = this.pausetree.getMostProbableString(t);
        // strip off "ms"
        durationString = durationString.substring(0, durationString.length() - 2);
        try {
            duration = Float.parseFloat(durationString);
        } catch (NumberFormatException nfe) {}
        
        if (duration > 2000) {
            logger.debug("Cutting long duration to 2000 ms -- was " + duration);
            duration = 2000;
        }
        return duration;
    }


}
