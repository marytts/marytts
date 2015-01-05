/**
 * Copyright 2000-2009 DFKI GmbH.
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
package marytts.tools.voiceimport;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.SortedMap;
import java.util.StringTokenizer;
import java.util.TreeMap;

import marytts.util.data.text.XwavesLabelfileReader;

/**
 * Compute unit labels from phone labels.
 * 
 * @author schroed
 *
 */
public class PhoneUnitLabelComputer extends VoiceImportComponent {
	protected File phonelabelDir;
	protected File unitlabelDir;
	protected String unitlabelExt;
	protected String pauseSymbol;

	protected DatabaseLayout db = null;
	protected int percent = 0;
	protected int basenameIndex;

	public String getName() {
		return "PhoneUnitLabelComputer";
	}

	@Override
	protected void initialiseComp() throws Exception {
		pauseSymbol = db.getAllophoneSet().getSilence().name();

		phonelabelDir = new File(db.getProp(DatabaseLayout.LABDIR));
		unitlabelDir = new File(db.getProp(DatabaseLayout.PHONELABDIR));
		unitlabelExt = db.getProp(DatabaseLayout.LABEXT);
	}

	public SortedMap<String, String> getDefaultProps(DatabaseLayout db) {
		this.db = db;
		if (props == null) {
			props = new TreeMap<String, String>();
		}
		return props;
	}

	protected void setupHelp() {
		props2Help = new TreeMap<String, String>();
	}

	public boolean compute() throws Exception {
		if (!phonelabelDir.exists()) {
			throw new IOException("No such directory: " + phonelabelDir);
		}

		if (!unitlabelDir.exists()) {
			System.out.print(DatabaseLayout.PHONELABDIR + " " + db.getProp(DatabaseLayout.PHONELABDIR) + " does not exist; ");
			if (!unitlabelDir.mkdir()) {
				throw new Exception("Could not create PHONELABDIR");
			}
			System.out.print("Created successfully.\n");
		}

		System.out.println("Computing unit labels for " + bnl.getLength() + " files.");
		System.out.println("From phonetic label files: " + db.getProp(DatabaseLayout.LABDIR) + "*"
				+ db.getProp(DatabaseLayout.LABEXT));
		System.out.println("To       unit label files: " + unitlabelDir + "*" + unitlabelExt);
		for (basenameIndex = 0; basenameIndex < bnl.getLength(); basenameIndex++) {
			percent = 100 * basenameIndex / bnl.getLength();
			computePhoneLabel(bnl.getName(basenameIndex));
		}
		System.out.println("Finished computing unit labels");
		return true;
	}

	public void computePhoneLabel(String baseName) throws Exception {
		File labFile = new File(db.getProp(DatabaseLayout.LABDIR), baseName + db.getProp(DatabaseLayout.LABEXT));
		if (!labFile.exists()) {
			System.out.println("Utterance [" + baseName + "] does not have a phonetic label file.");
			System.out.println("Removing this utterance from the base utterance list.");
			bnl.remove(baseName);
			basenameIndex--;
			return;
		}

		// parse labFile:
		XwavesLabelfileReader labFileData = new XwavesLabelfileReader(labFile.getPath());
		ArrayList<Double> endTimes = new ArrayList<Double>(Arrays.asList(labFileData.getTimes()));
		ArrayList<String> labels = new ArrayList<String>(Arrays.asList(labFileData.getLabelSymbols()));

		// ensure that each labeled interval ends after the previous one:
		ListIterator<Double> timeIterator = endTimes.listIterator();
		double time = timeIterator.next();
		while (timeIterator.hasNext()) {
			double nextTime = timeIterator.next();
			if (time == nextTime) {
				int index = timeIterator.previousIndex() - 1;
				String label = labels.get(index);
				// doesn't matter which of the two times we remove -- they're the same:
				timeIterator.remove();
				// but from the labels, we remove the previous one:
				labels.remove(index);
				System.err.format("WARNING: labeled interval %d (%s) has zero duration; deleting it!\n", index + 1, label);
			} else if (time > nextTime) {
				throw new Exception("ERROR: labeled intervals are out of order; please fix the label file!");
			}
			time = nextTime;
		}

		// merge consecutive pauses:
		ListIterator<String> labelIterator = labels.listIterator();
		String label = labelIterator.next();
		while (labelIterator.hasNext()) {
			String nextLabel = labelIterator.next();
			if (label.equals(nextLabel) && label.equals(pauseSymbol)) {
				labelIterator.remove();
				endTimes.remove(labelIterator.previousIndex());
			}
			label = nextLabel;
		}

		// get midtimes:
		List<Double> midTimes = getMidTimes(labels, endTimes);

		// convert labels to unit labels:
		String[] unitLabelLines = toUnitLabels(labels, endTimes, midTimes);

		// write to phonelab file:
		File phoneLabFile = new File(unitlabelDir, baseName + unitlabelExt);
		PrintWriter out = new PrintWriter(phoneLabFile);
		// header:
		for (String headerLine : labFileData.getHeader()) {
			out.println(headerLine);
		}
		out.println("format: end time, unit index, phone");
		out.println("#");
		// labels:
		for (String unitLabelLine : unitLabelLines) {
			out.println(unitLabelLine);
		}
		out.close();
	}

