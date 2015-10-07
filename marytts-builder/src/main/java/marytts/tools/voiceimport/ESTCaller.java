/**
 * Portions Copyright 2006 DFKI GmbH.
 * Portions Copyright 2001 Sun Microsystems, Inc.
 * Portions Copyright 1999-2001 Language Technologies Institute, 
 * Carnegie Mellon University.
 * All Rights Reserved.  Use is subject to license terms.
 * 
 * Permission is hereby granted, free of charge, to use and distribute
 * this software and its documentation without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of this work, and to
 * permit persons to whom this work is furnished to do so, subject to
 * the following conditions:
 * 
 * 1. The code must retain the above copyright notice, this list of
 *    conditions and the following disclaimer.
 * 2. Any modifications must be clearly marked as such.
 * 3. Original authors' names are not deleted.
 * 4. The authors' names are not used to endorse or promote products
 *    derived from this software without specific prior written
 *    permission.
 *
 * DFKI GMBH AND THE CONTRIBUTORS TO THIS WORK DISCLAIM ALL WARRANTIES WITH
 * REGARD TO THIS SOFTWARE, INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS, IN NO EVENT SHALL DFKI GMBH NOR THE
 * CONTRIBUTORS BE LIABLE FOR ANY SPECIAL, INDIRECT OR CONSEQUENTIAL
 * DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR
 * PROFITS, WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS
 * ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE OF
 * THIS SOFTWARE.
 */
package marytts.tools.voiceimport;

import java.io.File;
import java.io.IOException;

import marytts.util.io.General;

/**
 * The ESTCaller class emulates the behaviour of the EST-calling shell scripts for the calculation of pitchmarks, lpc,
 * mel-cepstrum etc.
 * 
 * @author sacha
 * 
 */
/*
 * Note: some Java 5.0 compliant code, using a ProcessBuilder instead of a Runtime.exec() to call the external processes, has been
 * commented out but kept in case we change this design spec.
 */
public class ESTCaller {
	/* Fields */
	private DatabaseLayout db = null;
	private String ESTDIR = "/project/mary/Festival/speech_tools/";

	// private ProcessBuilder pb = null; // JAVA 5.0 compliant code

	/****************/
	/* CONSTRUCTORS */
	/****************/

	/**
	 * Constructor which gets the location of the EST speech tools from the database layout's config settings.
	 * 
	 * @param newDb
	 *            a database layout, please refer to the DatabaseLayout class for more info.
	 */
	public ESTCaller(DatabaseLayout newDb) {

		db = newDb;
		ESTDIR = db.getProp(DatabaseLayout.ESTDIR);

		/* Check if the ESTs can be found in ESTDIR: */
		checkEST();
		/* (This will throw a RuntimeException if the ESTs can't be found.) */

		/* Java 5.0 compliant code below. */
		/*
		 * pb = new ProcessBuilder( new String[]{" "} ); Map env = pb.environment(); env.put("ESTDIR", ESTDIR ); pb.directory(
		 * db.baseDir() ); pb.redirectErrorStream( true );
		 */

	}

	/*****************/
	/* OTHER METHODS */
	/*****************/

	/**
	 * Checks if the EST can be found with the given ESTDIR directory
	 * 
	 * @returns false if ESTDIR can be found and ch_wave can be run, true otherwise.
	 * 
	 * @throws RuntimeException
	 *             if the execution of ch_wave fails with an IOException (meaning ch_wave can't be found or executed).
	 */
	private void checkEST() {

		/* Check if ch_wave is present and executes with the given ESTDIR path */
		try {
			Runtime.getRuntime().exec(ESTDIR + "/bin/ch_wave -version");
		} catch (IOException e) {
			/* This IOException is thrown by exec when ch_wave can't be found or can't be run. */
			throw new RuntimeException("Running ch_wave from [" + ESTDIR + "] failed.\n"
					+ "Please check your ESTDIR environment variable, your specific EST path"
					+ " or the execution rights in this path.", e);
		}

		/*
		 * TODO: check for ALL the individual EST binaries that we use ? (i.e. pitchmark, sig2fv, etc.)
		 */

	}

