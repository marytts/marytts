/**
 * Copyright 2009 DFKI GmbH.
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

package marytts.tools.analysis;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import marytts.modules.phonemiser.Allophone;
import marytts.modules.phonemiser.AllophoneSet;
import marytts.signalproc.analysis.AlignedLabels;
import marytts.signalproc.analysis.Labels;
import marytts.util.data.text.XwavesLabelfileReader;
import marytts.util.string.StringUtils;

/**
 * This class aligns a label file with an XML file in MARY ALLOPHONES format, modifying the structure of the XML file as needed to
 * match the label file. After calling alignXMLTranscriptions(), it is guaranteed that an iteration through all PHONE and BOUNDARY
 * nodes of the XML file matches the label file.
 * 
 * @author marc
 *
 */
public class TranscriptionAligner {

	protected Map<String, Integer> aligncost;
	protected int defaultcost;
	protected int defaultBoundaryCost;
	protected int skipcost;
	protected AllophoneSet allophoneSet;
	// String for a boundary
	protected String possibleBnd;
	protected String entrySeparator;

	protected boolean ensureInitialBoundary = false;

	public TranscriptionAligner() {
		this(null);
	}

	public TranscriptionAligner(AllophoneSet allophoneSet) {
		this(allophoneSet, null);
	}

	public TranscriptionAligner(AllophoneSet allophoneSet, String entrySeparator) {
		this.aligncost = new HashMap<String, Integer>();
		this.defaultcost = 10;

		// phone set is used for splitting the sampa strings and setting the costs
		this.allophoneSet = allophoneSet;
		if (allophoneSet != null) {
			possibleBnd = allophoneSet.getSilence().name();
		} else {
			possibleBnd = "_";
		}
		if (entrySeparator != null) {
			this.entrySeparator = entrySeparator;
		} else {
			this.entrySeparator = "|";
		}

		this.setDistance();

		defaultcost = this.getMaxCost();
		// align boundaries only to itself
		defaultBoundaryCost = 20 * defaultcost;
		// distance between pauses is zero, with slight conservative bias
		aligncost.put(possibleBnd + " " + possibleBnd, 0);

		skipcost = defaultcost * 1 / 10; // 0.25 / 0.3 /0.33 seem all fine
	}

	public void SetEnsureInitialBoundary(boolean value) {
		this.ensureInitialBoundary = value;
	}

	public boolean getEnsureInitialBoundary() {
		return ensureInitialBoundary;
	}

	public String getEntrySeparator() {
		return entrySeparator;
	}

	/**
	 * This reads in a label file and returns a String of the phonetic symbols, separated by the entry separator character
	 * entrySeparator.
	 * 
	 * @param entrySeparator
	 *            entry separator
	 * @param ensureInitialBoundary
	 *            ensure initial boundary
	 * @param trfname
	 *            trf name
	 * @throws IOException
	 *             if something goes wrong with opening/reading the file
	 * @return result
	 * 
	 */
	public static String readLabelFile(String entrySeparator, boolean ensureInitialBoundary, String trfname) throws IOException {
		// reader for label file.
		BufferedReader lab = new BufferedReader(new FileReader(trfname));
		try {
			// get XwavesLabelfileDataSouce to parse Xwaves label file and store times and labels:
			XwavesLabelfileReader xlds = new XwavesLabelfileReader(trfname);

			// join them to a string, with entrySeparator as glue:
			String result = StringUtils.join(entrySeparator, xlds.getLabelSymbols());

			// if Label File does not start with pause symbol, insert it
			// as well as a pause duration of zero (...)
			if (ensureInitialBoundary && result.charAt(0) != '_') {
				result = "_" + entrySeparator + result;
			}
			return result;
		} finally {
			lab.close();
		}
	}

	/**
	 * This sets the distance by using the phone set of the aligner object. Phone set must already be specified.
	 */
	private void setDistance() {

		if (null == this.allophoneSet) {
			System.err.println("No allophone set -- cannot use intelligent distance metrics");
			return;
		}

		for (String fromSym : this.allophoneSet.getAllophoneNames()) {
			for (String toSym : this.allophoneSet.getAllophoneNames()) {

				int diff = 0;

				Allophone fromPh = this.allophoneSet.getAllophone(fromSym);
				Allophone toPh = this.allophoneSet.getAllophone(toSym);

				// for each difference increase distance
				diff += (!fromSym.equals(toSym)) ? 2 : 0;
				diff += (fromPh.isFricative() != toPh.isFricative()) ? 2 : 0;
				diff += (fromPh.isGlide() != toPh.isGlide()) ? 2 : 0;
				diff += (fromPh.isLiquid() != toPh.isLiquid()) ? 2 : 0;
				diff += (fromPh.isNasal() != toPh.isNasal()) ? 2 : 0;
				diff += (fromPh.isPlosive() != toPh.isPlosive()) ? 1 : 0;
				diff += (fromPh.isSonorant() != toPh.isSonorant()) ? 2 : 0;
				diff += (fromPh.isSyllabic() != toPh.isSyllabic()) ? 1 : 0;
				diff += (fromPh.isVoiced() != toPh.isVoiced()) ? 1 : 0;
				diff += (fromPh.isVowel() != toPh.isVowel()) ? 2 : 0;
				diff += Math.abs(fromPh.sonority() - toPh.sonority());

				String key = fromSym + " " + toSym;

				this.aligncost.put(key, diff);
			}
		}
	}

