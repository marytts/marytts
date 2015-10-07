/**
 * Portions Copyright 2006 DFKI GmbH.
 * Portions Copyright 2001 Sun Microsystems, Inc.
 * Portions Copyright 1999-2001 Language Technologies Institute, 
 * Carnegie Mellon University.
 * All Rights Reserved.  Use is subject to license terms.
 * 
 * Permission is hereby granted, free of charge, to use and distribute
 * this software and its documentation without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of this work, and to
 * permit persons to whom this work is furnished to do so, subject to
 * the following conditions:
 * 
 * 1. The code must retain the above copyright notice, this list of
 *    conditions and the following disclaimer.
 * 2. Any modifications must be clearly marked as such.
 * 3. Original authors' names are not deleted.
 * 4. The authors' names are not used to endorse or promote products
 *    derived from this software without specific prior written
 *    permission.
 *
 * DFKI GMBH AND THE CONTRIBUTORS TO THIS WORK DISCLAIM ALL WARRANTIES WITH
 * REGARD TO THIS SOFTWARE, INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS, IN NO EVENT SHALL DFKI GMBH NOR THE
 * CONTRIBUTORS BE LIABLE FOR ANY SPECIAL, INDIRECT OR CONSEQUENTIAL
 * DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR
 * PROFITS, WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS
 * ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE OF
 * THIS SOFTWARE.
 */
package marytts.unitselection.select.viterbi;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.DecimalFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import marytts.exceptions.SynthesisException;
import marytts.unitselection.data.DiphoneUnit;
import marytts.unitselection.data.Unit;
import marytts.unitselection.data.UnitDatabase;
import marytts.unitselection.select.DiphoneTarget;
import marytts.unitselection.select.JoinCostFunction;
import marytts.unitselection.select.SelectedUnit;
import marytts.unitselection.select.StatisticalCostFunction;
import marytts.unitselection.select.Target;
import marytts.unitselection.select.TargetCostFunction;
import marytts.util.MaryUtils;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**
 * Provides support for the Viterbi Algorithm.
 *
 * Implementation Notes
 * <p>
 * For each candidate for the current unit, calculate the cost between it and the first candidate in the next unit. Save only the
 * path that has the least cost. By default, if two candidates come from units that are adjacent in the database, the cost is 0
 * (i.e., they were spoken together, so they are a perfect match).
 * <p>
 * 
 * Repeat the previous process for each candidate in the next unit, creating a list of least cost paths between the candidates
 * between the current unit and the unit following it.
 * <p>
 * 
 * Toss out all candidates in the current unit that are not included in a path.
 * <p>
 * 
 * Move to the next unit and repeat the process.
 */
public class Viterbi {
	// a general flag indicating which type of viterbi search
	// to use:
	// -1: unlimited search
	// n>0: beam search, retain only the n best paths at each step.
	protected int beamSize;
	protected final float wTargetCosts;
	protected final float wJoinCosts;
	protected final float wSCosts;

	protected ViterbiPoint firstPoint = null;
	protected ViterbiPoint lastPoint = null;
	private UnitDatabase database;
	protected TargetCostFunction targetCostFunction;
	protected JoinCostFunction joinCostFunction;
	protected StatisticalCostFunction sCostFunction;
	protected Logger logger;
	// for debugging, try to get an idea of the average effect of join vs. target costs:
	protected double cumulJoinCosts;
	protected int nJoinCosts;
	protected double cumulTargetCosts;
	protected int nTargetCosts;

	// Keep track of average costs for each voice: map UnitDatabase->DebugStats
	private static Map<UnitDatabase, DebugStats> debugStats = new HashMap<UnitDatabase, DebugStats>();

