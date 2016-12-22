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

import java.io.IOException;
import java.util.SortedMap;
import java.util.TreeMap;

import marytts.cart.DecisionNode;
import marytts.cart.DirectedGraph;
import marytts.cart.DirectedGraphNode;
import marytts.cart.LeafNode;
import marytts.cart.Node;
import marytts.cart.LeafNode.FeatureVectorLeafNode;
import marytts.cart.LeafNode.FloatLeafNode;
import marytts.cart.io.DirectedGraphWriter;
import marytts.exceptions.MaryConfigurationException;
import marytts.features.FeatureVector;
import marytts.tools.voiceimport.traintrees.AgglomerativeClusterer;
import marytts.tools.voiceimport.traintrees.DurationDistanceMeasure;
import marytts.unitselection.data.FeatureFileReader;
import marytts.unitselection.data.UnitFileReader;
import marytts.util.math.MathUtils;

/**
 * A class which converts a text file in festvox format into a one-file-per-utterance format in a given directory.
 * 
 * @author schroed
 *
 */
public class DurationTreeTrainer extends VoiceImportComponent {
	protected DatabaseLayout db = null;

	private final String name = "DurationTreeTrainer";
	public final String DURTREE = name + ".durTree";
	public final String FEATUREFILE = name + ".featureFile";
	public final String UNITFILE = name + ".unitFile";
	public final String MAXDATA = name + ".maxData";
	public final String PROPORTIONTESTDATA = name + ".propTestData";

	public String getName() {
		return name;
	}

	public SortedMap<String, String> getDefaultProps(DatabaseLayout theDb) {
		this.db = theDb;
		if (props == null) {
			props = new TreeMap<String, String>();
			String fileSeparator = System.getProperty("file.separator");
			props.put(FEATUREFILE, db.getProp(db.FILEDIR) + "phoneFeatures" + db.getProp(db.MARYEXT));
			props.put(UNITFILE, db.getProp(db.FILEDIR) + "phoneUnits" + db.getProp(db.MARYEXT));
			props.put(DURTREE, db.getProp(db.FILEDIR) + "dur.graph.mry");
			props.put(MAXDATA, "0");
			props.put(PROPORTIONTESTDATA, "0.1");
		}
		return props;
	}

	protected void setupHelp() {
		props2Help = new TreeMap<String, String>();
		props2Help.put(FEATUREFILE, "file containing all phone units and their target cost features");
		props2Help.put(UNITFILE, "file containing all phone units");
		props2Help.put(DURTREE, "file containing the duration tree. Will be created by this module");
		props2Help.put(MAXDATA, "if >0, gives the maximum number of syllables to use for training the tree");
		props2Help.put(PROPORTIONTESTDATA,
				"the proportion of the data to use as test data (choose so that 1/value is an integer)");
	}

	@Override
	public boolean compute() throws IOException, MaryConfigurationException {
		logger.info("Duration tree trainer started.");
		FeatureFileReader featureFile = FeatureFileReader.getFeatureFileReader(getProp(FEATUREFILE));
		UnitFileReader unitFile = new UnitFileReader(getProp(UNITFILE));

		FeatureVector[] allFeatureVectors = featureFile.getFeatureVectors();
		int maxData = Integer.parseInt(getProp(MAXDATA));
		if (maxData == 0)
			maxData = allFeatureVectors.length;
		FeatureVector[] featureVectors = new FeatureVector[Math.min(maxData, allFeatureVectors.length)];
		System.arraycopy(allFeatureVectors, 0, featureVectors, 0, featureVectors.length);
		logger.debug("Total of " + allFeatureVectors.length + " feature vectors -- will use " + featureVectors.length);

		AgglomerativeClusterer clusterer = new AgglomerativeClusterer(featureVectors, featureFile.getFeatureDefinition(), null,
				new DurationDistanceMeasure(unitFile), Float.parseFloat(getProp(PROPORTIONTESTDATA)));
		DirectedGraphWriter writer = new DirectedGraphWriter();
		DirectedGraph graph;
		int iteration = 0;
		do {
			graph = clusterer.cluster();
			iteration++;
			if (graph != null) {
				writer.saveGraph(graph, getProp(DURTREE) + ".level" + iteration);
			}
		} while (clusterer.canClusterMore());

		if (graph == null) {
			return false;
		}

		// Now replace each leaf with a FloatLeafNode containing mean and stddev
		for (LeafNode leaf : graph.getLeafNodes()) {
			FeatureVectorLeafNode fvLeaf = (FeatureVectorLeafNode) leaf;
			FeatureVector[] fvs = fvLeaf.getFeatureVectors();
			double[] dur = new double[fvs.length];
			for (int i = 0; i < fvs.length; i++) {
				dur[i] = unitFile.getUnit(fvs[i].getUnitIndex()).duration / (float) unitFile.getSampleRate();
			}
			double mean = MathUtils.mean(dur);
			double stddev = MathUtils.standardDeviation(dur, mean);
			FloatLeafNode floatLeaf = new FloatLeafNode(new float[] { (float) stddev, (float) mean });
			Node mother = fvLeaf.getMother();
			assert mother != null;
			if (mother.isDecisionNode()) {
				((DecisionNode) mother).replaceDaughter(floatLeaf, fvLeaf.getNodeIndex());
			} else {
				assert mother.isDirectedGraphNode();
				assert ((DirectedGraphNode) mother).getLeafNode() == fvLeaf;
				((DirectedGraphNode) mother).setLeafNode(floatLeaf);
			}
		}
		writer.saveGraph(graph, getProp(DURTREE));
		return true;

	}

	/**
	 * Provide the progress of computation, in percent, or -1 if that feature is not implemented.
	 * 
	 * @return -1 if not implemented, or an integer between 0 and 100.
	 */
	public int getProgress() {
		return -1;
	}

	public static void main(String[] args) throws Exception {
		DurationTreeTrainer dct = new DurationTreeTrainer();
		DatabaseLayout db = new DatabaseLayout(dct);
		dct.compute();
	}

}