	/**
	 * 
	 * This computes the alignment that has the lowest distance between two Strings.
	 * 
	 * There are three differences to the normal Levenshtein-distance:
	 * 
	 * 1. Only insertions and deletions are allowed, no replacements (i.e. no "diagonal" transitions) 2. insertion costs are
	 * dependent on a particular phone on the input side (the one they are aligned to) 3. deletion is equivalent to a symbol on
	 * the input side that is not aligned. There are costs associated with that.
	 * 
	 * The method returns the output string with alignment boundaries ('#') inserted.
	 * 
	 * @param in
	 *            in
	 * @param out
	 *            out
	 * @return p_al[ostr.length]
	 */
	protected String distanceAlign(String in, String out) {
		String[] istr = in.split(Pattern.quote(entrySeparator));
		String[] ostr = out.split(Pattern.quote(entrySeparator));
		String delim = "#";

		// distances:
		// 1. previous distance (= previous column in matrix)
		int[] p_d = new int[ostr.length + 1];
		// 2. current distance
		int[] d = new int[ostr.length + 1];
		// 3. dummy array for swapping, when switching to new column
		int[] _d;

		// array indicating if a skip was performed (= if current character has not been aligned)
		// same arrays as for distances
		boolean[] p_sk = new boolean[ostr.length + 1];
		boolean[] sk = new boolean[ostr.length + 1];
		boolean[] _sk;

		// arrays storing the alignments corresponding to distances
		String[] p_al = new String[ostr.length + 1];
		String[] al = new String[ostr.length + 1];
		String[] _al;

		// initialize values
		p_d[0] = 0;
		p_al[0] = "";
		p_sk[0] = true;

		// ... still initializing
		for (int j = 1; j < ostr.length + 1; j++) {
			// only possibility first is to align the first letter
			// of the input string to everything
			p_al[j] = p_al[j - 1] + " " + ostr[j - 1];
			p_d[j] = p_d[j - 1] + symDist(istr[0], ostr[j - 1]);
			p_sk[j] = false;
		}

		// constant penalty for not aligning a character
		int skConst = this.skipcost;

		// align
		// can start at 1, since 0 has been treated in initialization
		for (int i = 1; i < istr.length; i++) {

			// zero'st row stands for skipping from the beginning on
			d[0] = p_d[0] + skConst;
			al[0] = p_al[0] + " " + delim;
			sk[0] = true;

			for (int j = 1; j < ostr.length + 1; j++) {

				// translation cost between symbols ( j-1, because 0 row
				// inserted for not aligning at beginning)
				int tr_cost = symDist(istr[i], ostr[j - 1]);

				// skipping cost greater zero if not yet aligned
				int sk_cost = p_sk[j] ? skConst : 0;

				if (sk_cost + p_d[j] < tr_cost + d[j - 1]) {
					// skipping cheaper

					// cost is cost from previous input char + skipping
					d[j] = sk_cost + p_d[j];
					// alignment is from prev. input + delimiter
					al[j] = p_al[j] + " " + delim;
					// yes, we skipped
					sk[j] = true;

				} else {
					// aligning cheaper

					// cost is that from previously aligned output + distance
					d[j] = tr_cost + d[j - 1];
					// alignment continues from previously aligned
					al[j] = al[j - 1] + " " + ostr[j - 1];
					// nope, didn't skip
					sk[j] = false;

				}
			}

			// swapping
			_d = p_d;
			p_d = d;
			d = _d;

			_sk = p_sk;
			p_sk = sk;
			sk = _sk;

			_al = p_al;
			p_al = al;
			al = _al;
		}
		return p_al[ostr.length];
	}

	/**
	 * Align the two given sequences of labels and return a mapping array indicating which index in first should be aligned to
	 * which index in second.
	 * 
	 * @param first
	 *            first
	 * @param second
	 *            second
	 * @return an array m of integers -- for each index i in first, m[i] gives the (rightmost) corresponding index in second.
	 */
	public AlignedLabels alignLabels(Labels first, Labels second) {
		String firstLabels = StringUtils.join(entrySeparator, first.getLabelSymbols());
		String secondLabels = StringUtils.join(entrySeparator, second.getLabelSymbols());
		String aligned = distanceAlign(firstLabels, secondLabels);
		// Now, in aligned, the hash signs separate fields corresponding to first;
		// the field contains the label symbols of second (space-separated)
		// that match this index in first.
		if (aligned.endsWith("#")) {
			aligned = aligned + " "; // make sure that the split operation does not discard a final empty field
		}
		String[] fields = aligned.split("#");
		assert fields.length == first.items.length;
		int iSecond = -1; // start before first item
		int[] map = new int[fields.length];
		for (int i = 0; i < fields.length; i++) {
			int numLabels;
			String f = fields[i].trim();
			if (f.equals("")) {
				numLabels = 0;
			} else {
				numLabels = f.split(" ").length;
			}
			iSecond += numLabels;
			map[i] = Math.max(iSecond, 0); // if first elements in second are skipped, still map to 0, not to -1.
		}

		return new AlignedLabels(first, second, map);
	}

	private int getMaxCost() {
		if (aligncost.isEmpty())
			return defaultcost;
		int maxMapping = Collections.max(aligncost.values());
		return (maxMapping > defaultcost) ? maxMapping : defaultcost;
	}

	private int symDist(String aString1, String aString2) {

		String key = aString1 + " " + aString2;

		// if a value is stored, return it
		if (this.aligncost.containsKey(key)) {
			return aligncost.get(key);
		} else if (aString1.equals(aString2)) {
			return 0;
		} else if (aString1.equals(possibleBnd) || aString2.equals(possibleBnd)) {
			// one but not the other is a possible boundary:
			return defaultBoundaryCost;
		}
		return defaultcost;
	}

}
