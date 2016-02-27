/**
 * Copyright 2004-2006 DFKI GmbH.
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
package marytts.signalproc;

import marytts.signalproc.window.Window;

/**
 * 
 * @author Marc Schr&ouml;der
 * 
 *         A set of static getters for System properties.
 * 
 */
public class Defaults {
	public static int getWindowSize() {
		return Integer.getInteger("signalproc.default.windowsize", 512).intValue();
	}

	public static int getWindowType() {
		return Window.getTypeByName(System.getProperty("signalproc.default.window", "HAMMING"));
	}

	public static int getFFTSize() {
		return Integer.getInteger("signalproc.default.fftsize", 1024).intValue();
	}

	public static int getFrameShift() {
		int shift = Integer.getInteger("signalproc.default.frameshift", -1).intValue();
		if (shift == -1)
			shift = getWindowSize() / 2;
		return shift;
	}

}
