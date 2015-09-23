/**
 * Copyright 2006 DFKI GmbH.
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
package marytts.tools.voiceimport.vocalizations;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.SortedMap;
import java.util.StringTokenizer;
import java.util.TreeMap;

import marytts.exceptions.MaryConfigurationException;
import marytts.tools.voiceimport.DatabaseLayout;
import marytts.tools.voiceimport.VoiceImportComponent;
import marytts.util.data.ESTTrackReader;
import marytts.util.data.MaryHeader;
import marytts.util.io.BasenameList;
import marytts.vocalizations.VocalizationUnitFileReader;

/**
 * Back-channel unit writer
 * 
 * @author sathish pammi
 *
 */
public class VocalizationUnitfileWriter extends VoiceImportComponent {

	protected String vocalizationsDir;
	protected BasenameList bnlVocalizations;

	// protected File maryDir;
	protected String unitFileName;
	protected File unitlabelDir;
	protected int samplingRate;
	// protected String pauseSymbol;
	protected String unitlabelExt = ".lab";

	protected DatabaseLayout db = null;
	protected int percent = 0;
	// protected BasenameList bachChannelList;

	public String LABELDIR = "VocalizationUnitfileWriter.backchannelLabDir";
	public String UNITFILE = "VocalizationUnitfileWriter.unitFile";
	public final String PMARKDIR = "VocalizationUnitfileWriter.pitchmarkDir";
	// public String BASELIST = "VocalizationUnitfileWriter.backchannelBaseNamesList";

	public final String PMDIR = "db.pmDir";
	public final String PMEXT = "db.pmExtension";

	public String getName() {
		return "VocalizationUnitfileWriter";
	}

