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
package marytts.tools.voiceimport;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.util.SortedMap;
import java.util.TreeMap;

import marytts.unitselection.data.UnitFileReader;
import marytts.util.data.ESTTrackReader;
import marytts.util.data.MaryHeader;

public class PhoneUnitfileWriter extends VoiceImportComponent {
	protected File maryDir;
	protected String unitFileName;
	protected File unitlabelDir;
	protected int samplingRate;
	protected String pauseSymbol;
	protected PhoneLabelFeatureAligner aligner;

	protected String unitlabelExt = ".lab";

	protected DatabaseLayout db = null;
	protected int percent = 0;

	public String UNITFILE = "PhoneUnitfileWriter.unitFile";

	public String getName() {
		return "PhoneUnitfileWriter";
	}

	@Override
	protected void initialiseComp() throws Exception {
		maryDir = new File(db.getProp(DatabaseLayout.FILEDIR));

		samplingRate = Integer.parseInt(db.getProp(DatabaseLayout.SAMPLINGRATE));
		pauseSymbol = db.getAllophoneSet().getSilence().name();

		unitFileName = getProp(UNITFILE);
		unitlabelDir = new File(db.getProp(DatabaseLayout.PHONELABDIR));
		aligner = new PhoneLabelFeatureAligner();
		db.initialiseComponent(aligner);
	}

	public SortedMap<String, String> getDefaultProps(DatabaseLayout db) {
		this.db = db;
		if (props == null) {
			props = new TreeMap<String, String>();
			props.put(UNITFILE, db.getProp(DatabaseLayout.FILEDIR) + "phoneUnits" + db.getProp(DatabaseLayout.MARYEXT));
		}
		return props;
	}

	protected void setupHelp() {
		props2Help = new TreeMap<String, String>();
		props2Help.put(UNITFILE, "file containing all phone units. Will be created by this module");
	}

	public boolean compute() throws Exception {

		System.out.println("Unitfile writer started.");
		System.out.println("Verifying that unit feature and label files are perfectly aligned...");

		if (!aligner.compute())
			throw new IllegalStateException("Database is NOT perfectly aligned. Cannot create unit file.");
		System.out.println("OK, alignment verified.");
		DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(unitFileName)));
		long posNumUnits = new MaryHeader(MaryHeader.UNITS).writeTo(out);
		out.writeInt(-1); // number of units; needs to be corrected later.
		out.writeInt(samplingRate);

		// Loop over all utterances
		int index = 0; // the unique index number of units in the unit file
		long globalStart = 0l; // time, given as sample position with samplingRate
		long localStart = 0l; // time, given as sample position with samplingRate
		long totalNbrSamples = 0l;
		long localNbrSamples = 0l;
		long localNbrSamplesFromPM = 0l;
		ESTTrackReader pmFile = null;
		for (int i = 0; i < bnl.getLength(); i++) {
			percent = 100 * i / bnl.getLength();
			/* Open the relevant pitchmark file */
			pmFile = new ESTTrackReader(db.getProp(DatabaseLayout.PMDIR) + bnl.getName(i) + db.getProp(DatabaseLayout.PMEXT));
			// Output the utterance start marker: "null" unit
			out.writeLong(globalStart);
			out.writeInt(-1);
			index++;
			// Open the label file and reset the local time pointer
			BufferedReader labels = new BufferedReader(new InputStreamReader(new FileInputStream(new File(unitlabelDir,
					bnl.getName(i) + unitlabelExt)), "UTF-8"));
			String line;
			localNbrSamples = 0l;
			localStart = 0l;
			// Skip label file header
			while ((line = labels.readLine()) != null) {
				if (line.startsWith("#"))
					break; // line starting with "#" marks end of header
			}
			// Now read the actual units
			System.out.println("Start unit listing in file " + bnl.getName(i));
			while ((line = labels.readLine()) != null) {
				// Get the line and the double value in first position
				line = line.trim();
				if (line.equals(""))
					continue; // ignore empty lines
				String[] parts = line.split("\\s", 3);
				String unit_name = parts[2];
				double endTime = Double.valueOf(parts[0]).doubleValue();
				/* Relocate the label-specific end time on the nearest pitchmark */
				endTime = (double) (pmFile.getClosestTime(endTime));
				long end = (long) (endTime * (double) (samplingRate));
				// Output the unit
				int duration = (int) (end - localStart);
				out.writeLong(globalStart + localStart);
				out.writeInt(duration);
				// System.out.println( "Unit [" + index + "] starts at [" + localStart + "] and has duration [" + duration + "]."
				// );
				System.out.println("Unit [" + index + "] <" + unit_name + "> starts at [" + (double) localStart / (samplingRate)
						+ "] and has duration [" + (double) duration / (samplingRate) + "].");
				// Update various pointers
				localStart = end;
				index++;
				localNbrSamples += duration;
				totalNbrSamples += duration;
			}
			// Output the utterance end marker: "null" unit
			out.writeLong(globalStart + localStart);
			out.writeInt(-1);
			index++;
			/*
			 * Locate the global start of the next file: this corrects the discrepancy between the duration of the label file and
			 * the duration of the pitchmark file (which is considered as the authority).
			 */
			localNbrSamplesFromPM = (long) ((double) (pmFile.getTimeSpan()) * (double) (samplingRate));
			globalStart += localNbrSamplesFromPM;
			/* Clean the house */
			labels.close();
			System.out.println("    " + bnl.getName(i) + " (" + index + ") (This file has [" + localNbrSamples
					+ "] samples from .lab, rectified to [" + localNbrSamplesFromPM + "] from the pitchmarks, diff ["
					+ (localNbrSamplesFromPM - localNbrSamples) + "], cumul [" + globalStart + "])");
			if ((localNbrSamplesFromPM - localNbrSamples) < 0)
				System.out.println("BORK BORK BORK: .lab file longer than pitchmarks !");
		}
		out.close();
		// Now index is the number of units. Set this in the file:
		RandomAccessFile raf = new RandomAccessFile(unitFileName, "rw");
		raf.seek(posNumUnits);
		raf.writeInt(index);
		raf.close();
		System.out.println("Number of processed units: " + index);

		UnitFileReader tester = new UnitFileReader(unitFileName);
		int unitsOnDisk = tester.getNumberOfUnits();
		if (unitsOnDisk == index) {
			System.out.println("Can read right number of units");
			return true;
		} else {
			System.out.println("Read wrong number of units: " + unitsOnDisk);
			return false;
		}
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
		PhoneUnitfileWriter ufw = new PhoneUnitfileWriter();
		new DatabaseLayout(ufw);
		ufw.compute();
	}

}
