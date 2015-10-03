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
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.SortedMap;
import java.util.TreeMap;

import marytts.exceptions.MaryConfigurationException;
import marytts.features.FeatureDefinition;
import marytts.features.FeatureVector;
import marytts.unitselection.data.FeatureFileReader;
import marytts.unitselection.data.UnitFileReader;
import marytts.util.data.MaryHeader;

public class PhoneFeatureFileWriter extends VoiceImportComponent {
	protected File maryDir;
	protected FeatureDefinition featureDefinition;
	protected int percent = 0;
	protected String featureExt = ".pfeats";
	protected UnitFileReader unitFileReader;

	protected String name = "PhoneFeatureFileWriter";

	public String FEATUREDIR = "PhoneFeatureFileWriter.featureDir";
	public String FEATUREFILE = "PhoneFeatureFileWriter.featureFile";
	public String UNITFILE = "PhoneFeatureFileWriter.unitFile";
	public String WEIGHTSFILE = "PhoneFeatureFileWriter.weightsFile";

	public String getName() {
		return name;
	}

	@Override
	protected void initialiseComp() {
		File unitfeatureDir = new File(getProp(FEATUREDIR));
		if (!unitfeatureDir.exists()) {
			System.out.print(FEATUREDIR + " " + getProp(FEATUREDIR) + " does not exist; ");
			if (!unitfeatureDir.mkdir()) {
				throw new Error("Could not create FEATUREDIR");
			}
			System.out.print("Created successfully.\n");
		}
	}

	public SortedMap getDefaultProps(DatabaseLayout db) {
		if (props == null) {
			props = new TreeMap();
			props.put(FEATUREDIR, db.getProp(db.ROOTDIR) + "phonefeatures" + System.getProperty("file.separator"));
			props.put(FEATUREFILE, db.getProp(db.FILEDIR) + "phoneFeatures" + db.getProp(db.MARYEXT));
			props.put(UNITFILE, db.getProp(db.FILEDIR) + "phoneUnits" + db.getProp(db.MARYEXT));
			props.put(WEIGHTSFILE, db.getProp(db.CONFIGDIR) + "phoneUnitFeatureDefinition.txt");
		}
		return props;
	}

	protected void setupHelp() {
		props2Help = new TreeMap();
		props2Help.put(FEATUREDIR, "directory containing the phone features");
		props2Help.put(FEATUREFILE, "file containing all phone units and their target cost features."
				+ "Will be created by this module");
		props2Help.put(UNITFILE, "file containing all phone units");
		props2Help.put(WEIGHTSFILE, "file containing the list of phone target cost features, their values and weights");

	}

