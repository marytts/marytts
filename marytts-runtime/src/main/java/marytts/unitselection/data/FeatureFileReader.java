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
package marytts.unitselection.data;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import marytts.exceptions.MaryConfigurationException;
import marytts.features.FeatureDefinition;
import marytts.features.FeatureVector;
import marytts.util.data.MaryHeader;

public class FeatureFileReader {
	protected MaryHeader hdr;
	protected FeatureDefinition featureDefinition;
	protected FeatureVector[] featureVectors;

	/**
	 * Get a feature file reader representing the given feature file.
	 * 
	 * @param fileName
	 *            the filename of a valid feature file.
	 * @return a feature file object representing the given file.
	 * @throws IOException
	 *             if there was a problem reading the file
	 * @throws MaryConfigurationException
	 *             if the file is not a valid feature file.
	 */
	public static FeatureFileReader getFeatureFileReader(String fileName) throws IOException, MaryConfigurationException {
		int fileType = MaryHeader.peekFileType(fileName);
		if (fileType == MaryHeader.UNITFEATS)
			return new FeatureFileReader(fileName);
		else if (fileType == MaryHeader.HALFPHONE_UNITFEATS)
			return new HalfPhoneFeatureFileReader(fileName);
		throw new MaryConfigurationException("File " + fileName + ": Type " + fileType + " is not a known unit feature file type");
	}

	/**
	 * Empty constructor; need to call load() separately when using this.
	 * 
	 * @see #load(String fileName)
	 */
	public FeatureFileReader() {
	}

	public FeatureFileReader(String fileName) throws IOException, MaryConfigurationException {
		load(fileName);
	}

	public void load(String fileName) throws IOException, MaryConfigurationException {
		loadFromByteBuffer(fileName);
	}

	protected void loadFromStream(String fileName) throws IOException, MaryConfigurationException {
		/* Open the file */
		DataInputStream dis = null;
		dis = new DataInputStream(new BufferedInputStream(new FileInputStream(fileName)));

		/* Load the Mary header */
		hdr = new MaryHeader(dis);
		if (hdr.getType() != MaryHeader.UNITFEATS && hdr.getType() != MaryHeader.HALFPHONE_UNITFEATS) {
			throw new IOException("File [" + fileName + "] is not a valid Mary feature file.");
		}
		featureDefinition = new FeatureDefinition(dis);
		int numberOfUnits = dis.readInt();
		featureVectors = new FeatureVector[numberOfUnits];
		for (int i = 0; i < numberOfUnits; i++) {
			featureVectors[i] = featureDefinition.readFeatureVector(i, dis);
		}

	}

	protected void loadFromByteBuffer(String fileName) throws IOException, MaryConfigurationException {
		/* Open the file */
		FileInputStream fis = new FileInputStream(fileName);
		FileChannel fc = fis.getChannel();
		ByteBuffer bb = fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size());
		fis.close();

		/* Load the Mary header */
		hdr = new MaryHeader(bb);
		if (hdr.getType() != MaryHeader.UNITFEATS && hdr.getType() != MaryHeader.HALFPHONE_UNITFEATS) {
			throw new MaryConfigurationException("File [" + fileName + "] is not a valid Mary feature file.");
		}
		featureDefinition = new FeatureDefinition(bb);
		int numberOfUnits = bb.getInt();
		featureVectors = new FeatureVector[numberOfUnits];
		for (int i = 0; i < numberOfUnits; i++) {
			featureVectors[i] = featureDefinition.readFeatureVector(i, bb);
		}

	}

	/**
	 * Get the unit feature vector for the given unit index number.
	 * 
	 * @param unitIndex
	 *            the absolute index number of a unit in the database
	 * @return the corresponding feature vector
	 */
	public FeatureVector getFeatureVector(int unitIndex) {
		return featureVectors[unitIndex];
	}

	/**
	 * Return a shallow copy of the array of feature vectors.
	 * 
	 * @return a new array containing the internal feature vectors
	 */
	public FeatureVector[] getCopyOfFeatureVectors() {
		return (FeatureVector[]) featureVectors.clone();
	}

	/**
	 * Return the internal array of feature vectors.
	 * 
	 * @return the internal array of feature vectors.
	 */
	public FeatureVector[] getFeatureVectors() {
		return featureVectors;
	}

	/**
	 * feature vector mapping according to new feature definition Note: The new feature definition should be a subset of original
	 * feature definition
	 * 
	 * @param newFeatureDefinition
	 *            newFeatureDefinition
	 * @return newFV
	 */
	public FeatureVector[] featureVectorMapping(FeatureDefinition newFeatureDefinition) {

		if (!this.featureDefinition.contains(newFeatureDefinition)) {
			throw new RuntimeException("the new feature definition is not a subset of original feature definition");
		}

		int numberOfFeatures = newFeatureDefinition.getNumberOfFeatures();
		int noByteFeatures = newFeatureDefinition.getNumberOfByteFeatures();
		int noShortFeatures = newFeatureDefinition.getNumberOfShortFeatures();
		int noContiniousFeatures = newFeatureDefinition.getNumberOfContinuousFeatures();

		if (numberOfFeatures != (noByteFeatures + noShortFeatures + noContiniousFeatures)) {
			throw new RuntimeException("The sum of byte, short and continious features are not equal to number of features");
		}

		String[] featureNames = new String[numberOfFeatures];
		for (int j = 0; j < numberOfFeatures; j++) {
			featureNames[j] = newFeatureDefinition.getFeatureName(j);
		}
		int[] featureIndexes = featureDefinition.getFeatureIndexArray(featureNames);
		FeatureVector[] newFV = new FeatureVector[this.getNumberOfUnits()];

		for (int i = 0; i < this.getNumberOfUnits(); i++) {

			// create features array
			byte[] byteFeatures = new byte[noByteFeatures];
			short[] shortFeatures = new short[noShortFeatures];
			float[] continiousFeatures = new float[noContiniousFeatures];

			int countByteFeatures = 0;
			int countShortFeatures = 0;
			int countFloatFeatures = 0;

			for (int j = 0; j < featureIndexes.length; j++) {
				if (newFeatureDefinition.isByteFeature(j)) {
					byteFeatures[countByteFeatures++] = featureVectors[i].getByteFeature(featureIndexes[j]);
				} else if (newFeatureDefinition.isShortFeature(j)) {
					shortFeatures[countShortFeatures++] = featureVectors[i].getShortFeature(featureIndexes[j]);
				} else if (newFeatureDefinition.isContinuousFeature(j)) {
					continiousFeatures[countFloatFeatures++] = featureVectors[i].getContinuousFeature(featureIndexes[j]);
				}
			}

			newFV[i] = newFeatureDefinition.toFeatureVector(i, byteFeatures, shortFeatures, continiousFeatures);
		}

		return newFV;
	}

	/**
	 * Get the unit feature vector for the given unit.
	 * 
	 * @param unit
	 *            a unit in the database
	 * @return the corresponding feature vector
	 */
	public FeatureVector getFeatureVector(Unit unit) {
		return featureVectors[unit.index];
	}

	public FeatureDefinition getFeatureDefinition() {
		return featureDefinition;
	}

	public int getNumberOfUnits() {
		return (featureVectors.length);
	}
}
