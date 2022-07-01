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
 */
public class DynamicTwoHalvesWindow extends DynamicWindow {
	protected double prescale;

	public DynamicTwoHalvesWindow(int windowType) {
		super(windowType);
		prescale = 1.;
	}

	public DynamicTwoHalvesWindow(int windowType, double prescale) {
		super(windowType);
		this.prescale = prescale;
	}

	/**
	 * apply the left half of a window of the specified type to the data. The left half will be as long as the given len.
	 *
	 * @param data
	 *            data
	 * @param off
	 *            off
	 * @param len
	 *            len
	 */
	public void applyInlineLeftHalf(double[] data, int off, int len) {
		Window w = Window.get(windowType, 2 * len, prescale);
		w.apply(data, off, data, off, 0, len);
	}

	/**
	 * apply the right half of a window of the specified type to the data. The right half will be as long as the given len.
	 *
	 * @param data
	 *            data
	 * @param off
	 *            off
	 * @param len
	 *            len
	 */
	public void applyInlineRightHalf(double[] data, int off, int len) {
		Window w = Window.get(windowType, 2 * len, prescale);
		w.apply(data, off, data, off, len, len);
	}
}
