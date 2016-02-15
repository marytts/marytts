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
import java.io.IOException;
import java.util.SortedMap;
import java.util.TreeMap;

import marytts.exceptions.ExecutionException;
import marytts.exceptions.MaryConfigurationException;
import marytts.tools.voiceimport.DatabaseLayout;
import marytts.tools.voiceimport.VoiceImportComponent;
import marytts.util.io.BasenameList;
import marytts.util.io.FileUtils;
import marytts.util.io.General;

/**
 * MLSA feature files extractor for vocalizations. It extracts mgc features, strengths and logf0 features
 * 
 * @author sathish
 *
 */
public class MLSAFeatureFileComputer extends VoiceImportComponent {

	private String waveExt = ".wav";
	private String rawExt = ".raw";
	private String lf0Ext = ".lf0";
	private String strExt = ".str";
	private String mgcExt = ".mgc";
	private int progress = -1;

	// Default LF0 parameters
	private int SAMPFREQ = 16000;
	private int FRAMESHIFT = 80;
	private int LOWERF0_VALUE = 100;
	private int UPPERF0_VALUE = 500;
	// Default MGC parameters
	private int FRAMELEN = 400;
	private int FFTLEN = 512;
	private float FREQWARP = 0.42f;
	private int MGCORDER = 24; // MGCORDER is actually 24 plus 1 include MGC[0]
	// Default STR parameters
	private int STRORDER = 5;

	protected DatabaseLayout db = null;
	protected BasenameList bnlVocalizations;

	private final String WAVEDIR = getName() + ".vocalizationWaveDir";
	private final String MLSADIR = getName() + ".vocalizationMLSAFilesDir";
	private final String RAWDIR = getName() + ".rawFilesDir";
	private final String SCRIPTSDIR = getName() + ".scriptsDir";
	private final String TCLCOMMAND = getName() + ".tclsh-commandlinePath";
	private final String SOXCOMMAND = getName() + ".sox-commandlinePath";
	private final String SPTKPATH = getName() + ".SPTK-Path";
	private final String LOWERLF0 = getName() + ".LowerF0Value";
	private final String UPPERLF0 = getName() + ".UpperF0Value";

	@Override
	public SortedMap<String, String> getDefaultProps(DatabaseLayout db) {
		this.db = db;
		if (props == null) {
			props = new TreeMap();
			props.put(WAVEDIR, db.getProp(db.VOCALIZATIONSDIR) + File.separator + "wav");
			props.put(SCRIPTSDIR, db.getProp(db.VOCALIZATIONSDIR) + File.separator + "scripts");
			String mlsaDir = db.getProp(db.VOCALIZATIONSDIR) + "mlsa";
			props.put(MLSADIR, mlsaDir);
			props.put(RAWDIR, db.getProp(db.VOCALIZATIONSDIR) + File.separator + "raw");
			props.put(TCLCOMMAND, "/usr/bin/tclsh");
			props.put(SOXCOMMAND, "/usr/bin/sox");
			props.put(SPTKPATH, "/usr/bin/");
			props.put(LOWERLF0, LOWERF0_VALUE + "");
			props.put(UPPERLF0, UPPERF0_VALUE + "");
		}
		return props;
	}

	@Override
	protected void setupHelp() {
		if (props2Help == null) {
			props2Help = new TreeMap();
			props2Help.put(WAVEDIR, "directory that contains vocalization wave files ");
			props2Help.put(SCRIPTSDIR, "directory that contains external scripts used to compute MGC, LF0 and MGC features");
			props2Help.put(MLSADIR, "mlsa features directory");
			props2Help.put(RAWDIR, "raw files directory");
			props2Help.put(TCLCOMMAND, "tcl executable command");
			props2Help.put(SOXCOMMAND, "sox executable command");
			props2Help.put(SPTKPATH, "Path that contains SPTK executables");
			props2Help.put(LOWERLF0, "lowest pitch value specification");
			props2Help.put(UPPERLF0, "highest pitch value specification");
		}

	}

	/**
	 * compute logf0, mgc, strength features
	 * 
	 * @throws Exception
	 *             Exception
	 */
	@Override
	public boolean compute() throws Exception {

		copyFilesandScripts();
		convertWAVE2RAW();
		computeLF0Features();
		computeMGCFeatures();
		computeSTRFeatures();

		return true;
	}

