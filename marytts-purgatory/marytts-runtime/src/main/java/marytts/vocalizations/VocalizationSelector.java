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
package marytts.vocalizations;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.List;

import marytts.exceptions.MaryConfigurationException;
import marytts.features.FeatureDefinition;
import marytts.features.FeatureVector;
import marytts.modules.synthesis.Voice;
import marytts.server.MaryProperties;
import marytts.unitselection.data.Unit;
import marytts.unitselection.select.Target;
import marytts.unitselection.select.VocalizationFFRTargetCostFunction;
import marytts.util.MaryUtils;
import marytts.util.math.MathUtils;
import marytts.util.math.Polynomial;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.w3c.dom.Element;

/**
 * Select suitable vocalization for a given target using a cost function
 * 
 * @author sathish
 * 
 */
public class VocalizationSelector {

	protected VocalizationFeatureFileReader featureFileReader;
	protected FeatureDefinition featureDefinition;
	protected FeatureDefinition f0FeatureDefinition;
	protected VocalizationIntonationReader vIntonationReader;
	protected VocalizationUnitFileReader unitFileReader;
	protected VocalizationFFRTargetCostFunction vffrtUnitCostFunction = null;
	protected VocalizationFFRTargetCostFunction vffrtContourCostFunction = null;
	protected boolean f0ContourImposeSupport;
	protected boolean usePrecondition;
	protected double contourCostWeight;
	private DecimalFormat df;

	protected int noOfSuitableUnits = 1;

	protected Logger logger = MaryUtils.getLogger("Vocalization Selector");

	public VocalizationSelector(Voice voice) throws MaryConfigurationException {

		String unitFileName = MaryProperties.getFilename("voice." + voice.getName() + ".vocalization.unitfile");
		String featureFile = MaryProperties.getFilename("voice." + voice.getName() + ".vocalization.featurefile");
		String featureDefinitionFile = MaryProperties.getFilename("voice." + voice.getName()
				+ ".vocalization.featureDefinitionFile");
		f0ContourImposeSupport = MaryProperties.getBoolean("voice." + voice.getName() + ".f0ContourImposeSupport", false);
		df = new DecimalFormat("##.##");
		try {
			BufferedReader fDBufferedReader = new BufferedReader(new FileReader(new File(featureDefinitionFile)));
			this.featureDefinition = new FeatureDefinition(fDBufferedReader, true);
			this.featureFileReader = new VocalizationFeatureFileReader(featureFile);
			vffrtUnitCostFunction = new VocalizationFFRTargetCostFunction(this.featureFileReader, this.featureDefinition);
			unitFileReader = new VocalizationUnitFileReader(unitFileName);

			if (this.featureFileReader.getNumberOfUnits() != this.unitFileReader.getNumberOfUnits()) {
				throw new MaryConfigurationException("Feature file reader and unit file reader is not aligned properly");
			}

			if (this.f0ContourImposeSupport) {
				String intonationFDFile = MaryProperties.getFilename("voice." + voice.getName()
						+ ".vocalization.intonation.featureDefinitionFile");
				String intonationFile = MaryProperties.getFilename("voice." + voice.getName() + ".vocalization.intonationfile");
				usePrecondition = MaryProperties.getBoolean("voice." + voice.getName() + ".vocalization.usePrecondition", false);
				contourCostWeight = (new Double(MaryProperties.getProperty("voice." + voice.getName()
						+ ".vocalization.contourCostWeight", "0.5"))).doubleValue();
				if (contourCostWeight < 0 || contourCostWeight > 1.0) {
					throw new MaryConfigurationException("contourCostWeight should be between 0 and 1");
				}
				BufferedReader f0FDBufferedReader = new BufferedReader(new FileReader(new File(intonationFDFile)));
				f0FeatureDefinition = new FeatureDefinition(f0FDBufferedReader, true);
				vIntonationReader = new VocalizationIntonationReader(intonationFile);
				noOfSuitableUnits = MaryProperties.getInteger("voice." + voice.getName()
						+ ".vocalization.intonation.numberOfSuitableUnits");
				vffrtContourCostFunction = new VocalizationFFRTargetCostFunction(this.featureFileReader, this.f0FeatureDefinition);
			}
		} catch (IOException e) {
			throw new MaryConfigurationException("Problem loading vocalization files for voice ", e);
		}
	}

