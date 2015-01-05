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
package marytts.unitselection.data;

import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;

import marytts.util.Pair;
import marytts.util.data.Datagram;

/**
 * Provides the actual timeline test case for the timeline reading/writing symmetry.
 */
public class TestableTimelineReader extends TimelineReader {

	public TestableTimelineReader(String fileName, boolean tryMemoryMapping) throws Exception {
		super(fileName, tryMemoryMapping);
	}

	@Override
	public Pair<ByteBuffer, Long> getByteBufferAtTime(long targetTimeInSamples) throws IOException, BufferUnderflowException {
		return super.getByteBufferAtTime(targetTimeInSamples);
	}

	@Override
	public long skipNextDatagram(ByteBuffer bb) throws IOException {
		return super.skipNextDatagram(bb);
	}

	@Override
	public Datagram getNextDatagram(ByteBuffer bb) {
		return super.getNextDatagram(bb);
	}

}
