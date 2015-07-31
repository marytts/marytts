package marytts.modules.phonemiser;

import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import marytts.exceptions.MaryConfigurationException;

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

	@Test(expectedExceptions = IllegalArgumentException.class)
	public void testSyllabifyWithEmptyInput() {
		allophoneSet.syllabify("");
	}

	@Test(dataProvider = "syllabifierData")
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
				{ ",mama'mama", ", m a - m a - ' m a - m a" },
				{ "StaInSla:k", "' S t aI n - S l a: k" },
				{ "StRUntsdUm", "' S t R U n ts - d U m" },
				{ "a:b6", "' a: - b 6" },
				{ "daU6vU6st", "' d aU - 6 - v U 6 s t" },
				{ "fOY6maU6", "' f OY - 6 - m aU - 6" },
				{ ",ha6tbE6ktU6mg@'ve:6", ", h a 6 t - b E 6 k - t U 6 m - g @ - ' v e: 6" },
				{ "'pfaU@n,SlOY@", "' pf aU - @ n - , S l OY - @" }
		};
		// @formatter:on
	}

	@Test
	public void testSyllabifyWithoutNucleus() {
		String actual = allophoneSet.syllabify("s");
		String expected = "' s";
		Assert.assertEquals(actual, expected);
	}

	@Test(dataProvider = "legacySyllabifierData")
	public void testLegacySyllabify(String phones, String expected) {
		String actual = allophoneSet.syllabify(phones);
		Assert.assertEquals(actual, expected);
	}

	@DataProvider
	private Object[][] legacySyllabifierData() {
		// @formatter:off
		return new Object[][] {
				{ "ma1", "' m a" },
				{ "ma1ma", "' m a - m a" },
				{ "mama1", "m a - ' m a" },
				{ "ma1mama", "' m a - m a - m a" },
				{ "mama1ma", "m a - ' m a - m a" },
				{ "mamama1", "m a - m a - ' m a" },
				{ "ma1mamama", "' m a - m a - m a - m a" },
				{ "mama1mama", "m a - ' m a - m a - m a" },
				{ "mamama1ma", "m a - m a - ' m a - m a" },
				{ "mamamama1", "m a - m a - m a - ' m a" }
		};
		// @formatter:on
	}

	@Test
	public void testLegacySyllabifyWithoutNucleus() {
		String actual = allophoneSet.syllabify("s1");
		String expected = "' s";
		Assert.assertEquals(actual, expected);
	}

}
