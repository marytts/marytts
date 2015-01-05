package marytts.voice.CmuSltHsmm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.util.Locale;

import javax.sound.sampled.AudioInputStream;
import javax.xml.parsers.ParserConfigurationException;

import marytts.LocalMaryInterface;
import marytts.MaryInterface;
import marytts.datatypes.MaryDataType;
import marytts.htsengine.HMMVoice;
import marytts.modules.synthesis.Voice;
import marytts.util.MaryRuntimeUtils;
import marytts.util.dom.DomUtils;

import org.junit.BeforeClass;
import org.junit.Test;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

public class LoadVoiceIT {

	@BeforeClass
	public static void beforeClass() throws Exception {
		MaryRuntimeUtils.ensureMaryStarted();
	}

	@Test
	public void canLoadVoice() throws Exception {
		Config config = new Config();
		Voice voice = new HMMVoice(config.getName(), null);
		assertNotNull(voice);
	}

	@Test
	public void canSetVoice() throws Exception {
		MaryInterface mary = new LocalMaryInterface();
		String voiceName = new Config().getName();
		mary.setVoice(voiceName);
		assertEquals(voiceName, mary.getVoice());
	}

	@Test
	public void canProcessTextToSpeech() throws Exception {
		MaryInterface mary = new LocalMaryInterface();
		mary.setVoice(new Config().getName());
		AudioInputStream audio = mary.generateAudio("Hello world");
		assertNotNull(audio);
	}

	@Test
	public void canProcessToTargetfeatures() throws Exception {
		MaryInterface mary = new LocalMaryInterface();
		mary.setOutputType(MaryDataType.TARGETFEATURES.name());
		String out = mary.generateText("Hello world");
		assertNotNull(out);
	}

	@Test
	public void canProcessTokensToTargetfeatures() throws Exception {
		MaryInterface mary = new LocalMaryInterface();
		mary.setInputType(MaryDataType.TOKENS.name());
		mary.setOutputType(MaryDataType.TARGETFEATURES.name());
		Document doc = getExampleTokens(mary.getLocale());
		String out = mary.generateText(doc);
		assertNotNull(out);
	}

	@Test
	public void canProcessTokensToSpeech() throws Exception {
		MaryInterface mary = new LocalMaryInterface();
		mary.setInputType(MaryDataType.TOKENS.name());
		Document doc = getExampleTokens(mary.getLocale());
		AudioInputStream audio = mary.generateAudio(doc);
		assertNotNull(audio);
	}

	private Document getExampleTokens(Locale locale) throws ParserConfigurationException, SAXException, IOException {
		String example = MaryDataType.getExampleText(MaryDataType.TOKENS, locale);
		assertNotNull(example);
		Document doc = DomUtils.parseDocument(example);
		return doc;
	}

}
