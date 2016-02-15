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
package marytts.unitselection.select;

import marytts.exceptions.MaryConfigurationException;
import marytts.server.MaryProperties;
import marytts.unitselection.data.DiphoneUnit;
import marytts.unitselection.data.SCostFileReader;
import marytts.unitselection.data.Unit;
import marytts.util.data.MaryHeader;

/**
 * StatisticalModelCost for a given unit
 * 
 * @author sathish pammi
 * 
 */
public class StatisticalModelCost implements StatisticalCostFunction {

	protected SCostFileReader sCostReader;
	protected float sCostWeight;

	/****************/
	/* DATA FIELDS */
	/****************/
	private MaryHeader hdr = null;

	/****************/
	/* CONSTRUCTORS */
	/****************/

	/**
	 * Empty constructor; when using this, call init() separately to initialise this class.
	 * 
	 * @see #init(String)
	 */
	public StatisticalModelCost() {
	}

	/**
	 * Initialise this scost function by reading the appropriate settings from the MaryProperties using the given configPrefix.
	 * 
	 * @param configPrefix
	 *            the prefix for the (voice-specific) config entries to use when looking up files to load.
	 * @throws MaryConfigurationException
	 *             MaryConfigurationException
	 */
	public void init(String configPrefix) throws MaryConfigurationException {
		try {
			String sCostFileName = MaryProperties.needFilename(configPrefix + ".sCostFile");
			sCostWeight = Float.parseFloat(MaryProperties.getProperty(configPrefix + ".sCostWeight", "1.0"));
			sCostReader = new SCostFileReader(sCostFileName);
		} catch (Exception e) {
			throw new MaryConfigurationException("Cannot initialise scost model", e);
		}
	}

	/*****************/
	/* ACCESSORS */
	/*****************/

	/**
	 * Get the number of units.
	 * 
	 * @return sCostReader.getNumberOfUnits()
	 */
	public int getNumberOfUnits() {
		return (sCostReader.getNumberOfUnits());
	}

	/*****************/
	/* MISC METHODS */
	/*****************/

	public double cost(Unit u1, Unit u2) {

		// Units of length 0 cannot be joined:
		if (u1.duration == 0 || u2.duration == 0)
			return Double.POSITIVE_INFINITY;
		// In the case of diphones, replace them with the relevant part:
		if (u1 instanceof DiphoneUnit) {
			u1 = ((DiphoneUnit) u1).right;
		}
		if (u2 instanceof DiphoneUnit) {
			u2 = ((DiphoneUnit) u2).left;
		}

		/**
		 * TODO uncomment below line to make ---> cost of successive units = 0
		 */
		// if (u1.getIndex()+1 == u2.getIndex()) return 0;

		double sCost1 = sCostReader.getSCost(u1.index);
		double sCost2 = sCostReader.getSCost(u2.index);

		return ((sCost1 + sCost2) / 2.0);
	}

	/**
	 * Cost function for a given units if consecutive == true, and if they are consecutive units make cost = 0
	 * 
	 * @param u1
	 *            u1
	 * @param u2
	 *            u2
	 * @param consecutive
	 *            consecutive
	 * @return ((sCost1 + sCost2) / 2.0)
	 */
	public double cost(Unit u1, Unit u2, boolean consecutive) {

		// Units of length 0 cannot be joined:
		if (u1.duration == 0 || u2.duration == 0)
			return Double.POSITIVE_INFINITY;
		// In the case of diphones, replace them with the relevant part:
		if (u1 instanceof DiphoneUnit) {
			u1 = ((DiphoneUnit) u1).right;
		}
		if (u2 instanceof DiphoneUnit) {
			u2 = ((DiphoneUnit) u2).left;
		}

		// if consecutive == true, and if they are consecutive units make cost = 0
		if (consecutive && (u1.index + 1 == u2.index))
			return 0;

		double sCost1 = sCostReader.getSCost(u1.index);
		double sCost2 = sCostReader.getSCost(u2.index);

		return ((sCost1 + sCost2) / 2.0);
	}

}
