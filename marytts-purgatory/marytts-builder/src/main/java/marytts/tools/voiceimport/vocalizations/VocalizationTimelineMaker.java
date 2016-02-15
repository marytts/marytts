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

package marytts.tools.voiceimport.vocalizations;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.SortedMap;
import java.util.TreeMap;

import marytts.tools.voiceimport.DatabaseLayout;
import marytts.tools.voiceimport.TimelineWriter;
import marytts.tools.voiceimport.VoiceImportComponent;
import marytts.tools.voiceimport.WavReader;
import marytts.util.data.Datagram;
import marytts.util.data.ESTTrackReader;
import marytts.util.io.BasenameList;

public class VocalizationTimelineMaker extends VoiceImportComponent {

	String vocalizationsDir;
	BasenameList bnlVocalizations;

	protected DatabaseLayout db = null;
	protected int percent = 0;
	public final String WAVETIMELINE = "VocalizationTimelineMaker.waveTimeline";
	public final String WAVEDIR = "VocalizationTimelineMaker.inputWaveDir";
	public final String PMARKDIR = "VocalizationTimelineMaker.pitchmarkDir";

	public final String PMDIR = "db.pmDir";
	public final String PMEXT = "db.pmExtension";

	public String getName() {
		return "VocalizationTimelineMaker";
	}

