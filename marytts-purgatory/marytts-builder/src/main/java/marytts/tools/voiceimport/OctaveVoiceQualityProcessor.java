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
package marytts.tools.voiceimport;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.StringTokenizer;
import java.util.TreeMap;

import marytts.signalproc.analysis.VoiceQuality;
import marytts.util.io.StreamGobbler;
import marytts.util.MaryUtils;

public class OctaveVoiceQualityProcessor extends VoiceImportComponent {

	protected DatabaseLayout db;
	private String name = "OctaveVoiceQualityProcessor";
	protected String snackExtension = ".snack";
	protected String octaveExtension = ".octave";
	protected String voiceQualityExtension = ".vq";
	protected String scriptSnackFileName;
	protected String scriptOctaveFileName;

	int numVqParams = 5; // number of voice quality parameters extracted from the sound files:
							// OQG, GOG, SKG, RCG, IC

	private int percent = 0;
	// private final String FRAMELENGTH = "0.01"; // Default for snack
	// private final String WINDOWLENGTH = "0.025"; // Default for f0 snack ( formants uses a bigger window)

	public final String SAMPLINGRATE = "OctaveVoiceQualityProcessor.samplingRate";
	public final String MINPITCH = "OctaveVoiceQualityProcessor.minPitch";
	public final String MAXPITCH = "OctaveVoiceQualityProcessor.maxPitch";
	public final String FRAMELENGTH = "OctaveVoiceQualityProcessor.frameLength";
	public final String WINDOWLENGTH = "OctaveVoiceQualityProcessor.windowLength";
	public final String NUMFORMANTS = "OctaveVoiceQualityProcessor.numFormants";
	public final String LPCORDER = "OctaveVoiceQualityProcessor.lpcOrder";
	public final String VQDIR = "OctaveVoiceQualityProcessor.vqDir";
	public final String OCTAVEPATH = "OctaveVoiceQualityProcessor.octavePath";

	protected void setupHelp() {
		if (props2Help == null) {
			props2Help = new TreeMap();
			props2Help.put(SAMPLINGRATE, "Sampling frequency in Hertz. Default: 16000");
			props2Help.put(MINPITCH, "minimum value for the pitch (in Hz). Default: female 60, male 40");
			props2Help.put(MAXPITCH, "maximum value for the pitch (in Hz). Default: female 500, male 400");
			props2Help.put(FRAMELENGTH, "frame length (in seconds) for VQ calculation Default: 0.005 sec.");
			props2Help.put(WINDOWLENGTH, "window length (in seconds) for VQ calculation Default: 0.025 sec.");
			props2Help.put(NUMFORMANTS, "Default 4, maximum 7");
			props2Help.put(LPCORDER, "Default 12, if NUMFORMANTS=4 min LPCORDER=12\n" + "if NUMFORMANTS=5 min LPCORDER=14\n"
					+ "if NUMFORMANTS=6 min LPCORDER=16\n" + "if NUMFORMANTS=7 min LPCORDER=18\n");
			props2Help.put(VQDIR, "directory containing the voice quality files. Will be created if it does not exist");
			props2Help.put(OCTAVEPATH, "octave executable path");
		}
	}

	public final String getName() {
		return name;
	}

	@Override
	protected void initialiseComp() {
		scriptSnackFileName = db.getProp(db.TEMPDIR) + "snack_call.tcl";
		scriptOctaveFileName = db.getProp(db.TEMPDIR) + "octave_call.m";
	}

