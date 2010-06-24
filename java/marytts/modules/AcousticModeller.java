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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.traversal.NodeIterator;
import org.w3c.dom.traversal.TreeWalker;

import marytts.datatypes.MaryData;
import marytts.datatypes.MaryDataType;
import marytts.datatypes.MaryXML;

import marytts.features.FeatureProcessorManager;
import marytts.features.FeatureRegistry;

import marytts.modules.acoustic.CARTModel;
import marytts.modules.acoustic.Model;
import marytts.modules.acoustic.BoundaryModel;
import marytts.modules.acoustic.ModelType;
import marytts.modules.phonemiser.Allophone;
import marytts.modules.phonemiser.AllophoneSet;
import marytts.modules.synthesis.Voice;

import marytts.server.MaryProperties;

import marytts.unitselection.select.UnitSelector;

import marytts.util.MaryUtils;
import marytts.util.dom.MaryDomUtils;

/**
 * Predict duration and F0 using CARTs or other models
 * 
 * @author steiner
 * 
 */
public class AcousticModeller extends InternalModule {

    private String propertyPrefix;

    private FeatureProcessorManager featureProcessorManager;

    private Map<String, List<Element>> elementLists;

    private Map<String, Model> models;

    // three constructors adapted from DummyAllophones2AcoustParams (used if this is in modules.classes.list):

    public AcousticModeller() {
        this((Locale) null);
    }

    /**
     * Constructor to be called with instantiated objects.
     * 
     * @param locale
     */
    public AcousticModeller(String locale) {
        this(MaryUtils.string2locale(locale));
    }

    /**
     * Constructor to be called with instantiated objects.
     * 
     * @param locale
     */
    public AcousticModeller(Locale locale) {
        super("AcousticModeller", MaryDataType.ALLOPHONES, MaryDataType.ACOUSTPARAMS, locale);
    }

    // three constructors adapted from CARTF0Modeller (used if this is in a voice's preferredModules):

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

    /**
     * Wait until voice is loaded, then get its acousticModels to load all the Models into a Map and initialize them
     */
    public void delayedStartup(Voice voice) throws Exception {

        // get featureProcessorManager
        if (featureProcessorManager == null) {
            Locale locale = getLocale();
            if (locale == null) {
                locale = voice.getLocale();
            }
            featureProcessorManager = FeatureRegistry.getFeatureProcessorManager(locale);
        }

        // get Model Maps from voice, if they are defined:
        Map<String, Map<String, String>> modelMaps = voice.getAcousticModels();
        if (modelMaps == null) {
            return;
        }

        // initialize the Element List and Model Maps:
        elementLists = new HashMap<String, List<Element>>();
        models = new HashMap<String, Model>();

        // add boundary "model" (which could of course be overwritten by appropriate properties in voice config):
        models.put("boundary", new BoundaryModel("boundary", null, "duration", null, null));

        // iterate over the models defined in the voice:
        for (String modelName : modelMaps.keySet()) {
            Map<String, String> modelMap = modelMaps.get(modelName);

            // unpack Strings from Model Map:
            String modelType = modelMap.get(Model.TYPE);
            String modelDataFileName = modelMap.get(Model.DATA);
            String modelAttributeName = modelMap.get(Model.ATTRIBUTE);

            // the following are null if not defined; this is handled in the Model constructor:
            String modelAttributeFormat = modelMap.get(Model.ATTRIBUTE_FORMAT);
            String modelElementList = modelMap.get(Model.SCOPE);

            // consult the ModelType enum to find appropriate Model subclass...
            ModelType possibleModelTypes = ModelType.fromString(modelType);
            // if modelType is not in ModelType.values(), we don't know how to handle it:
            if (possibleModelTypes == null) {
                logger.warn("Cannot handle unknown model type: " + modelType);
                throw new Exception();
            }

            // ...and instantiate it in a switch statement:
            Model model = null;
            switch (possibleModelTypes) {
            case CART:
                model = new CARTModel(modelType, modelDataFileName, modelAttributeName, modelAttributeFormat, modelElementList, featureProcessorManager);
            }

            // if we got this far, model should not be null:
            assert model != null;

            // otherwise, load datafile and put the model in the Model Map:
            model.loadDataFile();
            models.put(modelName, model);
        }
    }

    public MaryData process(MaryData d) {
        Document doc = d.getDocument();
        MaryData output = new MaryData(outputType(), d.getLocale());

        // cascaded voice identification:
        Element voice = (Element) doc.getElementsByTagName(MaryXML.VOICE).item(0);
        Voice maryVoice = Voice.getVoice(voice);
        if (maryVoice == null) {
            maryVoice = d.getDefaultVoice();
        }
        if (maryVoice == null) {
            // Determine Locale in order to use default voice
            Locale locale = MaryUtils.string2locale(doc.getDocumentElement().getAttribute("xml:lang"));
            maryVoice = Voice.getDefaultVoice(locale);
        }

        try {
            if (models == null) {
                delayedStartup(maryVoice);
            }
        } catch (Exception e) {
            // unless voice provides suitable models, pass out unmodified MaryXML, just like DummyAllophones2AcoustParams:
            logger.warn("No acoustic models defined in " + maryVoice.getName() + "; could not process!");
            output.setDocument(doc);
            return output;
        }

        // parse the MaryXML Document to populate Lists of relevant Elements:
        parseDocument(doc);

        // apply critical Models to Elements:
        models.get("duration").applyTo(elementLists.get("segments"));
        models.get("leftF0").applyTo(elementLists.get("firstVoicedSegments"), elementLists.get("firstVowels"));
        models.get("midF0").applyTo(elementLists.get("firstVowels"));
        models.get("rightF0").applyTo(elementLists.get("lastVoicedSegments"), elementLists.get("firstVowels"));
        models.get("boundary").applyTo(elementLists.get("boundaries"));

        // hack duration attributes:
        hackSegmentDurations(elementLists.get("segments"));

        // apply other Models:
        for (String modelName : models.keySet()) {
            // ignore critical Models already applied above:
            if (!modelName.equals("duration") && !modelName.equals("leftF0") && !modelName.equals("midF0")
                    && !modelName.equals("rightF0") && !modelName.equals("boundary")) {
                Model model = models.get(modelName);
                model.applyTo(elementLists.get(model.getTargetElementListName()));
                // remember, the Model constructor will apply the model to "segments" if the targetElementListName is null
            }
        }

        output.setDocument(doc);
        return output;
    }

