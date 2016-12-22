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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.SortedMap;
import java.util.StringTokenizer;
import java.util.TreeMap;

import marytts.cart.CART;
import marytts.cart.LeafNode.LeafType;
import marytts.cart.io.MaryCARTWriter;
import marytts.cart.io.WagonCARTReader;
import marytts.exceptions.MaryConfigurationException;
import marytts.features.FeatureDefinition;
import marytts.features.FeatureVector;
import marytts.unitselection.data.FeatureFileReader;
import marytts.unitselection.data.HnmTimelineReader;
import marytts.unitselection.data.TimelineReader;
import marytts.unitselection.data.UnitFileReader;
import marytts.util.data.Datagram;
import marytts.util.string.PrintfFormat;

/**
 * A class which converts a text file in festvox format into a one-file-per-utterance format in a given directory.
 * 
 * @author schroed
 *
 */
public class F0CARTTrainer extends VoiceImportComponent {
	protected File f0Dir;
	protected File leftF0FeaturesFile;
	protected File midF0FeaturesFile;
	protected File rightF0FeaturesFile;
	protected File f0DescFile;
	protected File wagonLeftTreeFile;
	protected File wagonMidTreeFile;
	protected File wagonRightTreeFile;
	protected String featureExt = ".pfeats";
	protected DatabaseLayout db = null;
	protected int percent = 0;
	protected boolean useStepwiseTraining = false;

	private final String name = "F0CARTTrainer";
	public final String STEPWISETRAINING = name + ".stepwiseTraining";
	public final String FEATUREFILE = name + ".featureFile";
	public final String UNITFILE = name + ".unitFile";
	public final String WAVETIMELINE = name + ".waveTimeline";
	public final String ISHNMTIMELINE = name + ".isHnmTimeline";
	public final String F0LEFTTREEFILE = name + ".f0LeftTreeFile";
	public final String F0RIGHTTREEFILE = name + ".f0RightTreeFile";
	public final String F0MIDTREEFILE = name + ".f0MidTreeFile";

	public F0CARTTrainer() {
		setupHelp();
	}

	public String getName() {
		return name;
	}

	@Override
	protected void initialiseComp() {
		String rootDir = db.getProp(DatabaseLayout.ROOTDIR);
		String f0DirName = db.getProp(DatabaseLayout.TEMPDIR);
		this.f0Dir = new File(f0DirName);
		if (!f0Dir.exists()) {
			System.out.print("temp dir " + f0DirName + " does not exist; ");
			if (!f0Dir.mkdir()) {
				throw new Error("Could not create F0DIR");
			}
			System.out.print("Created successfully.\n");
		}
		this.leftF0FeaturesFile = new File(f0Dir, "f0.left.feats");
		this.midF0FeaturesFile = new File(f0Dir, "f0.mid.feats");
		this.rightF0FeaturesFile = new File(f0Dir, "f0.right.feats");
		this.f0DescFile = new File(f0Dir, "f0.desc");
		this.wagonLeftTreeFile = new File(f0Dir, "f0.left.tree");
		this.wagonMidTreeFile = new File(f0Dir, "f0.mid.tree");
		this.wagonRightTreeFile = new File(f0Dir, "f0.right.tree");
		this.useStepwiseTraining = Boolean.valueOf(getProp(STEPWISETRAINING)).booleanValue();
	}

	public SortedMap<String, String> getDefaultProps(DatabaseLayout dbl) {
		this.db = dbl;
		if (props == null) {
			props = new TreeMap<String, String>();
			String filedir = db.getProp(DatabaseLayout.FILEDIR);
			String maryext = db.getProp(DatabaseLayout.MARYEXT);
			props.put(STEPWISETRAINING, "false");
			props.put(FEATUREFILE, filedir + "phoneFeatures" + maryext);
			props.put(UNITFILE, filedir + "phoneUnits" + maryext);
			props.put(WAVETIMELINE,
					db.getProp(DatabaseLayout.FILEDIR) + "timeline_waveforms" + db.getProp(DatabaseLayout.MARYEXT));
			props.put(ISHNMTIMELINE, "false");
			props.put(F0LEFTTREEFILE, filedir + "f0.left.tree");
			props.put(F0RIGHTTREEFILE, filedir + "f0.right.tree");
			props.put(F0MIDTREEFILE, filedir + "f0.mid.tree");
		}
		return props;
	}

