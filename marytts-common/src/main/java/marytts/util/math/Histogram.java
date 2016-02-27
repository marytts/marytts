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
package marytts.util.math;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DecimalFormat;

public class Histogram {

	// private data used internally by this class.
	private double[] m_hist;
	private double[] m_data;
	private double[] m_binCenters;
	private String m_name;
	private double m_min;
	private double m_max;
	private int m_nbins;
	private int m_entries;
	private double m_overflow;
	private double m_underflow;
	private boolean m_debug;
	private double m_bandwidth;

	public Histogram(double[] data) {
		double min = MathUtils.min(data);
		double max = MathUtils.max(data);
		setHistogram(data, 15, min, max);
	}

	/**
	 * A simple constructor
	 * 
	 * @param data
	 *            data
	 * @param nbins
	 *            nbins
	 */
	public Histogram(double[] data, int nbins) {
		double min = MathUtils.min(data);
		double max = MathUtils.max(data);
		setHistogram(data, nbins, min, max);
	}

	/**
	 * Constructor which sets name, number of bins, and range.
	 * 
	 * @param data
	 *            samples
	 * @param nbins
	 *            the number of bins the histogram should have. The range specified by min and max will be divided up into this
	 *            many bins.
	 * @param min
	 *            the minimum of the range covered by the histogram bins
	 * @param max
	 *            the maximum value of the range covered by the histogram bins
	 */
	public Histogram(double[] data, int nbins, double min, double max) {
		setHistogram(data, nbins, min, max);
	}

	/**
	 * Settings to Histogram
	 * 
	 * @param data
	 *            data
	 * @param nbins
	 *            nbins
	 * @param min
	 *            min
	 * @param max
	 *            max
	 */
	public void setHistogram(double[] data, int nbins, double min, double max) {
		m_nbins = nbins;
		m_min = min;
		m_max = max;
		m_data = data;
		m_hist = new double[m_nbins];
		m_binCenters = new double[m_nbins];
		m_underflow = 0;
		m_overflow = 0;
		setBandWidth();
		for (double x : m_data) {
			fill(x);
		}
		m_binCenters = this.setSampleArray();
	}

	public void changeSettings(int nbins) {
		m_nbins = nbins;
		m_binCenters = new double[m_nbins];
		m_hist = new double[m_nbins];
		setBandWidth();
		for (double x : m_data) {
			fill(x);
		}
		m_binCenters = this.setSampleArray();
	}

	/**
	 * Enter data into the histogram. The fill method takes the given value, works out which bin this corresponds to, and
	 * increments this bin by one.
	 * 
	 * @param x
	 *            is the value to add in to the histogram
	 */
	public void fill(double x) {
		// use findBin method to work out which bin x falls in
		BinInfo bin = findBin(x);
		// check the result of findBin in case it was an overflow or underflow
		if (bin.isUnderflow) {
			m_underflow++;
		}
		if (bin.isOverflow) {
			m_overflow++;
		}
		if (bin.isInRange) {
			m_hist[bin.index]++;
		}
		// print out some debug information if the flag is set
		if (m_debug) {
			System.out.println("debug: fill: value " + x + " # underflows " + m_underflow + " # overflows " + m_overflow
					+ " bin index " + bin.index);
		}
		// count the number of entries made by the fill method
		m_entries++;
	}

	/**
	 * Private class used internally to store info about which bin of the histogram to use for a number to be filled.
	 */
	private class BinInfo {
		public int index;
		public boolean isUnderflow;
		public boolean isOverflow;
		public boolean isInRange;
	}

	/**
	 * Private internal utility method to figure out which bin of the histogram a number falls in.
	 * 
	 * @return info on which bin x falls in.
	 */
	private BinInfo findBin(double x) {
		BinInfo bin = new BinInfo();
		bin.isInRange = false;
		bin.isUnderflow = false;
		bin.isOverflow = false;
		// first check if x is outside the range of the normal histogram bins
		if (x < m_min) {
			bin.isUnderflow = true;
		} else if (x > m_max) {
			bin.isOverflow = true;
		} else {
			// search for histogram bin into which x falls
			double binWidth = this.getBandWidth(); // (m_max - m_min)/m_nbins;
			for (int i = 0; i < m_nbins; i++) {
				double highEdge = m_min + (i + 1) * binWidth;
				if (x <= highEdge) {
					bin.isInRange = true;
					bin.index = i;
					break;
				}
			}
		}
		return bin;

	}

	public void setBandWidth() {
		m_bandwidth = (m_max - m_min) / m_nbins;
	}

	public double getBandWidth() {
		return m_bandwidth;
	}