	/**
	 * Creates a Viterbi class to process the given utterance. A queue of ViterbiPoints corresponding to the Items in the Relation
	 * segs is built up.
	 * 
	 * @param targets
	 *            targets
	 * @param database
	 *            database
	 * @param wTargetCosts
	 *            wTargetCosts
	 * @param beamSize
	 *            beamSize
	 */
	public Viterbi(List<Target> targets, UnitDatabase database, float wTargetCosts, int beamSize) {
		this.database = database;
		this.targetCostFunction = database.getTargetCostFunction();
		this.joinCostFunction = database.getJoinCostFunction();
		this.sCostFunction = database.getSCostFunction();
		this.logger = MaryUtils.getLogger("Viterbi");
		this.wTargetCosts = wTargetCosts;
		wJoinCosts = 1 - wTargetCosts;
		wSCosts = 0;
		this.beamSize = beamSize;
		this.cumulJoinCosts = 0;
		this.nJoinCosts = 0;
		this.cumulTargetCosts = 0;
		this.nTargetCosts = 0;
		ViterbiPoint last = null;
		// for each segment, build a ViterbiPoint
		for (Target target : targets) {
			ViterbiPoint nextPoint = new ViterbiPoint(target);

			if (last != null) { // continue to build up the queue
				last.setNext(nextPoint);
			} else { // firstPoint is the start of the queue
				firstPoint = nextPoint;
				// dummy start path:
				firstPoint.getPaths().add(new ViterbiPath(null, null, 0));
			}
			last = nextPoint;
		}
		// And add one point where the paths from the last candidate can end:
		lastPoint = new ViterbiPoint(null);
		last.setNext(lastPoint);
		if (beamSize == 0) {
			throw new IllegalStateException("General beam search not implemented");
		}
	}

	/**
	 * Creates a Viterbi class to process the given utterance. A queue of ViterbiPoints corresponding to the Items in the Relation
	 * segs is built up.
	 * 
	 * @param targets
	 *            targets
	 * @param database
	 *            database
	 * @param wTargetCosts
	 *            wTargetCosts
	 * @param wSCosts
	 *            wSCosts
	 * @param beamSize
	 *            beamSize
	 */
	public Viterbi(List<Target> targets, UnitDatabase database, float wTargetCosts, float wSCosts, int beamSize) {
		this.database = database;
		this.targetCostFunction = database.getTargetCostFunction();
		this.joinCostFunction = database.getJoinCostFunction();
		this.sCostFunction = database.getSCostFunction();
		this.logger = MaryUtils.getLogger("Viterbi");
		this.wTargetCosts = wTargetCosts;
		this.wSCosts = wSCosts;
		wJoinCosts = 1 - (wTargetCosts + wSCosts);
		this.beamSize = beamSize;
		this.cumulJoinCosts = 0;
		this.nJoinCosts = 0;
		this.cumulTargetCosts = 0;
		this.nTargetCosts = 0;
		ViterbiPoint last = null;
		// for each segment, build a ViterbiPoint
		for (Target target : targets) {
			ViterbiPoint nextPoint = new ViterbiPoint(target);

			if (last != null) { // continue to build up the queue
				last.setNext(nextPoint);
			} else { // firstPoint is the start of the queue
				firstPoint = nextPoint;
				// dummy start path:
				firstPoint.getPaths().add(new ViterbiPath(null, null, 0));
			}
			last = nextPoint;
		}
		// And add one point where the paths from the last candidate can end:
		lastPoint = new ViterbiPoint(null);
		last.setNext(lastPoint);
		if (beamSize == 0) {
			throw new IllegalStateException("General beam search not implemented");
		}
	}

