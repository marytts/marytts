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
package marytts.tools.dbselection;

import java.util.Set;

import marytts.features.FeatureDefinition;
import marytts.features.FeatureVector;
import marytts.util.FeatureUtils;
import marytts.util.io.FileUtils;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * @author marc
 *
 */
public class CoverageDefinitionTest {
	private FeatureDefinition featDef;
	private CoverageFeatureProvider cfProvider;

	@Before
	public void setup() throws Exception {
		String targetFeaturesData = FileUtils.getStreamAsString(getClass().getResourceAsStream("helloworld.targetfeatures"),
				"UTF-8");
		featDef = FeatureUtils.readFeatureDefinition(targetFeaturesData);
		FeatureVector[] features = FeatureUtils.readFeatureVectors(targetFeaturesData);
		byte[][] data = new byte[1][];
		data[0] = CoverageUtils.toCoverageFeatures("phone next_phone selection_prosody", featDef, features);
		cfProvider = new InMemoryCFProvider(data, null);
	}

	@Test
	public void canInstantiate() throws Exception {
		// setup / exercise
		CoverageDefinition def = new CoverageDefinition(featDef, cfProvider, null);
		def.initialiseCoverage();
		// verify
		CoverageDefinition.CoverageStatistics corpusStats = def.getCorpusStatistics();
		Set<String> phones = corpusStats.coveredPhones;
		assertTrue(phones.contains("h"));
		assertFalse(phones.contains("EI"));
		assertEquals(8, phones.size());
		Set<String> diphones = corpusStats.coveredDiphones;
		assertTrue(diphones.contains("h_@"));
		assertFalse(diphones.contains("@_h"));
		assertEquals(9, diphones.size());
	}
}
