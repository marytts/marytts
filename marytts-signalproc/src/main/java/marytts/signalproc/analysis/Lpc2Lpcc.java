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
package marytts.signalproc.analysis;

import java.io.IOException;

import marytts.util.data.ESTTrackReader;
import marytts.util.data.ESTTrackWriter;

/**
 * 
 * Internally does the conversion between LPCs and LPCCs.
 * 
 */
public class Lpc2Lpcc {
	private static float[][] convertData(float[][] lpc, int cepstrumOrder) {
		int nLPC = lpc[0].length;
		double gain = 0;
		double[] a = new double[nLPC];
		a[0] = 1.0; // Set a[0] to 1.0 once and for all
		double[] c = new double[cepstrumOrder + 1];
		float[][] lpcc = new float[lpc.length][cepstrumOrder + 1];

		// For each LPC vector:
		for (int i = 0; i < lpc.length; i++) {
			// Dereference the gain, stored as a[0] in the EST format
			gain = (double) (lpc[i][0]);
			// Cast the LPC coeffs from float to double
			// Note: a[0] has been permanently set to one in the above.
			for (int k = 1; k < nLPC; k++) {
				a[k] = (double) (lpc[i][k]);
			}
			// Do the conversion
			c = CepstrumLPCAnalyser.lpc2lpcc(a, gain, cepstrumOrder);
			// Cast the cesptrum back to floats
			for (int k = 0; k <= cepstrumOrder; k++) {
				lpcc[i][k] = (float) (c[k]);
			}
			// Note: lpcc[i][0] is now set to log(gain).
		}
		return (lpcc);
	}

	/**
	 * A method to convert between two files, from LPCs to LPCCs in EST format.
	 * 
	 * @param cepstrumOrder
	 *            The requested cepstrum order.
	 * @param inFileName
	 *            The name of the input file.
	 * @param outFileName
	 *            The name of the output file.
	 * 
	 * @throws IOException
	 *             IOException
	 */
	public static void convert(String inFileName, String outFileName, int cepstrumOrder) throws IOException {
		// Load the input file
		ESTTrackReader etr = new ESTTrackReader(inFileName);
		// Convert
		float[][] lpcc = convertData(etr.getFrames(), cepstrumOrder);
		// Output the lpcc
		ESTTrackWriter etw = new ESTTrackWriter(etr.getTimes(), lpcc, "lpcc");
		etw.doWriteAndClose(outFileName, etr.isBinary(), etr.isBigEndian());

	}

	/**
	 * @param args
	 *            args
	 * @throws IOException
	 *             IOException
	 */
	public static void main(String[] args) throws IOException {
		// Usage: ESTlpcToESTlpcc cepstrumOrder inFileName outFileName
		convert(args[1], args[2], new Integer(args[0]).intValue());
	}

}
