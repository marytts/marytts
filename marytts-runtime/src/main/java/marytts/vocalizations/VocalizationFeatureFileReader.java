/**
 * Copyright 2010 DFKI GmbH.
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
package marytts.vocalizations;

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

public class VocalizationFeatureFileReader extends marytts.unitselection.data.FeatureFileReader {

	public VocalizationFeatureFileReader(String fileName) throws IOException, MaryConfigurationException {
		load(fileName);
	}

	@Override
	protected void loadFromStream(String fileName) throws IOException, MaryConfigurationException {
		/* Open the file */
		DataInputStream dis = null;
		dis = new DataInputStream(new BufferedInputStream(new FileInputStream(fileName)));

		/* Load the Mary header */
		hdr = new MaryHeader(dis);
		if (hdr.getType() != MaryHeader.LISTENERFEATS) {
			throw new MaryConfigurationException("File [" + fileName + "] is not a valid Mary listener feature file.");
		}
		featureDefinition = new FeatureDefinition(dis);
		int numberOfUnits = dis.readInt();
		featureVectors = new FeatureVector[numberOfUnits];
		for (int i = 0; i < numberOfUnits; i++) {
			featureVectors[i] = featureDefinition.readFeatureVector(i, dis);
		}
	}

	@Override
	protected void loadFromByteBuffer(String fileName) throws IOException, MaryConfigurationException {
		/* Open the file */
		/* Open the file */
		FileInputStream fis = new FileInputStream(fileName);
		FileChannel fc = fis.getChannel();
		ByteBuffer bb = fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size());
		fis.close();

		/* Load the Mary header */
		hdr = new MaryHeader(bb);
		if (hdr.getType() != MaryHeader.LISTENERFEATS) {
			throw new MaryConfigurationException("File [" + fileName + "] is not a valid Mary listener feature file.");
		}
		featureDefinition = new FeatureDefinition(bb);
		int numberOfUnits = bb.getInt();
		featureVectors = new FeatureVector[numberOfUnits];
		for (int i = 0; i < numberOfUnits; i++) {
			featureVectors[i] = featureDefinition.readFeatureVector(i, bb);
		}
	}

	/**
	 * @param args
	 *            args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
