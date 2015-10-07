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
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import marytts.cart.CART;
import marytts.cart.FeatureVectorCART;
import marytts.cart.LeafNode;
import marytts.cart.Node;
import marytts.cart.LeafNode.LeafType;
import marytts.cart.impose.FeatureArrayIndexer;
import marytts.cart.impose.MaryNode;
import marytts.cart.io.MaryCARTReader;
import marytts.cart.io.MaryCARTWriter;
import marytts.cart.io.WagonCARTReader;
import marytts.cart.io.WagonCARTWriter;
import marytts.exceptions.MaryConfigurationException;
import marytts.features.FeatureDefinition;
import marytts.features.FeatureVector;
import marytts.unitselection.data.FeatureFileReader;
import marytts.unitselection.data.MCepDatagram;
import marytts.unitselection.data.MCepTimelineReader;
import marytts.unitselection.data.UnitFileReader;
import marytts.util.data.Datagram;
import marytts.util.io.StreamGobbler;

public class CARTBuilder extends VoiceImportComponent {

	private MCepTimelineReader mcepTimeline;
	private UnitFileReader unitFile;
	private String wagonDirName;
	private String wagonDescFile;
	private String wagonFeatsFile;
	private String wagonCartFile;
	private String wagonDisTabsFile;
	private int numProcesses;
	private boolean callWagon;

	private DatabaseLayout db;
	private int percent = 0;
	public final String ACFEATUREFILE = "CARTBuilder.acFeatureFile";
	public final String FEATURESEQFILE = "CARTBuilder.featureSeqFile";
	public final String TOPLEVELTREEFILE = "CARTBuilder.topLevelTreeFile";
	public final String CARTFILE = "CARTBuilder.cartFile";

	public final String MCEPTIMELINE = "CARTBuilder.mcepTimeline";
	public final String UNITFILE = "CARTBuilder.unitFile";
	public final String READFEATURESEQUENCE = "CARTBuilder.readFeatureSequence";
	public final String MAXLEAFSIZE = "CARTBuilder.maxLeafSize";
	public final String CALLWAGON = "CARTBuilder.callWagon";

	public final String NUMPROCESSES = "CARTBuilder.numProcesses";

	public String getName() {
		return "CARTBuilder";
	}

	@Override
	protected void initialiseComp() {
		callWagon = Boolean.parseBoolean(db.getProp(CALLWAGON));
		wagonDirName = db.getProp(DatabaseLayout.TEMPDIR);
		wagonDescFile = wagonDirName + "wagon.desc";
		wagonFeatsFile = wagonDirName + "wagon.feats";
		wagonCartFile = wagonDirName + "wagon.cart";
		wagonDisTabsFile = wagonDirName + "wagon.distabs";
		// make sure that we have at least a feature sequence file
		File featSeqFile = new File(getProp(FEATURESEQFILE));
		if (!featSeqFile.exists()) {
			File topLevelTreeFile = new File(getProp(TOPLEVELTREEFILE));
			if (!topLevelTreeFile.exists()) {
				try {
					PrintWriter featSeqOut = new PrintWriter(new OutputStreamWriter(new FileOutputStream(featSeqFile), "UTF-8"));
					featSeqOut.println("# Automatically generated feature sequence file for CARTBuilder\n"
							+ "# Add features (one per line) to refine\n"
							+ "# Defines the feature sequence used to build the top-level CART\n" + "phone");
					featSeqOut.flush();
					featSeqOut.close();
				} catch (Exception e) {
					System.out.println("Warning: no feature sequence file " + getProp(FEATURESEQFILE)
							+ " and no top level tree file " + getProp(TOPLEVELTREEFILE) + "; CARTBuilder will not run.");
				}
			}
		}
		String numProcessesString = getProp(NUMPROCESSES);
		if (numProcessesString == null) {
			numProcesses = 1;
		} else {
			try {
				numProcesses = Integer.parseInt(numProcessesString);
			} catch (NumberFormatException nfe) {
				numProcesses = 1;
			}
		}
		if (numProcesses < 1)
			numProcesses = 1;
	}

	public SortedMap<String, String> getDefaultProps(DatabaseLayout theDb) {
		this.db = theDb;
		if (props == null) {
			props = new TreeMap<String, String>();
			String filedir = db.getProp(DatabaseLayout.FILEDIR);
			String maryext = db.getProp(DatabaseLayout.MARYEXT);
			props.put(ACFEATUREFILE, filedir + "halfphoneFeatures_ac" + maryext);
			props.put(FEATURESEQFILE, db.getProp(DatabaseLayout.CONFIGDIR) + "featureSequence.txt");
			props.put(TOPLEVELTREEFILE, db.getProp(DatabaseLayout.CONFIGDIR) + "topLevel.tree");
			props.put(CARTFILE, filedir + "cart" + maryext);

			props.put(MCEPTIMELINE, filedir + "timeline_mcep" + maryext);
			props.put(UNITFILE, filedir + "halfphoneUnits" + maryext);
			props.put(READFEATURESEQUENCE, "true");
			props.put(MAXLEAFSIZE, "10000000");
			props.put(CALLWAGON, "false");
			props.put(NUMPROCESSES, "1");
		}

		return props;
	}

