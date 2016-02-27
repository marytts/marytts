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
package marytts.signalproc.sinusoidal;

/**
 * A baseline class for all sinusoidal analyzers.
 * 
 * @author Oytun T&uuml;rk
 * 
 */
public class BaseSinusoidalAnalyzer {

	public static final int FIXEDRATE_FULLBAND_ANALYZER = 1;
	public static final int PITCHSYNCHRONOUS_FULLBAND_ANALYZER = 2;
	public static final int FIXEDRATE_MULTIRESOLUTION_ANALYZER = 3;
	public static final int PITCHSYNCHRONOUS_MULTIRESOLUTION_ANALYZER = 4;

}
