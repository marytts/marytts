/**
 * Copyright 2009 DFKI GmbH.
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

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Vector;

import marytts.signalproc.analysis.Mfccs;
import marytts.util.io.General;

/**
 * A component for extracting mfcc files from wave files
 * 
 * @author Marcela, Sathish Pammi
 *
 */
public class SPTKMFCCExtractor extends VoiceImportComponent {

	public final String SPTKINST = "SPTKMFCCExtractor.sptkinstallationDir";
	public final String SOXCOMMAND = "SPTKMFCCExtractor.soxCommand";
	public final String INWAVDIR = "SPTKMFCCExtractor.inputWAVEDIR";
	public final String OUTMFCCDIR = "SPTKMFCCExtractor.outputMFCCDIR";
	private DatabaseLayout db;
	private String sCostDirectory;
	private String mfccExt = ".mfcc";
	protected int percent = 0;

	protected void setupHelp() {
		props2Help = new TreeMap();
		props2Help.put(SOXCOMMAND, "Sox binary file absolute path.  ex: /usr/bin/sox ");
		props2Help.put(SPTKINST, "SPTK installation directory. ex: /home/user/sw/SPTK-3.1/");
		props2Help.put(INWAVDIR, "Input wave directory");
		props2Help.put(OUTMFCCDIR, "Output MFCC directory");
	}

	public SortedMap<String, String> getDefaultProps(DatabaseLayout db) {
		this.db = db;
		if (props == null) {
			props = new TreeMap();
			props.put(SOXCOMMAND, "/usr/bin/sox");
			props.put(SPTKINST, "/home/user/sw/SPTK-3.1/");
			props.put(INWAVDIR, db.getProp(db.ROOTDIR) + File.separator + db.getProp(db.WAVDIR));
			props.put(OUTMFCCDIR, db.getProp(db.ROOTDIR) + File.separator + "sCost" + File.separator + "wavemfcc");
		}
		return props;
	}

	public final String getName() {
		return "SPTKMFCCExtractor";
	}

	@Override
	protected void initialiseComp() {
		// sCost dir creation, if doesn't exists
		sCostDirectory = db.getProp(db.ROOTDIR) + File.separator + "sCost";
		File sCostDir = new File(sCostDirectory);
		if (!sCostDir.exists()) {
			System.out.print(sCostDir.getAbsolutePath() + " does not exist; ");
			if (!sCostDir.mkdir()) {
				throw new Error("Could not create " + sCostDir.getAbsolutePath());
			}
			System.out.print("Created successfully.\n");
		}
	}

	public boolean compute() throws Exception {

		// check for SOX
		File soxFile = new File(getProp(SOXCOMMAND));
		if (!soxFile.exists()) {
			throw new IOException("SOX command setting is wrong. Because file " + soxFile.getAbsolutePath() + " does not exist");
		}

		// check for SPTK
		File sptkFile = new File(getProp(SPTKINST) + File.separator + "bin" + File.separator + "mcep");
		if (!sptkFile.exists()) {
			throw new IOException("SPTK path setting is wrong. Because file " + sptkFile.getAbsolutePath() + " does not exist");
		}

		// output mfcc dir creator
		File waveMFCCDir = new File(getProp(OUTMFCCDIR));
		if (!waveMFCCDir.exists()) {
			System.out.print(waveMFCCDir.getAbsolutePath() + " does not exist; ");
			if (!waveMFCCDir.mkdir()) {
				throw new Error("Could not create " + waveMFCCDir.getAbsolutePath());
			}
			System.out.print("Created successfully.\n");
		}

		// Now process all files, one by one
		for (int i = 0; i < bnl.getLength(); i++) {
			percent = 100 * i / bnl.getLength();
			String baseName = bnl.getName(i);
			String inFile = getProp(INWAVDIR) + File.separator + baseName + db.getProp(db.WAVEXT);
			String outFile = getProp(OUTMFCCDIR) + File.separator + baseName + mfccExt;
			getSptkMfcc(inFile, outFile);
		}

		return true;
	}

