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
package marytts.features;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import marytts.util.io.StreamUtils;
import marytts.util.string.ByteStringTranslator;
import marytts.util.string.IntStringTranslator;
import marytts.util.string.ShortStringTranslator;

/**
 * A feature definition object represents the "meaning" of feature vectors. It consists of a list of byte-valued, short-valued and
 * continuous features by name and index position in the feature vector; the respective possible feature values (and corresponding
 * byte and short codes); and, optionally, the weights and, for continuous features, weighting functions for each feature.
 * 
 * @author Marc Schr&ouml;der
 * @author steiner
 */
public class FeatureDefinition {
	public static final String BYTEFEATURES = "ByteValuedFeatureProcessors";
	public static final String SHORTFEATURES = "ShortValuedFeatureProcessors";
	public static final String CONTINUOUSFEATURES = "ContinuousFeatureProcessors";
	public static final String FEATURESIMILARITY = "FeatureSimilarity";
	public static final char WEIGHT_SEPARATOR = '|';
	public static final String EDGEFEATURE = "edge";
	public static final String EDGEFEATURE_START = "start";
	public static final String EDGEFEATURE_END = "end";
	public static final String NULLVALUE = "0";

	private int numByteFeatures;
	private int numShortFeatures;
	private int numContinuousFeatures;
	private float[] featureWeights;
	private IntStringTranslator featureNames;
	// feature values: for byte and short features only
	private ByteStringTranslator[] byteFeatureValues;
	private ShortStringTranslator[] shortFeatureValues;
	private String[] floatWeightFuncts; // for continuous features only
	private float[][][] similarityMatrices = null;

	/**
	 * Create a feature definition object, reading textual data from the given BufferedReader.
	 * 
	 * @param input
	 *            a BufferedReader from which a textual feature definition can be read.
	 * @param readWeights
	 *            a boolean indicating whether or not to read weights from input. If weights are read, they will be normalized so
	 *            that they sum to one.
	 * @throws IOException
	 *             if a reading problem occurs
	 *
	 */
	public FeatureDefinition(BufferedReader input, boolean readWeights) throws IOException {
		// Section BYTEFEATURES
		String line = input.readLine();
		if (line == null)
			throw new IOException("Could not read from input");
		while (line.matches("^\\s*#.*") || line.matches("\\s*")) {
			line = input.readLine();
		}
		if (!line.trim().equals(BYTEFEATURES)) {
			throw new IOException("Unexpected input: expected '" + BYTEFEATURES + "', read '" + line + "'");
		}
		List<String> byteFeatureLines = new ArrayList<String>();
		while (true) {
			line = input.readLine();
			if (line == null)
				throw new IOException("Could not read from input");
			line = line.trim();
			if (line.equals(SHORTFEATURES))
				break; // Found end of section
			byteFeatureLines.add(line);
		}
		// Section SHORTFEATURES
		List<String> shortFeatureLines = new ArrayList<String>();
		while (true) {
			line = input.readLine();
			if (line == null)
				throw new IOException("Could not read from input");
			line = line.trim();
			if (line.equals(CONTINUOUSFEATURES))
				break; // Found end of section
			shortFeatureLines.add(line);
		}
		// Section CONTINUOUSFEATURES
		List<String> continuousFeatureLines = new ArrayList<String>();
		boolean readFeatureSimilarity = false;
		while ((line = input.readLine()) != null) { // it's OK if we hit the end of the file now
			line = line.trim();
			// if (line.equals(FEATURESIMILARITY) || line.equals("")) break; // Found end of section
			if (line.equals(FEATURESIMILARITY)) {
				// readFeatureSimilarityMatrices(input);
				readFeatureSimilarity = true;
				break;
			} else if (line.equals("")) { // empty line: end of section
				break;
			}
			continuousFeatureLines.add(line);
		}
		numByteFeatures = byteFeatureLines.size();
		numShortFeatures = shortFeatureLines.size();
		numContinuousFeatures = continuousFeatureLines.size();
		int total = numByteFeatures + numShortFeatures + numContinuousFeatures;
		featureNames = new IntStringTranslator(total);
		byteFeatureValues = new ByteStringTranslator[numByteFeatures];
		shortFeatureValues = new ShortStringTranslator[numShortFeatures];
		float sumOfWeights = 0; // for normalisation of weights
		if (readWeights) {
			featureWeights = new float[total];
			floatWeightFuncts = new String[numContinuousFeatures];
		}

		for (int i = 0; i < numByteFeatures; i++) {
			line = byteFeatureLines.get(i);
			String featureDef;
			if (readWeights) {
				int seppos = line.indexOf(WEIGHT_SEPARATOR);
				if (seppos == -1)
					throw new IOException("Weight separator '" + WEIGHT_SEPARATOR + "' not found in line '" + line + "'");
				String weightDef = line.substring(0, seppos).trim();
				featureDef = line.substring(seppos + 1).trim();
				// The weight definition is simply the float number:
				featureWeights[i] = Float.parseFloat(weightDef);
				sumOfWeights += featureWeights[i];
				if (featureWeights[i] < 0)
					throw new IOException("Negative weight found in line '" + line + "'");
			} else {
				featureDef = line;
			}
			// Now featureDef is a String in which the feature name and all feature values
			// are separated by white space.
			String[] nameAndValues = featureDef.split("\\s+", 2);
			featureNames.set(i, nameAndValues[0]); // the feature name
			byteFeatureValues[i] = new ByteStringTranslator(nameAndValues[1].split("\\s+")); // the feature values
		}

		for (int i = 0; i < numShortFeatures; i++) {
			line = shortFeatureLines.get(i);
			String featureDef;
			if (readWeights) {
				int seppos = line.indexOf(WEIGHT_SEPARATOR);
				if (seppos == -1)
					throw new IOException("Weight separator '" + WEIGHT_SEPARATOR + "' not found in line '" + line + "'");
				String weightDef = line.substring(0, seppos).trim();
				featureDef = line.substring(seppos + 1).trim();
				// The weight definition is simply the float number:
				featureWeights[numByteFeatures + i] = Float.parseFloat(weightDef);
				sumOfWeights += featureWeights[numByteFeatures + i];
				if (featureWeights[numByteFeatures + i] < 0)
					throw new IOException("Negative weight found in line '" + line + "'");
			} else {
				featureDef = line;
			}
			// Now featureDef is a String in which the feature name and all feature values
			// are separated by white space.
			String[] nameAndValues = featureDef.split("\\s+", 2);
			featureNames.set(numByteFeatures + i, nameAndValues[0]); // the feature name
			shortFeatureValues[i] = new ShortStringTranslator(nameAndValues[1].split("\\s+")); // the feature values
		}

		for (int i = 0; i < numContinuousFeatures; i++) {
			line = continuousFeatureLines.get(i);
			String featureDef;
			if (readWeights) {
				int seppos = line.indexOf(WEIGHT_SEPARATOR);
				if (seppos == -1)
					throw new IOException("Weight separator '" + WEIGHT_SEPARATOR + "' not found in line '" + line + "'");
				String weightDef = line.substring(0, seppos).trim();
				featureDef = line.substring(seppos + 1).trim();
				// The weight definition is the float number plus a definition of a weight function:
				String[] weightAndFunction = weightDef.split("\\s+", 2);
				featureWeights[numByteFeatures + numShortFeatures + i] = Float.parseFloat(weightAndFunction[0]);
				sumOfWeights += featureWeights[numByteFeatures + numShortFeatures + i];
				if (featureWeights[numByteFeatures + numShortFeatures + i] < 0)
					throw new IOException("Negative weight found in line '" + line + "'");
				try {
					floatWeightFuncts[i] = weightAndFunction[1];
				} catch (ArrayIndexOutOfBoundsException e) {
					// System.out.println( "weightDef string was: '" + weightDef + "'." );
					// System.out.println( "Splitting part 1: '" + weightAndFunction[0] + "'." );
					// System.out.println( "Splitting part 2: '" + weightAndFunction[1] + "'." );
					throw new RuntimeException("The string [" + weightDef + "] appears to be a badly formed"
							+ " weight plus weighting function definition.");
				}
			} else {
				featureDef = line;
			}
			// Now featureDef is the feature name
			// or the feature name followed by the word "float"
			if (featureDef.endsWith("float")) {
				String[] featureDefSplit = featureDef.split("\\s+", 2);
				featureNames.set(numByteFeatures + numShortFeatures + i, featureDefSplit[0]);
			} else {
				featureNames.set(numByteFeatures + numShortFeatures + i, featureDef);
			}
		}
		// Normalize weights to sum to one:
		if (readWeights) {
			for (int i = 0; i < total; i++) {
				featureWeights[i] /= sumOfWeights;
			}
		}

		// read feature similarities here, if any
		if (readFeatureSimilarity) {
			readFeatureSimilarityMatrices(input);
		}
	}

