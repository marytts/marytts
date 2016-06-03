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

import java.util.Locale;

import org.apache.commons.lang.StringUtils;


import org.custommonkey.xmlunit.*;
import org.testng.Assert;
import org.testng.annotations.*;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import java.io.IOException;
import javax.xml.parsers.ParserConfigurationException;

import marytts.util.dom.DomUtils;

import org.apache.log4j.BasicConfigurator;

import marytts.io.XMLSerializer;
import marytts.data.Utterance;

/**
 *  TODO: think about a real test....
 */
public class XMLSerializerTest {


	@Test
	public void testToString()
        throws Exception
    {

        // Document expectedDoc;
		// String words = "<maryxml xmlns=\"http://mary.dfki.de/2002/MaryXML\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" version=\"0.5\" xml:lang=\"fr\"><p>"+ tokenised +"<s>"+ tokenised +"<t sounds_like=\"" + expected + "\">" + tokenised + "</t></s></p></maryxml>";
		// expectedDoc = DomUtils.parseDocument(words);


        // System.out.println("======== expected result =========");
        // System.out.println(DomUtils.serializeToString(expectedDoc));

        // MaryData output_data = module.process(input_data);

        // System.out.println("======== achieved result =========");
        // System.out.println(DomUtils.serializeToString(output_data.getDocument()));
        // Diff diff = XMLUnit.compareXML(expectedDoc, output_data.getDocument());


        // System.out.println("======== Diff =========");
        // // System.out.println(diff.toString());
        // Assert.assertEquals(DomUtils.serializeToString(expectedDoc), DomUtils.serializeToString(output_data.getDocument()));

        // Initialize the reference
        String str_original_document = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><maryxml xmlns=\"http://mary.dfki.de/2002/MaryXML\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" version=\"0.5\" xml:lang=\"en-US\"><p>Welcome to the world of speech synthesis!<s>Welcome to the world of speech synthesis!<t>Welcome</t><t>to</t></s></p></maryxml>";
        Document original_document = DomUtils.parseDocument(str_original_document);
        XMLSerializer xml_seri = new XMLSerializer();
        Utterance utt = xml_seri.unpackDocument(original_document);
        System.out.println(xml_seri.toString(utt));
    }
}