	public int getProgress() {
		return percent;
	}

	/***
	 * Calculate mfcc using SPTK, uses sox to convert wav&rarr;raw
	 * 
	 * @param inFile
	 *            inFile
	 * @param outFile
	 *            outFile
	 * @throws IOException
	 *             IOException
	 * @throws InterruptedException
	 *             InterruptedException
	 * @throws Exception
	 *             Exception
	 */
	public void getSptkMfcc(String inFile, String outFile) throws IOException, InterruptedException, Exception {

		String tmpFile = db.getProp(db.TEMPDIR) + File.separator + "tmp.mfc";
		String tmpRawFile = db.getProp(db.TEMPDIR) + File.separator + "tmp.raw";
		String cmd;

		// SPTK parameters
		int fs = 16000;
		int frameLength = 400;
		int frameLengthOutput = 512;
		int framePeriod = 80;
		int mgcOrder = 24;
		int mgcDimension = 25;
		// Mary header parameters
		double ws = (frameLength / fs); // window size in seconds
		double ss = (framePeriod / fs); // skip size in seconds

		// SOX and SPTK commands
		String sox = getProp(SOXCOMMAND);
		String x2x = " " + getProp(SPTKINST) + File.separator + "/bin/x2x";
		String frame = " " + getProp(SPTKINST) + File.separator + "/bin/frame";
		String window = " " + getProp(SPTKINST) + File.separator + "/bin/window";
		String mcep = " " + getProp(SPTKINST) + File.separator + "/bin/mcep";
		String swab = " " + getProp(SPTKINST) + File.separator + "/bin/swab";

		// convert the wav file to raw file with sox
		cmd = sox + " " + inFile + " " + tmpRawFile;
		General.launchProc(cmd, "sox", inFile);

		System.out.println("Extracting MGC coefficients from " + inFile);

		cmd = x2x + " +sf " + tmpRawFile + " | " + frame + " +f -l " + frameLength + " -p " + framePeriod + " | " + window
				+ " -l " + frameLength + " -L " + frameLengthOutput + " -w 1 -n 1 | " + mcep + " -a 0.42 -m " + mgcOrder
				+ "  -l " + frameLengthOutput + " | " + swab + " +f > " + tmpFile;

		System.out.println("cmd=" + cmd);
		General.launchBatchProc(cmd, "getSptkMfcc", inFile);

		// Now get the data and add the Mary header
		int numFrames;
		DataInputStream mfcData = null;
		Vector<Float> mfc = new Vector<Float>();

		mfcData = new DataInputStream(new BufferedInputStream(new FileInputStream(tmpFile)));
		try {
			while (true) {
				mfc.add(mfcData.readFloat());
			}
		} catch (EOFException e) {
		}
		mfcData.close();

		numFrames = mfc.size();
		int numVectors = numFrames / mgcDimension;
		Mfccs mgc = new Mfccs(numVectors, mgcDimension);

		int k = 0;
		for (int i = 0; i < numVectors; i++) {
			for (int j = 0; j < mgcDimension; j++) {
				mgc.mfccs[i][j] = mfc.get(k);
				k++;
			}
		}
		// Mary header parameters
		mgc.params.samplingRate = fs; /* samplingRateInHz */
		mgc.params.skipsize = (float) ss; /* skipSizeInSeconds */
		mgc.params.winsize = (float) ws; /* windowSizeInSeconds */

		mgc.writeMfccFile(outFile);

		// remove temp files
		// 1. tempmfcc file
		File tempFile = new File(tmpFile);
		if (tempFile.exists()) {
			tempFile.delete();
		}
		// 2. temp raw file
		tempFile = new File(tmpRawFile);
		if (tempFile.exists()) {
			tempFile.delete();
		}
	}

}
