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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.SortedMap;
import java.util.TreeMap;

import marytts.exceptions.MaryConfigurationException;
import marytts.features.FeatureDefinition;
import marytts.features.FeatureVector;
import marytts.unitselection.data.FeatureFileReader;
import marytts.unitselection.data.Unit;
import marytts.unitselection.data.UnitFileReader;
import marytts.util.data.MaryHeader;
import marytts.util.math.ArrayUtils;
import marytts.util.math.Polynomial;

public class AcousticFeatureFileWriter extends VoiceImportComponent {
	protected File maryDir;
	protected FeatureFileReader feats;
	protected FeatureDefinition inFeatureDefinition;
	protected File outFeatureFile;
	protected FeatureDefinition outFeatureDefinition;
	protected UnitFileReader unitFileReader;
	protected FeatureFileReader contours;
	protected DatabaseLayout db = null;
	protected int percent = 0;

	public final String UNITFILE = "AcousticFeatureFileWriter.unitFile";
	public final String CONTOURFILE = "AcousticFeatureFileWriter.contourFile";
	public final String FEATUREFILE = "AcousticFeatureFileWriter.featureFile";
	public final String ACFEATUREFILE = "AcousticFeatureFileWriter.acFeatureFile";
	public final String ACFEATDEF = "AcousticFeatureFileWriter.acFeatDef";

	public String getName() {
		return "AcousticFeatureFileWriter";
	}

	public SortedMap<String, String> getDefaultProps(DatabaseLayout theDb) {
		this.db = theDb;
		if (props == null) {
			props = new TreeMap<String, String>();
			String fileDir = theDb.getProp(theDb.FILEDIR);
			String maryExt = theDb.getProp(theDb.MARYEXT);
			props.put(UNITFILE, fileDir + "halfphoneUnits" + maryExt);
			props.put(CONTOURFILE, fileDir + "syllableF0Polynomials" + maryExt);
			props.put(FEATUREFILE, fileDir + "halfphoneFeatures" + maryExt);
			props.put(ACFEATUREFILE, fileDir + "halfphoneFeatures_ac" + maryExt);
			props.put(ACFEATDEF, theDb.getProp(theDb.CONFIGDIR) + "halfphoneUnitFeatureDefinition_ac.txt");
		}
		return props;
	}

	protected void setupHelp() {
		if (props2Help == null) {
			props2Help = new TreeMap<String, String>();
			props2Help.put(UNITFILE, "file containing all halfphone units");
			props2Help.put(CONTOURFILE, "file containing the polynomial contours for all syllables, indexed by phone features");
			props2Help.put(FEATUREFILE, "file containing all halfphone units and their target cost features");
			props2Help.put(ACFEATUREFILE, "file containing all halfphone units and their target cost features"
					+ " plus the acoustic target cost features. Will be created by this module.");
			props2Help.put(ACFEATDEF, "file containing the list of phone target cost features, their values and weights");
		}
	}

