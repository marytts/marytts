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
package marytts.language.en;

import marytts.LocalMaryInterface;
import marytts.MaryInterface;
import marytts.datatypes.MaryDataType;
import marytts.exceptions.SynthesisException;
import marytts.modeling.features.FeatureDefinition;
import marytts.modeling.features.FeatureRegistry;
import marytts.util.FeatureUtils;
import java.io.IOException;

import org.w3c.dom.Document;

import org.testng.Assert;
import org.testng.annotations.*;

/**
 * Some more coverage tests with actual language modules
 *
 * @author marc
 *
 */
public class MaryInterfaceEnIT {

	private MaryInterface mary;

	@BeforeMethod
	public void setUp() throws Exception {
		mary = new LocalMaryInterface();
	}

	@Test
	public void convertTextToAcoustparams() throws Exception {
		mary.setOutputType(MaryDataType.ACOUSTPARAMS.name());
		Document doc = mary.generateXML("Hello world");
		Assert.assertNotNull(doc);
	}

	@Test
	public void convertTextToTargetfeatures() throws Exception {
		mary.setOutputType(MaryDataType.TARGETFEATURES.name());
		String tf = mary.generateText("Hello world");
		Assert.assertNotNull(tf);
	}

	@Test
	public void convertTextToPhonemes() throws Exception {
		mary.setOutputType(MaryDataType.PHONEMES.name());
		Document doc = mary.generateXML("Applejuice");
		Assert.assertNotNull(doc);
	}

	@Test(expectedExceptions = IOException.class)
	public void canSelectTargetfeatures() throws Exception {
		mary.setOutputType(MaryDataType.TARGETFEATURES.name());
		String featureNames = "phone stressed";
		mary.setOutputTypeParams(featureNames);
		String tf = mary.generateText("Hello world");
		FeatureDefinition expected = FeatureRegistry.getTargetFeatureComputer(mary.getLocale(), featureNames)
            .getFeatureDefinition();
		FeatureDefinition actual = FeatureUtils.readFeatureDefinition(tf);
		Assert.assertEquals(expected, actual, expected.featureEqualsAnalyse(actual));
	}

}
