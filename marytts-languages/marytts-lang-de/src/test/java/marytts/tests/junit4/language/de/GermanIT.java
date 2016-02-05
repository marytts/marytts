/**
 * 
 */
package marytts.tests.junit4.language.de;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;

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

	@Test
	public void Phonemise() throws Exception {
		String text = "Haus";
		String text2 = "πρεσβύτερος";
		String pos = "NN";
		String pos2 = "XY";
		String result;
		JPhonemiser JP = new JPhonemiser();
		assertEquals(JP.phonemise(text, pos, new StringBuilder()), "' h aU s");
		boolean thrown = false;
		try{
			JP.phonemise(text2, pos2, new StringBuilder());
		}catch (IllegalArgumentException e){
			thrown = true;
		}
		catch (ClassCastException e){
			thrown = true;
		}
		catch (NullPointerException e){
			thrown = true;
		}
		assertTrue(thrown);
		
	}
}