	public SortedMap getDefaultProps(DatabaseLayout db) {
		this.db = db;
		if (props == null) {
			props = new TreeMap();
			props.put(SAMPLINGRATE, "16000");
			if (db.getProp(db.GENDER).equals("female")) {
				props.put(MINPITCH, "60");
				props.put(MAXPITCH, "400");
			} else {
				props.put(MINPITCH, "60");
				props.put(MAXPITCH, "400");
			}
			props.put(FRAMELENGTH, "0.005");
			props.put(WINDOWLENGTH, "0.025");
			props.put(NUMFORMANTS, "4");
			props.put(LPCORDER, "12");
			props.put(VQDIR, db.getProp(db.ROOTDIR) + "vq" + System.getProperty("file.separator"));
			props.put(OCTAVEPATH, "/usr/bin/octave");
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

		/*
		 * In order to get the same number of frames when calculating f0 and formants with snack, we should keep constant the
		 * following variables: -maxpitch 400 for F0 calculation -minpitch 60 for F0 calculation -windowlength 0.03 for formants
		 * calculation -framelength should be the same for f0, formants and this SnackVoiceQualityProcessor, this value can be
		 * change, ex: 0.005, 0.01 etc.
		 */
		File scriptSnack = new File(scriptSnackFileName);

		if (scriptSnack.exists())
			scriptSnack.delete();
		PrintWriter toScriptSnack = new PrintWriter(new FileWriter(scriptSnack));
		toScriptSnack.println("# extracting pitch anf formants using snack");
		toScriptSnack.println("package require snack");
		toScriptSnack.println("snack::sound s");
		toScriptSnack.println("s read [lindex $argv 0]");
		toScriptSnack.println("set fd [open [lindex $argv 1] w]");
		toScriptSnack
				.println("set f0 [s pitch -method esps -maxpitch [lindex $argv 2] -minpitch [lindex $argv 3] -framelength [lindex $argv 4] ]");
		toScriptSnack.println("set f0_length [llength $f0]");
		// toScriptSnack.println("puts \"f0 length = $f0_length\"");
		toScriptSnack
				.println("set formants [s formant -numformants [lindex $argv 5] -lpcorder [lindex $argv 6] -framelength [lindex $argv 4] -windowlength 0.03]");
		toScriptSnack.println("set formants_length [llength $formants]");
		// toScriptSnack.println("puts \"formants length = $formants_length\"");
		toScriptSnack.println("set n 0");
		toScriptSnack.println("foreach line $f0 {");
		toScriptSnack.println("puts -nonewline $fd \"[lindex $line 0] \"");
		toScriptSnack.println("puts $fd [lindex $formants $n]");
		toScriptSnack.println("incr n");
		toScriptSnack.println("}");
		toScriptSnack.println("close $fd");
		toScriptSnack.println("exit");
		toScriptSnack.close();

		File scriptOctave = new File(scriptOctaveFileName);
		if (scriptOctave.exists())
			scriptOctave.delete();
		PrintWriter toScriptOctave = new PrintWriter(new FileWriter(scriptOctave));
		toScriptOctave.println("arg_list = argv ();");
		toScriptOctave.println("cd " + db.getProp(db.TEMPDIR));
		// calculateVoiceQuality(filename, filesnack, gender, par_name, debug);
		toScriptOctave.println("calculateVoiceQuality(arg_list{1}, arg_list{2}, arg_list{3}, arg_list{4});");
		toScriptOctave.close();

		String[] baseNameArray = bnl.getListAsArray();
		// to test String[] baseNameArray = {"curious", "u"};
		System.out.println("Computing voice quality for " + baseNameArray.length + " utterances.");

		/* Ensure the existence of the target pitchmark directory */
		File dir = new File(getProp(VQDIR));
		if (!dir.exists()) {
			System.out.println("Creating the directory [" + getProp(VQDIR) + "].");
			dir.mkdir();
		}

		// Some general parameters that apply to all the sound files
		int samplingRate = Integer.parseInt(getProp(SAMPLINGRATE));
		// frameLength and windowLength in samples
		int frameLength = Math.round(Float.parseFloat(getProp(FRAMELENGTH)) * samplingRate);
		int windowLength = Math.round(Float.parseFloat(getProp(WINDOWLENGTH)) * samplingRate);

		/* execute octave and voice quality parameters extraction */
		for (int i = 0; i < baseNameArray.length; i++) {
			percent = 100 * i / baseNameArray.length;

			/* call snack for calculating f0 and formants */
			String wavFile = db.getProp(db.WAVDIR) + baseNameArray[i] + db.getProp(db.WAVEXT);
			String octaveFile = getProp(VQDIR) + baseNameArray[i] + octaveExtension;
			String snackFile = getProp(VQDIR) + baseNameArray[i] + snackExtension;
			String vqFile = getProp(VQDIR) + baseNameArray[i] + voiceQualityExtension;

			System.out.println("Writing (snack) f0+formants+bandWidths to " + snackFile);
			boolean isWindows = true;
			String strSnackTmp = scriptSnackFileName + " " + wavFile + " " + snackFile + " " + getProp(MAXPITCH) + " "
					+ getProp(MINPITCH) + " " + getProp(FRAMELENGTH) + " " + getProp(NUMFORMANTS) + " " + getProp(LPCORDER);
			if (MaryUtils.isWindows())
				strSnackTmp = "cmd.exe /c " + db.getExternal(db.TCLPATH) + "/tclsh " + strSnackTmp;
			else
				strSnackTmp = db.getExternal(db.TCLPATH) + "/tclsh " + strSnackTmp;
			// System.out.println("Executing: " + strSnackTmp);
			Process snack = Runtime.getRuntime().exec(strSnackTmp);
			StreamGobbler errorGobbler1 = new StreamGobbler(snack.getErrorStream(), "err");
			// read from output stream
			StreamGobbler outputGobbler1 = new StreamGobbler(snack.getInputStream(), "out");
			// start reading from the streams
			errorGobbler1.start();
			outputGobbler1.start();
			// close everything down
			snack.waitFor();
			snack.exitValue();

			/* call octave for calculating VQ parameters */
			// System.out.println("Calculating  OQG GOG SKG RCG IC");
			// TODO: gender does not appear properly
			String strOctaveTmp = getProp(OCTAVEPATH) + " --silent " + scriptOctaveFileName + " " + wavFile + " " + snackFile
					+ " " + getProp(db.GENDER) + " " + octaveFile;
			// System.out.println("Executing: " + strOctaveTmp);
			Process octave = Runtime.getRuntime().exec(strOctaveTmp);
			StreamGobbler errorGobbler2 = new StreamGobbler(octave.getErrorStream(), "err");
			// read from output stream
			StreamGobbler outputGobbler2 = new StreamGobbler(octave.getInputStream(), "out");
			// start reading from the streams
			errorGobbler2.start();
			outputGobbler2.start();
			// close everything down
			octave.waitFor();
			octave.exitValue();

			// Read the sound file
			WavReader soundFile = new WavReader(wavFile);
			// Check sampling rate of sound file
			assert samplingRate == soundFile.getSampleRate();

			// get a wrapper voice quality class for this file
			VoiceQuality vq = new VoiceQuality(numVqParams, samplingRate, frameLength / (float) samplingRate, windowLength
					/ (float) samplingRate);

			readOctaveData(vq, octaveFile);

			System.out.println("Writing (octave) vq parameters to " + vqFile);
			vq.writeVqFile(vqFile);

		}
		return true;
	}

	private void readOctaveData(VoiceQuality vq, String octaveFile) throws IOException {
		double[][] octaveData = null;
		int numLines, numData;
		BufferedReader reader = new BufferedReader(new FileReader(octaveFile));
		int i, j;
		try {
			String line;
			String strVal;
			StringTokenizer s;
			double value;

			// find out the number of lines in the file
			List<String> lines = new ArrayList<String>();
			while ((line = reader.readLine()) != null) {
				lines.add(line);
			}
			numLines = lines.size();
			numData = vq.params.dimension;
			octaveData = new double[numData][numLines];
			for (i = 0; i < numLines; i++) {

				strVal = (String) lines.get(i);
				s = new StringTokenizer(strVal);

				for (j = 0; j < numData; j++) {
					if (s.hasMoreTokens())
						octaveData[j][i] = Double.parseDouble(s.nextToken());
				}
			}
			vq.allocate(numLines, octaveData);

		} catch (IOException ioe) {
			ioe.printStackTrace();
		} catch (NumberFormatException nfe) {
			nfe.printStackTrace();
		}
	}

	/**
	 * Provide the progress of computation, in percent, or -1 if that feature is not implemented.
	 * 
	 * @return -1 if not implemented, or an integer between 0 and 100.
	 */
	public int getProgress() {
		return percent;
	}

	// to test/compare vq values of several files
	public static void main1(String[] args) throws Exception {

		String path = "/project/mary/marcela/HMM-voices/arctic_test/vq-octave/";
		String whisperFile = path + "whisper.vq";
		String modalFile = path + "modal.vq";
		String creakFile = path + "creak.vq";
		String harshFile = path + "harsh.vq";

		VoiceQuality vq1 = new VoiceQuality();
		System.out.println("Reading: " + whisperFile);
		vq1.readVqFile(whisperFile);
		// vq1.printPar();
		vq1.printMeanStd();

		VoiceQuality vq2 = new VoiceQuality();
		System.out.println("Reading: " + modalFile);
		vq2.readVqFile(modalFile);
		// vq2.printPar();
		vq2.printMeanStd();

		VoiceQuality vq3 = new VoiceQuality();
		System.out.println("Reading: " + creakFile);
		vq3.readVqFile(creakFile);
		// vq3.printPar();
		vq3.printMeanStd();

		VoiceQuality vq4 = new VoiceQuality();
		System.out.println("Reading: " + harshFile);
		vq4.readVqFile(harshFile);
		// vq4.printPar();
		vq4.printMeanStd();

	}

	public static void main(String[] args) throws Exception {
		/*
		 * OctaveVoiceQualityProcessor vq = new OctaveVoiceQualityProcessor(); DatabaseLayout db = new DatabaseLayout(vq);
		 * vq.compute();
		 */
		// values extracted with Java program
		// main1(args);

		String file = "/project/mary/marcela/UnitSel-voices/slt-arctic/vq/curious.vq";
		VoiceQuality vq1 = new VoiceQuality();
		System.out.println("Reading: " + file);
		vq1.readVqFile(file);
		vq1.printPar();
		vq1.printMeanStd();
		// MaryUtils.plot(vq1.getGOG(), "Normal");
		// vq1.applyZscoreNormalization();
		// MaryUtils.plot(vq1.getGOG(), "after z-score");

	}

}
