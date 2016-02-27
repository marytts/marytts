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
package marytts.signalproc.adaptation.prosody;

/**
 * A collection of PitchStatistics.
 * 
 * @author Oytun T&uuml;rk
 * 
 */
public class PitchStatisticsCollection {
	public PitchStatistics[] entries;

	public PitchStatisticsCollection() {
		this(0);
	}

	public PitchStatisticsCollection(int numEntries) {
		allocate(numEntries);
	}

	public PitchStatisticsCollection(PitchStatisticsCollection existing) {
		allocate(existing.entries.length);

		for (int i = 0; i < existing.entries.length; i++)
			entries[i] = new PitchStatistics(existing.entries[i]);
	}

	public void allocate(int numEntries) {
		entries = null;
		if (numEntries > 0)
			entries = new PitchStatistics[numEntries];
	}

	public PitchStatistics getGlobalStatisticsSourceHz() {
		return getGlobalStatistics(PitchStatistics.STATISTICS_IN_HERTZ, true);
	}

	public PitchStatistics getGlobalStatisticsTargetHz() {
		return getGlobalStatistics(PitchStatistics.STATISTICS_IN_HERTZ, false);
	}

	public PitchStatistics getGlobalStatisticsSourceLogHz() {
		return getGlobalStatistics(PitchStatistics.STATISTICS_IN_LOGHERTZ, true);
	}

	public PitchStatistics getGlobalStatisticsTargetLogHz() {
		return getGlobalStatistics(PitchStatistics.STATISTICS_IN_LOGHERTZ, false);
	}

	public PitchStatistics getGlobalStatistics(int statisticsType, boolean isSource) {
		PitchStatistics p = null;

		PitchStatisticsCollection c = getStatistics(true, statisticsType, isSource);
		if (c != null)
			p = new PitchStatistics(c.entries[0]);

		return p;
	}

	public PitchStatisticsCollection getLocalStatisticsSourceHz() {
		return getLocalStatistics(PitchStatistics.STATISTICS_IN_HERTZ, true);
	}

	public PitchStatisticsCollection getLocalStatisticsTargetHz() {
		return getLocalStatistics(PitchStatistics.STATISTICS_IN_HERTZ, false);
	}

	public PitchStatisticsCollection getLocalStatisticsSourceLogHz() {
		return getLocalStatistics(PitchStatistics.STATISTICS_IN_LOGHERTZ, true);
	}

	public PitchStatisticsCollection getLocalStatisticsTargetLogHz() {
		return getLocalStatistics(PitchStatistics.STATISTICS_IN_LOGHERTZ, false);
	}

	public PitchStatisticsCollection getLocalStatistics(int statisticsType, boolean isSource) {
		return getStatistics(false, statisticsType, isSource);
	}

	public PitchStatisticsCollection getStatistics(boolean isGlobal, int statisticsType, boolean isSource) {
		PitchStatisticsCollection c = null;

		if (entries != null) {
			int total = 0;
			int i;
			for (i = 0; i < entries.length; i++) {
				if (entries[i].isGlobal == isGlobal && entries[i].type == statisticsType && entries[i].isSource == isSource)
					total++;
			}

			if (total > 0) {
				c = new PitchStatisticsCollection(total);

				int count = 0;
				for (i = 0; i < entries.length; i++) {
					if (entries[i].isGlobal == isGlobal && entries[i].type == statisticsType && entries[i].isSource == isSource
							&& count < total) {
						c.entries[count] = new PitchStatistics(entries[i]);
						count++;
					}
				}

			}
		}

		return c;
	}
}