	/**
	 * Get feature definition used to select suitable candidate
	 * 
	 * @return Feature Definition
	 */
	public FeatureDefinition getFeatureDefinition() {
		return this.featureDefinition;
	}

	/**
	 * Get best candidate pair to impose F0 contour on other
	 * 
	 * @param domElement
	 *            xml request for vocalization
	 * @return SourceTargetPair best candidate pair
	 */
	public SourceTargetPair getBestCandidatePairtoImposeF0(Element domElement) {

		VocalizationCandidate[] vCosts = getBestMatchingCandidates(domElement);
		VocalizationCandidate[] vIntonationCosts = getBestIntonationCandidates(domElement);

		int noOfSuitableF0Units;
		if (usePrecondition) {
			noOfSuitableF0Units = getNumberContoursAboveThreshold(vCosts, vIntonationCosts);
		} else {
			noOfSuitableF0Units = noOfSuitableUnits;
		}

		if (noOfSuitableF0Units == 0) {
			return new SourceTargetPair(vCosts[0].unitIndex, vCosts[0].unitIndex, 0);
		}

		VocalizationCandidate[] suitableCandidates = new VocalizationCandidate[noOfSuitableUnits];
		System.arraycopy(vCosts, 0, suitableCandidates, 0, noOfSuitableUnits);
		VocalizationCandidate[] suitableF0Candidates = new VocalizationCandidate[noOfSuitableF0Units];
		System.arraycopy(vIntonationCosts, 0, suitableF0Candidates, 0, noOfSuitableF0Units);

		Target targetUnit = createTarget(domElement);
		if (logger.getEffectiveLevel().equals(Level.DEBUG)) {
			debugLogCandidates(targetUnit, suitableCandidates, suitableF0Candidates);
		}

		SourceTargetPair[] sortedImposeF0Data = vocalizationF0DistanceComputer(suitableCandidates, suitableF0Candidates,
				domElement);

		return sortedImposeF0Data[0];
	}

	/**
	 * compute a threshold according to precondition and get number of contours above computed threshold
	 * 
	 * formula : for all CC(j) < CCmax where j1, j2, j3 ... are contour candidates CCmax (threshold) = min (CC(i1), CC(i2),
	 * CC(i3)....) where i1, i2, i3 .. are unit candidates
	 * 
	 * @param vCosts
	 *            VocalizationCandidates
	 * @param vIntonationCosts
	 *            VocalizationF0Candidates
	 * @return int number of contours above computed threshold
	 */
	private int getNumberContoursAboveThreshold(VocalizationCandidate[] vCosts, VocalizationCandidate[] vIntonationCosts) {

		// get minimum cc cost for all units
		double[] costs = new double[noOfSuitableUnits];
		for (int i = 0; i < costs.length; i++) {
			int unitIndex = vCosts[i].unitIndex;
			VocalizationCandidate contourCandidate = getCandidateByIndex(vIntonationCosts, unitIndex);
			if (contourCandidate != null) {
				costs[i] = contourCandidate.cost;
			} else {
				costs[i] = Double.MAX_VALUE;
			}
		}

		double threshold = MathUtils.min(costs);
		int contourSetSize = 0;
		for (int i = 0; i < vIntonationCosts.length; i++) {
			if (vIntonationCosts[i].cost < threshold) {
				contourSetSize = i + 1;
			} else {
				break;
			}
		}

		return contourSetSize;
	}

	/**
	 * search candidate by unitindex
	 * 
	 * @param vIntonationCosts
	 *            VocalizationF0Candidates
	 * @param unitIndex
	 *            unitindex
	 * @return VocalizationCandidate of given index
	 */
	private VocalizationCandidate getCandidateByIndex(VocalizationCandidate[] vIntonationCosts, int unitIndex) {

		for (int i = 0; i < vIntonationCosts.length; i++) {
			if (vIntonationCosts[i].unitIndex == unitIndex) {
				return vIntonationCosts[i];
			}
		}
		return null;
	}

