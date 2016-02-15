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
import marytts.unitselection.data.Unit;

/**
 * A statistical cost function
 * 
 * @author Sathish Pammi
 * 
 */
public interface StatisticalCostFunction {

	public double cost(Unit u1, Unit u2);

	/**
	 * Initialise this scost cost function by reading the appropriate settings from the MaryProperties using the given
	 * configPrefix.
	 * 
	 * @param configPrefix
	 *            the prefix for the (voice-specific) config entries to use when looking up files to load.
	 * @throws MaryConfigurationException
	 *             MaryConfigurationException
	 */
	public void init(String configPrefix) throws MaryConfigurationException;

}