	/**
	 * read similarity matrices from feature definition file
	 * 
	 * @param input
	 *            input
	 * @throws IOException
	 *             IOException
	 */
	private void readFeatureSimilarityMatrices(BufferedReader input) throws IOException {

		String line = null;

		similarityMatrices = new float[this.getNumberOfByteFeatures()][][];
		for (int i = 0; i < this.getNumberOfByteFeatures(); i++) {
			similarityMatrices[i] = null;
		}

		while ((line = input.readLine()) != null) {

			if ("".equals(line)) {
				return;
			}

			String[] featureUniqueValues = line.trim().split("\\s+");
			String featureName = featureUniqueValues[0];

			if (!isByteFeature(featureName)) {
				throw new RuntimeException(
						"Similarity matrix support is for bytefeatures only, but not for other feature types...");
			}

			int featureIndex = this.getFeatureIndex(featureName);
			int noUniqValues = featureUniqueValues.length - 1;
			similarityMatrices[featureIndex] = new float[noUniqValues][noUniqValues];

			for (int i = 1; i <= noUniqValues; i++) {

				Arrays.fill(similarityMatrices[featureIndex][i - 1], 0);
				String featureValue = featureUniqueValues[i];

				String matLine = input.readLine();
				if (matLine == null) {
					throw new RuntimeException("Feature definition file is having unexpected format...");
				}

				String[] lines = matLine.trim().split("\\s+");
				if (!featureValue.equals(lines[0])) {
					throw new RuntimeException("Feature definition file is having unexpected format...");
				}
				if (lines.length != i) {
					throw new RuntimeException("Feature definition file is having unexpected format...");
				}
				for (int j = 1; j < i; j++) {
					float similarity = (new Float(lines[j])).floatValue();
					similarityMatrices[featureIndex][i - 1][j - 1] = similarity;
					similarityMatrices[featureIndex][j - 1][i - 1] = similarity;
				}

			}
		}

	}

	/**
	 * Create a feature definition object, reading binary data from the given DataInput.
	 * 
	 * @param input
	 *            a DataInputStream or a RandomAccessFile from which a binary feature definition can be read.
	 * @throws IOException
	 *             if a reading problem occurs
	 */
	public FeatureDefinition(DataInput input) throws IOException {
		// Section BYTEFEATURES
		numByteFeatures = input.readInt();
		byteFeatureValues = new ByteStringTranslator[numByteFeatures];
		// Initialise global arrays to byte feature length first;
		// we have no means of knowing how many short or continuous
		// features there will be, so we need to resize later.
		// This will happen automatically for featureNames, but needs
		// to be done by hand for featureWeights.
		featureNames = new IntStringTranslator(numByteFeatures);
		featureWeights = new float[numByteFeatures];
		// There is no need to normalise weights here, because
		// they have already been normalized before the binary
		// file was written.
		for (int i = 0; i < numByteFeatures; i++) {
			featureWeights[i] = input.readFloat();
			String featureName = input.readUTF();
			featureNames.set(i, featureName);
			byte numberOfValuesEncoded = input.readByte(); // attention: this is an unsigned byte
			int numberOfValues = numberOfValuesEncoded & 0xFF;
			byteFeatureValues[i] = new ByteStringTranslator(numberOfValues);
			for (int b = 0; b < numberOfValues; b++) {
				String value = input.readUTF();
				byteFeatureValues[i].set((byte) b, value);
			}
		}
		// Section SHORTFEATURES
		numShortFeatures = input.readInt();
		if (numShortFeatures > 0) {
			shortFeatureValues = new ShortStringTranslator[numShortFeatures];
			// resize weight array:
			float[] newWeights = new float[numByteFeatures + numShortFeatures];
			System.arraycopy(featureWeights, 0, newWeights, 0, numByteFeatures);
			featureWeights = newWeights;

			for (int i = 0; i < numShortFeatures; i++) {
				featureWeights[numByteFeatures + i] = input.readFloat();
				String featureName = input.readUTF();
				featureNames.set(numByteFeatures + i, featureName);
				short numberOfValues = input.readShort();
				shortFeatureValues[i] = new ShortStringTranslator(numberOfValues);
				for (short s = 0; s < numberOfValues; s++) {
					String value = input.readUTF();
					shortFeatureValues[i].set(s, value);
				}
			}
		}
		// Section CONTINUOUSFEATURES
		numContinuousFeatures = input.readInt();
		floatWeightFuncts = new String[numContinuousFeatures];
		if (numContinuousFeatures > 0) {
			// resize weight array:
			float[] newWeights = new float[numByteFeatures + numShortFeatures + numContinuousFeatures];
			System.arraycopy(featureWeights, 0, newWeights, 0, numByteFeatures + numShortFeatures);
			featureWeights = newWeights;
		}
		for (int i = 0; i < numContinuousFeatures; i++) {
			featureWeights[numByteFeatures + numShortFeatures + i] = input.readFloat();
			floatWeightFuncts[i] = input.readUTF();
			String featureName = input.readUTF();
			featureNames.set(numByteFeatures + numShortFeatures + i, featureName);
		}
	}

	/**
	 * Create a feature definition object, reading binary data from the given byte buffer.
	 * 
	 * @param bb
	 *            a byte buffer from which a binary feature definition can be read.
	 * @throws IOException
	 *             if a reading problem occurs
	 */
	public FeatureDefinition(ByteBuffer bb) throws IOException {
		// Section BYTEFEATURES
		numByteFeatures = bb.getInt();
		byteFeatureValues = new ByteStringTranslator[numByteFeatures];
		// Initialise global arrays to byte feature length first;
		// we have no means of knowing how many short or continuous
		// features there will be, so we need to resize later.
		// This will happen automatically for featureNames, but needs
		// to be done by hand for featureWeights.
		featureNames = new IntStringTranslator(numByteFeatures);
		featureWeights = new float[numByteFeatures];
		// There is no need to normalise weights here, because
		// they have already been normalized before the binary
		// file was written.
		for (int i = 0; i < numByteFeatures; i++) {
			featureWeights[i] = bb.getFloat();
			String featureName = StreamUtils.readUTF(bb);
			featureNames.set(i, featureName);
			byte numberOfValuesEncoded = bb.get(); // attention: this is an unsigned byte
			int numberOfValues = numberOfValuesEncoded & 0xFF;
			byteFeatureValues[i] = new ByteStringTranslator(numberOfValues);
			for (int b = 0; b < numberOfValues; b++) {
				String value = StreamUtils.readUTF(bb);
				byteFeatureValues[i].set((byte) b, value);
			}
		}
		// Section SHORTFEATURES
		numShortFeatures = bb.getInt();
		if (numShortFeatures > 0) {
			shortFeatureValues = new ShortStringTranslator[numShortFeatures];
			// resize weight array:
			float[] newWeights = new float[numByteFeatures + numShortFeatures];
			System.arraycopy(featureWeights, 0, newWeights, 0, numByteFeatures);
			featureWeights = newWeights;

			for (int i = 0; i < numShortFeatures; i++) {
				featureWeights[numByteFeatures + i] = bb.getFloat();
				String featureName = StreamUtils.readUTF(bb);
				featureNames.set(numByteFeatures + i, featureName);
				short numberOfValues = bb.getShort();
				shortFeatureValues[i] = new ShortStringTranslator(numberOfValues);
				for (short s = 0; s < numberOfValues; s++) {
					String value = StreamUtils.readUTF(bb);
					shortFeatureValues[i].set(s, value);
				}
			}
		}
		// Section CONTINUOUSFEATURES
		numContinuousFeatures = bb.getInt();
		floatWeightFuncts = new String[numContinuousFeatures];
		if (numContinuousFeatures > 0) {
			// resize weight array:
			float[] newWeights = new float[numByteFeatures + numShortFeatures + numContinuousFeatures];
			System.arraycopy(featureWeights, 0, newWeights, 0, numByteFeatures + numShortFeatures);
			featureWeights = newWeights;
		}
		for (int i = 0; i < numContinuousFeatures; i++) {
			featureWeights[numByteFeatures + numShortFeatures + i] = bb.getFloat();
			floatWeightFuncts[i] = StreamUtils.readUTF(bb);
			String featureName = StreamUtils.readUTF(bb);
			featureNames.set(numByteFeatures + numShortFeatures + i, featureName);
		}
	}

