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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import marytts.exceptions.MaryConfigurationException;
import marytts.features.FeatureDefinition;
import marytts.features.FeatureProcessorManager;
import marytts.features.FeatureVector;
import marytts.htsengine.CartTreeSet;
import marytts.htsengine.HMMData;
import marytts.htsengine.HTSModel;
import marytts.htsengine.HTSParameterGeneration;
import marytts.htsengine.HTSUttModel;
import marytts.unitselection.select.Target;
import marytts.util.MaryUtils;

import org.apache.log4j.Logger;
import org.w3c.dom.Element;

/**
 * Model for predicting duration and F0 from HMMs
 *
 * @author marcela
 *
 */
public class HMMModel extends Model {
	/**
	 * Configuration information of the model
	 */
	private HMMData htsData = null;
	/**
	 * HMM trees and pdfs for this model.
	 */
	private CartTreeSet cart;
	/**
	 * Feature definition used when training HMMs.
	 */
	FeatureDefinition hmmFeatureDefinition;
	/**
	 * to calculate duration in seconds.
	 */
	private float fperiodsec;

	protected static Logger logger = MaryUtils.getLogger("HMMModel");

	/**
	 * If the model is instantiated because the same HHMModel is used for predicting both F0 and duration, set this variable true;
	 * this is done in Voice.loadAcousticModels(), when creating the models.
	 */
	private boolean predictDurAndF0 = false;

	/**
	 * This list keeps a copy of the utterance model, this is done when the same HMMModel is used for predicting durations and F0,
	 * the idea is to keep in the utterance model list the state durations predicted together with duration, these state durations
	 * are used when predicting F0, so the same state duration is applied.
	 */
	private Map<List<Element>, HTSUttModel> uttModels = new WeakHashMap<List<Element>, HTSUttModel>();

	/**
	 * Model constructor
	 *
	 * @param featureManager
	 *            the feature processor manager used to compute the symbolic features used for prediction
	 * @param voiceName
	 *            in HMM models this data file corresponds to the configuration file of the HMM voice
	 * @param dataStream
	 *            dataStream
	 * @param targetAttributeName
	 *            attribute in MARYXML to predict
	 * @param targetAttributeFormat
	 *            print style, not used in HMM models
	 * @param featureName
	 *            not used in HMMModel
	 * @param predictFrom
	 *            not used in HMMModel
	 * @param applyTo
	 *            not used in HMMModel
	 *
	 * @throws MaryConfigurationException
	 *             if there are missing files or problems loading trees and pdf files.
	 */
	public HMMModel(FeatureProcessorManager featureManager, String voiceName, InputStream dataStream, String targetAttributeName,
			String targetAttributeFormat, String featureName, String predictFrom, String applyTo)
					throws MaryConfigurationException {
		super(featureManager, voiceName, dataStream, targetAttributeName, targetAttributeFormat, featureName, predictFrom,
				applyTo);
		if (!(targetAttributeName.contentEquals("d") || targetAttributeName.contentEquals("f0"))) {
			throw new MaryConfigurationException("targetAttributeName = " + targetAttributeName + " Not known");
		}
		load();
	}

	/**
	 * This variable is set to true whenever the same HMMModel is used to predict both duration and F0. by default the variable is
	 * false, so that means that two different HMMModels are used for predicting duration and F0, in this case there is no state
	 * durations information to predict F0.
	 *
	 * @param bval
	 *            bval
	 */
	public void setPredictDurAndF0(boolean bval) {
		predictDurAndF0 = bval;
	}

	/**
	 * Load trees and pdfs, from HMM configuration file.
	 *
	 * @throws MaryConfigurationException
	 *             if there are missing files or problems loading trees and pdf files.
	 */
	@Override
	protected void loadData() throws IOException, MaryConfigurationException {
		if (htsData == null)
			htsData = new HMMData();
		// we use the configuration of the HMM voice whose hmm models will be used
		htsData.initHMMDataForHMMModel(voiceName);
		cart = htsData.getCartTreeSet();
		fperiodsec = ((float) htsData.getFperiod() / (float) htsData.getRate());
		predictionFeatureNames = htsData.getFeatureDefinition().getFeatureNames();
	}

