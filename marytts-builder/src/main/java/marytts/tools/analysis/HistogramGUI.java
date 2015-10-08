/**
 * Copyright 2008 DFKI GmbH.
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

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package marytts.tools.analysis;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;

/**
 * 
 * @author sathish
 */
public class HistogramGUI extends Thread {

	final double[] data;
	final int noBins;

	public HistogramGUI(double[] sdata) {
		this.data = sdata;
		this.noBins = 25;
		start();
	}

	public HistogramGUI(double[] sdata, int nbins) {
		this.data = sdata;
		this.noBins = nbins;
		start();
	}

	public HistogramGUI(String file, int column) throws Exception {
		this.data = this.getFileData(file, column);
		this.noBins = 25;
		start();
	}

	public HistogramGUI(String file) throws Exception {
		this.data = this.getFileData(file, 0);
		this.noBins = 25;
		start();
	}

	public void run() {
		DrawHistogram hgui = new DrawHistogram(this.data);
		hgui.setVisible(true);
	}

	public double[] getFileData(String file, int index) throws Exception {
		Double[] sdata;

		ArrayList<Double> arl = new ArrayList<Double>();
		BufferedReader bfr = new BufferedReader(new FileReader(new File(file)));
		String line;
		while ((line = bfr.readLine()) != null) {
			line = line.trim();
			String[] sval = line.split("\\s+");
			// **TODO: File reading by assuming it has a single value
			// * Need to modify again
			if (sval.length <= index) {
				throw new RuntimeException("the file '" + file + "' contains 0 to " + (sval.length - 1)
						+ " columns only. The column " + index + " is not available.");
			}
			double xval = Double.valueOf(sval[index]);
			arl.add(xval);
		}

		sdata = (Double[]) arl.toArray(new Double[arl.size()]);
		double[] xdata = new double[sdata.length];
		for (int i = 0; i < sdata.length; i++) {
			xdata[i] = sdata[i].doubleValue();
		}
		return xdata;

	}

	/**
	 * @param args
	 *            the command line arguments
	 * @throws Exception
	 *             Exception
	 */
	public static void main(String args[]) throws Exception {

		new HistogramGUI("/home/sathish/Work/test/time0017.mfcc", 12);

	}

}
