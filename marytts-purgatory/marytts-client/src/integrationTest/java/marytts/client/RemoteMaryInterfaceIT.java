package marytts.client;

import java.io.IOException;
import java.util.Locale;

import javax.sound.sampled.AudioInputStream;
import javax.xml.parsers.ParserConfigurationException;

import marytts.LocalMaryInterface;
import marytts.MaryInterface;
import marytts.datatypes.MaryDataType;
import marytts.exceptions.SynthesisException;
import marytts.features.FeatureDefinition;
import marytts.features.FeatureRegistry;
import marytts.server.http.MaryHttpServer;
import marytts.util.FeatureUtils;
import marytts.util.MaryRuntimeUtils;
import marytts.util.dom.DomUtils;
import marytts.voice.CmuSltHsmm.Config;

import org.testng.Assert;
import org.testng.annotations.*;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

public class RemoteMaryInterfaceIT {

	private static final int testPort = 59111;

	@BeforeClass
	public static void setupClass() throws Exception {
		MaryRuntimeUtils.ensureMaryStarted();
		System.getProperties().setProperty("socket.port", String.valueOf(testPort));
		MaryHttpServer maryserver = new MaryHttpServer();
		maryserver.start();
		waitForMaryServer(maryserver);
	}

	private static void waitForMaryServer(MaryHttpServer maryserver) throws IllegalStateException, InterruptedException {
		long start = System.currentTimeMillis();
		long threshold = 5000; // wait for up to 5 seconds
		while (!maryserver.isReady()) {
			Thread.sleep(50);
			if (System.currentTimeMillis() - start > threshold) {
				throw new IllegalStateException("Server not ready in time, aborting");
			}
		}
		// And now give the server just a bit more after it claims it's ready:
		Thread.sleep(50);

	}

	MaryInterface mary;

	@BeforeMethod
	public void setUp() throws Exception {
		mary = new RemoteMaryInterface("localhost", testPort);
	}

	@Test
	public void canGetMaryInterface() throws Exception {
		Assert.assertNotNull(mary);
		Assert.assertEquals("TEXT", mary.getInputType());
		Assert.assertEquals("AUDIO", mary.getOutputType());
		Assert.assertEquals(Locale.US, mary.getLocale());
	}

	@Test
	public void canSetInputType() throws Exception {
		String in = "RAWMARYXML";
		Assert.assertTrue(!in.equals(mary.getInputType()));
		mary.setInputType(in);
		Assert.assertEquals(in, mary.getInputType());
	}

	@Test(expectedExceptions = IllegalArgumentException.class)
	public void unknownInputType() throws Exception {
		mary.setInputType("something strange");
	}

	@Test(expectedExceptions = IllegalArgumentException.class)
	public void nullInputType() throws Exception {
		mary.setInputType(null);
	}

	@Test(expectedExceptions = IllegalArgumentException.class)
	public void notAnInputType() throws Exception {
		mary.setInputType("AUDIO");
	}

	@Test
	public void canSetOutputType() throws Exception {
		String out = "TOKENS";
		Assert.assertTrue(!out.equals(mary.getOutputType()));
		mary.setOutputType(out);
		Assert.assertEquals(out, mary.getOutputType());
	}

	@Test(expectedExceptions = IllegalArgumentException.class)
	public void unknownOutputType() throws Exception {
		mary.setOutputType("something strange");
	}

	@Test(expectedExceptions = IllegalArgumentException.class)
	public void nullOutputType() throws Exception {
		mary.setOutputType(null);
	}

	@Test(expectedExceptions = IllegalArgumentException.class)
	public void notAnOutputType() throws Exception {
		mary.setOutputType("TEXT");
	}

	@Test(expectedExceptions = IllegalArgumentException.class)
	public void cannotSetUnsupportedLocale() throws Exception {
		Locale loc = new Locale("abcde");
		mary.setLocale(loc);
	}

	@Test(expectedExceptions = IllegalArgumentException.class)
	public void cannotSetNullLocale() throws Exception {
		mary.setLocale(null);
	}