	/**
	 * Predict duration for the list of elements. If the same HMMModel is used to predict duration and F0 then a utterance model
	 * is created and kept in a WeakHashMap, so the next call to this module, for predicting F0, can use that utterance model.
	 *
	 * @param elements
	 *            elements from MaryXML for which to predict the values
	 *
	 * @throws MaryConfigurationException
	 *             if error searching in HMM trees.
	 */
	@Override
	public void applyTo(List<Element> elements) throws MaryConfigurationException {
		logger.debug("predicting duration");
		HTSUttModel um = predictAndSetDuration(elements, elements);
		if (predictDurAndF0) { // this same model will be used for predicting F0 -- remember um
			uttModels.put(elements, um);
		}
	}

	/**
	 * Predict F0 for the list of elements and apply to another list of elements. If the same HMMModel is used to predict duration
	 * and F0 then there must be a utterance model created in a previous call to this module, that will be used to predict F0. If
	 * there is no previously created utterance model then one is created.
	 *
	 * @param predictFromElements
	 *            elements from MaryXML for which to predict the values
	 * @param applyToElements
	 *            elements from MaryXML for which to apply the predicted values
	 *
	 * @throws MaryConfigurationException
	 *             if error searching in HMM trees.
	 */
	@Override
	public void applyFromTo(List<Element> predictFromElements, List<Element> applyToElements) throws MaryConfigurationException {
		logger.debug("predicting F0");
		// Two possibilities: Either we have an uttModel due to a previous call to applyTo()
		// (in which case the lookup key should be applyToElements),
		// or we don't -- in which case we must create an uttModel from the XML.
		HTSUttModel um;
		if (predictDurAndF0) {
			logger.debug("using already created utterance model, it contains predicted state durations.");
			um = uttModels.get(applyToElements); // it must be already created so get it from the uttModels Map
		} else {
			logger.debug("creating utterance model with equal values for state durations.");
			um = createUttModel(predictFromElements); // create a um, state durations are set equal for all states
		}
		assert um != null;
		predictAndSetF0(applyToElements, um);
	}

	/**
	 * Predict durations and state durations from predictFromElements and apply durations to applyToElements. A utterance model is
	 * created that contains the predicted state durations.
	 *
	 * @param predictFromElements
	 *            elements to predict from
	 * @param applyToElements
	 *            elements to apply predicted durations
	 *
	 * @return HTSUttModel a utterance model
	 *
	 * @throws MaryConfigurationException
	 *             if error searching in HMM trees.
	 */
	private HTSUttModel predictAndSetDuration(List<Element> predictFromElements, List<Element> applyToElements)
			throws MaryConfigurationException {
		List<Element> predictorElements = predictFromElements;
		List<Target> predictorTargets = getTargets(predictorElements);
		FeatureVector fv = null;
		HTSUttModel um = new HTSUttModel();
		FeatureDefinition feaDef = htsData.getFeatureDefinition();
		double diffdurOld = 0.0;
		double diffdurNew = 0.0;
		String durAttributeName = "d";

		try {
			// (1) Predict the values
			for (int i = 0; i < predictorTargets.size(); i++) {

				// Retrieve values
				fv = predictorTargets.get(i).getFeatureVector();
				um.addUttModel(new HTSModel(cart.getNumStates()));
				HTSModel m = um.getUttModel(i);
				Element element = applyToElements.get(i);

				/* this function also sets the phone name, the phone between - and + */
				m.setPhoneName(fv.getFeatureAsString(feaDef.getFeatureIndex("phone"), feaDef));

				/* Check if context-dependent gv (gv without sil) */
				if (htsData.getUseContextDependentGV()) {
					if (m.getPhoneName().contentEquals("_"))
						m.setGvSwitch(false);
				}

				/* increment number of models in utterance model */
				um.setNumModel(um.getNumModel() + 1);

				/* update number of states */
				um.setNumState(um.getNumState() + cart.getNumStates());

				String formattedTargetValue;
				double duration;

				// if the attribute already exists for this element keep it
				if (element.hasAttribute(durAttributeName)) {
					// Element is in milliseconds already, so convert to second in order to get reformatted in ms après
					formattedTargetValue = element.getAttribute(durAttributeName);
					duration = Float.parseFloat(formattedTargetValue) / 1000;
					formattedTargetValue = String.format(targetAttributeFormat, duration);
					um.setTotalFrame(um.getTotalFrame() + (int) Math.round(duration / fperiodsec));
				} else {

					// Estimate state duration from state duration model (Gaussian)
					diffdurNew = cart.searchDurInCartTree(m, fv, htsData, diffdurOld);
					diffdurOld = diffdurNew;
					duration = m.getTotalDur() * fperiodsec; // in seconds
					um.setTotalFrame(um.getTotalFrame() + m.getTotalDur());
				}

				/*
				 * Find pdf for LF0, this function sets the pdf for each state. and determines, according to the HMM models,
				 * whether the states are voiced or unvoiced, (it can be possible that some states are voiced and some unvoiced).
				 */
				cart.searchLf0InCartTree(m, fv, feaDef, htsData.getUV());
				for (int mstate = 0; mstate < cart.getNumStates(); mstate++) {
					for (int frame = 0; frame < m.getDur(mstate); frame++) {
						if (m.getVoiced(mstate))
							um.setLf0Frame(um.getLf0Frame() + 1);
					}
				}

				// "evaluate" pseudo XPath syntax:
				// TODO this needs to be extended to take into account targetAttributeNames like "foo/@bar", which would add the
				// bar attribute to the foo child of this element, creating the child if not already present...
				if (durAttributeName.startsWith("@")) {
					durAttributeName = durAttributeName.replaceFirst("@", "");
				}
				formattedTargetValue = String.format(targetAttributeFormat, duration);

				// set the new attribute value:
				element.setAttribute(durAttributeName, formattedTargetValue);
			}

			return um;
		} catch (Exception e) {
			throw new MaryConfigurationException("Error searching in tree when predicting duration. ", e);
		}
	}