	/**
	 * polynomial distance computer between two units
	 * 
	 * @param suitableCandidates
	 *            vocalization candidates
	 * @param suitableF0Candidates
	 *            intonation candidates
	 * @return an array of candidate pairs
	 */
	private SourceTargetPair[] vocalizationF0DistanceComputer(VocalizationCandidate[] suitableCandidates,
			VocalizationCandidate[] suitableF0Candidates, Element domElement) {

		int noPossibleImpositions = suitableCandidates.length * suitableF0Candidates.length;
		SourceTargetPair[] imposeF0Data = new SourceTargetPair[noPossibleImpositions];
		int count = 0;

		for (int i = 0; i < suitableCandidates.length; i++) {
			for (int j = 0; j < suitableF0Candidates.length; j++) {
				int sourceIndex = suitableCandidates[i].unitIndex;
				int targetIndex = suitableF0Candidates[j].unitIndex;

				double contourCost = getContourCostDistance(sourceIndex, targetIndex);
				double mergeCost = getMergeCost(sourceIndex, targetIndex, domElement);
				double cost = (contourCost * contourCostWeight) + (mergeCost * (1 - contourCostWeight));
				logger.debug("Unit Index " + sourceIndex + " & Contour Index " + targetIndex + " :: Countour cost: "
						+ df.format(contourCost) + " + Merge Cost: " + df.format(mergeCost) + " --> TotalCost: "
						+ df.format(cost));
				imposeF0Data[count++] = new SourceTargetPair(sourceIndex, targetIndex, cost);
			}
		}

		Arrays.sort(imposeF0Data);

		return imposeF0Data;
	}

	/**
	 * Compute mergeCost for unit and contour candidates Formula = segmentalformCost(u(i)) + intonationCost(c(i)) +
	 * voiceQualityCost(u(i)) + 0.5 * (meaningCost(u(i)) + meaningCost(c(i)) )
	 * 
	 * @param sourceIndex
	 *            unit index
	 * @param targetIndex
	 *            unit index
	 * @return double merge cost
	 */
	private double getMergeCost(int sourceIndex, int targetIndex, Element domElement) {

		Target targetUnit = createTarget(domElement);
		Target targetContour = createIntonationTarget(domElement);

		Unit unitCandidate = this.unitFileReader.getUnit(sourceIndex);
		Unit contourCandidate = this.unitFileReader.getUnit(targetIndex);

		// unit features
		double segmentalCost = vffrtUnitCostFunction.featureCost(targetUnit, unitCandidate, "name");
		double voiceQualityCost = vffrtUnitCostFunction.featureCost(targetUnit, unitCandidate, "voicequality");
		double meaningUnitCost = 0;
		String[] meaningFeatureNames = vffrtUnitCostFunction.getFeatureDefinition().getContinuousFeatureNameArray();
		for (int i = 0; i < meaningFeatureNames.length; i++) {
			meaningUnitCost += vffrtUnitCostFunction.featureCost(targetUnit, unitCandidate, meaningFeatureNames[i]);
		}

		// contour features
		double intonationCost = this.vffrtContourCostFunction.featureCost(targetContour, contourCandidate, "intonation");
		double meaningContourCost = 0;
		String[] meaningContourFeatures = vffrtContourCostFunction.getFeatureDefinition().getContinuousFeatureNameArray();
		for (int i = 0; i < meaningContourFeatures.length; i++) {
			meaningContourCost += vffrtContourCostFunction
					.featureCost(targetContour, contourCandidate, meaningContourFeatures[i]);
		}

		double mergeCost = segmentalCost + voiceQualityCost + intonationCost + 0.5 * (meaningUnitCost + meaningContourCost);

		return mergeCost;
	}

	/**
	 * compute contour distance (polynomial distance)
	 * 
	 * @param sourceIndex
	 *            unit index
	 * @param targetIndex
	 *            unit index
	 * @return polynomial distance measure
	 */
	private double getContourCostDistance(int sourceIndex, int targetIndex) {
		double distance;
		if (targetIndex == sourceIndex) {
			distance = 0;
		} else {
			double[] targetCoeffs = vIntonationReader.getIntonationCoeffs(targetIndex);
			double[] sourceCoeffs = vIntonationReader.getIntonationCoeffs(sourceIndex);
			if (targetCoeffs != null && sourceCoeffs != null && targetCoeffs.length == sourceCoeffs.length) {
				distance = Polynomial.polynomialDistance(sourceCoeffs, targetCoeffs);
			} else {
				distance = Double.MAX_VALUE;
			}
		}
		return distance;
	}

	/**
	 * get a best matching candidate for a given target
	 * 
	 * @param domElement
	 *            xml request for vocalization
	 * @return unit index of best matching candidate
	 */
	public int getBestMatchingCandidate(Element domElement) {

		Target targetUnit = createTarget(domElement);
		int numberUnits = this.unitFileReader.getNumberOfUnits();
		double minCost = Double.MAX_VALUE;
		int index = 0;
		for (int i = 0; i < numberUnits; i++) {
			Unit singleUnit = this.unitFileReader.getUnit(i);
			double cost = vffrtUnitCostFunction.cost(targetUnit, singleUnit);
			if (cost < minCost) {
				minCost = cost;
				index = i;
			}
		}

		return index;
	}

