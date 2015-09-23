/**
 * 
 */
package marytts.tools.analysis;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import marytts.client.MaryClient;
import marytts.config.LanguageConfig;
import marytts.exceptions.MaryConfigurationException;
import marytts.modules.phonemiser.AllophoneSet;
import marytts.signalproc.analysis.Labels;
import marytts.util.MaryUtils;
import marytts.util.data.text.PraatPitchTier;
import marytts.util.dom.DomUtils;

/**
 * Synthesize a given sentence with a given voice using a given server, to audio and to a PitchTier file reflecting the predicted
 * (ACOUSTPARAMS) intonation curve.
 * 
 * @author marc
 * 
 */
public class SynthesizeToPitchTier {

	private MaryClient mary;
	private String inputFormat;
	private String locale;
	private String voice;

	public SynthesizeToPitchTier(MaryClient mary, String inputFormat, String locale, String voice) {
		this.mary = mary;
		this.inputFormat = inputFormat;
		this.locale = locale;
		this.voice = voice;
	}

	private AllophoneSet getAllophoneSet() throws MaryConfigurationException {
		// TODO: make more generic
		return AllophoneSet.getAllophoneSet(getClass()
				.getResourceAsStream("/marytts/language/en_GB/lexicon/allophones.en_GB.xml"), "dummy");
	}

	public void synthAudioToFile(String input, String filename) throws IOException {
		OutputStream audioOutStream = new FileOutputStream(filename);
		try {
			mary.process(input, inputFormat, "AUDIO", locale, "WAVE", voice, audioOutStream);
		} finally {
			audioOutStream.close();
		}

	}

	public void synthPredictedPitchTier(String input, Labels reference, String filename) throws IOException {
		try {
			synthPitchTier(input, filename, "ACOUSTPARAMS", reference);
		} catch (Exception e) {
			throw new IOException("Cannot synthesize pitch tier", e);
		}
	}

	public void synthRealisedPitchTier(String input, String filename) throws IOException {
		try {
			synthPitchTier(input, filename, "REALISED_ACOUSTPARAMS", null);
		} catch (Exception e) {
			throw new IOException("Cannot synthesize pitch tier", e);
		}
	}

	private void synthPitchTier(String input, String filename, String outputFormat, Labels reference) throws IOException,
			ParserConfigurationException, SAXException, MaryConfigurationException {
		ByteArrayOutputStream acoustparamsData = new ByteArrayOutputStream();
		mary.process(input, "TEXT", outputFormat, locale, null, voice, acoustparamsData);
		Document acoustparams = DomUtils.parseDocument(new ByteArrayInputStream(acoustparamsData.toByteArray()));
		if (reference != null) {
			CopySynthesis copy = new CopySynthesis(getAllophoneSet());
			copy.imposeDurations(reference, acoustparams);
		}
		PraatPitchTier pitchTier = new PraatPitchTier(acoustparams);
		if (reference != null) {
			pitchTier.setXmin(0);
			pitchTier.setXmax(reference.items[reference.items.length - 1].time);
		}
		FileWriter pitchTierWriter = new FileWriter(filename);
		pitchTier.writeTo(pitchTierWriter);
		pitchTierWriter.close();
	}

	/**
	 * @param args
	 *            args
	 * @throws Exception
	 *             Exception
	 */
	public static void main(String[] args) throws Exception {
		MaryClient mary = MaryClient.getMaryClient();
		String locale = System.getProperty("locale");
		String voice = System.getProperty("voice");
		boolean stretchToReference = Boolean.getBoolean("stretch");
		boolean synthPredicted = Boolean.getBoolean("synthPredicted");
		boolean synthRealised = Boolean.getBoolean("synthRealised");
		boolean synthAudio = Boolean.getBoolean("synthAudio");

		SynthesizeToPitchTier synth = new SynthesizeToPitchTier(mary, "TEXT", locale, voice);
		for (String textfile : args) {
			String text = FileUtils.readFileToString(new File(textfile));
			String basename = StringUtils.removeEnd(textfile, ".txt");
			Labels reference = null;
			if (stretchToReference) {
				reference = new Labels(basename + ".lab");
			}

			String filename = basename + "." + voice;
			if (synthPredicted) {
				synth.synthPredictedPitchTier(text, reference, filename + ".predicted.PitchTier");
			}
			if (synthRealised) {
				synth.synthRealisedPitchTier(text, filename + ".realised.PitchTier");
			}
			if (synthAudio) {
				synth.synthAudioToFile(text, filename + ".wav");
			}
			System.out.println(basename);
		}

	}
}
