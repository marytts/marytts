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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import marytts.cart.CART;
import marytts.cart.DirectedGraph;
import marytts.cart.io.DirectedGraphWriter;
import marytts.exceptions.MaryConfigurationException;
import marytts.features.FeatureDefinition;
import marytts.features.FeatureVector;
import marytts.tools.voiceimport.traintrees.AgglomerativeClusterer;
import marytts.tools.voiceimport.traintrees.F0ContourPolynomialDistanceMeasure;
import marytts.tools.voiceimport.traintrees.Wagon;
import marytts.unitselection.data.FeatureFileReader;
import marytts.util.math.ArrayUtils;

public class F0PolynomialTreeTrainer extends VoiceImportComponent {
	protected File maryDir;
	protected FeatureFileReader features;
	protected FeatureDefinition featureDefinition;
	protected FeatureFileReader contours;
	protected DatabaseLayout db = null;

	public final String FEATUREFILE = "F0PolynomialTreeTrainer.featureFile";
	public final String F0FEATUREFILE = "F0PolynomialTreeTrainer.f0FeatureFile";
	public final String F0TREE = "F0PolynomialTreeTrainer.treeFile";
	public final String MAXDATA = "F0PolynomialTreeTrainer.maxData";
	public final String PROPORTIONTESTDATA = "F0PolynomialTreeTrainer.propTestData";

	public final String WAGONDIR = "F0PolynomialTreeTrainer.wagonDir";
	public final String WAGONEXECUTABLE = "F0PolynomialTreeTrainer.wagonExecutable";
	public final String BALANCE = "F0PolynomialTreeTrainer.wagonBalance";
	public final String STOP = "F0PolynomialTreeTrainer.wagonStop";

	public String getName() {
		return "F0PolynomialTreeTrainer";
	}

	public SortedMap<String, String> getDefaultProps(DatabaseLayout theDb) {
		this.db = theDb;
		if (props == null) {
			props = new TreeMap<String, String>();
			String fileDir = db.getProp(db.FILEDIR);
			String maryExt = db.getProp(db.MARYEXT);
			props.put(FEATUREFILE, fileDir + "halfphoneFeatures" + maryExt);
			props.put(F0FEATUREFILE, fileDir + "syllableF0Polynomials" + maryExt);
			props.put(F0TREE, fileDir + "f0contourtree.mry");
			props.put(MAXDATA, "0");
			props.put(PROPORTIONTESTDATA, "0.1");

			props.put(WAGONDIR, "f0contours");
			props.put(WAGONEXECUTABLE, System.getenv("ESTDIR") + "/bin/wagon");
			props.put(BALANCE, "0");
			props.put(STOP, "50");
		}
		return props;
	}

	protected void setupHelp() {
		if (props2Help == null) {
			props2Help = new TreeMap<String, String>();
			props2Help.put(FEATUREFILE, "file containing all halfphone units and their target cost features");
			props2Help.put(F0FEATUREFILE, "file containing syllable-based polynom coefficients on vowels");
			props2Help.put(F0TREE, "the path for saving the resulting f0 contour tree");
			props2Help.put(MAXDATA, "if >0, gives the maximum number of syllables to use for training the tree");
			props2Help.put(PROPORTIONTESTDATA,
					"the proportion of the data to use as test data (choose so that 1/value is an integer)");

			props2Help.put(WAGONDIR, "directory in which to store the wagon files");
			props2Help.put(WAGONEXECUTABLE, "full path of the wagon executable from the Edinburgh speech tools");
			props2Help.put(BALANCE, "the wagon balance value");
			props2Help.put(STOP, "the wagon stop criterion (min number of items in leaf)");
		}
	}

