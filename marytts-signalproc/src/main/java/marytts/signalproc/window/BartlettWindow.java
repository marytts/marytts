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
package marytts.signalproc.window;

import marytts.util.math.MathUtils;

/**
 * @author Marc Schr&ouml;der
 *
 *         Implements a Bartlett window
 *
 */
public class BartlettWindow extends Window {
	public BartlettWindow(int length) {
		super(length);
	}

	public BartlettWindow(int length, double prescalingFactor) {
		super(length, prescalingFactor);
	}

	protected void initialise() {
		int halfN = window.length / 2;
		window[halfN] = prescalingFactor;
		double top = 2 * prescalingFactor;
		for (int i = 0; i < halfN; i++) {
			window[i] = top * i / (window.length - 1);
			window[halfN + i + 1] = prescalingFactor - window[i];
		}
	}

	public String toString() {
		return "Bartlett window";
	}
}
