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

package marytts.modules.acoustic;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import marytts.datatypes.MaryXML;
import marytts.exceptions.MaryConfigurationException;
import marytts.features.FeatureProcessorManager;
import marytts.features.FeatureRegistry;
import marytts.features.FeatureVector;
import marytts.features.TargetFeatureComputer;
import marytts.unitselection.select.Target;
import marytts.unitselection.select.UnitSelector;

import org.w3c.dom.Element;

/**
 * Base class for acoustic modeling; specific Models should extend this and override methods as needed.
 * 
 * @author steiner
 * 
 */
public abstract class Model {

    protected String type;

    protected String dataFile;

    protected String targetAttributeName;

    protected String targetAttributeFormat;

    protected String featureName;

    protected TargetFeatureComputer featureComputer;

    protected String predictFrom;

    protected String applyTo;

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
     *            printf-style format String to specify the attribute value, i.e. "%.3f" to round to 3 decimal places; "%s" by
     *            default
     * @param featureName
     *            name of custom continuous feature, or null
     * @param predictFrom
     *            key of Element Lists from which to predict values; "segments" by default
     * @param applyTo
     *            key of Element Lists to which to apply values; "segments" by default
     */
    protected Model(String type, String dataFileName, String targetAttributeName, String targetAttributeFormat,
            String featureName, String predictFrom, String applyTo) {
        this.type = type;
        this.dataFile = dataFileName;
        this.targetAttributeName = targetAttributeName;
        if (targetAttributeFormat == null) {
            targetAttributeFormat = "%s";
        }
        this.targetAttributeFormat = targetAttributeFormat;
        this.featureName = featureName;
        if (predictFrom == null) {
            predictFrom = "segments";
        }
        this.predictFrom = predictFrom;
        if (applyTo == null) {
            applyTo = "segments";
        }
        this.applyTo = applyTo;
    }

    /**
     * Setter for the TargetFeatureComputer
     * 
     * @param featureComputer
     *            the TargetFeatureComputer
     * @param featureProcessorManager
     *            ignored except where a featureComputer must be overwritten from the Model's dataFile
     * 
     * @throws MaryConfigurationException
     */
    public void setFeatureComputer(TargetFeatureComputer featureComputer, FeatureProcessorManager featureProcessorManager)
            throws MaryConfigurationException {
        this.featureComputer = featureComputer;
    }

    /**
     * Load dataFile for this model; only extension classes know how to do this
     */
    public abstract void loadDataFile();

    /**
     * Apply this Model to a List of Elements, predicting from those same Elements
     * 
     * @param elements
     */
    public void applyTo(List<Element> elements) {
        applyFromTo(elements, elements);
    }

    /**
     * Apply this Model to a List of Elements, predicting from a different List of Elements
     * 
     * @param predictFromElements
     *            Elements from which to predict the values
     * @param applyToElements
     *            Elements to which to apply the values predicted by this Model
     */
    public void applyFromTo(List<Element> predictFromElements, List<Element> applyToElements) {
        assert predictFromElements.size() == applyToElements.size();

        List<Target> predictFromTargets = getTargets(predictFromElements);

        for (int i = 0; i < applyToElements.size(); i++) {
            Target target = predictFromTargets.get(i);

            float targetValue = (float) evaluate(target);

            Element element = applyToElements.get(i);

            // "evaluate" pseudo XPath syntax:
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
     * @return target value
     */
    protected abstract float evaluate(Target target);

    // several getters:
    
    /**
     * @return the dataFile name
     */
    public String getDataFileName() {
        return dataFile;
    }

    /**
     * @return the featureName
     */
    public String getFeatureName() {
        return featureName;
    }

    /**
     * @return the targetAttributeName
     */
    public String getTargetAttributeName() {
        return targetAttributeName;
    }

    /**
     * @return the key of Element Lists from which to predict with this Model
     */
    public String getPredictFrom() {
        return predictFrom;
    }

    /**
     * @return the key of Element Lists to which to apply this Model
     */
    public String getApplyTo() {
        return applyTo;
    }
}