	protected void setupHelp() {
		props2Help = new TreeMap<String, String>();
		props2Help.put(ACFEATUREFILE, "file containing all halfphone units and their target cost features"
				+ " plus the acoustic target cost features");
		props2Help.put(FEATURESEQFILE, "file containing the feature sequence for the basic tree");
		props2Help.put(TOPLEVELTREEFILE, "file containing the basic tree");
		props2Help.put(CARTFILE, "file containing the preselection CART. Will be created by this module");
		props2Help.put(MCEPTIMELINE, "file containing the mcep files");
		props2Help.put(UNITFILE, "file containing all halfphone units");
		props2Help.put(READFEATURESEQUENCE, "if \"true\", basic tree is read from feature sequence file;"
				+ " if \"false\", basic tree is read from top level tree file.");
		props2Help.put(MAXLEAFSIZE, "the maximum number of units in a leaf of the basic tree");
		props2Help.put(NUMPROCESSES, "number of wagon processes to run in parallel - bewteen 1 and the number of CPUs");
		props2Help.put(CALLWAGON,
				"whether to call wagon to build an acoustics-based pre-selection sub-tree for each top-level leaf");
	}

	public boolean compute() throws Exception {

		WagonCARTWriter wr = new WagonCARTWriter();
		long time = System.currentTimeMillis();

		// read in the features with feature file indexer
		System.out.println("Reading feature file ...");
		String featureFile = getProp(ACFEATUREFILE);
		FeatureFileReader ffr = FeatureFileReader.getFeatureFileReader(featureFile);
		FeatureVector[] featureVectorsCopy = ffr.getCopyOfFeatureVectors();
		FeatureDefinition featureDefinition = ffr.getFeatureDefinition();
		// remove the feature vectors of edge units
		List<FeatureVector> fVList = new ArrayList<FeatureVector>();
		int edgeIndex = featureDefinition.getFeatureIndex(FeatureDefinition.EDGEFEATURE);
		for (int i = 0; i < featureVectorsCopy.length; i++) {
			FeatureVector nextFV = featureVectorsCopy[i];
			if (!nextFV.isEdgeVector(edgeIndex))
				fVList.add(nextFV);
		}
		int fVListSize = fVList.size();
		int removed = featureVectorsCopy.length - fVListSize;
		System.out.println("Removed " + removed + " edge vectors; " + "remaining vectors : " + fVListSize);
		FeatureVector[] featureVectors = new FeatureVector[fVListSize];
		for (int i = 0; i < featureVectors.length; i++) {
			featureVectors[i] = (FeatureVector) fVList.get(i);
		}
		CART topLevelCART;
		boolean fromFeatureSequence = Boolean.valueOf(getProp(READFEATURESEQUENCE)).booleanValue();
		if (fromFeatureSequence) {
			/* Build the top level tree from a feature sequence */
			FeatureArrayIndexer fai = new FeatureArrayIndexer(featureVectors, featureDefinition);
			System.out.println(" ... done!");
			// read in the feature sequence
			// open the file
			System.out.println("Reading feature sequence ...");
			String featSeqFile = getProp(FEATURESEQFILE);
			BufferedReader buf = new BufferedReader(new FileReader(new File(featSeqFile)));
			// each line contains one feature
			String line = buf.readLine();
			// collect features in a list
			List<String> features = new ArrayList<String>();
			while (line != null) {
				// Skip empty lines and lines starting with #:
				if (!(line.trim().equals("") || line.startsWith("#"))) {
					features.add(line.trim());
				}
				line = buf.readLine();
			}
			// convert list to int array
			int[] featureSequence = new int[features.size()];
			for (int i = 0; i < features.size(); i++) {
				featureSequence[i] = featureDefinition.getFeatureIndex((String) features.get(i));
			}
			System.out.println(" ... done!");

			// sort the features according to feature sequence
			System.out.println("Sorting features ...");
			fai.deepSort(featureSequence);
			System.out.println(" ... done!");
			// get the resulting tree
			MaryNode topLevelTree = fai.getTree();

			// convert the top-level CART to Wagon Format
			System.out.println("Building CART from tree ...");
			topLevelCART = new FeatureVectorCART(topLevelTree, fai);
			System.out.println(" ... done!");
		} else {
			/* read in the top-level tree from file */
			String filename = getProp(TOPLEVELTREEFILE);
			System.out.println("Reading empty top-level tree from file " + filename + " ...");
			BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(new File(filename)), "UTF-8"));
			topLevelCART = new CART();
			WagonCARTReader wagonReader = new WagonCARTReader(LeafType.FeatureVectorLeafNode);
			topLevelCART.setRootNode(wagonReader.load(reader, featureDefinition));