	/**
	 * Write this feature definition in binary format to the given output.
	 * 
	 * @param out
	 *            a DataOutputStream or RandomAccessFile to which the FeatureDefinition should be written.
	 * @throws IOException
	 *             if a problem occurs while writing.
	 */
	public void writeBinaryTo(DataOutput out) throws IOException {
		// TODO to avoid duplicate code, replace this with writeBinaryTo(out, List<Integer>()) or some such

		// Section BYTEFEATURES
		out.writeInt(numByteFeatures);
		for (int i = 0; i < numByteFeatures; i++) {
			if (featureWeights != null) {
				out.writeFloat(featureWeights[i]);
			} else {
				out.writeFloat(0);
			}
			out.writeUTF(getFeatureName(i));

			int numValues = getNumberOfValues(i);
			byte numValuesEncoded = (byte) numValues; // an unsigned byte
			out.writeByte(numValuesEncoded);
			for (int b = 0; b < numValues; b++) {
				String value = getFeatureValueAsString(i, b);
				out.writeUTF(value);
			}
		}
		// Section SHORTFEATURES
		out.writeInt(numShortFeatures);
		for (int i = numByteFeatures; i < numByteFeatures + numShortFeatures; i++) {
			if (featureWeights != null) {
				out.writeFloat(featureWeights[i]);
			} else {
				out.writeFloat(0);
			}
			out.writeUTF(getFeatureName(i));
			short numValues = (short) getNumberOfValues(i);
			out.writeShort(numValues);
			for (short b = 0; b < numValues; b++) {
				String value = getFeatureValueAsString(i, b);
				out.writeUTF(value);
			}
		}
		// Section CONTINUOUSFEATURES
		out.writeInt(numContinuousFeatures);
		for (int i = numByteFeatures + numShortFeatures; i < numByteFeatures + numShortFeatures + numContinuousFeatures; i++) {
			if (featureWeights != null) {
				out.writeFloat(featureWeights[i]);
				out.writeUTF(floatWeightFuncts[i - numByteFeatures - numShortFeatures]);
			} else {
				out.writeFloat(0);
				out.writeUTF("");
			}
			out.writeUTF(getFeatureName(i));
		}
	}

	/**
	 * Write this feature definition in binary format to the given output, dropping featuresToDrop
	 * 
	 * @param out
	 *            a DataOutputStream or RandomAccessFile to which the FeatureDefinition should be written.
	 * @param featuresToDrop
	 *            List of Integers containing the indices of features to drop from DataOutputStream
	 * @throws IOException
	 *             if a problem occurs while writing.
	 */
	private void writeBinaryTo(DataOutput out, List<Integer> featuresToDrop) throws IOException {
		// how many features of each type are to be dropped
		int droppedByteFeatures = 0;
		int droppedShortFeatures = 0;
		int droppedContinuousFeatures = 0;
		for (int f : featuresToDrop) {
			if (f < numByteFeatures) {
				droppedByteFeatures++;
			} else if (f < numByteFeatures + numShortFeatures) {
				droppedShortFeatures++;
			} else if (f < numByteFeatures + numShortFeatures + numContinuousFeatures) {
				droppedContinuousFeatures++;
			}
		}
		// Section BYTEFEATURES
		out.writeInt(numByteFeatures - droppedByteFeatures);
		for (int i = 0; i < numByteFeatures; i++) {
			if (featuresToDrop.contains(i)) {
				continue;
			}
			if (featureWeights != null) {
				out.writeFloat(featureWeights[i]);
			} else {
				out.writeFloat(0);
			}
			out.writeUTF(getFeatureName(i));

			int numValues = getNumberOfValues(i);
			byte numValuesEncoded = (byte) numValues; // an unsigned byte
			out.writeByte(numValuesEncoded);
			for (int b = 0; b < numValues; b++) {
				String value = getFeatureValueAsString(i, b);
				out.writeUTF(value);
			}
		}
		// Section SHORTFEATURES
		out.writeInt(numShortFeatures - droppedShortFeatures);
		for (int i = numByteFeatures; i < numByteFeatures + numShortFeatures; i++) {
			if (featuresToDrop.contains(i)) {
				continue;
			}
			if (featureWeights != null) {
				out.writeFloat(featureWeights[i]);
			} else {
				out.writeFloat(0);
			}
			out.writeUTF(getFeatureName(i));
			short numValues = (short) getNumberOfValues(i);
			out.writeShort(numValues);
			for (short b = 0; b < numValues; b++) {
				String value = getFeatureValueAsString(i, b);
				out.writeUTF(value);
			}
		}
		// Section CONTINUOUSFEATURES
		out.writeInt(numContinuousFeatures - droppedContinuousFeatures);
		for (int i = numByteFeatures + numShortFeatures; i < numByteFeatures + numShortFeatures + numContinuousFeatures; i++) {
			if (featuresToDrop.contains(i)) {
				continue;
			}
			if (featureWeights != null) {
				out.writeFloat(featureWeights[i]);
				out.writeUTF(floatWeightFuncts[i - numByteFeatures - numShortFeatures]);
			} else {
				out.writeFloat(0);
				out.writeUTF("");
			}
			out.writeUTF(getFeatureName(i));
		}
	}

	/**
	 * Get the total number of features.
	 * 
	 * @return the number of features
	 */
	public int getNumberOfFeatures() {
		return numByteFeatures + numShortFeatures + numContinuousFeatures;
	}

	/**
	 * Get the number of byte features.
	 * 
	 * @return the number of features
	 */
	public int getNumberOfByteFeatures() {
		return numByteFeatures;
	}

	/**
	 * Get the number of short features.
	 * 
	 * @return the number of features
	 */
	public int getNumberOfShortFeatures() {
		return numShortFeatures;
	}

	/**
	 * Get the number of continuous features.
	 * 
	 * @return the number of features
	 */
	public int getNumberOfContinuousFeatures() {
		return numContinuousFeatures;
	}

	/**
	 * For the feature with the given index, return the weight.
	 * 
	 * @param featureIndex
	 *            featureIndex
	 * @return a non-negative weight.
	 */
	public float getWeight(int featureIndex) {
		return featureWeights[featureIndex];
	}

	public float[] getFeatureWeights() {
		return featureWeights;
	}

	/**
	 * Get the name of any weighting function associated with the given feature index. For byte-valued and short-valued features,
	 * this method will always return null; for continuous features, the method will return the name of a weighting function, or
	 * null.
	 * 
	 * @param featureIndex
	 *            featureIndex
	 * @return the name of a weighting function, or null
	 */
	public String getWeightFunctionName(int featureIndex) {
		return floatWeightFuncts[featureIndex - numByteFeatures - numShortFeatures];
	}

	// //////////////////// META-INFORMATION METHODS ///////////////////////

	/**
	 * Translate between a feature index and a feature name.
	 * 
	 * @param index
	 *            a feature index, as could be used to access a feature value in a FeatureVector.
	 * @return the name of the feature corresponding to the index
	 * @throws IndexOutOfBoundsException
	 *             if index&lt;0 or index&gt;getNumberOfFeatures()
	 */
	public String getFeatureName(int index) {
		return featureNames.get(index);
	}

	/**
	 * Translate between an array of feature indexes and an array of feature names.
	 * 
	 * @param index
	 *            an array of feature indexes, as could be used to access a feature value in a FeatureVector.
	 * @return an array with the name of the features corresponding to the index
	 * @throws IndexOutOfBoundsException
	 *             if any of the indexes is &lt;0 or &gt;getNumberOfFeatures()
	 */
	public String[] getFeatureNameArray(int[] index) {
		String[] ret = new String[index.length];
		for (int i = 0; i < index.length; i++) {
			ret[i] = getFeatureName(index[i]);
		}
		return (ret);
	}