	/**
	 * Initialize this component
	 * 
	 * @throws MaryConfigurationException
	 *             if there is problem with basename list
	 */
	@Override
	protected void initialiseComp() throws Exception {

		createDirectoryifNotExists(getProp(MLSADIR));
		createDirectoryifNotExists(getProp(RAWDIR));

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
			throw new MaryConfigurationException("Problem with basename list " + e);
		}
	}

	/**
	 * copy required scripts and files for third-party software execution
	 * 
	 * @throws Exception
	 *             if it can't copy files from source location
	 */
	private void copyFilesandScripts() throws Exception {

		String sep = System.getProperty("file.separator");

		createDirectoryifNotExists(getProp(SCRIPTSDIR));
		createDirectoryifNotExists(db.getProp(db.ROOTDIR) + sep + "filters");

		String logF0ShellScript = getProp(this.SCRIPTSDIR) + sep + "get_f0.sh";
		String logF0TCLScript = getProp(this.SCRIPTSDIR) + sep + "get_f0.tcl";
		String mgcShellScript = getProp(this.SCRIPTSDIR) + sep + "get_mgc.sh";
		String strShellScript = getProp(this.SCRIPTSDIR) + sep + "get_str.sh";
		String strTCLScript = getProp(this.SCRIPTSDIR) + sep + "get_str.tcl";
		String filterFile = db.getProp(db.ROOTDIR) + sep + "filters" + sep + "mix_excitation_filters.txt";
		String sourceDir = db.getProp(db.MARYBASE) + sep + "lib" + sep + "external" + sep + "vocalizations";

		if (!FileUtils.exists(logF0ShellScript)) {
			String sourceScript = sourceDir + sep + "scripts" + sep + "get_f0.sh";
			FileUtils.copy(sourceScript, logF0ShellScript);
			(new File(logF0ShellScript)).setExecutable(true);
		}

		if (!FileUtils.exists(logF0TCLScript)) {
			String sourceScript = sourceDir + sep + "scripts" + sep + "get_f0.tcl";
			FileUtils.copy(sourceScript, logF0TCLScript);
			(new File(logF0TCLScript)).setExecutable(true);
		}

		if (!FileUtils.exists(strShellScript)) {
			String sourceScript = sourceDir + sep + "scripts" + sep + "get_str.sh";
			FileUtils.copy(sourceScript, strShellScript);
			(new File(strShellScript)).setExecutable(true);
		}

		if (!FileUtils.exists(strTCLScript)) {
			String sourceScript = sourceDir + sep + "scripts" + sep + "get_str.tcl";
			FileUtils.copy(sourceScript, strTCLScript);
			(new File(strTCLScript)).setExecutable(true);
		}

		if (!FileUtils.exists(mgcShellScript)) {
			String sourceScript = sourceDir + sep + "scripts" + sep + "get_mgc.sh";
			FileUtils.copy(sourceScript, mgcShellScript);
			(new File(mgcShellScript)).setExecutable(true);
		}

		if (!FileUtils.exists(filterFile)) {
			String sourceScript = sourceDir + sep + "filters" + sep + "mix_excitation_filters.txt";
			FileUtils.copy(sourceScript, filterFile);
		}
	}

	/**
	 * Convert all WAV files to RAW files to support SPTK
	 * 
	 * @throws IOException
	 *             if can't run command
	 * @throws ExecutionException
	 *             if commandline throws an error
	 */
	private void convertWAVE2RAW() throws Exception {

		for (int i = 0; i < bnlVocalizations.getLength(); i++) {
			String waveFile = getProp(WAVEDIR) + File.separator + bnlVocalizations.getName(i) + waveExt;
			String rawFile = getProp(RAWDIR) + File.separator + bnlVocalizations.getName(i) + rawExt;
			String command = getProp(SOXCOMMAND) + " " + waveFile + " " + rawFile;
			// System.out.println( "Command: " + command );

			try {
				General.launchProc(command, "MLSAFeatureFileComputer", bnlVocalizations.getName(i));
			} catch (Exception e) {
				throw new ExecutionException("\nCommand failed : " + command + "\n" + e);
			}

			File rawEmptyFile = new File(rawFile);
			if (rawEmptyFile.length() <= 0) { // delete all empty files
				rawEmptyFile.delete();
			}
			System.out.println("Creating " + bnlVocalizations.getName(i) + " RAW file");

			File rawNFile = new File(rawFile);
			if (!rawNFile.exists()) {
				throw new ExecutionException("The following command failed: \n " + command + "\n");
			}
		}
	}

	/**
	 * Compute LF0 features for all RAW files using Snack and SPTK
	 * 
	 * @throws Exception
	 *             if can't run command
	 */
	private void computeLF0Features() throws Exception {
		String bcCommand = "/usr/bin/bc";

		for (int i = 0; i < bnlVocalizations.getLength(); i++) {
			String rawFile = getProp(RAWDIR) + File.separator + bnlVocalizations.getName(i) + rawExt;
			String lf0File = getProp(MLSADIR) + File.separator + bnlVocalizations.getName(i) + lf0Ext;
			String command = getProp(this.SCRIPTSDIR) + File.separator + "get_f0.sh " + bcCommand + " " + getProp(SPTKPATH) + " "
					+ getProp(TCLCOMMAND) + " " + getProp(this.SCRIPTSDIR) + " " + rawFile + " " + lf0File + " " + SAMPFREQ + " "
					+ FRAMESHIFT + " " + getProp(LOWERLF0) + " " + getProp(UPPERLF0);
			try {
				General.launchProc(command, "MLSAFeatureFileComputer", bnlVocalizations.getName(i));
			} catch (Exception e) {
				throw new ExecutionException("\nCommand failed : " + command + "\n" + e);
			}
			File lf0EmptyFile = new File(lf0File);
			if (lf0EmptyFile.length() <= 0) { // delete all empty files
				lf0EmptyFile.delete();
			}
			// System.out.println( "Command: " + command );
			File lf0NFile = new File(lf0File);
			if (!lf0NFile.exists()) {
				throw new ExecutionException("The following command failed: \n " + command + "\n");
			}
			System.out.println("Computed LF0 features for " + bnlVocalizations.getName(i));
		}
	}

	/**
	 * Compute MGC features for all RAW files using SPTK
	 * 
	 * @throws Exception
	 *             if can't run command
	 */
	private void computeMGCFeatures() throws Exception {

		for (int i = 0; i < bnlVocalizations.getLength(); i++) {
			String rawFile = getProp(RAWDIR) + File.separator + bnlVocalizations.getName(i) + rawExt;
			String mgcFile = getProp(MLSADIR) + File.separator + bnlVocalizations.getName(i) + mgcExt;
			String command = getProp(this.SCRIPTSDIR) + File.separator + "get_mgc.sh " + getProp(SPTKPATH) + " " + FRAMELEN + " "
					+ FRAMESHIFT + " " + FFTLEN + " " + FREQWARP + " " + MGCORDER + " " + rawFile + " " + mgcFile;
			try {
				General.launchProc(command, "MLSAFeatureFileComputer", bnlVocalizations.getName(i));
			} catch (Exception e) {
				throw new ExecutionException("\nCommand failed : " + command + "\n" + e);
			}
			File lf0EmptyFile = new File(mgcFile);
			if (lf0EmptyFile.length() <= 0) { // delete all empty files
				lf0EmptyFile.delete();
			}
			// System.out.println( "Command: " + command );

			File lf0NFile = new File(mgcFile);
			if (!lf0NFile.exists()) {
				throw new ExecutionException("The following command failed: \n " + command + "\n");
			}
			System.out.println("Computed MGC features for " + bnlVocalizations.getName(i));
		}
	}

	/**
	 * Compute STRENGTH features for all RAW files using SPTK
	 * 
	 * @throws Exception
	 *             if can't run command
	 */
	private void computeSTRFeatures() throws Exception {
		String bcCommand = "/usr/bin/bc";

		for (int i = 0; i < bnlVocalizations.getLength(); i++) {
			String rawFile = getProp(RAWDIR) + File.separator + bnlVocalizations.getName(i) + rawExt;
			String strFile = getProp(MLSADIR) + File.separator + bnlVocalizations.getName(i) + strExt;
			String command = getProp(this.SCRIPTSDIR) + File.separator + "get_str.sh " + bcCommand + " " + getProp(SPTKPATH)
					+ " " + getProp(TCLCOMMAND) + " " + getProp(this.SCRIPTSDIR) + " " + SAMPFREQ + " " + FRAMESHIFT + " "
					+ getProp(LOWERLF0) + " " + getProp(UPPERLF0) + " " + STRORDER + " " + rawFile + " " + strFile + " ";
			try {
				General.launchProc(command, "MLSAFeatureFileComputer", bnlVocalizations.getName(i));
			} catch (Exception e) {
				throw new ExecutionException("\nCommand failed : " + command + "\n" + e);
			}
			File lf0EmptyFile = new File(strFile);
			if (lf0EmptyFile.length() <= 0) { // delete all empty files
				lf0EmptyFile.delete();
			}
			// System.out.println( "Command: " + command );
			File lf0NFile = new File(strFile);
			if (!lf0NFile.exists()) {
				throw new ExecutionException("The following command failed: \n " + command + "\n");
			}
			System.out.println("Computed Strength features for " + bnlVocalizations.getName(i));
		}
	}

	/**
	 * Create new directory if the directory doesn't exist
	 * 
	 * @param dirName
	 *            dirName
	 * @throws Exception
	 *             Exception
	 */
	private void createDirectoryifNotExists(String dirName) throws Exception {
		if (!(new File(dirName)).exists()) {
			System.out.println(dirName + " directory does not exist; ");
			if (!(new File(dirName)).mkdirs()) {
				throw new Exception("Could not create directory " + dirName);
			}
			System.out.println("Created successfully.\n");
		}
	}

	/**
	 * Return this voice import component name
	 * 
	 * @return "MLSAFeatureFileComputer"
	 */
	@Override
	public String getName() {
		return "MLSAFeatureFileComputer";
	}

	/**
	 * Return the progress of this component
	 * 
	 * @return this.progress
	 */
	@Override
	public int getProgress() {
		return this.progress;
	}

	/**
	 * @param args
	 *            args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}
}
