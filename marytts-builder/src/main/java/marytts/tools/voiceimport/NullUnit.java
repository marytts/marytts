/**
 * Copyright 2000-2009 DFKI GmbH.
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
package marytts.tools.voiceimport;

import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Null-Unit entry for the Catalog.
 */
public class NullUnit extends Unit {

	public NullUnit(String unitType) {
		super();
		this.unitType = unitType;
	}

	public boolean dumpTargetValues(DataOutputStream out) throws IOException {
		return false;
	}

	public void dumpJoinValues(DataOutputStream out, int numValues) throws IOException {
		for (int i = 0; i < numValues * 2; i++) {
			out.writeFloat(0);
		}
	}

}
