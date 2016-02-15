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

package marytts.language.de;

import static org.junit.Assert.assertEquals;

import java.util.Locale;

import marytts.datatypes.MaryXML;
import marytts.features.ByteValuedFeatureProcessor;
import marytts.features.FeatureProcessorManager;
import marytts.features.FeatureRegistry;
import marytts.server.Mary;
import marytts.unitselection.select.Target;
import marytts.util.dom.MaryDomUtils;

import org.junit.BeforeClass;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * @author marc
 *
 */
public class FeatureProcessorIT {

	private static FeatureProcessorManager fpm;

	@BeforeClass
	public static void setupClass() throws Exception {
		if (Mary.currentState() == Mary.STATE_OFF) {
			Mary.startup();
		}
		fpm = FeatureRegistry.determineBestFeatureProcessorManager(Locale.GERMAN);
	}

	// /// Utilities
	private Target createRareWordTarget() {
		return createWordTarget("Sprachsynthese");
	}

	private Target createFrequentWordTarget() {
		return createWordTarget("der");
	}

	private Target createWordTarget(String word) {
		Document doc = MaryXML.newDocument();
		Element s = MaryXML.appendChildElement(doc.getDocumentElement(), MaryXML.SENTENCE);
		Element t = MaryXML.appendChildElement(s, MaryXML.TOKEN);
		MaryDomUtils.setTokenText(t, word);
		Element syl = MaryXML.appendChildElement(t, MaryXML.SYLLABLE);
		Element ph = MaryXML.appendChildElement(syl, MaryXML.PHONE);
		Target target = new Target("dummy", ph);
		return target;
	}

	// /// Tests

	@Test
	public void testWordFrequency() {
		// Setup SUT
		ByteValuedFeatureProcessor wf = (ByteValuedFeatureProcessor) fpm.getFeatureProcessor("word_frequency");
		Target t1 = createRareWordTarget();
		Target t2 = createFrequentWordTarget();
		// Exercise SUT
		byte f1 = wf.process(t1);
		byte f2 = wf.process(t2);
		// verify
		assertEquals(0, f1);
		assertEquals(9, f2);
	}

}
