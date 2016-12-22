/* ----------------------------------------------------------------- */
/*           The HMM-Based Speech Synthesis Engine "hts_engine API"  */
/*           developed by HTS Working Group                          */
/*           http://hts-engine.sourceforge.net/                      */
/* ----------------------------------------------------------------- */
/*                                                                   */
/*  Copyright (c) 2001-2010  Nagoya Institute of Technology          */
/*                           Department of Computer Science          */
/*                                                                   */
/*                2001-2008  Tokyo Institute of Technology           */
/*                           Interdisciplinary Graduate School of    */
/*                           Science and Engineering                 */
/*                                                                   */
/* All rights reserved.                                              */
/*                                                                   */
/* Redistribution and use in source and binary forms, with or        */
/* without modification, are permitted provided that the following   */
/* conditions are met:                                               */
/*                                                                   */
/* - Redistributions of source code must retain the above copyright  */
/*   notice, this list of conditions and the following disclaimer.   */
/* - Redistributions in binary form must reproduce the above         */
/*   copyright notice, this list of conditions and the following     */
/*   disclaimer in the documentation and/or other materials provided */
/*   with the distribution.                                          */
/* - Neither the name of the HTS working group nor the names of its  */
/*   contributors may be used to endorse or promote products derived */
/*   from this software without specific prior written permission.   */
/*                                                                   */
/* THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND            */
/* CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,       */
/* INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF          */
/* MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE          */
/* DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS */
/* BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,          */
/* EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED   */
/* TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,     */
/* DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON */
/* ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,   */
/* OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY    */
/* OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE           */
/* POSSIBILITY OF SUCH DAMAGE.                                       */
/* ----------------------------------------------------------------- */
/**
 * Copyright 2011 DFKI GmbH.
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
import java.util.SortedMap;
import java.util.TreeMap;

import marytts.util.io.FileUtils;
import marytts.util.io.General;

/**
 * This program was modified from previous version to: 1. copy $MARY_BASE/lib/external/hts directory to the voice building
 * directory 2. check again that all the external necessary programs are installed. 3. check as before that wav and text
 * directories exist and make conversions: voiceDir/wav &rarr; voiceDir/hts/data/raw userProvidedDir/utts (festival format) &rarr;
 * voiceDir/text (one file per transcription) userProvidedDir/raw move to voiceDir/hts/data/raw
 *
 * @author marcela
 *
 */
public class HMMVoiceDataPreparation extends VoiceImportComponent {
	private DatabaseLayout db;
	private String name = "HMMVoiceDataPreparation";
	public final String ADAPTSCRIPTS = name + ".adaptScripts";
	public final String USERRAWDIR = name + ".userRawDirectory";
	public final String USERUTTDIR = name + ".userUttDirectory";
	private String marybase;
	private String voiceDir;
	private String soxPath;
	private String sep;
	private String dataDir;
	private String scriptsDir;

	public String getName() {
		return name;
	}

	/**
	 * Get the map of properties2values containing the default values
	 * 
	 * @param db
	 *            db
	 * @return map of props2values
	 */
	public SortedMap<String, String> getDefaultProps(DatabaseLayout db) {
		this.db = db;
		if (props == null) {
			props = new TreeMap<String, String>();
			props.put(USERRAWDIR, "");
			props.put(USERUTTDIR, "");
			props.put(ADAPTSCRIPTS, "false");
		}
		return props;
	}

	protected void setupHelp() {
		props2Help = new TreeMap<String, String>();
		props2Help.put(USERRAWDIR, "raw files directory, user provided directory (default empty)");
		props2Help.put(USERUTTDIR,
				"utterance directory (transcriptions in festival format), user provided directory (default empty)");
		props2Help.put(ADAPTSCRIPTS,
				"ADAPTSCRIPTS=false: speaker dependent scripts, ADAPTSCRIPTS=true: speaker adaptation/adaptive scripts.  ");
	}

