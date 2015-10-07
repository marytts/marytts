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

package marytts.util.data.text;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import marytts.signalproc.analysis.Label;
import marytts.signalproc.analysis.Labels;

/**
 * A class to read and parse labels in a text file. The file format should conform to that used by ESPS Xwaves and the many other
 * labeling programs which support that format.
 * 
 * @author Ingmar Steiner
 */
public class XwavesLabelfileReader {
	// main class variables (reader, times, labels, header lines)
	protected BufferedReader reader;
	protected Double[] times;
	protected String[] labels;
	protected String[] header;

	/**
	 * Read data from a Label file.
	 * 
	 * @param filename
	 *            Label filename as a String
	 * @throws IOException
	 *             IOException
	 */
	public XwavesLabelfileReader(String filename) throws IOException {
		this(new FileReader(filename));
	}

	/**
	 * Read data from a Label file.
	 * 
	 * @param reader
	 *            Label file as a Reader
	 * @throws IOException
	 *             IOException
	 */
	public XwavesLabelfileReader(Reader reader) throws IOException {
		this.reader = new BufferedReader(reader);
		parseLabels();
		reader.close();
	}

	/**
	 * Read lines from the label file and parse them. As each line is parsed, the label in that line and its end time are appended
	 * to the appropriate arrays, and the initial header lines are stored in a third vector.
	 * 
	 * @throws IOException
	 *             IOException
	 */
	private void parseLabels() throws IOException {
		// initialize some variables
		String line;
		boolean headerComplete = false;
		ArrayList<Double> timesList = new ArrayList<Double>();
		ArrayList<String> labelsList = new ArrayList<String>();
		ArrayList<String> headersList = new ArrayList<String>();

		// Legend for regular expression:
		//
		// ^ start of line
		// \\s* leading whitespace
		// ( start of first captured group (time)
		// \\d+ one or more digits
		// (?: followed by a non-capturing group containing
		// \\. a period and
		// \\d+ one or more digits
		// )? this group is optional
		// ) end of first captured group
		// \\s+ whitespace
		// .+? second column, which is ignored (not captured)
		// \\s+? whitespace
		// (.*) second captured group (label)
		// $ end of line
		Pattern linePattern = Pattern.compile("^\\s*(\\d+(?:\\.\\d+)?)\\s+.+?\\s+?(.*)$");
		boolean matches = false;

		// initialize some more variables for each line's captured groups
		String timeStr = null;
		String label = null;
		double time;

		// read the file line by line
		while ((line = reader.readLine()) != null) {
			// apply the regex Pattern to the current line...
			Matcher lineMatcher = linePattern.matcher(line);
			// ...and see if it matches
			matches = lineMatcher.matches();

			if (matches) {
				// some label files might be headerless;
				// in that case, a well-formed line indicates that we are already seeing label data
				headerComplete = true;

				// parse the line by accessing the groups captured by the regex Matcher
				// the first group is the label's end time
				timeStr = lineMatcher.group(1);
				// the second group is the label itself
				label = lineMatcher.group(2);

				try {
					// parse the end time into a Double and append it to times
					time = Double.parseDouble(timeStr);
					timesList.add(time);
				} catch (NumberFormatException nfe) {
					// number could not be parsed; this should never actually happen!
					throw nfe;
				}

				// append label to labels
				labelsList.add(label);

			} else {
				// line could not be parsed by regex; are we still in the header?
				if (!headerComplete) {
					if (line.trim().startsWith("#"))
						// hash line signals end of header (but is not itself part of the header)
						headerComplete = true;
					else
						// no hash line seen so far, line seems to be part of header
						headersList.add(line);
				} else {
					// header was already complete, or we are dealing with a headerless label file,
					// but we found a line that could not be parsed!
					System.err.println("Malformed line found outside of header:\n" + line);
					throw new IOException();
				}
			}
		}

		// it should never happen that times and labels do not have the same number of elements!
		assert timesList.size() == labelsList.size() : "";

		times = new Double[timesList.size()];
		int t;
		for (t = 0; t < timesList.size(); t++) {
			times[t] = timesList.get(t);
		}

		labels = (String[]) labelsList.toArray(new String[0]);
		header = (String[]) headersList.toArray(new String[0]);

		return;

	}

	/**
	 * getter method for times
	 * 
	 * @return times as ArrayList of Doubles
	 */
	public Double[] getTimes() {
		return times;
	}

	/**
	 * getter method for labels
	 * 
	 * @return labels as ArrayList of Strings
	 */
	public String[] getLabelSymbols() {
		return labels;
	}

	public Labels getLabels() {
		Label[] items = new Label[labels.length];
		assert times.length == labels.length;
		for (int i = 0; i < items.length; i++) {
			items[i] = new Label(times[i], labels[i]);
		}
		return new Labels(items);
	}

	/**
	 * getter method for header
	 * 
	 * @return header lines as ArrayList of Strings
	 */
	public String[] getHeader() {
		return header;
	}

}
