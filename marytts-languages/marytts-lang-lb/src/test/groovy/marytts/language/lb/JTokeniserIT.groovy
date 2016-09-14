package marytts.language.lb

import java.util.Locale

import org.testng.annotations.Test

import marytts.modules.JTokeniser
import marytts.modules.ModuleRegistry
import marytts.tests.modules.MaryModuleTestCase

class JTokeniserIT extends MaryModuleTestCase {
	
	def locale

	JTokeniserIT() throws Exception {
		super(true)
		module = ModuleRegistry.getModule(JTokeniser.class)
		locale = Locale.forLanguageTag('lb')
	}

	String inputEnding() {
		'txt'
	}

	String outputEnding() {
		'tokenised'
	}

	@Test
	void testMultiPunct() {
		processAndCompare 'dots1', locale
		processAndCompare 'dots2', locale
		processAndCompare 'dots3', locale
		processAndCompare 'exclam', locale
		processAndCompare 'quest', locale
	}
}
