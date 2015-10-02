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

import java.io.IOException;
import java.io.InputStream;

import marytts.exceptions.MaryConfigurationException;
import marytts.unitselection.data.Unit;

/**
 * A join cost function for evaluating the goodness-of-fit of a given pair of left and right unit.
 * 
 * @author Marc Schr&ouml;der
 *
 */
public interface JoinCostFunction {
	/**
	 * Compute the goodness-of-fit of joining two units, given the corresponding targets
	 * 
	 * @param t1
	 *            the left target
	 * @param u1
	 *            the proposed left unit
	 * @param t2
	 *            the right target
	 * @param u2
	 *            the proposed right unit
	 * @return a non-negative number; smaller values mean better fit, i.e. smaller cost.
	 */
	public double cost(Target t1, Unit u1, Target t2, Unit u2);

	/**
	 * Initialise this join cost function by reading the appropriate settings from the MaryProperties using the given
	 * configPrefix.
	 * 
	 * @param configPrefix
	 *            the prefix for the (voice-specific) config entries to use when looking up files to load.
	 * @throws MaryConfigurationException
	 *             if there is a configuration problem
	 */
	public void init(String configPrefix) throws MaryConfigurationException;

	/**
	 * Load weights and values from the given file
	 * 
	 * @param joinFileName
	 *            the file from which to read default weights and join cost features
	 * @param weightStream
	 *            an optional file from which to read weights, taking precedence over
	 * @param precompiledCostFileName
	 *            an optional file containing precompiled join costs
	 * @param wSignal
	 *            Relative weight of the signal-based join costs relative to the phonetic join costs computed from the target
	 * @throws IOException
	 *             IOException
	 * @throws MaryConfigurationException
	 *             MaryConfigurationException
	 */
	@Deprecated
	public void load(String joinFileName, InputStream weightStream, String precompiledCostFileName, float wSignal)
			throws IOException, MaryConfigurationException;

}
