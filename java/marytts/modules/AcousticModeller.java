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
import java.util.StringTokenizer;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.traversal.NodeIterator;
import org.w3c.dom.traversal.TreeWalker;

import marytts.cart.DirectedGraph;
import marytts.cart.io.DirectedGraphReader;
import marytts.datatypes.MaryData;
import marytts.datatypes.MaryDataType;
import marytts.datatypes.MaryXML;
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

    private HashMap<String, Model> models;

    private List<Element> segments;

    private List<Element> boundaries;

    private List<Element> firstVoicedSegments;

    private List<Element> firstVowels;

    private List<Element> lastVoicedSegments;

    private List<Element> voicedSegments;

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
     * critically, this load all the Models into a Map and initializes them
     */
    public void startup() throws Exception {
        super.startup();

        // initialize the Model Map:
        models = new HashMap<String, Model>();

        // add boundary "model" (which could of course be overwritten by appropriate properties in voice config):
        models.put("boundary", new BoundaryModel("boundary", null, "duration", null));

        // iterate over the models defined in the voice config:
        String modelsString = MaryProperties.needProperty(propertyPrefix + "acoustic-models.segmental");
        StringTokenizer modelStrings = new StringTokenizer(modelsString);
        do {
            String modelName = modelStrings.nextToken();

            // get more properties from voice config, depending on the model name:
            String modelType = MaryProperties.needProperty(propertyPrefix + modelName + ".model");
            String modelDataFileName = MaryProperties.needFilename(propertyPrefix + modelName + ".data");
            String modelAttributeName = MaryProperties.needProperty(propertyPrefix + modelName + ".attribute");
            // modelAttributeFormat is null if not defined; this is handled in the Model constructor:
            String modelAttributeFormat = MaryProperties.getProperty(propertyPrefix + modelName + ".attribute.format");

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
                model = new CARTModel(modelType, modelDataFileName, modelAttributeName, modelAttributeFormat);
            }

            // if we got this far, model should not be null:
            assert model != null;

            // otherwise, load datafile and put the model in the Model Map:
            model.loadDataFile();
            models.put(modelName, model);
        } while (modelStrings.hasMoreTokens());
    }

    public MaryData process(MaryData d) {
        // parse the MaryXML Document to populate Lists of relevant Elements:
        Document doc = d.getDocument();
        parseDocument(doc);

        // apply critical Models to Elements:
        models.get("duration").applyTo(segments);
        models.get("leftF0").applyTo(firstVoicedSegments, firstVowels);
        models.get("midF0").applyTo(firstVowels);
        models.get("rightF0").applyTo(lastVoicedSegments, firstVowels);
        models.get("boundary").applyTo(boundaries);

        // hack duration attributes:
        hackSegmentDurations(segments);

        // TODO apply other Models, such as Voice Quality CARTs, but we need some elegant way of knowing which Elements to apply
        // the Models to, from properties in the voice config...
        try{
            models.get("oqg").applyTo(voicedSegments);
        } catch (NullPointerException e){
            // there is not Model for OQG... ignore
        }

        MaryData output = new MaryData(outputType(), d.getLocale());
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
        segments = new ArrayList<Element>();
        voicedSegments = new ArrayList<Element>();
        firstVoicedSegments = new ArrayList<Element>();
        firstVowels = new ArrayList<Element>();
        lastVoicedSegments = new ArrayList<Element>();
        boundaries = new ArrayList<Element>();

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
    }

    /**
     * list of known model types as constants; can be extended but needs to mesh with Model subclasses and switch statement in
     * startUp():
     * 
     * @author steiner
     * 
     */
    public enum ModelType {
        // enumerate model types here:
        CART;

        // get the appropriate model type from a string (which can be lower or mixed case):
        // adapted from http://www.xefer.com/2006/12/switchonstring
        public static ModelType fromString(String string) {
            try {
                ModelType modelString = valueOf(string.toUpperCase());
                return modelString;
            } catch (Exception e) {
                return null;
            }
        }
    }

    /**
     * Base class for acoustic modelling; specific Models should extend this and override methods as needed.
     * 
     * @author steiner
     * 
     */
    private class Model {
        protected String type;

        protected String dataFile;

        protected String targetAttributeName;

        protected String targetAttributeFormat;

        /**
         * Model constructor
         * 
         * @param type
         *            type of Model
         * @param dataFileName
         *            data file for this Model
         * @param targetAttributeName
         *            attribute in MaryXML to predict
         * @param targetAttributeFormat
         *            printf-style format String to specify the attribute value, i.e. "%.3f" to round to 3 decimal places
         */
        protected Model(String type, String dataFileName, String targetAttributeName, String targetAttributeFormat) {
            this.type = type;
            this.dataFile = dataFileName;
            this.targetAttributeName = targetAttributeName;
            if (targetAttributeFormat == null) {
                targetAttributeFormat = "%s";
            }
            this.targetAttributeFormat = targetAttributeFormat;
        }

        protected void loadDataFile() {
            return; // only subclasses know how to load their respective datafiles
        }

        /**
         * Apply this Model to a List of Elements, predicting from those same Elements
         * 
         * @param elements
         */
        protected void applyTo(List<Element> elements) {
            applyTo(elements, elements);
        }

        /**
         * Apply this Model to a List of Elements, predicting from a different List of Elements
         * 
         * @param applicableElements
         *            Elements to which to apply the values predicted by this Model
         * @param predictorElements
         *            Elements from which to predict the values
         */
        protected void applyTo(List<Element> applicableElements, List<Element> predictorElements) {
            assert applicableElements.size() == predictorElements.size();

            List<Target> predictorTargets = getTargets(predictorElements);

            for (int i = 0; i < applicableElements.size(); i++) {
                Target target = predictorTargets.get(i);
                float targetValue = (float) evaluate(target);

                Element element = applicableElements.get(i);

                // "evaulate" pseudo XPath syntax:
                // TODO this need to be extended to take into account targetAttributeNames like "foo/@bar", which would add the
                // bar attribute to the foo child of this element, creating it if not already present...
                if (targetAttributeName.startsWith("@")) {
                    targetAttributeName = targetAttributeName.replaceFirst("@", "");
                }

                // format targetValue according to targetAttributeFormat
                String formattedTargetValue = String.format(targetAttributeFormat, targetValue);
                // if the attribute already exists for this element, append targetValue:
                if (element.hasAttribute(targetAttributeName)) {
                    formattedTargetValue = element.getAttribute(targetAttributeName) + " " + formattedTargetValue;
                }

                // set the new attribute value:
                element.setAttribute(targetAttributeName, formattedTargetValue);
            }
        }

        /**
         * For a list of <code>PHONE</code> elements, return a list of Targets, where each Target is constructed from the
         * corresponding Element.
         * 
         * @param elements
         *            List of Elements
         * @return List of Targets
         */
        protected List<Target> getTargets(List<Element> elements) {
            List<Target> targets = new ArrayList<Target>(elements.size());
            for (Element element : elements) {
                assert element.getTagName() == MaryXML.PHONE;
                String phone = UnitSelector.getPhoneSymbol(element);
                Target target = new Target(phone, element);
                targets.add(target);
            }
            return computeFeatureVectors(targets);
        }

        /**
         * Base class does not compute feature vectors, because it doesn't know which TargetFeatureComputer to apply
         * 
         * @param targets
         * @return the same targets
         */
        protected List<Target> computeFeatureVectors(List<Target> targets) {
            return targets;
        }

        /**
         * Base class does not predict meaningful values
         * 
         * @param target
         * @return NaN
         */
        protected float evaluate(Target target) {
            return Float.NaN;
        }

    }

    /**
     * Model subclass which currently predicts only a flat 400 ms duration for each boundary Element
     * <p>
     * Could be replaced by a PauseTree or something else, but that would require a CARTModel instead of this.
     * 
     * @author steiner
     * 
     */
    private class BoundaryModel extends Model {
        protected BoundaryModel(String type, String dataFileName, String targetAttributeName, String targetAttributeFormat) {
            super(type, dataFileName, targetAttributeName, targetAttributeFormat);
        }

        protected void applyTo(List<Element> elements) {
            for (Element element : elements) {
                if (!element.hasAttribute(targetAttributeName)) {
                    element.setAttribute(targetAttributeName, "400");
                }
            }
        }
    }

    /**
     * Model subclass for applying a CART to a list of Targets
     * 
     * @author steiner
     * 
     */
    private class CARTModel extends Model {
        private DirectedGraph cart;

        protected CARTModel(String type, String dataFileName, String targetAttributeName, String targetAttributeFormat) {
            super(type, dataFileName, targetAttributeName, targetAttributeFormat);
        }

        protected void loadDataFile() {
            this.cart = null;
            try {
                File cartFile = new File(dataFile);
                cart = new DirectedGraphReader().load(cartFile.getAbsolutePath());
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        protected List<Target> computeFeatureVectors(List<Target> targets) {
            TargetFeatureComputer featureComputer = FeatureRegistry.getTargetFeatureComputer(featureProcessorManager, cart
                    .getFeatureDefinition().getFeatureNames());
            for (Target target : targets) {
                FeatureVector targetFeatureVector = featureComputer.computeFeatureVector(target);
                target.setFeatureVector(targetFeatureVector); // this is critical!
            }
            return targets;
        }

        /**
         * Apply the CART to a Target to get its predicted value
         */
        protected float evaluate(Target target) {
            float[] result = (float[]) cart.interpret(target);
            float value = result[1]; // assuming result is [stdev, val]
            return value;
        }
    }
}
