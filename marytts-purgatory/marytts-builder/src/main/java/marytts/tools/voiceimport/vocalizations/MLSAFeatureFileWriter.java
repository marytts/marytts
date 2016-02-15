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
package marytts.tools.voiceimport.vocalizations;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.SortedMap;
import java.util.TreeMap;

import marytts.exceptions.MaryConfigurationException;
import marytts.tools.voiceimport.DatabaseLayout;
import marytts.tools.voiceimport.VoiceImportComponent;
import marytts.util.data.MaryHeader;
import marytts.util.io.BasenameList;
import marytts.util.io.LEDataInputStream;
import marytts.util.math.MathUtils;
import marytts.vocalizations.MLSAFeatureFileReader;
import marytts.vocalizations.VocalizationUnitFileReader;

/**
 * A component to write all MLSA features (logf0, mgc and strengths) into a single file
 * 
 * @author sathish
 * 
 */
public class MLSAFeatureFileWriter extends VoiceImportComponent {

	private String lf0Ext = ".lf0";
	private String strExt = ".str";
	private String mgcExt = ".mgc";
	private int progress = 0;
	protected VocalizationUnitFileReader listenerUnits;

	private int MGCORDER = 25; // MGCORDER is actually 24 plus 1 include MGC[0]
	private int STRORDER = 5;
	private int LF0ORDER = 1;

	protected DatabaseLayout db = null;
	protected BasenameList bnlVocalizations;

	public final String MLSADIR = getName() + ".vocalizationMLSAFilesDir";
	public final String UNITFILE = getName() + ".unitFile";
	public final String OUTMLSAFILE = getName() + ".mlsaOutputFile";

	@Override
	public SortedMap<String, String> getDefaultProps(DatabaseLayout db) {
		this.db = db;
		if (props == null) {
			props = new TreeMap();
			String mlsaDir = db.getProp(db.VOCALIZATIONSDIR) + "mlsa";
			props.put(MLSADIR, mlsaDir);
			props.put(UNITFILE, db.getProp(db.VOCALIZATIONSDIR) + File.separator + "files" + File.separator
					+ "vocalization_units" + db.getProp(db.MARYEXT));
			props.put(OUTMLSAFILE, db.getProp(db.VOCALIZATIONSDIR) + File.separator + "files" + File.separator
					+ "vocalization_mlsa_features" + db.getProp(db.MARYEXT));
		}
		return props;
	}

	@Override
	protected void setupHelp() {

		if (props2Help == null) {
			props2Help = new TreeMap();
			props2Help.put(MLSADIR, "mlsa features directory");
			props2Help.put(UNITFILE, "unit file representing all vocalizations");
			props2Help.put(OUTMLSAFILE, "a single file to write all mlsa features");
		}

	}

	@Override
	public boolean compute() throws Exception {

		listenerUnits = new VocalizationUnitFileReader(getProp(UNITFILE));

		// write features into timeline file
		DataOutputStream out = new DataOutputStream(
				new BufferedOutputStream(new FileOutputStream(new File(getProp(OUTMLSAFILE)))));
		writeHeaderTo(out);
		writeUnitFeaturesTo(out);
		out.close();

		MLSAFeatureFileReader tester = new MLSAFeatureFileReader(getProp(OUTMLSAFILE));
		int unitsOnDisk = tester.getNumberOfUnits();
		if (unitsOnDisk == listenerUnits.getNumberOfUnits()) {
			System.out.println("Can read right number of units");
			return true;
		} else {
			System.out.println("Read wrong number of units: " + unitsOnDisk);
			return false;
		}
	}

