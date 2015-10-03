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
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import marytts.exceptions.MaryConfigurationException;
import marytts.features.FeatureDefinition;
import marytts.features.FeatureVector;
import marytts.tools.voiceimport.DatabaseLayout;
import marytts.tools.voiceimport.VoiceImportComponent;
import marytts.util.data.MaryHeader;
import marytts.util.io.BasenameList;
import marytts.vocalizations.VocalizationAnnotationReader;
import marytts.vocalizations.VocalizationFeatureFileReader;
import marytts.vocalizations.VocalizationUnitFileReader;

/**
 * vocalization feature file writer
 * 
 * @author sathish
 *
 */
public class VocalizationFeatureFileWriter extends VoiceImportComponent {

	protected File outFeatureFile;
	protected FeatureDefinition featureDefinition;
	protected VocalizationUnitFileReader listenerUnits;

	protected DatabaseLayout db = null;
	protected int percent = 0;
	private ArrayList<String> featureCategories; // feature categories
	private Map<String, Map<String, String>> annotationData; // basename --> (feature category, feature value)

	protected String vocalizationsDir;
	protected BasenameList bnlVocalizations;

	private final String name = "VocalizationFeatureFileWriter";
	public final String UNITFILE = name + ".unitFile";
	public final String FEATUREFILE = name + ".featureFile";
	public final String MANUALFEATURES = name + ".annotationFeatureFile";
	public final String FEATDEF = name + ".featureDefinition";

	public final String POLYNOMORDER = name + ".polynomOrder";
	public final String SHOWGRAPH = name + ".showGraph";
	public final String INTERPOLATE = name + ".interpolate";
	public final String MINPITCH = name + ".minPitch";
	public final String MAXPITCH = name + ".maxPitch";

	// public String BASELIST = name + ".backchannelBNL";

	public String getName() {
		return name;
	}

	public SortedMap<String, String> getDefaultProps(DatabaseLayout db) {
		this.db = db;
		if (props == null) {
			props = new TreeMap<String, String>();
			String fileDir = db.getProp(db.VOCALIZATIONSDIR) + File.separator + "files" + File.separator;
			String maryExt = db.getProp(db.MARYEXT);
			props.put(UNITFILE, fileDir + "vocalization_units" + maryExt);
			props.put(FEATUREFILE, fileDir + "vocalization_features" + maryExt);
			props.put(MANUALFEATURES, db.getProp(db.VOCALIZATIONSDIR) + File.separator + "features" + File.separator
					+ "annotation_vocalizations_features.txt");
			props.put(FEATDEF, db.getProp(db.VOCALIZATIONSDIR) + File.separator + "features" + File.separator
					+ "vocalization_feature_definition.txt");
		}
		return props;
	}

	protected void setupHelp() {
		if (props2Help == null) {
			props2Help = new TreeMap<String, String>();
			props2Help.put(UNITFILE, "file containing all halfphone units");
			props2Help.put(FEATUREFILE, "file containing all halfphone units and their target cost features");
		}
	}

