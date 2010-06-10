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
 * Predict phone basename-mean and unit-mean voice quality using CARTs.
 *
 * @author Ingmar Steiner (based on CARTDurationModeller by Marc Schr&ouml;der)
 * @deprecated
 */

public class CARTVQModeller extends InternalModule
{
    protected DirectedGraph basenameCart = new CART();
    protected DirectedGraph unitCart = new CART();
    protected TargetFeatureComputer featureComputer;
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
        String basenameCartFilename = MaryProperties.getFilename(propertyPrefix+"cart.basename");
        if (basenameCartFilename != null) { // there is a default model for the language
            File basenameCartFile = new File(basenameCartFilename);
            basenameCart = new DirectedGraphReader().load(basenameCartFile.getAbsolutePath());
            featureComputer = FeatureRegistry.getTargetFeatureComputer(featureProcessorManager, basenameCart.getFeatureDefinition().getFeatureNames());
        } else {
            basenameCart = null;
        }
        String unitCartFilename = MaryProperties.getFilename(propertyPrefix+"cart.unit");
        if (unitCartFilename != null) { // there is a default model for the language
            File unitCartFile = new File(unitCartFilename);
            unitCart = new DirectedGraphReader().load(unitCartFile.getAbsolutePath());
//            featureComputer = FeatureRegistry.getTargetFeatureComputer(featureProcessorManager, basenameCart.getFeatureDefinition().getFeatureNames());
        } else {
            unitCart = null;
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

            DirectedGraph currentBasenameCart = basenameCart;
            DirectedGraph currentUnitCart = unitCart;
            TargetFeatureComputer currentFeatureComputer = featureComputer;
            
            if (currentBasenameCart == null) {
                throw new NullPointerException("No cart for predicting basename-mean VQ");
            }
            if (currentUnitCart == null) {
                throw new NullPointerException("No cart for predicting unit-mean VQ");
            }
            
            TreeWalker tw = MaryDomUtils.createTreeWalker(sentence, MaryXML.PHONE, MaryXML.BOUNDARY);
            Element segmentOrBoundary;
            while ((segmentOrBoundary = (Element)tw.nextNode()) != null) {
                String phone = UnitSelector.getPhoneSymbol(segmentOrBoundary);
                Target t = new Target(phone, segmentOrBoundary);
                t.setFeatureVector(currentFeatureComputer.computeFeatureVector(t));

                float[] bnvq = (float[]) currentBasenameCart.interpret(t);
                assert bnvq != null : "Null basename VQ";
                assert bnvq.length == 2 : "Unexpected basename VQ length: "+bnvq.length;
                float basename_vq = bnvq[1];
                float bn_stddevInSeconds = bnvq[0];

                float[] unvq = (float[]) currentUnitCart.interpret(t);
                assert unvq != null : "Null unit VQ";
                assert unvq.length == 2 : "Unexpected unit VQ length: "+unvq.length;
                float unit_vq = unvq[1];
                float un_stddevInSeconds = unvq[0];

                segmentOrBoundary.setAttribute("basename_vq", String.valueOf(basename_vq));
                segmentOrBoundary.setAttribute("unit_vq", String.valueOf(unit_vq));
            }
        }
        MaryData output = new MaryData(outputType(), d.getLocale());
        output.setDocument(doc);
        return output;
    }

}