	/**
	 * An equivalent to the make_pm_wave shell script
	 * 
	 * @param pitchmarksDirName
	 *            The layout of the processed database
	 * @param baseNameArray
	 *            The array of basenames of the .wav files to process
	 * @param pitchmarksExt
	 *            pitchmarksExt
	 * 
	 */
	public void make_pm_wave(String[] baseNameArray, String pitchmarksDirName, String pitchmarksExt) {

		System.out.println("---- Calculating the pitchmarks...");

		String cmdLine = null;

		/* For each file (or each basename): */
		for (int i = 0; i < baseNameArray.length; i++) {

			/* Make the command lines and launch them */
			/* - Scaling + resampling: */
			cmdLine = ESTDIR + "/bin/ch_wave -scaleN 0.9 -F 16000 " + "-o tmp" + baseNameArray[i] + db.getProp(db.WAVEXT) + " "
					+ db.getProp(db.WAVDIR) + baseNameArray[i] + db.getProp(db.WAVEXT);
			General.launchProc(cmdLine, "Pitchmarks ", baseNameArray[i]);

			/* Ensure the existence of the target pitchmark directory */
			File dir = new File(pitchmarksDirName);
			if (!dir.exists()) {
				System.out.println("Creating the directory [" + pitchmarksDirName + "].");
				dir.mkdir();
			}

			/* - Pitchmarks extraction: */
			cmdLine = ESTDIR
					+ "/bin/pitchmark -min 0.0057 -max 0.012 -def 0.01 -wave_end -lx_lf 140 -lx_lo 111 -lx_hf 80 -lx_ho 51 -med_o 0 -fill -otype est "
					+ "-o " + pitchmarksDirName + baseNameArray[i] + pitchmarksExt + " tmp" + baseNameArray[i]
					+ db.getProp(db.WAVEXT);
			General.launchProc(cmdLine, "Pitchmarks ", baseNameArray[i]);

			/* - Cleanup the temporary file: */
			cmdLine = "rm -f tmp" + baseNameArray[i] + db.getProp(db.WAVEXT);
			General.launchProc(cmdLine, "Pitchmarks ", baseNameArray[i]);

		}
		System.out.println("---- Pitchmarks done.");
	}

	/**
	 * An equivalent to the make_lpc shell script
	 * 
	 * @param correctedPitchmarksDirName
	 *            corrected pitchmarks with the directory name
	 * @param baseNameArray
	 *            The array of basenames of the .wav files to process
	 * @param correctedPitchmarksExt
	 *            correctedPitchmarksExt
	 * @param lpcDirName
	 *            lpcDirName
	 * @param lpcExt
	 *            lpcExt
	 * 
	 */
	public void make_lpc(String[] baseNameArray, String correctedPitchmarksDirName, String correctedPitchmarksExt,
			String lpcDirName, String lpcExt) {

		System.out.println("---- Calculating the LPC coefficents...");

		String cmdLine = null;

		/* For each file (or each basename): */
		for (int i = 0; i < baseNameArray.length; i++) {

			/* Ensure the existence of the target directory */
			File dir = new File(lpcDirName);
			if (!dir.exists()) {
				System.out.println("Creating the directory [" + lpcDirName + "].");
				dir.mkdir();
			}

			/* Make the command line */
			cmdLine = ESTDIR + "/bin/sig2fv "
					+ "-window_type hamming -factor 3 -otype est_binary -preemph 0.95 -coefs lpc -lpc_order 16 " + "-pm "
					+ correctedPitchmarksDirName + baseNameArray[i] + correctedPitchmarksExt + " -o " + lpcDirName
					+ baseNameArray[i] + lpcExt + " " + db.getProp(db.WAVDIR) + baseNameArray[i] + db.getProp(db.WAVEXT);
			// System.out.println( cmdLine );

			/* Launch the relevant process */
			General.launchProc(cmdLine, "LPC ", baseNameArray[i]);
		}
		System.out.println("---- LPC coefficients done.");
	}

	/**
	 * An equivalent to the make_mcep shell script
	 * 
	 * @param correctedPitchmarksDirName
	 *            The layout of the processed database
	 * @param baseNameArray
	 *            The array of basenames of the .wav files to process
	 * @param correctedPitchmarksExt
	 *            correctedPitchmarksExt
	 * @param mcepDirName
	 *            mcepDirName
	 * @param mcepExt
	 *            mcepExt
	 * 
	 */
	public void make_mcep(String[] baseNameArray, String correctedPitchmarksDirName, String correctedPitchmarksExt,
			String mcepDirName, String mcepExt) {

		System.out.println("---- Calculating the Mel-Cepstrum coefficents...");

		String cmdLine = null;

		/* For each file (or each basename): */
		for (int i = 0; i < baseNameArray.length; i++) {

			/* Ensure the existence of the target mel cepstrum directory */
			File dir = new File(mcepDirName);
			if (!dir.exists()) {
				System.out.println("Creating the directory [" + mcepDirName + "].");
				dir.mkdir();
			}

			/* Make the command line */
			cmdLine = ESTDIR
					+ "/bin/sig2fv "
					+ "-window_type hamming -factor 2.5 -otype est_binary -coefs melcep -melcep_order 12 -fbank_order 24 -shift 0.01 -preemph 0.97 "
					+ "-pm " + correctedPitchmarksDirName + baseNameArray[i] + correctedPitchmarksExt + " -o " + mcepDirName
					+ baseNameArray[i] + mcepExt + " " + db.getProp(db.WAVDIR) + baseNameArray[i] + db.getProp(db.WAVEXT);
			// System.out.println( cmdLine );
			/*
			 * Note: parameter "-delta melcep" has been commented out in the original script. Refer to the EST docs on
			 * http://www.cstr.ed.ac.uk/projects/speech_tools/manual-1.2.0/ for the meaning of the command line parameters.
			 */

			/* Launch the relevant process */
			System.out.println(baseNameArray[i]); // some feedback is always nice
			General.launchProc(cmdLine, "Mel-Cepstrum ", baseNameArray[i]);
		}
		System.out.println("---- Mel-Cepstrum coefficients done.");
	}

}