	/**
	 * Save the histogram data to a file. The file format is very simple, human-readable text so it can be imported into Excel or
	 * cut and pasted into other applications.
	 * 
	 * @param fileName
	 *            name of the file to write the histogram to. Note this must be valid for your operating system, e.g. a unix
	 *            filename might not work under windows
	 * @exception IOException
	 *                if file cannot be opened or written to.
	 */

	public void writeToFile(String fileName) throws IOException {
		PrintWriter outfile = new PrintWriter(new FileOutputStream(fileName));
		outfile.println("// Output from Histogram class");
		outfile.println("// metaData: ");
		// outfile.println("name \"" + m_name + "\"");
		outfile.println("bins " + m_nbins);
		outfile.println("min " + m_min);
		outfile.println("max " + m_max);
		outfile.println("totalEntries " + m_entries);
		outfile.println("underflow " + m_underflow);
		outfile.println("overflow " + m_overflow);
		outfile.println("// binData:");
		for (int i = 0; i < m_nbins; i++) {
			outfile.println(i + " " + m_binCenters[i] + " " + m_hist[i]);
		}
		outfile.println("// end.");
		outfile.close();
	}

	/**
	 * Print the histogram data to the console. Output is only basic, intended for debugging purposes. A good example of formatted
	 * output.
	 */
	public void show() {
		DecimalFormat df = new DecimalFormat(" ##0.00;-##0.00");
		double binWidth = (m_max - m_min) / m_nbins;
		// System.out.println ("Histogram \"" + m_name +
		// "\", " + m_entries + " entries");
		System.out.println(" bin range        height");
		for (int i = 0; i < m_nbins; i++) {
			double binLowEdge = m_min + i * binWidth;
			double binHighEdge = binLowEdge + binWidth;
			System.out.println(df.format(binLowEdge) + " to " + df.format(binHighEdge) + "   " + df.format(m_hist[i]));
		}
	}

	public double[] setSampleArray() {
		double[] binCenters = new double[m_nbins];
		double binWidth = (m_max - m_min) / m_nbins;
		for (int i = 0; i < m_nbins; i++) {
			double binLowEdge = m_min + i * binWidth;
			double binHighEdge = binLowEdge + binWidth;
			binCenters[i] = (binLowEdge + binHighEdge) / 2.0;
		}
		return binCenters;
	}

	/**
	 * Get number of entries in the histogram. This should correspond to the number of times the fill method has been used.
	 * 
	 * @return number of entries
	 */
	public int entries() {
		return m_entries;
	}

	/**
	 * Get the name of the histogram. The name is an arbitrary label for the user, and is set by the constructor.
	 * 
	 * @return histogram name
	 */
	public String name() {
		return m_name;
	}

	/**
	 * Get the number of bins in the histogram. The range of the histogram defined by min and max is divided into this many bins.
	 * 
	 * @return number of bins
	 */
	public int numberOfBins() {
		return m_nbins;
	}

	/**
	 * Get lower end of histogram range
	 * 
	 * @return minimum x value covered by histogram
	 */
	public double min() {
		return m_min;
	}

	public double mean() {
		return MathUtils.mean(m_data);
	}

	public double variance() {
		return MathUtils.variance(m_data);
	}

	public double stdDev() {
		return MathUtils.standardDeviation(m_data);
	}

	/**
	 * Get upper end of histogram range
	 * 
	 * @return maximum x value covered by histogram
	 */
	public double max() {
		return m_max;
	}

	/**
	 * Get the height of the overflow bin. Any value passed to the fill method which falls above the range of the histogram will
	 * be counted in the overflow bin.
	 * 
	 * @return number of overflows
	 */
	public double overflow() {
		return m_overflow;
	}

	/**
	 * Get the height of the underflow bin. Any value passed to the fill method which falls below the range of the histogram will
	 * be counted in the underflow bin.
	 * 
	 * @return number of underflows
	 */
	public double underflow() {
		return m_underflow;
	}

	/**
	 * This method gives you the bin contents in the form of an array. It might be useful for example if you want to use the
	 * histogram in some other way, for example to pass to a plotting package.
	 * 
	 * @return array of bin heights
	 */
	public double[] getHistArray() {
		return m_hist;
	}

	public double[] getSampleArray() {
		return m_binCenters;
	}

	public double[] getDataArray() {
		return m_data;
	}

	/**
	 * Set debug flag.
	 * 
	 * @param flag
	 *            debug flag (true or false)
	 */
	public void setDebug(boolean flag) {
		m_debug = flag;
	}

	/**
	 * Get debug flag.
	 * 
	 * @return value of debug flag (true or false)
	 */
	public boolean getDebug() {
		return m_debug;
	}

}