	/**
	 * Predict F0 from the utterance model and apply to elements
	 *
	 * @param applyToElements
	 *            elements to apply predicted F0s
	 * @param um
	 *            utterance model that contains the set of elements (phonemes) and state durations for generating F0.
	 *
	 * @throws MaryConfigurationException
	 *             if error generating F0 out of HMMs trees and pdfs.
	 */
	private void predictAndSetF0(List<Element> applyToElements, HTSUttModel um) throws MaryConfigurationException {

		HTSModel m;
		try {
			String f0AttributeName = "f0";
			HTSParameterGeneration pdf2par = new HTSParameterGeneration();
			/* Once we have all the phone models Process UttModel */
			/* Generate sequence of speech parameter vectors, generate parameters out of sequence of pdf's */
			boolean debug = false; /* so it does not save the generated parameters. */
			/* this function generates features just for the trees and pdf that are not null in the HMM cart */
			pdf2par.htsMaximumLikelihoodParameterGeneration(um, htsData);

			// (2) include the predicted values in applicableElements (as it is done in Model)
			boolean voiced[] = pdf2par.getVoicedArray();
			int numVoiced = 0;
			// make sure that the number of applicable elements is the same as the predicted number of elements
			assert applyToElements.size() == um.getNumModel();
			float f0;
			String formattedTargetValue;
			int t = 0;
			for (int i = 0; i < applyToElements.size(); i++) { // this will be the same as the utterance model set
				m = um.getUttModel(i);
				int k = 1;
				int numVoicedInModel = m.getNumVoiced();
				formattedTargetValue = "";
				// System.out.format("phone = %s dur_in_frames=%d num_voiced_frames=%d : ", m.getPhoneName(), m.getTotalDur(),
				// numVoicedInModel);
				for (int mstate = 0; mstate < cart.getNumStates(); mstate++) {
					for (int frame = 0; frame < m.getDur(mstate); frame++) {
						if (voiced[t++]) { // numVoiced and t are not the same because voiced values can be true or false,
							// numVoiced count just the voiced
							f0 = (float) Math.exp(pdf2par.getlf0Pst().getPar(numVoiced++, 0));
							formattedTargetValue += "(" + Integer.toString((int) ((k * 100.0) / numVoicedInModel)) + ","
									+ Integer.toString((int) f0) + ")";
							k++;
						}
					}
				}
				Element element = applyToElements.get(i);
				// "evaluate" pseudo XPath syntax:
				// TODO this needs to be extended to take into account targetAttributeNames like "foo/@bar", which would add the
				// bar attribute to the foo child of this element, creating the child if not already present...
				if (f0AttributeName.startsWith("@")) {
					f0AttributeName = f0AttributeName.replaceFirst("@", "");
				}
				// format targetValue according to targetAttributeFormat
				// String formattedTargetValue = String.format(targetAttributeFormat, targetValue);
				// set the new attribute value:
				// if the whole segment is unvoiced then f0 should not be fixed?
				if (formattedTargetValue.length() > 0)
					element.setAttribute(f0AttributeName, formattedTargetValue);
				// System.out.println(formattedTargetValue);
			}
			// once finished re-set to null um
			// um = null;

		} catch (Exception e) {
			throw new MaryConfigurationException("Error generating F0 out of HMMs trees and pdfs. ", e);
		}

	}

