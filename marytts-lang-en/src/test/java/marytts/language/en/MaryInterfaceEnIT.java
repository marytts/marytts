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



import marytts.MaryInterface;
import marytts.datatypes.MaryDataType;

import org.junit.Test;
import org.w3c.dom.Document;

import static org.junit.Assert.*;

/**
 * Some more coverage tests with actual language modules
 * @author marc
 *
 */
public class MaryInterfaceEnIT {

	@Test
	public void convertTextToAcoustparams() throws Exception {
		// setup
		MaryInterface mary = MaryInterface.getLocalMaryInterface();
		mary.setOutputType(MaryDataType.ACOUSTPARAMS);
		Document doc = mary.generateXML("Hello world");
		assertNotNull(doc);
	}

	@Test
	public void convertTextToTargetfeatures() throws Exception {
		// setup
		MaryInterface mary = MaryInterface.getLocalMaryInterface();
		mary.setOutputType(MaryDataType.TARGETFEATURES);
		mary.setOutputTypeParams("phone stressed");
		String tf = mary.generateText("Hello world");
		assertNotNull(tf);
	}
	
	
}