	/**
	 * Carry out a Viterbi search in for a prepared queue of ViterbiPoints. In a nutshell, each Point represents a target item (a
	 * target segment); for each target Point, a number of Candidate units in the voice database are determined; a Path structure
	 * is built up, based on local best transitions. Concretely, a Path consists of a (possibly empty) previous Path, a current
	 * Candidate, and a Score. This Score is a quality measure of the Path; it is calculated as the sum of the previous Path's
	 * score, the Candidate's score, and the Cost of joining the Candidate to the previous Path's Candidate. At each step, only
	 * one Path leading to each Candidate is retained, viz. the Path with the best Score. All that is left to do is to call
	 * result() to get the best-rated path from among the paths associated with the last Point, and to associate the resulting
	 * Candidates with the segment items they will realise.
	 * 
	 * @throws SynthesisException
	 *             if for any part of the target chain, no candidates can be found
	 */
	public void apply() throws SynthesisException {
		logger.debug("Viterbi running with beam size " + beamSize);
		// go through all but the last point
		// (since last point has no item)
		for (ViterbiPoint point = firstPoint; point.next != null; point = point.next) {
			// The candidates for the current item:
			// candidate selection is carried out by UnitSelector
			Target target = point.target;
			List<ViterbiCandidate> candidates = database.getCandidates(target);
			if (candidates.size() == 0) {
				if (target instanceof DiphoneTarget) {
					logger.debug("No diphone '" + target.getName() + "' -- will build from halfphones");
					DiphoneTarget dt = (DiphoneTarget) target;
					// replace diphone viterbi point with two half-phone viterbi points
					Target left = dt.left;
					Target right = dt.right;
					point.setTarget(left);
					ViterbiPoint newP = new ViterbiPoint(right);
					newP.next = point.next;
					point.next = newP;
					candidates = database.getCandidates(left);
					if (candidates.size() == 0)
						throw new SynthesisException("Cannot even find any halfphone unit for target " + left);
				} else {
					throw new SynthesisException("Cannot find any units for target " + target);
				}
			}
			assert candidates.size() > 0;

			// absolutely critical since candidates is no longer a SortedSet:
			Collections.sort(candidates);

			point.candidates = candidates;
			assert beamSize != 0; // general beam search not implemented

			// Now go through all existing paths and all candidates
			// for the current item;
			// tentatively extend each existing path to each of
			// the candidates, but only retain the best one
			List<ViterbiPath> paths = point.paths;
			int nPaths = paths.size();
			if (beamSize != -1 && beamSize < nPaths) {
				// beam search, look only at the best n paths:
				nPaths = beamSize;
			}
			// for searchStrategy == -1, no beam -- look at all candidates.
			int i = 0;
			int iMax = nPaths;
			for (ViterbiPath pp : paths) {
				assert pp != null;
				// We are at the very beginning of the search,
				// or have a usable path to extend
				candidates = point.candidates;
				assert candidates != null;
				int j = 0;
				int jMax = beamSize;
				// Go through the candidates as returned by the iterator of the sorted set,
				// i.e. sorted according to increasing target cost.
				for (ViterbiCandidate c : candidates) {
					// For the candidate c, create a path extending the
					// previous path pp to that candidate, taking into
					// account the target and join costs:
					ViterbiPath np = getPath(pp, c);
					// Compare this path to the existing best path
					// (if any) leading to candidate c; only retain
					// the one with the better score.
					addPath(point.next, np);
					if (++j == jMax)
						break;
				}
				if (++i == iMax)
					break;
			}
		}
	}

	/**
	 * Add the new path to the state path if it is better than the current path. In this, state means the position of the
	 * candidate associated with this path in the candidate queue for the corresponding segment item. In other words, this method
	 * uses newPath as the one path leading to the candidate newPath.candidate, if it has a better score than the previously best
	 * path leading to that candidate.
	 *
	 * @param point
	 *            where the path is added
	 * @param newPath
	 *            the path to add if its score is best
	 */
	void addPath(ViterbiPoint point, ViterbiPath newPath) {
		// get the position of newPath's candidate
		// in path array statePath of point
		ViterbiCandidate candidate = newPath.candidate;
		assert candidate != null;
		ViterbiPath bestPathSoFar = candidate.bestPath;
		List<ViterbiPath> paths = point.getPaths();
		if (bestPathSoFar == null) {
			// we don't have a path for the candidate yet, so this is best
			paths.add(newPath);
			candidate.setBestPath(newPath);
		} else if (newPath.score < bestPathSoFar.score) {
			// newPath is a better path for the candidate
			paths.remove(bestPathSoFar);
			paths.add(newPath);
			candidate.setBestPath(newPath);
		}
	}

