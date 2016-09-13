package marytts.language.lb

import java.util.Locale

import org.testng.annotations.Test

import marytts.modules.JTokeniser
import marytts.modules.ModuleRegistry
import marytts.tests.modules.MaryModuleTestCase

class JTokeniserIT extends MaryModuleTestCase {

	JTokeniserIT() throws Exception {
		super(true)
		module = ModuleRegistry.getModule(JTokeniser.class)
	}

	String inputEnding() {
		'txt'
	}

	String outputEnding() {
		'tokenised'
	}

	@Test
	void testDots() {
		processAndCompare 'dots1', Locale.forLanguageTag('lb')
	}
}