	/**
	 * get a array of best candidates sorted according to cost
	 * 
	 * @param domElement
	 *            xml request for vocalization
	 * @return an array of best vocalization candidates
	 */
	public VocalizationCandidate[] getBestMatchingCandidates(Element domElement) {
		// FeatureDefinition featDef = this.featureFileReader.getFeatureDefinition();
		Target targetUnit = createTarget(domElement);
		int numberUnits = this.unitFileReader.getNumberOfUnits();
		VocalizationCandidate[] vocalizationCandidates = new VocalizationCandidate[numberUnits];
		for (int i = 0; i < numberUnits; i++) {
			Unit singleUnit = this.unitFileReader.getUnit(i);
			double cost = vffrtUnitCostFunction.cost(targetUnit, singleUnit);
			vocalizationCandidates[i] = new VocalizationCandidate(i, cost);
		}
		Arrays.sort(vocalizationCandidates);
		return vocalizationCandidates;
	}

	/**
	 * Debug messages for selected candidates
	 * 
	 * @param targetUnit
	 *            target unit
	 * @param suitableCandidates
	 *            suitable vocalization candidates
	 * @param suitableF0Candidates
	 *            suitable intonation candidates
	 */
	private void debugLogCandidates(Target targetUnit, VocalizationCandidate[] suitableCandidates,
			VocalizationCandidate[] suitableF0Candidates) {
		FeatureVector targetFeatures = targetUnit.getFeatureVector();
		FeatureDefinition fd = featureFileReader.getFeatureDefinition();
		int fiName = fd.getFeatureIndex("name");
		int fiIntonation = fd.getFeatureIndex("intonation");
		int fiVQ = fd.getFeatureIndex("voicequality");
		for (int i = 0; i < suitableCandidates.length; i++) {
			int unitIndex = suitableCandidates[i].unitIndex;
			double unitCost = suitableCandidates[i].cost;
			FeatureVector fv = featureFileReader.getFeatureVector(unitIndex);
			StringBuilder sb = new StringBuilder();
			sb.append("Candidate ").append(i).append(": ").append(unitIndex).append(" ( " + unitCost + " ) ").append(" -- ");
			byte bName = fv.getByteFeature(fiName);
			if (fv.getByteFeature(fiName) != 0 && targetFeatures.getByteFeature(fiName) != 0) {
				sb.append(" ").append(fv.getFeatureAsString(fiName, fd));
			}
			if (fv.getByteFeature(fiVQ) != 0 && targetFeatures.getByteFeature(fiName) != 0) {
				sb.append(" ").append(fv.getFeatureAsString(fiVQ, fd));
			}
			if (fv.getByteFeature(fiIntonation) != 0 && targetFeatures.getByteFeature(fiIntonation) != 0) {
				sb.append(" ").append(fv.getFeatureAsString(fiIntonation, fd));
			}
			for (int j = 0; j < targetFeatures.getLength(); j++) {
				if (targetFeatures.isContinuousFeature(j) && !Float.isNaN((Float) targetFeatures.getFeature(j))
						&& !Float.isNaN((Float) fv.getFeature(j))) {
					String featureName = fd.getFeatureName(j);
					sb.append(" ").append(featureName).append("=").append(fv.getFeature(j));
				}
			}
			logger.debug(sb.toString());
		}
		for (int i = 0; i < suitableF0Candidates.length; i++) {
			int unitIndex = suitableF0Candidates[i].unitIndex;
			double unitCost = suitableF0Candidates[i].cost;
			FeatureVector fv = featureFileReader.getFeatureVector(unitIndex);
			StringBuilder sb = new StringBuilder();
			sb.append("F0 Candidate ").append(i).append(": ").append(unitIndex).append(" ( " + unitCost + " ) ").append(" -- ");
			byte bName = fv.getByteFeature(fiName);
			if (fv.getByteFeature(fiName) != 0 && targetFeatures.getByteFeature(fiName) != 0) {
				sb.append(" ").append(fv.getFeatureAsString(fiName, fd));
			}
			if (fv.getByteFeature(fiVQ) != 0 && targetFeatures.getByteFeature(fiName) != 0) {
				sb.append(" ").append(fv.getFeatureAsString(fiVQ, fd));
			}
			if (fv.getByteFeature(fiIntonation) != 0 && targetFeatures.getByteFeature(fiIntonation) != 0) {
				sb.append(" ").append(fv.getFeatureAsString(fiIntonation, fd));
			}
			for (int j = 0; j < targetFeatures.getLength(); j++) {
				if (targetFeatures.isContinuousFeature(j) && !Float.isNaN((Float) targetFeatures.getFeature(j))
						&& !Float.isNaN((Float) fv.getFeature(j))) {
					String featureName = fd.getFeatureName(j);
					sb.append(" ").append(featureName).append("=").append(fv.getFeature(j));
				}
			}
			logger.debug(sb.toString());
		}
	}