	/**
	 * Collect and return the best path, as a List of SelectedUnit objects. Note: This is a replacement for result().
	 * 
	 * @return the list of selected units, or null if no path could be found.
	 */
	public List<SelectedUnit> getSelectedUnits() {
		LinkedList<SelectedUnit> selectedUnits = new LinkedList<SelectedUnit>();
		if (firstPoint == null || firstPoint.getNext() == null) {
			return selectedUnits; // null case
		}
		ViterbiPath best = findBestPath();
		if (best == null) {
			// System.out.println("No best path found");
			return null;
		}
		for (ViterbiPath path = best; path != null; path = path.getPrevious()) {
			if (path.candidate != null) {
				Unit u = path.candidate.unit;
				Target t = path.candidate.target;
				if (u instanceof DiphoneUnit) {
					assert t instanceof DiphoneTarget;
					DiphoneUnit du = (DiphoneUnit) u;
					DiphoneTarget dt = (DiphoneTarget) t;
					selectedUnits.addFirst(new SelectedUnit(du.right, dt.right));
					selectedUnits.addFirst(new SelectedUnit(du.left, dt.left));
				} else {
					selectedUnits.addFirst(new SelectedUnit(u, t));
				}
			}
		}
		if (logger.getEffectiveLevel().equals(Level.DEBUG)) {
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			int prevIndex = -1; // index number of the previous unit
			int[] lengthHistogram = new int[10];
			int length = 0;
			int numUnits = selectedUnits.size();
			StringBuilder line = new StringBuilder();
			for (int i = 0; i < numUnits; i++) {
				SelectedUnit u = (SelectedUnit) selectedUnits.get(i);
				int index = u.getUnit().index;
				if (prevIndex + 1 == index) { // adjacent units
					length++;
				} else {
					if (lengthHistogram.length <= length) {
						int[] dummy = new int[length + 1];
						System.arraycopy(lengthHistogram, 0, dummy, 0, lengthHistogram.length);
						lengthHistogram = dummy;
					}
					lengthHistogram[length]++;
					pw.print(line);
					// Find filename from which the stretch that just finished
					// stems:
					if (i > 0) {
						assert i >= length;
						Unit firstUnitInStretch = ((SelectedUnit) selectedUnits.get(i - length)).getUnit();
						String origin = database.getFilenameAndTime(firstUnitInStretch);
						// Print origin from column 80:
						for (int col = line.length(); col < 80; col++)
							pw.print(" ");
						pw.print(origin);
					}
					pw.println();
					length = 1;
					line.setLength(0);
				}
				line.append(database.getTargetCostFunction().getFeature(u.getUnit(), "phone") + "(" + u.getUnit().index + ")");
				prevIndex = index;
			}
			if (lengthHistogram.length <= length) {
				int[] dummy = new int[length + 1];
				System.arraycopy(lengthHistogram, 0, dummy, 0, lengthHistogram.length);
				lengthHistogram = dummy;
			}
			lengthHistogram[length]++;
			pw.print(line);
			// Find filename from which the stretch that just finished
			// stems:
			Unit firstUnitInStretch = ((SelectedUnit) selectedUnits.get(numUnits - length)).getUnit();
			String origin = database.getFilenameAndTime(firstUnitInStretch);
			// Print origin from column 80:
			for (int col = line.length(); col < 80; col++)
				pw.print(" ");
			pw.print(origin);
			pw.println();
			logger.debug("Selected units:\n" + sw.toString());
			// Compute average length of stretches:
			int total = 0;
			int nStretches = 0;
			for (int l = 1; l < lengthHistogram.length; l++) {
				// lengthHistogram[0] will be 0 anyway
				total += lengthHistogram[l] * l;
				nStretches += lengthHistogram[l];
			}
			float avgLength = total / (float) nStretches;
			DecimalFormat df = new DecimalFormat("0.000");
			logger.debug("Avg. consecutive length: " + df.format(avgLength) + " units");
			// Cost of best path
			double totalCost = best.score;
			int elements = selectedUnits.size();
			double avgCostBestPath = totalCost / (elements - 1);
			double avgTargetCost = cumulTargetCosts / nTargetCosts;
			double avgJoinCost = cumulJoinCosts / nJoinCosts;
			logger.debug("Avg. cost: best path " + df.format(avgCostBestPath) + ", avg. target " + df.format(avgTargetCost)
					+ ", join " + df.format(avgJoinCost) + " (n=" + nTargetCosts + ")");
			DebugStats stats = debugStats.get(database);
			if (stats == null) {
				stats = new DebugStats();
				debugStats.put(database, stats);
			}
			stats.n++;
			// iterative computation of mean:
			// m(n) = m(n-1) + (x(n) - m(n-1)) / n
			stats.avgLength += (avgLength - stats.avgLength) / stats.n;
			stats.avgCostBestPath += (avgCostBestPath - stats.avgCostBestPath) / stats.n;
			stats.avgTargetCost += (avgTargetCost - stats.avgTargetCost) / stats.n;
			stats.avgJoinCost += (avgJoinCost - stats.avgJoinCost) / stats.n;
			logger.debug("Total average of " + stats.n + " utterances for this voice:");
			logger.debug("Avg. length: " + df.format(stats.avgLength) + ", avg. cost best path: "
					+ df.format(stats.avgCostBestPath) + ", avg. target cost: " + df.format(stats.avgTargetCost)
					+ ", avg. join cost: " + df.format(stats.avgJoinCost));

		}

		return selectedUnits;
	}

