package marytts.language.en;

import java.util.Locale;

import org.testng.Assert;
import org.testng.annotations.*;

import marytts.modules.ModuleRegistry;
import marytts.tests.modules.MaryModuleTestCase;

public class PreprocessIT extends MaryModuleTestCase {

	public PreprocessIT() throws Exception {
		super(true);
		module = ModuleRegistry.getModule(Preprocess.class);
	}

	@Test
	public void testParensAndNumber() throws Exception {
		assert processAndCompare("parens-and-number.tokenised", "parens-and-number.words", Locale.US);
	}
}
