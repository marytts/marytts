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
import java.io.InputStream;
import java.util.SortedMap;
import java.util.TreeMap;

import marytts.util.MaryUtils;
import marytts.util.data.ESTTrackWriter;
import marytts.util.data.text.PraatTextfileDoubleDataSource;
import marytts.util.io.General;
import marytts.util.math.ArrayUtils;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;

public class PraatPitchmarker extends VoiceImportComponent {
	protected DatabaseLayout db = null;
	protected String pointpExt = ".PointProcess";
	protected String tmpScript;

	protected int percent = 0;

	public final String COMMAND = getName() + ".command";
	public final String MINPITCH = getName() + ".minPitch";
	public final String MAXPITCH = getName() + ".maxPitch";
	public final String PRAATPMDIR = getName() + ".pmDir";
	public final String WAVEDIR = getName() + ".waveDir";

	public final String PMDIR = "db.pmDir";
	public final String PMEXT = "db.pmExtension";

	protected void setupHelp() {
		if (props2Help == null) {
			props2Help = new TreeMap();
			props2Help.put(COMMAND, "The command that is used to launch praat");
			props2Help.put(MINPITCH, "minimum value for the pitch (in Hz). Default: female 100, male 75");
			props2Help.put(MAXPITCH, "maximum value for the pitch (in Hz). Default: female 500, male 300");
		}

	}

	public String getName() {
		return "PraatPitchmarker";
	}

	@Override
	protected void initialiseComp() {
		tmpScript = db.getProp(db.TEMPDIR) + "script.praat";
	}

	public SortedMap getDefaultProps(DatabaseLayout db) {
		this.db = db;
		if (props == null) {
			props = new TreeMap();
			props.put(COMMAND, "praat");
			if (db.getProp(db.GENDER).equals("female")) {
				props.put(MINPITCH, "100");
				props.put(MAXPITCH, "500");
			} else {
				props.put(MINPITCH, "75");
				props.put(MAXPITCH, "300");
			}
			props.put(WAVEDIR, db.getProp(db.WAVDIR));
			props.put(PRAATPMDIR, db.getProp(db.PMDIR));
			String rootDir = db.getProp(db.ROOTDIR);
		}
		return props;
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
	 * @param basename
	 *            basename of the corresponding wav file
	 * @param pitchmarks
	 *            the input pitchmarks
	 * @throws IOException
	 *             IOException
	 * @return the adjusted pitchmarks
	 */
	private float[] adjustPitchmarks(String basename, float[] pitchmarks) throws IOException {
		/* Load the wav file */
		String fName = getProp(WAVEDIR) + basename + db.getProp(db.WAVEXT);
		WavReader wf = new WavReader(fName);
		short[] w = wf.getSamples();
		float[] pmOut = null;
		try {
			/* Shift to the closest peak */
			pmOut = shiftToClosestPeak(pitchmarks, w, wf.getSampleRate());
			/* Shift to the zero immediately preceding the closest peak */
			pmOut = shiftToPreviousZero(pmOut, w, wf.getSampleRate());
		} catch (RuntimeException e) {
			throw new RuntimeException("For utterance [" + basename + "]:", e);
		}
		return pmOut;
	}

	@Deprecated
	protected boolean praatPitchmarks(String basename) throws IOException {
		String wavFilename = new File(getProp(WAVEDIR) + basename + db.getProp(db.WAVEXT)).getAbsolutePath();
		String pointprocessFilename = getProp(PRAATPMDIR) + basename + pointpExt;

		String strTmp = getProp(COMMAND) + " " + tmpScript + " " + wavFilename + " " + pointprocessFilename + " "
				+ getProp(MINPITCH) + " " + getProp(MAXPITCH);

		if (MaryUtils.isWindows())
			strTmp = "cmd.exe /c " + strTmp;

		General.launchProc(strTmp, "PraatPitchmarker", basename);

		// Now convert the praat format into EST pm format:
		estPitchmarks(basename);

		return true;
	}

	/**
	 * Convert Praat PointProcess files to EST pm files
	 * 
	 * @param basename
	 *            of files to process
	 * @throws IOException
	 *             IOException
	 */
	protected void estPitchmarks(String basename) throws IOException {
		String pointprocessFilename = getProp(PRAATPMDIR) + basename + pointpExt;
		String pmFilename = getProp(PRAATPMDIR) + basename + db.getProp(PMEXT);
		FileReader pmReader = new FileReader(pointprocessFilename);
		double[] pm = new PraatTextfileDoubleDataSource(pmReader).getAllData();
		pmReader.close();
		float[] pitchmarks = ArrayUtils.copyDouble2Float(pm);
		new ESTTrackWriter(pitchmarks, null, "pitchmarks").doWriteAndClose(pmFilename, false, false);
	}

	/**
	 * The standard compute() method of the VoiceImportComponent interface.
	 * 
	 * @throws IOException
	 *             IOException
	 */
	public boolean compute() throws IOException {
		percent = 0;

		String[] baseNameArray = bnl.getListAsArray();
		System.out.println("Computing pitchmarks for " + baseNameArray.length + " utterances.");

		/* Ensure the existence of the target pitchmark directory */
		File dir = new File(getProp(PRAATPMDIR));
		if (!dir.exists()) {
			System.out.println("Creating the directory [" + getProp(PRAATPMDIR) + "].");
			dir.mkdir();
		}

		// script.praat is provided as template.
		InputStream scriptResource = getClass().getResourceAsStream("script.praat");
		FileWriter scriptWriter = new FileWriter(tmpScript);
		IOUtils.copy(scriptResource, scriptWriter);
		scriptWriter.close();

		// ensure that basenames.lst is up to date:
		String baseNameListFileName = db.getProp("db.basenameFile");
		bnl.write(baseNameListFileName);

		// run Praat process:
		String praatCommand = StringUtils.join(new String[] { getProp(COMMAND), tmpScript, baseNameListFileName,
				getProp(WAVEDIR), getProp(PRAATPMDIR), getProp(MINPITCH), getProp(MAXPITCH) }, " ");
		System.out.println("Running Praat as: " + praatCommand);
		if (MaryUtils.isWindows()) {
			praatCommand = "cmd.exe /c " + praatCommand;
		}
		General.launchProc(praatCommand, getName(), "");

		// convert to EST format
		for (int i = 0; i < baseNameArray.length; i++) {
			percent = 100 * i / baseNameArray.length;
			estPitchmarks(baseNameArray[i]);
		}

		return true;
	}

	/**
	 * Provide the progress of computation, in percent, or -1 if that feature is not implemented.
	 * 
	 * @return -1 if not implemented, or an integer between 0 and 100.
	 */
	public int getProgress() {
		// hack to get progress from Praat through file I/O:
		File percentFile = new File(db.getProp(db.TEMPDIR) + "percent");
		if (percentFile.exists()) {
			percent = -1;
			try {
				FileReader percentReader = new FileReader(percentFile);
				String percentString = IOUtils.toString(percentReader);
				percent = Integer.parseInt(percentString);
				percentReader.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return percent;
	}
}
