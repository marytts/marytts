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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.SortedMap;
import java.util.TreeMap;

import marytts.signalproc.analysis.SPTKPitchReaderWriter;
import marytts.tools.voiceimport.DatabaseLayout;
import marytts.tools.voiceimport.VoiceImportComponent;
import marytts.util.MaryUtils;
import marytts.util.data.text.SnackTextfileDoubleDataSource;
import marytts.util.io.BasenameList;
import marytts.util.io.General;

public class SnackF0ContourExtractor extends VoiceImportComponent {

	String vocalizationsDir;
	BasenameList bnlVocalizations;

	protected DatabaseLayout db = null;
	// protected String correctedPmExt = ".pm.corrected";
	protected String snackPmExt = ".snack";
	protected String lf0Ext = ".lf0";
	protected String scriptFileName;

	private int percent = 0;

	public final String MINPITCH = "SnackF0ContourExtractor.minPitch";
	public final String MAXPITCH = "SnackF0ContourExtractor.maxPitch";
	public final String COMMAND = "SnackF0ContourExtractor.command";
	public final String LF0DIR = "SnackF0ContourExtractor.pitchContoursDir";
	public final String WAVEDIR = "SnackF0ContourExtractor.vocalizationWaveDir";
	public final String SAMPLERATE = "SnackF0ContourExtractor.samplingRate";
	public final String FRAMEPERIOD = "SnackF0ContourExtractor.framePeriod";

	public String getName() {
		return "SnackF0ContourExtractor";
	}

	@Override
	protected void initialiseComp() {
		scriptFileName = db.getProp(db.VOCALIZATIONSDIR) + "script.snack";
		if (!(new File(getProp(LF0DIR))).exists()) {

			System.out.println("vocalizations/lf0 directory does not exist; ");
			if (!(new File(getProp(LF0DIR))).mkdirs()) {
				throw new Error("Could not create vocalizations/lf0");
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
			props.put(COMMAND, "praat");
			if (db.getProp(db.GENDER).equals("female")) {
				props.put(MINPITCH, "100");
				props.put(MAXPITCH, "500");
			} else {
				props.put(MINPITCH, "75");
				props.put(MAXPITCH, "300");
			}
			props.put(WAVEDIR, db.getProp(db.VOCALIZATIONSDIR) + File.separator + "wav");
			props.put(LF0DIR, db.getProp(db.VOCALIZATIONSDIR) + File.separator + "lf0");
			props.put(FRAMEPERIOD, "80");
			props.put(SAMPLERATE, "16000");
			// vocalizationsDir = db.getProp(db.ROOTDIR)+File.separator+"vocalizations";
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
	 * @throws IOException
	 *             IOException
	 * @throws InterruptedException
	 *             InterruptedException
	 */
	public boolean compute() throws IOException, InterruptedException {

		String[] baseNameArray = bnlVocalizations.getListAsArray();
		System.out.println("Computing pitchmarks for " + baseNameArray.length + " utterances.");

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
		toScript.println("set frameperiod [ lindex $argv 4 ]");
		toScript.println("set samplerate [ lindex $argv 5 ]");
		toScript.println("set framelength [expr {double($frameperiod) / $samplerate}]");
		toScript.println(" ");
		toScript.println("set fd [open [lindex $argv 1] w]");
		toScript.println("puts $fd [join [s pitch -method esps -maxpitch [lindex $argv 2] -minpitch [lindex $argv 3] -framelength $framelength] \\n]");
		// toScript.println( "puts stdout [ lindex $argv 4 ]");
		// toScript.println( "puts stdout $framelength");
		toScript.println("close $fd");
		toScript.println(" ");
		toScript.println("exit");
		toScript.println(" ");
		toScript.close();

		System.out.println("Computing pitchmarks for " + baseNameArray.length + " utterances.");

		/* Ensure the existence of the target pitchmark directory */
		/*
		 * File dir = new File(db.getProp(PMDIR)); if (!dir.exists()) { System.out.println( "Creating the directory [" +
		 * db.getProp(PMDIR) + "]." ); dir.mkdir(); }
		 */

		/* execute snack */
		for (int i = 0; i < baseNameArray.length; i++) {
			percent = 100 * i / baseNameArray.length;
			String wavFile = getProp(WAVEDIR) + baseNameArray[i] + db.getProp(db.WAVEXT);
			String snackFile = getProp(LF0DIR) + baseNameArray[i] + snackPmExt;
			String outLF0File = getProp(LF0DIR) + baseNameArray[i] + lf0Ext;
			System.out.println("Writing pm file to " + snackFile);

			boolean isWindows = true; // TODO This is WRONG, and never used. Consider removal.
			String strTmp = scriptFileName + " " + wavFile + " " + snackFile + " " + getProp(MAXPITCH) + " " + getProp(MINPITCH)
					+ " " + getProp(FRAMEPERIOD) + " " + getProp(SAMPLERATE);

			if (MaryUtils.isWindows())
				strTmp = "cmd.exe /c " + strTmp;
			else
				strTmp = getProp(COMMAND) + " " + strTmp;

			General.launchProc(strTmp, "SnackF0ContourExtractor", baseNameArray[i]);

			// Now convert the snack format into EST pm format
			double[] pm = new SnackTextfileDoubleDataSource(new FileReader(snackFile)).getAllData();
			SPTKPitchReaderWriter sptkPitch = new SPTKPitchReaderWriter(pm, null);
			sptkPitch.writeIntoSPTKLF0File(outLF0File);
		}
		return true;

	}

	/**
	 * @param args
	 *            args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

	public int getProgress() {
		return percent;
	}

	protected void setupHelp() {
		if (props2Help == null) {
			props2Help = new TreeMap();
			props2Help.put(COMMAND, "The command that is used to launch snack");
			props2Help.put(MINPITCH, "minimum value for the pitch (in Hz). Default: female 100, male 75");
			props2Help.put(MAXPITCH, "maximum value for the pitch (in Hz). Default: female 500, male 300");
		}
	}

	private static class StreamGobbler extends Thread {
		InputStream is;
		String type;

		public StreamGobbler(InputStream is, String type) {
			this.is = is;
			this.type = type;
		}

		public void run() {
			try {
				InputStreamReader isr = new InputStreamReader(is);
				BufferedReader br = new BufferedReader(isr);
				String line = null;
				while ((line = br.readLine()) != null)
					System.out.println(type + ">" + line);
			} catch (IOException ioe) {
				ioe.printStackTrace();
			}
		}
	}

}
