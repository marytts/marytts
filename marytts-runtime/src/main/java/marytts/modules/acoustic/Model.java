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

import java.io.IOException;
import java.io.InputStream;
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
import org.w3c.dom.Node;

/**
 * Base class for acoustic modeling; specific Models should extend this and override methods as needed.
 * 
 * @author steiner
 * 
 */
public abstract class Model {

	/**
	 * The stream from which we will read our acoustic model.
	 */
	protected InputStream dataStream;

	/**
	 * The voice with which this model is associated
	 */
	protected String voiceName;

	/**
	 * The attribute into which the predicted acoustic feature should be written.
	 */
	protected String targetAttributeName;

	protected String targetAttributeFormat;

	/**
	 * The name of the predicted acoustic feature, if any. The feature processor that will be created from this will read the
	 * value from {@link #targetAttributeName}.
	 */
	protected String featureName;

	/**
	 * The feature processors used for prediction.
	 */
	protected FeatureProcessorManager featureManager;

	/**
	 * The names of the features used for prediction.
	 */
	protected String predictionFeatureNames;

	/**
	 * The producer of feature vectors for the features in {@link #predictionFeatureNames} as computed by the feature processors
	 * in {@link #featureManager}.
	 */
	protected TargetFeatureComputer featureComputer;

	protected String predictFrom;

	protected String applyTo;

	/**
	 * Model constructor
	 * 
	 * @param featureManager
	 *            the feature processor manager used to compute the symbolic features used for prediction
	 * @param voiceName
	 *            name of the voice
	 * @param dataStream
	 *            data file for this Model
	 * @param targetAttributeName
	 *            attribute in MaryXML to predict
	 * @param targetAttributeFormat
	 *            printf-style format String to specify the attribute value, i.e. "%.3f" to round to 3 decimal places; "%s" by
	 *            default
	 * @param featureName
	 *            name of the custom continuous feature predicted by this model, or null
	 * @param predictFrom
	 *            key of Element Lists from which to predict values; "segments" by default
	 * @param applyTo
	 *            key of Element Lists to which to apply values; "segments" by default
	 */
	protected Model(FeatureProcessorManager featureManager, String voiceName, InputStream dataStream, String targetAttributeName,
			String targetAttributeFormat, String featureName, String predictFrom, String applyTo) {
		this.featureManager = featureManager;
		this.voiceName = voiceName;
		this.dataStream = dataStream;
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
	 * Try to load this model and set the target feature computer appropriately. This must be called from the constructor of
	 * subclasses, so that the subclass implementation of loadDataFile() is visible.
	 * 
	 * @throws MaryConfigurationException
	 *             if the model cannot be set up properly.
	 */
	protected final void load() throws MaryConfigurationException {
		try {
			loadData();
		} catch (IOException ioe) {
			throw new MaryConfigurationException("Cannot load model data from stream", ioe);
		}
		setupFeatureComputer();
	}

	/**
	 * Load dataFile for this model; only extension classes know how to do this
	 * 
	 * @throws IOException
	 *             if any files cannot be properly read
	 * @throws MaryConfigurationException
	 *             if files can be read but contain problematic content
	 */
	protected abstract void loadData() throws IOException, MaryConfigurationException;

	protected final void setupFeatureComputer() throws MaryConfigurationException {
		try {
			featureComputer = FeatureRegistry.getTargetFeatureComputer(featureManager, predictionFeatureNames);
		} catch (IllegalArgumentException iae) {
			throw new MaryConfigurationException("Incompatible features between model and feature processor manager.\n"
					+ "The model needs the following features:\n" + predictionFeatureNames + "\n"
					+ "The FeatureProcessorManager for locale " + featureManager.getLocale() + " ("
					+ featureManager.getClass().toString() + ") can produce the following features:\n"
					+ featureManager.listFeatureProcessorNames(), iae);
		}
	}

	/**
	 * Apply this Model to a List of Elements, predicting from those same Elements
	 * 
	 * @param elements
	 *            Elements for which to predict the values
	 * @throws MaryConfigurationException
	 *             if attribute values cannot be predicted because of an invalid voice configuration
	 */
	public void applyTo(List<Element> elements) throws MaryConfigurationException {
		applyFromTo(elements, elements);
	}

	/**
	 * Apply this Model to a List of Elements, predicting from a different List of Elements
	 * 
	 * @param predictFromElements
	 *            Elements from which to predict the values
	 * @param applyToElements
	 *            Elements to which to apply the values predicted by this Model
	 * @throws MaryConfigurationException
	 *             if attribute values cannot be predicted because of an invalid voice configuration
	 */
	public void applyFromTo(List<Element> predictFromElements, List<Element> applyToElements) throws MaryConfigurationException {
		assert predictFromElements != null;
		assert applyToElements != null;
		assert predictFromElements.size() == applyToElements.size();

		List<Target> predictFromTargets = getTargets(predictFromElements);

		for (int i = 0; i < applyToElements.size(); i++) {
			Target target = predictFromTargets.get(i);

			float targetValue;
			try {
				targetValue = (float) evaluate(target);
			} catch (Exception e) {
				throw new MaryConfigurationException("Could not predict value for target: '" + target + "'", e);
			}

			Element element = applyToElements.get(i);

			// "evaluate" pseudo XPath syntax:
			// TODO this needs to be extended to take into account
			// targetAttributeNames like "foo/@bar", which would add the
			// bar attribute to the foo child of this element, creating the
			// child if not already present...
			if (targetAttributeName.startsWith("@")) {
				targetAttributeName = targetAttributeName.replaceFirst("@", "");
			}

			String formattedTargetValue = null;
			try {
				formattedTargetValue = String.format(targetAttributeFormat, targetValue);
			} catch (Exception e) {
				throw new MaryConfigurationException("Could not format target value '" + targetValue + "' using format '"
						+ targetAttributeFormat + "'", e);
			}

			// System.out.println("formattedTargetValue = " +
			// formattedTargetValue);

			// if the attribute already exists for this element, append
			// targetValue:
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
			// compute FeatureVectors for Targets:
			FeatureVector targetFeatureVector = featureComputer.computeFeatureVector(target);
			target.setFeatureVector(targetFeatureVector); // this is critical!
			element.setUserData("target", target, Target.targetFeatureCloner);
		}
		return targets;
	}

	/**
	 * Evaluate model on a Target to obtain the target value as a float.
	 * 
	 * @param target
	 *            target
	 * @return target value
	 * @throws Exception
	 *             if the target value cannot be predicted
	 */
	protected abstract float evaluate(Target target) throws Exception;

	// several getters:

	/**
	 * 
	 * @return the name of the voice that this model is associated with
	 */
	public String getVoiceName() {
		return voiceName;
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
