package marytts.language.de

import marytts.fst.FSTLookup
import marytts.server.MaryProperties
import org.testng.annotations.Test

class GermanIT {

    @Test
    void loadLexiconStream() {
        // setup
        def lexiconProperty = MaryProperties.needProperty('de.lexicon')
        def lexiconStream = MaryProperties.needStream('de.lexicon')
        def lexicon = new FSTLookup(lexiconStream, lexiconProperty)
        def word = 'Mensch' // capitalised
        def word2 = 'schön' // with umlaut
        // exercise
        def phone = lexicon.lookup(word)
        def phone2 = lexicon.lookup(word2)
        // verify
        assert phone.length > 0, "no transcription for $word"
        assert phone2.length > 0, "no transcription for $word2"
        assert phone[0] == "' m E n S", "wrong transcription for '$word'"
        assert phone2[0] == "' S 2: n", "wrong transcription for '$word2'"
    }

    // Testing for the output when a greek work if phonemised
    @Test
    void PhonemiserT() {
        def module = new JPhonemiser()
        // phonemise
        def result = module.phonemise("αββ", "XY", new StringBuilder())
        def result2 = module.phonemise("λόγος", "XY", new StringBuilder())
        // verify
        assert result == "' ? a l - f a: - ' b E - t a: - ' b E - t a:"
        assert result2 == "' g a - m a:"
    }
}
