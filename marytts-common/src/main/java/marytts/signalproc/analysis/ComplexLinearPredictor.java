/**
 * Copyright 2007 DFKI GmbH.
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
package marytts.signalproc.analysis;

import marytts.util.math.ComplexArray;

/**
 * A class for nesting results of complex valued linear prediction
 * 
 * @author Oytun T&uuml;rk
 * 
 */
public class ComplexLinearPredictor {
	public ComplexArray complexLPCoeffs;
	public ComplexArray reflectionCoeffs;
	public double variance; // LP gain squared
	public double gain;

	public ComplexLinearPredictor(int predictionOrder) {
		if (predictionOrder > 0) {
			complexLPCoeffs = new ComplexArray(predictionOrder);
			reflectionCoeffs = new ComplexArray(predictionOrder);
		} else {
			complexLPCoeffs = null;
			reflectionCoeffs = null;
		}
	}
}