	@Override
	public boolean compute() throws IOException, MaryConfigurationException {
		System.out.println("Acoustic feature file writer started.");

		maryDir = new File(db.getProp(db.FILEDIR));
		if (!maryDir.exists()) {
			maryDir.mkdir();
			System.out.println("Created the output directory [" + (db.getProp(db.FILEDIR)) + "] to store the feature file.");
		}
		// System.out.println("A");
		unitFileReader = new UnitFileReader(getProp(UNITFILE));
		// System.out.println("B");
		contours = new FeatureFileReader(getProp(CONTOURFILE));
		// System.out.println("C");

		feats = new FeatureFileReader(getProp(FEATUREFILE));
		inFeatureDefinition = feats.getFeatureDefinition();
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		inFeatureDefinition.writeTo(pw, true);
		// And now, append the two float features for duration and f0:
		pw.println("100 linear | unit_duration");
		pw.println("100 linear | unit_logf0");
		pw.println("0 linear | unit_logf0delta");
		pw.close();
		String fd = sw.toString();
		System.out.println("Generated the following feature definition:");
		System.out.println(fd);
		StringReader sr = new StringReader(fd);
		BufferedReader br = new BufferedReader(sr);
		outFeatureDefinition = new FeatureDefinition(br, true);

		outFeatureFile = new File(getProp(ACFEATUREFILE));
		DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(outFeatureFile)));
		writeHeaderTo(out);
		writeUnitFeaturesTo(out);
		out.close();
		System.out.println("Number of processed units: " + unitFileReader.getNumberOfUnits());

		// make sure we have a feature definition with acoustic features
		File featWeights = new File(getProp(ACFEATDEF));
		if (!featWeights.exists()) {
			try {
				PrintWriter featWeightsOut = new PrintWriter(new OutputStreamWriter(new FileOutputStream(featWeights), "UTF-8"),
						true);

				outFeatureDefinition.generateFeatureWeightsFile(featWeightsOut);
			} catch (Exception e) {
				System.out.println("No halfphone feature weights ac file " + getProp(ACFEATDEF));
				return false;
			}
		}
		FeatureFileReader tester = FeatureFileReader.getFeatureFileReader(getProp(ACFEATUREFILE));
		int unitsOnDisk = tester.getNumberOfUnits();
		if (unitsOnDisk == unitFileReader.getNumberOfUnits()) {
			System.out.println("Can read right number of units");
			int r = new Random().nextInt(unitsOnDisk);
			System.out.println("feature vector " + r + ":");
			System.out.println("Orig: " + feats.getFeatureVector(r).toString());
			System.out.println("AC  : " + tester.getFeatureVector(r).toString());
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
		int unitSampleRate = unitFileReader.getSampleRate();
		FeatureDefinition featureDefinition = feats.getFeatureDefinition();
		int fiPhoneme = featureDefinition.getFeatureIndex("phone");
		byte fvPhoneme_0 = featureDefinition.getFeatureValueAsByte(fiPhoneme, "0");
		byte fvPhoneme_Silence = featureDefinition.getFeatureValueAsByte(fiPhoneme, "_");
		int fiVowel = featureDefinition.getFeatureIndex("ph_vc");
		byte fvVowel = featureDefinition.getFeatureValueAsByte(fiVowel, "+");
		int fiLR = featureDefinition.getFeatureIndex("halfphone_lr");
		byte fvLR_L = featureDefinition.getFeatureValueAsByte(fiLR, "L");
		byte fvLR_R = featureDefinition.getFeatureValueAsByte(fiLR, "R");
		int fiSylStart = featureDefinition.getFeatureIndex("segs_from_syl_start");
		int fiSylEnd = featureDefinition.getFeatureIndex("segs_from_syl_end");
		int iSylVowel = -1;
		List<Float> unitDurs = new ArrayList<Float>();

		out.writeInt(numUnits);
		System.out.println("Number of units : " + numUnits);
		int iCurrent = 0;
		for (int i = 0; i < numUnits; i++) {
			percent = 100 * i / numUnits;
			FeatureVector inFV = feats.getFeatureVector(i);
			Unit u = unitFileReader.getUnit(i);
			float dur = u.duration / (float) unitSampleRate;

			// No syllable structure for edge and silence phone entries:
			if (inFV.getByteFeature(fiPhoneme) == fvPhoneme_0 || inFV.getByteFeature(fiPhoneme) == fvPhoneme_Silence) {
				unitDurs.add(dur);
				continue;
			}
			// Else, unit belongs to a syllable
			if (inFV.getByteFeature(fiSylStart) == 0 && inFV.getByteFeature(fiLR) == fvLR_L) { // first segment in syllable
				if (iCurrent < i) { // Something to output before this syllable
					assert i - iCurrent == unitDurs.size();
					writeFeatureVectors(out, iCurrent, iSylVowel, i - 1, unitDurs);
				}
				unitDurs.clear();
				iSylVowel = -1;
				iCurrent = i;
			}

			unitDurs.add(dur);

			if (inFV.getByteFeature(fiVowel) == fvVowel && iSylVowel == -1) { // the first vowel in the syllable
				iSylVowel = i;
			}

			if (inFV.getByteFeature(fiSylEnd) == 0 && inFV.getByteFeature(fiLR) == fvLR_R) { // last segment in syllable
				writeFeatureVectors(out, iCurrent, iSylVowel, i, unitDurs);
				iSylVowel = -1;
				unitDurs.clear();
				iCurrent = i + 1;
			}

		}

		assert numUnits - iCurrent == unitDurs.size();
		writeFeatureVectors(out, iCurrent, iSylVowel, numUnits - 1, unitDurs);
	}

	private void writeFeatureVectors(DataOutput out, int iFirst, int iVowel, int iLast, List<Float> unitDurs) throws IOException {
		float[] coeffs = null;
		if (iVowel != -1) { // Syllable contains a vowel
			coeffs = contours.getFeatureVector(iVowel).getContinuousFeatures();
			boolean isZero = true;
			for (int c = 0; c < coeffs.length; c++) {
				if (coeffs[c] != 0) {
					isZero = false;
					break;
				}
			}
			if (isZero) {
				coeffs = null;
			}
		}
		assert unitDurs.size() == iLast - iFirst + 1;

		float sylDur = 0;
		for (int i = 0, max = unitDurs.size(); i < max; i++) {
			sylDur += unitDurs.get(i);
		}
		// System.out.println("Syl dur: "+sylDur+", "+unitDurs.size()+" units");

		float uStart = 0, uEnd = 0;
		for (int i = 0; iFirst + i <= iLast; i++) {
			float logF0 = Float.NaN;
			float logF0delta = Float.NaN;
			if (coeffs != null && unitDurs.get(i) > 0) {
				float relUStart = uStart / sylDur; // in [0, 1[
				float relUEnd = (uStart + unitDurs.get(i)) / sylDur; // in [0, 1[
				double[] predUnitContour = Polynomial.generatePolynomialValues(ArrayUtils.copyFloat2Double(coeffs), 10,
						relUStart, relUEnd);
				// System.out.printf("From %.2f to %.2f:", relUStart, relUEnd);
				// for (int k=0; k<predUnitContour.length; k++) System.out.printf(" %.2f", predUnitContour[k]);
				// And fit a linear curve to this:
				double[] unitCoeffs = Polynomial.fitPolynomial(predUnitContour, 1);
				assert unitCoeffs.length == 2; // unitCoeffs[0] is the slope, unitCoeffs[1] the value at left end of interval.
				// We need the f0 value in the middle of the unit:
				logF0 = (float) (unitCoeffs[1] + 0.5 * unitCoeffs[0]);
				logF0delta = (float) unitCoeffs[0];
				// System.out.printf(" -- midF0=%.2f, slope=%.2f\n", logF0, logF0delta);
			}
			FeatureVector fv = feats.getFeatureVector(iFirst + i);
			String line = fv.toString() + " " + unitDurs.get(i) + " " + logF0 + " " + logF0delta;
			// System.out.println("fv: "+line);
			FeatureVector outFV = outFeatureDefinition.toFeatureVector(0, line);
			outFV.writeTo(out);
			uStart += unitDurs.get(i);
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
		outFeatureDefinition.writeBinaryTo(out);
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
		AcousticFeatureFileWriter acfeatsWriter = new AcousticFeatureFileWriter();
		DatabaseLayout db = new DatabaseLayout(acfeatsWriter);
		acfeatsWriter.compute();
	}

}
