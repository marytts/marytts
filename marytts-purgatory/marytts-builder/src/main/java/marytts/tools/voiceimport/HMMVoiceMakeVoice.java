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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.SortedMap;
import java.util.TreeMap;

public class HMMVoiceMakeVoice extends VoiceImportComponent {

	private DatabaseLayout db;
	private String name = "HMMVoiceMakeVoice";

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
			// props.put("command:", "perl hts/scripts/Training.pl hts/scripts/Config.pm");
		}
		return props;
	}

	protected void setupHelp() {
		props2Help = new TreeMap<String, String>();
		/*
		 * props2Help.put("command:", "This command can be executed on the command line by: " +
		 * "first going to the directory: /your_voice/hts/ " + "and in this directory execute: " + "\"make voice\" " +
		 * "a log file is created in this directory.");
		 */

	}

	/**
	 * Do the computations required by this component.
	 * 
	 * @throws Exception
	 *             Exception
	 * @return true on success, false on failure
	 */
	public boolean compute() throws Exception {

		String cmdLine;
		String voicedir = db.getProp(db.ROOTDIR);

		/* Run: perl hts/scripts/Training.pl hts/scripts/Config.pm (It can take several hours...) */
		cmdLine = db.getExternal(db.PERLPATH) + "/perl " + voicedir + "hts/scripts/Training.pl " + voicedir
				+ "hts/scripts/Config.pm";
		launchProcWithLogFile(cmdLine, "", voicedir);

		return true;
	}

	/**
	 * A general process launcher for the various tasks (copied from ESTCaller.java)
	 * 
	 * @param cmdLine
	 *            the command line to be launched.
	 * @param task
	 *            a task tag for error messages, such as "Pitchmarks" or "LPC".
	 * @param voicedir
	 *            voicedir
	 */
	private void launchProcWithLogFile(String cmdLine, String task, String voicedir) {

		Process proc = null;
		BufferedReader procStdout = null;
		String line = null;

		Date today;
		String output;
		SimpleDateFormat formatter;
		formatter = new SimpleDateFormat("yyyy.MM.dd-H:mm:ss", new Locale("en", "US"));
		today = new Date();
		output = formatter.format(today);
		String logFile = voicedir + "hts/log-" + output;

		System.out.println("\nRunning: " + cmdLine);
		System.out.println("\nThe training procedure can take several hours...");
		System.out.println("Detailed information about the training status can be found in the logfile:\n  " + logFile);
		System.out.println("\nTraining voice: " + db.getProp(db.VOICENAME));
		System.out.println("The following is general information about execution of training steps:");
		// String[] cmd = null; // Java 5.0 compliant code

		try {
			FileWriter log = new FileWriter(logFile);
			/* Java 5.0 compliant code below. */
			/* Hook the command line to the process builder: */
			ProcessBuilder pb = new ProcessBuilder(cmdLine.split(" "));
			pb.directory(new File(db.getProp(db.ROOTDIR)));
			pb.redirectErrorStream(true);
			/* Launch the process: */
			proc = pb.start();

			/* Collect process's combined stdout & stderr send it to System.out: */
			procStdout = new BufferedReader(new InputStreamReader(proc.getInputStream()));
			while ((line = procStdout.readLine()) != null) {
				if (line.contains("Start "))
					System.out.println("\nStep: " + line);
				log.write(line + "\n");
			}

			/* Wait and check the exit value */
			proc.waitFor();
			if (proc.exitValue() != 0) {
				BufferedReader errReader = new BufferedReader(new InputStreamReader(proc.getErrorStream()));
				while ((line = errReader.readLine()) != null) {
					System.err.println("ERR> " + line);
				}
				errReader.close();
				throw new RuntimeException(task + " computation failed on file [" + voicedir + "]!\n" + "Command line was: ["
						+ cmdLine + "].");
			}
			log.close();
		} catch (IOException e) {
			throw new RuntimeException(task + " computation provoked an IOException on file [" + voicedir + "].", e);
		} catch (InterruptedException e) {
			throw new RuntimeException(task + " computation interrupted on file [" + voicedir + "].", e);
		}

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
