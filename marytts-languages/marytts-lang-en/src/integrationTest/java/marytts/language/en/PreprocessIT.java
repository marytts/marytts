package marytts.language.en;

import java.util.Locale;

import org.junit.Test;

import marytts.modules.ModuleRegistry;
import marytts.tests.modules.MaryModuleTestCase;

public class PreprocessIT extends MaryModuleTestCase {

    public PreprocessIT() throws Exception {
        super(true);
        module = ModuleRegistry.getModule(Preprocess.class);
    }

    protected String inputEnding() {
        return "tokenised";
    }

    protected String outputEnding() {
        return "words";
    }

    @Test
    public void testParensAndNumber() throws Exception {
        processAndCompare("parens-and-number", Locale.US);
    }
}
