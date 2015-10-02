/**
 * Copyright 2000-2009 DFKI GmbH.
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
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import marytts.cart.StringPredictionTree;
import marytts.features.FeatureDefinition;
import marytts.features.FeatureVector;
import marytts.machinelearning.GmmDiscretizer;
import weka.classifiers.trees.j48.BinC45ModelSelection;
import weka.classifiers.trees.j48.C45PruneableClassifierTree;
import weka.classifiers.trees.j48.TreeConverter;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;

/*
 * This Class traverses intonised xml-files, and labeller files and trains a duration
 * model to predict the labeled durations from the features of the xml file
 * 
 */
public class PauseDurationTrainer extends VoiceImportComponent {

	/**
	 * Tuple holding list of feature vectors and feature definition.
	 */
	private class VectorsAndDefinition {
		public VectorsAndDefinition(List<FeatureVector> fv, FeatureDefinition fd) {
			this.fv = fv;
			this.fd = fd;
		}

		private List<FeatureVector> fv;
		private FeatureDefinition fd;

		public List<FeatureVector> getFv() {
			return fv;
		}

		public void setFv(List<FeatureVector> fv) {
			this.fv = fv;
		}

		public FeatureDefinition getFd() {
			return fd;
		}

		public void setFd(FeatureDefinition fd) {
			this.fd = fd;
		}
	}

	// maybe specify in config file?
	public final String[] featureNames = new String[] { "breakindex", "ph_cplace", "ph_ctype", "next_pos",
			"next_wordbegin_ctype", "next_wordbegin_cplace", "words_from_phrase_end", "words_from_phrase_start"/**/};

	// feature files used for training ("pause features")
	public final String FVFILES = "PauseDurationTrainer.featureDir";
	// label files providing durations
	public final String LABFILES = "PauseDurationTrainer.lab";
	// resulting trained decision tree
	public final String TRAINEDTREE = "PauseDurationTrainer.tree";

	protected DatabaseLayout db = null;

	private String fvExt = ".pfeats";
	private String labExt = ".lab";

	public SortedMap<String, String> getDefaultProps(DatabaseLayout db) {
		this.db = db;
		if (props == null) {
			props = new TreeMap<String, String>();

			// dir with pause feature files
			String pauseFv = System.getProperty(FVFILES);
			if (pauseFv == null) {
				pauseFv = db.getProp(db.ROOTDIR) + "pausefeatures" + System.getProperty("file.separator");
			}
			props.put(FVFILES, pauseFv);

			// dir with lab files containing the pause durations
			String labs = System.getProperty(LABFILES);
			if (labs == null) {
				labs = db.getProp(db.ROOTDIR) + "lab" + System.getProperty("file.separator");
			}
			props.put(LABFILES, labs);

			// resulting decision tree
			String tree = System.getProperty(TRAINEDTREE);
			if (tree == null) {
				tree = db.getProp(db.ROOTDIR) + "durations.tree";
			}
			props.put(TRAINEDTREE, tree);

		}
		return props;
	}