	@Override
	public boolean compute() throws IOException, MaryConfigurationException {
		// make sure that we have a featureweightsfile
		File featWeights = new File(getProp(WEIGHTSFILE));
		if (!featWeights.exists() || featWeights.length() == 0) {
			PrintWriter featWeightsOut = new PrintWriter(new OutputStreamWriter(new FileOutputStream(featWeights), "UTF-8"), true);
			BufferedReader uttFeats = new BufferedReader(new InputStreamReader(new FileInputStream(new File(getProp(FEATUREDIR)
					+ bnl.getName(0) + featureExt)), "UTF-8"));
			FeatureDefinition featDef = new FeatureDefinition(uttFeats, false); // false: do not read weights
			uttFeats.close();
			featDef.generateFeatureWeightsFile(featWeightsOut);
		}
		System.out.println("Featurefile writer started.");
		unitFileReader = new UnitFileReader(getProp(UNITFILE));
		featureDefinition = new FeatureDefinition(new BufferedReader(new InputStreamReader(new FileInputStream(
				getProp(WEIGHTSFILE)), "UTF-8")), true); // true: read weights

		DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(getProp(FEATUREFILE))));
		writeHeaderTo(out);
		writeUnitFeaturesTo(out);
		out.close();
		System.out.println("Number of processed units: " + unitFileReader.getNumberOfUnits());

		FeatureFileReader tester = FeatureFileReader.getFeatureFileReader(getProp(FEATUREFILE));
		int unitsOnDisk = tester.getNumberOfUnits();
		if (unitsOnDisk == unitFileReader.getNumberOfUnits()) {
			System.out.println("Can read right number of units");
			return true;
		} else {
			System.out.println("Read wrong number of units: " + unitsOnDisk);
			return false;
		}
	}

	/**
	 * @param out
	 *            out
	 * @throws IOException
	 *             IOException
	 * @throws UnsupportedEncodingException
	 *             UnsupportedEncodingException
	 * @throws FileNotFoundException
	 *             FileNotFoundException
	 */
	protected void writeUnitFeaturesTo(DataOutput out) throws IOException, UnsupportedEncodingException, FileNotFoundException {
		int numUnits = unitFileReader.getNumberOfUnits();
		out.writeInt(numUnits);
		System.out.println("Number of units : " + numUnits);
		int index = 0; // the unique index number of units in the unit file
		// Dummy feature vector corresponding to an edge unit:
		FeatureVector start = featureDefinition.createEdgeFeatureVector(0, true);
		FeatureVector end = featureDefinition.createEdgeFeatureVector(0, false);
		// Loop over all utterances
		for (int i = 0; i < bnl.getLength(); i++) {
			percent = 100 * i / bnl.getLength();
			System.out.print("    " + bnl.getName(i) + " : Entering at index (" + index + ") -- ");
			BufferedReader uttFeats = new BufferedReader(new InputStreamReader(new FileInputStream(new File(getProp(FEATUREDIR)
					+ bnl.getName(i) + featureExt)), "UTF-8"));
			FeatureDefinition uttFeatDefinition = new FeatureDefinition(uttFeats, false); // false: do not read weights
			if (!uttFeatDefinition.featureEquals(featureDefinition)) {
				throw new IllegalArgumentException("Features in file " + bnl.getName(i) + " do not match definition file "
						+ getProp(WEIGHTSFILE) + " because:\n" + uttFeatDefinition.featureEqualsAnalyse(featureDefinition));
			}
			// skip the clear text section: read until an empty line occurs
			String line;
			while ((line = uttFeats.readLine()) != null) {
				if (line.trim().equals(""))
					break;
			}
			// Check the index consistency
			if (index > numUnits) {
				throw new IOException("Inconsistency between feature files and unit file: " + "the reached index [" + index
						+ "] is bigger than the number of units in the unit file [" + numUnits + "] !");
			}
			// Empty entry corresponding to start of utterance:
			if (!unitFileReader.isEdgeUnit(index)) {
				// System.out.println( "Unit [" + index + "] : StarTime [" + unitFileReader.getStartTime(i) + "] Duration [" +
				// unitFileReader.getDuration(i) + "]." );
				throw new IOException("Inconsistency between feature files and unit file: Unit " + index
						+ " should correspond to start of file " + bnl.getName(i) + ", but is not an edge unit!");
			}
			start.writeTo(out);
			index++;
			// Check the index consistency
			if (index > numUnits) {
				throw new IOException("Inconsistency between feature files and unit file: " + "the reached index [" + index
						+ "] is bigger than the number of units in the unit file [" + numUnits + "] !");
			}
			// read the binary section, and write it
			while ((line = uttFeats.readLine()) != null) {
				if (line.trim().equals(""))
					break;
				FeatureVector fv = featureDefinition.toFeatureVector(0, line);
				if (unitFileReader.isEdgeUnit(index)) {
					throw new IOException("Inconsistency between feature files and unit file: Unit " + index + "("
							+ fv.getFeatureAsString(0, featureDefinition) + ") should correspond to feature line '" + line
							+ "' of file " + bnl.getName(i) + ", but is an edge unit!");
				}
				fv.writeTo(out);
				index++;
			}
			// Check the index consistency
			if (index > numUnits) {
				throw new IOException("Inconsistency between feature files and unit file: " + "the reached index [" + index
						+ "] is bigger than the number of units in the unit file [" + numUnits + "] !");
			}
			if (!unitFileReader.isEdgeUnit(index)) {
				throw new IOException("Inconsistency between feature files and unit file: Unit " + index
						+ " should correspond to end of file " + bnl.getName(i) + ", but is not an edge unit!");
			}
			end.writeTo(out);
			index++;
			System.out.println("Exiting at index (" + index + ").");
			System.out.flush();
			uttFeats.close();
		}
	}

	/**
	 * Write the header of this feature file to the given DataOutput
	 * 
	 * @param out
	 *            out
	 * @throws IOException
	 *             IOException
	 */
	protected void writeHeaderTo(DataOutput out) throws IOException {
		new MaryHeader(MaryHeader.UNITFEATS).writeTo(out);
		featureDefinition.writeBinaryTo(out);
	}

	/**
	 * Provide the progress of computation, in percent, or -1 if that feature is not implemented.
	 * 
	 * @return -1 if not implemented, or an integer between 0 and 100.
	 */
	public int getProgress() {
		return percent;
	}

	/**
	 * @param args
	 *            args
	 * @throws Exception
	 *             Exception
	 */
	public static void main(String[] args) throws Exception {
		PhoneFeatureFileWriter ffw = new PhoneFeatureFileWriter();
		DatabaseLayout db = new DatabaseLayout(ffw);
		ffw.compute();
	}

}
