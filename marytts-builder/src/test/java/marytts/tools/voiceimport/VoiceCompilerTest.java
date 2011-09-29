package marytts.tools.voiceimport;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class VoiceCompilerTest {

	@Test
	public void testPackageName1() {
		String voice = "cmu-slt-hsmm";
		String expected = "CmuSltHsmm";
		String actual = VoiceCompiler.toPackageName(voice);
		assertEquals(expected, actual);
	}

	@Test
	public void testPackageName2() {
		String voice = "Peter Müller";
		String expected = "PeterMLler";
		String actual = VoiceCompiler.toPackageName(voice);
		assertEquals(expected, actual);
	}

	@Test
	public void testPackageName3() {
		String voice = "  %123 bla_bla";
		String expected = "V123Bla_bla";
		String actual = VoiceCompiler.toPackageName(voice);
		assertEquals(expected, actual);
	}
}
