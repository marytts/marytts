/**
 * Copyright 2006 DFKI GmbH.
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
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.SortedMap;
import java.util.TreeMap;

import marytts.util.io.StreamGobbler;

/**
 * Class to train sphinx labeler
 * 
 * @author Anna Hunecke
 * 
 */
public class SphinxTrainer extends VoiceImportComponent {

	private DatabaseLayout db;

	public final String STDIR = "SphinxTrainer.stDir";

	public final String getName() {
		return "SphinxTrainer";
	}

	public SortedMap getDefaultProps(DatabaseLayout db) {
		this.db = db;
		if (props == null) {
			props = new TreeMap();
			props.put(STDIR, db.getProp(db.ROOTDIR) + "st" + System.getProperty("file.separator"));
		}
		return props;
	}

	protected void setupHelp() {
		props2Help = new TreeMap();
		props2Help.put(STDIR, "directory containing all files used for training and labeling");
	}

	/**
	 * Do the computations required by this component.
	 * 
	 * @throws Exception
	 *             Exception
	 * @return true on success, false on failure
	 */
	public boolean compute() throws Exception {
		System.out.println("Training HMMs for Sphinx labeling ...");

		// Run the sphinxtrain scripts
		Runtime rtime = Runtime.getRuntime();

		// get a shell
		Process process = rtime.exec("/bin/bash");
		// get an output stream to write to the shell
		PrintWriter pw = new PrintWriter(new OutputStreamWriter(process.getOutputStream()));
		// go to directory where the scripts are
		pw.print("cd " + getProp(STDIR) + "\n");
		pw.flush();
		// call the scripts and exit
		pw.print("(scripts_pl/00.verify/verify_all.pl " + "; scripts_pl/01.vector_quantize/slave.VQ.pl "
				+ "; scripts_pl/02.ci_schmm/slave_convg.pl " + "; scripts_pl/03.makeuntiedmdef/make_untied_mdef.pl "
				+ "; scripts_pl/04.cd_schmm_untied/slave_convg.pl " + "; scripts_pl/05.buildtrees/make_questions.pl "
				+ "; scripts_pl/05.buildtrees/slave.treebuilder.pl " + "; scripts_pl/06.prunetree/slave.state-tie-er.pl "
				+ "; scripts_pl/07.cd-schmm/slave_convg.pl " + "; scripts_pl/08.deleted-interpolation/deleted_interpolation.pl "
				+ "; scripts_pl/09.make_s2_models/make_s2_models.pl " + "; exit)\n");
		pw.flush();
		pw.close();

		// collect the output
		// read from error stream
		StreamGobbler errorGobbler = new StreamGobbler(process.getErrorStream(), "err");

		// read from output stream
		StreamGobbler outputGobbler = new StreamGobbler(process.getInputStream(), "out");
		// start reading from the streams
		errorGobbler.start();
		outputGobbler.start();

		// close everything down
		process.waitFor();
		process.exitValue();
		System.out.println("... done.");
		return true;
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