	/**
	 * get a array of best candidates sorted according to cost (cost computed on f0_feature_definition features only)
	 * 
	 * @param domElement
	 *            xml request for vocalization
	 * @return VocalizationCandidate[] a array of best candidates
	 */
	private VocalizationCandidate[] getBestIntonationCandidates(Element domElement) {

		Target targetUnit = createIntonationTarget(domElement);
		int numberUnits = this.unitFileReader.getNumberOfUnits();
		VocalizationCandidate[] vocalizationCandidates = new VocalizationCandidate[numberUnits];
		for (int i = 0; i < numberUnits; i++) {
			Unit singleUnit = this.unitFileReader.getUnit(i);
			double cost = vffrtContourCostFunction.cost(targetUnit, singleUnit);
			vocalizationCandidates[i] = new VocalizationCandidate(i, cost);
		}
		Arrays.sort(vocalizationCandidates);
		return vocalizationCandidates;
	}

	/**
	 * create target from XML request
	 * 
	 * @param domElement
	 *            xml request for vocalization
	 * @return Target target represents xml request
	 */
	private Target createTarget(Element domElement) {

		// FeatureDefinition featDef = this.featureFileReader.getFeatureDefinition();
		FeatureDefinition featDef = this.featureDefinition;
		int numFeatures = featDef.getNumberOfFeatures();
		int numByteFeatures = featDef.getNumberOfByteFeatures();
		int numShortFeatures = featDef.getNumberOfShortFeatures();
		int numContiniousFeatures = featDef.getNumberOfContinuousFeatures();
		byte[] byteFeatures = new byte[numByteFeatures];
		short[] shortFeatures = new short[numShortFeatures];
		float[] floatFeatures = new float[numContiniousFeatures];
		int byteCount = 0;
		int shortCount = 0;
		int floatCount = 0;

		for (int i = 0; i < numFeatures; i++) {

			String featName = featDef.getFeatureName(i);
			String featValue = "0";

			if (featDef.isByteFeature(featName) || featDef.isShortFeature(featName)) {
				if (domElement.hasAttribute(featName)) {
					featValue = domElement.getAttribute(featName);
				}

				boolean hasFeature = featDef.hasFeatureValue(featName, featValue);
				if (!hasFeature)
					featValue = "0";

				if (featDef.isByteFeature(i)) {
					byteFeatures[byteCount++] = featDef.getFeatureValueAsByte(i, featValue);
				} else if (featDef.isShortFeature(i)) {
					shortFeatures[shortCount++] = featDef.getFeatureValueAsShort(i, featValue);
				}
			} else {
				if (domElement.hasAttribute("meaning")) {
					featValue = domElement.getAttribute("meaning");
				}
				// float contFeature = getMeaningScaleValue ( featName, featValue );
				floatFeatures[floatCount++] = getMeaningScaleValue(featName, featValue);
			}
		}

		FeatureVector newFV = featDef.toFeatureVector(0, byteFeatures, shortFeatures, floatFeatures);

		String name = "0";
		if (domElement.hasAttribute("name")) {
			name = domElement.getAttribute("name");
		}

		Target newTarget = new Target(name, domElement);
		newTarget.setFeatureVector(newFV);

		return newTarget;
	}