	/**
	 * Do the computations required by this component.
	 * 
	 * @throws Exception
	 *             Exception
	 * @return true on success, false on failure
	 */
	public boolean compute() throws Exception {
		boolean raw = false;
		boolean text = false;
		boolean wav = false;
		marybase = db.getProp(DatabaseLayout.MARYBASE);
		voiceDir = db.getProp(DatabaseLayout.ROOTDIR);
		soxPath = db.getExternal(DatabaseLayout.SOXPATH);
		sep = System.getProperty("file.separator");
		dataDir = voiceDir + "hts" + sep + "data" + sep;
		scriptsDir = dataDir + "scripts" + sep;

		// For both speaker indep. or adapt scripts the programs are the same
		// check again that all the external necessary programs are installed.
		System.out.println("\nHMMVoiceDataPreparation:\nChecking paths of external programs");
		if (!checkExternalPaths())
			return false;

		if (getProp(ADAPTSCRIPTS).contentEquals("false")) {

			// 1. copy from $MARY_TTS/lib/external/hts directory in the voice building directory
			String sourceFolder = marybase + sep + "lib" + sep + "external" + sep + "hts";
			String htsFolder = voiceDir + sep + "hts";
			FileUtils.copyFolderRecursive(sourceFolder, htsFolder, false);

			// 2. check as before that wav, raw and text directories exist and are in the correct place
			System.out
					.println("\nChecking wav/raw and text directories and files for running HTS speaker independent training scripts...");

			// default locations of directories:
			String wavDirName = voiceDir + "wav";
			String textDirName = voiceDir + "text";
			String rawDirName = dataDir + "raw";
			String uttsDirName = dataDir + "utts";

			// 2.1 check raw and wav files:
			String userRawDirName = getProp(USERRAWDIR);
			if (existWithFiles(rawDirName)) {
				raw = true;
				// the raw files should be the same as in wav file, if wav file empty convert from raw --> wav
				if (!existWithFiles(wavDirName))
					convertRaw2Wav(rawDirName, wavDirName);
			} else {
				// check if the user has provided a raw directory
				if (!userRawDirName.equals("")) {
					File userRawDir = new File(userRawDirName);

					// check if user provided raw dir contains files
					if (existWithFiles(userRawDirName)) {
						// copy the user provided raw directory to hts/data/raw/
						System.out.println("Copying files from: " + userRawDirName + "  to: " + rawDirName);
						FileUtils.copyFolder(userRawDirName, rawDirName);

						// the raw files should be the same as in wav file, if wav file empty convert from raw --> wav
						if (!existWithFiles(wavDirName))
							convertRaw2Wav(rawDirName, wavDirName);

						raw = true;
					} else
						System.out.println("User provided raw directory: " + userRawDirName
								+ " does not exist or does not contain files\n");
				}
				// if we still do not have raw files...
				// then there must be a wav directory, check that it contains files, if so convert wav --> raw
				if (!raw) {
					System.out.println("Checking if " + wavDirName + " contains files");
					if (existWithFiles(wavDirName)) {
						convertWav2Raw(wavDirName, rawDirName);
						raw = true;
					} else {
						System.out.println("There are no wav files in " + wavDirName);
					}
				}
			}

			// 2.2 check text files:
			if (existWithFiles(textDirName)) {
				text = true;
			} else if (existWithFiles(uttsDirName)) {
				convertUtt2Text(uttsDirName, textDirName);
				text = true;
			} else {
				// check if the user has provided a utterance directory
				String userUttDirName = getProp(USERUTTDIR);
				if (!userUttDirName.equals("")) {
					// check if user provided utt dir contains files
					if (existWithFiles(userUttDirName)) {
						// convert utt --> text (transcriptions festival format --> MARY format)
						convertUtt2Text(userUttDirName, textDirName);
						text = true;
					} else
						System.out.println("User provided utterance directory: " + userUttDirName
								+ " does not exist or does not contain files\n");
				} else {
					System.out.println("\nThere are no text files in " + textDirName);
					text = false;
				}
			}

			if (raw && text) {
				System.out.println("\nHMMVoiceDataPreparation finished:\n"
						+ "HTS speaker independent scripts copied in current voice building directory --> hts\n"
						+ "wav/raw and text directories in place.");
				return true;
			} else
				return false;

		} else { // ADAPTSCRIPTS == true

			// Here it is checked that the raw files are in data/raw, wav, phonelab and phonefeatures must be
			// provided by the user...
			// 1. copy from $MARY_TTS/lib/external/hts-adapt directory in the voice building directory
			String sourceFolder = marybase + sep + "lib" + sep + "external" + sep + "hts-adapt";
			String htsFolder = voiceDir + sep + "hts";
			FileUtils.copyFolderRecursive(sourceFolder, htsFolder, false);

			// 2. check as before that wav, raw and text directories exist and are in the correct place
			// System.out.println("\nChecking raw directory for running HTS adaptive training scripts...");

			File dirSpeakersRaw = new File(dataDir + "/raw");

			String[] speakers;
			if (dirSpeakersRaw.exists() && dirSpeakersRaw.list().length > 0) {
				speakers = dirSpeakersRaw.list();
				for (int i = 0; i < speakers.length; i++) {
					File dirSpeakerRaw = new File(dataDir + "/raw/" + speakers[i]);
					if (dirSpeakerRaw.exists() && dirSpeakerRaw.list().length > 0) {
						raw = true;
					} else {
						System.out.println("Error: directory " + voiceDir + "/raw/" + speakers[i] + " does not contain files.");
						raw = false;
						break;
					}
				}
			} else {
				System.out.println("Error: directory " + voiceDir + "/raw does not contain files.");
				raw = false;
			}

			if (raw) {
				System.out.println("\nHMMVoiceDataPreparation finished:\n"
						+ "HTS adapt scripts copied in current voice building directory --> hts\n" + "raw directory in place.");
				return true;
			} else
				return false;

		}

	}

