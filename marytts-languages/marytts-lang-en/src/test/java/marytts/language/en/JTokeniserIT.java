package marytts.language.en;

import java.util.Locale;

import org.junit.Test;

import marytts.language.en.JTokeniser;
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
	public void testMultiPunct() throws Exception {
		processAndCompare("dots1", Locale.US);
		processAndCompare("dots2", Locale.US);
		processAndCompare("dots3", Locale.US);
		processAndCompare("exclam", Locale.US);
		processAndCompare("quest", Locale.US);
	}
}
