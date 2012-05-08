package marytts.language.fr;

import java.io.IOException;

import marytts.exceptions.MaryConfigurationException;
import marytts.util.MaryUtils;

public class Phonemiser extends marytts.modules.JPhonemiser {

    public Phonemiser() throws IOException, MaryConfigurationException {
        super("fr.");
    }
    
    @Override 
    public String phonemise(String text, String pos, StringBuilder g2pMethod)
    {
        // First, try a simple userdict and lexicon lookup:

        text = text.replaceAll("[0-9]+","");
        String result = userdictLookup(text, pos);
        if (result != null) {
            g2pMethod.append("userdict");
            return result;
        }
        
        result = lexiconLookup(text, pos);
        if (result != null) {
            g2pMethod.append("lexicon");
            return result;
        }

        // Lookup attempts failed. Try normalising exotic letters
        // (diacritics on vowels, etc.), look up again:
        String normalised = MaryUtils.normaliseUnicodeLetters(text, getLocale());
        if (!normalised.equals(text)) {
            result = userdictLookup(normalised, pos);
            if (result != null) {
                g2pMethod.append("userdict");
                return result;
            }
            result = lexiconLookup(normalised, pos);
            if (result != null) {
                g2pMethod.append("lexicon");
                return result;
            }
        }
           
        // Cannot find it in the lexicon -- apply letter-to-sound rules
        // to the normalised form
        

        String phones = lts.predictPronunciation(text);
        result = lts.syllabify(phones);
        if (result != null) {
            g2pMethod.append("rules");
            return result;
        }

        return null;
    }


}
