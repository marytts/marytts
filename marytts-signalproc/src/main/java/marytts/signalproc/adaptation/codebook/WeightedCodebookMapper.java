/**
 * Copyright 2007 DFKI GmbH.
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
package marytts.signalproc.adaptation.codebook;

import java.util.Arrays;

import marytts.signalproc.adaptation.Context;
import marytts.signalproc.adaptation.VocalTractTransformationFunction;
import marytts.signalproc.analysis.distance.DistanceComputer;
import marytts.util.math.MathUtils;

/**
 * This class performs mapping of acoustic features to be transformed to the codebook entries
 * 
 * @author Oytun T&uuml;rk
 */
public class WeightedCodebookMapper extends VocalTractTransformationFunction {
	public WeightedCodebookMapperParams mapperParams;
	private double[] bestMatchDists;
	private int[] bestMatchIndices; // indices in codebook
	private int[] sortedIndicesOfBestMatchIndices; // indices in array bestMatchIndices
	double[] weights;
	public WeightedCodebookLsfMatch returnedMatch;

	public WeightedCodebookMapper(WeightedCodebookMapperParams mp) {
		mapperParams = new WeightedCodebookMapperParams(mp);

		if (mapperParams.numBestMatches > 0) {
			bestMatchDists = new double[mapperParams.numBestMatches];
			bestMatchIndices = new int[mapperParams.numBestMatches];
			sortedIndicesOfBestMatchIndices = new int[mapperParams.numBestMatches];
		} else {
			bestMatchDists = null;
			bestMatchIndices = null;
			sortedIndicesOfBestMatchIndices = null;
		}

		returnedMatch = null;
	}

	// Simple phone based selection
	public int[] preselect(Context currentContext, WeightedCodebook codebook, boolean isMatchUsingTargetCodebook,
			int minimumCandidates) {
		double[] scores = new double[codebook.header.totalEntries];
		int[] indices = new int[codebook.header.totalEntries];
		int total = 0;
		int i, j;

		if (!isMatchUsingTargetCodebook) {
			for (i = 0; i < codebook.entries.length; i++)
				scores[i] = currentContext.matchScore(codebook.entries[i].sourceItem.context);
		} else {
			for (i = 0; i < codebook.entries.length; i++)
				scores[i] = currentContext.matchScore(codebook.entries[i].targetItem.context);
		}

		double[] possibleScores = currentContext.getPossibleScores();

		total = 0;
		for (i = 0; i < possibleScores.length; i++) {
			for (j = 0; j < scores.length; j++) {
				if (scores[j] == possibleScores[i]) {
					indices[total] = j;
					total++;
				}
			}

			if (total >= minimumCandidates)
				break;
		}

		if (total < minimumCandidates) {
			for (i = 0; i < codebook.entries.length; i++)
				indices[i] = i;
		}

		return indices;
	}

