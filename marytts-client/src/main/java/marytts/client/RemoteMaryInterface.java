package marytts.client;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.Vector;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import org.w3c.dom.Document;

import marytts.MaryInterface;
import marytts.client.MaryClient.DataType;
import marytts.client.MaryClient.Voice;
import marytts.exceptions.SynthesisException;
import marytts.util.dom.DomUtils;
import marytts.util.http.Address;

public class RemoteMaryInterface implements MaryInterface {
	private MaryClient client;
	private DataType inputType;
	private DataType outputType;
	private Locale locale;
	private String voiceName;
	private String effects;
	private String style;
	private String outputTypeParams;
	private boolean isStreaming;
	
	public RemoteMaryInterface() throws IOException {
		client = MaryClient.getMaryClient();
		init();
	}
	
	public RemoteMaryInterface(String serverHost, int serverPort) throws IOException {
		client = MaryClient.getMaryClient(new Address(serverHost, serverPort));
		init();
	}
	
	private void init() throws IOException {
		setReasonableDefaults();
	}
	
	private void setReasonableDefaults() throws IOException {
		inputType = new DataType("TEXT", true, false);
		outputType = new DataType("AUDIO", false, true);
		locale = Locale.US;
		voiceName = client.getVoices(locale).firstElement().name();
		effects = null;
		style = null;
		outputTypeParams = null;
		isStreaming = false;
	}
	
	@Override
	public void setInputType(String newInputType) throws SynthesisException {
		Vector<DataType> allInputTypes;
		try {
			allInputTypes = client.getInputDataTypes();
		} catch (IOException e) {
			throw new SynthesisException("Cannot get input data types from server", e);
		}
		for (DataType d : allInputTypes) {
			if (d.name().equals(newInputType)) {
				inputType = d;
				return;
			}
		}
		throw new SynthesisException("No such input type: "+newInputType);
	}

	@Override
	public String getInputType() {
		return inputType.name();
	}

	@Override
	public void setOutputType(String newOutputType) throws SynthesisException {
		Vector<DataType> allOutputTypes;
		try {
			allOutputTypes = client.getOutputDataTypes();
		} catch (IOException e) {
			throw new SynthesisException("Cannot get output data types from server", e);
		}
		for (DataType d : allOutputTypes) {
			if (d.name().equals(newOutputType)) {
				outputType = d;
				return;
			}
		}
		throw new SynthesisException("No such output type: "+newOutputType);
	}

	@Override
	public String getOutputType() {
		return outputType.name();
	}

	@Override
	public void setLocale(Locale newLocale) throws SynthesisException {
		Set<Locale> supportedLocales;
		try {
			supportedLocales = client.getLocales();
		} catch (IOException e) {
			throw new SynthesisException("Cannot get list of supported locales from server", e);
		}
		if (!supportedLocales.contains(newLocale)) {
			throw new SynthesisException("Unsupported locale: "+newLocale);
		}
		locale = newLocale;
		Vector<Voice> voicesForLocale;
		try {
			 voicesForLocale = client.getVoices(locale);
		} catch (IOException e) {
			throw new SynthesisException("Cannot get list of voices for locale from server", e);
		}
		if (voicesForLocale.size() > 0) {
			voiceName = voicesForLocale.get(0).name();
		} else {
			voiceName = null;
		}
		
		
	}

	@Override
	public Locale getLocale() {
		return locale;
	}

	@Override
	public void setVoice(String newVoiceName) throws SynthesisException {
		try {
			for (Voice v : client.getVoices()) {
				if (v.name().equals(newVoiceName)) {
					voiceName = newVoiceName;
					locale = v.getLocale();
					return;
				}
			}
		} catch (IOException e) {
			throw new SynthesisException("Cannot get list of voices from server", e);
		}
		throw new IllegalArgumentException("Not a valid voice name: "+newVoiceName);
	}

	@Override
	public String getVoice() {
		return voiceName;
	}

	@Override
	public void setAudioEffects(String audioEffects) {
		effects = audioEffects;
	}

	@Override
	public String getAudioEffects() {
		return effects;
	}

	@Override
	public void setStyle(String newStyle) {
		style = newStyle;
	}

	@Override
	public String getStyle() {
		return style;
	}

	@Override
	public void setOutputTypeParams(String params) {
		outputTypeParams = params;
	}

	@Override
	public String getOutputTypeParams() {
		return outputTypeParams;
	}

	@Override
	public void setStreamingAudio(boolean newIsStreaming) {
		isStreaming = newIsStreaming;
		if (isStreaming) {
			throw new RuntimeException("Streaming audio not yet implemented in this interface");
		}
	}

	@Override
	public boolean isStreamingAudio() {
		return isStreaming;
	}

	@Override
	public String generateText(String text) throws SynthesisException {
		verifyInputTypeIsText();
		verifyOutputTypeIsText();
		try {
			byte[] result = processStringToBytes(text);
			return new String(result, "UTF-8");
		} catch (Exception ioe) {
			throw new SynthesisException(ioe);
		}
	}

