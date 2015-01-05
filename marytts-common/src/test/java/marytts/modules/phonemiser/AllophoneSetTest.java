package marytts.modules.phonemiser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;

import marytts.exceptions.MaryConfigurationException;

import org.junit.Test;

public class AllophoneSetTest {

	@Test
	public void testLoadOnceReuse() throws MaryConfigurationException {
		InputStream alloStream = AllophoneSetTest.class.getResourceAsStream("allophones.de.xml");
		AllophoneSet allo1a = AllophoneSet.getAllophoneSet(alloStream, "test1");
		InputStream alloStream1b = AllophoneSetTest.class.getResourceAsStream("allophones.de.xml");
		AllophoneSet allo1b = AllophoneSet.getAllophoneSet(alloStream1b, "test1");
		assertEquals(allo1a, allo1b);
	}

	@Test
	public void testUnloadedIsUnavailable() {
		assertFalse(AllophoneSet.hasAllophoneSet("laaleeloo"));
	}

	@Test
	public void testLoadedIsAvailable() throws MaryConfigurationException {
		InputStream alloStream = AllophoneSetTest.class.getResourceAsStream("allophones.de.xml");
		AllophoneSet orig = AllophoneSet.getAllophoneSet(alloStream, "test2");
		assertTrue(AllophoneSet.hasAllophoneSet("test2"));
		AllophoneSet copy = AllophoneSet.getAllophoneSetById("test2");
		assertEquals(orig, copy);

	}
}
