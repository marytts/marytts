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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IllegalFormatException;
import java.util.List;
import java.util.Locale;

import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.traversal.NodeIterator;
import org.w3c.dom.traversal.TreeWalker;
import org.xml.sax.SAXException;

import marytts.cart.CART;
import marytts.cart.DirectedGraph;
import marytts.cart.io.DirectedGraphReader;
import marytts.datatypes.MaryData;
import marytts.datatypes.MaryDataType;
import marytts.datatypes.MaryXML;
import marytts.features.FeatureDefinition;
import marytts.features.FeatureProcessorManager;
import marytts.features.FeatureRegistry;
import marytts.features.FeatureVector;
import marytts.features.TargetFeatureComputer;
import marytts.modules.phonemiser.Allophone;
import marytts.modules.phonemiser.AllophoneSet;
import marytts.modules.synthesis.Voice;
import marytts.server.MaryProperties;
import marytts.unitselection.UnitSelectionVoice;
import marytts.unitselection.select.Target;
import marytts.unitselection.select.UnitSelector;
import marytts.util.MaryUtils;
import marytts.util.dom.DomUtils;
import marytts.util.dom.MaryDomUtils;

/**
 * Predict duration and F0 using CARTs or other predictors
 * 
 * @author steiner
 * 
 */
public class AcousticModeller extends InternalModule {

    private String propertyPrefix;

    private FeatureProcessorManager featureProcessorManager;

    private HashMap<String, DirectedGraph> carts;

    /**
     * Constructor which can be directly called from init info in the config file. This constructor will use the registered
     * feature processor manager for the given locale.
     * 
     * @param locale
     *            a locale string, e.g. "en"
     * @param propertyPrefix
     *            the prefix to be used when looking up entries in the config files, e.g. "english.duration"
     * @throws Exception
     */
    public AcousticModeller(String locale, String propertyPrefix) throws Exception {
        this(MaryUtils.string2locale(locale), propertyPrefix, FeatureRegistry.getFeatureProcessorManager(MaryUtils
                .string2locale(locale)));
    }

    /**
     * Constructor which can be directly called from init info in the config file. Different languages can call this code with
     * different settings.
     * 
     * @param locale
     *            a locale string, e.g. "en"
     * @param propertyPrefix
     *            the prefix to be used when looking up entries in the config files, e.g. "english.f0"
     * @param featprocClass
     *            a package name for an instance of FeatureProcessorManager, e.g. "marytts.language.en.FeatureProcessorManager"
     * @throws Exception
     */
    public AcousticModeller(String locale, String propertyPrefix, String featprocClassInfo) throws Exception {
        this(MaryUtils.string2locale(locale), propertyPrefix, (FeatureProcessorManager) MaryUtils
                .instantiateObject(featprocClassInfo));
    }

    /**
     * Constructor to be called with instantiated objects.
     * 
     * @param locale
     * @param propertyPrefix
     *            the prefix to be used when looking up entries in the config files, e.g. "english.f0"
     * @param featureProcessorManager
     *            the manager to use when looking up feature processors.
     */
    protected AcousticModeller(Locale locale, String propertyPrefix, FeatureProcessorManager featureProcessorManager) {
        super("AcousticModeller", MaryDataType.ALLOPHONES, MaryDataType.ACOUSTPARAMS, locale);
        if (propertyPrefix.endsWith(".")) {
            this.propertyPrefix = propertyPrefix;
        } else {
            this.propertyPrefix = propertyPrefix + ".";
        }
        this.featureProcessorManager = featureProcessorManager;
    }

