/**
 * Copyright 2010 DFKI GmbH.
 * All Rights Reserved.  Use is subject to license terms.
 *
 * This file is part of MARY TTS.
 *
 * MARY TTS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package marytts.modules;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Locale;

import marytts.cart.CART;
import marytts.cart.DirectedGraph;
import marytts.cart.StringPredictionTree;
import marytts.cart.LeafNode.LeafType;
import marytts.cart.io.DirectedGraphReader;
import marytts.cart.io.MaryCARTReader;
import marytts.cart.io.WagonCARTReader;
import marytts.datatypes.MaryData;
import marytts.datatypes.MaryDataType;
import marytts.datatypes.MaryXML;
import marytts.features.ByteValuedFeatureProcessor;
import marytts.features.FeatureDefinition;
import marytts.features.FeatureProcessorManager;
import marytts.features.FeatureRegistry;
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
 * Predict voice quality using a CART.
 *
 * @author Ingmar Steiner (based on CartDurationModeller by Marc Schr&ouml;der)
 */

public class CARTVQModeller extends InternalModule
{
    protected DirectedGraph cart = new CART();
    // TODO: use a simple regression tree, with FloatLeafNode, for pausetree:
    protected StringPredictionTree vqtree;
    protected TargetFeatureComputer featureComputer;
    protected TargetFeatureComputer vqFeatureComputer;
    private String propertyPrefix;
    private FeatureProcessorManager featureProcessorManager;

    /**
     * Constructor which can be directly called from init info in the config file.
     * This constructor will use the registered feature processor manager for the given locale.
     * @param locale a locale string, e.g. "en"
     * @param propertyPrefix the prefix to be used when looking up entries in the config files, e.g. "english.duration"
     * @throws Exception
     */
    public CARTVQModeller(String locale, String propertyPrefix)
    throws Exception {
        this(MaryUtils.string2locale(locale), propertyPrefix,
                FeatureRegistry.getFeatureProcessorManager(MaryUtils.string2locale(locale)));
    }
    
    /**
     * Constructor which can be directly called from init info in the config file.
     * Different languages can call this code with different settings.
     * @param locale a locale string, e.g. "en"
     * @param propertyPrefix the prefix to be used when looking up entries in the config files, e.g. "english.duration"
     * @param featprocClassInfo a package name for an instance of FeatureProcessorManager, e.g. "marytts.language.en.FeatureProcessorManager"
     * @throws Exception
     */
    public CARTVQModeller(String locale, String propertyPrefix, String featprocClassInfo)
    throws Exception
    {
        this(MaryUtils.string2locale(locale), propertyPrefix,
                (FeatureProcessorManager)MaryUtils.instantiateObject(featprocClassInfo));
    }
    
    /**
     * Constructor to be called with instantiated objects.
     * @param locale
     * @param propertyPrefix the prefix to be used when looking up entries in the config files, e.g. "english.duration"
     * @praam featureProcessorManager the manager to use when looking up feature processors.
     */
    protected CARTVQModeller(Locale locale,
               String propertyPrefix, FeatureProcessorManager featureProcessorManager)
    {
        super("CARTVQModeller",
                MaryDataType.VQ,
                MaryDataType.DURATIONS, locale);
        if (propertyPrefix.endsWith(".")) this.propertyPrefix = propertyPrefix;
        else this.propertyPrefix = propertyPrefix + ".";
        this.featureProcessorManager = featureProcessorManager;
    }

    public void startup() throws Exception
    {
        super.startup();
        String cartFilename = MaryProperties.getFilename(propertyPrefix+"cart");
        if (cartFilename != null) { // there is a default model for the language
            File cartFile = new File(cartFilename);
            cart = new DirectedGraphReader().load(cartFile.getAbsolutePath());
            featureComputer = FeatureRegistry.getTargetFeatureComputer(featureProcessorManager, cart.getFeatureDefinition().getFeatureNames());
        } else {
            cart = null;
        }
        
        String vqFilename = MaryProperties.getFilename(propertyPrefix+"vqtree");
        if (vqFilename != null) {
            File vqFile = new File(vqFilename);

            File vqFdFile = new File(MaryProperties.needFilename(propertyPrefix+"vqfeatures"));
            FeatureDefinition vqFeatureDefinition = new FeatureDefinition(new BufferedReader(new FileReader(vqFdFile)), false);
            vqFeatureComputer = FeatureRegistry.getTargetFeatureComputer(featureProcessorManager, vqFeatureDefinition.getFeatureNames());
            vqtree = new StringPredictionTree(new BufferedReader(new FileReader(vqFile)), vqFeatureDefinition);
        } else {
            this.vqtree = null;
        }
    }

