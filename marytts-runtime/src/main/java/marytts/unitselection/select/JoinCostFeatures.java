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
package marytts.unitselection.select;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.channels.FileChannel;
import java.util.Vector;

import marytts.exceptions.MaryConfigurationException;
import marytts.features.ByteValuedFeatureProcessor;
import marytts.features.MaryGenericFeatureProcessors;
import marytts.modules.phonemiser.Allophone;
import marytts.server.MaryProperties;
import marytts.signalproc.display.Histogram;
import marytts.unitselection.data.DiphoneUnit;
import marytts.unitselection.data.Unit;
import marytts.unitselection.weightingfunctions.WeightFunc;
import marytts.unitselection.weightingfunctions.WeightFunctionManager;
import marytts.util.MaryUtils;
import marytts.util.data.MaryHeader;
import marytts.util.io.StreamUtils;

public class JoinCostFeatures implements JoinCostFunction {

	protected float wSignal;
	protected float wPhonetic;

	protected boolean debugShowCostGraph = false;
	protected double[] cumulWeightedSignalCosts = null;
	protected int nCostComputations = 0;

	protected PrecompiledJoinCostReader precompiledCosts;

	protected JoinCostReporter jcr;

	/****************/
	/* DATA FIELDS */
	/****************/
	private MaryHeader hdr = null;

	private float[] featureWeight = null;
	private WeightFunc[] weightFunction = null;
	private boolean[] isLinear = null; // wether the i'th weight function is a linear function

	private float[][] leftJCF = null;
	private float[][] rightJCF = null;

	/****************/
	/* CONSTRUCTORS */
	/****************/

	/**
	 * Empty constructor; when using this, call load() separately to initialise this class.
	 * 
	 * @see #load(String joinFileName, InputStream weightStream, String precompiledCostFileName, float wSignal)
	 */
	public JoinCostFeatures() {
	}

	/**
	 * Constructor which read a Mary Join Cost file.
	 * 
	 * @param fileName
	 *            fileName
	 * @throws IOException
	 *             IOException
	 * @throws MaryConfigurationException
	 *             MaryConfigurationException
	 */
	public JoinCostFeatures(String fileName) throws IOException, MaryConfigurationException {
		load(fileName, null, null, (float) 0.5);
	}

	/**
	 * Initialise this join cost function by reading the appropriate settings from the MaryProperties using the given
	 * configPrefix.
	 * 
	 * @param configPrefix
	 *            the prefix for the (voice-specific) config entries to use when looking up files to load.
	 * @throws MaryConfigurationException
	 *             MaryConfigurationException
	 */
	public void init(String configPrefix) throws MaryConfigurationException {
		String joinFileName = MaryProperties.needFilename(configPrefix + ".joinCostFile");
		String precomputedJoinCostFileName = MaryProperties.getFilename(configPrefix + ".precomputedJoinCostFile");
		float wSignal = Float.parseFloat(MaryProperties.getProperty(configPrefix + ".joincostfunction.wSignal", "1.0"));
		try {
			InputStream joinWeightStream = MaryProperties.getStream(configPrefix + ".joinCostWeights");
			load(joinFileName, joinWeightStream, precomputedJoinCostFileName, wSignal);
		} catch (IOException ioe) {
			throw new MaryConfigurationException("Problem loading join file " + joinFileName, ioe);
		}
	}

	/**
	 * Load weights and values from the given file
	 * 
	 * @param joinFileName
	 *            the file from which to read default weights and join cost features
	 * @param weightStream
	 *            an optional file from which to read weights, taking precedence over
	 * @param precompiledCostFileName
	 *            an optional file containing precompiled join costs
	 * @param wSignal
	 *            Relative weight of the signal-based join costs relative to the phonetic join costs computed from the target
	 * @throws IOException
	 *             IOException
	 * @throws MaryConfigurationException
	 *             MaryConfigurationException
	 */
	public void load(String joinFileName, InputStream weightStream, String precompiledCostFileName, float wSignal)
			throws IOException, MaryConfigurationException {
		loadFromByteBuffer(joinFileName, weightStream, precompiledCostFileName, wSignal);
	}

