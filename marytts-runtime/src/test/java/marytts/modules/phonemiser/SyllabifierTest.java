package marytts.modules.phonemiser;

import static org.junit.Assert.*;

import java.io.InputStream;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(JUnitParamsRunner.class)
public class SyllabifierTest {

	private Syllabifier syllabifier;

	@Before
	public void setUp() throws Exception {
		InputStream resource = getClass().getResourceAsStream("/marytts/features/allophones.ROOT.xml");
		AllophoneSet allophoneSet = AllophoneSet.getAllophoneSet(resource, "");
		syllabifier = new Syllabifier(allophoneSet);
	}

	@Test
	// @formatter:off
	@Parameters({
		"ma, ' m a",
		"'ma, ' m a",
		"mama, ' m a - m a",
		"'mama, ' m a - m a",
		"ma'ma, m a - ' m a",
		"mamama, ' m a - m a - m a",
		"'mamama, ' m a - m a - m a",
		"ma'mama, m a - ' m a - m a",
		"mama'ma, m a - m a - ' m a",
		"mamamama, ' m a - m a - m a - m a",
		"'mamamama, ' m a - m a - m a - m a",
		"ma'mamama, m a - ' m a - m a - m a",
		"mama'mama, m a - m a - ' m a - m a",
		"mamama'ma, m a - m a - m a - ' m a",
		"\\,mama'mama, \\, m a - m a - ' m a - m a"
		})
	// @formatter:on
	public void testSyllabify(String phones, String expected) {
		String actual = syllabifier.syllabify(phones);
		assertEquals(expected, actual);
	}

}
