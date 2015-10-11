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
package marytts.signalproc.analysis;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import marytts.signalproc.window.Window;

/**
 * Implements a structured header with file I/O functionality for binary files that store frame based line spectral frequency
 * vectors
 * 
 * @author Oytun T&uuml;rk
 */
public class LsfFileHeader extends FeatureFileHeader {
	public float preCoef; // Preemphasis coefficient
	public int windowType; // Type of analysis window (See class marytts.signalproc.window.Window for details
	public boolean isBarkScaled; // Bark-scaled or not

	public LsfFileHeader() {
		super();
		preCoef = 0.0f;
		windowType = Window.HAMMING;
		isBarkScaled = false;
	}

	public LsfFileHeader(LsfFileHeader existingHeader) {
		super(existingHeader);
		preCoef = existingHeader.preCoef;
		windowType = existingHeader.windowType;
		isBarkScaled = existingHeader.isBarkScaled;
	}

	public LsfFileHeader(String lsfFile) {
		super(lsfFile);

		try {
			readHeader(lsfFile);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public boolean isIdenticalAnalysisParams(LsfFileHeader hdr) {
		boolean bRet = super.isIdenticalAnalysisParams(hdr);

		if (!bRet)
			return false;

		if (this.preCoef != hdr.preCoef)
			return false;
		if (this.windowType != hdr.windowType)
			return false;
		if (this.isBarkScaled != hdr.isBarkScaled)
			return false;

		return bRet;
	}

	@Override
	public void readHeader(DataInput stream) throws IOException {
		if (stream != null) {
			super.readHeader(stream);

			preCoef = stream.readFloat();
			windowType = stream.readInt();
			isBarkScaled = stream.readBoolean();
		}
	}

	@Override
	public void writeHeader(DataOutput ler) throws IOException {
		super.writeHeader(ler);
		ler.writeFloat(preCoef);
		ler.writeInt(windowType);
		ler.writeBoolean(isBarkScaled);
	}
}
