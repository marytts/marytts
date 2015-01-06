package marytts.modules.phonemiser;

import java.io.InputStream;
import java.util.LinkedList;

import org.testng.Assert;
import org.testng.annotations.*;

public class SyllabifierTest {

	private Syllabifier syllabifier;

	@BeforeTest
	public void setUp() throws Exception {
		InputStream resource = getClass().getResourceAsStream("/marytts/features/allophones.ROOT.xml");
		AllophoneSet allophoneSet = AllophoneSet.getAllophoneSet(resource, "");
		syllabifier = new Syllabifier(allophoneSet);
	}

	@Test(dataProvider = "syllabifierData")
	public void testSyllabify(String phones, String expected) {
		String actual = syllabifier.syllabify(phones);
		Assert.assertEquals(expected, actual);
	}

	@DataProvider
	private Object[][] syllabifierData() {
		// @formatter:off
		return new Object[][] {
				{ "ma", "' m a" },
				{ "'ma", "' m a" },
				{ "mama", "' m a - m a" },
				{ "'mama", "' m a - m a" },
				{ "ma'ma", "m a - ' m a" },
				{ "mamama", "' m a - m a - m a" },
				{ "'mamama", "' m a - m a - m a" },
				{ "ma'mama", "m a - ' m a - m a" },
				{ "mama'ma", "m a - m a - ' m a" },
				{ "mamamama", "' m a - m a - m a - m a" },
				{ "'mamamama", "' m a - m a - m a - m a" },
				{ "ma'mamama", "m a - ' m a - m a - m a" },
				{ "mama'mama", "m a - m a - ' m a - m a" },
				{ "mamama'ma", "m a - m a - m a - ' m a" },
				{ ",mama'mama", ", m a - m a - ' m a - m a" }
		};
		// @formatter:on
	}

	@Test(expectedExceptions = IllegalArgumentException.class)
	public void testSyllabfyNullList() {
		LinkedList<String> nullList = null;
		syllabifier.syllabify(nullList);
	}
}
