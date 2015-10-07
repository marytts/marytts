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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.SortedMap;
import java.util.TreeMap;

import marytts.tools.voiceimport.DatabaseLayout;
import marytts.tools.voiceimport.PraatPitchmarker;
import marytts.util.io.BasenameList;

public class VocalizationPitchmarker extends PraatPitchmarker {

	String vocalizationsDir;
	BasenameList bnlVocalizations;

	public String getName() {
		return "VocalizationPitchmarker";
	}

	@Override
	protected void initialiseComp() {
		tmpScript = db.getProp(db.TEMPDIR) + "script.praat";

		if (!(new File(getProp(PRAATPMDIR))).exists()) {

			System.out.println("vocalizations/pm directory does not exist; ");
			if (!(new File(getProp(PRAATPMDIR))).mkdirs()) {
				throw new Error("Could not create vocalizations/pm");
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
			props.put(PRAATPMDIR, db.getProp(db.VOCALIZATIONSDIR) + File.separator + "pm");
			// vocalizationsDir = db.getProp(db.ROOTDIR)+File.separator+"vocalizations";
		}
		return props;
	}

	/**
	 * The standard compute() method of the VoiceImportComponent interface.
	 * 
	 * @throws IOException
	 *             IOException
	 */
	public boolean compute() throws IOException {

		String[] baseNameArray = bnlVocalizations.getListAsArray();
		System.out.println("Computing pitchmarks for " + baseNameArray.length + " utterances.");

		/* Ensure the existence of the target pitchmark directory */
		File dir = new File(getProp(PRAATPMDIR));
		if (!dir.exists()) {
			System.out.println("Creating the directory [" + getProp(PRAATPMDIR) + "].");
			dir.mkdirs();
		}

		// script.praat is provided as template. Perhaps it could be used instead of hardcoding the following:
		File script = new File(tmpScript);

		if (script.exists())
			script.delete();
		PrintWriter toScript = new PrintWriter(new FileWriter(script));
		// use Praat form to provide ARGV (NOTE: these must be explicitly given during call to Praat!)
		toScript.println("form Provide arguments");
		toScript.println("  sentence wavFile input.wav");
		toScript.println("  sentence pointpFile output.PointProcess");
		toScript.println("  real minPitch 75");
		toScript.println("  real maxPitch 600");
		toScript.println("endform");
		toScript.println("Read from file... 'wavFile$'");
		// Remove DC offset, if present:
		toScript.println("Subtract mean");
		// First, low-pass filter the speech signal to make it more robust against noise
		// (i.e., mixed noise+periodicity regions treated more likely as periodic)
		toScript.println("sound = Filter (pass Hann band)... 0 1000 100");
		// Then determine pitch curve:
		toScript.println("pitch = To Pitch... 0 minPitch maxPitch");
		// Get some debug info:
		toScript.println("min_f0 = Get minimum... 0 0 Hertz Parabolic");
		toScript.println("max_f0 = Get maximum... 0 0 Hertz Parabolic");
		// And convert to pitch marks:
		toScript.println("plus sound");
		toScript.println("To PointProcess (cc)");
		// Fill in 100 Hz pseudo-pitchmarks in unvoiced regions:
		toScript.println("Voice... 0.01 0.02000000001");
		toScript.println("Write to short text file... 'pointpFile$'");
		toScript.println("lastSlash = rindex(wavFile$, \"/\")");
		toScript.println("baseName$ = right$(wavFile$, length(wavFile$) - lastSlash) - \".wav\"");
		toScript.println("printline 'baseName$'   f0 range: 'min_f0:0' - 'max_f0:0' Hz");
		toScript.close();

		System.out.println("Running Praat as: " + getProp(COMMAND) + " " + tmpScript);
		for (int i = 0; i < baseNameArray.length; i++) {
			percent = 100 * i / baseNameArray.length;
			praatPitchmarks(baseNameArray[i]);
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

}
