/**
 * Copyright 2000-2016 DFKI GmbH.
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
package marytts.io;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.apache.log4j.BasicConfigurator;
import org.junit.BeforeClass;
import org.junit.Test;


import marytts.io.XMLSerializer;
import marytts.data.Utterance;

public class XMLSerializerTest {
	String textString = "Hallöchen Welt!";

	@BeforeClass
	public static void setUp() throws Exception {
		BasicConfigurator.configure();
	}

	@Test
	public void testToString() throws Exception {
        XMLSerializer xml_seri = new XMLSerializer();
        System.out.println(xml_seri.toString(new Utterance("textString")));
        assert(false);
    }
}
