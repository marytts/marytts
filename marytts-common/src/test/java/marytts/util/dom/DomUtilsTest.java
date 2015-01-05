package marytts.util.dom;

import java.io.ByteArrayInputStream;

import marytts.util.io.FileUtils;

import org.junit.Test;

public class DomUtilsTest {

	@Test
	public void validatingParseString() throws Exception {
		String docAsString = FileUtils.getStreamAsString(DomUtilsTest.class.getResourceAsStream("sample.maryxml"), "UTF-8");
		DomUtils.parseDocument(docAsString, true);
	}

	@Test
	public void validatingParseStream() throws Exception {
		DomUtils.parseDocument(DomUtilsTest.class.getResourceAsStream("sample.maryxml"), true);
	}

}