	@Override
	protected void initialiseComp() {
		// maryDir = new File(db.getProp(db.FILEDIR));

		samplingRate = Integer.parseInt(db.getProp(db.SAMPLINGRATE));
		// pauseSymbol = System.getProperty("pause.symbol", "pau");

		unitFileName = getProp(UNITFILE);
		unitlabelDir = new File(getProp(LABELDIR));

		String timelineDir = db.getProp(db.VOCALIZATIONSDIR) + File.separator + "files";
		if (!(new File(timelineDir)).exists()) {

			System.out.println("vocalizations/files directory does not exist; ");
			if (!(new File(timelineDir)).mkdirs()) {
				throw new Error("Could not create vocalizations/files");
			}
			System.out.println("Created successfully.\n");

		}

		try {
			String basenameFile = db.getProp(db.VOCALIZATIONSDIR) + File.separator + "basenames.lst";
			if ((new File(basenameFile)).exists()) {
				System.out.println("Loading basenames of vocalizations from '" + basenameFile + "' list...");
				bnlVocalizations = new BasenameList(basenameFile);
				System.out.println("Found " + bnlVocalizations.getLength() + " vocalizations in basename list");
			} else {
				String vocalWavDir = db.getProp(db.VOCALIZATIONSDIR) + File.separator + "wav";
				System.out.println("Loading basenames of vocalizations from '" + vocalWavDir + "' directory...");
				bnlVocalizations = new BasenameList(vocalWavDir, ".wav");
				System.out.println("Found " + bnlVocalizations.getLength() + " vocalizations in " + vocalWavDir + " directory");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public SortedMap getDefaultProps(DatabaseLayout db) {
		this.db = db;
		if (props == null) {
			props = new TreeMap();
			String vocalizationsrootDir = db.getProp(db.VOCALIZATIONSDIR);
			props.put(LABELDIR, vocalizationsrootDir + File.separator + "lab" + System.getProperty("file.separator"));
			props.put(
					UNITFILE,
					vocalizationsrootDir + File.separator + "files" + File.separator + "vocalization_units"
							+ db.getProp(db.MARYEXT));
			props.put(PMARKDIR, db.getProp(db.VOCALIZATIONSDIR) + File.separator + "pm");
			// props.put(BASELIST, "backchannel.lst");
		}
		return props;
	}

	protected void setupHelp() {
		props2Help = new TreeMap();
		props2Help.put(LABELDIR, "directory containing the phone labels");
		props2Help.put(UNITFILE, "file containing all phone units. Will be created by this module");
	}

	@Override
	public boolean compute() throws IOException, MaryConfigurationException {
		if (!unitlabelDir.exists()) {
			System.out.print(LABELDIR + " " + getProp(LABELDIR) + " does not exist; ");
			throw new Error("LABELDIR not found");
		}

		System.out.println("Back channel unitfile writer started...");
		BackChannelUnits bcUnits = new BackChannelUnits(unitlabelDir.getAbsolutePath(), this.bnlVocalizations);
		DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(unitFileName)));
		long posNumUnits = new MaryHeader(MaryHeader.LISTENERUNITS).writeTo(out);
		int numberOfBCUnits = bcUnits.getNumberOfUnits();
		out.writeInt(numberOfBCUnits);
		out.writeInt(samplingRate);
		long globalStart = 0l; // time, given as sample position with samplingRate
		for (int i = 0; i < numberOfBCUnits; i++) {
			UnitLabel[] fileLabels = bcUnits.getUnitLabels(i);
			double unitTimeSpan = bcUnits.getTimeSpan(i);
			int localLabLength = fileLabels.length;
			out.writeInt(localLabLength);
			for (int j = 0; j < localLabLength; j++) {
				double startTime = fileLabels[j].startTime;
				double endTime = fileLabels[j].endTime;
				double duration = endTime - startTime;
				long end = (long) (endTime * (double) (samplingRate));
				long start = (long) (startTime * (double) (samplingRate));
				out.writeLong(globalStart + start);
				out.writeInt((int) (end - start));
				out.writeInt(fileLabels[j].unitName.toCharArray().length);
				out.writeChars(fileLabels[j].unitName);
			}
			globalStart += ((long) ((double) (unitTimeSpan) * (double) (samplingRate)));
		}

		out.close();
		VocalizationUnitFileReader tester = new VocalizationUnitFileReader(unitFileName);
		int unitsOnDisk = tester.getNumberOfUnits();
		if (unitsOnDisk == numberOfBCUnits) {
			System.out.println("Can read right number of units: " + unitsOnDisk);
			return true;
		} else {
			System.out.println("Read wrong number of units: " + unitsOnDisk);
			return false;
		}
	}

	class BackChannelUnits {
		int numberOfUnits;
		UnitLabel[][] unitLabels;
		double[] unitTimeSpan;

		BackChannelUnits(String unitlabelDir, BasenameList basenameList) throws IOException {
			this.numberOfUnits = basenameList.getLength();
			unitLabels = new UnitLabel[this.numberOfUnits][];
			unitTimeSpan = new double[this.numberOfUnits];
			for (int i = 0; i < this.numberOfUnits; i++) {
				String fileName = unitlabelDir + File.separator + basenameList.getName(i) + unitlabelExt;
				ESTTrackReader pmFile = new ESTTrackReader(getProp(PMARKDIR) + File.separator + basenameList.getName(i)
						+ db.getProp(PMEXT));
				unitLabels[i] = readLabFile(fileName);
				unitTimeSpan[i] = pmFile.getTimeSpan();
			}
		}

		int getNumberOfUnits() {
			return this.numberOfUnits;
		}

		UnitLabel[] getUnitLabels(int i) {
			return this.unitLabels[i];
		}

		double getTimeSpan(int i) {
			return this.unitTimeSpan[i];
		}
	}

	class UnitLabel {
		String unitName;
		double startTime;
		double endTime;
		int unitIndex;

		public UnitLabel(String unitName, double startTime, double endTime, int unitIndex) {
			this.unitName = unitName;
			this.startTime = startTime;
			this.endTime = endTime;
			this.unitIndex = unitIndex;
		}
	}

	/**
	 * @param labFile
	 *            labFile
	 * @throws IOException
	 *             IOException
	 * @return ulab
	 */
	private UnitLabel[] readLabFile(String labFile) throws IOException {

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
		int unitIndex = 0;
		while ((line = labels.readLine()) != null) {
			labelUnit = null;
			if (line != null) {
				List labelUnitData = getLabelUnitData(line);
				if (labelUnitData == null)
					continue;
				labelUnit = (String) labelUnitData.get(2);
				unitIndex = Integer.parseInt((String) labelUnitData.get(1));
				endTimeStamp = Double.parseDouble((String) labelUnitData.get(0));
			}
			if (labelUnit == null)
				break;
			lines.add(labelUnit.trim() + " " + startTimeStamp + " " + endTimeStamp + " " + unitIndex);
			startTimeStamp = endTimeStamp;
		}
		labels.close();

		UnitLabel[] ulab = new UnitLabel[lines.size()];
		Iterator<String> itr = lines.iterator();
		for (int i = 0; itr.hasNext(); i++) {
			String element = itr.next();
			String[] wrds = element.split("\\s+");
			ulab[i] = new UnitLabel(wrds[0], (new Double(wrds[1])).doubleValue(), (new Double(wrds[2])).doubleValue(),
					(new Integer(wrds[3])).intValue());
		}
		return ulab;
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
	private ArrayList getLabelUnitData(String line) throws IOException {
		if (line == null)
			return null;
		if (line.trim().equals(""))
			return null;
		ArrayList unitData = new ArrayList();
		StringTokenizer st = new StringTokenizer(line.trim());
		// the first token is the time
		unitData.add(st.nextToken());
		// the second token is the unit index
		unitData.add(st.nextToken());
		// the third token is the phone
		unitData.add(st.nextToken());
		return unitData;
	}

	/**
	 * Provide the progress of computation, in percent, or -1 if that feature is not implemented.
	 * 
	 * @return -1 if not implemented, or an integer between 0 and 100.
	 */
	public int getProgress() {
		return percent;
	}

	public static void main(String[] args) throws Exception {
		VocalizationUnitfileWriter ufw = new VocalizationUnitfileWriter();
		new DatabaseLayout(ufw);
		ufw.compute();
	}

}