	/**
	 * Create a utterance model list from feature vectors predicted from elements.
	 *
	 * @param predictFromElements
	 *            elements from MaryXML from where to get feature vectors.
	 *
	 * @return Utterance model um containing state durations and pdfs already searched on the trees to generate F0.
	 *
	 * @throws MaryConfigurationException
	 *             if error searching in HMM trees.
	 */
	private HTSUttModel createUttModel(List<Element> predictFromElements) throws MaryConfigurationException {
		int i, k, s, t, mstate, frame, durInFrames, durStateInFrames, numVoicedInModel;
		HTSModel m;
		List<Element> predictorElements = predictFromElements;
		List<Target> predictorTargets = getTargets(predictorElements);
		FeatureVector fv;
		HTSUttModel um = new HTSUttModel();
		FeatureDefinition feaDef = htsData.getFeatureDefinition();
		float duration;
		double diffdurOld = 0.0;
		double diffdurNew = 0.0;
		float f0s[] = null;
		try {
			// (1) Predict the values
			for (i = 0; i < predictorTargets.size(); i++) {
				fv = predictorTargets.get(i).getFeatureVector();
				Element e = predictFromElements.get(i);
				um.addUttModel(new HTSModel(cart.getNumStates()));
				m = um.getUttModel(i);
				/* this function also sets the phone name, the phone between - and + */
				m.setPhoneName(fv.getFeatureAsString(feaDef.getFeatureIndex("phone"), feaDef));
				/* Check if context-dependent gv (gv without sil) */
				if (htsData.getUseContextDependentGV()) {
					if (m.getPhoneName().contentEquals("_"))
						m.setGvSwitch(false);
				}
				/* increment number of models in utterance model */
				um.setNumModel(um.getNumModel() + 1);
				/* update number of states */
				um.setNumState(um.getNumState() + cart.getNumStates());
				// get the duration from the element
				duration = Integer.parseInt(e.getAttribute("d")) * 0.001f; // in sec.
				// distribute the duration (in frames) among the five states, here it is done the same amount for each state
				durInFrames = (int) (duration / fperiodsec);
				durStateInFrames = (int) (durInFrames / cart.getNumStates());
				m.setTotalDur(0); // reset to set new value according to duration
				for (s = 0; s < cart.getNumStates(); s++) {
					m.setDur(s, durStateInFrames);
					m.setTotalDur(m.getTotalDur() + m.getDur(s));
				}
				um.setTotalFrame(um.getTotalFrame() + m.getTotalDur());
				System.out.format("createUttModel: duration=%.3f sec. durInFrames=%d  durStateInFrames=%d  m.getTotalDur()=%d\n",
						duration, durInFrames, durStateInFrames, m.getTotalDur());

				/*
				 * Find pdf for LF0, this function sets the pdf for each state. and determines, according to the HMM models,
				 * whether the states are voiced or unvoiced, (it can be possible that some states are voiced and some unvoiced).
				 */
				cart.searchLf0InCartTree(m, fv, feaDef, htsData.getUV());
				for (mstate = 0; mstate < cart.getNumStates(); mstate++) {
					for (frame = 0; frame < m.getDur(mstate); frame++)
						if (m.getVoiced(mstate))
							um.setLf0Frame(um.getLf0Frame() + 1);
				}
			}
			return um;
		} catch (Exception e) {
			throw new MaryConfigurationException("Error searching in tree when creating utterance model. ", e);
		}
	}

	/**
	 * Apply the HMM to a Target to get its predicted value, this method is not used in HMMModel.
	 *
	 * @throws RuntimeException
	 *             if this method is called.
	 */
	@Override
	protected float evaluate(Target target) {
		throw new RuntimeException("This method should never be called");
	}

}
