/**
 * 
 */
package marytts.tests.junit4.language.de;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.util.logging.Logger;

import marytts.datatypes.MaryDataType;
import marytts.fst.FSTLookup;
import marytts.language.de.JPhonemiser;
import marytts.server.MaryProperties;

import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author marc
 *
 */
public class GermanIT {

	@Test
	public void loadLexiconStream() throws Exception {
		// setup
		String lexiconProperty = MaryProperties.needProperty("de.lexicon");
		InputStream lexiconStream = MaryProperties.needStream("de.lexicon");
		FSTLookup lexicon = new FSTLookup(lexiconStream, lexiconProperty);
		String word = "Mensch"; // capitalised
		String word2 = "schön"; // with umlaut
		// exercise
		String[] phone = lexicon.lookup(word);
		String[] phone2 = lexicon.lookup(word2);
		// verify
		assertTrue("no transcription for " + word, phone.length > 0);
		assertTrue("no transcription for " + word2, phone2.length > 0);
		assertEquals("wrong transcription for '" + word + "':", "' m E n S", phone[0]);
		assertEquals("wrong transcription for '" + word2 + "':", "' S 2: n", phone2[0]);
	}

	// Testing for the output when a greek work if phonemised
	@Test
	public void PhonemiserT() throws Exception {
		JPhonemiser module = new JPhonemiser();
		String result = "";
		String result2 = "";
		// phonemise
		result = module.phonemise("αββ", "XY", new StringBuilder());
		result2 = module.phonemise("λόγος", "XY", new StringBuilder());
		// verify
		assertEquals(result, null);
		assertEquals(result2, null);
	}

}