	public boolean compute() throws Exception {

		// object to store all instances
		Instances data = null;
		FeatureDefinition fd = null;

		// pause durations are added at the end
		// all of them are collected first
		// then discretized
		List<Integer> durs = new ArrayList<Integer>();

		for (int i = 0; i < bnl.getLength(); i++) {

			VectorsAndDefinition features = this.readFeaturesFor(bnl.getName(i));

			if (null == features)
				continue;

			List<FeatureVector> vectors = features.getFv();
			fd = features.getFd();

			if (data == null)
				data = initData(fd);

			// reader for label file.
			BufferedReader lab = new BufferedReader(new FileReader(getProp(LABFILES) + bnl.getName(i) + labExt));

			List<String> labSyms = new ArrayList<String>();
			List<Integer> labDurs = new ArrayList<Integer>();
			int prevTime = 0;
			int currTime = 0;

			String line;
			while ((line = lab.readLine()) != null) {
				if (line.startsWith("#"))
					continue;

				String[] lineLmnts = line.split("\\s+");

				if (lineLmnts.length != 3)
					throw new IllegalArgumentException("Expected three columns in label file, got " + lineLmnts.length);

				labSyms.add(lineLmnts[2]);

				// collect durations
				currTime = (int) (1000 * Float.parseFloat(lineLmnts[0]));
				int dur = currTime - prevTime;
				labDurs.add(dur);
				prevTime = currTime;

			}

			int symbolFeature = fd.getFeatureIndex("phone");
			int breakindexFeature = fd.getFeatureIndex("breakindex");

			int currLabelNr = 0;

			// treatment of first pause(s)...
			while (labSyms.get(currLabelNr).equals("_"))
				currLabelNr++;

			for (FeatureVector fv : vectors) {

				String fvSym = fv.getFeatureAsString(symbolFeature, fd);

				// all pauses on feature vector side are ignored, they are captured within boundary treatment
				if (fvSym.equals("_"))
					continue;

				if (!fvSym.equals(labSyms.get(currLabelNr)))
					throw new IllegalArgumentException("Phone symbol of label file (" + fvSym + ") and of feature vector ("
							+ labSyms.get(currLabelNr) + ") don't correspond. Run CorrectedTranscriptionAligner first.");

				int pauseDur = 0;
				// durations are taken from pauses on label side
				if ((currLabelNr + 1) < labSyms.size() && labSyms.get(currLabelNr + 1).equals("_")) {
					currLabelNr++;
					pauseDur = labDurs.get(currLabelNr);
				}

				int bi = fv.getFeatureAsInt(breakindexFeature);
				if (bi > 1) {
					// add new training point with fv
					durs.add(pauseDur);
					data.add(createInstance(data, fd, fv));

				} // for each break index > 1

				currLabelNr++;
			}// for each featurevector

		} // for each file

		// set duration target attribute
		data = enterDurations(data, durs);

		// train classifier
		StringPredictionTree wagonTree = trainTree(data, fd);

		FileWriter fw = new FileWriter(getProp(TRAINEDTREE));
		fw.write(wagonTree.toString());
		fw.close();

		return true;
	}

	private StringPredictionTree trainTree(Instances data, FeatureDefinition fd) throws Exception {

		System.out.println("training duration tree (" + data.numInstances() + " instances) ...");

		// build the tree without using the J48 wrapper class
		// standard parameters are:
		// binary split selection with minimum x instances at the leaves, tree is pruned, confidence value, subtree raising,
		// cleanup, don't collapse
		C45PruneableClassifierTree decisionTree = new C45PruneableClassifierTree(new BinC45ModelSelection(2, data, true), true,
				0.25f, true, true, false);

		decisionTree.buildClassifier(data);

		System.out.println("...done");

		return TreeConverter.c45toStringPredictionTree(decisionTree, fd, data);
	}

	private Instances enterDurations(Instances data, List<Integer> durs) {

		// System.out.println("discretizing durations...");

		// now discretize and set target attributes (= pause durations)
		// for that, first train discretizer
		GmmDiscretizer discr = GmmDiscretizer.trainDiscretizer(durs, 6, true);

		// used to store the collected values
		ArrayList<String> targetVals = new ArrayList<String>();

		for (int mappedDur : discr.getPossibleValues()) {
			targetVals.add(mappedDur + "ms");
		}

		// FastVector attributeDeclarations = data.;

		// attribute declaration finished
		data.insertAttributeAt(new Attribute("target", targetVals), data.numAttributes());

		for (int i = 0; i < durs.size(); i++) {

			Instance currInst = data.instance(i);
			int dur = durs.get(i);

			// System.out.println(" mapping " + dur + " to " + discr.discretize(dur) + " - bi:" +
			// data.instance(i).value(data.attribute("breakindex")));

			currInst.setValue(data.numAttributes() - 1, discr.discretize(dur) + "ms");

		}

		// Make the last attribute be the class
		data.setClassIndex(data.numAttributes() - 1);

		return data;
	}