	/**
	 * write all features into a single file
	 * 
	 * @param out
	 *            DataOutputStream
	 * @throws IOException
	 *             if it can't write data into DataOutputStream
	 */
	private void writeUnitFeaturesTo(DataOutputStream out) throws IOException {

		int numUnits = listenerUnits.getNumberOfUnits();
		out.writeInt(numUnits); // write number of units
		out.writeInt(this.LF0ORDER); // write lf0 order
		out.writeInt(this.MGCORDER); // write MGC order
		out.writeInt(this.STRORDER); // write STR order

		assert bnlVocalizations.getLength() == numUnits : "number of units and size of basename list should be same";

		for (int i = 0; i < bnlVocalizations.getLength(); i++) {

			String mgcFile = getProp(MLSADIR) + File.separator + bnlVocalizations.getName(i) + mgcExt;
			String lf0File = getProp(MLSADIR) + File.separator + bnlVocalizations.getName(i) + lf0Ext;
			String strFile = getProp(MLSADIR) + File.separator + bnlVocalizations.getName(i) + strExt;

			LEDataInputStream lf0Data = new LEDataInputStream(new BufferedInputStream(new FileInputStream(lf0File)));
			LEDataInputStream mgcData = new LEDataInputStream(new BufferedInputStream(new FileInputStream(mgcFile)));
			LEDataInputStream strData = new LEDataInputStream(new BufferedInputStream(new FileInputStream(strFile)));

			int mgcSize = (int) ((new File(mgcFile)).length() / 4); // 4 bytes for float
			int lf0Size = (int) ((new File(lf0File)).length() / 4); // 4 bytes for float
			int strSize = (int) ((new File(strFile)).length() / 4); // 4 bytes for float

			float sizes[] = new float[3];
			int n = 0;
			sizes[n++] = mgcSize / (this.MGCORDER);
			sizes[n++] = lf0Size;
			sizes[n++] = strSize / this.STRORDER;
			int numberOfFrames = (int) MathUtils.getMin(sizes);
			out.writeInt(numberOfFrames); // number of frames in this vocalization

			// first write LF0 data
			int numberOfLF0Frames = numberOfFrames;
			out.writeInt(numberOfLF0Frames); // number of LF0 frames
			for (int j = 0; j < numberOfLF0Frames; j++) {
				out.writeFloat(lf0Data.readFloat());
			}

			// second write MGC data
			int numberOfMGCFrames = numberOfFrames * (this.MGCORDER);
			out.writeInt(numberOfMGCFrames); // number of MGC frames
			for (int j = 0; j < numberOfMGCFrames; j++) {
				out.writeFloat(mgcData.readFloat());
			}

			// Third write STR data
			int numberOfSTRFrames = numberOfFrames * this.STRORDER;
			out.writeInt(numberOfSTRFrames); // number of MGC frames
			for (int j = 0; j < numberOfSTRFrames; j++) {
				out.writeFloat(strData.readFloat());
			}
		}
	}

	/**
	 * Initialize this component
	 * 
	 * @throws Exception
	 *             if there is problem with basename list
	 */
	@Override
	protected void initialiseComp() throws Exception {

		String timelineDir = db.getProp(db.VOCALIZATIONSDIR) + File.separator + "files";
		createDirectoryifNotExists(timelineDir);

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
			throw new MaryConfigurationException("Problem with basename list " + e);
		}
	}

	/**
	 * Write the header of this feature file to the given DataOutput
	 * 
	 * @param out
	 *            DataOutput to write header
	 * @throws IOException
	 *             if it can't write data into DataOutput
	 */
	protected void writeHeaderTo(DataOutput out) throws IOException {
		new MaryHeader(MaryHeader.LISTENERFEATS).writeTo(out);
	}

	/**
	 * Create new directory if the directory doesn't exist
	 * 
	 * @param dirName
	 *            directory path
	 * @throws Exception
	 *             if it fails
	 */
	private void createDirectoryifNotExists(String dirName) throws Exception {
		if (!(new File(dirName)).exists()) {
			System.out.println(dirName + " directory does not exist; ");
			if (!(new File(dirName)).mkdirs()) {
				throw new Exception("Could not create directory " + dirName);
			}
			System.out.println("Created successfully.\n");
		}
	}

	@Override
	public String getName() {
		return "MLSAFeatureFileWriter";
	}

	@Override
	public int getProgress() {
		return this.progress;
	}

	/**
	 * @param args
	 *            args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}
}
