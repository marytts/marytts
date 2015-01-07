package marytts.modules.phonemiser;

import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import marytts.exceptions.MaryConfigurationException;

import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang.StringUtils;
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

	@Test(dataProvider = "allophoneArrayData")
	public void testSplitIntoAllophones(String phoneString, Allophone[] expecteds) {
		Allophone[] actuals = allophoneSet.splitIntoAllophones(phoneString);
		Assert.assertEquals(expecteds, actuals);
	}

	@DataProvider
	private Object[][] allophoneArrayData() {
		Allophone t = allophoneSet.getAllophone("t");
		Allophone oy = allophoneSet.getAllophone("OY");
		Allophone[] allophones = new Allophone[] { t, oy, t, oy, t, oy };
		// @formatter:off
		return new Object[][] {
				{ "tOYtOYtOY", allophones },
				{ "'tOYtOYtOY", allophones },
				{ ",tOYtOY'tOY", allophones },
				{ "tOY tOY tOY", allophones },
				{ "'tOY tOY tOY", allophones },
				{ ",tOY tOY 'tOY", allophones },
				{ "tOY-tOY-tOY", allophones },
				{ "'tOY-tOY-tOY", allophones },
				{ ",tOY-tOY-'tOY", allophones }
		};
		// @formatter:on
	}

	@Test(dataProvider = "allophoneStringData")
	public void testSplitAllophoneString(String phoneString, String expected) {
		String actual = allophoneSet.splitAllophoneString(phoneString);
		Assert.assertEquals(actual, expected);
	}

	@Test(dataProvider = "allophoneStringData")
	public void testSplitIntoAllophoneList(String phoneString, String allophoneListString) {
		String[] allophones = StringUtils.split(allophoneListString);
		List<String> expected = Arrays.asList(allophones);
		List<String> actual = allophoneSet.splitIntoAllophoneList(phoneString);
		Assert.assertEquals(actual, expected);
	}

	@DataProvider
	private Object[][] allophoneStringData() {
		// @formatter:off
		return new Object[][] {
				{ "tOYtOYtOY", "t OY t OY t OY" },
				{ "'tOYtOYtOY", "' t OY t OY t OY" },
				{ ",tOYtOY'tOY", ", t OY t OY ' t OY" },
				{ "tOY tOY tOY", "t OY t OY t OY" },
				{ "'tOY tOY tOY", "' t OY t OY t OY" },
				{ ",tOY tOY 'tOY", ", t OY t OY ' t OY" },
				{ "tOY-tOY-tOY", "t OY - t OY - t OY" },
				{ "'tOY-tOY-tOY", "' t OY - t OY - t OY" },
				{ ",tOY-tOY-'tOY", ", t OY - t OY - ' t OY" }
		};
		// @formatter:on
	}

	@Test(expectedExceptions = IllegalArgumentException.class)
	public void testSplitIntoAllophoneListWithInvalidInput() {
		Assert.assertNull(allophoneSet.splitIntoAllophoneList("!@#$%^"));
	}

	@Test(dataProvider = "syllabifierData", expectedExceptions = NotImplementedException.class)
	public void testSyllabify(String phones, String expected) {
		String actual = allophoneSet.syllabify(phones);
		Assert.assertEquals(actual, expected);
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
}
