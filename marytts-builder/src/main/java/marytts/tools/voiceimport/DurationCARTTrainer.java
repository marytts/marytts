/**
 * Copyright 2007 DFKI GmbH.
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
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.SortedMap;
import java.util.TreeMap;

import marytts.cart.CART;
import marytts.cart.Node;
import marytts.cart.LeafNode.LeafType;
import marytts.cart.io.MaryCARTWriter;
import marytts.cart.io.WagonCARTReader;
import marytts.exceptions.MaryConfigurationException;
import marytts.features.FeatureDefinition;
import marytts.unitselection.data.FeatureFileReader;
import marytts.unitselection.data.HnmTimelineReader;
import marytts.unitselection.data.TimelineReader;
import marytts.unitselection.data.Unit;
import marytts.unitselection.data.UnitFileReader;

/**
 * A class which converts a text file in festvox format into a one-file-per-utterance format in a given directory.
 * 
 * @author schroed
 *
 */
public class DurationCARTTrainer extends VoiceImportComponent {
	protected File unitlabelDir;
	protected File unitfeatureDir;
	protected File durationDir;
	protected File durationFeatsFile;
	protected File durationDescFile;
	protected File wagonTreeFile;
	protected DatabaseLayout db = null;
	protected int percent = 0;
	protected boolean useStepwiseTraining = false;

	private final String name = "DurationCARTTrainer";
	public final String DURTREE = name + ".durTree";
	public final String STEPWISETRAINING = name + ".stepwiseTraining";
	public final String FEATUREFILE = name + ".featureFile";
	public final String UNITFILE = name + ".unitFile";
	public final String WAVETIMELINE = name + ".waveTimeline";
	public final String ISHNMTIMELINE = name + ".isHnmTimeline";

	public String getName() {
		return name;
	}

	@Override
	protected void initialiseComp() {
		this.unitlabelDir = new File(db.getProp(DatabaseLayout.PHONELABDIR));
		this.unitfeatureDir = new File(db.getProp(DatabaseLayout.PHONEFEATUREDIR));
		String durDir = db.getProp(DatabaseLayout.TEMPDIR);
		this.durationDir = new File(durDir);
		if (!durationDir.exists()) {
			System.out.print("temp dir " + durDir + " does not exist; ");
			if (!durationDir.mkdir()) {
				throw new Error("Could not create DURDIR");
			}
			System.out.print("Created successfully.\n");
		}
		this.durationFeatsFile = new File(durDir + "dur.feats");
		this.durationDescFile = new File(durDir + "dur.desc");
		this.wagonTreeFile = new File(durDir + "dur.tree");
		this.useStepwiseTraining = Boolean.valueOf(getProp(STEPWISETRAINING)).booleanValue();

	}

	public SortedMap<String, String> getDefaultProps(DatabaseLayout dbl) {
		this.db = dbl;
		if (props == null) {
			props = new TreeMap<String, String>();
			props.put(STEPWISETRAINING, "false");
			props.put(FEATUREFILE, db.getProp(DatabaseLayout.FILEDIR) + "phoneFeatures" + db.getProp(DatabaseLayout.MARYEXT));
			props.put(UNITFILE, db.getProp(DatabaseLayout.FILEDIR) + "phoneUnits" + db.getProp(DatabaseLayout.MARYEXT));
			props.put(WAVETIMELINE,
					db.getProp(DatabaseLayout.FILEDIR) + "timeline_waveforms" + db.getProp(DatabaseLayout.MARYEXT));
			props.put(ISHNMTIMELINE, "false");
			props.put(DURTREE, db.getProp(DatabaseLayout.FILEDIR) + "dur.tree");
		}
		return props;
	}

	protected void setupHelp() {
		props2Help = new TreeMap<String, String>();
		props2Help.put(STEPWISETRAINING, "\"false\" or \"true\" ???????????????????????????????????????????????????????????");
		props2Help.put(FEATUREFILE, "file containing all phone units and their target cost features");
		props2Help.put(UNITFILE, "file containing all phone units");
		props2Help.put(WAVETIMELINE, "file containing all waveforms or models that can genarate them");
		props2Help.put(ISHNMTIMELINE, "file containing all wave files");
		props2Help.put(DURTREE, "file containing the duration CART. Will be created by this module");
	}

