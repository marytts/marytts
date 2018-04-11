package marytts.language.en;

import java.util.Locale;

import org.testng.Assert;
import org.testng.annotations.*;

import marytts.modules.ModuleRegistry;
import marytts.tests.modules.MaryModuleTestCase;

public class PreprocessIT extends MaryModuleTestCase {

    public PreprocessIT() throws Exception {
    }


    @BeforeSuite(alwaysRun = true)
    public void setup() throws Exception {
        setup(true); // need mary startup
        module = ModuleRegistry.getDefaultModule(Preprocess.class.getName());
	Assert.assertNotNull(module);
    }


    @Test
    public void testParensAndNumber() throws Exception {
        assert processAndCompare("parens-and-number.tokenised", "parens-and-number.words", Locale.US);
    }
}
