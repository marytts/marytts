package marytts.modules.phonemiser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;

import marytts.exceptions.MaryConfigurationException;

import org.junit.Before;
import org.junit.Test;

public class AllophoneSetTest {

	private AllophoneSet allophoneSet;

	@Before
	public void setUp() throws MaryConfigurationException {
		InputStream alloStream = AllophoneSetTest.class.getResourceAsStream("allophones.de.xml");
		allophoneSet = AllophoneSet.getAllophoneSet(alloStream, "test");
	}

	@Test
	public void testLoadOnceReuse() throws MaryConfigurationException {
		InputStream alloStream1b = AllophoneSetTest.class.getResourceAsStream("allophones.de.xml");
		AllophoneSet allo1b = AllophoneSet.getAllophoneSet(alloStream1b, "test");
		assertEquals(allophoneSet, allo1b);
	}

	@Test
	public void testUnloadedIsUnavailable() {
		assertFalse(AllophoneSet.hasAllophoneSet("laaleeloo"));
	}

	@Test
	public void testLoadedIsAvailable() throws MaryConfigurationException {
		assertTrue(AllophoneSet.hasAllophoneSet("test"));
		AllophoneSet copy = AllophoneSet.getAllophoneSetById("test");
		assertEquals(allophoneSet, copy);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testGetInvalidAllophone() {
		allophoneSet.getAllophone("fnord");
	}
}
