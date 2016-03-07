package marytts.tools.dbselection;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

import org.fest.assertions.Assertions;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import com.google.common.io.Resources;


import org.testng.Assert;
import org.testng.annotations.*;


/**
 * Tests for WikipediaMarkupCleaner
 *
 * @author ingmar
 *
 */
public class WikipediaMarkupCleanerTest {

	private WikipediaMarkupCleaner wikiCleaner;
	private URL markupResource;

	@BeforeMethod
	public void setup() throws IOException {
		wikiCleaner = new WikipediaMarkupCleaner();
		markupResource = Resources.getResource(getClass(), "Autorack.mediawiki");
	}

	@Test
	public void testRemoveMarkup() throws IOException, URISyntaxException {
		// read markup from test resource
		String page = Resources.toString(markupResource, Charsets.UTF_8);
		// process to extract markup-less text
		String pageWithoutMarkup = wikiCleaner.removeMarkup(page).firstElement();

		// get expected text and compare with actual processed text
		URL expectedResource = Resources.getResource(getClass(), "Autorack.txt");

        // Asserting
        Assert.assertEquals(pageWithoutMarkup, Resources.toString(expectedResource, Charsets.UTF_8));
	}

}