			System.out.println(" ... done!");

			// fill in the leafs of the tree
			System.out.println("Filling leafs of top-level tree ...");

			wagonReader.fillLeafs(topLevelCART.getRootNode(), featureVectors);

			System.out.println(" ... done!");
		}

		System.out.println("Checking top-level CART for reasonable leaf sizes ...");
		int minSize = 5;
		int maxSize = Integer.parseInt(getProp(MAXLEAFSIZE));
		int nTooSmall = 0;
		int nTooBig = 0;
		int nLeaves = 0;
		for (LeafNode leaf : topLevelCART.getLeafNodes()) {
			if (leaf.getNumberOfData() < minSize) {
				// Ignore a few meaningless combinations:
				String path = leaf.getDecisionPath();
				if (path.indexOf("phone==0") == -1 && path.indexOf("vc==0") == -1
						&& !(path.indexOf("prev_vc==+") != -1 && path.indexOf("prev_c") != -1)
						&& !(path.indexOf("prev_vc==-") != -1 && path.indexOf("prev_vheight") != -1)) {

					// System.out.println("leaf too small: "+leaf.getDecisionPath());
					nTooSmall++;
				}

			} else if (leaf.getNumberOfData() > maxSize) {
				System.out.println("               LEAF TOO BIG: " + leaf.getDecisionPath());
				nTooBig++;
			}
			nLeaves++;
		}
		if (nTooSmall > 0 || nTooBig > 0) {
			System.out.println("Bad top-level cart: " + nTooSmall + "/" + nLeaves + " leaves are too small, " + nTooBig + "/"
					+ nLeaves + " are too big");
		} else {
			System.out.println("... OK!");
		}

		if (callWagon) {
			boolean ok = replaceLeaves(topLevelCART, featureDefinition);
			if (!ok) {
				System.out.println("Could not replace leaves");
				return false;
			}
		}

		// dump big CART to binary file
		String destinationFile = getProp(CARTFILE);
		MaryCARTWriter ww = new MaryCARTWriter();
		ww.dumpMaryCART(topLevelCART, destinationFile);

		// say how long you took
		long timeDiff = System.currentTimeMillis() - time;
		System.out.println("Processing took " + timeDiff + " milliseconds.");

		return true;
	}

	/**
	 * Read in the CARTs from festival/trees/ directory, and store them in a CARTMap
	 * 
	 * @param filename
	 *            the festvox directory of a voice
	 * @param featDef
	 *            featDef
	 * @throws IOException
	 *             IOException
	 * @throws MaryConfigurationException
	 *             MaryConfigurationException
	 * @return cart
	 */
	public CART importCART(String filename, FeatureDefinition featDef) throws IOException, MaryConfigurationException {
		// open CART-File
		System.out.println("Reading CART from " + filename + " ...");

		// create a wagon cart reader for this class of tree
		WagonCARTReader wagonReader = new WagonCARTReader(LeafType.IntAndFloatArrayLeafNode);

		// build and return CART
		// old: CART cart = new ExtendedClassificationTree();
		CART cart = new CART();
		// old: cart.load(filename,featDef,null);
		cart.setRootNode(wagonReader.load(filename, featDef, null));

		System.out.println(" ... done!");
		return cart;
	}

	/**
	 * For each leaf in the CART, run Wagon on the feature vectors in this CART, and replace leaf by resulting CART
	 * 
	 * @param cart
	 *            the CART
	 * @param featureDefinition
	 *            the definition of the features
	 * @throws IOException
	 *             IOException
	 * @throws MaryConfigurationException
	 *             MaryConfigurationException
	 * @return true when done
	 */
	public boolean replaceLeaves(CART cart, FeatureDefinition featureDefinition) throws IOException, MaryConfigurationException {
		try {
			System.out.println("Replacing Leaves ...");

			System.out.println("Cart has " + cart.getNumNodes() + " nodes");

			// create wagon dir if it does not exist
			File wagonDir = new File(wagonDirName);
			if (!wagonDir.exists()) {
				wagonDir.mkdir();
			}
			// get the filenames for the various files used by wagon
			String featureDefFile = wagonDescFile;
			String featureVectorsFile = wagonFeatsFile;
			String cartFile = wagonCartFile;
			String distanceTableFile = wagonDisTabsFile;
			// dump the feature definitions
			PrintWriter out = new PrintWriter(new FileOutputStream(new File(featureDefFile)));
			Set<String> featuresToIgnore = new HashSet<String>();
			featuresToIgnore.add("unit_logf0");
			featuresToIgnore.add("unit_duration");
			featureDefinition.generateAllDotDescForWagon(out, featuresToIgnore);
			out.close();

			System.out.println("Will run " + numProcesses + " wagon processes in parallel");
			WagonCallerThread[] wagons = new WagonCallerThread[numProcesses];

			int stop = 50; // do not want leaves smaller than this
			List<LeafNode> leaves = new ArrayList<LeafNode>();
			for (LeafNode leaf : cart.getLeafNodes()) {
				leaves.add(leaf);
			}
			int nLeaves = leaves.size();
			System.out.println("Computing acoustic subtrees for " + nLeaves + " unit clusters");
			/* call Wagon successively */
			// go through the CART
			int wagonID = 0;
			for (int i = 0; i < nLeaves; i++) {
				long startTime = System.currentTimeMillis();
				percent = 100 * i / nLeaves;
				LeafNode leaf = (LeafNode) leaves.get(i);
				FeatureVector[] featureVectors = ((LeafNode.FeatureVectorLeafNode) leaf).getFeatureVectors();
				if (featureVectors.length <= stop)
					continue;
				wagonID++;
				System.out.println("Leaf replacement no. " + wagonID + " started at " + new Date());
				// dump the feature vectors
				System.out.println(wagonID + "> Dumping " + featureVectors.length + " feature vectors...");
				String featureFileName = featureVectorsFile + wagonID;
				dumpFeatureVectors(featureVectors, featureDefinition, featureFileName);
				long endTime = System.currentTimeMillis();
				System.out.println(wagonID + ">... dumping feature vectors took " + (endTime - startTime) + " ms");
				startTime = endTime;
				// dump the distance tables
				System.out.println(wagonID + "> Computing distance tables...");
				String distanceFileName = distanceTableFile + wagonID;
				buildAndDumpDistanceTables(featureVectors, distanceFileName, featureDefinition);
				endTime = System.currentTimeMillis();
				System.out.println(wagonID + "> ... computing distance tables took " + (endTime - startTime) + " ms");
				startTime = endTime;
				// Dispatch call to Wagon to one of the wagon callers:
				WagonCallerThread wagon = new WagonCallerThread(String.valueOf(wagonID), leaf, featureDefinition, featureVectors,
						featureDefFile, featureFileName, distanceFileName, cartFile + wagonID, 0, stop,
						db.getProp(DatabaseLayout.ESTDIR));
				boolean dispatched = false;
				while (!dispatched) {
					for (int w = 0; w < numProcesses && !dispatched; w++) {
						if (wagons[w] == null) {
							System.out.println("Dispatching wagon " + wagonID + " as process " + (w + 1) + " out of "
									+ numProcesses);
							wagons[w] = wagon;
							wagon.start();
							dispatched = true;
						} else if (wagons[w].finished()) {
							if (!wagons[w].success()) {
								System.out.println("Wagon " + wagons[w].id() + " failed. Aborting");
								return false;
							}
							System.out.println("Dispatching wagon " + wagonID + " as process " + (w + 1) + " out of "
									+ numProcesses);
							wagons[w] = wagon;
							wagon.start();
							dispatched = true;
						}
					}
					if (!dispatched) {
						try {
							Thread.sleep(100);
						} catch (InterruptedException ie) {
						}
					}
				}
			}
			// Now make sure we wait for all wagons to finish
			for (int w = 0; w < numProcesses; w++) {
				if (wagons[w] != null) {
					while (!wagons[w].finished()) {
						try {
							wagons[w].join();
						} catch (InterruptedException ie) {
						}
					}
					if (!wagons[w].success()) {
						System.out.println("Wagon " + wagons[w].id() + " failed. Aborting");
						return false;
					}
				}
			}

		} catch (IOException ioe) {
			IOException newIOE = new IOException("Error replacing leaves");
			newIOE.initCause(ioe);
			throw newIOE;
		}
		System.out.println(" ... done!");
		return true;
	}

	/**
	 * Dump the given feature vectors to a file with the given filename
	 * 
	 * @param featureVectors
	 *            the feature vectors
	 * @param featDef
	 *            the feature definition
	 * @param filename
	 *            the filename
	 * @throws FileNotFoundException
	 *             FileNotFoundException
	 */
	public void dumpFeatureVectors(FeatureVector[] featureVectors, FeatureDefinition featDef, String filename)
			throws FileNotFoundException {
		// open file
		PrintWriter out = new PrintWriter(new BufferedOutputStream(new FileOutputStream(filename)));
		// get basic feature info
		int numByteFeats = featDef.getNumberOfByteFeatures();
		int numShortFeats = featDef.getNumberOfShortFeatures();
		int numFloatFeats = featDef.getNumberOfContinuousFeatures();
		// loop through the feature vectors
		for (int i = 0; i < featureVectors.length; i++) {
			// Print the feature string
			out.print(i + " " + featDef.toFeatureString(featureVectors[i]));
			// print a newline if this is not the last vector
			if (i + 1 != featureVectors.length) {
				out.print("\n");
			}
		}
		// dump and close
		out.flush();
		out.close();
	}

	/**
	 * Build the distance tables for the units from which we have the feature vectors and dump them to a file with the given
	 * filename
	 * 
	 * @param featureVectors
	 *            the feature vectors of the units
	 * @param filename
	 *            the filename
	 * @param featDef
	 *            featDef
	 * @throws IOException
	 *             IOException
	 * @throws MaryConfigurationException
	 *             MaryConfigurationException
	 */
	public void buildAndDumpDistanceTables(FeatureVector[] featureVectors, String filename, FeatureDefinition featDef)
			throws IOException, MaryConfigurationException {
		/* Load the MelCep timeline and the unit file */
		if (mcepTimeline == null) {
			try {
				mcepTimeline = new MCepTimelineReader(getProp(MCEPTIMELINE));
			} catch (IOException e) {
				throw new RuntimeException("Failed to read the Mel-Cepstrum timeline [" + getProp(MCEPTIMELINE)
						+ "] due to the following IOException: ", e);
			}
		}
		if (unitFile == null) {
			try {
				unitFile = new UnitFileReader(getProp(UNITFILE));
			} catch (IOException e) {
				throw new RuntimeException("Failed to read the unit file [" + getProp(UNITFILE)
						+ "] due to the following IOException: ", e);
			}
		}

		/* Dereference the number of units once and for all */
		int numUnits = featureVectors.length;
		/*
		 * Read the Mel Cepstra for each unit, and cumulate their sufficient statistics in the same loop
		 */
		double[][][] melCep = new double[numUnits][][];
		double val = 0;
		double[] sum = new double[mcepTimeline.getOrder()];
		double[] sumSq = new double[mcepTimeline.getOrder()];
		double[] sigma2 = new double[mcepTimeline.getOrder()];
		double N = 0.0;
		for (int i = 0; i < numUnits; i++) {
			// System.out.println( "FEATURE_VEC_IDX=" + i + " UNITIDX=" + featureVectors[i].getUnitIndex() );
			/* Read the datagrams for the current unit */
			Datagram[] buff = null;
			MCepDatagram[] dat = null;
			// System.out.println( featDef.toFeatureString( featureVectors[i] ) );
			try {
				buff = mcepTimeline.getDatagrams(unitFile.getUnit(featureVectors[i].getUnitIndex()), unitFile.getSampleRate());
				// System.out.println( "NUMFRAMES=" + buff.length );
				dat = new MCepDatagram[buff.length];
				for (int d = 0; d < buff.length; d++) {
					dat[d] = (MCepDatagram) (buff[d]);
				}
			} catch (Exception e) {
				throw new RuntimeException("Failed to read the datagrams for unit number [" + featureVectors[i].getUnitIndex()
						+ "] from the Mel-cepstrum timeline due to the following Exception: ", e);
			}
			N += (double) (dat.length); // Update the frame counter
			melCep[i] = new double[dat.length][];
			for (int j = 0; j < dat.length; j++) {
				melCep[i][j] = dat[j].getCoeffsAsDouble();
				/* Cumulate the sufficient statistics */
				for (int k = 0; k < mcepTimeline.getOrder(); k++) {
					val = melCep[i][j][k];
					sum[k] += val;
					sumSq[k] += (val * val);
				}
			}
		}
		/* Finalize the variance calculation */
		for (int k = 0; k < mcepTimeline.getOrder(); k++) {
			val = sum[k];
			sigma2[k] = (sumSq[k] - (val * val) / N) / N;
		}
		// System.out.println("Read MFCCs, now computing distances");
		/* Compute the unit distance matrix */
		double[][] dist = new double[numUnits][numUnits];
		for (int i = 0; i < numUnits; i++) {
			dist[i][i] = 0.0; // <= Set the diagonal to 0.0
			for (int j = 1; j < numUnits; j++) {
				/*
				 * Get the DTW distance between the two sequences: System.out.println( "Entering DTW : " + featDef.getFeatureName(
				 * 0 ) + " " + featureVectors[i].getFeatureAsString( 0, featDef ) + ".length=" + melCep[i].length + " ; " +
				 * featureVectors[j].getFeatureAsString( 0, featDef ) + ".length=" + melCep[j].length + " ." );
				 * System.out.flush();
				 */
				if (melCep[i].length == 0 || melCep[j].length == 0) {
					if (melCep[i].length == melCep[j].length) { // both 0 length
						dist[i][j] = dist[j][i] = 0;
					} else {
						dist[i][j] = dist[j][i] = 100000; // a large number
					}
				} else {
					// dist[i][j] = dist[j][i] = dtwDist( melCep[i], melCep[j], sigma2 );
					// System.out.println("Using Mahalanobis distance\n"+
					// "Distance is "+dist[i][j]);

					double f0Weight = 100; // ad hoc value
					double durWeight = 1000; // ad hoc value

					double spectralDist = stretchDist(melCep[i], melCep[j], sigma2);
					double f0Dist = f0Weight * f0Dist(featureVectors[i], featureVectors[j], featDef);
					double durDist = durWeight * durDist(featureVectors[i], featureVectors[j], featDef);
					// System.out.println("Spectral distance: "+spectralDist+" -- F0 distance: "+f0Dist+" -- Duration distance: "+durDist);
					dist[i][j] = dist[j][i] = spectralDist + f0Dist + durDist;
				}
			}
		}
		/* Write the matrix to disk */
		// System.out.println( "Writing distance matrix to file [" + filename + "]");
		PrintWriter out = new PrintWriter(new BufferedOutputStream(new FileOutputStream(filename)));
		for (int i = 0; i < numUnits; i++) {
			for (int j = 0; j < numUnits; j++) {
				out.print((float) (dist[i][j]) + " ");
			}
			out.print("\n");
		}
		out.flush();
		out.close();

	}

	private double f0Dist(FeatureVector fv1, FeatureVector fv2, FeatureDefinition fd) {
		int iLogF0 = fd.getFeatureIndex("unit_logf0");
		float logf0_1 = fv1.getContinuousFeature(iLogF0);
		float logF0_2 = fv2.getContinuousFeature(iLogF0);
		return Math.abs(logf0_1 - logF0_2);
	}

	private double durDist(FeatureVector fv1, FeatureVector fv2, FeatureDefinition fd) {
		int iLogF0 = fd.getFeatureIndex("unit_duration");
		float logf0_1 = fv1.getContinuousFeature(iLogF0);
		float logF0_2 = fv2.getContinuousFeature(iLogF0);
		return Math.abs(logf0_1 - logF0_2);
	}

	/**
	 * Computes an average Mahalanobis distance along the simple time-stretched correspondence between two frame sequences.
	 * 
	 * @param seq1
	 *            a frame sequence
	 * @param seq2
	 *            another frame sequence
	 * @param sigma2
	 *            the variance of the vectors
	 * @return the average Mahalanobis distance between the two frame sequences
	 */
	private double stretchDist(double[][] seq1, double[][] seq2, double[] sigma2) {
		double[][] shorter;
		double[][] longer;
		if (seq1.length < seq2.length) {
			shorter = seq1;
			longer = seq2;
		} else {
			shorter = seq2;
			longer = seq1;
		}
		float lengthFactor = shorter.length / (float) longer.length;
		double totalDist = 0;
		for (int i = 0; i < longer.length; i++) {
			int iShorter = (int) (lengthFactor * i);
			double dist = mahalanobis(longer[i], shorter[iShorter], sigma2);
			if (Double.isInfinite(dist) || Double.isNaN(dist))
				dist = 100000; // a large number
			totalDist += dist;
		}
		return totalDist / longer.length;
	}

	/**
	 * Computes an average Mahalanobis distance along the optimal DTW path between two vector sequences.
	 * 
	 * The DTW constraint used here is: D(i,j) = min { D(i-2,j-1) + 2*d(i-1,j) + d(i,j) ; D(i-1,j-1) + 2*d(i,j) ; D(i-1,j-2) +
	 * 2*d(i,j-1) + d(i,j) }
	 * 
	 * At the end of the DTW, the cumulated distance is normalized by the number of local distances cumulated along the optimal
	 * path. Hence, the resulting unit distance is homogeneous to an average having the order of magnitude of a single Mahalanobis
	 * distance, and that for each pair of units.
	 * 
	 * @param seq1
	 *            The first sequence of (Mel-cepstrum) vectors.
	 * @param seq2
	 *            The second sequence of (Mel-cepstrum) vectors.
	 * @param sigma2
	 *            The variance of the vectors.
	 * @return The average Mahalanobis distance along the optimal DTW path.
	 */
	private double dtwDist(double[][] seq1, double[][] seq2, double[] sigma2) {

		if ((seq1.length <= 0) || (seq2.length <= 0)) {
			throw new RuntimeException("Can't compute a DTW distance from a sequence with length 0 or negative. "
					+ "(seq1.length=" + seq1.length + "; seq2.length=" + seq2.length + ")");
		}

		int l1 = seq1.length;
		int l2 = seq2.length;
		double[][] d = new double[l1][l2];
		double[][] D = new double[l1][l2];
		int[][] Nd = new int[l1][l2]; // <= Number of cumulated distances, for the final averaging
		double[] minV = new double[3];
		int[] minNd = new int[3];
		int minIdx = 0;
		/* Fill the local distance matrix */
		for (int i = 0; i < l1; i++) {
			for (int j = 0; j < l2; j++) {
				d[i][j] = mahalanobis(seq1[i], seq2[j], sigma2);
			}
		}
		/* Compute the optimal DTW distance: */
		/* - 1st row/column: */
		/* (This part works for 1 frame or more in either sequence.) */
		D[0][0] = 2 * d[0][0];
		for (int i = 1; i < l1; i++) {
			D[i][0] = d[i][0];
			Nd[i][0] = 1;
		}
		for (int i = 1; i < l2; i++) {
			D[0][i] = d[0][i];
			Nd[0][i] = 1;
		}
		/* - 2nd row/column: */
		/* (This part works for 2 frames or more in either sequence.) */
		/* corner: i==1, j==1 */
		if ((l1 > 1) && (l2 > 1)) {
			minV[0] = 2 * d[0][1] + d[1][1];
			minNd[0] = 3;
			minV[1] = D[0][0] + 2 * d[1][1];
			minNd[1] = Nd[0][0] + 2;
			minV[2] = 2 * d[1][0] + d[1][1];
			minNd[2] = 3;
			minIdx = minV[0] < minV[1] ? 0 : 1;
			minIdx = minV[2] < minV[minIdx] ? 2 : minIdx;
			D[1][1] = minV[minIdx];
			Nd[1][1] = minNd[minIdx];

			/* 2nd row: j==1 ; 2nd col: i==1 */
			for (int i = 2; i < l1; i++) {
				// Row:
				minV[0] = D[i - 2][0] + 2 * d[i - 1][1] + d[i][1];
				minNd[0] = Nd[i - 2][0] + 3;
				minV[1] = D[i - 1][0] + 2 * d[i][1];
				minNd[1] = Nd[i - 1][0] + 2;
				minV[2] = 2 * d[i][0] + d[i][1];
				minNd[2] = 3;
				minIdx = minV[0] < minV[1] ? 0 : 1;
				minIdx = minV[2] < minV[minIdx] ? 2 : minIdx;
				D[i][1] = minV[minIdx];
				Nd[i][1] = minNd[minIdx];
			}
			for (int i = 2; i < l2; i++) {
				// Column:
				minV[0] = 2 * d[0][i] + d[1][i];
				minNd[0] = 3;
				minV[1] = D[0][i - 1] + 2 * d[1][i];
				minNd[1] = Nd[0][i - 1] + 2;
				minV[2] = D[0][i - 2] + 2 * d[1][i - 1] + d[1][i];
				minNd[2] = Nd[0][i - 2] + 3;
				minIdx = minV[0] < minV[1] ? 0 : 1;
				minIdx = minV[2] < minV[minIdx] ? 2 : minIdx;
				D[1][i] = minV[minIdx];
				Nd[1][i] = minNd[minIdx];
			}

		}
		/* - Rest of the matrix: */
		/* (This part works for 3 frames or more in either sequence.) */
		if ((l1 > 2) && (l2 > 2)) {
			for (int i = 2; i < l1; i++) {
				for (int j = 2; j < l2; j++) {
					minV[0] = D[i - 2][j - 1] + 2 * d[i - 1][j] + d[i][j];
					minNd[0] = Nd[i - 2][j - 1] + 3;
					minV[1] = D[i - 1][j - 1] + 2 * d[i][j];
					minNd[1] = Nd[i - 1][j - 1] + 2;
					minV[2] = D[i - 1][j - 2] + 2 * d[i][j - 1] + d[i][j];
					minNd[0] = Nd[i - 1][j - 2] + 3;
					minIdx = minV[0] < minV[1] ? 0 : 1;
					minIdx = minV[2] < minV[minIdx] ? 2 : minIdx;
					D[i][j] = minV[minIdx];
					Nd[i][j] = minNd[minIdx];
				}
			}
		}
		/* Return */
		return (D[l1 - 1][l2 - 1] / (double) (Nd[l1 - 1][l2 - 1]));
	}

	/**
	 * Mahalanobis distance between two feature vectors.
	 * 
	 * @param v1
	 *            A feature vector.
	 * @param v2
	 *            Another feature vector.
	 * @param sigma2
	 *            The variance of the distribution of the considered feature vectors.
	 * @return The mahalanobis distance between v1 and v2.
	 */
	private double mahalanobis(double[] v1, double[] v2, double[] sigma2) {
		double sum = 0.0;
		double diff = 0.0;
		for (int i = 0; i < v1.length; i++) {
			diff = v1[i] - v2[i];
			sum += ((diff * diff) / sigma2[i]);
		}
		return (sum);
	}

	/**
	 * Provide the progress of computation, in percent, or -1 if that feature is not implemented.
	 * 
	 * @return -1 if not implemented, or an integer between 0 and 100.
	 */
	public int getProgress() {
		return percent;
	}

	public static class WagonCallerThread extends Thread {
		// the Edinburgh Speech tools directory
		protected String ESTDIR;
		protected String arguments;
		protected File cartFile;
		protected File valueFile;
		protected File distanceTableFile;
		protected String id;
		protected LeafNode leafToReplace;
		protected FeatureDefinition featureDefinition;
		protected FeatureVector[] featureVectors;
		protected boolean finished = false;
		protected boolean success = false;

		public WagonCallerThread(String id, LeafNode leafToReplace, FeatureDefinition featureDefinition,
				FeatureVector[] featureVectors, String descFilename, String valueFilename, String distanceTableFilename,
				String cartFilename, int balance, int stop, String ESTDIR) {
			this.id = id;
			this.leafToReplace = leafToReplace;
			this.featureDefinition = featureDefinition;
			this.featureVectors = featureVectors;
			this.ESTDIR = ESTDIR;

			this.valueFile = new File(valueFilename);
			this.distanceTableFile = new File(distanceTableFilename);
			this.cartFile = new File(cartFilename);

			this.arguments = "-desc " + descFilename + " -data " + valueFilename + " -balance " + balance + " -distmatrix "
					+ distanceTableFilename + " -stop " + stop + " -output " + cartFilename;
		}

		public void run() {
			try {
				long startTime = System.currentTimeMillis();
				System.out.println(id + "> Calling wagon as follows:");
				System.out.println(ESTDIR + "/bin/wagon " + arguments);
				Process p = Runtime.getRuntime().exec(ESTDIR + "/bin/wagon " + arguments);
				// collect the output
				// read from error stream
				StreamGobbler errorGobbler = new StreamGobbler(p.getErrorStream(), id + " err");

				// read from output stream
				StreamGobbler outputGobbler = new StreamGobbler(p.getInputStream(), id + " out");
				// start reading from the streams
				errorGobbler.start();
				outputGobbler.start();
				p.waitFor();
				if (p.exitValue() != 0) {
					finished = true;
					success = false;
				} else {
					success = true;
					System.out.println(id + "> Wagon call took " + (System.currentTimeMillis() - startTime) + " ms");

					// read in the resulting CART
					System.out.println(id + "> Reading CART");
					BufferedReader buf = new BufferedReader(new FileReader(cartFile));
					// old CART newCART = new ExtendedClassificationTree(buf, featureDefinition);
					CART newCART = new CART();
					WagonCARTReader wagonReader = new WagonCARTReader(LeafType.IntAndFloatArrayLeafNode);
					newCART.setRootNode(wagonReader.load(buf, featureDefinition));
					buf.close();

					// Fix the new cart's leaves:
					// They are currently the index numbers in featureVectors;
					// but what we need is the unit index numbers!
					for (LeafNode leaf : newCART.getLeafNodes()) {
						int[] data = (int[]) leaf.getAllData();
						for (int i = 0; i < data.length; i++) {
							data[i] = featureVectors[data[i]].getUnitIndex();
						}
					}

					// replace the leaf by the CART
					System.out.println(id + "> Replacing leaf");
					System.out.println(id + "> (before: " + leafToReplace.getRootNode().getNumberOfNodes() + " nodes, adding "
							+ newCART.getNumNodes() + ")");
					Node newNode = CART.replaceLeafByCart(newCART, leafToReplace);
					System.out.println(id + "> done -- cart now has " + newNode.getRootNode().getNumberOfNodes() + " nodes.");

					finished = true;
				}
				if (!Boolean.getBoolean("wagon.keepfiles")) {
					valueFile.delete();
					distanceTableFile.delete();
				}

			} catch (Exception e) {
				e.printStackTrace();
				finished = true;
				success = false;
				throw new RuntimeException("Exception running wagon");
			}

		}

		public boolean finished() {
			return finished;
		}

		public boolean success() {
			return success;
		}

		public String id() {
			return id;
		}

	}

	public static void main(String[] args) throws Exception {
		CARTBuilder cartBuilder = new CARTBuilder();
		DatabaseLayout db = new DatabaseLayout(cartBuilder);
		// compute
		/*
		 * boolean ok = cartBuilder.compute(); if (ok) System.out.println("Finished successfully!"); else
		 * System.out.println("Failed.");
		 */
		// loading a cart in MaryCART format
		String path = "/project/mary/marcela/Unit-Selection-voices/DFKI_German_Poker/mary_files/";
		String halfPhonesFile = path + "halfphoneFeatures.mry";
		FeatureFileReader ffr = FeatureFileReader.getFeatureFileReader(halfPhonesFile);
		FeatureDefinition feaDef = ffr.getFeatureDefinition();

		String cartFile = path + "cart.mry.new";
		MaryCARTReader rm = new MaryCARTReader();
		CART cart = rm.load(cartFile);

		// loading a cart in WagonCART format
		// String cartFile = path + "cart.mry";
		// WagonCARTReader wr = new WagonCARTReader("ExtendedClassificationTree");
		// cart.setRootNode(wr.load(cartFile,feaDef, null));

		System.out.println("Finished  loading the tree!");

		// check the leaves and their decision paths
		String pwFile = path + "decnodes.txt";
		PrintWriter pw = new PrintWriter(new FileWriter(new File(pwFile)));
		System.out.println("Number of nodes loaded: " + cart.getNumNodes());
		int i = 0;
		for (LeafNode leaf : cart.getLeafNodes()) {
			if (leaf.getNumberOfData() > 0) {
				i++;
				pw.println(i + ": " + leaf.getDecisionPath());
			} else
				pw.println("   " + leaf.getDecisionPath());
		}
		pw.close();
		System.out.println("Generated decision paths file:" + pwFile);

	}

}