	/**
	 * Get names of all features
	 * 
	 * @return an array of all feature name strings
	 */
	public String[] getFeatureNameArray() {
		String[] names = new String[getNumberOfFeatures()];
		for (int i = 0; i < names.length; i++) {
			names[i] = getFeatureName(i);
		}
		return (names);
	}

	/**
	 * Get names of byte features
	 * 
	 * @return an array of byte feature name strings
	 */
	public String[] getByteFeatureNameArray() {
		String[] byteFeatureNames = new String[numByteFeatures];
		for (int i = 0; i < numByteFeatures; i++) {
			assert isByteFeature(i);
			byteFeatureNames[i] = getFeatureName(i);
		}
		return byteFeatureNames;
	}

	/**
	 * Get names of short features
	 * 
	 * @return an array of short feature name strings
	 */
	public String[] getShortFeatureNameArray() {
		String[] shortFeatureNames = new String[numShortFeatures];
		for (int i = 0; i < numShortFeatures; i++) {
			int shortFeatureIndex = numByteFeatures + i;
			assert isShortFeature(shortFeatureIndex);
			shortFeatureNames[i] = getFeatureName(shortFeatureIndex);
		}
		return shortFeatureNames;
	}

	/**
	 * Get names of continuous features
	 * 
	 * @return an array of continuous feature name strings
	 */
	public String[] getContinuousFeatureNameArray() {
		String[] continuousFeatureNames = new String[numContinuousFeatures];
		for (int i = 0; i < numContinuousFeatures; i++) {
			int continuousFeatureIndex = numByteFeatures + numShortFeatures + i;
			assert isContinuousFeature(continuousFeatureIndex);
			continuousFeatureNames[i] = getFeatureName(continuousFeatureIndex);
		}
		return continuousFeatureNames;
	}

	/**
	 * List all feature names, separated by white space, in their order of definition.
	 * 
	 * @return buf converted into a string
	 */
	public String getFeatureNames() {
		StringBuilder buf = new StringBuilder();
		for (int i = 0, n = getNumberOfFeatures(); i < n; i++) {
			if (buf.length() > 0)
				buf.append(" ");
			buf.append(featureNames.get(i));
		}
		return buf.toString();
	}

	/**
	 * Indicate whether the feature definition contains the feature with the given name
	 * 
	 * @param name
	 *            the feature name in question, e.g. "next_next_phone"
	 * @return featureNames.contains(name)
	 */
	public boolean hasFeature(String name) {
		return featureNames.contains(name);
	}

	/**
	 * Query a feature as identified by the given featureName as to whether the given featureValue is a known value of that
	 * feature. In other words, this will return true exactly if the given feature is a byte feature and
	 * getFeatureValueAsByte(featureName, featureValue) will not throw an exception or if the given feature is a short feature and
	 * getFeatureValueAsShort(featureName, featureValue) will not throw an exception.
	 * 
	 * @param featureName
	 *            featureName
	 * @param featureValue
	 *            featureValue
	 * @return hasFeatureValue(getFeatureIndex(featureName), featureValue)
	 */
	public boolean hasFeatureValue(String featureName, String featureValue) {
		return hasFeatureValue(getFeatureIndex(featureName), featureValue);
	}

	/**
	 * Query a feature as identified by the given featureIndex as to whether the given featureValue is a known value of that
	 * feature. In other words, this will return true exactly if the given feature is a byte feature and
	 * getFeatureValueAsByte(featureIndex, featureValue) will not throw an exception or if the given feature is a short feature
	 * and getFeatureValueAsShort(featureIndex, featureValue) will not throw an exception.
	 * 
	 * @param featureIndex
	 *            featureIndex
	 * @param featureValue
	 *            featureValue
	 * @return false if featureIndex &lt; 0, byteFeatureValues[featureIndex].contains(featureValue) if featureIndex &lt;
	 *         numByteFeatures, shortFeatureValues[featureIndex - numByteFeatures].contains(featureValue) if featureIndex &lt;
	 *         numByteFeatures + numShortFeatures, false otherwise
	 */
	public boolean hasFeatureValue(int featureIndex, String featureValue) {
		if (featureIndex < 0) {
			return false;
		}
		if (featureIndex < numByteFeatures) {
			return byteFeatureValues[featureIndex].contains(featureValue);
		}
		if (featureIndex < numByteFeatures + numShortFeatures) {
			return shortFeatureValues[featureIndex - numByteFeatures].contains(featureValue);
		}
		return false;
	}

