package marytts.modules.acoustic;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import marytts.cart.DirectedGraph;
import marytts.cart.io.DirectedGraphReader;
import marytts.datatypes.MaryXML;
import marytts.features.FeatureRegistry;
import marytts.features.FeatureVector;
import marytts.features.TargetFeatureComputer;
import marytts.unitselection.select.Target;
import marytts.unitselection.select.UnitSelector;

import org.w3c.dom.Element;

/**
 * Base class for acoustic modelling; specific Models should extend this and override methods as needed.
 * 
 * @author steiner
 * 
 */
public abstract class Model {

    public static final String TYPE = "type";

    public static final String DATA = "data";

    public static final String ATTRIBUTE = "attribute";

    public static final String ATTRIBUTE_FORMAT = "attribute.format";

    public static final String SCOPE = "scope";

    protected String type;

    protected String dataFile;

    protected String targetAttributeName;

    protected String targetAttributeFormat;

    private String targetElementListName;

    protected TargetFeatureComputer featureComputer;

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
    protected Model(String type, String dataFileName, String targetAttributeName, String targetAttributeFormat,
            String targetElementListName) {
        this.type = type;
        this.dataFile = dataFileName;
        this.targetAttributeName = targetAttributeName;
        if (targetAttributeFormat == null) {
            targetAttributeFormat = "%s";
        }
        this.targetAttributeFormat = targetAttributeFormat;
        if (targetElementListName == null) {
            targetElementListName = "segments";
        }
        this.targetElementListName = targetElementListName;
        // featureComputer should be loaded in subclasses:
        featureComputer = null;
    }

    /**
     * Load datafile for this model; only subclasses know how to do this
     */
    public abstract void loadDataFile();

    /**
     * Apply this Model to a List of Elements, predicting from those same Elements
     * 
     * @param elements
     */
    public void applyTo(List<Element> elements) {
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
    public void applyTo(List<Element> applicableElements, List<Element> predictorElements) {
        assert applicableElements.size() == predictorElements.size();

        List<Target> predictorTargets = getTargets(predictorElements);

        for (int i = 0; i < applicableElements.size(); i++) {
            Target target = predictorTargets.get(i);
            float targetValue = (float) evaluate(target);

            Element element = applicableElements.get(i);

            // "evaulate" pseudo XPath syntax:
            // TODO this needs to be extended to take into account targetAttributeNames like "foo/@bar", which would add the
            // bar attribute to the foo child of this element, creating the child if not already present...
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
        // compute FeatureVectors for Targets:
        for (Target target : targets) {
            FeatureVector targetFeatureVector = featureComputer.computeFeatureVector(target);
            target.setFeatureVector(targetFeatureVector); // this is critical!
        }
        return targets;
    }

    /**
     * Evaluate model on a Target to obtain the target value as a float.
     * 
     * @param target
     */
    protected abstract float evaluate(Target target);

    /**
     * @return the targetElementListName
     */
    public String getTargetElementListName() {
        return targetElementListName;
    }

}
