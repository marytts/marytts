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
package marytts.tools.voiceimport;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.StringTokenizer;

public class UnitLabel {
	public String unitName;
	public double startTime;
	public double endTime;
	public int unitIndex;
	public double sCost;

	public UnitLabel(String unitName, double startTime, double endTime, int unitIndex) {
		this.unitName = unitName;
		this.startTime = startTime;
		this.endTime = endTime;
		this.unitIndex = unitIndex;
		this.sCost = 0;
	}

	public UnitLabel(String unitName, double startTime, double endTime, int unitIndex, double sCost) {
		this.unitName = unitName;
		this.startTime = startTime;
		this.endTime = endTime;
		this.unitIndex = unitIndex;
		this.sCost = sCost;
	}

	public double getSCost() {
		return this.sCost;
	}

	public double getStartTime() {
		return this.startTime;
	}

	public double getEndTime() {
		return this.endTime;
	}

	public int getUnitIndex() {
		return this.unitIndex;
	}

	public String getUnitName() {
		return this.unitName;
	}

	public void setStartTime(double startTime) {
		this.startTime = startTime;
	}

	public void setEndTime(double endTime) {
		this.endTime = endTime;
	}

	public void setUnitIndex(int unitIndex) {
		this.unitIndex = unitIndex;
	}

	public void setUnitName(String unitName) {
		this.unitName = unitName;
	}

	/**
	 * @param labFile
	 *            labFile
	 * @throws IOException
	 *             IOException
	 * @return ulab
	 */
	public static UnitLabel[] readLabFile(String labFile) throws IOException {

		ArrayList<String> lines = new ArrayList<String>();
		BufferedReader labels = new BufferedReader(new InputStreamReader(new FileInputStream(new File(labFile)), "UTF-8"));
		String line;

		// Read Label file first
		// 1. Skip label file header:
		while ((line = labels.readLine()) != null) {
			if (line.startsWith("#"))
				break; // line starting with "#" marks end of header
		}

		// 2. Put data into an ArrayList
		String labelUnit = null;
		double startTimeStamp = 0.0;
		double endTimeStamp = 0.0;
		double sCost = 0.0;
		int unitIndex = 0;
		while ((line = labels.readLine()) != null) {
			labelUnit = null;
			if (line != null) {
				List labelUnitData = getLabelUnitData(line);
				sCost = (new Double((String) labelUnitData.get(3))).doubleValue();
				labelUnit = (String) labelUnitData.get(2);
				unitIndex = Integer.parseInt((String) labelUnitData.get(1));
				endTimeStamp = Double.parseDouble((String) labelUnitData.get(0));
			}
			if (labelUnit == null)
				break;
			lines.add(labelUnit.trim() + " " + startTimeStamp + " " + endTimeStamp + " " + unitIndex + " " + sCost);
			startTimeStamp = endTimeStamp;
		}
		labels.close();

		UnitLabel[] ulab = new UnitLabel[lines.size()];
		Iterator<String> itr = lines.iterator();
		for (int i = 0; itr.hasNext(); i++) {
			String element = itr.next();
			String[] wrds = element.split("\\s+");
			ulab[i] = new UnitLabel(wrds[0], (new Double(wrds[1])).doubleValue(), (new Double(wrds[2])).doubleValue(),
					(new Integer(wrds[3])).intValue(), (new Double(wrds[4])).doubleValue());
		}
		return ulab;
	}

	public static void writeLabFile(UnitLabel[] ulab, String outFile) throws IOException {
		PrintWriter pw = new PrintWriter(new FileWriter(outFile));
		pw.println("#");
		for (int i = 0; i < ulab.length; i++) {
			pw.printf(Locale.US, "%.6f %d %s\n", ulab[i].getEndTime(), ulab[i].getUnitIndex(), ulab[i].getUnitName());
		}
		pw.flush();
		pw.close();
	}

	/**
	 * To get Label Unit DATA (time stamp, index, phone unit)
	 * 
	 * @param line
	 *            line
	 * @return ArrayList contains time stamp, index and phone unit
	 * @throws IOException
	 *             IOException
	 */
	private static ArrayList getLabelUnitData(String line) throws IOException {
		if (line == null)
			return null;
		ArrayList unitData = new ArrayList();
		StringTokenizer st = new StringTokenizer(line.trim());
		// the first token is the time
		unitData.add(st.nextToken());
		// the second token is the unit index
		unitData.add(st.nextToken());
		// the third token is the phone
		unitData.add(st.nextToken());
		// the fourth token is sCost
		if (st.hasMoreTokens())
			unitData.add(st.nextToken());
		else
			unitData.add("0");
		return unitData;
	}

}
