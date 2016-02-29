/**
 * Copyright 2007 DFKI GmbH.
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

package marytts.signalproc.analysis;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

import marytts.util.io.FileUtils;
import marytts.util.string.StringUtils;

/**
 * A simple class to generate a Praat script. Change the "main" function to generate different Praat scripts. Then use Praat to
 * run these scripts for automatically analyzing files.
 * 
 * @author oytun.turk
 * 
 */
public class PraatScriptGenerator {

	public static void main(String[] args) throws IOException {
		String wavFolder = args[0]; // Location of wav files to be analyzed
		String[] wavFiles = FileUtils.getFileList(wavFolder, "wav", true);

		if (wavFiles != null) {
			String scriptFile = args[1];
			FileOutputStream fos = new FileOutputStream(scriptFile);
			OutputStreamWriter out = new OutputStreamWriter(fos, "UTF-8");

			String outputFile;

			// Harmonicity analysis
			for (int i = 0; i < wavFiles.length; i++) {
				out.write("Read from file... " + wavFiles[i] + System.getProperty("line.separator"));
				out.write("To Harmonicity (cc)... 0.01 75 0.1 1.0" + System.getProperty("line.separator"));
				outputFile = StringUtils.modifyExtension(wavFiles[i], "hcc");
				out.write("Write to text file... " + outputFile + System.getProperty("line.separator"));
				out.write("select all" + System.getProperty("line.separator"));
				out.write("Remove" + System.getProperty("line.separator"));

				out.write("Read from file... " + wavFiles[i] + System.getProperty("line.separator"));
				out.write("To Harmonicity (ac)... 0.01 75 0.1 4.5" + System.getProperty("line.separator"));
				outputFile = StringUtils.modifyExtension(wavFiles[i], "hac");
				out.write("Write to text file... " + outputFile + System.getProperty("line.separator"));
				out.write("select all" + System.getProperty("line.separator"));
				out.write("Remove" + System.getProperty("line.separator"));

				out.write("Read from file... " + wavFiles[i] + System.getProperty("line.separator"));
				out.write("To Harmonicity (gne)... 500 4500 1000 80" + System.getProperty("line.separator"));
				outputFile = StringUtils.modifyExtension(wavFiles[i], "hgne");
				out.write("Write to text file... " + outputFile + System.getProperty("line.separator"));
				out.write("select all" + System.getProperty("line.separator"));
				out.write("Remove" + System.getProperty("line.separator"));

				System.out.println("Added harmonicity file " + String.valueOf(i + 1) + " of " + String.valueOf(wavFiles.length));
			}
			out.close();
			//

			// Jitter/shimmer analysis
			scriptFile = args[2];
			fos = new FileOutputStream(scriptFile);
			out = new OutputStreamWriter(fos, "UTF-8");
			for (int i = 0; i < wavFiles.length; i++) {
				out.write("Read from file... " + wavFiles[i] + System.getProperty("line.separator"));
				out.write("snd = selected (\"Sound\")" + System.getProperty("line.separator"));
				out.write("To Pitch (cc)... 0 60 15 yes 0.03 0.45 0.01 0.35 0.14 600" + System.getProperty("line.separator"));
				out.write("pit = selected (\"Pitch\")" + System.getProperty("line.separator"));
				out.write("select snd" + System.getProperty("line.separator"));
				out.write("plus pit" + System.getProperty("line.separator"));
				out.write("To PointProcess (cc)" + System.getProperty("line.separator"));

				out.write("# Jitter" + System.getProperty("line.separator"));
				out.write("jitter_local = Get jitter (local)... 0 0 0.0001 0.02 1.3" + System.getProperty("line.separator"));
				out.write("jitter_local_abs = Get jitter (local, absolute)... 0 0 0.0001 0.02 1.3"
						+ System.getProperty("line.separator"));
				out.write("rap = Get jitter (rap)... 0 0 0.0001 0.02 1.3" + System.getProperty("line.separator"));
				out.write("ppq = Get jitter (ppq5)... 0 0 0.0001 0.02 1.3" + System.getProperty("line.separator"));
				out.write("ddp = Get jitter (ddp)... 0 0 0.0001 0.02 1.3" + System.getProperty("line.separator"));

				out.write("# write values to file" + System.getProperty("line.separator"));
				outputFile = StringUtils.modifyExtension(wavFiles[i], "jit" + System.getProperty("line.separator"));
				out.write("fileappend \"jitterOut.txt\" 'fn$''tab$''jitter_local''tab$''jitter_local_abs''tab$''rap''tab$''ppq''tab$''ddp''newline$'"
						+ System.getProperty("line.separator"));

				out.write("# calculation of shimmer" + System.getProperty("line.separator"));
				out.write("plus snd" + System.getProperty("line.separator"));

				out.write("shim_local = Get shimmer (local)... 0 0 0.0001 0.02 1.3 1.6" + System.getProperty("line.separator"));
				out.write("shim_local_db = Get shimmer (local_dB)... 0 0 0.0001 0.02 1.3 1.6"
						+ System.getProperty("line.separator"));
				out.write("apq3 = Get shimmer (apq3)... 0 0 0.0001 0.02 1.3 1.6" + System.getProperty("line.separator"));
				out.write("apq5 = Get shimmer (apq5)... 0 0 0.0001 0.02 1.3 1.6" + System.getProperty("line.separator"));
				out.write("apq11 = Get shimmer (apq11)... 0 0 0.0001 0.02 1.3 1.6" + System.getProperty("line.separator"));
				out.write("dda = Get shimmer (dda)... 0 0 0.0001 0.02 1.3 1.6" + System.getProperty("line.separator"));

				out.write("# write values to file" + System.getProperty("line.separator"));
				outputFile = StringUtils.modifyExtension(wavFiles[i], "shi" + System.getProperty("line.separator"));
				out.write("fileappend \"shimmerOut.txt\" 'fn$''tab$''shim_local''tab$''shim_local_db''tab$''apq3''tab$''apq5''tab$''apq11''tab$''dda''newline$'"
						+ System.getProperty("line.separator"));

				System.out.println("Added jitter/shimmer file " + String.valueOf(i + 1) + " of "
						+ String.valueOf(wavFiles.length));

				out.write("select all" + System.getProperty("line.separator"));
				out.write("Remove" + System.getProperty("line.separator"));
			}
			out.close();
			//
		}
	}

}
