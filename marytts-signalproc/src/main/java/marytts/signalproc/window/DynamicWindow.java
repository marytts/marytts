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
package marytts.signalproc.window;

import marytts.signalproc.process.InlineDataProcessor;

/**
 * @author Marc Schr&ouml;der
 * 
 */
public class DynamicWindow implements InlineDataProcessor {
	protected int windowType;

	/**
	 * An inline data processor applying a window of the requested type to the data. The window length will always be equal to the
	 * data length.
	 * 
	 * @param windowType
	 *            window type
	 */
	public DynamicWindow(int windowType) {
		this.windowType = windowType;
	}

	public double[] values(int len) {
		Window w = Window.get(windowType, len);
		return w.window;
	}

	/**
	 * apply a window of the specified type, with length len, to the data.
	 * 
	 * @param data
	 *            data
	 * @param off
	 *            off
	 * @param len
	 *            len
	 */
	public void applyInline(double[] data, int off, int len) {
		Window w = Window.get(windowType, len);
		w.applyInline(data, off, len);
	}

}
