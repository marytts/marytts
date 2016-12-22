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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import marytts.util.io.StreamGobbler;

/**
 * Class to call Wagon
 * 
 * @author sacha, Anna Hunecke, Marc Schröder
 * 
 */
public class WagonCaller {

	// the Edinburgh Speech tools directory
	private String ESTDIR = "/project/mary/Festival/speech_tools/";
	// the file containing the feature definitions
	private String featureDefFile;

	/**
	 * Build a new WagonCaller
	 * 
	 * @param ESTDIR
	 *            the EST directory
	 * @param featureDescFile
	 *            the feature definition file
	 */
	public WagonCaller(String ESTDIR, String featureDescFile) {
		this.ESTDIR = ESTDIR;
		this.featureDefFile = featureDescFile;
		// try out if wagon runs
		try {
			System.out.print("Test call of wagon ... ");
			Runtime.getRuntime().exec(ESTDIR + "/bin/wagon -version");
			System.out.print("Ok!\n");
		} catch (IOException e) {
			throw new RuntimeException("Test run of wagon failed! \n" + "Please check the installation path of EST: " + ESTDIR
					+ " or the execution rights in this path.", e);
		}
	}

	/**
	 * Build a new WagonCaller
	 * 
	 * @param featureDescFile
	 *            the feature definition file
	 */
	public WagonCaller(String featureDescFile) {
		// Read the environment variable ESTDIR from the system:
		this.featureDefFile = featureDescFile;
		String getESTDIR = System.getProperty("ESTDIR");
		if (getESTDIR == null) {
			System.out.println("Warning: The environment variable ESTDIR was not found on your system.");
			System.out.println("         Defaulting ESTDIR to [" + ESTDIR + "].");
		} else
			ESTDIR = getESTDIR;

		// try out if wagon runs
		try {
			System.out.print("Test call of wagon ... ");
			Runtime.getRuntime().exec(ESTDIR + "/bin/wagon -version");
			System.out.print("Ok!\n");
		} catch (IOException e) {
			throw new RuntimeException("Test run of wagon failed! \n" + "Please check the installation path of EST: " + ESTDIR
					+ " or the execution rights in this path.", e);
		}
	}

	/**
	 * Call the wagon program
	 * 
	 * @param valueFile
	 *            the file containing the values of the units
	 * @param distanceTableFile
	 *            the distance tables for the units
	 * @param destinationFile
	 *            the file to dump the tree to
	 * @return true on success, false on failure
	 */
	public boolean callWagon(String valueFile, String distanceTableFile, String destinationFile) {
		return callWagon("-desc " + featureDefFile + " -data " + valueFile + " -balance 0" + " -distmatrix " + distanceTableFile
				+ " -stop 10" + " -output " + destinationFile + " -verbose");
	}

	/**
	 * Call the wagon program This method allows to set the stop and balance values
	 * 
	 * @param valueFile
	 *            the file containing the values of the units
	 * @param distanceTableFile
	 *            the distance tables for the units
	 * @param destinationFile
	 *            the file to dump the tree to
	 * @param balance
	 *            if balance = 0 stop is used; else if number of indices at node divided by balance is greater than stop it is
	 *            used as stop
	 * @param stop
	 *            minimum number of indices in leaf
	 * @return true on success, false on failure
	 */
	public boolean callWagon(String valueFile, String distanceTableFile, String destinationFile, int balance, int stop) {
		return callWagon("-desc " + featureDefFile + " -data " + valueFile + " -balance " + balance + " -distmatrix "
				+ distanceTableFile + " -stop " + stop + " -output " + destinationFile);
	}

	/**
	 * Call the wagon program with the given argument line. This method is for those who know what they are doing.
	 * 
	 * @param arguments
	 *            all arguments to wagon in one string
	 * @return true on success, false on failure
	 */
	public boolean callWagon(String arguments) {
		try {
			System.out.println("Calling wagon as follows:");
			System.out.println(ESTDIR + "/bin/wagon " + arguments);
			Process p = Runtime.getRuntime().exec(ESTDIR + "/bin/wagon " + arguments);
			// collect the output
			// read from error stream
			StreamGobbler errorGobbler = new StreamGobbler(p.getErrorStream(), "err");

			// read from output stream
			StreamGobbler outputGobbler = new StreamGobbler(p.getInputStream(), "out");
			// start reading from the streams
			errorGobbler.start();
			outputGobbler.start();
			p.waitFor();
			if (p.exitValue() == 0)
				return true;
			return false;
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("Exception running wagon");
		}
	}

}