	/**
	 * Determine whether the feature with the given name is a byte feature.
	 * 
	 * @param featureName
	 *            featureName
	 * @return true if the feature is a byte feature, false if the feature is not known or is not a byte feature
	 */
	public boolean isByteFeature(String featureName) {
		try {
			int index = getFeatureIndex(featureName);
			return isByteFeature(index);
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * Determine whether the feature with the given index number is a byte feature.
	 * 
	 * @param index
	 *            index
	 * @return true if the feature is a byte feature, false if the feature is not a byte feature or is invalid
	 */
	public boolean isByteFeature(int index) {
		return 0 <= index && index < numByteFeatures;
	}

	/**
	 * Determine whether the feature with the given name is a short feature.
	 * 
	 * @param featureName
	 *            featureName
	 * @return true if the feature is a short feature, false if the feature is not known or is not a short feature
	 */
	public boolean isShortFeature(String featureName) {
		try {
			int index = getFeatureIndex(featureName);
			return isShortFeature(index);
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * Determine whether the feature with the given index number is a short feature.
	 * 
	 * @param index
	 *            index
	 * @return true if the feature is a short feature, false if the feature is not a short feature or is invalid
	 */
	public boolean isShortFeature(int index) {
		index -= numByteFeatures;
		return 0 <= index && index < numShortFeatures;
	}

	/**
	 * Determine whether the feature with the given name is a continuous feature.
	 * 
	 * @param featureName
	 *            featureName
	 * @return true if the feature is a continuous feature, false if the feature is not known or is not a continuous feature
	 */
	public boolean isContinuousFeature(String featureName) {
		try {
			int index = getFeatureIndex(featureName);
			return isContinuousFeature(index);
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * Determine whether the feature with the given index number is a continuous feature.
	 * 
	 * @param index
	 *            index
	 * @return true if the feature is a continuous feature, false if the feature is not a continuous feature or is invalid
	 */
	public boolean isContinuousFeature(int index) {
		index -= numByteFeatures;
		index -= numShortFeatures;
		return 0 <= index && index < numContinuousFeatures;
	}

	/**
	 * true, if given feature index contains similarity matrix
	 * 
	 * @param featureIndex
	 *            featureIndex
	 * @return true if this.similarityMatrices different from null and this.similarityMatrices[featureIndex] different from null,
	 *         false otherwise
	 */
	public boolean hasSimilarityMatrix(int featureIndex) {

		if (featureIndex >= this.getNumberOfByteFeatures()) {
			return false;
		}
		if (this.similarityMatrices != null && this.similarityMatrices[featureIndex] != null) {
			return true;
		}
		return false;
	}

	/**
	 * true, if given feature name contains similarity matrix
	 * 
	 * @param featureName
	 *            featureName
	 * @return hasSimilarityMatrix(this.getFeatureIndex(featureName))
	 */
	public boolean hasSimilarityMatrix(String featureName) {
		return hasSimilarityMatrix(this.getFeatureIndex(featureName));
	}

	/**
	 * To get a similarity between two feature values
	 * 
	 * @param featureIndex
	 *            featureIndex
	 * @param i
	 *            i
	 * @param j
	 *            j
	 * @return this.similarityMatrices[featureIndex][i][j]
	 */
	public float getSimilarity(int featureIndex, byte i, byte j) {
		if (!hasSimilarityMatrix(featureIndex)) {
			throw new RuntimeException("the given feature index  ");
		}
		return this.similarityMatrices[featureIndex][i][j];
	}

	/**
	 * Translate between a feature name and a feature index.
	 * 
	 * @param featureName
	 *            a valid feature name
	 * @return a feature index, as could be used to access a feature value in a FeatureVector.
	 * @throws IllegalArgumentException
	 *             if the feature name is unknown.
	 */
	public int getFeatureIndex(String featureName) {
		return featureNames.get(featureName);
	}

	/**
	 * Translate between an array of feature names and an array of feature indexes.
	 * 
	 * @param featureName
	 *            an array of valid feature names
	 * @return an array of feature indexes, as could be used to access a feature value in a FeatureVector.
	 * @throws IllegalArgumentException
	 *             if one of the feature names is unknown.
	 */
	public int[] getFeatureIndexArray(String[] featureName) {
		int[] ret = new int[featureName.length];
		for (int i = 0; i < featureName.length; i++) {
			ret[i] = getFeatureIndex(featureName[i]);
		}
		return (ret);
	}

	/**
	 * Get the number of possible values for the feature with the given index number. This method must only be called for
	 * byte-valued or short-valued features.
	 * 
	 * @param featureIndex
	 *            the index number of the feature.
	 * @return for byte-valued and short-valued features, return the number of values.
	 * @throws IndexOutOfBoundsException
	 *             if featureIndex &lt; 0 or featureIndex &ge; getNumberOfByteFeatures() + getNumberOfShortFeatures().
	 */
	public int getNumberOfValues(int featureIndex) {
		if (featureIndex < numByteFeatures)
			return byteFeatureValues[featureIndex].getNumberOfValues();
		featureIndex -= numByteFeatures;
		if (featureIndex < numShortFeatures)
			return shortFeatureValues[featureIndex].getNumberOfValues();
		throw new IndexOutOfBoundsException("Feature no. " + featureIndex + " is not a byte-valued or short-valued feature");
	}

	/**
	 * Get the list of possible String values for the feature with the given index number. This method must only be called for
	 * byte-valued or short-valued features. The position in the String array corresponds to the byte or short value of the
	 * feature obtained from a FeatureVector.
	 * 
	 * @param featureIndex
	 *            the index number of the feature.
	 * @return for byte-valued and short-valued features, return the array of String values.
	 * @throws IndexOutOfBoundsException
	 *             if featureIndex &lt; 0 or featureIndex &ge; getNumberOfByteFeatures() + getNumberOfShortFeatures().
	 */
	public String[] getPossibleValues(int featureIndex) {
		if (featureIndex < numByteFeatures)
			return byteFeatureValues[featureIndex].getStringValues();
		featureIndex -= numByteFeatures;
		if (featureIndex < numShortFeatures)
			return shortFeatureValues[featureIndex].getStringValues();
		throw new IndexOutOfBoundsException("Feature no. " + featureIndex + " is not a byte-valued or short-valued feature");
	}

	/**
	 * For the feature with the given index number, translate its byte or short value to its String value. This method must only
	 * be called for byte-valued or short-valued features.
	 * 
	 * @param featureIndex
	 *            the index number of the feature.
	 * @param value
	 *            the feature value. This must be in the range of acceptable values for the given feature.
	 * @return for byte-valued and short-valued features, return the String representation of the feature value.
	 * @throws IndexOutOfBoundsException
	 *             if featureIndex &lt; 0 or featureIndex &ge; getNumberOfByteFeatures() + getNumberOfShortFeatures()
	 * @throws IndexOutOfBoundsException
	 *             if value is not a legal value for this feature
	 * 
	 * 
	 */
	public String getFeatureValueAsString(int featureIndex, int value) {
		if (featureIndex < numByteFeatures)
			return byteFeatureValues[featureIndex].get((byte) value);
		featureIndex -= numByteFeatures;
		if (featureIndex < numShortFeatures)
			return shortFeatureValues[featureIndex].get((short) value);
		throw new IndexOutOfBoundsException("Feature no. " + featureIndex + " is not a byte-valued or short-valued feature");
	}

	/**
	 * Simple access to string-based features.
	 * 
	 * @param featureName
	 *            featureName
	 * @param fv
	 *            fv
	 * @return getFeatureValueAsString(i, fv.getFeatureAsInt(i))F
	 */
	public String getFeatureValueAsString(String featureName, FeatureVector fv) {
		int i = getFeatureIndex(featureName);
		return getFeatureValueAsString(i, fv.getFeatureAsInt(i));
	}

	/**
	 * For the feature with the given name, translate its String value to its byte value. This method must only be called for
	 * byte-valued features.
	 * 
	 * @param featureName
	 *            the name of the feature.
	 * @param value
	 *            the feature value. This must be among the acceptable values for the given feature.
	 * @return for byte-valued features, return the byte representation of the feature value.
	 * @throws IllegalArgumentException
	 *             if featureName is not a valid feature name, or if featureName is not a byte-valued feature.
	 * @throws IllegalArgumentException
	 *             if value is not a legal value for this feature
	 */
	public byte getFeatureValueAsByte(String featureName, String value) {
		int featureIndex = getFeatureIndex(featureName);
		return getFeatureValueAsByte(featureIndex, value);
	}

	/**
	 * For the feature with the given index number, translate its String value to its byte value. This method must only be called
	 * for byte-valued features.
	 * 
	 * @param featureIndex
	 *            the name of the feature.
	 * @param value
	 *            the feature value. This must be among the acceptable values for the given feature.
	 * @return for byte-valued features, return the byte representation of the feature value.
	 * @throws IllegalArgumentException
	 *             if featureName is not a valid feature name, or if featureName is not a byte-valued feature.
	 * @throws IllegalArgumentException
	 *             if value is not a legal value for this feature
	 */
	public byte getFeatureValueAsByte(int featureIndex, String value) {
		if (featureIndex >= numByteFeatures)
			throw new IndexOutOfBoundsException("Feature no. " + featureIndex + " is not a byte-valued feature");
		try {
			return byteFeatureValues[featureIndex].get(value);
		} catch (IllegalArgumentException iae) {
			StringBuilder message = new StringBuilder("Illegal value '" + value + "' for feature " + getFeatureName(featureIndex)
					+ "; Legal values are:\n");
			for (String v : getPossibleValues(featureIndex)) {
				message.append(" " + v);
			}
			throw new IllegalArgumentException(message.toString());
		}

	}

	/**
	 * For the feature with the given name, translate its String value to its short value. This method must only be called for
	 * short-valued features.
	 * 
	 * @param featureName
	 *            the name of the feature.
	 * @param value
	 *            the feature value. This must be among the acceptable values for the given feature.
	 * @return for short-valued features, return the short representation of the feature value.
	 * @throws IllegalArgumentException
	 *             if featureName is not a valid feature name, or if featureName is not a short-valued feature.
	 * @throws IllegalArgumentException
	 *             if value is not a legal value for this feature
	 */
	public short getFeatureValueAsShort(String featureName, String value) {
		int featureIndex = getFeatureIndex(featureName);
		featureIndex -= numByteFeatures;
		if (featureIndex < numShortFeatures)
			return shortFeatureValues[featureIndex].get(value);
		throw new IndexOutOfBoundsException("Feature '" + featureName + "' is not a short-valued feature");
	}

	/**
	 * For the feature with the given name, translate its String value to its short value. This method must only be called for
	 * short-valued features.
	 * 
	 * @param featureIndex
	 *            the name of the feature.
	 * @param value
	 *            the feature value. This must be among the acceptable values for the given feature.
	 * @return for short-valued features, return the short representation of the feature value.
	 * @throws IllegalArgumentException
	 *             if featureName is not a valid feature name, or if featureName is not a short-valued feature.
	 * @throws IllegalArgumentException
	 *             if value is not a legal value for this feature
	 */
	public short getFeatureValueAsShort(int featureIndex, String value) {
		featureIndex -= numByteFeatures;
		if (featureIndex < numShortFeatures)
			return shortFeatureValues[featureIndex].get(value);
		throw new IndexOutOfBoundsException("Feature no. " + featureIndex + " is not a short-valued feature");
	}

	/**
	 * Determine whether two feature definitions are equal, with respect to number, names, and possible values of the three kinds
	 * of features (byte-valued, short-valued, continuous). This method does not compare any weights.
	 * 
	 * @param other
	 *            the feature definition to compare to
	 * @return true if all features and values are identical, false otherwise
	 */
	public boolean featureEquals(FeatureDefinition other) {
		if (numByteFeatures != other.numByteFeatures || numShortFeatures != other.numShortFeatures
				|| numContinuousFeatures != other.numContinuousFeatures)
			return false;
		// Compare the feature names and values for byte and short features:
		for (int i = 0; i < numByteFeatures + numShortFeatures + numContinuousFeatures; i++) {
			if (!getFeatureName(i).equals(other.getFeatureName(i)))
				return false;
		}
		// Compare the values for byte and short features:
		for (int i = 0; i < numByteFeatures + numShortFeatures; i++) {
			if (getNumberOfValues(i) != other.getNumberOfValues(i))
				return false;
			for (int v = 0, n = getNumberOfValues(i); v < n; v++) {
				if (!getFeatureValueAsString(i, v).equals(other.getFeatureValueAsString(i, v)))
					return false;
			}
		}
		return true;
	}

	/**
	 * An extension of the previous method.
	 * 
	 * @param other
	 *            other
	 * @return number of byte features, or number of short features, or number of continuous features, or feature name
	 */
	public String featureEqualsAnalyse(FeatureDefinition other) {
		if (numByteFeatures != other.numByteFeatures) {
			return ("The number of BYTE features differs: " + numByteFeatures + " versus " + other.numByteFeatures);
		}
		if (numShortFeatures != other.numShortFeatures) {
			return ("The number of SHORT features differs: " + numShortFeatures + " versus " + other.numShortFeatures);
		}
		if (numContinuousFeatures != other.numContinuousFeatures) {
			return ("The number of CONTINUOUS features differs: " + numContinuousFeatures + " versus " + other.numContinuousFeatures);
		}
		// Compare the feature names and values for byte and short features:
		for (int i = 0; i < numByteFeatures + numShortFeatures + numContinuousFeatures; i++) {
			if (!getFeatureName(i).equals(other.getFeatureName(i))) {
				return ("The feature name differs at position [" + i + "]: " + getFeatureName(i) + " versus " + other
						.getFeatureName(i));
			}
		}
		// Compare the values for byte and short features:
		for (int i = 0; i < numByteFeatures + numShortFeatures; i++) {
			if (getNumberOfValues(i) != other.getNumberOfValues(i)) {
				return ("The number of values differs at position [" + i + "]: " + getNumberOfValues(i) + " versus " + other
						.getNumberOfValues(i));
			}
			for (int v = 0, n = getNumberOfValues(i); v < n; v++) {
				if (!getFeatureValueAsString(i, v).equals(other.getFeatureValueAsString(i, v))) {
					return ("The feature value differs at position [" + i + "] for feature value [" + v + "]: "
							+ getFeatureValueAsString(i, v) + " versus " + other.getFeatureValueAsString(i, v));
				}
			}
		}
		return "";
	}

	/**
	 * Determine whether two feature definitions are equal, regarding both the actual feature definitions and the weights. The
	 * comparison of weights will succeed if both have no weights or if both have exactly the same weights
	 * 
	 * @param obj
	 *            the feature definition to compare to
	 * @return true if all features, values and weights are identical, false otherwise
	 * @see #featureEquals(FeatureDefinition)
	 */
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof FeatureDefinition))
			return false;
		FeatureDefinition other = (FeatureDefinition) obj;
		if (featureWeights == null) {
			if (other.featureWeights != null)
				return false;
			// Both are null
		} else { // featureWeights != null
			if (other.featureWeights == null)
				return false;
			// Both != null
			if (featureWeights.length != other.featureWeights.length)
				return false;
			for (int i = 0; i < featureWeights.length; i++) {
				if (featureWeights[i] != other.featureWeights[i])
					return false;
			}
			assert floatWeightFuncts != null;
			assert other.floatWeightFuncts != null;
			if (floatWeightFuncts.length != other.floatWeightFuncts.length)
				return false;
			for (int i = 0; i < floatWeightFuncts.length; i++) {
				if (floatWeightFuncts[i] == null) {
					if (other.floatWeightFuncts[i] != null)
						return false;
					// Both are null
				} else { // != null
					if (other.floatWeightFuncts[i] == null)
						return false;
					// Both != null
					if (!floatWeightFuncts[i].equals(other.floatWeightFuncts[i]))
						return false;
				}
			}
		}
		// OK, weights are equal
		return featureEquals(other);
	}

	/**
	 * Determine whether this FeatureDefinition is a superset of, or equal to, another FeatureDefinition.
	 * <p>
	 * Specifically,
	 * <ol>
	 * <li>every byte-valued feature in <b>other</b> must be in <b>this</b>, likewise for short-valued and continuous-valued
	 * features;</li>
	 * <li>for byte-valued and short-valued features, the possible feature values must be the same in <b>this</b> and
	 * <b>other</b>.</li>
	 * </ol>
	 * 
	 * @param other
	 *            FeatureDefinition
	 * @return <i>true</i> if
	 *         <ol>
	 *         <li>all features in <b>other</b> are also in <b>this</b>, and every feature in <b>other</b> is of the same type in
	 *         <b>this</b>; and</li> <li>every feature in <b>other</b> has the same possible values as the feature in <b>this</b>
	 *         </li>
	 *         </ol>
	 *         <i>false</i> otherwise
	 */
	public boolean contains(FeatureDefinition other) {
		List<String> thisByteFeatures = Arrays.asList(this.getByteFeatureNameArray());
		List<String> otherByteFeatures = Arrays.asList(other.getByteFeatureNameArray());
		if (!thisByteFeatures.containsAll(otherByteFeatures)) {
			return false;
		}
		for (String commonByteFeature : otherByteFeatures) {
			String[] thisByteFeaturePossibleValues = this.getPossibleValues(this.getFeatureIndex(commonByteFeature));
			String[] otherByteFeaturePossibleValues = other.getPossibleValues(other.getFeatureIndex(commonByteFeature));
			if (!Arrays.equals(thisByteFeaturePossibleValues, otherByteFeaturePossibleValues)) {
				return false;
			}
		}
		List<String> thisShortFeatures = Arrays.asList(this.getShortFeatureNameArray());
		List<String> otherShortFeatures = Arrays.asList(other.getShortFeatureNameArray());
		if (!thisShortFeatures.containsAll(otherShortFeatures)) {
			return false;
		}
		for (String commonShortFeature : otherShortFeatures) {
			String[] thisShortFeaturePossibleValues = this.getPossibleValues(this.getFeatureIndex(commonShortFeature));
			String[] otherShortFeaturePossibleValues = other.getPossibleValues(other.getFeatureIndex(commonShortFeature));
			if (!Arrays.equals(thisShortFeaturePossibleValues, otherShortFeaturePossibleValues)) {
				return false;
			}
		}
		List<String> thisContinuousFeatures = Arrays.asList(this.getContinuousFeatureNameArray());
		List<String> otherContinuousFeatures = Arrays.asList(other.getContinuousFeatureNameArray());
		if (!thisContinuousFeatures.containsAll(otherContinuousFeatures)) {
			return false;
		}
		return true;
	}

	/**
	 * Create a new FeatureDefinition that contains a subset of the features in this.
	 * 
	 * @param featureNamesToDrop
	 *            array of Strings containing the names of the features to drop from the new FeatureDefinition
	 * @return new FeatureDefinition
	 */
	public FeatureDefinition subset(String[] featureNamesToDrop) {
		// construct a list of indices for the features to be dropped:
		List<Integer> featureIndicesToDrop = new ArrayList<Integer>();
		for (String featureName : featureNamesToDrop) {
			int featureIndex;
			try {
				featureIndex = getFeatureIndex(featureName);
				featureIndicesToDrop.add(featureIndex);
			} catch (IllegalArgumentException e) {
				System.err.println("WARNING: feature " + featureName + " not found in FeatureDefinition; ignoring.");
			}
		}

		// create a new FeatureDefinition by way of a byte array:
		FeatureDefinition subDefinition = null;
		try {
			ByteArrayOutputStream toMemory = new ByteArrayOutputStream();
			DataOutput output = new DataOutputStream(toMemory);
			writeBinaryTo(output, featureIndicesToDrop);

			byte[] memory = toMemory.toByteArray();

			ByteArrayInputStream fromMemory = new ByteArrayInputStream(memory);
			DataInput input = new DataInputStream(fromMemory);

			subDefinition = new FeatureDefinition(input);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// make sure that subDefinition really is a subset of this
		assert this.contains(subDefinition);

		return subDefinition;
	}

	/**
	 * Create a feature vector consistent with this feature definition by reading the data from a String representation. In that
	 * String, the String values for each feature must be separated by white space. For example, this format is created by
	 * toFeatureString(FeatureVector).
	 * 
	 * @param unitIndex
	 *            an index number to assign to the feature vector
	 * @param featureString
	 *            the string representation of a feature vector.
	 * @return the feature vector created from the String.
	 * @throws IllegalArgumentException
	 *             if the feature values listed are not consistent with the feature definition.
	 * @see #toFeatureString(FeatureVector)
	 */
	public FeatureVector toFeatureVector(int unitIndex, String featureString) {
		String[] featureValues = featureString.split("\\s+");
		if (featureValues.length != numByteFeatures + numShortFeatures + numContinuousFeatures)
			throw new IllegalArgumentException("Expected " + (numByteFeatures + numShortFeatures + numContinuousFeatures)
					+ " features, got " + featureValues.length);
		byte[] bytes = new byte[numByteFeatures];
		short[] shorts = new short[numShortFeatures];
		float[] floats = new float[numContinuousFeatures];
		for (int i = 0; i < numByteFeatures; i++) {
			bytes[i] = Byte.parseByte(featureValues[i]);
		}
		for (int i = 0; i < numShortFeatures; i++) {
			shorts[i] = Short.parseShort(featureValues[numByteFeatures + i]);
		}
		for (int i = 0; i < numContinuousFeatures; i++) {
			floats[i] = Float.parseFloat(featureValues[numByteFeatures + numShortFeatures + i]);
		}
		return new FeatureVector(bytes, shorts, floats, unitIndex);
	}

	public FeatureVector toFeatureVector(int unitIndex, byte[] bytes, short[] shorts, float[] floats) {
		if (!((numByteFeatures == 0 && bytes == null || numByteFeatures == bytes.length)
				&& (numShortFeatures == 0 && shorts == null || numShortFeatures == shorts.length) && (numContinuousFeatures == 0
				&& floats == null || numContinuousFeatures == floats.length))) {
			throw new IllegalArgumentException("Expected " + numByteFeatures + " bytes (got "
					+ (bytes == null ? "0" : bytes.length) + "), " + numShortFeatures + " shorts (got "
					+ (shorts == null ? "0" : shorts.length) + "), " + numContinuousFeatures + " floats (got "
					+ (floats == null ? "0" : floats.length) + ")");
		}
		return new FeatureVector(bytes, shorts, floats, unitIndex);
	}

	/**
	 * Create a feature vector consistent with this feature definition by reading the data from the given input.
	 * 
	 * @param input
	 *            a DataInputStream or RandomAccessFile to read the feature values from.
	 * @param currentUnitIndex
	 *            currentUnitIndex
	 * @throws IOException
	 *             IOException
	 * @return a FeatureVector.
	 */
	public FeatureVector readFeatureVector(int currentUnitIndex, DataInput input) throws IOException {
		byte[] bytes = new byte[numByteFeatures];
		input.readFully(bytes);
		short[] shorts = new short[numShortFeatures];
		for (int i = 0; i < shorts.length; i++) {
			shorts[i] = input.readShort();
		}
		float[] floats = new float[numContinuousFeatures];
		for (int i = 0; i < floats.length; i++) {
			floats[i] = input.readFloat();
		}
		return new FeatureVector(bytes, shorts, floats, currentUnitIndex);
	}

	/**
	 * Create a feature vector consistent with this feature definition by reading the data from the byte buffer.
	 * 
	 * @param currentUnitIndex
	 *            currentUnitIndex
	 * @param bb
	 *            a byte buffer to read the feature values from.
	 * @throws IOException
	 *             IOException
	 * @return a FeatureVector.
	 */
	public FeatureVector readFeatureVector(int currentUnitIndex, ByteBuffer bb) throws IOException {
		byte[] bytes = new byte[numByteFeatures];
		bb.get(bytes);
		short[] shorts = new short[numShortFeatures];
		for (int i = 0; i < shorts.length; i++) {
			shorts[i] = bb.getShort();
		}
		float[] floats = new float[numContinuousFeatures];
		for (int i = 0; i < floats.length; i++) {
			floats[i] = bb.getFloat();
		}
		return new FeatureVector(bytes, shorts, floats, currentUnitIndex);
	}

	/**
	 * Create a feature vector that marks a start or end of a unit. All feature values are set to the neutral value "0", except
	 * for the EDGEFEATURE, which is set to start if start == true, to end otherwise.
	 * 
	 * @param unitIndex
	 *            index of the unit
	 * @param start
	 *            true creates a start vector, false creates an end vector.
	 * @return a feature vector representing an edge.
	 */
	public FeatureVector createEdgeFeatureVector(int unitIndex, boolean start) {
		int edgeFeature = getFeatureIndex(EDGEFEATURE);
		assert edgeFeature < numByteFeatures; // we can assume this is byte-valued
		byte edge;
		if (start)
			edge = getFeatureValueAsByte(edgeFeature, EDGEFEATURE_START);
		else
			edge = getFeatureValueAsByte(edgeFeature, EDGEFEATURE_END);
		byte[] bytes = new byte[numByteFeatures];
		short[] shorts = new short[numShortFeatures];
		float[] floats = new float[numContinuousFeatures];
		for (int i = 0; i < numByteFeatures; i++) {
			bytes[i] = getFeatureValueAsByte(i, NULLVALUE);
		}
		for (int i = 0; i < numShortFeatures; i++) {
			shorts[i] = getFeatureValueAsShort(numByteFeatures + i, NULLVALUE);
		}
		for (int i = 0; i < numContinuousFeatures; i++) {
			floats[i] = 0;
		}
		bytes[edgeFeature] = edge;
		return new FeatureVector(bytes, shorts, floats, unitIndex);
	}

	/**
	 * Convert a feature vector into a String representation.
	 * 
	 * @param fv
	 *            a feature vector which must be consistent with this feature definition.
	 * @return a String containing the String values of all features, separated by white space.
	 * @throws IllegalArgumentException
	 *             if the feature vector is not consistent with this feature definition
	 * @throws IndexOutOfBoundsException
	 *             if any value of the feature vector is not consistent with this feature definition
	 */
	public String toFeatureString(FeatureVector fv) {
		if (numByteFeatures != fv.getNumberOfByteFeatures() || numShortFeatures != fv.getNumberOfShortFeatures()
				|| numContinuousFeatures != fv.getNumberOfContinuousFeatures())
			throw new IllegalArgumentException("Feature vector '" + fv + "' is inconsistent with feature definition");
		StringBuilder buf = new StringBuilder();
		for (int i = 0; i < numByteFeatures; i++) {
			if (buf.length() > 0)
				buf.append(" ");
			buf.append(getFeatureValueAsString(i, fv.getByteFeature(i)));
		}
		for (int i = numByteFeatures; i < numByteFeatures + numShortFeatures; i++) {
			if (buf.length() > 0)
				buf.append(" ");
			buf.append(getFeatureValueAsString(i, fv.getShortFeature(i)));
		}
		for (int i = numByteFeatures + numShortFeatures; i < numByteFeatures + numShortFeatures + numContinuousFeatures; i++) {
			if (buf.length() > 0)
				buf.append(" ");
			buf.append(fv.getContinuousFeature(i));
		}
		return buf.toString();
	}

	/**
	 * Export this feature definition in the text format which can also be read by this class.
	 * 
	 * @param out
	 *            the destination of the data
	 * @param writeWeights
	 *            whether to write weights before every line
	 */
	public void writeTo(PrintWriter out, boolean writeWeights) {
		out.println("ByteValuedFeatureProcessors");
		for (int i = 0; i < numByteFeatures; i++) {
			if (writeWeights) {
				out.print(featureWeights[i] + " | ");
			}
			out.print(getFeatureName(i));
			for (int v = 0, vmax = getNumberOfValues(i); v < vmax; v++) {
				out.print(" ");
				String val = getFeatureValueAsString(i, v);
				out.print(val);
			}
			out.println();
		}
		out.println("ShortValuedFeatureProcessors");
		for (int i = 0; i < numShortFeatures; i++) {
			if (writeWeights) {
				out.print(featureWeights[numByteFeatures + i] + " | ");
			}
			out.print(getFeatureName(numByteFeatures + i));
			for (int v = 0, vmax = getNumberOfValues(numByteFeatures + i); v < vmax; v++) {
				out.print(" ");
				String val = getFeatureValueAsString(numByteFeatures + i, v);
				out.print(val);
			}
			out.println();
		}
		out.println("ContinuousFeatureProcessors");
		for (int i = 0; i < numContinuousFeatures; i++) {
			if (writeWeights) {
				out.print(featureWeights[numByteFeatures + numShortFeatures + i]);
				out.print(" ");
				out.print(floatWeightFuncts[i]);
				out.print(" | ");
			}

			out.print(getFeatureName(numByteFeatures + numShortFeatures + i));
			out.println();
		}

	}

	/**
	 * Export this feature definition in the "all.desc" format which can be read by wagon.
	 * 
	 * @param out
	 *            the destination of the data
	 */
	public void generateAllDotDescForWagon(PrintWriter out) {
		generateAllDotDescForWagon(out, null);
	}

	/**
	 * Export this feature definition in the "all.desc" format which can be read by wagon.
	 * 
	 * @param out
	 *            the destination of the data
	 * @param featuresToIgnore
	 *            a set of Strings containing the names of features that wagon should ignore. Can be null.
	 */
	public void generateAllDotDescForWagon(PrintWriter out, Set<String> featuresToIgnore) {
		out.println("(");
		out.println("(occurid cluster)");
		for (int i = 0, n = getNumberOfFeatures(); i < n; i++) {
			out.print("( ");
			String featureName = getFeatureName(i);
			out.print(featureName);
			if (featuresToIgnore != null && featuresToIgnore.contains(featureName)) {
				out.print(" ignore");
			}
			if (i < numByteFeatures + numShortFeatures) { // list values
				for (int v = 0, vmax = getNumberOfValues(i); v < vmax; v++) {
					out.print("  ");
					// Print values surrounded by double quotes, and make sure any
					// double quotes in the value are preceded by a backslash --
					// otherwise, we get problems e.g. for sentence_punc
					String val = getFeatureValueAsString(i, v);
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
			} else { // float feature
				out.println(" float )");
			}

		}
		out.println(")");
	}

	/**
	 * Print this feature definition plus weights to a .txt file
	 * 
	 * @param out
	 *            the destination of the data
	 */
	public void generateFeatureWeightsFile(PrintWriter out) {
		out.println("# This file lists the features and their weights to be used for\n" + "# creating the MARY features file.\n"
				+ "# The same file can also be used to override weights in a run-time system.\n"
				+ "# Three sections are distinguished: Byte-valued, Short-valued, and\n" + "# Continuous features.\n" + "#\n"
				+ "# Lines starting with '#' are ignored; they can be used for comments\n"
				+ "# anywhere in the file. Empty lines are also ignored.\n" + "# Entries must have the following form:\n"
				+ "# \n" + "# <weight definition> | <feature definition>\n" + "# \n"
				+ "# For byte and short features, <weight definition> is simply the \n"
				+ "# (float) number representing the weight.\n" + "# For continuous features, <weight definition> is the\n"
				+ "# (float) number representing the weight, followed by an optional\n"
				+ "# weighting function including arguments.\n" + "#\n"
				+ "# The <feature definition> is the feature name, which in the case of\n"
				+ "# byte and short features is followed by the full list of feature values.\n" + "#\n"
				+ "# Note that the feature definitions must be identical between this file\n"
				+ "# and all unit feature files for individual database utterances.\n"
				+ "# THIS FILE WAS GENERATED AUTOMATICALLY");
		out.println();
		out.println("ByteValuedFeatureProcessors");
		List<String> getValuesOf10 = new ArrayList<String>();
		getValuesOf10.add("phone");
		getValuesOf10.add("ph_vc");
		getValuesOf10.add("prev_phone");
		getValuesOf10.add("next_phone");
		getValuesOf10.add("stressed");
		getValuesOf10.add("syl_break");
		getValuesOf10.add("prev_syl_break");
		getValuesOf10.add("next_is_pause");
		getValuesOf10.add("prev_is_pause");
		List<String> getValuesOf5 = new ArrayList<String>();
		getValuesOf5.add("cplace");
		getValuesOf5.add("ctype");
		getValuesOf5.add("cvox");
		getValuesOf5.add("vfront");
		getValuesOf5.add("vheight");
		getValuesOf5.add("vlng");
		getValuesOf5.add("vrnd");
		getValuesOf5.add("vc");
		for (int i = 0; i < numByteFeatures; i++) {
			String featureName = getFeatureName(i);
			if (getValuesOf10.contains(featureName)) {
				out.print("10 | " + featureName);
			} else {
				boolean found = false;
				for (String match : getValuesOf5) {
					if (featureName.matches(".*" + match)) {
						out.print("5 | " + featureName);
						found = true;
						break;
					}
				}
				if (!found) {
					out.print("0 | " + featureName);
				}
			}
			for (int v = 0, vmax = getNumberOfValues(i); v < vmax; v++) {
				String val = getFeatureValueAsString(i, v);
				out.print(" " + val);
			}
			out.print("\n");
		}
		out.println("ShortValuedFeatureProcessors");
		for (int i = 0; i < numShortFeatures; i++) {
			int n = i + numByteFeatures;
			String featureName = getFeatureName(n);
			out.print("0 | " + featureName);
			for (int v = 0, vmax = getNumberOfValues(n); v < vmax; v++) {
				String val = getFeatureValueAsString(n, v);
				out.print(" " + val);
			}
			out.print("\n");
		}
		out.println("ContinuousFeatureProcessors");
		for (int i = 0; i < numContinuousFeatures; i++) {
			String featureName = getFeatureName(i + numByteFeatures + numShortFeatures);
			int featureValue;
			switch (featureName) {
			case "unit_duration":
				featureValue = 1000;
				break;
			case "unit_logf0":
				featureValue = 100;
				break;
			default:
				featureValue = 0;
				break;
			}
			out.printf("%d linear | %s\n", featureValue, featureName);
		}
		out.flush();
		out.close();
	}

	/**
	 * Compares two feature vectors in terms of how many discrete features they have in common. WARNING: this assumes that the
	 * feature vectors are issued from the same FeatureDefinition; only the number of features is checked for compatibility.
	 * 
	 * @param v1
	 *            A feature vector.
	 * @param v2
	 *            Another feature vector to compare v1 with.
	 * @return The number of common features.
	 */
	public static int diff(FeatureVector v1, FeatureVector v2) {

		int ret = 0;

		/* Byte valued features */
		if (v1.byteValuedDiscreteFeatures.length < v2.byteValuedDiscreteFeatures.length) {
			throw new RuntimeException("v1 and v2 don't have the same number of byte-valued features: ["
					+ v1.byteValuedDiscreteFeatures.length + "] versus [" + v2.byteValuedDiscreteFeatures.length + "].");
		}
		for (int i = 0; i < v1.byteValuedDiscreteFeatures.length; i++) {
			if (v1.byteValuedDiscreteFeatures[i] == v2.byteValuedDiscreteFeatures[i])
				ret++;
		}

		/* Short valued features */
		if (v1.shortValuedDiscreteFeatures.length < v2.shortValuedDiscreteFeatures.length) {
			throw new RuntimeException("v1 and v2 don't have the same number of short-valued features: ["
					+ v1.shortValuedDiscreteFeatures.length + "] versus [" + v2.shortValuedDiscreteFeatures.length + "].");
		}
		for (int i = 0; i < v1.shortValuedDiscreteFeatures.length; i++) {
			if (v1.shortValuedDiscreteFeatures[i] == v2.shortValuedDiscreteFeatures[i])
				ret++;
		}

		/* TODO: would checking float-valued features make sense ? (Code below.) */
		/* float valued features */
		/*
		 * if ( v1.continuousFeatures.length < v2.continuousFeatures.length ) { throw new RuntimeException(
		 * "v1 and v2 don't have the same number of continuous features: [" + v1.continuousFeatures.length + "] versus [" +
		 * v2.continuousFeatures.length + "]." ); } float epsilon = 1.0e-6f; float d = 0.0f; for ( int i = 0; i <
		 * v1.continuousFeatures.length; i++ ) { d = ( v1.continuousFeatures[i] > v2.continuousFeatures[i] ?
		 * (v1.continuousFeatures[i] - v2.continuousFeatures[i]) : (v2.continuousFeatures[i] - v1.continuousFeatures[i]) ); // =>
		 * this avoids Math.abs() if ( d < epsilon ) ret++; }
		 */

		return (ret);
	}

}