    /**
     * Hack duration attributes so that <code>d</code> attribute values are in milliseconds, and add <code>end</code> attributes
     * containing the cumulative end time.
     * 
     * @param elements
     *            a List of segment Elements
     */
    private void hackSegmentDurations(List<Element> elements) {
        float cumulEndInSeconds = 0;
        for (Element segment : elements) {
            float durationInSeconds = Float.parseFloat(segment.getAttribute("d"));
            cumulEndInSeconds += durationInSeconds;

            // cumulative end time in seconds:
            String endStr = Float.toString(cumulEndInSeconds);
            segment.setAttribute("end", endStr);

            // duration rounded to milliseconds:
            String durationInMilliseconds = String.format("%.0f", (durationInSeconds * 1000));
            segment.setAttribute("d", durationInMilliseconds);
        }
    }

    /**
     * Parse the Document to populate the Lists of Elements
     * 
     * @param doc
     */
    private void parseDocument(Document doc) {
        // initialize Element Lists:
        List<Element> segments = new ArrayList<Element>();
        List<Element> boundaries = new ArrayList<Element>();
        List<Element> firstVoicedSegments = new ArrayList<Element>();
        List<Element> firstVowels = new ArrayList<Element>();
        List<Element> lastVoicedSegments = new ArrayList<Element>();
        List<Element> voicedSegments = new ArrayList<Element>();

        // walk over all syllables in MaryXML document:
        TreeWalker treeWalker = MaryDomUtils.createTreeWalker(doc, MaryXML.SYLLABLE, MaryXML.BOUNDARY);
        Node node;
        while ((node = treeWalker.nextNode()) != null) {
            Element element = (Element) node;

            // handle boundaries
            if (node.getNodeName().equals(MaryXML.BOUNDARY)) {
                boundaries.add(element);
                continue;
            }

            // from this point on, we should be dealing only with syllables:
            assert node.getNodeName().equals(MaryXML.SYLLABLE);

            // get AllophoneSet for syllable
            AllophoneSet allophoneSet = null; // TODO should this be here, or rather outside the loop?
            try {
                allophoneSet = AllophoneSet.determineAllophoneSet(element);
            } catch (Exception e) {
                e.printStackTrace();
            }
            assert allophoneSet != null;

            // initialize some variables:
            Element segment;
            Element firstVoicedSegment = null;
            Element firstVowel = null;
            Element lastVoicedSegment = null;

            // iterate over "ph" children of syllable:
            for (segment = MaryDomUtils.getFirstElementByTagName(node, MaryXML.PHONE); segment != null; segment = MaryDomUtils
                    .getNextOfItsKindIn(segment, element)) {

                // in passing, append segment to segments List:
                segments.add(segment);

                // get "p" attribute...
                String phone = UnitSelector.getPhoneSymbol(segment);
                // ...and get the corresponding allophone, which knows about its phonological features:
                Allophone allophone = allophoneSet.getAllophone(phone);
                if (allophone.isVoiced()) { // all and only voiced segments are potential F0 anchors
                    voicedSegments.add(segment);
                    if (firstVoicedSegment == null) {
                        firstVoicedSegment = segment;
                    }
                    if (firstVowel == null && allophone.isVowel()) {
                        firstVowel = segment;
                    }
                    lastVoicedSegment = segment; // keep overwriting this; finally it's the last voiced segment
                }
            }

            try {
                // at this point, no TBU should be null:
                assert firstVoicedSegment != null;
                assert firstVowel != null;
                assert lastVoicedSegment != null;

                // we have what we need, append to Lists:
                firstVoicedSegments.add(firstVoicedSegment);
                firstVowels.add(firstVowel);
                lastVoicedSegments.add(lastVoicedSegment);
            } catch (AssertionError e) {
                logger.debug("WARNING: could not identify F0 anchors in malformed syllable: " + element.getAttribute("ph"));
                e.printStackTrace();
            }
        }

        // put the Element Lists into the Map:
        elementLists.put("segments", segments);
        elementLists.put("voicedSegments", voicedSegments);
        elementLists.put("firstVoicedSegments", firstVoicedSegments);
        elementLists.put("firstVowels", firstVowels);
        elementLists.put("lastVoicedSegments", lastVoicedSegments);
        elementLists.put("boundaries", boundaries);
    }

}
