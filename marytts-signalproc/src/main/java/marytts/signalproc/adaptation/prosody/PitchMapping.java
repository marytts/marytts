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
 * A class to support pitch transformation in voice conversion.
 * 
 * @author Oytun T&uuml;rk
 * 
 */
public class PitchMapping extends PitchTransformationData {
	// These are for feature requests from the codebook
	public static final int SOURCE = 1;
	public static final int TARGET = 2;
	public static final int SOURCE_TARGET = 3;
	public static final int TARGET_SOURCE = 4;
	//

	public PitchMappingFileHeader header; // Binary file header

	// These two contain identical information in different forms
	// f0Statistics is always read from the codebook first
	// Then f0StatisticsMapping is created from f0Statistics using the function setF0StatisticsMapping()
	public PitchStatisticsCollection f0StatisticsCollection;
	public PitchStatisticsMapping f0StatisticsMapping;

	//

	public PitchMapping() {
		this(0);
	}

	public PitchMapping(int totalF0StatisticsIn) {
		if (header == null)
			header = new PitchMappingFileHeader(totalF0StatisticsIn);

		allocate();
	}

	public void allocate() {
		allocate(header.totalF0StatisticsEntries);
	}

	public void allocate(int totalF0StatisticsIn) {
		if (totalF0StatisticsIn > 0) {
			f0StatisticsCollection = new PitchStatisticsCollection(totalF0StatisticsIn);
			header.totalF0StatisticsEntries = totalF0StatisticsIn;
		} else {
			f0StatisticsCollection = null;
			header.totalF0StatisticsEntries = 0;
		}
	}

	public void setF0StatisticsMapping() {
		if (f0StatisticsCollection != null)
			f0StatisticsMapping = new PitchStatisticsMapping(f0StatisticsCollection);
		else
			f0StatisticsMapping = null;
	}
}
