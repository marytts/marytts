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
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.SortedMap;
import java.util.TreeMap;

import marytts.exceptions.MaryConfigurationException;
import marytts.unitselection.data.SCostFileReader;
import marytts.unitselection.data.UnitFileReader;
import marytts.util.data.MaryHeader;

public class SCostUnitFileWriter extends VoiceImportComponent {
	protected File maryDir;
	protected String unitFileName;
	protected File unitlabelDir;
	protected int samplingRate;
	protected String pauseSymbol;
	protected PhoneLabelFeatureAligner aligner;
	protected String unitlabelExt = ".hplab";
	protected DatabaseLayout db = null;
	protected int percent = 0;

	public String LABELDIR = "SCostUnitFileWriter.sCostlabelDir";
	public String UNITFILE = "SCostUnitFileWriter.halfphoneunitFile";
	public String SCOSTFILE = "SCostUnitFileWriter.sCostFile";

	public String getName() {
		return "SCostUnitFileWriter";
	}

	@Override
	protected void initialiseComp() throws Exception {
		maryDir = new File(db.getProp(db.FILEDIR));

		samplingRate = Integer.parseInt(db.getProp(db.SAMPLINGRATE));
		pauseSymbol = System.getProperty("pause.symbol", "pau");

		unitFileName = getProp(UNITFILE);
		unitlabelDir = new File(getProp(LABELDIR));
		if (!unitlabelDir.exists()) {
			System.out.print(LABELDIR + " " + getProp(LABELDIR) + " does not exist; ");
			if (!unitlabelDir.mkdir()) {
				throw new Error("Could not create LABELDIR");
			}
			System.out.print("Created successfully.\n");
		}
		aligner = new PhoneLabelFeatureAligner();
		db.initialiseComponent(aligner);
	}

	public SortedMap getDefaultProps(DatabaseLayout db) {
		this.db = db;
		if (props == null) {
			props = new TreeMap();
			String rootDir = db.getProp(db.ROOTDIR);
			props.put(LABELDIR, rootDir + "sCost" + File.separator + "scostlabels" + System.getProperty("file.separator"));
			props.put(UNITFILE, db.getProp(db.FILEDIR) + "halfphoneUnits" + db.getProp(db.MARYEXT));
			props.put(SCOSTFILE, db.getProp(db.FILEDIR) + "sCostUnitsFile" + db.getProp(db.MARYEXT));

		}
		return props;
	}

	protected void setupHelp() {
		props2Help = new TreeMap();
		props2Help.put(LABELDIR, "directory containing the phone labels");
		props2Help.put(UNITFILE, "file containing all half phone units");
		props2Help.put(SCOSTFILE, "file containing scost of halfphone units. Will be created by this module");
	}

	@Override
	public boolean compute() throws IOException, MaryConfigurationException {
		if (!unitlabelDir.exists()) {
			System.out.print(LABELDIR + " " + getProp(LABELDIR) + " does not exist; ");
			throw new Error("Could not create LABELDIR");
		}

		ArrayList<Double> sCostList = new ArrayList<Double>();
		for (int i = 0; i < bnl.getLength(); i++) {
			System.out.println("reading..." + bnl.getName(i));
			percent = 100 * i / bnl.getLength();
			String labFile = unitlabelDir + File.separator + bnl.getName(i) + unitlabelExt;
			UnitLabel[] uttData = UnitLabel.readLabFile(labFile);
			sCostList.add(0.0);
			for (int j = 0; j < uttData.length; j++) {
				sCostList.add(uttData[j].getSCost());
			}
			sCostList.add(0.0);
		}
		int numberOfUnits = sCostList.size();

		UnitFileReader units = new UnitFileReader(getProp(UNITFILE));
		// int noOfUnits = units.getNumberOfUnits();
		if (numberOfUnits == units.getNumberOfUnits()) {
			System.out.println("Number of units in SCost file: " + numberOfUnits + " and no. of units in units file: "
					+ units.getNumberOfUnits());
		} else {
			System.out.println("ERROR: Number of units in SCost file: " + numberOfUnits + " and no. of units in units file: "
					+ units.getNumberOfUnits());
			return false;
		}
		System.out.println(numberOfUnits + " --> " + units.getNumberOfUnits());

		/**
		 * TODO : change file path
		 * 
		 */
		DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(getProp(SCOSTFILE))));
		long posNumUnits = new MaryHeader(MaryHeader.SCOST).writeTo(out);
		out.writeInt(numberOfUnits);

		for (int i = 0; i < numberOfUnits; i++) {
			double sCost = sCostList.get(i).doubleValue();
			out.writeFloat((float) sCost);
		}

		out.close();

		SCostFileReader tester = new SCostFileReader(getProp(SCOSTFILE));
		int unitsOnDisk = tester.getNumberOfUnits();
		if (unitsOnDisk == numberOfUnits) {
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
		SCostUnitFileWriter ufw = new SCostUnitFileWriter();
		new DatabaseLayout(ufw);
		ufw.compute();
	}

}
