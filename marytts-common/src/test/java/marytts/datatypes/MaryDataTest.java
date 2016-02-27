/**
 * Copyright 2000-2006 DFKI GmbH.
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
package marytts.datatypes;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Locale;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.log4j.BasicConfigurator;
import org.junit.BeforeClass;
import org.junit.Test;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

public class MaryDataTest {
	String textString = "Hallöchen Welt!";
	String maryxmlString = "<maryxml version=\"0.4\"\n" + " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"
			+ "xmlns=\"http://mary.dfki.de/2002/MaryXML\" xml:lang=\"de\">\n" + "<s>\n" + "<t>\n" + "Willkommen\n" + "</t>\n"
			+ "<t>\n" + "!\n" + "</t>\n" + "</s>\n" + "</maryxml>";

	@BeforeClass
	public static void setUp() throws Exception {
		BasicConfigurator.configure();
	}

	@Test
	public void testConstructor1() {
		try {
			MaryData md = new MaryData(null, Locale.GERMAN);
		} catch (NullPointerException e) {
			return;
		}
		fail();
	}

	@Test
	public void testConstructor2() {
		MaryData md = new MaryData(MaryDataType.RAWMARYXML, null, false);
		assertTrue(md.getDocument() == null);
	}

	@Test
	public void testConstructor3() {
		MaryData md = new MaryData(MaryDataType.RAWMARYXML, null, true);
		assertTrue(md.getDocument() != null);
	}

	@Test
	public void testConstructor4() {
		MaryData md = new MaryData(MaryDataType.RAWMARYXML, null, true);
		assertTrue(md.getPlainText() == null);
	}

	@Test
	public void testConstructor5() {
		MaryData md = new MaryData(MaryDataType.RAWMARYXML, null, true);
		assertTrue(md.getAudio() == null);
	}

	@Test
	public void testConstructor6() {
		MaryData md = new MaryData(MaryDataType.RAWMARYXML, null, true);
		assertTrue(md.getData() instanceof Document);
	}

	@Test
	public void testConstructor7() {
		MaryData md = new MaryData(MaryDataType.TEXT, Locale.GERMAN, true);
		assertTrue(md.getDocument() == null);
	}

	@Test
	public void testConstructor8() {
		MaryData md = new MaryData(MaryDataType.AUDIO, null, true);
		assertTrue(md.getDocument() == null);
	}

	@Test
	public void testConstructor9() {
		MaryData md = new MaryData(MaryDataType.SSML, null, true);
		assertTrue(md.getDocument() == null);
	}

	@Test
	public void testTextRead1() throws Exception {
		MaryData md = new MaryData(MaryDataType.TEXT, Locale.GERMAN);
		md.setData(textString);
		assertTrue(md.getPlainText().trim().equals(textString.trim()));
	}

	@Test
	public void testTextWrite() throws Exception {
		MaryData md = new MaryData(MaryDataType.TEXT, Locale.GERMAN);
		md.setData(textString);
		StringWriter sw = new StringWriter();
		md.writeTo(sw);
		assertTrue(sw.toString().trim().equals(textString.trim()));
	}

	@Test
	public void testXMLRead1() throws Exception {
		MaryData md = new MaryData(MaryDataType.TOKENS, Locale.GERMAN);
		md.setData(maryxmlString);
		assertTrue(md.getDocument() != null);
	}

	@Test
	public void testXMLWrite() throws Exception {
		MaryData md = new MaryData(MaryDataType.TOKENS, Locale.GERMAN);
		md.setData(maryxmlString);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		md.writeTo(baos);
		assertTrue(!baos.toString().equals(""));
	}

	@Test
	public void testNamespace() throws Exception {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(true);
		DocumentBuilder docBuilder = factory.newDocumentBuilder();
		Document doc = docBuilder.parse(new InputSource(new StringReader(maryxmlString)));
		assertNotNull(doc.getDocumentElement().getNamespaceURI());
		Document doc2 = docBuilder.parse(this.getClass().getResourceAsStream("test1.maryxml"));
		assertNotNull(doc2.getDocumentElement().getNamespaceURI());

	}
}
