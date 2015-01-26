/**
 * Copyright 2000-2006 DFKI GmbH.
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
package marytts.modules;

import java.util.Locale;

import marytts.datatypes.MaryData;
import marytts.datatypes.MaryDataType;

/**
 * A dummy module doing nothing.
 *
 * @author Marc Schr&ouml;der
 */

public class DummyModule implements MaryModule {
	public String name() {
		return "Dummy";
	}

	@Deprecated
	public MaryDataType inputType() {
		return getInputType();
	}

	public MaryDataType getInputType() {
		return MaryDataType.TEXT;
	}

	@Deprecated
	public MaryDataType outputType() {
		return getOutputType();
	}

	public MaryDataType getOutputType() {
		return MaryDataType.AUDIO;
	}

	public Locale getLocale() {
		return null;
	}

	public void startup() throws Exception {
	}

	public void powerOnSelfTest() throws Error {
	}

	public void shutdown() {
	}

	public int getState() {
		return MODULE_OFFLINE;
	}

	public MaryData process(MaryData d) throws Exception {
		return d;
	}

}
