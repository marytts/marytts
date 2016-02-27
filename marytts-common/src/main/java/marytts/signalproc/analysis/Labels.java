/**
 * Copyright 2007, 2011 DFKI GmbH.
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

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.traversal.NodeIterator;

import marytts.exceptions.InvalidDataException;
import marytts.util.dom.DomUtils;
import marytts.util.io.FileUtils;
import marytts.util.string.StringUtils;

/**
 * A collection of EST formatted labels with ascii text file input/output functionality
 * 
 * @author Oytun Türk, reworked by Marc Schröder
 */
public class Labels extends AlignmentData {
	public Label[] items;

	public Labels(Label[] items) {
		this.items = deepCopy(items);
	}

	public Labels(String[] lines) {
		this(lines, 3);
	}

	public Labels(String[] lines, int minItemsPerLine) {
		initFromLines(lines, minItemsPerLine);
	}

	// Create ESTLabels from existing ones
	public Labels(Labels e) {
		if (e != null) {
			this.items = deepCopy(e.items);
		}
	}

	public Labels(InputStream in) throws IOException {
		initFromStream(in);
	}

	public Labels(String labelFile) throws IOException {
		initFromStream(new FileInputStream(labelFile));
	}

	public Labels(Document acoustparams) {
		initFromAcoustparams(acoustparams);
	}

	public String[] getLabelSymbols() {
		if (items == null)
			return null;
		String[] symbols = new String[items.length];
		for (int i = 0; i < items.length; i++) {
			symbols[i] = items[i].phn;
		}
		return symbols;
	}

	private void initFromStream(InputStream in) throws IOException {
		String allText = FileUtils.getStreamAsString(in, "ASCII");
		String[] lines = allText.split("\n");
		initFromLines(lines, 3);
	}

	private void initFromLines(String[] lines, int minimumItemsInOneLine) {
		ArrayList<Label> labels = new ArrayList<Label>();

		for (int i = 0; i < lines.length; i++) {
			String[] labelInfos = lines[i].trim().split("\\s+");
			if (labelInfos.length >= minimumItemsInOneLine && StringUtils.isNumeric(labelInfos[0])
					&& StringUtils.isNumeric(labelInfos[1])) {
				Label l = new Label();
				labels.add(l);
				if (labelInfos.length > 0)
					l.time = Float.parseFloat(labelInfos[0]);

				if (labelInfos.length > 1)
					l.status = Integer.parseInt(labelInfos[1]);

				if (labelInfos.length > 2)
					l.phn = labelInfos[2].trim();

				int restStartMin = 4;
				if (labelInfos.length > 3 && StringUtils.isNumeric(labelInfos[3]))
					l.ll = Float.parseFloat(labelInfos[3]);
				else {
					restStartMin = 3;
					l.ll = Float.NEGATIVE_INFINITY;
				}

				// Read additional fields if any in String format
				// also convert these to double values if they are
				// numeric
				if (labelInfos.length > restStartMin) {
					int numericCount = 0;
					l.rest = new String[labelInfos.length - restStartMin];
					l.valuesRest = new double[labelInfos.length - restStartMin];
					for (int j = 0; j < l.rest.length; j++) {
						l.rest[j] = labelInfos[j + restStartMin];
						if (StringUtils.isNumeric(l.rest[j]))
							l.valuesRest[j] = Double.valueOf(l.rest[j]);
						else
							l.valuesRest[j] = Double.NEGATIVE_INFINITY;
					}
				}
			}
		}
		items = (Label[]) labels.toArray(new Label[0]);
	}

	/**
	 * 
	 * @param acoustparams
	 * @throws InvalidDataException
	 *             if any phone or boundary in acoustparams has no duration.
	 */
	private void initFromAcoustparams(Document acoustparams) {
		ArrayList<Label> labels = new ArrayList<Label>();
		String PHONE = "ph";
		String A_PHONE_DURATION = "d";
		String A_PHONE_SYMBOL = "p";
		String A_PHONE_END = "end";
		String BOUNDARY = "boundary";
		String A_BOUNDARY_DURATION = "duration";
		NodeIterator it = DomUtils.createNodeIterator(acoustparams, PHONE, BOUNDARY);
		Element e = null;
		double startTime = 0;
		double endTime = 0;
		double duration = 0;
		double endResetCorrection = 0;
		String phoneSymbol;
		while ((e = (Element) it.nextNode()) != null) {
			startTime = /* previous */endTime;
			if (e.getTagName().equals(PHONE)) {
				phoneSymbol = e.getAttribute(A_PHONE_SYMBOL);
				// Expect end attribute to reset itself sometimes:
				double endValue = Double.parseDouble(e.getAttribute(A_PHONE_END));
				if (endValue < startTime) { // reset
					endResetCorrection = startTime;
				}
				endTime = endResetCorrection + endValue;
				// Too imprecise to use only full milliseconds:
				// if (!e.hasAttribute(A_PHONE_DURATION)) {
				// throw new InvalidDataException("No duration for phone '"+phoneSymbol+"'");
				// }
				// duration = 0.001 * Double.parseDouble(e.getAttribute(A_PHONE_DURATION));
				// endTime = startTime + duration;
			} else { // BOUNDARY
				assert e.getTagName().equals(BOUNDARY);
				if (!e.hasAttribute(A_BOUNDARY_DURATION)) {
					throw new InvalidDataException("No duration for boundary");
				}
				duration = 0.001 * Double.parseDouble(e.getAttribute(A_BOUNDARY_DURATION));
				endTime = startTime + duration;
				phoneSymbol = "_";
			}
			labels.add(new Label(endTime, phoneSymbol));
		}
		items = (Label[]) labels.toArray(new Label[0]);
	}

	public void print() {
		for (int i = 0; i < items.length; i++)
			items[i].print();
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("[");
		for (int i = 0; i < items.length; i++) {
			sb.append("(").append(items[i]).append(") ");
		}
		sb.append("]");
		return sb.toString();
	}

	/**
	 * For the given time, return the index of the label at that time, if any.
	 * 
	 * @param time
	 *            time in seconds
	 * @return the index of the label at that time, or -1 if there isn't any
	 */
	public int getLabelIndexAtTime(double time) {
		if (items == null) {
			return -1;
		}
		// We return the first label whose end time is >= time:
		for (int i = 0; i < items.length; i++) {
			if (items[i].time >= time) {
				return i;
			}
		}
		return -1;
	}

	private Label[] deepCopy(Label[] in) {
		if (in == null)
			return null;
		Label[] out = new Label[in.length];
		for (int i = 0; i < in.length; i++) {
			if (in[i] != null) {
				out[i] = new Label(in[i]);
			}
		}
		return out;
	}

	@Override
	public boolean equals(Object other) {
		if (this == other)
			return true;
		if (!(other instanceof Labels))
			return false;
		Labels o = (Labels) other;
		return Arrays.deepEquals(this.items, o.items);
	}

	@Override
	public int hashCode() {
		return 0;
	}

}
