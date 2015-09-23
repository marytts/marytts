/**
 * Copyright 2000-2009 DFKI GmbH.
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
package marytts.tools.dbselection;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.text.DecimalFormat;

public class MaryScriptCreator {

	/**
	 * @param args
	 *            args
	 * @throws Exception
	 *             Exception
	 */
	public static void main(String[] args) throws Exception {
		// input: one line ( dummy "text" )
		// output: file named voice0001.txt containing text
		String prefix = System.getProperty("prefix", "prompt");
		File outDir = new File(System.getProperty("outdir", "./text"));
		String inputEncoding = System.getProperty("encoding", "UTF-8");
		boolean ignoreFirst = Boolean.parseBoolean(System.getProperty("ignoreFirst", "true"));
		boolean useFirstAsBasename = Boolean.parseBoolean(System.getProperty("useFirstAsBasename", "false"));
		if (!outDir.exists())
			outDir.mkdir();
		BufferedReader in = new BufferedReader(new InputStreamReader(System.in, inputEncoding));
		String line;
		DecimalFormat f = new DecimalFormat("0000");
		int i = 0;
		while ((line = in.readLine()) != null) {
			i++;
			// all we do is search for the first and last quotation marks; the rest is ignored
			// int first = line.indexOf('"');
			// int last = line.lastIndexOf('"');
			int first = 0;
			if (ignoreFirst)
				first = line.indexOf(" ") + 1;
			int last = line.length();
			if (first == -1 || last == -1 || last <= first) {
				System.err.println("Line no. " + i + " has no space -- skipping: " + line);
			}
			String filename;
			if (useFirstAsBasename) {
				filename = line.substring(0, line.indexOf(" "));
				if (!filename.endsWith(".txt"))
					filename += ".txt";
			} else {
				filename = prefix + f.format(i) + ".txt";
			}
			File outFile = new File(outDir, filename);
			PrintWriter out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(outFile), "UTF-8"));
			out.println(line.substring(first, last));
			out.close();
		}

	}

}