	protected void setupHelp() {
		props2Help = new TreeMap<String, String>();
		props2Help.put(STEPWISETRAINING, "\"false\" or \"true\" ????????????????????????");
		props2Help.put(FEATUREFILE, "file containing all phone units and their target cost features");
		props2Help.put(UNITFILE, "file containing all phone units");
		props2Help.put(WAVETIMELINE, "file containing all waveforms or models that can genarate them");
		props2Help.put(ISHNMTIMELINE, "file containing all wave files");
		props2Help.put(F0LEFTTREEFILE, "file containing the left f0 CART. Will be created by this module");
		props2Help.put(F0RIGHTTREEFILE, "file containing the right f0 CART. Will be created by this module");
		props2Help.put(F0MIDTREEFILE, "file containing the middle f0 CART. Will be created by this module");
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

		PrintWriter toLeftFeaturesFile = new PrintWriter(new FileOutputStream(leftF0FeaturesFile));
		PrintWriter toMidFeaturesFile = new PrintWriter(new FileOutputStream(midF0FeaturesFile));
		PrintWriter toRightFeaturesFile = new PrintWriter(new FileOutputStream(rightF0FeaturesFile));

		System.out.println("F0 CART trainer: exporting f0 features");

		FeatureDefinition featureDefinition = featureFile.getFeatureDefinition();
		byte isVowel = featureDefinition.getFeatureValueAsByte("ph_vc", "+");
		int iVC = featureDefinition.getFeatureIndex("ph_vc");
		int iSegsFromSylStart = featureDefinition.getFeatureIndex("segs_from_syl_start");
		int iSegsFromSylEnd = featureDefinition.getFeatureIndex("segs_from_syl_end");
		int iCVoiced = featureDefinition.getFeatureIndex("ph_cvox");
		byte isCVoiced = featureDefinition.getFeatureValueAsByte("ph_cvox", "+");

		int nSyllables = 0;
		for (int i = 0, len = unitFile.getNumberOfUnits(); i < len; i++) {
			// We estimate that feature extraction takes 1/10 of the total time
			// (that's probably wrong, but never mind)
			percent = 10 * i / len;
			FeatureVector fv = featureFile.getFeatureVector(i);
			if (fv.getByteFeature(iVC) == isVowel) {
				// Found a vowel, i.e. found a syllable.
				int mid = i;
				FeatureVector fvMid = fv;
				// Now find first/last voiced unit in the syllable:
				int first = i;
				for (int j = 1, lookLeft = (int) fvMid.getByteFeature(iSegsFromSylStart); j < lookLeft; j++) {
					fv = featureFile.getFeatureVector(mid - j); // units are in sequential order
					// No need to check if there are any vowels to the left of this one,
					// because of the way we do the search in the top-level loop.
					if (fv.getByteFeature(iCVoiced) != isCVoiced) {
						break; // mid-j is not voiced
					}
					first = mid - j; // OK, the unit we are currently looking at is part of the voiced syllable section
				}
				int last = i;
				for (int j = 1, lookRight = (int) fvMid.getByteFeature(iSegsFromSylEnd); j < lookRight; j++) {
					fv = featureFile.getFeatureVector(mid + j); // units are in sequential order

					if (fv.getByteFeature(iVC) != isVowel && fv.getByteFeature(iCVoiced) != isCVoiced) {
						break; // mid+j is not voiced
					}
					last = mid + j; // OK, the unit we are currently looking at is part of the voiced syllable section
				}
				// TODO: make this more robust, e.g. by fitting two straight lines to the data:
				Datagram[] midDatagrams = waveTimeline.getDatagrams(unitFile.getUnit(mid), unitFile.getSampleRate());
				Datagram[] leftDatagrams = waveTimeline.getDatagrams(unitFile.getUnit(first), unitFile.getSampleRate());
				Datagram[] rightDatagrams = waveTimeline.getDatagrams(unitFile.getUnit(last), unitFile.getSampleRate());
				if (midDatagrams != null && midDatagrams.length > 0 && leftDatagrams != null && leftDatagrams.length > 0
						&& rightDatagrams != null && rightDatagrams.length > 0) {
					float midF0 = waveTimeline.getSampleRate() / (float) midDatagrams[midDatagrams.length / 2].getDuration();
					float leftF0 = waveTimeline.getSampleRate() / (float) leftDatagrams[0].getDuration();
					float rightF0 = waveTimeline.getSampleRate()
							/ (float) rightDatagrams[rightDatagrams.length - 1].getDuration();
					System.out.println("Syllable at " + mid + " (length " + (last - first + 1) + "): left = " + ((int) leftF0)
							+ ", mid = " + ((int) midF0) + ", right = " + rightF0);
					toLeftFeaturesFile.println(leftF0 + " " + featureDefinition.toFeatureString(fvMid));
					toMidFeaturesFile.println(midF0 + " " + featureDefinition.toFeatureString(fvMid));
					toRightFeaturesFile.println(rightF0 + " " + featureDefinition.toFeatureString(fvMid));
					nSyllables++;

				}
				// Skip the part we just covered:
				i = last;
			}
		}
		toLeftFeaturesFile.close();
		toMidFeaturesFile.close();
		toRightFeaturesFile.close();
		System.out.println("F0 features extracted for " + nSyllables + " syllables");

		if (useStepwiseTraining)
			percent = 1; // estimated
		else
			percent = 10; // estimated
		PrintWriter toDesc = new PrintWriter(new FileOutputStream(f0DescFile));
		generateFeatureDescriptionForWagon(featureDefinition, toDesc);
		toDesc.close();

		// Now, call wagon
		WagonCaller wagonCaller = new WagonCaller(db.getProp(DatabaseLayout.ESTDIR), null);
		boolean ok;
		if (useStepwiseTraining) {
			// Split the data set in training and test part:
			// TODO: hardcoded path = EVIL
			Process traintest = Runtime.getRuntime().exec(
					"/project/mary/Festival/festvox/src/general/traintest " + leftF0FeaturesFile.getAbsolutePath());
			try {
				traintest.waitFor();
			} catch (InterruptedException ie) {
			}
			ok = wagonCaller.callWagon("-data " + leftF0FeaturesFile.getAbsolutePath() + ".train" + " -test "
					+ leftF0FeaturesFile.getAbsolutePath() + ".test -stepwise" + " -desc " + f0DescFile.getAbsolutePath()
					+ " -stop 10 " + " -output " + wagonLeftTreeFile.getAbsolutePath());
		} else {
			ok = wagonCaller.callWagon("-data " + leftF0FeaturesFile.getAbsolutePath() + " -desc " + f0DescFile.getAbsolutePath()
					+ " -stop 10 " + " -output " + wagonLeftTreeFile.getAbsolutePath());
		}
		if (!ok)
			return false;
		percent = 40;
		if (useStepwiseTraining) {
			// Split the data set in training and test part:
			// hardcoded path = EVIL
			Process traintest = Runtime.getRuntime().exec(
					"/project/mary/Festival/festvox/src/general/traintest " + midF0FeaturesFile.getAbsolutePath());
			try {
				traintest.waitFor();
			} catch (InterruptedException ie) {
			}
			ok = wagonCaller.callWagon("-data " + midF0FeaturesFile.getAbsolutePath() + ".train" + " -test "
					+ midF0FeaturesFile.getAbsolutePath() + ".test -stepwise" + " -desc " + f0DescFile.getAbsolutePath()
					+ " -stop 10 " + " -output " + wagonMidTreeFile.getAbsolutePath());
		} else {
			ok = wagonCaller.callWagon("-data " + midF0FeaturesFile.getAbsolutePath() + " -desc " + f0DescFile.getAbsolutePath()
					+ " -stop 10 " + " -output " + wagonMidTreeFile.getAbsolutePath());
		}
		if (!ok)
			return false;
		percent = 70;
		if (useStepwiseTraining) {
			// Split the data set in training and test part:
			// hardcoded path = EVIL
			Process traintest = Runtime.getRuntime().exec(
					"/project/mary/Festival/festvox/src/general/traintest " + rightF0FeaturesFile.getAbsolutePath());
			try {
				traintest.waitFor();
			} catch (InterruptedException ie) {
			}
			ok = wagonCaller.callWagon("-data " + rightF0FeaturesFile.getAbsolutePath() + ".train" + " -test "
					+ rightF0FeaturesFile.getAbsolutePath() + ".test -stepwise" + " -desc " + f0DescFile.getAbsolutePath()
					+ " -stop 10 " + " -output " + wagonRightTreeFile.getAbsolutePath());
		} else {
			ok = wagonCaller.callWagon("-data " + rightF0FeaturesFile.getAbsolutePath() + " -desc "
					+ f0DescFile.getAbsolutePath() + " -stop 10 " + " -output " + wagonRightTreeFile.getAbsolutePath());
		}

		if (ok) {

			// F0 Left file
			String destinationFile = getProp(F0LEFTTREEFILE);
			WagonCARTReader wagonLReader = new WagonCARTReader(LeafType.FloatLeafNode);
			marytts.cart.Node rootLNode = wagonLReader.load(new BufferedReader(new FileReader(wagonLeftTreeFile)),
					featureDefinition);
			CART leftF0Cart = new CART(rootLNode, featureDefinition);
			MaryCARTWriter wwl = new MaryCARTWriter();
			wwl.dumpMaryCART(leftF0Cart, destinationFile);

			// F0 Mid tree
			destinationFile = getProp(F0MIDTREEFILE);
			WagonCARTReader wagonMReader = new WagonCARTReader(LeafType.FloatLeafNode);
			marytts.cart.Node rootMNode = wagonMReader.load(new BufferedReader(new FileReader(wagonMidTreeFile)),
					featureDefinition);
			CART midF0Cart = new CART(rootMNode, featureDefinition);
			MaryCARTWriter wwm = new MaryCARTWriter();
			wwm.dumpMaryCART(midF0Cart, destinationFile);

			// F0 Right tree
			destinationFile = getProp(F0RIGHTTREEFILE);
			WagonCARTReader wagonRReader = new WagonCARTReader(LeafType.FloatLeafNode);
			marytts.cart.Node rootRNode = wagonRReader.load(new BufferedReader(new FileReader(wagonRightTreeFile)),
					featureDefinition);
			CART rightF0Cart = new CART(rootRNode, featureDefinition);
			MaryCARTWriter wwr = new MaryCARTWriter();
			wwr.dumpMaryCART(rightF0Cart, destinationFile);

		}

		percent = 100;

		return ok;
	}