    public void startup() throws Exception {
        super.startup();

        // mapping container for arbitrary CARTs:
        carts = new HashMap<String, DirectedGraph>();

        // this list could be extended to load additional CARTs:
        String[] cartProperties = { "duration.cart", "f0.cart.left", "f0.cart.mid", "f0.cart.right" };
        // load and put CARTs into container:
        for (String cartProperty : cartProperties) {
            String cartFilename = MaryProperties.getFilename(propertyPrefix + cartProperty);
            File cartFile = new File(cartFilename);
            try {
                DirectedGraph cart = new DirectedGraphReader().load(cartFile.getAbsolutePath());
                carts.put(cartProperty, cart);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    public MaryData process(MaryData d) {
        Document doc = d.getDocument();

        // iterate over all sentences:
        NodeIterator sentenceIt = MaryDomUtils.createNodeIterator(doc, MaryXML.SENTENCE);
        Node sentenceNode = null;
        while ((sentenceNode = sentenceIt.nextNode()) != null) {

            /*
             * Figure out the maryVoice for this sentence:
             */
            Element voice = (Element) MaryDomUtils.getAncestor(sentenceNode, MaryXML.VOICE);
            Voice maryVoice = Voice.getVoice(voice);
            if (maryVoice == null) {
                maryVoice = d.getDefaultVoice();
            }
            if (maryVoice == null) {
                // Determine Locale in order to use default voice
                Locale locale = MaryUtils.string2locale(doc.getDocumentElement().getAttribute("xml:lang"));
                maryVoice = Voice.getDefaultVoice(locale);
            }

            /*
             * Get CARTs for this sentence:
             */
            DirectedGraph currentDurCart = carts.get("duration.cart");
            if (maryVoice != null) {
                DirectedGraph voiceDurCart = maryVoice.getDurationGraph();
                if (voiceDurCart != null) {
                    currentDurCart = voiceDurCart;
                    logger.debug("Using voice duration graph");
                }
            }
            if (currentDurCart == null) {
                throw new NullPointerException("No cart for predicting duration");
            }

            DirectedGraph currentF0LeftCart = carts.get("f0.cart.left");
            DirectedGraph currentF0MidCart = carts.get("f0.cart.mid");
            DirectedGraph currentF0RightCart = carts.get("f0.cart.right");
            if (maryVoice instanceof UnitSelectionVoice) {
                CART[] voiceTrees = ((UnitSelectionVoice) maryVoice).getF0Trees();
                if (voiceTrees != null) {
                    currentF0LeftCart = (DirectedGraph) voiceTrees[0];
                    currentF0MidCart = (DirectedGraph) voiceTrees[1];
                    currentF0RightCart = (DirectedGraph) voiceTrees[2];
                    logger.debug("Using voice carts");
                }
            }
            if (currentF0LeftCart == null || currentF0MidCart == null || currentF0RightCart == null) {
                throw new NullPointerException("Do not have f0 prediction tree");
            }

            /*
             * And now, the actual processing:
             */

            // wrap the sentence for convenient access to the relevant pieces:
            SentenceWrapper sentence = new SentenceWrapper(doc, sentenceNode);

            // wrap the CARTs for the current voice:
            CARTWrapper durCartWrapper = new CARTWrapper(currentDurCart);
            CARTWrapper f0LeftCartWrapper = new CARTWrapper(currentF0LeftCart);
            CARTWrapper f0MidCartWrapper = new CARTWrapper(currentF0MidCart);
            CARTWrapper f0RightCartWrapper = new CARTWrapper(currentF0RightCart);

            // enrich the MaryXML elements with the predicted values:
            durCartWrapper.enrich(sentence.segments, "d");
            // Note that through the use of %.0f in the format strings, F0 values are rounded, not floored:
            f0LeftCartWrapper.enrich(sentence.initialTBUs, "f0", "(0,%.0f)");
            f0MidCartWrapper.enrich(sentence.medialTBUs, "f0", "(50,%.0f)");
            f0RightCartWrapper.enrich(sentence.finalTBUs, "f0", "(100,%.0f)");

            // final hack for all duration attributes:
            hackDurations(sentence);
        }

        MaryData output = new MaryData(outputType(), d.getLocale());
        output.setDocument(doc);
        return output;
    }

    /**
     * Hack duration attributes so that <code>d</code> attribute values are in milliseconds, and add <code>end</code> attributes
     * containing the cumulative end time. Also adds <code>duration</code> attributes to boundaries.
     * 
     * @param sentence
     *            SentenceWrapper
     */
    private void hackDurations(SentenceWrapper sentence) {
        float cumulEndInSeconds = 0;
        for (Element segment : sentence.segments) {
            float durationInSeconds = Float.parseFloat(segment.getAttribute("d"));
            cumulEndInSeconds += durationInSeconds;

            // cumulative end time in seconds:
            String endStr = Float.toString(cumulEndInSeconds);
            segment.setAttribute("end", endStr);

            // duration rounded to milliseconds:
            String durationInMilliseconds = String.format("%.0f", (durationInSeconds * 1000));
            segment.setAttribute("d", durationInMilliseconds);
        }
        // add a flat 400 ms as the duration of every boundary. StringPredictionTree based boundary duration prediction with
        // pausetree and pausefeatures is ignored, because it doesn't seem to be used anymore.
        String durAttrName = "duration";
        for (Element boundary : sentence.boundaries) {
            if (!boundary.hasAttribute(durAttrName)) {
                boundary.setAttribute(durAttrName, "400");
            }
        }
    }

    /**
     * Wrapper class for a sentence, which does all the parsing and provides convenient access to the relevant Elements
     * <p>
     * Apart from the <code>segments</code> and <code>boundaries</code>, for all syllables, the relevant tone-bearing units
     * (TBUs), i.e. the first voiced segment, the first vowel, and the last voiced segment, are provided as
     * <code>initialTBUs</code>, <code>medialTBUs</code>, <code>finalTBUs</code>, respectively.
     * 
     * @author steiner
     * 
     */
    private class SentenceWrapper {
        protected List<Element> segments;

        protected List<Element> boundaries;

        protected List<Element> initialTBUs;

        protected List<Element> medialTBUs;

        protected List<Element> finalTBUs;

        /**
         * Wrapper for this sentence.
         * 
         * @param doc
         *            root MaryXML Document
         * @param sentenceNode
         *            root Node of the sentence to wrap, i.e. <code>&lt;s&gt;</code> tag
         */
        protected SentenceWrapper(Document doc, Node sentenceNode) {
            segments = new ArrayList<Element>();
            boundaries = new ArrayList<Element>();
            initialTBUs = new ArrayList<Element>();
            medialTBUs = new ArrayList<Element>();
            finalTBUs = new ArrayList<Element>();

            // parse the document, filling the above element lists
            parseDocument(doc, sentenceNode);
        }

        /**
         * Main processing method of a SentenceWrapper, which parses the sentence and fills the Lists with Elements.
         * 
         * @param doc
         *            root MaryXML Document, passed in from constructor
         * @param sentenceNode
         *            root Node of the sentence to wrap, passed in from constructor
         */
        private void parseDocument(Document doc, Node sentenceNode) {
            // walk over all syllables in MaryXML document:
            TreeWalker treeWalker = MaryDomUtils.createTreeWalker(doc, sentenceNode, MaryXML.SYLLABLE, MaryXML.BOUNDARY);
            AllophoneSet allophoneSet = null; // TODO should this be here, or rather inside the loop?
            Node syllableOrBoundaryNode;
            while ((syllableOrBoundaryNode = treeWalker.nextNode()) != null) {
                Element syllableOrBoundaryElement = (Element) syllableOrBoundaryNode;

                // handle boundaries
                if (syllableOrBoundaryNode.getNodeName().equals(MaryXML.BOUNDARY)) {
                    boundaries.add(syllableOrBoundaryElement);
                    continue;
                }

                // get AllophoneSet for syllable
                try {
                    allophoneSet = AllophoneSet.determineAllophoneSet(syllableOrBoundaryElement);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                assert allophoneSet != null;

                // initialize some variables:
                Element segmentElement;
                Element initialTBU = null;
                Element medialTBU = null;
                Element finalTBU = null;

                // iterate over "ph" children of syllable:
                for (segmentElement = MaryDomUtils.getFirstElementByTagName(syllableOrBoundaryNode, MaryXML.PHONE); segmentElement != null; segmentElement = MaryDomUtils
                        .getNextOfItsKindIn(segmentElement, syllableOrBoundaryElement)) {

                    // in passing, append segment to segments List:
                    segments.add(segmentElement);

                    // get "p" attribute...
                    String phone = UnitSelector.getPhoneSymbol(segmentElement);
                    // ...and get the corresponding allophone, which knows about its phonological features:
                    Allophone allophone = allophoneSet.getAllophone(phone);
                    if (allophone.isVoiced()) { // all and only voiced segments are TBUs
                        if (initialTBU == null) {
                            initialTBU = segmentElement; // first TBU we find is the initial one
                        }
                        if (medialTBU == null && allophone.isVowel()) {
                            medialTBU = segmentElement; // first vowel we find is medial TBU
                        }
                        finalTBU = segmentElement; // keep overwriting this; finally it's the last TBU
                    }
                }

                // at this point, no TBU should be null:
                assert initialTBU != null;
                assert medialTBU != null;
                assert finalTBU != null;

                // we have what we need, append to Lists:
                initialTBUs.add(initialTBU);
                medialTBUs.add(medialTBU);
                finalTBUs.add(finalTBU);
            }
        }

    }

    /**
     * Wrapper class to apply a CART to a list of MaryXML Elements
     * 
     * @author steiner
     * 
     */
    private class CARTWrapper {

        private DirectedGraph cart;

        private TargetFeatureComputer featureComputer;

        /**
         * 
         * @param cart
         *            to wrap
         */
        private CARTWrapper(DirectedGraph cart) {
            this.cart = cart;

            this.featureComputer = FeatureRegistry.getTargetFeatureComputer(featureProcessorManager, cart.getFeatureDefinition()
                    .getFeatureNames());
        }

        /**
         * @see #enrich(List, String, String)
         * @param elements
         *            List of Elements to be enriched
         * @param attrName
         *            name of attribute with which to enrich elements
         */
        private void enrich(List<Element> elements, String attrName) {
            enrich(elements, attrName, "%s");
        }

        /**
         * For each Element in the List <code>elements</code>, get the corresponding Target, use the CART to interpret it, and add
         * the result to the element as the value of an attribute named <code>attrName</code>, formatted according to the format
         * string <code>attrFormat</code>. If the element already has an attribute named <code>attrName</code>, append a space and
         * the formatted result to the old attribute value.
         * 
         * @param elements
         *            List of Elements to be enriched
         * @param attrName
         *            name of attribute with which to enrich elements
         * @param attrFormat
         *            format string to apply to attribute values, e.g. "%.2f" to round to two decimal places
         */
        private void enrich(List<Element> elements, String attrName, String attrFormat) {

            List<Target> targets = getTargets(elements);

            for (int e = 0, t = 0; e < elements.size() && t < targets.size(); e++, t++) {
                Target target = targets.get(t);
                float[] result = (float[]) cart.interpret(target);
                float value = result[1]; // assuming result is [stdev, val]

                String attrValue = null;
                try {
                    attrValue = String.format(attrFormat, value);
                } catch (IllegalFormatException ife) {
                    ife.printStackTrace();
                }

                Element element = elements.get(e);
                // if attr is already present, append:
                if (element.hasAttribute(attrName)) {
                    attrValue = element.getAttribute(attrName) + " " + attrValue;
                }
                element.setAttribute(attrName, attrValue);
            }
        }

        /**
         * For a list of <code>PHONE</code> elements, return a list of Targets, where each Target is constructed from the
         * corresponding element.
         * <p>
         * <i>Note: It would be more efficient to do this only once, when the elements are defined by
         * {@link AcousticModeller.SentenceWrapper#parseDocument(Document, Node)}. However, that would require that all CARTs use
         * the same feature definition! Is that really true?</i>
         * 
         * @param elements
         *            List of Elements
         * @return List of Targets
         */
        private List<Target> getTargets(List<Element> elements) {
            List<Target> targets = new ArrayList<Target>(elements.size());
            for (Element element : elements) {
                assert element.getTagName() == MaryXML.PHONE;
                String phone = UnitSelector.getPhoneSymbol(element);
                Target target = new Target(phone, element);
                FeatureVector targetFeatureVector = featureComputer.computeFeatureVector(target);
                target.setFeatureVector(targetFeatureVector); // this is critical!
                targets.add(target);
            }
            return targets;
        }
    }
}
