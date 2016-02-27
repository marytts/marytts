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
package marytts.modules.prosody;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * The priorites of parts-of-speech for accent assignment. These priorities reflect the "probability" of a token to carry a pitch
 * accent. Lower priorities mean higher probabilities. Priority of 1 means unconditional accent assignment.
 */
public class POSAccentPriorities {
	private Properties priorities;

	public POSAccentPriorities(String propertiesFilename) throws IOException {
		priorities = new Properties();
		priorities.load(new FileInputStream(propertiesFilename));
	}

	/**
	 * Determine whether a part-of-speech always gets an accent.
	 * 
	 * @param pos
	 *            pos
	 * @return getPriority(pos) equals 1
	 */
	public boolean getsUnconditionalAccent(String pos) {
		return getPriority(pos) == 1;
	}

	/**
	 * Determine whether one part-of-speech is more likely to get an accent than another one.
	 * 
	 * @param posA
	 *            posA
	 * @param posB
	 *            posB
	 * @return getPriority(posA) &lt; getPriority(posB)
	 */
	public boolean moreAccentuated(String posA, String posB) {
		return getPriority(posA) < getPriority(posB);
	}

	/**
	 * Provide the priority of a part-of-speech for getting an accent. Lower values mean higher priority / probability.
	 * 
	 * @param pos
	 *            pos
	 * @return 100
	 */
	public int getPriority(String pos) {
		String value = priorities.getProperty(pos);
		if (value != null) {
			try {
				return Integer.parseInt(value);
			} catch (NumberFormatException e) {
			}
			// Invalid entries are treated as missing entries
		}
		return 100; // an arbitrary, very high value for missing entries
	}

}