	private String[] align(String basename) throws IOException {
		BufferedReader labels = new BufferedReader(new InputStreamReader(new FileInputStream(new File(
				db.getProp(DatabaseLayout.PHONELABDIR), basename + DatabaseLayout.LABEXT)), "UTF-8"));
		BufferedReader features = new BufferedReader(new InputStreamReader(new FileInputStream(new File(
				db.getProp(DatabaseLayout.PHONEFEATUREDIR), basename + featureExt)), "UTF-8"));
		String line;
		// Skip label file header:
		while ((line = labels.readLine()) != null) {
			if (line.startsWith("#"))
				break; // line starting with "#" marks end of header
		}
		// Skip features file header:
		while ((line = features.readLine()) != null) {
			if (line.trim().equals(""))
				break; // empty line marks end of header
		}

		// Now go through all feature file units
		boolean correct = true;
		int unitIndex = -1;
		float prevEnd = 0;
		List<String> aligned = new ArrayList<String>();
		while (correct) {
			unitIndex++;
			String labelLine = labels.readLine();
			String featureLine = features.readLine();
			if (featureLine == null) { // incomplete feature file
				return null;
			} else if (featureLine.trim().equals("")) { // end of features
				if (labelLine == null)
					break; // normal end found
				else
					// label file is longer than feature file
					return null;
			}
			// Verify that the two labels are the same:
			StringTokenizer st = new StringTokenizer(labelLine.trim());
			// The third token in each line is the label
			float end = Float.parseFloat(st.nextToken());
			st.nextToken(); // skip
			String labelUnit = st.nextToken();

			st = new StringTokenizer(featureLine.trim());
			// The expect that the first token in each line is the label
			String featureUnit = st.nextToken();
			if (!featureUnit.equals(labelUnit)) {
				// Non-matching units found
				return null;
			}
			// OK, now we assume we have two matching lines.
			if (!featureUnit.startsWith("_")) { // discard all silences
				// Output format: unit duration, followed by the feature line
				aligned.add(new PrintfFormat(Locale.ENGLISH, "%.3f").sprintf(end - prevEnd) + " " + featureLine.trim());
			}
			prevEnd = end;
		}
		return (String[]) aligned.toArray(new String[0]);
	}

	private void generateFeatureDescriptionForWagon(FeatureDefinition fd, PrintWriter out) {
		out.println("(");
		out.println("(f0 float)");
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
		F0CARTTrainer f0ct = new F0CARTTrainer();
		DatabaseLayout db = new DatabaseLayout(f0ct);
		f0ct.compute();
	}

}
