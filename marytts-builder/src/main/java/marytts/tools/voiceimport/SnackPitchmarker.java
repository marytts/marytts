/**
 * Copyright 2007 DFKI GmbH.
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
package marytts.tools.voiceimport;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.SortedMap;
import java.util.TreeMap;

import marytts.signalproc.analysis.PitchMarks;
import marytts.util.io.StreamGobbler;
import marytts.util.MaryUtils;
import marytts.util.data.ESTTrackWriter;
import marytts.util.data.text.SnackTextfileDoubleDataSource;
import marytts.util.signal.SignalProcUtils;

public class SnackPitchmarker extends VoiceImportComponent {
	protected DatabaseLayout db = null;
	// protected String correctedPmExt = ".pm.corrected";
	protected String snackPmExt = ".snack";
	protected String scriptFileName;

	private int percent = 0;

	public final String MINPITCH = "SnackPitchmarker.minPitch";
	public final String MAXPITCH = "SnackPitchmarker.maxPitch";
	public final String COMMAND = "SnackPitchmarker.command";

	public final String PMDIR = "db.pmDir";
	public final String PMEXT = "db.pmExtension";

	protected void setupHelp() {
		if (props2Help == null) {
			props2Help = new TreeMap();
			props2Help.put(MINPITCH, "minimum value for the pitch (in Hz). Default: female 100, male 75");
			props2Help.put(MAXPITCH, "maximum value for the pitch (in Hz). Default: female 500, male 300");
			props2Help
					.put(COMMAND, "the command that is used to launch Tcl; the Tcl installation must provide the SNACK package");
		}
	}

	public final String getName() {
		return "SnackPitchmarker";
	}

	@Override
	protected void initialiseComp() {
		scriptFileName = db.getProp(db.TEMPDIR) + "pm.tcl";
	}

	public SortedMap getDefaultProps(DatabaseLayout db) {
		this.db = db;
		if (props == null) {
			props = new TreeMap();
			if (db.getProp(db.GENDER).equals("female")) {
				props.put(MINPITCH, "100");
				props.put(MAXPITCH, "500");
			} else {
				props.put(MINPITCH, "75");
				props.put(MAXPITCH, "300");
			}
			if (MaryUtils.isWindows())
				props.put(COMMAND, "c:/tcl/tclsh.exe"); // TODO someone with windows, please confirm or correct
			else
				props.put(COMMAND, "/usr/bin/tclsh");

		}
		return props;
	}

	/**
	 * The standard compute() method of the VoiceImportComponent interface.
	 * 
	 * @throws Exception
	 *             Exception
	 */
	public boolean compute() throws Exception {

		File script = new File(scriptFileName);

		// What is the purpose of trunk/marytts/tools/voiceimport/pm.tcl, if it's hardcoded below here?
		if (script.exists())
			script.delete();
		PrintWriter toScript = new PrintWriter(new FileWriter(script));
		toScript.println("#!" + getProp(COMMAND));
		toScript.println(" ");
		toScript.println("package require snack");
		toScript.println(" ");
		toScript.println("snack::sound s");
		toScript.println(" ");
		toScript.println("s read [lindex $argv 0]");
		toScript.println(" ");
		toScript.println("set fd [open [lindex $argv 1] w]");
		toScript.println("puts $fd [join [s pitch -method esps -maxpitch [lindex $argv 2] -minpitch [lindex $argv 3]] \\n]");
		toScript.println("close $fd");
		toScript.println(" ");
		toScript.println("exit");
		toScript.println(" ");
		toScript.close();

		String[] baseNameArray = bnl.getListAsArray();
		System.out.println("Computing pitchmarks for " + baseNameArray.length + " utterances.");

		/* Ensure the existence of the target pitchmark directory */
		File dir = new File(db.getProp(PMDIR));
		if (!dir.exists()) {
			System.out.println("Creating the directory [" + db.getProp(PMDIR) + "].");
			dir.mkdir();
		}

		/* execute snack */
		for (int i = 0; i < baseNameArray.length; i++) {
			percent = 100 * i / baseNameArray.length;
			String wavFile = db.getProp(db.WAVDIR) + baseNameArray[i] + db.getProp(db.WAVEXT);
			String snackFile = db.getProp(PMDIR) + baseNameArray[i] + snackPmExt;
			String pmFile = db.getProp(PMDIR) + baseNameArray[i] + db.getProp(PMEXT);
			// String correctedPmFile = getProp(PMDIR) + baseNameArray[i] + correctedPmExt;
			System.out.println("Writing pm file to " + snackFile);

			boolean isWindows = true; // TODO This is WRONG, and never used. Consider removal.
			String strTmp = scriptFileName + " " + wavFile + " " + snackFile + " " + getProp(MAXPITCH) + " " + getProp(MINPITCH);

			if (MaryUtils.isWindows())
				strTmp = "cmd.exe /c " + strTmp;
			else
				strTmp = getProp(COMMAND) + " " + strTmp;

			// System.out.println("strTmp: "+strTmp);
			Process snack = Runtime.getRuntime().exec(strTmp);

			StreamGobbler errorGobbler = new StreamGobbler(snack.getErrorStream(), "err");

			// read from output stream
			StreamGobbler outputGobbler = new StreamGobbler(snack.getInputStream(), "out");

			// start reading from the streams
			errorGobbler.start();
			outputGobbler.start();

			// close everything down
			snack.waitFor();
			snack.exitValue();

			// Now convert the snack format into EST pm format
			double[] pm = new SnackTextfileDoubleDataSource(new FileReader(snackFile)).getAllData();

			WavReader wf = new WavReader(wavFile);
			int sampleRate = wf.getSampleRate();
			PitchMarks snackPitchmarker = SignalProcUtils.pitchContour2pitchMarks(pm, sampleRate, wf.getNumSamples(), 0.0075,
					0.01, false, 0);
			int[] pitchmarkSamples = snackPitchmarker.pitchMarks;

			float[] pitchmarkSeconds = new float[pitchmarkSamples.length];
			for (int j = 0; j < pitchmarkSeconds.length; j++) {
				pitchmarkSeconds[j] = (float) pitchmarkSamples[j] / (float) sampleRate;
			}

			new ESTTrackWriter(pitchmarkSeconds, null, "pitchmarks").doWriteAndClose(pmFile, false, false);

			// And correct pitchmark locations
			// pitchmarkSeconds = adjustPitchmarks(wf, pitchmarkSeconds);
			// new ESTTrackWriter(pitchmarkSeconds, null, "pitchmarks").doWriteAndClose(correctedPmFile, false, false);

		}
		return true;
	}

	/**
	 * Shift the pitchmarks to the closest peak.
	 * 
	 * @param pmIn
	 *            pmIn
	 * @param w
	 *            w
	 * @param sampleRate
	 *            sampleRate
	 */
	private float[] shiftToClosestPeak(float[] pmIn, short[] w, int sampleRate) {

		final int HORIZON = 32; // <= number of samples to seek before and after the pitchmark
		float[] pmOut = new float[pmIn.length];

		/* Browse the pitchmarks */
		int pm = 0;
		int pmwmax = w.length - 1;
		int TO = 0;
		int max = 0;
		for (int pi = 0; pi < pmIn.length; pi++) {
			pm = (int) (pmIn[pi] * sampleRate);
			// If the pitchmark goes out of the waveform (this sometimes
			// happens with the last one due to rounding errors), just clip it.
			if (pm > pmwmax) {
				// If this was not the last pitchmark, there is a problem
				if (pi < (pmIn.length - 1)) {
					throw new RuntimeException("Some pitchmarks are located above the location of the last waveform sample !");
				}
				// Else, if it was the last pitchmark, clip it:
				pmOut[pi] = (float) ((double) (pmwmax) / (double) (sampleRate));
			}
			// Else, if the pitchmark is in the waveform:
			else {
				/* Seek the max of the wav samples around the pitchmark */
				max = pm;
				// - Back:
				TO = (pm - HORIZON) < 0 ? 0 : (pm - HORIZON);
				for (int i = pm - 1; i >= TO; i--) {
					if (w[i] > w[max])
						max = i;
				}
				// - Forth:
				TO = (pm + HORIZON + 1) > w.length ? w.length : (pm + HORIZON + 1);
				for (int i = pm + 1; i < TO; i++) {
					if (w[i] >= w[max])
						max = i;
				}
				/* Translate the pitchmark */
				pmOut[pi] = (float) ((double) (max) / (double) (sampleRate));
			}
		}

		return pmOut;
	}

	/**
	 * Shift the pitchmarks to the previous zero crossing.
	 * 
	 * @param pmIn
	 *            pmIn
	 * @param w
	 *            w
	 * @param sampleRate
	 *            sampleRate
	 */
	private float[] shiftToPreviousZero(float[] pmIn, short[] w, int sampleRate) {

		final int HORIZON = 32; // <= number of samples to seek before the pitchmark
		float[] pmOut = new float[pmIn.length];

		/* Browse the pitchmarks */
		int pm = 0;
		int TO = 0;
		int zero = 0;
		for (int pi = 0; pi < pmIn.length; pi++) {
			pm = (int) (pmIn[pi] * sampleRate);
			/* If the initial pitchmark is on a zero, don't shift the pitchmark. */
			if (w[pm] == 0) {
				pmOut[pi] = pmIn[pi];
				continue;
			}
			/* Else: */
			/* Seek the zero crossing preceding the pitchmark */
			TO = (pm - HORIZON) < 0 ? 0 : (pm - HORIZON);
			for (zero = pm; (zero > TO) && ((w[zero] * w[zero + 1]) > 0); zero--)
				;
			/* If no zero crossing was found, don't move the pitchmark */
			if ((zero == TO) && ((w[zero] * w[zero + 1]) > 0)) {
				pmOut[pi] = pmIn[pi];
			}
			/* If a zero crossing was found, translate the pitchmark */
			else {
				pmOut[pi] = (float) ((double) ((-w[zero]) < w[zero + 1] ? zero : (zero + 1)) / (double) (sampleRate));
			}
		}

		return pmOut;
	}

	/**
	 * Adjust pitchmark position to the zero crossing preceding the closest peak.
	 * 
	 * @param wf
	 *            basename of the corresponding wav file
	 * @param pitchmarks
	 *            the input pitchmarks
	 * @throws IOException
	 *             IOException
	 * @return the adjusted pitchmarks
	 */
	private float[] adjustPitchmarks(WavReader wf, float[] pitchmarks) throws IOException {
		/* Load the wav file */

		short[] w = wf.getSamples();
		float[] pmOut = null;
		try {
			/* Shift to the closest peak */
			pmOut = shiftToClosestPeak(pitchmarks, w, wf.getSampleRate());
			/* Shift to the zero immediately preceding the closest peak */
			pmOut = shiftToPreviousZero(pmOut, w, wf.getSampleRate());
		} catch (RuntimeException e) {
			throw new RuntimeException(e);
		}
		return pmOut;
	}

	/**
	 * Provide the progress of computation, in percent, or -1 if that feature is not implemented.
	 * 
	 * @return -1 if not implemented, or an integer between 0 and 100.
	 */
	public int getProgress() {
		return percent;
	}
}
