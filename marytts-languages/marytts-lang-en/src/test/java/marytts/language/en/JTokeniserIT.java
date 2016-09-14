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
	public void testDots1() throws Exception {
		processAndCompare("dots1", Locale.US);
	}

	@Test
	public void testDots2() throws Exception {
		processAndCompare("dots2", Locale.US);
	}

	@Test
	public void testDots3() throws Exception {
		processAndCompare("dots3", Locale.US);
	}

	@Test
	public void testExclam() throws Exception {
		processAndCompare("exclam", Locale.US);
	}

	@Test
	public void testQuest() throws Exception {
		processAndCompare("quest", Locale.US);
	}
}
