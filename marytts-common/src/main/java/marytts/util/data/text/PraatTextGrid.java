/**
 * Copyright 2010 DFKI GmbH.
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

// duplicates some functionality of mpi.eudico.server.corpora.clomimpl.praat (http://www.lat-mpi.eu/tools/elan/)

package marytts.util.data.text;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Vector;

/**
 * Representation of a Praat TextGrid annotation. A TextGrid consists of a number of tiers (which can be either TextTiers or, more
 * commonly, IntervalTiers), containing mutually independent label strings and their associated time points.
 * 
 * @author steiner
 * 
 */
public class PraatTextGrid {
	// start time of TextGrid
	private double xmin = Double.NaN;

	// end time of TextGrid
	private double xmax = Double.NaN;

	// tiers containing the actual data
	private Vector<PraatTier> tiers;

	/**
	 * bare constructor
	 */
	public PraatTextGrid() {
		setTiers(new Vector<PraatTier>());
	}

	/**
	 * constructor accepting a Vector of PraatTiers
	 * 
	 * @param tiers
	 *            tiers
	 */
	public PraatTextGrid(Vector<PraatTier> tiers) {
		setTiers(tiers);
	}

	/**
	 * getter for TextGrid start time. queries all tiers and sets start time to earliest time found
	 * 
	 * @return start time of TextGrid
	 */
	public double getXmin() {
		@SuppressWarnings("hiding")
		double xmin = Double.NaN;
		for (PraatTier tier : this.tiers) {
			if (Double.isNaN(xmin) || (!Double.isNaN(tier.getXmin()) && tier.getXmin() < xmin)) {
				xmin = tier.getXmin();
			}
		}
		return xmin;
	}

	/**
	 * getter for TextGrid end time. queries all tiers and sets end time to latest time found
	 * 
	 * @return end time of TextGrid
	 */
	public double getXmax() {
		@SuppressWarnings("hiding")
		double xmax = Double.NaN;
		for (PraatTier tier : this.tiers) {
			if (Double.isNaN(xmax) || (!Double.isNaN(tier.getXmax()) && tier.getXmax() > xmax)) {
				xmax = tier.getXmax();
			}
		}
		return xmax;
	}

	/**
	 * getter for number of tiers
	 * 
	 * @return number of tiers
	 */
	public int getNumberOfTiers() {
		return this.tiers.size();
	}

	/**
	 * getter for individual tier
	 * 
	 * @param index
	 *            of desired tier
	 * @return desired tier
	 */
	public PraatTier getTier(int index) {
		return this.tiers.get(index);
	}

	/**
	 * replace tiers by specified Vector of tiers
	 * 
	 * @param tiers
	 *            tiers
	 */
	public void setTiers(Vector<PraatTier> tiers) {
		this.tiers = tiers;
	}

	/**
	 * add a new tier at the end of the TextGrid's tiers
	 * 
	 * @param tier
	 *            to be appended
	 */
	public void appendTier(PraatTier tier) {
		this.tiers.add(tier);
	}

	/**
	 * write TextGrid to text file which can be used by Praat
	 * 
	 * @param fileName
	 *            of TextGrid file
	 * @throws IOException
	 *             IOException
	 */
	public void writeToFile(String fileName) throws IOException {
		FileWriter fw = new FileWriter(fileName);
		fw.append(this.toString());
		fw.close();
	}

	/**
	 * string representation of TextGrid, as it is written into text files by Praat and this.writeToFile(). Note that leading
	 * whitespace is not generated
	 */
	public String toString() {
		StringBuilder str = new StringBuilder();
		str.append("File type = \"ooTextFile\"\n");
		str.append("Object class = \"TextGrid\"");
		str.append("\n");
		str.append("xmin = " + getXmin() + " \n");
		str.append("xmax = " + getXmax() + " \n");
		str.append("tiers? <exists> \n");
		str.append("size = " + getNumberOfTiers() + " \n");
		str.append("item []: \n");
		for (int t = 0; t < getNumberOfTiers(); t++) {
			str.append("item [" + (t + 1) + "]:\n");
			str.append(getTier(t).toString());
		}
		return str.toString();
	}
}
