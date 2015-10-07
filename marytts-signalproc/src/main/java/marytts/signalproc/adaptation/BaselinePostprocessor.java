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
package marytts.signalproc.adaptation;

/**
 * Baseline class for post-processing operations after voice conversion
 * 
 * @author Oytun T&uuml;rk
 */
public class BaselinePostprocessor {
	public BaselinePostprocessor() {
		this(null);
	}

	public BaselinePostprocessor(BaselinePostprocessor existing) {
		if (existing != null) {
			// Copy class members if you add any
		} else {
			// Set default class member values
		}
	}

	public void run(BaselineAdaptationSet trainingSet) {

	}

}