	@Override
	public boolean compute() throws IOException, MaryConfigurationException {
		logger.info("F0 polynomial tree trainer started.");

		features = new FeatureFileReader(getProp(FEATUREFILE));
		featureDefinition = features.getFeatureDefinition();
		contours = new FeatureFileReader(getProp(F0FEATUREFILE));
		if (features.getNumberOfUnits() != contours.getNumberOfUnits()) {
			throw new IllegalArgumentException(
					"Number of units differs between feature file and contour file -- they are out of sync");
		}

		int maxData = Integer.parseInt(getProp(MAXDATA));

		// 1. Extract the feature vectors for which there are contours
		List<FeatureVector> relevantFVList = new ArrayList<FeatureVector>();
		for (int i = 0, numUnits = contours.getNumberOfUnits(); i < numUnits; i++) {
			FeatureVector fv = contours.getFeatureVector(i);
			float[] floats = fv.getContinuousFeatures();
			boolean isZero = ArrayUtils.isZero(floats);
			if (!isZero) {
				relevantFVList.add(features.getFeatureVector(i));
				if (maxData > 0 && relevantFVList.size() >= maxData)
					break;
			}
		}
		FeatureVector[] relevantFV = relevantFVList.toArray(new FeatureVector[0]);
		logger.debug("Read " + relevantFV.length + " f0 contours and corresponding feature vectors");

		// 2. Grow a tree
		// CART cart = trainWagon(relevantFV);
		DirectedGraph graph = trainAgglomerativeCluster(relevantFV);

		// 3. Save resulting tree in MARY format.
		if (graph != null) {
			DirectedGraphWriter writer = new DirectedGraphWriter();
			writer.saveGraph(graph, getProp(F0TREE));
			return true;
		}
		return false;
	}

	private CART trainWagon(FeatureVector[] relevantFV) throws IOException {
		// 2. Call wagon with these feature vectors
		// and a distance measure based on contour distances
		Wagon.setWagonExecutable(new File(getProp(WAGONEXECUTABLE)));
		File wagonDir = new File(getProp(WAGONDIR));
		if (!wagonDir.exists()) {
			wagonDir.mkdirs();
		}
		Wagon w = new Wagon("f0contours", featureDefinition, relevantFV, new F0ContourPolynomialDistanceMeasure(contours),
				wagonDir, Integer.parseInt(getProp(BALANCE)), Integer.parseInt(getProp(STOP)));
		w.run();
		if (w.success())
			return w.getCART();
		return null;
	}

	private DirectedGraph trainAgglomerativeCluster(FeatureVector[] relevantFV) throws IOException {
		List<String> featuresToUse = new ArrayList<String>();
		for (int i = 0, numByteFeatures = featureDefinition.getNumberOfByteFeatures(); i < numByteFeatures; i++) {
			String f = featureDefinition.getFeatureName(i);
			if (!f.contains("phone") && !f.contains("halfphone") && !f.contains("vc") && !f.contains("ctype")
					&& !f.contains("cvox") && !f.contains("edge") && !f.contains("vfront") && !f.contains("vlng")
					&& !f.contains("vheight") && !f.contains("cplace") && !f.contains("vrnd")
					&& !f.contains("selection_next_phone_class")) {
				featuresToUse.add(f);
				// System.out.println("adding feature "+f);
			} else {
				// System.err.println("ignoring feature "+f);
			}
		}

		AgglomerativeClusterer clusterer = new AgglomerativeClusterer(relevantFV, featureDefinition, featuresToUse,
				new F0ContourPolynomialDistanceMeasure(contours));
		DirectedGraphWriter writer = new DirectedGraphWriter();
		DirectedGraph graph;
		int iteration = 0;
		do {
			graph = clusterer.cluster();
			iteration++;
			if (graph != null) {
				writer.saveGraph(graph, getProp(F0TREE) + ".level" + iteration);
			}
		} while (clusterer.canClusterMore());
		return graph;
	}

	/**
	 * Provide the progress of computation, in percent, or -1 if that feature is not implemented.
	 * 
	 * @return -1 if not implemented, or an integer between 0 and 100.
	 */
	public int getProgress() {
		return -1;
	}

	/**
	 * @param args
	 *            args
	 * @throws Exception
	 *             Exception
	 */
	public static void main(String[] args) throws Exception {
		F0PolynomialTreeTrainer acfeatsWriter = new F0PolynomialTreeTrainer();
		DatabaseLayout db = new DatabaseLayout(acfeatsWriter);
		acfeatsWriter.compute();
	}

}
