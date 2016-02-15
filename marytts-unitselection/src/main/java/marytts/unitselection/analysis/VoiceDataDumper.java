/**
 * Copyright 2010 DFKI GmbH.
 * All Rights Reserved.  Use is subject to license terms.
 *
 * This file is part of MARY TTS.
 *
 * MARY TTS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package marytts.unitselection.analysis;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.BufferUnderflowException;

import marytts.exceptions.MaryConfigurationException;
import marytts.features.FeatureDefinition;
import marytts.features.FeatureVector;
import marytts.server.MaryProperties;
import marytts.unitselection.data.FeatureFileReader;
import marytts.unitselection.data.TimelineReader;
import marytts.unitselection.data.Unit;
import marytts.unitselection.data.UnitDatabase;
import marytts.unitselection.data.UnitFileReader;
import marytts.util.data.Datagram;
import marytts.util.data.text.PraatInterval;
import marytts.util.data.text.PraatIntervalTier;
import marytts.util.data.text.PraatTextGrid;

/**
 * Convenience class to dump relevant data from a unit selection voice to a Praat TextGrid and a wav file for inspection of
 * timeline data in external tools (e.g. Praat, WaveSurfer, etc.)
 * 
 * @author steiner
 * 
 */
public class VoiceDataDumper {
	protected UnitDatabase unitDB;

	protected FeatureFileReader featureFileReader;

	protected long numSamples = 0;

	protected FeatureDefinition featureDefinition;

	protected int phoneFeatureIndex;

	protected int halfphoneLRFeatureIndex;

	public VoiceDataDumper() {

	}

	/**
	 * @see marytts.util.data.audio.WavWriter#byteswap(int)
	 * @param val
	 *            val
	 * @return (((val &amp; 0xff000000) &gt;&gt;&gt; 24) + ((val &amp; 0x00ff0000) &gt;&gt;&gt; 8) + ((val &amp; 0x0000ff00)
	 *         &lt;&lt; 8) + ((val &amp; 0x000000ff) &lt;&lt; 24))
	 */
	protected int byteswap(int val) {
		return (((val & 0xff000000) >>> 24) + ((val & 0x00ff0000) >>> 8) + ((val & 0x0000ff00) << 8) + ((val & 0x000000ff) << 24));
	}

	/**
	 * @see marytts.util.data.audio.WavWriter#byteswap(short)
	 * @param val
	 *            val
	 * @return ((short) ((((int) (val) &amp; 0xff00) &gt;&gt;&gt; 8) + (((int) (val) &amp; 0x00ff) &lt;&lt; 8)))
	 */
	protected short byteswap(short val) {
		return ((short) ((((int) (val) & 0xff00) >>> 8) + (((int) (val) & 0x00ff) << 8)));
	}

	/**
	 * Load audio timeline from file
	 * 
	 * @param fileName
	 *            to load
	 * @return TimelineReader
	 * @throws IOException
	 *             IOException
	 * @throws MaryConfigurationException
	 *             MaryConfigurationException
	 */
	protected TimelineReader loadAudioTimeline(String fileName) throws IOException, MaryConfigurationException {
		return new TimelineReader(fileName);
	}

	/**
	 * Load unit database from various relevant files
	 * 
	 * @param audioTimelineFileName
	 *            to load
	 * @param basenameTimelineFileName
	 *            to load
	 * @param unitFileName
	 *            to load
	 * @throws IOException
	 *             IOException
	 * @throws MaryConfigurationException
	 *             MaryConfigurationException
	 */
	protected void loadUnitDatabase(String audioTimelineFileName, String basenameTimelineFileName, String unitFileName)
			throws IOException, MaryConfigurationException {
		unitDB = new UnitDatabase();
		UnitFileReader unitFileReader = new UnitFileReader(unitFileName);
		TimelineReader audioTimelineReader = loadAudioTimeline(audioTimelineFileName);
		TimelineReader basenameTimelineReader = new TimelineReader(basenameTimelineFileName);
		unitDB.load(null, null, unitFileReader, null, audioTimelineReader, basenameTimelineReader, 0);
	}

	/**
	 * Load unit feature file from file
	 * 
	 * @param fileName
	 *            to load
	 * @throws IOException
	 *             IOException
	 * @throws MaryConfigurationException
	 *             MaryConfigurationException
	 */
	protected void loadFeatureFile(String fileName) throws IOException, MaryConfigurationException {
		featureFileReader = new FeatureFileReader(fileName);
		featureDefinition = featureFileReader.getFeatureDefinition();
		phoneFeatureIndex = featureDefinition.getFeatureIndex("phone");
		halfphoneLRFeatureIndex = featureDefinition.getFeatureIndex("halfphone_lr");
	}