	public WeightedCodebookLsfMatch transform(double[] inputLsfs, WeightedCodebook codebook,
			boolean isVocalTractMatchUsingTargetCodebook, int[] preselectedIndices) {
		double currentDist;
		double worstBestDist = -1.0;
		int worstBestDistInd = 0;
		int i;

		if (mapperParams.distanceMeasure == WeightedCodebookMapperParams.LSF_EUCLIDEAN_DISTANCE) {
			// for (i=0; i<codebook.header.totalLsfEntries; i++)
			for (i = 0; i < preselectedIndices.length; i++) {
				if (i >= mapperParams.numBestMatches) {
					if (!isVocalTractMatchUsingTargetCodebook)
						currentDist = DistanceComputer.getEuclideanDistance(inputLsfs,
								codebook.entries[preselectedIndices[i]].sourceItem.lsfs);
					else
						currentDist = DistanceComputer.getEuclideanDistance(inputLsfs,
								codebook.entries[preselectedIndices[i]].targetItem.lsfs);

					if (currentDist < worstBestDist) {
						bestMatchDists[worstBestDistInd] = currentDist;
						bestMatchIndices[worstBestDistInd] = preselectedIndices[i];
						worstBestDistInd = MathUtils.getMaxIndex(bestMatchDists);
						worstBestDist = bestMatchDists[worstBestDistInd];
					}
				} else {
					if (!isVocalTractMatchUsingTargetCodebook)
						bestMatchDists[i] = DistanceComputer.getEuclideanDistance(inputLsfs,
								codebook.entries[preselectedIndices[i]].sourceItem.lsfs);
					else
						bestMatchDists[i] = DistanceComputer.getEuclideanDistance(inputLsfs,
								codebook.entries[preselectedIndices[i]].targetItem.lsfs);

					bestMatchIndices[i] = preselectedIndices[i];

					if (i == 0 || bestMatchDists[i] > worstBestDist) {
						worstBestDist = bestMatchDists[i];
						worstBestDistInd = i;
					}
				}
			}
		} else if (mapperParams.distanceMeasure == WeightedCodebookMapperParams.LSF_INVERSE_HARMONIC_DISTANCE) {
			for (i = 0; i < preselectedIndices.length; i++) {
				if (i >= mapperParams.numBestMatches) {
					if (!isVocalTractMatchUsingTargetCodebook)
						currentDist = DistanceComputer.getLsfInverseHarmonicDistance(inputLsfs,
								codebook.entries[preselectedIndices[i]].sourceItem.lsfs, mapperParams.freqRange);
					else
						currentDist = DistanceComputer.getLsfInverseHarmonicDistance(inputLsfs,
								codebook.entries[preselectedIndices[i]].targetItem.lsfs, mapperParams.freqRange);

					if (currentDist < worstBestDist) {
						bestMatchDists[worstBestDistInd] = currentDist;
						bestMatchIndices[worstBestDistInd] = preselectedIndices[i];
						worstBestDistInd = MathUtils.getMaxIndex(bestMatchDists);
						worstBestDist = bestMatchDists[worstBestDistInd];
					}
				} else {
					if (!isVocalTractMatchUsingTargetCodebook)
						bestMatchDists[i] = DistanceComputer.getLsfInverseHarmonicDistance(inputLsfs,
								codebook.entries[preselectedIndices[i]].sourceItem.lsfs, mapperParams.freqRange);
					else
						bestMatchDists[i] = DistanceComputer.getLsfInverseHarmonicDistance(inputLsfs,
								codebook.entries[preselectedIndices[i]].targetItem.lsfs, mapperParams.freqRange);

					bestMatchIndices[i] = preselectedIndices[i];

					if (i == 0 || bestMatchDists[i] > worstBestDist) {
						worstBestDist = bestMatchDists[i];
						worstBestDistInd = i;
					}
				}
			}
		} else if (mapperParams.distanceMeasure == WeightedCodebookMapperParams.LSF_INVERSE_HARMONIC_DISTANCE_SYMMETRIC) {
			for (i = 0; i < preselectedIndices.length; i++) {
				if (i >= mapperParams.numBestMatches) {
					if (!isVocalTractMatchUsingTargetCodebook)
						currentDist = DistanceComputer.getLsfInverseHarmonicDistanceSymmetric(inputLsfs,
								codebook.entries[preselectedIndices[i]].sourceItem.lsfs, mapperParams.alphaForSymmetric,
								mapperParams.freqRange);
					else
						currentDist = DistanceComputer.getLsfInverseHarmonicDistanceSymmetric(inputLsfs,
								codebook.entries[preselectedIndices[i]].targetItem.lsfs, mapperParams.alphaForSymmetric,
								mapperParams.freqRange);

					if (currentDist < worstBestDist) {
						bestMatchDists[worstBestDistInd] = currentDist;
						bestMatchIndices[worstBestDistInd] = preselectedIndices[i];
						worstBestDistInd = MathUtils.getMaxIndex(bestMatchDists);
						worstBestDist = bestMatchDists[worstBestDistInd];
					}
				} else {
					if (!isVocalTractMatchUsingTargetCodebook)
						bestMatchDists[i] = DistanceComputer.getLsfInverseHarmonicDistanceSymmetric(inputLsfs,
								codebook.entries[preselectedIndices[i]].sourceItem.lsfs, mapperParams.alphaForSymmetric,
								mapperParams.freqRange);
					else
						bestMatchDists[i] = DistanceComputer.getLsfInverseHarmonicDistanceSymmetric(inputLsfs,
								codebook.entries[preselectedIndices[i]].targetItem.lsfs, mapperParams.alphaForSymmetric,
								mapperParams.freqRange);

					bestMatchIndices[i] = preselectedIndices[i];

					if (i == 0 || bestMatchDists[i] > worstBestDist) {
						worstBestDist = bestMatchDists[i];
						worstBestDistInd = i;
					}
				}
			}
		} else if (mapperParams.distanceMeasure == WeightedCodebookMapperParams.LSF_MAHALANOBIS_DISTANCE) {
			double[][] inverseCovarianceMatrix = null;

			for (i = 0; i < preselectedIndices.length; i++) {
				if (i >= mapperParams.numBestMatches) {
					if (!isVocalTractMatchUsingTargetCodebook)
						currentDist = DistanceComputer.getMahalanobisDistance(inputLsfs,
								codebook.entries[preselectedIndices[i]].sourceItem.lsfs, inverseCovarianceMatrix);
					else
						currentDist = DistanceComputer.getMahalanobisDistance(inputLsfs,
								codebook.entries[preselectedIndices[i]].targetItem.lsfs, inverseCovarianceMatrix);

					if (currentDist < worstBestDist) {
						bestMatchDists[worstBestDistInd] = currentDist;
						bestMatchIndices[worstBestDistInd] = preselectedIndices[i];
						worstBestDistInd = MathUtils.getMaxIndex(bestMatchDists);
						worstBestDist = bestMatchDists[worstBestDistInd];
					}
				} else {
					if (!isVocalTractMatchUsingTargetCodebook)
						bestMatchDists[i] = DistanceComputer.getMahalanobisDistance(inputLsfs,
								codebook.entries[preselectedIndices[i]].sourceItem.lsfs, inverseCovarianceMatrix);
					else
						bestMatchDists[i] = DistanceComputer.getMahalanobisDistance(inputLsfs,
								codebook.entries[preselectedIndices[i]].targetItem.lsfs, inverseCovarianceMatrix);

					bestMatchIndices[i] = preselectedIndices[i];

					if (i == 0 || bestMatchDists[i] > worstBestDist) {
						worstBestDist = bestMatchDists[i];
						worstBestDistInd = i;
					}
				}
			}
		} else if (mapperParams.distanceMeasure == WeightedCodebookMapperParams.LSF_ABSOLUTE_VALUE_DISTANCE) {

			for (i = 0; i < preselectedIndices.length; i++) {
				if (i >= mapperParams.numBestMatches) {
					if (!isVocalTractMatchUsingTargetCodebook)
						currentDist = DistanceComputer.getAbsoluteValueDistance(inputLsfs,
								codebook.entries[preselectedIndices[i]].sourceItem.lsfs);
					else
						currentDist = DistanceComputer.getAbsoluteValueDistance(inputLsfs,
								codebook.entries[preselectedIndices[i]].targetItem.lsfs);

					if (currentDist < worstBestDist) {
						bestMatchDists[worstBestDistInd] = currentDist;
						bestMatchIndices[worstBestDistInd] = preselectedIndices[i];
						worstBestDistInd = MathUtils.getMaxIndex(bestMatchDists);
						worstBestDist = bestMatchDists[worstBestDistInd];
					}
				} else {
					if (!isVocalTractMatchUsingTargetCodebook)
						bestMatchDists[i] = DistanceComputer.getAbsoluteValueDistance(inputLsfs,
								codebook.entries[preselectedIndices[i]].sourceItem.lsfs);
					else
						bestMatchDists[i] = DistanceComputer.getAbsoluteValueDistance(inputLsfs,
								codebook.entries[preselectedIndices[i]].targetItem.lsfs);

					bestMatchIndices[i] = preselectedIndices[i];

					if (i == 0 || bestMatchDists[i] > worstBestDist) {
						worstBestDist = bestMatchDists[i];
						worstBestDistInd = i;
					}
				}
			}
		} else
			return null;

		// Get sorted indices of best distances (i.e. lowests) and perform weighting
		// Note that bestMatchDists is not actually sorted, only sorted indices returned!
		sortedIndicesOfBestMatchIndices = MathUtils.quickSort(bestMatchDists, 0,
				Math.min(mapperParams.numBestMatches, codebook.header.totalEntries) - 1);

		// bestMatchIndices[sortedIndicesOfBestMatchIndices[0]] is the best matching codebook entry index
		// bestMatchDists[0] is the best score
		// codebook.entries[bestMatchIndices[sortedIndicesOfBestMatchIndices[0]]] are the source and target lsf set of the best
		// match

		weights = getWeights(bestMatchDists, Math.min(mapperParams.numBestMatches, codebook.header.totalEntries),
				mapperParams.weightingMethod, mapperParams.weightingSteepness);

		int j;
		String strBestIndices = "";
		String strWeights = "";

		returnedMatch = new WeightedCodebookLsfMatch(Math.min(mapperParams.numBestMatches, codebook.header.totalEntries),
				mapperParams.lpOrder);
		Arrays.fill(returnedMatch.entry.sourceItem.lsfs, 0.0);
		Arrays.fill(returnedMatch.entry.targetItem.lsfs, 0.0);

		for (i = 0; i < returnedMatch.totalMatches; i++) {
			returnedMatch.weights[i] = weights[i];
			returnedMatch.indices[i] = bestMatchIndices[sortedIndicesOfBestMatchIndices[i]];

			for (j = 0; j < mapperParams.lpOrder; j++) {
				returnedMatch.entry.sourceItem.lsfs[j] += returnedMatch.weights[i]
						* codebook.entries[returnedMatch.indices[i]].sourceItem.lsfs[j];
				returnedMatch.entry.targetItem.lsfs[j] += returnedMatch.weights[i]
						* codebook.entries[returnedMatch.indices[i]].targetItem.lsfs[j];
			}

			strBestIndices += String.valueOf(returnedMatch.indices[i]) + " ";
			strWeights += String.valueOf(returnedMatch.weights[i]) + " ";

			if (i > 0 && weights[i] > weights[i - 1])
				System.out.println("Weight should be less than prev weight!!!");
		}

		System.out.println("Best entry indices = " + strBestIndices + " with weights = " + strWeights);

		return returnedMatch;
	}

