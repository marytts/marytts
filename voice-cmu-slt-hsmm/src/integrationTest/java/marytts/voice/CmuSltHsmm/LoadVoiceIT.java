package marytts.voice.CmuSltHsmm;

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

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
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
        Assert.assertNotNull(voice);
    }

    @Test
    public void canSetVoice() throws Exception {
        MaryInterface mary = new LocalMaryInterface();
        String voiceName = new Config().getName();
        mary.setVoice(voiceName);
        Assert.assertEquals(voiceName, mary.getVoice());
    }

    @Test
    public void canProcessTextToSpeech() throws Exception {
        MaryInterface mary = new LocalMaryInterface();
        mary.setVoice(new Config().getName());
        AudioInputStream audio = mary.generateAudio("Hello world");
        Assert.assertNotNull(audio);
    }

    @Test
    public void canProcessToTargetfeatures() throws Exception {
        MaryInterface mary = new LocalMaryInterface();
        mary.setOutputType(MaryDataType.TARGETFEATURES.name());
        String out = mary.generateText("Hello world");
        Assert.assertNotNull(out);
    }

    @Test
    public void canProcessTokensToTargetfeatures() throws Exception {
        MaryInterface mary = new LocalMaryInterface();
        mary.setInputType(MaryDataType.TOKENS.name());
        mary.setOutputType(MaryDataType.TARGETFEATURES.name());
        Document doc = getExampleTokens(mary.getLocale());
        String out = mary.generateText(doc);
        Assert.assertNotNull(out);
    }

    @Test
    public void canProcessTokensToSpeech() throws Exception {
        MaryInterface mary = new LocalMaryInterface();
        mary.setInputType(MaryDataType.TOKENS.name());
        Document doc = getExampleTokens(mary.getLocale());
        AudioInputStream audio = mary.generateAudio(doc);
        Assert.assertNotNull(audio);
    }

    private Document getExampleTokens(Locale locale) throws ParserConfigurationException, SAXException, IOException {
        String example = MaryDataType.getExampleText(MaryDataType.TOKENS, locale);
        Assert.assertNotNull(example);
        Document doc = DomUtils.parseDocument(example);
        return doc;
    }

}