	@Override
	public String generateText(Document doc) throws SynthesisException {
		verifyInputTypeIsXML();
		verifyOutputTypeIsText();
		try {
			String xmlAsString = DomUtils.document2String(doc);
			byte[] result = processStringToBytes(xmlAsString);
			return new String(result, "UTF-8");
		} catch (Exception ioe) {
			throw new SynthesisException(ioe);
		}
	}

	@Override
	public Document generateXML(String text) throws SynthesisException {
		verifyInputTypeIsText();
		verifyOutputTypeIsXML();
		try {
			byte[] result = processStringToBytes(text);
			return DomUtils.parseDocument(new ByteArrayInputStream(result));
		} catch (Exception ioe) {
			throw new SynthesisException(ioe);
		}
	}

	@Override
	public Document generateXML(Document doc) throws SynthesisException {
		verifyInputTypeIsXML();
		verifyOutputTypeIsXML();
		try {
			String xmlAsString = DomUtils.document2String(doc);
			byte[] result = processStringToBytes(xmlAsString);
			return DomUtils.parseDocument(new ByteArrayInputStream(result));
		} catch (Exception ioe) {
			throw new SynthesisException(ioe);
		}
	}
	

	@Override
	public AudioInputStream generateAudio(String text)
			throws SynthesisException {
		verifyInputTypeIsText();
		verifyOutputTypeIsAudio();
		try {
			byte[] result = processStringToBytes(text);
			return AudioSystem.getAudioInputStream(new ByteArrayInputStream(result));
		} catch (Exception ioe) {
			throw new SynthesisException(ioe);
		}
	}

	@Override
	public AudioInputStream generateAudio(Document doc)
			throws SynthesisException {
		verifyInputTypeIsXML();
		verifyOutputTypeIsAudio();
		try {
			String xmlAsString = DomUtils.document2String(doc);
			byte[] result = processStringToBytes(xmlAsString);
			return AudioSystem.getAudioInputStream(new ByteArrayInputStream(result));
		} catch (Exception ioe) {
			throw new SynthesisException(ioe);
		}
	}

	private byte[] processStringToBytes(String input) throws Exception {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		client.process(input, inputType.name(), outputType.name(), locale.toString(), "WAVE", voiceName, style, effects, outputTypeParams, baos, 0);
		return baos.toByteArray();
	}

	private static final Set<String> KNOWN_TEXT_TYPES = new HashSet<String>(
			Arrays.asList(new String[] { 
					"HALFPHONE_TARGETFEATURES", "HTSCONTEXT", "MBROLA",
					"PRAAT_TEXTGRID", "REALISED_DURATIONS", "SIMPLEPHONEMES",
					"TARGETFEATURES", "TEXT"
		}));
	
	private static final Set<String> KNOWN_XML_TYPES = new HashSet<String>(
		Arrays.asList(new String[] { 
				"ACOUSTPARAMS", "ALLOPHONES", "APML", "DURATIONS", "INTONATION",
				"PARTSOFSPEECH", "PHONEMES", "RAWMARYXML", 
				"REALISED_ACOUSTPARAMS", "SABLE", "SSML", "TOKENS", "WORDS" 
	}));

	private static final Set<String> KNOWN_AUDIO_TYPES = new HashSet<String>(
			Arrays.asList(new String[] { 
					"AUDIO"
		}));

	private void verifyOutputTypeIsXML() {
		// Approximate verification: we catch things we know cannot be right
		if (KNOWN_TEXT_TYPES.contains(outputType.name())
				|| KNOWN_AUDIO_TYPES.contains(outputType.name())) {
			throw new IllegalArgumentException("Cannot provide XML output for non-XML-based output type "+outputType);
		}
	}

	private void verifyInputTypeIsXML() {
		// Approximate verification: we catch things we know cannot be right
		if (KNOWN_TEXT_TYPES.contains(inputType.name())
				|| KNOWN_AUDIO_TYPES.contains(inputType.name())) {
			throw new IllegalArgumentException("Cannot provide XML input for non-XML-based input type "+inputType);
		}
	}
	
	private void verifyInputTypeIsText() {
		// Approximate verification: we catch things we know cannot be right
		if (KNOWN_XML_TYPES.contains(inputType.name())
				|| KNOWN_AUDIO_TYPES.contains(inputType.name())) {
			throw new IllegalArgumentException("Cannot provide plain-text input for XML-based input type "+inputType);
		}
	}

	private void verifyOutputTypeIsAudio() {
		if (!outputType.name().equals("AUDIO")) {
			throw new IllegalArgumentException("Cannot provide audio output for non-audio output type "+outputType);
		}
	}

	private void verifyOutputTypeIsText() {
		// Approximate verification: we catch things we know cannot be right
		if (KNOWN_XML_TYPES.contains(outputType.name())
				|| KNOWN_AUDIO_TYPES.contains(outputType.name())) {
			throw new IllegalArgumentException("Cannot provide text output for non-text output type "+outputType);
		}
	}
}
