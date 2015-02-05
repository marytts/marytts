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

public class HalfPhoneFeatureFileReader extends FeatureFileReader {
	protected FeatureDefinition leftWeights;
	protected FeatureDefinition rightWeights;

	public HalfPhoneFeatureFileReader() {
		super();
	}

	public HalfPhoneFeatureFileReader(String fileName) throws IOException, MaryConfigurationException {
		super(fileName);
	}

	@Override
	protected void loadFromStream(String fileName) throws IOException, MaryConfigurationException {
		/* Open the file */
		DataInputStream dis = null;
		dis = new DataInputStream(new BufferedInputStream(new FileInputStream(fileName)));
		/* Load the Mary header */
		hdr = new MaryHeader(dis);
		if (hdr.getType() != MaryHeader.HALFPHONE_UNITFEATS) {
			throw new IOException("File [" + fileName + "] is not a valid Mary Halfphone Features file.");
		}
		leftWeights = new FeatureDefinition(dis);
		rightWeights = new FeatureDefinition(dis);
		assert leftWeights.featureEquals(rightWeights) : "Halfphone unit feature file contains incompatible feature definitions for left and right units -- this should not happen!";
		featureDefinition = leftWeights; // one of them, for super class
		int numberOfUnits = dis.readInt();
		featureVectors = new FeatureVector[numberOfUnits];
		for (int i = 0; i < numberOfUnits; i++) {
			featureVectors[i] = featureDefinition.readFeatureVector(i, dis);
		}
	}

	@Override
	protected void loadFromByteBuffer(String fileName) throws IOException, MaryConfigurationException {
		/* Open the file */
		FileInputStream fis = new FileInputStream(fileName);
		FileChannel fc = fis.getChannel();
		ByteBuffer bb = fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size());
		fis.close();

		/* Load the Mary header */
		hdr = new MaryHeader(bb);
		if (hdr.getType() != MaryHeader.HALFPHONE_UNITFEATS) {
			throw new MaryConfigurationException("File [" + fileName + "] is not a valid Mary Halfphone Features file.");
		}
		leftWeights = new FeatureDefinition(bb);
		rightWeights = new FeatureDefinition(bb);
		assert leftWeights.featureEquals(rightWeights) : "Halfphone unit feature file contains incompatible feature definitions for left and right units -- this should not happen!";
		featureDefinition = leftWeights; // one of them, for super class
		int numberOfUnits = bb.getInt();
		featureVectors = new FeatureVector[numberOfUnits];
		for (int i = 0; i < numberOfUnits; i++) {
			featureVectors[i] = featureDefinition.readFeatureVector(i, bb);
		}
	}

	public FeatureDefinition getLeftWeights() {
		return leftWeights;
	}

	public FeatureDefinition getRightWeights() {
		return rightWeights;
	}

}
