package marytts.modules.phonemiser;

import java.io.InputStream;

import marytts.exceptions.MaryConfigurationException;

import org.testng.Assert;
import org.testng.annotations.*;

public class AllophoneSetTest {

	private AllophoneSet allophoneSet;

	@BeforeTest
	public void setUp() throws MaryConfigurationException {
		InputStream alloStream = AllophoneSetTest.class.getResourceAsStream("allophones.de.xml");
		allophoneSet = AllophoneSet.getAllophoneSet(alloStream, "test");
	}

	@Test
	public void testLoadOnceReuse() throws MaryConfigurationException {
		InputStream alloStream1b = AllophoneSetTest.class.getResourceAsStream("allophones.de.xml");
		AllophoneSet allo1b = AllophoneSet.getAllophoneSet(alloStream1b, "test");
		Assert.assertEquals(allophoneSet, allo1b);
	}

	@Test
	public void testUnloadedIsUnavailable() {
		Assert.assertFalse(AllophoneSet.hasAllophoneSet("laaleeloo"));
	}

	@Test
	public void testLoadedIsAvailable() throws MaryConfigurationException {
		Assert.assertTrue(AllophoneSet.hasAllophoneSet("test"));
		AllophoneSet copy = AllophoneSet.getAllophoneSetById("test");
		Assert.assertEquals(allophoneSet, copy);
	}

	@Test(expectedExceptions = IllegalArgumentException.class)
	public void testGetInvalidAllophone() {
		allophoneSet.getAllophone("fnord");
	}
}