	private Instance createInstance(Instances data, FeatureDefinition fd, FeatureVector fv) {
		// relevant features + one target
		Instance currInst = new DenseInstance(data.numAttributes());
		currInst.setDataset(data);

		// read only relevant features
		for (String attName : this.featureNames) {
			int featNr = fd.getFeatureIndex(attName);

			String value = fv.getFeatureAsString(featNr, fd);
			currInst.setValue(data.attribute(attName), value);
		}

		return currInst;
	}

	private Instances initData(FeatureDefinition fd) {
		// this stores the attributes together with allowed values
		ArrayList<Attribute> attributeDeclarations = new ArrayList<Attribute>();

		// first declare all the relevant attributes.
		// Assume that the feature definition and relevant features of the first
		// in the list are the same as the others.

		for (int attribute = 0; attribute < fd.getNumberOfFeatures(); attribute++) {

			String attName = fd.getFeatureName(attribute);

			// skip phone
			if (attName.equals("phone")) {
				continue;
			}

			// ...collect possible values
			ArrayList<String> attVals = new ArrayList<String>();
			for (String value : fd.getPossibleValues(attribute)) {
				attVals.add(value);
			}

			attributeDeclarations.add(new Attribute(attName, attVals));

		}

		// now, create the dataset adding the datapoints
		return new Instances("pausedurations", attributeDeclarations, 0);
	}

	/**
	 * This reads in the features for the symbols in the input (phonemic/automatic) file from a feature stream stored in textual
	 * format.
	 * 
	 * @param featureTable
	 *            a LineNumberReader from which the feature table is read.
	 * @throws IOException
	 *             if the input stream is ill-formed
	 */
	private VectorsAndDefinition readFeatureTable(LineNumberReader featureTable) throws IOException {

		List<FeatureVector> featureVectors = new ArrayList<FeatureVector>();

		// read the beginning of the file, containing the feature definition
		FeatureDefinition fd = new FeatureDefinition(featureTable, false);

		try {
			// for later checks, get index of phone identity feature
			fd.getFeatureIndex("phone");
			fd.getFeatureIndex("breakindex");
		} catch (IllegalArgumentException e) {
			throw new IOException("Unexpected FeatureDefinition: Does not contain the features 'phone' and 'breakindex'.");
		}

		// skip section with string representation
		while (!featureTable.readLine().equals("")) {
		}

		// now, read the features line by line
		String line = "";

		while ((line = featureTable.readLine()) != null) {

			FeatureVector fv;
			try {
				fv = fd.toFeatureVector(0, line);
			} catch (Exception e) {
				e.printStackTrace();
				throw new IOException("Unexpected Input in line " + String.valueOf(featureTable.getLineNumber()));
			}

			featureVectors.add(fv);

		}

		return new VectorsAndDefinition(featureVectors, fd);
	}

	/**
	 * This reads in some pause feature file and returns feature vectors
	 * 
	 * 
	 * @param basename
	 *            basename
	 * @return readFeatureTable(lnr)
	 * @throws IOException
	 *             IOException
	 */
	private VectorsAndDefinition readFeaturesFor(String basename) throws IOException {
		FileInputStream fis;

		// First, test if there is a corresponding .rawmaryxml file in textdir:
		File fvFile = new File(getProp(FVFILES) + basename + fvExt);
		if (fvFile.exists()) {
			fis = new FileInputStream(fvFile);
		} else {
			return null;
		}

		System.out.println("processing " + getProp(FVFILES) + basename + fvExt);

		// didn't work ... FeatureFileReader ffr = new FeatureFileReader();
		LineNumberReader lnr = new LineNumberReader(new InputStreamReader(fis));

		return readFeatureTable(lnr);
	}

	public String getName() {
		return "PauseDurationTrainer";
	}

	@Override
	public int getProgress() {
		return 0;
	}

	protected void setupHelp() {
		props2Help = new TreeMap<String, String>();
		props2Help.put(FVFILES, "Directory containing the pause feature files.");
		props2Help.put(LABFILES, "Directory containing label files from which pause durations are taken.");
		props2Help.put(TRAINEDTREE, "Result of training.");
	}

}