	/**
	 * Get total duration of a Datagram array
	 * 
	 * @param datagrams
	 *            whose duration to get
	 * @return total duration in seconds
	 */
	protected double getDuration(Datagram[] datagrams) {
		double totalDuration = 0;
		for (Datagram datagram : datagrams) {
			totalDuration += datagram.getDuration() / (float) unitDB.getAudioTimeline().getSampleRate();
		}
		return totalDuration;
	}

	/**
	 * Get raw samples from all Datagrams in an array
	 * 
	 * @param datagrams
	 *            whose samples to get
	 * @return raw samples as stored in the Datagrams
	 * @throws IOException
	 *             IOException
	 */
	protected byte[] getSamples(Datagram[] datagrams) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		for (Datagram datagram : datagrams) {
			byte[] data = datagram.getData();
			baos.write(data);
		}
		byte[] samples = baos.toByteArray();
		return samples;
	}

	/**
	 * Dump units to Praat TextGrid. This will have three tiers:
	 * <ol>
	 * <li>halfphone units, labeled with unit indices;</li>
	 * <li>phone units, labeled with allophones;</li>
	 * <li>basenames, labeled with basename of original utterance.</li>
	 * </ol>
	 * 
	 * @param fileName
	 *            of new TextGrid
	 * @throws IOException
	 *             if data files cannot be read, or TextGrid cannot be written
	 */
	protected void dumpTextGrid(String fileName) throws IOException {
		// init the tiers:
		PraatIntervalTier unitTier = new PraatIntervalTier("unitindex");
		PraatIntervalTier phoneTier = new PraatIntervalTier("halfphone");
		PraatIntervalTier basenameTier = new PraatIntervalTier("basename");

		// init some variables:
		double prevHalfPhoneUnitDurationInSeconds = 0;
		double basenameDurationInSeconds = 0;
		String basenameLabel = null;

		// iterate over all units:
		for (int unitIndex = 0; unitIndex < unitDB.getUnitFileReader().getNumberOfUnits(); unitIndex++) {
			// if (unitIndex > 727) {
			// break;
			// }
			Unit unit = unitDB.getUnitFileReader().getUnit(unitIndex);
			if (unit.isEdgeUnit()) {
				// if this is the left edge, basenameDurationInSeconds will be 0
				if (basenameDurationInSeconds > 0) {
					// add basename interval
					PraatInterval basenameInterval = new PraatInterval(basenameDurationInSeconds, basenameLabel);
					basenameTier.appendInterval(basenameInterval);
					basenameDurationInSeconds = 0;
				}
				continue; // ignore edge units (also, avoid ticket:335)
			}

			// iterate over datagrams to get exact duration:
			Datagram[] datagrams;
			try {
				datagrams = unitDB.getAudioTimeline().getDatagrams(unit, unitDB.getAudioTimeline().getSampleRate());
			} catch (BufferUnderflowException e) {
				throw e;
			}
			double halfPhoneUnitDurationInSeconds = getDuration(datagrams);
			// cumulative sample count for wav file header:
			byte[] buf = getSamples(datagrams);
			numSamples += buf.length;

			// keep track of basename duration and label:
			basenameDurationInSeconds += halfPhoneUnitDurationInSeconds;
			basenameLabel = unitDB.getFilename(unit);

			// halfphone unit interval (labeled with unit index):
			PraatInterval interval = new PraatInterval(halfPhoneUnitDurationInSeconds, Integer.toString(unit.index));
			unitTier.appendInterval(interval);

			// lazy way of checking that we have both halves of the phone:
			FeatureVector features = featureFileReader.getFeatureVector(unit);
			String halfphoneLR = features.getFeatureAsString(halfphoneLRFeatureIndex, featureDefinition);
			if (halfphoneLR.equals("R")) {
				// phone interval:
				double phoneUnitDurationInSeconds = halfPhoneUnitDurationInSeconds + prevHalfPhoneUnitDurationInSeconds;
				String phoneLabel = features.getFeatureAsString(phoneFeatureIndex, featureDefinition);
				PraatInterval phoneInterval = new PraatInterval(phoneUnitDurationInSeconds, phoneLabel);
				phoneTier.appendInterval(phoneInterval);
			}
			prevHalfPhoneUnitDurationInSeconds = halfPhoneUnitDurationInSeconds;
		}

		// update time domains:
		unitTier.updateBoundaries();
		phoneTier.updateBoundaries();
		basenameTier.updateBoundaries();

		// create TextGrid:
		PraatTextGrid textGrid = new PraatTextGrid();
		textGrid.appendTier(unitTier);
		textGrid.appendTier(phoneTier);
		textGrid.appendTier(basenameTier);

		// write to text file:
		BufferedWriter output = new BufferedWriter(new PrintWriter(fileName));
		output.write(textGrid.toString());
		output.close();
	}

	/**
	 * Adapted from {@link marytts.util.data.audio.WavWriter#export(String, int, byte[])} and
	 * {@link marytts.util.data.audio.WavWriter#doWrite(String, int)}
	 * 
	 * @param fileName
	 *            fileName
	 * @throws IOException
	 *             IOException
	 */
	protected void dumpAudio(String fileName) throws IOException {
		// refuse to write wav file if we don't know how many samples there are:
		if (!(numSamples > 0)) {
			return;
		}

		// open wav file, and write header:
		DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(fileName)));
		int nBytesPerSample = 2;
		dos.writeBytes("RIFF"); // "RIFF" in ascii
		dos.writeInt(byteswap((int) (36 + numSamples))); // Chunk size
		dos.writeBytes("WAVEfmt ");
		dos.writeInt(byteswap(16)); // chunk size, 16 for PCM
		dos.writeShort(byteswap((short) 1)); // PCM format
		dos.writeShort(byteswap((short) 1)); // Mono, one channel
		dos.writeInt(byteswap(unitDB.getAudioTimeline().getSampleRate())); // Samplerate
		dos.writeInt(byteswap(unitDB.getAudioTimeline().getSampleRate() * nBytesPerSample)); // Byte-rate
		dos.writeShort(byteswap((short) (nBytesPerSample))); // Nbr of bytes per samples x nbr of channels
		dos.writeShort(byteswap((short) (nBytesPerSample * 8))); // nbr of bits per sample
		dos.writeBytes("data");
		dos.writeInt(byteswap((int) numSamples));

		// implicitly unit-wise buffered writing of samples:
		for (int unitIndex = 0; unitIndex < unitDB.getUnitFileReader().getNumberOfUnits(); unitIndex++) {
			Unit unit = unitDB.getUnitFileReader().getUnit(unitIndex);
			if (unit.isEdgeUnit()) {
				continue; // ignore edge units (also, avoid ticket:335)
			}

			Datagram[] datagrams = unitDB.getAudioTimeline().getDatagrams(unit, unitDB.getAudioTimeline().getSampleRate());
			byte[] buf = getSamples(datagrams);

			// write buffer to file:
			// Byte-swap the samples
			byte b = 0;
			for (int j = 0; j < buf.length - 1; j += 2) {
				b = buf[j];
				try {
					buf[j] = buf[j + 1];
				} catch (ArrayIndexOutOfBoundsException e) {
					throw e;
				}
				buf[j + 1] = b;
			}
			dos.write(buf);
		}

		dos.close();
	}

	/**
	 * Get file names from voice config file. Dump relevant data from audio timeline, unit file, etc. to Praat TextGrid and wav
	 * file.
	 * 
	 * @param voiceName
	 *            for config file to read (e.g. "bits3")
	 * @throws Exception
	 *             Exception
	 */
	protected void dumpData(String voiceName) throws Exception {

		String audioTimelineFileName = MaryProperties.needFilename("voice." + voiceName + ".audioTimelineFile");
		String basenameTimelineFileName = MaryProperties.needFilename("voice." + voiceName + ".basenameTimeline");
		String unitFileName = MaryProperties.needFilename("voice." + voiceName + ".unitsFile");
		String featureFileName = MaryProperties.needFilename("voice." + voiceName + ".featureFile");
		String textGridFilename = audioTimelineFileName.replace(".mry", ".TextGrid");
		String wavFilename = audioTimelineFileName.replace(".mry", ".wav");

		loadUnitDatabase(audioTimelineFileName, basenameTimelineFileName, unitFileName);
		loadFeatureFile(featureFileName);
		System.out.println("All files loaded.");
		dumpTextGrid(textGridFilename);
		System.out.println("Dumped TextGrid to " + textGridFilename);
		dumpAudio(wavFilename);
		System.out.println("Dumped audio to " + wavFilename);
	}

	/**
	 * Main method. Add VOICE jar to classpath, then call with
	 * 
	 * <pre>
	 * -ea -Xmx1gb -Dmary.base=$MARYBASE VOICE
	 * </pre>
	 * 
	 * or something similar
	 * 
	 * @param args
	 *            voice name (without the Locale) of voice to dump data from
	 * @throws Exception
	 *             Exception
	 */
	public static void main(String[] args) throws Exception {
		new VoiceDataDumper().dumpData(args[0]);
	}

}
