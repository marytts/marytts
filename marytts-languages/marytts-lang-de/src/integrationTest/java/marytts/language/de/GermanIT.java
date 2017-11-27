/**
 *
 */
package marytts.language.de;

import java.io.InputStream;
import java.util.logging.Logger;

import marytts.fst.FSTLookup;
import marytts.language.de.JPhonemiser;

import org.testng.Assert;
import org.testng.annotations.*;

/**
 * @author marc
 *
 */
public class GermanIT {

    // @Test
    // public void loadLexiconStream() throws Exception {
    //     // setup
    //     String lexiconProperty = MaryProperties.needProperty("de.lexicon");
    //     InputStream lexiconStream = MaryProperties.needStream("de.lexicon");
    //     FSTLookup lexicon = new FSTLookup(lexiconStream, lexiconProperty);
    //     String word = "Mensch"; // capitalised
    //     String word2 = "schön"; // with umlaut
    //     // exercise
    //     String[] phone = lexicon.lookup(word);
    //     String[] phone2 = lexicon.lookup(word2);
    //     // verify
    //     Assert.assertTrue(phone.length > 0, "no transcription for " + word);
    //     Assert.assertTrue(phone2.length > 0, "no transcription for " + word2);
    //     Assert.assertEquals("' m E n S", phone[0], "wrong transcription for '" + word + "':");
    //     Assert.assertEquals("' S 2: n", phone2[0], "wrong transcription for '" + word2 + "':");
    // }

    // // Testing for the output when a greek work if phonemised
    // @Test
    // public void PhonemiserT() throws Exception {

    //     JPhonemiser module = new JPhonemiser();
    //     String result = "";
    //     String result2 = "";
    //     // phonemise
    //     result = module.phonemise("αββ", "XY", new StringBuilder());
    //     result2 = module.phonemise("λόγος", "XY", new StringBuilder());

    //     // verify
    //     Assert.assertEquals(result, null);
    //     Assert.assertEquals(result2, null);
    // }

}