	public WeightedCodebookLsfMatch transform(double[] inputLsfs, WeightedCodebook codebook,
			boolean isVocalTractMatchUsingTargetCodebook) {
		int[] allIndices = new int[codebook.entries.length];
		for (int i = 0; i < allIndices.length; i++)
			allIndices[i] = i;

		return transform(inputLsfs, codebook, isVocalTractMatchUsingTargetCodebook, allIndices);
	}

	public WeightedCodebookLsfMatch transformOld(double[] inputLsfs, WeightedCodebook codebook,
			boolean isVocalTractMatchUsingTargetCodebook) {
		double currentDist;
		double worstBestDist = -1.0;
		int worstBestDistInd = 0;
		int i;

		if (mapperParams.distanceMeasure == WeightedCodebookMapperParams.LSF_EUCLIDEAN_DISTANCE) {
			for (i = 0; i < codebook.header.totalEntries; i++) {
				if (i >= mapperParams.numBestMatches) {
					if (!isVocalTractMatchUsingTargetCodebook)
						currentDist = DistanceComputer.getEuclideanDistance(inputLsfs, codebook.entries[i].sourceItem.lsfs);
					else
						currentDist = DistanceComputer.getEuclideanDistance(inputLsfs, codebook.entries[i].targetItem.lsfs);

					if (currentDist < worstBestDist) {
						bestMatchDists[worstBestDistInd] = currentDist;
						bestMatchIndices[worstBestDistInd] = i;
						worstBestDistInd = MathUtils.getMaxIndex(bestMatchDists);
						worstBestDist = bestMatchDists[worstBestDistInd];
					}
				} else {
					if (!isVocalTractMatchUsingTargetCodebook)
						bestMatchDists[i] = DistanceComputer.getEuclideanDistance(inputLsfs, codebook.entries[i].sourceItem.lsfs);
					else
						bestMatchDists[i] = DistanceComputer.getEuclideanDistance(inputLsfs, codebook.entries[i].targetItem.lsfs);

					bestMatchIndices[i] = i;

					if (i == 0 || bestMatchDists[i] > worstBestDist) {
						worstBestDist = bestMatchDists[i];
						worstBestDistInd = i;
					}
				}
			}
		} else if (mapperParams.distanceMeasure == WeightedCodebookMapperParams.LSF_INVERSE_HARMONIC_DISTANCE) {
			for (i = 0; i < codebook.header.totalEntries; i++) {
				if (i >= mapperParams.numBestMatches) {
					if (!isVocalTractMatchUsingTargetCodebook)
						currentDist = DistanceComputer.getLsfInverseHarmonicDistance(inputLsfs,
								codebook.entries[i].sourceItem.lsfs, mapperParams.freqRange);
					else
						currentDist = DistanceComputer.getLsfInverseHarmonicDistance(inputLsfs,
								codebook.entries[i].targetItem.lsfs, mapperParams.freqRange);

					if (currentDist < worstBestDist) {
						bestMatchDists[worstBestDistInd] = currentDist;
						bestMatchIndices[worstBestDistInd] = i;
						worstBestDistInd = MathUtils.getMaxIndex(bestMatchDists);
						worstBestDist = bestMatchDists[worstBestDistInd];
					}
				} else {
					if (!isVocalTractMatchUsingTargetCodebook)
						bestMatchDists[i] = DistanceComputer.getLsfInverseHarmonicDistance(inputLsfs,
								codebook.entries[i].sourceItem.lsfs, mapperParams.freqRange);
					else
						bestMatchDists[i] = DistanceComputer.getLsfInverseHarmonicDistance(inputLsfs,
								codebook.entries[i].targetItem.lsfs, mapperParams.freqRange);

					bestMatchIndices[i] = i;

					if (i == 0 || bestMatchDists[i] > worstBestDist) {
						worstBestDist = bestMatchDists[i];
						worstBestDistInd = i;
					}
				}
			}
		} else if (mapperParams.distanceMeasure == WeightedCodebookMapperParams.LSF_INVERSE_HARMONIC_DISTANCE_SYMMETRIC) {
			for (i = 0; i < codebook.header.totalEntries; i++) {
				if (i >= mapperParams.numBestMatches) {
					if (!isVocalTractMatchUsingTargetCodebook)
						currentDist = DistanceComputer.getLsfInverseHarmonicDistanceSymmetric(inputLsfs,
								codebook.entries[i].sourceItem.lsfs, mapperParams.alphaForSymmetric, mapperParams.freqRange);
					else
						currentDist = DistanceComputer.getLsfInverseHarmonicDistanceSymmetric(inputLsfs,
								codebook.entries[i].targetItem.lsfs, mapperParams.alphaForSymmetric, mapperParams.freqRange);

					if (currentDist < worstBestDist) {
						bestMatchDists[worstBestDistInd] = currentDist;
						bestMatchIndices[worstBestDistInd] = i;
						worstBestDistInd = MathUtils.getMaxIndex(bestMatchDists);
						worstBestDist = bestMatchDists[worstBestDistInd];
					}
				} else {
					if (!isVocalTractMatchUsingTargetCodebook)
						bestMatchDists[i] = DistanceComputer.getLsfInverseHarmonicDistanceSymmetric(inputLsfs,
								codebook.entries[i].sourceItem.lsfs, mapperParams.alphaForSymmetric, mapperParams.freqRange);
					else
						bestMatchDists[i] = DistanceComputer.getLsfInverseHarmonicDistanceSymmetric(inputLsfs,
								codebook.entries[i].targetItem.lsfs, mapperParams.alphaForSymmetric, mapperParams.freqRange);

					bestMatchIndices[i] = i;

					if (i == 0 || bestMatchDists[i] > worstBestDist) {
						worstBestDist = bestMatchDists[i];
						worstBestDistInd = i;
					}
				}
			}
		} else if (mapperParams.distanceMeasure == WeightedCodebookMapperParams.LSF_MAHALANOBIS_DISTANCE) {
			double[][] inverseCovarianceMatrix = null;

			for (i = 0; i < codebook.header.totalEntries; i++) {
				if (i >= mapperParams.numBestMatches) {
					if (!isVocalTractMatchUsingTargetCodebook)
						currentDist = DistanceComputer.getMahalanobisDistance(inputLsfs, codebook.entries[i].sourceItem.lsfs,
								inverseCovarianceMatrix);
					else
						currentDist = DistanceComputer.getMahalanobisDistance(inputLsfs, codebook.entries[i].targetItem.lsfs,
								inverseCovarianceMatrix);

					if (currentDist < worstBestDist) {
						bestMatchDists[worstBestDistInd] = currentDist;
						bestMatchIndices[worstBestDistInd] = i;
						worstBestDistInd = MathUtils.getMaxIndex(bestMatchDists);
						worstBestDist = bestMatchDists[worstBestDistInd];
					}
				} else {
					if (!isVocalTractMatchUsingTargetCodebook)
						bestMatchDists[i] = DistanceComputer.getMahalanobisDistance(inputLsfs,
								codebook.entries[i].sourceItem.lsfs, inverseCovarianceMatrix);
					else
						bestMatchDists[i] = DistanceComputer.getMahalanobisDistance(inputLsfs,
								codebook.entries[i].targetItem.lsfs, inverseCovarianceMatrix);

					bestMatchIndices[i] = i;

					if (i == 0 || bestMatchDists[i] > worstBestDist) {
						worstBestDist = bestMatchDists[i];
						worstBestDistInd = i;
					}
				}
			}
		} else if (mapperParams.distanceMeasure == WeightedCodebookMapperParams.LSF_ABSOLUTE_VALUE_DISTANCE) {

			for (i = 0; i < codebook.header.totalEntries; i++) {
				if (i >= mapperParams.numBestMatches) {
					if (!isVocalTractMatchUsingTargetCodebook)
						currentDist = DistanceComputer.getAbsoluteValueDistance(inputLsfs, codebook.entries[i].sourceItem.lsfs);
					else
						currentDist = DistanceComputer.getAbsoluteValueDistance(inputLsfs, codebook.entries[i].targetItem.lsfs);

					if (currentDist < worstBestDist) {
						bestMatchDists[worstBestDistInd] = currentDist;
						bestMatchIndices[worstBestDistInd] = i;
						worstBestDistInd = MathUtils.getMaxIndex(bestMatchDists);
						worstBestDist = bestMatchDists[worstBestDistInd];
					}
				} else {
					if (!isVocalTractMatchUsingTargetCodebook)
						bestMatchDists[i] = DistanceComputer.getAbsoluteValueDistance(inputLsfs,
								codebook.entries[i].sourceItem.lsfs);
					else
						bestMatchDists[i] = DistanceComputer.getAbsoluteValueDistance(inputLsfs,
								codebook.entries[i].targetItem.lsfs);

					bestMatchIndices[i] = i;

					if (i == 0 || bestMatchDists[i] > worstBestDist) {
						worstBestDist = bestMatchDists[i];
						worstBestDistInd = i;
					}
				}
			}
		} else
			return null;

		// Get sorted indices of best distances (i.e. lowests) and perform weighting
		// Note that bestMatchDists is not actually sorted, only sorted indices returned!
		sortedIndicesOfBestMatchIndices = MathUtils.quickSort(bestMatchDists, 0,
				Math.min(mapperParams.numBestMatches, codebook.header.totalEntries) - 1);

		// bestMatchIndices[sortedIndicesOfBestMatchIndices[0]] is the best matching codebook entry index
		// bestMatchDists[0] is the best score
		// codebook.entries[bestMatchIndices[sortedIndicesOfBestMatchIndices[0]]] are the source and target lsf set of the best
		// match

		weights = getWeights(bestMatchDists, Math.min(mapperParams.numBestMatches, codebook.header.totalEntries),
				mapperParams.weightingMethod, mapperParams.weightingSteepness);

		int j;
		String strBestIndices = "";
		String strWeights = "";

		returnedMatch = new WeightedCodebookLsfMatch(Math.min(mapperParams.numBestMatches, codebook.header.totalEntries),
				mapperParams.lpOrder);
		Arrays.fill(returnedMatch.entry.sourceItem.lsfs, 0.0);
		Arrays.fill(returnedMatch.entry.targetItem.lsfs, 0.0);

		for (i = 0; i < returnedMatch.totalMatches; i++) {
			returnedMatch.weights[i] = weights[i];
			returnedMatch.indices[i] = bestMatchIndices[sortedIndicesOfBestMatchIndices[i]];

			for (j = 0; j < mapperParams.lpOrder; j++) {
				returnedMatch.entry.sourceItem.lsfs[j] += returnedMatch.weights[i]
						* codebook.entries[returnedMatch.indices[i]].sourceItem.lsfs[j];
				returnedMatch.entry.targetItem.lsfs[j] += returnedMatch.weights[i]
						* codebook.entries[returnedMatch.indices[i]].targetItem.lsfs[j];
			}

			strBestIndices += String.valueOf(returnedMatch.indices[i]) + " ";
			strWeights += String.valueOf(returnedMatch.weights[i]) + " ";

			if (i > 0 && weights[i] > weights[i - 1])
				System.out.println("Weight should be less than prev weight!!!");
		}

		System.out.println("Best entry indices = " + strBestIndices + " with weights = " + strWeights);

		return returnedMatch;
	}

	public static double[] getWeights(double[] bestDistances, int numBestDistances, int weightingMethod, double steepness) {
		double[] outputWeights = MathUtils.normalizeToRange(bestDistances, numBestDistances, 0.0, Math.max(1.0, steepness + 1.0));

		if (weightingMethod == WeightedCodebookMapperParams.EXPONENTIAL_HALF_WINDOW) {
			for (int i = 0; i < outputWeights.length; i++)
				outputWeights[i] = Math.exp(-steepness * outputWeights[i]);
		} else if (weightingMethod == WeightedCodebookMapperParams.TRIANGLE_HALF_WINDOW) {
			for (int i = 0; i < outputWeights.length; i++)
				outputWeights[i] = 1.0 / Math.pow(outputWeights[i], i * steepness) + (1.0 + steepness);
		}

		return MathUtils.normalizeToSumUpTo(outputWeights, 1.0);
	}

	public static void main(String[] args) {

	}
}
