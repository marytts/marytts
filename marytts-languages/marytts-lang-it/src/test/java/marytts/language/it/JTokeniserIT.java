package marytts.language.it;

import java.util.Locale;

import org.junit.Test;

import marytts.language.it.JTokeniser;
import marytts.modules.ModuleRegistry;
import marytts.tests.modules.MaryModuleTestCase;

public class JTokeniserIT extends MaryModuleTestCase {

	public JTokeniserIT() throws Exception {
		super(true);
		module = ModuleRegistry.getModule(JTokeniser.class);
	}

	protected String inputEnding() {
		return "txt";
	}

	protected String outputEnding() {
		return "tokenised";
	}

	@Test
	public void testDots() throws Exception {
		processAndCompare("dots1", Locale.ITALIAN);
	}
}
