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
	void testDots1() {
		processAndCompare 'dots1', locale
	}

	@Test
	void testDots2() {
		processAndCompare 'dots2', locale
	}

	@Test
	void testDots3() {
		processAndCompare 'dots3', locale
	}

	@Test
	void testExclam() {
		processAndCompare 'exclam', locale
	}

	@Test
	void testQuest() {
		processAndCompare 'quest', locale
	}
}