	/**
	 * Get mid points for an utterance, given a list its phone labels and a list of corresponding end points.
	 * 
	 * @param labels
	 *            of the phones
	 * @param endTimes
	 *            of the phones
	 * @return a list of midpoint times (in seconds) for the phones
	 */
	protected List<Double> getMidTimes(List<String> labels, List<Double> endTimes) {
		// in this class, we don't actually need any midpoint times, so return null:
		return null;
	}

	/**
	 * Convert phone labels to unit labels. This base implementation returns the phone labels; subclasses may want to override
	 * that behaviour.
	 * 
	 * @param phoneLabels
	 *            the phone labels, one phone per line, with each line containing three fields: 1. the end time of the current
	 *            phone, in seconds, since the beginning of the file; 2. a number to be ignored; 3. the phone symbol.
	 * @return an array of lines, in the same format as the phoneLabels input array, but with unit symbols instead of phone
	 *         symbols. The number in the middle now denotes the unit index. This array may or may not have the same number of
	 *         lines as phoneLabels.
	 */
	@Deprecated
	protected String[] toUnitLabels(String[] phoneLabels) {
		String[] unitLabels = new String[phoneLabels.length];
		int unitIndex = 0;
		for (int i = 0; i < phoneLabels.length; i++) {
			String line = phoneLabels[i];
			unitIndex++;
			StringTokenizer st = new StringTokenizer(line.trim());
			// first token is time
			String time = st.nextToken();
			// next token is some number, throw away
			st.nextToken();
			// next token is phone
			String phone = st.nextToken();
			unitLabels[i] = time + " " + unitIndex + " " + phone;
		}
		return unitLabels;
	}

	/**
	 * Generate a sequence of Strings, corresponding to the lines in an Xwaves-compatible label file, by interleaving a List of
	 * label Strings with a List of end time points.
	 * 
	 * @param labels
	 *            a List of label Strings
	 * @param endTimes
	 *            a List of time points representing the end points of these labels
	 * @param midTimes
	 *            a List of time points representing the mid points of these labels (can be null)
	 * @return the label files lines
	 */
	protected String[] toUnitLabels(List<String> labels, List<Double> endTimes, List<Double> midTimes) {
		assert labels.size() == endTimes.size();
		if (midTimes != null) {
			assert midTimes.size() == endTimes.size();
		}

		ArrayList<String> unitLines = new ArrayList<String>(labels.size());

		for (int i = 0; i < labels.size(); i++) {
			String label = labels.get(i);
			double endTime = endTimes.get(i);
			if (midTimes != null) {
				double midTime = midTimes.get(i);
				unitLines.add(String.format(Locale.US, "%f %d %s_L", midTime, unitLines.size() + 1, label));
				unitLines.add(String.format(Locale.US, "%f %d %s_R", endTime, unitLines.size() + 1, label));
			} else {
				unitLines.add(String.format(Locale.US, "%f %d %s", endTime, unitLines.size() + 1, label));
			}
		}
		return (String[]) unitLines.toArray(new String[unitLines.size()]);
	}

	/**
	 * Provide the progress of computation, in percent, or -1 if that feature is not implemented.
	 * 
	 * @return -1 if not implemented, or an integer between 0 and 100.
	 */
	public int getProgress() {
		return percent;
	}

}