	@Override
	protected void initialiseComp() {
		String timelineDir = db.getProp(db.VOCALIZATIONSDIR) + File.separator + "files";
		if (!(new File(timelineDir)).exists()) {

			System.out.println("vocalizations/files directory does not exist; ");
			if (!(new File(timelineDir)).mkdirs()) {
				throw new Error("Could not create vocalizations/files");
			}
			System.out.println("Created successfully.\n");

		}

		try {
			String basenameFile = db.getProp(db.VOCALIZATIONSDIR) + File.separator + "basenames.lst";
			if ((new File(basenameFile)).exists()) {
				System.out.println("Loading basenames of vocalizations from '" + basenameFile + "' list...");
				bnlVocalizations = new BasenameList(basenameFile);
				System.out.println("Found " + bnlVocalizations.getLength() + " vocalizations in basename list");
			} else {
				String vocalWavDir = db.getProp(db.VOCALIZATIONSDIR) + File.separator + "wav";
				System.out.println("Loading basenames of vocalizations from '" + vocalWavDir + "' directory...");
				bnlVocalizations = new BasenameList(vocalWavDir, ".wav");
				System.out.println("Found " + bnlVocalizations.getLength() + " vocalizations in " + vocalWavDir + " directory");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public SortedMap getDefaultProps(DatabaseLayout db) {
		this.db = db;
		if (props == null) {
			props = new TreeMap();
			props.put(WAVEDIR, db.getProp(db.VOCALIZATIONSDIR) + File.separator + "wav");
			props.put(PMARKDIR, db.getProp(db.VOCALIZATIONSDIR) + File.separator + "pm");
			props.put(WAVETIMELINE, db.getProp(db.VOCALIZATIONSDIR) + File.separator + "files" + File.separator
					+ "vocalization_wave_timeline" + db.getProp(db.MARYEXT));
			// vocalizationsDir = db.getProp(db.ROOTDIR)+File.separator+"vocalizations";
		}
		return props;
	}

	protected void setupHelp() {
		props2Help = new TreeMap();
		props2Help.put(WAVETIMELINE, "file containing all wave files. Will be created by this module");
	}

	/**
	 * Reads and concatenates a list of waveforms into one single timeline file.
	 *
	 */
	public boolean compute() {
		System.out.println("---- Making a pitch synchronous waveform timeline\n\n");

		/* Export the basename list into an array of strings */
		String[] baseNameArray = bnlVocalizations.getListAsArray();
		System.out.println("Processing [" + baseNameArray.length + "] utterances.\n");

		try {
			/*
			 * 1) Determine the reference sampling rate as being the sample rate of the first encountered wav file
			 */
			WavReader wav = new WavReader(getProp(WAVEDIR) + baseNameArray[0] + db.getProp(db.WAVEXT));
			int globSampleRate = wav.getSampleRate();
			System.out.println("---- Detected a global sample rate of: [" + globSampleRate + "] Hz.");

			System.out.println("---- Folding the wav files according to the pitchmarks...");

			/* 2) Open the destination timeline file */

			/* Make the file name */
			String waveTimelineName = getProp(WAVETIMELINE);
			System.out.println("Will create the waveform timeline in file [" + waveTimelineName + "].");

			/* Processing header: */
			String processingHeader = "\n";

			/* Instantiate the TimelineWriter: */
			TimelineWriter waveTimeline = new TimelineWriter(waveTimelineName, processingHeader, globSampleRate, 0.1);

			/* 3) Write the datagrams and feed the index */

			float totalDuration = 0.0f; // Accumulator for the total timeline duration
			long totalTime = 0l;
			int numDatagrams = 0;

			/* For each EST track file: */
			ESTTrackReader pmFile = null;
			for (int i = 0; i < baseNameArray.length; i++) {
				percent = 100 * i / baseNameArray.length;

				/* - open+load */
				System.out.println(baseNameArray[i]);
				pmFile = new ESTTrackReader(getProp(PMARKDIR) + baseNameArray[i] + db.getProp(PMEXT));
				totalDuration += pmFile.getTimeSpan();
				wav = new WavReader(getProp(WAVEDIR) + baseNameArray[i] + db.getProp(db.WAVEXT));
				short[] wave = wav.getSamples();
				/* - Reset the frame locations in the local file */
				int frameStart = 0;
				int frameEnd = 0;
				int duration = 0;
				long localTime = 0l;
				/* - For each frame in the WAV file: */
				for (int f = 0; f < pmFile.getNumFrames(); f++) {

					/* Locate the corresponding segment in the wave file */
					frameStart = frameEnd;
					frameEnd = (int) ((double) pmFile.getTime(f) * (double) (globSampleRate));
					assert frameEnd <= wave.length : "Frame ends after end of wave data: " + frameEnd + " > " + wave.length;

					duration = frameEnd - frameStart;
					ByteArrayOutputStream buff = new ByteArrayOutputStream(2 * duration);
					DataOutputStream subWave = new DataOutputStream(buff);
					for (int k = 0; k < duration; k++) {
						subWave.writeShort(wave[frameStart + k]);
					}

					// Handle the case when the last pitch marks falls beyond the end of the signal

					/* Feed the datagram to the timeline */
					waveTimeline.feed(new Datagram(duration, buff.toByteArray()), globSampleRate);
					totalTime += duration;
					localTime += duration;
					numDatagrams++;
				}
				// System.out.println( baseNameArray[i] + " -> pm file says [" + localTime + "] samples, wav file says ["+
				// wav.getNumSamples() + "] samples." );
			}
			waveTimeline.close();

			System.out.println("---- Done.");

			/* 7) Print some stats and close the file */
			System.out.println("---- Waveform timeline result:");
			System.out.println("Number of files scanned: " + baseNameArray.length);
			System.out.println("Total speech duration: [" + totalTime + "] samples / ["
					+ ((float) (totalTime) / (float) (globSampleRate)) + "] seconds.");
			System.out.println("(Speech duration approximated from EST Track float times: [" + totalDuration + "] seconds.)");
			System.out.println("Number of frames: [" + numDatagrams + "].");
			System.out.println("Size of the index: [" + waveTimeline.getIndex().getNumIdx() + "] ("
					+ (waveTimeline.getIndex().getNumIdx() * 16) + " bytes, i.e. "
					+ new DecimalFormat("#.##").format((double) (waveTimeline.getIndex().getNumIdx()) * 16.0 / 1048576.0)
					+ " megs).");
			System.out.println("---- Waveform timeline done.");

		} catch (SecurityException e) {
			System.err.println("Error: you don't have write access to the target database directory.");
			e.printStackTrace();
			return false;
		} catch (Exception e) {
			e.printStackTrace();
			System.err.println(e);
			return false;
		}

		return (true);
	}

	/**
	 * Provide the progress of computation, in percent, or -1 if that feature is not implemented.
	 * 
	 * @return -1 if not implemented, or an integer between 0 and 100.
	 */
	public int getProgress() {
		return percent;
	}

	/**
	 * @param args
	 *            args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
