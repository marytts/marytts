/**
 * 
 */
package marytts.util.dom;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import marytts.datatypes.MaryXML;
import marytts.exceptions.MaryConfigurationException;

import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * @author marc
 *
 */
public class MaryDomUtilsTest {

	private Document createInvalidMaryXML() {
		Document doc = MaryXML.newDocument();
		Element para = MaryXML.appendChildElement(doc.getDocumentElement(), MaryXML.PARAGRAPH);
		Element sent = MaryXML.appendChildElement(para, MaryXML.SENTENCE);
		sent.setTextContent("Hello world"); // but text content not allowed in sentences, so invalid
		return doc;
	}

	private Document createValidMaryXML() {
		Document doc = MaryXML.newDocument();
		Element para = MaryXML.appendChildElement(doc.getDocumentElement(), MaryXML.PARAGRAPH);
		Element sent = MaryXML.appendChildElement(para, MaryXML.SENTENCE);
		Element t1 = MaryXML.appendChildElement(sent, MaryXML.TOKEN);
		t1.setTextContent("Hello");
		Element t2 = MaryXML.appendChildElement(sent, MaryXML.TOKEN);
		t2.setTextContent("world");
		return doc;
	}

	@Test
	public void canValidateMaryXML() throws MaryConfigurationException {
		// setup SUT
		Document doc = createValidMaryXML();
		// exercise / verify
		assertTrue(MaryDomUtils.isSchemaValid(doc));
	}

	@Test
	public void canDetectInvalidMaryXML() throws MaryConfigurationException {
		// setup SUT
		Document doc = createInvalidMaryXML();
		// exercise / verify
		assertFalse(MaryDomUtils.isSchemaValid(doc));
	}

}