	/**
	 * Construct a new path element linking a previous path to the given candidate. The (penalty) score associated with the new
	 * path is calculated as the sum of the score of the old path plus the score of the candidate itself plus the join cost of
	 * appending the candidate to the nearest candidate in the given path. This join cost takes into account optimal coupling if
	 * the database has OPTIMAL_COUPLING set to 1. The join position is saved in the new path, as the features "unit_prev_move"
	 * and "unit_this_move".
	 *
	 * @param path
	 *            the previous path, or null if this candidate starts a new path
	 * @param candiate
	 *            the candidate to add to the path
	 *
	 * @return a new path, consisting of this candidate appended to the previous path, and with the cumulative (penalty) score
	 *         calculated.
	 */
	private ViterbiPath getPath(ViterbiPath path, ViterbiCandidate candidate) {
		double cost;

		Target candidateTarget = candidate.target;
		Unit candidateUnit = candidate.unit;

		double joinCost;
		double sCost = 0;
		double targetCost;
		// Target costs:
		targetCost = candidate.targetCost;

		if (path == null || path.candidate == null) {
			joinCost = 0;
		} else {
			// Join costs:
			ViterbiCandidate prevCandidate = path.candidate;
			Target prevTarget = prevCandidate.target;
			Unit prevUnit = prevCandidate.unit;
			joinCost = joinCostFunction.cost(prevTarget, prevUnit, candidateTarget, candidateUnit);
			if (sCostFunction != null)
				sCost = sCostFunction.cost(prevUnit, candidateUnit);
		}
		// Total cost is a weighted sum of join cost and target cost:
		// cost = (1-r) * joinCost + r * targetCost,
		// where r is given as the property "viterbi.wTargetCost" in a config file.
		targetCost *= wTargetCosts;
		joinCost *= wJoinCosts;
		sCost *= wSCosts;
		cost = joinCost + targetCost + sCost;
		if (joinCost < Float.POSITIVE_INFINITY)
			cumulJoinCosts += joinCost;
		nJoinCosts++;
		cumulTargetCosts += targetCost;
		nTargetCosts++;
		// logger.debug(candidateUnit+": target cost "+targetCost+", join cost "+joinCost);

		if (path != null) {
			cost += path.score;
		}

		return new ViterbiPath(candidate, path, cost);
	}

	/**
	 * Find the best path. This requires apply() to have been run. For this best path, we set the pointers to the *next* path
	 * elements correctly.
	 *
	 * @return the best path, or null if no best path could be found.
	 */
	private ViterbiPath findBestPath() {
		assert beamSize != 0;
		// All paths end in lastPoint, and take into account
		// previous path segment's scores. Therefore, it is
		// sufficient to find the best path from among the
		// paths for lastPoint.
		List<ViterbiPath> paths = lastPoint.getPaths();
		if (paths.isEmpty()) // no path, we failed
			return null;

		// as paths is no longer a SortedSet, they must be explicitly sorted:
		Collections.sort(paths);
		ViterbiPath best = paths.get(0);

		// Set *next* pointers correctly:
		ViterbiPath path = best;
		double totalCost = best.score;
		int elements = 0;
		while (path != null) {
			elements++;
			ViterbiPath prev = path.previous;
			if (prev != null)
				prev.setNext(path);
			path = prev;
		}
		return best;
	}

	private class DebugStats {
		int n;
		double avgLength;
		double avgCostBestPath;
		double avgTargetCost;
		double avgJoinCost;
	}

}
