package marytts.util.dom;

import java.io.ByteArrayInputStream;

import org.apache.commons.io.IOUtils;
import org.testng.annotations.Test;

public class DomUtilsTest {

	@Test
	public void validatingParseString() throws Exception {
		String docAsString = IOUtils.toString(DomUtilsTest.class.getResourceAsStream("sample.maryxml"), "UTF-8");
		DomUtils.parseDocument(docAsString, true);
	}

	@Test
	public void validatingParseStream() throws Exception {
		DomUtils.parseDocument(DomUtilsTest.class.getResourceAsStream("sample.maryxml"), true);
	}

}
