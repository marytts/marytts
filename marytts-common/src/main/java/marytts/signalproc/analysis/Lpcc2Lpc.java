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
 * Internally does the conversion between LPCCs and LPCs.
 * 
 */
public class Lpcc2Lpc {
	private static float[][] convertData(float[][] lpcc, int lpcOrder) {

		int nCep = lpcc[0].length;
		double[] c = new double[nCep];
		double[] a = new double[lpcOrder + 1];
		float[][] lpc = new float[lpcc.length][lpcOrder + 1];

		// For each LPC vector:
		for (int i = 0; i < lpcc.length; i++) {
			// Cast the LPCC coeffs from float to double
			for (int k = 0; k < nCep; k++) {
				c[k] = (double) (lpcc[i][k]);
			}
			// Do the conversion
			a = CepstrumLPCAnalyser.lpcc2lpc(c, lpcOrder);
			// Recover the energy from the cepstrum vector
			lpc[i][0] = (float) (Math.exp(c[0]));
			// Cast the LPCs back to floats
			for (int k = 1; k <= lpcOrder; k++) {
				lpc[i][k] = (float) (a[k]);
			}
		}
		return (lpc);
	}

	/**
	 * A method to convert between two files, from LPCs to LPCCs in EST format.
	 * 
	 * @param lpcOrder
	 *            The requested cepstrum order.
	 * @param inFileName
	 *            The name of the input file.
	 * @param outFileName
	 *            The name of the output file.
	 * 
	 * @throws IOException
	 *             IO Exception
	 */
	public static void convert(String inFileName, String outFileName, int lpcOrder) throws IOException {
		// Load the input file
		ESTTrackReader etr = new ESTTrackReader(inFileName);
		// Convert
		float[][] lpc = convertData(etr.getFrames(), lpcOrder);
		// Output the lpcc
		ESTTrackWriter etw = new ESTTrackWriter(etr.getTimes(), lpc, "lpc");
		etw.doWriteAndClose(outFileName, etr.isBinary(), etr.isBigEndian());
	}

	/**
	 * @param args
	 *            args
	 * @throws IOException
	 *             IO Exception
	 */
	public static void main(String[] args) throws IOException {
		// Usage: ESTlpccToESTlpc lpcOrder inFileName outFileName
		convert(args[1], args[2], new Integer(args[0]).intValue());
	}

}