    public MaryData process(MaryData d)
    throws Exception
    {
        Document doc = d.getDocument(); 
        NodeIterator sentenceIt = MaryDomUtils.createNodeIterator(doc, MaryXML.SENTENCE);
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

            DirectedGraph currentCart = cart;
            TargetFeatureComputer currentFeatureComputer = featureComputer;
            if (maryVoice != null) {
                DirectedGraph voiceCart = maryVoice.getVQGraph();
                if (voiceCart != null) {
                    currentCart  = voiceCart;
                    logger.debug("Using voice VQ graph");
                    FeatureDefinition voiceFeatDef = voiceCart.getFeatureDefinition();
                    currentFeatureComputer = FeatureRegistry.getTargetFeatureComputer(featureProcessorManager, voiceFeatDef.getFeatureNames());
                }
            }
            
            if (currentCart == null) {
                throw new NullPointerException("No cart for predicting VQ");
            }
            
            // cumulative duration from beginning of sentence, in seconds:
            float end = 0;

            TreeWalker tw = MaryDomUtils.createTreeWalker(sentence, MaryXML.PHONE, MaryXML.BOUNDARY);
            Element segmentOrBoundary;
            Element previous = null;
            while ((segmentOrBoundary = (Element)tw.nextNode()) != null) {
                String phone = UnitSelector.getPhoneSymbol(segmentOrBoundary);
                Target t = new Target(phone, segmentOrBoundary);
                t.setFeatureVector(currentFeatureComputer.computeFeatureVector(t));
                ByteValuedFeatureProcessor[] fnord = currentFeatureComputer.getByteValuedFeatureProcessors();
                float vq_OQG;
//                if (segmentOrBoundary.getTagName().equals(MaryXML.BOUNDARY)) { // a pause
//                    vq_OQG = enterPauseDuration(segmentOrBoundary, previous, vqtree, vqFeatureComputer);
//                } else {
                    float[] vq = (float[])currentCart.interpret(t);
                    assert vq != null : "Null VQ";
                    assert vq.length == 2 : "Unexpected VQ length: "+vq.length;
                    vq_OQG = vq[1];
                    float stddevInSeconds = vq[0];
//                }
//                end += vq_OQG;
//                int durInMillis = (int) (1000 * vq_OQG);
//                if (segmentOrBoundary.getTagName().equals(MaryXML.BOUNDARY)) {
//                    segmentOrBoundary.setAttribute("duration", String.valueOf(durInMillis));
//                } else { // phone
                    segmentOrBoundary.setAttribute("vq", String.valueOf(vq_OQG));
//                }
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
    private float enterPauseDuration(Element boundary, Element previous, 
            StringPredictionTree currentPauseTree, TargetFeatureComputer currentPauseFeatureComputer)
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
        
        if (currentPauseTree == null)
            return duration;
        
        assert currentPauseFeatureComputer != null;
        String phone = previous.getAttribute("p");
        Target t = new Target(phone, previous);
        t.setFeatureVector(currentPauseFeatureComputer.computeFeatureVector(t));
                
        String durationString = currentPauseTree.getMostProbableString(t);
        // strip off "ms"
        durationString = durationString.substring(0, durationString.length() - 2);
        try {
            duration = Float.parseFloat(durationString);
        } catch (NumberFormatException nfe) {}
        
        if (duration > 2) {
            logger.debug("Cutting long duration to 2 s -- was " + duration);
            duration = 2;
        }
        return duration;
    }


}

