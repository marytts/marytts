/**
 * Copyright 2010 DFKI GmbH.
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
package marytts.signalproc.analysis;

import java.io.DataOutput;
import java.io.IOException;

public class VoiceQualityFileHeader extends FeatureFileHeader {

	public VoiceQualityFileHeader() {
		super();
	}

	public VoiceQualityFileHeader(VoiceQualityFileHeader existingHeader) {
		super(existingHeader);
	}

	public VoiceQualityFileHeader(String vqFile) {
		super(vqFile);
	}

	public boolean isIdenticalAnalysisParams(VoiceQualityFileHeader hdr) {
		return super.isIdenticalAnalysisParams(hdr);
	}

	@Override
	public void writeHeader(DataOutput ler) throws IOException {
		super.writeHeader(ler);
	}

}
