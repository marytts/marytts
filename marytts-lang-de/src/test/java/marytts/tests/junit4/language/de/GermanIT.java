/**
 * 
 */
package marytts.tests.junit4.language.de;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import marytts.fst.FSTLookup;
import marytts.server.MaryProperties;

import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author marc
 *
 */
public class GermanIT {
	@BeforeClass
	public static void loadMaryProperties() throws Exception {
		MaryProperties.readProperties();
	}
	
	@Test
	public void loadLexiconFile() throws Exception {
		// setup
		String lexiconFilename = MaryProperties.needFilename("de.lexicon");
		FSTLookup lexicon = new FSTLookup(lexiconFilename);
		String word = "Mensch"; // capitalised
		String word2 = "schön"; // with umlaut
		// exercise
		String[] phone = lexicon.lookup(word);
		String[] phone2 = lexicon.lookup(word2);
		// verify
		assertTrue("no transcription for "+word, phone.length > 0);
		assertTrue("no transcription for "+word2, phone2.length > 0);
		assertEquals("wrong transcription for '"+word+"':", "' m E n S", phone[0]);
		assertEquals("wrong transcription for '"+word2+"':", "' S 2: n", phone2[0]);
	}
	
	@Test
	public void loadLexiconStream() throws Exception {
		// setup
		String lexiconFilename = MaryProperties.needFilename("de.lexicon");
		FileInputStream fis = new FileInputStream(lexiconFilename);
		FSTLookup lexicon = new FSTLookup(fis, lexiconFilename);
		String word = "Mensch"; // capitalised
		String word2 = "schön"; // with umlaut
		// exercise
		String[] phone = lexicon.lookup(word);
		String[] phone2 = lexicon.lookup(word2);
		// verify
		assertTrue("no transcription for "+word, phone.length > 0);
		assertTrue("no transcription for "+word2, phone2.length > 0);
		assertEquals("wrong transcription for '"+word+"':", "' m E n S", phone[0]);
		assertEquals("wrong transcription for '"+word2+"':", "' S 2: n", phone2[0]);
		
	}

}