	/**
	 * create F0 target from XML request
	 * 
	 * @param domElement
	 *            xml request for intonation
	 * @return Target target represents xml request
	 */
	private Target createIntonationTarget(Element domElement) {

		// FeatureDefinition featDef = this.featureFileReader.getFeatureDefinition();
		FeatureDefinition featDef = this.f0FeatureDefinition;
		int numFeatures = featDef.getNumberOfFeatures();
		int numByteFeatures = featDef.getNumberOfByteFeatures();
		int numShortFeatures = featDef.getNumberOfShortFeatures();
		int numContiniousFeatures = featDef.getNumberOfContinuousFeatures();
		byte[] byteFeatures = new byte[numByteFeatures];
		short[] shortFeatures = new short[numShortFeatures];
		float[] floatFeatures = new float[numContiniousFeatures];
		int byteCount = 0;
		int shortCount = 0;
		int floatCount = 0;

		for (int i = 0; i < numFeatures; i++) {

			String featName = featDef.getFeatureName(i);
			String featValue = "0";

			if (featDef.isByteFeature(featName) || featDef.isShortFeature(featName)) {
				if (domElement.hasAttribute(featName)) {
					featValue = domElement.getAttribute(featName);
				}

				boolean hasFeature = featDef.hasFeatureValue(featName, featValue);
				if (!hasFeature)
					featValue = "0";

				if (featDef.isByteFeature(i)) {
					byteFeatures[byteCount++] = featDef.getFeatureValueAsByte(i, featValue);
				} else if (featDef.isShortFeature(i)) {
					shortFeatures[shortCount++] = featDef.getFeatureValueAsShort(i, featValue);
				}
			} else {
				if (domElement.hasAttribute("meaning")) {
					featValue = domElement.getAttribute("meaning");
				}
				// float contFeature = getMeaningScaleValue ( featName, featValue );
				floatFeatures[floatCount++] = getMeaningScaleValue(featName, featValue);
			}
		}

		FeatureVector newFV = featDef.toFeatureVector(0, byteFeatures, shortFeatures, floatFeatures);

		String name = "0";
		if (domElement.hasAttribute("name")) {
			name = domElement.getAttribute("name");
		}

		Target newTarget = new Target(name, domElement);
		newTarget.setFeatureVector(newFV);

		return newTarget;
	}

	/**
	 * get value on meaning scale as a float value
	 * 
	 * @param featureName
	 *            feature names
	 * @param meaningAttribute
	 *            meaning attribute
	 * @return a float value for a meaning feature
	 */
	private float getMeaningScaleValue(String featureName, String meaningAttribute) {

		String[] categories = meaningAttribute.split("\\s+");
		List<String> categoriesList = Arrays.asList(categories);

		if ("anger".equals(featureName) && categoriesList.contains("anger")) {
			return 5;
		} else if ("sadness".equals(featureName) && categoriesList.contains("sadness")) {
			return 5;
		} else if ("amusement".equals(featureName) && categoriesList.contains("amusement")) {
			return 5;
		} else if ("happiness".equals(featureName) && categoriesList.contains("happiness")) {
			return 5;
		} else if ("contempt".equals(featureName) && categoriesList.contains("contempt")) {
			return 5;
		} else if ("certain".equals(featureName) && categoriesList.contains("uncertain")) {
			return -2;
		} else if ("certain".equals(featureName) && categoriesList.contains("certain")) {
			return 2;
		} else if ("agreeing".equals(featureName) && categoriesList.contains("disagreeing")) {
			return -2;
		} else if ("agreeing".equals(featureName) && categoriesList.contains("agreeing")) {
			return 2;
		} else if ("interested".equals(featureName) && categoriesList.contains("uninterested")) {
			return -2;
		} else if ("interested".equals(featureName) && categoriesList.contains("interested")) {
			return 2;
		} else if ("anticipation".equals(featureName) && categoriesList.contains("low-anticipation")) {
			return -2;
		} else if ("anticipation".equals(featureName) && categoriesList.contains("anticipation")) {
			return 2;
		} else if ("anticipation".equals(featureName) && categoriesList.contains("high-anticipation")) {
			return 2;
		} else if ("solidarity".equals(featureName) && categoriesList.contains("solidarity")) {
			return 5;
		} else if ("solidarity".equals(featureName) && categoriesList.contains("low-solidarity")) {
			return 1;
		} else if ("solidarity".equals(featureName) && categoriesList.contains("high-solidarity")) {
			return 5;
		} else if ("antagonism".equals(featureName) && categoriesList.contains("antagonism")) {
			return 5;
		} else if ("antagonism".equals(featureName) && categoriesList.contains("high-antagonism")) {
			return 5;
		} else if ("antagonism".equals(featureName) && categoriesList.contains("low-antagonism")) {
			return 1;
		}

		return Float.NaN;
	}
}