	@Test
	public void canProcessToTokens() throws Exception {
		// setup
		mary.setOutputType("TOKENS");
		// exercise
		Document tokens = mary.generateXML("Hello world");
		// verify
		Assert.assertNotNull(tokens);
	}

	@Test(expectedExceptions = IllegalArgumentException.class)
	public void refuseWrongInput1() throws Exception {
		// setup
		mary.setInputType(MaryDataType.RAWMARYXML.name());
		// method with string arg does not match declared input type:
		mary.generateXML("some text");
	}

	@Test(expectedExceptions = IllegalArgumentException.class)
	public void refuseWrongOutput1() throws Exception {
		// requesting xml output but set to default output type AUDIO:
		mary.generateXML("some text");
	}

	@Test(expectedExceptions = IllegalArgumentException.class)
	public void refuseWrongOutput2() throws Exception {
		// setup
		mary.setOutputType("TOKENS");
		// requesting audio putput but set to XML output type:
		mary.generateAudio("some text");
	}

	@Test(expectedExceptions = IllegalArgumentException.class)
	public void refuseWrongOutput3() throws Exception {
		// setup
		mary.setOutputType("TOKENS");
		// requesting text putput but set to XML output type:
		mary.generateText("some text");
	}

	@Test
	public void canProcessTokensToAllophones() throws Exception {
		// setup
		Document tokens = getExampleTokens();
		Assert.assertNotNull(tokens);
		mary.setInputType("TOKENS");
		mary.setOutputType("ALLOPHONES");
		// exercise
		Document allos = mary.generateXML(tokens);
		// verify
		Assert.assertNotNull(allos);
	}

	private Document getExampleTokens() throws SynthesisException {
		MaryInterface m;
		try {
			m = new RemoteMaryInterface("localhost", testPort);
		} catch (IOException e) {
			throw new SynthesisException("Cannot set up helper mary interface", e);
		}
		m.setInputType("TEXT");
		m.setOutputType("TOKENS");
		Document tokens = m.generateXML("Hello world");
		return tokens;
	}

	@Test
	public void convertTextToAcoustparams() throws Exception {
		mary.setOutputType("ACOUSTPARAMS");
		Document doc = mary.generateXML("Hello world");
		Assert.assertNotNull(doc);
	}

	@Test
	public void convertTextToTargetfeatures() throws Exception {
		mary.setOutputType("TARGETFEATURES");
		String tf = mary.generateText("Hello world");
		Assert.assertNotNull(tf);
	}

	@Test
	public void convertTokensToTargetfeatures() throws Exception {
		Document tokens = getExampleTokens();
		mary.setInputType("TOKENS");
		mary.setOutputType("TARGETFEATURES");
		String tf = mary.generateText(tokens);
		Assert.assertNotNull(tf);
	}

	@Test
	public void canSelectTargetfeatures() throws Exception {
		String input = "Hello world";
		mary.setOutputType("TARGETFEATURES");
		String allFeatures = mary.generateText(input);
		String featureNames = "phone stressed";
		mary.setOutputTypeParams(featureNames);
		String selectedFeatures = mary.generateText(input);
		Assert.assertTrue(!allFeatures.equals(selectedFeatures));
		Assert.assertTrue(allFeatures.length() > selectedFeatures.length());
	}

	@Test
	public void canProcessTextToSpeech() throws Exception {
		mary.setVoice("cmu-slt-hsmm");
		AudioInputStream audio = mary.generateAudio("Hello world");
		Assert.assertNotNull(audio);
	}

	@Test
	public void canProcessTokensToSpeech() throws Exception {
		mary.setInputType("TOKENS");
		Document doc = getExampleTokens();
		AudioInputStream audio = mary.generateAudio(doc);
		Assert.assertNotNull(audio);
	}

	@Test(expectedExceptions = IllegalArgumentException.class)
	public void cannotSetInvalidVoiceName() throws Exception {
		mary.setVoice("abcde");
	}

	@Test(expectedExceptions = IllegalArgumentException.class)
	public void cannotSetNullVoiceName() throws Exception {
		mary.setVoice(null);
	}

}