	/**
	 * Load weights and values from the given file
	 * 
	 * @param joinFileName
	 *            the file from which to read default weights and join cost features
	 * @param weightStream
	 *            an optional file from which to read weights, taking precedence over
	 * @param precompiledCostFileName
	 *            an optional file containing precompiled join costs
	 * @param wSignal
	 *            Relative weight of the signal-based join costs relative to the phonetic join costs computed from the target
	 * @throws IOException
	 *             IOException
	 * @throws MaryConfigurationException
	 *             MaryConfigurationException
	 */
	private void loadFromByteBuffer(String joinFileName, InputStream weightStream, String precompiledCostFileName, float wSignal)
			throws IOException, MaryConfigurationException {
		if (precompiledCostFileName != null) {
			precompiledCosts = new PrecompiledJoinCostReader(precompiledCostFileName);
		}
		this.wSignal = wSignal;
		wPhonetic = 1 - wSignal;
		/* Open the file */
		FileInputStream fis = new FileInputStream(joinFileName);
		FileChannel fc = fis.getChannel();
		ByteBuffer bb = fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size());
		/* Read the Mary header */
		hdr = new MaryHeader(bb);
		if (hdr.getType() != MaryHeader.JOINFEATS) {
			throw new IOException("File [" + joinFileName + "] is not a valid Mary join features file.");
		}
		try {
			/* Read the feature weights and feature processors */
			int numberOfFeatures = bb.getInt();
			featureWeight = new float[numberOfFeatures];
			weightFunction = new WeightFunc[numberOfFeatures];
			isLinear = new boolean[numberOfFeatures];
			WeightFunctionManager wfm = new WeightFunctionManager();
			String wfStr = null;
			for (int i = 0; i < numberOfFeatures; i++) {
				featureWeight[i] = bb.getFloat();
				wfStr = StreamUtils.readUTF(bb);
				if ("".equals(wfStr))
					weightFunction[i] = wfm.getWeightFunction("linear");
				else
					weightFunction[i] = wfm.getWeightFunction(wfStr);
			}
			// Overwrite weights and weight functions from file?
			if (weightStream != null) {
				MaryUtils.getLogger("JoinCostFeatures").debug("Overwriting join cost weights");
				Object[] weightData = readJoinCostWeightsStream(weightStream);
				featureWeight = (float[]) weightData[0];
				String[] wf = (String[]) weightData[1];
				if (featureWeight.length != numberOfFeatures)
					throw new IllegalArgumentException("Join cost file contains " + numberOfFeatures
							+ " features, but weight file contains " + featureWeight.length + " feature weights!");
				for (int i = 0; i < numberOfFeatures; i++) {
					weightFunction[i] = wfm.getWeightFunction(wf[i]);
				}
			}
			for (int i = 0; i < numberOfFeatures; i++) {
				isLinear[i] = weightFunction[i].whoAmI().equals("linear");
			}

			/* Read the left and right Join Cost Features */
			int numberOfUnits = bb.getInt();
			FloatBuffer fb = bb.asFloatBuffer();
			leftJCF = new float[numberOfUnits][];
			rightJCF = new float[numberOfUnits][];
			for (int i = 0; i < numberOfUnits; i++) {
				// System.out.println("Reading join features for unit "+i+" out of "+numberOfUnits);
				leftJCF[i] = new float[numberOfFeatures];
				fb.get(leftJCF[i]);
				rightJCF[i] = new float[numberOfFeatures];
				fb.get(rightJCF[i]);
			}
		} catch (EOFException e) {
			IOException ioe = new IOException("The currently read Join Cost File has prematurely reached EOF.");
			ioe.initCause(e);
			throw ioe;

		}
		if (MaryProperties.getBoolean("debug.show.cost.graph")) {
			debugShowCostGraph = true;
			cumulWeightedSignalCosts = new double[featureWeight.length];
			jcr = new JoinCostReporter(cumulWeightedSignalCosts);
			jcr.showInJFrame("Average signal join costs", false, false);
			jcr.start();
		}

	}

	/**
	 * Load weights and values from the given file
	 * 
	 * @param joinFileName
	 *            the file from which to read default weights and join cost features
	 * @param weightStream
	 *            an optional file from which to read weights, taking precedence over
	 * @param precompiledCostFileName
	 *            an optional file containing precompiled join costs
	 * @param wSignal
	 *            Relative weight of the signal-based join costs relative to the phonetic join costs computed from the target
	 * @throws IOException
	 *             IOException
	 * @throws MaryConfigurationException
	 *             MaryConfigurationException
	 */
	private void loadFromStream(String joinFileName, InputStream weightStream, String precompiledCostFileName, float wSignal)
			throws IOException, MaryConfigurationException {
		if (precompiledCostFileName != null) {
			precompiledCosts = new PrecompiledJoinCostReader(precompiledCostFileName);
		}
		this.wSignal = wSignal;
		wPhonetic = 1 - wSignal;
		/* Open the file */
		File fid = new File(joinFileName);
		DataInput raf = new DataInputStream(new BufferedInputStream(new FileInputStream(fid)));
		/* Read the Mary header */
		hdr = new MaryHeader(raf);
		if (hdr.getType() != MaryHeader.JOINFEATS) {
			throw new MaryConfigurationException("File [" + joinFileName + "] is not a valid Mary join features file.");
		}
		try {
			/* Read the feature weights and feature processors */
			int numberOfFeatures = raf.readInt();
			featureWeight = new float[numberOfFeatures];
			weightFunction = new WeightFunc[numberOfFeatures];
			isLinear = new boolean[numberOfFeatures];
			WeightFunctionManager wfm = new WeightFunctionManager();
			String wfStr = null;
			for (int i = 0; i < numberOfFeatures; i++) {
				featureWeight[i] = raf.readFloat();
				wfStr = raf.readUTF();
				if ("".equals(wfStr))
					weightFunction[i] = wfm.getWeightFunction("linear");
				else
					weightFunction[i] = wfm.getWeightFunction(wfStr);
			}
			// Overwrite weights and weight functions from file?
			if (weightStream != null) {
				MaryUtils.getLogger("JoinCostFeatures").debug("Overwriting join cost weights");
				Object[] weightData = readJoinCostWeightsStream(weightStream);
				featureWeight = (float[]) weightData[0];
				String[] wf = (String[]) weightData[1];
				if (featureWeight.length != numberOfFeatures)
					throw new IllegalArgumentException("Join cost file contains " + numberOfFeatures
							+ " features, but weight file contains " + featureWeight.length + " feature weights!");
				for (int i = 0; i < numberOfFeatures; i++) {
					weightFunction[i] = wfm.getWeightFunction(wf[i]);
				}
			}
			for (int i = 0; i < numberOfFeatures; i++) {
				isLinear[i] = weightFunction[i].whoAmI().equals("linear");
			}

			/* Read the left and right Join Cost Features */
			int numberOfUnits = raf.readInt();
			leftJCF = new float[numberOfUnits][];
			rightJCF = new float[numberOfUnits][];
			for (int i = 0; i < numberOfUnits; i++) {
				// System.out.println("Reading join features for unit "+i+" out of "+numberOfUnits);
				leftJCF[i] = new float[numberOfFeatures];
				for (int j = 0; j < numberOfFeatures; j++) {
					leftJCF[i][j] = raf.readFloat();
				}
				rightJCF[i] = new float[numberOfFeatures];
				for (int j = 0; j < numberOfFeatures; j++) {
					rightJCF[i][j] = raf.readFloat();
				}
			}
		} catch (EOFException e) {
			IOException ioe = new IOException("The currently read Join Cost File has prematurely reached EOF.");
			ioe.initCause(e);
			throw ioe;

		}
		if (MaryProperties.getBoolean("debug.show.cost.graph")) {
			debugShowCostGraph = true;
			cumulWeightedSignalCosts = new double[featureWeight.length];
			jcr = new JoinCostReporter(cumulWeightedSignalCosts);
			jcr.showInJFrame("Average signal join costs", false, false);
			jcr.start();
		}

	}

	/**
	 * Read the join cost weight specifications from the given file. The weights will be normalized such that they sum to one.
	 * 
	 * @param fileName
	 *            the text file containing the join weights
	 * @throws IOException
	 *             IOException
	 * @throws FileNotFoundException
	 *             FileNotFoundException
	 * @return readJoinCostWeightsStream(new FileInputStream(fileName))
	 * */
	public static Object[] readJoinCostWeightsFile(String fileName) throws IOException, FileNotFoundException {
		return readJoinCostWeightsStream(new FileInputStream(fileName));
	}

	/**
	 * Read the join cost weight specifications from the given file. The weights will be normalized such that they sum to one.
	 * 
	 * @param weightStream
	 *            the text file containing the join weights
	 * @throws IOException
	 *             IOException
	 * @throws FileNotFoundException
	 *             FileNotFoundException
	 * @return Object[] { fw, wfun }
	 * */
	public static Object[] readJoinCostWeightsStream(InputStream weightStream) throws IOException, FileNotFoundException {
		Vector v = new Vector(16, 16);
		Vector vf = new Vector(16, 16);
		/* Open the file */
		BufferedReader in = new BufferedReader(new InputStreamReader(weightStream, "UTF-8"));
		/* Loop through the lines */
		String line = null;
		String[] fields = null;
		float sumOfWeights = 0;
		while ((line = in.readLine()) != null) {
			// System.out.println( line );
			line = line.split("#", 2)[0]; // Remove possible trailing comments
			line = line.trim(); // Remove leading and trailing blanks
			if (line.equals(""))
				continue; // Empty line: don't parse
			line = line.split(":", 2)[1].trim(); // Remove the line number and :
			// System.out.print( "CLEANED: [" + line + "]" );
			fields = line.split("\\s", 2); // Separate the weight value from the function name
			float aWeight = Float.parseFloat(fields[0]);
			sumOfWeights += aWeight;
			v.add(new Float(aWeight)); // Push the weight
			vf.add(fields[1]); // Push the function
			// System.out.println( "NBFEA=" + numberOfFeatures );
		}
		in.close();
		// System.out.flush();
		/* Export the vector of weighting function names as a String array: */
		String[] wfun = (String[]) vf.toArray(new String[vf.size()]);
		/*
		 * For the weights, create a float array containing the weights, normalized such that they sum to one:
		 */
		float[] fw = new float[v.size()];
		for (int i = 0; i < fw.length; i++) {
			Float aWeight = (Float) v.get(i);
			fw[i] = aWeight.floatValue() / sumOfWeights;
		}
		/* Return these as an Object[2]. */
		return new Object[] { fw, wfun };
	}

	/*****************/
	/* ACCESSORS */
	/*****************/

	/**
	 * Get the number of feature weights and weighting functions.
	 * 
	 * @return (featureWeight.length)
	 */
	public int getNumberOfFeatures() {
		return (featureWeight.length);
	}

	/**
	 * Get the number of units.
	 * 
	 * @return (leftJCF.length)
	 */
	public int getNumberOfUnits() {
		return (leftJCF.length);
	}

	/**
	 * Gets the array of left join cost features for a particular unit index.
	 * 
	 * @param u
	 *            The index of the considered unit.
	 * 
	 * @return The array of left join cost features for the given unit.
	 */
	public float[] getLeftJCF(int u) {
		if (u < 0) {
			throw new RuntimeException("The unit index [" + u + "] is out of range: a unit index can't be negative.");
		}
		if (u > getNumberOfUnits()) {
			throw new RuntimeException("The unit index [" + u + "] is out of range: this file contains [" + getNumberOfUnits()
					+ "] units.");
		}
		return (leftJCF[u]);
	}

	/**
	 * Gets the array of right join cost features for a particular unit index.
	 * 
	 * @param u
	 *            The index of the considered unit.
	 * 
	 * @return The array of right join cost features for the given unit.
	 */
	public float[] getRightJCF(int u) {
		if (u < 0) {
			throw new RuntimeException("The unit index [" + u + "] is out of range: a unit index can't be negative.");
		}
		if (u > getNumberOfUnits()) {
			throw new RuntimeException("The unit index [" + u + "] is out of range: this file contains [" + getNumberOfUnits()
					+ "] units.");
		}
		return (rightJCF[u]);
	}

	/*****************/
	/* MISC METHODS */
	/*****************/

	/**
	 * Deliver the join cost between two units described by their index.
	 * 
	 * @param u1
	 *            the left unit
	 * @param u2
	 *            the right unit
	 * 
	 * @return the cost of joining the right Join Cost features of the left unit with the left Join Cost Features of the right
	 *         unit.
	 */
	public double cost(int u1, int u2) {
		/* Check the given indexes */
		if (u1 < 0) {
			throw new RuntimeException("The left unit index [" + u1 + "] is out of range: a unit index can't be negative.");
		}
		// if ( u1 > getNumberOfUnits() ) {
		if (u1 > leftJCF.length) {
			throw new RuntimeException("The left unit index [" + u1 + "] is out of range: this file contains ["
					+ getNumberOfUnits() + "] units.");
		}
		if (u2 < 0) {
			throw new RuntimeException("The right unit index [" + u2 + "] is out of range: a unit index can't be negative.");
		}
		// if ( u2 > getNumberOfUnits() ) {
		if (u2 > leftJCF.length) {
			throw new RuntimeException("The right unit index [" + u2 + "] is out of range: this file contains ["
					+ getNumberOfUnits() + "] units.");
		}
		if (debugShowCostGraph) {
			jcr.tick();
		}
		/* Cumulate the join costs for each feature */
		double res = 0.0;
		float[] v1 = rightJCF[u1];
		float[] v2 = leftJCF[u2];
		for (int i = 0; i < v1.length; i++) {
			float a = v1[i];
			float b = v2[i];
			// if (!Float.isNaN(v1[i]) && !Float.isNaN(v2[i])) {
			if (!(a != a) && !(b != b)) {
				double c;
				if (isLinear[i]) {
					c = featureWeight[i] * (a > b ? (a - b) : (b - a));
				} else {
					c = featureWeight[i] * weightFunction[i].cost(a, b);
				}
				res += c;
				if (debugShowCostGraph) {
					cumulWeightedSignalCosts[i] += wSignal * c;
				}
			} // if anything is NaN, count the cost as 0.
		}
		return (res);
	}

	/**
	 * A combined cost computation, as a weighted sum of the signal-based cost (computed from the units) and the phonetics-based
	 * cost (computed from the targets).
	 * 
	 * @param t1
	 *            The left target.
	 * @param u1
	 *            The left unit.
	 * @param t2
	 *            The right target.
	 * @param u2
	 *            The right unit.
	 * 
	 * @return the cost of joining the left unit with the right unit, as a non-negative value.
	 */
	public double cost(Target t1, Unit u1, Target t2, Unit u2) {
		// Units of length 0 cannot be joined:
		if (u1.duration == 0 || u2.duration == 0)
			return Double.POSITIVE_INFINITY;
		// In the case of diphones, replace them with the relevant part:
		boolean bothDiphones = true;
		if (u1 instanceof DiphoneUnit) {
			u1 = ((DiphoneUnit) u1).right;
		} else {
			bothDiphones = false;
		}
		if (u2 instanceof DiphoneUnit) {
			u2 = ((DiphoneUnit) u2).left;
		} else {
			bothDiphones = false;
		}

		if (u1.index + 1 == u2.index)
			return 0;
		// Either not half phone synthesis, or at a diphone boundary
		double cost = 1; // basic penalty for joins of non-contiguous units.
		if (bothDiphones && precompiledCosts != null) {
			cost += precompiledCosts.cost(t1, u1, t2, u2);
		} else { // need to actually compute the cost
			cost += cost(u1.index, u2.index);
		}
		return cost;
	}

	/**
	 * A phonetic join cost, computed solely from the target.
	 * 
	 * @param t1
	 *            the left target
	 * @param t2
	 *            the right target
	 * @return a non-negative join cost, usually between 0 (best) and 1 (worst).
	 * @deprecated
	 */
	protected double cost(Target t1, Target t2) {
		// TODO: This is really ad hoc for the moment. Redo once we know what we are doing.
		// Add penalties for a number of criteria.
		double cost = 0;
		ByteValuedFeatureProcessor stressProcessor = new MaryGenericFeatureProcessors.Stressed("",
				new MaryGenericFeatureProcessors.SyllableNavigator());
		// Stressed?
		boolean stressed1 = stressProcessor.process(t1) == (byte) 1;
		boolean stressed2 = stressProcessor.process(t1) == (byte) 1;
		// Try to avoid joining in a stressed syllable:
		if (stressed1 || stressed2)
			cost += 0.2;
		Allophone p1 = t1.getAllophone();
		Allophone p2 = t2.getAllophone();

		// Discourage joining vowels:
		if (p1.isVowel() || p2.isVowel())
			cost += 0.2;
		// Discourage joining glides:
		if (p1.isGlide() || p2.isGlide())
			cost += 0.2;
		// Discourage joining voiced segments:
		if (p1.isVoiced() || p2.isVoiced())
			cost += 0.1;
		// If both are voiced, it's really bad
		if (p1.isVoiced() && p2.isVoiced())
			cost += 0.1;
		// Slightly penalize nasals and liquids
		if (p1.isNasal() || p2.isNasal())
			cost += 0.05;
		if (p1.isLiquid() || p2.isLiquid())
			cost += 0.05;
		// Fricatives -- nothing?
		// Plosives -- nothing?

		if (cost > 1)
			cost = 1;
		return cost;
	}

	public static class JoinCostReporter extends Histogram {
		private double[] data;
		private int lastN = 0;
		private int nCostComputations = 0;

		public JoinCostReporter(double[] data) {
			super(0, 1, data);
			this.data = data;
		}

		public void start() {
			new Thread() {
				public void run() {
					while (isVisible()) {
						try {
							Thread.sleep(500);
						} catch (InterruptedException ie) {
						}
						updateGraph();
					}
				}
			}.start();
		}

		/**
		 * Register one new cost computation
		 */
		public void tick() {
			nCostComputations++;
		}

		protected void updateGraph() {
			if (nCostComputations == lastN)
				return;
			lastN = nCostComputations;
			double[] newCosts = new double[data.length];
			for (int i = 0; i < newCosts.length; i++) {
				newCosts[i] = data[i] / nCostComputations;
			}
			updateData(0, 1, newCosts);
			repaint();
		}
	}

}
