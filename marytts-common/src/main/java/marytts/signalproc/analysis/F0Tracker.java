/**
 * Copyright 2004-2006 DFKI GmbH.
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
package marytts.signalproc.analysis;

import java.util.ArrayList;
import java.util.List;

import marytts.signalproc.window.Window;
import marytts.util.data.DoubleDataSource;

/**
 * 
 * @author Marc Schr&ouml;der
 * 
 *         A common basis for F0 tracking algorithms. The following main steps are assumed: 1. preprocessing of the signal 2.
 *         estimation of candidates for F0 3. selection of a path through the candidates 4. post-processing of the F0 contour
 * 
 */
public abstract class F0Tracker {
	public static final int DEFAULT_MINF0 = 70;
	public static final int DEFAULT_MAXF0 = 700;
	protected TransitionCost transitionCost;

	public F0Tracker() {
		this.transitionCost = getTransitionCost();
	}

	public F0Contour analyse(DoubleDataSource signal, int samplingRate) {
		DoubleDataSource preprocessedSignal = preprocess(signal);
		FrameBasedAnalyser candidateEstimator = getCandidateEstimator(preprocessedSignal, samplingRate);
		// Go through the frames, collect the candidates, and then find the best path through the
		// candidates
		F0Contour f0 = new F0Contour(transitionCost, candidateEstimator.getFrameShiftTime());
		FrameBasedAnalyser.FrameAnalysisResult oneResult;
		while ((oneResult = candidateEstimator.analyseNextFrame()) != null) {
			f0.addFrameAnalysis((F0Candidate[]) oneResult.get());
		}
		f0.findPath();
		return f0;
	}

	protected abstract DoubleDataSource preprocess(DoubleDataSource signal);

	protected abstract FrameBasedAnalyser getCandidateEstimator(DoubleDataSource preprocessedSignal, int samplingRate);

	protected abstract TransitionCost getTransitionCost();

	public class F0Candidate {
		protected double frequency;
		protected double score;

		/**
		 * Create a default F0 candidate, representing the option "unvoiced", score 0.
		 */
		protected F0Candidate() {
			frequency = Double.NaN;
			score = 0;
		}

		protected F0Candidate(double frequency, double score) {
			this.frequency = frequency;
			this.score = score;
		}

		public boolean betterThan(F0Candidate other) {
			return this.score > other.score;
		}
	}

	public abstract class TransitionCost {
		protected TransitionCost() {
		}

		protected abstract double getCost(F0Candidate a, F0Candidate b);
	}

	public abstract class CandidateEstimator extends FrameBasedAnalyser {
		protected int nCandidates;

		public CandidateEstimator(DoubleDataSource signal, Window window, int frameShift, int samplingRate, int nCandidates) {
			super(signal, window, frameShift, samplingRate);
			this.nCandidates = nCandidates;
		}

		/**
		 * Apply this FrameBasedAnalyser to the given data.
		 * 
		 * @param frame
		 *            the data to analyse, which must be of the length prescribed by this FrameBasedAnalyser, i.e. by
		 *            {@link #getFrameLengthSamples()}.
		 * @return an array of F0Candidates
		 * @throws IllegalArgumentException
		 *             if frame does not have the prescribed length
		 */
		public Object analyse(double[] frame) {
			if (frame.length != getFrameLengthSamples())
				throw new IllegalArgumentException("Expected frame of length " + getFrameLengthSamples() + ", got "
						+ frame.length);
			F0Candidate[] candidates = new F0Candidate[nCandidates];
			candidates[0] = new F0Candidate(); // default = unvoiced
			findCandidates(candidates, frame);
			normaliseCandidatesScores(candidates);
			return candidates;
		}

		protected abstract void findCandidates(F0Candidate[] candidates, double[] frame);

		protected void normaliseCandidatesScores(F0Candidate[] candidates) {
			assert candidates != null;
			int iBest = 0;
			for (int i = 0; i < candidates.length; i++) {
				if (candidates[i] == null)
					break;
				else if (candidates[i].betterThan(candidates[iBest]))
					iBest = i;
			}
			double bestScore = candidates[iBest].score;
			if (bestScore == 0)
				return;
			// Normalise scores relative to best score:
			for (int i = 0; i < candidates.length; i++) {
				if (candidates[i] == null)
					break;
				candidates[i].score /= bestScore;
			}
		}

		protected void addCandidate(F0Candidate[] candidates, F0Candidate newCandidate) {
			// If there is still space left in candidates, simply add the new candidate;
			// else, replace the weakest candidate with the new one if the new one is better.
			int iWorst = 0;
			for (int i = 0; i < candidates.length; i++) {
				if (candidates[i] == null) {
					candidates[i] = newCandidate;
					return;
				} else if (candidates[iWorst].betterThan(candidates[i])) {
					iWorst = i;
				}
			}
			if (newCandidate.betterThan(candidates[iWorst])) {
				candidates[iWorst] = newCandidate;
			}
		}
	}

	public class F0Contour {
		protected List candidateLattice;
		protected double[] contour;
		protected TransitionCost transitionCost;
		protected double frameShiftTime;

		protected F0Contour(TransitionCost transitionCost, double frameShiftTime) {
			candidateLattice = new ArrayList();
			contour = null;
			this.transitionCost = transitionCost;
			this.frameShiftTime = frameShiftTime;
		}

		public F0Contour(String ptcFile) {

		}

		protected void addFrameAnalysis(F0Candidate[] candidates) {
			candidateLattice.add(candidates);
		}

		protected void findPath() {
			assert candidateLattice != null;
			assert contour == null;
			contour = new double[candidateLattice.size()];
			for (int i = 0; i < contour.length; i++) {
				contour[i] = getBest(i).frequency;
			}
		}

		protected F0Candidate getBest(int index) {
			F0Candidate[] candidates = (F0Candidate[]) candidateLattice.get(index);
			assert candidates.length >= 1;
			int iBest = 0;
			for (int i = 0; i < candidates.length; i++) {
				if (candidates[i] == null)
					break;
				if (candidates[i].betterThan(candidates[iBest]))
					iBest = i;
			}
			return candidates[iBest];
		}

		public double[] getContour() {
			return contour;
		}

		public double getFrameShiftTime() {
			return frameShiftTime;
		}
	}
}