	/**
	 * Check the paths of all the necessary external programs
	 * 
	 * @throws Exception
	 *             Exception
	 * @return true if all the paths are defined
	 */
	private boolean checkExternalPaths() throws Exception {

		boolean result = true;

		if (db.getExternal(DatabaseLayout.AWKPATH) == null) {
			System.out.println("  *Missing path for awk");
			result = false;
		}
		if (db.getExternal(DatabaseLayout.PERLPATH) == null) {
			System.out.println("  *Missing path for perl");
			result = false;
		}
		if (db.getExternal(DatabaseLayout.BCPATH) == null) {
			System.out.println("  *Missing path for bc");
			result = false;
		}
		if (db.getExternal(DatabaseLayout.TCLPATH) == null) {
			System.out.println("  *Missing path for tclsh");
			result = false;
		}
		if (db.getExternal(DatabaseLayout.SOXPATH) == null) {
			System.out.println("  *Missing path for sox");
			result = false;
		}
		if (db.getExternal(DatabaseLayout.HTSPATH) == null) {
			System.out.println("  *Missing path for hts/htk");
			result = false;
		}
		if (db.getExternal(DatabaseLayout.HTSENGINEPATH) == null) {
			System.out.println("  *Missing path for hts_engine");
			result = false;
		}
		if (db.getExternal(DatabaseLayout.SPTKPATH) == null) {
			System.out.println("  *Missing path for sptk");
			result = false;
		}
		if (db.getExternal(DatabaseLayout.EHMMPATH) == null) {
			System.out.println("  *Missing path for ehmm");
			result = false;
		}

		if (!result)
			System.out
					.println("Please run MARYBASE/lib/external/check_install_external_programs.sh and check/install the missing programs");
		else
			System.out.println("Paths for all external programs are defined.");

		return result;
	}

	/**
	 * Checks if the directory exist and has files
	 * 
	 * @param dirName
	 *            dirName
	 * @return true if dir exists and dir.list has a greater length than 0, false otherwise
	 */
	private boolean existWithFiles(String dirName) {
		File dir = new File(dirName);
		if (dir.exists() && dir.list().length > 0)
			return true;
		else
			return false;
	}

	private void convertRaw2Wav(String rawDirName, String wavDirName) {
		String Fs = db.getProperty(DatabaseLayout.SAMPLINGRATE);
		String cmdLine;
		String raw2wavCmd = scriptsDir + "raw2wav.sh";
		System.out.println("Converting raw files to wav from: " + rawDirName + "  to: " + wavDirName);
		File wavDir = new File(wavDirName);
		if (!wavDir.exists())
			wavDir.mkdir();
		cmdLine = "chmod +x " + raw2wavCmd;
		General.launchProc(cmdLine, "raw2wav", voiceDir);
		cmdLine = raw2wavCmd + " " + soxPath + " " + rawDirName + " " + wavDirName + " " + Fs;
		General.launchProc(cmdLine, "raw2wav", voiceDir);
	}

	private void convertWav2Raw(String wavDirName, String rawDirName) {
		String cmdLine;
		String wav2rawCmd = scriptsDir + "wav2raw.sh";
		System.out.println("Converting wav files to raw from: " + wavDirName + "  to: " + rawDirName);
		File rawDir = new File(rawDirName);
		if (!rawDir.exists())
			rawDir.mkdir();
		cmdLine = "chmod +x " + wav2rawCmd;
		General.launchProc(cmdLine, "wav2raw", voiceDir);
		cmdLine = wav2rawCmd + " " + soxPath + " " + wavDirName + " " + rawDirName;
		General.launchProc(cmdLine, "wav2raw", voiceDir);
	}

	private void convertUtt2Text(String userUttDirName, String textDirName) {
		String cmdLine;
		String utt2transCmd = scriptsDir + "utt2trans.sh"; // festival to mary format
		System.out.println("\nConverting transcription files (festival format) to text from: " + userUttDirName + "  to: "
				+ textDirName);
		File textDir = new File(textDirName);
		if (!textDir.exists())
			textDir.mkdir();
		cmdLine = "chmod +x " + utt2transCmd;
		General.launchProc(cmdLine, "utt2trans", voiceDir);
		cmdLine = utt2transCmd + " " + userUttDirName + " " + textDirName;
		General.launchProc(cmdLine, "utt2trans", voiceDir);

	}

	/**
	 * Provide the progress of computation, in percent, or -1 if that feature is not implemented.
	 * 
	 * @return -1 if not implemented, or an integer between 0 and 100.
	 */
	public int getProgress() {
		return -1;
	}

}