	@Override
	protected void initialiseComp() {

		featureCategories = new ArrayList<String>(); // feature categories
		annotationData = new HashMap<String, Map<String, String>>(); // basename --> (feature category, feature value)

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

	@Override
	public boolean compute() throws IOException, MaryConfigurationException {
		// read feature definition
		BufferedReader fDBfr = new BufferedReader(new FileReader(new File(getProp(FEATDEF))));
		// featureDefinition = new FeatureDefinition(fDBfr, true);
		featureDefinition = new FeatureDefinition(fDBfr, true);
		listenerUnits = new VocalizationUnitFileReader(getProp(UNITFILE));

		// load annotation
		VocalizationAnnotationReader annotationReader = new VocalizationAnnotationReader(getProp(MANUALFEATURES),
				bnlVocalizations);
		annotationData = annotationReader.getVocalizationsAnnotation();
		featureCategories = annotationReader.getFeatureList();

		// write features into timeline file
		DataOutputStream out = new DataOutputStream(
				new BufferedOutputStream(new FileOutputStream(new File(getProp(FEATUREFILE)))));
		writeHeaderTo(out);
		writeUnitFeaturesTo(out);
		out.close();
		logger.debug("Number of processed units: " + listenerUnits.getNumberOfUnits());

		VocalizationFeatureFileReader tester = new VocalizationFeatureFileReader(getProp(FEATUREFILE));
		int unitsOnDisk = tester.getNumberOfUnits();
		if (unitsOnDisk == listenerUnits.getNumberOfUnits()) {
			System.out.println("Can read right number of units");
			return true;
		} else {
			System.out.println("Read wrong number of units: " + unitsOnDisk);
			return false;
		}
	}

	protected void writeUnitFeaturesTo(DataOutput out) throws IOException, UnsupportedEncodingException, FileNotFoundException {

		int numUnits = listenerUnits.getNumberOfUnits();

		out.writeInt(numUnits);
		// logger.debug("Number of vocalization units : "+numUnits);
		System.out.println("Number of vocalization units : " + numUnits);
		System.out.println("Annotation number of units: " + annotationData.size());
		if (annotationData.size() != listenerUnits.getNumberOfUnits()) {
			throw new RuntimeException("Number of units in vocalizations unit-file is not equal to number of basenames. ");
		}

		if (featureCategories.size() != featureDefinition.getNumberOfFeatures()) {
			throw new RuntimeException(
					"Number of categories in feature_definition is not equal to features given in annotation file ");
		}

		/**
		 * TODO sanity check for each basename
		 */

		int noOfFeatures = featureDefinition.getNumberOfFeatures();
		String[] featureNames = new String[noOfFeatures];
		for (int i = 0; i < featureNames.length; i++) {
			featureNames[i] = featureDefinition.getFeatureName(i);
		}

		for (int i = 0; i < bnlVocalizations.getLength(); i++) {

			byte[] byteFeatures;
			short[] shortFeatures;
			float[] continiousFeatures;

			String baseName = bnlVocalizations.getName(i);
			Map<String, String> singleAnnotation = annotationData.get(baseName);

			int noByteFeatures = featureDefinition.getNumberOfByteFeatures();
			int noShortFeatures = featureDefinition.getNumberOfShortFeatures();
			int noContiniousFeatures = featureDefinition.getNumberOfContinuousFeatures();

			// create features array
			byteFeatures = new byte[noByteFeatures];
			shortFeatures = new short[noShortFeatures];
			continiousFeatures = new float[noContiniousFeatures];

			int countByteFeatures = 0;
			int countShortFeatures = 0;
			int countFloatFeatures = 0;

			for (int j = 0; j < featureNames.length; j++) {
				String fName = featureNames[j];
				if (featureDefinition.isByteFeature(j)) { // Byte feature
					if (singleAnnotation.containsKey(fName)) {
						byteFeatures[countByteFeatures++] = featureDefinition.getFeatureValueAsByte(j,
								singleAnnotation.get(fName));
					} else {
						byteFeatures[countByteFeatures++] = (byte) 0;
					}
				} else if (featureDefinition.isShortFeature(j)) { // Short feature
					if (singleAnnotation.containsKey(fName)) {
						shortFeatures[countShortFeatures++] = featureDefinition.getFeatureValueAsShort(j,
								singleAnnotation.get(fName));
					} else {
						shortFeatures[countShortFeatures++] = (short) 0;
					}
				} else if (featureDefinition.isContinuousFeature(j)) { // Continuous feature
					if (!singleAnnotation.containsKey(fName)) {
						continiousFeatures[countFloatFeatures++] = Float.NaN;
					} else if ("NRI".equals(singleAnnotation.get(fName))) {
						continiousFeatures[countFloatFeatures++] = Float.NaN;
					} else {
						continiousFeatures[countFloatFeatures++] = (new Float(singleAnnotation.get(fName))).floatValue();
					}
				}
			}

			FeatureVector outFV = featureDefinition.toFeatureVector(i, byteFeatures, shortFeatures, continiousFeatures);
			outFV.writeTo(out);
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
		new MaryHeader(MaryHeader.LISTENERFEATS).writeTo(out);
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
		VocalizationFeatureFileWriter acfeatsWriter = new VocalizationFeatureFileWriter();
		DatabaseLayout db = new DatabaseLayout(acfeatsWriter);
		acfeatsWriter.compute();
	}

}