	@Override
	public boolean compute() throws IOException, MaryConfigurationException {
		FeatureFileReader featureFile = FeatureFileReader.getFeatureFileReader(getProp(FEATUREFILE));
		UnitFileReader unitFile = new UnitFileReader(getProp(UNITFILE));
		TimelineReader waveTimeline = null;
		if (getProp(ISHNMTIMELINE).compareToIgnoreCase("true") == 0)
			waveTimeline = new HnmTimelineReader(getProp(WAVETIMELINE));
		else
			waveTimeline = new TimelineReader(getProp(WAVETIMELINE));

		PrintWriter toFeaturesFile = new PrintWriter(new FileOutputStream(durationFeatsFile));
		System.out.println("Duration CART trainer: exporting duration features");

		FeatureDefinition featureDefinition = featureFile.getFeatureDefinition();
		int nUnits = 0;
		for (int i = 0, len = unitFile.getNumberOfUnits(); i < len; i++) {
			// We estimate that feature extraction takes 1/10 of the total time
			// (that's probably wrong, but never mind)
			percent = 10 * i / len;
			Unit u = unitFile.getUnit(i);
			float dur = u.duration / (float) unitFile.getSampleRate();
			if (dur >= 0.01) { // enforce a minimum duration for training data
				toFeaturesFile.println(dur + " " + featureDefinition.toFeatureString(featureFile.getFeatureVector(i)));
				nUnits++;
			}
		}
		if (useStepwiseTraining)
			percent = 1;
		else
			percent = 10;
		toFeaturesFile.close();
		System.out.println("Duration features extracted for " + nUnits + " units");

		PrintWriter toDesc = new PrintWriter(new FileOutputStream(durationDescFile));
		generateFeatureDescriptionForWagon(featureDefinition, toDesc);
		toDesc.close();

		boolean ok = false;
		// Now, call wagon
		WagonCaller wagonCaller = new WagonCaller(db.getProp(DatabaseLayout.ESTDIR), null);
		if (useStepwiseTraining) {
			// Split the data set in training and test part:
			// TODO: hardcoded path = EVIL
			Process traintest = Runtime.getRuntime().exec(
					"/project/mary/Festival/festvox/src/general/traintest " + durationFeatsFile.getAbsolutePath());
			try {
				traintest.waitFor();
			} catch (InterruptedException ie) {
			}
			ok = wagonCaller.callWagon("-data " + durationFeatsFile.getAbsolutePath() + ".train" + " -test "
					+ durationFeatsFile.getAbsolutePath() + ".test -stepwise" + " -desc " + durationDescFile.getAbsolutePath()
					+ " -stop 10 " + " -output " + wagonTreeFile.getAbsolutePath());
		} else {
			ok = wagonCaller.callWagon("-data " + durationFeatsFile.getAbsolutePath() + " -desc "
					+ durationDescFile.getAbsolutePath() + " -stop 10 " + " -output " + wagonTreeFile.getAbsolutePath());
		}
		if (ok) {
			String destinationFile = getProp(DURTREE);
			WagonCARTReader wagonDURReader = new WagonCARTReader(LeafType.FloatLeafNode);
			Node rootNode = wagonDURReader.load(new BufferedReader(new FileReader(wagonTreeFile)), featureDefinition);
			CART durCart = new CART(rootNode, featureDefinition);
			MaryCARTWriter wwdur = new MaryCARTWriter();
			wwdur.dumpMaryCART(durCart, destinationFile);
		}
		percent = 100;
		return ok;

	}

	private void generateFeatureDescriptionForWagon(FeatureDefinition fd, PrintWriter out) {
		out.println("(");
		out.println("(segment_duration float)");
		int nDiscreteFeatures = fd.getNumberOfByteFeatures() + fd.getNumberOfShortFeatures();
		for (int i = 0, n = fd.getNumberOfFeatures(); i < n; i++) {
			out.print("( ");
			out.print(fd.getFeatureName(i));
			if (i < nDiscreteFeatures) { // list values
				if (fd.getNumberOfValues(i) == 20 && fd.getFeatureValueAsString(i, 19).equals("19")) {
					// one of our pseudo-floats
					out.println(" float )");
				} else { // list the values
					for (int v = 0, vmax = fd.getNumberOfValues(i); v < vmax; v++) {
						out.print("  ");
						String val = fd.getFeatureValueAsString(i, v);
						if (val.indexOf('"') != -1) {
							StringBuilder buf = new StringBuilder();
							for (int c = 0; c < val.length(); c++) {
								char ch = val.charAt(c);
								if (ch == '"')
									buf.append("\\\"");
								else
									buf.append(ch);
							}
							val = buf.toString();
						}
						out.print("\"" + val + "\"");
					}
					out.println(" )");
				}
			} else { // float feature
				out.println(" float )");
			}

		}
		out.println(")");
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
		DurationCARTTrainer dct = new DurationCARTTrainer();
		DatabaseLayout db = new DatabaseLayout(dct);
		dct.compute();
	}

}
