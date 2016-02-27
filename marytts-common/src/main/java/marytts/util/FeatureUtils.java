/**
 * Copyright 2011 DFKI GmbH.
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
package marytts.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import marytts.features.FeatureDefinition;
import marytts.features.FeatureVector;

/**
 * @author marc
 * 
 */
public class FeatureUtils {

	public static FeatureDefinition readFeatureDefinition(String targetFeaturesData) throws IOException {
		BufferedReader in = new BufferedReader(new StringReader(targetFeaturesData));
		try {
			return new FeatureDefinition(in, false);
		} finally {
			in.close();
		}
	}

	public static FeatureDefinition readFeatureDefinition(InputStream featureStream) throws IOException {
		BufferedReader in = new BufferedReader(new InputStreamReader(featureStream, "UTF-8"));
		try {
			return new FeatureDefinition(in, false);
		} finally {
			in.close();
		}
	}

	public static FeatureVector[] readFeatureVectors(String targetFeaturesData) throws IOException {
		BufferedReader br = new BufferedReader(new StringReader(targetFeaturesData));
		FeatureDefinition def = new FeatureDefinition(br, false); // false: do not read weights
		// skip the clear text section: read until an empty line occurs
		String line;
		while ((line = br.readLine()) != null) {
			if (line.trim().equals(""))
				break;
		}
		// read the binary section
		List<FeatureVector> fvs = new ArrayList<FeatureVector>();
		while ((line = br.readLine()) != null) {
			if (line.trim().equals(""))
				break;
			FeatureVector fv = def.toFeatureVector(0, line);
			fvs.add(fv);
		}
		return (FeatureVector[]) fvs.toArray(new FeatureVector[fvs.size()]);
	}
}
